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
   ShinyFunction(SEXP expr, const std::string& name, SEXP where):
      id_(s_maxShinyFunctionId++),
      firstLine_(0),
      lastLine_(0),
      name_(name),
      where_(where)
   {
      // If the srcref attribute is present on the expression, use it to
      // compute the first and last lines of the function in the original
      // source file.
      SEXP srcref = r::sexp::getAttrib(expr, "srcref");
      if (srcref != NULL && TYPEOF(srcref) != NILSXP)
      {
         SEXP firstRef = VECTOR_ELT(srcref, 0);
         firstLine_ = INTEGER(firstRef)[0];
         SEXP lastRef = VECTOR_ELT(srcref, r::sexp::length(srcref) - 1);
         lastLine_ = INTEGER(lastRef)[2];
      }
      // If the srcfile attribute is present, extract it
      SEXP srcfile = r::sexp::getAttrib(expr, "srcfile");
      if (srcfile != NULL && TYPEOF(srcfile) != NILSXP)
      {
         SEXP file = r::sexp::findVar("filename", srcfile);
         r::sexp::extract(file, &srcfilename_);
      }
   }

   bool contains(std::string filename, int line)
   {
      if (!(line >= firstLine_ && line <= lastLine_))
         return false;

      return module_context::resolveAliasedPath(srcfilename_) ==
             module_context::resolveAliasedPath(filename);
   }

   int getId()
   {
      return id_;
   }

   std::string getName()
   {
      return name_;
   }

   SEXP getWhere()
   {
      return where_;
   }

private:
   int id_;
   int firstLine_;
   int lastLine_;
   std::string name_;
   std::string srcfilename_;
   SEXP where_;
};

// A list of the Shiny functions we know about (see notes in
// rs_registerShinyFunction for an explanation of how this memory is managed)
std::vector<boost::shared_ptr<ShinyFunction> > s_shinyFunctions;

class Breakpoint
{
public:
   int type;
   int lineNumber;
   int id;
   std::string path;
   bool bound;
   Breakpoint(int typeIn, int lineNumberIn, int idIn, std::string pathIn):
      type(typeIn),
      lineNumber(lineNumberIn),
      id(idIn),
      path(pathIn),
      bound(false)
   {}
};

// A list of the breakpoints we know about. Note that this is a slave list;
// the client maintains the master copy and is responsible for synchronizing
// with this list. This list is maintained so we can inject breakpoints
// synchronously when Shiny creates an anonymous function object.
std::vector<boost::shared_ptr<Breakpoint> > s_breakpoints;
bool s_breakpointsInSync = false;

ShinyFunction* findShinyFunction(std::string filename, int line)
{
   BOOST_FOREACH(boost::shared_ptr<ShinyFunction> psf, s_shinyFunctions)
   {
      if (psf->contains(filename, line))
         return psf.get();
   }

   // Didn't find a match
   return NULL;
}

boost::shared_ptr<Breakpoint> breakpointFromJson(json::Object& obj)
{
   return boost::make_shared<Breakpoint>(obj["type"].get_int(),
                                         obj["line_number"].get_int(),
                                         obj["id"].get_int(),
                                         obj["path"].get_str());

}

std::vector<int> getShinyBreakpointLines(ShinyFunction& sf)
{
   std::vector<int> lines;
   BOOST_FOREACH(boost::shared_ptr<Breakpoint> pbp, s_breakpoints)
   {
      if (sf.contains(pbp->path, pbp->lineNumber))
      {
         lines.push_back(pbp->lineNumber);
         pbp->bound = true;
      }
   }
   return lines;
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
      shinyFunctionId = psf->getId();
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
   s_breakpointsInSync = false;
   return Success();
}

