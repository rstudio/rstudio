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
#include <r/RInternal.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;
using namespace rstudio::r::sexp;
using namespace rstudio::r::exec;

namespace rstudio {
namespace session {
namespace modules {
namespace breakpoints {
namespace
{

int s_maxShinyFunctionId = 0;

// Represents a currently running Shiny function.
class ShinyFunction : boost::noncopyable
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

   bool contains(std::string filename, int line) const
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

   int getSize()
   {
      return lastLine_ - firstLine_;
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

// Breakpoint data known by the server (subset of fields known by the client)
#define TYPE_FUNCTION 0
#define TYPE_TOPLEVEL 1

class Breakpoint : boost::noncopyable
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
   {}
};

// A list of the breakpoints we know about. Note that this is a slave list;
// the client maintains the master copy and is responsible for synchronizing
// with this list. This list is maintained so we can inject breakpoints
// synchronously.
std::vector<boost::shared_ptr<Breakpoint> > s_breakpoints;

// Returns the Shiny function that contains the given line, if any.
// Finds the smallest (innermost) function in the case where more than one
// expression encloses the line.
boost::shared_ptr<ShinyFunction> findShinyFunction(std::string filename,
                                                   int line)
{
   boost::shared_ptr<ShinyFunction> bestPsf;
   int bestSize = INT_MAX;
   BOOST_FOREACH(boost::shared_ptr<ShinyFunction> psf, s_shinyFunctions)
   {
      if (psf->contains(filename, line) &&
          psf->getSize() < bestSize)
      {
         bestSize = psf->getSize();
         bestPsf = psf;
      }
   }

   return bestPsf;
}

boost::shared_ptr<Breakpoint> breakpointFromJson(json::Object& obj)
{
   return boost::make_shared<Breakpoint>(obj["type"].get_int(),
                                         obj["line_number"].get_int(),
                                         obj["id"].get_int(),
                                         obj["path"].get_str());

}

std::vector<int> getShinyBreakpointLines(const ShinyFunction& sf)
{
   std::vector<int> lines;
   BOOST_FOREACH(boost::shared_ptr<Breakpoint> pbp, s_breakpoints)
   {
      if (sf.contains(pbp->path, pbp->lineNumber) &&
          pbp->type == TYPE_TOPLEVEL)
         lines.push_back(pbp->lineNumber);
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
   int lineNumber = 0;
   bool inSync = false;
   Error error = json::readParams(
            request.params, &functionName, &fileName, &lineNumber);
   if (error)
   {
       return error;
   }

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

   response["sync_state"] = inSync;
   response["package_name"] = packageName;
   response["is_package_function"] = packageName.length() > 0;
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
   std::string functionName, fileName, packageName, projPackageName;
   json::Array steps;
   bool set = false;
   Error error = json::readParams(request.params,
            &functionName,
            &fileName,
            &packageName,
            &steps);
   if (error)
      return error;

   // Always search the global namespace (and attached namespaces)
   // first. Manipulating the source references for the copy in a searchable
   // namespace has (inexplicable) side effects in package namespaces, but the
   // reverse is not true (see case 3795).
   set |= setBreakpoint(functionName, fileName, "", steps);

   // If we're in package development mode, try to set a breakpoint in the
   // package's namespace.
   const projects::ProjectContext& projectContext = projects::projectContext();
   if (projectContext.config().buildType == r_util::kBuildTypePackage)
   {
      projPackageName = projectContext.packageInfo().name();
      set |= setBreakpoint(
               functionName, fileName, projPackageName, steps);
   }

   // If a package name was specified, try to set a breakpoint in that package's
   // namespace, too (unless we did already).
   if (packageName.length() > 0 &&
       packageName != projPackageName)
   {
      set |= setBreakpoint(functionName, fileName, packageName, steps);
   }

   // Couldn't find a function to set a breakpoint on--maybe a bad parameter?
   if (!set)
   {
      return Error(json::errc::ParamInvalid, ERROR_LOCATION);
   }

   return Success();
}

std::vector<boost::shared_ptr<Breakpoint> >::iterator posOfBreakpointId(int id)
{
   std::vector<boost::shared_ptr<Breakpoint> >::iterator psbi;
   for (psbi = s_breakpoints.begin();
        psbi != s_breakpoints.end();
        psbi++)
   {
      if ((*psbi)->id == id)
         break;
   }
   return psbi;
}

// Called by the R garbage collector when a Shiny function is cleaned up;
// we use this as a trigger to clean up our own references to the function.
void unregisterShinyFunction(SEXP ptr)
{
   // Extract the cached pointer
   ShinyFunction* psf = static_cast<ShinyFunction*>
         (r::sexp::getExternalPtrAddr(ptr));
   if (psf == NULL)
      return;

   // Look over each Shiny function we know about; if this was a function
   // we were tracking, release it.
   for (std::vector<boost::shared_ptr<ShinyFunction> >::iterator psfi =
               s_shinyFunctions.begin();
        psfi != s_shinyFunctions.end();
        psfi++)
   {
      if (psfi->get() == psf)
      {
         s_shinyFunctions.erase(psfi);
         break;
      }
   }
   r::sexp::clearExternalPtr(ptr);
}

// Called by Shiny (through a debug hook set up in tools:rstudio) to register
// a Shiny function for debugging.
//
// 'params' is an ENVSXP expected to contain the following contents:
// expr  - The original expression from which the Shiny function was generated
// fun   - The function generated from that expression
// name  - The name of the variable or field containing the object
// where - The environment or reference object containing the object
// label - The name to be shown for the object in the debugger
//
// Sets up a data structure and attaches it to the function as an EXTPTRSXP
// attribute; unregistration is performed when R garbage-collects this pointer.
void rs_registerShinyFunction(SEXP params)
{
   Protect protect;
   SEXP expr = r::sexp::findVar("expr", params);
   SEXP fun = r::sexp::findVar("fun", params);
   SEXP name = r::sexp::findVar("name", params);
   SEXP where = r::sexp::findVar("where", params);

   // Get the name of the object we're about to attach.
   std::string objName;
   Error error = r::sexp::extract(name, &objName);
   if (error)
      return;

   boost::shared_ptr<ShinyFunction> psf =
            boost::make_shared<ShinyFunction>(expr, objName, where);

   // The Shiny server function itself is always the first one registered when
   // a Shiny session starts. If we had other functions "running", they
   // likely simply haven't been GC'ed yet--forcefully clean them up.
   SEXP isShinyServer = r::sexp::getAttrib(fun, "shinyServerFunction");
   if (isShinyServer != NULL &&
       TYPEOF(isShinyServer) != NILSXP)
   {
      s_shinyFunctions.clear();
   }

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

   r::exec::RFunction(".rs.setShinyFunction", name, where, fun).call();

   // If we found breakpoint lines in this Shiny function, set breakpoints
   // on it.
   std::vector<int> lines = getShinyBreakpointLines(*psf);
   if (lines.size() > 0)
   {
      // Copy the function into the Shiny object first
      r::exec::RFunction(".rs.setShinyBreakpoints", name, where, lines).call();
   }
}

// Executes the contents of the given file under the debugger
SEXP rs_debugSourceFile(SEXP filename, SEXP encoding, SEXP local)
{
   // Get the file that was sourced
   std::string path;
   Error error = r::sexp::extract(filename, &path, true);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   FilePath filePath = module_context::resolveAliasedPath(path);

   // Find all the lines in the file that have breakpoints
   std::vector<int> lines;
   BOOST_FOREACH(boost::shared_ptr<Breakpoint> pbp, s_breakpoints)
   {
      if (module_context::resolveAliasedPath(pbp->path) == filePath)
      {
         lines.push_back(pbp->lineNumber);
      }
   }

   // Execute the contents with breakpoints. Don't log errors here, since it's
   // acceptable for errors to be raised from the code in the file, and don't
   // disable the user's error handlers.
   Protect protect;
   SEXP lineSEXP = lines.size() > 0 ?
                        r::sexp::create(lines, &protect) :
                        R_NilValue;
   error = r::exec::RFunction(".rs.executeDebugSource", filename, encoding,
                              lineSEXP, local)
                             .callUnsafe();

   // Let the client know we're done; this is the client's cue to re-inject
   // breakpoints.
   json::Object result;
   result["path"] = path;
   result["succeeded"] = error ? false : true;
   ClientEvent debugSourceCompleted(client_events::kDebugSourceCompleted,
                                    result);
   module_context::enqueClientEvent(debugSourceCompleted);

   return R_NilValue;
}

Error initBreakpoints()
{
   // Register rs_debugSourceFile; called from the console (as debugSource)
   R_CallMethodDef debugSource;
   debugSource.name = "rs_debugSourceFile";
   debugSource.fun = (DL_FUNC)rs_debugSourceFile;
   debugSource.numArgs = 3;
   r::routines::addCallMethod(debugSource);

   // Register rs_registerShinyFunction; called from registerShinyDebugHook
   R_CallMethodDef registerShiny;
   registerShiny.name = "rs_registerShinyFunction";
   registerShiny.fun = (DL_FUNC)rs_registerShinyFunction;
   registerShiny.numArgs = 1;
   r::routines::addCallMethod(registerShiny);

   // Initializes the set of breakpoints the server knows about by populating
   // it from client state. This set is used for synchronous breakpoint
   // injection when a Shiny function is registered or debugSource is run.
   json::Value breakpointStateValue =
      r::session::clientState().getProjectPersistent("debug-breakpoints",
                                                     "debugBreakpointsState");
   if (!breakpointStateValue.is_null() &&
       json::isType<core::json::Object>(breakpointStateValue))
   {
      json::Object breakpointState = breakpointStateValue.get_obj();
      
      // Protect against the breakpoint array being serialized as an
      // empty object
      json::Value jsonBreakpointArray = breakpointState["breakpoints"];
      if (!json::isType<core::json::Array>(jsonBreakpointArray))
      {
         Error error = json::errors::typeMismatch(
                  jsonBreakpointArray,
                  json::ArrayType,
                  ERROR_LOCATION);
         LOG_ERROR(error);
      }
      else
      {
         json::Array breakpointArray = jsonBreakpointArray.get_array();
         s_breakpoints.clear();
         BOOST_FOREACH(json::Value bp, breakpointArray)
         {
            if (json::isType<core::json::Object>(bp))
            {
               s_breakpoints.push_back(breakpointFromJson(bp.get_obj()));
            }
         }
      }
   }

   return Success();
}

Error updateBreakpoints(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse*)
{
   json::Array breakpointArr;
   bool set = false, arm = false;
   Error error = json::readParams(request.params, &breakpointArr, &set, &arm);
   if (error)
      return error;

   BOOST_FOREACH(json::Value bp, breakpointArr)
   {
      boost::shared_ptr<Breakpoint> breakpoint
            (breakpointFromJson(bp.get_obj()));
      std::vector<boost::shared_ptr<Breakpoint> >::iterator psbi =
            posOfBreakpointId(breakpoint->id);

      // Erase anything we already know about this breakpoint
      if (psbi != s_breakpoints.end())
         s_breakpoints.erase(psbi);

      // If setting or updating the brekapoint, reintroduce it
      if (set)
         s_breakpoints.push_back(breakpoint);

      // Is this breakpoint associated with a running Shiny function? If it is,
      // and the caller wants the changes armed immediately, reflect them
      if (arm && breakpoint->type == TYPE_TOPLEVEL) {
         boost::shared_ptr<ShinyFunction> psf =
               findShinyFunction(breakpoint->path, breakpoint->lineNumber);
         if (psf)
         {
            // Collect all the breakpoints associated with this function and
            // update the function's state
            std::vector<int> lines = getShinyBreakpointLines(*psf);
            r::exec::RFunction(".rs.setShinyBreakpoints", psf->getName(),
                                                          psf->getWhere(),
                                                          lines).call();
         }
      }
   }

   return Success();
}

Error removeAllBreakpoints(const json::JsonRpcRequest&,
                           json::JsonRpcResponse*)
{
   s_breakpoints.clear();
   return Success();
}

} // anonymous namespace

bool haveSrcrefAttribute()
{
   // check whether this is R 2.14 or greater
   bool haveSrcref = false;
   Error error = r::exec::evaluateString("getRversion() >= '2.14.0'", &haveSrcref);
   if (error)
      LOG_ERROR(error);
   return haveSrcref;
}

bool haveAdvancedStepCommands()
{
   bool haveCommands = false;
   Error error = r::exec::RFunction(".rs.haveAdvancedSteppingCommands")
                                                      .call(&haveCommands);
   if (error)
       LOG_ERROR(error);
   return haveCommands;
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_function_state", getFunctionState))
      (bind(registerRpcMethod, "set_function_breakpoints", setBreakpoints))
      (bind(registerRpcMethod, "remove_all_breakpoints", removeAllBreakpoints))
      (bind(registerRpcMethod, "update_breakpoints", updateBreakpoints))
      (bind(sourceModuleRFile, "SessionBreakpoints.R"))
      (bind(initBreakpoints));

   return initBlock.execute();
}


} // namepsace breakpoints
} // namespace modules
} // namespace session
} // namespace rstudio


