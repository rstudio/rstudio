/*
 * SessionUserCommands.cpp
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

#include "SessionUserCommands.hpp"

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>

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

json::Array jsonFromUserCommandResult(SEXP resultSEXP)
{
   json::Array jsonData;
   if (TYPEOF(resultSEXP) != VECSXP)
      return jsonData;
   
   Error error;
   std::size_t n = r::sexp::length(resultSEXP);
   
   for (std::size_t i = 0; i < n; ++i)
   {
      json::Object jsonResult;
      SEXP elementSEXP = VECTOR_ELT(resultSEXP, i);
      
      std::string action;
      error = r::sexp::getNamedListElement(elementSEXP, "action", &action);
      if (error)
      {
         LOG_ERROR(error);
         return json::Array();
      }
      jsonResult["action"] = action;
      
      std::vector<int> range;
      error = r::sexp::getNamedListElement(elementSEXP, "range", &range);
      if (error)
      {
         LOG_ERROR(error);
         return json::Array();
      }
      
      jsonResult["range"] = json::toJsonArray(range);
      
      std::string text;
      error = r::sexp::getNamedListElement(elementSEXP, "text", &text);
      if (error)
      {
         LOG_ERROR(error);
         return json::Array();
      }
      jsonResult["text"]  = text;
      
      jsonData.push_back(jsonResult);
   }
   return jsonData;
}

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
   // Ensure response is empty array on failure
   Error error;
   pResponse->setResult(json::Array());
   
   // Read JSON params
   std::string name;
   json::Array contentJson;
   int rowStart;
   int columnStart;
   int rowEnd;
   int columnEnd;
   error = json::readParams(request.params,
                            &name,
                            &contentJson,
                            &rowStart,
                            &columnStart,
                            &rowEnd,
                            &columnEnd);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   std::vector<std::string> content;
   if (!json::fillVectorString(contentJson, &content))
      return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
   
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
   userCommand.addParam(content);
   userCommand.addParam(rowStart);
   userCommand.addParam(columnStart);
   userCommand.addParam(rowEnd);
   userCommand.addParam(columnEnd);
   
   error = userCommand.call(&resultSEXP, &protect);
   if (error)
      return error;
   
   // Create JSON data from result
   json::Array jsonData = jsonFromUserCommandResult(resultSEXP);
   
   pResponse->setResult(jsonData);
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

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;
   
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
