/*
 * SessionAbout.cpp
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

#include "SessionAbout.hpp"

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

#include "session-config.h"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace about {
namespace {

Error productInfo(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   json::Object result;
   result["version"] = RSTUDIO_VERSION;
   result["version_major"] = RSTUDIO_VERSION_MAJOR;
   result["version_minor"] = RSTUDIO_VERSION_MINOR;
   result["version_patch"] = RSTUDIO_VERSION_PATCH;
   result["version_suffix"] = RSTUDIO_VERSION_SUFFIX;
   result["commit"] = RSTUDIO_GIT_COMMIT;
   result["build"] = RSTUDIO_BUILD_ID;
   result["release_name"] = RSTUDIO_RELEASE_NAME;
   result["build_type"] = RSTUDIO_BUILD_TYPE;
   result["date"] = RSTUDIO_BUILD_DATE;
   result["copyright_year"] = RSTUDIO_COPYRIGHT_YEAR;
   result["os"] = RSTUDIO_PACKAGE_OS;
   pResponse->setResult(result);
   return Success();
}

Error productNotice(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   json::Object result;
   result["notice"] = module_context::resourceFileAsString("NOTICE");
   pResponse->setResult(result);
   return Success();
}

Error rVersion(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   json::Object result;
   result["version"] = module_context::rVersion();
   result["label"] = module_context::rVersionLabel();
   result["r_home"] = module_context::rHomeDir();
   result["module"] = module_context::rVersionModule();

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
      (bind(registerRpcMethod, "get_product_notice", productNotice))
      (bind(registerRpcMethod, "get_rversion_info", rVersion))
   ;
   return initBlock.execute();
}

} // namespace about
} // namespace modules
} // namespace session
} // namespace rstudio
