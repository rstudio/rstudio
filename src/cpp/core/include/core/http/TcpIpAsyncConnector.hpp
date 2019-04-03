/*
 * TcpIpAsyncConnector.hpp
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

#ifndef CORE_HTTP_TCP_IP_ASYNC_CONNECTOR_HPP
#define CORE_HTTP_TCP_IP_ASYNC_CONNECTOR_HPP

#include <boost/function.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <boost/asio/deadline_timer.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <boost/asio/placeholders.hpp>

#include <core/http/TcpIpSocketUtils.hpp>
#include <core/Thread.hpp>

// special version of unexpected exception handler which makes
// sure to call the user's ErrorHandler
#define CATCH_UNEXPECTED_ASYNC_CONNECTOR_EXCEPTION \
   catch(const std::exception& e) \
   { \
      handleUnexpectedError(std::string("Unexpected exception: ") + \
                            e.what(), ERROR_LOCATION) ;  \
   } \
   catch(...) \
   { \
      handleUnexpectedError("Unknown exception", ERROR_LOCATION); \
   }

namespace rstudio {
namespace core {
namespace http {  

class TcpIpAsyncConnector :
      public boost::enable_shared_from_this<TcpIpAsyncConnector>,
      boost::noncopyable
{
public:
   typedef boost::function<void()> ConnectedHandler;
   typedef boost::function<void(const core::Error&)> ErrorHandler;

public:
   TcpIpAsyncConnector(boost::asio::io_service& ioService,
                       boost::asio::ip::tcp::socket* pSocket)
     : pSocket_(pSocket),
       resolver_(ioService),
       isConnected_(false),
       hasFailed_(false)
   {
   }

public:
   void connect(const std::string& address,
                const std::string& port,
                const ConnectedHandler& connectedHandler,
                const ErrorHandler& errorHandler,
                const boost::posix_time::time_duration& timeout =
                   boost::posix_time::time_duration(boost::posix_time::pos_infin))
   {
      // save handlers
      connectedHandler_ = connectedHandler;
      errorHandler_ = errorHandler;

      if (!timeout.is_special())
      {
         // start a timer that will cancel any outstanding asynchronous operations
         // when it elapses if the connection operation has not succeeded
         pConnectionTimer_.reset(new boost::asio::deadline_timer(resolver_.get_io_service(), timeout));
         pConnectionTimer_->async_wait(boost::bind(&TcpIpAsyncConnector::onConnectionTimeout,
                                                   TcpIpAsyncConnector::shared_from_this(),
                                                   boost::asio::placeholders::error));
      }

      // start an async resolve
      boost::asio::ip::tcp::resolver::query query(address, port);
      resolver_.async_resolve(
            query,
            boost::bind(&TcpIpAsyncConnector::handleResolve,
                        TcpIpAsyncConnector::shared_from_this(),
                        boost::asio::placeholders::error,
                        boost::asio::placeholders::iterator));
   }

private:

   void onConnectionTimeout(const boost::system::error_code& ec)
   {
      try
      {
         if (ec == boost::system::errc::operation_canceled)
            return;

         LOCK_MUTEX(mutex_)
         {
            if (isConnected_ || hasFailed_)
               return;

            // timer has elapsed and the socket is still not connected
            // cancel any outstanding async operations
            resolver_.cancel();
            pSocket_->cancel();

            // invoke error handler since the connection has failed
            handleError(systemError(boost::system::errc::timed_out, ERROR_LOCATION));
         }
         END_LOCK_MUTEX
      }
      CATCH_UNEXPECTED_ASYNC_CONNECTOR_EXCEPTION
   }

   void handleResolve(
         const boost::system::error_code& ec,
         boost::asio::ip::tcp::resolver::iterator endpoint_iterator)
   {
      if (ec == boost::asio::error::operation_aborted)
         return;

      LOCK_MUTEX(mutex_)
      {
         if (hasFailed_)
            return;

         try
         {
            if (!ec)
            {
               // try endpoints until we successfully connect with one
               boost::asio::ip::tcp::endpoint endpoint = *endpoint_iterator;
               pSocket_->async_connect(
                  endpoint,
                  boost::bind(&TcpIpAsyncConnector::handleConnect,
                              TcpIpAsyncConnector::shared_from_this(),
                              boost::asio::placeholders::error,
                              ++endpoint_iterator));
            }
            else
            {
               handleErrorCode(ec, ERROR_LOCATION);
            }
         }
         CATCH_UNEXPECTED_ASYNC_CONNECTOR_EXCEPTION
      }
      END_LOCK_MUTEX
   }

   void handleConnect(
         const boost::system::error_code& ec,
         boost::asio::ip::tcp::resolver::iterator endpoint_iterator)
   {
      if (ec == boost::asio::error::operation_aborted)
         return;

      LOCK_MUTEX(mutex_)
      {
         if (hasFailed_)
            return;

         try
         {
            if (!ec)
            {
               isConnected_ = true;

               if (pConnectionTimer_)
                  pConnectionTimer_->cancel();

               if (connectedHandler_)
                  connectedHandler_();
            }
            else if (endpoint_iterator !=
                     boost::asio::ip::tcp::resolver::iterator())
            {
               // try next endpoint
               pSocket_->close();
               boost::asio::ip::tcp::endpoint endpoint = *endpoint_iterator;
               pSocket_->async_connect(
                  endpoint,
                  boost::bind(&TcpIpAsyncConnector::handleConnect,
                              TcpIpAsyncConnector::shared_from_this(),
                              boost::asio::placeholders::error,
                              ++endpoint_iterator));
            }
            else
            {
               handleErrorCode(ec, ERROR_LOCATION);
            }
         }
         CATCH_UNEXPECTED_ASYNC_CONNECTOR_EXCEPTION
      }
      END_LOCK_MUTEX
   }

   void handleError(const Error& error)
   {
      hasFailed_ = true;

      if (errorHandler_)
         errorHandler_(error);
   }

   void handleErrorCode(const boost::system::error_code& ec,
                        const ErrorLocation& location)
   {
      if (pConnectionTimer_)
         pConnectionTimer_->cancel();

      handleError(Error(ec, location));
   }

   void handleUnexpectedError(const std::string& description,
                              const ErrorLocation& location)
   {
      if (pConnectionTimer_)
         pConnectionTimer_->cancel();

      Error error = systemError(boost::system::errc::state_not_recoverable,
                                description,
                                location);
      handleError(error);
   }

private:
   boost::asio::ip::tcp::socket* pSocket_;
   boost::asio::ip::tcp::resolver resolver_;
   ConnectedHandler connectedHandler_;
   ErrorHandler errorHandler_;

   bool isConnected_;
   bool hasFailed_;
   boost::mutex mutex_;
   boost::shared_ptr<boost::asio::deadline_timer> pConnectionTimer_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_TCP_IP_ASYNC_CONNECTOR_HPP
