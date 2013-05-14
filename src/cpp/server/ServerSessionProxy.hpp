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

namespace core {
   class Error;
} 

namespace server {
namespace session_proxy {

core::Error initialize();

void setSessionLaunchRequiresAuth(bool requiresAuth);
void setSessionLaunchPassword(const std::string& username,
                              const std::string& password);

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
   
} // namespace session_proxy
} // namespace server

#endif // SERVER_SESSION_PROXY_HPP

