/*
 * AsyncConnectionImpl.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP
#define CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP

#include <boost/array.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <boost/asio/write.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/asio/placeholders.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/http/RequestParser.hpp>
#include <core/http/AsyncConnection.hpp>

namespace core {
namespace http {
   
template <typename ProtocolType>
class AsyncConnectionImpl :
   public AsyncConnection,
   public boost::enable_shared_from_this<AsyncConnectionImpl<ProtocolType> >,
   boost::noncopyable
{
public:
   typedef boost::function<void(
         boost::shared_ptr<AsyncConnectionImpl<ProtocolType> >,
         http::Request*)> Handler;

   typedef boost::function<void(http::Response*)> ResponseFilter;

public:
   AsyncConnectionImpl(boost::asio::io_service& ioService,
                       const Handler& handler,
                       const ResponseFilter& responseFilter =ResponseFilter())
      : ioService_(ioService),
        socket_(ioService),
        handler_(handler),
        responseFilter_(responseFilter)
        
   {
   }
   
   typename ProtocolType::socket& socket() 
   { 
      return socket_; 
   }

   void startReading()
   {
      readSome();
   }

   virtual boost::asio::io_service& ioService()
   {
      return ioService_;
   }

   virtual const http::Request& request() const
   {
      return request_;
   }

   virtual http::Response& response()
   {
      return response_;
   }

   virtual void writeResponse()
   {
      // add extra response headers
      response_.setHeader("Date", util::httpDate());
      response_.setHeader("Connection", "close");

      // call the response filter if we have one
      if (responseFilter_)
         responseFilter_(&response_);

      // write
      boost::asio::async_write(
          socket_,
          response_.toBuffers(),
          boost::bind(
               &AsyncConnectionImpl<ProtocolType>::handleWrite,
               AsyncConnectionImpl<ProtocolType>::shared_from_this(),
               boost::asio::placeholders::error)
      );
   }

   virtual void writeResponse(const http::Response& response)
   {
      response_.assign(response);
      writeResponse();
   }

   virtual void writeError(const Error& error)
   {
      response_.setError(error);
      writeResponse();
   }
   
private:
   
   void handleRead(const boost::system::error_code& e,
                   std::size_t bytesTransferred)
   {
      try
      {
         if (!e)
         {
            // parse next chunk
            RequestParser::status status = requestParser_.parse(
                                             request_,
                                             buffer_.data(), 
                                             buffer_.data() + bytesTransferred);
            
            // error - return bad request
            if (status == RequestParser::error)
            {
               response_.setStatusCode(http::status::BadRequest);
               writeResponse();
            }
            
            // incomplete -- keep reading
            else if (status == RequestParser::incomplete)
            {
               readSome();
            }
            
            // got valid request -- handle it 
            else
            {
               handler_(AsyncConnectionImpl<ProtocolType>::shared_from_this(),
                        &request_);
            }
         }
         else // error reading
         {
            // log the error if it wasn't connection terminated
            Error error(e, ERROR_LOCATION);
            if (!isConnectionTerminatedError(error))
               LOG_ERROR(error);
            
            // close the socket
            error = closeServerSocket(socket_);
            if (error)
               LOG_ERROR(error);
            
            //
            // no more async operations are initiated here so the shared_ptr to 
            // this connection no more references and is automatically destroyed
            //
         }
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   

   void handleWrite(const boost::system::error_code& e)
   {
      try
      {
         if (e)
         {
            // log the error if it wasn't connection terminated
            Error error(e, ERROR_LOCATION);
            if (!http::isConnectionTerminatedError(error))
               LOG_ERROR(error);
         }
         
         // close the socket
         Error error = closeServerSocket(socket_);
         if (error)
            LOG_ERROR(error);
         
         //
         // no more async operations are initiated here so the shared_ptr to 
         // this connection no more references and is automatically destroyed
         //
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   
   void readSome()
   {
      socket_.async_read_some(
         boost::asio::buffer(buffer_),
         boost::bind(
               &AsyncConnectionImpl<ProtocolType>::handleRead,
               AsyncConnectionImpl<ProtocolType>::shared_from_this(),
               boost::asio::placeholders::error,
               boost::asio::placeholders::bytes_transferred)
      );
   }

private:
   boost::asio::io_service& ioService_;
   typename ProtocolType::socket socket_;
   Handler handler_;
   ResponseFilter responseFilter_;
   boost::array<char, 8192> buffer_ ;
   RequestParser requestParser_ ;
   http::Request request_;
   http::Response response_;
};
   

} // namespace http
} // namespace core

#endif // CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP


