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

#include <r/RSexp.hpp>
#include <r/RExec.hpp>
#include <r/session/RSession.hpp>
#include <r/RInterface.hpp>
#include <session/SessionModuleContext.hpp>

#include "EnvironmentUtils.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace environment {

EnvironmentMonitor s_environmentMonitor;

namespace {

// return the context for the topmost function on the callstack
RCNTXT* getTopFunctionContext()
{
   RCNTXT* pRContext = r::getGlobalContext();
   while (!(pRContext->callflag & CTXT_FUNCTION) && pRContext->callflag)
   {
      pRContext = pRContext->nextcontext;
   }
   return pRContext;
}

SEXP getTopFunctionEnvironment()
{
   // if we're in an eval tree (i.e. browser), return the environment of the
   // fuction on top of the stack; otherwise, return the global environment
   return r::getGlobalContext()->evaldepth > 0 ?
            getTopFunctionContext()->cloenv :
            R_GlobalEnv;
}

json::Array callFramesAsJson()
{
   RCNTXT* pRContext = r::getGlobalContext();
   json::Array listFrames;
   while (pRContext->callflag)
   {
      if (pRContext->callflag & CTXT_FUNCTION)
      {
         json::Object varFrame;
         varFrame["eval_depth"] = pRContext->evaldepth;
         varFrame["function_name"] =
               r::sexp::asString(PRINTNAME(CAR(pRContext->call)));
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
    listEnvironment(getTopFunctionEnvironment(), false, &rProtect, &vars);

    // get object details and transform to json
    json::Array listJson;
    std::transform(vars.begin(),
                   vars.end(),
                   std::back_inserter(listJson),
                   varToJson);
    return listJson;
}

Error listEnvironment(const json::JsonRpcRequest&,
                      json::JsonRpcResponse* pResponse)
{
   // return list
   pResponse->setResult(environmentListAsJson());
   return Success();
}


void onDetectChanges(module_context::ChangeSource source)
{
   s_environmentMonitor.checkForChanges();
}

std::string getTopFunctionName()
{
   return r::sexp::asString(PRINTNAME(CAR(getTopFunctionContext()->call)));
}

void onConsolePrompt(boost::shared_ptr<int> pContextDepth)
{
   int contextDepth = getTopFunctionContext()->evaldepth;

   // we entered (or left) a call frame
   if (*pContextDepth != contextDepth)
   {
      *pContextDepth = contextDepth;
      json::Object varJson;

      // start monitoring the enviroment at the new depth
      s_environmentMonitor.setMonitoredEnvironment(getTopFunctionEnvironment());

      // emit an event to the client indicating the new call frame and the
      // current state of the environment
      varJson["context_depth"] = contextDepth;
      varJson["environment_list"] = environmentListAsJson();
      varJson["call_frames"] = callFramesAsJson();
      varJson["function_name"] = getTopFunctionName();

      ClientEvent event (client_events::kContextDepthChanged, varJson);
      module_context::enqueClientEvent(event);
   }
}

} // anonymous namespace

json::Value environmentStateAsJson()
{
   json::Object stateJson;
   stateJson["context_depth"] = getTopFunctionContext()->evaldepth;
   stateJson["function_name"] = getTopFunctionName();
   return stateJson;
}

Error initialize()
{
   boost::shared_ptr<int> pContextDepth = boost::make_shared<int>(0);

   // begin monitoring the global environment
   s_environmentMonitor.setMonitoredEnvironment(getTopFunctionEnvironment());

   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onConsolePrompt.connect(bind(onConsolePrompt, pContextDepth));

   json::JsonRpcFunction listEnv = boost::bind(listEnvironment, _1, _2);

   // source R functions
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "list_environment", listEnv))
      (bind(sourceModuleRFile, "SessionEnvironment.R"));

   return initBlock.execute();
}
   
} // namespace environment
} // namespace modules
} // namesapce session

