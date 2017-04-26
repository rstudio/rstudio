/*
 * SessionWorkbench.cpp
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


#include "SessionWorkbench.hpp"

#include <algorithm>

#include <boost/function.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Debug.hpp>
#include <core/Exec.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/system/Environment.hpp>
#include <core/system/ShellUtils.hpp>

#include <r/ROptions.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RClientState.hpp> 
#include <r/RFunctionHook.hpp>
#include <r/RRoutines.hpp>

#include <session/projects/SessionProjects.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/RVersionSettings.hpp>
#include <session/SessionTerminalShell.hpp>

#include "SessionVCS.hpp"
#include "SessionGit.hpp"
#include "SessionSVN.hpp"

#include "SessionSpelling.hpp"

#include <R_ext/RStartup.h>
extern "C" SA_TYPE SaveAction;

#include "session-config.h"
#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace workbench {

namespace {

module_context::WaitForMethodFunction s_waitForEditorContext;

SEXP rs_getEditorContext(SEXP typeSEXP)
{
   int type = r::sexp::asInteger(typeSEXP);
   
   json::Object eventData;
   eventData["type"] = "editor_context";
   eventData["data"] = type;
   
   // send the event
   ClientEvent editorContextEvent(client_events::kEditorCommand, eventData);
   
   // wait for event to complete
   json::JsonRpcRequest request;
   
   bool succeeded = s_waitForEditorContext(&request, editorContextEvent);
   if (!succeeded)
      return R_NilValue;
   
   std::string id;
   std::string path;
   std::string contents;
   json::Array selection;
   
   Error error = json::readObjectParam(request.params, 0,
                                       "id", &id,
                                       "path", &path,
                                       "contents", &contents,
                                       "selection", &selection);
   if (error)
   {
      LOG_ERROR(error);
      return R_NilValue;
   }
   
   // if the id is empty, implies the source window is closed or
   // no documents were available
   if (id.empty())
      return R_NilValue;
   
   using namespace r::sexp;
   Protect protect;
   ListBuilder builder(&protect);
   
   builder.add("id", id);
   builder.add("path", path);
   builder.add("contents", core::algorithm::split(contents, "\n"));
   
   // add in the selection ranges
   ListBuilder selectionBuilder(&protect);
   for (std::size_t i = 0; i < selection.size(); ++i)
   {
      json::Object object = selection[i].get_obj();
      
      json::Array rangeJson;
      std::string text;
      Error error = json::readObject(object,
                                     "range", &rangeJson,
                                     "text", &text);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      
      std::vector<int> range;
      if (!json::fillVectorInt(rangeJson, &range))
      {
         LOG_WARNING_MESSAGE("failed to parse document range");
         continue;
      }
      
      // the ranges passed use 0-based indexing;
      // transform to 1-based indexing for R
      for (std::size_t i = 0; i < range.size(); ++i)
         ++range[i];
      
      ListBuilder builder(&protect);
      builder.add("range", range);
      builder.add("text", text);
      
      selectionBuilder.add(builder);
   }
   
   builder.add("selection", selectionBuilder);
   
   return r::sexp::create(builder, &protect);
}

Error setClientState(const json::JsonRpcRequest& request, 
                     json::JsonRpcResponse* pResponse)
{   
   pResponse->setSuppressDetectChanges(true);

   // extract params
   json::Object temporaryState, persistentState, projPersistentState;
   Error error = json::readParams(request.params, 
                                  &temporaryState,
                                  &persistentState,
                                  &projPersistentState);
   if (error)
      return error ;
   
   // set state
   r::session::ClientState& clientState = r::session::clientState();
   clientState.putTemporary(temporaryState);
   clientState.putPersistent(persistentState);
   clientState.putProjectPersistent(projPersistentState);
   
   return Success();
}
   
     
// IN: WorkbenchMetrics object
// OUT: Void
Error setWorkbenchMetrics(const json::JsonRpcRequest& request, 
                          json::JsonRpcResponse* pResponse)
{
   // extract fields
   r::session::RClientMetrics metrics ;
   Error error = json::readObjectParam(request.params, 0,
                                 "consoleWidth", &(metrics.consoleWidth),
                                 "graphicsWidth", &(metrics.graphicsWidth),
                                 "graphicsHeight", &(metrics.graphicsHeight),
                                 "devicePixelRatio", &(metrics.devicePixelRatio));
   if (error)
      return error;
   
   // set the metrics
   r::session::setClientMetrics(metrics);
   
   return Success();
}

CRANMirror toCRANMirror(const json::Object& cranMirrorJson)
{
   CRANMirror cranMirror;
   json::readObject(cranMirrorJson,
                    "name", &cranMirror.name,
                    "host", &cranMirror.host,
                    "url", &cranMirror.url,
                    "country", &cranMirror.country);
   return cranMirror;
}

/* Call to this is commented out below
BioconductorMirror toBioconductorMirror(const json::Object& mirrorJson)
{
   BioconductorMirror mirror;
   json::readObject(mirrorJson,
                    "name", &mirror.name,
                    "url", &mirror.url);
   return mirror;
}
*/

