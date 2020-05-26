/*
 * StreamWriter.hpp
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

#ifndef CORE_HTTP_STREAM_WRITER_HPP
#define CORE_HTTP_STREAM_WRITER_HPP

#include <boost/enable_shared_from_this.hpp>

#include <shared_core/Error.hpp>
#include <core/http/Response.hpp>
#include <core/http/AsyncClient.hpp>

namespace rstudio {
namespace core {
namespace http {

template <typename SocketType>
class StreamWriter : public boost::enable_shared_from_this<StreamWriter<SocketType> >,
                     boost::noncopyable
{
public:
   StreamWriter(SocketType& socket,
                const http::Response& response,
                const boost::function<void(void)>& onComplete,
                const core::http::ErrorHandler& onError) :
      socket_(socket),
      onComplete_(onComplete),
      onError_(onError),
      response_(new http::Response())
   {
      response_->assign(response);
   }

   void write()
   {
      writeResponseHeaders();
   }

private:
   void writeResponseHeaders()
   {
      boost::asio::async_write(socket_,
                               response_->headerBuffers(),
                               boost::bind(&StreamWriter::onHeadersWritten,
                                           StreamWriter<SocketType>::shared_from_this(),
                                           _1, _2));
   }

   void onHeadersWritten(const boost::system::error_code& ec,
                         size_t written)
   {
      if (handleError(ec))
         return;

      writeNextStreamChunk();
   }

   void writeNextStreamChunk()
   {
      boost::shared_ptr<core::http::StreamResponse> response = response_->getStreamResponse();
      std::shared_ptr<core::http::StreamBuffer> buffer = response->nextBuffer();

      if (buffer)
      {
         // capture this instance so we stay alive while the writes are executing
         boost::shared_ptr<StreamWriter> sharedThis = StreamWriter<SocketType>::shared_from_this();

         // first, write the chunk header
         writeChunkHeader(buffer->size,
          [=](const boost::system::error_code& ec, size_t written) mutable
          {
            if (sharedThis->handleError(ec))
               return;

             // next, write the chunk body
             boost::asio::mutable_buffers_1 buff(buffer->data, buffer->size);
             boost::asio::async_write(socket_, buff,
              [=](const boost::system::error_code& ec, size_t written) mutable
              {
                 // capture buffer here to ensure the chunk data is not freed until after
                 // it is written to the socket (required by boost to prevent corruption)
                 buffer.reset();

                 if (sharedThis->handleError(ec))
                    return;

                 // finally, write the chunk footer
                 sharedThis->writeChunkFooter([=](const boost::system::error_code& ec, size_t written)
                 {
                    if (sharedThis->handleError(ec))
                       return;

                    // keep writing chunks until we run out
                    sharedThis->writeNextStreamChunk();
                 });
              });
          });
      }
      else
      {
         // no more chunks to send - send final empty chunk
         writeFinalChunk();
      }
   }

   void writeChunkHeader(size_t chunkSize,
                         const core::http::Socket::Handler& handler)
   {
      std::stringstream sstr;
      sstr << std::hex << chunkSize << "\r\n";

      boost::shared_ptr<std::string> headerBuff(new std::string(sstr.str()));
      boost::asio::const_buffers_1 buffer(static_cast<const void*>(headerBuff->c_str()), headerBuff->size());
      boost::asio::async_write(socket_, buffer,
       [=](const boost::system::error_code& ec, size_t written) mutable
       {
          // free header bytes (ensuring they exist during the write)
          headerBuff.reset();
          handler(ec, written);
       });
   }

   void writeChunkFooter(const core::http::Socket::Handler& handler)
   {
      const char* footer = "\r\n";

      boost::asio::const_buffers_1 buffer(static_cast<const void*>(footer), 2);
      boost::asio::async_write(socket_, buffer, handler);
   }

   void writeFinalChunk()
   {
      const char* finalChunk = "0\r\n\r\n";
      boost::asio::const_buffers_1 buffer(static_cast<const void*>(finalChunk), 5);

      // capture this instance so we stay alive while the writes are executing
      boost::shared_ptr<StreamWriter> sharedThis = StreamWriter<SocketType>::shared_from_this();

      boost::asio::async_write(socket_, buffer,
       [=](const boost::system::error_code& ec, size_t written)
       {
          if (sharedThis->handleError(ec))
             return;

          // no errors, and we have completed writing the stream
          sharedThis->onComplete_();
       });
   }

   bool handleError(const boost::system::error_code& ec)
   {
      if (ec)
      {
         core::Error error(ec, ERROR_LOCATION);

         if (!core::http::isConnectionTerminatedError(error))
         {
            onError_(error);
         }
         else
         {
            // if the connection prematurely closes (from the client's side)
            // we will count this as a successful operation
            onComplete_();
         }

         return true;
      }

      return false;
   }

private:
   SocketType& socket_;
   boost::function<void(void)> onComplete_;
   core::http::ErrorHandler onError_;
   boost::shared_ptr<http::Response> response_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_STREAM_WRITER_HPP

