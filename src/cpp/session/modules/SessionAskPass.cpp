/*
 * SessionAskPass.cpp
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

#include "SessionAskPass.hpp"

#include <boost/bind.hpp>

#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <core/json/Json.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif


using namespace rstudio::core ;

namespace rstudio {
namespace session {
namespace modules { 
namespace ask_pass {

namespace {

std::string s_askPassWindow;
module_context::WaitForMethodFunction s_waitForAskPass;

void onClientInit()
{
   s_askPassWindow = "";
}


// show error message from R
SEXP rs_askForPassword(SEXP promptSEXP)
{
   try
   {
      std::string prompt = r::sexp::asString(promptSEXP);

      PasswordInput input;
      Error error = askForPassword(prompt, "", &input);
      if (error)
      {
         LOG_ERROR(error);
         return R_NilValue;
      }
      else if (input.cancelled || input.password.empty())
      {
         return R_NilValue;
      }
      else
      {
         r::sexp::Protect rProtect;
         return r::sexp::create(input.password, &rProtect);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

} // anonymous namespace

std::string activeWindow()
{
   return s_askPassWindow;
}

void setActiveWindow(const std::string& window)
{
   s_askPassWindow = window;
}

Error askForPassword(const std::string& prompt,
                     const std::string& rememberPrompt,
                     PasswordInput* pInput)
{
   json::Object payload;
   payload["prompt"] = prompt;
   payload["remember_prompt"] = rememberPrompt;
   payload["window"] = s_askPassWindow;
   ClientEvent askPassEvent(client_events::kAskPass, payload);

   // wait for method
   core::json::JsonRpcRequest request;
   if (!s_waitForAskPass(&request, askPassEvent))
   {
      return systemError(boost::system::errc::operation_canceled,
                         ERROR_LOCATION);
   }

   // read params
   json::Value value;
   bool remember = false;
   Error error = json::readParams(request.params, &value, &remember);
   if (error)
      return error;

   // null passphrase means dialog was cancelled
   if (!json::isType<std::string>(value))
   {
      pInput->cancelled = true;
      return Success();
   }

   // read inputs
   pInput->remember = remember;
   pInput->password = value.get_value<std::string>();

   // decrypt if necessary
#ifdef RSTUDIO_SERVER
   if (options().programMode() == kSessionProgramModeServer)
   {
      // In server mode, passphrases are encrypted
      error = core::system::crypto::rsaPrivateDecrypt(
                                             pInput->password,
                                             &pInput->password);
      if (error)
         return error;
   }
#endif

   return Success();
}


Error initialize()
{
   module_context::events().onClientInit.connect(onClientInit);

   // register waitForMethod handler
   s_waitForAskPass = module_context::registerWaitForMethod(
                                                "askpass_completed");

   // register rs_askForPassword with R
   R_CallMethodDef methodDefAskPass ;
   methodDefAskPass.name = "rs_askForPassword" ;
   methodDefAskPass.fun = (DL_FUNC) rs_askForPassword ;
   methodDefAskPass.numArgs = 1;
   r::routines::addCallMethod(methodDefAskPass);

   // complete initialization
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "SessionAskPass.R"));
   return initBlock.execute();

}
   
   
} // namespace content_urls
} // namespace modules
} // namespace session
} // namespace rstudio

