/*
 * SessionHttpMethods.hpp
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

#include "SessionHttpMethods.hpp"
#include "SessionConsoleInput.hpp"
#include "SessionMainProcess.hpp"
#include "SessionSuspend.hpp"
#include "SessionClientInit.hpp"
#include "SessionInit.hpp"
#include "SessionUriHandlers.hpp"
#include "SessionDirs.hpp"
#include "SessionRpc.hpp"

#include "session-config.h"

#include <boost/algorithm/string.hpp>
#include <boost/foreach.hpp>

#include <core/gwt/GwtLogHandler.hpp>
#include <core/gwt/GwtFileHandler.hpp>

#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#include <core/text/TemplateFilter.hpp>

#include <r/RExec.hpp>
#include <r/session/RSession.hpp>
#include <r/session/REventLoop.hpp>

#include <session/RVersionSettings.hpp>
#include <session/SessionHttpConnection.hpp>
#include <session/SessionHttpConnectionListener.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionScopes.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace {

core::http::UriHandlerFunction s_defaultUriHandler;

// version of the executable -- this is the legacy version designator. we
// set it to double max so that it always invalidates legacy clients
double s_version = std::numeric_limits<double>::max();

// names of waitForMethod handlers (used to screen out of bkgnd processing)
std::vector<std::string> s_waitForMethodNames;

// url for next session
std::string s_nextSessionUrl;

boost::posix_time::ptime timeoutTimeFromNow()
{
   int timeoutMinutes = options().timeoutMinutes();
   if (timeoutMinutes > 0)
   {
      return boost::posix_time::second_clock::universal_time() +
             boost::posix_time::minutes(options().timeoutMinutes());
   }
   else
   {
      return boost::posix_time::ptime(boost::posix_time::not_a_date_time);
   }
}

void processDesktopGuiEvents()
{
   // keep R gui alive when we are in destkop mode
   if (options().programMode() == kSessionProgramModeDesktop)
   {
      // execute safely since this can call arbitrary R code (and
      // (can also cause jump_to_top if an interrupt is pending)
      Error error = rstudio::r::exec::executeSafely(
                        rstudio::r::session::event_loop::processEvents);
      if (error)
         LOG_ERROR(error);
   }
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
   if (pJsonRpcRequest->clientId != persistentState().activeClientId())
   {
      Error error(json::errc::InvalidClientId, ERROR_LOCATION);
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // check for legacy client version (need to invalidate any client using
   // the old version field)
   if ( (pJsonRpcRequest->version > 0) &&
        (s_version > pJsonRpcRequest->version) )
   {
      Error error(json::errc::InvalidClientVersion, ERROR_LOCATION);
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // check for client version
   if (!pJsonRpcRequest->clientVersion.empty() &&
       http_methods::clientVersion() != pJsonRpcRequest->clientVersion)
   {
      Error error(json::errc::InvalidClientVersion, ERROR_LOCATION);
      ptrConnection->sendJsonRpcError(error);
      return false;
   }

   // got through all of the validation, return true
   return true;
}

void endHandleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                         http_methods::ConnectionType connectionType,
                         core::http::Response* pResponse)
{
   ptrConnection->sendResponse(*pResponse);
   if (!console_input::executing())
      module_context::events().onDetectChanges(module_context::ChangeSourceURI);
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

Error startHttpConnectionListener()
{
   initializeHttpConnectionListener();
   return httpConnectionListener().start();
}

bool isTimedOut(const boost::posix_time::ptime& timeoutTime)
{
   using namespace boost::posix_time;

   // never time out in desktop mode
   if (options().programMode() == kSessionProgramModeDesktop)
      return false;

   // check for an client disconnection based timeout
   int disconnectedTimeoutMinutes = options().disconnectedTimeoutMinutes();
   if (disconnectedTimeoutMinutes > 0)
   {
      ptime lastEventConnection =
         httpConnectionListener().eventsConnectionQueue().lastConnectionTime();
      if (!lastEventConnection.is_not_a_date_time())
      {
         if ( (lastEventConnection + minutes(disconnectedTimeoutMinutes)
               < second_clock::universal_time()) )
         {
            return true;
         }
      }
   }

   // check for a foreground inactivity based timeout
   if (timeoutTime.is_not_a_date_time())
      return false;
   else
      return second_clock::universal_time() > timeoutTime;
}

bool isWaitForMethodUri(const std::string& uri)
{
   BOOST_FOREACH(const std::string& methodName, s_waitForMethodNames)
   {
      if (isMethod(uri, methodName))
         return true;
   }

   return false;
}

bool isJsonRpcRequest(boost::shared_ptr<HttpConnection> ptrConnection)
{
   return boost::algorithm::starts_with(ptrConnection->request().uri(),
                                        "/rpc/");
}

void polledEventHandler()
{
   // if R is getting called after a fork this is likely multicore or
   // some other parallel computing package that uses fork. in this
   // case be defensive by shutting down as many things as we can
   // which might cause mischief in the child process
   if (main_process::wasForked())
   {
      // no more polled events
      rstudio::r::session::event_loop::permanentlyDisablePolledEventHandler();

      // done
      return;
   }

   // static lastPerformed value used for throttling
   using namespace boost::posix_time;
   static ptime s_lastPerformed;
   if (s_lastPerformed.is_not_a_date_time())
      s_lastPerformed = microsec_clock::universal_time();

   // throttle to no more than once every 50ms
   static time_duration s_intervalMs = milliseconds(50);
   if (microsec_clock::universal_time() <= (s_lastPerformed + s_intervalMs))
      return;

   // notify modules
   module_context::onBackgroundProcessing(false);

   // set last performed (should be set after calling onBackgroundProcessing so
   // that long running background processing handlers can't overflow the 50ms
   // interval between background processing invocations)
   s_lastPerformed = microsec_clock::universal_time();

   // check for a pending connections only while R is processing
   // (otherwise we'll handle them directly in waitForMethod)
   if (console_input::executing())
   {
      // check the uri of the next connection
      std::string nextConnectionUri =
       httpConnectionListener().mainConnectionQueue().peekNextConnectionUri();

      // if the uri is empty or if it one of our special waitForMethod calls
      // then bails so that the waitForMethod logic can handle it
      if (nextConnectionUri.empty() || isWaitForMethodUri(nextConnectionUri))
         return;

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
            client_init::handleClientInit(
                  boost::bind(enqueClientEvent, busyEvent), ptrConnection);
         }
         else
         {
            handleConnection(ptrConnection, http_methods::BackgroundConnection);
         }
      }
   }
}

bool registeredWaitForMethod(const std::string& method,
                             const ClientEvent& event,
                             core::json::JsonRpcRequest* pRequest)
{
   // enque the event which notifies the client we want input
   module_context::enqueClientEvent(event);

   // wait for method
   return http_methods::waitForMethod(method,
                        boost::bind(http_methods::waitForMethodInitFunction, 
                                    event),
                        suspend::disallowSuspend,
                        pRequest);
}

} // anonymous namespace

namespace module_context
{
module_context::WaitForMethodFunction registerWaitForMethod(
      const std::string& methodName)
{
   s_waitForMethodNames.push_back(methodName);
   return boost::bind(registeredWaitForMethod, methodName, _2, _1);
}

} // namespace module_context

namespace http_methods {

// client version -- this is determined by the git revision hash. the client
// and the server can diverge if a new version of the server was installed
// underneath a previously rendered client. if versions diverge then a reload
// of the client is forced
std::string clientVersion()
{
   // never return a version in desktop mode
   if (options().programMode() == kSessionProgramModeDesktop)
      return std::string();

   // never return a version in standalone mode
   if (options().standalone())
      return std::string();

   // clientVersion is the git revision hash
   return RSTUDIO_GIT_REVISION_HASH;
}

void waitForMethodInitFunction(const ClientEvent& initEvent)
{
   module_context::enqueClientEvent(initEvent);

   if (console_input::executing())
   {
      ClientEvent busyEvent(client_events::kBusy, true);
      module_context::enqueClientEvent(busyEvent);
   }
   else
   {
      console_input::reissueLastConsolePrompt();
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
   if (main_process::wasForked())
   {
      LOG_ERROR_MESSAGE("Waiting for method " + method + " after fork");
      return false;
   }

   // establish timeouts
   boost::posix_time::ptime timeoutTime = timeoutTimeFromNow();
   boost::posix_time::time_duration connectionQueueTimeout =
                                   boost::posix_time::milliseconds(50);

   // wait until we get the method we are looking for
   while(true)
   {
      // suspend if necessary (does not return if a suspend occurs)
      suspend::suspendIfRequested(allowSuspend);

      // check for timeout
      if ( isTimedOut(timeoutTime) )
      {
         if (allowSuspend())
         {
            // note that we timed out
            suspend::setSuspendedFromTimeout(true);

            // attempt to suspend (does not return if it succeeds)
            if (!suspend::suspendSession(false))
            {
               // reset timeout flag
               suspend::setSuspendedFromTimeout(false);

               // if it fails then reset the timeout timer so we don't keep
               // hammering away on the failure case
               timeoutTime = timeoutTimeFromNow();
            }
         }
      }

      // if we have at least one async process running then this counts
      // as "activity" and resets the timeout timer
      if (main_process::haveActiveChildren())
         timeoutTime = timeoutTimeFromNow();

      // look for a connection (waiting for the specified interval)
      boost::shared_ptr<HttpConnection> ptrConnection =
          httpConnectionListener().mainConnectionQueue().dequeConnection(
                                            connectionQueueTimeout);


      // perform background processing (true for isIdle)
      module_context::onBackgroundProcessing(true);

      // process pending events in desktop mode
      processDesktopGuiEvents();

      if (ptrConnection)
      {
         // check for client_init
         if ( isMethod(ptrConnection, kClientInit) )
         {
            client_init::handleClientInit(initFunction, ptrConnection);
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
               init::ensureSessionInitialized();

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
         if (!rstudio::r::session::event_loop::polledEventHandlerInitialized())
            rstudio::r::session::event_loop::initializePolledEventHandler(
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

void handleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                      ConnectionType connectionType)
{
   // check for a uri handler registered by a module
   const core::http::Request& request = ptrConnection->request();
   std::string uri = request.uri();
   core::http::UriAsyncHandlerFunction uriHandler = 
     uri_handlers::handlers().handlerFor(uri);

   if (uriHandler) // uri handler
   {
      // r code may execute - ensure session is initialized
      init::ensureSessionInitialized();

      uriHandler(request, boost::bind(endHandleConnection,
                                      ptrConnection,
                                      connectionType,
                                      _1));
   }
   else if (isJsonRpcRequest(ptrConnection)) // check for json-rpc
   {
      // r code may execute - ensure session is initialized
      init::ensureSessionInitialized();

      // attempt to parse & validate
      json::JsonRpcRequest jsonRpcRequest;
      if (parseAndValidateJsonRpcConnection(ptrConnection, &jsonRpcRequest))
      {
         // quit_session: exit process
         if (jsonRpcRequest.method == kQuitSession)
         {
#ifdef _WIN32
            // if we are on windows then we can't quit while the browser
            // context is active
            if (rstudio::r::session::browserContextActive())
            {
               module_context::consoleWriteError(
                        "Error: unable to quit when browser is active\n");
               json::JsonRpcResponse response;
               response.setResult(false);
               ptrConnection->sendJsonRpcResponse(response);
               return;
            }
#endif

            // see whether we should save the workspace
            bool saveWorkspace = true;
            std::string switchToProject, hostPageUrl;
            json::Value switchToVersionJson;
            Error error = json::readParams(jsonRpcRequest.params,
                                           &saveWorkspace,
                                           &switchToProject,
                                           &switchToVersionJson,
                                           &hostPageUrl) ;
            if (error)
               LOG_ERROR(error);
            
            // note switch to project
            if (!switchToProject.empty())
            {
               // if we don't have a project file then create it (can
               // occur when e.g. opening a project from a directory for
               // which we don't yet have a .Rproj file)
               if (switchToProject != kProjectNone)
               {
                  FilePath projFile = module_context::resolveAliasedPath(switchToProject);
                  if (projFile.parent().exists() && !projFile.exists())
                  {
                     Error error = r_util::writeProjectFile(
                              projFile,
                              projects::ProjectContext::buildDefaults(),
                              projects::ProjectContext::defaultConfig());
                     if (error)
                        LOG_ERROR(error);
                  }
               }

               if (options().switchProjectsWithUrl())
               {
                  using namespace module_context;

                  std::string projDir;
                  r_util::SessionScope scope;
                  if (switchToProject == kProjectNone)
                  {
                     scope = r_util::SessionScope::projectNone(
                                          options().sessionScope().id());

                     // update the project and working dir
                     activeSession().setProject(kProjectNone);
                     activeSession().setWorkingDir(
                       createAliasedPath(dirs::getDefaultWorkingDirectory()));
                  }
                  else
                  {
                     // extract the directory (aliased)
                     using namespace module_context;
                     FilePath projFile = module_context::resolveAliasedPath(switchToProject);
                     projDir = createAliasedPath(projFile.parent());
                     scope = r_util::SessionScope::fromProject(
                              projDir,
                              options().sessionScope().id(),
                              filePathToProjectId(options().userScratchPath(),
                                   FilePath(options().getOverlayOption(
                                               kSessionSharedStoragePath))));

                     // update the project and working dir
                     activeSession().setProject(projDir);
                     activeSession().setWorkingDir(projDir);
                  }

                  // set next session url
                  s_nextSessionUrl = r_util::createSessionUrl(hostPageUrl,
                                                              scope);
               }
               else
               {
                  projects::ProjectsSettings(options().userScratchPath()).
                                    setSwitchToProjectPath(switchToProject);
               }

               // note switch to R version if requested
               if (json::isType<json::Object>(switchToVersionJson))
               {
                  using namespace module_context;
                  std::string version, rHome;
                  Error error = json::readObject(
                                            switchToVersionJson.get_obj(),
                                            "version", &version,
                                            "r_home", &rHome);
                  if (!error)
                  {
                     // set version for active session
                     activeSession().setRVersion(version, rHome);

                     // if we had a project directory as well then
                     // set it's version (this is necessary because
                     // project versions override session versions)
                     if (switchToProject != kProjectNone)
                     {
                        FilePath projFile = resolveAliasedPath(switchToProject);
                        std::string projDir = createAliasedPath(projFile.parent());
                        RVersionSettings verSettings(
                                            options().userScratchPath(),
                                            FilePath(options().getOverlayOption(
                                                  kSessionSharedStoragePath)));
                        verSettings.setProjectLastRVersion(projDir,
                                                           version,
                                                           rHome);
                     }
                 }
                 else
                    LOG_ERROR(error);
               }
            }

            // exit status
            int status = switchToProject.empty() ? EXIT_SUCCESS : EX_CONTINUE;

            // acknowledge request & quit session
            json::JsonRpcResponse response;
            response.setResult(true);
            ptrConnection->sendJsonRpcResponse(response);
            rstudio::r::session::quit(saveWorkspace, status); // does not return
         }
         else if (jsonRpcRequest.method == kSuspendSession)
         {
            // check for force
            bool force = true;
            Error error = json::readParams(jsonRpcRequest.params, &force);
            if (error)
               LOG_ERROR(error);

            // acknowledge request and set flags to suspend session
            ptrConnection->sendJsonRpcResponse();
            if (force)
               suspend::handleUSR2(0);
            else
               suspend::handleUSR1(0);
         }

         // interrupt
         else if ( jsonRpcRequest.method == kInterrupt )
         {
            // Discard any buffered input
            console_input::clearConsoleInputBuffer();

            // aknowledge request
            ptrConnection->sendJsonRpcResponse();

            // only accept interrupts while R is processing input
            if (console_input::executing())
               rstudio::r::exec::setInterruptsPending(true);
         }

         // other rpc method, handle it
         else
         {
            jsonRpcRequest.isBackgroundConnection =
                  (connectionType == BackgroundConnection);
            rpc::handleRpcRequest(jsonRpcRequest, ptrConnection, connectionType);
         }
      }
   }
   else if (s_defaultUriHandler)
   {
      core::http::Response response;
       s_defaultUriHandler(request, &response);
       ptrConnection->sendResponse(response);
   }
   else
   {
      core::http::Response response;
      response.setNotFoundError(request.uri());
      ptrConnection->sendResponse(response);
   }
}

WaitResult startHttpConnectionListenerWithTimeout()
{
   Error error = startHttpConnectionListener();

   // When the rsession restarts, it may take a few ms for the port to become
   // available; therefore, retry connection, but only for address_in_use error
   if (!error)
       return WaitResult(WaitSuccess, Success());
   else if (error.code() != boost::system::errc::address_in_use)
      return WaitResult(WaitError, error);
   else
      return WaitResult(WaitContinue, error);
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
   FilePath wwwPath(options.wwwLocalPath());
   FilePath progressPagePath = wwwPath.complete("progress.htm");
   module_context::registerUriHandler(
         "/progress",
          boost::bind(text::handleTemplateRequest, progressPagePath, _1, _2));

   // initialize gwt symbol maps
   gwt::initializeSymbolMaps(options.wwwSymbolMapsPath());

   // set default handler
   s_defaultUriHandler = gwt::fileHandlerFunction(options.wwwLocalPath(), "/");
}

std::string nextSessionUrl()
{
   return s_nextSessionUrl;
}

} // namespace http_methods
} // namespace session
} // namespace rstudio

