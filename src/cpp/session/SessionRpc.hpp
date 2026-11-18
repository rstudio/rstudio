/*
 * SessionRpc.hpp
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

#ifndef SESSION_RPC_HPP
#define SESSION_RPC_HPP

#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <session/SessionHttpConnection.hpp>

#include "SessionHttpMethods.hpp"

namespace rstudio {
namespace session {
namespace rpc {

void formatRpcRequest(SEXP name,
                      SEXP args,
                      core::json::JsonRpcRequest* pRequest);

void raiseJsonRpcResponseError(core::json::JsonRpcResponse& response);

void handleRpcRequest(const core::json::JsonRpcRequest& request,
                      boost::shared_ptr<HttpConnection> ptrConnection,
                      http_methods::ConnectionType connectionType);

// runs an RPC handler so that an R error escaping it -- raised by R code it
// ran unprotected -- ends here rather than longjmping past the request, which
// would leave the request unanswered; unless the handler had responded, it is
// then completed with the error. the jump still skips the handler's own C++
// destructors, so this is a backstop, not a substitute for protected R code
void invokeRpcHandler(const core::json::JsonRpcAsyncFunction& handler,
                      const core::json::JsonRpcRequest& request,
                      const core::json::JsonRpcFunctionContinuation& continuation);

void setRpcDelay(int delayMs);

core::Error initialize();

bool isOfflineableRequest(boost::shared_ptr<HttpConnection> ptrConnection);

void sendJsonAsyncPendingResponse(const core::json::JsonRpcRequest &request,
                                  boost::shared_ptr<HttpConnection> ptrConnection,
                                  std::string &asyncHandle);

void endHandleRpcRequestIndirect(
        const std::string& asyncHandle,
        const core::Error& executeError,
        core::json::JsonRpcResponse* pJsonRpcResponse);

} // namespace rpc
} // namespace session
} // namespace rstudio

#endif
