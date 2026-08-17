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

bool FixedBufferProxy::queueChunk(const http::Response& response,
                                   const std::string& chunk)
{
   LOCK_MUTEX(mutex_)
   {
      // Decide the client-facing framing once, from the upstream response.
      // Content-Length framing preserves a known upstream length end-to-end
      // (progress bars; HTTP/1.1 forbids CL + chunked together). Chunked is the
      // fallback when the upstream length is unknown (upstream was chunked,
      // EOF-delimited, or declares an unparseable Content-Length -- matching
      // AsyncClient's own fallback to EOF-delimited reading in that case, see
      // responseBodyComplete()/streamedBodyComplete() in AsyncClient.hpp).
      if (framing_ == Framing::Undecided)
      {
         bool upstreamChunked =
            response.headerValue(kTransferEncoding) == kChunkedTransferEncoding;
         boost::optional<uintmax_t> contentLength =
            safe_convert::stringTo<uintmax_t>(response.headerValue("Content-Length"));
         framing_ = (contentLength && !upstreamChunked)
                       ? Framing::ContentLength
                       : Framing::Chunked;
      }

      // The empty chunk is AsyncClient's completion signal. In Content-Length
      // framing there is no terminator to write; in chunked framing we still must
      // emit the 0\r\n\r\n terminator, so fall through and enqueue it below.
      bool isFinal = chunk.empty();

      // Format the outbound bytes for this piece: raw for Content-Length, or a
      // size-prefixed HTTP chunk for chunked framing.
      std::string formatted =
         (framing_ == Framing::Chunked)
            ? http::util::formatMessageAsHttpChunk(chunk)
            : chunk; // Content-Length: write body bytes verbatim

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
         // path — headers are flushed here, at the first queueChunk, before any
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

         if (framing_ == Framing::Chunked)
         {
            resp.removeHeader("Content-Length");
            resp.setHeader(kTransferEncoding, kChunkedTransferEncoding);
         }
         // Content-Length framing: keep the upstream Content-Length as-is.

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
            pClientConnection_->close();
            pServerConnection_->close();
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
         pClientConnection_->close();
         pServerConnection_->close();
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
         pClientConnection_->close();
         pServerConnection_->close();
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
      pClientConnection_->close();
      pServerConnection_->close();
      return true;
   }

   return false;
}

} // namespace http
} // namespace core
} // namespace rstudio
