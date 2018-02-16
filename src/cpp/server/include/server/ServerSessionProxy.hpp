/*
 * ServerSessionProxy.hpp
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

#ifndef SERVER_SESSION_PROXY_HPP
#define SERVER_SESSION_PROXY_HPP

#include <string>

#include <core/http/AsyncConnection.hpp>
#include <core/http/TcpIpAsyncClient.hpp>

#include <core/r_util/RSessionContext.hpp>

#include "ServerSessionManager.hpp"

namespace rstudio {
namespace core {
   class Error;
} 
}

namespace rstudio {
namespace server {
namespace session_proxy {

class LocalhostAsyncClient : public core::http::TcpIpAsyncClient
{
public:
   LocalhostAsyncClient(boost::asio::io_service& ioService,
                        const std::string& address,
                        const std::string& port)
      : core::http::TcpIpAsyncClient(ioService, address, port)
   {
   }

private:
   // detect when we've got the whole response and force a response and a
   // close of the socket (this is because the current version of httpuv
   // expects a close from the client end of the socket). however, don't
   // do this for Jetty (as it often doesn't send a Content-Length header)
   virtual bool stopReadingAndRespond()
   {
      std::string server = response_.headerValue("Server");
      if (boost::algorithm::contains(server, "Jetty"))
      {
         return false;
      }
      else
      {
         return response_.body().length() >= response_.contentLength();
      }
   }

   // ensure that we don't close the connection when a websockets
   // upgrade is taking place
   virtual bool keepConnectionAlive()
   {
      return response_.statusCode() == core::http::status::SwitchingProtocols;
   }
};

struct RequestType
{
   enum
   {
      Rpc,
      Content,
      Events
   };
};

typedef boost::function<void (const core::http::Response&,
                              const std::string& baseAddress,
                              boost::shared_ptr<core::http::IAsyncClient>)> LocalhostResponseHandler;

core::Error initialize();

core::Error runVerifyInstallationSession();

void proxyContentRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection) ;

void proxyRpcRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection) ;

void proxyEventsRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection);

void proxyLocalhostRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection);
   
bool requiresSession(const core::http::Request& request);

typedef boost::function<bool(
    boost::shared_ptr<core::http::AsyncConnection>,
    const core::r_util::SessionContext&
    )> ProxyFilter;
void setProxyFilter(ProxyFilter filter);

typedef boost::function<void(core::http::Request*)> ProxyRequestFilter;
void setProxyRequestFilter(ProxyRequestFilter filter);

typedef boost::function<bool(
    boost::shared_ptr<core::http::AsyncConnection>,
    const std::string&,
    core::r_util::SessionContext*)> SessionContextSource;
void setSessionContextSource(SessionContextSource source);

} // namespace session_proxy
} // namespace server
} // namespace rstudio

#endif // SERVER_SESSION_PROXY_HPP

