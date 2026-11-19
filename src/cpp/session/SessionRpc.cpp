/*
 * SessionRpc.cpp
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

#include <exception>
#include <string>

#include <boost/make_shared.hpp>

#include "SessionRpc.hpp"
#include "SessionHttpMethods.hpp"
#include "SessionClientEventQueue.hpp"
#include "SessionAsyncRpcConnection.hpp"

#include <shared_core/Memory.hpp>
#include <shared_core/json/Json.hpp>

#include <core/json/JsonRpc.hpp>
#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <core/MainThread.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RJson.hpp>
#include <r/RJsonRpc.hpp>
#include <r/RRoutines.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace {

// a delay used when processing RPC methods (used to simulate network latency)
int s_rpcDelayMs = -1;

// leaked: read by the offline service and http listener threads (#18318)
std::set<std::string>& s_offlineableUris = core::make_leaked<std::set<std::string>>();

// json rpc methods
core::json::JsonRpcAsyncMethods* s_pJsonRpcMethods = nullptr;
   
void endHandleRpcRequestDirect(boost::shared_ptr<rpc::RpcDelivery> pDelivery,
                         boost::shared_ptr<HttpConnection> ptrConnection,
                         boost::posix_time::ptime executeStartTime,
                         const core::Error& executeError,
                         json::JsonRpcResponse* pJsonRpcResponse)
{
   // a second response would be written to a connection already answered
   if (!rpc::claimRpcDelivery(pDelivery.get()))
      return;

   // return error or result then continue waiting for requests
   if (executeError)
   {
      ptrConnection->sendJsonRpcError(executeError);
      rpc::markRpcDeliverySent(pDelivery.get());
   }
   else
   {
      // allow modules to detect changes after rpc calls
      if (!pJsonRpcResponse->suppressDetectChanges())
      {
         module_context::events().onDetectChanges(
               module_context::ChangeSourceRPC);
      }

      // are there (or will there likely be) events pending?
      // (if not then notify the client)
      bool eventsPending =
          pJsonRpcResponse->hasAfterResponse() ||
          clientEventQueue().eventAddedSince(executeStartTime);
      pJsonRpcResponse->setField(kEventsPending, eventsPending ? "true" : "false");

      // send the response
      ptrConnection->sendJsonRpcResponse(*pJsonRpcResponse);
      rpc::markRpcDeliverySent(pDelivery.get());

      // run after response if we have one (then detect changes again)
      if (pJsonRpcResponse->hasAfterResponse())
         rpc::runAfterResponse(pDelivery.get(), pJsonRpcResponse);
   }
}


void saveJsonResponse(const core::Error& error, core::json::JsonRpcResponse *pSrc,
                      core::Error *pError,      core::json::JsonRpcResponse *pDest)
{
   *pError = error;
   *pDest = *pSrc;
}

// invoke an HTTP RPC directly from R.
SEXP rs_invokeRpc(SEXP name, SEXP args)
{
   // generate RPC request from this R command
   json::JsonRpcRequest request;
   rpc::formatRpcRequest(name, args, &request);

   // check to see if the RPC exists
   auto it = s_pJsonRpcMethods->find(request.method);
   if (it == s_pJsonRpcMethods->end())
   {
      // specified method doesn't exist
      r::exec::error("Requested RPC method " + request.method + " does not exist.");
      return R_NilValue;
   }

   std::pair<bool, json::JsonRpcAsyncFunction> reg = it->second;
   json::JsonRpcAsyncFunction handlerFunction = reg.second;

   if (!reg.first)
   {
      // this indicates an async RPC, which isn't currently handled
      r::exec::error("Requested RPC method " + request.method + " is asynchronous.");
      return R_NilValue;
   }

   // invoke handler and record response
   core::json::JsonRpcResponse response;
   core::Error rpcError = Success();
   handlerFunction(request,
                   boost::bind(saveJsonResponse, _1, _2, &rpcError, &response));

   // raise an R error if the RPC fails
   if (rpcError)
   {
      r::exec::error(log::writeError(rpcError));
   }

   // emit formatted response if enabled
   if (!core::system::getenv("RSTUDIO_SESSION_RPC_DEBUG").empty())
   {
      std::cout << "<<<" << std::endl;
      response.getRawResponse().writeFormatted(std::cout);
      std::cout << std::endl;
   }

   // convert JSON response back to R
   SEXP result = R_NilValue;
   r::sexp::Protect protect;
   result = r::sexp::create(response.result(), &protect);

   // raise an R error if the RPC returns an error
   rpc::raiseJsonRpcResponseError(response);

   return result;
}

} // anonymous namespace


namespace module_context {

Error registerAsyncRpcMethod(const std::string& name,
                             const core::json::JsonRpcAsyncFunction& function)
{
   s_pJsonRpcMethods->insert(
         std::make_pair(name, std::make_pair(false, function)));
   return Success();
}

Error registerRpcMethod(const std::string& name,
                        const core::json::JsonRpcFunction& function)
{
   s_pJsonRpcMethods->insert(
         std::make_pair(name,
                        std::make_pair(true, json::adaptToAsync(function))));
   return Success();
}

void registerRpcMethod(const core::json::JsonRpcAsyncMethod& method)
{
   s_pJsonRpcMethods->insert(method);
}

} // namespace module_context

namespace rpc {

namespace {

// who claimed an RpcDelivery: the backstop, on the main thread, may release
// only a claim the main thread made
constexpr int kUnclaimed = 0;
constexpr int kClaimedOnMainThread = 1;
constexpr int kClaimedElsewhere = 2;

struct RpcHandlerInvocation
{
   const json::JsonRpcAsyncFunction* pHandler;
   const json::JsonRpcRequest* pRequest;
   const json::JsonRpcFunctionContinuation* pContinuation;
   RpcDelivery* pDelivery;

   // a C++ exception must not unwind through R_UnwindProtect(), which would
   // leave R's context stack pointing at its frame; it's rethrown after
   std::exception_ptr exception;

   // R's error message from before the handler ran. an interrupt writes no
   // message, and clears R's pending flag and RStudio's wasInterrupted (the
   // newline R prints) before it unwinds, so an unchanged message is how the
   // backstop tells one from an error
   std::string previousErrorMessage;
};

SEXP invokeRpcHandlerImpl(void* data)
{
   RpcHandlerInvocation* pInvocation = static_cast<RpcHandlerInvocation*>(data);
   try
   {
      (*pInvocation->pHandler)(*pInvocation->pRequest, *pInvocation->pContinuation);
   }
   catch (...)
   {
      pInvocation->exception = std::current_exception();
   }
   return R_NilValue;
}

// runs as an R error (or interrupt) unwinds through the handler, after which
// R continues unwinding; this runs after R has left the handler's context, so
// it can send the response
void answerInterruptedRpc(void* data, Rboolean jump)
{
   if (!jump)
      return;

   RpcHandlerInvocation* pInvocation = static_cast<RpcHandlerInvocation*>(data);
   RpcDelivery* pDelivery = pInvocation->pDelivery;

   // the unwinding carries on past invokeRpcHandler(), so held after-response
   // work is dropped, as the handler's remaining work is, and a continuation
   // the handler kept runs its own
   pDelivery->deferAfterResponse = false;
   pDelivery->afterResponse.clear();

   if (pDelivery->sent)
      return;

   // the handler's own response may have been claimed and then interrupted
   // before it was sent; a claim made on another thread is left to it
   int claimant = kClaimedOnMainThread;
   pDelivery->claimant.compare_exchange_strong(claimant, kUnclaimed);

   try
   {
      const std::string& method = pInvocation->pRequest->method;
      std::string errorMessage = r::exec::getErrorMessage();
      Error error = r::rCodeExecutionError(
               errorMessage == pInvocation->previousErrorMessage ?
                  "R code was interrupted while handling '" + method + "'" :
                  "R error while handling '" + method + "': " + errorMessage,
               ERROR_LOCATION);
      json::JsonRpcResponse response;
      (*pInvocation->pContinuation)(error, &response);
   }
   CATCH_UNEXPECTED_EXCEPTION
}

} // anonymous namespace

bool claimRpcDelivery(RpcDelivery* pDelivery)
{
   int claimant = kUnclaimed;
   return pDelivery->claimant.compare_exchange_strong(
            claimant,
            core::thread::isMainThread() ? kClaimedOnMainThread : kClaimedElsewhere);
}

void markRpcDeliverySent(RpcDelivery* pDelivery)
{
   pDelivery->sent = true;
}

namespace {

void runAfterResponseNow(const boost::function<void()>& afterResponse, bool suppressDetectChanges)
{
   if (afterResponse)
      afterResponse();

   if (!suppressDetectChanges)
      module_context::events().onDetectChanges(module_context::ChangeSourceRPC);
}

} // anonymous namespace

void runAfterResponse(RpcDelivery* pDelivery, json::JsonRpcResponse* pResponse)
{
   // only the work is held, not the response it came with
   if (pDelivery != nullptr && pDelivery->deferAfterResponse && core::thread::isMainThread())
   {
      pDelivery->afterResponse = pResponse->afterResponse();
      pDelivery->suppressDetectChanges = pResponse->suppressDetectChanges();
      return;
   }

   runAfterResponseNow(pResponse->afterResponse(), pResponse->suppressDetectChanges());
}

void invokeRpcHandler(const json::JsonRpcAsyncFunction& handler,
                      const json::JsonRpcRequest& request,
                      const json::JsonRpcFunctionContinuation& continuation,
                      const boost::shared_ptr<RpcDelivery>& pDelivery)
{
   // offline requests are handled off the main thread without R, which
   // can't be entered there
   if (!core::thread::isMainThread())
   {
      handler(request, continuation);
      return;
   }

   // R_UnwindProtect() rather than R_ToplevelExec(): it doesn't stop the
   // unwinding, so an interrupt meant for R code that's running still
   // reaches it. like any context, it restores R's handler stack when it
   // ends, so after-response work (which can register calling handlers) is
   // held until it has (see RpcDelivery)
   RpcHandlerInvocation invocation = { &handler, &request, &continuation, pDelivery.get(), nullptr, r::exec::getErrorMessage() };
   pDelivery->deferAfterResponse = true;
   r::sexp::Protect protect;
   SEXP tokenSEXP;
   protect.add(tokenSEXP = R_MakeUnwindCont());
   R_UnwindProtect(invokeRpcHandlerImpl, &invocation, answerInterruptedRpc, &invocation, tokenSEXP);
   pDelivery->deferAfterResponse = false;

   boost::function<void()> afterResponse;
   afterResponse.swap(pDelivery->afterResponse);
   if (afterResponse)
      runAfterResponseNow(afterResponse, pDelivery->suppressDetectChanges);

   if (invocation.exception)
      std::rethrow_exception(invocation.exception);
}

void formatRpcRequest(SEXP name,
                      SEXP args,
                      core::json::JsonRpcRequest* pRequest)
{
   // find name of RPC to invoke
   std::string method = r::sexp::safeAsString(name, "");

   // assemble a request
   pRequest->method = method;

   // form argument list; convert from R to JSON
   core::json::Value rpcArgs;
   Error error = r::json::jsonValueFromObject(args, &rpcArgs);
   if (!core::system::getenv("RSTUDIO_SESSION_RPC_DEBUG").empty())
      std::cout << ">>>" << std::endl;
   if (rpcArgs.getType() == json::Type::OBJECT)
   {
      // named pair parameters
      pRequest->kwparams = rpcArgs.getValue<json::Object>();
      if (!core::system::getenv("RSTUDIO_SESSION_RPC_DEBUG").empty())
         pRequest->kwparams.writeFormatted(std::cout);
   }
   else if (rpcArgs.getType() == json::Type::ARRAY)
   {
      // array parameters
      pRequest->params = rpcArgs.getValue<json::Array>();
      if (!core::system::getenv("RSTUDIO_SESSION_RPC_DEBUG").empty())
         pRequest->params.writeFormatted(std::cout);
   }
   if (!core::system::getenv("RSTUDIO_SESSION_RPC_DEBUG").empty())
      std::cout << std::endl;
}

void raiseJsonRpcResponseError(json::JsonRpcResponse& response)
{
   // raise an R error if the RPC returns an error
   if (response.error().getType() == json::Type::OBJECT)
   {
      // formulate verbose error string
      json::Object err = response.error().getObject();
      std::string message = err["message"].getString();
      if (err.find("error") != err.end())
         message += ", Error " + err["error"].getString();
      if (err.find("category") != err.end())
         message += ", Category " + err["category"].getString();
      if (err.find("code") != err.end())
         message += ", Code " + err["code"].getString();
      if (err.find("location") != err.end())
         message += " at " + err["location"].getString();

      r::exec::error(message);
   }
}

void sendJsonAsyncPendingResponse(const core::json::JsonRpcRequest &request,
                                  boost::shared_ptr<HttpConnection> ptrConnection,
                                  std::string &asyncHandle)
{
   // indirect return (asyncHandle style)
   json::JsonRpcResponse response;
   response.setAsyncHandle(asyncHandle);
   response.setField(kEventsPending, "false");
   ptrConnection->sendJsonRpcResponse(response);
}

namespace {

// pDelivery is null for a caller outside dispatch (AsyncRpcConnection)
void endHandleRpcRequestIndirectImpl(
        RpcDelivery* pDelivery,
        const std::string& asyncHandle,
        const core::Error& executeError,
        json::JsonRpcResponse* pJsonRpcResponse)
{
   // a second completion for the same handle would be dropped by the client
   if (pDelivery != nullptr && !claimRpcDelivery(pDelivery))
      return;

   // pJsonRpcResponse may be a nullptr, so handle that safely
   json::JsonRpcResponse emptyResponse;
   if (pJsonRpcResponse == nullptr)
      pJsonRpcResponse = &emptyResponse;

   if (executeError)
      pJsonRpcResponse->setError(executeError);

   if (!pJsonRpcResponse->hasField(kEventsPending))
      pJsonRpcResponse->setField(kEventsPending, "false");

   json::Object value;
   value["handle"] = asyncHandle;
   value["response"] = pJsonRpcResponse->getRawResponse();
   ClientEvent evt(client_events::kAsyncCompletion, value);
   module_context::enqueClientEvent(evt);

   if (pDelivery != nullptr)
      markRpcDeliverySent(pDelivery);

   // run after response if we have one (then detect changes again)
   if (pJsonRpcResponse->hasAfterResponse())
      runAfterResponse(pDelivery, pJsonRpcResponse);
}

void endHandleRpcRequestIndirectOnce(
        boost::shared_ptr<RpcDelivery> pDelivery,
        const std::string& asyncHandle,
        const core::Error& executeError,
        json::JsonRpcResponse* pJsonRpcResponse)
{
   endHandleRpcRequestIndirectImpl(pDelivery.get(), asyncHandle, executeError, pJsonRpcResponse);
}

} // anonymous namespace

void endHandleRpcRequestIndirect(
        const std::string& asyncHandle,
        const core::Error& executeError,
        json::JsonRpcResponse* pJsonRpcResponse)
{
   endHandleRpcRequestIndirectImpl(nullptr, asyncHandle, executeError, pJsonRpcResponse);
}

void handleRpcRequest(const core::json::JsonRpcRequest& request,
                      boost::shared_ptr<HttpConnection> ptrConnection,
                      http_methods::ConnectionType connectionType)
{
   // delay handling this RPC if requested
   if (s_rpcDelayMs > 0)
   {
      boost::this_thread::sleep_for(boost::chrono::milliseconds(s_rpcDelayMs));
   }
   
   // record the time just prior to execution of the event
   // (so we can determine if any events were added during execution)
   using namespace boost::posix_time;
   ptime executeStartTime = microsec_clock::universal_time();
   
   // execute the method
   auto it = s_pJsonRpcMethods->find(request.method);
   if (it != s_pJsonRpcMethods->end())
   {
      std::pair<bool, json::JsonRpcAsyncFunction> reg = it->second;
      json::JsonRpcAsyncFunction handlerFunction = reg.second;
      boost::shared_ptr<RpcDelivery> pDelivery = boost::make_shared<RpcDelivery>();

      // For asyncRpc the http response was already sent - just call the handler and emit the event
      if (ptrConnection->isAsyncRpc())
      {
         boost::shared_ptr<rpc::AsyncRpcConnection> asyncConn =
                 boost::static_pointer_cast<rpc::AsyncRpcConnection>(ptrConnection);
         invokeRpcHandler(handlerFunction,
                          request,
                          boost::bind(endHandleRpcRequestIndirectOnce,
                                      pDelivery,
                                      asyncConn->asyncHandle(),
                                      _1,
                                      _2),
                          pDelivery);
      }
      // Sync rpc
      else if (reg.first)
      {
         // direct return
         invokeRpcHandler(handlerFunction,
                          request,
                          boost::bind(endHandleRpcRequestDirect,
                                      pDelivery,
                                      ptrConnection,
                                      executeStartTime,
                                      _1,
                                      _2),
                          pDelivery);
      }
      // registerAsyncRpc - http connection is still open, send the async response, then emit the event
      else
      {
         std::string asyncHandle = core::system::generateUuid(true);
         sendJsonAsyncPendingResponse(request, ptrConnection, asyncHandle);

         invokeRpcHandler(handlerFunction,
                          request,
                          boost::bind(endHandleRpcRequestIndirectOnce,
                                      pDelivery,
                                      asyncHandle,
                                      _1,
                                      _2),
                          pDelivery);
      }
   }
   else
   {
      Error executeError = Error(json::errc::MethodNotFound, ERROR_LOCATION);
      executeError.addProperty("method", request.method);

      // we need to know about these because they represent unexpected
      // application states
      LOG_ERROR(executeError);

      if (ptrConnection->isAsyncRpc())
      {
         boost::shared_ptr<rpc::AsyncRpcConnection> asyncConn =
                 boost::static_pointer_cast<rpc::AsyncRpcConnection>(ptrConnection);
         endHandleRpcRequestIndirect(asyncConn->asyncHandle(), executeError, nullptr);
      }
      else
      {
         endHandleRpcRequestDirect(boost::make_shared<rpc::RpcDelivery>(),
                                   ptrConnection,
                                   executeStartTime,
                                   executeError,
                                   nullptr);
      }
   }
}

void setRpcDelay(int delayMs)
{
   s_rpcDelayMs = delayMs;
}

bool isOfflineableRequest(boost::shared_ptr<HttpConnection> ptrConnection)
{
   // Only specific requests that do not use the R runtime are offlineable (e.g. save_document)
   if (s_offlineableUris.find(ptrConnection->request().uri()) == s_offlineableUris.end())
      return false;
   return true;
}

Error initialize()
{
   // intentionally allocate methods on the heap and let them leak
   // (we had seen issues in the past where an abnormally terminated
   // R process could leak the process stuck in the destructor of
   // this map pegging the processor at 100%; avoid this by allowing
   // the OS to clean up memory itself after the process is gone)
   s_pJsonRpcMethods = new core::json::JsonRpcAsyncMethods;

   RS_REGISTER_CALL_METHOD(rs_invokeRpc);

   s_offlineableUris.insert("/rpc/save_document");
   s_offlineableUris.insert("/rpc/save_document_diff");
   s_offlineableUris.insert("/rpc/open_document");
   s_offlineableUris.insert("/rpc/set_client_state");
   s_offlineableUris.insert("/rpc/list_files");
   s_offlineableUris.insert("/rpc/modify_document_properties");
   s_offlineableUris.insert("/rpc/check_for_external_edit");
   s_offlineableUris.insert("/rpc/start_terminal");
   s_offlineableUris.insert("/rpc/process_start");
   s_offlineableUris.insert("/rpc/process_notify_start");
   s_offlineableUris.insert("/rpc/process_notify_visible");
   s_offlineableUris.insert("/rpc/process_get_buffer_chunk");
   s_offlineableUris.insert("/rpc/process_erase_buffer");
   s_offlineableUris.insert("/rpc/process_set_size");
   s_offlineableUris.insert("/rpc/get_source_template");
   s_offlineableUris.insert("/rpc/new_document");

   return Success();
}

} // namespace rpc
} // namespace session
} // namespace rstudio

