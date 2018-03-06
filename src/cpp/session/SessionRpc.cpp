/*
 * SessionRpc.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <string>

#include "SessionRpc.hpp"
#include "SessionHttpMethods.hpp"
#include "SessionClientEventQueue.hpp"

#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RJson.hpp>
#include <r/RJsonRpc.hpp>
#include <r/RRoutines.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace {

// json rpc methods
core::json::JsonRpcAsyncMethods* s_pJsonRpcMethods = NULL;
   
void endHandleRpcRequestDirect(boost::shared_ptr<HttpConnection> ptrConnection,
                         boost::posix_time::ptime executeStartTime,
                         const core::Error& executeError,
                         json::JsonRpcResponse* pJsonRpcResponse)
{
   // return error or result then continue waiting for requests
   if (executeError)
   {
      ptrConnection->sendJsonRpcError(executeError);
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
      if ( !clientEventQueue().eventAddedSince(executeStartTime) &&
           !pJsonRpcResponse->hasAfterResponse() )
      {
         pJsonRpcResponse->setField(kEventsPending, "false");
      }

      // send the response
      ptrConnection->sendJsonRpcResponse(*pJsonRpcResponse);

      // run after response if we have one (then detect changes again)
      if (pJsonRpcResponse->hasAfterResponse())
      {
         pJsonRpcResponse->runAfterResponse();
         if (!pJsonRpcResponse->suppressDetectChanges())
         {
            module_context::events().onDetectChanges(
                  module_context::ChangeSourceRPC);
         }
      }
   }
}

void endHandleRpcRequestIndirect(
      const std::string& asyncHandle,
      const core::Error& executeError,
      json::JsonRpcResponse* pJsonRpcResponse)
{
   json::JsonRpcResponse temp;
   json::JsonRpcResponse& jsonRpcResponse =
                                 pJsonRpcResponse ? *pJsonRpcResponse : temp;

   BOOST_ASSERT(!jsonRpcResponse.hasAfterResponse());
   if (executeError)
   {
      jsonRpcResponse.setError(executeError);
   }
   json::Object value;
   value["handle"] = asyncHandle;
   value["response"] = jsonRpcResponse.getRawResponse();
   ClientEvent evt(client_events::kAsyncCompletion, value);
   module_context::enqueClientEvent(evt);
}

void saveJsonResponse(const core::Error&, core::json::JsonRpcResponse *pSrc,
                      core::json::JsonRpcResponse *pDest)
{
   *pDest = *pSrc;
}

// invoke an HTTP RPC directly from R.
SEXP rs_invokeRpc(SEXP name, SEXP args)
{
   // find name of RPC to invoke
   std::string method = r::sexp::safeAsString(name, "");
   auto it = s_pJsonRpcMethods->find(method);
   if (it == s_pJsonRpcMethods->end())
   {
      // specified method doesn't exist
      return R_NilValue;    
   }

   std::pair<bool, json::JsonRpcAsyncFunction> reg = it->second;
   json::JsonRpcAsyncFunction handlerFunction = reg.second;

   if (!reg.first)
   {
      // this indicates an async RPC, which isn't currently handled
      return R_NilValue;
   }

   // assemble a request
   core::json::JsonRpcRequest request;
   request.method = method;

   // form argument list; convert from R to JSON
   core::json::Value rpcArgs;
   Error error = r::json::jsonValueFromObject(args, &rpcArgs);
   if (rpcArgs.type() == json::ObjectType)
   {
      // named pair parameters
      request.kwparams = rpcArgs.get_obj();
   }
   else if (rpcArgs.type() == json::ArrayType)
   {
      // object parameters
      request.params = rpcArgs.get_array();
   }

   // invoke handler and record response
   core::json::JsonRpcResponse response;
   handlerFunction(request,
                   boost::bind(saveJsonResponse, _1, _2, &response));

   // convert JSON response back to R
   SEXP result = R_NilValue;
   r::sexp::Protect protect;
   result = r::sexp::create(response.result(), &protect);
   
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

void handleRpcRequest(const core::json::JsonRpcRequest& request,
                      boost::shared_ptr<HttpConnection> ptrConnection,
                      http_methods::ConnectionType connectionType)
{
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

      if (reg.first)
      {
         // direct return
         handlerFunction(request,
                         boost::bind(endHandleRpcRequestDirect,
                                     ptrConnection,
                                     executeStartTime,
                                     _1,
                                     _2));
      }
      else
      {
         // indirect return (asyncHandle style)
         std::string handle = core::system::generateUuid(true);
         json::JsonRpcResponse response;
         response.setAsyncHandle(handle);
         response.setField(kEventsPending, "false");
         ptrConnection->sendJsonRpcResponse(response);

         handlerFunction(request,
                         boost::bind(endHandleRpcRequestIndirect,
                                     handle,
                                     _1,
                                     _2));
      }
   }
   else
   {
      Error executeError = Error(json::errc::MethodNotFound, ERROR_LOCATION);
      executeError.addProperty("method", request.method);

      // we need to know about these because they represent unexpected
      // application states
      LOG_ERROR(executeError);

      endHandleRpcRequestDirect(ptrConnection, executeStartTime, executeError, NULL);
   }
}

Error initialize()
{
   // intentionally allocate methods on the heap and let them leak
   // (we had seen issues in the past where an abnormally terminated
   // R process could leak the process stuck in the destructor of
   // this map pegging the processor at 100%; avoid this by allowing
   // the OS to clean up memory itself after the process is gone)
   s_pJsonRpcMethods = new core::json::JsonRpcAsyncMethods;

   r::routines::registerCallMethod(
            "rs_invokeRpc",
            (DL_FUNC) rs_invokeRpc,
            2);

   return Success();
}

} // namespace rpc
} // namespace session
} // namespace rstudio

