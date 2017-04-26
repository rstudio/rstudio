/*
 * SessionClientInit.hpp
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

#include "SessionClientInit.hpp"
#include "SessionInit.hpp"
#include "SessionSuspend.hpp"
#include "SessionHttpMethods.hpp"
#include "SessionDirs.hpp"

#include "modules/SessionAgreement.hpp"
#include "modules/SessionAuthoring.hpp"
#include "modules/rmarkdown/SessionRMarkdown.hpp"
#include "modules/connections/SessionConnections.hpp"
#include "modules/SessionBreakpoints.hpp"
#include "modules/SessionRAddins.hpp"
#include "modules/SessionErrors.hpp"
#include "modules/SessionFind.hpp"
#include "modules/SessionHTMLPreview.hpp"
#include "modules/SessionLists.hpp"
#include "modules/clang/SessionClang.hpp"
#include "modules/SessionMarkers.hpp"
#include "modules/SessionPlots.hpp"
#include "modules/SessionSVN.hpp"
#include "modules/SessionSource.hpp"
#include "modules/SessionVCS.hpp"
#include "modules/build/SessionBuild.hpp"
#include "modules/environment/SessionEnvironment.hpp"
#include "modules/presentation/SessionPresentation.hpp"

#include <r/session/RSession.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/ROptions.hpp>

#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/system/Environment.hpp>

#include <session/SessionConsoleProcess.hpp>
#include <session/SessionClientEventService.hpp>
#include <session/SessionHttpConnection.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionPackageProvidedExtension.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/projects/SessionProjectSharing.hpp>

#include <session/projects/SessionProjects.hpp>

#include "session-config.h"

using namespace rstudio::core;

extern "C" const char *locale2charset(const char *);

namespace rstudio {
namespace session {
namespace client_init {

void handleClientInit(const boost::function<void()>& initFunction,
                      boost::shared_ptr<HttpConnection> ptrConnection)
{
   // alias options
   Options& options = session::options();
   
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
   FilePath tempDir = rstudio::r::session::utils::tempDir();
   Error error = tempDir.ensureDirectory();
   if (error)
      LOG_ERROR(error);
   sessionInfo["temp_dir"] = tempDir.absolutePath();
   
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
   
   // agreement
   sessionInfo["hasAgreement"] = modules::agreement::hasAgreement();
   sessionInfo["pendingAgreement"] = modules::agreement::pendingAgreement();

   // docs url
   sessionInfo["docsURL"] = session::options().docsURL();

   // get alias to console_actions and get limit
   rstudio::r::session::ConsoleActions& consoleActions = rstudio::r::session::consoleActions();
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

   sessionInfo["markers_state"] = modules::markers::markersStateAsJson();

   sessionInfo["rstudio_version"] = std::string(RSTUDIO_VERSION);

   sessionInfo["ui_prefs"] = userSettings().uiPrefs();

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
   sessionInfo["default_project_dir"] = options.defaultProjectDir();

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
   sessionInfo["is_github_repo"] = modules::git::isGithubRepository();

   // contents of all lists
   sessionInfo["lists"] = modules::lists::allListsAsJson();

   sessionInfo["console_processes"] =
         console_process::processesAsJson();

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

      sessionInfo["build_tools_bookdown_website"] =
                              module_context::isBookdownWebsite();

      FilePath buildTargetDir = projects::projectContext().buildTargetPath();
      if (!buildTargetDir.empty())
      {
         sessionInfo["build_target_dir"] = module_context::createAliasedPath(
                                                                buildTargetDir);
         sessionInfo["has_pkg_src"] = (type == r_util::kBuildTypePackage) &&
                                      buildTargetDir.childPath("src").exists();
         sessionInfo["has_pkg_vig"] =
               (type == r_util::kBuildTypePackage) &&
               buildTargetDir.childPath("vignettes").exists();
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

   sessionInfo["presentation_state"] = modules::presentation::presentationStateAsJson();
   sessionInfo["presentation_commands"] = options.allowPresentationCommands();

   sessionInfo["tutorial_api_available"] = false;
   sessionInfo["tutorial_api_client_origin"] = json::Value();

   sessionInfo["build_state"] = modules::build::buildStateAsJson();
   sessionInfo["devtools_installed"] = module_context::isMinimumDevtoolsInstalled();
   sessionInfo["have_cairo_pdf"] = modules::plots::haveCairoPdf();

   sessionInfo["have_srcref_attribute"] =
         modules::breakpoints::haveSrcrefAttribute();

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

   sessionInfo["environment_state"] = modules::environment::environmentStateAsJson();
   sessionInfo["error_state"] = modules::errors::errorStateAsJson();

   // send whether we should show the user identity
   sessionInfo["show_identity"] =
           (options.programMode() == kSessionProgramModeServer) &&
           options.showUserIdentity();

   sessionInfo["rmarkdown_available"] =
         modules::rmarkdown::rmarkdownPackageAvailable();

   sessionInfo["packrat_available"] =
                     module_context::isRequiredPackratInstalled();


   sessionInfo["knit_params_available"] =
         modules::rmarkdown::knitParamsAvailable();

   sessionInfo["knit_working_dir_available"] = 
         modules::rmarkdown::knitWorkingDirAvailable();

   sessionInfo["clang_available"] = modules::clang::isAvailable();

   // don't show help home until we figure out a sensible heuristic
   // sessionInfo["show_help_home"] = options.showHelpHome();
   sessionInfo["show_help_home"] = false;

   sessionInfo["multi_session"] = options.multiSession();

   json::Object rVersionsJson;
   rVersionsJson["r_version"] = module_context::rVersion();
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

   sessionInfo["drivers_support_licensing"] = options.supportsDriversLicensing();

   module_context::events().onSessionInfo(&sessionInfo);

   // send response  (we always set kEventsPending to false so that the client
   // won't poll for events until it is ready)
   json::JsonRpcResponse jsonRpcResponse ;
   jsonRpcResponse.setField(kEventsPending, "false");
   jsonRpcResponse.setResult(sessionInfo) ;
   ptrConnection->sendJsonRpcResponse(jsonRpcResponse);

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

