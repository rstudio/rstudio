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
    const std::string&
    )> ProxyFilter;
void setProxyFilter(ProxyFilter filter);

typedef boost::function<bool(
    boost::shared_ptr<core::http::AsyncConnection>,
    const std::string&,
    core::r_util::SessionContext*)> SessionContextSource;
void setSessionContextSource(SessionContextSource source);

} // namespace session_proxy
} // namespace server
} // namespace rstudio

#endif // SERVER_SESSION_PROXY_HPP

