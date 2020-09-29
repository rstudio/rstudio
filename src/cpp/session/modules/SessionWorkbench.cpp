/*
 * SessionWorkbench.cpp
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

#include "SessionWorkbench.hpp"

#include <algorithm>

#include <boost/function.hpp>
#include <boost/format.hpp>

#include <core/CrashHandler.hpp>
#include <shared_core/Error.hpp>
#include <core/Debug.hpp>
#include <core/Exec.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/ShellUtils.hpp>

#include <r/ROptions.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RClientState.hpp> 
#include <r/RFunctionHook.hpp>
#include <r/RRoutines.hpp>

#include <session/projects/SessionProjects.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

#include <session/SessionConsoleProcess.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/RVersionSettings.hpp>
#include <session/SessionTerminalShell.hpp>

#include "SessionSpelling.hpp"
#include "SessionReticulate.hpp"

#include <R_ext/RStartup.h>
extern "C" SA_TYPE SaveAction;

#include "session-config.h"
#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace workbench {

namespace {

module_context::WaitForMethodFunction s_waitForEditorContext;

SEXP rs_getEditorContext(SEXP typeSEXP)
{
   int type = r::sexp::asInteger(typeSEXP);
   
   json::Object eventData;
   eventData["type"] = "editor_context";
   eventData["data"] = type;
   
   // send the event
   ClientEvent editorContextEvent(client_events::kEditorCommand, eventData);
   
   // wait for event to complete
   json::JsonRpcRequest request;
   
   bool succeeded = s_waitForEditorContext(&request, editorContextEvent);
   if (!succeeded)
      return R_NilValue;
   
   std::string id;
   std::string path;
   std::string contents;
   json::Array selection;
   
   Error error = json::readObjectParam(request.params, 0,
                                       "id", &id,
                                       "path", &path,
                                       "contents", &contents,
                                       "selection", &selection);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   // if the id is empty, implies the source window is closed or
   // no documents were available
   if (id.empty())
      return R_NilValue;
   
   using namespace r::sexp;
   Protect protect;
   ListBuilder builder(&protect);
   
   builder.add("id", id);
   builder.add("path", path);
   builder.add("contents", core::algorithm::split(contents, "\n"));
   
   // add in the selection ranges
   ListBuilder selectionBuilder(&protect);
   for (std::size_t i = 0; i < selection.getSize(); ++i)
   {
      const json::Object& object = selection[i].getObject();
      
      json::Array rangeJson;
      std::string text;
      Error error = json::readObject(object,
                                     "range", rangeJson,
                                     "text", text);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      
      std::vector<int> range;
      if (!rangeJson.toVectorInt(range))
      {
         LOG_WARNING_MESSAGE("failed to parse document range");
         continue;
      }
      
      // the ranges passed use 0-based indexing;
      // transform to 1-based indexing for R
      for (std::size_t i = 0; i < range.size(); ++i)
         ++range[i];
      
      ListBuilder builder(&protect);
      builder.add("range", range);
      builder.add("text", text);
      
      selectionBuilder.add(builder);
   }
   
   builder.add("selection", selectionBuilder);
   
   return r::sexp::create(builder, &protect);
}

Error setClientState(const json::JsonRpcRequest& request, 
                     json::JsonRpcResponse* pResponse)
{   
   pResponse->setSuppressDetectChanges(true);

   // extract params
   json::Object temporaryState, persistentState, projPersistentState;
   Error error = json::readParams(request.params, 
                                  &temporaryState,
                                  &persistentState,
                                  &projPersistentState);
   if (error)
      return error;
   
   // set state
   r::session::ClientState& clientState = r::session::clientState();
   clientState.putTemporary(temporaryState);
   clientState.putPersistent(persistentState);
   clientState.putProjectPersistent(projPersistentState);
   
   return Success();
}
   
     
// IN: WorkbenchMetrics object
// OUT: Void
Error setWorkbenchMetrics(const json::JsonRpcRequest& request, 
                          json::JsonRpcResponse* /*pResponse*/)
{
   // extract fields
   r::session::RClientMetrics metrics;
   Error error = json::readObjectParam(request.params, 0,
                                 "consoleWidth", &(metrics.consoleWidth),
                                 "buildConsoleWidth", &(metrics.buildConsoleWidth),
                                 "graphicsWidth", &(metrics.graphicsWidth),
                                 "graphicsHeight", &(metrics.graphicsHeight),
                                 "devicePixelRatio", &(metrics.devicePixelRatio));
   if (error)
      return error;
   
   // set the metrics
   r::session::setClientMetrics(metrics);
   
   return Success();
}

