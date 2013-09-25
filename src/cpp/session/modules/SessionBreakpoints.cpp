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
#include <r/Rinternals.h>

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

int s_maxShinyFunctionId = 0;

class ShinyFunction
{
public:
   SEXP expr;
   SEXP function;
   int id;
   ShinyFunction(SEXP exprIn, SEXP functionIn):
      expr(exprIn),
      function(functionIn),
      id(s_maxShinyFunctionId++)
   {}
};

std::vector<ShinyFunction*> s_wpShinyFunctions;

ShinyFunction* findShinyFunction(std::string filename, int line)
{
   BOOST_FOREACH(ShinyFunction* psf, s_wpShinyFunctions)
   {
      // Extract from the expression its location in the source file and see if
      // the line number given is in range.
      SEXP srcref = r::sexp::getAttrib(psf->expr, "srcref");
      if (srcref == NULL || TYPEOF(srcref) == NILSXP)
         continue;

      if (!(line >= INTEGER(srcref)[0] &&
            line <= INTEGER(srcref)[2]))
         continue;

      // Extract from the expression the source file in which it resides and
      // see if it matches the source file given.
      SEXP srcfile = r::sexp::getAttrib(srcref, "srcfile");
      if (srcfile == NULL || TYPEOF(srcfile) == NILSXP)
         continue;

      SEXP file = r::sexp::findVar("filename", srcfile);
      if (file == NULL || TYPEOF(file) == NILSXP)
         continue;

      std::string srcfilename;
      Error error = r::sexp::extract(file, &srcfilename);
      if (error)
         continue;

      // Found a match
      if (srcfilename == filename)
         return psf;
   }

   // Didn't find a match
   return NULL;
}

// Runs a series of pre-flight checks to determine whether we can set a
// breakpoint at the given location, and, if we can, what kind of breakpoint
// we should set.
Error getFunctionState(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   json::Object response;
   std::string functionName, fileName, packageName;
   int lineNumber = 0, shinyFunctionId = 0;
   bool inSync = false, isShinyFunction = false;
   Error error = json::readParams(
            request.params, &functionName, &fileName, &lineNumber);
   if (error)
   {
       return error;
   }

   ShinyFunction* psf = findShinyFunction(fileName, lineNumber);
   if (psf)
   {
      isShinyFunction = true;
      shinyFunctionId = psf->id;
   }
   else
   {
      // check whether the function is in a package
      packageName = module_context::packageNameForSourceFile(
                       module_context::resolveAliasedPath(fileName));

      // get the source refs and code for the function
      SEXP srcRefs = NULL;
      Protect protect;
      std::string functionCode;
      error = r::exec::RFunction(".rs.getFunctionSourceRefs",
                                 functionName,
                                 fileName,
                                 packageName)
            .call(&srcRefs, &protect);
      if (!error)
      {
         error = r::exec::RFunction(".rs.getFunctionSourceCode",
                                    functionName,
                                    fileName,
                                    packageName)
               .call(&functionCode);
      }
      // compare with the disk if we were able to get the source code;
      // otherwise, assume it's out of sync
      if (!error)
         inSync = !environment::functionDiffersFromSource(srcRefs, functionCode);
   }

   response["sync_state"] = inSync;
   response["package_name"] = packageName;
   response["is_package_function"] = packageName.length() > 0;
   response["is_shiny_function"] = isShinyFunction;
   response["shiny_function_id"] = shinyFunctionId;
   pResponse->setResult(response);

   return Success();
}

