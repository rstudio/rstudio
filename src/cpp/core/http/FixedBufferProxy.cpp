/*
 * FixedBufferProxy.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <boost/algorithm/string/predicate.hpp>
#include <boost/asio/dispatch.hpp>
#include <boost/asio/error.hpp>
#include <boost/optional.hpp>

#include <core/http/FixedBufferProxy.hpp>
#include <core/http/Util.hpp>
#include <shared_core/SafeConvert.hpp>

using namespace boost::placeholders;

// Locked-region wrappers that tear this proxy down on an unexpected exception
// rather than logging and carrying on. Used in place of core/Thread.hpp's
// LOCK_MUTEX/END_LOCK_MUTEX everywhere in this file.
//
// That pair swallows anything escaping the locked block (via
// CATCH_UNEXPECTED_EXCEPTION) and then lets control fall out of the block into
// whatever follows -- which for this class is never survivable. In queueChunk()
// the statement that follows is `return true`, reporting a chunk as delivered
// when it was in fact dropped: the client gets a Content-Length-framed body
// with a hole in it and only a log line as evidence. A throw after
// clientWriteInProgress_ is latched but before the write is initiated is worse
// still -- no completion handler will ever run to clear it, so the proxy stalls
// with the upstream paused and both connections held open forever.
//
// bad_alloc is the realistic trigger: formatMessageAsHttpChunk() copies a piece
// up to maxChunkSize (1MB) on every body chunk, under exactly the memory
// pressure that motivated streaming in the first place.
//
// The outer try covers lock acquisition itself (boost::thread_resource_error,
// the one exception LOCK_MUTEX handled explicitly); the inner one covers the
// body, so its teardown runs with mutex_ still held and cannot interleave with
// a completion handler arriving on another thread. See failConnection().
// An unbalanced brace-splitting macro pair is not something clang-format can
// reason about: it flattens the inner and outer catch levels below to the same
// indent, losing the one distinction that matters here. Laid out by hand to
// match the LOCK_MUTEX/END_LOCK_MUTEX pair it stands in for.
// clang-format off
#define LOCK_MUTEX_OR_FAIL(m, context)                                         \
   try                                                                         \
   {                                                                           \
      boost::lock_guard<boost::mutex> lock(m);                                 \
      try                                                                      \
      {

#define END_LOCK_MUTEX_OR_FAIL(context)                                        \
      }                                                                        \
      catch (const std::exception& e)                                          \
      {                                                                        \
         failConnection(context, e.what());                                    \
      }                                                                        \
      catch (...)                                                              \
      {                                                                        \
         failConnection(context, "unknown exception");                         \
      }                                                                        \
   }                                                                           \
   catch (const std::exception& e)                                             \
   {                                                                           \
      failConnection(context, e.what());                                       \
   }                                                                           \
   catch (...)                                                                 \
   {                                                                           \
      failConnection(context, "unknown exception");                            \
   }
// clang-format on

namespace rstudio {
namespace core {
namespace http {

FixedBufferProxy::FixedBufferProxy(const boost::shared_ptr<AsyncConnection>& pClientConnection,
                                    uint64_t maxBufferSize) :
   pClientConnection_(pClientConnection),
   maxBufferSize_(maxBufferSize),
   wroteHeaders_(false),
   currentBufferSize_(0),
   bufferFull_(false)
{
}

void FixedBufferProxy::proxy(const boost::shared_ptr<IAsyncClient>& pServerConnection)
{
   // Snapshot the Set-Cookie headers already on the client connection's
   // response -- refreshed auth cookies, stamped during authentication before
   // the request reached the handler that built us (see
   // ServerSecureUriHandler's refreshAuthCookies call, which runs before it
   // dispatches). writeHeaders() carries them over onto the proxied response,
   // which would otherwise replace them wholesale.
   //
   // Read here rather than there because here is the one point where that
   // response is still ours alone: we are on the client connection's own
   // request-handling path, before the upstream request is executed, so no
   // other writer exists yet. By header-assembly time an upstream error
   // handler on another context may be mutating that same response on its way
   // to its own response claim, and this iteration would be racing it.
   for (const http::Header& header : pClientConnection_->response().headers())
   {
      if (boost::iequals(header.name, "Set-Cookie"))
         preservedCookies_.push_back(header);
   }

   pServerConnection_ = pServerConnection;
   pServerConnection_->setFixedBufferHandler(boost::bind(&FixedBufferProxy::queueChunk,
                                                          shared_from_this(),
                                                          _1, _2));

   // queueChunk() may decline (return false) under backpressure while
   // leaving the connection open, expecting to be resumed once writeChunk()
   // drains -- see setFixedBufferHandlerSupportsPause()'s declaration for why this
   // must be opted into rather than assumed for every FixedBufferHandler consumer.
   pServerConnection_->setFixedBufferHandlerSupportsPause(true);
}

// Choose the framing this proxy will use toward the client. Called once, at
// the first queueChunk(), and never revisited: everything it looks at
// (upstream status/headers, the client's own request) is already fixed by the
// time any body byte arrives.
FixedBufferProxy::Framing
FixedBufferProxy::decideFraming(const http::Response& response) const
{
   // RFC 7230 3.3.3 rule 1: "Any response to a HEAD request and any response
   // with a 1xx (Informational), 204 (No Content), or 304 (Not Modified)
   // status code is always terminated by the first empty line after the header
   // fields, regardless of the header fields present in the message, and thus
   // cannot contain a message body." This overrides every header-derived
   // signal below -- including an upstream Content-Length, which for HEAD and
   // 304 describes the body an equivalent GET would have returned rather than
   // anything on this wire.
   //
   // Getting this wrong is not merely cosmetic: it is what made us stamp
   // Transfer-Encoding onto a 204, which 3.3.1 forbids outright ("A server
   // MUST NOT send a Transfer-Encoding header field in any response with a
   // status code of 1xx (Informational) or 204 (No Content)"), and then follow
   // it with a 0\r\n\r\n terminator that rule 1 says cannot be there.
   const std::string& method = pClientConnection_->request().method();
   int statusCode = response.statusCode();
   bool informational = statusCode >= 100 && statusCode < 200;
   if (informational ||
       statusCode == http::status::NoContent ||
       statusCode == http::status::NotModified ||
       method == "HEAD")
   {
      return Framing::NoBody;
   }

   // The remaining case 3.3.1 forbids Transfer-Encoding for: "A server MUST NOT
   // send a Transfer-Encoding header field in any 2xx (Successful) response to
   // a CONNECT request." Per 3.3.3 rule 2 such a response has no body framing
   // at all -- the connection becomes a tunnel immediately after the headers,
   // and the client is required to ignore any Content-Length or
   // Transfer-Encoding it sees.
   //
   // rserver implements no CONNECT tunneling (nothing anywhere dispatches on
   // the method), so this is not us declining to tunnel -- it is a CONNECT that
   // reached a proxy handler because its request-target happened to route
   // there, and was then forwarded like any other method. NoBody is the
   // least-wrong response we can frame: headers, no body, then the close we
   // always perform. Only 2xx qualifies; per RFC 7231 4.3.6 any other status
   // means no tunnel was formed and the connection is still governed by HTTP,
   // so those keep ordinary framing.
   bool successful = statusCode >= 200 && statusCode < 300;
   if (successful && method == "CONNECT")
      return Framing::NoBody;

   // Content-Length framing preserves a known upstream length end-to-end
   // (progress bars; HTTP/1.1 forbids CL + chunked together). The fallbacks
   // below handle the unknown-length case (upstream was chunked, EOF-delimited,
   // or declares an unparseable Content-Length -- matching AsyncClient's own
   // fallback to EOF-delimited reading in that case, see
   // responseBodyComplete()/streamedBodyComplete() in AsyncClient.hpp).
   // Ask the shared parser rather than comparing the raw field: this must
   // reach the same verdict AsyncClient did when it decided whether to
   // de-chunk, or we re-chunk a body that is already chunk-framed (RFC 7230
   // 3.3.1: "A sender MUST NOT apply chunked more than once to a message
   // body"). It also implements 3.3.3 rule 3 -- Transfer-Encoding overrides a
   // Content-Length sent alongside it, and the Chunked branch below removes
   // that Content-Length before forwarding, as rule 3 requires.
   bool upstreamChunked = util::parseTransferEncoding(response.headers()).chunkedIsFinal;
   boost::optional<uintmax_t> contentLength =
      safe_convert::stringTo<uintmax_t>(response.headerValue("Content-Length"));
   if (contentLength && !upstreamChunked)
      return Framing::ContentLength;

   // Length unknown, so the body needs a framing this hop generates itself.
   // Chunked requires an HTTP/1.1 (or later) client: RFC 7230 3.3.1, "A server
   // MUST NOT send a response containing Transfer-Encoding unless the
   // corresponding request indicates HTTP/1.1 (or later)." That rule has teeth
   // here -- an HTTP/1.0 recipient does not de-chunk, and because we always
   // send Connection: close it would instead apply 3.3.3 rule 7 and hand the
   // hex chunk-size lines to the user as body content.
   //
   // Note this is the *client's* request version, not the upstream response
   // version: the two hops are framed independently, and the client is the
   // only party that has to understand what we emit here.
   const http::Request& request = pClientConnection_->request();
   bool clientSupportsChunked =
      request.httpVersionMajor() > 1 ||
      (request.httpVersionMajor() == 1 && request.httpVersionMinor() >= 1);
   if (clientSupportsChunked)
      return Framing::Chunked;

   // HTTP/1.0 client with an unknown length: send no framing header at all and
   // let the connection close delimit the body (3.3.3 rule 7). This is exactly
   // conformant here rather than a degradation, because FixedBufferProxy
   // already closes both connections on every terminal path and already
   // advertises Connection: close.
   return Framing::CloseDelimited;
}

bool FixedBufferProxy::queueChunk(const http::Response& response,
                                   const std::string& chunk)
{
   // Discard anything still arriving after a teardown. closeUpstream()'s
   // disableHandlers() already stops delivery in the ordinary case --
   // deliverChunks() re-reads handlersDisabled_ before every piece -- but
   // failConnection() can only try to reach it: if pServerConnection_->close()
   // throws on the way there, the handler stays attached and pieces keep
   // coming. `true`, not `false`, because `false` means "pause and redeliver"
   // and nothing will ever resume us now (see setFixedBufferHandlerSupportsPause()).
   if (failed_)
      return true;

   LOCK_MUTEX_OR_FAIL(mutex_, "queueChunk")
   {
      // Decide the client-facing framing once, from the upstream response and
      // this proxy's own client request -- see decideFraming().
      if (framing_ == Framing::Undecided)
         framing_ = decideFraming(response);

      // The empty chunk is AsyncClient's completion signal. In Content-Length
      // and CloseDelimited framing there is no terminator to write; in chunked
      // framing we still must emit the 0\r\n\r\n terminator, so fall through
      // and enqueue it below.
      bool isFinal = chunk.empty();

      // Size the outbound bytes for this piece: a size-prefixed HTTP chunk in
      // chunked framing, raw bytes in the two length/close-delimited framings,
      // and nothing at all when the response cannot carry a body.
      //
      // Size first, format later (below, once accepted). The buffer-full check
      // that follows may decline this piece for redelivery, and the motivating
      // workload -- a large download to a slow client -- sits at the buffer
      // limit for most of its life, so building the enveloped copy up front
      // would mean formatting the bulk of the body twice and throwing the first
      // copy away.
      std::size_t formattedSize;
      switch (framing_)
      {
         case Framing::Chunked:
            formattedSize = http::util::httpChunkSize(chunk.size());
            break;

         case Framing::NoBody:
            // Drop the bytes rather than relaying them. A body on a HEAD/1xx/
            // 204/304 response is a malformed upstream, and per RFC 7230 3.3.3
            // rule 1 the client will not read one -- so anything we forwarded
            // would land on the wire as the start of a *different* message,
            // which is the response-smuggling shape. Nothing is enqueued, so
            // the rest of this function writes headers and then closes.
            formattedSize = 0;
            break;

         default:
            // ContentLength / CloseDelimited: write body bytes verbatim.
            formattedSize = chunk.size();
            break;
      }

      // Account against the *formatted* (enveloped) size we actually enqueue, so
      // currentBufferSize_ stays consistent with what onChunkWrote() subtracts
      // (writeBuffer_.front().size()) in both framings.
      //
      // Guarantee forward progress regardless of how maxBufferSize_ and
      // AsyncClient's chunk/envelope sizing relate to each other: when nothing
      // is queued and no write is in flight, there is nothing this piece could
      // ever resume after if declined, so accept it unconditionally rather than
      // risking a hang with no future resume trigger. Normal backpressure still
      // applies once anything is queued or outstanding.
      bool bufferIdle = writeBuffer_.empty() && !clientWriteInProgress_;
      if (!bufferIdle && currentBufferSize_ + formattedSize > maxBufferSize_)
      {
         bufferFull_ = true;

         // we are temporarily out of space and cannot buffer any more chunks
         // until more data is written to the outgoing (client) connection
         // signal to connection to stop reading new data, and redeliver this chunk
         // when we have space for it
         return false;
      }

      // Only latch completion once this piece is guaranteed to be enqueued (or
      // was already accepted above) -- an oversized chunk accepted into an idle
      // buffer can leave the completion signal itself exceeding maxBufferSize_
      // when it arrives before that chunk's write completes. If receivedFinal_
      // were set on the declined attempt, onChunkWrote() would close both
      // connections as soon as the in-flight write drains -- without ever
      // writing chunked framing's required 0\r\n\r\n terminator, and without
      // the completion signal ever being redelivered.
      if (isFinal)
         receivedFinal_ = true;

      if (formattedSize > 0)
      {
         currentBufferSize_ += formattedSize;

         // The envelope is built here, at the one point where the piece is
         // certain to be enqueued; httpChunkSize() above predicted exactly
         // this many bytes, which is what keeps currentBufferSize_ and
         // onChunkWrote()'s front().size() subtraction in step.
         if (framing_ == Framing::Chunked)
            writeBuffer_.emplace(http::util::formatMessageAsHttpChunk(chunk));
         else
            writeBuffer_.emplace(chunk);
      }

      if (!wroteHeaders_)
      {
         // Latch both flags before handing the work off: writeHeaders() may not
         // run until later, on another thread (see the dispatch below), and
         // until it has initiated its write a chunk queued in the meantime must
         // fall through to the "a write is already in flight" branch rather than
         // start a second write of its own.
         wroteHeaders_ = true;
         clientWriteInProgress_ = true;

         // hand writeHeaders() the upstream response to assemble from; see the
         // member's declaration for why this is a copy and why it needs no lock
         // on the far side
         serverResponse_.assign(response);

         // Assemble the client-facing headers and write them as a single unit
         // on the client connection's strand.
         //
         // Both halves have to be on that strand, not just the write.
         // writeHeaders() assembles directly into pClientConnection_->response(),
         // and that connection has other writers -- its own read path can turn a
         // request-parse error into writeResponse(BadRequest) at any moment.
         // Those writers hand asio buffers that point straight into the
         // response's own member strings (Message::appendHeader() buffers
         // Header::name/Header::value; Response::appendFirstLineBuffers() buffers
         // statusCodeStr_/statusMessage_ -- nothing is copied into a
         // serialization buffer), so mutating that response from off-strand while
         // such a write is in flight frees the storage asio is reading. That is a
         // use-after-free, not merely a duplicate response, and
         // AsyncConnectionImpl::claimResponse() cannot arbitrate it: the claim
         // only sees the write call, which comes after the mutation.
         //
         // dispatch() rather than post() so that the wiring which already shares
         // a strand between the two connections -- proxyLocalhostRequest() hands
         // the upstream AsyncClient this connection's own strand -- still runs
         // the assembly inline, exactly as it did when it was written here.
         boost::asio::dispatch(pClientConnection_->getStrand(),
                               boost::bind(&FixedBufferProxy::writeHeaders,
                                           shared_from_this()));
      }
      else
      {
         // Guard against a write already outstanding (headers or a prior
         // chunk): writeBuffer_.size() == 1 alone doesn't imply nothing is
         // in flight -- e.g. the Content-Length final completion signal
         // enqueues nothing, so the buffer can still show exactly the one
         // chunk queued by an earlier call whose header write hasn't
         // completed (onHeadersWrote hasn't run) yet. Kicking off writeChunk()
         // here in that window would race a second asyncWrite of the same
         // still-unpopped front() entry against the one onHeadersWrote will
         // issue once it runs. When a write is already in flight, its own
         // completion handler (onHeadersWrote/onChunkWrote) will call
         // writeChunk() again once it's safe to do so.
         if (!writeBuffer_.empty() && !clientWriteInProgress_)
         {
            writeChunk();
         }
         else if (writeBuffer_.empty() && receivedFinal_ && !clientWriteInProgress_)
         {
            // Content-Length final with nothing left to write: close now.
            closeConnections();
         }
      }
   }
   END_LOCK_MUTEX_OR_FAIL("queueChunk")

   // Reached either on success or after a teardown above. `true` is right in
   // both cases: on the teardown path the piece is not coming back (see the
   // failed_ check at the top of this function).
   return true;
}

// Assemble the client-facing response headers and start writing them.
//
// Always runs on pClientConnection_'s strand -- see the dispatch in queueChunk()
// for why the assembly must be there and not merely the write. Deliberately
// takes no lock: everything it reads (serverResponse_, framing_) was set before
// that dispatch and is never touched again, and the write's completion
// (onHeadersWrote) is posted by the connection rather than run reentrantly, so
// nothing here re-enters mutex_ -- which matters because the inline-dispatch
// case arrives with queueChunk() still holding it.
//
// Guarded rather than left to unwind, and by its own handler rather than
// queueChunk()'s: only the inline-dispatch case runs inside queueChunk()'s
// locked region, and every allocating call below (removeHopByHopHeaders,
// addHeaders, setHeader) is a throw site. When the dispatch is *not* inline
// this runs straight off the client connection's strand, so an escape here
// unwinds through boost::asio into AsyncServerImpl::runServiceThread()'s
// ioContext().run() call, which logs it and lets that server worker thread
// exit for good -- one fewer thread serving every other connection, for a
// single failed header assembly.
void FixedBufferProxy::writeHeaders()
{
   // A teardown may have happened between queueChunk()'s dispatch and now.
   // Writing headers then would re-latch clientWriteInProgress_ on a connection
   // that is already closed and whose completion handler bails out on failed_.
   if (failed_)
      return;

   try
   {
      assembleAndWriteHeaders();
   }
   catch (const std::exception& e)
   {
      failConnection("writeHeaders", e.what());
   }
   catch (...)
   {
      failConnection("writeHeaders", "unknown exception");
   }
}

void FixedBufferProxy::assembleAndWriteHeaders()
{
   // Assemble into our own copy of the upstream response and hand the finished
   // article over, rather than building it in place in
   // pClientConnection_->response() -- which this never touches at all now,
   // read or write. Only the connection can safely mutate that: it claims its
   // single response before assigning anything, and being on its strand is not
   // a substitute, because an earlier winner's asyncWrite holds buffers
   // pointing into that response's member strings and is still in flight while
   // we run -- a strand serializes handlers, not the writes they start. See
   // AsyncConnection::response()'s threading note.
   http::Response& resp = serverResponse_;

   // Strip hop-by-hop headers (below) before restoring the cookies preserved
   // in proxy(), not after: a nominated-by-Connection removal (e.g. an
   // upstream sending "Connection: Set-Cookie") must only ever be able to
   // strip the *upstream's* headers, never the proxy's own refreshed auth
   // cookies re-added afterward. resp is the upstream response and nothing
   // else at this point -- queueChunk() copied it and no client-side header
   // has been merged in yet.
   //
   // TODO(rstudio-pro-11740 follow-on): if the /s/ path is later opted into
   // streaming (see plan Resolved Questions, "Streaming /s/ and launcher"),
   // its auth-cookie stamping (getAuthCookies(), ServerSessionProxy.cpp:267,
   // 1003) will need to move from handleProxyResponse's single post-completion
   // writeResponse(response, true, authCookies) call into *this* header-write
   // path -- headers are flushed here, off the first queueChunk, before any
   // body bytes are streamed, so cookies can no longer be added at response
   // completion time the way the buffered path does today. preservedCookies_
   // is the intended seam: the future streaming-enabled /s/ wiring should
   // stamp refreshed auth cookies onto the client connection's response before
   // FixedBufferProxy::proxy() snapshots it, not at completion time.
   //
   // This connection to the client is not the same connection, nor
   // subject to the same per-hop semantics, as the one to the upstream
   // server -- strip Connection/Keep-Alive/Upgrade/TE/Trailer/
   // Proxy-Authenticate/Proxy-Authorization/Proxy-Connection/
   // Transfer-Encoding (and anything else Connection nominates) before
   // restoring preserved headers and setting our own framing headers
   // below.
   http::util::removeHopByHopHeaders(&resp);
   resp.addHeaders(preservedCookies_);

   // FixedBufferProxy always closes both connections once the body
   // finishes (every terminal path in this class calls close() on both),
   // the same "close after this response" behavior
   // AsyncConnectionImpl::writeResponse() signals via this header when
   // called with its default close=true -- but writeResponseHeaders()
   // takes no close argument and so never sets it (it does apply the
   // rest: HTTP-version, Date, nosniff, and the response filter).
   // Without this, a client that pools/pipelines HTTP/1.1 connections has
   // no signal that this one is about to be closed.
   resp.setHeader("Connection", "close");

   // Framing headers. removeHopByHopHeaders() above has already stripped
   // any upstream Transfer-Encoding, so the only way one reaches the
   // client is the explicit set in the Chunked case -- every other
   // framing is guaranteed not to emit one, which is what keeps us on the
   // right side of 3.3.1's MUST NOTs for 1xx/204 and HTTP/1.0 clients.
   switch (framing_)
   {
      case Framing::Chunked:
         resp.removeHeader("Content-Length");
         resp.setHeader(kTransferEncoding, kChunkedTransferEncoding);
         break;

      case Framing::CloseDelimited:
         // No framing header at all; our close delimits the body (RFC
         // 7230 3.3.3 rule 7). By construction there is no *parseable*
         // upstream Content-Length here (that would have selected
         // ContentLength framing), but an unparseable one may still be
         // present -- drop it rather than forward a length we are not
         // honoring.
         resp.removeHeader("Content-Length");
         break;

      case Framing::NoBody:
         // RFC 7230 3.3.2: "A server MUST NOT send a Content-Length
         // header field in any response with a status code of 1xx
         // (Informational) or 204 (No Content)." For HEAD and 304 the
         // upstream Content-Length is both permitted and useful -- it
         // describes the body an equivalent GET would return -- so it
         // stays.
         if ((resp.statusCode() >= 100 && resp.statusCode() < 200) ||
             resp.statusCode() == http::status::NoContent)
         {
            resp.removeHeader("Content-Length");
         }
         break;

      default:
         // Content-Length framing: keep the upstream Content-Length as-is.
         break;
   }

   pClientConnection_->writeResponseHeaders(resp,
                                            boost::bind(&FixedBufferProxy::onHeadersWrote,
                                                        shared_from_this(),
                                                        boost::asio::placeholders::error));
}

void FixedBufferProxy::onHeadersWrote(const boost::system::error_code& ec)
{
   // Ahead of handleError(): after a teardown both connections are already
   // closed, so the ec we are being handed is our own doing (operation_aborted)
   // and there is nothing left for handleError() to close.
   if (failed_)
      return;

   if (handleError(ec))
      return;

   LOCK_MUTEX_OR_FAIL(mutex_, "onHeadersWrote")
   {
      // the header write completed; no write is outstanding until writeChunk()
      // below (re-)initiates one
      clientWriteInProgress_ = false;

      // write the first chunk
      writeChunk();
   }
   END_LOCK_MUTEX_OR_FAIL("onHeadersWrote")
}

void FixedBufferProxy::writeChunk()
{
   if (writeBuffer_.empty())
   {
      if (bufferFull_)
      {
         // we previously hit a full buffer condition
         // inform the connection that we are ready to process chunks again
         bufferFull_ = false;
         pServerConnection_->resumeChunkProcessing();
      }
      else if (receivedFinal_)
      {
         // the final chunk arrived but produced no outbound bytes to flush
         // (e.g. the empty final chunk of a Content-Length: 0 / 204 response,
         // where headers were just written and no body chunk was ever
         // enqueued) - nothing left to write, close now.
         closeConnections();
      }

      return;
   }

   const std::string& chunk = writeBuffer_.front();

   clientWriteInProgress_ = true;
   boost::asio::const_buffer buffer(chunk.c_str(), chunk.size());
   pClientConnection_->asyncWrite(buffer,
                                  boost::bind(&FixedBufferProxy::onChunkWrote,
                                                    shared_from_this(),
                                                    boost::asio::placeholders::error));
}

void FixedBufferProxy::onChunkWrote(const boost::system::error_code& ec)
{
   // see onHeadersWrote() for why this precedes handleError()
   if (failed_)
      return;

   if (handleError(ec))
      return;

   LOCK_MUTEX_OR_FAIL(mutex_, "onChunkWrote")
   {
      // the write that was outstanding completed; no write is outstanding
      // until writeChunk() below (re-)initiates one
      clientWriteInProgress_ = false;

      currentBufferSize_ -= writeBuffer_.front().size();
      writeBuffer_.pop();

      if (receivedFinal_ && writeBuffer_.empty())
      {
         // we wrote the last outbound bytes (chunked terminator or final body
         // bytes) - close connections
         closeConnections();
         return;
      }

      // keep writing any queued chunks until we're empty
      writeChunk();
   }
   END_LOCK_MUTEX_OR_FAIL("onChunkWrote")
}

bool FixedBufferProxy::handleError(const boost::system::error_code& ec)
{
   if (!ec)
      return false;

   // Guarded for the same reason the locked regions are, and separately from
   // them because both callers reach this ahead of taking the lock: every path
   // below can throw (Error construction and LOG_ERROR allocate,
   // close()/disableHandlers() are virtual calls into asio), and an escape
   // unwinds straight out of a completion handler into the io_context.
   //
   // The result is unconditional: every path below is a terminal one, so a
   // caller must stop either way -- on the failConnection() path because the
   // proxy is now torn down, and otherwise because handleErrorImpl() closed
   // what it needed to.
   // already_started means another writer owns this client connection's one
   // response and may still be writing it, which is why handleErrorImpl()
   // detaches from the upstream and deliberately leaves that connection alone.
   // The exception path has to honor the same rule: closing the client here
   // would truncate exactly the response the claim exists to protect, and a
   // failed upstream close is no reason to do that.
   bool closeClientConnection = (ec != boost::asio::error::already_started);

   try
   {
      handleErrorImpl(ec);
   }
   catch (const std::exception& e)
   {
      failConnection("handleError", e.what(), closeClientConnection);
   }
   catch (...)
   {
      failConnection("handleError", "unknown exception", closeClientConnection);
   }

   return true;
}

void FixedBufferProxy::handleErrorImpl(const boost::system::error_code& ec)
{
   // already_started is not a transport failure: it is
   // AsyncConnectionImpl::claimResponse() telling us another writer owns this
   // client connection's one response and got there first, so nothing we
   // queued will ever reach the client. That winner's write may still be in
   // flight, and closing the client connection -- what every other error path
   // here does -- would truncate or reset the very response the claim exists
   // to protect. Detach from the upstream and leave the client connection to
   // its winner, which closes it once its own write completes.
   //
   // No logging: claimResponse() already logged the losing attempt.
   if (ec == boost::asio::error::already_started)
   {
      closeUpstream();
      return;
   }

   Error error(ec, ERROR_LOCATION);

   if (!http::isConnectionTerminatedError(error))
      LOG_ERROR(error);

   // close both connections to stop all data transfer
   closeConnections();
}

void FixedBufferProxy::closeConnections()
{
   pClientConnection_->close();
   closeUpstream();
}

void FixedBufferProxy::closeUpstream()
{
   pServerConnection_->close();

   // AsyncClient::close() only closes the socket -- it never clears
   // fixedBufferHandler_, the boost::function that holds a shared_ptr back to
   // us (see proxy()). AsyncClient's own internal disableHandlers() call (in
   // its handleError()) is unreachable once we've already closed it
   // ourselves (its closed_ guard short-circuits first), and under
   // backpressure pause no callback ever fires at all. Without this, every
   // caller above leaks this FixedBufferProxy, pClientConnection_, and any
   // buffered chunks forever.
   pServerConnection_->disableHandlers();
}

namespace {

// Run one step of a teardown, swallowing anything it throws so that a failure
// in any single step cannot skip the steps after it. Deliberately silent: the
// only caller must not throw, and the logger is exactly as likely to fail as
// the step was.
template <typename Step>
void attemptTeardownStep(Step step)
{
   try
   {
      step();
   }
   catch (...)
   {
   }
}

} // anonymous namespace

void FixedBufferProxy::failConnection(const char* context,
                                      const char* what,
                                      bool closeClientConnection)
{
   if (failed_.exchange(true))
      return;

   try
   {
      LOG_ERROR_MESSAGE(std::string("Proxy: tearing down upstream and downstream "
                                    "connections after unexpected exception in ") +
                        context + ": " + what);
   }
   catch (...)
   {
      // Swallow these; we are trying to unwind from an ongoing rare exception
      // such as bad allocation. We want to continue cleanup if we can't log.
   }

   // Attempt each step independently rather than going through
   // closeConnections()/closeUpstream(), which chain: a throw from an earlier
   // step there abandons the later ones, and failed_ means nothing will retry
   // them. Skipping disableHandlers() is the expensive one -- it is what breaks
   // the FixedBufferProxy <-> AsyncClient reference cycle, so without it this
   // proxy, both connections and every buffered chunk leak for the life of the
   // process (see closeUpstream()). Same order closeConnections() uses.
   if (closeClientConnection)
      attemptTeardownStep([&] { pClientConnection_->close(); });

   attemptTeardownStep([&] { pServerConnection_->close(); });
   attemptTeardownStep([&] { pServerConnection_->disableHandlers(); });
}

} // namespace http
} // namespace core
} // namespace rstudio
