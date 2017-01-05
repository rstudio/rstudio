/*
 * RStudioAPI.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <core/Macros.hpp>
#include <core/Algorithm.hpp>
#include <core/Debug.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace rstudioapi {

namespace {
}

module_context::WaitForMethodFunction s_waitForShowDialog;

ClientEvent showDialogEvent(const std::string& title,
                            const std::string& message,
                            bool prompt,
                            const std::string& promptDefault)
{
   json::Object data;
   data["title"] = title;
   data["message"] = message;
   data["prompt"] = prompt;
   data["default"] = promptDefault;
   return ClientEvent(client_events::kRStudioAPIShowDialog, data);
}

SEXP rs_showDialog(SEXP titleSEXP,
                   SEXP messageSEXP,
                   SEXP promptSEXP,
                   SEXP promptDefaultSEXP)
{  
   try
   {
      std::string title = r::sexp::asString(titleSEXP);
      std::string message = r::sexp::asString(messageSEXP);
      bool prompt = r::sexp::asLogical(promptSEXP);
      std::string promptDefault = r::sexp::asString(promptDefaultSEXP);
      
      ClientEvent event = showDialogEvent(title, message, prompt, promptDefault);

      // wait for rstudioapi_show_dialog_completed 
      json::JsonRpcRequest request;
      if (!s_waitForShowDialog(&request, event))
      {
         return R_NilValue;
      }

      if (!request.params[0].is_null())
      {
         std::string promptValue;
         Error error = json::readParam(request.params, 0, &promptValue);
         if (error)
         {
            LOG_ERROR(error);
            return R_NilValue;
         }

         r::sexp::Protect rProtect;
         return r::sexp::create(promptValue, &rProtect);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   return R_NilValue;
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // register waitForMethod handler
   s_waitForShowDialog = module_context::registerWaitForMethod("rstudioapi_show_dialog_completed");

   RS_REGISTER_CALL_METHOD(rs_showDialog, 4);

   return Success();
}

} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

