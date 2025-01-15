/*
 * SessionMain.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
#include <boost/scope_exit.hpp>

#ifndef _WIN32
#include <netdb.h>
#include <pwd.h>
#include <sys/types.h>
#include <unistd.h>
#endif

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

#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/join.hpp>

#include <core/CrashHandler.hpp>
#include <core/BoostSignals.hpp>
#include <core/BoostThread.hpp>
#include <core/ConfigUtils.hpp>
#include <core/FileLock.hpp>
#include <core/Exec.hpp>
#include <core/Scope.hpp>
#include <core/Settings.hpp>
#include <core/Thread.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/ProgramStatus.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/URL.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/UriHandler.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>
#include <core/system/LibraryLoader.hpp>
#include <core/system/ParentProcessMonitor.hpp>
#include <core/system/Xdg.hpp>
#include <core/system/encryption/Encryption.hpp>

#ifdef _WIN32
# include <core/system/Win32RuntimeLibrary.hpp>
#endif

#include <core/system/FileMonitor.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/r_util/RSessionContext.hpp>
#include <core/r_util/REnvironment.hpp>
#include <core/WaitUtils.hpp>

#include <r/RExec.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RInterface.hpp>
#include <r/RJsonRpc.hpp>
#include <r/ROptions.hpp>
#include <r/RSexp.hpp>
#include <r/RUtil.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RSessionState.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RGraphics.hpp>
#include <r/session/REventLoop.hpp>

#include <monitor/MonitorClient.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionContentUrls.hpp>
#include <session/SessionServerRpc.hpp>
#include <session/SessionScopes.hpp>
#include <session/SessionClientEventService.hpp>
#include <session/SessionUrlPorts.hpp>
#include <session/RVersionSettings.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/StderrLogDestination.hpp>
#include <shared_core/system/encryption/EncryptionConfiguration.hpp>

#include "SessionAddins.hpp"

#include "SessionModuleContextInternal.hpp"
#include <session/SessionModuleContext.hpp>

#include "SessionClientEventQueue.hpp"
#include "SessionClientInit.hpp"
#include "SessionConsoleInput.hpp"
#include "SessionCRANOverlay.hpp"
#include "SessionDirs.hpp"
#include "SessionHttpMethods.hpp"
#include "SessionInit.hpp"
#include "SessionMainProcess.hpp"
#include "SessionRpc.hpp"
#include "SessionOfflineService.hpp"

#include <session/SessionRUtil.hpp>
#include <session/SessionPackageProvidedExtension.hpp>

#include "modules/RStudioAPI.hpp"
#include "modules/SessionAbout.hpp"
#include "modules/SessionAskPass.hpp"
#include "modules/SessionAskSecret.hpp"
#include "modules/SessionAuthoring.hpp"
#include "modules/SessionBreakpoints.hpp"
#include "modules/SessionCpp.hpp"
#include "modules/SessionHTMLPreview.hpp"
#include "modules/SessionClipboard.hpp"
#include "modules/SessionCodeSearch.hpp"
#include "modules/SessionConfigFile.hpp"
#include "modules/SessionConsole.hpp"
#include "modules/SessionCopilot.hpp"
#include "modules/SessionCRANMirrors.hpp"
#include "modules/SessionCrypto.hpp"
#include "modules/SessionDebugging.hpp"
#include "modules/SessionErrors.hpp"
#include "modules/SessionFiles.hpp"
#include "modules/SessionFind.hpp"
#include "modules/SessionGraphics.hpp"
#include "modules/SessionDependencies.hpp"
#include "modules/SessionDependencyList.hpp"
#include "modules/SessionDirty.hpp"
#include "modules/SessionWorkbench.hpp"
#include "modules/SessionHelp.hpp"
#include "modules/SessionPlots.hpp"
#include "modules/SessionPath.hpp"
#include "modules/SessionPackages.hpp"
#include "modules/SessionPackrat.hpp"
#include "modules/SessionPlumberViewer.hpp"
#include "modules/SessionProfiler.hpp"
#include "modules/SessionRAddins.hpp"
#include "modules/SessionRCompletions.hpp"
#include "modules/SessionRenv.hpp"
#include "modules/SessionRPubs.hpp"
#include "modules/SessionRHooks.hpp"
#include "modules/SessionRSConnect.hpp"
#include "modules/SessionShinyViewer.hpp"
#include "modules/SessionSpelling.hpp"
#include "modules/SessionSource.hpp"
#include "modules/SessionTests.hpp"
#include "modules/SessionThemes.hpp"
#include "modules/SessionTutorial.hpp"
#include "modules/SessionUpdates.hpp"
#include "modules/SessionVCS.hpp"
#include "modules/SessionHistory.hpp"
#include "modules/SessionLimits.hpp"
#include "modules/SessionLists.hpp"
#include "modules/SessionUserPrefs.hpp"
#include "modules/automation/SessionAutomation.hpp"
#include "modules/build/SessionBuild.hpp"
#include "modules/clang/SessionClang.hpp"
#include "modules/connections/SessionConnections.hpp"
#include "modules/customsource/SessionCustomSource.hpp"
#include "modules/data/SessionData.hpp"
#include "modules/environment/SessionEnvironment.hpp"
#include "modules/jobs/SessionJobs.hpp"
#include "modules/overlay/SessionOverlay.hpp"
#include "modules/plumber/SessionPlumber.hpp"
#include "modules/presentation/SessionPresentation.hpp"
#include "modules/preview/SessionPreview.hpp"
#include "modules/rmarkdown/RMarkdownTemplates.hpp"
#include "modules/rmarkdown/SessionRMarkdown.hpp"
#include "modules/rmarkdown/SessionRmdNotebook.hpp"
#include "modules/rmarkdown/SessionBookdown.hpp"
#include "modules/quarto/SessionQuarto.hpp"
#include "modules/shiny/SessionShiny.hpp"
#include "modules/shiny/SessionPyShiny.hpp"
#include "modules/sql/SessionSql.hpp"
#include "modules/stan/SessionStan.hpp"
#include "modules/viewer/SessionViewer.hpp"
#include "modules/SessionDiagnostics.hpp"
#include "modules/SessionMarkers.hpp"
#include "modules/SessionSnippets.hpp"
#include "modules/SessionUserCommands.hpp"
#include "modules/SessionRAddins.hpp"
#include "modules/mathjax/SessionMathJax.hpp"
#include "modules/panmirror/SessionPanmirror.hpp"
#include "modules/zotero/SessionZotero.hpp"
#include "modules/SessionLibPathsIndexer.hpp"
#include "modules/SessionObjectExplorer.hpp"
#include "modules/SessionReticulate.hpp"
#include "modules/SessionPythonEnvironments.hpp"
#include "modules/SessionCrashHandler.hpp"
#include "modules/SessionRVersions.hpp"
#include "modules/SessionTerminal.hpp"
#include "modules/SessionFonts.hpp"
#include "modules/SessionSystemResources.hpp"

#include <session/SessionProjectTemplate.hpp>

#include "modules/SessionGit.hpp"
#include "modules/SessionSVN.hpp"

#include <session/SessionConsoleProcess.hpp>
#include <session/SessionSuspend.hpp>

#include <session/projects/ProjectsSettings.hpp>
#include <session/projects/SessionProjects.hpp>
#include "projects/SessionProjectsInternal.hpp"

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

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

namespace {

std::string s_fallbackLibraryPath;

} // end anonymous namespace

bool disableExecuteRprofile()
{
   // check for session-specific override
   if (options().rRunRprofile() == kRunRprofileNo)
      return true;
   if (options().rRunRprofile() == kRunRprofileYes)
      return false;

   bool disableExecuteRprofile = false;

   const projects::ProjectContext& projContext = projects::projectContext();
   if (projContext.hasProject())
   {
      disableExecuteRprofile = projContext.config().disableExecuteRprofile;
   }

   return disableExecuteRprofile;
}

bool quitChildProcesses()
{
   // allow project override
   const projects::ProjectContext& projContext = projects::projectContext();
   if (projContext.hasProject())
   {
      switch(projContext.config().quitChildProcessesOnExit)
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
   return rsession::options().quitChildProcessesOnExit();
}

// terminates all child processes, including those unknown to us
// no waiting is done to ensure the children shutdown
// this is merely a best effort to stop children
void terminateAllChildProcesses()
{
   if (!quitChildProcesses())
      return;

   Error error = system::terminateChildProcesses();
   if (error)
      LOG_ERROR(error);
}

namespace overlay {
Error initialize();
Error initializeSessionProxy();
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

void handleINT(int)
{
   rstudio::r::exec::setInterruptsPending(true);
}

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
   // stop the offline service thread -- we don't want to service any
   // more incoming requests while preparing to restart
   offlineService().stop();
   
   // when launcher sessions restart, they need to set a special exit code
   // to ensure that the rsession-run script restarts the rsession process
   // instead of having to submit an entirely new launcher session
   int exitStatus = options().getBoolOverlayOption(kLauncherSessionOption)
         ? EX_SUSPEND_RESTART_LAUNCHER_SESSION
         : EX_CONTINUE;

   rstudio::r::session::RSuspendOptions options(exitStatus);
   Error error = json::readObjectParam(
            request.params, 0,
            "save_minimal", &(options.saveMinimal),
            "save_workspace", &(options.saveWorkspace),
            "exclude_packages", &(options.excludePackages),
            "after_restart", &(options.afterRestartCommand));
   if (error)
      return error;
   
   // read optional build library path (ignore errors)
   json::readObjectParam(
            request.params, 0,
            "built_package_path", &(options.builtPackagePath));

   pResponse->setAfterResponse(boost::bind(doSuspendForRestart, options));
   return Success();
}

void consoleWriteInput(const std::string& input)
{
   using namespace console_input;
   
   std::string prompt = rstudio::r::options::getOption<std::string>("prompt");
   consoleInput(prompt + input);
}


Error isAuthenticated(const core::json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // Automatic success. Auth failure would have prevented us from
   // getting here.
   pResponse->setResult(true);
   return Success();
}

Error initializeSessionState()
{
   using namespace rstudio::r::session;
   
   state::SessionStateCallbacks callbacks;
   callbacks.consoleWriteInput = consoleWriteInput;
   state::initialize(callbacks);
   
   return Success();
}

   
Error startClientEventService()
{
   return clientEventService().start(rsession::persistentState().activeClientId());
}

Error startOfflineService()
{
   return offlineService().start();
}

Error registerSignalHandlers()
{
   using boost::bind;
   using namespace rstudio::core::system;

   ExecBlock registerBlock;

   module_context::initializeConsoleCtrlHandler();

   // SIGINT: set interrupt flag on R session
   registerBlock.addFunctions()
         (bind(handleSignal, SigInt, handleINT));

   // USR1, USR2, and TERM: perform suspend in server mode
   if (rsession::options().programMode() == kSessionProgramModeServer)
   {
      registerBlock.addFunctions()
         (bind(handleSignal, SigUsr1, suspend::handleUSR1))
         (bind(handleSignal, SigUsr2, suspend::handleUSR2))
         (bind(handleSignal, SigTerm, suspend::handleUSR2));
   }
   // USR1 and USR2: ignore in desktop mode
   else
   {
      registerBlock.addFunctions()
         (bind(ignoreSignal, SigUsr1))
         (bind(ignoreSignal, SigUsr2));
   }

   return registerBlock.execute();
}


Error runPreflightScript()
{
   // alias options
   Options& options = rsession::options();

   // run the preflight script (if specified)
   if (rsession::options().programMode() == kSessionProgramModeServer)
   {
      FilePath preflightScriptPath = options.preflightScriptPath();
      if (!preflightScriptPath.isEmpty())
      {
         if (preflightScriptPath.exists())
         {
            // run the script (ignore errors and continue no matter what
            // the outcome of the script is)
            std::string script = preflightScriptPath.getAbsolutePath();
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
                                   preflightScriptPath.getAbsolutePath());
         }
      }
   }

   // always return success
   return Success();
}

// implemented below
void stopMonitorWorkerThread();
Error ensureLibRSoValid();

void exitEarly(int status)
{
   stopMonitorWorkerThread();
   FileLock::cleanUp();
   FilePath(s_fallbackLibraryPath).removeIfExists();
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

#ifdef _WIN32
   {
      // on Windows, check if we're using UCRT
      // ignore errors here since older versions of R don't define
      // the 'crt' memober on R.version
      std::string crt;
      rstudio::r::exec::evaluateString("R.version$crt", &crt);

      // initialize runtime library
      Error error = rstudio::core::runtime::initialize(crt == "ucrt");
      if (error)
         LOG_ERROR(error);
   }
#endif

   // execute core initialization functions
   using boost::bind;
   using namespace rstudio::core::system;
   using namespace rsession::module_context;
   ExecBlock initialize;
   initialize.addFunctions()

      // client event service
      (startClientEventService)
         
      // session state
      (initializeSessionState)

      // json-rpc listeners
      (bind(registerRpcMethod, kConsoleInput, bufferConsoleInput))
      (bind(registerRpcMethod, kSuspendForRestart, suspendForRestart))
      (bind(registerRpcMethod, kAuthStatus, isAuthenticated))

      // signal handlers
      (registerSignalHandlers)
         
      // main module context
      (module_context::initialize)

      // debugging
      (modules::debugging::initialize)

      // prefs (early init required -- many modules including projects below require
      // preference access)
      (modules::prefs::initialize)

      // projects (early project init required -- module inits below
      // can then depend on e.g. computed defaultEncoding)
      (projects::initialize)

      // source database
      (source_database::initialize)

      // content urls
      (content_urls::initialize)

      // URL port transformations
      (url_ports::initialize)

      // overlay R
      (bind(sourceModuleRFile, "SessionOverlay.R"))

      // addins
      (addins::initialize)

      // console processes
      (console_process::initialize)
         
      // http methods
      (http_methods::initialize)

      // r utils
      (r_utils::initialize)

      // suspend timeout
      (suspend::initialize)

      // modules with c++ implementations
      (modules::spelling::initialize)
      (modules::lists::initialize)
      (modules::limits::initialize)
      (modules::ppe::initialize)
      (modules::ask_pass::initialize)
      (modules::console::initialize)
#ifdef RSTUDIO_SERVER
      (modules::crypto::initialize)
#endif
      (modules::code_search::initialize)
      (modules::clipboard::initialize)
      (modules::clang::initialize)
      (modules::cpp::initialize)
      (modules::connections::initialize)
      (modules::files::initialize)
      (modules::find::initialize)
      (modules::environment::initialize)
      (modules::dependencies::initialize)
      (modules::dependency_list::initialize)
      (modules::dirty::initialize)
      (modules::workbench::initialize)
      (modules::data::initialize)
      (modules::help::initialize)
      (modules::presentation::initialize)
      (modules::preview::initialize)
      (modules::plots::initialize)
      (modules::packages::initialize)
      (modules::cran_mirrors::initialize)
      (modules::profiler::initialize)
      (modules::viewer::initialize)
      (modules::quarto::initialize)
      (modules::rmarkdown::initialize)
      (modules::rmarkdown::notebook::initialize)
      (modules::rmarkdown::templates::initialize)
      (modules::rmarkdown::bookdown::initialize)
      (modules::rpubs::initialize)
      (modules::pyshiny::initialize)
      (modules::shiny::initialize)
      (modules::sql::initialize)
      (modules::stan::initialize)
      (modules::plumber::initialize)
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
      (modules::plumber_viewer::initialize)
      (modules::rsconnect::initialize)
      (modules::packrat::initialize)
      (modules::renv::initialize)
      (modules::rhooks::initialize)
      (modules::r_packages::initialize)
      (modules::diagnostics::initialize)
      (modules::markers::initialize)
      (modules::snippets::initialize)
      (modules::user_commands::initialize)
      (modules::r_addins::initialize)
      (modules::projects::templates::initialize)
      (modules::mathjax::initialize)
      (modules::panmirror::initialize)
      (modules::zotero::initialize)
      (modules::rstudioapi::initialize)
      (modules::libpaths::initialize)
      (modules::explorer::initialize)
      (modules::ask_secret::initialize)
      (modules::reticulate::initialize)
      (modules::python_environments::initialize)
      (modules::tests::initialize)
      (modules::jobs::initialize)
      (modules::themes::initialize)
      (modules::customsource::initialize)
      (modules::crash_handler::initialize)
      (modules::r_versions::initialize)
      (modules::terminal::initialize)
      (modules::config_file::initialize)
      (modules::tutorial::initialize)
      (modules::graphics::initialize)
      (modules::fonts::initialize)
      (modules::system_resources::initialize)
      (modules::copilot::initialize)
      (modules::automation::initialize)

      // workers
      (workers::web_request::initialize)

      // R code
      (bind(sourceModuleRFile, "SessionCodeTools.R"))
      (bind(sourceModuleRFile, "SessionPatches.R"))

      (startOfflineService)

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
         if (!diagFile.isEmpty())
         {
            std::cout << "Diagnostics report written to: "
                      << diagFile << std::endl
                      << "Please audit the report and remove any sensitive information "
                      << "before submitting." << std::endl << std::endl;

            Error error = rstudio::r::exec::RFunction(".rs.showDiagnostics").call();
            if (error)
               LOG_ERROR(error);
         }
      }
      rsession::options().verifyInstallationHomeDir().removeIfExists();

      int exitCode = modules::overlay::verifyInstallation();
      exitEarly(exitCode);
   }

   // register all of the json rpc methods implemented in R
   json::JsonRpcMethods rMethods;
   error = rstudio::r::json::getRpcMethods(&rMethods);
   if (error)
      return error;

   for (json::JsonRpcMethod method : rMethods)
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
      LOG_ERROR_MESSAGE("The previous R session terminated abnormally");
      ClientEvent abendWarningEvent(kAbendWarning);
      rsession::clientEventQueue().add(abendWarningEvent);
   }

   if (s_printCharsetWarning)
      rstudio::r::exec::warning("Character set is not UTF-8; please change your locale");

   error = ensureLibRSoValid();
   if (error)
   {
      rstudio::r::session::reportWarningToConsole(error.getProperty("description")
         + ". Please contact your system administrator to correct this libR.so install.");
      LOG_ERROR(error);
   }

   // propagate console history options
   rstudio::r::session::consoleHistory().setRemoveDuplicates(
                                 prefs::userPrefs().removeHistoryDuplicates());


   // register function editor on windows
#ifdef _WIN32
   error = rstudio::r::exec::RFunction(".rs.registerFunctionEditor").call();
   if (error)
      LOG_ERROR(error);
#endif

   // clear out stale uploaded tmp files
   if (module_context::userUploadedFilesScratchPath().exists())
   {
      std::vector<FilePath> childPaths;
      error = module_context::userUploadedFilesScratchPath().getChildren(childPaths);
      if (error)
         LOG_ERROR(error);

      constexpr double secondsPerDay = 60*60*24;
      for (const FilePath& childPath : childPaths)
      {
         double diffTime = std::difftime(std::time(nullptr),
                                         childPath.getLastWriteTime());
         if (diffTime > secondsPerDay)
         {
            Error error = childPath.remove();
            if (error)
               LOG_ERROR(error);
         }
      }
   }

   // set flag indicating we had an abnormal end (if this doesn't get
   // unset by the time we launch again then we didn't terminate normally
   // i.e. either the process dying unexpectedly or a call to R_Suicide)
   bool isTesting =
         rsession::options().runTests() ||
         rsession::options().runAutomation();
   
   if (!isTesting)
   {
      rsession::persistentState().setAbend(true);
   }

   // begin session
   using namespace module_context;
   activeSession().beginSession(rVersion(), rHomeDir(), rVersionLabel());
   LOG_DEBUG_MESSAGE("Beginning session: " + activeSession().id() + " with activityState: " + activeSession().activityState() + " for username: " + core::system::username() + " effective uid: " + std::to_string(core::system::effectiveUserId()));

   // setup fork handlers
   main_process::setupForkHandlers();

   // success!
   return Success();
}

void rInitComplete()
{
   module_context::syncRSaveAction();
   module_context::events().onInitComplete();
}

void notifyIfRVersionChanged()
{
   using namespace rstudio::r::session::state;

   SessionStateInfo info = getSessionStateInfo();

   if (info.activeRVersion != info.suspendedRVersion)
   {
      const char* fmt =
            "R version change [%1% -> %2%] detected when restoring session; "
            "search path not restored";

      boost::format formatter(fmt);
      formatter
            % std::string(info.suspendedRVersion)
            % std::string(info.activeRVersion);

      std::string msg = formatter.str();
      ::REprintf("%s\n", msg.c_str());
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
   json::JsonRpcRequest request;

   bool succeeded = http_methods::waitForMethod(kEditCompleted,
                                        editEvent,
                                        suspend::disallowSuspend,
                                        &request);
   if (!succeeded)
      return false;

   // user cancelled edit
   if (request.params[0].isNull())
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
      return 0;
   }
}


FilePath rChooseFile(bool newFile)
{
   // fire choose file event
   ClientEvent chooseFileEvent(kChooseFile, newFile);
   rsession::clientEventQueue().add(chooseFileEvent);

   // wait for choose_file_completed
   json::JsonRpcRequest request;

   bool succeeded = http_methods::waitForMethod(kChooseFileCompleted,
                                        chooseFileEvent,
                                        suspend::disallowSuspend,
                                        &request);
   if (!succeeded)
      return FilePath();

   // extract the file name
   std::string fileName;
   if (!request.params[0].isNull())
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

void rConsoleReset()
{
   if (prefs::userPrefs().discardPendingConsoleInputOnError())
      rsession::console_input::clearConsoleInputBuffer();
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
   json::JsonRpcRequest request;

   bool succeeded = http_methods::waitForMethod(kLocatorCompleted,
                                        locatorEvent,
                                        suspend::disallowSuspend,
                                        &request);
   if (!succeeded)
      return false;

   // see if we got a point
   if ((request.params.getSize() > 0) && !request.params[0].isNull())
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
      // for files in the temporary directory, show as content
      //
      // (perform this check first to handle case where
      // tempdir lives within the user's home directory)
      if (filePath.isWithin(module_context::tempDir()))
      {
         module_context::showContent(title, filePath);
      }

      // for files in the user's home directory and pdfs use an external browser
      else if (module_context::isVisibleUserFile(filePath) ||
          (filePath.getExtensionLowerCase() == ".pdf"))
      {
         module_context::showFile(filePath);
      }

      // otherwise, show as content
      else
      {
         module_context::showContent(title, filePath);
      }
   }
   else // (rsession::options().programMode() == kSessionProgramModeDesktop
   {
#ifdef _WIN32
    if (!filePath.getExtension().empty())
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
   if ((filePath.getMimeContentType() == "text/html") &&
       filePath.isWithin(module_context::tempDir()) &&
       rstudio::r::util::hasRequiredVersion("2.14"))
   {
      std::string path = filePath.getRelativePath(module_context::tempDir());
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

   module_context::activeSession().setActivityState(core::r_util::kActivityStateSaved, true);
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
         // If the launcher is enabled, keeping the activeSession around until the job shows an exit status
         // at which point it will be removed by rworkspaces
         if (options().getBoolOverlayOption(kLauncherSessionOption))
            module_context::activeSession().setActivityState(r_util::kActivityStateDestroyPending, true);
         else
         {
            Error error = module_context::activeSession().destroy();
            if (error)
               LOG_ERROR(error);
         }

         // fire destroy event to modules
         module_context::events().onDestroyed();
      }

      // clean up locks
      FileLock::cleanUp();

      // stop file monitor (need to do this explicitly as otherwise we can
      // run into issues during close where the runtime attempts to clean
      // up its data structures at same time that monitor wants to exit)
      //
      // https://github.com/rstudio/rstudio/issues/5222
      system::file_monitor::stop();

      // stop the monitor thread
      stopMonitorWorkerThread();

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

      // terminate unknown child processes
      // processes launched by means we do not control
      terminateAllChildProcesses();
   }
   CATCH_UNEXPECTED_EXCEPTION

}

void rSerialization(int action, const FilePath& targetPath)
{
   json::Object serializationActionObject;
   serializationActionObject["type"] = action;
   if (!targetPath.isEmpty())
   {
      serializationActionObject["targetPath"] =
                           module_context::createAliasedPath(targetPath);
   }

   ClientEvent event(kSessionSerialization, serializationActionObject);
   rsession::clientEventQueue().add(event);
}

void rRunTests()
{
   // run tests
   int status = tests::run();
   
   // try to clean up session
   rCleanup(true);
   
   // exit if we haven't already
   exitEarly(status);
}

void rRunAutomationImpl()
{
   ClientEvent event(client_events::kRunAutomation);
   module_context::enqueClientEvent(event);
}

void rRunAutomation()
{
   // it seems like automation runs can fail to start if the
   // RunAutomation client event is received too soon after
   // startup, so we use a 3 second delay just to give the
   // client more time to fully finish initialization
   module_context::scheduleDelayedWork(
            boost::posix_time::seconds(3),
            rRunAutomationImpl,
            false);
}

void ensureRProfile()
{
   // check if we need to create the profile (bail if we don't)
   Options& options = rsession::options();
   if (!options.createProfile())
      return;

   FilePath rProfilePath = options.userHomePath().completePath(".Rprofile");
   if (!rProfilePath.exists() && !prefs::userState().autoCreatedProfile())
   {
      prefs::userState().setAutoCreatedProfile(true);

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

   FilePath publicPath = options.userHomePath().completePath("Public");
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

      FilePath noticePath = publicPath.completePath("AboutPublic.txt");
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

      // we no longer exit with ::abort because it generated unwanted exceptions
      // ::_Exit should perform the same functionality (not running destructors and exiting process)
      // without generating an exception
      std::_Exit(EXIT_FAILURE);
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
   // check options for session-specific override
   if (options().rRestoreWorkspace() == kRestoreWorkspaceNo)
      return false;
   else if (options().rRestoreWorkspace() == kRestoreWorkspaceYes)
      return true;

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
   return prefs::userPrefs().loadWorkspace() ||
          !rsession::options().initialEnvironmentFileOverride().isEmpty();
}

bool alwaysSaveHistoryOption()
{
   return prefs::userPrefs().alwaysSaveHistory();
}

FilePath getStartupEnvironmentFilePath()
{
   FilePath envFile = rsession::options().initialEnvironmentFileOverride();
   if (!envFile.isEmpty())
      return envFile;
   else
      return dirs::rEnvironmentDir().completePath(".RData");
}

void loadCranRepos(const std::string& repos,
                   rstudio::r::session::ROptions* pROptions)
{
   std::vector<std::string> parts;
   boost::split(parts, repos, boost::is_any_of("|"));
   pROptions->rCRANSecondary = "";

   std::vector<std::string> secondary;
   for (size_t idxParts = 0; idxParts < parts.size() - 1; idxParts += 2)
   {
      if (string_utils::toLower(parts[idxParts]) == "cran")
         pROptions->rCRANUrl = parts[idxParts + 1];
      else
         secondary.push_back(std::string(parts[idxParts]) + "|" + parts[idxParts + 1]);
   }
   pROptions->rCRANSecondary = algorithm::join(secondary, "|");
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
   json::JsonRpcRequest request;

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
      switch (projContext.config().saveWorkspace)
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
   std::string action = prefs::userPrefs().saveWorkspace();
   if (action == kSaveWorkspaceAlways)
      return rstudio::r::session::kSaveActionSave;
   else if (action == kSaveWorkspaceNever)
      return rstudio::r::session::kSaveActionNoSave;
   else if (action == kSaveWorkspaceAsk)
      return rstudio::r::session::kSaveActionAsk;

   return rstudio::r::session::kSaveActionAsk;
}

void syncRSaveAction()
{
   r::session::setSaveAction(saveWorkspaceAction());
}

} // namespace module_context
} // namespace session
} // namespace rstudio

namespace {

int sessionExitFailure(const core::Error& error,
                       const core::ErrorLocation& location)
{
   if (error)
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

/*
 * If linux couldn't load libR.so while the rsession binary was loading, it may
 * have loaded a different libR.so from one of the default library locations:
 * /lib/
 * /usr/local/lib/
 * /usr/local/lib64/R/
 * etc.
 * This is usually a system installation of R, potentially with a different
 * version of R than this session is configured to run.
 *
 * This can put the session in a state where it thinks it's running one version
 * of R, with all R libraries and paths set for version A, but the actual R
 * version we're running is some version B. We check for that here so we can
 * alert the user once R has completely loaded.
 */
