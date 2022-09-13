/*
 * SessionHttpConnectionImpl.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
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
#include <boost/asio/ssl.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/RequestParser.hpp>
#include <core/http/Socket.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/http/StreamWriter.hpp>

#include <core/json/JsonRpc.hpp>

#include <session/SessionHttpConnection.hpp>

#include "SessionHttpConnectionUtils.hpp"

using namespace boost::placeholders;

namespace rstudio {
namespace session {

template <typename ProtocolType>
class HttpConnectionImpl :
   public HttpConnection,
   public boost::enable_shared_from_this<HttpConnectionImpl<ProtocolType> >,
   boost::noncopyable
{
public:
   typedef boost::function<void(boost::shared_ptr<HttpConnectionImpl<ProtocolType> >)> HeadersParsedHandler;
   typedef boost::function<void(
         boost::shared_ptr<HttpConnectionImpl<ProtocolType> >)> Handler;


public:
   HttpConnectionImpl(boost::asio::io_service& ioService,
                      boost::shared_ptr<boost::asio::ssl::context> sslContext,
                      const HeadersParsedHandler& headersParsed,
                      const Handler& handler)
      : headersParsedHandler_(headersParsed), handler_(handler),
        receivedTime_(std::chrono::steady_clock::now())
   {
      if (sslContext)
      {
         sslStream_.reset(new boost::asio::ssl::stream<typename ProtocolType::socket>(ioService, *sslContext));

         // get socket and store it in a separate shared pointer
         // the owner is the SSL stream pointer - this ensures we don't double delete
         socket_.reset(sslStream_, &sslStream_->next_layer());
      }
      else
      {
         socket_.reset(new typename ProtocolType::socket(ioService));
      }
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

   // request/response (used by Handler)
   virtual const core::http::Request& request() { return request_; }

   virtual void sendResponse(const core::http::Response &response)
   {
      try
      {
         if (response.isStreamResponse())
         {
            if (sslStream_)
            {
               boost::shared_ptr<core::http::StreamWriter<boost::asio::ssl::stream<typename ProtocolType::socket>> > pWriter(
                        new core::http::StreamWriter<boost::asio::ssl::stream<typename ProtocolType::socket>>(
                           *sslStream_,
                           response,
                           boost::bind(&HttpConnectionImpl::onStreamComplete,
                                       HttpConnectionImpl<ProtocolType>::shared_from_this()),
                           boost::bind(&HttpConnectionImpl::handleError,
                                       HttpConnectionImpl<ProtocolType>::shared_from_this(),
                                       _1)));

               pWriter->write();
            }
            else
            {
               boost::shared_ptr<core::http::StreamWriter<typename ProtocolType::socket> > pWriter(
                        new core::http::StreamWriter<typename ProtocolType::socket>(
                           socket(),
                           response,
                           boost::bind(&HttpConnectionImpl::onStreamComplete,
                                       HttpConnectionImpl<ProtocolType>::shared_from_this()),
                           boost::bind(&HttpConnectionImpl::handleError,
                                       HttpConnectionImpl<ProtocolType>::shared_from_this(),
                                       _1)));
               pWriter->write();
            }

            return;
         }

         // write the non streaming response
         if (sslStream_)
         {
            boost::asio::write(*sslStream_,
                               response.toBuffers(
                                     core::http::Header::connectionClose()));
         }
         else
         {
            boost::asio::write(socket(),
                               response.toBuffers(
                                     core::http::Header::connectionClose()));
         }
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

   virtual void sendJsonRpcResponse(
           core::json::JsonRpcResponse& jsonRpcResponse)
   {
      // setup response
      core::http::Response response;

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
      core::Error error = core::http::closeSocket(*socket_);
      if (error)
         LOG_ERROR(error);
   }

   // other useful introspection methods
   virtual std::string requestId() const { return requestId_; }

   // start reading the request from the connection. once a request
   // is successfully read the Connection is passed to the Handler
   void startReading()
   {
      if (sslStream_)
      {
         // Begin ssl handshake - on success handleHandake will invoke readSome() to start reading
         sslStream_->async_handshake(boost::asio::ssl::stream_base::server,
                                     boost::bind(&HttpConnectionImpl<ProtocolType>::handleHandshake,
                                                 HttpConnectionImpl<ProtocolType>::shared_from_this(),
                                                 boost::asio::placeholders::error));
      }
      else
      {
         readSome();
      }
   }

   // get the socket
   typename ProtocolType::socket& socket() { return *socket_; }

   virtual void setUploadHandler(const core::http::UriAsyncUploadHandlerFunction& uploadHandler)
   {
      auto me = HttpConnectionImpl<ProtocolType>::shared_from_this();
      auto continuation = [=](core::http::Response* pResponse)
      {
         me->sendResponse(*pResponse);
      };

      // request_ guaranteed to stay alive with the duration of this object as continuation captures
      // a shared pointer to this object
      core::http::FormHandler formHandler = boost::bind(uploadHandler,
                                                        boost::cref(request_),
                                                        _1,
                                                        _2,
                                                        continuation);

      requestParser_.setFormHandler(formHandler);
   }

   virtual bool isAsyncRpc() const
   {
      return false;
   }

   virtual std::chrono::steady_clock::time_point receivedTime() const
   {
      return receivedTime_;
   }

private:

   // async request reading interface
   void readSome()
   {
      // NOTE: the call to HttpConnection::shared_from_this() is what
      // continues to keep this object alive during processing. when we
      // are finished processing the connection will go out of scope
      // (unless the handler chooses to retain a copy of it e.g. to perform
      // processing in a background thread)

      if (sslStream_)
      {
         sslStream_->async_read_some(
            boost::asio::buffer(buffer_),
            boost::bind(
                  &HttpConnectionImpl<ProtocolType>::handleRead,
                  HttpConnectionImpl<ProtocolType>::shared_from_this(),
                  boost::asio::placeholders::error,
                  boost::asio::placeholders::bytes_transferred));
      }
      else
      {
         socket_->async_read_some(
            boost::asio::buffer(buffer_),
            boost::bind(
                  &HttpConnectionImpl<ProtocolType>::handleRead,
                  HttpConnectionImpl<ProtocolType>::shared_from_this(),
                  boost::asio::placeholders::error,
                  boost::asio::placeholders::bytes_transferred));
      }
   }

   void handleHandshake(const boost::system::error_code& ec)
   {
      if (ec)
      {
         core::Error error(ec, ERROR_LOCATION);
         if (!core::http::isConnectionTerminatedError(error))
            LOG_ERROR(error);

         return;
      }

      // ssl stream established - start reading
      readSome();
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

            // headers parsed - body parsing has not yet begun
            else if (status == core::http::RequestParser::headers_parsed)
            {
               headersParsedHandler_(HttpConnectionImpl<ProtocolType>::shared_from_this());

               // establish request id
               requestId_ = connection::rstudioRequestIdFromRequest(request_);

               // we need to resume body parsing by recalling the parse
               // method and providing the exact same buffer to continue
               // from where we left off
               handleRead(e, bytesTransferred);

               return;
            }

            // form complete - do nothing since the form handler
            // has been invoked by the request parser as appropriate
            else if (status == core::http::RequestParser::form_complete)
            {
               return;
            }

            // got valid request -- handle it
            else
            {
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

   void onStreamComplete()
   {
      close();
   }

   void handleError(const core::Error& error)
   {
      LOG_ERROR(error);
      close();
   }

private:
   // optional ssl stream
   // not used if the connection is not ssl enabled
   boost::shared_ptr<boost::asio::ssl::stream<typename ProtocolType::socket> > sslStream_;

   // underlying socket
   boost::shared_ptr<typename ProtocolType::socket> socket_;
   boost::array<char, 8192> buffer_;
   core::http::RequestParser requestParser_;
   core::http::Request request_;
   std::string requestId_;
   HeadersParsedHandler headersParsedHandler_;
   Handler handler_;
   std::chrono::steady_clock::time_point receivedTime_;
};

} // namespace session
} // namespace rstudio

#endif // SESSION_HTTP_CONNECTION_HPP

