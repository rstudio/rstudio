/*
 * SessionEnvironment.cpp
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

#include "SessionEnvironment.hpp"
#include "EnvironmentMonitor.hpp"

#include <algorithm>

#include <core/Exec.hpp>

#define INTERNAL_R_FUNCTIONS
#include <r/RJson.hpp>
#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/session/RSession.hpp>
#include <r/RInterface.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionPersistentState.hpp>
#include <boost/foreach.hpp>

#include "EnvironmentUtils.hpp"

#define TOP_FUNCTION 1

using namespace core;

namespace session {
namespace modules { 
namespace environment {

EnvironmentMonitor s_environmentMonitor;

namespace {

bool handleRBrowseEnv(const core::FilePath& filePath)
{
   if (filePath.filename() == "wsbrowser.html")
   {
      module_context::showContent("R objects", filePath);
      return true;
   }
   else
   {
      return false;
   }
}

SEXP getOriginalFunctionCallObject(const RCNTXT* pContext)
{
   SEXP callObject = pContext->callfun;
   // enabling tracing on a function turns it into an S4 object with an
   // 'original' slot that includes the function's original contents. use
   // this instead if it's set up. (consider: is it safe to assume that S4
   // objects here are always traced functions, or do we need to compare classes
   // to be safe?)
   if (Rf_isS4(callObject))
   {
      callObject = r::sexp::getAttrib(callObject, "original");
   }
   return callObject;
}

Error getFileNameFromContext(const RCNTXT* pContext,
                             std::string* pFileName)
{
   SEXP srcref = pContext->srcref;
   return r::exec::RFunction(".rs.sourceFileFromRef", srcref)
                 .call(pFileName);
}

SEXP sourceRefsOfContext(const RCNTXT* pContext)
{
   return r::sexp::getAttrib(getOriginalFunctionCallObject(pContext), "srcref");
}

bool hasSourceRefs(const RCNTXT* pContext)
{
   SEXP srcref = sourceRefsOfContext(pContext);
   return srcref != NULL && TYPEOF(srcref) != NILSXP;
}

bool isDebugHiddenContext(RCNTXT* pContext)
{
   SEXP hideFlag = r::sexp::getAttrib(pContext->callfun, "hideFromDebugger");
   return TYPEOF(hideFlag) != NILSXP && r::sexp::asLogical(hideFlag);
}

// given a context from the context stack, indicate whether it should be emitted
// to the callstack.
bool isPrintableContext(RCNTXT* pContext)
{
   if (pContext->callflag & CTXT_FUNCTION)
   {
      if (isDebugHiddenContext(pContext))
      {
         // hidden function
         return false;
      }
      // printable function
      return true;
   }
   // not a function at all
   return false;
}

// return the function context at the given depth
RCNTXT* getFunctionContext(const int depth,
                           bool findUserCode = false,
                           int* pFoundDepth = NULL,
                           SEXP* pEnvironment = NULL)
{
   RCNTXT* pRContext = r::getGlobalContext();
   int currentDepth = 0;
   while (pRContext->callflag)
   {
      if (isPrintableContext(pRContext))
      {
         // if the caller asked us to find user code, don't stop unless the
         // context we're examining has a source file attached
         if (++currentDepth >= depth &&
             !(findUserCode && !hasSourceRefs(pRContext)))
         {
             break;
         }
      }
      pRContext = pRContext->nextcontext;
   }

   // indicate the depth at which we stopped and the environment we found at
   // that depth, if requested
   if (pFoundDepth)
   {
      *pFoundDepth = currentDepth;
   }
   if (pEnvironment)
   {
      *pEnvironment = currentDepth == 0 ? R_GlobalEnv : pRContext->cloenv;
   }
   return pRContext;
}

// return whether the context stack contains a pure (interactive) browser
bool inBrowseContext()
{
   RCNTXT* pRContext = r::getGlobalContext();
   while (pRContext->callflag)
   {
      if ((pRContext->callflag & CTXT_BROWSER) &&
          !(pRContext->callflag & CTXT_FUNCTION))
      {
         return true;
      }
      pRContext = pRContext->nextcontext;
   }
   return false;
}

// Return whether the current context is being evaluated inside a hidden
// (debugger internal) function at the top level.
bool insideDebugHiddenFunction()
{
   RCNTXT* pRContext = r::getGlobalContext();
   while (pRContext->callflag)
   {
      if (pRContext->callflag & CTXT_FUNCTION)
      {
         // If we find a debugger internal function before any user function,
         // hide it from the user callstack.
         if (isDebugHiddenContext(pRContext))
            return true;

         // If we find a user function before we encounter a debugger internal
         // function, don't hide the user code it invokes.
         if (hasSourceRefs(pRContext))
             return false;
      }
      pRContext = pRContext->nextcontext;
   }
   return false;
}

Error functionNameFromContext(const RCNTXT* pContext,
                              std::string* pFunctionName)
{
   return r::exec::RFunction(".rs.functionNameFromCall", pContext->call)
                            .call(pFunctionName);
}

json::Array callFramesAsJson()
{
   RCNTXT* pRContext = r::getGlobalContext();
   RCNTXT* pSrcContext = pRContext;
   json::Array listFrames;
   int contextDepth = 0;
   Error error;

   while (pRContext->callflag)
   {
      // If this context is hidden from the debugger, we want to avoid
      // delivering source refs for the context that invoked it. Pick up
      // source refs from the next context in this case.
      if (isDebugHiddenContext(pRContext))
      {
          pSrcContext = pRContext->nextcontext;
      }
      else if (isPrintableContext(pRContext))
      {
         json::Object varFrame;
         std::string functionName;
         error = functionNameFromContext(pRContext, &functionName);
         if (error)
         {
            LOG_ERROR(error);
         }
         varFrame["context_depth"] = ++contextDepth;
         varFrame["function_name"] = functionName;

         // in the linked list of R contexts, the srcref associated with each
         // context points to the place from which the context was invoked.
         // however, for traditional debugging, we want the call frame to show
         // where control *left* the frame to go to the next frame. pSrcContext
         // keeps track of the previous invocation.
         std::string filename;
         error = getFileNameFromContext(pSrcContext, &filename);
         if (error)
            LOG_ERROR(error);
         varFrame["file_name"] = filename;
         sourceRefToJson(pSrcContext->srcref, &varFrame);
         pSrcContext = pRContext;

         // extract the first line of the function. the client can optionally
         // use this to compute the source location as an offset into the
         // function rather than as an absolute file position (useful when
         // we need to debug a copy of the function rather than the real deal).
         SEXP srcRef = sourceRefsOfContext(pSrcContext);
         if (srcRef && TYPEOF(srcRef) != NILSXP)
         {
            varFrame["function_line_number"] = INTEGER(srcRef)[0];
         }

         std::string argList;
         SEXP args = CDR(pRContext->call);
         switch (TYPEOF(args))
         {
            case LISTSXP:
               error = r::exec::RFunction(".rs.argumentListSummary", args)
                 .call(&argList);
              break;
            case LANGSXP:
               error = r::exec::RFunction(".rs.promiseDescription", args)
                 .call(&argList);
               break;
         }
         if (error)
         {
            LOG_ERROR(error);
         }
         varFrame["argument_list"] = error ? "" : argList;

         listFrames.push_back(varFrame);
      }
      pRContext = pRContext->nextcontext;
   }
   return listFrames;
}

json::Array environmentListAsJson()
{
    using namespace r::sexp;
    Protect rProtect;
    std::vector<Variable> vars;
    json::Array listJson;

    if (s_environmentMonitor.hasEnvironment())
    {
       SEXP env = s_environmentMonitor.getMonitoredEnvironment();
       if (env != NULL)
          listEnvironment(env, false, &rProtect, &vars);

       // get object details and transform to json
       std::transform(vars.begin(),
                      vars.end(),
                      std::back_inserter(listJson),
                      boost::bind(varToJson, env, _1));
    }

    return listJson;
}

Error listEnvironment(boost::shared_ptr<int> pContextDepth,
                      const json::JsonRpcRequest&,
                      json::JsonRpcResponse* pResponse)
{
   // return list
   pResponse->setResult(environmentListAsJson());
   return Success();
}

// Sets an environment by name. Used when the environment can be reliably
// identified by its name (e.g. package environments).
Error setEnvironmentName(std::string environmentName)
{
   SEXP environment;
   if (environmentName == "R_GlobalEnv")
   {
      environment = R_GlobalEnv;
   }
   else if (environmentName == "base")
   {
      environment = R_BaseEnv;
   }
   else
   {
      r::sexp::Protect protect;
      Error error = r::exec::RFunction("as.environment", environmentName)
               .call(&environment, &protect);
      if (error)
      {
         s_environmentMonitor.setMonitoredEnvironment(R_GlobalEnv, true);
         return error;
      }
   }

   s_environmentMonitor.setMonitoredEnvironment(environment, true);
   return Success();
}

Error setEnvironment(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string environmentName;
   Error error = json::readParam(request.params, 0, &environmentName);
   if (error)
      return error;

   error = setEnvironmentName(environmentName);
   if (error)
      return error;

   persistentState().setActiveEnvironmentName(environmentName);
   return Success();
}

// Sets an environment by its frame number. Used for unnamed, transient
// function environments.
Error setEnvironmentFrame(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   int frameNumber = 0;
   Error error = json::readParam(request.params, 0, &frameNumber);
   if (error)
      return error;

   SEXP environment;
   r::sexp::Protect protect;
   error = r::exec::RFunction("sys.frame", frameNumber)
            .call(&environment, &protect);
   if (error)
      return error;

   s_environmentMonitor.setMonitoredEnvironment(environment, true);
   return Success();
}

// given a function context, indicate whether the copy of the source code
// for the function is different than the source code on disk.
bool functionIsOutOfSync(const RCNTXT *pContext,
                         std::string *pFunctionCode)
{
   Error error;

   // start by extracting the source code from the call site
   error = r::exec::RFunction(".rs.sourceCodeFromFunction",
                              getOriginalFunctionCallObject(pContext))
         .call(pFunctionCode);
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   // make sure the function has source references
   if (!hasSourceRefs(pContext))
   {
      return true;
   }

   return functionDiffersFromSource(
            sourceRefsOfContext(pContext), *pFunctionCode);
}

// Returns a JSON array containing the names and associated call frame numbers
// of the current environment stack.
json::Value environmentNames(SEXP env)
{
   SEXP environments;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.environmentList", env)
                                    .call(&environments, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }
   else
   {
      json::Value namesJson;
      error = r::json::jsonValueFromObject(environments, &namesJson);
      if (error)
      {
         LOG_ERROR(error);
         return json::Array();
      }
      return namesJson;
   }
}

// create a JSON object that contains information about the current environment;
// used both to initialize the environment state on first load and to send
// information about the new environment on a context change
json::Object commonEnvironmentStateData(int depth)
{
   json::Object varJson;
   bool useProvidedSource = false;
   std::string functionCode;

   varJson["context_depth"] = depth;
   varJson["environment_list"] = environmentListAsJson();
   varJson["call_frames"] = callFramesAsJson();

   // if we're in a debug context, add information about the function currently
   // being debugged
   if (depth > 0)
   {
      RCNTXT* pContext = getFunctionContext(depth);
      std::string functionName;
      Error error = functionNameFromContext(pContext, &functionName);
      if (error)
      {
         LOG_ERROR(error);
      }
      varJson["function_name"] = functionName;
      varJson["environment_name"] = functionName + "()";

      // see if the function to be debugged is out of sync with its saved
      // sources (if available).
      if ((pContext))
      {
         useProvidedSource =
               functionIsOutOfSync(pContext, &functionCode) &&
               functionCode != "NULL";
      }
   }
   else
   {
      varJson["function_name"] = "";

      // emit the name of the environment we're currently working with
      std::string environmentName;
      if (s_environmentMonitor.hasEnvironment())
      {
         Error error = r::exec::RFunction("environmentName",
                                    s_environmentMonitor.getMonitoredEnvironment())
                                    .call(&environmentName);
         if (error)
            LOG_ERROR(error);
      }
      varJson["environment_name"] = environmentName;
   }

   // list the environment stack at this context depth
   varJson["environments"] = environmentNames(depth > 0 ?
               s_environmentMonitor.getMonitoredEnvironment() :
               R_GlobalEnv);

   // always emit the code for the function, even if we don't think that the
   // client's going to need it. we only checked the saved copy of the function
   // above; the client may be aware of local/unsaved changes to the function,
   // in which case it will need to fall back on a server-provided copy.
   varJson["use_provided_source"] = useProvidedSource;
   varJson["function_code"] = functionCode;

   return varJson;
}

void enqueContextDepthChangedEvent(int depth)
{
   // emit an event to the client indicating the new call frame and the
   // current state of the environment
   ClientEvent event (client_events::kContextDepthChanged,
                      commonEnvironmentStateData(depth));
   module_context::enqueClientEvent(event);
}

void enqueBrowserLineChangedEvent(const SEXP srcref)
{
   json::Object varJson;
   sourceRefToJson(srcref, &varJson);
   ClientEvent event (client_events::kBrowserLineChanged, varJson);
   module_context::enqueClientEvent(event);
}

Error setContextDepth(boost::shared_ptr<int> pContextDepth,
                      const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   // get the requested depth
   int requestedDepth;
   Error error = json::readParam(request.params, 0, &requestedDepth);
   if (error)
      return error;

   // set state for the new depth
   *pContextDepth = requestedDepth;
   SEXP env = NULL;
   getFunctionContext(requestedDepth, false, NULL, &env);
   s_environmentMonitor.setMonitoredEnvironment(env);

   // populate the new state on the client
   enqueContextDepthChangedEvent(*pContextDepth);

   return Success();
}

Error getEnvironmentState(boost::shared_ptr<int> pContextDepth,
                          const json::JsonRpcRequest&,
                          json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(commonEnvironmentStateData(*pContextDepth));
   return Success();
}

void onDetectChanges(module_context::ChangeSource source)
{
   s_environmentMonitor.checkForChanges();
}

void onConsolePrompt(boost::shared_ptr<int> pContextDepth,
                     boost::shared_ptr<RCNTXT*> pCurrentContext)
{
   int depth = 0;
   SEXP environmentTop = NULL;
   RCNTXT* pRContext =
         getFunctionContext(TOP_FUNCTION, true, &depth, &environmentTop);

   if (environmentTop != s_environmentMonitor.getMonitoredEnvironment() ||
       depth != *pContextDepth ||
       pRContext != *pCurrentContext)
   {
      json::Object varJson;

      // if we appear to be switching into debug mode, make sure there's a
      // browser call somewhere on the stack. if there isn't, then we're
      // probably just waiting for user input inside a function (e.g. scan());
      // assume the user isn't interested in seeing the function's internals.
      if (*pContextDepth == 0 &&
          !inBrowseContext())
      {
         return;
      }

      // start monitoring the enviroment at the new depth
      s_environmentMonitor.setMonitoredEnvironment(environmentTop);
      *pContextDepth = depth;
      *pCurrentContext = pRContext;
      enqueContextDepthChangedEvent(depth);
   }
   // if we're debugging and stayed in the same frame, update the line number
   else if (depth > 0)
   {
      // we don't want to send linenumber updates if the current depth is inside
      // a debug-hidden function
      if (!insideDebugHiddenFunction())
      {
         enqueBrowserLineChangedEvent(r::getGlobalContext()->srcref);
      }
   }
}

Error getEnvironmentNames(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(environmentNames(R_GlobalEnv));
   return Success();
}

void initEnvironmentMonitoring()
{
   // Check to see whether we're actively debugging. If we are, the debug
   // environment trumps whatever the user wants to browse in at the top level.
   int contextDepth = 0;
   getFunctionContext(TOP_FUNCTION, false, &contextDepth);
   if (contextDepth == 0)
   {
      // Not actively debugging; see if we have a stored environment name to
      // begin monitoring.
      std::string envName = persistentState().activeEnvironmentName();
      if (!envName.empty())
      {
         setEnvironmentName(envName);
      }
   }
}

// Remove the given objects from the currently monitored environment.
Error removeObjects(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   json::Array objectNames;
   Error error = json::readParam(request.params, 0, &objectNames);
   if (error)
      return error;

   error = r::exec::RFunction(".rs.removeObjects",
                        objectNames,
                        s_environmentMonitor.getMonitoredEnvironment()).call();
   if (error)
      return error;

   return Success();
}

} // anonymous namespace

json::Value environmentStateAsJson()
{
   int contextDepth = 0;
   getFunctionContext(TOP_FUNCTION, true, &contextDepth);
   return commonEnvironmentStateData(contextDepth);
}

Error initialize()
{
   boost::shared_ptr<int> pContextDepth =
         boost::make_shared<int>(0);
   boost::shared_ptr<RCNTXT*> pCurrentContext =
         boost::make_shared<RCNTXT*>(r::getGlobalContext());

   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onConsolePrompt.connect(bind(onConsolePrompt,
                                         pContextDepth,
                                         pCurrentContext));

   json::JsonRpcFunction listEnv =
         boost::bind(listEnvironment, pContextDepth, _1, _2);
   json::JsonRpcFunction setCtxDepth =
         boost::bind(setContextDepth, pContextDepth, _1, _2);
   json::JsonRpcFunction getEnv =
         boost::bind(getEnvironmentState, pContextDepth, _1, _2);

   initEnvironmentMonitoring();

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRBrowseFileHandler, handleRBrowseEnv))
      (bind(registerRpcMethod, "list_environment", listEnv))
      (bind(registerRpcMethod, "set_context_depth", setCtxDepth))
      (bind(registerRpcMethod, "set_environment", setEnvironment))
      (bind(registerRpcMethod, "set_environment_frame", setEnvironmentFrame))
      (bind(registerRpcMethod, "get_environment_names", getEnvironmentNames))
      (bind(registerRpcMethod, "remove_objects", removeObjects))
      (bind(registerRpcMethod, "get_environment_state", getEnv))
      (bind(sourceModuleRFile, "SessionEnvironment.R"));

   return initBlock.execute();
}
   
} // namespace environment
} // namespace modules
} // namesapce session

