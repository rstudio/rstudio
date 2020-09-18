/*
 * RStudioAPI.cpp
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

#include <core/Macros.hpp>
#include <core/Algorithm.hpp>
#include <core/Debug.hpp>
#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>
#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace rstudioapi {

namespace {

module_context::WaitForMethodFunction s_waitForShowDialog;
module_context::WaitForMethodFunction s_waitForOpenFileDialog;
module_context::WaitForMethodFunction s_waitForRStudioApiResponse;

SEXP rs_executeAppCommand(SEXP commandSEXP, SEXP quietSEXP)
{
   json::Object data;
   data["command"] = r::sexp::safeAsString(commandSEXP);
   data["quiet"] = r::sexp::asLogical(quietSEXP);
   ClientEvent event(client_events::kExecuteAppCommand, data);
   module_context::enqueClientEvent(event);
   return R_NilValue;
}

ClientEvent showDialogEvent(const std::string& title,
                            const std::string& message,
                            int dialogIcon,
                            bool prompt,
                            const std::string& promptDefault,
                            const std::string& ok,
                            const std::string& cancel,
                            const std::string& url)
{
   json::Object data;
   data["title"] = title;
   data["message"] = message;
   data["dialogIcon"] = dialogIcon;
   data["prompt"] = prompt;
   data["default"] = promptDefault;
   data["ok"] = ok;
   data["cancel"] = cancel;
   data["url"] = url;
   return ClientEvent(client_events::kRStudioAPIShowDialog, data);
}

SEXP rs_showDialog(SEXP titleSEXP,
                   SEXP messageSEXP,
                   SEXP dialogIconSEXP,
                   SEXP promptSEXP,
                   SEXP promptDefaultSEXP,
                   SEXP okSEXP,
                   SEXP cancelSEXP,
                   SEXP urlSEXP)
{  
   try
   {
      std::string title = r::sexp::asString(titleSEXP);
      std::string message = r::sexp::asString(messageSEXP);
      int dialogIcon = r::sexp::asInteger(dialogIconSEXP);
      bool prompt = r::sexp::asLogical(promptSEXP);
      std::string promptDefault = r::sexp::asString(promptDefaultSEXP);
      std::string ok = r::sexp::asString(okSEXP);
      std::string cancel = r::sexp::asString(cancelSEXP);
      std::string url = r::sexp::asString(urlSEXP);
      
      ClientEvent event = showDialogEvent(
         title,
         message,
         dialogIcon,
         prompt,
         promptDefault,
         ok,
         cancel,
         url);

      // wait for rstudioapi_show_dialog_completed 
      json::JsonRpcRequest request;
      if (!s_waitForShowDialog(&request, event))
      {
         return R_NilValue;
      }

      if (dialogIcon == 4 && !request.params[1].isNull()) {
         bool result;
         Error error = json::readParam(request.params, 1, &result);
         if (error)
         {
            LOG_ERROR(error);
            return R_NilValue;
         }

         r::sexp::Protect rProtect;
         return r::sexp::create(result, &rProtect);
      }
      else if (!request.params[0].isNull())
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

SEXP rs_openFileDialog(SEXP typeSEXP,
                       SEXP captionSEXP,
                       SEXP labelSEXP,
                       SEXP pathSEXP,
                       SEXP filterSEXP,
                       SEXP existingSEXP)
{
   // extract components
   int type = r::sexp::asInteger(typeSEXP);
   std::string caption = r::sexp::asString(captionSEXP);
   std::string label = r::sexp::asString(labelSEXP);
   std::string path = r::sexp::safeAsString(pathSEXP);
   std::string filter = r::sexp::safeAsString(filterSEXP);
   bool existing = r::sexp::asLogical(existingSEXP);
   
   // default to all files when filter is empty
   if (filter.empty())
      filter = "All Files (*)";
   
   // when path is empty, use project path if available, user home path
   // otherwise
   FilePath filePath;
   if (path.empty())
   {
      if (projects::projectContext().hasProject())
         filePath = projects::projectContext().directory();
      else
         filePath = module_context::userHomePath();
   }
   else
   {
      filePath = module_context::resolveAliasedPath(path);
   }
   
   json::Object data;
   data["type"] = type;
   data["caption"] = caption;
   data["label"] = label;
   data["file"] = module_context::createFileSystemItem(filePath);
   data["filter"] = filter;
   data["existing"] = existing;
   ClientEvent event(client_events::kOpenFileDialog, data);
   
   json::JsonRpcRequest request;
   if (!s_waitForOpenFileDialog(&request, event))
      return R_NilValue;
   
   std::string selection;
   Error error = json::readParams(request.params, &selection);
   if (error)
       LOG_ERROR(error);

   if (selection.empty())
      return R_NilValue;
   
   r::sexp::Protect protect;
   return r::sexp::create(selection, &protect);
}

SEXP rs_highlightUi(SEXP queriesSEXP)
{
   json::Value data;
   Error error = r::json::jsonValueFromList(queriesSEXP, &data);
   if (error)
      LOG_ERROR(error);
   
   ClientEvent event(client_events::kHighlightUi, data);
   module_context::enqueClientEvent(event);
   
   return queriesSEXP;
}

SEXP rs_userIdentity()
{
   r::sexp::Protect protect;
   // Check RSTUDIO_USER_IDENTITY_DISPLAY first; it is used in Pro to override the system user
   // identity (username) with a display name in some auth forms
   std::string display = core::system::getenv("RSTUDIO_USER_IDENTITY_DISPLAY");
   if (display.empty())
   {
      // no env var; look up value in options provided at session start
      display = session::options().userIdentity();
   }

   return r::sexp::create(display, &protect);
}

SEXP rs_systemUsername()
{
   r::sexp::Protect protect;
   return r::sexp::create(core::system::username(), &protect);
}

SEXP rs_documentProperties(SEXP idSEXP,
                           SEXP includeContentsSEXP)
{
   // resolve params
   std::string id = r::sexp::asString(idSEXP);
   bool includeContents = r::sexp::asLogical(includeContentsSEXP);

   // retrieve document
   using session::source_database::SourceDocument;
   boost::shared_ptr<SourceDocument> pDoc(new SourceDocument);
   Error error = source_database::get(id, pDoc);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }

   // convert to R object
   r::sexp::Protect protect;
   SEXP object = pDoc->toRObject(&protect, includeContents);
   return object;
}


SEXP rs_sendApiRequest(SEXP requestSEXP)
{
   Error error;
   
   bool sync = false;
   error = r::sexp::getNamedListElement(requestSEXP, "sync", &sync);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   // build client event
   json::Object requestJson;
   error = r::json::jsonValueFromObject(requestSEXP, &requestJson);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   ClientEvent event(
            client_events::kRStudioApiRequest,
            requestJson);
      
   if (sync)
   {
      // use waitfor method to ensure we wait for response from client
      json::JsonRpcRequest request;
      if (!s_waitForRStudioApiResponse(&request, event))
      {
         r::exec::warning("Failed to execute API request");
         return R_NilValue;
      }
      
      // read response
      json::Object responseJson;
      Error error = json::readParam(request.params, 0, &responseJson);
      if (error)
      {
         LOG_ERROR(error);
         return R_NilValue;
      }
      
      r::sexp::Protect protect;
      return r::sexp::create(responseJson, &protect);
   }
   else
   {
      // just fire the event and immediately return
      module_context::enqueClientEvent(event);
      r::sexp::Protect protect;
      return r::sexp::create(true, &protect);
   }
}

} // end anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // register waitForMethod handlers
   s_waitForShowDialog         = registerWaitForMethod("rstudioapi_show_dialog_completed");
   s_waitForOpenFileDialog     = registerWaitForMethod("open_file_dialog_completed");
   s_waitForRStudioApiResponse = registerWaitForMethod("rstudioapi_response");

   // register R callables
   RS_REGISTER_CALL_METHOD(rs_showDialog);
   RS_REGISTER_CALL_METHOD(rs_openFileDialog);
   RS_REGISTER_CALL_METHOD(rs_executeAppCommand);
   RS_REGISTER_CALL_METHOD(rs_highlightUi);
   RS_REGISTER_CALL_METHOD(rs_userIdentity);
   RS_REGISTER_CALL_METHOD(rs_systemUsername);
   RS_REGISTER_CALL_METHOD(rs_documentProperties);
   RS_REGISTER_CALL_METHOD(rs_sendApiRequest);
   
   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "RStudioAPI.R"));

   return initBlock.execute();
}

} // namespace connections
} // namespace modules
} // namespace session
} // namespace rstudio

