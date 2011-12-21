/*
 * SessionAskPass.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionAskPass.hpp"

#include <core/Exec.hpp>
#include <core/json/Json.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include "config.h"

#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif


using namespace core ;

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

} // anonymous namespace

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
   bool remember;
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

   return Success();
}
   
   
} // namespace content_urls
} // namespace modules
} // namesapce session

