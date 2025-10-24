/*
 * TcpIpAsyncConnector.hpp
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

#ifndef CORE_HTTP_TCP_IP_ASYNC_CONNECTOR_HPP
#define CORE_HTTP_TCP_IP_ASYNC_CONNECTOR_HPP

#include <boost/function.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <boost/asio/system_timer.hpp>
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
                            e.what(), ERROR_LOCATION);  \
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
   TcpIpAsyncConnector(boost::asio::io_context& ioContext,
                       boost::asio::ip::tcp::socket* pSocket)
     : service_(ioContext),
       pSocket_(pSocket),
       resolver_(ioContext),
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

      address_ = address;
      port_ = port;

      if (!timeout.is_special())
      {
         // start a timer that will cancel any outstanding asynchronous operations
         // when it elapses if the connection operation has not succeeded
         pConnectionTimer_.reset(new boost::asio::system_timer(service_, std::chrono::milliseconds(timeout.total_milliseconds())));
         pConnectionTimer_->async_wait(boost::bind(&TcpIpAsyncConnector::onConnectionTimeout,
                                                   TcpIpAsyncConnector::shared_from_this(),
                                                   boost::asio::placeholders::error));
      }

      auto self = TcpIpAsyncConnector::shared_from_this();
      auto callback = [this, self](
            const boost::system::error_code& ec,
            boost::asio::ip::tcp::resolver::results_type results)
      {
         handleResolve(ec, results.begin());
      };

      resolver_.async_resolve(address, port, std::move(callback));
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

            LOG_DEBUG_MESSAGE("In onConnectionTimeout - cancelling socket connection to: " + address_ + ":" + port_);

            // timer has elapsed and the socket is still not connected
            // cancel any outstanding async operations
            resolver_.cancel();
            // avoid throwing an exception by checking for an open socket first
            if (pSocket_->is_open())
               pSocket_->cancel();
            else
               LOG_DEBUG_MESSAGE("Socket to: " + address_ + ":" + port_ + " is already closed in onConnectionTimeout");

            // invoke error handler since the connection has failed
            handleError(systemError(boost::system::errc::timed_out, ERROR_LOCATION));
         }
         END_LOCK_MUTEX
      }
      CATCH_UNEXPECTED_ASYNC_CONNECTOR_EXCEPTION
   }

   void handleResolve(
         const boost::system::error_code& ec,
         boost::asio::ip::tcp::resolver::results_type::iterator endpoint_iterator)
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
               // work-around - in some rare instances, we've seen that Boost will still
               // return us an empty endpoint_iterator, even when successful, which is
               // contrary to the documentation
               if (endpoint_iterator == boost::asio::ip::tcp::resolver::results_type::iterator())
               {
                  handleErrorCode(boost::system::error_code(boost::system::errc::io_error,
                                                            boost::system::system_category()),
                                  ERROR_LOCATION);
                  return;
               }

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
         boost::asio::ip::tcp::resolver::results_type::iterator endpoint_iterator)
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
                     boost::asio::ip::tcp::resolver::results_type::iterator())
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
   boost::asio::io_context& service_;
   boost::asio::ip::tcp::socket* pSocket_;
   boost::asio::ip::tcp::resolver resolver_;
   ConnectedHandler connectedHandler_;
   ErrorHandler errorHandler_;

   std::string address_;
   std::string port_;

   bool isConnected_;
   bool hasFailed_;
   boost::mutex mutex_;
   boost::shared_ptr<boost::asio::system_timer> pConnectionTimer_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_TCP_IP_ASYNC_CONNECTOR_HPP
