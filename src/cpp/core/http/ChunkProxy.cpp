/*
 * ChunkProxy.cpp
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

#include <core/http/ChunkProxy.hpp>
#include <core/http/Util.hpp>

namespace rstudio {
namespace core {
namespace http {

namespace {

bool isLastChunk(const std::string& chunk)
{
   return chunk == "0\r\n\r\n";
}

} // anonymous namespace
ChunkProxy::ChunkProxy(const boost::shared_ptr<AsyncConnection>& pClientConnection,
                       uint64_t maxBufferSize) :
   pClientConnection_(pClientConnection),
   maxBufferSize_(maxBufferSize),
   wroteHeaders_(false),
   currentBufferSize_(0),
   bufferFull_(false)
{
}

void ChunkProxy::proxy(const boost::shared_ptr<IAsyncClient>& pServerConnection)
{
   pServerConnection_ = pServerConnection;
   pServerConnection_->setChunkHandler(boost::bind(&ChunkProxy::queueChunk,
                                                   shared_from_this(),
                                                   _1, _2));
}

bool ChunkProxy::queueChunk(const http::Response& response,
                            const std::string& chunk)
{
   LOCK_MUTEX(mutex_)
   {
      if (currentBufferSize_ + chunk.size() > maxBufferSize_)
      {
         bufferFull_ = true;

         // we are temporarily out of space and cannot buffer any more chunks
         // until more data is written to the outgoing (client) connection
         // signal to connection to stop reading new data, and redeliver this chunk
         // when we have space for it
         return false;
      }

      // queue the chunk
      std::string formattedChunk = http::util::formatMessageAsHttpChunk(chunk);
      currentBufferSize_ += formattedChunk.size();
      writeBuffer_.emplace(std::move(formattedChunk));

      if (!wroteHeaders_)
      {
         // write the response headers and first chunk
         http::Response& resp = pClientConnection_->response();
         resp.assign(response);

         pClientConnection_->writeResponseHeaders(boost::bind(&ChunkProxy::onHeadersWrote,
                                                              shared_from_this(),
                                                              boost::asio::placeholders::error));
         wroteHeaders_ = true;
      }
      else
      {
         if (writeBuffer_.size() == 1)
         {
            // we're the only chunk in the buffer, so we need to initiate a write
            writeChunk();
         }
      }
   }
   END_LOCK_MUTEX

   return true;
}

void ChunkProxy::onHeadersWrote(const boost::system::error_code& ec)
{
   if (handleError(ec))
      return;

   LOCK_MUTEX(mutex_)
   {
      // write the first chunk
      writeChunk();
   }
   END_LOCK_MUTEX
}

void ChunkProxy::writeChunk()
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

      return;
   }

   const std::string& chunk = writeBuffer_.front();

   boost::asio::const_buffers_1 buffer(chunk.c_str(), chunk.size());
   pClientConnection_->asyncWrite(buffer,
                                  boost::bind(&ChunkProxy::onChunkWrote,
                                              shared_from_this(),
                                              boost::asio::placeholders::error));
}

void ChunkProxy::onChunkWrote(const boost::system::error_code& ec)
{
   if (handleError(ec))
      return;

   LOCK_MUTEX(mutex_)
   {
      const std::string& chunk = writeBuffer_.front();
      bool lastChunk = isLastChunk(chunk);
      currentBufferSize_ -= writeBuffer_.front().size();
      writeBuffer_.pop();

      if (lastChunk)
      {
         // we wrote the last chunk - close connections
         pClientConnection_->close();
         pServerConnection_->close();
         return;
      }

      // keep writing any queued chunks until we're empty
      writeChunk();
   }
   END_LOCK_MUTEX
}

bool ChunkProxy::handleError(const boost::system::error_code& ec)
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
