/*
 * SessionHttpConnectionImpl.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_HTTP_CONNECTION_IMPL_HPP
#define SESSION_HTTP_CONNECTION_IMPL_HPP


#include <boost/array.hpp>

#include <boost/utility.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/asio/write.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/SafeConvert.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/RequestParser.hpp>
#include <core/http/SocketUtils.hpp>

#include <core/json/JsonRpc.hpp>

#include <session/SessionHttpConnection.hpp>

namespace session {

std::string rstudioRequestIdFromRequest(const core::http::Request& request)
{
   return request.headerValue("X-RS-RID");
}

template <typename ProtocolType>
class HttpConnectionImpl :
   public HttpConnection,
   public boost::enable_shared_from_this<HttpConnectionImpl<ProtocolType> >,
   boost::noncopyable
{
public:
   typedef boost::function<void(
         boost::shared_ptr<HttpConnectionImpl<ProtocolType> >)> Handler;


public:
   HttpConnectionImpl(boost::asio::io_service& ioService,
                      const Handler& handler)
      : socket_(ioService), handler_(handler)
   {
   }

   virtual ~HttpConnectionImpl()
   {
      // close here as a precaution
      try
      {
         close();
      }
      catch(...)
      {
      }
   }

public:

   // request/resposne (used by Handler)
   virtual const core::http::Request& request() { return request_; }

   virtual void sendResponse(const core::http::Response &response)
   {
      try
      {
         // write the response
         boost::asio::write(socket_,
                            response.toBuffers(
                                  core::http::Header::connectionClose()));
      }
      catch(const boost::system::system_error& e)
      {
         // establish error
         core::Error error = core::Error(e.code(), ERROR_LOCATION);
         error.addProperty("request-uri", request_.uri());

         // log the error if it wasn't connection terminated
         if (!core::http::isConnectionTerminatedError(error))
            LOG_ERROR(error);
      }
      CATCH_UNEXPECTED_EXCEPTION

      // always close connection
      try
      {
         close();
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

   virtual void sendJsonRpcError(const core::Error& error)
   {
      core::json::JsonRpcResponse jsonRpcResponse;
      jsonRpcResponse.setError(error);
      sendJsonRpcResponse(jsonRpcResponse);
   }

   virtual void sendJsonRpcResponse()
   {
      core::json::JsonRpcResponse jsonRpcResponse ;
      sendJsonRpcResponse(jsonRpcResponse);
   }

   virtual void sendJsonRpcResponse(
                        const core::json::JsonRpcResponse& jsonRpcResponse)
   {
      // setup response
      core::http::Response response ;

      // automagic gzip support
      if (request().acceptsEncoding(core::http::kGzipEncoding))
         response.setContentEncoding(core::http::kGzipEncoding);

      // set response
      core::json::setJsonRpcResponse(jsonRpcResponse, &response);

      // send the response
      sendResponse(response);
   }

   // close (occurs automatically after writeResponse, here in case it
   // need to be closed in other circumstances
   virtual void close()
   {
      // always close connection
      core::Error error = core::http::closeSocket(socket_);
      if (error)
         LOG_ERROR(error);
   }

   // other useful introspection methods
   virtual std::string requestId() const { return requestId_; }

   // start reading the request from the connection. once a request
   // is successfully read the Connection is passed to the Handler
   void startReading()
   {
      readSome();
   }

   // get the socket
   typename ProtocolType::socket& socket() { return socket_; }


private:

   // async request reading interface
   void readSome()
   {
      // NOTE: the call to HttpConnection::shared_from_this() is what
      // continues to keep this object alive during processing. when we
      // are finished processing the connection will go out of scope
      // (unless the handler chooses to retain a copy of it e.g. to perform
      // processing in a background thread)

      socket_.async_read_some(
         boost::asio::buffer(buffer_),
         boost::bind(
               &HttpConnectionImpl<ProtocolType>::handleRead,
               HttpConnectionImpl<ProtocolType>::shared_from_this(),
               boost::asio::placeholders::error,
               boost::asio::placeholders::bytes_transferred));
   }

   void handleRead(const boost::system::error_code& e,
                   std::size_t bytesTransferred)
   {
      try
      {
         if (!e)
         {
            // parse next chunk
            core::http::RequestParser::status status = requestParser_.parse(
                                        request_,
                                        buffer_.data(),
                                        buffer_.data() + bytesTransferred);

            // error - return bad request
            if (status == core::http::RequestParser::error)
            {
               core::http::Response response;
               response.setStatusCode(core::http::status::BadRequest);
               sendResponse(response);

               // no more async operations w/ shared_from_this() initiated so this
               // object has no more references to it and will be destroyed
            }

            // incomplete -- keep reading
            else if (status == core::http::RequestParser::incomplete)
            {
               readSome();
            }

            // got valid request -- handle it
            else
            {
               // establish request id
               requestId_ = rstudioRequestIdFromRequest(request_);

               // call handler
               handler_(HttpConnectionImpl<ProtocolType>::shared_from_this());

               // no more async operations w/ shared_from_this() initiated so this
               // object has no more references to it and will be destroyed. note
               // though that the handler may choose to retain a reference
               // (e.g. if it handles the connection in a background thread)
            }
         }
         else // error reading
         {
            // log the error if it wasn't connection terminated
            core::Error error(e, ERROR_LOCATION);
            if (!core::http::isConnectionTerminatedError(error))
               LOG_ERROR(error);

            // close the connection
            close();

            // no more async operations w/ shared_from_this() initiated so this
            // object has no more references to it and will be destroyed
         }
      }
      CATCH_UNEXPECTED_EXCEPTION
   }

private:
   typename ProtocolType::socket socket_;
   boost::array<char, 8192> buffer_ ;
   core::http::RequestParser requestParser_ ;
   core::http::Request request_;
   std::string requestId_;
   Handler handler_;
};

} // namespace session

#endif // SESSION_HTTP_CONNECTION_HPP

