/*
 * SessionServerRpc.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#ifndef SESSION_SERVER_RPC_HPP
#define SESSION_SERVER_RPC_HPP

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/SocketRpc.hpp>
#include <core/json/JsonRpc.hpp>

namespace rstudio {
namespace session {
namespace server_rpc {

core::Error invokeServerRpc(const core::json::JsonRpcRequest& request, core::json::JsonRpcResponse* pResponse);

core::Error invokeServerRpc(const std::string& endpoint,
                            const core::json::Object& request,
                            core::json::Value* pResult);

// invokes RPC asynchronously
// callbacks are run on a background thread (not the main thread!)
void invokeServerRpcAsync(const std::string& endpoint,
                          const core::json::Object& request,
                          const core::socket_rpc::RpcResultHandler& onResult,
                          const core::socket_rpc::RpcErrorHandler& onError);

core::Error initialize();

} // namespace rpc
} // namespace session
} // namespace rstudio

#endif
