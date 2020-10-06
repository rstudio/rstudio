/*
 * SessionAskSecret.cpp
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

#include "SessionAskSecret.hpp"

#include <boost/bind.hpp>

#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <shared_core/json/Json.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace ask_secret {

namespace {

std::string s_askPassWindow;
module_context::WaitForMethodFunction s_waitForAskPass;

void onClientInit()
{
   s_askPassWindow = "";
}


// show error message from R
SEXP rs_askForSecret(
   SEXP nameSEXP,
   SEXP titleSEXP,
   SEXP promptSEXP,
   SEXP canRememberSEXP,
   SEXP hasSecretSEXP)
{
   try
   {
      std::string name = r::sexp::asString(nameSEXP);
      std::string title = r::sexp::asString(titleSEXP);
      std::string prompt = r::sexp::asString(promptSEXP);
      bool canRemember = r::sexp::asLogical(canRememberSEXP);
      bool hasSecret = r::sexp::asLogical(hasSecretSEXP);

      SecretInput input;
      Error error = askForSecret(name, title, prompt, canRemember, hasSecret, &input);
      if (error)
      {
         LOG_ERROR(error);
         return R_NilValue;
      }
      else if (input.cancelled || input.secret.empty())
      {
         return R_NilValue;
      }
      else
      {
         r::sexp::Protect rProtect;
         return r::sexp::create(input.secret, &rProtect);
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

Error askForSecret(const std::string& name,
                   const std::string& title,
                   const std::string& prompt,
                   bool canRemember,
                   bool hasSecret,
                   SecretInput* pInput)
{
   json::Object payload;
   payload["title"] = title;
   payload["prompt"] = prompt;
   payload["window"] = s_askPassWindow;
   payload["can_remember"] = canRemember;
   payload["has_secret"] = hasSecret;
   ClientEvent askSecretEvent(client_events::kAskSecret, payload);

   // wait for method
   core::json::JsonRpcRequest request;
   if (!s_waitForAskPass(&request, askSecretEvent))
   {
      return systemError(boost::system::errc::operation_canceled,
                         ERROR_LOCATION);
   }

   // read params
   json::Value value;
   bool remember = false;
   bool changed = false;
   Error error = json::readParams(request.params, &value, &remember, &changed);
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
   pInput->changed = changed;

   // if secret changed
   if (pInput->changed)
   {
      pInput->secret = value.getValue<std::string>();

      // decrypt if necessary
#ifdef RSTUDIO_SERVER
      if (options().programMode() == kSessionProgramModeServer)
      {
         // In server mode, passphrases are encrypted
         error = core::system::crypto::rsaPrivateDecrypt(
                                                pInput->secret,
                                                &pInput->secret);
         if (error)
            return error;
      }
#endif
   }
   else
   {
      SEXP lastSecretSEXP;
      r::sexp::Protect protect;
      Error error = r::exec::RFunction(".rs.retrieveSecret", name)
         .call(&lastSecretSEXP, &protect);

      if (error)
         return error;

      std::string lastSecret = r::sexp::asString(lastSecretSEXP);
      pInput->secret = lastSecret;
   }

   if (pInput->remember && pInput->changed)
   {
      Error error = r::exec::RFunction(".rs.rememberSecret", name, pInput->secret)
         .call();

      if (error)
         return error;
   }
   else if (!pInput->remember)
   {
      Error error = r::exec::RFunction(".rs.forgetSecret", name)
         .call();

      if (error)
         return error;
   }

   return Success();
}

Error initialize()
{
   module_context::events().onClientInit.connect(onClientInit);

   // register waitForMethod handler
   s_waitForAskPass = module_context::registerWaitForMethod(
                                                "asksecret_completed");

   // register .Call methods
   RS_REGISTER_CALL_METHOD(rs_askForSecret);

   // complete initialization
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "SessionAskSecret.R"));
   return initBlock.execute();

}
   
   
} // namespace ask_secret
} // namespace modules
} // namespace session
} // namespace rstudio