// try to detect a terminal on linux desktop
FilePath detectedTerminalPath()
{
#if defined(_WIN32) || defined(__APPLE__)
   return FilePath();
#else
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      std::vector<FilePath> terminalPaths;
      terminalPaths.push_back(FilePath("/usr/bin/gnome-terminal"));
      terminalPaths.push_back(FilePath("/usr/bin/konsole"));
      terminalPaths.push_back(FilePath("/usr/bin/xfce4-terminal"));
      terminalPaths.push_back(FilePath("/usr/bin/xterm"));

      BOOST_FOREACH(const FilePath& terminalPath, terminalPaths)
      {
         if (terminalPath.exists())
            return terminalPath;
      }

      return FilePath();
   }
   else
   {
      return FilePath();
   }
#endif
}

Error setPrefs(const json::JsonRpcRequest& request, json::JsonRpcResponse*)
{
   // read params
   json::Object generalPrefs, historyPrefs, editingPrefs, packagesPrefs,
                projectsPrefs, sourceControlPrefs, compilePdfPrefs,
                terminalPrefs;
   Error error = json::readObjectParam(request.params, 0,
                              "general_prefs", &generalPrefs,
                              "history_prefs", &historyPrefs,
                              "editing_prefs", &editingPrefs,
                              "packages_prefs", &packagesPrefs,
                              "projects_prefs", &projectsPrefs,
                              "source_control_prefs", &sourceControlPrefs,
                              "compile_pdf_prefs", &compilePdfPrefs,
                              "terminal_prefs", &terminalPrefs);
   if (error)
      return error;
   json::Object uiPrefs;
   error = json::readParam(request.params, 1, &uiPrefs);
   if (error)
      return error;


   // read and set general prefs
   int saveAction;
   bool loadRData, rProfileOnResume, restoreProjectRVersion, showLastDotValue;
   bool reuseSessionsForProjectLinks;
   std::string initialWorkingDir, showUserHomePage;
   json::Object defaultRVersionJson;
   error = json::readObject(generalPrefs,
                            "show_user_home_page", &showUserHomePage,
                            "reuse_sessions_for_project_links", &reuseSessionsForProjectLinks,
                            "save_action", &saveAction,
                            "load_rdata", &loadRData,
                            "rprofile_on_resume", &rProfileOnResume,
                            "initial_working_dir", &initialWorkingDir,
                            "default_r_version", &defaultRVersionJson,
                            "restore_project_r_version", &restoreProjectRVersion,
                            "show_last_dot_value", &showLastDotValue);
   if (error)
      return error;

   // detect if lastDotValue changed
   bool lastDotValueChanged = userSettings().showLastDotValue() != showLastDotValue;

   // update settings
   userSettings().beginUpdate();
   userSettings().setShowUserHomePage(showUserHomePage);
   userSettings().setReuseSessionsForProjectLinks(reuseSessionsForProjectLinks);
   userSettings().setSaveAction(saveAction);
   userSettings().setLoadRData(loadRData);
   userSettings().setRprofileOnResume(rProfileOnResume);
   userSettings().setShowLastDotValue(showLastDotValue);
   userSettings().setInitialWorkingDirectory(FilePath(initialWorkingDir));
   userSettings().endUpdate();

   // refresh environment if lastDotValueChanged
   if (lastDotValueChanged)
   {
      ClientEvent refreshEvent(client_events::kEnvironmentRefresh);
      module_context::enqueClientEvent(refreshEvent);
   }

   // sync underlying R save action
   module_context::syncRSaveAction();

   // versions prefs
   std::string defaultRVersion, defaultRVersionHome;
   error = json::readObject(defaultRVersionJson,
                            "version", &defaultRVersion,
                            "r_home", &defaultRVersionHome);
   if (error)
      return error;

   RVersionSettings versionSettings(module_context::userScratchPath(),
                                    FilePath(options().getOverlayOption(
                                                kSessionSharedStoragePath)));
   versionSettings.setDefaultRVersion(defaultRVersion, defaultRVersionHome);
   versionSettings.setRestoreProjectRVersion(restoreProjectRVersion);

   // read and set history prefs
   bool alwaysSave, removeDuplicates;
   error = json::readObject(historyPrefs,
                            "always_save", &alwaysSave,
                            "remove_duplicates", &removeDuplicates);
   if (error)
      return error;
   userSettings().beginUpdate();
   userSettings().setAlwaysSaveHistory(alwaysSave);
   userSettings().setRemoveHistoryDuplicates(removeDuplicates);
   userSettings().endUpdate();

   // read and set editing prefs
   int lineEndings;
   error = json::readObject(editingPrefs,
                            "line_endings", &lineEndings);
   if (error)
      return error;
   userSettings().beginUpdate();
   userSettings().setLineEndings((core::string_utils::LineEnding)lineEndings);
   userSettings().endUpdate();

   // read and set packages prefs
   bool useInternet2, cleanupAfterCheckSuccess, viewDirAfterCheckFailure;
   bool hideObjectFiles, useDevtools, useSecureDownload, useNewlineInMakefiles;
   json::Object cranMirrorJson;
   error = json::readObject(packagesPrefs,
                            "cran_mirror", &cranMirrorJson,
                            "use_internet2", &useInternet2,
/* see note on bioconductor below
                            "bioconductor_mirror", &bioconductorMirrorJson);
*/
                            "cleanup_after_check_success", &cleanupAfterCheckSuccess,
                            "viewdir_after_check_failure", &viewDirAfterCheckFailure,
                            "hide_object_files", &hideObjectFiles,
                            "use_devtools", &useDevtools,
                            "use_secure_download", &useSecureDownload,
                            "use_newline_in_makefiles", &useNewlineInMakefiles);

   if (error)
       return error;
   userSettings().beginUpdate();
   userSettings().setUseDevtools(useDevtools);
   userSettings().setSecurePackageDownload(useSecureDownload);
   userSettings().setCRANMirror(toCRANMirror(cranMirrorJson));
   userSettings().setUseInternet2(useInternet2);
   userSettings().setCleanupAfterRCmdCheck(cleanupAfterCheckSuccess);
   userSettings().setHideObjectFiles(hideObjectFiles);
   userSettings().setViewDirAfterRCmdCheck(viewDirAfterCheckFailure);
   userSettings().setUseNewlineInMakefiles(useNewlineInMakefiles);

   // NOTE: currently there is no UI for bioconductor mirror so we
   // don't want to set it (would have side effect of overwriting
   // user-specified BioC_Mirror option)
   /*
   userSettings().setBioconductorMirror(toBioconductorMirror(
                                                bioconductorMirrorJson));
   */
   userSettings().endUpdate();

   // verify cran mirror security (will either update to https or
   // will print a warning)
   module_context::reconcileSecureDownloadConfiguration();

   // read and set projects prefs
   bool restoreLastProject;
   error = json::readObject(projectsPrefs,
                            "restore_last_project", &restoreLastProject);
   if (error)
      return error;
   userSettings().beginUpdate();
   userSettings().setAlwaysRestoreLastProject(restoreLastProject);
   userSettings().endUpdate();

   // read and set source control prefs
   bool vcsEnabled, useGitBash;
   std::string gitExe, svnExe, terminalPath;
   error = json::readObject(sourceControlPrefs,
                            "vcs_enabled", &vcsEnabled,
                            "git_exe_path", &gitExe,
                            "svn_exe_path", &svnExe,
                            "terminal_path", &terminalPath,
                            "use_git_bash", &useGitBash);
   if (error)
      return error;
   userSettings().beginUpdate();
   userSettings().setVcsEnabled(vcsEnabled);

   FilePath gitExePath(gitExe);
   if (gitExePath == git::detectedGitExePath())
      userSettings().setGitExePath(FilePath());
   else
      userSettings().setGitExePath(gitExePath);

   FilePath svnExePath(svnExe);
   if (svnExePath == svn::detectedSvnExePath())
      userSettings().setSvnExePath(FilePath());
   else
      userSettings().setSvnExePath(svnExePath);

   FilePath terminalFilePath(terminalPath);
   if (terminalFilePath == detectedTerminalPath())
      userSettings().setVcsTerminalPath(FilePath());
   else
      userSettings().setVcsTerminalPath(terminalFilePath);

   userSettings().setVcsUseGitBash(useGitBash);

   userSettings().endUpdate();


   // read and update compile pdf prefs
   bool cleanOutput, enableShellEscape;
   error = json::readObject(compilePdfPrefs,
                            "clean_output", &cleanOutput,
                            "enable_shell_escape", &enableShellEscape);
   if (error)
      return error;
   userSettings().beginUpdate();
   userSettings().setCleanTexi2DviOutput(cleanOutput);
   userSettings().setEnableLaTeXShellEscape(enableShellEscape);
   userSettings().endUpdate();

   // read and update terminal prefs
   int defaultTerminalShell;
   error = json::readObject(terminalPrefs,
                            "default_shell", &defaultTerminalShell);
   if (error)
      return error;
   userSettings().beginUpdate();
   userSettings().setDefaultTerminalShellValue(
      static_cast<console_process::TerminalShell::TerminalShellType>(defaultTerminalShell));
   userSettings().endUpdate();

   // set ui prefs
   userSettings().setUiPrefs(uiPrefs);

   // fire preferences saved event
   module_context::events().onPreferencesSaved();

   return Success();
}


