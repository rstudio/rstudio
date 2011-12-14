/*
 * SessionWorkbench.cpp
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


#include "SessionWorkbench.hpp"

#include <algorithm>

#include <boost/function.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
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

#include <session/projects/SessionProjects.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

#include "SessionVCS.hpp"
#include "SessionGit.hpp"
#include "SessionSVN.hpp"

#include "SessionConsoleProcess.hpp"

#include <R_ext/RStartup.h>
extern "C" SA_TYPE SaveAction;

using namespace core;

namespace session {
namespace modules { 
namespace workbench {

namespace {   
      
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
                                 "graphicsHeight", &(metrics.graphicsHeight));
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

BioconductorMirror toBioconductorMirror(const json::Object& mirrorJson)
{
   BioconductorMirror mirror;
   json::readObject(mirrorJson,
                    "name", &mirror.name,
                    "url", &mirror.url);
   return mirror;
}

Error setPrefs(const json::JsonRpcRequest& request, json::JsonRpcResponse*)
{
   // read params
   json::Object generalPrefs, historyPrefs, packagesPrefs, projectsPrefs,
                sourceControlPrefs;
   Error error = json::readObjectParam(request.params, 0,
                              "general_prefs", &generalPrefs,
                              "history_prefs", &historyPrefs,
                              "packages_prefs", &packagesPrefs,
                              "projects_prefs", &projectsPrefs,
                              "source_control_prefs", &sourceControlPrefs);
   if (error)
      return error;
   json::Object uiPrefs;
   error = json::readParam(request.params, 1, &uiPrefs);
   if (error)
      return error;


   // read and set general prefs
   int saveAction;
   bool loadRData;
   std::string initialWorkingDir;
   error = json::readObject(generalPrefs,
                            "save_action", &saveAction,
                            "load_rdata", &loadRData,
                            "initial_working_dir", &initialWorkingDir);
   if (error)
      return error;

   userSettings().beginUpdate();
   userSettings().setSaveAction(saveAction);
   userSettings().setLoadRData(loadRData);
   userSettings().setInitialWorkingDirectory(FilePath(initialWorkingDir));
   userSettings().endUpdate();

   // sync underlying R save action
   module_context::syncRSaveAction();

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

   // read and set packages prefs
   json::Object cranMirrorJson;
   error = json::readObject(packagesPrefs,
                            "cran_mirror", &cranMirrorJson);
   /* see note on bioconductor below
                            "bioconductor_mirror", &bioconductorMirrorJson);
   */
   if (error)
       return error;
   userSettings().beginUpdate();
   userSettings().setCRANMirror(toCRANMirror(cranMirrorJson));

   // NOTE: currently there is no UI for bioconductor mirror so we
   // don't want to set it (would have side effect of overwriting
   // user-specified BioC_Mirror option)
   /*
   userSettings().setBioconductorMirror(toBioconductorMirror(
                                                bioconductorMirrorJson));
   */
   userSettings().endUpdate();


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
   if (terminalFilePath == source_control::detectedTerminalPath())
      userSettings().setVcsTerminalPath(FilePath());
   else
      userSettings().setVcsTerminalPath(terminalFilePath);

   userSettings().setVcsUseGitBash(useGitBash);

   userSettings().endUpdate();

   // set ui prefs
   userSettings().setUiPrefs(uiPrefs);

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
   // get general prefs
   json::Object generalPrefs;
   generalPrefs["save_action"] = userSettings().saveAction();
   generalPrefs["load_rdata"] = userSettings().loadRData();
   generalPrefs["initial_working_dir"] = module_context::createAliasedPath(
         userSettings().initialWorkingDirectory());

   // get history prefs
   json::Object historyPrefs;
   historyPrefs["always_save"] = userSettings().alwaysSaveHistory();
   historyPrefs["remove_duplicates"] = userSettings().removeHistoryDuplicates();

   // get packages prefs
   json::Object packagesPrefs;
   packagesPrefs["cran_mirror"] = toCRANMirrorJson(
                                      userSettings().cranMirror());
   packagesPrefs["bioconductor_mirror"] = toBioconductorMirrorJson(
                                      userSettings().bioconductorMirror());

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
      terminalPath = source_control::detectedTerminalPath();
   sourceControlPrefs["terminal_path"] = terminalPath.absolutePath();

   sourceControlPrefs["use_git_bash"] = userSettings().vcsUseGitBash();

   sourceControlPrefs["have_rsa_public_key"] =
      modules::source_control::defaultSshKeyDir().childPath(
                                                   "id_rsa.pub").exists();

   // initialize and set result object
   json::Object result;
   result["general_prefs"] = generalPrefs;
   result["history_prefs"] = historyPrefs;
   result["packages_prefs"] = packagesPrefs;
   result["projects_prefs"] = projectsPrefs;
   result["source_control_prefs"] = sourceControlPrefs;

   pResponse->setResult(result);

   return Success();
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
      FilePath gitExePath = git::detectedGitExePath();
      if (!gitExePath.empty())
         terminalPath = gitExePath.parent().childPath("sh.exe");
   }

