/*
 * SessionMain.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include <core/FilePath.hpp>
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
#include <core/gwt/GwtLogHandler.hpp>
#include <core/gwt/GwtFileHandler.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>
#include <core/system/ParentProcessMonitor.hpp>
#include <core/system/FileMonitor.hpp>
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

extern "C" const char *locale2charset(const char *);

#include <monitor/MonitorClient.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionContentUrls.hpp>

#include "SessionAddins.hpp"

#include "SessionModuleContextInternal.hpp"

#include "SessionClientEventQueue.hpp"
#include "SessionClientEventService.hpp"

#include "modules/SessionAgreement.hpp"
#include "modules/SessionAskPass.hpp"
#include "modules/SessionAuthoring.hpp"
#include "modules/SessionHTMLPreview.hpp"
#include "modules/SessionCodeSearch.hpp"
#include "modules/SessionConsole.hpp"
#include "modules/SessionConsoleProcess.hpp"
#include "modules/SessionCrypto.hpp"
#include "modules/SessionFiles.hpp"
#include "modules/SessionFind.hpp"
#include "modules/SessionDirty.hpp"
#include "modules/SessionWorkbench.hpp"
#include "modules/SessionHelp.hpp"
#include "modules/SessionPlots.hpp"
#include "modules/SessionPath.hpp"
#include "modules/SessionPackages.hpp"
#include "modules/SessionRPubs.hpp"
#include "modules/SessionSpelling.hpp"
#include "modules/SessionSource.hpp"
#include "modules/SessionVCS.hpp"
#include "modules/SessionHistory.hpp"
#include "modules/SessionLimits.hpp"
#include "modules/SessionLists.hpp"
#include "modules/build/SessionBuild.hpp"
#include "modules/data/SessionData.hpp"
#include "modules/environment/SessionEnvironment.hpp"
#include "modules/presentation/SessionPresentation.hpp"

#include "modules/SessionGit.hpp"
#include "modules/SessionSVN.hpp"

#include <session/projects/SessionProjects.hpp>
#include "projects/SessionProjectsInternal.hpp"

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
core::json::JsonRpcAsyncMethods s_jsonRpcMethods;
   
// R browseUrl handlers
std::vector<module_context::RBrowseUrlHandler> s_rBrowseUrlHandlers;
   
// R browseFile handlers
std::vector<module_context::RBrowseFileHandler> s_rBrowseFileHandlers;

// names of waitForMethod handlers (used to screen out of bkgnd processing)
std::vector<std::string> s_waitForMethodNames;

// last prompt we issued
std::string s_lastPrompt;

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
const char * const kHandleUnsavedChangesCompleted = "handle_unsaved_changes_completed";
const char * const kQuitSession = "quit_session" ;   
const char * const kInterrupt = "interrupt";

// convenience function for disallowing suspend (note still doesn't override
// the presence of s_forceSuspend = 1)
bool disallowSuspend() { return false; }
   
// request suspends (cooperative and forced) using interrupts
volatile sig_atomic_t s_suspendRequested = 0;
volatile sig_atomic_t s_forceSuspend = 0;
volatile sig_atomic_t s_forceSuspendInterruptedR = 0;
bool s_suspendedFromTimeout = false;

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
   FilePath installedPath("/var/lib/rstudio-server/installed");
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
      LOG_ERROR_MESSAGE("No value within /var/lib/rstudio-server/installed");
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
   // check for a project
   if (projects::projectContext().hasProject())
      return projects::projectContext().directory();

   // see if there is an override from the environment (perhaps based
   // on a folder drag and drop or other file association)
   FilePath workingDirPath = session::options().initialWorkingDirOverride();
   if (workingDirPath.exists() && workingDirPath.isDirectory())
   {
      return workingDirPath;
   }
   else
   {
      // if not then just return default working dir
      return getDefaultWorkingDirectory();
   }
}

std::string switchToProject(const http::Request& request)
{
   std::string referrer = request.headerValue("referer");
   std::string baseURL, queryString;
   http::URL(referrer).split(&baseURL, &queryString);
   http::Fields fields;
   http::util::parseQueryString(queryString, &fields);
   std::string project = http::util::fieldValue(fields, "project");

   if (!project.empty())
   {
      // resolve project
      FilePath projectPath = module_context::resolveAliasedPath(project);
      if ((projectPath.extensionLowerCase() != ".rproj") &&
          projectPath.isDirectory())
      {
         FilePath discoveredPath = r_util::projectFromDirectory(projectPath);
         if (!discoveredPath.empty())
            projectPath = discoveredPath;
      }
      project = module_context::createAliasedPath(projectPath);

      // check if we're already in this project
      if (projects::projectContext().hasProject())
      {
         std::string currentProject = module_context::createAliasedPath(
                                          projects::projectContext().file());
         if (project != currentProject)
            return project;
         else
            return std::string();
      }
      // no project active so need to switch
      else
      {
         return project;
      }
   }
   // no project in the query string
   else
   {
      return std::string();
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

   // set RSTUDIO_HTTP_REFERER environment variable based on Referer
   if (options.programMode() == kSessionProgramModeServer)
   {
      std::string referer = ptrConnection->request().headerValue("referer");
      core::system::setenv("RSTUDIO_HTTP_REFERER", referer);
   }

   // prepare session info 
   json::Object sessionInfo ;
   sessionInfo["clientId"] = clientId;
   sessionInfo["mode"] = options.programMode();
   
   sessionInfo["userIdentity"] = options.userIdentity();

   // only send log_dir and scratch_dir if we are in desktop mode
   if (options.programMode() == kSessionProgramModeDesktop)
   {
      sessionInfo["log_dir"] = options.userLogPath().absolutePath();
      sessionInfo["scratch_dir"] = options.userScratchPath().absolutePath();
   }

   // temp dir
   FilePath tempDir = r::session::utils::tempDir();
   Error error = tempDir.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   sessionInfo["temp_dir"] = tempDir.absolutePath();
   
   // installed version
   sessionInfo["version"] = installedVersion();
   
   // default prompt
   sessionInfo["prompt"] = r::options::getOption<std::string>("prompt");

   // client state
   json::Object clientStateObject;
   r::session::clientState().currentState(&clientStateObject);
   sessionInfo["client_state"] = clientStateObject;
   
   // source documents
   json::Array jsonDocs;
   error = modules::source::clientInitDocuments(&jsonDocs);
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

   sessionInfo["rnw_weave_types"] = modules::authoring::supportedRnwWeaveTypes();
   sessionInfo["latex_program_types"] = modules::authoring::supportedLatexProgramTypes();
   sessionInfo["tex_capabilities"] = modules::authoring::texCapabilitiesAsJson();
   sessionInfo["compile_pdf_state"] = modules::authoring::compilePdfStateAsJson();

   sessionInfo["html_capabilities"] = modules::html_preview::capabilitiesAsJson();

   sessionInfo["find_in_files_state"] = modules::find::findInFilesStateAsJson();

   sessionInfo["rstudio_version"] = std::string(RSTUDIO_VERSION);

   sessionInfo["ui_prefs"] = userSettings().uiPrefs();
   
   // initial working directory
   std::string initialWorkingDir = module_context::createAliasedPath(
                                                getInitialWorkingDirectory());
   sessionInfo["initial_working_dir"] = initialWorkingDir;

   // active project file
   if (projects::projectContext().hasProject())
   {
      sessionInfo["active_project_file"] = module_context::createAliasedPath(
                              projects::projectContext().file());
      sessionInfo["project_ui_prefs"] = projects::projectContext().uiPrefs();
   }
   else
   {
      sessionInfo["active_project_file"] = json::Value();
      sessionInfo["project_ui_prefs"] = json::Value();
   }

   sessionInfo["system_encoding"] = std::string(::locale2charset(NULL));

   std::vector<std::string> vcsAvailable;
   if (modules::source_control::isGitInstalled())
      vcsAvailable.push_back(modules::git::kVcsId);
   if (modules::source_control::isSvnInstalled())
      vcsAvailable.push_back(modules::svn::kVcsId);
   sessionInfo["vcs_available"] = boost::algorithm::join(vcsAvailable, ",");
   sessionInfo["vcs"] = modules::source_control::activeVCSName();
   sessionInfo["default_ssh_key_dir"] =module_context::createAliasedPath(
                              modules::source_control::defaultSshKeyDir());

   // contents of all lists
   sessionInfo["lists"] = modules::lists::allListsAsJson();

   sessionInfo["console_processes"] =
         session::modules::console_process::processesAsJson();

   // is internal preview supported by the client browser
   std::string userAgent = ptrConnection->request().userAgent();
   sessionInfo["internal_pdf_preview_enabled"] =
               modules::authoring::isPdfViewerSupported(userAgent);

   // send sumatra pdf exe path if we are on windows
#ifdef _WIN32
   sessionInfo["sumatra_pdf_exe_path"] =
               options.sumatraPath().complete("SumatraPDF.exe").absolutePath();
#endif

   // are build tools enabled
   if (projects::projectContext().hasProject())
   {
      std::string type = projects::projectContext().config().buildType;
      sessionInfo["build_tools_type"] = type;

      FilePath buildTargetDir = projects::projectContext().buildTargetPath();
      if (!buildTargetDir.empty())
      {
         sessionInfo["build_target_dir"] = module_context::createAliasedPath(
                                                                buildTargetDir);
         sessionInfo["has_pkg_src"] = (type == r_util::kBuildTypePackage) &&
                                      buildTargetDir.childPath("src").exists();
      }
      else
      {
         sessionInfo["build_target_dir"] = json::Value();
         sessionInfo["has_pkg_src"] = false;
      }

   }
   else
   {
      sessionInfo["build_tools_type"] = r_util::kBuildTypeNone;
      sessionInfo["build_target_dir"] = json::Value();
      sessionInfo["has_pkg_src"] = false;
   }

   sessionInfo["presentation_state"] = modules::presentation::presentationStateAsJson();

   sessionInfo["show_environment_tab"] = userSettings().showEnvironmentTab();

   sessionInfo["build_state"] = modules::build::buildStateAsJson();
   sessionInfo["devtools_installed"] = module_context::isPackageInstalled(
                                                                  "devtools");

   sessionInfo["have_rcpp_attributes"] = module_context::haveRcppAttributes();

   sessionInfo["have_cairo_pdf"] = modules::plots::haveCairoPdf();

   // console history -- we do this at the end because
   // restoreBuildRestartContext may have reset it
   json::Array historyArray;
   r::session::consoleHistory().asJson(&historyArray);
   sessionInfo["console_history"] = historyArray;
   sessionInfo["console_history_capacity"] =
                              r::session::consoleHistory().capacity();

   sessionInfo["allow_vcs_exe_edit"] = options.allowVcsExecutableEdit();
   sessionInfo["allow_cran_repos_edit"] = options.allowCRANReposEdit();
   sessionInfo["allow_vcs"] = options.allowVcs();
   sessionInfo["allow_pkg_install"] = options.allowPackageInstallation();
   sessionInfo["allow_shell"] = options.allowShell();
   sessionInfo["allow_file_download"] = options.allowFileDownloads();
   sessionInfo["allow_remove_public_folder"] = options.allowRemovePublicFolder();

   // check whether a switch project is required
   sessionInfo["switch_to_project"] = switchToProject(ptrConnection->request());

   sessionInfo["environment_state"] = modules::environment::environmentStateAsJson();

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

void endHandleRpcRequestDirect(boost::shared_ptr<HttpConnection> ptrConnection,
                         boost::posix_time::ptime executeStartTime,
                         const core::Error& executeError,
                         json::JsonRpcResponse* pJsonRpcResponse)
{
   // return error or result then continue waiting for requests
   if (executeError)
   {
      ptrConnection->sendJsonRpcError(executeError);
   }
   else
   {
      // allow modules to detect changes after rpc calls
      if (!pJsonRpcResponse->suppressDetectChanges())
         detectChanges(module_context::ChangeSourceRPC);

      // are there (or will there likely be) events pending?
      // (if not then notify the client)
      if ( !clientEventQueue().eventAddedSince(executeStartTime) &&
           !pJsonRpcResponse->hasAfterResponse() )
      {
         pJsonRpcResponse->setField(kEventsPending, "false");
      }

      // send the response
      ptrConnection->sendJsonRpcResponse(*pJsonRpcResponse);

      // run after response if we have one (then detect changes again)
      if (pJsonRpcResponse->hasAfterResponse())
      {
         pJsonRpcResponse->runAfterResponse();
         if (!pJsonRpcResponse->suppressDetectChanges())
            detectChanges(module_context::ChangeSourceRPC);
      }
   }
}

void endHandleRpcRequestIndirect(
      const std::string& asyncHandle,
      const core::Error& executeError,
      json::JsonRpcResponse* pJsonRpcResponse)
{
   json::JsonRpcResponse temp;
   json::JsonRpcResponse& jsonRpcResponse =
                                 pJsonRpcResponse ? *pJsonRpcResponse : temp;

   BOOST_ASSERT(!jsonRpcResponse.hasAfterResponse());
   if (executeError)
   {
      jsonRpcResponse.setError(executeError);
   }
   json::Object value;
   value["handle"] = asyncHandle;
   value["response"] = jsonRpcResponse.getRawResponse();
   ClientEvent evt(client_events::kAsyncCompletion, value);
   module_context::enqueClientEvent(evt);
}

void handleRpcRequest(const core::json::JsonRpcRequest& request,
                      boost::shared_ptr<HttpConnection> ptrConnection,
                      ConnectionType connectionType)
{
   // record the time just prior to execution of the event
   // (so we can determine if any events were added during execution)
   using namespace boost::posix_time; 
   ptime executeStartTime = microsec_clock::universal_time();
   
   // execute the method
   json::JsonRpcAsyncMethods::const_iterator it =
                                     s_jsonRpcMethods.find(request.method);
   if (it != s_jsonRpcMethods.end())
   {
      std::pair<bool, json::JsonRpcAsyncFunction> reg = it->second;
      json::JsonRpcAsyncFunction handlerFunction = reg.second;

      if (reg.first)
      {
         // direct return
         handlerFunction(request,
                         boost::bind(endHandleRpcRequestDirect,
                                     ptrConnection,
                                     executeStartTime,
                                     _1,
                                     _2));
      }
      else
      {
         // indirect return (asyncHandle style)
         std::string handle = core::system::generateUuid(true);
         json::JsonRpcResponse response;
         response.setAsyncHandle(handle);
         response.setField(kEventsPending, "false");
         ptrConnection->sendJsonRpcResponse(response);

         handlerFunction(request,
                         boost::bind(endHandleRpcRequestIndirect,
                                     handle,
                                     _1,
                                     _2));
      }
   }
   else
   {
      Error executeError = Error(json::errc::MethodNotFound, ERROR_LOCATION);
      executeError.addProperty("method", request.method);

      // we need to know about these because they represent unexpected
      // application states
      LOG_ERROR(executeError);

      endHandleRpcRequestDirect(ptrConnection, executeStartTime, executeError, NULL);
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

bool isWaitForMethodUri(const std::string& uri)
{
   BOOST_FOREACH(const std::string& methodName, s_waitForMethodNames)
   {
      if (isMethod(uri, methodName))
         return true;
   }

   return false;
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

void endHandleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                         ConnectionType connectionType,
                         http::Response* pResponse)
{
   ptrConnection->sendResponse(*pResponse);
   if (!s_rProcessingInput)
      detectChanges(module_context::ChangeSourceURI);
}

void handleConnection(boost::shared_ptr<HttpConnection> ptrConnection,
                      ConnectionType connectionType)
{
   // check for a uri handler registered by a module
   const http::Request& request = ptrConnection->request();
   std::string uri = request.uri();
   http::UriAsyncHandlerFunction uriHandler = s_uriHandlers.handlerFor(uri);

   if (uriHandler) // uri handler
   {
      // r code may execute - ensure session is initialized
      ensureSessionInitialized();

      uriHandler(request, boost::bind(endHandleConnection,
                                      ptrConnection,
                                      connectionType,
                                      _1));
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
            std::string switchToProject;
            Error error = json::readParams(jsonRpcRequest.params,
                                           &saveWorkspace,
                                           &switchToProject) ;
            if (error)
               LOG_ERROR(error);

            // note switch to project
            if (!switchToProject.empty())
            {
               session::projects::projectContext().setNextSessionProject(
                                                                  switchToProject);
            }

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
            jsonRpcRequest.isBackgroundConnection =
                  (connectionType == BackgroundConnection);
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

}

void atForkParent()
{
   if (boost::this_thread::get_id() != s_mainThreadId)
      return;

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
   if (s_rProcessingInput)
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

bool haveRunningChildren()
{
   return module_context::processSupervisor().hasRunningChildren() ||
          modules::authoring::hasRunningChildren();
}

bool canSuspend(const std::string& prompt)
{
   return !haveRunningChildren() && r::session::isSuspendable(prompt);
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

   // establish timeouts
   boost::posix_time::ptime timeoutTime = timeoutTimeFromNow();
   boost::posix_time::time_duration connectionQueueTimeout =
                                   boost::posix_time::milliseconds(50);

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
            // note that we timed out
            s_suspendedFromTimeout = true;

            // attempt to suspend (does not return if it succeeds)
            if ( !suspendSession(false) )
            {
               // reset timeout flag
               s_suspendedFromTimeout = false;

               // if it fails then reset the timeout timer so we don't keep
               // hammering away on the failure case
               timeoutTime = timeoutTimeFromNow();
            }
         }
      }

      // if we have at least one async process running then this counts
      // as "activity" and resets the timeout timer
      if(haveRunningChildren())
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

// forward declare convenience wait for method init function which
// enques the specified event and then issues either the last console
// prompt or a busy event
void waitForMethodInitFunction(const ClientEvent& initEvent);

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


void doSuspendForRestart(const r::session::RSuspendOptions& options)
{
   module_context::consoleWriteOutput("\nRestarting R session...\n\n");

   r::session::suspendForRestart(options);
}

Error suspendForRestart(const core::json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   r::session::RSuspendOptions options;
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

Error registerSignalHandlers()
{
   using boost::bind;
   using namespace core::system;

   // USR1 and USR2: perform suspend in server mode
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      ExecBlock registerBlock ;
      registerBlock.addFunctions()
         (bind(handleSignal, SigUsr1, handleUSR1))
         (bind(handleSignal, SigUsr2, handleUSR2));
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
      
Error rInit(const r::session::RInitInfo& rInitInfo) 
{
   // save state we need to reference later
   s_rSessionResumed = rInitInfo.resumed;
   
   // record built-in waitForMethod handlers
   s_waitForMethodNames.push_back(kLocatorCompleted);
   s_waitForMethodNames.push_back(kEditCompleted);
   s_waitForMethodNames.push_back(kChooseFileCompleted);
   s_waitForMethodNames.push_back(kHandleUnsavedChangesCompleted);

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

      // modules with c++ implementations
      (modules::spelling::initialize)
      (modules::lists::initialize)
      (modules::path::initialize)
      (modules::limits::initialize)
      (modules::ask_pass::initialize)
      (modules::agreement::initialize)
      (modules::console::initialize)
      (modules::console_process::initialize)
#ifdef RSTUDIO_SERVER
      (modules::crypto::initialize)
#endif
      (modules::files::initialize)
      (modules::find::initialize)
      (modules::environment::initialize)
      (modules::dirty::initialize)
      (modules::workbench::initialize)
      (modules::data::initialize)
      (modules::help::initialize)
      (modules::presentation::initialize)
      (modules::plots::initialize)
      (modules::packages::initialize)
      (modules::rpubs::initialize)
      (modules::source::initialize)
      (modules::source_control::initialize)
      (modules::authoring::initialize)
      (modules::html_preview::initialize)
      (modules::history::initialize)
      (modules::code_search::initialize)
      (modules::build::initialize)

      // workers
      (workers::web_request::initialize)

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
      // in desktop mode we write a success message and execute diagnostics
      if (session::options().programMode() == kSessionProgramModeDesktop)
      {
         std::cout << "Successfully initialized R session."
                   << std::endl << std::endl;
         FilePath diagFile = module_context::sourceDiagnostics();
         if (!diagFile.empty())
         {
            std::cout << "Diagnostics report written to: "
                      << diagFile << std::endl << std::endl;

            Error error = r::exec::RFunction(".rs.showDiagnostics").call();
            if (error)
               LOG_ERROR(error);
         }
      }

      session::options().verifyInstallationHomeDir().removeIfExists();
      ::exit(EXIT_SUCCESS);
   }

   // register all of the json rpc methods implemented in R
   json::JsonRpcMethods rMethods ;
   error = r::json::getRpcMethods(&rMethods);
   if (error)
      return error ;
   BOOST_FOREACH(const json::JsonRpcMethod& method, rMethods)
   {
      s_jsonRpcMethods.insert(json::adaptMethodToAsync(method));
   }

   // add gwt handlers if we are running desktop mode
   if (session::options().programMode() == kSessionProgramModeDesktop)
      registerGwtHandlers();

   // enque abend warning event if necessary.
   using namespace session::client_events;
   if (session::persistentState().hadAbend())
   {
      LOG_ERROR_MESSAGE("session hadabend");

      ClientEvent abendWarningEvent(kAbendWarning);
      session::clientEventQueue().add(abendWarningEvent);
   }

   if (s_printCharsetWarning)
      r::exec::warning("Character set is not UTF-8; please change your locale");

   // propagate console history options
   r::session::consoleHistory().setRemoveDuplicates(
                                 userSettings().removeHistoryDuplicates());


   // register function editor on windows
#ifdef _WIN32
   error = r::exec::RFunction(".rs.registerFunctionEditor").call();
   if (error)
      LOG_ERROR(error);
#endif

   // set flag indicating we had an abnormal end (if this doesn't get
   // unset by the time we launch again then we didn't terminate normally
   // i.e. either the process dying unexpectedly or a call to R_Suicide)
   session::persistentState().setAbend(true);
   
   // setup fork handlers
   setupForkHandlers();

   // success!
   return Success();
}

void rDeferredInit(bool newSession)
{
   module_context::events().onDeferredInit(newSession);

   // fire an event to the client
   ClientEvent event(client_events::kDeferredInitCompleted);
   module_context::enqueClientEvent(event);
}
   
void consolePrompt(const std::string& prompt, bool addToHistory)
{
   // save the last prompt (for re-issuing)
   s_lastPrompt = prompt;

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

   // call prompt hook
   module_context::events().onConsolePrompt(prompt);
}

void reissueLastConsolePrompt()
{
   consolePrompt(s_lastPrompt, false);
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
                        boost::bind(canSuspend, prompt),
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

   // fire onBeforeExecute and onConsoleInput events if this isn't a cancel
   if (!pConsoleInput->cancel)
   {
      module_context::events().onBeforeExecute();
      module_context::events().onConsoleInput(pConsoleInput->text);
   }

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
   ClientEvent editEvent = session::showEditorEvent(fileContents, true, false);
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
   if (s_wasForked)
      return;

   ClientEvent busyEvent(kBusy, busy);
   session::clientEventQueue().add(busyEvent);
}
      
void rConsoleWrite(const std::string& output, int otype)   
{
   if (s_wasForked)
      return;

   int event = otype == 1 ? kConsoleWriteError : kConsoleWriteOutput;
   ClientEvent writeEvent(event, output);
   session::clientEventQueue().add(writeEvent);

   // fire event
   module_context::events().onConsoleOutput(
                  otype == 1 ? module_context::ConsoleOutputError :
                               module_context::ConsoleOutputNormal,
                  output);

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
   
void rSuspended(const r::session::RSuspendOptions& options)
{
   // log to monitor
   using namespace monitor;
   std::string data;
   if (s_suspendedFromTimeout)
      data = safe_convert::numberToString(session::options().timeoutMinutes());
   client().logEvent(Event(kSessionScope, kSessionSuspendEvent, data));

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
   bool succeeded = waitForMethod(
                        kHandleUnsavedChangesCompleted,
                        boost::bind(waitForMethodInitFunction, event),
                        disallowSuspend,
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
   if (s_wasForked)
      return;

   // log to monitor
   using namespace monitor;
   client().logEvent(Event(kSessionScope, kSessionQuitEvent));

   // notify modules
   module_context::events().onQuit();

   // enque a quit event
   bool switchProjects =
         !session::projects::projectContext().nextSessionProject().empty();
   ClientEvent quitEvent(kQuit, switchProjects);
   session::clientEventQueue().add(quitEvent);
}
   
// NOTE: this event is never received on windows (because we can't
// override suicide on windows)
void rSuicide(const std::string& message)
{
   if (s_wasForked)
      return;

   // log to monitor
   using namespace monitor;
   client().logEvent(Event(kSessionScope, kSessionSuicideEvent));

   // log the error
   LOG_ERROR_MESSAGE("R SUICIDE: " + message);
   
   // enque suicide event so the client knows
   ClientEvent suicideEvent(kSuicide, message);
   session::clientEventQueue().add(suicideEvent);
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
   if (projects::projectContext().hasProject())
   {
      return projects::projectContext().directory();
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

SA_TYPE saveWorkspaceOption()
{
   // convert from internal type to R type
   int saveAction = module_context::saveWorkspaceAction();
   if (saveAction == r::session::kSaveActionSave)
      return SA_SAVE;
   else if (saveAction == r::session::kSaveActionNoSave)
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
          !session::options().initialEnvironmentFileOverride().empty();
}

FilePath rHistoryDir()
{
   // for projects we always use the project directory
   if (projects::projectContext().hasProject())
   {
      return projects::projectContext().directory();
   }

   // for server we use the default working directory
   else if (session::options().programMode() == kSessionProgramModeServer)
   {
      return getDefaultWorkingDirectory();
   }

   // for desktop we take the current path
   else
   {
      return FilePath::safeCurrentPath(session::options().userHomePath());
   }
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
   FilePath envFile = session::options().initialEnvironmentFileOverride();
   if (!envFile.empty())
      return envFile;
   else
      return rEnvironmentDir().complete(".RData");
}

void waitForMethodInitFunction(const ClientEvent& initEvent)
{
   module_context::enqueClientEvent(initEvent);

   if (s_rProcessingInput)
   {
      ClientEvent busyEvent(client_events::kBusy, true);
      module_context::enqueClientEvent(busyEvent);
   }
   else
   {
      reissueLastConsolePrompt();
   }
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

Error registerAsyncUriHandler(
                         const std::string& name,
                         const http::UriAsyncHandlerFunction& handlerFunction)
{

   s_uriHandlers.add(http::UriHandler(name,
                                      handlerFunction));
   return Success();
}

Error registerUriHandler(const std::string& name,
                         const http::UriHandlerFunction& handlerFunction)
{

   s_uriHandlers.add(http::UriHandler(name,
                                      handlerFunction));
   return Success();
}


Error registerAsyncLocalUriHandler(
                         const std::string& name,
                         const http::UriAsyncHandlerFunction& handlerFunction)
{
   s_uriHandlers.add(http::UriHandler(kLocalUriLocationPrefix + name,
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


Error registerAsyncRpcMethod(const std::string& name,
                             const core::json::JsonRpcAsyncFunction& function)
{
   s_jsonRpcMethods.insert(
         std::make_pair(name, std::make_pair(false, function)));
   return Success();
}

Error registerRpcMethod(const std::string& name,
                        const core::json::JsonRpcFunction& function)
{
   s_jsonRpcMethods.insert(
         std::make_pair(name,
                        std::make_pair(true, json::adaptToAsync(function))));
   return Success();
}

bool rSessionResumed()
{
   return s_rSessionResumed;
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
         return r::session::kSaveActionSave;
      case r_util::NoValue:
         return r::session::kSaveActionNoSave;
      case r_util::AskValue:
         return r::session::kSaveActionAsk;
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
   r::session::setSaveAction(saveWorkspaceOption());
}


namespace {

bool registeredWaitForMethod(const std::string& method,
                             const ClientEvent& event,
                             core::json::JsonRpcRequest* pRequest)
{
   // enque the event which notifies the client we want input
   module_context::enqueClientEvent(event);

   // wait for method
   return waitForMethod(method,
                        boost::bind(waitForMethodInitFunction, event),
                        disallowSuspend,
                        pRequest);
}

} // anonymous namepace

WaitForMethodFunction registerWaitForMethod(const std::string& methodName)
{
   s_waitForMethodNames.push_back(methodName);
   return boost::bind(registeredWaitForMethod, methodName, _2, _1);
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
   std::string name = ctypeEnvName();
   std::string ctype = core::system::getenv(name);

   if (boost::regex_search(ctype, boost::regex("UTF-8$")))
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
      s_mainThreadId = boost::this_thread::get_id();

      // determine character set
      s_printCharsetWarning = !ensureUtf8Charset();

      // read program options
      Options& options = session::options();
      ProgramStatus status = options.read(argc, argv) ;
      if (status.exit())
         return status.exitCode() ;

      // initialize monitor
      monitor::initializeMonitorClient(kMonitorSocketPath,
                                       options.monitorSharedSecret());

      // register monitor log writer
      core::system::addLogWriter(monitor::client().createLogWriter(
                                                options.programIdentity()));

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

      // set version
      s_version = installedVersion();

      // set the rstudio environment variable so code can check for
      // whether rstudio is running
      core::system::setenv("RSTUDIO", "1");

      // set the rstudio user identity environment variable (can differ from
      // username in debug configurations). this is provided so that 
      // rpostback knows what local stream to connect back to
      core::system::setenv(kRStudioUserIdentity, options.userIdentity());
      if (desktopMode)
      {
         // do the same for port number, for rpostback in rdesktop configs
         core::system::setenv(kRSessionPortNumber, options.wwwPort());
      }
           
      // ensure we aren't being started as a low (priviliged) account
      if (serverMode &&
          core::system::currentUserIsPrivilleged(options.minimumUserId()))
      {
         Error error = systemError(boost::system::errc::permission_denied,
                                   ERROR_LOCATION);
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
      session::initializeClientEventQueue();

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
      error = session::persistentState().initialize();
      if (error)
         return sessionExitFailure(error, ERROR_LOCATION) ;

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

      // we've gotten through startup so let's log a start event
      using namespace monitor;
      client().logEvent(Event(kSessionScope, kSessionStartEvent));


      // install home and doc dir overrides if requested (for debugger mode)
      if (!options.rHomeDirOverride().empty())
         core::system::setenv("R_HOME", options.rHomeDirOverride());
      if (!options.rDocDirOverride().empty())
         core::system::setenv("R_DOC_DIR", options.rDocDirOverride());

      // r options
      r::session::ROptions rOptions ;
      rOptions.userHomePath = options.userHomePath();
      rOptions.userScratchPath = userScratchPath;
      rOptions.scopedScratchPath = module_context::scopedScratchPath();
      rOptions.logPath = options.userLogPath();
      rOptions.sessionPort = options.wwwPort();
      rOptions.startupEnvironmentFilePath = getStartupEnvironmentFilePath();
      rOptions.persistentState = boost::bind(&PersistentState::settings,
                                             &(persistentState()));
      rOptions.rEnvironmentDir = boost::bind(rEnvironmentDir);
      rOptions.rHistoryDir = boost::bind(rHistoryDir);
      rOptions.alwaysSaveHistory = boost::bind(alwaysSaveHistoryOption);
      rOptions.rSourcePath = options.coreRSourcePath();
      if (!desktopMode) // ignore r-libs-user in desktop mode
         rOptions.rLibsUser = options.rLibsUser();
      // CRAN repos: user setting first then global server option
      rOptions.rCRANRepos = userSettings().cranMirror().url;
      if (rOptions.rCRANRepos.empty())
         rOptions.rCRANRepos = options.rCRANRepos();
      rOptions.useInternet2 = userSettings().useInternet2();
      rOptions.rCompatibleGraphicsEngineVersion =
                              options.rCompatibleGraphicsEngineVersion();
      rOptions.serverMode = serverMode;
      rOptions.autoReloadSource = options.autoReloadSource();
      rOptions.restoreWorkspace = restoreWorkspaceOption();
      rOptions.saveWorkspace = saveWorkspaceOption();
      rOptions.rProfileOnResume = serverMode &&
                                  userSettings().rProfileOnResume();
      
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
      error = r::session::run(rOptions, rCallbacks) ;
      if (error)
      {
          // this is logically equivilant to R_Suicide
          client().logEvent(Event(kSessionScope, kSessionSuicideEvent));

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



