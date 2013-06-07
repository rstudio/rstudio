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
#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/session/RSession.hpp>
#include <r/RInterface.hpp>
#include <session/SessionModuleContext.hpp>

#include "EnvironmentUtils.hpp"

#define TOP_FUNCTION 1

using namespace core ;

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

// given a context from the context stack, indicate whether it is executing a
// user-defined function
bool isUserFunctionContext(RCNTXT *pContext)
{
   return (pContext->callflag & CTXT_FUNCTION) &&
         pContext->srcref &&
         r::sexp::asInteger(pContext->srcref) > 0;
}

// return the function context at the given depth
RCNTXT* getFunctionContext(const int depth,
                           int* pFoundDepth = NULL,
                           SEXP* pEnvironment = NULL)
{
   RCNTXT* pRContext = r::getGlobalContext();
   int currentDepth = 0;
   while (pRContext->callflag)
   {
      if (isUserFunctionContext(pRContext))
      {
         if (++currentDepth == depth)
         {
            break;
         }
      }
      pRContext = pRContext->nextcontext;
   }
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

SEXP getEnvironment(const int depth)
{
   return depth == 0 ? R_GlobalEnv : getFunctionContext(depth)->cloenv;
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

std::string functionNameFromContext(RCNTXT* pContext)
{
   return r::sexp::asString(PRINTNAME(CAR(pContext->call)));
}

std::string getFunctionName(int depth)
{
   return functionNameFromContext(getFunctionContext(depth));
}

void getSourceRefFromContext(const RCNTXT* pContext,
                             std::string* pFileName,
                             int* pLineNumber)
{
   SEXP srcref = pContext->srcref;
   *pLineNumber = r::sexp::asInteger(srcref);
   Error error = r::exec::RFunction(".rs.sourceFileFromRef", srcref)
                 .call(pFileName);
   if (error)
   {
      LOG_ERROR(error);
   }
   return;
}

json::Array callFramesAsJson()
{
   RCNTXT* pRContext = r::getGlobalContext();
   RCNTXT* pSrcContext = pRContext;
   json::Array listFrames;
   int contextDepth = 0;

   while (pRContext->callflag)
   {
      if (isUserFunctionContext(pRContext))
      {
         json::Object varFrame;
         varFrame["context_depth"] = ++contextDepth;
         varFrame["function_name"] = functionNameFromContext(pRContext);

         // in the linked list of R contexts, the srcref associated with each
         // context points to the place from which the context was invoked.
         // however, for traditional debugging, we want the call frame to show
         // where control *left* the frame to go to the next frame. pSrcContext
         // keeps track of the previous invocation.
         std::string filename;
         int lineNumber = 0;
         getSourceRefFromContext(pSrcContext, &filename, &lineNumber);
         varFrame["file_name"] = filename;
         varFrame["line_number"] = lineNumber;
         pSrcContext = pRContext;

         std::string argList;
         Error error = r::exec::RFunction(".rs.argumentListSummary",
                                    CDR(pRContext->call)).call(&argList);
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

json::Array environmentListAsJson(int depth)
{
    using namespace r::sexp;
    Protect rProtect;
    std::vector<Variable> vars;
    listEnvironment(getEnvironment(depth), false, &rProtect, &vars);

    // get object details and transform to json
    json::Array listJson;
    std::transform(vars.begin(),
                   vars.end(),
                   std::back_inserter(listJson),
                   varToJson);
    return listJson;
}

Error listEnvironment(boost::shared_ptr<int> pContextDepth,
                      const json::JsonRpcRequest&,
                      json::JsonRpcResponse* pResponse)
{
   // return list
   pResponse->setResult(environmentListAsJson(*pContextDepth));
   return Success();
}

void enqueContextDepthChangedEvent(int depth)
{
   json::Object varJson;

   // emit an event to the client indicating the new call frame and the
   // current state of the environment
   varJson["context_depth"] = depth;
   varJson["environment_list"] = environmentListAsJson(depth);
   varJson["call_frames"] = callFramesAsJson();
   varJson["function_name"] = getFunctionName(depth);

   ClientEvent event (client_events::kContextDepthChanged, varJson);
   module_context::enqueClientEvent(event);
}

void enqueBrowserLineChangedEvent(int newLineNumber)
{
   json::Object varJson;
   varJson["line_number"] = newLineNumber;
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

   // populate the new state on the client
   enqueContextDepthChangedEvent(*pContextDepth);

   return Success();
}


void onDetectChanges(module_context::ChangeSource source)
{
   s_environmentMonitor.checkForChanges();
}

void onConsolePrompt(boost::shared_ptr<int> pContextDepth)
{
   int depth = 0;
   SEXP environmentTop = NULL;
   getFunctionContext(TOP_FUNCTION, &depth, &environmentTop);

   // we entered (or left) a call frame
   if (environmentTop != s_environmentMonitor.getMonitoredEnvironment() ||
       depth != *pContextDepth)
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
      enqueContextDepthChangedEvent(depth);
   }
   // if we're debugging and stayed in the same frame, update the line number
   else if (depth > 0)
   {
      int lineNumber = r::sexp::asInteger(r::getGlobalContext()->srcref);
      enqueBrowserLineChangedEvent(lineNumber);
   }
}


} // anonymous namespace

json::Value environmentStateAsJson()
{
   json::Object stateJson;
   int contextDepth = 0;
   RCNTXT* pContext = getFunctionContext(TOP_FUNCTION, &contextDepth);
   stateJson["context_depth"] = contextDepth;
   stateJson["function_name"] = functionNameFromContext(pContext);
   stateJson["call_frames"] = callFramesAsJson();
   return stateJson;
}

Error initialize()
{
   boost::shared_ptr<int> pContextDepth = boost::make_shared<int>(0);

   // begin monitoring the environment
   SEXP environmentTop = NULL;
   getFunctionContext(TOP_FUNCTION, pContextDepth.get(), &environmentTop);
   s_environmentMonitor.setMonitoredEnvironment(environmentTop);

   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onConsolePrompt.connect(bind(onConsolePrompt, pContextDepth));

   json::JsonRpcFunction listEnv =
         boost::bind(listEnvironment, pContextDepth, _1, _2);
   json::JsonRpcFunction setCtxDepth =
         boost::bind(setContextDepth, pContextDepth, _1, _2);

   // source R functions
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRBrowseFileHandler, handleRBrowseEnv))
      (bind(registerRpcMethod, "list_environment", listEnv))
      (bind(registerRpcMethod, "set_context_depth", setCtxDepth))
      (bind(sourceModuleRFile, "SessionEnvironment.R"));

   return initBlock.execute();
}
   
} // namespace environment
} // namespace modules
} // namesapce session

