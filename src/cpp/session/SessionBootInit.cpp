/*
 * SessionBootInit.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include "SessionBootInit.hpp"

#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionHttpConnection.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace boot_init {

void handleBootInit(boost::shared_ptr<HttpConnection> ptrConnection)
{
   json::Object bootInfo ;
   bootInfo["use_retina_icons"] = false;

   // send response  (we always set kEventsPending to false so that the client
   // won't poll for events until it is ready)
   json::JsonRpcResponse jsonRpcResponse ;
   jsonRpcResponse.setField(kEventsPending, "false");
   jsonRpcResponse.setResult(bootInfo) ;
   ptrConnection->sendJsonRpcResponse(jsonRpcResponse);
}

} // namespace init
} // namespace session
} // namespace rstudio

