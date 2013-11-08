/*
 * SessionAbout.cpp
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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

#include "SessionAbout.hpp"

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

#include <string>

#include "session-config.h"

using namespace core;

namespace session {
namespace modules {
namespace about {
namespace {

Error productInfo(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   json::Object result;
   result["version"] = RSTUDIO_VERSION;
   result["notice"] = module_context::resourceFileAsString("NOTICE");
   pResponse->setResult(result);
   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_product_info", productInfo))
   ;
   return initBlock.execute();
}

} // namespace about
} // namespace modules
} // namespace session
