/*
 * SessionHttpMethods.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#ifndef SESSION_HTTP_METHODS_HPP
#define SESSION_HTTP_METHODS_HPP

#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/WaitUtils.hpp>
#include <session/SessionHttpConnection.hpp>
#include <session/SessionModuleContext.hpp>

namespace rstudio {
namespace session {

class ClientEvent;

namespace http_methods {

enum ConnectionType
{
   ForegroundConnection,
   BackgroundConnection
};

bool waitForMethod(const std::string& method,
                   const boost::function<void()>& initFunction,
                   const boost::function<bool()>& allowSuspend,
                   core::json::JsonRpcRequest* pRequest);
bool waitForMethod(const std::string& method,
                   const ClientEvent& initEvent,
                   const boost::function<bool()>& allowSuspend,
                   core::json::JsonRpcRequest* pRequest);
void waitForMethodInitFunction(const ClientEvent& initEvent);

void handleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                      ConnectionType connectionType);
core::WaitResult startHttpConnectionListenerWithTimeout();
void registerGwtHandlers();
std::string clientVersion();
std::string nextSessionUrl();

} // namespace http_methods
} // namespace session
} // namespace rstudio

#endif