Error setShinyBreakpoint(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse*)
{
   std::string fileName;
   int line = 0, id = 0;
   bool set = false, mutated = false;

   Error error = json::readParams(request.params, &fileName, &line, &id, &set);
   if (error)
      return error;

   // Start by figuring out which Shiny function this breakpoint exists in.
   // If it doesn't exist anywhere, fail silently.
   ShinyFunction* psf = findShinyFunction(fileName, line);
   if (psf == NULL)
      return Success();

   // Now look for the breakpoint in our list. (It may not exist if we haven't
   // sync'ed the breakpoint list from the client).
   std::vector<boost::shared_ptr<Breakpoint> >::iterator psbi;
   for (psbi = s_breakpoints.begin();
        psbi != s_breakpoints.end();
        psbi++)
   {
      if ((*psbi)->id == id)
         break;
   }

   // Creating a new breakpoint
   if (set && psbi == s_breakpoints.end())
   {
      s_breakpoints.push_back(
               boost::make_shared<Breakpoint>(0, line, id, fileName));
      mutated = true;
   }
   // Removing a breakpoint
   else if (!set && psbi != s_breakpoints.end())
   {
      s_breakpoints.erase(psbi);
      mutated = true;
   }

   // If we mutated the breakpoint list, recompute the list of breakpoints
   // that exist in the Shiny function, and set them.
   if (mutated)
   {
      std::vector<int> lines = getShinyBreakpointLines(*psf);
      r::exec::RFunction(".rs.setShinyBreakpoints",
                         psf->getName(),
                         psf->getWhere(),
                         lines).call();

   }
   return Success();
}

// Called on init and when we need an up-to-date list of breakpoints from the
// client--just slurps information from the client's persisted store of
// breakpoints.
void syncClientBreakpoints()
{
   try
   {
      json::Value breakpointStateValue =
         r::session::clientState().getProjectPersistent("debug-breakpoints",
                                                        "debugBreakpointsState");
      if (!breakpointStateValue.is_null())
      {
         json::Object breakpointState = breakpointStateValue.get_obj();
         json::Array breakpointArray = breakpointState["breakpoints"].get_array();
         s_breakpoints.clear();
         BOOST_FOREACH(json::Value bp, breakpointArray)
         {
            s_breakpoints.push_back(breakpointFromJson(bp.get_obj()));
         }
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

// Called by the R garbage collector when a Shiny function is cleaned up;
// we use this as a trigger to clean up our own references to the function.
void unregisterShinyFunction(SEXP where)
{
   // Look over each Shiny function we know about; if this was a function
   // we were tracking, release it.
   for (std::vector<boost::shared_ptr<ShinyFunction> >::iterator psfi =
               s_shinyFunctions.begin();
        psfi != s_shinyFunctions.end();
        psfi++)
   {
      if ((*psfi)->getWhere() == where)
      {
         std::cerr << "unregistered " << (*psfi)->getId() << std::endl;
         s_shinyFunctions.erase(psfi);
         break;
      }
   }
}

} // anonymous namespace

void rs_registerShinyFunction(SEXP params)
{
   Protect protect;
   SEXP expr = r::sexp::findVar("expr", params);
   SEXP fun = r::sexp::findVar("fun", params);
   SEXP name = r::sexp::findVar("name", params);
   SEXP where = r::sexp::findVar("where", params);

   std::string objName;
   Error error = r::sexp::extract(name, &objName);
   if (error)
      return;

   boost::shared_ptr<ShinyFunction> psf =
            boost::make_shared<ShinyFunction>(expr, objName, where);
   s_shinyFunctions.push_back(psf);

   // Attach the information we just created to the Shiny function.
   SEXP sid = r::sexp::create(psf->getId(), &protect);
   r::sexp::setAttrib(fun, "_rs_shinyDebugPtr",
                      r::sexp::makeExternalPtr(psf.get(),
                                               unregisterShinyFunction,
                                               &protect));
   r::sexp::setAttrib(fun, "_rs_shinyDebugId", sid);
   r::sexp::setAttrib(fun, "_rs_shinyDebugLabel",
                      r::sexp::findVar("label", params));

   // Look over the list of breakpoints we know about and see if any of them
   // are unbound breakpoints in the region of the file just identified.
   if (!s_breakpointsInSync)
      syncClientBreakpoints();

   r::exec::RFunction(".rs.setShinyFunction", name, where, fun).call();
   // If we found breakpoint lines in this Shiny function, set breakpoints
   // on it.
   std::vector<int> lines = getShinyBreakpointLines(*psf);
   if (lines.size() > 0)
   {
      // Copy the function into the Shiny object first
      r::exec::RFunction(".rs.setShinyBreakpoints", name, where, lines).call();
   }

   std::cerr << "registered " << objName << ": " << psf->getId() << std::endl;
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
      (bind(registerRpcMethod, "set_shiny_breakpoint", setShinyBreakpoint))
      (bind(sourceModuleRFile, "SessionBreakpoints.R"))
      (bind(initBreakpoints));

   return initBlock.execute();
}


} // namepsace breakpoints
} // namespace modules
} // namesapce session


