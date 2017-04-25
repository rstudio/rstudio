/*
 * SessionMain.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <session/SessionMain.hpp>

// required to avoid Win64 winsock order of include
// compilation problem
#include <boost/asio/io_service.hpp>

#include <string>
#include <vector>
#include <queue>
#include <map>
#include <algorithm>
#include <cstdlib>
#include <csignal>
#include <limits>

#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/format.hpp>

#include <boost/signals.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/join.hpp>

#include <core/Error.hpp>
#include <core/BoostThread.hpp>
#include <core/ConfigUtils.hpp>
#include <core/FilePath.hpp>
#include <core/FileLock.hpp>
#include <core/Exec.hpp>
#include <core/Scope.hpp>
#include <core/Settings.hpp>
#include <core/Thread.hpp>
#include <core/Log.hpp>
#include <core/LogWriter.hpp>
#include <core/system/System.hpp>
#include <core/ProgramStatus.hpp>
#include <core/system/System.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/URL.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/UriHandler.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>
#include <core/system/ParentProcessMonitor.hpp>
#include <core/system/FileMonitor.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/r_util/RSessionContext.hpp>
#include <core/r_util/REnvironment.hpp>
#include <core/WaitUtils.hpp>

#include <r/RJsonRpc.hpp>
#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RInterface.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RSessionState.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RGraphics.hpp>
#include <r/session/REventLoop.hpp>
#include <r/RUtil.hpp>

#include <monitor/MonitorClient.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionContentUrls.hpp>
#include <session/SessionScopes.hpp>
#include <session/SessionClientEventService.hpp>
#include <session/RVersionSettings.hpp>

#include "SessionAddins.hpp"

#include "SessionModuleContextInternal.hpp"

#include "SessionClientEventQueue.hpp"
#include "SessionClientInit.hpp"
#include "SessionConsoleInput.hpp"
#include "SessionDirs.hpp"
#include "SessionHttpMethods.hpp"
#include "SessionInit.hpp"
#include "SessionMainProcess.hpp"
#include "SessionSuspend.hpp"

#include <session/SessionRUtil.hpp>
#include <session/SessionPackageProvidedExtension.hpp>

#include "modules/RStudioAPI.hpp"
#include "modules/SessionAbout.hpp"
#include "modules/SessionAgreement.hpp"
#include "modules/SessionAskPass.hpp"
#include "modules/SessionAuthoring.hpp"
#include "modules/SessionBreakpoints.hpp"
#include "modules/SessionHTMLPreview.hpp"
#include "modules/SessionCodeSearch.hpp"
#include "modules/SessionConsole.hpp"
#include "modules/SessionCrypto.hpp"
#include "modules/SessionErrors.hpp"
#include "modules/SessionFiles.hpp"
#include "modules/SessionFind.hpp"
#include "modules/SessionDependencies.hpp"
#include "modules/SessionDirty.hpp"
#include "modules/SessionWorkbench.hpp"
#include "modules/SessionHelp.hpp"
#include "modules/SessionPlots.hpp"
#include "modules/SessionPath.hpp"
#include "modules/SessionPackages.hpp"
#include "modules/SessionPackrat.hpp"
#include "modules/SessionProfiler.hpp"
#include "modules/SessionRAddins.hpp"
#include "modules/SessionRCompletions.hpp"
#include "modules/SessionRPubs.hpp"
#include "modules/SessionRHooks.hpp"
#include "modules/SessionRSConnect.hpp"
#include "modules/SessionShinyViewer.hpp"
#include "modules/SessionSpelling.hpp"
#include "modules/SessionSource.hpp"
#include "modules/SessionUpdates.hpp"
#include "modules/SessionVCS.hpp"
#include "modules/SessionHistory.hpp"
#include "modules/SessionLimits.hpp"
#include "modules/SessionLists.hpp"
#include "modules/build/SessionBuild.hpp"
#include "modules/clang/SessionClang.hpp"
#include "modules/connections/SessionConnections.hpp"
#include "modules/data/SessionData.hpp"
#include "modules/environment/SessionEnvironment.hpp"
#include "modules/overlay/SessionOverlay.hpp"
#include "modules/presentation/SessionPresentation.hpp"
#include "modules/rmarkdown/RMarkdownTemplates.hpp"
#include "modules/rmarkdown/SessionRMarkdown.hpp"
#include "modules/rmarkdown/SessionRmdNotebook.hpp"
#include "modules/shiny/SessionShiny.hpp"
#include "modules/viewer/SessionViewer.hpp"
#include "modules/SessionDiagnostics.hpp"
#include "modules/SessionMarkers.hpp"
#include "modules/SessionSnippets.hpp"
#include "modules/SessionUserCommands.hpp"
#include "modules/SessionRAddins.hpp"
#include "modules/mathjax/SessionMathJax.hpp"
#include "modules/SessionLibPathsIndexer.hpp"
#include "modules/SessionObjectExplorer.hpp"

#include <session/SessionProjectTemplate.hpp>

#include "modules/SessionGit.hpp"
#include "modules/SessionSVN.hpp"

#include <session/SessionConsoleProcess.hpp>

#include <session/projects/ProjectsSettings.hpp>
#include <session/projects/SessionProjects.hpp>
#include "projects/SessionProjectsInternal.hpp"

#include "workers/SessionWebRequestWorker.hpp"

#include <session/SessionHttpConnectionListener.hpp>

#include "session-config.h"

#include <tests/TestRunner.hpp>

using namespace rstudio;
using namespace rstudio::core;

// Use rsession alias to avoid collision with 'session'
// object brought in by Catch
namespace rsession = rstudio::session;
using namespace rsession;
using namespace rsession::client_events;

// forward-declare overlay methods
namespace rstudio {
namespace session {
namespace overlay {
Error initialize();
} // namespace overlay
} // namespace session
} // namespace rstudio

namespace {

// R browseUrl handlers
std::vector<module_context::RBrowseUrlHandler> s_rBrowseUrlHandlers;
   
// R browseFile handlers
std::vector<module_context::RBrowseFileHandler> s_rBrowseFileHandlers;

// indicates whether we should destroy the session at cleanup time
// (true if the user does a full quit)
bool s_destroySession = false;

// did we fail to coerce the charset to UTF-8
bool s_printCharsetWarning = false;

void detectChanges(module_context::ChangeSource source)
{
   module_context::events().onDetectChanges(source);
}
 
// allow console_input requests to come in when we aren't explicitly waiting
// on them (i.e. waitForMethod("console_input")). place them into into a buffer
// which is then checked by rConsoleRead prior to it calling waitForMethod
Error bufferConsoleInput(const core::json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // extract the input
   return console_input::extractConsoleInput(request);
}

void doSuspendForRestart(const rstudio::r::session::RSuspendOptions& options)
{
   module_context::consoleWriteOutput("\nRestarting R session...\n\n");

   rstudio::r::session::suspendForRestart(options);
}

Error suspendForRestart(const core::json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   rstudio::r::session::RSuspendOptions options(EX_CONTINUE);
   Error error = json::readObjectParam(
                               request.params, 0,
                               "save_minimal", &(options.saveMinimal),
                               "save_workspace", &(options.saveWorkspace),
                               "exclude_packages", &(options.excludePackages));
   if (error)
      return error;

   pResponse->setAfterResponse(boost::bind(doSuspendForRestart, options));
   return Success();
}


Error ping(const core::json::JsonRpcRequest& request,
           json::JsonRpcResponse* pResponse)
{
   return Success();
}

Error startClientEventService()
{
   return clientEventService().start(rsession::persistentState().activeClientId());
}

Error registerSignalHandlers()
{
   using boost::bind;
   using namespace rstudio::core::system;

   // USR1 and USR2: perform suspend in server mode
   if (rsession::options().programMode() == kSessionProgramModeServer)
   {
      ExecBlock registerBlock ;
      registerBlock.addFunctions()
         (bind(handleSignal, SigUsr1, suspend::handleUSR1))
         (bind(handleSignal, SigUsr2, suspend::handleUSR2));
      return registerBlock.execute();
   }
   // USR1 and USR2: ignore in desktop mode
   else
   {
      ExecBlock registerBlock ;
      registerBlock.addFunctions()
         (bind(ignoreSignal, SigUsr1))
         (bind(ignoreSignal, SigUsr2));
      return registerBlock.execute();
   }
}


Error runPreflightScript()
{
   // alias options
   Options& options = rsession::options();

   // run the preflight script (if specified)
   if (rsession::options().programMode() == kSessionProgramModeServer)
   {
      FilePath preflightScriptPath = options.preflightScriptPath();
      if (!preflightScriptPath.empty())
      {
         if (preflightScriptPath.exists())
         {
            // run the script (ignore errors and continue no matter what
            // the outcome of the script is)
            std::string script = preflightScriptPath.absolutePath();
            core::system::ProcessResult result;
            Error error = runCommand(script,
                                     core::system::ProcessOptions(),
                                     &result);
            if (error)
            {
               error.addProperty("preflight-script", script);
               LOG_ERROR(error);
            }
         }
         else
         {
            LOG_WARNING_MESSAGE("preflight script does not exist: " +
                                preflightScriptPath.absolutePath());
         }
      }
   }

   // always return success
   return Success();
}

void exitEarly(int status)
{
   FileLock::cleanUp();
   ::exit(status);
}
      
Error rInit(const rstudio::r::session::RInitInfo& rInitInfo) 
{
   // save state we need to reference later
   suspend::setSessionResumed(rInitInfo.resumed);
   
   // record built-in waitForMethod handlers
   module_context::registerWaitForMethod(kLocatorCompleted);
   module_context::registerWaitForMethod(kEditCompleted);
   module_context::registerWaitForMethod(kChooseFileCompleted);
   module_context::registerWaitForMethod(kUserPromptCompleted);
   module_context::registerWaitForMethod(kHandleUnsavedChangesCompleted);
   module_context::registerWaitForMethod(kRStudioAPIShowDialogMethod);

   // execute core initialization functions
   using boost::bind;
   using namespace rstudio::core::system;
   using namespace rsession::module_context;
   ExecBlock initialize ;
   initialize.addFunctions()
   
      // client event service
      (startClientEventService)

      // json-rpc listeners
      (bind(registerRpcMethod, kConsoleInput, bufferConsoleInput))
      (bind(registerRpcMethod, "suspend_for_restart", suspendForRestart))
      (bind(registerRpcMethod, "ping", ping))

      // signal handlers
      (registerSignalHandlers)

      // main module context
      (module_context::initialize)

      // projects (early project init required -- module inits below
      // can then depend on e.g. computed defaultEncoding)
      (projects::initialize)

      // source database
      (source_database::initialize)

      // content urls
      (content_urls::initialize)

      // overlay R
      (bind(sourceModuleRFile, "SessionOverlay.R"))
   
      // addins
      (addins::initialize)

      // console processes
      (console_process::initialize)
         
      // r utils
      (r_utils::initialize)

      // modules with c++ implementations
      (modules::spelling::initialize)
      (modules::lists::initialize)
      (modules::path::initialize)
      (modules::limits::initialize)
      (modules::ppe::initialize)
      (modules::ask_pass::initialize)
      (modules::agreement::initialize)
      (modules::console::initialize)
#ifdef RSTUDIO_SERVER
      (modules::crypto::initialize)
#endif
      (modules::code_search::initialize)
      (modules::clang::initialize)
      (modules::connections::initialize)
      (modules::files::initialize)
      (modules::find::initialize)
      (modules::environment::initialize)
      (modules::dependencies::initialize)
      (modules::dirty::initialize)
      (modules::workbench::initialize)
      (modules::data::initialize)
      (modules::help::initialize)
      (modules::presentation::initialize)
      (modules::plots::initialize)
      (modules::packages::initialize)
      (modules::profiler::initialize)
      (modules::viewer::initialize)
      (modules::rmarkdown::initialize)
      (modules::rmarkdown::notebook::initialize)
      (modules::rmarkdown::templates::initialize)
      (modules::rpubs::initialize)
      (modules::shiny::initialize)
      (modules::source::initialize)
      (modules::source_control::initialize)
      (modules::authoring::initialize)
      (modules::html_preview::initialize)
      (modules::history::initialize)
      (modules::build::initialize)
      (modules::overlay::initialize)
      (modules::breakpoints::initialize)
      (modules::errors::initialize)
      (modules::updates::initialize)
      (modules::about::initialize)
      (modules::shiny_viewer::initialize)
      (modules::rsconnect::initialize)
      (modules::packrat::initialize)
      (modules::rhooks::initialize)
      (modules::r_packages::initialize)
      (modules::diagnostics::initialize)
      (modules::markers::initialize)
      (modules::snippets::initialize)
      (modules::user_commands::initialize)
      (modules::r_addins::initialize)
      (modules::projects::templates::initialize)
      (modules::mathjax::initialize)
      (modules::rstudioapi::initialize)
      (modules::libpaths::initialize)
      (modules::explorer::initialize)

      // workers
      (workers::web_request::initialize)

      // R code
      (bind(sourceModuleRFile, "SessionCodeTools.R"))
      (bind(sourceModuleRFile, "SessionCompletionHooks.R"))
   
      // unsupported functions
      (bind(rstudio::r::function_hook::registerUnsupported, "bug.report", "utils"))
      (bind(rstudio::r::function_hook::registerUnsupported, "help.request", "utils"))
   ;

   Error error = initialize.execute();
   if (error)
      return error;
   
   // if we are in verify installation mode then we should exit (successfully) now
   if (rsession::options().verifyInstallation())
   {
      // in desktop mode we write a success message and execute diagnostics
      if (rsession::options().programMode() == kSessionProgramModeDesktop)
      {
         std::cout << "Successfully initialized R session."
                   << std::endl << std::endl;
         FilePath diagFile = module_context::sourceDiagnostics();
         if (!diagFile.empty())
         {
            std::cout << "Diagnostics report written to: "
                      << diagFile << std::endl << std::endl;

            Error error = rstudio::r::exec::RFunction(".rs.showDiagnostics").call();
            if (error)
               LOG_ERROR(error);
         }
      }

      rsession::options().verifyInstallationHomeDir().removeIfExists();
      exitEarly(EXIT_SUCCESS);
   }

   // run unit tests
   if (rsession::options().runTests())
   {
      int result = tests::run();
      exitEarly(result);
   }
   
   // register all of the json rpc methods implemented in R
   json::JsonRpcMethods rMethods ;
   error = rstudio::r::json::getRpcMethods(&rMethods);
   if (error)
      return error ;
   BOOST_FOREACH(const json::JsonRpcMethod& method, rMethods)
   {
      registerRpcMethod(json::adaptMethodToAsync(method));
   }

   // add gwt handlers if we are running desktop mode
   if ((rsession::options().programMode() == kSessionProgramModeDesktop) ||
       rsession::options().standalone())
   {
      http_methods::registerGwtHandlers();
   }

   // enque abend warning event if necessary (but not in standalone
   // mode since those processes are often aborted unceremoniously)
   using namespace rsession::client_events;
   if (rsession::persistentState().hadAbend() && !options().standalone())
   {
      LOG_ERROR_MESSAGE("session hadabend");

      ClientEvent abendWarningEvent(kAbendWarning);
      rsession::clientEventQueue().add(abendWarningEvent);
   }

   if (s_printCharsetWarning)
      rstudio::r::exec::warning("Character set is not UTF-8; please change your locale");

   // propagate console history options
   rstudio::r::session::consoleHistory().setRemoveDuplicates(
                                 userSettings().removeHistoryDuplicates());


   // register function editor on windows
#ifdef _WIN32
   error = rstudio::r::exec::RFunction(".rs.registerFunctionEditor").call();
   if (error)
      LOG_ERROR(error);
#endif

   // set flag indicating we had an abnormal end (if this doesn't get
   // unset by the time we launch again then we didn't terminate normally
   // i.e. either the process dying unexpectedly or a call to R_Suicide)
   rsession::persistentState().setAbend(true);

   // begin session
   using namespace module_context;
   activeSession().beginSession(rVersion(), rHomeDir());

   // setup fork handlers
   main_process::setupForkHandlers();

   // success!
   return Success();
}

void notifyIfRVersionChanged()
{
   using namespace rstudio::r::session::state;
   
   SessionStateInfo info = getSessionStateInfo();
   
   if (info.activeRVersion != info.suspendedRVersion)
   {
      const char* fmt =
            "R version change [%1% -> %2%] detected when restoring session; "
            "search path not restored\n";
      
      boost::format formatter(fmt);
      formatter
            % std::string(info.suspendedRVersion)
            % std::string(info.activeRVersion);
      
      std::string msg = formatter.str();
      ::REprintf(msg.c_str());
   }
}

void rSessionInitHook(bool newSession)
{
   // allow any packages listening to complete initialization
   modules::rhooks::invokeHook(kSessionInitHook, newSession);

   // finish off initialization
   module_context::events().afterSessionInitHook(newSession);
   
   // notify the user if the R version has changed
   notifyIfRVersionChanged();

   // fire an event to the client
   ClientEvent event(client_events::kDeferredInitCompleted);
   module_context::enqueClientEvent(event);
}

void rDeferredInit(bool newSession)
{
   module_context::events().onDeferredInit(newSession);
   
   // schedule execution of the session init hook
   module_context::scheduleDelayedWork(
                        boost::posix_time::seconds(1),
                        boost::bind(rSessionInitHook, newSession));
}
   
int rEditFile(const std::string& file)
{
   // read file contents
   FilePath filePath(file);
   std::string fileContents;
   if (filePath.exists())
   {
      Error readError = core::readStringFromFile(filePath, &fileContents);
      if (readError)
      {
         LOG_ERROR(readError);
         return 1; // r will raise/report an error indicating edit failed
      }
   }
   
   // fire edit event
   ClientEvent editEvent = rsession::showEditorEvent(fileContents, true, false);
   rsession::clientEventQueue().add(editEvent);

   // wait for edit_completed 
   json::JsonRpcRequest request ;
   bool succeeded = http_methods::waitForMethod(kEditCompleted,
                                        editEvent,
                                        suspend::disallowSuspend,
                                        &request);

   if (!succeeded)
      return false;
   
   // user cancelled edit
   if (request.params[0].is_null())
   {
      return 0; // no-op, object will be re-parsed from original content
   }
   
   // user confirmed edit
   else
   {
      // extract the content
      std::string editedFileContents;
      Error error = json::readParam(request.params, 0, &editedFileContents);
      if (error)
      {
         LOG_ERROR(error);
         return 1; // error (r will notify user via the console) 
      }
      
      // write the content back to the file (append newline expected by R) 
      editedFileContents += "\n";
      Error writeError = core::writeStringToFile(filePath, editedFileContents);
      if (writeError)
      {
         LOG_ERROR(writeError);
         return 1 ; // error (r will notify user via the console) 
      }
      
      // success!
      return 0 ;
   }
}
   
   
FilePath rChooseFile(bool newFile)
{
   // fire choose file event
   ClientEvent chooseFileEvent(kChooseFile, newFile);
   rsession::clientEventQueue().add(chooseFileEvent);
   
   // wait for choose_file_completed 
   json::JsonRpcRequest request ;
   bool succeeded = http_methods::waitForMethod(kChooseFileCompleted,
                                        chooseFileEvent,
                                        suspend::disallowSuspend,
                                        &request);
   
   if (!succeeded)
      return FilePath();

   // extract the file name
   std::string fileName;
   if (!request.params[0].is_null())
   {
      Error error = json::readParam(request.params, 0, &fileName);
      if (error)
         LOG_ERROR(error);
      
      // resolve aliases and return it
      return module_context::resolveAliasedPath(fileName);
   }
   else
   {
      return FilePath();
   }
}


void rBusy(bool busy)
{
   if (main_process::wasForked())
      return;

   // screen out busy = true events that occur when R isn't busy
   if (busy && !console_input::executing())
      return;

   ClientEvent busyEvent(kBusy, busy);
   rsession::clientEventQueue().add(busyEvent);
}
      
void rConsoleWrite(const std::string& output, int otype)   
{
   if (main_process::wasForked())
      return;

   int event = otype == 1 ? kConsoleWriteError : kConsoleWriteOutput;
   ClientEvent writeEvent(event, output);
   rsession::clientEventQueue().add(writeEvent);

   // fire event
   module_context::events().onConsoleOutput(
                  otype == 1 ? module_context::ConsoleOutputError :
                               module_context::ConsoleOutputNormal,
                  output);

}
   
void rConsoleHistoryReset()
{
   json::Array historyJson;
   rstudio::r::session::consoleHistory().asJson(&historyJson);
   json::Object resetJson;
   resetJson["history"] = historyJson;
   resetJson["preserve_ui_context"] = false;
   ClientEvent event(kConsoleResetHistory, resetJson);
   rsession::clientEventQueue().add(event);
}

bool rLocator(double* x, double* y)
{
   // since locator can be called in a loop we need to checkForChanges
   // here (because we'll never get back to the REPL). this enables
   // identify() to correctly update the plot after each click
   detectChanges(module_context::ChangeSourceREPL);
   
   // fire locator event
   ClientEvent locatorEvent(kLocator);
   rsession::clientEventQueue().add(locatorEvent);
   
   // wait for locator_completed 
   json::JsonRpcRequest request ;
   bool succeeded = http_methods::waitForMethod(kLocatorCompleted,
                                        locatorEvent,
                                        suspend::disallowSuspend,
                                        &request);

   if (!succeeded)
      return false;
   
   // see if we got a point
   if ((request.params.size() > 0) && !request.params[0].is_null())
   {
      // read the x and y
      Error error = json::readObjectParam(request.params, 0,
                                          "x", x,
                                          "y", y);
      if (error)
      {
         LOG_ERROR(error);
         return false;
      }
      
      // return true
      return true;
   }
   else
   {
      return false;
   }
}
   
void rShowFile(const std::string& title, const FilePath& filePath, bool del)
{
   if (rsession::options().programMode() == kSessionProgramModeServer)
   {
      // for files in the user's home directory and pdfs use an external browser
      if (module_context::isVisibleUserFile(filePath) ||
          (filePath.extensionLowerCase() == ".pdf"))
      {
         module_context::showFile(filePath);
      }
      else
      {
         module_context::showContent(title, filePath);
      }
   }
   else // (rsession::options().programMode() == kSessionProgramModeDesktop
   {
#ifdef _WIN32
    if (!filePath.extension().empty())
    {
       module_context::showFile(filePath);
       del = false;
    }
    else
    {
       module_context::showContent(title, filePath);
    }
#else
    module_context::showContent(title, filePath);
#endif
   }

   if (del)
   {
      Error error = filePath.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }
}
   
void rBrowseURL(const std::string& url)   
{
   // first see if any of our handlers want to take it
   for (std::vector<module_context::RBrowseUrlHandler>::const_iterator 
            it = s_rBrowseUrlHandlers.begin(); 
            it != s_rBrowseUrlHandlers.end();
            ++it)
   {
      if ((*it)(url))
         return;
   }
   
   // raise event to client
   rsession::clientEventQueue().add(browseUrlEvent(url));
}
   
void rBrowseFile(const core::FilePath& filePath)
{
   // see if any of our handlers want to take it
   for (std::vector<module_context::RBrowseFileHandler>::const_iterator 
            it = s_rBrowseFileHandlers.begin(); 
            it != s_rBrowseFileHandlers.end();
            ++it)
   {
      if ((*it)(filePath))
         return;
   }

   // see if this is an html file in the session temporary directory (in which
   // case we can serve it over http)
   if ((filePath.mimeContentType() == "text/html") &&
       filePath.isWithin(module_context::tempDir()) &&
       rstudio::r::util::hasRequiredVersion("2.14"))
   {
      std::string path = filePath.relativePath(module_context::tempDir());
      std::string url = module_context::sessionTempDirUrl(path);
      rsession::clientEventQueue().add(browseUrlEvent(url));
   }
   // otherwise just show the file
   else
   {
      module_context::showFile(filePath);
   }
}

void rShowHelp(const std::string& helpURL)   
{
   ClientEvent showHelpEvent(kShowHelp, helpURL);
   rsession::clientEventQueue().add(showHelpEvent);
}
      
void rShowMessage(const std::string& message)   
{
   ClientEvent event = showErrorMessageEvent("R Error", message);
   rsession::clientEventQueue().add(event);
}

void logExitEvent(const monitor::Event& precipitatingEvent)
{
   using namespace monitor;
   client().logEvent(precipitatingEvent);
   client().logEvent(Event(kSessionScope, kSessionExitEvent));
}
   
void rSuspended(const rstudio::r::session::RSuspendOptions& options)
{
   // log to monitor
   using namespace monitor;
   std::string data;
   if (suspend::suspendedFromTimeout())
      data = safe_convert::numberToString(rsession::options().timeoutMinutes());
   logExitEvent(Event(kSessionScope, kSessionSuspendEvent, data));

   // fire event
   module_context::onSuspended(options, &(persistentState().settings()));
}
   
void rResumed()
{
   module_context::onResumed(persistentState().settings());
}

bool rHandleUnsavedChanges()
{
   // enque the event
   ClientEvent event(client_events::kHandleUnsavedChanges);
   module_context::enqueClientEvent(event);

   // wait for method
   json::JsonRpcRequest request;
   bool succeeded = http_methods::waitForMethod(
                        kHandleUnsavedChangesCompleted,
                        boost::bind(http_methods::waitForMethodInitFunction, 
                                    event),
                        suspend::disallowSuspend,
                        &request);

   if (!succeeded)
      return false;

   // read response and return it
   bool handled = false;
   Error error = json::readParam(request.params, 0, &handled);
   if (error)
      LOG_ERROR(error);
   return handled;
}

void rQuit()
{   
   if (main_process::wasForked())
      return;

   // log to monitor
   using namespace monitor;
   logExitEvent(Event(kSessionScope, kSessionQuitEvent));

   // notify modules
   module_context::events().onQuit();

   // enque a quit event
   bool switchProjects;
   if (options().switchProjectsWithUrl())
   {
      switchProjects = !http_methods::nextSessionUrl().empty();
   }
   else
   {
      switchProjects = !projects::ProjectsSettings(options().userScratchPath())
                              .switchToProjectPath().empty();
   }

   // if we aren't switching projects then destroy the active session at cleanup
   s_destroySession = !switchProjects;

   json::Object jsonData;
   jsonData["switch_projects"] = switchProjects;
   jsonData["next_session_url"] = http_methods::nextSessionUrl();
   ClientEvent quitEvent(kQuit, jsonData);
   rsession::clientEventQueue().add(quitEvent);
}
   
// NOTE: this event is never received on windows (because we can't
// override suicide on windows)
void rSuicide(const std::string& message)
{
   if (main_process::wasForked())
      return;

   // log to monitor
   using namespace monitor;
   logExitEvent(Event(kSessionScope, kSessionSuicideEvent));

   // log the error if it was unexpected
   if (!message.empty())
      LOG_ERROR_MESSAGE("R SUICIDE: " + message);
   
   // enque suicide event so the client knows
   ClientEvent suicideEvent(kSuicide, message);
   rsession::clientEventQueue().add(suicideEvent);
}

// terminate all children of the provided process supervisor
// and then wait a brief period to attempt to reap the child
void terminateAllChildren(core::system::ProcessSupervisor* pSupervisor,
                          const ErrorLocation& location)
{
   // send kill signal
   pSupervisor->terminateAll();

   // wait and reap children (but for no longer than 1 second)
   if (!pSupervisor->wait(boost::posix_time::milliseconds(10),
                          boost::posix_time::milliseconds(1000)))
   {
      core::log::logWarningMessage(
            "Process supervisor did not terminate within 1 second",
            location);
   }
}

void rCleanup(bool terminatedNormally)
{
   try
   {
      // bail completely if we were forked
      if (main_process::wasForked())
         return;

      // note that we didn't abend
      if (terminatedNormally)
         rsession::persistentState().setAbend(false);

      // set active session flag indicating we are no longer running
      module_context::activeSession().endSession();

      // fire shutdown event to modules
      module_context::events().onShutdown(terminatedNormally);

      // destroy session if requested
      if (s_destroySession)
      {
         Error error = module_context::activeSession().destroy();
         if (error)
            LOG_ERROR(error);
      }
      
      // clean up locks
      FileLock::cleanUp();

      // cause graceful exit of clientEventService (ensures delivery
      // of any pending events prior to process termination). wait a
      // very brief interval first to allow the quit or other termination
      // related events to get into the queue
      boost::this_thread::sleep(boost::posix_time::milliseconds(100));

      // only stop the http services if we are in server mode. in desktop
      // mode we had issues with both OSX crashing and with Windows taking
      // the full 3 seconds to terminate. the cleanup is kind of a nice
      // to have and most important on the server where we delete the
      // unix domain socket file so it is no big deal to bypass it
      if (rsession::options().programMode() == kSessionProgramModeServer)
      {
         clientEventService().stop();
         httpConnectionListener().stop();
      }

      // terminate known child processes
      terminateAllChildren(&module_context::processSupervisor(),
                           ERROR_LOCATION);
   }
   CATCH_UNEXPECTED_EXCEPTION

}   
   
void rSerialization(int action, const FilePath& targetPath)
{
   json::Object serializationActionObject ;
   serializationActionObject["type"] = action;
   if (!targetPath.empty())
   {
      serializationActionObject["targetPath"] =
                           module_context::createAliasedPath(targetPath);
   }
   
   ClientEvent event(kSessionSerialization, serializationActionObject);
   rsession::clientEventQueue().add(event);
}

   
void ensureRProfile()
{
   // check if we need to create the proifle (bail if we don't)
   Options& options = rsession::options();
   if (!options.createProfile())
      return;

   FilePath rProfilePath = options.userHomePath().complete(".Rprofile");
   if (!rProfilePath.exists() && !userSettings().autoCreatedProfile())
   {
      userSettings().setAutoCreatedProfile(true);
      
      std::string p;
      p = "# .Rprofile -- commands to execute at the beginning of each R session\n"
          "#\n"
          "# You can use this file to load packages, set options, etc.\n"
          "#\n"
          "# NOTE: changes in this file won't be reflected until after you quit\n"
          "# and start a new session\n"
          "#\n\n";
      
      Error error = writeStringToFile(rProfilePath, p);
      if (error)
         LOG_ERROR(error);
   }
}
      
void ensurePublicFolder()
{
   // check if we need to create the public folder (bail if we don't)
   Options& options = rsession::options();
   if (!options.createPublicFolder())
      return;

   FilePath publicPath = options.userHomePath().complete("Public");
   if (!publicPath.exists())
   {
      // create directory
      Error error = publicPath.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // write notice
      boost::format fmt(
       "\n"
       "Files within your public folder are readable (but not writeable)\n"
       "by others. The path for other users to access your public folder is:\n"
       "\n"
       "  /shared/%1%/\n"
       "\n"
       "For example, to source a file named \"Utils.R\" from your public\n"
       "folder another user would enter the command:\n"
       "\n"
       "  source(\"/shared/%1%/Utils.R\")\n"
       "\n"
       "To load a dataset named \"Data.csv\" they would enter the command:\n"
       "\n"
       "  read.csv(\"/shared/%1%/Data.csv\")\n"
       "\n"
       "Other users can also browse and open the files available in your\n"
       "public folder by:\n"
       "\n"
       "  1) Selecting the Open File... menu item\n"
       "  2) Entering /shared/%1%/ as the file name\n"
       "  3) Clicking the Open button (or pressing the Enter key)\n"
       "\n"
      );
      std::string notice = boost::str(fmt % options.userIdentity());

      FilePath noticePath = publicPath.complete("AboutPublic.txt");
      error = writeStringToFile(noticePath, notice);
      if (error)
         LOG_ERROR(error);
   }
}

void ensureRLibsUser(const core::FilePath& userHomePath,
                     const std::string& rLibsUser)
{
   FilePath rLibsUserPath = FilePath::resolveAliasedPath(rLibsUser,
                                                         userHomePath);
   Error error = rLibsUserPath.ensureDirectory();
   if (error)
      LOG_ERROR(error);
}

#ifdef __APPLE__
// we now launch our child processes from the desktop using our standard
// process management code which closes all file descriptors thereby
// breaking parent_process_monitor. So on the Mac we use the more simplistic
// approach of polling for ppid == 1. This is fine because we expect that
// the Desktop will _always_ outlive us (it waits for us to exit before
// closing) so anytime it exits before we do it must be a crash). we don't
// call abort() however because we don't want a crash report to occur
void detectParentTermination()
{
   while(true)
   {
      boost::this_thread::sleep(boost::posix_time::milliseconds(500));
      if (::getppid() == 1)
      {
         exitEarly(EXIT_FAILURE);
      }
   }
}
#else
void detectParentTermination()
{
   using namespace parent_process_monitor;
   ParentTermination result = waitForParentTermination();
   if (result == ParentTerminationAbnormal)
   {
      LOG_ERROR_MESSAGE("Parent terminated");
      core::system::abort();
   }
   else if (result == ParentTerminationNormal)
   {
      //LOG_ERROR_MESSAGE("Normal terminate");
   }
   else if (result == ParentTerminationWaitFailure)
   {
      LOG_ERROR_MESSAGE("waitForParentTermination failed");
   }
}
#endif

SA_TYPE saveWorkspaceOption()
{
   // convert from internal type to R type
   int saveAction = module_context::saveWorkspaceAction();
   if (saveAction == rstudio::r::session::kSaveActionSave)
      return SA_SAVE;
   else if (saveAction == rstudio::r::session::kSaveActionNoSave)
      return SA_NOSAVE;
   else
      return SA_SAVEASK;
}

bool restoreWorkspaceOption()
{
   // allow project override
   const projects::ProjectContext& projContext = projects::projectContext();
   if (projContext.hasProject())
   {
      switch(projContext.config().restoreWorkspace)
      {
      case r_util::YesValue:
         return true;
      case r_util::NoValue:
         return false;
      default:
         // fall through
         break;
      }
   }

   // no project override
   return userSettings().loadRData() ||
          !rsession::options().initialEnvironmentFileOverride().empty();
}

bool alwaysSaveHistoryOption()
{
   // allow project override
   const projects::ProjectContext& projContext = projects::projectContext();
   if (projContext.hasProject())
   {
      switch(projContext.config().alwaysSaveHistory)
      {
      case r_util::YesValue:
         return true;
      case r_util::NoValue:
         return false;
      default:
         // fall through
         break;
      }
   }

   return userSettings().alwaysSaveHistory();
}

FilePath getStartupEnvironmentFilePath()
{
   FilePath envFile = rsession::options().initialEnvironmentFileOverride();
   if (!envFile.empty())
      return envFile;
   else
      return dirs::rEnvironmentDir().complete(".RData");
}

} // anonymous namespace


// provide definition methods for rsession::module_context
namespace rstudio {
namespace session { 
namespace module_context {
   
Error registerRBrowseUrlHandler(const RBrowseUrlHandler& handler)
{
   s_rBrowseUrlHandlers.push_back(handler);
   return Success();
}
   
Error registerRBrowseFileHandler(const RBrowseFileHandler& handler)
{
   s_rBrowseFileHandlers.push_back(handler);
   return Success();
}

UserPrompt::Response showUserPrompt(const UserPrompt& userPrompt)
{
   // enque user prompt event
   json::Object obj;
   obj["type"] = static_cast<int>(userPrompt.type);
   obj["caption"] = userPrompt.caption;
   obj["message"] = userPrompt.message;
   obj["yesLabel"] = userPrompt.yesLabel;
   obj["noLabel"] = userPrompt.noLabel;
   obj["yesIsDefault"] = userPrompt.yesIsDefault;
   obj["includeCancel"] = userPrompt.includeCancel;
   ClientEvent userPromptEvent(client_events::kUserPrompt, obj);
   rsession::clientEventQueue().add(userPromptEvent);

   // wait for user_prompt_completed
   json::JsonRpcRequest request ;
   http_methods::waitForMethod(kUserPromptCompleted,
                       userPromptEvent,
                       suspend::disallowSuspend,
                       &request);

   // read the response param
   int response;
   Error error = json::readParam(request.params, 0, &response);
   if (error)
   {
      LOG_ERROR(error);
      return UserPrompt::ResponseCancel;
   }

   // return response (don't cast so that we can make sure the integer
   // returned matches one of enumerated type values and warn otherwise)
   switch (response)
   {
      case UserPrompt::ResponseYes:
         return UserPrompt::ResponseYes;

      case UserPrompt::ResponseNo:
         return UserPrompt::ResponseNo;

      case UserPrompt::ResponseCancel:
         return UserPrompt::ResponseCancel;

      default:
         LOG_WARNING_MESSAGE("Unexpected user prompt response: " +
                             boost::lexical_cast<std::string>(response));

         return UserPrompt::ResponseCancel;
   };
}

int saveWorkspaceAction()
{
   // allow project override
   const projects::ProjectContext& projContext = projects::projectContext();
   if (projContext.hasProject())
   {
      switch(projContext.config().saveWorkspace)
      {
      case r_util::YesValue:
         return rstudio::r::session::kSaveActionSave;
      case r_util::NoValue:
         return rstudio::r::session::kSaveActionNoSave;
      case r_util::AskValue:
         return rstudio::r::session::kSaveActionAsk;
      default:
         // fall through
         break;
      }
   }

   // no project override, read from settings
   return userSettings().saveAction();
}

void syncRSaveAction()
{
   rstudio::r::session::setSaveAction(saveWorkspaceOption());
}

} // namespace module_context
} // namespace session
} // namespace rstudio

namespace {

int sessionExitFailure(const core::Error& error,
                       const core::ErrorLocation& location)
{
   if (!error.expected())
      core::log::logError(error, location);

   return EXIT_FAILURE;
}


std::string ctypeEnvName()
{
   if (!core::system::getenv("LC_ALL").empty())
      return "LC_ALL";
   if (!core::system::getenv("LC_CTYPE").empty())
      return "LC_CTYPE";
   if (!core::system::getenv("LANG").empty())
      return "LANG";
   return "LC_CTYPE";
}

/*
If possible, we want to coerce the character set to UTF-8.
We can't do this by directly calling setlocale because R
will override those settings when it starts up. Instead we
set the corresponding environment variables, which R will
use.

The exception is Win32, which doesn't allow UTF-8 to be used
as an ANSI codepage.

Returns false if we tried and failed to set the charset to
UTF-8, either because we didn't recognize the locale string
format or because the system didn't accept our new locale
string.
*/
bool ensureUtf8Charset()
{
#if _WIN32
   return true;
#else
   std::string name = ctypeEnvName();
   std::string ctype = core::system::getenv(name);

   if (regex_utils::search(ctype, boost::regex("UTF-8$")))
      return true;

#if __APPLE__
   // For Mac, we attempt to do the fixup in DesktopMain.cpp. If it didn't
   // work, let's not do anything rash here--just let the UTF-8 warning show.
   return false;
#else

   std::string newCType;
   if (ctype.empty() || ctype == "C" || ctype == "POSIX")
   {
      newCType = "en_US.UTF-8";
   }
   else
   {
      using namespace boost;

      smatch match;
      if (regex_utils::match(ctype, match, regex("(\\w+_\\w+)(\\.[^@]+)?(@.+)?")))
      {
         // Try to replace the charset while keeping everything else the same.
         newCType = match[1] + ".UTF-8" + match[3];
      }
   }

   if (!newCType.empty())
   {
      if (setlocale(LC_CTYPE, newCType.c_str()))
      {
         core::system::setenv(name, newCType);
         setlocale(LC_CTYPE, "");
         return true;
      }
   }

   return false;
#endif
#endif
}


} // anonymous namespace

