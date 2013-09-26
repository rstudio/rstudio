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
#include <r/session/RClientState.hpp>
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
   bool contains(std::string filename, int line)
   {
      // Extract from the expression its location in the source file and see if
      // the line number given is in range.
      SEXP srcref = r::sexp::getAttrib(function, "srcref");
      if (srcref == NULL || TYPEOF(srcref) == NILSXP)
         return false;

      if (!(line >= INTEGER(srcref)[0] &&
            line <= INTEGER(srcref)[2]))
         return false;

      // Extract from the expression the source file in which it resides and
      // see if it matches the source file given.
      SEXP srcfile = r::sexp::getAttrib(srcref, "srcfile");
      if (srcfile == NULL || TYPEOF(srcfile) == NILSXP)
         return false;

      SEXP file = r::sexp::findVar("filename", srcfile);
      if (file == NULL || TYPEOF(file) == NILSXP)
         return false;

      std::string srcfilename;
      Error error = r::sexp::extract(file, &srcfilename);
      if (error)
         return false;

      return module_context::resolveAliasedPath(srcfilename) ==
             module_context::resolveAliasedPath(filename);
   }
};

// A list of the Shiny functions we know about (see notes in
// rs_registerExprFunction for an explanation of how this memory is managed)
std::vector<ShinyFunction*> s_wpShinyFunctions;

class Breakpoint
{
public:
   int type;
   int lineNumber;
   int id;
   std::string path;
   Breakpoint(int typeIn, int lineNumberIn, int idIn, std::string pathIn):
      type(typeIn),
      lineNumber(lineNumberIn),
      id(idIn),
      path(pathIn)
   {
      std::cerr << "recorded a breakpoint, id " << idIn << std::endl;
   }
};

// A list of the breakpoints we know about. Note that this is a slave list;
// the client maintains the master copy and is responsible for synchronizing
// with this list.
std::vector<boost::shared_ptr<Breakpoint> > s_breakpoints;
bool s_breakpointsInSync = false;

ShinyFunction* findShinyFunction(std::string filename, int line)
{
   BOOST_FOREACH(ShinyFunction* psf, s_wpShinyFunctions)
   {
      if (psf->contains(filename, line))
         return psf;
   }

   // Didn't find a match
   return NULL;
}

boost::shared_ptr<Breakpoint> breakpointFromJson(json::Object& obj)
{
   return boost::make_shared<Breakpoint>(
            Breakpoint(obj["type"].get_int(),
                       obj["line_number"].get_int(),
                       obj["id"].get_int(),
                       obj["path"].get_str()));

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

   // check whether the function belongs to an expression in a running
   // Shiny application
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

Error setBreakpointsDirty(const json::JsonRpcRequest& ,
                          json::JsonRpcResponse* )
{
   std::cerr << "marking breakpoint list dirty" << std::endl;
   s_breakpointsInSync = false;
   return Success();
}

void syncClientBreakpoints()
{
   try
   {
      json::Value breakpointStateValue =
         r::session::clientState().getProjectPersistent("debug-breakpoints",
                                                        "debugBreakpointsState");
      json::Object breakpointState = breakpointStateValue.get_obj();
      json::Array breakpointArray = breakpointState["breakpoints"].get_array();

      s_breakpoints.clear();
      BOOST_FOREACH(json::Value bp, breakpointArray)
      {
         s_breakpoints.push_back(breakpointFromJson(bp.get_obj()));
      }
   }
   catch (...)
   {
      // OK if we fail to get the breakpoints here--the client may have not set
      // any yet
   }
   s_breakpointsInSync = true;
}

Error initBreakpoints()
{
   syncClientBreakpoints();
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

void rs_registerExprFunction(SEXP params)
{
   SEXP expr = r::sexp::findVar("expr", params);
   SEXP fun = r::sexp::findVar("fun", params);

   // The memory allocated here is attached to the SEXP "fun" as an attribute
   // of type EXTPTRSXP. When the function is cleaned up by the garbage
   // collector, R calls the finalizer, wherein the memory is freed.
   ShinyFunction* psf = new ShinyFunction(expr, fun);
   s_wpShinyFunctions.push_back(psf);
   std::cerr << "registered Shiny function " << psf->id << std::endl;

   // Look over the list of breakpoints we know about and see if any of them
   // are unbound breakpoints in the region of the file just identified.
   if (!s_breakpointsInSync)
      syncClientBreakpoints();

   std::vector<int> lines;
   BOOST_FOREACH(boost::shared_ptr<Breakpoint> pbp, s_breakpoints)
   {
      if (psf->contains(pbp->path, pbp->lineNumber))
      {
         lines.push_back(pbp->lineNumber);
         std::cerr << "   contains breakpoint on line " << pbp->lineNumber << std::endl;
      }
   }

   // If we found breakpoint lines in this Shiny function, set breakpoints
   // on it.
   if (lines.size() > 0)
   {
      r::exec::RFunction(".rs.setShinyBreakpoints",
                         std::string("fun"),
                         params,
                         lines).call();
   }

   SEXP extptr = r::sexp::makeExternalPtr(psf, unregisterShinyFunction);
   r::sexp::setAttrib(fun, "_rs_shinyDebugInfo", extptr);
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
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_function_state", getFunctionState))
      (bind(registerRpcMethod, "set_function_breakpoints", setBreakpoints))
      (bind(registerRpcMethod, "set_breakpoints_dirty", setBreakpointsDirty))
      (bind(sourceModuleRFile, "SessionBreakpoints.R"))
      (bind(initBreakpoints));

   return initBlock.execute();
}


} // namepsace breakpoints
} // namespace modules
} // namesapce session


