/*
 * SessionHttpMethods.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_HTTP_METHODS_HPP
#define SESSION_HTTP_METHODS_HPP

#include <shared_core/json/Json.hpp>
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

bool isJsonRpcRequest(boost::shared_ptr<HttpConnection> ptrConnection);

bool isAsyncJsonRpcRequest(boost::shared_ptr<HttpConnection> ptrConnection);

void handleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                      ConnectionType connectionType);
boost::shared_ptr<HttpConnection> handleAsyncRpc(boost::shared_ptr<HttpConnection> ptrConnection);
core::WaitResult startHttpConnectionListenerWithTimeout();
void registerGwtHandlers();
std::string clientVersion();
std::string nextSessionUrl();
bool protocolDebugEnabled();
core::Error initialize();
bool verifyRequestSignature(const core::http::Request& request);

} // namespace http_methods
} // namespace session
} // namespace rstudio

#endif
