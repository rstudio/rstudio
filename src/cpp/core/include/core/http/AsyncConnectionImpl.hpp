/*
 * AsyncConnectionImpl.hpp
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

#ifndef CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP
#define CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP

#include <boost/array.hpp>
#include <boost/optional.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/date_time/posix_time/ptime.hpp>

#include <boost/asio/write.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/asio/ssl.hpp>
#include <boost/asio/ip/tcp.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>

#include <core/http/AsyncUriHandler.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/http/StreamWriter.hpp>
#include <core/http/RequestParser.hpp>
#include <core/http/AsyncConnection.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace http {

typedef boost::function<void(const boost::system::error_code&, std::size_t)> ReadHandler;
typedef boost::function<void(const boost::system::error_code&, bool)> WriteHandler;

class ISocketOperations
{
public:
   virtual void asyncReadSome(const boost::asio::mutable_buffer& buffers, ReadHandler handler) = 0;
   virtual void asyncWrite(const boost::asio::mutable_buffer& buffers, Socket::Handler handler) = 0;
   virtual void asyncWrite(const boost::asio::const_buffer& buffers, Socket::Handler handler) = 0;
   virtual void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers, Socket::Handler handler) = 0;
};

template <typename StreamType>
class SocketOperations : public ISocketOperations
{
public:
   SocketOperations(const boost::shared_ptr<StreamType>& stream,
                    boost::asio::io_context::strand& strand) :
                          stream_(stream),
                          strand_(strand)
   {
   }

   virtual ~SocketOperations()
   {
   }

   virtual void asyncReadSome(const boost::asio::mutable_buffer& buffers, ReadHandler handler)
   {
      stream_->async_read_some(buffers, boost::asio::bind_executor(strand_, handler));
   }

   virtual void asyncWrite(const boost::asio::mutable_buffer& buffers, Socket::Handler handler)
   {
      boost::asio::async_write(*stream_, buffers, boost::asio::bind_executor(strand_, handler));
   }

   virtual void asyncWrite(const boost::asio::const_buffer& buffer, Socket::Handler handler)
   {
      boost::asio::async_write(*stream_, buffer, boost::asio::bind_executor(strand_, handler));
   }

   virtual void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers, Socket::Handler handler)
   {
      boost::asio::async_write(*stream_, buffers, boost::asio::bind_executor(strand_, handler));
   }

private:
   boost::shared_ptr<StreamType> stream_;
   boost::asio::io_context::strand& strand_;
};

template <typename SocketType>
class AsyncConnectionImpl :
   public AsyncConnection,
   public boost::enable_shared_from_this<AsyncConnectionImpl<SocketType> >,
   boost::noncopyable
{
public:
   typedef boost::function<void(
         boost::shared_ptr<AsyncConnectionImpl<SocketType> >,
         http::Request*)> Handler;

    typedef boost::function<void(
         boost::weak_ptr<AsyncConnectionImpl<SocketType>>, bool, bool)> ClosedHandler;

   typedef boost::function<bool(
         boost::shared_ptr<AsyncConnectionImpl<SocketType> >,
         http::Request*)> HeadersParsedHandler;

public:
   AsyncConnectionImpl(boost::asio::io_context& ioContext,
                       boost::shared_ptr<boost::asio::ssl::context> sslContext,
                       long requestSequence,
                       const HeadersParsedHandler& onHeadersParsed,
                       const Handler& onRequestParsed,
                       const ClosedHandler& onClosed,
                       const RequestFilter& requestFilter = RequestFilter(),
                       const ResponseFilter& responseFilter = ResponseFilter())
      : ioContext_(ioContext),
        onHeadersParsed_(onHeadersParsed),
        onRequestParsed_(onRequestParsed),
        onClosed_(onClosed),
        requestFilter_(requestFilter),
        responseFilter_(responseFilter),
        closed_(false),
        requestSequence_(requestSequence),
        bytesTransferred_(0),
        strand_(ioContext)
        
   {
      if (sslContext)
      {
         sslStream_.reset(new boost::asio::ssl::stream<SocketType>(ioContext, *sslContext));

         // get socket and store it in a separate shared pointer
         // the owner is the SSL stream pointer - this ensures we don't double delete
         socket_.reset(sslStream_, &sslStream_->next_layer());

         socketOperations_.reset(new SocketOperations<boost::asio::ssl::stream<SocketType> >(sslStream_, strand_));
      }
      else
      {
         socket_.reset(new SocketType(ioContext));
         socketOperations_.reset(new SocketOperations<SocketType>(socket_, strand_));
      }
      request_.setRequestSequence(requestSequence);
   }

   virtual ~AsyncConnectionImpl()
   {
      try
      {
         close(true);
      }
      catch(...)
      {
      }
   }

   SocketType& socket()
   {
      return *socket_;
   }

   void startReading()
   {
      startTime_ = boost::posix_time::microsec_clock::universal_time();
      request_.setStartTime(startTime_);

      if (sslStream_)
      {
         // begin ssl handshake
         sslStream_->async_handshake(boost::asio::ssl::stream_base::server,
                                     boost::bind(&AsyncConnectionImpl<SocketType>::handleHandshake,
                                                 AsyncConnectionImpl<SocketType>::shared_from_this(),
                                                 boost::asio::placeholders::error));
      }
      else
      {
         readSome();
      }
   }

   virtual boost::asio::io_context& ioContext()
   {
      return ioContext_;
   }

   virtual const http::Request& request() const
   {
      return request_;
   }

   virtual http::Response& response()
   {
      return response_;
   }

   virtual void writeResponse(bool close = true, Socket::Handler handler = Socket::NullHandler)
   {
      sendingResponse_ = true;

      // add extra response headers
      if (!response_.containsHeader("Date"))
         response_.setHeader("Date", util::httpDate());
      if (close)
         response_.setHeader("Connection", "close");
      response_.setHeader("X-Content-Type-Options", "nosniff");

      // call the response filter if we have one
      if (responseFilter_)
         responseFilter_(originalRequest_, &response_);

      if (response_.isStreamResponse())
      {
         boost::shared_ptr<core::http::StreamWriter<SocketType> > pWriter(
                  new core::http::StreamWriter<SocketType>(
                     socket(), // using socket(), not *socket in case of SSL connection
                     response_,
                     boost::bind(&AsyncConnectionImpl<SocketType>::onStreamComplete,
                                 AsyncConnectionImpl<SocketType>::shared_from_this(),
                                 close,
                                 handler),
                     boost::bind(&AsyncConnectionImpl<SocketType>::handleStreamError,
                                 AsyncConnectionImpl<SocketType>::shared_from_this(),
                                 close,
                                 handler,
                                 _1)));

         pWriter->write();
         return;
      }
      else
      {
         // make sure that if no body and content-length were specified,
         // and the status code is not 1xx or 204,
         // we send 0 for Content-Length
         // otherwise, this response will be invalid
         if ((response_.body().empty() && response_.headerValue("Content-Length").empty()) &&
             (response_.statusCode() < 100 || response_.statusCode() > 199) &&
             response_.statusCode() != 204)
         {
             response_.setContentLength(0);
         }

         // After asyncWrite completes, we want to first do our cleanup
         // (this->handleWrite()) and then call the caller's handler
         Socket::Handler handlers = boost::bind(
            &Socket::joinHandlers,
            static_cast<Socket::Handler>(boost::bind(
               &AsyncConnectionImpl<SocketType>::handleWrite,
               AsyncConnectionImpl<SocketType>::shared_from_this(),
               boost::asio::placeholders::error,
               close)),
            handler,
            _1,
            _2
         );

         // write
         socketOperations_->asyncWrite(response_.toBuffers(), handlers);
      }
   }

   virtual void writeResponse(const http::Response& response,
                              bool close = true,
                              const Headers& additionalHeaders = Headers(),
                              Socket::Handler handler = Socket::NullHandler)
   {
      response_.assign(response, additionalHeaders);
      writeResponse(close, handler);
   }

   virtual void writeResponseHeaders(Socket::Handler handler)
   {
      if (!response_.containsHeader("Date"))
         response_.setHeader("Date", util::httpDate());

      // write only the header buffers
      socketOperations_->asyncWrite(response_.headerBuffers(), handler);
   }

   virtual void writeError(const Error& error)
   {
      response_.setError(error);
      writeResponse();
   }

   // satisfy lower-level http::Socket interface (used when the connection
   // is upgraded to a websocket connection and no longer conforms to the
   // request/response protocol used by the class in the ordinary course
   // of business)

   virtual void asyncReadSome(boost::asio::mutable_buffer buffer,
                              Socket::Handler handler)
   {
      socketOperations_->asyncReadSome(buffer, handler);
   }

   virtual void asyncWrite(
                     const std::vector<boost::asio::const_buffer>& buffers,
                     Socket::Handler handler)
   {
      socketOperations_->asyncWrite(buffers, handler);
   }

   virtual void asyncWrite(
                     const boost::asio::const_buffer& buffer,
                     Socket::Handler handler)
   {
      socketOperations_->asyncWrite(buffer, handler);
   }

   virtual void close()
   {
      close(false);
   }

   virtual void close(bool fromDestructor)
   {
      if (fromDestructor && !closed_)
      {
         LOG_DEBUG_MESSAGE("Closing connection without an explicit response for URI: " + request().debugInfo());
      }

      // ensure the socket is only closed once - boost considers
      // multiple closes an error, and this can lead to a segfault
      ClosedHandler closedHandler;
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         if (!closed_)
         {
            Error error = closeSocket(*socket_);
            if (error && !core::http::isConnectionTerminatedError(error))
               logConnectionError(error);

            closed_ = true;
            closedHandler = onClosed_;

            // cleanup any associated data with the connection
            connectionData_.clear();
         }
      }
      END_LOCK_MUTEX;

      // notify that we have closed the connection
      // we do this after giving up the mutex to prevent potential deadlock
      if (closedHandler)
         closedHandler(AsyncConnectionImpl<SocketType>::weak_from_this(), fromDestructor, requestParsed_);
   }

   virtual boost::asio::io_context::strand& getStrand()
   {
      return strand_;
   }

   void setUploadHandler(const AsyncUriUploadHandlerFunction& handler)
   {
      FormHandler formHandler = boost::bind(handler,
                                            AsyncConnectionImpl<SocketType>::shared_from_this(),
                                            _1,
                                            _2);

      requestParser_.setFormHandler(formHandler);
   }

   virtual void continueParsing()
   {
      // continue parsing by reinvoking the read handler
      // with the amount of bytes that were read last time it was called
      // this is posted to the io_context to be invoked asynchronously
      // so callers are not reentrantly locked
      boost::asio::post(
               ioContext_,
               boost::bind(
                  &AsyncConnectionImpl<SocketType>::handleRead,
                  AsyncConnectionImpl<SocketType>::shared_from_this(),
                  boost::system::error_code(),
                  bytesTransferred_));
   }

   virtual void setData(const boost::any& data)
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         connectionData_ = data;
      }
      END_LOCK_MUTEX
   }

   virtual boost::any getData()
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         return connectionData_;
      }
      END_LOCK_MUTEX

      return boost::any();
   }

   bool closed() const
   {
      return closed_;
   }

   bool requestParsed() const
   {
      return requestParsed_;
   }

   bool sendingResponse() const
   {
      return sendingResponse_;
   }

   boost::posix_time::ptime startTime() const
   {
      return startTime_;
   }

   long requestSequence() const
   {
      return requestSequence_;
   }

   void setUsername(const std::string& username)
   {
      request_.setUsername(username);
   }

   const std::string& username() const
   {
      return request().username();
   }

   void setHandlerPrefix(const std::string& handlerPrefix)
   {
      request_.setHandlerPrefix(handlerPrefix);
   }

   const std::string& handlerPrefix() const
   {
      return request().handlerPrefix();
   }
   
private:

   void logConnectionError(Error error)
   {
      error.addProperty("request", request().debugInfo());
      LOG_ERROR(error);
   }

   void handleRead(const boost::system::error_code& e,
                   std::size_t bytesTransferred)
   {
      try
      {
         if (!e)
         {
            bytesTransferred_ = bytesTransferred;

            // we must synchronize access to the RequestParser, because while returning
            // from a suspending form handler, we could be told to resume processing
            // before the request parser properly saves its temporary state, causing all sorts of havoc
            RequestParser::status status = RequestParser::error;
            RECURSIVE_LOCK_MUTEX(mutex_)
            {
               // parse next chunk
               status = requestParser_.parse(request_,
                                             buffer_.data(),
                                             buffer_.data() + bytesTransferred);
            }
            END_LOCK_MUTEX

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

            // headers parsed - body parsing has not yet begun
            else if (status == RequestParser::headers_parsed)
            {
               // record the original request
               originalRequest_.assign(request_);

               // call the request filter if we have one
               if (requestFilter_)
               {
                  // call the filter (passing a continuation to be invoked
                  // once the filter is completed)
                  requestFilter_(
                     ioContext(),
                     &request_,
                     boost::bind(
                        &AsyncConnectionImpl<SocketType>::requestFilterContinuation,
                        AsyncConnectionImpl<SocketType>::shared_from_this(),
                        _1, e, bytesTransferred
                     ));
               }
               else
               {
                  if (!callHeadersParsedHandler())
                  {
                     writeResponse();
                     return;
                  }

                  // we need to resume body parsing by recalling the parse
                  // method and providing the exact same buffer to continue
                  // from where we left off
                  handleRead(e, bytesTransferred);
               }

               return;
            }
            
            // pause - save current state
            else if (status == RequestParser::pause)
            {
               // we will need to reinvoke this method with the same
               // buffer when told to resume, so for now simply return
               // to keep our internal state the same
               return;
            }

            // form complete - do nothing since the form handler
            // has been invoked by the request parser as appropriate
            else if (status == RequestParser::form_complete)
            {
               return;
            }

            // got valid request -- handle it 
            else
            {  
               callHandler();
            }
         }
         else // error reading
         {
            // log the error if it wasn't connection terminated
            Error error(e, ERROR_LOCATION);
            if (!isConnectionTerminatedError(error))
               logConnectionError(error);
            
            // close the socket
            close();
            
            //
            // no more async operations are initiated here so the shared_ptr to 
            // this connection no more references and is automatically destroyed
            //
         }
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   
   void requestFilterContinuation(boost::shared_ptr<http::Response> response,
                                  const boost::system::error_code& e,
                                  std::size_t bytesTransferred)
   {
      if (response)
      {
         response_.assign(*response);
         writeResponse();
      }
      else
      {
         if (!callHeadersParsedHandler())
         {
            writeResponse();
            return;
         }

         // we need to resume body parsing by recalling the parse
         // method and providing the exact same buffer to continue
         // from where we left off
         handleRead(e, bytesTransferred);
      }
   }

   bool callHeadersParsedHandler()
   {
      return onHeadersParsed_(AsyncConnectionImpl<SocketType>::shared_from_this(),
                              &request_);
   }

   void callHandler()
   {
      requestParsed_ = true;
      onRequestParsed_(AsyncConnectionImpl<SocketType>::shared_from_this(),
                       &request_);
   }

   void handleWrite(const boost::system::error_code& e, bool closeSocket)
   {
      try
      {
         if (e)
         {
            // log the error if it wasn't connection terminated
            Error error(e, ERROR_LOCATION);
            if (!http::isConnectionTerminatedError(error) &&
                !http::isWrongProtocolTypeError((error)))
            {
               logConnectionError(error);
            }
         }
         
         // close the socket, if requested or if error
         if (e || closeSocket)
         {
            close();
         }

         //
         // no more async operations are initiated here so the shared_ptr to 
         // this connection no more references and is automatically destroyed
         //
      }
      CATCH_UNEXPECTED_EXCEPTION
   }
   
   void readSome()
   {
      socketOperations_->asyncReadSome(boost::asio::buffer(buffer_),
                                       boost::bind(&AsyncConnectionImpl<SocketType>::handleRead,
                                                   AsyncConnectionImpl<SocketType>::shared_from_this(),
                                                   boost::asio::placeholders::error,
                                                   boost::asio::placeholders::bytes_transferred));
   }

   void handleHandshake(const boost::system::error_code& ec)
   {
      if (ec)
      {
         Error error(ec, ERROR_LOCATION);
         if (!core::http::isConnectionTerminatedError(error))
            logConnectionError(error);

         return;
      }

      // ssl stream established - start reading
      readSome();
   }

   void onStreamComplete(bool close, Socket::Handler handler)
   {
      if (close)
         this->close();
      // -1 isn't the correct number of bytes written, but we don't have access
      // to that information at this point.
      handler(boost::system::error_code(), -1);
   }

   void handleStreamError(bool close, Socket::Handler handler, const Error& error)
   {
      if (!core::http::isConnectionTerminatedError(error))
         logConnectionError(error);
      if (close)
         this->close();
      // jcheng: boost::system::generic_category() isn't correct, but I don't
      // know how to get an error_code category out of an Error object.
      // -1 isn't the correct number of bytes written, but we don't have access
      // to that information at this point.
      handler(boost::system::error_code(error.getCode(), boost::system::generic_category()), -1);
   }

private:
   boost::asio::io_context& ioContext_;

   // optional ssl stream
   // not used if the connection is not ssl enabled
   boost::shared_ptr<boost::asio::ssl::stream<SocketType> > sslStream_;

   // underlying socket
   boost::shared_ptr<SocketType> socket_;

   // socket wrapper to forward calls to an SSL stream or a raw socket
   // depending on whether or not SSL is enabled
   boost::shared_ptr<ISocketOperations> socketOperations_;

   HeadersParsedHandler onHeadersParsed_;
   Handler onRequestParsed_;
   ClosedHandler onClosed_;
   FormHandler formHandler_;
   RequestFilter requestFilter_;
   ResponseFilter responseFilter_;
   boost::array<char, 8192> buffer_;
   RequestParser requestParser_;
   Request originalRequest_;
   http::Request request_;
   http::Response response_;

   boost::recursive_mutex mutex_;
   bool closed_ = false;
   bool requestParsed_ = false;
   bool sendingResponse_ = false;
   boost::posix_time::ptime startTime_;
   int requestSequence_;

   size_t bytesTransferred_;

   boost::any connectionData_;

protected:
   boost::asio::io_context::strand strand_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_CONNECTION_IMPL_HPP


