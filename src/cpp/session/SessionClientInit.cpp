/*
 * SessionClientInit.hpp
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

#include "SessionClientInit.hpp"
#include "SessionInit.hpp"
#include "SessionSuspend.hpp"
#include "SessionHttpMethods.hpp"
#include "SessionDirs.hpp"

#include "modules/SessionAuthoring.hpp"
#include "modules/rmarkdown/SessionRMarkdown.hpp"
#include "modules/rmarkdown/SessionBlogdown.hpp"
#include "modules/rmarkdown/SessionBookdown.hpp"
#include "modules/connections/SessionConnections.hpp"
#include "modules/SessionBreakpoints.hpp"
#include "modules/SessionDependencyList.hpp"
#include "modules/SessionRAddins.hpp"
#include "modules/SessionErrors.hpp"
#include "modules/SessionFind.hpp"
#include "modules/SessionGraphics.hpp"
#include "modules/SessionHTMLPreview.hpp"
#include "modules/SessionLists.hpp"
#include "modules/clang/SessionClang.hpp"
#include "modules/SessionMarkers.hpp"
#include "modules/SessionPlots.hpp"
#include "modules/SessionReticulate.hpp"
#include "modules/SessionSVN.hpp"
#include "modules/SessionSource.hpp"
#include "modules/SessionVCS.hpp"
#include "modules/SessionFonts.hpp"
#include "modules/build/SessionBuild.hpp"
#include "modules/jobs/SessionJobs.hpp"
#include "modules/environment/SessionEnvironment.hpp"
#include "modules/presentation/SessionPresentation.hpp"
#include "modules/overlay/SessionOverlay.hpp"

#include <r/session/RSession.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/ROptions.hpp>

#include <core/CrashHandler.hpp>
#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/Cookie.hpp>
#include <core/http/CSRFToken.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionConsoleProcess.hpp>
#include <session/SessionClientEventService.hpp>
#include <session/SessionHttpConnection.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionPackageProvidedExtension.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/projects/SessionProjectSharing.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

#include <session/projects/SessionProjects.hpp>

#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <server_core/UrlPorts.hpp>
#endif

using namespace rstudio::core;

extern "C" const char *locale2charset(const char *);

namespace rstudio {
namespace session {
namespace client_init {
namespace {

std::string userIdentityDisplay(const http::Request& request)
{
   std::string userIdentity = request.headerValue(kRStudioUserIdentityDisplay);
   if (!userIdentity.empty())
      return userIdentity;
   else
      return session::options().userIdentity();
}

#ifdef RSTUDIO_SERVER
Error makePortTokenCookie(boost::shared_ptr<HttpConnection> ptrConnection, 
      http::Response& response)
{
   // extract the base URL
   json::JsonRpcRequest request;
   Error error = parseJsonRpcRequest(ptrConnection->request().body(), &request);
   if (error)
      return error;
   std::string baseURL;

   error = json::readParams(request.params, &baseURL);
   if (error)
      return error;

   // save the base URL to persistent state (for forming absolute URLs)
   persistentState().setActiveClientUrl(baseURL);

   // generate a new port token
   persistentState().setPortToken(server_core::generateNewPortToken());

   std::string path = ptrConnection->request().rootPath();

   // compute the cookie path; find the first / after the http(s):// preamble. we make the cookie
   // specific to this session's URL since it's possible for different sessions (paths) to use
   // different tokens on the same server. This won't be done if the path was passed by the server
   std::size_t pos = baseURL.find('/', 9);
   if (pos != std::string::npos && path == kRequestDefaultRootPath)
   {
      path = baseURL.substr(pos);
   }

   // create the cookie; don't set an expiry date as this will be a session cookie
   http::Cookie cookie(
            ptrConnection->request(), 
            kPortTokenCookie, 
            persistentState().portToken(), 
            path,
            options().sameSite(),
            true, // HTTP only -- client doesn't get to read this token
            options().useSecureCookies()
         );
   response.addCookie(cookie);

   return Success();
}
#endif

} // anonymous namespace

void handleClientInit(const boost::function<void()>& initFunction,
                      boost::shared_ptr<HttpConnection> ptrConnection)
{
   // alias options
   Options& options = session::options();
   
   // check for valid CSRF headers in server mode 
   if (options.programMode() == kSessionProgramModeServer && 
       !core::http::validateCSRFHeaders(ptrConnection->request()))
   {
      ptrConnection->sendJsonRpcError(Error(json::errc::Unauthorized, ERROR_LOCATION));
      return;
   }

   // calculate initialization parameters
   std::string clientId = persistentState().newActiveClientId();
   bool resumed = suspend::sessionResumed() || init::isSessionInitialized();

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

   if (options.programMode() == kSessionProgramModeServer)
   {
      // set RSTUDIO_HTTP_REFERER environment variable based on Referer
      std::string referer = ptrConnection->request().headerValue("referer");
      core::system::setenv("RSTUDIO_HTTP_REFERER", referer);

      // set RSTUDIO_USER_IDENTITY_DISPLAY environment variable based on
      // header value (complements RSTUDIO_USER_IDENTITY)
      core::system::setenv("RSTUDIO_USER_IDENTITY_DISPLAY", 
            userIdentityDisplay(ptrConnection->request()));
   }

   // prepare session info 
   json::Object sessionInfo;
   sessionInfo["clientId"] = clientId;
   sessionInfo["mode"] = options.programMode();

   // build initialization options for client
   json::Object initOptions;
   initOptions["restore_workspace"] = options.rRestoreWorkspace();
   initOptions["run_rprofile"] = options.rRunRprofile();
   sessionInfo["init_options"] = initOptions;
   
   sessionInfo["userIdentity"] = userIdentityDisplay(ptrConnection->request());
   sessionInfo["systemUsername"] = core::system::username();

   // only send log_dir and scratch_dir if we are in desktop mode
   if (options.programMode() == kSessionProgramModeDesktop)
   {
      sessionInfo["log_dir"] = options.userLogPath().getAbsolutePath();
      sessionInfo["scratch_dir"] = options.userScratchPath().getAbsolutePath();
   }

   // temp dir
   FilePath tempDir = rstudio::r::session::utils::tempDir();
   Error error = tempDir.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   sessionInfo["temp_dir"] = tempDir.getAbsolutePath();

   // R_LIBS_USER
   sessionInfo["r_libs_user"] = module_context::rLibsUser();
   
   // user home path
   sessionInfo["user_home_path"] = session::options().userHomePath().getAbsolutePath();
   
   // installed client version
   sessionInfo["client_version"] = http_methods::clientVersion();
   
   // default prompt
   sessionInfo["prompt"] = rstudio::r::options::getOption<std::string>("prompt");

   // client state
   json::Object clientStateObject;
   rstudio::r::session::clientState().currentState(&clientStateObject);
   sessionInfo["client_state"] = clientStateObject;
   
   // source documents
   json::Array jsonDocs;
   error = modules::source::clientInitDocuments(&jsonDocs);
   if (error)
      LOG_ERROR(error);
   sessionInfo["source_documents"] = jsonDocs;
   
   // docs url
   sessionInfo["docsURL"] = session::options().docsURL();

   // get alias to console_actions and get limit
   rstudio::r::session::ConsoleActions& consoleActions = rstudio::r::session::consoleActions();
   sessionInfo["console_actions_limit"] = consoleActions.capacity();
 
   // check if reticulate's Python session has been initialized
   sessionInfo["python_initialized"] = modules::reticulate::isPythonInitialized();
   
   // propagate RETICULATE_PYTHON if set
   sessionInfo["reticulate_python"] = core::system::getenv("RETICULATE_PYTHON");
   
   // get current console language
   sessionInfo["console_language"] = modules::reticulate::isReplActive() ? "Python" : "R";

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

   sessionInfo["markers_state"] = modules::markers::markersStateAsJson();

   std::string sessionVersion = std::string(RSTUDIO_VERSION);
   sessionInfo["rstudio_version"] = sessionVersion;

   // check to ensure the version of this rsession process matches the version
   // of the rserver that started us - we immediately clear this env var so that
   // it is not persisted across session suspends
   std::string version = core::system::getenv(kRStudioVersion);
   core::system::unsetenv(kRStudioVersion);
   if (!version.empty() && version != sessionVersion)
   {
      module_context::consoleWriteError("Session version " + sessionVersion +
                                        " does not match server version " + version + " - "
                                        "this is an unsupported configuration, and you may "
                                        "experience unexpected issues as a result.\n\n");
   }

   sessionInfo["user_prefs"] = prefs::allPrefLayers();
   sessionInfo["user_state"] = prefs::allStateLayers();

   sessionInfo["have_advanced_step_commands"] =
                        modules::breakpoints::haveAdvancedStepCommands();
   
   // initial working directory
   std::string initialWorkingDir = module_context::createAliasedPath(
                                          dirs::getInitialWorkingDirectory());
   sessionInfo["initial_working_dir"] = initialWorkingDir;
   std::string defaultWorkingDir = module_context::createAliasedPath(
                                          dirs::getDefaultWorkingDirectory());
   sessionInfo["default_working_dir"] = defaultWorkingDir;

   // default project dir
   sessionInfo["default_project_dir"] = options.deprecatedDefaultProjectDir();

   // active project file
   if (projects::projectContext().hasProject())
   {
      sessionInfo["active_project_file"] = module_context::createAliasedPath(
                              projects::projectContext().file());
      sessionInfo["project_ui_prefs"] = projects::projectContext().uiPrefs();
      sessionInfo["project_open_docs"] = projects::projectContext().openDocs();
      sessionInfo["project_supports_sharing"] = 
         projects::projectContext().supportsSharing();
      sessionInfo["project_parent_browseable"] = 
         projects::projectContext().parentBrowseable();
      sessionInfo["project_user_data_directory"] =
       module_context::createAliasedPath(
             dirs::getProjectUserDataDir(ERROR_LOCATION));

   }
   else
   {
      sessionInfo["active_project_file"] = json::Value();
      sessionInfo["project_ui_prefs"] = json::Value();
      sessionInfo["project_open_docs"] = json::Value();
      sessionInfo["project_supports_sharing"] = false;
      sessionInfo["project_owned_by_user"] = false;
      sessionInfo["project_user_data_directory"] = json::Value();
   }

   sessionInfo["system_encoding"] = std::string(::locale2charset(nullptr));

   std::vector<std::string> vcsAvailable;
   if (modules::source_control::isGitInstalled())
      vcsAvailable.push_back(modules::git::kVcsId);
   if (modules::source_control::isSvnInstalled())
      vcsAvailable.push_back(modules::svn::kVcsId);
   sessionInfo["vcs_available"] = boost::algorithm::join(vcsAvailable, ",");
   sessionInfo["vcs"] = modules::source_control::activeVCSName();
   sessionInfo["default_ssh_key_dir"] =module_context::createAliasedPath(
                              modules::source_control::defaultSshKeyDir());
   sessionInfo["is_github_repo"] = modules::git::isGithubRepository();

   // contents of all lists
   sessionInfo["lists"] = modules::lists::allListsAsJson();

   sessionInfo["console_processes"] =
         console_process::processesAsJson(console_process::ClientSerialization);

   // send sumatra pdf exe path if we are on windows
#ifdef _WIN32
   sessionInfo["sumatra_pdf_exe_path"] =
               options.sumatraPath().completePath("SumatraPDF.exe").getAbsolutePath();
#endif

   // are build tools enabled
   if (projects::projectContext().hasProject())
   {
      std::string type = projects::projectContext().config().buildType;
      sessionInfo["build_tools_type"] = type;

      sessionInfo["build_tools_bookdown_website"] =
                              module_context::isBookdownWebsite();

      FilePath buildTargetDir = projects::projectContext().buildTargetPath();
      if (!buildTargetDir.isEmpty())
      {
         sessionInfo["build_target_dir"] = module_context::createAliasedPath(
                                                                buildTargetDir);
         sessionInfo["has_pkg_src"] = (type == r_util::kBuildTypePackage) &&
            buildTargetDir.completeChildPath("src").exists();
         sessionInfo["has_pkg_vig"] =
               (type == r_util::kBuildTypePackage) &&
                  buildTargetDir.completeChildPath("vignettes").exists();
      }
      else
      {
         sessionInfo["build_target_dir"] = json::Value();
         sessionInfo["has_pkg_src"] = false;
         sessionInfo["has_pkg_vig"] = false;
      }

   }
   else
   {
      sessionInfo["build_tools_type"] = r_util::kBuildTypeNone;
      sessionInfo["build_tools_bookdown_website"] = false;
      sessionInfo["build_target_dir"] = json::Value();
      sessionInfo["has_pkg_src"] = false;
      sessionInfo["has_pkg_vig"] = false;
   }

   sessionInfo["blogdown_config"] = modules::rmarkdown::blogdown::blogdownConfig();
   sessionInfo["is_bookdown_project"] = module_context::isBookdownProject();
   sessionInfo["is_distill_project"] = module_context::isDistillProject();
   
   sessionInfo["graphics_backends"] = modules::graphics::supportedBackends();

   sessionInfo["presentation_state"] = modules::presentation::presentationStateAsJson();
   sessionInfo["presentation_commands"] = options.allowPresentationCommands();

   sessionInfo["tutorial_api_available"] = false;
   sessionInfo["tutorial_api_client_origin"] = json::Value();

   sessionInfo["build_state"] = modules::build::buildStateAsJson();
   sessionInfo["devtools_installed"] = module_context::isMinimumDevtoolsInstalled();
   sessionInfo["have_cairo_pdf"] = modules::plots::haveCairoPdf();

   // console history -- we do this at the end because
   // restoreBuildRestartContext may have reset it
   json::Array historyArray;
   rstudio::r::session::consoleHistory().asJson(&historyArray);
   sessionInfo["console_history"] = historyArray;
   sessionInfo["console_history_capacity"] =
                              rstudio::r::session::consoleHistory().capacity();

   sessionInfo["disable_packages"] = module_context::disablePackages();

   sessionInfo["disable_check_for_updates"] =
          !core::system::getenv("RSTUDIO_DISABLE_CHECK_FOR_UPDATES").empty();

   sessionInfo["allow_vcs_exe_edit"] = options.allowVcsExecutableEdit();
   sessionInfo["allow_cran_repos_edit"] = options.allowCRANReposEdit();
   sessionInfo["allow_vcs"] = options.allowVcs();
   sessionInfo["allow_pkg_install"] = options.allowPackageInstallation();
   sessionInfo["allow_shell"] = options.allowShell();
   sessionInfo["allow_terminal_websockets"] = options.allowTerminalWebsockets();
   sessionInfo["allow_file_download"] = options.allowFileDownloads();
   sessionInfo["allow_file_upload"] = options.allowFileUploads();
   sessionInfo["allow_remove_public_folder"] = options.allowRemovePublicFolder();
   sessionInfo["allow_full_ui"] = options.allowFullUI();
   sessionInfo["websocket_ping_interval"] = options.webSocketPingInterval();
   sessionInfo["websocket_connect_timeout"] = options.webSocketConnectTimeout();

   // publishing may be disabled globally or just for external services, and
   // via configuration options or environment variables
   bool allowPublish = options.allowPublish() &&
      core::system::getenv("RSTUDIO_DISABLE_PUBLISH").empty();
   sessionInfo["allow_publish"] = allowPublish;

   sessionInfo["allow_external_publish"] = options.allowRpubsPublish() &&
      options.allowExternalPublish() &&
      core::system::getenv("RSTUDIO_DISABLE_EXTERNAL_PUBLISH").empty() &&
      allowPublish;

   // allow opening shared projects if it's enabled, and if there's shared
   // storage from which we can discover the shared projects
   sessionInfo["allow_open_shared_projects"] = 
         core::system::getenv(kRStudioDisableProjectSharing).empty() &&
         !options.getOverlayOption(kSessionSharedStoragePath).empty();

   sessionInfo["project_sharing_enumerate_server_users"] = true;
   sessionInfo["launcher_session"] = false;

   sessionInfo["environment_state"] = modules::environment::environmentStateAsJson();
   sessionInfo["error_state"] = modules::errors::errorStateAsJson();

   // send whether we should show the user identity
   sessionInfo["show_identity"] =
           (options.programMode() == kSessionProgramModeServer) &&
           options.showUserIdentity();

   sessionInfo["packrat_available"] =
                     module_context::isRequiredPackratInstalled();

   sessionInfo["renv_available"] =
         module_context::isRequiredRenvInstalled();

   // check rmarkdown package presence and capabilities
   sessionInfo["rmarkdown_available"] =
         modules::rmarkdown::rmarkdownPackageAvailable();
   sessionInfo["knit_params_available"] =
         modules::rmarkdown::knitParamsAvailable();
   sessionInfo["knit_working_dir_available"] = 
         modules::rmarkdown::knitWorkingDirAvailable();
   sessionInfo["ppt_available"] = 
         modules::rmarkdown::pptAvailable();

   sessionInfo["clang_available"] = modules::clang::isAvailable();

   // don't show help home until we figure out a sensible heuristic
   // sessionInfo["show_help_home"] = options.showHelpHome();
   sessionInfo["show_help_home"] = false;

   sessionInfo["multi_session"] = options.multiSession();

   json::Object rVersionsJson;
   rVersionsJson["r_version"] = module_context::rVersion();
   rVersionsJson["r_version_label"] = module_context::rVersionLabel();
   rVersionsJson["r_home_dir"] = module_context::rHomeDir();
   sessionInfo["r_versions_info"] = rVersionsJson;

   sessionInfo["show_user_home_page"] = options.showUserHomePage();
   sessionInfo["user_home_page_url"] = json::Value();
   
   sessionInfo["r_addins"] = modules::r_addins::addinRegistryAsJson();
   sessionInfo["package_provided_extensions"] = modules::ppe::indexer().getPayload();

   sessionInfo["connections_enabled"] = modules::connections::connectionsEnabled();
   sessionInfo["activate_connections"] = modules::connections::activateConnections();
   sessionInfo["connection_list"] = modules::connections::connectionsAsJson();
   sessionInfo["active_connections"] = modules::connections::activeConnectionsAsJson();

   sessionInfo["session_id"] = module_context::activeSession().id();
   sessionInfo["session_label"] = module_context::activeSession().label();

   sessionInfo["drivers_support_licensing"] = options.supportsDriversLicensing();

   sessionInfo["quit_child_processes_on_exit"] = options.quitChildProcessesOnExit();
   
   sessionInfo["git_commit_large_file_size"] = options.gitCommitLargeFileSize();

   sessionInfo["default_rsconnect_server"] = options.defaultRSConnectServer();

   sessionInfo["job_state"] = modules::jobs::jobState();

   sessionInfo["launcher_jobs_enabled"] = modules::overlay::launcherJobsFeatureDisplayed();

   json::Object packageDependencies;
   error = modules::dependency_list::getDependencyList(&packageDependencies);
   if (error)
      LOG_ERROR(error);
   sessionInfo["package_dependencies"] = packageDependencies;

   // crash handler settings
   bool canModifyCrashSettings =
         options.programMode() == kSessionProgramModeDesktop &&
         crash_handler::configSource() == crash_handler::ConfigSource::User;
   sessionInfo["crash_handler_settings_modifiable"] = canModifyCrashSettings;

   bool promptForCrashHandlerPermission = canModifyCrashSettings && !crash_handler::hasUserBeenPromptedForPermission();
   sessionInfo["prompt_for_crash_handler_permission"] = promptForCrashHandlerPermission;

   sessionInfo["project_id"] = session::options().sessionScope().project();

   if (session::options().getBoolOverlayOption(kSessionUserLicenseSoftLimitReached))
   {
      sessionInfo["license_message"] =
            "There are more concurrent users of RStudio Server Pro than your license supports. "
            "Please obtain an updated license to continue using the product.";
   }

   // session route for load balanced sessions
   sessionInfo["session_node"] = session::modules::overlay::sessionNode();

   module_context::events().onSessionInfo(&sessionInfo);

   // create response  (we always set kEventsPending to false so that the client
   // won't poll for events until it is ready)
   json::JsonRpcResponse jsonRpcResponse;
   jsonRpcResponse.setField(kEventsPending, "false");
   jsonRpcResponse.setResult(sessionInfo);

   // set response
   core::http::Response response;
   core::json::setJsonRpcResponse(jsonRpcResponse, &response);

#ifdef RSTUDIO_SERVER
   if (options.programMode() == kSessionProgramModeServer)
   {
      Error error = makePortTokenCookie(ptrConnection, response);
      if (error)
      {
         LOG_ERROR(error);
      }
   }
#endif

   ptrConnection->sendResponse(response);

   // complete initialization of session
   init::ensureSessionInitialized();
   
   // notify modules of the client init
   module_context::events().onClientInit();
   
   // call the init function
   initFunction();
}

} // namespace init
} // namespace session
} // namespace rstudio

