/*
 * SessionBreakpoints.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "environment/EnvironmentUtils.hpp"

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/utility.hpp>
#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RUtil.hpp>
#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace core;
using namespace r::sexp;
using namespace r::exec;

namespace session {
namespace modules {
namespace breakpoints {
namespace
{

void onPackageLoaded(const std::string& pkgname)
{
   // check whether this package is the one currently under development
   const projects::ProjectContext& projectContext = projects::projectContext();
   if (projectContext.config().buildType == r_util::kBuildTypePackage &&
       projectContext.packageInfo().name() == pkgname)
   {
      // if it is, emit a package loaded event to the client
      ClientEvent packageLoadedEvent(client_events::kActivePackageLoaded);
      module_context::enqueClientEvent(packageLoadedEvent);
   }
}

// Called by the client to ascertain whether the given function in the given
// file is in sync with the corresponding R object
Error getFunctionSyncState(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string functionName, fileName;
   Error error = json::readParams(request.params, &functionName, &fileName);
   if (error)
   {
       return error;
   }

   // get the source refs and code for the function
   SEXP srcRefs = NULL;
   Protect protect;
   error = r::exec::RFunction(".rs.getFunctionSourceRefs",
                              functionName,
                              fileName)
         .call(&srcRefs, &protect);
   if (error)
   {
      return error;
   }

   std::string functionCode;
   error = r::exec::RFunction(".rs.getFunctionSourceCode",
                              functionName,
                              fileName)
         .call(&functionCode);
   if (error)
   {
      return error;
   }

   // compare with the disk
   bool inSync = !environment::functionDiffersFromSource(srcRefs, functionCode);
   pResponse->setResult(json::Value(inSync));
   return Success();
}

} // anonymous namespace

json::Value debugStateAsJson()
{
   json::Object state;

   // get the debug state from R
   SEXP debugState = r::sexp::findVar(".rs.topDebugState");
   if (TYPEOF(debugState) == ENVSXP)
   {
      state["top_level_debug"] = json::Value(true);
      state["debug_step"] = json::Value(
          r::sexp::asInteger(r::sexp::findVar("currentDebugStep", debugState)));
      state["debug_file"] = json::Value(
          r::sexp::asString(r::sexp::findVar("currentDebugFile", debugState)));
      SEXP srcref = r::sexp::findVar("currentDebugSrcref", debugState);
      environment::sourceRefToJson(srcref, &state);
   }
   else
   {
      state["top_level_debug"] = json::Value(false);
   }

   return state;
}

Error initialize()
{
   // subscribe to events
   using boost::bind;
   using namespace module_context;

   events().onPackageLoaded.connect(onPackageLoaded);

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_function_sync_state", getFunctionSyncState))
      (bind(sourceModuleRFile, "SessionBreakpoints.R"));

   return initBlock.execute();
}


} // namepsace breakpoints
} // namespace modules
} // namesapce session


