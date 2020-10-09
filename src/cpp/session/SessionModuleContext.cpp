/*
 * SessionModuleContext.cpp
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

#include "SessionModuleContextInternal.hpp"

#include <vector>

#include <boost/assert.hpp>
#include <boost/utility.hpp>
#include <boost/format.hpp>
#include <boost/numeric/conversion/cast.hpp>

#include <core/BoostSignals.hpp>
#include <core/BoostThread.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/Log.hpp>
#include <core/Base64.hpp>
#include <core/Hash.hpp>
#include <core/Settings.hpp>
#include <core/DateTime.hpp>
#include <core/FileSerializer.hpp>
#include <core/markdown/Markdown.hpp>
#include <core/system/FileScanner.hpp>
#include <core/IncrementalCommand.hpp>
#include <core/PeriodicCommand.hpp>
#include <core/collection/Tree.hpp>

#include <core/http/Util.hpp>

#include <core/system/Process.hpp>
#include <core/system/FileMonitor.hpp>
#include <core/system/FileChangeEvent.hpp>
#include <core/system/Environment.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/system/Xdg.hpp>

#include <core/r_util/RPackageInfo.hpp>

#include <r/RSexp.hpp>
#include <r/RUtil.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>
#include <r/RJsonRpc.hpp>
#include <r/RSourceManager.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RFunctionHook.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RConsoleActions.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionClientEvent.hpp>
#include <session/SessionClientEventService.hpp>

#include <session/http/SessionRequest.hpp>

#include "SessionRpc.hpp"
#include "SessionClientEventQueue.hpp"
#include "SessionMainProcess.hpp"

#include <session/projects/SessionProjects.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionContentUrls.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

#include <shared_core/system/User.hpp>

#include "modules/SessionBreakpoints.hpp"
#include "modules/SessionFiles.hpp"
#include "modules/SessionReticulate.hpp"
#include "modules/SessionVCS.hpp"

#include "session-config.h"

using namespace rstudio::core;

namespace rstudio {
namespace session {   
namespace module_context {
      
namespace {

// simple service for handling console_input rpc requests
class ConsoleInputService : boost::noncopyable
{
public:
   
   ConsoleInputService()
   {
      core::thread::safeLaunchThread(
               boost::bind(&ConsoleInputService::run, this),
               &thread_);
   }
   
   ~ConsoleInputService()
   {
      enqueue("!");
   }
   
   void enqueue(const std::string& input)
   {
      requests_.enque(input);
   }
   
private:
   
   void run()
   {
      while (true)
      {
         std::string input;
         while (requests_.deque(&input))
         {
            if (input == "!")
               return;
            
            core::http::Response response;
            Error error = session::http::sendSessionRequest(
                     "/rpc/console_input",
                     input,
                     &response);
            if (error)
               LOG_ERROR(error);
         }
         
         requests_.wait();
      }
   }
   
   boost::thread thread_;
   core::thread::ThreadsafeQueue<std::string> requests_;
};

ConsoleInputService& consoleInputService()
{
   static ConsoleInputService instance;
   return instance;
}

// enqueClientEvent from R
SEXP rs_enqueClientEvent(SEXP nameSEXP, SEXP dataSEXP)
{
   try
   {
      // extract name
      std::string name = r::sexp::asString(nameSEXP);
      
      // extract json value (for primitive types we only support scalars
      // since this is the most common type of event data). to return an
      // array of primitives you need to wrap them in a list/object
      Error extractError;
      json::Value data;
      switch(TYPEOF(dataSEXP))
      {
         case NILSXP:
         {
            // do nothing, data will be a null json value
            break;
         }   
         case VECSXP:
         {
            extractError = r::json::jsonValueFromList(dataSEXP, &data);
            break;
         }   
         default:
         {
            extractError = r::json::jsonValueFromScalar(dataSEXP, &data);
            break;
         }
      }
      
      // check for error
      if (extractError)
      {
         LOG_ERROR(extractError);
         throw r::exec::RErrorException(
                                        "Couldn't extract json value from event data");
      }
      
      // determine the event type from the event name
      int type = -1;
      if (name == "package_status_changed")
         type = session::client_events::kPackageStatusChanged;
      else if (name == "unhandled_error")
         type = session::client_events::kUnhandledError;
      else if (name == "enable_rstudio_connect")
         type = session::client_events::kEnableRStudioConnect;
      else if (name == "shiny_gadget_dialog")
         type = session::client_events::kShinyGadgetDialog;
      else if (name == "rmd_params_ready")
         type = session::client_events::kRmdParamsReady;
      else if (name == "jump_to_function")
         type = session::client_events::kJumpToFunction;
      else if (name == "send_to_console")
         type = session::client_events::kSendToConsole;
      else if (name == "rprof_started")
        type = session::client_events::kRprofStarted;
      else if (name == "rprof_stopped")
        type = session::client_events::kRprofStopped;
      else if (name == "rprof_created")
        type = session::client_events::kRprofCreated;
      else if (name == "editor_command")
         type = session::client_events::kEditorCommand;
      else if (name == "navigate_shiny_frame")
         type = session::client_events::kNavigateShinyFrame;
      else if (name == "update_new_connection_dialog")
         type = session::client_events::kUpdateNewConnectionDialog;
      else if (name == "terminal_subprocs")
         type = session::client_events::kTerminalSubprocs;
      else if (name == "rstudioapi_show_dialog")
         type = session::client_events::kRStudioAPIShowDialog;
      else if (name == "object_explorer_event")
         type = session::client_events::kObjectExplorerEvent;
      else if (name == "send_to_terminal")
         type = session::client_events::kSendToTerminal;
      else if (name == "clear_terminal")
         type = session::client_events::kClearTerminal;
      else if (name == "add_terminal")
         type = session::client_events::kAddTerminal;
      else if (name == "activate_terminal")
         type = session::client_events::kActivateTerminal;
      else if (name == "terminal_cwd")
         type = session::client_events::kTerminalCwd;
      else if (name == "remove_terminal")
         type = session::client_events::kRemoveTerminal;
      else if (name == "show_page_viewer")
         type = session::client_events::kShowPageViewerEvent;
      else if (name == "data_output_completed")
         type = session::client_events::kDataOutputCompleted;
      else if (name == "new_document_with_code")
         type = session::client_events::kNewDocumentWithCode;
      else if (name == "available_packages_ready")
         type = session::client_events::kAvailablePackagesReady;
      else if (name == "compute_theme_colors")
         type = session::client_events::kComputeThemeColors;
      else if (name == "tutorial_command")
         type = session::client_events::kTutorialCommand;
      else if (name == "tutorial_launch")
         type = session::client_events::kTutorialLaunch;
      else if (name == "reticulate_event")
         type = session::client_events::kReticulateEvent;
      else if (name == "environment_assigned")
         type = session::client_events::kEnvironmentAssigned;
      else if (name == "environment_refresh")
         type = session::client_events::kEnvironmentRefresh;
      else if (name == "environment_removed")
         type = session::client_events::kEnvironmentRemoved;
      else if (name == "environment_changed")
         type = session::client_events::kEnvironmentChanged;

      if (type != -1)
      {
         ClientEvent event(type, data);
         session::clientEventQueue().add(event);
      }
      else
      {
         LOG_ERROR_MESSAGE("Unexpected event name from R: " + name);
      }
   }
   catch (r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   return R_NilValue;
}

SEXP rs_activatePane(SEXP paneSEXP)
{
   module_context::activatePane(r::sexp::safeAsString(paneSEXP));
   return R_NilValue;
}

// show error message from R
SEXP rs_showErrorMessage(SEXP titleSEXP, SEXP messageSEXP)
{
   std::string title = r::sexp::asString(titleSEXP);
   std::string message = r::sexp::asString(messageSEXP);
   module_context::showErrorMessage(title, message);
   return R_NilValue;
}

// log error message from R
SEXP rs_logErrorMessage(SEXP messageSEXP)
{
   std::string message = r::sexp::asString(messageSEXP);
   LOG_ERROR_MESSAGE(message);
   return R_NilValue;
}  

// log warning message from R
SEXP rs_logWarningMessage(SEXP messageSEXP)
{
   std::string message = r::sexp::asString(messageSEXP);
   LOG_WARNING_MESSAGE(message);
   return R_NilValue;
}  
   
// sleep the main thread (debugging function used to test rpc/abort)
SEXP rs_threadSleep(SEXP secondsSEXP)
{
   int seconds = r::sexp::asInteger(secondsSEXP);
   boost::this_thread::sleep(boost::posix_time::seconds(seconds));
   return R_NilValue;
}

// get rstudio mode
SEXP rs_rstudioProgramMode()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(session::options().programMode(), &rProtect);
}

// get rstudio edition
SEXP rs_rstudioEdition()
{
   return R_NilValue;
}

// get version
SEXP rs_rstudioVersion()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(std::string(RSTUDIO_VERSION), &rProtect);
}

// get release name
SEXP rs_rstudioReleaseName()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(std::string(RSTUDIO_RELEASE_NAME), &rProtect);
}

// get citation
SEXP rs_rstudioCitation()
{
   FilePath resPath = session::options().rResourcesPath();
   FilePath citationPath = resPath.completeChildPath("CITATION");

   // the citation file may not exist when working in e.g.
   // development configurations so just ignore if it's missing
   if (!citationPath.exists())
      return R_NilValue;

   SEXP citationSEXP;
   r::sexp::Protect rProtect;
   Error error = r::exec::RFunction("utils:::readCitationFile",
                                    citationPath.getAbsolutePath())
                                                   .call(&citationSEXP,
                                                         &rProtect);

   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   else
   {
      return citationSEXP;
   }
}

SEXP rs_setUsingMingwGcc49(SEXP usingSEXP)
{
   bool usingMingwGcc49 = r::sexp::asLogical(usingSEXP);
   prefs::userState().setUsingMingwGcc49(usingMingwGcc49);
   return R_NilValue;
}

// ensure file hidden
SEXP rs_ensureFileHidden(SEXP fileSEXP)
{
#ifdef _WIN32
   std::string file = r::sexp::asString(fileSEXP);
   if (!file.empty())
   {
      FilePath filePath = module_context::resolveAliasedPath(file);
      Error error = core::system::makeFileHidden(filePath);
      if (error)
         LOG_ERROR(error);
   }
#endif

   return R_NilValue;
}

SEXP rs_sourceDiagnostics()
{
   module_context::sourceDiagnostics();
   return R_NilValue;
}

SEXP rs_packageLoaded(SEXP pkgnameSEXP)
{
   if (main_process::wasForked())
      return R_NilValue;
   
   std::string pkgname = r::sexp::safeAsString(pkgnameSEXP);

   // fire server event
   events().onPackageLoaded(pkgname);

   // fire client event
   ClientEvent packageLoadedEvent(
            client_events::kPackageLoaded,
            json::Value(pkgname));
   enqueClientEvent(packageLoadedEvent);

   return R_NilValue;
}

SEXP rs_packageUnloaded(SEXP pkgnameSEXP)
{
   if (main_process::wasForked())
      return R_NilValue;
   
   std::string pkgname = r::sexp::safeAsString(pkgnameSEXP);
   ClientEvent packageUnloadedEvent(
            client_events::kPackageUnloaded,
            json::Value(pkgname));
   enqueClientEvent(packageUnloadedEvent);
   
   return R_NilValue;
}

SEXP rs_userPrompt(SEXP typeSEXP,
                   SEXP captionSEXP,
                   SEXP messageSEXP,
                   SEXP yesLabelSEXP,
                   SEXP noLabelSEXP,
                   SEXP includeCancelSEXP,
                   SEXP yesIsDefaultSEXP)
{
   UserPrompt prompt(r::sexp::asInteger(typeSEXP),
                     r::sexp::safeAsString(captionSEXP),
                     r::sexp::safeAsString(messageSEXP),
                     r::sexp::safeAsString(yesLabelSEXP),
                     r::sexp::safeAsString(noLabelSEXP),
                     r::sexp::asLogical(includeCancelSEXP),
                     r::sexp::asLogical(yesIsDefaultSEXP));

   UserPrompt::Response response = showUserPrompt(prompt);

   r::sexp::Protect rProtect;
   return r::sexp::create(response, &rProtect);
}

SEXP rs_restartR(SEXP afterRestartSEXP)
{
   std::string afterRestart = r::sexp::safeAsString(afterRestartSEXP);
   json::Object dataJson;
   dataJson["after_restart"] = afterRestart;
   ClientEvent event(client_events::kSuspendAndRestart, dataJson);
   module_context::enqueClientEvent(event);
   return R_NilValue;
}

SEXP rs_generateShortUuid()
{
   // generate a short uuid -- we make this available in R code so that it's
   // possible to create random identifiers without perturbing the state of the
   // RNG that R uses
   std::string uuid = core::system::generateShortenedUuid();
   r::sexp::Protect rProtect;
   return r::sexp::create(uuid, &rProtect);
}

SEXP rs_markdownToHTML(SEXP contentSEXP)
{
   std::string content = r::sexp::safeAsString(contentSEXP);
   std::string htmlContent;
   Error error = markdown::markdownToHTML(content,
                                          markdown::Extensions(),
                                          markdown::HTMLOptions(),
                                          &htmlContent);
   if (error)
   {
      LOG_ERROR(error);
      htmlContent = content;
   }

   r::sexp::Protect rProtect;
   return r::sexp::create(htmlContent, &rProtect);
}

inline std::string persistantValueName(SEXP nameSEXP)
{
   return "rstudioapi_persistent_values_" + r::sexp::safeAsString(nameSEXP);
}

SEXP rs_setPersistentValue(SEXP nameSEXP, SEXP valueSEXP)
{
   std::string name = persistantValueName(nameSEXP);
   std::string value = r::sexp::safeAsString(valueSEXP);
   persistentState().settings().set(name, value);
   return R_NilValue;
}

SEXP rs_getPersistentValue(SEXP nameSEXP)
{
   std::string name = persistantValueName(nameSEXP);
   if (persistentState().settings().contains(name))
   {
      std::string value = persistentState().settings().get(name);
      r::sexp::Protect rProtect;
      return r::sexp::create(value, &rProtect);
   }
   else
   {
      return R_NilValue;
   }
}

SEXP rs_setRpcDelay(SEXP delayMsSEXP)
{
   int delayMs = r::sexp::asInteger(delayMsSEXP);
   rstudio::session::rpc::setRpcDelay(delayMs);
   return delayMsSEXP;
}

} // anonymous namespace


// register a scratch path which is monitored

namespace {

typedef std::map<FilePath,OnFileChange> MonitoredScratchPaths;
MonitoredScratchPaths s_monitoredScratchPaths;
bool s_monitorByScanning = false;

FilePath monitoredParentPath()
{
   FilePath monitoredPath = userScratchPath().completePath(kMonitoredPath);
   Error error = monitoredPath.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   return monitoredPath;
}

bool monitoredScratchFilter(const FileInfo& fileInfo)
{
   return true;
}


void onFilesChanged(const std::vector<core::system::FileChangeEvent>& changes)
{
   for (const core::system::FileChangeEvent& fileChange : changes)
   {
      FilePath changedFilePath(fileChange.fileInfo().absolutePath());
      for (MonitoredScratchPaths::const_iterator
               it = s_monitoredScratchPaths.begin();
               it != s_monitoredScratchPaths.end();
               ++it)
      {
         if (changedFilePath.isWithin(it->first))
         {
            it->second(fileChange);
            break;
         }
      }

   }
}

boost::shared_ptr<tree<FileInfo> > monitoredPathTree()
{
   boost::shared_ptr<tree<FileInfo> > pMonitoredTree(new tree<FileInfo>());
   core::system::FileScannerOptions options;
   options.recursive = true;
   options.filter = monitoredScratchFilter;
   Error scanError = scanFiles(FileInfo(monitoredParentPath()),
                               options,
                               pMonitoredTree.get());
   if (scanError)
      LOG_ERROR(scanError);

   return pMonitoredTree;
}

bool scanForMonitoredPathChanges(boost::shared_ptr<tree<FileInfo> > pPrevTree)
{
   // check for changes
   std::vector<core::system::FileChangeEvent> changes;
   boost::shared_ptr<tree<FileInfo> > pCurrentTree = monitoredPathTree();
   core::system::collectFileChangeEvents(pPrevTree->begin(),
                                         pPrevTree->end(),
                                         pCurrentTree->begin(),
                                         pCurrentTree->end(),
                                         &changes);

   // fire events
   onFilesChanged(changes);

   // reset the tree
   *pPrevTree = *pCurrentTree;

   // scan again after interval
   return true;
}

void onMonitoringError(const Error& error)
{
   // log the error
   LOG_ERROR(error);

   // fallback to periodically scanning for changes
   if (!s_monitorByScanning)
   {
      s_monitorByScanning = true;
      module_context::schedulePeriodicWork(
         boost::posix_time::seconds(3),
         boost::bind(scanForMonitoredPathChanges, monitoredPathTree()),
         true);
   }
}

void initializeMonitoredUserScratchDir()
{
   // setup callbacks and register
   core::system::file_monitor::Callbacks cb;
   cb.onRegistrationError = onMonitoringError;
   cb.onMonitoringError = onMonitoringError;
   cb.onFilesChanged = onFilesChanged;
   core::system::file_monitor::registerMonitor(
                                    monitoredParentPath(),
                                    true,
                                    monitoredScratchFilter,
                                    cb);
}


} // anonymous namespace

FilePath registerMonitoredUserScratchDir(const std::string& dirName,
                                         const OnFileChange& onFileChange)
{
   // create the subdir
   FilePath dirPath = monitoredParentPath().completePath(dirName);
   Error error = dirPath.ensureDirectory();
   if (error)
      LOG_ERROR(error);

   // register the path
   s_monitoredScratchPaths[dirPath] = onFileChange;

   // return it
   return dirPath;
}


namespace {
   
// manage signals used for custom save and restore
class SuspendHandlers : boost::noncopyable 
{
public:
   SuspendHandlers() : nextGroup_(0) {}
   
public:   
   void add(const SuspendHandler& handler)
   {
      int group = nextGroup_++;
      suspendSignal_.connect(group, handler.suspend());
      resumeSignal_.connect(group, handler.resume());
   }
   
   void suspend(const r::session::RSuspendOptions& options,
                Settings* pSettings)
   {
      suspendSignal_(options, pSettings);
   }
   
   void resume(const Settings& settings)
   {
      resumeSignal_(settings);
   }
   
private:
   
   // use groups to ensure signal order. call suspend handlers in order 
   // of subscription and call resume handlers in reverse order of
   // subscription.
   
   int nextGroup_;
   
   RSTUDIO_BOOST_SIGNAL<void(const r::session::RSuspendOptions&,Settings*),
                 RSTUDIO_BOOST_LAST_VALUE<void>,
                 int,
                 std::less<int> > suspendSignal_;
                  
   RSTUDIO_BOOST_SIGNAL<void(const Settings&),
                 RSTUDIO_BOOST_LAST_VALUE<void>,
                 int,
                 std::greater<int> > resumeSignal_;
};

// handlers instance
SuspendHandlers& suspendHandlers()
{
   static SuspendHandlers instance;
   return instance;
}
   
} // anonymous namespace
   
void addSuspendHandler(const SuspendHandler& handler)
{
   suspendHandlers().add(handler);
}
   
void onSuspended(const r::session::RSuspendOptions& options,
                 Settings* pPersistentState)
{
   pPersistentState->beginUpdate();
   suspendHandlers().suspend(options, pPersistentState);
   pPersistentState->endUpdate();
   
}

void onResumed(const Settings& persistentState)
{
   suspendHandlers().resume(persistentState);
}

// idle work

namespace {

typedef std::vector<boost::shared_ptr<ScheduledCommand> >
                                                      ScheduledCommands;
ScheduledCommands s_scheduledCommands;
ScheduledCommands s_idleScheduledCommands;

void addScheduledCommand(boost::shared_ptr<ScheduledCommand> pCommand,
                         bool idleOnly)
{
   if (idleOnly)
      s_idleScheduledCommands.push_back(pCommand);
   else
      s_scheduledCommands.push_back(pCommand);
}

void executeScheduledCommands(ScheduledCommands* pCommands)
{
   // make a copy of scheduled commands before executing them
   // (this is because a scheduled command could result in
   // R code executing which in turn could cause the list of
   // scheduled commands to be mutated and these iterators
   // invalidated)
   ScheduledCommands commands = *pCommands;

   // execute all commands
   std::for_each(commands.begin(),
                 commands.end(),
                 boost::bind(&ScheduledCommand::execute, _1));

   // remove any commands which are finished
   pCommands->erase(
                 std::remove_if(
                    pCommands->begin(),
                    pCommands->end(),
                    boost::bind(&ScheduledCommand::finished, _1)),
                 pCommands->end());
}


} // anonymous namespace

void scheduleIncrementalWork(
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute,
         bool idleOnly)
{
   addScheduledCommand(boost::shared_ptr<ScheduledCommand>(
                           new IncrementalCommand(incrementalDuration,
                                                  execute)),
                         idleOnly);
}

void scheduleIncrementalWork(
         const boost::posix_time::time_duration& initialDuration,
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute,
         bool idleOnly)
{
   addScheduledCommand(boost::shared_ptr<ScheduledCommand>(
                           new IncrementalCommand(initialDuration,
                                                  incrementalDuration,
                                                  execute)),
                           idleOnly);
}


void schedulePeriodicWork(const boost::posix_time::time_duration& period,
                          const boost::function<bool()> &execute,
                          bool idleOnly,
                          bool immediate)
{
   addScheduledCommand(boost::shared_ptr<ScheduledCommand>(
                           new PeriodicCommand(period, execute, immediate)),
                       idleOnly);
}


namespace {

bool performDelayedWork(const boost::function<void()> &execute,
                        boost::shared_ptr<bool> pExecuted)
{
   if (*pExecuted)
      return false;

   *pExecuted = true;

   execute();

   return false;
}

bool isPackagePosixMakefile(const FilePath& srcPath)
{
   if (!srcPath.exists())
      return false;

   using namespace projects;
   ProjectContext& context = session::projects::projectContext();
   if (!context.hasProject())
      return false;

   if (context.config().buildType != r_util::kBuildTypePackage)
      return false;

   FilePath parentDir = srcPath.getParent();
   if (parentDir.getFilename() != "src")
      return false;

   FilePath packagePath = context.buildTargetPath();
   if (parentDir.getParent() != packagePath)
      return false;

   std::string filename = srcPath.getFilename();
   return (filename == "Makevars" ||
           filename == "Makevars.in" ||
           filename == "Makefile" ||
           filename == "Makefile.in");
}

void performIdleOnlyAsyncRpcMethod(
      const core::json::JsonRpcRequest& request,
      const core::json::JsonRpcFunctionContinuation& continuation,
      const core::json::JsonRpcAsyncFunction& function)
{
   if (request.isBackgroundConnection)
   {
      module_context::scheduleDelayedWork(
          boost::posix_time::milliseconds(100),
          boost::bind(function, request, continuation),
          true);
   }
   else
   {
      function(request, continuation);
   }
}

} // anonymous namespeace

void scheduleDelayedWork(const boost::posix_time::time_duration& period,
                         const boost::function<void()> &execute,
                         bool idleOnly)
{
   boost::shared_ptr<bool> pExecuted(new bool(false));

   schedulePeriodicWork(period,
                        boost::bind(performDelayedWork, execute, pExecuted),
                        idleOnly,
                        false);
}


void onBackgroundProcessing(bool isIdle)
{
   // allow process supervisor to poll for events
   processSupervisor().poll();

   // check for file monitor changes
   core::system::file_monitor::checkForChanges();

   // fire event
   events().onBackgroundProcessing(isIdle);

   // execute incremental commands
   executeScheduledCommands(&s_scheduledCommands);
   if (isIdle)
      executeScheduledCommands(&s_idleScheduledCommands);
}

#ifdef _WIN32

namespace {

BOOL CALLBACK consoleCtrlHandler(DWORD type)
{
   switch (type)
   {
   case CTRL_C_EVENT:
   case CTRL_BREAK_EVENT:
      rstudio::r::exec::setInterruptsPending(true);
      return true;
   default:
      return false;
   }
}

} // end anonymous namespace

#endif

void initializeConsoleCtrlHandler()
{
#ifdef _WIN32
   // accept Ctrl + C interrupts
   ::SetConsoleCtrlHandler(nullptr, FALSE);

   // remove an old registration (if any)
   ::SetConsoleCtrlHandler(consoleCtrlHandler, FALSE);

   // register console control handler
   ::SetConsoleCtrlHandler(consoleCtrlHandler, TRUE);
#endif
}

Error registerIdleOnlyAsyncRpcMethod(
                             const std::string& name,
                             const core::json::JsonRpcAsyncFunction& function)
{
   return registerAsyncRpcMethod(name,
                                 boost::bind(performIdleOnlyAsyncRpcMethod,
                                                _1, _2, function));
}

core::string_utils::LineEnding lineEndings(const core::FilePath& srcFile)
{
   // potential special case for Makevars
   if (prefs::userPrefs().useNewlinesInMakefiles() && isPackagePosixMakefile(srcFile))
      return string_utils::LineEndingPosix;

   // get the global default behavior
   string_utils::LineEnding lineEndings = prefs::userPrefs().lineEndings();

   // use project-level override if available
   using namespace session::projects;
   ProjectContext& context = projectContext();
   if (context.hasProject())
   {
      if (context.config().lineEndings != r_util::kLineEndingsUseDefault)
         lineEndings = (string_utils::LineEnding)context.config().lineEndings;
   }

   // if we are doing no conversion (passthrough) and there is an existing file
   // then we need to peek inside it to see what the existing line endings are
   if (lineEndings == string_utils::LineEndingPassthrough)
      string_utils::detectLineEndings(srcFile, &lineEndings);

   // return computed lineEndings
   return lineEndings;
}

Error readAndDecodeFile(const FilePath& filePath,
                        const std::string& encoding,
                        bool allowSubstChars,
                        std::string* pContents)
{
   // read contents
   std::string encodedContents;
   Error error = readStringFromFile(filePath, &encodedContents,
                                    options().sourceLineEnding());

   if (error)
      return error;

   // convert to UTF-8
   return convertToUtf8(encodedContents,
                        encoding,
                        allowSubstChars,
                        pContents);
}

Error convertToUtf8(const std::string& encodedContents,
                    const std::string& encoding,
                    bool allowSubstChars,
                    std::string* pContents)
{
   Error error;
   error = r::util::iconvstr(encodedContents, encoding, "UTF-8",
                             allowSubstChars, pContents);
   if (error)
      return error;

   stripBOM(pContents);

   // Detect invalid UTF-8 sequences and recover
   error = string_utils::utf8Clean(pContents->begin(),
                                   pContents->end(),
                                   '?');
   return error;
}

FilePath userHomePath()
{
   return session::options().userHomePath();
}

std::string createAliasedPath(const FileInfo& fileInfo)
{
   return createAliasedPath(FilePath(fileInfo.absolutePath()));
}
   
std::string createAliasedPath(const FilePath& path)
{
   return FilePath::createAliasedPath(path, userHomePath());
}   

FilePath resolveAliasedPath(const std::string& aliasedPath)
{
   return FilePath::resolveAliasedPath(aliasedPath, userHomePath());
}

FilePath userScratchPath()
{
   return session::options().userScratchPath();
}

FilePath userUploadedFilesScratchPath()
{
   return session::options().userScratchPath().completeChildPath("uploaded-files");
}

FilePath scopedScratchPath()
{
   if (projects::projectContext().hasProject())
      return projects::projectContext().scratchPath();
   else
      return userScratchPath();
}

FilePath sharedScratchPath()
{
   if (projects::projectContext().hasProject())
      return projects::projectContext().sharedScratchPath();
   else
      return userScratchPath();
}

FilePath sharedProjectScratchPath()
{
   if (projects::projectContext().hasProject())
   {
      return sharedScratchPath();
   }
   else
   {
      return FilePath();
   }
}

FilePath sessionScratchPath()
{
   r_util::ActiveSession& active = activeSession();
   if (!active.empty())
      return active.scratchPath();
   else
      return scopedScratchPath();
}

FilePath oldScopedScratchPath()
{
   if (projects::projectContext().hasProject())
      return projects::projectContext().oldScratchPath();
   else
      return userScratchPath();
}

std::string rLibsUser()
{
   return core::system::getenv("R_LIBS_USER");
}
   
bool isVisibleUserFile(const FilePath& filePath)
{
   return (filePath.isWithin(module_context::userHomePath()) &&
           !filePath.isWithin(module_context::userScratchPath()));
}

FilePath safeCurrentPath()
{
   return FilePath::safeCurrentPath(userHomePath());
}
   
FilePath tempFile(const std::string& prefix, const std::string& extension)
{
   return r::session::utils::tempFile(prefix, extension);
}

FilePath tempDir()
{
   return r::session::utils::tempDir();
}


FilePath findProgram(const std::string& name)
{
   std::string which;
   Error error = r::exec::RFunction("Sys.which", name).call(&which);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   else
   {
      return FilePath(which);
   }
}

bool addTinytexToPathIfNecessary()
{
   // avoid some pathological cases where e.g. TinyTeX folder
   // exists but doesn't have the pdflatex binary (don't
   // attempt to re-add the folder multiple times)
   static bool s_added = false;
   if (s_added)
      return true;
   
   if (!module_context::findProgram("pdflatex").isEmpty())
      return false;
   
   SEXP binDirSEXP = R_NilValue;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.tinytexBin").call(&binDirSEXP, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   
   if (!r::sexp::isString(binDirSEXP))
      return false;
   
   std::string binDir = r::sexp::asString(binDirSEXP);
   FilePath binPath = module_context::resolveAliasedPath(binDir);
   if (!binPath.exists())
      return false;
   
   s_added = true;
   core::system::addToSystemPath(binPath);
   return true;
}

bool isPdfLatexInstalled()
{
   addTinytexToPathIfNecessary();
   return !module_context::findProgram("pdflatex").isEmpty();
}

namespace {

bool hasTextMimeType(const FilePath& filePath)
{
   std::string mimeType = filePath.getMimeContentType("");
   if (mimeType.empty())
      return false;

   return boost::algorithm::starts_with(mimeType, "text/") ||
          boost::algorithm::ends_with(mimeType, "+xml") ||
          boost::algorithm::ends_with(mimeType, "/xml");
}

bool hasBinaryMimeType(const FilePath& filePath)
{
   // screen known text types
   if (hasTextMimeType(filePath))
      return false;

   std::string mimeType = filePath.getMimeContentType("");
   if (mimeType.empty())
      return false;

   return boost::algorithm::starts_with(mimeType, "application/") ||
          boost::algorithm::starts_with(mimeType, "image/") ||
          boost::algorithm::starts_with(mimeType, "audio/") ||
          boost::algorithm::starts_with(mimeType, "video/");
}

bool isJsonFile(const FilePath& filePath)
{
   std::string mimeType = filePath.getMimeContentType();
   return boost::algorithm::ends_with(mimeType, "json");
}

} // anonymous namespace

bool isTextFile(const FilePath& targetPath)
{
   if (hasTextMimeType(targetPath))
      return true;
   
   if (isJsonFile(targetPath))
      return true;

   if (hasBinaryMimeType(targetPath))
      return false;
   
   if (targetPath.getSize() == 0)
      return true;

#ifndef _WIN32
   
   // the behavior of the 'file' command in the macOS High Sierra beta
   // changed such that '--mime' no longer ensured that mime-type strings
   // were actually emitted. using '-I' instead appears to work around this.
#ifdef __APPLE__
   const char * const kMimeTypeArg = "-I";
#else
   const char * const kMimeTypeArg = "--mime";
#endif
   
   core::shell_utils::ShellCommand cmd("file");
   cmd << "--dereference";
   cmd << kMimeTypeArg;
   cmd << "--brief";
   cmd << targetPath;
   core::system::ProcessResult result;
   Error error = core::system::runCommand(cmd,
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
   {
      LOG_ERROR(error);
      return !!error;
   }

   // strip encoding
   std::string fileType = boost::algorithm::trim_copy(result.stdOut);
   fileType = fileType.substr(0, fileType.find(';'));

   // check value
   return boost::algorithm::starts_with(fileType, "text/") ||
          boost::algorithm::ends_with(fileType, "+xml") ||
          boost::algorithm::ends_with(fileType, "/xml") ||
          boost::algorithm::ends_with(fileType, "x-empty") ||
          boost::algorithm::equals(fileType, "application/json") ||
          boost::algorithm::equals(fileType, "application/postscript");
#else

   // read contents of file
   std::string contents;
   Error error = core::readStringFromFile(targetPath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return !!error;
   }

   // does it have null bytes?
   std::string nullBytes;
   nullBytes.push_back('\0');
   nullBytes.push_back('\0');
   return !boost::algorithm::contains(contents, nullBytes);

#endif

}

Error rBinDir(core::FilePath* pRBinDirPath)
{
   std::string rHomeBin;
   r::exec::RFunction rHomeBinFunc("R.home", "bin");
   Error error = rHomeBinFunc.call(&rHomeBin);
   if (error)
      return error;

   *pRBinDirPath = FilePath(rHomeBin);
   return Success();
}


Error rScriptPath(FilePath* pRScriptPath)
{
   FilePath rHomeBinPath;
   Error error = rBinDir(&rHomeBinPath);
   if (error)
      return error;

#ifdef _WIN32
*pRScriptPath = rHomeBinPath.completePath("Rterm.exe");
#else
*pRScriptPath = rHomeBinPath.completePath("R");
#endif
   return Success();
}

shell_utils::ShellCommand rCmd(const core::FilePath& rBinDir)
{
#ifdef _WIN32
      return shell_utils::ShellCommand(rBinDir.completeChildPath("Rcmd.exe"));
#else
      shell_utils::ShellCommand rCmd(rBinDir.completeChildPath("R"));
      rCmd << "CMD";
      return rCmd;
#endif
}

// get the R local help port
std::string rLocalHelpPort()
{
   std::string port;
   Error error = r::exec::RFunction(".rs.httpdPort").call(&port);
   if (error)
      LOG_ERROR(error);
   return port;
}

std::vector<FilePath> getLibPaths()
{
   std::vector<std::string> libPathsString;
   r::exec::RFunction rfLibPaths(".libPaths");
   Error error = rfLibPaths.call(&libPathsString);
   if (error)
      LOG_ERROR(error);

   std::vector<FilePath> libPaths;
   for (const std::string& path : libPathsString)
   {
      libPaths.push_back(module_context::resolveAliasedPath(path));
   }

   return libPaths;
}

bool disablePackages()
{
   return !core::system::getenv("RSTUDIO_DISABLE_PACKAGES").empty();
}

// check if a package is installed
bool isPackageInstalled(const std::string& packageName)
{
   r::session::utils::SuppressOutputInScope suppressOutput;

   bool installed = false;
   r::exec::RFunction func(".rs.isPackageInstalled", packageName);
   Error error = func.call(&installed);
   return !error ? installed : false;
}

bool isPackageVersionInstalled(const std::string& packageName,
                               const std::string& version)
{
   r::session::utils::SuppressOutputInScope suppressOutput;

   bool installed = false;
   r::exec::RFunction func(".rs.isPackageVersionInstalled",
                           packageName, version);
   Error error = func.call(&installed);
   return !error ? installed : false;
}

bool isMinimumDevtoolsInstalled()
{
   return isPackageVersionInstalled("devtools", "1.4.1");
}

bool isMinimumRoxygenInstalled()
{
   return isPackageVersionInstalled("roxygen2", "4.0");
}

std::string packageVersion(const std::string& packageName)
{
   std::string version;
   Error error = r::exec::RFunction(".rs.packageVersionString", packageName)
                                                               .call(&version);
   if (error)
   {
      LOG_ERROR(error);
      return "(Unknown)";
   }
   else
   {
      return version;
   }
}

bool hasMinimumRVersion(const std::string &version)
{
   bool hasVersion = false;
   boost::format fmt("getRversion() >= '%1%'");
   std::string versionTest = boost::str(fmt % version);
   Error error = r::exec::evaluateString(versionTest, &hasVersion);
   if (error)
      LOG_ERROR(error);
   return hasVersion;
}

PackageCompatStatus getPackageCompatStatus(
      const std::string& packageName,
      const std::string& packageVersion,
      int protocolVersion)
{
   r::session::utils::SuppressOutputInScope suppressOutput;
   int compatStatus = COMPAT_UNKNOWN;
   r::exec::RFunction func(".rs.getPackageCompatStatus",
                           packageName, packageVersion, protocolVersion);
   Error error = func.call(&compatStatus);
   if (error)
   {
      LOG_ERROR(error);
      return COMPAT_UNKNOWN;
   }
   return static_cast<PackageCompatStatus>(compatStatus);
}

Error installPackage(const std::string& pkgPath, const std::string& libPath)
{
   // get R bin directory
   FilePath rBinDir;
   Error error = module_context::rBinDir(&rBinDir);
   if (error)
      return error;

   // setup options and command
   core::system::ProcessOptions options;
#ifdef _WIN32
   shell_utils::ShellCommand installCommand(rBinDir.completeChildPath("R.exe"));
#else
   shell_utils::ShellCommand installCommand(rBinDir.completeChildPath("R"));
#endif

   installCommand << core::shell_utils::EscapeFilesOnly;

   // for packrat projects we execute the profile and set the working
   // directory to the project directory; for other contexts we just
   // propagate the R_LIBS
   if (module_context::packratContext().modeOn)
   {
      options.workingDir = projects::projectContext().directory();
   }
   else
   {
      installCommand << "--vanilla";
      core::system::Options env;
      core::system::environment(&env);
      std::string libPaths = libPathsString();
      if (!libPaths.empty())
         core::system::setenv(&env, "R_LIBS", libPathsString());
      options.environment = env;
   }

   installCommand << "CMD" << "INSTALL";

   // if there is a lib path then provide it
   if (!libPath.empty())
   {
      installCommand << "-l";
      installCommand << "\"" + libPath + "\"";
   }

   // add pakage path
   installCommand << "\"" + pkgPath + "\"";
   core::system::ProcessResult result;

   // run the command
   error = core::system::runCommand(installCommand,
                                    options,
                                    &result);

   if (error)
      return error;

   if ((result.exitStatus != EXIT_SUCCESS) && !result.stdErr.empty())
   {
      return systemError(boost::system::errc::state_not_recoverable,
                         "Error installing package: " + result.stdErr,
                         ERROR_LOCATION);
   }

   return Success();
}


std::string packageNameForSourceFile(const core::FilePath& sourceFilePath)
{
   // check whether we are in a package
   FilePath sourceDir = sourceFilePath.getParent();
   if (sourceDir.getFilename() == "R" &&
       r_util::isPackageDirectory(sourceDir.getParent()))
   {
      r_util::RPackageInfo pkgInfo;
      Error error = pkgInfo.read(sourceDir.getParent());
      if (error)
      {
         LOG_ERROR(error);
         return std::string();
      }

      return pkgInfo.name();
   }
   else
   {
      return std::string();
   }
}

bool isUnmonitoredPackageSourceFile(const FilePath& filePath)
{
   // if it's in the current package then it's fine
   using namespace projects;
   if (projectContext().hasProject() &&
      (projectContext().config().buildType == r_util::kBuildTypePackage) &&
       filePath.isWithin(projectContext().buildTargetPath()))
   {
      return false;
   }

   // ensure we are dealing with a directory
   FilePath dir = filePath;
   if (!dir.isDirectory())
      dir = filePath.getParent();

   // see if one the file's parent directories has a DESCRIPTION
   while (!dir.isEmpty())
   {
      FilePath descPath = dir.completeChildPath("DESCRIPTION");
      if (descPath.exists())
      {
         // get path relative to package dir
         std::string relative = filePath.getRelativePath(dir);
         if (boost::algorithm::starts_with(relative, "R/") ||
             boost::algorithm::starts_with(relative, "src/") ||
             boost::algorithm::starts_with(relative, "inst/include/"))
         {
            return true;
         }
         else
         {
            return false;
         }
      }

      dir = dir.getParent();
   }

   return false;
}


SEXP rs_packageNameForSourceFile(SEXP sourceFilePathSEXP)
{
   r::sexp::Protect protect;
   FilePath sourceFilePath = module_context::resolveAliasedPath(r::sexp::asString(sourceFilePathSEXP));
   return r::sexp::create(packageNameForSourceFile(sourceFilePath), &protect);
}

SEXP rs_base64encode(SEXP dataSEXP, SEXP binarySEXP)
{
   bool binary = r::sexp::asLogical(binarySEXP);
   const char* pData;
   std::size_t n;

   if (TYPEOF(dataSEXP) == STRSXP)
   {
      SEXP charSEXP = STRING_ELT(dataSEXP, 0);
      pData = CHAR(charSEXP);
      n = r::sexp::length(charSEXP);
   }
   else if (TYPEOF(dataSEXP) == RAWSXP)
   {
      pData = reinterpret_cast<const char*>(RAW(dataSEXP));
      n = r::sexp::length(dataSEXP);
   }
   else
   {
      LOG_ERROR_MESSAGE("Unexpected data type");
      return R_NilValue;
   }

   std::string output;
   Error error = base64::encode(pData, n, &output);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect protect;
   if (binary)
      return r::sexp::createRawVector(output, &protect);
   else
      return r::sexp::create(output, &protect);
}

SEXP rs_base64encodeFile(SEXP pathSEXP)
{
   std::string path = r::sexp::asString(pathSEXP);
   FilePath filePath = module_context::resolveAliasedPath(path);

   std::string output;
   Error error = base64::encode(filePath, &output);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect protect;
   return r::sexp::create(output, &protect);
}

SEXP rs_base64decode(SEXP dataSEXP, SEXP binarySEXP)
{
   bool binary = r::sexp::asLogical(binarySEXP);
   const char* pData;
   std::size_t n;

   if (TYPEOF(dataSEXP) == STRSXP)
   {
      SEXP charSEXP = STRING_ELT(dataSEXP, 0);
      pData = CHAR(charSEXP);
      n = r::sexp::length(charSEXP);
   }
   else if (TYPEOF(dataSEXP) == RAWSXP)
   {
      pData = reinterpret_cast<const char*>(RAW(dataSEXP));
      n = r::sexp::length(dataSEXP);
   }
   else
   {
      LOG_ERROR_MESSAGE("Unexpected data type");
      return R_NilValue;
   }

   std::string output;
   Error error = base64::decode(pData, n, &output);
   if (error)
      LOG_ERROR(error);

   r::sexp::Protect protect;
   if (binary)
      return r::sexp::createRawVector(output, &protect);
   else
      return r::sexp::create(output, &protect);
}

SEXP rs_resolveAliasedPath(SEXP pathSEXP)
{
   std::string path = r::sexp::asUtf8String(pathSEXP);
   FilePath resolved = module_context::resolveAliasedPath(path);
   r::sexp::Protect protect;
   return r::sexp::create(resolved.getAbsolutePath(), &protect);
}

SEXP rs_sessionModulePath()
{
   r::sexp::Protect protect;
   return r::sexp::create(
      session::options().modulesRSourcePath().getAbsolutePath(), &protect);
}

json::Object createFileSystemItem(const FileInfo& fileInfo)
{
   json::Object entry;

   std::string aliasedPath = module_context::createAliasedPath(fileInfo);
   std::string rawPath =
      module_context::resolveAliasedPath(aliasedPath).getAbsolutePath();

   entry["path"] = aliasedPath;
   if (aliasedPath != rawPath)
      entry["raw_path"] = rawPath;
   entry["dir"] = fileInfo.isDirectory();

   // length requires cast
   try
   {
      entry["length"] = boost::numeric_cast<boost::uint64_t>(fileInfo.size());
   }
   catch (const boost::bad_numeric_cast& e)
   {
      LOG_ERROR_MESSAGE(std::string("Error converting file size: ") +
                        e.what());
      entry["length"] = 0;
   }
   
   entry["exists"] = FilePath(fileInfo.absolutePath()).exists();

   entry["lastModified"] = date_time::millisecondsSinceEpoch(
                                                   fileInfo.lastWriteTime());
   return entry;
}

json::Object createFileSystemItem(const FilePath& filePath)
{
   return createFileSystemItem(FileInfo(filePath));
}

std::string rVersion()
{
   std::string rVersion;
   Error error = rstudio::r::exec::RFunction(".rs.rVersionString").call(
                                                                  &rVersion);
   if (error)
      LOG_ERROR(error);
   return rVersion;
}

std::string rVersionLabel()
{
   std::string versionLabel = system::getenv("RSTUDIO_R_VERSION_LABEL");
   return versionLabel;
}

std::string rHomeDir()
{
   // get the current R home directory
   std::string rVersionHome;
   Error error = rstudio::r::exec::RFunction("R.home").call(&rVersionHome);
   if (error)
      LOG_ERROR(error);
   return rVersionHome;
}


r_util::ActiveSession& activeSession()
{
   static boost::shared_ptr<r_util::ActiveSession> pSession;
   if (!pSession)
   {
      std::string id = options().sessionScope().id();
      if (!id.empty())
         pSession = activeSessions().get(id);
      else if (options().programMode() == kSessionProgramModeDesktop)
      {
         // if no active session, create one and use the launcher token as a
         // synthetic session ID
         //
         // we only do this in desktop mode to preserve backwards compatibility
         // with some functionality that depends on session data
         // persisting after rstudio has been closed
         //
         // this entire clause will likely need to be reverted in a future release
         // once we ensure that all execution modes have this squared away
         pSession = activeSessions().emptySession(options().launcherToken());
      }
      else
      {
         // if no scope was specified, we are in singleton session mode
         // check to see if there is an existing active session, and use that
         std::vector<boost::shared_ptr<r_util::ActiveSession> > sessions =
               activeSessions().list(userHomePath(), options().projectSharingEnabled());
         if (sessions.size() == 1)
         {
            // there is only one session, so this must be singleton session mode
            // reopen that session
            pSession = sessions.front();
         }
         else
         {
            // create a new session entry
            // this should not really run because in single session mode there will
            // only be one session but we'll create one here just in case for safety
            std::string sessionId;

            // pass ~ as the default working directory
            // this is resolved deeper in the code in SessionClientInit to turn into
            // the actual user preference or session default directory that it should be
            activeSessions().create(options().sessionScope().project(), "~", &sessionId);
            pSession = activeSessions().get(sessionId);
         }
      }
   }
   return *pSession;
}


r_util::ActiveSessions& activeSessions()
{
   static boost::shared_ptr<r_util::ActiveSessions> pSessions;
   if (!pSessions)
      pSessions.reset(new r_util::ActiveSessions(userScratchPath()));
   return *pSessions;
}



std::string libPathsString()
{
   // call into R to get the string
   std::string libPaths;
   Error error = r::exec::RFunction(".rs.libPathsString").call(&libPaths);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }

   // this is presumably system-encoded, so convert this to utf8 before return
   return string_utils::systemToUtf8(libPaths);
}

Error sourceModuleRFile(const std::string& rSourceFile)
{
   FilePath modulesPath = session::options().modulesRSourcePath();
   FilePath srcPath = modulesPath.completePath(rSourceFile);
   return r::sourceManager().sourceTools(srcPath);
}

Error sourceModuleRFileWithResult(const std::string& rSourceFile,
                                  const FilePath& workingDir,
                                  core::system::ProcessResult* pResult)
{
   // R binary
   FilePath rProgramPath;
   Error error = module_context::rScriptPath(&rProgramPath);
   if (error)
      return error;
   std::string rBin = string_utils::utf8ToSystem(rProgramPath.getAbsolutePath());

   // vanilla execution of a single expression
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--vanilla");
   args.push_back("-e");

   // build source command
   boost::format fmt("source('%1%')");
   FilePath modulesPath = session::options().modulesRSourcePath();
   FilePath srcFilePath = modulesPath.completePath(rSourceFile);
   std::string srcPath = core::string_utils::utf8ToSystem(
      srcFilePath.getAbsolutePath());
   std::string escapedSrcPath = string_utils::jsLiteralEscape(srcPath);
   std::string cmd = boost::str(fmt % escapedSrcPath);
   args.push_back(cmd);

   // options
   core::system::ProcessOptions options;
   options.terminateChildren = true;
   options.workingDir = workingDir;

   // allow child process to inherit our R_LIBS
   core::system::Options childEnv;
   core::system::environment(&childEnv);
   std::string libPaths = libPathsString();
   if (!libPaths.empty())
      core::system::setenv(&childEnv, "R_LIBS", libPaths);
   options.environment = childEnv;

   // run the child
   return core::system::runProgram(rBin, args, "", options, pResult);
}

      
void enqueClientEvent(const ClientEvent& event)
{
   session::clientEventQueue().add(event);
}

bool isDirectoryMonitored(const FilePath& directory)
{
   return session::projects::projectContext().isMonitoringDirectory(directory) ||
          session::modules::files::isMonitoringDirectory(directory);
}

bool isRScriptInPackageBuildTarget(const FilePath &filePath)
{
   using namespace session::projects;

   if (projectContext().config().buildType == r_util::kBuildTypePackage)
   {
      FilePath pkgPath = projects::projectContext().buildTargetPath();
      std::string pkgRelative = filePath.getRelativePath(pkgPath);
      return boost::algorithm::starts_with(pkgRelative, "R/");
   }
   else
   {
      return false;
   }
}

SEXP rs_isRScriptInPackageBuildTarget(SEXP filePathSEXP)
{
   r::sexp::Protect protect;
   FilePath filePath = module_context::resolveAliasedPath(
      r::sexp::asUtf8String(filePathSEXP));
   return r::sexp::create(isRScriptInPackageBuildTarget(filePath), &protect);
}

bool fileListingFilter(const core::FileInfo& fileInfo, bool hideObjectFiles)
{
   // check extension for special file types which are always visible
   core::FilePath filePath(fileInfo.absolutePath());
   std::string ext = filePath.getExtensionLowerCase();
   std::string name = filePath.getFilename();
   if (ext == ".r" ||
       ext == ".rprofile" ||
       ext == ".rbuildignore" ||
       ext == ".rdata"    ||
       ext == ".rhistory" ||
       ext == ".ruserdata" ||
       ext == ".renviron" ||
       ext == ".httr-oauth" ||
       ext == ".github" ||
       ext == ".gitignore" ||
       ext == ".gitattributes" ||
       ext == ".circleci")
   {
      return true;
   }
   else if (name == ".travis.yml")
   {
      return true;
   }
   else if (name == ".gitlab-ci.yml")
   {
      return true;
   }
   else if (name == ".build.yml")
   {
      return true;
   }
   else if (hideObjectFiles &&
            (ext == ".o" || ext == ".so" || ext == ".dll") &&
            filePath.getParent().getFilename() == "src")
   {
      return false;
   }
   else
   {
      return !filePath.isHidden();
   }
}

namespace {

// enque file changed event
void enqueFileChangedEvent(
      const core::system::FileChangeEvent& event,
      boost::shared_ptr<modules::source_control::FileDecorationContext> pCtx)
{
   // create file change object
   json::Object fileChange;
   fileChange["type"] = event.type();
   json::Object fileSystemItem = createFileSystemItem(event.fileInfo());

   if (prefs::userPrefs().vcsAutorefresh())
   {
      pCtx->decorateFile(
               FilePath(event.fileInfo().absolutePath()),
               &fileSystemItem);
   }

   fileChange["file"] = fileSystemItem;

   // enque it
   ClientEvent clientEvent(client_events::kFileChanged, fileChange);
   module_context::enqueClientEvent(clientEvent);
}

} // namespace

void enqueFileChangedEvent(const core::system::FileChangeEvent &event)
{
   FilePath filePath = FilePath(event.fileInfo().absolutePath());

   using namespace session::modules::source_control;
   auto pCtx = fileDecorationContext(filePath, true);
   enqueFileChangedEvent(event, pCtx);
}

void enqueFileChangedEvents(const core::FilePath& vcsStatusRoot,
                            const std::vector<core::system::FileChangeEvent>& events)
{
   using namespace modules::source_control;

   if (events.empty())
      return;

   // try to find the common parent of the events
   FilePath commonParentPath = FilePath(events.front().fileInfo().absolutePath()).getParent();
   for (const core::system::FileChangeEvent& event : events)
   {
      // if not within the common parent then revert to the vcs status root
      if (!FilePath(event.fileInfo().absolutePath()).isWithin(commonParentPath))
      {
         commonParentPath = vcsStatusRoot;
         break;
      }
   }

   using namespace session::modules::source_control;
   auto pCtx = fileDecorationContext(commonParentPath, true);

   // fire client events as necessary
   for (const core::system::FileChangeEvent& event : events)
   {
      enqueFileChangedEvent(event, pCtx);
   }
}

Error enqueueConsoleInput(const std::string& consoleInput)
{
   // construct our JSON RPC
   json::Array jsonParams;
   jsonParams.push_back(consoleInput);
   jsonParams.push_back("");
   
   json::Object jsonRpc;
   jsonRpc["method"] = "console_input";
   jsonRpc["params"] = jsonParams;
   jsonRpc["clientId"] = clientEventService().clientId();
   
   // serialize for transmission
   std::ostringstream oss;
   jsonRpc.write(oss);
   
   // and fire it off
   consoleInputService().enqueue(oss.str());
   
   return Success();
}

// NOTE: we used to call explicitly back into r::session to write output
// and errors however the fact that these functions are called from
// background threads during std stream capture means that they must
// provide thread safety guarantees. since the r::session module generally
// assumes single-threaded operation it is more straightforward to have
// the code here ape the actions of RWriteConsoleEx with a more explicit
// thread safety constraint

void consoleWriteOutput(const std::string& output)
{
   // NOTE: all actions herein must be threadsafe! (see comment above)

   // add console action
   r::session::consoleActions().add(kConsoleActionOutput, output);

   // enque write output (same as session::rConsoleWrite)
   ClientEvent event(client_events::kConsoleWriteOutput, output);
   enqueClientEvent(event);

   // fire event
   module_context::events().onConsoleOutput(
                                    module_context::ConsoleOutputNormal,
                                    output);
}

void consoleWriteError(const std::string& message)
{
   // NOTE: all actions herein must be threadsafe! (see comment above)

   // add console action
   r::session::consoleActions().add(kConsoleActionOutputError, message);

   // enque write error (same as session::rConsoleWrite)
   ClientEvent event(client_events::kConsoleWriteError, message);
   enqueClientEvent(event);

   // fire event
   module_context::events().onConsoleOutput(
                                    module_context::ConsoleOutputError,
                                    message);
}

void showErrorMessage(const std::string& title, const std::string& message)
{
   session::clientEventQueue().add(showErrorMessageEvent(title, message));
}

void showFile(const FilePath& filePath, const std::string& window)
{
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      // for pdfs handle specially for each platform
      if (filePath.getExtensionLowerCase() == ".pdf")
      {
         std::string path = filePath.getAbsolutePath();
         Error error = r::exec::RFunction(".rs.shellViewPdf", path).call();
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         ClientEvent event = browseUrlEvent("file:///" + filePath.getAbsolutePath());
         module_context::enqueClientEvent(event);
      }
   }
   else if (session::options().programMode() == kSessionProgramModeServer)
   {
      if (!isPathViewAllowed(filePath))
      {
         module_context::showErrorMessage(
            "File Download Error",
            "This system administrator has not granted you permission "
            "to view this file.\n");
      }
      else if (session::options().allowFileDownloads())
      {
         std::string url = createFileUrl(filePath);
         ClientEvent event = browseUrlEvent(url);
         module_context::enqueClientEvent(event);
      }
      else
      {
         module_context::showErrorMessage(
            "File Download Error",
            "Unable to show file because file downloads are restricted "
            "on this server.\n");
      }
   }
}

std::string createFileUrl(const core::FilePath& filePath)
{
    // determine url based on whether this is in ~ or not
    std::string url;
    if (isVisibleUserFile(filePath))
    {
       std::string relPath = filePath.getRelativePath(
          module_context::userHomePath());
       url = "files/" + relPath;
    }
    else
    {
       url = "file_show?path=" + core::http::util::urlEncode(
          filePath.getAbsolutePath(), true);
    }
    return url;
}


void showContent(const std::string& title, const core::FilePath& filePath)
{
   // first provision a content url
   std::string contentUrl = content_urls::provision(title, filePath);

   // fire event
   json::Object contentItem;
   contentItem["title"] = title;
   contentItem["contentUrl"] = contentUrl;
   ClientEvent event(client_events::kShowContent, contentItem);
   module_context::enqueClientEvent(event);
}


std::string resourceFileAsString(const std::string& fileName)
{
   FilePath resPath = session::options().rResourcesPath();
   FilePath filePath = resPath.completePath(fileName);
   std::string fileContents;
   Error error = readStringFromFile(filePath, &fileContents);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }

   return fileContents;
}

// given a pair of paths, return the second in the context of the first
std::string pathRelativeTo(const FilePath& sourcePath,
                           const FilePath& targetPath)
{
   std::string relative;
   if (targetPath == sourcePath)
   {
      relative = ".";
   }
   else if (targetPath.isWithin(sourcePath))
   {
      relative = targetPath.getRelativePath(sourcePath);
   }
   else
   {
      relative = createAliasedPath(targetPath);
   }
   return relative;
}

void activatePane(const std::string& pane)
{
   ClientEvent event(client_events::kActivatePane, pane);
   module_context::enqueClientEvent(event);
}

FilePath shellWorkingDirectory()
{
   std::string initialDirSetting = prefs::userPrefs().terminalInitialDirectory();
   if (initialDirSetting == kTerminalInitialDirectoryProject)
   {
      if (projects::projectContext().hasProject())
         return projects::projectContext().directory();
      else
         return module_context::safeCurrentPath();
   }
   else if (initialDirSetting == kTerminalInitialDirectoryCurrent)
      return module_context::safeCurrentPath();
   else if (initialDirSetting == kTerminalInitialDirectoryHome)
      return system::User::getUserHomePath();
   else
      return module_context::safeCurrentPath();
}

Events& events()
{
   static Events instance;
   return instance;
}


core::system::ProcessSupervisor& processSupervisor()
{
   static core::system::ProcessSupervisor instance;
   return instance;
}

FilePath sourceDiagnostics()
{
   FilePath diagnosticsPath =
         options().coreRSourcePath().completeChildPath("Diagnostics.R");
   
   Error error = r::exec::RFunction("source")
         .addParam(string_utils::utf8ToSystem(diagnosticsPath.getAbsolutePath()))
         .addParam("chdir", true)
         .call();
   
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   else
   {
      // note this path is also in Diagnostics.R so changes to the path
      // need to be synchronized there
      std::string reportPath = core::system::getenv("RSTUDIO_DIAGNOSTICS_REPORT");
      if (reportPath.empty())
         reportPath = "~/rstudio-diagnostics/diagnostics-report.txt";
      return module_context::resolveAliasedPath(reportPath);
   }
}
   
namespace {
void beginRpcHandler(json::JsonRpcFunction function,
                     json::JsonRpcRequest request,
                     std::string asyncHandle)
{
   try
   {
      json::JsonRpcResponse response;
      Error error = function(request, &response);
      BOOST_ASSERT(!response.hasAfterResponse());
      if (error)
      {
         response.setError(error);
      }
      json::Object value;
      value["handle"] = asyncHandle;
      value["response"] = response.getRawResponse();
      ClientEvent evt(client_events::kAsyncCompletion, value);
      enqueClientEvent(evt);
   }
   CATCH_UNEXPECTED_EXCEPTION

}
} // anonymous namespace

core::Error executeAsync(const json::JsonRpcFunction& function,
                         const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // Immediately return a response to the server with a handle that
   // identifies this invocation. In the meantime, kick off the actual
   // operation on a new thread.

   std::string handle = core::system::generateUuid(true);
   core::thread::safeLaunchThread(bind(beginRpcHandler,
                                       function,
                                       request,
                                       handle));
   pResponse->setAsyncHandle(handle);
   return Success();
}

core::json::Object compileOutputAsJson(const CompileOutput& compileOutput)
{
   json::Object compileOutputJson;
   compileOutputJson["type"] = compileOutput.type;
   compileOutputJson["output"] = compileOutput.output;
   return compileOutputJson;
}

std::string CRANReposURL()
{
   std::string url;
   r::exec::evaluateString("getOption('repos')[['CRAN']]", &url);
   if (url.empty())
      url = prefs::userPrefs().getCRANMirror().url;
   return url;
}

std::string rstudioCRANReposURL()
{
   std::string protocol = prefs::userPrefs().useSecureDownload() ?
                                                           "https" : "http";
   return protocol + "://cran.rstudio.com/";
}

SEXP rs_rstudioCRANReposUrl()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(rstudioCRANReposURL(), &rProtect);
}

std::string downloadFileMethod(const std::string& defaultMethod)
{
   std::string method;
   Error error = r::exec::evaluateString(
                           "getOption('download.file.method', '" +
                            defaultMethod + "')", &method);
   if (error)
      LOG_ERROR(error);
   return method;
}

std::string CRANDownloadOptions()
{
   std::string options;
   Error error = r::exec::RFunction(".rs.CRANDownloadOptionsString").call(&options);
   if (error)
      LOG_ERROR(error);
   return options;
}

bool haveSecureDownloadFileMethod()
{
   bool secure = false;
   Error error = r::exec::RFunction(".rs.haveSecureDownloadFileMethod").call(
                                                                      &secure);
   if (error)
      LOG_ERROR(error);
   return secure;
}

shell_utils::ShellCommand RCommand::buildRCmd(const core::FilePath& rBinDir)
{
#if defined(_WIN32)
   shell_utils::ShellCommand rCmd(rBinDir.completeChildPath("Rcmd.exe"));
#else
   shell_utils::ShellCommand rCmd(rBinDir.completeChildPath("R"));
   rCmd << "CMD";
#endif
   return rCmd;
}

core::Error recursiveCopyDirectory(const core::FilePath& fromDir,
                                   const core::FilePath& toDir)
{
   using namespace string_utils;
   r::exec::RFunction fileCopy("file.copy");
   fileCopy.addParam("from", utf8ToSystem(fromDir.getAbsolutePath()));
   fileCopy.addParam("to", utf8ToSystem(toDir.getAbsolutePath()));
   fileCopy.addParam("recursive", true);
   return fileCopy.call();
}

bool isSessionTempPath(FilePath filePath)
{
   // get the real path
   Error error = core::system::realPath(filePath, &filePath);
   if (error)
      LOG_ERROR(error);

   // get the session temp dir real path; needed since the file path above is
   // also a real path--e.g. on OS X, it refers to /private/tmp rather than
   // /tmp
   FilePath tempDir;
   error = core::system::realPath(module_context::tempDir(), &tempDir);
   if (error)
      LOG_ERROR(error);

   return filePath.isWithin(tempDir);
}

namespace {

bool isUsingRHelpServer()
{
   // don't use R help server in server mode
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      return false;
   }

#ifdef _WIN32
   // There is a known issue serving content from the session temporary folder
   // on R 4.0.0 for Windows:
   //
   //    https://github.com/rstudio/rstudio/issues/6737
   //
   // so we avoid using the help server in that case.
   if (r::util::hasExactVersion("4.0.0"))
   {
      return false;
   }
#endif

   // we're running on desktop with a suitable version of R;
   // okay to use the help server
   return true;
}

} // end anonymous namespace

std::string sessionTempDirUrl(const std::string& sessionTempPath)
{
   static bool useRHelpServer = isUsingRHelpServer();

   if (useRHelpServer)
   {
      boost::format fmt("http://localhost:%1%/session/%2%");
      return boost::str(fmt % rLocalHelpPort() % sessionTempPath);
   }
   else
   {
      boost::format fmt("session/%1%");
      return boost::str(fmt % sessionTempPath);
   }
}

bool isPathViewAllowed(const FilePath& filePath)
{
   // Check to see if restrictions are in place
   if (!options().restrictDirectoryView())
      return true;

   // No paths are restricted in desktop mode
   if (options().programMode() != kSessionProgramModeServer)
      return true;

   // Viewing content in the home directory is always allowed
   if (filePath.isWithin(userHomePath().getParent()))
      return true;
      
   // Viewing content in the session temporary files path is always allowed
   if (isSessionTempPath(filePath))
      return true;

   // Allow users to view the system's configuration
   if (filePath.isWithin(core::system::xdg::systemConfigDir()))
      return true;

   // Viewing content in R libraries is always allowed
   std::vector<FilePath> libPaths = getLibPaths();
   for (const auto& dir: libPaths)
   {
      if (filePath.isWithin(dir))
      {
         return true;
      }
   }

   // Check session option for explicitly whitelisted directories
   std::string whitelistDirs = session::options().directoryViewWhitelist();
   if (!whitelistDirs.empty())
   {
      std::vector<std::string> dirs = core::algorithm::split(whitelistDirs, ":");
      for (const auto& dir: dirs)
      {
         if (filePath.isWithin(FilePath(dir)))
         {
            return true;
         }
      }
   }

   // All other paths are implicitly disallowed
   return false;
}

namespace {

bool hasStem(const FilePath& filePath, const std::string& stem)
{
   return filePath.getStem() == stem;
}

} // anonymous namespace

Error uniqueSaveStem(const core::FilePath& directoryPath,
                     const std::string& base,
                     std::string* pStem)
{
   return uniqueSaveStem(directoryPath, base, "", pStem);
}

Error uniqueSaveStem(const FilePath& directoryPath,
                     const std::string& base,
                     const std::string& delimiter,
                     std::string* pStem)
{
   // determine unique file name
   std::vector<FilePath> children;
   Error error = directoryPath.getChildren(children);
   if (error)
      return error;

   // search for unique stem
   int i = 0;
   *pStem = base;
   while(true)
   {
      // seek stem
      std::vector<FilePath>::const_iterator it = std::find_if(
                                                children.begin(),
                                                children.end(),
                                                boost::bind(hasStem, _1, *pStem));
      // break if not found
      if (it == children.end())
         break;

      // update stem and search again
      boost::format fmt(base + delimiter + "%1%");
      *pStem = boost::str(fmt % boost::io::group(std::setfill('0'),
                                                 std::setw(2),
                                                 ++i));
   }

   return Success();
}

json::Object plotExportFormat(const std::string& name,
                              const std::string& extension)
{
   json::Object formatJson;
   formatJson["name"] = name;
   formatJson["extension"] = extension;
   return formatJson;
}

Error createSelfContainedHtml(const FilePath& sourceFilePath,
                              const FilePath& targetFilePath)
{
   r::exec::RFunction func(".rs.pandocSelfContainedHtml");
   func.addParam(string_utils::utf8ToSystem(sourceFilePath.getAbsolutePath()));
   func.addParam(string_utils::utf8ToSystem(
      session::options().rResourcesPath().completePath("pandoc_template.html").getAbsolutePath()));
   func.addParam(string_utils::utf8ToSystem(targetFilePath.getAbsolutePath()));
   return func.call();
}

bool isUserFile(const FilePath& filePath)
{
   if (projects::projectContext().hasProject())
   {
      // if we are in a package project then screen our src- files
      if (projects::projectContext().config().buildType ==
                                              r_util::kBuildTypePackage)
      {
          FilePath pkgPath = projects::projectContext().buildTargetPath();
          std::string pkgRelative = filePath.getRelativePath(pkgPath);
          if (boost::algorithm::starts_with(pkgRelative, "src-"))
             return false;
      }

      // if we are in a website project then screen the output dir
      if (!module_context::websiteOutputDir().empty())
      {
         FilePath sitePath = projects::projectContext().buildTargetPath();
         std::string siteRelative = filePath.getRelativePath(sitePath);
         if (boost::algorithm::starts_with(
                siteRelative, module_context::websiteOutputDir() + "/"))
            return false;
      }

      // screen the packrat directory
      FilePath projPath = projects::projectContext().directory();
      std::string pkgRelative = filePath.getRelativePath(projPath);
      if (boost::algorithm::starts_with(pkgRelative, "packrat/"))
         return false;
   }

   return true;
}

namespace {

// NOTE: sync changes with SessionCompilePdf.cpp logEntryJson
json::Value sourceMarkerJson(const SourceMarker& sourceMarker)
{
   json::Object obj;
   obj["type"] = static_cast<int>(sourceMarker.type);
   obj["path"] = module_context::createAliasedPath(sourceMarker.path);
   obj["line"] = sourceMarker.line;
   obj["column"] = sourceMarker.column;
   obj["message"] = sourceMarker.message.text();
   obj["log_path"] = "";
   obj["log_line"] = -1;
   obj["show_error_list"] = sourceMarker.showErrorList;
   return std::move(obj);
}

} // anonymous namespace

json::Array sourceMarkersAsJson(const std::vector<SourceMarker>& markers)
{
   json::Array markersJson;
   std::transform(markers.begin(),
                  markers.end(),
                  std::back_inserter(markersJson),
                  sourceMarkerJson);
   return markersJson;
}

SourceMarker::Type sourceMarkerTypeFromString(const std::string& type)
{
   if (type == "error")
      return SourceMarker::Error;
   else if (type == "warning")
      return SourceMarker::Warning;
   else if (type == "box")
      return SourceMarker::Box;
   else if (type == "info")
      return SourceMarker::Info;
   else if (type == "style")
      return SourceMarker::Style;
   else if (type == "usage")
      return SourceMarker::Usage;
   else
      return SourceMarker::Error;
}

core::json::Array sourceMarkersAsJson(const std::vector<SourceMarker>& markers);

bool isLoadBalanced()
{
   return !core::system::getenv(kRStudioSessionRoute).empty();
}

#ifdef _WIN32
bool usingMingwGcc49()
{
   // return true if the setting is true
   bool gcc49 = prefs::userState().usingMingwGcc49();
   if (gcc49)
      return true;

   // otherwise check R version
   r::exec::RFunction func(".rs.builtWithRtoolsGcc493");
   Error error = func.call(&gcc49);
   if (error)
      LOG_ERROR(error);
   return gcc49;

}
#else
bool usingMingwGcc49()
{
   return false;
}
#endif

namespace {

#ifdef __APPLE__
void warnXcodeLicense()
{
   const char* msg =
R"EOF(Warning: macOS is reporting that you have not yet agreed to the Xcode license.
This can occur if Xcode has been updated or reinstalled (e.g. as part of a macOS update).
Some features (e.g. Git / SVN) may be disabled.

Please run:

    sudo xcodebuild -license accept

in a terminal to accept the Xcode license, and then restart RStudio.
)EOF";
   
   std::cerr << msg << std::endl;
}
#endif

} // end anonymous namespace

bool isMacOS()
{
#ifdef __APPLE__
   return true;
#else
   return false;
#endif
}

bool hasMacOSDeveloperTools()
{
   if (!isMacOS())
      return false;
   
   core::system::ProcessResult result;
   Error error = core::system::runCommand(
            "/usr/bin/xcrun --find --show-sdk-path",
            core::system::ProcessOptions(),
            &result);

   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   if (result.exitStatus == 69)
      checkXcodeLicense();

   return result.exitStatus == 0;
}

bool hasMacOSCommandLineTools()
{
   if (!isMacOS())
      return false;
   
   return FilePath("/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk").exists();
}

void checkXcodeLicense()
{
#ifdef __APPLE__
   
   // avoid repeatedly warning the user
   static bool s_licenseChecked;
   if (s_licenseChecked)
      return;
   
   s_licenseChecked = true;
   
   core::system::ProcessResult result;
   Error error = core::system::runCommand(
            "/usr/bin/xcrun --find --show-sdk-path",
            core::system::ProcessOptions(),
            &result);
   
   // if an error occurs, log it but avoid otherwise annoying the user
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // exit code 69 implies license error
   if (result.exitStatus == 69)
      warnXcodeLicense();
   
#endif
}

std::string getActiveLanguage()
{
   if (modules::reticulate::isReplActive())
   {
      return "Python";
   }
   else
   {
      return "R";
   }
}

Error adaptToLanguage(const std::string& language)
{
   // check to see what language is active in main console
   using namespace r::exec;
   
   // check to see what language is currently active (but default to r)
   std::string activeLanguage = getActiveLanguage();

   // now, detect if we are transitioning languages
   if (language != activeLanguage)
   {
      // since it may take some time for the console input to be processed,
      // we screen out consecutive transition attempts (otherwise we can
      // get multiple interleaved attempts to launch the REPL with console
      // input)
      static RSTUDIO_BOOST_CONNECTION conn;
      if (conn.connected())
         return Success();
      
      // establish the connection, and then simply disconnect once we
      // receive the signal
      conn = module_context::events().onConsolePrompt.connect([&](const std::string&) {
         conn.disconnect();
      });
      
      if (activeLanguage == "R")
      {
         if (language == "Python")
         {
            // r -> python: activate the reticulate REPL
            Error error =
                  module_context::enqueueConsoleInput("reticulate::repl_python()");
            if (error)
               LOG_ERROR(error);
         }
      }
      else if (activeLanguage == "Python")
      {
         if (language == "R")
         {
            // python -> r: deactivate the reticulate REPL
            Error error =
                  module_context::enqueueConsoleInput("quit");
         }
      }
   }
   
   return Success();
}

Error initialize()
{
   // register .Call methods
   RS_REGISTER_CALL_METHOD(rs_activatePane);
   RS_REGISTER_CALL_METHOD(rs_base64decode);
   RS_REGISTER_CALL_METHOD(rs_base64encode);
   RS_REGISTER_CALL_METHOD(rs_base64encodeFile);
   RS_REGISTER_CALL_METHOD(rs_enqueClientEvent);
   RS_REGISTER_CALL_METHOD(rs_ensureFileHidden);
   RS_REGISTER_CALL_METHOD(rs_generateShortUuid);
   RS_REGISTER_CALL_METHOD(rs_getPersistentValue);
   RS_REGISTER_CALL_METHOD(rs_isRScriptInPackageBuildTarget);
   RS_REGISTER_CALL_METHOD(rs_logErrorMessage);
   RS_REGISTER_CALL_METHOD(rs_logWarningMessage);
   RS_REGISTER_CALL_METHOD(rs_markdownToHTML);
   RS_REGISTER_CALL_METHOD(rs_packageLoaded);
   RS_REGISTER_CALL_METHOD(rs_packageNameForSourceFile);
   RS_REGISTER_CALL_METHOD(rs_packageUnloaded);
   RS_REGISTER_CALL_METHOD(rs_resolveAliasedPath);
   RS_REGISTER_CALL_METHOD(rs_restartR);
   RS_REGISTER_CALL_METHOD(rs_rstudioCitation);
   RS_REGISTER_CALL_METHOD(rs_rstudioCRANReposUrl);
   RS_REGISTER_CALL_METHOD(rs_rstudioEdition);
   RS_REGISTER_CALL_METHOD(rs_rstudioProgramMode);
   RS_REGISTER_CALL_METHOD(rs_rstudioVersion);
   RS_REGISTER_CALL_METHOD(rs_rstudioReleaseName);
   RS_REGISTER_CALL_METHOD(rs_sessionModulePath);
   RS_REGISTER_CALL_METHOD(rs_setPersistentValue);
   RS_REGISTER_CALL_METHOD(rs_setUsingMingwGcc49);
   RS_REGISTER_CALL_METHOD(rs_showErrorMessage);
   RS_REGISTER_CALL_METHOD(rs_sourceDiagnostics);
   RS_REGISTER_CALL_METHOD(rs_threadSleep);
   RS_REGISTER_CALL_METHOD(rs_userPrompt);
   RS_REGISTER_CALL_METHOD(rs_setRpcDelay);

   // initialize monitored scratch dir
   initializeMonitoredUserScratchDir();

   // source the ModuleTools.R file
   FilePath modulesPath = session::options().modulesRSourcePath();
   return r::sourceManager().sourceTools(modulesPath.completePath("ModuleTools.R"));
}


} // namespace module_context         
} // namespace session
} // namespace rstudio
