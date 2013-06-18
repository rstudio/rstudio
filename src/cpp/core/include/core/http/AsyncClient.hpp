/*
 * AsyncClient.hpp
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

#ifndef CORE_HTTP_ASYNC_CLIENT_HPP
#define CORE_HTTP_ASYNC_CLIENT_HPP

#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <boost/asio/write.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/asio/placeholders.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/read.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/deadline_timer.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/ResponseParser.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/http/ConnectionRetryProfile.hpp>

// special version of unexpected exception handler which makes
// sure to call the user's ErrorHandler
#define CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION \
   catch(const std::exception& e) \
   { \
      handleUnexpectedError(std::string("Unexpected exception: ") + \
                            e.what(), ERROR_LOCATION) ;  \
   } \
   catch(...) \
   { \
      handleUnexpectedError("Unknown exception", ERROR_LOCATION); \
   }

namespace core {
namespace http {

typedef boost::function<void(const http::Response&)> ResponseHandler;
typedef boost::function<void(const core::Error&)> ErrorHandler;


template <typename SocketService>
class AsyncClient :
   public boost::enable_shared_from_this<AsyncClient<SocketService> >,
   boost::noncopyable
{
public:
   AsyncClient(boost::asio::io_service& ioService,
               bool logToStderr = false)
      : ioService_(ioService),
        connectionRetryContext_(ioService),
        logToStderr_(logToStderr)
   {
   }

   virtual ~AsyncClient()
   {
   }

   // populate the request before calling execute
   http::Request& request() { return request_; }

   // set (optional) connection retry profile. must do this prior
   // to calling execute
   void setConnectionRetryProfile(
           const http::ConnectionRetryProfile& connectionRetryProfile)
   {
      connectionRetryContext_.profile = connectionRetryProfile;
   }

   // execute the async client
   void execute(const ResponseHandler& responseHandler,
                const ErrorHandler& errorHandler)
   {
      // set handlers
      responseHandler_ = responseHandler;
      errorHandler_ = errorHandler;

      // connect and write request (implmented in a protocol
      // specific manner by subclassees)
      connectAndWriteRequest();
   }

   // if an embedder of this class calls close() on AsyncClient in it's
   // destructor (for more rigorous cleanup) then it's possible that the
   // onError handler will still be called as a result of the socket close.
   // the callback might then be interacting with a C++ object that has
   // already been deleted. for this case (which does occur in the
   // desktop::NetworkReply class) we provide a method that disables
   // any pending handlers
   void disableHandlers()
   {
      responseHandler_ = ResponseHandler();
      errorHandler_ = ErrorHandler();
   }

   void close()
   {
      Error error = closeSocket(socket().lowest_layer());
      if (error)
         logError(error);
   }

protected:

   boost::asio::io_service& ioService() { return ioService_; }

   virtual SocketService& socket() = 0;

   void handleConnectionError(const Error& connectionError)
   {
      // retry if necessary, otherwise just forward the error to
      // customary error handling scheme

      if (!retryConnectionIfRequired(connectionError))
         handleError(connectionError);
   }

   // asynchronously write the request (called by subclasses after
   // they finish connecting)
   void writeRequest()
   {
      // write
      boost::asio::async_write(
          socket(),
          request_.toBuffers(Header::connectionClose()),
          boost::bind(
               &AsyncClient<SocketService>::handleWrite,
               AsyncClient<SocketService>::shared_from_this(),
               boost::asio::placeholders::error)
      );
   }

   void handleError(const Error& error)
   {
      // close the socket
      close();

      if (errorHandler_)
         errorHandler_(error);
   }

   void handleErrorCode(const boost::system::error_code& ec,
                        const ErrorLocation& location)
   {
      handleError(Error(ec, location));
   }

   void handleUnexpectedError(const std::string& description,
                              const ErrorLocation& location)
   {
      Error error = systemError(boost::system::errc::state_not_recoverable,
                                description,
                                location);
      handleError(error);
   }
   
private:

   virtual void connectAndWriteRequest() = 0;


   bool retryConnectionIfRequired(const Error& connectionError)
   {
      // retry if this is a connection unavailable error and the
      // caller has provided a connection retry profile
      if (http::isConnectionUnavailableError(connectionError) &&
          !connectionRetryContext_.profile.empty())
      {
         // if this is our first retry then set our stop trying time
         // and call the (optional) recovery function
         if (connectionRetryContext_.stopTryingTime.is_not_a_date_time())
         {
            connectionRetryContext_.stopTryingTime =
                  boost::posix_time::microsec_clock::universal_time() +
                  connectionRetryContext_.profile.maxWait;

            if (connectionRetryContext_.profile.recoveryFunction)
               connectionRetryContext_.profile.recoveryFunction();
         }

         // if we aren't alrady past the maximum wait time then
         // wait the appropriate interval and attempt connection again
         if (boost::posix_time::microsec_clock::universal_time() <
             connectionRetryContext_.stopTryingTime)
         {
            return scheduleRetry(); // continuation
         }
         else // otherwise we've waited long enough, bail and
              // perform normal error handling
         {
            return false;
         }
      }
      else // not an error subject to retrying or no retry profile provided
      {
         return false;
      }
   }


   bool scheduleRetry()
   {
      // set expiration
      boost::system::error_code ec;
      connectionRetryContext_.retryTimer.expires_from_now(
                  connectionRetryContext_.profile.retryInterval,
                  ec);

      // attempt to schedule retry timer (should always succeed but
      // include error check to be paranoid/robust)
      if (!ec)
      {
         connectionRetryContext_.retryTimer.async_wait(boost::bind(
               &AsyncClient<SocketService>::handleConnectionRetryTimer,
               AsyncClient<SocketService>::shared_from_this(),
               boost::asio::placeholders::error));

         return true;
      }
      else
      {
         logError(Error(ec, ERROR_LOCATION));
         return false;
      }
   }

   void handleConnectionRetryTimer(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            connectAndWriteRequest();
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void handleWrite(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // initiate async read of the first line of the response
            boost::asio::async_read_until(
              socket(),
              responseBuffer_,
              "\r\n",
              boost::bind(&AsyncClient<SocketService>::handleReadStatusLine,
                          AsyncClient<SocketService>::shared_from_this(),
                          boost::asio::placeholders::error));
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void handleReadStatusLine(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // parase status line
            Error error = ResponseParser::parseStatusLine(&responseBuffer_,
                                                          &response_);
            if (error)
            {
               handleError(error);
            }
            else
            {
               // initiate async read of the headers
               boost::asio::async_read_until(
                 socket(),
                 responseBuffer_,
                 "\r\n\r\n",
                 boost::bind(&AsyncClient<SocketService>::handleReadHeaders,
                             AsyncClient<SocketService>::shared_from_this(),
                             boost::asio::placeholders::error));
            }
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void readSomeContent()
   {
      // provide a hook for subclasses to force termination of
      // content reads (this is needed for named pipes on windows,
      // where the client disconnecting from the server is part
      // of the normal pipe shutdown sequence). without this
      // the subsequent call to handleReadContent will perform
      // the close and respond when it gets a shutdown error (as
      // a result of the server shutting down)
      if (stopReadingAndRespond())
      {
         closeAndRespond();
         return;
      }

      boost::asio::async_read(
         socket(),
         responseBuffer_,
         boost::asio::transfer_at_least(1),
         boost::bind(&AsyncClient<SocketService>::handleReadContent,
                     AsyncClient<SocketService>::shared_from_this(),
                     boost::asio::placeholders::error));
   }

   virtual bool stopReadingAndRespond()
   {
      return false;
   }

   void handleReadHeaders(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // parse headers
            ResponseParser::parseHeaders(&responseBuffer_, &response_);

            // append any lefover buffer contents to the body
            if (responseBuffer_.size() > 0)
               ResponseParser::appendToBody(&responseBuffer_, &response_);

            // start reading content
            readSomeContent();
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   void handleReadContent(const boost::system::error_code& ec)
   {
      try
      {
         if (!ec)
         {
            // copy content
            ResponseParser::appendToBody(&responseBuffer_, &response_);

            // continue reading content
            readSomeContent();
         }
         else if (ec == boost::asio::error::eof ||
                  isShutdownError(ec))
         {
            closeAndRespond();
         }
         else
         {
            handleErrorCode(ec, ERROR_LOCATION);
         }
      }
      CATCH_UNEXPECTED_ASYNC_CLIENT_EXCEPTION
   }

   virtual bool isShutdownError(const boost::system::error_code& ec)
   {
      return false;
   }

   void closeAndRespond()
   {
      close();

      if (responseHandler_)
         responseHandler_(response_);
   }

   void logError(const Error& error) const
   {
      if (logToStderr_)
      {
         std::cerr << error << std::endl;
      }
      else
      {
         LOG_ERROR(error);
      }
   }

// struct and instance variable to track connection retry state
private:
   struct ConnectionRetryContext
   {
      ConnectionRetryContext(boost::asio::io_service& ioService)
         : stopTryingTime(boost::posix_time::not_a_date_time),
           retryTimer(ioService)
      {
      }

      http::ConnectionRetryProfile profile;
      boost::posix_time::ptime stopTryingTime;
      boost::asio::deadline_timer retryTimer;
   };

protected:
   http::Response response_;

private:
   boost::asio::io_service& ioService_;
   ConnectionRetryContext connectionRetryContext_;
   bool logToStderr_;
   ResponseHandler responseHandler_;
   ErrorHandler errorHandler_;
   http::Request request_;
   boost::asio::streambuf responseBuffer_;
};
   

} // namespace http
} // namespace core

#endif // CORE_HTTP_ASYNC_CLIENT_HPP