Error adaptToLanguage(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* /*pResponse*/)
{
   // get the requested language
   std::string language;
   Error error = json::readParams(request.params, &language);
   if (error)
      return error;
   
   return module_context::adaptToLanguage(language);
}

Error executeCode(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* /*pResponse*/)
{
   // get the code
   std::string code;
   Error error = json::readParam(request.params, 0, &code);
   if (error)
      return error;

   // execute the code (show error in the console)
   error = r::exec::executeString("{" + code + "}");
   if (error)
   {
      std::string errMsg = "Error executing code: " + code + "\n";
      errMsg += r::endUserErrorMessage(error);
      module_context::consoleWriteError(errMsg + "\n");
   }

   return Success();
}


Error createSshKey(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string path, type, passphrase;
   bool overwrite;
   Error error = json::readObjectParam(request.params, 0,
                                       "path", &path,
                                       "type", &type,
                                       "passphrase", &passphrase,
                                       "overwrite", &overwrite);
   if (error)
      return error;

#ifdef RSTUDIO_SERVER
   // In server mode, passphrases are encrypted
   using namespace rstudio::core::system::crypto;
   error = rsaPrivateDecrypt(passphrase, &passphrase);
   if (error)
      return error;
#endif

   // resolve key path
   FilePath sshKeyPath = module_context::resolveAliasedPath(path);
   error = sshKeyPath.getParent().ensureDirectory();
   if (error)
      return error;
   FilePath sshPublicKeyPath = sshKeyPath.getParent().completePath(
                                             sshKeyPath.getStem() + ".pub");
   if (sshKeyPath.exists() || sshPublicKeyPath.exists())
   {
      if (!overwrite)
      {
         json::Object resultJson;
         resultJson["failed_key_exists"] = true;
         pResponse->setResult(resultJson);
         return Success();
      }
      else
      {
         Error error = sshKeyPath.removeIfExists();
         if (error)
            return error;
         error = sshPublicKeyPath.removeIfExists();
         if (error)
            return error;
      }
   }

   // compose a shell command to create the key
   shell_utils::ShellCommand cmd("ssh-keygen");

   // type
   cmd << "-t" << type;

   // passphrase (optional)
   cmd << "-N";
   if (!passphrase.empty())
      cmd << passphrase;
   else
      cmd << std::string("");

   // path
   cmd << "-f" << sshKeyPath;

   // process options
   core::system::ProcessOptions options;

   // detach the session so there is no terminal
#ifndef _WIN32
   options.detachSession = true;
#endif

   // customize the environment on Win32
#ifdef _WIN32
   core::system::Options childEnv;
   core::system::environment(&childEnv);

   // set HOME to USERPROFILE
   core::system::setHomeToUserProfile(&childEnv);

   // add msys_ssh to path
   core::system::addToPath(&childEnv,
                           session::options().msysSshPath().getAbsolutePath());

   options.environment = childEnv;
#endif

   // run it
   core::system::ProcessResult result;
   error = runCommand(shell_utils::sendStdErrToStdOut(cmd),
                      options,
                      &result);
   if (error)
      return error;

   // return exit code and output
   json::Object resultJson;
   resultJson["failed_key_exists"] = false;
   resultJson["exit_status"] = result.exitStatus;
   resultJson["output"] = result.stdOut;
   pResponse->setResult(resultJson);
   return Success();
}



// path edit file postback script (provided as GIT_EDITOR and SVN_EDITOR)
std::string s_editFileCommand;

// function we can call to wait for edit_completed
module_context::WaitForMethodFunction s_waitForEditCompleted;

