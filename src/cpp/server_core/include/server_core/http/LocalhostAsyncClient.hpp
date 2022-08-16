/*
 * LocalhostAsyncClient.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
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

#ifndef SERVER_CORE_LOCALHOST_ASYNC_CLIENT_HPP
#define SERVER_CORE_LOCALHOST_ASYNC_CLIENT_HPP

#include <core/http/TcpIpAsyncClient.hpp>
#include <core/http/TcpIpAsyncClientSsl.hpp>

namespace rstudio {
namespace server_core {
namespace http {

namespace {

// detect when we've got the whole response and force a response and a
// close of the socket (this is because the current version of httpuv
// expects a close from the client end of the socket). however, don't
// do this for Jetty (as it often doesn't send a Content-Length header)
// and do not do it if we are streaming chunked encoding
bool stopReadingAndRespondImpl(const core::http::Response& response,
                               bool chunkedEncoding)
{
   std::string server = response.headerValue("Server");
   if (boost::algorithm::contains(server, "Jetty"))
   {
      return false;
   }
   else
   {
      return !chunkedEncoding &&
             (response.body().length() >= response.contentLength());
   }
}

// ensure that we don't close the connection when a websockets
// upgrade is taking place
bool keepConnectionAliveImpl(const core::http::Response& response)
{
   return response.statusCode() == core::http::status::SwitchingProtocols;
}

} // anonymous namespace

class LocalhostAsyncClient : public core::http::TcpIpAsyncClient
{
public:
   LocalhostAsyncClient(boost::asio::io_service& ioService,
                        const std::string& address,
                        const std::string& port,
                        const boost::posix_time::time_duration& connectionTimeout =
                                               boost::posix_time::time_duration(boost::posix_time::pos_infin))
      : core::http::TcpIpAsyncClient(ioService, address, port, connectionTimeout)
   {
   }

private:
   virtual bool stopReadingAndRespond()
   {
      return stopReadingAndRespondImpl(response_, chunkedEncoding_);
   }

   virtual bool keepConnectionAlive()
   {
      return keepConnectionAliveImpl(response_);
   }
};

class LocalhostAsyncClientSsl : public core::http::TcpIpAsyncClientSsl
{
public:
   LocalhostAsyncClientSsl(boost::asio::io_service& ioService,
                           const std::string& address,
                           const std::string& port,
                           bool verifySslCerts,
                           const std::string& certificateAuthority = std::string(),
                           const boost::posix_time::time_duration& connectionTimeout =
                                      boost::posix_time::time_duration(boost::posix_time::pos_infin),
                           const std::string& hostname = std::string(),
                           const std::string& verifyAddress = std::string())
      : core::http::TcpIpAsyncClientSsl(ioService, address, port, verifySslCerts, certificateAuthority, connectionTimeout, hostname, verifyAddress)
   {
   }

private:
   virtual bool stopReadingAndRespond()
   {
      return stopReadingAndRespondImpl(response_, chunkedEncoding_);
   }

   virtual bool keepConnectionAlive()
   {
      return keepConnectionAliveImpl(response_);
   }
};

} // namespace http
} // namespace server_core
} // namespace rstudio

#endif // SERVER_CORE_LOCALHOST_ASYNC_CLIENT_HPP
