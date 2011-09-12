/*
 * SessionModuleContext.cpp
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

#include "SessionModuleContextInternal.hpp"

#include <vector>

#include <boost/utility.hpp>
#include <boost/signal.hpp>
#include <boost/numeric/conversion/cast.hpp>

#include <core/BoostThread.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileInfo.hpp>
#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/DateTime.hpp>
#include <core/FileSerializer.hpp>
#include <core/IncrementalCommand.hpp>

#include <core/http/Util.hpp>

#include <core/system/Process.hpp>
#include <core/system/FileMonitor.hpp>
#include <core/system/FileChangeEvent.hpp>

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
#include "SessionClientEventQueue.hpp"

#include <session/projects/SessionProjects.hpp>

#include "modules/SessionContentUrls.hpp"
#include "modules/SessionSourceControl.hpp"
#include "modules/SessionFiles.hpp"

#include "config.h"

using namespace core ;

namespace session {   
namespace module_context {
      
namespace {
   
bool s_googleDocsIntegrationEnabled = false;

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
      if (name == "workspace_refresh")
         type = session::client_events::kWorkspaceRefresh;
      else if (name == "package_status_changed")
         type = session::client_events::kPackageStatusChanged;
      else if (name == "installed_packages_changed")
         type = session::client_events::kInstalledPackagesChanged;
      
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

// get scratch path from R
SEXP rs_scratchPath()
{
   FilePath scratchPath = module_context::userScratchPath();
   r::sexp::Protect rProtect;
   return r::sexp::create(scratchPath.absolutePath(), &rProtect);
}

// get scoped scratch path from R
SEXP rs_scopedScratchPath()
{
   FilePath scopedScratchPath = module_context::scopedScratchPath();
   r::sexp::Protect rProtect;
   return r::sexp::create(scopedScratchPath.absolutePath(), &rProtect);
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

// get rstudio version from R
SEXP rs_rstudioVersion()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(std::string(RSTUDIO_VERSION), &rProtect);
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

// override of Sys.sleep to notify listeners of a sleep
CCODE s_originalSysSleepFunction;
SEXP sysSleepHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   r::function_hook::checkArity(op, args, call);

   try
   {
      events().onSysSleep();
   }
   CATCH_UNEXPECTED_EXCEPTION

   return s_originalSysSleepFunction(call, op, args, rho);
}
   
} // anonymous namespace

Error initialize()
{
   // register rs_enqueClientEvent with R 
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_enqueClientEvent" ;
   methodDef.fun = (DL_FUNC) rs_enqueClientEvent ;
   methodDef.numArgs = 2;
   r::routines::addCallMethod(methodDef);
   
   // register rs_scratchPath with R
   R_CallMethodDef methodDef2 ; 
   methodDef2.name = "rs_scratchPath" ;
   methodDef2.fun = (DL_FUNC) rs_scratchPath ;
   methodDef2.numArgs = 0;
   r::routines::addCallMethod(methodDef2);

   // register rs_scopedScratchPath with R
   R_CallMethodDef methodDefScopedScratchPath ;
   methodDefScopedScratchPath.name = "rs_scopedScratchPath" ;
   methodDefScopedScratchPath.fun = (DL_FUNC) rs_scopedScratchPath ;
   methodDefScopedScratchPath.numArgs = 0;
   r::routines::addCallMethod(methodDefScopedScratchPath);
   
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

   // register rs_rstudioVersion with R
   R_CallMethodDef methodDef7 ;
   methodDef7.name = "rs_rstudioVersion" ;
   methodDef7.fun = (DL_FUNC) rs_rstudioVersion ;
   methodDef7.numArgs = 0;
   r::routines::addCallMethod(methodDef7);

   // register rs_ensureFileHidden with R
   R_CallMethodDef methodDef8;
   methodDef8.name = "rs_ensureFileHidden" ;
   methodDef8.fun = (DL_FUNC) rs_ensureFileHidden ;
   methodDef8.numArgs = 1;
   r::routines::addCallMethod(methodDef8);
   
   // register Sys.sleep() hook to notify modules of sleep (currently
   // used by plots to check for changes on sleep so we can support the
   // most common means of animating plots in R)
   Error error = r::function_hook::registerReplaceHook(
                                              "Sys.sleep",
                                              sysSleepHook,
                                              &s_originalSysSleepFunction);
   if (error)
      return error;

   // source the ModuleTools.R file
   FilePath modulesPath = session::options().modulesRSourcePath();
   return r::sourceManager().sourceTools(modulesPath.complete("ModuleTools.R"));
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
   
   void suspend(Settings* pSettings)
   {
      suspendSignal_(pSettings);
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
   
   boost::signal<void(Settings*), 
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
   
void onSuspended(Settings* pPersistentState)
{
   pPersistentState->beginUpdate();
   s_suspendHandlers.suspend(pPersistentState);
   pPersistentState->endUpdate();
   
}

void onResumed(const Settings& persistentState)
{
   s_suspendHandlers.resume(persistentState);
}

// idle work

namespace {

typedef std::vector<boost::shared_ptr<IncrementalCommand> >
                                                      IncrementalCommands;
IncrementalCommands s_incrementalCommands;
IncrementalCommands s_idleIncrementalCommands;

void addIncrementalCommand(boost::shared_ptr<IncrementalCommand> pCommand,
                           bool idleOnly)
{
   if (idleOnly)
      s_idleIncrementalCommands.push_back(pCommand);
   else
      s_incrementalCommands.push_back(pCommand);
}

void executeIncrementalCommands(IncrementalCommands* pCommands)
{
   // execute all commands
   std::for_each(pCommands->begin(),
                 pCommands->end(),
                 boost::bind(&IncrementalCommand::execute, _1));

   // remove any commands which are finished
   pCommands->erase(
                 std::remove_if(
                    pCommands->begin(),
                    pCommands->end(),
                    boost::bind(&IncrementalCommand::finished, _1)),
                 pCommands->end());
}


} // anonymous namespace

void scheduleIncrementalWork(
         const boost::posix_time::time_duration& incrementalDuration,
         const boost::function<bool()>& execute,
         bool idleOnly)
{
   addIncrementalCommand(boost::shared_ptr<IncrementalCommand>(
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
   addIncrementalCommand(boost::shared_ptr<IncrementalCommand>(
                           new IncrementalCommand(initialDuration,
                                                  incrementalDuration,
                                                  execute)),
                         idleOnly);
}


void scheduleIncrementalCommand(
                  boost::shared_ptr<core::IncrementalCommand> pCommand,
                  bool idleOnly)
{
   if (idleOnly)
      s_idleIncrementalCommands.push_back(pCommand);
   else
      s_incrementalCommands.push_back(pCommand);
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
   executeIncrementalCommands(&s_incrementalCommands);
   if (isIdle)
      executeIncrementalCommands(&s_idleIncrementalCommands);
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
      entry["length"] = boost::numeric_cast<uint64_t>(fileInfo.size());
   }
   catch (const boost::bad_numeric_cast& e)
   {
      LOG_ERROR_MESSAGE(std::string("Error converting file size: ") +
                        e.what());
      entry["length"] = 0;
   }

   entry["lastModified"] = date_time::millisecondsSinceEpoch(
                                                   fileInfo.lastWriteTime());
   return entry;
}

json::Object createFileSystemItem(const FilePath& filePath)
{
   return createFileSystemItem(FileInfo(filePath));
}

Error sourceModuleRFile(const std::string& rSourceFile)
{
   FilePath modulesPath = session::options().modulesRSourcePath();
   FilePath srcPath = modulesPath.complete(rSourceFile);
   return r::sourceManager().sourceTools(srcPath);
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

bool fileListingFilter(const core::FileInfo& fileInfo)
{
   // check extension for special file types which are always visible
   core::FilePath filePath(fileInfo.absolutePath());
   std::string ext = filePath.extensionLowerCase();
   if (ext == ".rprofile" ||
       ext == ".rdata"    ||
       ext == ".rhistory" ||
       ext == ".renviron" )
   {
      return true;
   }
   else
   {
      return !filePath.isHidden();
   }
}

// enque file changed event
void enqueFileChangedEvent(const core::system::FileChangeEvent& event,
                           const std::string& vcsStatus)
{
   // create file change object
   json::Object fileChange ;
   fileChange["type"] = event.type();
   json::Object fileSystemItem = createFileSystemItem(event.fileInfo());
   fileSystemItem["vcs_status"] = vcsStatus;
   fileChange["file"] = fileSystemItem;

   // enque it
   ClientEvent clientEvent(client_events::kFileChanged, fileChange);
   module_context::enqueClientEvent(clientEvent);
}


void enqueFileChangedEvents(const core::FilePath& vcsStatusRoot,
                            const std::vector<core::system::FileChangeEvent>& events)
{
   if (events.empty())
      return;

   // get vcs status in one shot
   session::modules::source_control::StatusResult statusResult;
   Error error = session::modules::source_control::status(vcsStatusRoot, &statusResult);
   if (error)
      LOG_ERROR(error);

   // fire client events as necessary
   BOOST_FOREACH(const system::FileChangeEvent& event, events)
   {
      core::FilePath filePath(event.fileInfo().absolutePath());
      std::string vcsStatus = statusResult.getStatus(filePath).status();
      module_context::enqueFileChangedEvent(event, vcsStatus);
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
}

void consoleWriteError(const std::string& message)
{
   // NOTE: all actions herein must be threadsafe! (see comment above)

   // add console action
   r::session::consoleActions().add(kConsoleActionOutputError, message);

   // enque write error (same as session::rConsoleWrite)
   ClientEvent event(client_events::kConsoleWriteError, message);
   enqueClientEvent(event);
}

void showErrorMessage(const std::string& title, const std::string& message)
{
   session::clientEventQueue().add(showErrorMessageEvent(title, message));
}

void showFile(const FilePath& filePath, const std::string& window)
{
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      // for pdfs allow default R handling -- uses options("pdfviewer")
      // on unix & osx and shell.exec on windows
      if (filePath.extensionLowerCase() == ".pdf")
      {
         std::string path = filePath.absolutePath();
         Error error = r::exec::RFunction(".rs.shellViewPdf", path).call();
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         // Send to the client, the desktop frame will handle it
         ClientEvent event = browseUrlEvent("file://" + filePath.absolutePath());
         module_context::enqueClientEvent(event);
      }
   }
   else if (session::options().programMode() == kSessionProgramModeServer)
   {
      // determine url based on whether this is in ~ or not
      std::string url ;

      if (isVisibleUserFile(filePath))
      {
         std::string relPath = filePath.relativePath(module_context::userHomePath());
         url = "files/" + relPath;
      }
      else
      {
         url = "file_show?path=" + http::util::urlEncode(filePath.absolutePath(), true);
      }

      // fire event
      ClientEvent event = browseUrlEvent(url);
      module_context::enqueClientEvent(event);
   }
}


void showContent(const std::string& title, const core::FilePath& filePath)
{
   // first provision a content url
   std::string contentUrl = modules::content_urls::provision(title, filePath);

   // fire event
   json::Object contentItem;
   contentItem["title"] = title;
   contentItem["contentUrl"] = contentUrl;
   ClientEvent event(client_events::kShowContent, contentItem);
   module_context::enqueClientEvent(event);
}

bool isGoogleDocsIntegrationEnabled()
{
   return s_googleDocsIntegrationEnabled;
}

void setGoogleDocsIntegrationEnabled(bool enabled)
{
   s_googleDocsIntegrationEnabled = enabled;
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

} // namespace module_context         
} // namespace session
