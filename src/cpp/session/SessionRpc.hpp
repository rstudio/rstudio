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

#include <atomic>

#include <boost/shared_ptr.hpp>

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

// a request's response, shared by its continuation (which an async handler may
// call after returning, even from another thread) and invokeRpcHandler()'s
// backstop. a continuation claims the response before sending it, and drops
// it if it was claimed already; once sent (or, for an async request,
// enqueued), it's marked sent
struct RpcDelivery
{
   std::atomic<int> claimant { 0 };
   std::atomic<bool> sent { false };

   // while invokeRpcHandler() runs the handler, a response's after-response
   // work is held here, and run once the handler returns: R discards the
   // calling handlers R code registers in a context when the context ends,
   // and that work registers some (a notebook's first chunk). main thread only
   std::atomic<bool> deferAfterResponse { false };
   boost::function<void()> afterResponse;
   bool suppressDetectChanges = false;
};

// returns true when the caller now owns the response and should send it
bool claimRpcDelivery(RpcDelivery* pDelivery);
void markRpcDeliverySent(RpcDelivery* pDelivery);

// runs an RPC handler so that an R error (or interrupt) unwinding through it
// -- raised by R code it ran unprotected -- doesn't leave the request
// unanswered: unless a response was sent, the continuation sends the error,
// and the unwinding then continues as it would have. it still skips the
// handler's own C++ destructors, so this is a backstop, not a substitute for
// protected R code. off the main thread (offline requests), it just runs the
// handler
void invokeRpcHandler(const core::json::JsonRpcAsyncFunction& handler,
                      const core::json::JsonRpcRequest& request,
                      const core::json::JsonRpcFunctionContinuation& continuation,
                      const boost::shared_ptr<RpcDelivery>& pDelivery);

// runs a response's after-response work, then lets modules detect changes;
// with pDelivery, it's held instead while the handler runs (see RpcDelivery)
void runAfterResponse(RpcDelivery* pDelivery, core::json::JsonRpcResponse* pResponse);

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
