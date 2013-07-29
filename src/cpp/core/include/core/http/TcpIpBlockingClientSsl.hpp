/*
 * TcpIpBlockingClientSsl.hpp
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

#ifndef CORE_HTTP_TCP_IP_BLOCKING_CLIENT_SSL_HPP
#define CORE_HTTP_TCP_IP_BLOCKING_CLIENT_SSL_HPP

#include <core/http/BlockingClient.hpp>

#include <core/http/TcpIpAsyncClientSsl.hpp>

namespace core {
namespace http {  

inline Error sendSslRequest(const std::string& address,
                            const std::string& port,
                            bool verify,
                            const http::Request& request,
                            http::Response* pResponse,)
{
   // create client
   boost::asio::io_service ioService;
   boost::shared_ptr<TcpIpAsyncClientSsl> pClient(
         new TcpIpAsyncClientSsl(ioService, address, port, verify));

   // execute blocking request
   return sendRequest<boost::asio::ssl::stream<boost::asio::ip::tcp::socket> >
                                                         (ioService,
                                                          pClient,
                                                          request,
                                                          pResponse);
}
   
} // namespace http
} // namespace core

#endif // CORE_HTTP_TCP_IP_BLOCKING_CLIENT_SSL_HPP
