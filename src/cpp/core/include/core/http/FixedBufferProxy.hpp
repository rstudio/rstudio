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

#include <boost/enable_shared_from_this.hpp>

#include <shared_core/Error.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/http/AsyncClient.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace core {
namespace http {

class FixedBufferProxy : public boost::enable_shared_from_this<FixedBufferProxy>,
                          boost::noncopyable
{
public:
   FixedBufferProxy(const boost::shared_ptr<AsyncConnection>& pClientConnection,
                     uint64_t maxBufferSize = defaultMaxBufferSize);

   void proxy(const boost::shared_ptr<IAsyncClient>& pServerConnection);

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
   void onHeadersWrote(const boost::system::error_code& ec);
   void writeChunk();
   void onChunkWrote(const boost::system::error_code& ec);
   bool handleError(const boost::system::error_code& ec);
   void closeConnections();

   boost::shared_ptr<AsyncConnection> pClientConnection_;
   boost::shared_ptr<IAsyncClient> pServerConnection_;
   http::Response serverResponse_;
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
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_FIXED_BUFFER_PROXY_HPP

