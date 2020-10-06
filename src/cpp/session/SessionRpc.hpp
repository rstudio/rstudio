/*
 * SessionRpc.hpp
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

void setRpcDelay(int delayMs);

core::Error initialize();

} // namespace rpc
} // namespace session
} // namespace rstudio

#endif
