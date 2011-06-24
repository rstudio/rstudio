/*
 * SessionMain.cpp
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

#include <session/SessionMain.hpp>

#include <string>
#include <vector>
#include <queue>
#include <map>
#include <algorithm>
#include <cstdlib>
#include <csignal>

#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/format.hpp>

#include <boost/signals.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/BoostThread.hpp>
#include <core/FilePath.hpp>
#include <core/Exec.hpp>
#include <core/Scope.hpp>
#include <core/Settings.hpp>
#include <core/Thread.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/ProgramStatus.hpp>
#include <core/system/System.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/UriHandler.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/gwt/GwtLogHandler.hpp>
#include <core/gwt/GwtFileHandler.hpp>
#include <core/system/ParentProcessMonitor.hpp>
#include <core/text/TemplateFilter.hpp>

#include <r/RJsonRpc.hpp>
#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RInterface.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RGraphics.hpp>
#include <r/session/REventLoop.hpp>
#include <R_ext/rlocale.h>

#include <session/SessionConstants.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionSourceDatabase.hpp>

#include "SessionAddins.hpp"

#include "SessionPersistentState.hpp"
#include "SessionProjects.hpp"
#include "SessionModuleContextInternal.hpp"

#include "SessionClientEventQueue.hpp"
#include "SessionClientEventService.hpp"

#include "modules/SessionAgreement.hpp"
#include "modules/SessionConsole.hpp"
#include "modules/SessionDiff.hpp"
#include "modules/SessionFiles.hpp"
#include "modules/SessionWorkspace.hpp"
#include "modules/SessionWorkbench.hpp"
#include "modules/SessionData.hpp"
#include "modules/SessionHelp.hpp"
#include "modules/SessionPlots.hpp"
#include "modules/SessionPath.hpp"
#include "modules/SessionPackages.hpp"
#include "modules/SessionSource.hpp"
#include "modules/SessionSourceControl.hpp"
#include "modules/SessionTeX.hpp"
#include "modules/SessionHistory.hpp"
#include "modules/SessionLimits.hpp"
#include "modules/SessionContentUrls.hpp"

#include "workers/SessionWebRequestWorker.hpp"

#include <session/SessionHttpConnectionListener.hpp>

#include "config.h"

using namespace core; 
using namespace session;
using namespace session::client_events;

namespace {
   
// uri handlers
http::UriHandlers s_uriHandlers;
http::UriHandlerFunction s_defaultUriHandler;

// json rpc methods
core::json::JsonRpcMethods s_jsonRpcMethods;
   
// R browseUrl handlers
std::vector<module_context::RBrowseUrlHandler> s_rBrowseUrlHandlers;
   
// R browseFile handlers
std::vector<module_context::RBrowseFileHandler> s_rBrowseFileHandlers;

// have we fully initialized? used by rConsoleRead and clientInit to
// tweak their behavior when the process is first starting
bool s_sessionInitialized = false;

// was the underlying r session resumed
bool s_rSessionResumed = false;

// manage global state indicating whether R is processing input
volatile sig_atomic_t s_rProcessingInput = 0;

// did we fail to coerce the charset to UTF-8
bool s_printCharsetWarning = false;

std::queue<r::session::RConsoleInput> s_consoleInputBuffer;

// json rpc methods we handle (the rest are delegated to the HttpServer)
const char * const kClientInit = "client_init" ;
const char * const kConsoleInput = "console_input" ;
const char * const kEditCompleted = "edit_completed";
const char * const kChooseFileCompleted = "choose_file_completed";
const char * const kLocatorCompleted = "locator_completed";
const char * const kQuitSession = "quit_session" ;   
const char * const kInterrupt = "interrupt";

// convenience function for disallowing suspend (note still doesn't override
// the presence of s_forceSuspend = 1)
bool disallowSuspend() { return false; }
   
// request suspends (cooperative and forced) using interrupts
volatile sig_atomic_t s_suspendRequested = 0;
volatile sig_atomic_t s_forceSuspend = 0;
volatile sig_atomic_t s_forceSuspendInterruptedR = 0;
   
// cooperative suspend -- the http server is forced to timeout and a flag 
// indicating that we should suspend at ourfirst valid opportunity is set
void handleUSR1(int unused)
{   
   // note that a suspend has been requested. the process will suspend
   // at the first instance that it is valid for it to do so 
   s_suspendRequested = 1;
}

// forced suspend -- R is interrupted, the http server is forced to timeout,
// and the 'force' flag is set
void handleUSR2(int unused)
{
   // note whether R was interrupted
   if (s_rProcessingInput)
      s_forceSuspendInterruptedR = 1;

   // set the r interrupt flag (always)
   r::exec::setInterruptsPending(true);

   // note that a suspend is being forced. 
   s_forceSuspend = 1;
}

// version of the executable
double s_version = 0;
   
// installed version (computed as the time in seconds since epoch that 
// the running/served code was installed) can be distinct from the version 
// of the currently running executable if a deployment occured after this 
// executable started running.
double installedVersion()
{
   // never return a version in desktop mode
   if (session::options().programMode() == kSessionProgramModeDesktop)
      return 0;

   // read installation time (as string) from file (return 0 if not found)
   FilePath installedPath("/etc/rstudio/installed");
   if (!installedPath.exists())
      return 0;
   
   // attempt to get the value (return 0 if we have any trouble)
   std::string installedStr;
   Error error = readStringFromFile(installedPath, &installedStr);
   if (error)
   {
      LOG_ERROR(error);
      return 0;
   }
   
   // empty string means 0
   if (installedStr.empty())
   {
      LOG_ERROR_MESSAGE("No value within /etc/rstudio/installed");
      return 0;
   }
   
   // attempt to parse the value into a double
   double installedSeconds = 0.0;
   try
   {
      std::istringstream istr(installedStr);
      istr >> installedSeconds;
      if (istr.fail())
         LOG_ERROR(systemError(boost::system::errc::io_error, ERROR_LOCATION));
   }
   CATCH_UNEXPECTED_EXCEPTION
    
   // return installed seconds as version
   return installedSeconds ;
}
   

void detectChanges(module_context::ChangeSource source)
{
   module_context::events().onDetectChanges(source);
}
 

// certain things are deferred until after we have sent our first response
// take care of these things here
void ensureSessionInitialized()
{
   // note that we are now fully initialized. we defer setting this
   // flag so that consoleRead and handleClientInit know that we have just
   // started up and can act accordingly
   s_sessionInitialized = true;

   // ensure the session is fully deserialized (deferred deserialization
   // is supported so that the workbench UI can load without having to wait
   // for the potentially very lengthy deserialization of the environment)
   r::session::ensureDeserialized();
}

FilePath getDefaultWorkingDirectory()
{
   // calculate using user settings
   FilePath defaultWorkingDir = userSettings().initialWorkingDirectory();

   // return it if it exists, otherwise use the default user home path
   if (defaultWorkingDir.exists() && defaultWorkingDir.isDirectory())
      return defaultWorkingDir;
   else
      return session::options().userHomePath();
}

FilePath getInitialWorkingDirectory()
{
   // get paths
   FilePath projectDirPath = module_context::activeProjectDirectory();
   FilePath workingDirPath = session::options().initialWorkingDirOverride();

   // check for a project
   if (!projectDirPath.empty())
   {
      return projectDirPath;
   }

   // see if there is an override from the environment (perhaps based
   // on a folder drag and drop or other file association)
   else if (workingDirPath.exists() && workingDirPath.isDirectory())
   {
      return workingDirPath;
   }

   else
   {
      // if not then just return default working dir
      return getDefaultWorkingDirectory();
   }
}


void handleClientInit(const boost::function<void()>& initFunction,
                      boost::shared_ptr<HttpConnection> ptrConnection)
{
   // alias options
   Options& options = session::options();
   
   // calculate initialization parameters
   std::string clientId = session::persistentState().newActiveClientId();
   bool resumed = s_rSessionResumed || s_sessionInitialized;

   // if we are resuming then we don't need to worry about events queued up
   // by R during startup (e.g. printing of the banner) being sent to the
   // client. so, clear out the events which might be pending in the
   // client event service and/or queue
   bool clearEvents = resumed;
   
   // reset the client event service for the new client (will cause
   // outstanding http requests from old clients to fail with
   // InvalidClientId). note that we can't simply stop() the
   // ClientEventService and start() a new one because in that case the
   // old client will never get disconnected because it won't get
   // the InvalidClientId error.
   clientEventService().setClientId(clientId, clearEvents);

   // prepare session info 
   json::Object sessionInfo ;
   sessionInfo["clientId"] = clientId;
   sessionInfo["mode"] = options.programMode();
   
   // only send the user identity if we are in server mode
   if (options.programMode() == kSessionProgramModeServer)
   {
      sessionInfo["userIdentity"] = options.userIdentity(); 
   }

   // only send log_dir and scratch_dir if we are in desktop mode
   if (options.programMode() == kSessionProgramModeDesktop)
   {
      sessionInfo["log_dir"] = options.userLogPath().absolutePath();
      sessionInfo["scratch_dir"] = options.userScratchPath().absolutePath();
   }

   // temp dir
   sessionInfo["temp_dir"] = r::session::utils::tempDir().absolutePath();
   
   // installed version
   sessionInfo["version"] = installedVersion();
   
   // default prompt
   sessionInfo["prompt"] = r::options::getOption<std::string>("prompt");

   // console history
   json::Array historyArray;
   r::session::consoleHistory().asJson(&historyArray);
   sessionInfo["console_history"] = historyArray;
   sessionInfo["console_history_capacity"] =
                              r::session::consoleHistory().capacity();
   
   // client state
   json::Object clientStateObject;
   r::session::clientState().currentState(&clientStateObject);
   sessionInfo["client_state"] = clientStateObject;
   
   // source documents
   json::Array jsonDocs;
   Error error = source_database::getSourceDocumentsJson(&jsonDocs);
   if (error)
      LOG_ERROR(error);
   sessionInfo["source_documents"] = jsonDocs;
   
   // agreement
   sessionInfo["hasAgreement"] = modules::agreement::hasAgreement();
   sessionInfo["pendingAgreement"] = modules::agreement::pendingAgreement();

   // docs url
   sessionInfo["docsURL"] = session::options().docsURL();

   // get alias to console_actions and get limit
   r::session::ConsoleActions& consoleActions = r::session::consoleActions();
   sessionInfo["console_actions_limit"] = consoleActions.capacity();

   // resumed
   sessionInfo["resumed"] = resumed; 
   if (resumed)
   {
      // console actions
      json::Object actionsObject;
      consoleActions.asJson(&actionsObject);
      sessionInfo["console_actions"] = actionsObject;
   }

   bool texInstalled;
   error = r::exec::RFunction(".rs.is_tex_installed").call(&texInstalled);
   sessionInfo["tex_installed"] = !error ? texInstalled : false;

   sessionInfo["googleDocsIntegrationEnabled"] =
         session::module_context::isGoogleDocsIntegrationEnabled();

   sessionInfo["rstudio_version"] = std::string(RSTUDIO_VERSION);

   sessionInfo["ui_prefs"] = userSettings().uiPrefs();
   
   // initial working directory
   std::string initialWorkingDir = module_context::createAliasedPath(
                                                getInitialWorkingDirectory());
   sessionInfo["initial_working_dir"] = initialWorkingDir;

   // active project file
   FilePath activeProjectFile = module_context::activeProjectFilePath();
   if (!activeProjectFile.empty())
      sessionInfo["active_project_file"] = module_context::createAliasedPath(
                                                               activeProjectFile);
   else
      sessionInfo["active_project_file"] = json::Value();

   sessionInfo["system_encoding"] = std::string(::locale2charset(NULL));

   sessionInfo["vcs"] = modules::source_control::activeVCSName();

   // send response  (we always set kEventsPending to false so that the client
   // won't poll for events until it is ready)
   json::JsonRpcResponse jsonRpcResponse ;
   jsonRpcResponse.setField(kEventsPending, "false");
   jsonRpcResponse.setResult(sessionInfo) ;
   ptrConnection->sendJsonRpcResponse(jsonRpcResponse);

   // complete initialization of session
   ensureSessionInitialized();
   
   // notify modules of the client init
   module_context::events().onClientInit();
   
   // call the init function
   initFunction();
}

enum ConnectionType
{
   ForegroundConnection,
   BackgroundConnection
};

void handleRpcRequest(const core::json::JsonRpcRequest& request,
                      boost::shared_ptr<HttpConnection> ptrConnection,
                      ConnectionType connectionType)
{
   // record the time just prior to execution of the event
   // (so we can determine if any events were added during execution)
   using namespace boost::posix_time; 
   ptime executeStartTime = microsec_clock::universal_time();
   
   // execute the method
   Error executeError;
   core::json::JsonRpcResponse jsonRpcResponse ;
   if (connectionType == BackgroundConnection)
      jsonRpcResponse.setSuppressDetectChanges(true);
   json::JsonRpcMethods::const_iterator it = s_jsonRpcMethods.find(request.method);
   if (it != s_jsonRpcMethods.end())
   {
      json::JsonRpcFunction handlerFunction = it->second ;
      executeError = handlerFunction(request, &jsonRpcResponse) ;
   }
   else
   {
      executeError = Error(json::errc::MethodNotFound, ERROR_LOCATION);
      executeError.addProperty("method", request.method);

      // we need to know about these because they represent unexpected
      // application states
      LOG_ERROR(executeError);
   }

   // return error or result then continue waiting for requests
   if (executeError)
   {
      ptrConnection->sendJsonRpcError(executeError);
   }
   else
   {
      // allow modules to detect changes after rpc calls
      if (!jsonRpcResponse.suppressDetectChanges())
         detectChanges(module_context::ChangeSourceRPC);
      
      // are there (or will there likely be) events pending?
      // (if not then notify the client)
      if ( !clientEventQueue().eventAddedSince(executeStartTime) &&
           !jsonRpcResponse.hasAfterResponse() )
      {
         jsonRpcResponse.setField(kEventsPending, "false");
      }
      
      // send the response
      ptrConnection->sendJsonRpcResponse(jsonRpcResponse);
      
      // run after response if we have one (then detect changes again)
      if (jsonRpcResponse.hasAfterResponse())
      {
         jsonRpcResponse.runAfterResponse();
         if (!jsonRpcResponse.suppressDetectChanges())
            detectChanges(module_context::ChangeSourceRPC);
      }
   }
}

bool isMethod(const std::string& uri, const std::string& method)
{
   return boost::algorithm::ends_with(uri, method);
}

bool isMethod(boost::shared_ptr<HttpConnection> ptrConnection,
              const std::string& method)
{
   return isMethod(ptrConnection->request().uri(), method);
}

bool isJsonRpcRequest(boost::shared_ptr<HttpConnection> ptrConnection)
{
   return boost::algorithm::starts_with(ptrConnection->request().uri(),
                                        "/rpc/");
}


bool parseAndValidateJsonRpcConnection(
         boost::shared_ptr<HttpConnection> ptrConnection,
         json::JsonRpcRequest* pJsonRpcRequest)
{
   // attempt to parse the request into a json-rpc request
   Error error = json::parseJsonRpcRequest(ptrConnection->request().body(),
                                           pJsonRpcRequest);
   if (error)
   {
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // check for invalid client id
   if (pJsonRpcRequest->clientId != session::persistentState().activeClientId())
   {
      Error error(json::errc::InvalidClientId, ERROR_LOCATION);
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // check for old client version
   if ( (pJsonRpcRequest->version > 0) &&
        (s_version > pJsonRpcRequest->version) )
   {
      Error error(json::errc::InvalidClientVersion, ERROR_LOCATION);
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // got through all of the validation, return true
   return true;
}

void handleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                      ConnectionType connectionType)
{
   // check for a uri handler registered by a module
   const http::Request& request = ptrConnection->request();
   std::string uri = request.uri();
   http::UriHandlerFunction uriHandler = s_uriHandlers.handlerFor(uri);

   if (uriHandler) // uri handler
   {
      // r code may execute - ensure session is initialized
      ensureSessionInitialized();

      http::Response response;
      uriHandler(request, &response);
      ptrConnection->sendResponse(response);

      // allow modules to check for changes after http requests
      if (connectionType == ForegroundConnection)
         detectChanges(module_context::ChangeSourceURI);
   }
   else if (isJsonRpcRequest(ptrConnection)) // check for json-rpc
   {
      // r code may execute - ensure session is initialized
      ensureSessionInitialized();

      // attempt to parse & validate
      json::JsonRpcRequest jsonRpcRequest;
      if (parseAndValidateJsonRpcConnection(ptrConnection, &jsonRpcRequest))
      {
         // quit_session: exit process
         if (jsonRpcRequest.method == kQuitSession)
         {
            // see whether we should save the workspace
            bool saveWorkspace = true;
            Error error = json::readParam(jsonRpcRequest.params,
                                          0,
                                          &saveWorkspace) ;
            if (error)
               LOG_ERROR(error);

            // acknowledge request & quit session
            ptrConnection->sendJsonRpcResponse();
            r::session::quit(saveWorkspace); // does not return
         }

         // interrupt
         else if ( jsonRpcRequest.method == kInterrupt )
         {
            // Discard any buffered input
            while (!s_consoleInputBuffer.empty())
               s_consoleInputBuffer.pop();

            // aknowledge request
            ptrConnection->sendJsonRpcResponse();

            // only accept interrupts while R is processing input
            if ( s_rProcessingInput )
               r::exec::setInterruptsPending(true);
         }

         // other rpc method, handle it
         else
         {
            handleRpcRequest(jsonRpcRequest, ptrConnection, connectionType);
         }
      }
   }
   else if (s_defaultUriHandler)
   {
       http::Response response;
       s_defaultUriHandler(request, &response);
       ptrConnection->sendResponse(response);
   }
   else
   {
      http::Response response;
      response.setError(http::status::NotFound, request.uri() + " not found");
      ptrConnection->sendResponse(response);
   }
}

// fork state
boost::thread::id s_mainThreadId;
bool s_wasForked = false;

// fork handlers (only applicatable to Unix platforms)
#ifndef _WIN32

void prepareFork()
{
   // only detect forks from the main thread (since we are going to be
   // calling into non-threadsafe code). this is ok because fork
   // detection is meant to handle forks that don't exec (and thus
   // continue running R code). only the main thread will ever do this
   if (boost::this_thread::get_id() != s_mainThreadId)
      return;

   // if the main thread is being forked then it could be multicore
   // (or another package which works in the same way). we don't want
   // file monitoring objects inherited by the forked multicore child
   // so we pause file monitoring in the parent first. we'll resume
   // after the fork in the atForkParent call below.
   session::modules::files::pauseDirectoryMonitor();

}

void atForkParent()
{
   if (boost::this_thread::get_id() != s_mainThreadId)
      return;

   // resume monitoring
   session::modules::files::resumeDirectoryMonitor();
}

void atForkChild()
{
   s_wasForked = true;
}

void setupForkHandlers()
{
   int rc = ::pthread_atfork(prepareFork, atForkParent, atForkChild);
   if (rc != 0)
      LOG_ERROR(systemError(errno, ERROR_LOCATION));
}
#else
void setupForkHandlers()
{

}
#endif


void polledEventHandler()
{
   // if R is getting called after a fork this is likely multicore or
   // some other parallel computing package that uses fork. in this
   // case be defensive by shutting down as many things as we can
   // which might cause mischief in the child process
   if (s_wasForked)
   {
      // no more polled events
      r::session::event_loop::permanentlyDisablePolledEventHandler();

      // done
      return;
   }

   // check for a pending connections only while R is processing
   // (otherwise we'll handle them directly in waitForMethod)
   if (s_rProcessingInput)
   {
      // check the uri of the next connection
      std::string nextConnectionUri =
       httpConnectionListener().mainConnectionQueue().peekNextConnectionUri();

      // if the uri is empty or if it one of our special waitForMethod calls
      // then bails so that the waitForMethod logic can handle it
      if (nextConnectionUri.empty() ||
          isMethod(nextConnectionUri, kLocatorCompleted) ||
          isMethod(nextConnectionUri, kEditCompleted) ||
          isMethod(nextConnectionUri, kChooseFileCompleted))
      {
         return;
      }

      // attempt to deque a connection and handle it. for now we just handle
      // a single connection at a time (we'll be called back again if processing
      // continues)
      boost::shared_ptr<HttpConnection> ptrConnection =
            httpConnectionListener().mainConnectionQueue().dequeConnection();
      if (ptrConnection)
      {
         if ( isMethod(ptrConnection, kClientInit) )
         {
            // client_init means the user is attempting to reload the browser
            // in the middle of a computation. process client_init and post
            // a busy event as our initFunction
            using namespace session::module_context;
            ClientEvent busyEvent(client_events::kBusy, true);
            handleClientInit(boost::bind(enqueClientEvent, busyEvent),
                             ptrConnection);
         }
         else
         {
            handleConnection(ptrConnection, BackgroundConnection);
         }
      }
   }
}

bool suspendSession(bool force)
{
   // need to make sure the global environment is loaded before we
   // attemmpt to save it!
   r::session::ensureDeserialized();

   // perform the suspend (does not return if successful)
   return r::session::suspend(force);
}

void suspendIfRequested(const boost::function<bool()>& allowSuspend)
{
   // never suspend in desktop mode
   if (session::options().programMode() == kSessionProgramModeDesktop)
      return;

   // check for forced suspend request
   if (s_forceSuspend)
   {
      // reset flag (if for any reason we fail we don't want to keep
      // hammering away on the failure case)
      s_forceSuspend = false;

      // did this force suspend interrupt R?
      if (s_forceSuspendInterruptedR)
      {
         // reset flag
         s_forceSuspendInterruptedR = false;

         // notify user
         r::session::reportAndLogWarning(
            "Session forced to suspend due to system upgrade, restart, maintenance, "
            "or other issue. Your session data was saved however running "
            "computations may have been interrupted.");
      }

      // execute the forced suspend (does not return)
      suspendSession(true);
   }

   // cooperative suspend request
   else if (s_suspendRequested && allowSuspend())
   {
      // reset flag (if for any reason we fail we don't want to keep
      // hammering away on the failure case)
      s_suspendRequested = false;

      // attempt suspend -- if this succeeds it doesn't return; if it fails
      // errors will be logged/reported internally and we will move on
      suspendSession(false);
   }
}

bool isTimedOut(const boost::posix_time::ptime& timeoutTime)
{
   // never time out in desktop mode
   if (session::options().programMode() == kSessionProgramModeDesktop)
      return false;

   if (timeoutTime.is_not_a_date_time())
      return false;
   else
      return boost::posix_time::second_clock::universal_time() > timeoutTime;
}

boost::posix_time::ptime timeoutTimeFromNow()
{
   int timeoutMinutes = session::options().timeoutMinutes();
   if (timeoutMinutes > 0)
   {
      return boost::posix_time::second_clock::universal_time() +
             boost::posix_time::minutes(session::options().timeoutMinutes());
   }
   else
   {
      return boost::posix_time::ptime(boost::posix_time::not_a_date_time);
   }
}

void processDesktopGuiEvents()
{
   // keep R gui alive when we are in destkop mode
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      // execute safely since this can call arbitrary R code (and
      // (can also cause jump_to_top if an interrupt is pending)
      Error error = r::exec::executeSafely(
                        r::session::event_loop::processEvents);
      if (error)
         LOG_ERROR(error);
   }
}


// wait for the specified method. will either:
//   - return true and the method request in pRequest
//   - return false indicating failure (e.g. called after fork in child)
//   - suspend or quit the process
// exit the process as a result of suspend or quit)
bool waitForMethod(const std::string& method,
                   const boost::function<void()>& initFunction,
                   const boost::function<bool()>& allowSuspend,
                   core::json::JsonRpcRequest* pRequest)
{
   if (s_wasForked)
   {
      LOG_ERROR_MESSAGE("Waiting for method " + method + " after fork");
      return false;
   }

   // determine mode
   bool desktopMode = session::options().programMode() ==
                                             kSessionProgramModeDesktop;

   // establish timeouts
   boost::posix_time::ptime timeoutTime = timeoutTimeFromNow();
   boost::posix_time::time_duration connectionQueueTimeout;
   if (desktopMode)
      connectionQueueTimeout =  boost::posix_time::milliseconds(50);
   else
      connectionQueueTimeout = boost::posix_time::milliseconds(500);

   // wait until we get the method we are looking for
   while(true)
   {
      // suspend if necessary (does not return if a suspend occurs)
      suspendIfRequested(allowSuspend);

      // check for timeout
      if ( isTimedOut(timeoutTime) )
      {
         if (allowSuspend())
         {
            // attempt to suspend (does not return if it succeeds)
            if ( !suspendSession(false) )
            {
               // if it fails then reset the timeout timer so we don't keep
               // hammering away on the failure case
               timeoutTime = timeoutTimeFromNow();
            }
         }
      }

      // look for a connection (waiting for the specified interval)
      boost::shared_ptr<HttpConnection> ptrConnection =
          httpConnectionListener().mainConnectionQueue().dequeConnection(
                                            connectionQueueTimeout);

      // process pending events in desktop mode
      processDesktopGuiEvents();

      if (ptrConnection)
      {
         // check for client_init
         if ( isMethod(ptrConnection, kClientInit) )
         {
             handleClientInit(initFunction, ptrConnection);
         }

         // check for the method we are waiting on
         else if ( isMethod(ptrConnection, method) )
         {
            // parse and validate request then proceed
            if (parseAndValidateJsonRpcConnection(ptrConnection, pRequest))
            {
               // respond to the method
               ptrConnection->sendJsonRpcResponse();

               // ensure initialized
               ensureSessionInitialized();

               break; // got the method, we are out of here!
            }
         }

         // another connection type, dispatch it
         else
         {
            handleConnection(ptrConnection, ForegroundConnection);
         }

         // since we got a connection we can reset the timeout time
         timeoutTime = timeoutTimeFromNow();

         // after we've processed at least one waitForMethod it is now safe to
         // initialize the polledEventHandler (which is used to maintain rsession
         // responsiveness even when R is executing code received at the console).
         // we defer this to make sure that the FIRST request is always handled
         // by the logic above. if we didn't do this then client_init or
         // console_input (often the first request) could go directly to
         // handleConnection which wouldn't know what to do with them
         if (!r::session::event_loop::polledEventHandlerInitialized())
            r::session::event_loop::initializePolledEventHandler(
                                                     polledEventHandler);
      }
   }

   // satisfied the request
   return true;
}


// wait for the specified method (will either return the method or 
// exit the process as a result of suspend or quit)
bool waitForMethod(const std::string& method,
                   const ClientEvent& initEvent,
                   const boost::function<bool()>& allowSuspend,
                   core::json::JsonRpcRequest* pRequest)
{
   return waitForMethod(method,
                        boost::bind(module_context::enqueClientEvent,
                                    initEvent),
                        allowSuspend,
                        pRequest);
}

void addToConsoleInputBuffer(const r::session::RConsoleInput& consoleInput)
{
   if (consoleInput.cancel || consoleInput.text.find('\n') == std::string::npos)
   {
      s_consoleInputBuffer.push(consoleInput);
      return;
   }

   // split input into list of commands
   boost::char_separator<char> lineSep("\n", "", boost::keep_empty_tokens);
   boost::tokenizer<boost::char_separator<char> > lines(consoleInput.text, lineSep);
   for (boost::tokenizer<boost::char_separator<char> >::iterator
        lineIter = lines.begin();
        lineIter != lines.end();
        ++lineIter)
   {
      // get line
      std::string line(*lineIter);

      // add to buffer
      s_consoleInputBuffer.push(line);
   }
}

// extract console input -- can be either null (user hit escape) or a string
Error extractConsoleInput(const json::JsonRpcRequest& request)
{
   if (request.params.size() == 1)
   {
      if (request.params[0].is_null())
      {
         addToConsoleInputBuffer(r::session::RConsoleInput());
         return Success();
      }
      else if (request.params[0].type() == json::StringType)
      {
         // get console input to return to R
         std::string text = request.params[0].get_str();
         addToConsoleInputBuffer(r::session::RConsoleInput(text));

         // return success
         return Success();
      }
      else
      {
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      }
   }
   else
   {
      return Error(json::errc::ParamMissing, ERROR_LOCATION);
   }
}

// allow console_input requests to come in when we aren't explicitly waiting
// on them (i.e. waitForMethod("console_input")). place them into into a buffer
// which is then checked by rConsoleRead prior to it calling waitForMethod
Error bufferConsoleInput(const core::json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // extract the input
   return extractConsoleInput(request);
}

Error startHttpConnectionListener()
{
   initializeHttpConnectionListener();
   return httpConnectionListener().start();
}

Error startClientEventService()
{
   return clientEventService().start(session::persistentState().activeClientId());
}

void registerGwtHandlers()
{
   // alias options
   session::Options& options = session::options();

   // establish logging handler
   module_context::registerUriHandler(
         "/log",
         boost::bind(gwt::handleLogRequest, options.userIdentity(), _1, _2));

   // establish progress handler
   FilePath wwwLocalPath(options.wwwLocalPath());
   FilePath progressPagePath = wwwLocalPath.complete("progress.htm");
   module_context::registerUriHandler(
         "/progress",
          boost::bind(text::handleTemplateRequest, progressPagePath, _1, _2));

   // set default handler
   s_defaultUriHandler = gwt::fileHandlerFunction(options.wwwLocalPath(), "/");
}

Error registerSignalHandlers()
{
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      using boost::bind;
      using namespace core::system;
      ExecBlock registerBlock ;
      registerBlock.addFunctions()
         (bind(handleSignal, SigUsr1, handleUSR1))
         (bind(handleSignal, SigUsr2, handleUSR2));
      return registerBlock.execute();
   }
   else
   {
      return Success();
   }
}


Error runPreflightScript()
{
   // alias options
   Options& options = session::options();

   // run the preflight script (if specified)
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      FilePath preflightScriptPath = options.preflightScriptPath();
      if (!preflightScriptPath.empty())
      {
         if (preflightScriptPath.exists())
         {
            // run the script (ignore errors and continue no matter what
            // the outcome of the script is)
            std::string script = preflightScriptPath.absolutePath();
            std::string output;
            Error error = core::system::captureCommand(script, &output);
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
      
Error rInit(const r::session::RInitInfo& rInitInfo) 
{
   // save state we need to reference later
   s_rSessionResumed = rInitInfo.resumed;
   
   // execute core initialization functions
   using boost::bind;
   using namespace core::system;
   using namespace session::module_context;
   ExecBlock initialize ;
   initialize.addFunctions()
   
      // client event service
      (startClientEventService)

      // json-rpc listeners
      (bind(registerRpcMethod, kConsoleInput, bufferConsoleInput))

      // signal handlers
      (registerSignalHandlers)

      // main module context
      (module_context::initialize)

      // source database
      (source_database::initialize)
   
      // modules with c++ implementations
      (modules::path::initialize)
      (modules::content_urls::initialize)
      (modules::limits::initialize)
      (modules::agreement::initialize)
      (modules::console::initialize)
      (modules::diff::initialize)
      (modules::files::initialize)
      (modules::workspace::initialize)
      (modules::workbench::initialize)
      (modules::data::initialize)
      (modules::help::initialize)
      (modules::plots::initialize)
      (modules::packages::initialize)
      (modules::source::initialize)
      (modules::source_control::initialize)
      (modules::tex::initialize)
      (modules::history::initialize)

      // workers
      (workers::web_request::initialize)

      // addins
      (addins::initialize)

      // R code
      (bind(sourceModuleRFile, "SessionCodeTools.R"))
   
      // unsupported functions
      (bind(r::function_hook::registerUnsupported, "bug.report", "utils"))
      (bind(r::function_hook::registerUnsupported, "help.request", "utils"))
   ;

   Error error = initialize.execute();
   if (error)
      return error;
   
   // if we are in verify installation mode then we should exit (successfully) now
   if (session::options().verifyInstallation())
   {
      FilePath(kVerifyInstallationHomeDir).removeIfExists();
      ::exit(EXIT_SUCCESS);
   }

   // register all of the json rpc methods implemented in R
   json::JsonRpcMethods rMethods ;
   error = r::json::getRpcMethods(&rMethods);
   if (error)
      return error ;
   s_jsonRpcMethods.insert(rMethods.begin(), rMethods.end());

   // add gwt handlers if we are running desktop mode
   if (session::options().programMode() == kSessionProgramModeDesktop)
      registerGwtHandlers();

   // enque abend warning event if necessary. this is done for server
   // mode only because in desktop mode we don't get a callback
   // indicating that R is exiting
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      using namespace session::client_events;
      if (session::persistentState().hadAbend())
      {
         LOG_ERROR_MESSAGE("session hadabend");

         ClientEvent abendWarningEvent(kAbendWarning);
         session::clientEventQueue().add(abendWarningEvent);
      }
   }

   if (s_printCharsetWarning)
      r::exec::warning("Character set is not UTF-8; please change your locale");

   // propagate console history options
   r::session::consoleHistory().setRemoveDuplicates(
                                 userSettings().removeHistoryDuplicates());

   // set flag indicating we had an abnormal end (if this doesn't get
   // unset by the time we launch again then we didn't terminate normally
   // i.e. either the process dying unexpectedly or a call to R_Suicide)
   session::persistentState().setAbend(true);
   
   // setup fork handlers
   setupForkHandlers();

   // success!
   return Success();
}
   
void consolePrompt(const std::string& prompt, bool addToHistory)
{
   // enque the event
   json::Object data ;
   data["prompt"] = prompt ;
   data["history"] = addToHistory;
   bool isDefaultPrompt = prompt == r::options::getOption<std::string>("prompt");
   data["default"] = isDefaultPrompt;
   ClientEvent consolePromptEvent(client_events::kConsolePrompt, data);
   session::clientEventQueue().add(consolePromptEvent);
   
   // allow modules to detect changes after execution of previous REPL
   detectChanges(module_context::ChangeSourceREPL);   
}

bool rConsoleRead(const std::string& prompt,
                  bool addToHistory,
                  r::session::RConsoleInput* pConsoleInput)
{
   // this is an invalid state in a forked (multicore) process
   if (s_wasForked)
   {
      LOG_WARNING_MESSAGE("rConsoleRead called in forked processs");
      return false;
   }

   // r is not processing input
   s_rProcessingInput = false;

   if (!s_consoleInputBuffer.empty())
   {
      *pConsoleInput = s_consoleInputBuffer.front();
      s_consoleInputBuffer.pop();
   }
   // otherwise prompt and wait for console_input from the client
   else
   {
      // fire console_prompt event (unless we are just starting up, in which
      // case we will either prompt as part of the response to client_init or
      // we shouldn't prompt at all because we are resuming a suspended session)
      if (s_sessionInitialized)
         consolePrompt(prompt, addToHistory);

      // wait for console_input
      json::JsonRpcRequest request ;
      bool succeeded = waitForMethod(
                        kConsoleInput,
                        boost::bind(consolePrompt, prompt, addToHistory),
                        boost::bind(r::session::isSuspendable, prompt),
                        &request);

      // exit process if we failed
      if (!succeeded)
         return false;

      // extract console input. if there is an error during extraction we log it
      // but still return and empty string and true (returning false will cause R
      // to abort)
      Error error = extractConsoleInput(request);
      if (error)
      {
         LOG_ERROR(error);
         *pConsoleInput = r::session::RConsoleInput("");
      }
      *pConsoleInput = s_consoleInputBuffer.front();
      s_consoleInputBuffer.pop();
   }

   // fire onBeforeExecute event if this isn't a cancel
   if (!pConsoleInput->cancel)
      module_context::events().onBeforeExecute();

   // we are about to return input to r so set the flag indicating that state
   s_rProcessingInput = true;

   ClientEvent promptEvent(kConsoleWritePrompt, prompt);
   session::clientEventQueue().add(promptEvent);
   ClientEvent inputEvent(kConsoleWriteInput, pConsoleInput->text + "\n");
   session::clientEventQueue().add(inputEvent);

   // always return true (returning false causes the process to exit)
   return true;
}


int rEditFile(const std::string& file)
{
   // read file contents
   FilePath filePath(file);
   std::string fileContents;
   Error readError = core::readStringFromFile(filePath, &fileContents);
   if (readError)
   {
      LOG_ERROR(readError);
      return 1; // r will raise/report an error indicating edit failed
   }
   
   // fire edit event
   ClientEvent editEvent(kShowEditor, fileContents);
   session::clientEventQueue().add(editEvent);

   // wait for edit_completed 
   json::JsonRpcRequest request ;
   bool succeeded = waitForMethod(kEditCompleted,
                                  editEvent,
                                  disallowSuspend,
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
   session::clientEventQueue().add(chooseFileEvent);
   
   // wait for choose_file_completed 
   json::JsonRpcRequest request ;
   bool succeeded = waitForMethod(kChooseFileCompleted,
                                 chooseFileEvent,
                                 disallowSuspend,
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
   ClientEvent busyEvent(kBusy, busy);
   session::clientEventQueue().add(busyEvent);
}
      
void rConsoleWrite(const std::string& output, int otype)   
{
   int event = otype == 1 ? kConsoleWriteError : kConsoleWriteOutput;
   ClientEvent writeEvent(event, output);
   session::clientEventQueue().add(writeEvent);
}
   
void rConsoleHistoryReset()
{
   json::Array historyJson;
   r::session::consoleHistory().asJson(&historyJson);
   json::Object resetJson;
   resetJson["history"] = historyJson;
   resetJson["preserve_ui_context"] = false;
   ClientEvent event(kConsoleResetHistory, resetJson);
   session::clientEventQueue().add(event);
}

bool rLocator(double* x, double* y)
{
   // since locator can be called in a loop we need to checkForChanges
   // here (because we'll never get back to the REPL). this enables
   // identify() to correctly update the plot after each click
   detectChanges(module_context::ChangeSourceREPL);
   
   // fire locator event
   ClientEvent locatorEvent(kLocator);
   session::clientEventQueue().add(locatorEvent);
   
   // wait for locator_completed 
   json::JsonRpcRequest request ;
   bool succeeded = waitForMethod(kLocatorCompleted,
                                  locatorEvent,
                                  disallowSuspend,
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
   if (session::options().programMode() == kSessionProgramModeServer)
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
   else // (session::options().programMode() == kSessionProgramModeDesktop
   {
      module_context::showContent(title, filePath);
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
   session::clientEventQueue().add(browseUrlEvent(url));
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
   
   // no handlers took it, send along to default
   module_context::showFile(filePath);
}

void rShowHelp(const std::string& helpURL)   
{
   ClientEvent showHelpEvent(kShowHelp, helpURL);
   session::clientEventQueue().add(showHelpEvent);
}
      
void rShowMessage(const std::string& message)   
{
   ClientEvent event = showErrorMessageEvent("R Error", message);
   session::clientEventQueue().add(event);
}
   
void rSuspended()
{
   module_context::onSuspended(&(persistentState().settings()));
}
   
void rResumed()
{
   module_context::onResumed(persistentState().settings());
}
      
void rQuit()
{   
   // enque a quit event
   ClientEvent quitEvent(kQuit);
   session::clientEventQueue().add(quitEvent);
}
   
// NOTE: this event is never received on windows (because we can't
// override suicide on windows)
void rSuicide(const std::string& message)
{
   if (s_wasForked)
      return;

   // log the error
   LOG_ERROR_MESSAGE("R SUICIDE: " + message);
   
   // enque suicide event so the client knows
   ClientEvent suicideEvent(kSuicide, message);
   session::clientEventQueue().add(suicideEvent);
}

void rCleanup(bool terminatedNormally)
{
   try
   {
      // bail completely if we were forked
      if (s_wasForked)
         return;

      // note that we didn't abend
      if (terminatedNormally)
         session::persistentState().setAbend(false);

      // fire shutdown event to modules
      module_context::events().onShutdown(terminatedNormally);

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
      if (session::options().programMode() == kSessionProgramModeServer)
      {
         clientEventService().stop();
         httpConnectionListener().stop();
      }
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
   session::clientEventQueue().add(event);
}

   
void ensureRProfile()
{
   Options& options = session::options();
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
   Options& options = session::options();
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

// NOTE: mirrors behavior of WorkbenchContext.getREnvironmentPath on the client
FilePath rEnvironmentDir()
{
   // for projects we always use the project directory
   FilePath activeProjectDir = module_context::activeProjectDirectory();
   if (!activeProjectDir.empty())
   {
      return activeProjectDir;
   }

   // for desktop the current path
   else if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      return FilePath::safeCurrentPath(session::options().userHomePath());
   }

   // for server the initial working dir
   else
   {
      return getInitialWorkingDirectory();
   }
}

FilePath rHistoryDir()
{
   // for projects we always use the project directory
   FilePath activeProjectDir = module_context::activeProjectDirectory();
   if (!activeProjectDir.empty())
   {
      return activeProjectDir;
   }

   // for server we use the default working directory
   else if (session::options().programMode() == kSessionProgramModeServer)
   {
      return getDefaultWorkingDirectory();
   }

   // for desktop we use the current path
   else
   {
      return FilePath::safeCurrentPath(session::options().userHomePath());
   }
}

FilePath getStartupEnvironmentFilePath()
{
   FilePath envFile = session::options().initialEnvironmentFileOverride();
   if (!envFile.empty())
      return envFile;
   else
      return rEnvironmentDir().complete(".RData");
}

} // anonymous namespace


// provide definition methods for session::module_context
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
 
Error registerUriHandler(const std::string& name, 
                         const http::UriHandlerFunction& handlerFunction)  
{
   
   s_uriHandlers.add(http::UriHandler(name,
                                      handlerFunction));
   return Success();
}
   
  
Error registerLocalUriHandler(const std::string& name, 
                              const http::UriHandlerFunction& handlerFunction)
{
   s_uriHandlers.add(http::UriHandler(kLocalUriLocationPrefix + name,
                                      handlerFunction));
   return Success();
}
   

Error registerRpcMethod(const std::string& name,
                        const core::json::JsonRpcFunction& function)
{
   s_jsonRpcMethods.insert(std::make_pair(name, function));
   return Success();
}

namespace {
bool continueChildProcess()
{
   // pump events so we can actually receive an interrupt
   polledEventHandler();

   // check for interrupts pending. note that we need to do this
   // before we call event_loop::processEvents because code within
   // there might clear the interrupts pending flag
   if (r::exec::interruptsPending())
      return false;

   // keep R gui alive when we are in desktop mode
   processDesktopGuiEvents();

   // return status
   return true;
}

} // anonymous namespace

Error executeInterruptableChild(std::string path,
                                core::system::Options args)
{
   return core::system::executeInterruptableChildProcess(path,
                                                         args,
                                                         100,
                                                         continueChildProcess);
}

} // namespace module_context
} // namespace session


namespace {

int sessionExitFailure(const core::Error& cause,
                       const core::ErrorLocation& location)
{
   core::log::logError(cause, location);
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
   std::string charset(locale2charset(NULL));
   if (charset == "UTF-8")
      return true;

   std::string name = ctypeEnvName();
   std::string ctype = core::system::getenv(name);

   std::string newCType;
   if (ctype.empty() || ctype == "C" || ctype == "POSIX")
   {
      newCType = "en_US.UTF-8";
   }
   else
   {
      using namespace boost;

      smatch match;
      if (regex_match(ctype, match, regex("(\\w+_\\w+)(\\.[^@]+)?(@.+)?")))
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

      // get main thread id (used to distinguish forks which occur
      // from the main thread vs. child threads)
      s_mainThreadId = boost::this_thread::get_id();

      // determine character set
      s_printCharsetWarning = !ensureUtf8Charset();

      // read program options
      Options& options = session::options();
      ProgramStatus status = options.read(argc, argv) ;
      if (status.exit())
         return status.exitCode() ;

      // convenience flags for server and desktop mode
      bool desktopMode = options.programMode() == kSessionProgramModeDesktop;
      bool serverMode = options.programMode() == kSessionProgramModeServer;

      // re-initialize log for desktop mode
      if (desktopMode)
      {
         initializeLog(options.programIdentity(),
                       core::system::kLogLevelWarning,
                       options.userLogPath());
      }

      // set version
      s_version = installedVersion();

      // set the rstudio user identity environment variable (can differ from
      // username in debug configurations). this is provided so that 
      // rpostback knows what local stream to connect back to
      core::system::setenv(kRStudioUserIdentity, options.userIdentity());
           
      // ensure we aren't being started as a low (priviliged) account
      if (serverMode &&
          core::system::currentUserIsPrivilleged(options.minimumUserId()))
      {
         Error error = systemError(boost::system::errc::permission_denied,
                                   ERROR_LOCATION);
         return sessionExitFailure(error, ERROR_LOCATION);
      }   
      
      // automatically reap children. note that dong this rather than
      // than SIGIGN on SIGCHILD allowed R_system/system to get correct
      // process exit codes back
      Error error = core::system::reapChildren();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // initialize client event queue
      session::initializeClientEventQueue();

      // detect parent termination
      if (desktopMode)
         core::thread::safeLaunchThread(detectParentTermination);

      // ensure that the user scratch path exists
      FilePath userScratchPath = options.userScratchPath();
      error = userScratchPath.ensureDirectory();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // initialize user settings
      error = userSettings().initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION) ;

      // initialize persistent state
      error = session::persistentState().initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION) ;

      // initialize projects -- must be after userSettings & persistentState are
      // initialized must be before setting working directory
      error = projects::initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);

      // set working directory
      FilePath workingDir = getInitialWorkingDirectory();
      error = workingDir.makeCurrentPath();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION);
      
      // start http connection listener
      error = startHttpConnectionListener();
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

      // install home and doc dir overrides if requested (for debugger mode)
      if (!options.rHomeDirOverride().empty())
         core::system::setenv("R_HOME", options.rHomeDirOverride());
      if (!options.rDocDirOverride().empty())
         core::system::setenv("R_DOC_DIR", options.rDocDirOverride());

      // r options
      r::session::ROptions rOptions ;
      rOptions.userHomePath = options.userHomePath();
      rOptions.userScratchPath = userScratchPath;
      rOptions.startupEnvironmentFilePath = getStartupEnvironmentFilePath();
      rOptions.rEnvironmentDir = boost::bind(rEnvironmentDir);
      rOptions.rHistoryDir = boost::bind(rHistoryDir);
      rOptions.alwaysSaveHistory = boost::bind(&UserSettings::alwaysSaveHistory,
                                               &(userSettings()));
      rOptions.rSourcePath = options.coreRSourcePath();
      if (!desktopMode) // ignore r-libs-user in desktop mode
         rOptions.rLibsUser = options.rLibsUser();
      rOptions.rLibsExtra = options.sessionPackagesPath();
      // CRAN repos: user setting first then global server option
      rOptions.rCRANRepos = userSettings().cranMirror().url;
      if (rOptions.rCRANRepos.empty())
         rOptions.rCRANRepos = options.rCRANRepos();
      rOptions.rCompatibleGraphicsEngineVersion =
                              options.rCompatibleGraphicsEngineVersion();
      rOptions.serverMode = serverMode;
      rOptions.autoReloadSource = options.autoReloadSource();
      rOptions.shellEscape = options.rShellEscape();
      rOptions.restoreWorkspace = userSettings().loadRData() ||
                                  !options.initialEnvironmentFileOverride().empty();
      // save action
      int saveAction = userSettings().saveAction();
      if (saveAction == r::session::kSaveActionSave)
         rOptions.saveWorkspace = SA_SAVE;
      else if (saveAction == r::session::kSaveActionNoSave)
         rOptions.saveWorkspace = SA_NOSAVE;
      else
         rOptions.saveWorkspace = SA_SAVEASK;
      
      // r callbacks
      r::session::RCallbacks rCallbacks;
      rCallbacks.init = rInit;
      rCallbacks.consoleRead = rConsoleRead;
      rCallbacks.editFile = rEditFile;
      rCallbacks.showFile = rShowFile;
      rCallbacks.chooseFile = rChooseFile;
      rCallbacks.busy = rBusy;
      rCallbacks.consoleWrite = rConsoleWrite;
      rCallbacks.consoleHistoryReset = rConsoleHistoryReset;
      rCallbacks.locator = rLocator;
      rCallbacks.suspended = rSuspended;
      rCallbacks.resumed = rResumed;
      rCallbacks.quit = rQuit;
      rCallbacks.suicide = rSuicide;
      rCallbacks.cleanup = rCleanup ;
      rCallbacks.browseURL = rBrowseURL;
      rCallbacks.browseFile = rBrowseFile;
      rCallbacks.showHelp = rShowHelp;
      rCallbacks.showMessage = rShowMessage;
      rCallbacks.serialization = rSerialization;
      
      // run r (does not return, terminates process using exit)
      error = r::session::run(rOptions, rCallbacks) ;
      if (error)
          return sessionExitFailure(error, ERROR_LOCATION);
      
      // return success for good form
      return EXIT_SUCCESS;
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // if we got this far we had an unexpected exception
   return EXIT_FAILURE ;
}