#elif defined(__APPLE__)

   // do nothing (we always launch Terminal.app)

#else

   // auto-detection (+ overridable by a setting)
   terminalPath = userSettings().vcsTerminalPath();
   if (terminalPath.empty())
      terminalPath = source_control::detectedTerminalPath();

#endif

   optionsJson["terminal_path"] = terminalPath.absolutePath();
   optionsJson["working_directory"] =
                  module_context::shellWorkingDirectory().absolutePath();
   pResponse->setResult(optionsJson);

   return Success();
}

Error startShellDialog(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
#ifndef _WIN32
   using namespace session::module_context;
   using namespace session::modules::console_process;

   // configure environment for shell
   core::system::Options shellEnv;
   core::system::environment(&shellEnv);

   // set dumb terminal
   core::system::setenv(&shellEnv, "TERM", "dumb");

   // set prompt
   std::string path = module_context::createAliasedPath(
                                 module_context::safeCurrentPath());
   std::string prompt = (path.length() > 30) ? "\\W$ " : "\\w$ ";
   core::system::setenv(&shellEnv, "PS1", prompt);

   // disable screen oriented facillites
   core::system::unsetenv(&shellEnv, "EDITOR");
   core::system::unsetenv(&shellEnv, "VISUAL");
   core::system::setenv(&shellEnv, "PAGER", "/bin/cat");

   // normally git will error out in a dumb terminal if EDITOR is not
   // defined, however if the user defines the core.editor git configuration
   // variable git will still try to invoke this editor. we define the
   // editor to /bin/true so that commands which have a default commit
   // message (like git revert) will still work and commands which have
   // no default commit message (like git commit) will fail with an
   // appropriate message ("Aborting commit due to empty commit message")
   core::system::setenv(&shellEnv, "GIT_EDITOR", "/bin/true");

   // for svn if we use /bin/true then svn commit proceeds with a prompt
   // which can actually lead the user back into an editor. for this reason
   // we show a more explicit error message and return false
   core::system::setenv(&shellEnv,
                        "SVN_EDITOR",
                        "echo \"Error: No commit message\" && false");

   // add custom git path if necessary
   std::string gitBinDir = git::nonPathGitBinDir();
   if (!gitBinDir.empty())
      core::system::addToPath(&shellEnv, gitBinDir);

   // add custom svn path if necessary
   std::string svnBinDir = svn::nonPathSvnBinDir();
   if (!svnBinDir.empty())
      core::system::addToPath(&shellEnv, svnBinDir);

   // set options
   core::system::ProcessOptions options;
   options.workingDir = module_context::shellWorkingDirectory();
   options.environment = shellEnv;

   // configure bash command
   core::shell_utils::ShellCommand bashCommand("/bin/bash");
   bashCommand << "--norc";

   // run process
   boost::shared_ptr<ConsoleProcess> ptrProc =
               ConsoleProcess::create(bashCommand,
                                      options,
                                      "Shell",
                                      true,
                                      InteractionAlways,
                                      console_process::kDefaultMaxOutputLines);

   ptrProc->setExitHandler(boost::bind(&source_control::enqueueRefreshEvent));

   pResponse->setResult(ptrProc->toJson());

   return Success();
#else // not supported on Win32
   return Error(json::errc::InvalidRequest, ERROR_LOCATION);
#endif
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
   FilePath filePath(request.queryParamValue("path"));
   if (!filePath.exists())
   {
      pResponse->setError(http::status::NotFound, "File not found");
      return;
   }

   // send it back
   pResponse->setCacheableFile(filePath, request);
}

SEXP capabilitiesX11Hook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   r::sexp::Protect rProtect;
   return r::sexp::create(false, &rProtect);
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

      // ensure that capabilitiesX11 always returns false
      error = r::function_hook::registerReplaceHook("capabilitiesX11",
                                                    capabilitiesX11Hook,
                                                    (CCODE*)NULL);
      if (error)
         return error;
   }
   
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
      (bind(registerRpcMethod, "start_shell_dialog", startShellDialog));
   return initBlock.execute();
}


} // namepsace workbench
} // namespace modules
} // namesapce session

