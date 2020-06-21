/*
 * TcpIpSocketUtils.hpp
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

#ifndef CORE_HTTP_TCP_IP_SOCKET_UTILS_HPP
#define CORE_HTTP_TCP_IP_SOCKET_UTILS_HPP

#include <boost/asio/ip/tcp.hpp>

#include <shared_core/Error.hpp>

#include <core/http/SocketAcceptorService.hpp>

namespace rstudio {
namespace core {
namespace http {  
     
template <typename SocketType>
Error connect(boost::asio::io_service& ioService,
              const std::string& address,
              const std::string& port,
              SocketType* pSocket)
{
   using boost::asio::ip::tcp;
   
   // resolve the address
   tcp::resolver resolver(ioService);
   tcp::resolver::query query(address, port);
   
   boost::system::error_code ec;
   tcp::resolver::iterator endpointIterator = resolver.resolve(query, ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   tcp::resolver::iterator end;
   ec = boost::asio::error::host_not_found;
   while (ec && endpointIterator != end)
   {
      // cleanup existing socket connection (if any). don't allow
      // an error shutting down to prevent us from trying a
      // subsequent connection
      Error closeError = closeSocket(*pSocket);
      if (closeError)
         LOG_ERROR(closeError);
      
      // attempt to connect
      pSocket->connect(*endpointIterator++, ec);
   }
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   // set tcp nodelay (propagate any errors)
   pSocket->set_option(tcp::no_delay(true), ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   else
      return Success();
}
                     

inline Error initTcpIpAcceptor(
            SocketAcceptorService<boost::asio::ip::tcp>& acceptorService,
            const std::string& address,
            const std::string& port)
{
   using boost::asio::ip::tcp;
   
   tcp::resolver resolver(acceptorService.ioService());
   tcp::resolver::query query(address, port);
   
   boost::system::error_code ec;
   tcp::resolver::iterator entries = resolver.resolve(query,ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   tcp::acceptor& acceptor = acceptorService.acceptor();
   const tcp::endpoint& endpoint = *entries;
   acceptor.open(endpoint.protocol(), ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   acceptor.set_option(tcp::acceptor::reuse_address(true), ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   acceptor.set_option(tcp::no_delay(true), ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   acceptor.bind(endpoint, ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   acceptor.listen(boost::asio::socket_base::max_connections, ec);
   if (ec)
      return Error(ec, ERROR_LOCATION);
   
   return Success();
}
   
} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_TCP_IP_SOCKET_UTILS_HPP