// Sets a breakpoint on a single copy of a function. Invoked several times to
// look for function copies in alternate environemnts. Returns true if a
// breakpoint was set; false otherwise.
bool setBreakpoint(const std::string& functionName,
                   const std::string& fileName,
                   const std::string& packageName,
                   const json::Array& steps)
{
   SEXP env = NULL;
   Protect protect;
   Error error = r::exec::RFunction(".rs.getEnvironmentOfFunction",
                                    functionName,
                                    fileName,
                                    packageName)
                                    .call(&env, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // if we found a function in the requested environment, set the breakpoint
   if (TYPEOF(env) == ENVSXP)
   {
      error = r::exec::RFunction(".rs.setFunctionBreakpoints",
                                 functionName,
                                 env,
                                 steps).call();
      if (error)
      {
         LOG_ERROR(error);
         return false;
      }
      // successfully set a breakpoint
      return true;
   }

   // did not find the function in the environment
   return false;
}

// Set a breakpoint on a function, potentially on multiple copies:
// 1. The private copy of the function inside the package under development
//    (even if from another package; it may be an imported copy)
// 2. The private copy of the function inside its own package (if from a
//    package)
// 3. The copy of the function on the global search path
//
// Note that this is not guaranteed to find ALL copies of the function in ANY
// environment--at most, three breakpoints are set.
Error setBreakpoints(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string functionName, fileName, packageName;
   json::Array steps;
   bool set = false;
   Error error = json::readParams(request.params,
            &functionName,
            &fileName,
            &packageName,
            &steps);
   if (error)
      return error;

   // If we're in package development mode, try to set a breakpoint in the
   // package's namespace first.
   const projects::ProjectContext& projectContext = projects::projectContext();
   if (projectContext.config().buildType == r_util::kBuildTypePackage)
   {
      set |= setBreakpoint(
               functionName, fileName,
               projectContext.packageInfo().name(), steps);
   }

   // If a package name was specified, try to set a breakpoint in that package's
   // namespace, too.
   if (packageName.length() > 0)
   {
      set |= setBreakpoint(functionName, fileName, packageName, steps);
   }

   // Always search the global namespace.
   set |= setBreakpoint(functionName, fileName, "", steps);

   // Couldn't find a function to set a breakpoint on--maybe a bad parameter?
   if (!set)
   {
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);
   }

   return Success();
}

void unregisterShinyFunction(SEXP extptr)
{
   ShinyFunction* psf =
         static_cast<ShinyFunction*>(r::sexp::getExternalPtrAddr(extptr));
   if (!psf)
      return;
   std::cerr << "finalizer called Shiny function " << psf->id << std::endl;
   std::vector<ShinyFunction*>::iterator exprFunPos =
         std::find(s_wpShinyFunctions.begin(), s_wpShinyFunctions.end(), psf);
   if (exprFunPos != s_wpShinyFunctions.end())
   {
      s_wpShinyFunctions.erase(exprFunPos);
   }
   delete psf;
   r::sexp::clearExternalPtr(extptr);
}

} // anonymous namespace

SEXP rs_registerExprFunction(SEXP expr, SEXP fun)
{
   // The memory allocated here is attached to the SEXP "fun" as an attribute
   // of type EXTPTRSXP. When the function is cleaned up by the garbage
   // collector, R calls the finalizer, wherein the memory is freed.
   ShinyFunction* psf = new ShinyFunction(expr, fun);
   s_wpShinyFunctions.push_back(psf);
   std::cerr << "registered Shiny function " << psf->id << std::endl;
   return r::sexp::makeExternalPtr(psf, unregisterShinyFunction);
}

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

bool haveSrcrefAttribute()
{
   // check whether this is R 2.14 or greater
   bool haveSrcref = false;
   Error error = r::exec::evaluateString("getRversion() >= '2.14.0'", &haveSrcref);
   if (error)
      LOG_ERROR(error);
   return haveSrcref;
}

Error initialize()
{
   // subscribe to events
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_function_state", getFunctionState))
      (bind(registerRpcMethod, "set_function_breakpoints", setBreakpoints))
      (bind(sourceModuleRFile, "SessionBreakpoints.R"));

   return initBlock.execute();
}


} // namepsace breakpoints
} // namespace modules
} // namesapce session