Error setUiPrefs(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   json::Object uiPrefs;
   Error error = json::readParams(request.params, &uiPrefs);
   if (error)
      return error;

   userSettings().setUiPrefs(uiPrefs);

   return Success();
}


json::Object toCRANMirrorJson(const CRANMirror& cranMirror)
{
   json::Object cranMirrorJson;
   cranMirrorJson["name"] = cranMirror.name;
   cranMirrorJson["host"] = cranMirror.host;
   cranMirrorJson["url"] = cranMirror.url;
   cranMirrorJson["country"] = cranMirror.country;
   return cranMirrorJson;
}

json::Object toBioconductorMirrorJson(
                           const BioconductorMirror& bioconductorMirror)
{
   json::Object bioconductorMirrorJson;
   bioconductorMirrorJson["name"] = bioconductorMirror.name;
   bioconductorMirrorJson["url"] = bioconductorMirror.url;
   return bioconductorMirrorJson;
}


Error getRPrefs(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   // proj settings
   RVersionSettings versionSettings(module_context::userScratchPath(),
                                    FilePath(options().getOverlayOption(
                                                kSessionSharedStoragePath)));
   json::Object defaultRVersionJson;
   defaultRVersionJson["version"] = versionSettings.defaultRVersion();
   defaultRVersionJson["r_home"] = versionSettings.defaultRVersionHome();

   // get general prefs
   json::Object generalPrefs;
   generalPrefs["show_user_home_page"] = userSettings().showUserHomePage();
   generalPrefs["reuse_sessions_for_project_links"] = userSettings().reuseSessionsForProjectLinks();
   generalPrefs["save_action"] = userSettings().saveAction();
   generalPrefs["load_rdata"] = userSettings().loadRData();
   generalPrefs["rprofile_on_resume"] = userSettings().rProfileOnResume();
   generalPrefs["initial_working_dir"] = module_context::createAliasedPath(
         userSettings().initialWorkingDirectory());
   generalPrefs["default_r_version"] = defaultRVersionJson;
   generalPrefs["restore_project_r_version"] = versionSettings.restoreProjectRVersion();
   generalPrefs["show_last_dot_value"] = userSettings().showLastDotValue();

   // get history prefs
   json::Object historyPrefs;
   historyPrefs["always_save"] = userSettings().alwaysSaveHistory();
   historyPrefs["remove_duplicates"] = userSettings().removeHistoryDuplicates();

   // get editing prefs
   json::Object editingPrefs;
   editingPrefs["line_endings"] = (int)userSettings().lineEndings();

   // get packages prefs
   json::Object packagesPrefs;
   packagesPrefs["use_devtools"] = userSettings().useDevtools();
   packagesPrefs["cran_mirror"] = toCRANMirrorJson(
                                      userSettings().cranMirror());
   packagesPrefs["use_internet2"] = userSettings().useInternet2();
   packagesPrefs["bioconductor_mirror"] = toBioconductorMirrorJson(
                                      userSettings().bioconductorMirror());
   packagesPrefs["cleanup_after_check_success"] = userSettings().cleanupAfterRCmdCheck();
   packagesPrefs["viewdir_after_check_failure"] = userSettings().viewDirAfterRCmdCheck();
   packagesPrefs["hide_object_files"] = userSettings().hideObjectFiles();
   packagesPrefs["use_secure_download"] = userSettings().securePackageDownload();
   packagesPrefs["use_newline_in_makefiles"] = userSettings().useNewlineInMakefiles();

   // get projects prefs
   json::Object projectsPrefs;
   projectsPrefs["restore_last_project"] = userSettings().alwaysRestoreLastProject();

   // get source control prefs
   json::Object sourceControlPrefs;
   sourceControlPrefs["vcs_enabled"] = userSettings().vcsEnabled();
   FilePath gitExePath = userSettings().gitExePath();
   if (gitExePath.empty())
      gitExePath = git::detectedGitExePath();
   sourceControlPrefs["git_exe_path"] = gitExePath.absolutePath();

   FilePath svnExePath = userSettings().svnExePath();
   if (svnExePath.empty())
      svnExePath = svn::detectedSvnExePath();
   sourceControlPrefs["svn_exe_path"] = svnExePath.absolutePath();

   FilePath terminalPath = userSettings().vcsTerminalPath();
   if (terminalPath.empty())
      terminalPath = detectedTerminalPath();
   sourceControlPrefs["terminal_path"] = terminalPath.absolutePath();

   sourceControlPrefs["use_git_bash"] = userSettings().vcsUseGitBash();

   FilePath sshKeyDir = modules::source_control::defaultSshKeyDir();
   FilePath rsaSshKeyPath = sshKeyDir.childPath("id_rsa");
   sourceControlPrefs["rsa_key_path"] =
                  module_context::createAliasedPath(rsaSshKeyPath);
   sourceControlPrefs["have_rsa_key"] = rsaSshKeyPath.exists();


   // get compile pdf prefs
   json::Object compilePdfPrefs;
   compilePdfPrefs["clean_output"] = userSettings().cleanTexi2DviOutput();
   compilePdfPrefs["enable_shell_escape"] = userSettings().enableLaTeXShellEscape();

   // get terminal prefs
   json::Object terminalPrefs;
   terminalPrefs["default_shell"] = userSettings().defaultTerminalShellValue();

   // initialize and set result object
   json::Object result;
   result["general_prefs"] = generalPrefs;
   result["history_prefs"] = historyPrefs;
   result["editing_prefs"] = editingPrefs;
   result["packages_prefs"] = packagesPrefs;
   result["projects_prefs"] = projectsPrefs;
   result["source_control_prefs"] = sourceControlPrefs;
   result["compile_pdf_prefs"] = compilePdfPrefs;
   result["spelling_prefs_context"] =
                  session::modules::spelling::spellingPrefsContextAsJson();
   result["terminal_prefs"] = terminalPrefs;

   pResponse->setResult(result);

   return Success();
}

