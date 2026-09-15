/*
 * FixedBufferProxy.hpp
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

#ifndef CORE_HTTP_FIXED_BUFFER_PROXY_HPP
#define CORE_HTTP_FIXED_BUFFER_PROXY_HPP

#include <atomic>
#include <boost/enable_shared_from_this.hpp>
#include <boost/optional.hpp>

#include <shared_core/Error.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/http/AsyncClient.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace core {
namespace http {

// Content-Length below which a proxied response is not worth streaming: peak
// memory is already bounded by the response itself, while streaming would give
// up the things only a held response can do -- failing over to another node,
// rewriting a body, synthesizing a clean error after a mid-body upstream
// failure. Sized to FixedBufferProxy's own outbound buffer, so a
// below-threshold response is one this proxy would never have had to split.
constexpr uint64_t kStreamingContentLengthThreshold = 1024 * 1024; // 1MB

// True when `response` should be held whole rather than streamed, purely on
// size grounds. Each proxy site ORs this with its own header-observable
// always-buffer cases to form its named buffering policy (see
// shouldBufferLocalhostResponse, shouldBufferLauncherResponse,
// shouldBufferNodeResponse) and hands that policy to setBufferPredicate().
//
// An absent or unparseable Content-Length returns false: those are exactly the
// chunked and EOF-delimited bodies with no declared upper bound, and they are
// what streaming exists for. A response carrying Transfer-Encoding also
// returns false regardless of any Content-Length present alongside it -- per
// RFC 7230 3.3.3 rule 3, Transfer-Encoding takes precedence and the body is
// unbounded, so a small or stale Content-Length must not be trusted.
bool isBelowStreamingThreshold(
   const Response& response,
   uint64_t threshold = kStreamingContentLengthThreshold);

class FixedBufferProxy : public boost::enable_shared_from_this<FixedBufferProxy>,
                          boost::noncopyable
{
public:
   FixedBufferProxy(const boost::shared_ptr<AsyncConnection>& pClientConnection,
                     uint64_t maxBufferSize = defaultMaxBufferSize);

   // preservedCookiesOverride, when supplied, replaces the automatic
   // "snapshot every Set-Cookie already on the client connection's response"
   // behavior described at preservedCookies_ below. A caller whose delivery
   // requires the same scope-based filtering on the streamed path as it
   // applies on its own buffered path (e.g. the launcher/remote-session
   // proxy's launcherCookieCarryOver()) computes that filtered set itself --
   // from the same client-connection response this class would otherwise read
   // unfiltered -- and passes it here so both delivery strategies end up with
   // identical cookies on the wire. Absent (the default) keeps the original
   // blind-copy behavior for every other caller.
   void proxy(const boost::shared_ptr<IAsyncClient>& pServerConnection,
             const boost::optional<Headers>& preservedCookiesOverride = boost::none);

private:
   // AsyncClient::breakChunks() (AsyncClient.hpp) caps each upstream piece at
   // maxChunkSize (1MB) before handing it to our FixedBufferHandler, and in
   // Chunked framing formatMessageAsHttpChunk() adds a small hex-length-prefixed
   // envelope on top. Size this comfortably above maxChunkSize + that envelope
   // overhead so a maximal piece always fits into an empty buffer on its own --
   // queueChunk()'s empty-buffer acceptance is a second line of defense if
   // either constant ever changes without updating the other.
   static constexpr uint64_t defaultMaxBufferSize = 1024*1024 + 4096; // ~1MB + 4KB

   // How this proxy frames the body it writes to the client. Decided once, at
   // the first queueChunk(), by decideFraming().
   enum class Framing
   {
      Undecided,
      NoBody,         // response cannot carry a body at all (RFC 7230 3.3.3
                      // rule 1): headers only, no framing header, no body
                      // bytes, no chunked terminator
      ContentLength,  // upstream length known: raw bytes, Content-Length kept
      Chunked,        // length unknown, client speaks HTTP/1.1+: chunked
      CloseDelimited  // length unknown, client is HTTP/1.0: raw bytes, no
                      // framing header, body delimited by our close
   };

   Framing decideFraming(const Response& response) const;
   bool queueChunk(const Response& response,
                   const std::string& chunk);
   void writeHeaders();

   // The body of writeHeaders(), split out only so that writeHeaders() can wrap
   // it in an exception guard without indenting all of it; see there.
   void assembleAndWriteHeaders();

   void onHeadersWrote(const boost::system::error_code& ec);
   void writeChunk();
   void onChunkWrote(const boost::system::error_code& ec);
   bool handleError(const boost::system::error_code& ec);

   // The body of handleError(), split out only so that handleError() can wrap
   // it in an exception guard without indenting all of it; see there.
   void handleErrorImpl(const boost::system::error_code& ec);
   void closeConnections();
   void closeUpstream();

   // Tear the proxy down after an unexpected exception: logs, marks the proxy
   // failed, and closes both connections. Never throws and is idempotent --
   // callers are catch blocks with nowhere to escalate to, and the same failure
   // can reach here twice (writeHeaders() dispatched inline returns into
   // queueChunk()'s still-running body). Each teardown step is attempted even
   // if an earlier one threw, since failed_ means nothing will retry them.
   //
   // closeClientConnection is for the one caller that must not touch that
   // connection even while failing: see handleError()'s already_started note.
   void failConnection(const char* context,
                       const char* what,
                       bool closeClientConnection = true);

   boost::shared_ptr<AsyncConnection> pClientConnection_;
   boost::shared_ptr<IAsyncClient> pServerConnection_;

   // The upstream response, as handed to the first queueChunk(), copied for
   // writeHeaders() to assemble the client-facing headers from once it reaches
   // the client connection's strand. A copy is needed because http::Response is
   // noncopyable (so it cannot simply be bound into the dispatched call) and
   // because the upstream one belongs to the AsyncClient, which is free to move
   // on. Cheap in practice: the streaming path never fills response_.body()
   // (see AsyncClient::streamedBodyComplete()), so this copies headers only,
   // once per response.
   //
   // Written by queueChunk() under mutex_ on its first (!wroteHeaders_) pass
   // and never touched again; read only by writeHeaders(). The dispatch()
   // between the two is the happens-before edge, so neither side needs a lock
   // for it.
   http::Response serverResponse_;

   // Set-Cookie headers already stamped on the client connection's response
   // when we were handed the upstream request -- refreshed auth cookies, in
   // practice. Snapshotted in proxy(), the one point where that response is
   // still ours alone to read; see there. Set from proxy()'s
   // preservedCookiesOverride instead, verbatim, when the caller supplies one.
   http::Headers preservedCookies_;

   uint64_t maxBufferSize_;

   boost::mutex mutex_;
   bool wroteHeaders_;
   std::queue<std::string> writeBuffer_;
   uint64_t currentBufferSize_;
   bool bufferFull_;
   Framing framing_ = Framing::Undecided; // decided at first queueChunk
   bool receivedFinal_ = false;           // upstream signaled completion
   bool clientWriteInProgress_ = false;   // an asyncWrite/writeResponseHeaders
                                           // is outstanding on pClientConnection_

   // Set once failConnection() has torn this proxy down; gates the paths that
   // would otherwise keep working a half-built response. Atomic because
   // failConnection() is reachable both with mutex_ held (the common case, from
   // a catch inside the locked region) and without it (a lock-acquisition
   // failure, which is what LOCK_MUTEX used to catch).
   std::atomic<bool> failed_{false};
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_FIXED_BUFFER_PROXY_HPP

