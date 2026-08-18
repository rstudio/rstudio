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
#include <boost/optional.hpp>

#include <core/http/FixedBufferProxy.hpp>
#include <core/http/Util.hpp>
#include <shared_core/SafeConvert.hpp>

using namespace boost::placeholders;

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
   int statusCode = response.statusCode();
   bool informational = statusCode >= 100 && statusCode < 200;
   if (informational ||
       statusCode == http::status::NoContent ||
       statusCode == http::status::NotModified ||
       pClientConnection_->request().method() == "HEAD")
   {
      return Framing::NoBody;
   }

   // Content-Length framing preserves a known upstream length end-to-end
   // (progress bars; HTTP/1.1 forbids CL + chunked together). The fallbacks
   // below handle the unknown-length case (upstream was chunked, EOF-delimited,
   // or declares an unparseable Content-Length -- matching AsyncClient's own
   // fallback to EOF-delimited reading in that case, see
   // responseBodyComplete()/streamedBodyComplete() in AsyncClient.hpp).
   bool upstreamChunked =
      response.headerValue(kTransferEncoding) == kChunkedTransferEncoding;
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
   LOCK_MUTEX(mutex_)
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

      // Format the outbound bytes for this piece: a size-prefixed HTTP chunk in
      // chunked framing, raw bytes in the two length/close-delimited framings,
      // and nothing at all when the response cannot carry a body.
      std::string formatted;
      switch (framing_)
      {
         case Framing::Chunked:
            formatted = http::util::formatMessageAsHttpChunk(chunk);
            break;

         case Framing::NoBody:
            // Drop the bytes rather than relaying them. A body on a HEAD/1xx/
            // 204/304 response is a malformed upstream, and per RFC 7230 3.3.3
            // rule 1 the client will not read one -- so anything we forwarded
            // would land on the wire as the start of a *different* message,
            // which is the response-smuggling shape. formatted stays empty, so
            // the rest of this function writes headers and then closes.
            break;

         default:
            // ContentLength / CloseDelimited: write body bytes verbatim.
            formatted = chunk;
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
      if (!bufferIdle && currentBufferSize_ + formatted.size() > maxBufferSize_)
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

      if (!formatted.empty())
      {
         currentBufferSize_ += formatted.size();
         writeBuffer_.emplace(std::move(formatted));
      }

      if (!wroteHeaders_)
      {
         // write the response headers and first chunk
         http::Response& resp = pClientConnection_->response();

         // preserve headers already stamped on the outgoing response (e.g.
         // refreshed auth cookies set before the proxy request executed), which
         // assign() would otherwise clobber. Set-Cookie is multi-valued.
         //
         // TODO(rstudio-pro-11740 follow-on): if the /s/ path is later opted into
         // streaming (see plan Resolved Questions, "Streaming /s/ and launcher"),
         // its auth-cookie stamping (getAuthCookies(), ServerSessionProxy.cpp:267,
         // 1003) will need to move from handleProxyResponse's single post-completion
         // writeResponse(response, true, authCookies) call into *this* header-write
         // path -- headers are flushed here, at the first queueChunk, before any
         // body bytes are streamed, so cookies can no longer be added at response
         // completion time the way the buffered path does today. The `preserved`
         // mechanism below (copying Set-Cookie already present on
         // pClientConnection_->response()) is the intended seam: the future
         // streaming-enabled /s/ wiring should stamp refreshed auth cookies onto
         // that response object *before* the first FixedBufferProxy::queueChunk() call
         // (i.e. at header-received time in AsyncClient, not at completion time).
         http::Headers preserved;
         for (const http::Header& h : resp.headers())
         {
            if (boost::iequals(h.name, "Set-Cookie"))
               preserved.push_back(h);
         }

         // Assign without extraHeaders here and strip hop-by-hop headers
         // (below) before restoring the preserved headers, not after: a
         // nominated-by-Connection removal (e.g. an upstream sending
         // "Connection: Set-Cookie") must only ever be able to strip the
         // *upstream's* headers, never the proxy's own refreshed auth
         // cookies re-added afterward.
         resp.assign(response);

         // This connection to the client is not the same connection, nor
         // subject to the same per-hop semantics, as the one to the upstream
         // server -- strip Connection/Keep-Alive/Upgrade/TE/Trailer/
         // Proxy-Authenticate/Proxy-Authorization/Proxy-Connection/
         // Transfer-Encoding (and anything else Connection nominates) before
         // restoring preserved headers and setting our own framing headers
         // below.
         http::util::removeHopByHopHeaders(&resp);
         resp.addHeaders(preserved);

         // FixedBufferProxy always closes both connections once the body
         // finishes (every terminal path below calls close() on both), the
         // same "close after this response" behavior
         // AsyncConnectionImpl::writeResponse() signals via this header when
         // called with its default close=true -- but writeResponseHeaders()
         // below is a thinner path that never sets it. Without this, a client
         // that pools/pipelines HTTP/1.1 connections has no signal that this
         // one is about to be closed.
         resp.setHeader("Connection", "close");

         // Framing headers. removeHopByHopHeaders() above has already stripped
         // any upstream Transfer-Encoding, so the only way one reaches the
         // client is the explicit set in the Chunked case below -- every other
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

         clientWriteInProgress_ = true;
         pClientConnection_->writeResponseHeaders(boost::bind(&FixedBufferProxy::onHeadersWrote,
                                                               shared_from_this(),
                                                               boost::asio::placeholders::error));
         wroteHeaders_ = true;
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
   END_LOCK_MUTEX

   return true;
}

void FixedBufferProxy::onHeadersWrote(const boost::system::error_code& ec)
{
   if (handleError(ec))
      return;

   LOCK_MUTEX(mutex_)
   {
      // the header write completed; no write is outstanding until writeChunk()
      // below (re-)initiates one
      clientWriteInProgress_ = false;

      // write the first chunk
      writeChunk();
   }
   END_LOCK_MUTEX
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
   if (handleError(ec))
      return;

   LOCK_MUTEX(mutex_)
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
   END_LOCK_MUTEX
}

bool FixedBufferProxy::handleError(const boost::system::error_code& ec)
{
   if (ec)
   {
      Error error(ec, ERROR_LOCATION);

      if (!http::isConnectionTerminatedError(error))
         LOG_ERROR(error);

      // close both connections to stop all data transfer
      closeConnections();
      return true;
   }

   return false;
}

void FixedBufferProxy::closeConnections()
{
   pClientConnection_->close();
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

} // namespace http
} // namespace core
} // namespace rstudio