template <typename T>
void ammendShellPaths(T* pTarget)
{
   // non-path git bin dir
   std::string gitBinDir = git::nonPathGitBinDir();
   if (!gitBinDir.empty())
      core::system::addToPath(pTarget, gitBinDir);

   // non-path svn bin dir
   std::string svnBinDir = svn::nonPathSvnBinDir();
   if (!svnBinDir.empty())
      core::system::addToPath(pTarget, svnBinDir);

   // msys_ssh path
   core::system::addToPath(pTarget,
                           session::options().msysSshPath().absolutePath());
}

Error getTerminalOptions(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   json::Object optionsJson;

   FilePath terminalPath;

#if defined(_WIN32)

   // if we are using git bash then return its path
   if (git::isGitEnabled() && userSettings().vcsUseGitBash())
   {
      terminalPath = console_process::getGitBashShell();
   }

#elif defined(__APPLE__)

   // do nothing (we always launch Terminal.app)

#else

   // auto-detection (+ overridable by a setting)
   terminalPath = userSettings().vcsTerminalPath();
   if (terminalPath.empty())
      terminalPath = detectedTerminalPath();

#endif

   // append shell paths as appropriate
   std::string extraPathEntries;
   ammendShellPaths(&extraPathEntries);

   optionsJson["terminal_path"] = terminalPath.absolutePath();
   optionsJson["working_directory"] =
                  module_context::shellWorkingDirectory().absolutePath();
   optionsJson["extra_path_entries"] = extraPathEntries;
   pResponse->setResult(optionsJson);

   return Success();
}

