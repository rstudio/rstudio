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
#include <core/RecursionGuard.hpp>

#define INTERNAL_R_FUNCTIONS
#include <r/RJson.hpp>
#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/session/RSession.hpp>
#include <r/RInterface.hpp>
#include <r/RRoutines.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionPersistentState.hpp>
#include <boost/foreach.hpp>

#include "EnvironmentUtils.hpp"

#define BROWSER_FUNCTION 0

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace environment {

// allocate on the heap so we control timing of destruction (if we leave it
// to the destructor we might release the underlying environment SEXP after
// R has already shut down)
EnvironmentMonitor* s_pEnvironmentMonitor = NULL;

// is the browser currently active? we store this state
// so that we can query this from R, without 'hiding' the
// browser state by pushing new contexts / frames on the stack
bool s_browserActive = false;

namespace {

// Keeps track of the data related to the most recent debugging event
class LineDebugState
{
   public:
      LineDebugState()
      {
         reset();
      }
      void reset()
      {
         lastDebugText = "";
         lastDebugLine = 0;
      }
      std::string lastDebugText;
      int lastDebugLine;
};

// The environment monitor and friends do work in reponse to events in R.
// In rare cases, this work can trigger the same events in R that are
// being responded to, leading to unwanted recursion. This simple guard
// increments the given counter on construction (and decrements on destruction)
// so vulnerable event handlers below can prevent reentrancy.
class EventRecursionGuard
{
public:
   EventRecursionGuard(int& counter): counter_(counter) { counter_++; }
   ~EventRecursionGuard() { counter_--; }
private:
   int& counter_;
};

bool isValidSrcref(SEXP srcref)
{
   return srcref && TYPEOF(srcref) != NILSXP;
}

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

RCNTXT* firstFunctionContext(RCNTXT* start)
{
   RCNTXT* firstFunContext = start;
   while ((firstFunContext->callfun == NULL ||
           firstFunContext->callfun == R_NilValue) &&
          firstFunContext->callflag)
      firstFunContext = firstFunContext->nextcontext;
   return firstFunContext;
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
   if (isValidSrcref(srcref))
   {
      return sourceFileFromRef(srcref, pFileName);
   }
   else
   {
      // If no source references, that's OK--just set an empty filename.
      pFileName->clear();
      return Success();
   }
}

// call objects can't be passed as primary values through our R interface
// (early evaluation can be triggered) so we wrap them in an attribute attached
// to a dummy value when we need to pass them through
Error invokeFunctionOnCall(const char* rFunction,
                           SEXP call, std::string* pResult)
{
   SEXP result;
   r::sexp::Protect protect;
   SEXP val = r::sexp::create("_rs_callval", &protect);
   r::sexp::setAttrib(val, "_rs_call", call);
   Error error = r::exec::RFunction(rFunction, val)
                            .call(&result, &protect);
   if (!error && r::sexp::length(result) > 0)
   {
      error = r::sexp::extract(result, pResult, true);
   }
   else
   {
      pResult->clear();
   }
   return error;
}

Error functionNameFromContext(const RCNTXT* pContext,
                              std::string* pFunctionName)
{
   return invokeFunctionOnCall(".rs.functionNameFromCall", pContext->call,
                               pFunctionName);
}

// Construct a simulated source reference from a context containing a
// function being debugged, and either the context containing the current
// invocation or a string containing the last debug ouput from R.
// We use this to highlight portions of deparsed functions when visually
// stepping through code for which source references are unvailable.
SEXP simulatedSourceRefsOfContext(const RCNTXT* pContext,
                                  const RCNTXT* pLineContext,
                                  const LineDebugState* pLineDebugState)
{
   SEXP simulatedSrcref = R_NilValue;
   r::sexp::Protect protect;
   // The objects we will later transmit to .rs.simulateSourceRefs below
   // include language objects that we need to protect from early evaluation.
   // Attach them to a carrier SEXP as attributes rather than passing directly.
   SEXP info = r::sexp::create("_rs_sourceinfo", &protect);
   r::sexp::setAttrib(info, "_rs_callfun", pContext->callfun);
   if (pLineContext != NULL)
   {
      r::sexp::setAttrib(info, "_rs_callobj", pLineContext->call);
   }
   else if (pLineDebugState != NULL)
   {
      SEXP lastDebugSEXP = r::sexp::create(
               pLineDebugState->lastDebugText, &protect);
      r::sexp::setAttrib(info, "_rs_calltext", lastDebugSEXP);
      SEXP lastLineSEXP = r::sexp::create(
               pLineDebugState->lastDebugLine, &protect);
      r::sexp::setAttrib(info, "_rs_lastline", lastLineSEXP);
   }
   Error error = r::exec::RFunction(".rs.simulateSourceRefs", info)
         .call(&simulatedSrcref, &protect);
   if (error)
      LOG_ERROR(error);
   return simulatedSrcref;
}

SEXP sourceRefsOfContext(const RCNTXT* pContext)
{
   return r::sexp::getAttrib(getOriginalFunctionCallObject(pContext), "srcref");
}

void getShinyFunctionLabel(const RCNTXT* pContext, std::string* label)
{
   SEXP s = r::sexp::getAttrib(
            getOriginalFunctionCallObject(pContext), "_rs_shinyDebugLabel");
   if (s != NULL && TYPEOF(s) != NILSXP)
   {
      r::sexp::extract(s, label);
   }
}

bool hasSourceRefs(const RCNTXT* pContext)
{
   return isValidSrcref(sourceRefsOfContext(pContext));
}

bool isDebugHiddenContext(RCNTXT* pContext)
{
   SEXP hideFlag = r::sexp::getAttrib(pContext->callfun, "hideFromDebugger");
   return TYPEOF(hideFlag) != NILSXP && r::sexp::asLogical(hideFlag);
}

bool isErrorHandlerContext(RCNTXT* pContext)
{
   SEXP errFlag = r::sexp::getAttrib(pContext->callfun, "errorHandlerType");
   return TYPEOF(errFlag) == INTSXP;
}

// return the function context at the given depth
RCNTXT* getFunctionContext(const int depth,
                           int* pFoundDepth = NULL,
                           SEXP* pEnvironment = NULL)
{
   RCNTXT* pRContext = r::getGlobalContext();
   RCNTXT* pFoundContext = NULL;
   int currentDepth = 0;
   int foundDepth = 0;
   SEXP browseEnv = R_NilValue;
   while (pRContext->callflag)
   {
      // if looking for the actively browsed function, pick the environment
      // evaluated by the browser on top of the stack
      if (pRContext->callflag & CTXT_BROWSER && browseEnv == R_NilValue) 
      {
         browseEnv = pRContext->cloenv;
      }
      if (pRContext->callflag & CTXT_FUNCTION)
      {
         currentDepth++;
         if (depth == BROWSER_FUNCTION && pRContext->cloenv == browseEnv) 
         {
            foundDepth = currentDepth;
            pFoundContext = pRContext;
            // continue traversing the callstack; there may be several 
            // functions eval'ing this environment and we want the "original"
            // (here meaning oldest on the callstack)
         }
         else if (depth > BROWSER_FUNCTION && currentDepth >= depth)
         {
            foundDepth = currentDepth;
            pFoundContext = pRContext;
            break;
         }
      }
      pRContext = pRContext->nextcontext;
   }

   // indicate the depth at which we stopped and the environment we found at
   // that depth, if requested
   if (pFoundDepth)
   {
      *pFoundDepth = foundDepth;
   }
   if (pEnvironment)
   {
      *pEnvironment = (foundDepth == 0 || pFoundContext == NULL) ? 
         R_GlobalEnv : 
         pFoundContext->cloenv;
   }
   return pFoundContext;
}

// Return whether we're in browse context--meaning that there's a browser on
// the context stack and at least one function (i.e. we're not browsing at the
// top level).
bool inBrowseContext()
{
   RCNTXT* pRContext = r::getGlobalContext();
   bool foundBrowser = false;
   bool foundFunction = false;
   while (pRContext->callflag)
   {
      if ((pRContext->callflag & CTXT_BROWSER) &&
          !(pRContext->callflag & CTXT_FUNCTION))
      {
         foundBrowser = true;
      }
      else if (pRContext->callflag & CTXT_FUNCTION)
      {
         foundFunction = true;
      }
      if (foundBrowser && foundFunction)
      {
         s_browserActive = true;
         return true;
      }
      pRContext = pRContext->nextcontext;
   }
   s_browserActive = false;
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

// Return the call frames and debug information as a JSON object.
json::Array callFramesAsJson(LineDebugState* pLineDebugState)
{
   RCNTXT* pRContext = r::getGlobalContext();
   RCNTXT* pPrevContext = pRContext;
   RCNTXT* pSrcContext = pRContext;
   json::Array listFrames;
   int contextDepth = 0;
   Error error;
   std::map<SEXP,RCNTXT*> envSrcrefCtx;

   while (pRContext->callflag)
   {
      // if this context has a valid srcref, use it to supply the srcrefs for
      // debugging in the environment of the callee. note that there may be
      // multiple srcrefs on the stack for a given closure; in this case we
      // always want to take the first one as it's the most current/specific.
      if (isValidSrcref(pRContext->srcref) && pRContext->nextcontext != NULL) 
      {
         SEXP env = pRContext->nextcontext->cloenv;
         if (envSrcrefCtx.find(env) == envSrcrefCtx.end())
            envSrcrefCtx[env] = pRContext;
      }

      if (pRContext->callflag & CTXT_FUNCTION)
      {
         json::Object varFrame;
         std::string functionName;
         varFrame["context_depth"] = ++contextDepth;

         error = functionNameFromContext(pRContext, &functionName);
         if (error)
         {
            LOG_ERROR(error);
         }
         varFrame["function_name"] = functionName;
         varFrame["is_error_handler"] = isErrorHandlerContext(pRContext);
         varFrame["is_hidden"] = isDebugHiddenContext(pRContext);

         // attempt to find the refs for the source that invoked this function;
         // use our own refs otherwise
         std::map<SEXP,RCNTXT*>::iterator srcCtx = envSrcrefCtx.find(pRContext->cloenv);
         if (srcCtx != envSrcrefCtx.end())
            pSrcContext = srcCtx->second;
         else
            pSrcContext = pRContext;

         // mark this as a source-equivalent function if it's evaluating user
         // code into the global environment
         varFrame["is_source_equiv"] = pRContext->cloenv == R_GlobalEnv && 
            isValidSrcref(pSrcContext->srcref);

         std::string filename;
         error = getFileNameFromContext(pSrcContext, &filename);
         if (error)
            LOG_ERROR(error);
         varFrame["file_name"] = filename;
         varFrame["aliased_file_name"] =
               module_context::createAliasedPath(FilePath(filename));

         SEXP srcref = pSrcContext->srcref;
         if (isValidSrcref(srcref))
         {
            varFrame["real_sourceref"] = true;
            sourceRefToJson(srcref, &varFrame);
         }
         else
         {
            varFrame["real_sourceref"] = false;
            // if this is the top frame, we simulate the sourceref using R
            // output of the last debugged statement; if it isn't, we
            // construct it by deparsing calls in the context stack.
            SEXP simulatedSrcref;
            if (contextDepth == 1 &&
                pLineDebugState != NULL &&
                pLineDebugState->lastDebugText.length() > 0)
               simulatedSrcref =
                     simulatedSourceRefsOfContext(pRContext, NULL,
                                                  pLineDebugState);
            else
               simulatedSrcref =
                     simulatedSourceRefsOfContext(pRContext, pPrevContext,
                                                  NULL);

            // store the line stepped over in the top frame, so we can infer
            // that the next line stepped over will be near this one
            if (contextDepth == 1 &&
                pLineDebugState != NULL &&
                isValidSrcref(simulatedSrcref))
            {
               int stepLine = INTEGER(simulatedSrcref)[0];
               if (stepLine > 0)
                  pLineDebugState->lastDebugLine = stepLine;
            }

            sourceRefToJson(simulatedSrcref, &varFrame);
         }

         // extract the first line of the function. the client can optionally
         // use this to compute the source location as an offset into the
         // function rather than as an absolute file position (useful when
         // we need to debug a copy of the function rather than the real deal).
         SEXP srcRef = sourceRefsOfContext(pSrcContext);
         if (isValidSrcref(srcRef))
         {
            varFrame["function_line_number"] = INTEGER(srcRef)[0];
         }
         else
         {
            // if we don't have a source ref, we'll debug using a deparsed
            // version of the function that starts on line 1
            varFrame["function_line_number"] = 1;
         }

         std::string callSummary;
         error = invokeFunctionOnCall(".rs.callSummary", pRContext->call,
                                      &callSummary);
         if (error)
            LOG_ERROR(error);

         varFrame["call_summary"] = error ? "" : callSummary;

         // If this is a Shiny function, provide its label
         std::string shinyLabel;
         getShinyFunctionLabel(pRContext, &shinyLabel);
         varFrame["shiny_function_label"] = shinyLabel;

         listFrames.push_back(varFrame);
      }
      pPrevContext = pRContext;
      pRContext = pRContext->nextcontext;
   }
   return listFrames;
}

json::Array environmentListAsJson()
{
    using namespace rstudio::r::sexp;
    Protect rProtect;
    std::vector<Variable> vars;
    json::Array listJson;

    if (s_pEnvironmentMonitor->hasEnvironment())
    {
       SEXP env = s_pEnvironmentMonitor->getMonitoredEnvironment();
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
Error setEnvironmentName(int contextDepth,
                         RCNTXT* pContext,
                         std::string environmentName)
{
   SEXP environment = R_GlobalEnv;
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
      // We need to traverse the search path manually looking for an environment
      // whose name matches the one the caller requested, because R's
      // as.environment() function only searches the global search path, and
      // we may wish to set an environment whose name only exists in a private
      // environment chain.
      //
      // This would be better wrapped in an R function, but this code may
      // run during session init when tools:rstudio isn't yet attached to the
      // search path.
      SEXP env = contextDepth > 0 ?
                        pContext->cloenv :
                        R_GlobalEnv;
      std::string candidateEnv;
      Error error;
      while (env != R_EmptyEnv)
      {
         error = r::exec::RFunction("environmentName", env).call(&candidateEnv);
         if (error)
            break;
         if (candidateEnv == environmentName)
         {
            environment = env;
            break;
         }
         // Proceed to the parent of the environment
         env = ENCLOS(env);
      }
      if (error || env == R_EmptyEnv)
      {
         s_pEnvironmentMonitor->setMonitoredEnvironment(R_GlobalEnv, true);
         return error;
      }
   }

   s_pEnvironmentMonitor->setMonitoredEnvironment(environment, true);
   return Success();
}

Error setEnvironment(boost::shared_ptr<int> pContextDepth,
                     boost::shared_ptr<RCNTXT*> pCurrentContext,
                     const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string environmentName;
   Error error = json::readParam(request.params, 0, &environmentName);
   if (error)
      return error;

   error = setEnvironmentName(*pContextDepth,
                              *pCurrentContext,
                              environmentName);
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

   s_pEnvironmentMonitor->setMonitoredEnvironment(environment, true);
   return Success();
}

// given a function context, indicate whether the copy of the source code
// for the function is different than the source code on disk.
bool functionIsOutOfSync(const RCNTXT *pContext,
                         std::string *pFunctionCode)
{
   Error error;
   r::sexp::Protect protect;
   SEXP sexpCode = R_NilValue;

   // start by extracting the source code from the call site
   error = r::exec::RFunction(".rs.sourceCodeFromFunction",
                              getOriginalFunctionCallObject(pContext))
         .call(&sexpCode, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return true;
   }

   error = r::sexp::extract(sexpCode, pFunctionCode, true);
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
json::Object commonEnvironmentStateData(
   int depth,
   LineDebugState* pLineDebugState)
{
   json::Object varJson;
   bool useProvidedSource = false;
   std::string functionCode;
   bool inFunctionEnvironment = false;

   varJson["context_depth"] = depth;
   varJson["environment_list"] = environmentListAsJson();
   varJson["call_frames"] = callFramesAsJson(pLineDebugState);

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

      // If the environment currently monitored is the function's environment,
      // return that environment, unless the environment is the global
      // environment (which happens for source-equivalent functions).
      SEXP env = s_pEnvironmentMonitor->getMonitoredEnvironment();
      if (env != R_GlobalEnv && env == pContext->cloenv)
      {
         varJson["environment_name"] = functionName + "()";
         varJson["environment_is_local"] = true;
         inFunctionEnvironment = true;
      }

      if (pContext && functionName != "eval")
      {
         // see if the function to be debugged is out of sync with its saved
         // sources (if available).
         useProvidedSource =
               functionIsOutOfSync(pContext, &functionCode) &&
               functionCode != "NULL";
      }
      varJson["function_name"] = functionName;
   }
   else
   {
      varJson["function_name"] = "";
   }

   if (!inFunctionEnvironment)
   {
      // emit the name of the environment we're currently working with
      std::string environmentName;
      bool local = false;
      if (s_pEnvironmentMonitor->hasEnvironment())
      {
         Error error = r::exec::RFunction(".rs.environmentName",
                                    s_pEnvironmentMonitor->getMonitoredEnvironment())
                                    .call(&environmentName);
         if (error)
            LOG_ERROR(error);

         error = r::exec::RFunction(".rs.environmentIsLocal",
                                    s_pEnvironmentMonitor->getMonitoredEnvironment())
                                    .call(&local);
         if (error)
            LOG_ERROR(error);
      }
      varJson["environment_name"] = environmentName;
      varJson["environment_is_local"] = local;
   }

   // always emit the code for the function, even if we don't think that the
   // client's going to need it. we only checked the saved copy of the function
   // above; the client may be aware of local/unsaved changes to the function,
   // in which case it will need to fall back on a server-provided copy.
   varJson["use_provided_source"] = useProvidedSource;
   varJson["function_code"] = functionCode;

   return varJson;
}

void enqueContextDepthChangedEvent(int depth,
                                   LineDebugState* pLineDebugState)
{
   // emit an event to the client indicating the new call frame and the
   // current state of the environment
   ClientEvent event (client_events::kContextDepthChanged,
                      commonEnvironmentStateData(depth, pLineDebugState));
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
                      boost::shared_ptr<LineDebugState> pLineDebugState,
                      const json::JsonRpcRequest& request,
                      json::JsonRpcResponse*)
{
   // get the requested depth
   int requestedDepth;
   Error error = json::readParam(request.params, 0, &requestedDepth);
   if (error)
      return error;

   // set state for the new depth
   *pContextDepth = requestedDepth;
   SEXP env = NULL;
   getFunctionContext(requestedDepth, NULL, &env);
   s_pEnvironmentMonitor->setMonitoredEnvironment(env);

   // populate the new state on the client
   enqueContextDepthChangedEvent(*pContextDepth, pLineDebugState.get());

   return Success();
}

Error getEnvironmentState(boost::shared_ptr<int> pContextDepth,
                          boost::shared_ptr<LineDebugState> pLineDebugState,
                          const json::JsonRpcRequest&,
                          json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(commonEnvironmentStateData(*pContextDepth,
                                                   pLineDebugState.get()));
   return Success();
}

void onDetectChanges(module_context::ChangeSource source)
{
   // Prevent recursive calls to this function
   DROP_RECURSIVE_CALLS;

   s_pEnvironmentMonitor->checkForChanges();
}

void onConsolePrompt(boost::shared_ptr<int> pContextDepth,
                     boost::shared_ptr<LineDebugState> pLineDebugState,
                     boost::shared_ptr<bool> pCapturingDebugOutput,
                     boost::shared_ptr<RCNTXT*> pCurrentContext)
{
   // Prevent recursive calls to this function
   DROP_RECURSIVE_CALLS;

   int depth = 0;
   SEXP environmentTop = NULL;
   RCNTXT* pRContext = NULL;

   // End debug output capture every time a console prompt occurs
   *pCapturingDebugOutput = false;

   // If we were debugging but there's no longer a browser on the context stack,
   // switch back to the top level; otherwise, examine the stack and find the
   // first function there running user code.
   if (*pContextDepth > 0 && !inBrowseContext())
   {
      pRContext = r::getGlobalContext();
      environmentTop = R_GlobalEnv;
   }
   else
   {
      // If we're not currently debugging, look for user code (we prefer to
      // show the user their own code on entering debug), but once debugging,
      // allow the user to explore other code.
      pRContext =
             getFunctionContext(BROWSER_FUNCTION, &depth, &environmentTop);
   }

   if (environmentTop != s_pEnvironmentMonitor->getMonitoredEnvironment() ||
       depth != *pContextDepth ||
       pRContext != *pCurrentContext)
   {
      // if we appear to be switching into debug mode, make sure there's a
      // browser call somewhere on the stack. if there isn't, then we're
      // probably just waiting for user input inside a function (e.g. scan());
      // assume the user isn't interested in seeing the function's internals.
      if (*pContextDepth == 0 &&
          !inBrowseContext())
      {
         return;
      }

      // if we're leaving debug mode, clear out the debug state to prepare
      // for the next debug session
      if (*pContextDepth > 0 && depth == 0)
      {
         pLineDebugState->reset();
      }

      // start monitoring the enviroment at the new depth
      s_pEnvironmentMonitor->setMonitoredEnvironment(environmentTop);
      *pContextDepth = depth;
      *pCurrentContext = pRContext;
      enqueContextDepthChangedEvent(depth, pLineDebugState.get());
   }
   // if we're debugging and stayed in the same frame, update the line number
   else if (depth > 0)
   {
      // we don't want to send linenumber updates if the current depth is inside
      // a debug-hidden function
      if (!insideDebugHiddenFunction())
      {
         // check to see if we have real source references for the currently
         // executing context
         SEXP srcref = r::getGlobalContext()->srcref;
         if (!isValidSrcref(srcref))
         {
            // we don't, so reconstruct them from R output
            RCNTXT *firstFunContext = firstFunctionContext(
                     r::getGlobalContext());
            srcref = simulatedSourceRefsOfContext(firstFunContext, NULL,
                                                  pLineDebugState.get());
         }
         enqueBrowserLineChangedEvent(srcref);
      }
   }
}

void onBeforeExecute()
{
   // The client tracks busy state based on whether a console prompt has
   // been issued (because R doesn't reliably deliver non-busy state) --
   // i.e. when a console prompt occurs the client leaves busy state.
   // During debugging the busy state is therefore exited as soon as a
   // Browse> prompt is hit. This is often not a problem as the debug
   // stop command will interrupt R if necessary. However, in the case
   // where the Next or Continue command results in R running without
   // hitting another breakpoint we've essentially lost the busy state.
   //
   // This handler (which executes right before console input is returned
   // to R) checks whether we are in the Browser and if so re-raises the
   // busy event to indicate that R is now back in a busy state. The busy
   // state will be immediately cleared if another Browse> prompt is hit
   // however if R continues running then the client will properly restore
   // the state of the interruptR command

   if (inBrowseContext())
   {
      ClientEvent event(client_events::kBusy, true);
      module_context::enqueClientEvent(event);
   }
}

Error getEnvironmentNames(boost::shared_ptr<int> pContextDepth,
                          boost::shared_ptr<RCNTXT*> pCurrentContext,
                          const json::JsonRpcRequest&,
                          json::JsonRpcResponse* pResponse)
{
   // If looking at a non-toplevel context, start from there; otherwise, start
   // from the global environment.
   SEXP env = *pContextDepth > 0 ?
                  (*pCurrentContext)->cloenv :
                  R_GlobalEnv;
   pResponse->setResult(environmentNames(env));
   return Success();
}

void initEnvironmentMonitoring()
{
   // Check to see whether we're actively debugging. If we are, the debug
   // environment trumps whatever the user wants to browse in at the top level.
   int contextDepth = 0;
   RCNTXT* pContext = getFunctionContext(BROWSER_FUNCTION, &contextDepth);
   if (contextDepth == 0 ||
       !inBrowseContext())
   {
      // Not actively debugging; see if we have a stored environment name to
      // begin monitoring.
      std::string envName = persistentState().activeEnvironmentName();
      if (!envName.empty())
      {
         // It's possible for this to fail if the environment we were
         // monitoring doesn't exist any more. If this is the case, reset
         // the monitor to the global environment.
         Error error = setEnvironmentName(contextDepth, pContext, envName);
         if (error)
         {
            persistentState().setActiveEnvironmentName("R_GlobalEnv");
         }
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
                        s_pEnvironmentMonitor->getMonitoredEnvironment()).call();
   if (error)
      return error;

   return Success();
}

// Remove all the objects from the currently monitored environment.
Error removeAllObjects(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   bool includeHidden = false;
   Error error = json::readParam(request.params, 0, &includeHidden);
   if (error)
      return error;

   error = r::exec::RFunction(".rs.removeAllObjects",
                        includeHidden,
                        s_pEnvironmentMonitor->getMonitoredEnvironment()).call();
   if (error)
      return error;

   return Success();
}

// Return the contents of the given object. Called on-demand by the client when
// the object is large enough that we don't want to get its contents
// immediately (i.e. as part of environmentListAsJson)
Error getObjectContents(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)

{
   std::string objectName;
   r::sexp::Protect protect;
   SEXP objContents;
   json::Value contents;
   Error error = json::readParam(request.params, 0, &objectName);
   if (error)
      return error;
   error = r::exec::RFunction(".rs.getObjectContents",
                              objectName,
                              s_pEnvironmentMonitor->getMonitoredEnvironment())
                              .call(&objContents, &protect);
   if (error)
      return error;

   error = r::json::jsonValueFromObject(objContents, &contents);
   if (error)
      return error;

   json::Object result;
   result["contents"] = contents;
   pResponse->setResult(result);
   return Success();
}

// Called by the client to force a re-query of the currently monitored
// context depth and environment.
Error requeryContext(boost::shared_ptr<int> pContextDepth,
                     boost::shared_ptr<LineDebugState> pLineDebugState,
                     boost::shared_ptr<RCNTXT*> pCurrentContext,
                     const json::JsonRpcRequest&,
                     json::JsonRpcResponse*)
{
   onConsolePrompt(pContextDepth, pLineDebugState,
                   boost::make_shared<bool>(false), pCurrentContext);
   return Success();
}

// Stores the last "debug: " R output. Used to reconstruct source references
// when unavailable (see simulatedSourceRefsOfContext).
void onConsoleOutput(boost::shared_ptr<LineDebugState> pLineDebugState,
                     boost::shared_ptr<bool> pCapturingDebugOutput,
                     module_context::ConsoleOutputType type,
                     const std::string& output)
{
   if (*pCapturingDebugOutput)
   {
      // stop capturing output if non-normal output occurs
      if (type != module_context::ConsoleOutputNormal)
      {
         *pCapturingDebugOutput = false;
         return;
      }
      pLineDebugState->lastDebugText.append(output);
   }
   else if (type == module_context::ConsoleOutputNormal &&
       output == "debug: ")
   {
      // start capturing debug output when R outputs "debug: "
      pLineDebugState->lastDebugText = "";
      *pCapturingDebugOutput = true;
   }
}


SEXP rs_jumpToFunction(SEXP file, SEXP line, SEXP col) 
{
   json::Object funcLoc;
   FilePath path(r::sexp::safeAsString(file));
   funcLoc["file_name"] = module_context::createAliasedPath(path);
   funcLoc["line_number"] = r::sexp::asInteger(line);
   funcLoc["column_number"] = r::sexp::asInteger(col);
   ClientEvent jumpEvent(client_events::kJumpToFunction, funcLoc);
   module_context::enqueClientEvent(jumpEvent);
   return R_NilValue;
}

} // anonymous namespace

json::Value environmentStateAsJson()
{
   int contextDepth = 0;
   getFunctionContext(BROWSER_FUNCTION, &contextDepth);
   // If there's no browser on the stack, stay at the top level even if
   // there are functions on the stack--this is not a user debug session.
   if (!inBrowseContext())
      contextDepth = 0;
   return commonEnvironmentStateData(contextDepth, NULL);
}

SEXP rs_isBrowserActive()
{
   r::sexp::Protect protect;
   return r::sexp::create(s_browserActive, &protect);
}

Error initialize()
{
   // store on the heap so that the destructor is never called (so we
   // don't end up releasing the underlying environment SEXP after
   // R has already shut down / deinitialized)
   s_pEnvironmentMonitor = new EnvironmentMonitor();

   boost::shared_ptr<int> pContextDepth =
         boost::make_shared<int>(0);
   boost::shared_ptr<RCNTXT*> pCurrentContext =
         boost::make_shared<RCNTXT*>(r::getGlobalContext());

   // functions that emit call frames also emit source references; these
   // values capture and supply the currently executing expression emitted by R
   // for the purpose of reconstructing references when none are present.
   boost::shared_ptr<LineDebugState> pLineDebugState =
         boost::make_shared<LineDebugState>();
   boost::shared_ptr<bool> pCapturingDebugOutput =
         boost::make_shared<bool>(false);

   r::routines::registerCallMethod(
            "rs_isBrowserActive",
            (DL_FUNC) rs_isBrowserActive,
            0);

   R_CallMethodDef methodDef ;
   methodDef.name = "rs_jumpToFunction" ;
   methodDef.fun = (DL_FUNC) rs_jumpToFunction ;
   methodDef.numArgs = 3;
   r::routines::addCallMethod(methodDef);

   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onConsolePrompt.connect(bind(onConsolePrompt,
                                         pContextDepth,
                                         pLineDebugState,
                                         pCapturingDebugOutput,
                                         pCurrentContext));
   events().onBeforeExecute.connect(onBeforeExecute);
   events().onConsoleOutput.connect(bind(onConsoleOutput,
                                         pLineDebugState,
                                         pCapturingDebugOutput, _1, _2));

   json::JsonRpcFunction listEnv =
         boost::bind(listEnvironment, pContextDepth, _1, _2);
   json::JsonRpcFunction setCtxDepth =
         boost::bind(setContextDepth, pContextDepth, pLineDebugState,
                     _1, _2);
   json::JsonRpcFunction getEnv =
         boost::bind(getEnvironmentState, pContextDepth, pLineDebugState,
                     _1, _2);
   json::JsonRpcFunction getEnvNames =
         boost::bind(getEnvironmentNames, pContextDepth, pCurrentContext,
                     _1, _2);
   json::JsonRpcFunction setEnvName =
         boost::bind(setEnvironment, pContextDepth, pCurrentContext,
                     _1, _2);
   json::JsonRpcFunction requeryCtx =
         boost::bind(requeryContext, pContextDepth, pLineDebugState,
                     pCurrentContext, _1, _2);

   initEnvironmentMonitoring();

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRBrowseFileHandler, handleRBrowseEnv))
      (bind(registerRpcMethod, "list_environment", listEnv))
      (bind(registerRpcMethod, "set_context_depth", setCtxDepth))
      (bind(registerRpcMethod, "set_environment", setEnvName))
      (bind(registerRpcMethod, "set_environment_frame", setEnvironmentFrame))
      (bind(registerRpcMethod, "get_environment_names", getEnvNames))
      (bind(registerRpcMethod, "remove_objects", removeObjects))
      (bind(registerRpcMethod, "remove_all_objects", removeAllObjects))
      (bind(registerRpcMethod, "get_environment_state", getEnv))
      (bind(registerRpcMethod, "get_object_contents", getObjectContents))
      (bind(registerRpcMethod, "requery_context", requeryCtx))
      (bind(sourceModuleRFile, "SessionEnvironment.R"));

   return initBlock.execute();
}
   
} // namespace environment
} // namespace modules
} // namesapce session
} // namespace rstudio

