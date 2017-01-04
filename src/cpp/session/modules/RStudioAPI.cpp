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

const char * const kRStudioAPIShowDialog = "rstudioapi_show_dialog";
const char * const kRStudioAPIShowDialogCompleted = "rstudioapi_show_dialog_completed";

module_context::WaitForMethodFunction s_waitForShowDialog;

ClientEvent showDialogEvent(const std::string& title,
                            const std::string& message)
{
   json::Object data;
   data["title"] = title;
   data["message"] = message;
   return ClientEvent(client_events::kRStudioAPIShowDialog, data);
}

SEXP rsShowDialog(SEXP titleSEXP, SEXP messageSEXP)
{  
   try
   {
      std::string title = r::sexp::asString(titleSEXP);
      std::string message = r::sexp::asString(messageSEXP);
      
      ClientEvent event = showDialogEvent(title, message);

      // wait for rstudioapi_show_dialog_completed 
      json::JsonRpcRequest request;
      if (!s_waitForShowDialog(&request, event))
      {
         return R_NilValue;
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

   // regsiter rs_rstudioCitation
   r::routines::registerCallMethod(
            "rs_showDialog",
            (DL_FUNC) rsShowDialog,
            0);

   ExecBlock initBlock ;
   return initBlock.execute();
}

} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