Error getTerminalShells(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   console_process::AvailableTerminalShells availableShells;
   json::Array shells;
   availableShells.toJson(&shells);
   pResponse->setResult(shells);
   return Success();
}

Error executeCode(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   // get the code
   std::string code;
   Error error = json::readParam(request.params, 0, &code);
   if (error)
      return error;

   // execute the code (show error in the console)
   error = r::exec::executeString("{" + code + "}");
   if (error)
   {
      std::string errMsg = "Error executing code: " + code + "\n";
      errMsg += r::endUserErrorMessage(error);
      module_context::consoleWriteError(errMsg + "\n");
   }

   return Success();
}


Error createSshKey(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string path, type, passphrase;
   bool overwrite;
   Error error = json::readObjectParam(request.params, 0,
                                       "path", &path,
                                       "type", &type,
                                       "passphrase", &passphrase,
                                       "overwrite", &overwrite);
   if (error)
      return error;

#ifdef RSTUDIO_SERVER
   // In server mode, passphrases are encrypted
   using namespace rstudio::core::system::crypto;
   error = rsaPrivateDecrypt(passphrase, &passphrase);
   if (error)
      return error;
#endif

   // resolve key path
   FilePath sshKeyPath = module_context::resolveAliasedPath(path);
   error = sshKeyPath.parent().ensureDirectory();
   if (error)
      return error;
   FilePath sshPublicKeyPath = sshKeyPath.parent().complete(
                                             sshKeyPath.stem() + ".pub");
   if (sshKeyPath.exists() || sshPublicKeyPath.exists())
   {
      if (!overwrite)
      {
         json::Object resultJson;
         resultJson["failed_key_exists"] = true;
         pResponse->setResult(resultJson);
         return Success();
      }
      else
      {
         Error error = sshKeyPath.removeIfExists();
         if (error)
            return error;
         error = sshPublicKeyPath.removeIfExists();
         if (error)
            return error;
      }
   }

   // compose a shell command to create the key
   shell_utils::ShellCommand cmd("ssh-keygen");

   // type
   cmd << "-t" << type;

   // passphrase (optional)
   cmd << "-N";
   if (!passphrase.empty())
      cmd << passphrase;
   else
      cmd << std::string("");

   // path
   cmd << "-f" << sshKeyPath;

   // process options
   core::system::ProcessOptions options;

   // detach the session so there is no terminal
#ifndef _WIN32
   options.detachSession = true;
#endif

   // customize the environment on Win32
#ifdef _WIN32
   core::system::Options childEnv;
   core::system::environment(&childEnv);

   // set HOME to USERPROFILE
   std::string userProfile = core::system::getenv(childEnv, "USERPROFILE");
   core::system::setenv(&childEnv, "HOME", userProfile);

   // add msys_ssh to path
   core::system::addToPath(&childEnv,
                           session::options().msysSshPath().absolutePath());

   options.environment = childEnv;
#endif

   // run it
   core::system::ProcessResult result;
   error = runCommand(shell_utils::sendStdErrToStdOut(cmd),
                      options,
                      &result);
   if (error)
      return error;

   // return exit code and output
   json::Object resultJson;
   resultJson["failed_key_exists"] = false;
   resultJson["exit_status"] = result.exitStatus;
   resultJson["output"] = result.stdOut;
   pResponse->setResult(resultJson);
   return Success();
}



