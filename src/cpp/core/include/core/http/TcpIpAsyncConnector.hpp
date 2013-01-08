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

#include <boost/asio/ip/tcp.hpp>
#include <boost/asio/placeholders.hpp>


#include <core/http/TcpIpSocketUtils.hpp>

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
       resolver_(ioService)
   {
   }

public:
   void connect(const std::string& address,
                const std::string& port,
                const ConnectedHandler& connectedHandler,
                const ErrorHandler& errorHandler)
   {
      // save handlers
      connectedHandler_ = connectedHandler;
      errorHandler_ = errorHandler;

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

   void handleResolve(
         const boost::system::error_code& ec,
         boost::asio::ip::tcp::resolver::iterator endpoint_iterator)
   {
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

   void handleConnect(
         const boost::system::error_code& ec,
         boost::asio::ip::tcp::resolver::iterator endpoint_iterator)
   {
      try
      {
         if (!ec)
         {
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

   void handleError(const Error& error)
   {
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
   boost::asio::ip::tcp::socket* pSocket_;
   boost::asio::ip::tcp::resolver resolver_;
   ConnectedHandler connectedHandler_;
   ErrorHandler errorHandler_;
};

} // namespace http
} // namespace core

#endif // CORE_HTTP_TCP_IP_ASYNC_CONNECTOR_HPP