// edit file postback handler
void editFilePostback(const std::string& file,
                      const module_context::PostbackHandlerContinuation& cont)
{
   // read file contents
   FilePath filePath(file);
   std::string fileContents;
   Error error = core::readStringFromFile(filePath, &fileContents);
   if (error)
   {
      LOG_ERROR(error);
      cont(EXIT_FAILURE, "");
      return;
   }

   // prepare edit event
   ClientEvent editEvent = session::showEditorEvent(fileContents, false, true);

   // wait for edit_completed
   json::JsonRpcRequest request;
   bool succeeded = s_waitForEditCompleted(&request, editEvent);

   // cancelled or otherwise didn't succeed
   if (!succeeded || request.params[0].isNull())
   {
      cont(EXIT_FAILURE, "");
      return;
   }

   // extract the content
   std::string editedFileContents;
   error = json::readParam(request.params, 0, &editedFileContents);
   if (error)
   {
      LOG_ERROR(error);
      cont(EXIT_FAILURE, "");
      return;
   }

   // write the content back to the file
   error = core::writeStringToFile(filePath, editedFileContents);
   if (error)
   {
      LOG_ERROR(error);
      cont(EXIT_FAILURE, "");
      return;
   }

   // success
   cont(EXIT_SUCCESS, "");
}

// options("pdfviewer")
void viewPdfPostback(const std::string& pdfPath,
                    const module_context::PostbackHandlerContinuation& cont)
{
   module_context::showFile(FilePath(pdfPath));
   cont(EXIT_SUCCESS, "");
}


void handleFileShow(const http::Request& request, http::Response* pResponse)
{
   // get the file path
   FilePath filePath = module_context::resolveAliasedPath(request.queryParamValue("path"));

   // treat disallowed paths identically to missing ones so this endpoint cannot be used to probe
   // for existence
   if (!filePath.exists() || !module_context::isPathViewAllowed(filePath))
   {
      pResponse->setNotFoundError(request);
      return;
   }

   // send it back
   pResponse->setCacheableFile(filePath, request);
}

void onUserSettingsChanged(const std::string& layer, const std::string& pref)
{
   if (pref == kSaveWorkspace)
   {
      // sync underlying R save action
      module_context::syncRSaveAction();
   }

   if (pref == kCranMirror)
   {
      // verify cran mirror security (will either update to https or
      // will print a warning)
      module_context::reconcileSecureDownloadConfiguration();
   }

   if (pref == kUseSecureDownload)
   {
      // reconcile secure download config
      module_context::reconcileSecureDownloadConfiguration();
   }
}

Error setUserCrashHandlerPrompted(const json::JsonRpcRequest& request,
                                  json::JsonRpcResponse* /*pResponse*/)
{
   bool crashHandlingEnabled;
   Error error = json::readParam(request.params, 0, &crashHandlingEnabled);
   if (error)
      return error;

   error = crash_handler::setUserHasBeenPromptedForPermission();
   if (error)
      return error;

   return crash_handler::setUserHandlerEnabled(crashHandlingEnabled);
}

} // anonymous namespace
   
std::string editFileCommand()
{
   // NOTE: only registered for server mode
   return s_editFileCommand;
}

Error initialize()
{
   // register for change notifications on user settings
   prefs::userPrefs().onChanged.connect(onUserSettingsChanged);

   // register postback handler for viewPDF (server-only)
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      std::string pdfShellCommand;
      Error error = module_context::registerPostbackHandler("pdfviewer",
                                                            viewPdfPostback,
                                                            &pdfShellCommand);
      if (error)
         return error;

      // set pdfviewer option
      error = r::options::setOption("pdfviewer", pdfShellCommand);
      if (error)
         return error;


      // register editfile handler and save its path
      error = module_context::registerPostbackHandler("editfile",
                                                      editFilePostback,
                                                      &s_editFileCommand);
      if (error)
         return error;

      // register edit_completed waitForMethod handler
      s_waitForEditCompleted = module_context::registerWaitForMethod(
                                                         "edit_completed");
   }
   
   // register waitForMethod for active document context
   using namespace module_context;
   s_waitForEditorContext = registerWaitForMethod("get_editor_context_completed");
   
   RS_REGISTER_CALL_METHOD(rs_getEditorContext, 1);
   
   // complete initialization
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerUriHandler, "/file_show", handleFileShow))
      (bind(registerRpcMethod, "set_client_state", setClientState))
      (bind(registerRpcMethod, "set_workbench_metrics", setWorkbenchMetrics))
      (bind(registerRpcMethod, "create_ssh_key", createSshKey))
      (bind(registerRpcMethod, "adapt_to_language", adaptToLanguage))
      (bind(registerRpcMethod, "execute_code", executeCode))
      (bind(registerRpcMethod, "set_user_crash_handler_prompted", setUserCrashHandlerPrompted));
   return initBlock.execute();
}


} // namespace workbench
} // namespace modules
} // namespace session
} // namespace rstudio