// run session
int main (int argc, char * const argv[]) 
{ 
   try
   {      
      // initialize log so we capture all errors including ones which occur
      // reading the config file (if we are in desktop mode then the log
      // will get re-initialized below)
      initializeSystemLog("rsession-" + core::system::username(),
                          core::system::kLogLevelWarning);

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // get main thread id (used to distinguish forks which occur
      // from the main thread vs. child threads)
      main_process::initThreadId();

      // ensure LANG and UTF-8 character set
#ifndef _WIN32
      r_util::ensureLang();
#endif
      s_printCharsetWarning = !ensureUtf8Charset();
      
      // read program options
      Options& options = rsession::options();
      ProgramStatus status = options.read(argc, argv) ;
      if (status.exit())
         return status.exitCode() ;


      // reflect stderr logging
      core::system::setLogToStderr(options.logStderr());

      // initialize monitor
      monitor::initializeMonitorClient(kMonitorSocketPath,
                                       options.monitorSharedSecret());

      // register monitor log writer (but not in standalone mode)
      if (!options.standalone())
      {
         core::system::addLogWriter(monitor::client().createLogWriter(
                                                options.programIdentity()));
      }

      // initialize file lock config
      FileLock::initialize();

      // convenience flags for server and desktop mode
      bool desktopMode = options.programMode() == kSessionProgramModeDesktop;
      bool serverMode = options.programMode() == kSessionProgramModeServer;

      // re-initialize log for desktop mode
      if (desktopMode)
      {
         if (options.verifyInstallation())
         {
            initializeStderrLog(options.programIdentity(),
                                core::system::kLogLevelWarning);
         }
         else
         {
            initializeLog(options.programIdentity(),
                          core::system::kLogLevelWarning,
                          options.userLogPath());
         }
      }

      // initialize overlay
      error = rsession::overlay::initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // set the rstudio environment variable so code can check for
      // whether rstudio is running
      core::system::setenv("RSTUDIO", "1");

      // environment variables so child processes know of ANSI color
      // escape sequence support in console
      if (userSettings().ansiConsoleMode() == core::text::AnsiColorOn)
      {
         if (options.defaultConsoleTerm().length() > 0)
            core::system::setenv("TERM", options.defaultConsoleTerm());
         if (options.defaultCliColorForce())
            core::system::setenv("CLICOLOR_FORCE", "1");
      }

      // set the rstudio user identity environment variable (can differ from
      // username in debug configurations). this is provided so that 
      // rpostback knows what local stream to connect back to
      core::system::setenv(kRStudioUserIdentity, options.userIdentity());
      if (desktopMode)
      {
         // do the same for port number, for rpostback in rdesktop configs
         core::system::setenv(kRSessionPortNumber, options.wwwPort());
      }

      // provide session stream for postback in server mode
      if (serverMode)
      {
         r_util::SessionContext context = options.sessionContext();
         std::string stream = r_util::sessionContextFile(context);
         core::system::setenv(kRStudioSessionStream, stream);
      }

      // set the standalone port if we are running in standalone mode
      if (options.standalone())
      {
         core::system::setenv(kRSessionStandalonePortNumber, options.wwwPort());
      }
           
      // ensure we aren't being started as a low (priviliged) account
      if (serverMode &&
          !options.verifyInstallation() &&
          core::system::currentUserIsPrivilleged(options.authMinimumUserId()))
      {
         Error error = systemError(boost::system::errc::permission_denied,
                                   ERROR_LOCATION);
         boost::format fmt("User '%1%' has id %2%, which is lower than the "
                           "minimum user id of %3% (this is controlled by "
                           "the the auth-minimum-user-id rserver option)");
         std::string msg = boost::str(fmt % core::system::username()
                                          % core::system::effectiveUserId()
                                          % options.authMinimumUserId());
         error.addProperty("message", msg);
         return sessionExitFailure(error, ERROR_LOCATION);
      }

#ifdef RSTUDIO_SERVER
      if (serverMode)
      {
         Error error = core::system::crypto::rsaInit();
         if (error)
            LOG_ERROR(error);
      }
#endif

      // start the file monitor
      core::system::file_monitor::initialize();

      // initialize client event queue. this must be done very early
      // in main so that any other code which needs to enque an event
      // has access to the queue
      rsession::initializeClientEventQueue();

      // detect parent termination
      if (desktopMode)
         core::thread::safeLaunchThread(detectParentTermination);

      // set the rpostback absolute path
      FilePath rpostback = options.rpostbackPath()
                           .parent().parent()
                           .childPath("rpostback");
      core::system::setenv(
            "RS_RPOSTBACK_PATH",
            string_utils::utf8ToSystem(rpostback.absolutePath()));

      // ensure that the user scratch path exists
      FilePath userScratchPath = options.userScratchPath();
      error = userScratchPath.ensureDirectory();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // initialize user settings
      error = userSettings().initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION) ;

      // startup projects -- must be after userSettings is initialized
      // but before persistentState and setting working directory
      projects::startup();

      // initialize persistent state
      error = rsession::persistentState().initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION) ;

      // set working directory
      FilePath workingDir = dirs::getInitialWorkingDirectory();
      error = workingDir.makeCurrentPath();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);
         
      // start http connection listener
      error = waitWithTimeout(
            http_methods::startHttpConnectionListenerWithTimeout, 0, 100, 1);
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // run optional preflight script -- needs to be after the http listeners
      // so the proxy server sees that we have startup up
      error = runPreflightScript();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // server-only user file/directory initialization
      if (serverMode)
      {
         // r profile file
         ensureRProfile();

         // public folder
         ensurePublicFolder();

         // ensure the user has an R library directory
         if (!options.rLibsUser().empty())
            ensureRLibsUser(options.userHomePath(), options.rLibsUser());
      }

      // we've gotten through startup so let's log a start event
      using namespace monitor;
      client().logEvent(Event(kSessionScope, kSessionStartEvent));
      
      // install home and doc dir overrides if requested (for debugger mode)
      if (!options.rHomeDirOverride().empty())
         core::system::setenv("R_HOME", options.rHomeDirOverride());
      if (!options.rDocDirOverride().empty())
         core::system::setenv("R_DOC_DIR", options.rDocDirOverride());

      // r options
      rstudio::r::session::ROptions rOptions ;
      rOptions.userHomePath = options.userHomePath();
      rOptions.userScratchPath = userScratchPath;
      rOptions.scopedScratchPath = module_context::scopedScratchPath();
      rOptions.sessionScratchPath = module_context::sessionScratchPath();
      rOptions.logPath = options.userLogPath();
      rOptions.sessionPort = options.wwwPort();
      rOptions.startupEnvironmentFilePath = getStartupEnvironmentFilePath();
      rOptions.rEnvironmentDir = boost::bind(dirs::rEnvironmentDir);
      rOptions.rHistoryDir = boost::bind(dirs::rHistoryDir);
      rOptions.alwaysSaveHistory = boost::bind(alwaysSaveHistoryOption);
      rOptions.rSourcePath = options.coreRSourcePath();
      if (!desktopMode) // ignore r-libs-user in desktop mode
         rOptions.rLibsUser = options.rLibsUser();
      // CRAN repos: global server option trumps user setting
      if (!options.rCRANRepos().empty())
         rOptions.rCRANRepos = options.rCRANRepos();
      else
         rOptions.rCRANRepos = userSettings().cranMirror().url;
      rOptions.useInternet2 = userSettings().useInternet2();
      rOptions.rCompatibleGraphicsEngineVersion =
                              options.rCompatibleGraphicsEngineVersion();
      rOptions.serverMode = serverMode;
      rOptions.autoReloadSource = options.autoReloadSource();
      rOptions.restoreWorkspace = restoreWorkspaceOption();
      rOptions.saveWorkspace = saveWorkspaceOption();
      rOptions.rProfileOnResume = serverMode &&
                                  userSettings().rProfileOnResume();
      rOptions.sessionScope = options.sessionScope();
      
      // r callbacks
      rstudio::r::session::RCallbacks rCallbacks;
      rCallbacks.init = rInit;
      rCallbacks.consoleRead = console_input::rConsoleRead;
      rCallbacks.editFile = rEditFile;
      rCallbacks.showFile = rShowFile;
      rCallbacks.chooseFile = rChooseFile;
      rCallbacks.busy = rBusy;
      rCallbacks.consoleWrite = rConsoleWrite;
      rCallbacks.consoleHistoryReset = rConsoleHistoryReset;
      rCallbacks.locator = rLocator;
      rCallbacks.deferredInit = rDeferredInit;
      rCallbacks.suspended = rSuspended;
      rCallbacks.resumed = rResumed;
      rCallbacks.handleUnsavedChanges = rHandleUnsavedChanges;
      rCallbacks.quit = rQuit;
      rCallbacks.suicide = rSuicide;
      rCallbacks.cleanup = rCleanup ;
      rCallbacks.browseURL = rBrowseURL;
      rCallbacks.browseFile = rBrowseFile;
      rCallbacks.showHelp = rShowHelp;
      rCallbacks.showMessage = rShowMessage;
      rCallbacks.serialization = rSerialization;
      
      // run r (does not return, terminates process using exit)
      error = rstudio::r::session::run(rOptions, rCallbacks) ;
      if (error)
      {
          // this is logically equivilant to R_Suicide
          logExitEvent(Event(kSessionScope, kSessionSuicideEvent));

          // return failure
          return sessionExitFailure(error, ERROR_LOCATION);
      }
      
      // return success for good form
      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}



