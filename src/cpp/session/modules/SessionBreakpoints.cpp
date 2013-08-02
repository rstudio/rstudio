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

// Called by the client to ascertain whether the given function in the given
// file is in sync with the corresponding R object
Error getFunctionState(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   json::Object response;
   std::string functionName, fileName;
   Error error = json::readParams(request.params, &functionName, &fileName);
   if (error)
   {
       return error;
   }

   // check whether the function is in a package
   std::string packageName(module_context::packageNameForSourceFile(
               module_context::resolveAliasedPath(fileName)));
   response["is_package_function"] = packageName.length() > 0;
   response["package_name"] = packageName;

   // get the source refs and code for the function
   SEXP srcRefs = NULL;
   Protect protect;
   error = r::exec::RFunction(".rs.getFunctionSourceRefs",
                              functionName,
                              fileName,
                              packageName)
         .call(&srcRefs, &protect);
   if (error)
   {
      return error;
   }

   std::string functionCode;
   error = r::exec::RFunction(".rs.getFunctionSourceCode",
                              functionName,
                              fileName,
                              packageName)
         .call(&functionCode);
   if (error)
   {
      return error;
   }

   // compare with the disk
   bool inSync = !environment::functionDiffersFromSource(srcRefs, functionCode);
   response["sync_state"] = inSync;
   pResponse->setResult(response);

   return Success();
}

} // anonymous namespace

json::Value debugStateAsJson()
{
   json::Object state;

   // look for the debug state environment created by debugSource; if
   // it exists, emit the pieces the client cares about.
   SEXP debugState = r::sexp::findVar(".rs.topDebugState");
   if (TYPEOF(debugState) == ENVSXP)
   {
      state["top_level_debug"] = true;
      state["debug_step"] =
          r::sexp::asInteger(r::sexp::findVar("currentDebugStep", debugState));
      state["debug_file"] =
          r::sexp::asString(r::sexp::findVar("currentDebugFile", debugState));
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

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_function_state", getFunctionState))
      (bind(sourceModuleRFile, "SessionBreakpoints.R"));

   return initBlock.execute();
}


} // namepsace breakpoints
} // namespace modules
} // namesapce session


