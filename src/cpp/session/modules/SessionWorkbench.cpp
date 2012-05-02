/*
 * SessionWorkbench.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include "SessionSpelling.hpp"

#include <R_ext/RStartup.h>
extern "C" SA_TYPE SaveAction;

#include "config.h"
#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif


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
   json::Object generalPrefs, historyPrefs, packagesPrefs, projectsPrefs,
                sourceControlPrefs, compilePdfPrefs;
   Error error = json::readObjectParam(request.params, 0,
                              "general_prefs", &generalPrefs,
                              "history_prefs", &historyPrefs,
                              "packages_prefs", &packagesPrefs,
                              "projects_prefs", &projectsPrefs,
                              "source_control_prefs", &sourceControlPrefs,
                              "compile_pdf_prefs", &compilePdfPrefs);
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
   if (terminalFilePath == detectedTerminalPath())
      userSettings().setVcsTerminalPath(FilePath());
   else
      userSettings().setVcsTerminalPath(terminalFilePath);

   userSettings().setVcsUseGitBash(useGitBash);

   userSettings().endUpdate();


   // read and update compile pdf prefs
   bool useTexi2Dvi, cleanOutput, enableShellEscape;
   error = json::readObject(compilePdfPrefs,
                            "use_texi2dvi", &useTexi2Dvi,
                            "clean_output", &cleanOutput,
                            "enable_shell_escape", &enableShellEscape);
   if (error)
      return error;
   userSettings().beginUpdate();
   userSettings().setUsetexi2Dvi(useTexi2Dvi);
   userSettings().setCleanTexi2DviOutput(cleanOutput);
   userSettings().setEnableLaTeXShellEscape(enableShellEscape);
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
   compilePdfPrefs["use_texi2dvi"] = userSettings().useTexi2Dvi();
   compilePdfPrefs["clean_output"] = userSettings().cleanTexi2DviOutput();
   compilePdfPrefs["enable_shell_escape"] = userSettings().enableLaTeXShellEscape();

   // initialize and set result object
   json::Object result;
   result["general_prefs"] = generalPrefs;
   result["history_prefs"] = historyPrefs;
   result["packages_prefs"] = packagesPrefs;
   result["projects_prefs"] = projectsPrefs;
   result["source_control_prefs"] = sourceControlPrefs;
   result["compile_pdf_prefs"] = compilePdfPrefs;
   result["spelling_prefs_context"] =
                  session::modules::spelling::spellingPrefsContextAsJson();

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
   using namespace core::system::crypto;
   error = rsaPrivateDecrypt(passphrase, &passphrase);
   if (error)
      return error;
#endif

   // resolve key path
   FilePath sshKeyPath = module_context::resolveAliasedPath(path);
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

   core::system::setenv(&shellEnv, "GIT_EDITOR", s_editFileCommand);
   core::system::setenv(&shellEnv, "SVN_EDITOR", s_editFileCommand);

   // ammend shell paths as appropriate
   ammendShellPaths(&shellEnv);

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

   ptrProc->onExit().connect(boost::bind(
                              &source_control::enqueueRefreshEvent));

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
      (bind(registerRpcMethod, "create_ssh_key", createSshKey))
      (bind(registerRpcMethod, "start_shell_dialog", startShellDialog));
   return initBlock.execute();
}


} // namepsace workbench
} // namespace modules
} // namesapce session