// path edit file postback script (provided as GIT_EDITOR and SVN_EDITOR)
std::string s_editFileCommand;

// function we can call to wait for edit_completed
module_context::WaitForMethodFunction s_waitForEditCompleted;

// edit file postback handler
void editFilePostback(const std::string& file,
                      const module_context::PostbackHandlerContinuation& cont)
{
   // read file contents
   FilePath filePath(file);
   std::string fileContents;
   Error error = core::readStringFromFile(filePath, &fileContents);
   if (error)
   {
      LOG_ERROR(error);
      cont(EXIT_FAILURE, "");
      return;
   }

   // prepare edit event
   ClientEvent editEvent = session::showEditorEvent(fileContents, false, true);

   // wait for edit_completed
   json::JsonRpcRequest request ;
   bool succeeded = s_waitForEditCompleted(&request, editEvent);

   // cancelled or otherwise didn't succeed
   if (!succeeded || request.params[0].is_null())
   {
      cont(EXIT_FAILURE, "");
      return;
   }

   // extract the content
   std::string editedFileContents;
   error = json::readParam(request.params, 0, &editedFileContents);
   if (error)
   {
      LOG_ERROR(error);
      cont(EXIT_FAILURE, "");
      return;
   }

   // write the content back to the file
   error = core::writeStringToFile(filePath, editedFileContents);
   if (error)
   {
      LOG_ERROR(error);
      cont(EXIT_FAILURE, "");
      return;
   }

   // success
   cont(EXIT_SUCCESS, "");
}