Error ensureLibRSoValid()
{
#ifdef __linux__
   std::string libPath = rsession::module_context::rHomeDir() + "/lib/libR.so";
   Error libError = core::system::verifyLibrary(libPath);
   if (libError)
   {
      libError.addProperty("description", "R shared object (" + libPath + ") failed load test with error: " + libError.getProperty("dlerror") +
         "\nLinux may have loaded a different libR.so than requested. "
         "This can result in \"package was built under R version X.Y.Z\" user warnings and R_HOME/R version mismatches."
         "\nR_HOME: " + rsession::module_context::rHomeDir() +
         "\nR Version: " + rsession::module_context::rVersion());
      return libError;
   }
#endif
   return Success();
}

// io_service for performing monitor work on the thread
boost::asio::io_service s_monitorIoService;

void monitorWorkerThreadFunc()
{
   boost::asio::io_service::work work(s_monitorIoService);
   s_monitorIoService.run();
}

void stopMonitorWorkerThread()
{
   s_monitorIoService.stop();
}

void initMonitorClient()
{
   if (!options().getBoolOverlayOption(kLauncherSessionOption))
   {
      monitor::initializeMonitorClient(core::system::getenv(kMonitorSocketPathEnvVar),
                                       options().monitorSharedSecret(),
                                       s_monitorIoService);
   }
   else
   {
      modules::overlay::initMonitorClient(s_monitorIoService);
   }

   // start the monitor work thread
   // we handle monitor calls in a separate thread to ensure that calls
   // to the monitor (which are likely across machines and thus very expensive)
   // do not hamper the liveliness of the session as a whole
   core::thread::safeLaunchThread(monitorWorkerThreadFunc);
}

