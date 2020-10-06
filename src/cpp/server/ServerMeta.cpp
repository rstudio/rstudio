/*
 * ServerMeta.cpp
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

#include "ServerMeta.hpp"

#include <core/Log.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/JsonRpc.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace meta {

namespace {

void handleInitMessagesRequest(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(json::Value());
}

} // anonymous namespace

void handleMetaRequest(const std::string& username,
                       const core::http::Request& request,
                       core::http::Response* pResponse)
{
   // parse request
   json::JsonRpcRequest jsonRpcRequest;
   Error parseError = parseJsonRpcRequest(request.body(), &jsonRpcRequest);
   if (parseError)
   {
      LOG_ERROR(parseError);
      json::setJsonRpcError(parseError, pResponse);
      return;
   }

   // check for supported methods
   if (jsonRpcRequest.method == "get_init_messages")
   {
      json::JsonRpcResponse jsonResponse;
      handleInitMessagesRequest(jsonRpcRequest, &jsonResponse);
      json::setJsonRpcResponse(jsonResponse, pResponse);
   }
   else
   {
      Error methodError = Error(json::errc::MethodNotFound, ERROR_LOCATION);
      methodError.addProperty("method", jsonRpcRequest.method);
      LOG_ERROR(methodError);
      json::setJsonRpcError(methodError, pResponse);
   }
}

} // namespace meta
} // namespace server
} // namespace rstudio