Error startTerminal(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   using namespace session::module_context;
   using namespace session::console_process;

   int shellTypeInt;
   int cols, rows; // initial pseudo-terminal size
   std::string termHandle; // empty if starting a new terminal
   std::string termCaption;
   std::string termTitle;
   bool useWebsockets;
   int termSequence = kNoTerminal;
   
   Error error = json::readParams(request.params,
                                  &shellTypeInt,
                                  &cols,
                                  &rows,
                                  &termHandle,
                                  &termCaption,
                                  &termTitle,
                                  &useWebsockets,
                                  &termSequence);
   if (error)
      return error;

   TerminalShell::TerminalShellType shellType =
         static_cast<TerminalShell::TerminalShellType>(shellTypeInt);
   if (shellType < TerminalShell::DefaultShell || shellType > TerminalShell::Max)
   {
       shellType = TerminalShell::DefaultShell;
   }
   
   // configure environment for shell
   core::system::Options shellEnv;
   core::system::environment(&shellEnv);

#ifndef _WIN32
   // set xterm title to show current working directory after each command
   core::system::setenv(&shellEnv, "PROMPT_COMMAND",
                        "echo -ne \"\\033]0;${PWD/#${HOME}/~}\\007\"");
   
   core::system::setenv(&shellEnv, "GIT_EDITOR", s_editFileCommand);
   core::system::setenv(&shellEnv, "SVN_EDITOR", s_editFileCommand);
#endif

   if (termSequence != kNoTerminal)
   {
      core::system::setenv(&shellEnv, "RSTUDIO_TERM",
                           boost::lexical_cast<std::string>(termSequence));
   }
   
   // ammend shell paths as appropriate
   ammendShellPaths(&shellEnv);

   // set options
   core::system::ProcessOptions options;
   options.workingDir = module_context::shellWorkingDirectory();
   options.environment = shellEnv;
   options.smartTerminal = true;
   options.reportHasSubprocs = true;
   options.cols = cols;
   options.rows = rows;

   // set path to shell
   AvailableTerminalShells shells;
   TerminalShell shell;
   if (shells.getInfo(shellType, &shell))
   {
      options.shellPath = shell.path;
      options.args = shell.args;
   }

   // last-ditch, use system shell
   if (!options.shellPath.exists())
   {
      TerminalShell sysShell;
      if (AvailableTerminalShells::getSystemShell(&sysShell))
      {
         options.shellPath = sysShell.path;
         options.args = sysShell.args;
      }
   }

   if (termCaption.empty())
      termCaption = "Shell";

   boost::shared_ptr<ConsoleProcessInfo> ptrProcInfo =
         boost::make_shared<ConsoleProcessInfo>(
            termCaption, termTitle, termHandle, termSequence,  shellType,
            console_process::Rpc, "" /*channelId*/,
            console_process::kDefaultTerminalMaxOutputLines);

   // run process
   bool websockets = session::options().allowTerminalWebsockets() && useWebsockets;
   boost::shared_ptr<ConsoleProcess> ptrProc =
               ConsoleProcess::createTerminalProcess(options, ptrProcInfo, websockets);

   ptrProc->onExit().connect(boost::bind(
                              &source_control::enqueueRefreshEvent));

   pResponse->setResult(ptrProc->toJson());

   return Success();
}