void beforeResume()
{
   LOG_DEBUG_MESSAGE("Setting activityState to resuming from: " + module_context::activeSession().activityState());
   module_context::activeSession().setActivityState(r_util::kActivityStateResuming, true);
}

void afterResume()
{
   LOG_DEBUG_MESSAGE("Resume complete");
}

std::string getenvForLog(const std::string& envVar)
{
   std::string envVal = core::system::getenv(envVar);
   return envVal.empty() ? "(empty)" : envVal;
}

void logStartingEnv()
{
#ifdef __linux__
   LOG_DEBUG_MESSAGE("Starting R session with LD_LIBRARY_PATH: " + getenvForLog("LD_LIBRARY_PATH"));
#endif
#ifdef __APPLE__
   std::string envVars[] = {"DYLD_LIBRARY_PATH", "DYLD_FALLBACK_LIBRARY_PATH", "DYLD_INSERT_LIBRARIES"};
   std::string msg = "Starting R session with: ";
   bool first = true;
   for (std::string varName:envVars)
   {
      if (!first)
         msg += ", ";
      else
         first = false;
      msg += varName + ": " + getenvForLog(varName);
   }
   LOG_DEBUG_MESSAGE(msg);
#endif
#ifdef _WIN32
   LOG_DEBUG_MESSAGE("Starting R session with PATH: " + getenvForLog("PATH"));
#endif
}

