/*
 * SessionModuleContext.cpp
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

#include "SessionModuleContextInternal.hpp"

#include <vector>

#include <boost/assert.hpp>
#include <boost/utility.hpp>
#include <boost/signal.hpp>
#include <boost/format.hpp>
#include <boost/numeric/conversion/cast.hpp>

#include <core/BoostThread.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
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
#include "SessionClientEventQueue.hpp"

#include <session/projects/SessionProjects.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionContentUrls.hpp>

#include "modules/SessionBreakpoints.hpp"
#include "modules/SessionVCS.hpp"
#include "modules/SessionFiles.hpp"

#include "session-config.h"

using namespace rstudio::core ;

namespace rstudio {
namespace session {   
namespace module_context {
      
namespace {

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
      Error extractError ;
      json::Value data ;
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
      int type = -1 ;
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
   catch(r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   return R_NilValue ;
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

// get citation
SEXP rs_rstudioCitation()
{
   FilePath resPath = session::options().rResourcesPath();
   FilePath citationPath = resPath.childPath("CITATION");

   SEXP citationSEXP;
   r::sexp::Protect rProtect;
   Error error = r::exec::RFunction("utils:::readCitationFile",
                                    citationPath.absolutePath())
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
   userSettings().setUsingMingwGcc49(usingMingwGcc49);
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

} // anonymous namespace


// register a scratch path which is monitored

namespace {

typedef std::map<FilePath,OnFileChange> MonitoredScratchPaths;
MonitoredScratchPaths s_monitoredScratchPaths;
bool s_monitorByScanning = false;

FilePath monitoredParentPath()
{
   FilePath monitoredPath = userScratchPath().complete(kMonitoredPath);
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
   BOOST_FOREACH(const core::system::FileChangeEvent& fileChange, changes)
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
   FilePath dirPath = monitoredParentPath().complete(dirName);
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
   
   boost::signal<void(const r::session::RSuspendOptions&,Settings*),
                 boost::last_value<void>,
                 int,
                 std::less<int> > suspendSignal_;
                  
   boost::signal<void(const Settings&),
                 boost::last_value<void>,
                 int,
                 std::greater<int> > resumeSignal_;
};

// handlers instance
SuspendHandlers s_suspendHandlers ;   
   
} // anonymous namespace
   
void addSuspendHandler(const SuspendHandler& handler)
{
   s_suspendHandlers.add(handler);
}
   
void onSuspended(const r::session::RSuspendOptions& options,
                 Settings* pPersistentState)
{
   pPersistentState->beginUpdate();
   s_suspendHandlers.suspend(options, pPersistentState);
   pPersistentState->endUpdate();
   
}

void onResumed(const Settings& persistentState)
{
   s_suspendHandlers.resume(persistentState);
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

   FilePath parentDir = srcPath.parent();
   if (parentDir.filename() != "src")
      return false;

   FilePath packagePath = context.buildTargetPath();
   if (parentDir.parent() != packagePath)
      return false;

   std::string filename = srcPath.filename();
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
   if (userSettings().useNewlineInMakefiles() && isPackagePosixMakefile(srcFile))
      return string_utils::LineEndingPosix;

   // get the global default behavior
   string_utils::LineEnding lineEndings = userSettings().lineEndings();

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
      return error ;

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
   return error ;
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


bool isPdfLatexInstalled()
{
   return !module_context::findProgram("pdflatex").empty();
}

namespace {

bool hasTextMimeType(const FilePath& filePath)
{
   std::string mimeType = filePath.mimeContentType("");
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

   std::string mimeType = filePath.mimeContentType("");
   if (mimeType.empty())
      return false;

   return boost::algorithm::starts_with(mimeType, "application/") ||
          boost::algorithm::starts_with(mimeType, "image/") ||
          boost::algorithm::starts_with(mimeType, "audio/") ||
          boost::algorithm::starts_with(mimeType, "video/");
}

bool isJsonFile(const FilePath& filePath)
{
   std::string mimeType = filePath.mimeContentType();
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
   
   if (targetPath.size() == 0)
      return true;

#ifndef _WIN32
   core::shell_utils::ShellCommand cmd("file");
   cmd << "--dereference";
   cmd << "--mime";
   cmd << "--brief";
   cmd << targetPath;
   core::system::ProcessResult result;
   Error error = core::system::runCommand(cmd,
                                          core::system::ProcessOptions(),
                                          &result);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   // strip encoding
   std::string fileType = boost::algorithm::trim_copy(result.stdOut);
   fileType = fileType.substr(0, fileType.find(';'));

   // check value
   return boost::algorithm::starts_with(fileType, "text/") ||
          boost::algorithm::ends_with(fileType, "+xml") ||
          boost::algorithm::ends_with(fileType, "/xml") ||
          boost::algorithm::ends_with(fileType, "x-empty") ||
          boost::algorithm::equals(fileType, "application/postscript");
#else

   // read contents of file
   std::string contents;
   Error error = core::readStringFromFile(targetPath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return error;
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
*pRScriptPath = rHomeBinPath.complete("Rterm.exe");
#else
*pRScriptPath = rHomeBinPath.complete("R");
#endif
   return Success();
}

shell_utils::ShellCommand rCmd(const core::FilePath& rBinDir)
{
#ifdef _WIN32
      return shell_utils::ShellCommand(rBinDir.childPath("Rcmd.exe"));
#else
      shell_utils::ShellCommand rCmd(rBinDir.childPath("R"));
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
   BOOST_FOREACH(const std::string& path, libPathsString)
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
   shell_utils::ShellCommand installCommand(rBinDir.childPath("R.exe"));
#else
   shell_utils::ShellCommand installCommand(rBinDir.childPath("R"));
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
   FilePath sourceDir = sourceFilePath.parent();
   if (sourceDir.filename() == "R" &&
       r_util::isPackageDirectory(sourceDir.parent()))
   {
      r_util::RPackageInfo pkgInfo;
      Error error = pkgInfo.read(sourceDir.parent());
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
      dir = filePath.parent();

   // see if one the file's parent directories has a DESCRIPTION
   while (!dir.empty())
   {
      FilePath descPath = dir.childPath("DESCRIPTION");
      if (descPath.exists())
      {
         // get path relative to package dir
         std::string relative = filePath.relativePath(dir);
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

      dir = dir.parent();
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


json::Object createFileSystemItem(const FileInfo& fileInfo)
{
   json::Object entry ;

   std::string aliasedPath = module_context::createAliasedPath(fileInfo);
   std::string rawPath =
         module_context::resolveAliasedPath(aliasedPath).absolutePath();

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
      else
      {
         // if no active session, create one and use the launcher token as a
         // synthetic session ID
         pSession = activeSessions().emptySession(
               options().launcherToken());
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
   FilePath srcPath = modulesPath.complete(rSourceFile);
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
   std::string rBin = string_utils::utf8ToSystem(rProgramPath.absolutePath());

   // vanilla execution of a single expression
   std::vector<std::string> args;
   args.push_back("--slave");
   args.push_back("--vanilla");
   args.push_back("-e");

   // build source command
   boost::format fmt("source('%1%')");
   FilePath modulesPath = session::options().modulesRSourcePath();
   FilePath srcFilePath = modulesPath.complete(rSourceFile);
   std::string srcPath = core::string_utils::utf8ToSystem(
                                                srcFilePath.absolutePath());
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
      std::string pkgRelative = filePath.relativePath(pkgPath);
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
   FilePath filePath = module_context::resolveAliasedPath(r::sexp::asString(filePathSEXP));
   return r::sexp::create(isRScriptInPackageBuildTarget(filePath), &protect);
}

bool fileListingFilter(const core::FileInfo& fileInfo)
{
   // check extension for special file types which are always visible
   core::FilePath filePath(fileInfo.absolutePath());
   std::string ext = filePath.extensionLowerCase();
   std::string name = filePath.filename();
   if (ext == ".r" ||
       ext == ".rprofile" ||
       ext == ".rbuildignore" ||
       ext == ".rdata"    ||
       ext == ".rhistory" ||
       ext == ".ruserdata" ||
       ext == ".renviron" ||
       ext == ".httr-oauth" ||
       ext == ".gitignore")
   {
      return true;
   }
   else if (name == ".travis.yml")
   {
      return true;
   }
   else if (userSettings().hideObjectFiles() &&
            (ext == ".o" || ext == ".so" || ext == ".dll") &&
            filePath.parent().filename() == "src")
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
   json::Object fileChange ;
   fileChange["type"] = event.type();
   json::Object fileSystemItem = createFileSystemItem(event.fileInfo());

   pCtx->decorateFile(FilePath(event.fileInfo().absolutePath()),
                      &fileSystemItem);

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
   boost::shared_ptr<FileDecorationContext> pCtx =
                                       fileDecorationContext(filePath);

   enqueFileChangedEvent(event, pCtx);
}

void enqueFileChangedEvents(const core::FilePath& vcsStatusRoot,
                            const std::vector<core::system::FileChangeEvent>& events)
{
   using namespace modules::source_control;

   if (events.empty())
      return;

   // try to find the common parent of the events
   FilePath commonParentPath = FilePath(events.front().fileInfo().absolutePath()).parent();
   BOOST_FOREACH(const core::system::FileChangeEvent& event, events)
   {
      // if not within the common parent then revert to the vcs status root
      if (!FilePath(event.fileInfo().absolutePath()).isWithin(commonParentPath))
      {
         commonParentPath = vcsStatusRoot;
         break;
      }
   }

   using namespace session::modules::source_control;
   boost::shared_ptr<FileDecorationContext> pCtx =
                                  fileDecorationContext(commonParentPath);

   // fire client events as necessary
   BOOST_FOREACH(const core::system::FileChangeEvent& event, events)
   {
      enqueFileChangedEvent(event, pCtx);
   }
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
      if (filePath.extensionLowerCase() == ".pdf")
      {
         std::string path = filePath.absolutePath();
         Error error = r::exec::RFunction(".rs.shellViewPdf", path).call();
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         ClientEvent event = browseUrlEvent("file:///" + filePath.absolutePath());
         module_context::enqueClientEvent(event);
      }
   }
   else if (session::options().programMode() == kSessionProgramModeServer)
   {
      if (session::options().allowFileDownloads())
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
    std::string url ;
    if (isVisibleUserFile(filePath))
    {
       std::string relPath = filePath.relativePath(
                                    module_context::userHomePath());
       url = "files/" + relPath;
    }
    else
    {
       url = "file_show?path=" + http::util::urlEncode(
                                filePath.absolutePath(), true);
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
   FilePath filePath = resPath.complete(fileName);
   std::string fileContents;
   Error error = readStringFromFile(filePath, &fileContents);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }

   return fileContents;
}

bool portmapPathForLocalhostUrl(const std::string& url, std::string* pPath)
{
   // extract the port
   boost::regex re("http[s]?://(?:localhost|127\\.0\\.0\\.1):([0-9]+)(/.*)?");
   boost::smatch match;
   if (regex_utils::search(url, match, re))
   {
      // calculate the path
      std::string path = match[2];
      if (path.empty())
         path = "/";
      path = "p/" + match[1] + path;
      *pPath = path;

      return true;
   }
   else
   {
      return false;
   }
}

// given a url, return a portmap path if applicable (i.e. we're in server
// mode and the path needs port mapping), and the unmodified url otherwise
std::string mapUrlPorts(const std::string& url)
{
   // if we are in server mode then we need to do port mapping
   if (session::options().programMode() != kSessionProgramModeServer)
      return url;

   // see if we can form a portmap path for this url
   std::string path;
   if (portmapPathForLocalhostUrl(url, &path))
      return path;

   return url;
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
      relative = targetPath.relativePath(sourcePath);
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
   if (projects::projectContext().hasProject())
      return projects::projectContext().directory();
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
   r::exec::RFunction sourceFx("source");
   sourceFx.addParam(string_utils::utf8ToSystem(
       options().coreRSourcePath().childPath("Diagnostics.R").absolutePath()));
   sourceFx.addParam("chdir", true);
   Error error = sourceFx.call();
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   else
   {
      // note this path is also in Diagnostics.R so changes to the path
      // need to be synchronized there
      return module_context::resolveAliasedPath(
                        "~/rstudio-diagnostics/diagnostics-report.txt");
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
      url = userSettings().cranMirror().url;
   return url;
}

std::string rstudioCRANReposURL()
{
   std::string protocol = userSettings().securePackageDownload() ?
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
   shell_utils::ShellCommand rCmd(rBinDir.childPath("Rcmd.exe"));
#else
   shell_utils::ShellCommand rCmd(rBinDir.childPath("R"));
   rCmd << "CMD";
#endif
   return rCmd;
}

core::Error recursiveCopyDirectory(const core::FilePath& fromDir,
                                   const core::FilePath& toDir)
{
   using namespace string_utils;
   r::exec::RFunction fileCopy("file.copy");
   fileCopy.addParam("from", utf8ToSystem(fromDir.absolutePath()));
   fileCopy.addParam("to", utf8ToSystem(toDir.absolutePath()));
   fileCopy.addParam("recursive", true);
   return fileCopy.call();
}

std::string sessionTempDirUrl(const std::string& sessionTempPath)
{
   if (session::options().programMode() == kSessionProgramModeDesktop)
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

namespace {

bool hasStem(const FilePath& filePath, const std::string& stem)
{
   return filePath.stem() == stem;
}

} // anonymous namespace

Error uniqueSaveStem(const FilePath& directoryPath,
                     const std::string& base,
                     std::string* pStem)
{
   // determine unique file name
   std::vector<FilePath> children;
   Error error = directoryPath.children(&children);
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
      boost::format fmt(base + "%1%");
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
   r::exec::RFunction func("rmarkdown:::pandoc_self_contained_html");
   func.addParam(string_utils::utf8ToSystem(sourceFilePath.absolutePath()));
   func.addParam(string_utils::utf8ToSystem(targetFilePath.absolutePath()));
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
          std::string pkgRelative = filePath.relativePath(pkgPath);
          if (boost::algorithm::starts_with(pkgRelative, "src-"))
             return false;
      }

      // if we are in a website project then screen the output dir
      if (!module_context::websiteOutputDir().empty())
      {
         FilePath sitePath = projects::projectContext().buildTargetPath();
         std::string siteRelative = filePath.relativePath(sitePath);
         if (boost::algorithm::starts_with(
                siteRelative, module_context::websiteOutputDir() + "/"))
            return false;
      }

      // screen the packrat directory
      FilePath projPath = projects::projectContext().directory();
      std::string pkgRelative = filePath.relativePath(projPath);
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
   return obj;
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
   bool gcc49 = userSettings().usingMingwGcc49();
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

Error initialize()
{
   // register rs_enqueClientEvent with R 
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_enqueClientEvent" ;
   methodDef.fun = (DL_FUNC) rs_enqueClientEvent ;
   methodDef.numArgs = 2;
   r::routines::addCallMethod(methodDef);
      
   // register rs_showErrorMessage with R 
   R_CallMethodDef methodDef3 ;
   methodDef3.name = "rs_showErrorMessage" ;
   methodDef3.fun = (DL_FUNC) rs_showErrorMessage ;
   methodDef3.numArgs = 2;
   r::routines::addCallMethod(methodDef3);
   
   // register rs_logErrorMessage with R 
   R_CallMethodDef methodDef4 ;
   methodDef4.name = "rs_logErrorMessage" ;
   methodDef4.fun = (DL_FUNC) rs_logErrorMessage ;
   methodDef4.numArgs = 1;
   r::routines::addCallMethod(methodDef4);
   
   // register rs_logWarningMessage with R 
   R_CallMethodDef methodDef5 ;
   methodDef5.name = "rs_logWarningMessage" ;
   methodDef5.fun = (DL_FUNC) rs_logWarningMessage ;
   methodDef5.numArgs = 1;
   r::routines::addCallMethod(methodDef5);

   // register rs_threadSleep with R (debugging function used to test rpc/abort)
   R_CallMethodDef methodDef6 ;
   methodDef6.name = "rs_threadSleep" ;
   methodDef6.fun = (DL_FUNC) rs_threadSleep ;
   methodDef6.numArgs = 1;
   r::routines::addCallMethod(methodDef6);

   // register rs_rstudioProgramMode with R
   R_CallMethodDef methodDef7 ;
   methodDef7.name = "rs_rstudioProgramMode" ;
   methodDef7.fun = (DL_FUNC) rs_rstudioProgramMode ;
   methodDef7.numArgs = 0;
   r::routines::addCallMethod(methodDef7);

   // register rs_ensureFileHidden with R
   R_CallMethodDef methodDef8;
   methodDef8.name = "rs_ensureFileHidden" ;
   methodDef8.fun = (DL_FUNC) rs_ensureFileHidden ;
   methodDef8.numArgs = 1;
   r::routines::addCallMethod(methodDef8);

   // register rs_sourceDiagnostics
   R_CallMethodDef methodDef9;
   methodDef9.name = "rs_sourceDiagnostics" ;
   methodDef9.fun = (DL_FUNC) rs_sourceDiagnostics;
   methodDef9.numArgs = 0;
   r::routines::addCallMethod(methodDef9);

   // register rs_activatePane
   R_CallMethodDef methodDef10;
   methodDef10.name = "rs_activatePane" ;
   methodDef10.fun = (DL_FUNC) rs_activatePane;
   methodDef10.numArgs = 1;
   r::routines::addCallMethod(methodDef10);

   // register rs_packageLoaded
   R_CallMethodDef methodDef11;
   methodDef11.name = "rs_packageLoaded" ;
   methodDef11.fun = (DL_FUNC) rs_packageLoaded;
   methodDef11.numArgs = 1;
   r::routines::addCallMethod(methodDef11);

   // register rs_packageUnloaded
   R_CallMethodDef methodDef12;
   methodDef12.name = "rs_packageUnloaded" ;
   methodDef12.fun = (DL_FUNC) rs_packageUnloaded;
   methodDef12.numArgs = 1;
   r::routines::addCallMethod(methodDef12);

   // register rs_userPrompt
   R_CallMethodDef methodDef13;
   methodDef13.name = "rs_userPrompt" ;
   methodDef13.fun = (DL_FUNC) rs_userPrompt;
   methodDef13.numArgs = 7;
   r::routines::addCallMethod(methodDef13);

   // register rs_restartR
   R_CallMethodDef methodDef14;
   methodDef14.name = "rs_restartR" ;
   methodDef14.fun = (DL_FUNC) rs_restartR;
   methodDef14.numArgs = 1;
   r::routines::addCallMethod(methodDef14);

   // register rs_restartR
   R_CallMethodDef methodDef15;
   methodDef15.name = "rs_rstudioCRANReposUrl" ;
   methodDef15.fun = (DL_FUNC) rs_rstudioCRANReposUrl;
   methodDef15.numArgs = 0;
   r::routines::addCallMethod(methodDef15);

   // register rs_rstudioEdition with R
   R_CallMethodDef methodDef16 ;
   methodDef16.name = "rs_rstudioEdition" ;
   methodDef16.fun = (DL_FUNC) rs_rstudioEdition ;
   methodDef16.numArgs = 0;
   r::routines::addCallMethod(methodDef16);

   R_CallMethodDef methodDef17;
   methodDef17.name = "rs_markdownToHTML";
   methodDef17.fun = (DL_FUNC) rs_markdownToHTML;
   methodDef17.numArgs = 1;
   r::routines::addCallMethod(methodDef17);
   
   // register persistent value functions
   RS_REGISTER_CALL_METHOD(rs_setPersistentValue, 2);
   RS_REGISTER_CALL_METHOD(rs_getPersistentValue, 1);

   // register rs_isRScriptInPackageBuildTarget
   r::routines::registerCallMethod(
            "rs_isRScriptInPackageBuildTarget",
            (DL_FUNC) rs_isRScriptInPackageBuildTarget,
            1);
   
   // register rs_packageNameForSourceFile
   r::routines::registerCallMethod(
            "rs_packageNameForSourceFile",
            (DL_FUNC) rs_packageNameForSourceFile,
            1);

   // register rs_rstudioVersion
   r::routines::registerCallMethod(
            "rs_rstudioVersion",
            (DL_FUNC) rs_rstudioVersion,
            0);

   // regsiter rs_rstudioCitation
   r::routines::registerCallMethod(
            "rs_rstudioCitation",
            (DL_FUNC) rs_rstudioCitation,
            0);

   // register rs_setUsingMingwGcc49
   r::routines::registerCallMethod(
            "rs_setUsingMingwGcc49",
            (DL_FUNC)rs_setUsingMingwGcc49,
            1);

   r::routines::registerCallMethod(
            "rs_generateShortUuid",
            (DL_FUNC)rs_generateShortUuid, 
            0);

   RS_REGISTER_CALL_METHOD(rs_base64encode, 2);
   RS_REGISTER_CALL_METHOD(rs_base64encodeFile, 1);
   RS_REGISTER_CALL_METHOD(rs_base64decode, 2);

   // initialize monitored scratch dir
   initializeMonitoredUserScratchDir();

   // source the ModuleTools.R file
   FilePath modulesPath = session::options().modulesRSourcePath();
   return r::sourceManager().sourceTools(modulesPath.complete("ModuleTools.R"));
}


} // namespace module_context         
} // namespace session
} // namespace rstudio
