/*
 * TcpIpAsyncClient.hpp
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

#ifndef CORE_HTTP_TCP_IP_ASYNC_CLIENT_HPP
#define CORE_HTTP_TCP_IP_ASYNC_CLIENT_HPP

#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>

#include <boost/asio/ip/tcp.hpp>

#include <core/Log.hpp>

#include <core/http/AsyncClient.hpp>
#include <core/http/TcpIpSocketUtils.hpp>
#include <core/http/TcpIpAsyncConnector.hpp>

namespace rstudio {
namespace core {
namespace http {  

class TcpIpAsyncClient :
      public AsyncClient<boost::asio::ip::tcp::socket>
{
public:
   TcpIpAsyncClient(boost::asio::io_service& ioService,
                    const std::string& address,
                    const std::string& port,
                    const boost::posix_time::time_duration& connectionTimeout =
                       boost::posix_time::time_duration(boost::posix_time::pos_infin))
     : AsyncClient<boost::asio::ip::tcp::socket>(ioService),
       socket_(ioService),
       address_(address),
       port_(port),
       connectionTimeout_(connectionTimeout)
   {
   }

protected:

   virtual boost::asio::ip::tcp::socket& socket()
   {
      return socket_;
   }

private:

   virtual void connectAndWriteRequest()
   {
      boost::shared_ptr<TcpIpAsyncConnector> pAsyncConnector(
                     new TcpIpAsyncConnector(ioService(), &(socket())));

      pAsyncConnector->connect(
            address_,
            port_,
            boost::bind(&TcpIpAsyncClient::writeRequest,
                        TcpIpAsyncClient::sharedFromThis()),
            boost::bind(&TcpIpAsyncClient::handleConnectionError,
                        TcpIpAsyncClient::sharedFromThis(),
                        _1),
            connectionTimeout_);

   }

   virtual std::string getDefaultHostHeader()
   {
      return address_ + ":" + port_;
   }

   const boost::shared_ptr<TcpIpAsyncClient> sharedFromThis()
   {
      boost::shared_ptr<AsyncClient<boost::asio::ip::tcp::socket> > ptrShared
                                                = shared_from_this();

      return boost::static_pointer_cast<TcpIpAsyncClient>(ptrShared);
   }

private:
   boost::asio::ip::tcp::socket socket_;
   std::string address_;
   std::string port_;
   boost::posix_time::time_duration connectionTimeout_;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_TCP_IP_ASYNC_CLIENT_HPP