#ifdef _WIN32

void win32InvalidParameterHandler(const wchar_t* expression,
                                  const wchar_t* function,
                                  const wchar_t* file,
                                  unsigned int line,
                                  uintptr_t pReserved)
{
   // This page intentionally left blank.
}

#endif

// When the nscd service is enabled, the first call to getaddrinfo calls getenv("LOCALDOMAIN")
// that will intermittently crash if done on a background thread as getenv is not thread-safe.
// Making this call on the main thread first prevents that from happening. See rstudio-pro#4628
void mainThreadWorkaround()
{
#ifdef __linux__
   struct addrinfo hints, *addrInfoResult;
   int addrInfoErrorCode;

   memset (&hints, 0, sizeof (hints));

   addrInfoErrorCode = ::getaddrinfo ("localhost", NULL, &hints, &addrInfoResult);
   if (addrInfoErrorCode == 0)
      ::freeaddrinfo(addrInfoResult);
#endif
}

} // anonymous namespace

// run session
int main(int argc, char * const argv[])
{
   mainThreadWorkaround();

   try
   {
      // create and use a job object -- this is necessary on Electron as node
      // creates processes with JOB_OBJECT_LIMIT_SILENT_BREAKAWAY_OK, and we want
      // to make sure child processes for the rsession are terminated if the rsession
      // process itself is shut down.
      //
      // the downside here is that the desktop front-end and the session will no
      // longer be part of the same job, but because rsession is not detached from
      // the desktop frontend, it will still be closed if the frontend is closed
#ifdef _WIN32
      core::system::initHook();
#endif

      // set an invalid parameter handler -- we do this to match the behavior of
      // standalone builds of R, which use the MinGW runtime and so normally just
      // ignore invalid parameters (or quietly log them if configured to do so)
      //
      // https://github.com/mirror/mingw-w64/blob/eff726c461e09f35eeaed125a3570fa5f807f02b/mingw-w64-crt/crt/crtexe.c#L98-L108
#ifdef _WIN32
      _set_invalid_parameter_handler(win32InvalidParameterHandler);
#endif

      // save fallback library path
      s_fallbackLibraryPath = core::system::getenv("RSTUDIO_FALLBACK_LIBRARY_PATH");
      
      // sleep on startup if requested (mainly for debugging)
      std::string sleepOnStartup = core::system::getenv("RSTUDIO_SESSION_SLEEP_ON_STARTUP");
      if (!sleepOnStartup.empty())
      {
         int sleepDuration = core::safe_convert::stringTo<int>(sleepOnStartup, 0);
         if (sleepDuration > 0)
         {
            boost::this_thread::sleep(boost::posix_time::seconds(sleepDuration));
         }
      }
      
      // initialize thread id
      core::thread::initializeMainThreadId(boost::this_thread::get_id());
      core::system::setEnableCallbacksRequireMainThread(true);
      
      // initialize PATH -- needs to happen early on to avoid stomping on environment
      // variables set in a users .Renviron or .Rprofile
      {
         Error error = modules::path::initialize();
         if (error)
            LOG_ERROR(error);
      }

      // terminate immediately with given exit code (for testing/debugging)
      std::string exitOnStartup = core::system::getenv("RSTUDIO_SESSION_EXIT_ON_STARTUP");
      if (!exitOnStartup.empty())
      {
         int exitCode = core::safe_convert::stringTo<int>(exitOnStartup, EXIT_FAILURE);

         std::cerr << "RSession terminating with exit code " << exitCode << " as requested.\n";
         std::cout << "RSession will now exit.\n";
         return core::safe_convert::stringTo<int>(exitOnStartup, EXIT_FAILURE);
      }

      // make sure the USER environment variable is set
      // (seemingly it isn't in Docker?)
#ifndef _WIN32
      std::string user = core::system::getenv("USER");
      if (user.empty())
      {
         uid_t uid = geteuid();
         struct passwd* pw = getpwuid(uid);
         if (pw != nullptr)
         {
            std::string user = pw->pw_name;
            core::system::setenv("USER", user);
         }
      }
#endif
      
      // initialize log so we capture all errors including ones which occur
      // reading the config file (if we are in desktop mode then the log
      // will get re-initialized below)
      
      std::string programId = "rsession-" + core::system::username();
      core::log::setProgramId(programId);
      core::system::initializeLog(programId,
                                  core::log::LogLevel::WARN,
                                  core::system::xdg::userLogDir(),
                                  true); // force log dir to be under user's home directory

      // ignore SIGPIPE
      Error error = core::system::ignoreSignal(core::system::SigPipe);
      if (error)
         LOG_ERROR(error);

      // move to own process group
#ifndef _WIN32
      ::setpgrp();
#endif

      logStartingEnv();

      rstudio::r::session::setResumeCallbacks(beforeResume, afterResume);

      // get main thread id (used to distinguish forks which occur
      // from the main thread vs. child threads)
      main_process::initThreadId();

      // ensure LANG and UTF-8 character set
#ifndef _WIN32
      r_util::ensureLang();
#endif
      s_printCharsetWarning = !ensureUtf8Charset();

      // remove DYLD_INSERT_LIBRARIES variable (injected on macOS Desktop
      // to support restrictions with hardened runtime)
#ifdef __APPLE__
      core::system::unsetenv("DYLD_INSERT_LIBRARIES");
#endif

      // fix up HOME / R_USER environment variables
      // (some users on Windows report these having trailing
      // slashes, which confuses a number of RStudio routines)
      boost::regex reTrailing("[/\\\\]+$");
      for (const char* envvar : {"HOME", "R_USER"})
      {
         std::string oldVal = core::system::getenv(envvar);
         if (!oldVal.empty())
         {
            std::string newVal = boost::regex_replace(oldVal, reTrailing, "");
            if (oldVal != newVal)
               core::system::setenv(envvar, newVal);
         }
      }

      // Initialize rpc to rserver before options. Rpc validates session scope with db session storage
      error = socket_rpc::initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // read program options
      std::ostringstream osWarnings;
      Options& options = rsession::options();
      ProgramStatus status = options.read(argc, argv, osWarnings);
      std::string optionsWarnings = osWarnings.str();

      if (!optionsWarnings.empty())
         program_options::reportWarnings(optionsWarnings, ERROR_LOCATION);

      if (status.exit())
         return status.exitCode();

      // print version if requested
      if (options.version())
      {
         std::string gitCommit(RSTUDIO_GIT_COMMIT);
         std::cout << RSTUDIO_VERSION ", \"" RSTUDIO_RELEASE_NAME "\" "
                      "(" << gitCommit.substr(0, 8) << ", " RSTUDIO_BUILD_DATE ") "
                      "for " RSTUDIO_PACKAGE_OS << std::endl;
         return 0;
      }

      // convenience flags for server and desktop mode
      bool desktopMode = options.programMode() == kSessionProgramModeDesktop;
      bool serverMode = options.programMode() == kSessionProgramModeServer;

      // catch unhandled exceptions
      core::crash_handler::ProgramMode mode = serverMode ?
               core::crash_handler::ProgramMode::Server :
               core::crash_handler::ProgramMode::Desktop;
      error = core::crash_handler::initialize(mode);
      if (error)
         LOG_ERROR(error);

      // set program mode environment variable so any child processes
      // (like rpostback) can determine what the program mode is
      core::system::setenv(kRStudioProgramMode, options.programMode());

      // reflect stderr logging
      if (options.logStderr())
         log::addLogDestination(
            std::shared_ptr<log::ILogDestination>(new log::StderrLogDestination(
                                                     core::system::generateShortenedUuid(),
                                                     log::LogLevel::WARN,
                                                     log::LogMessageFormatType::PRETTY)));

      // initialize monitor but stop its thread on exit
      initMonitorClient();
      BOOST_SCOPE_EXIT(void)
      {
         stopMonitorWorkerThread();
      }
      BOOST_SCOPE_EXIT_END

      // register monitor log writer (but not in standalone or verify installation mode)
      if (!options.standalone() && !options.verifyInstallation())
      {
         core::log::addLogDestination(
            monitor::client().createLogDestination(core::system::generateShortenedUuid(), log::LogLevel::WARN, options.programIdentity()));
      }

      // initialize file lock config
      FileLock::initialize(desktopMode ? FileLock::LOCKTYPE_ADVISORY : FileLock::LOCKTYPE_LINKBASED);

      // re-initialize log for desktop mode
      if (desktopMode)
      {
         if (options.verifyInstallation())
         {
            core::system::initializeStderrLog(options.programIdentity(),
                                core::log::LogLevel::WARN);
         }
         else
         {
            core::system::initializeLog(options.programIdentity(),
                                        core::log::LogLevel::WARN,
                                        options.userLogPath(),
                                        true); // force log dir to be under user's home directory
         }
      }

      // Initialize encryption customization early on
      core::system::crypto::encryption::initialize();
      LOG_INFO_MESSAGE("Encryption versions set to max: " + std::to_string(system::crypto::getMaximumEncryptionVersion()) + \
                       ", min: " + std::to_string(system::crypto::getMinimumEncryptionVersion()));

      error = rpc::initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

#ifdef RSTUDIO_SERVER
      error = server_rpc::initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);
#endif

      // initialize overlay
      error = rsession::overlay::initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // set the rstudio environment variable so code can check for
      // whether rstudio is running
      core::system::setenv("RSTUDIO", "1");

      // The pid of the session process
      core::system::setenv("RSTUDIO_SESSION_PID", core::safe_convert::numberToString(::getpid()));

      // Mirror the R getOptions("width") value in an environment variable
      core::system::setenv("RSTUDIO_CONSOLE_WIDTH",
               safe_convert::numberToString(rstudio::r::options::kDefaultWidth));
 
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

      // ensure we aren't being started as a low (privileged) account
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
      FilePath rpostback = module_context::rPostbackPath();
      core::system::setenv(
            "RS_RPOSTBACK_PATH",
            string_utils::utf8ToSystem(rpostback.getAbsolutePath()));

      std::string firstProjectPath = "";
      if (!options.verifyInstallation())
      {
         // Validate the config and data directories.
         core::system::xdg::verifyUserDirs();

         // determine if this is a new user and get the first project path if so
         bool newUser = false;

         FilePath userScratchPath = options.userScratchPath();
         if (userScratchPath.exists())
         {
            // if the lists directory has not yet been created,
            // this is a new user
            FilePath listsPath = userScratchPath.completeChildPath("monitored/lists");
            if (!listsPath.exists())
               newUser = true;
         }
         else
         {
            // create the scratch path
            error = userScratchPath.ensureDirectory();
            if (error)
               return sessionExitFailure(error, ERROR_LOCATION);

            newUser = true;
         }

         if (newUser)
         {
            // this is a brand new user
            // check to see if there is a first project template
            if (!options.firstProjectTemplatePath().empty())
            {
               // copy the project template to the user's home dir
               FilePath templatePath = FilePath(options.firstProjectTemplatePath());
               if (templatePath.exists())
               {
                  error = templatePath.copyDirectoryRecursive(
                     options.userHomePath().completeChildPath(
                        templatePath.getFilename()));
                  if (error)
                     LOG_ERROR(error);
                  else
                  {
                     FilePath firstProjPath = options.userHomePath().completeChildPath(templatePath.getFilename())
                                                     .completeChildPath(templatePath.getFilename() + ".Rproj");
                     if (firstProjPath.exists())
                        firstProjectPath = firstProjPath.getAbsolutePath();
                     else
                        LOG_WARNING_MESSAGE("Could not find first project path " + firstProjPath.getAbsolutePath() +
                                            ". Please ensure the template contains an Rproj file.");
                  }
               }
            }
         }
      }

      // initialize user preferences and state
      error = prefs::initializePrefs();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);
      error = prefs::initializeState();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // startup projects -- must be after userSettings is initialized
      // but before persistentState and setting working directory
      projects::startup(firstProjectPath);

      // initialize persistent state
      error = rsession::persistentState().initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // set working directory
      FilePath workingDir = dirs::getInitialWorkingDirectory();
      error = workingDir.makeCurrentPath();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // override the active session's working directory
      // it is created with the default value of ~, so if our session options
      // have specified that a different directory should be used, we should
      // persist the value to the session state as soon as possible
      module_context::activeSession().setWorkingDir(workingDir.getAbsolutePath());

      // start http connection listener
      error = waitWithTimeout(
            http_methods::startHttpConnectionListenerWithTimeout, 0, 100, 1);
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // start session proxy to route traffic to localhost-listening applications (like Shiny)
      // this has to come after regular overlay initialization as it depends on persistent state
      error = overlay::initializeSessionProxy();
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

      // set ANSI support environment variable before running code from .Rprofile
      modules::console::syncConsoleColorEnv();

      // r options
      rstudio::r::session::ROptions rOptions;
      rOptions.projectPath = projects::projectContext().hasProject() ? projects::projectContext().directory() : FilePath();
      rOptions.userHomePath = options.userHomePath();
      rOptions.userScratchPath = options.userScratchPath();
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

      // name of the source of the CRAN repo value (for logging)
      std::string source;

      // CRAN repos configuration follows; order of precedence is:
      //
      // 1. The user's personal preferences file (rstudio-prefs.json), if CRAN repo editing is
      //    permitted by policy
      // 2. The system-wide preferences file (rstudio-prefs.json)
      // 3. The repo settings specified in the loaded version of R
      // 4. The session's repo settings (in rsession.conf/repos.conf)
      // 5. The server's repo settings
      // 6. The default repo settings from the preferences schema (user-prefs-schema.json)
      // 7. If all else fails, fallback to the default in the overlay
      std::string layerName;
      auto val = prefs::userPrefs().readValue(kCranMirror, &layerName);
      if (val && ((options.allowCRANReposEdit() && layerName == kUserPrefsUserLayer) ||
                  layerName == kUserPrefsSystemLayer))
      {
         // If we got here, either (a) there is a user value and the user is allowed to set their
         // own CRAN repos, or (b) there's a system value, which we always respect
         rOptions.rCRANUrl = prefs::userPrefs().getCRANMirror().url;
         rOptions.rCRANSecondary = prefs::userPrefs().getCRANMirror().secondary;
         source = layerName + "-level preference file";
      }
      else if (!core::system::getenv("RSTUDIO_R_REPO").empty())
      {
         // repo was specified in the r version
         std::string repo = core::system::getenv("RSTUDIO_R_REPO");

         // the repo can either be a repos.conf-style file, or a URL
         FilePath reposFile(repo);
         if (reposFile.exists())
         {
            std::string reposConfig = Options::parseReposConfig(reposFile);
            loadCranRepos(reposConfig, &rOptions);
            source = reposFile.getAbsolutePath() + " via RSTUDIO_R_REPO environment variable";
         }
         else
         {
            rOptions.rCRANUrl = repo;
            source = "RSTUDIO_R_REPO environment variable";
         }
      }
      else if (!options.rCRANMultipleRepos().empty())
      {
         // repo was specified in a repos file
         loadCranRepos(options.rCRANMultipleRepos(), &rOptions);
         source = "repos file";
      }
      else if (!options.rCRANUrl().empty())
      {
         // Global server option
         rOptions.rCRANUrl = options.rCRANUrl();
         source = "global session option";
      }
      else if (val && layerName == kUserPrefsDefaultLayer)
      {
         // If we found defaults in the prefs schema, use them but let the overlay
         // determine if further processing of the default URL is needed
         rOptions.rCRANUrl = overlay::mapCRANMirrorUrl(prefs::userPrefs().getCRANMirror().url);
         rOptions.rCRANSecondary = prefs::userPrefs().getCRANMirror().secondary;
         source = "preference defaults";
      }
      else
      {
         // Hard-coded repo of last resort so we don't wind up without a repo setting (users will
         // not be able to install packages without one)
         rOptions.rCRANUrl = overlay::getDefaultCRANMirror();
         source = "hard-coded default";
      }

      LOG_INFO_MESSAGE("Set CRAN URL for session to '" + rOptions.rCRANUrl + "' (source: " +
            source + ")");

      rOptions.rCompatibleGraphicsEngineVersion =
                              options.rCompatibleGraphicsEngineVersion();
      rOptions.serverMode = serverMode;
      rOptions.autoReloadSource = options.autoReloadSource();
      rOptions.restoreWorkspace = restoreWorkspaceOption();
      rOptions.saveWorkspace = saveWorkspaceOption();
      if (options.rRestoreWorkspace() != kRestoreWorkspaceDefault)
      {
         // if workspace restore is set to a non-default option, apply it to
         // environment restoration as well (the intent of the option is usually
         // to recover a session with an overhelming or problematic environment)
         rOptions.restoreEnvironmentOnResume =
            options.rRestoreWorkspace() == kRestoreWorkspaceYes;
      }
      rOptions.disableRProfileOnStart = disableExecuteRprofile();
      rOptions.rProfileOnResume = serverMode &&
                                  prefs::userPrefs().runRprofileOnResume();
      rOptions.packratEnabled = persistentState().settings().getBool("packratEnabled");
      rOptions.sessionScope = options.sessionScope();
      rOptions.runScript = options.runScript();
      rOptions.suspendOnIncompleteStatement = options.suspendOnIncompleteStatement();

      // r callbacks
      rstudio::r::session::RCallbacks rCallbacks;
      rCallbacks.init = rInit;
      rCallbacks.initComplete = rInitComplete;
      rCallbacks.consoleRead = console_input::rConsoleRead;
      rCallbacks.editFile = rEditFile;
      rCallbacks.showFile = rShowFile;
      rCallbacks.chooseFile = rChooseFile;
      rCallbacks.busy = rBusy;
      rCallbacks.consoleWrite = rConsoleWrite;
      rCallbacks.consoleHistoryReset = rConsoleHistoryReset;
      rCallbacks.consoleReset = rConsoleReset;
      rCallbacks.locator = rLocator;
      rCallbacks.deferredInit = rDeferredInit;
      rCallbacks.suspended = rSuspended;
      rCallbacks.resumed = rResumed;
      rCallbacks.handleUnsavedChanges = rHandleUnsavedChanges;
      rCallbacks.quit = rQuit;
      rCallbacks.suicide = rSuicide;
      rCallbacks.cleanup = rCleanup;
      rCallbacks.browseURL = rBrowseURL;
      rCallbacks.browseFile = rBrowseFile;
      rCallbacks.showHelp = rShowHelp;
      rCallbacks.showMessage = rShowMessage;
      rCallbacks.serialization = rSerialization;
      
      // set test callback if enabled
      if (options.runTests())
      {
         rCallbacks.runTests = rRunTests;
      }
     
      // set automation callback if enabled
      if (options.runAutomation())
         rCallbacks.runAutomation = rRunAutomation;

      // run r (does not return, terminates process using exit)
      error = rstudio::r::session::run(rOptions, rCallbacks);
      if (error)
      {
          // this is logically equivalent to R_Suicide
          logExitEvent(Event(kSessionScope, kSessionSuicideEvent));

          // return failure
          return sessionExitFailure(error, ERROR_LOCATION);
      }

      // return success for good form
      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION

   // if we got this far we had an unexpected exception
   return EXIT_FAILURE;
}
