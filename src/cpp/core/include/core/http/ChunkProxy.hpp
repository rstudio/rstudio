/*
 * ChunkProxy.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_HTTP_CHUNK_PROXY_HPP
#define CORE_HTTP_CHUNK_PROXY_HPP

#include <boost/enable_shared_from_this.hpp>

#include <shared_core/Error.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/http/AsyncClient.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace core {
namespace http {

class ChunkProxy : public boost::enable_shared_from_this<ChunkProxy>,
                   boost::noncopyable
{
public:
   ChunkProxy(const boost::shared_ptr<AsyncConnection>& pClientConnection,
              uint64_t maxBufferSize = defaultMaxBufferSize);

   void proxy(const boost::shared_ptr<IAsyncClient>& pServerConnection);

private:
   static constexpr uint64_t defaultMaxBufferSize = 1024*1024; // 1MB

   bool queueChunk(const Response& response,
                   const std::string& chunk);
   void onHeadersWrote(const boost::system::error_code& ec);
   void writeChunk();
   void onChunkWrote(const boost::system::error_code& ec);
   bool handleError(const boost::system::error_code& ec);

   boost::shared_ptr<AsyncConnection> pClientConnection_;
   boost::shared_ptr<IAsyncClient> pServerConnection_;
   http::Response serverResponse_;
   uint64_t maxBufferSize_;

   boost::mutex mutex_;
   bool wroteHeaders_;
   std::queue<std::string> writeBuffer_;
   uint64_t currentBufferSize_;
   bool bufferFull_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_CHUNK_PROXY_HPP

