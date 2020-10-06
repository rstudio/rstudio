/*
 * ServerSessionProxyOverlay.cpp
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

#include <core/http/AsyncClient.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <server/ServerSessionProxy.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace session_proxy {
namespace overlay {

bool proxyRequest(
      int requestType,
      const boost::shared_ptr<http::Request>& pRequest,
      const r_util::SessionContext& context,
      boost::shared_ptr<http::AsyncConnection> ptrConnection,
      const http::ErrorHandler& errorHandler,
      const ClientHandler& clientHandler = ClientHandler())
{
   // not proxying the request
   return false;
}

bool proxyLocalhostRequest(
      http::Request& request,
      const std::string& port,
      const r_util::SessionContext& context,
      boost::shared_ptr<http::AsyncConnection> ptrConnection,
      const LocalhostResponseHandler& responseHandler,
      const http::ErrorHandler& errorHandler)
{
   // not proxying the request
   return false;
}

void proxyJupyterRequest(const r_util::SessionContext& context,
                         boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
                         const http::ErrorHandler& errorHandler)
{
}

void proxyVSCodeRequest(const r_util::SessionContext& context,
                        boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
                        const http::ErrorHandler& errorHandler)
{
}

Error runVerifyInstallationSession(core::system::User& user,
                                   bool* pHandled)
{
   *pHandled = false;
   return Success();
}

} // namespace overlay
} // namespace session_proxy
} // namespace server
} // namespace rstudio

