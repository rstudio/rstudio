/*
 * SessionErrors.cpp
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

#include <algorithm>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>

#include <boost/bind.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules {
namespace errors {
namespace {

// Error handler types understood by the client
const int ERRORS_MESSAGE = 0;
const int ERRORS_TRACEBACK = 1;
const int ERRORS_BREAK = 2;
const int ERRORS_CUSTOM = 3;

void enqueErrorHandlerChanged(int type)
{
   json::Object errorHandlerType;
   errorHandlerType["type"] = type;
   ClientEvent errorHandlerChanged(
            client_events::kErrorHandlerChanged, errorHandlerType);
   module_context::enqueClientEvent(errorHandlerChanged);
}

Error setErrHandlerType(int type, bool inMyCode,
                        boost::shared_ptr<SEXP> pErrorHandler)
{
   // clear the previous error handler; if we don't do this, the error handler
   // we set will be unset by DisableErrorHandlerScope during call evaluation
   r::options::setErrorOption(R_NilValue);

   Error error = r::exec::RFunction(
            ".rs.setErrorManagementType", type, inMyCode)
           .call();
   if (error)
      return error;

   *pErrorHandler = r::options::getOption("error");
   return Success();
}

Error setErrManagement(boost::shared_ptr<SEXP> pErrorHandler,
                       const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   int type = 0;
   bool inMyCode = false;
   Error error = json::readParams(request.params, &type, &inMyCode);
   if (error)
      return error;

   error = setErrHandlerType(type, inMyCode, pErrorHandler);
   if (error)
      return error;

   enqueErrorHandlerChanged(type);
   return Success();
}

Error initializeErrManagement(boost::shared_ptr<SEXP> pErrorHandler)
{
   SEXP currentHandler = r::options::getOption("error");
   if (currentHandler == R_NilValue)
      setErrHandlerType(ERRORS_TRACEBACK, true, pErrorHandler);
   return Success();
}

void onConsolePrompt(boost::shared_ptr<SEXP> pErrorHandler)
{
   // check to see if the error option has been changed from beneath us; if it
   // has, emit a client event so the client doesn't show an incorrect error
   // handler
   SEXP currentHandler = r::options::getOption("error");
   if (currentHandler != *pErrorHandler)
   {
      *pErrorHandler = currentHandler;
      enqueErrorHandlerChanged(currentHandler == R_NilValue ?
                                  ERRORS_MESSAGE :
                                  ERRORS_CUSTOM);
   }
}

} // anonymous namespace

Error initialize()
{
   boost::shared_ptr<SEXP> pErrorHandler =
         boost::make_shared<SEXP>(R_NilValue);

   using boost::bind;
   using namespace module_context;

   events().onConsolePrompt.connect(bind(onConsolePrompt,
                                         pErrorHandler));
   json::JsonRpcFunction setErrMgmt =
         boost::bind(setErrManagement, pErrorHandler, _1, _2);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_error_management_type", setErrMgmt))
      (bind(sourceModuleRFile, "SessionErrors.R"))
      (bind(initializeErrManagement, pErrorHandler));

   return initBlock.execute();
}


} // namepsace errors
} // namespace modules
} // namesapce session


