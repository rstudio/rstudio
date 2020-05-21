/*
 * SessionUserCommands.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

// #define RSTUDIO_ENABLE_DEBUG_MACROS
#define RSTUDIO_DEBUG_LABEL "commands"
#include <core/Macros.hpp>

#include "SessionUserCommands.hpp"

#include <boost/bind.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <core/system/Xdg.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace user_commands {

using namespace rstudio::core;

namespace {

Error noSuchSymbol(const std::string symbol, const ErrorLocation& location)
{
   Error error(r::errc::SymbolNotFoundError, location);
   error.addProperty("symbol", symbol);
   LOG_ERROR(error);
   return error;
}

Error executeUserCommand(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // Read JSON params
   std::string name;
   Error error = json::readParams(request.params, &name);
   
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // Locate the function with this name
   SEXP userCommandsEnvSEXP = r::sexp::findVar(".rs.userCommands", R_GlobalEnv);
   if (userCommandsEnvSEXP == R_UnboundValue)
      return noSuchSymbol(".rs.userCommands", ERROR_LOCATION);
   
   // Get the function bound in this environment
   SEXP fnSEXP = r::sexp::findVar(name, userCommandsEnvSEXP);
   if (fnSEXP == R_UnboundValue)
      return noSuchSymbol(name, ERROR_LOCATION);
   
   // Execute function
   r::sexp::Protect protect;
   SEXP resultSEXP = R_NilValue;
   
   r::exec::RFunction userCommand(fnSEXP);
   error = userCommand.call(&resultSEXP, &protect);
   
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   return Success();
}

SEXP rs_registerUserCommand(SEXP nameSEXP, SEXP shortcutsSEXP)
{
   std::string name = r::sexp::safeAsString(nameSEXP);
   
   std::vector<std::string> shortcuts;
   if (!r::sexp::fillVectorString(shortcutsSEXP, &shortcuts))
      return R_NilValue;
   
   json::Object jsonData;
   jsonData["name"] = name;
   jsonData["shortcuts"] = json::toJsonArray(shortcuts);
   
   ClientEvent event(client_events::kRegisterUserCommand, jsonData);
   module_context::enqueClientEvent(event);
   
   return R_NilValue;
}

void onDeferredInit(bool newSession)
{
   r::exec::RFunction loadUserCommands(".rs.loadUserCommands");
   loadUserCommands.addParam("keybindingPath", 
         core::system::xdg::userConfigDir().completePath("keybindings").getAbsolutePath());
         
   Error error = loadUserCommands.call();
   if (error)
      LOG_ERROR(error);
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;
   
   events().onDeferredInit.connect(onDeferredInit);
   
   RS_REGISTER_CALL_METHOD(rs_registerUserCommand, 2);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionUserCommands.R"))
         (bind(registerRpcMethod, "execute_user_command", executeUserCommand))
   ;
   return initBlock.execute();
}

} // namespace user_commands
} // namespace modules
} // namespace session
} // namespace rstudio