Error setCRANMirror(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   json::Object cranMirrorJson;
   Error error = json::readParam(request.params, 0, &cranMirrorJson);
   if (error)
      return error;
   CRANMirror cranMirror = toCRANMirror(cranMirrorJson);

   userSettings().beginUpdate();
   userSettings().setCRANMirror(cranMirror);
   userSettings().endUpdate();

   // verify cran mirror security (will either update to https or
   // will print a warning)
   module_context::reconcileSecureDownloadConfiguration();

   return Success();
}


// options("pdfviewer")
void viewPdfPostback(const std::string& pdfPath,
                    const module_context::PostbackHandlerContinuation& cont)
{
   module_context::showFile(FilePath(pdfPath));
   cont(EXIT_SUCCESS, "");
}


void handleFileShow(const http::Request& request, http::Response* pResponse)
{
   // get the file path
   FilePath filePath = module_context::resolveAliasedPath(request.queryParamValue("path"));
   if (!filePath.exists())
   {
      pResponse->setNotFoundError(request.uri());
      return;
   }

   // send it back
   pResponse->setCacheWithRevalidationHeaders();
   pResponse->setCacheableFile(filePath, request);
}

void onUserSettingsChanged()
{
   // sync underlying R save action
   module_context::syncRSaveAction();

   // fire event notifying the client that uiPrefs changed
   json::Object dataJson;
   dataJson["type"] = "global";
   dataJson["prefs"] = userSettings().uiPrefs();
   ClientEvent event(client_events::kUiPrefsChanged, dataJson);
   module_context::enqueClientEvent(event);
}

} // anonymous namespace
   
std::string editFileCommand()
{
   // NOTE: only registered for server mode
   return s_editFileCommand;
}

Error initialize()
{
   // register for change notifications on user settings
   userSettings().onChanged.connect(onUserSettingsChanged);

   // register postback handler for viewPDF (server-only)
   if (session::options().programMode() == kSessionProgramModeServer)
   {
      std::string pdfShellCommand ;
      Error error = module_context::registerPostbackHandler("pdfviewer",
                                                            viewPdfPostback,
                                                            &pdfShellCommand);
      if (error)
         return error ;

      // set pdfviewer option
      error = r::options::setOption("pdfviewer", pdfShellCommand);
      if (error)
         return error ;


      // register editfile handler and save its path
      error = module_context::registerPostbackHandler("editfile",
                                                      editFilePostback,
                                                      &s_editFileCommand);
      if (error)
         return error;

      // register edit_completed waitForMethod handler
      s_waitForEditCompleted = module_context::registerWaitForMethod(
                                                         "edit_completed");
   }
   
   // register waitForMethod for active document context
   using namespace module_context;
   s_waitForEditorContext = registerWaitForMethod("get_editor_context_completed");
   
   RS_REGISTER_CALL_METHOD(rs_getEditorContext, 1);
   
   // complete initialization
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerUriHandler, "/file_show", handleFileShow))
      (bind(registerRpcMethod, "set_client_state", setClientState))
      (bind(registerRpcMethod, "set_workbench_metrics", setWorkbenchMetrics))
      (bind(registerRpcMethod, "set_prefs", setPrefs))
      (bind(registerRpcMethod, "set_ui_prefs", setUiPrefs))
      (bind(registerRpcMethod, "get_r_prefs", getRPrefs))
      (bind(registerRpcMethod, "set_cran_mirror", setCRANMirror))
      (bind(registerRpcMethod, "get_terminal_options", getTerminalOptions))
      (bind(registerRpcMethod, "get_terminal_shells", getTerminalShells))
      (bind(registerRpcMethod, "create_ssh_key", createSshKey))
      (bind(registerRpcMethod, "start_terminal", startTerminal))
      (bind(registerRpcMethod, "execute_code", executeCode));
   return initBlock.execute();
}


} // namespace workbench
} // namespace modules
} // namespace session
} // namespace rstudio

