/*
 * SessionProjects.cpp
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

#include <session/projects/SessionProjects.hpp>

#include <core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RSessionUtils.hpp>

#include "SessionProjectFirstRun.hpp"
#include "SessionProjectsInternal.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace projects {

namespace {

ProjectContext s_projectContext;


void onSuspend(Settings*)
{
   // on suspend write out current project path as the one to use
   // on resume. we read this back in initalize (rather than in
   // the onResume handler) becuase we need it very early in the
   // processes lifetime and onResume happens too late
   s_projectContext.setNextSessionProject(
                              s_projectContext.file().absolutePath());
}

void onResume(const Settings&) {}

Error getNewProjectContext(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   json::Object contextJson;

   contextJson["rcpp_available"] = module_context::isPackageInstalled("Rcpp");
   contextJson["packrat_available"] =
         module_context::packratContext().available &&
         module_context::canBuildCpp();
   contextJson["working_directory"] = module_context::createAliasedPath(
         r::session::utils::safeCurrentPath());

   pResponse->setResult(contextJson);

   return Success();
}

Error createProject(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // read params
   std::string projectFile;
   json::Value newPackageJson;
   json::Value newShinyAppJson;
   Error error = json::readParams(request.params,
                                  &projectFile,
                                  &newPackageJson,
                                  &newShinyAppJson);
   if (error)
      return error;
   FilePath projectFilePath = module_context::resolveAliasedPath(projectFile);

   if (!newShinyAppJson.is_null())
   {
      // error if the shiny app dir already exists
      FilePath appDir = projectFilePath.parent();
      if (appDir.exists())
         return core::fileExistsError(ERROR_LOCATION);

      // now create it
      Error error = appDir.ensureDirectory();
      if (error)
         return error;

      // copy ui.R and server.R into the project
      const char * const kUI = "ui.R";
      const char * const kServer = "server.R";
      std::string shinyVer;
      if (module_context::isPackageVersionInstalled("shiny", "0.9"))
         shinyVer = "shiny-0.9";
      else
         shinyVer = "shiny";
      FilePath shinyDir = session::options().rResourcesPath().childPath(
                                                     "templates/" + shinyVer);
      error = shinyDir.childPath(kUI).copy(appDir.childPath(kUI));
      if (error)
         LOG_ERROR(error);
      error = shinyDir.childPath(kServer).copy(appDir.childPath(kServer));
      if (error)
         LOG_ERROR(error);

      // add first run actions for the source files
      addFirstRunDoc(projectFilePath, kUI);
      addFirstRunDoc(projectFilePath, kServer);

      // create the project file
      return r_util::writeProjectFile(projectFilePath,
                                      ProjectContext::buildDefaults(),
                                      ProjectContext::defaultConfig());
   }

   // default project
   else
   {
      // create the project directory if necessary
      error = projectFilePath.parent().ensureDirectory();
      if (error)
         return error;

      // create the project file
      if (!projectFilePath.exists())
      {
         return r_util::writeProjectFile(projectFilePath,
                                         ProjectContext::buildDefaults(),
                                         ProjectContext::defaultConfig());
      }
      else
      {
         return Success();
      }
   }
}

json::Object projectConfigJson(const r_util::RProjectConfig& config)
{
   json::Object configJson;
   configJson["version"] = config.version;
   json::Object rVersionJson;
   rVersionJson["number"] = config.rVersion.number;
   rVersionJson["arch"] = config.rVersion.arch;
   configJson["r_version"] = rVersionJson;
   configJson["restore_workspace"] = config.restoreWorkspace;
   configJson["save_workspace"] = config.saveWorkspace;
   configJson["always_save_history"] = config.alwaysSaveHistory;
   configJson["enable_code_indexing"] = config.enableCodeIndexing;
   configJson["use_spaces_for_tab"] = config.useSpacesForTab;
   configJson["num_spaces_for_tab"] = config.numSpacesForTab;
   configJson["auto_append_newline"] = config.autoAppendNewline;
   configJson["strip_trailing_whitespace"] = config.stripTrailingWhitespace;
   configJson["default_encoding"] = config.encoding;
   configJson["default_sweave_engine"] = config.defaultSweaveEngine;
   configJson["default_latex_program"] = config.defaultLatexProgram;
   configJson["root_document"] = config.rootDocument;
   configJson["build_type"] = config.buildType;
   configJson["package_use_devtools"] = config.packageUseDevtools;
   configJson["package_path"] = config.packagePath;
   configJson["package_install_args"] = config.packageInstallArgs;
   configJson["package_build_args"] = config.packageBuildArgs;
   configJson["package_build_binary_args"] = config.packageBuildBinaryArgs;
   configJson["package_check_args"] = config.packageCheckArgs;
   configJson["package_roxygenize"] = config.packageRoxygenize;
   configJson["makefile_path"] = config.makefilePath;
   configJson["custom_script_path"] = config.customScriptPath;
   configJson["tutorial_path"] = config.tutorialPath;
   return configJson;
}

json::Object projectBuildOptionsJson()
{
   RProjectBuildOptions buildOptions;
   Error error = s_projectContext.readBuildOptions(&buildOptions);
   if (error)
      LOG_ERROR(error);
   json::Object buildOptionsJson;
   buildOptionsJson["makefile_args"] = buildOptions.makefileArgs;

   json::Object autoRoxJson;
   autoRoxJson["run_on_check"] = buildOptions.autoRoxygenizeForCheck;
   autoRoxJson["run_on_package_builds"] =
                              buildOptions.autoRoxygenizeForBuildPackage;
   autoRoxJson["run_on_build_and_reload"] =
                              buildOptions.autoRoxygenizeForBuildAndReload;
   buildOptionsJson["auto_roxygenize_options"] = autoRoxJson;
   return buildOptionsJson;
}

json::Object projectVcsOptionsJson()
{
   RProjectVcsOptions vcsOptions;
   Error error = s_projectContext.readVcsOptions(&vcsOptions);
   if (error)
      LOG_ERROR(error);
   json::Object vcsOptionsJson;
   vcsOptionsJson["active_vcs_override"] = vcsOptions.vcsOverride;
   return vcsOptionsJson;
}

json::Object projectVcsContextJson()
{
   module_context::VcsContext vcsContext = module_context::vcsContext(
                                             s_projectContext.directory());

   json::Object contextJson;
   contextJson["detected_vcs"] = vcsContext.detectedVcs;
   json::Array applicableJson;
   BOOST_FOREACH(const std::string& vcs, vcsContext.applicableVcs)
   {
      applicableJson.push_back(vcs);
   }
   contextJson["applicable_vcs"] = applicableJson;

   contextJson["svn_repository_root"] = vcsContext.svnRepositoryRoot;
   contextJson["git_remote_origin_url"] = vcsContext.gitRemoteOriginUrl;

   return contextJson;
}

json::Object projectBuildContextJson()
{
   using namespace module_context;
   json::Object contextJson;
   contextJson["roxygen2_installed"] = isMinimumRoxygenInstalled();
   contextJson["devtools_installed"] = isMinimumDevtoolsInstalled();
   return contextJson;
}

Error readProjectOptions(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   // get project config json
   json::Object configJson = projectConfigJson(s_projectContext.config());

   // create project options json
   json::Object optionsJson;
   optionsJson["config"] = configJson;
   optionsJson["vcs_options"] = projectVcsOptionsJson();
   optionsJson["vcs_context"] = projectVcsContextJson();
   optionsJson["build_options"] = projectBuildOptionsJson();
   optionsJson["build_context"] = projectBuildContextJson();
   optionsJson["packrat_options"] = module_context::packratOptionsAsJson();
   optionsJson["packrat_context"] = module_context::packratContextAsJson();

   pResponse->setResult(optionsJson);
   return Success();
}

void setProjectConfig(const r_util::RProjectConfig& config)
{
   // set it
   s_projectContext.setConfig(config);

   // sync underlying R setting
   module_context::syncRSaveAction();
}

Error rProjectBuildOptionsFromJson(const json::Object& optionsJson,
                                   RProjectBuildOptions* pOptions)
{
   json::Object autoRoxJson;
   Error error = json::readObject(
       optionsJson,
       "makefile_args", &(pOptions->makefileArgs),
       "auto_roxygenize_options", &autoRoxJson);
   if (error)
      return error;

   return json::readObject(
       autoRoxJson,
       "run_on_check", &(pOptions->autoRoxygenizeForCheck),
       "run_on_package_builds", &(pOptions->autoRoxygenizeForBuildPackage),
       "run_on_build_and_reload", &(pOptions->autoRoxygenizeForBuildAndReload));
}

Error rProjectVcsOptionsFromJson(const json::Object& optionsJson,
                                 RProjectVcsOptions* pOptions)
{
   return json::readObject(
         optionsJson,
         "active_vcs_override", &(pOptions->vcsOverride));
}

Error writeProjectOptions(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // get the project config, vcs options, and build options
   json::Object configJson, vcsOptionsJson, buildOptionsJson;
   Error error = json::readObjectParam(request.params, 0,
                                       "config", &configJson,
                                       "vcs_options", &vcsOptionsJson,
                                       "build_options", &buildOptionsJson);
   if (error)
      return error;

   // read the config
   r_util::RProjectConfig config;
   error = json::readObject(
                    configJson,
                    "version", &(config.version),
                    "restore_workspace", &(config.restoreWorkspace),
                    "save_workspace", &(config.saveWorkspace),
                    "always_save_history", &(config.alwaysSaveHistory),
                    "enable_code_indexing", &(config.enableCodeIndexing),
                    "use_spaces_for_tab", &(config.useSpacesForTab),
                    "num_spaces_for_tab", &(config.numSpacesForTab),
                    "default_encoding", &(config.encoding),
                    "default_sweave_engine", &(config.defaultSweaveEngine),
                    "default_latex_program", &(config.defaultLatexProgram),
                    "root_document", &(config.rootDocument));
   if (error)
      return error;

   error = json::readObject(
                    configJson,
                    "auto_append_newline", &(config.autoAppendNewline),
                    "strip_trailing_whitespace", &(config.stripTrailingWhitespace));
   if (error)
      return error;

   error = json::readObject(
                    configJson,
                    "build_type", &(config.buildType),
                    "package_use_devtools", &(config.packageUseDevtools),
                    "package_path", &(config.packagePath),
                    "package_install_args", &(config.packageInstallArgs),
                    "package_build_args", &(config.packageBuildArgs),
                    "package_build_binary_args", &(config.packageBuildBinaryArgs),
                    "package_check_args", &(config.packageCheckArgs),
                    "package_roxygenize", &(config.packageRoxygenize),
                    "makefile_path", &(config.makefilePath),
                    "custom_script_path", &(config.customScriptPath),
                    "tutorial_path", &(config.tutorialPath));
   if (error)
      return error;

   // read the r version info
   json::Object rVersionJson;
   error = json::readObject(configJson, "r_version", &rVersionJson);
   if (error)
      return error;
   error = json::readObject(rVersionJson,
                            "number", &(config.rVersion.number),
                            "arch", &(config.rVersion.arch));
   if (error)
      return error;

   // read the vcs options
   RProjectVcsOptions vcsOptions;
   error = rProjectVcsOptionsFromJson(vcsOptionsJson, &vcsOptions);
   if (error)
      return error;

   // read the buld options
   RProjectBuildOptions buildOptions;
   error = rProjectBuildOptionsFromJson(buildOptionsJson, &buildOptions);
   if (error)
      return error;

   // write the config
   error = r_util::writeProjectFile(s_projectContext.file(),
                                    ProjectContext::buildDefaults(),
                                    config);
   if (error)
      return error;

   // set the config
   setProjectConfig(config);

   // write the vcs options
   error = s_projectContext.writeVcsOptions(vcsOptions);
   if (error)
      LOG_ERROR(error);

   // write the build options
   error = s_projectContext.writeBuildOptions(buildOptions);
   if (error)
      LOG_ERROR(error);

   return Success();
}

Error writeProjectVcsOptions(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   // read the vcs options
   json::Object vcsOptionsJson;
   Error error = json::readParam(request.params, 0, &vcsOptionsJson);
   if (error)
      return error;
   RProjectVcsOptions vcsOptions;
   error = rProjectVcsOptionsFromJson(vcsOptionsJson, &vcsOptions);
   if (error)
      return error;

   // write the vcs options
   error = s_projectContext.writeVcsOptions(vcsOptions);
   if (error)
      LOG_ERROR(error);

   return Success();
}


void onQuit()
{
   s_projectContext.setLastProjectPath(s_projectContext.file());
}

void syncProjectFileChanges()
{
   // read project file config
   bool providedDefaults;
   std::string userErrMsg;
   r_util::RProjectConfig config;
   Error error = r_util::readProjectFile(s_projectContext.file(),
                                         ProjectContext::defaultConfig(),
                                         ProjectContext::buildDefaults(),
                                         &config,
                                         &providedDefaults,
                                         &userErrMsg);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // set config
   setProjectConfig(config);

   // fire event to client
   json::Object dataJson;
   dataJson["type"] = "project";
   dataJson["prefs"] = s_projectContext.uiPrefs();
   ClientEvent event(client_events::kUiPrefsChanged, dataJson);
   module_context::enqueClientEvent(event);
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   BOOST_FOREACH(const core::system::FileChangeEvent& event, events)
   {
      // if the project file changed then sync its changes
      if (event.fileInfo().absolutePath() ==
          s_projectContext.file().absolutePath())
      {
         syncProjectFileChanges();
         break;
      }
   }
}

void onMonitoringDisabled()
{
   // NOTE: if monitoring is disabled then we can't sync changes to the
   // project file -- we could poll for this however since it is only
   // a conveninece to have these synced we don't do this
}

FilePath resolveProjectSwitch(const std::string& projectPath)
{
   FilePath projectFilePath;

   // clear any initial context settings which may be leftover
   // by a re-instatiation of rsession by desktop
   session::options().clearInitialContextSettings();

   // check for special "none" value (used for close project)
   if (projectPath == kProjectNone)
   {
      projectFilePath = FilePath();

      // flush the last project path so restarts won't put us back into
      // project context (see case 4015)
      s_projectContext.setLastProjectPath(FilePath());
   }
   else
   {
      projectFilePath = module_context::resolveAliasedPath(projectPath);
   }

   return projectFilePath;
}


}  // anonymous namespace


// Note that the logic here needs to be synchronized with the logic in
// core::r_util::RSessionContext::nextSessionWorkingDir (so that both
// reach the same conclusion about what the next working directory is)
void startup()
{
   // register suspend handler
   using namespace module_context;
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));

   // determine project file path
   FilePath projectFilePath;

   // alias some project context data
   std::string nextSessionProject = s_projectContext.nextSessionProject();
   std::string switchToProject = s_projectContext.switchToProjectPath();
   FilePath lastProjectPath = s_projectContext.lastProjectPath();

   // check for explicit request for a project (file association or url based)
   if (!session::options().initialProjectPath().empty())
   {
      projectFilePath = session::options().initialProjectPath();
   }

   // see if there is a project path hard-wired for the next session
   // (this would be used for resuming of a suspended session)
   else if (!nextSessionProject.empty())
   {
      // reset next session project path so its a one shot deal
      s_projectContext.setNextSessionProject("");

      projectFilePath = resolveProjectSwitch(nextSessionProject);
   }

   // see if this is a project switch
   else if (!switchToProject.empty())
   {
      // reset switch to project path so its a one shot deal
      s_projectContext.setSwitchToProjectPath("");

      projectFilePath = resolveProjectSwitch(switchToProject);
   }

   // check for other working dir override (implies a launch of a file
   // but not of a project). this code path is here to prevent
   // the next code path from executing
   else if (!session::options().initialWorkingDirOverride().empty())
   {
      projectFilePath = FilePath();
   }

   // check for restore last project
   else if (userSettings().alwaysRestoreLastProject() &&
            !lastProjectPath.empty())
   {

      // get last project path
      projectFilePath = lastProjectPath;

      // reset it to empty so that we only attempt to load the "lastProject"
      // a single time (this will be reset to the path below after we
      // clear the s_projectContext.initialize)
      s_projectContext.setLastProjectPath(FilePath());
   }

   // else no active project for this session
   else
   {
      projectFilePath = FilePath();
   }

   // if we have a project file path then try to initialize the
   // project context (show a warning to the user if we can't)
   if (!projectFilePath.empty())
   {
      std::string userErrMsg;
      Error error = s_projectContext.startup(projectFilePath, &userErrMsg);
      if (error)
      {
         // log the error
         error.addProperty("project-file", projectFilePath.absolutePath());
         error.addProperty("user-msg", userErrMsg);
         LOG_ERROR(error);

         // enque the error
         json::Object openProjError;
         openProjError["project"] = module_context::createAliasedPath(
                                                            projectFilePath);
         openProjError["message"] = userErrMsg;
         ClientEvent event(client_events::kOpenProjectError, openProjError);
         module_context::enqueClientEvent(event);
      }
   }
}

SEXP rs_writeProjectFile(SEXP projectFilePathSEXP)
{
   std::string absolutePath = r::sexp::asString(projectFilePathSEXP);
   FilePath projectFilePath(absolutePath);
   
   Error error = r_util::writeProjectFile(
            projectFilePath,
            ProjectContext::buildDefaults(),
            ProjectContext::defaultConfig());
   
   r::sexp::Protect protect;
   return error ?
            r::sexp::create(false, &protect) :
            r::sexp::create(true, &protect);
}

SEXP rs_addFirstRunDoc(SEXP projectFileAbsolutePathSEXP, SEXP docRelativePathSEXP)
{
   std::string projectFileAbsolutePath = r::sexp::asString(projectFileAbsolutePathSEXP);
   const FilePath projectFilePath(projectFileAbsolutePath);
   
   std::string docRelativePath = r::sexp::asString(docRelativePathSEXP);
   
   addFirstRunDoc(projectFilePath, docRelativePath);
   return R_NilValue;
}

Error initialize()
{
   r::routines::registerCallMethod(
            "rs_writeProjectFile",
            (DL_FUNC) rs_writeProjectFile,
            1);
   
   r::routines::registerCallMethod(
            "rs_addFirstRunDoc",
            (DL_FUNC) rs_addFirstRunDoc,
            2);
   
   // call project-context initialize
   Error error = s_projectContext.initialize();
   if (error)
      return error;

   // subscribe to file_monitor for project file changes
   projects::FileMonitorCallbacks cb;
   cb.onFilesChanged = onFilesChanged;
   cb.onMonitoringDisabled = onMonitoringDisabled;
   s_projectContext.subscribeToFileMonitor("", cb);

   // subscribe to quit for setting last project path
   module_context::events().onQuit.connect(onQuit);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_new_project_context", getNewProjectContext))
      (bind(registerRpcMethod, "create_project", createProject))
      (bind(registerRpcMethod, "read_project_options", readProjectOptions))
      (bind(registerRpcMethod, "write_project_options", writeProjectOptions))
      (bind(registerRpcMethod, "write_project_vcs_options", writeProjectVcsOptions))
   ;
   return initBlock.execute();
}

ProjectContext& projectContext()
{
   return s_projectContext;
}

} // namespace projects
} // namesapce session
} // namespace rstudio

