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
#include <session/SessionUserSettings.hpp>
#include "SessionErrors.hpp"

using namespace core;

namespace session {
namespace modules {
namespace errors {
namespace {

void enqueErrorHandlerChanged(int type)
{
   json::Object errorHandlerType;
   errorHandlerType["type"] = type;
   ClientEvent errorHandlerChanged(
            client_events::kErrorHandlerChanged, errorHandlerType);
   module_context::enqueClientEvent(errorHandlerChanged);
}

Error setErrHandler(int type, bool inMyCode,
                    boost::shared_ptr<SEXP> pErrorHandler)
{
   // clear the previous error handler; if we don't do this, the error handler
   // we set will be unset by DisableErrorHandlerScope during call evaluation
   r::options::setErrorOption(R_NilValue);

   Error error = r::exec::RFunction(
            ".rs.setErrorManagementType", type, inMyCode).call();
   if (error)
      return error;

   *pErrorHandler = r::options::getOption("error");
   return Success();
}

Error setErrHandlerType(int type,
                        boost::shared_ptr<SEXP> pErrorHandler)
{
   Error error = setErrHandler(type,
                               userSettings().handleErrorsInUserCodeOnly(),
                               pErrorHandler);
   if (error)
      return error;

   userSettings().setErrorHandlerType(type);
   enqueErrorHandlerChanged(type);
   return Success();
}

Error setErrHandlerType(boost::shared_ptr<SEXP> pErrorHandler,
                        const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   int type = 0;
   Error error = json::readParams(request.params, &type);
   if (error)
      return error;

   return setErrHandlerType(type, pErrorHandler);
}

// Initialize the error handler to the one that the user specified. Note that
// this initialization routine runs *before* .Rprofile is sourced, so any error
// handler set in .Rprofile will trump this one.
Error initializeErrManagement(boost::shared_ptr<SEXP> pErrorHandler,
                              boost::shared_ptr<bool> pHandleUserErrorsOnly)
{
   SEXP currentHandler = r::options::getOption("error");
   *pHandleUserErrorsOnly = userSettings().handleErrorsInUserCodeOnly();
   if (currentHandler == R_NilValue)
      setErrHandlerType(userSettings().errorHandlerType(), pErrorHandler);
   return Success();
}

void detectHandlerChange(boost::shared_ptr<SEXP> pErrorHandler)
{
   // check to see if the error option has been changed from beneath us; if
   // it has, emit a client event so the client doesn't show an incorrect
   // error handler
   SEXP currentHandler = r::options::getOption("error");
   if (currentHandler != *pErrorHandler)
   {
      *pErrorHandler = currentHandler;
      enqueErrorHandlerChanged(currentHandler == R_NilValue ?
                                  ERRORS_MESSAGE:
                                  ERRORS_CUSTOM);
   }
}

void onUserSettingsChanged(boost::shared_ptr<SEXP> pErrorHandler,
                           boost::shared_ptr<bool> pHandleUserErrorsOnly)
{
   // check to see if the setting for 'handle errors in user code only'
   // has been changed since we last looked at it; if it has, switch the
   // error handler in a corresponding way
   bool handleUserErrorsOnly = userSettings().handleErrorsInUserCodeOnly();
   if (handleUserErrorsOnly != *pHandleUserErrorsOnly)
   {
      Error error = setErrHandler(userSettings().errorHandlerType(),
                                  handleUserErrorsOnly,
                                  pErrorHandler);
      if (error)
         LOG_ERROR(error);

      *pHandleUserErrorsOnly = handleUserErrorsOnly;
   }
}

} // anonymous namespace

json::Value errorStateAsJson()
{
   json::Object state;
   state["error_handler_type"] = userSettings().errorHandlerType();
   return state;
}

Error initialize()
{
   boost::shared_ptr<SEXP> pErrorHandler =
         boost::make_shared<SEXP>(R_NilValue);
   boost::shared_ptr<bool> pHandleUserErrorsOnly =
         boost::make_shared<bool>(true);

   using boost::bind;
   using namespace module_context;

   // Check to see whether the error handler has changed immediately after init
   // (to find changes from e.g. .Rprofile) and after every console prompt
   events().onConsolePrompt.connect(bind(detectHandlerChange,
                                         pErrorHandler));
   events().onDeferredInit.connect(bind(detectHandlerChange,
                                        pErrorHandler));
   userSettings().onChanged.connect(bind(onUserSettingsChanged,
                                         pErrorHandler,
                                         pHandleUserErrorsOnly));

   json::JsonRpcFunction setErrMgmt =
         bind(setErrHandlerType, pErrorHandler, _1, _2);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_error_management_type", setErrMgmt))
      (bind(sourceModuleRFile, "SessionErrors.R"))
      (bind(initializeErrManagement, pErrorHandler, pHandleUserErrorsOnly));

   return initBlock.execute();
}

} // namepsace errors
} // namespace modules
} // namesapce session


