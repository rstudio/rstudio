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
#include "EnvironmentState.hpp"

#include <algorithm>

#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/session/RSession.hpp>
#include <r/RInterface.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace environment {

namespace {

json::Object varToJson(const r::sexp::Variable& var)
{
   json::Object varJson;
   varJson["name"] = var.first;
   SEXP varSEXP = var.second;
   varJson["type"] = r::sexp::typeAsString(varSEXP);
   varJson["len"] = r::sexp::length(varSEXP);
   return varJson;
}

Error listEnvironment(const json::JsonRpcRequest&,
                      json::JsonRpcResponse* pResponse,
                      boost::shared_ptr<bool> pInBrowse)
{
   using namespace r::sexp;
   Protect rProtect;
   std::vector<Variable> vars;
   RCNTXT* pRContext = R_getGlobalContext();
   SEXP* pEnv = &R_GlobalEnv;
   if (pRContext->evaldepth > 0)
   {
       while (!(pRContext->callflag & CTXT_FUNCTION) && pRContext->callflag)
       {
           pRContext = pRContext->nextcontext;
       }
       pEnv = &(pRContext->cloenv);
   }
   listEnvironment(*pEnv, true, &rProtect, &vars);

   // get object details and transform to json
   json::Array listJson;
   std::transform(vars.begin(),
                  vars.end(),
                  std::back_inserter(listJson),
                  varToJson);

   // return list
   pResponse->setResult(listJson);
   return Success();
}


void onDetectChanges(module_context::ChangeSource source)
{

}

void onConsolePrompt(boost::shared_ptr<bool> pInBrowse)
{
    bool browserContextActive = r::session::browserContextActive();
    if (*pInBrowse != browserContextActive)
    {
        *pInBrowse = browserContextActive;
        ClientEvent event (client_events::kBrowseModeChanged);
        module_context::enqueClientEvent(event);
    }
}

} // anonymous namespace

json::Value environmentStateAsJson()
{
   return environment::state::asJson();
}

Error initialize()
{
   boost::shared_ptr<bool> pInBrowse = boost::make_shared<bool>(false);

   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   events().onConsolePrompt.connect(bind(onConsolePrompt, pInBrowse));

   json::JsonRpcFunction listEnv = boost::bind(listEnvironment, _1, _2, pInBrowse);

   // source R functions
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(environment::state::initialize))
      (bind(registerRpcMethod, "list_environment", listEnv))
      (bind(sourceModuleRFile, "SessionEnvironment.R"));

   return initBlock.execute();
}
   
} // namespace environment
} // namespace modules
} // namesapce session

