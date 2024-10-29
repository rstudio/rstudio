/*
 * RSessionState.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <r/session/RSessionState.hpp>

#ifdef _WIN32
# include <fmt/xchar.h>
#endif

#include <unordered_set>

#include <boost/function.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Settings.hpp>
#include <core/Version.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RErrorCategory.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RGraphics.hpp>

#include "RClientMetrics.hpp"
#include "RSearchPath.hpp"
#include "graphics/RGraphicsPlotManager.hpp"

extern "C" {
RS_IMPORT bool R_Visible;
}

using namespace rstudio::core;

namespace rstudio {
namespace r {
   
using namespace exec;
         
namespace session {
namespace state {
  
namespace {
   
// file names
const char * const kSettingsFile = "settings";
const char * const kConsoleActionsFile = "console_actions";
const char * const kOptionsFile = "options";
const char * const kRVersion = "rversion";
const char * const kEnvironmentVars = "environment_vars";
const char * const kLibPathsFile = "libpaths";
const char * const kHistoryFile = "history";
const char * const kPlotsFile = "plots";
const char * const kPlotsDir = "plots_dir";
const char * const kSearchPath = "search_path";
const char * const kGlobalEnvironment = "global_environment";
const char * const kAfterRestartCommand = "after_restart_command";
const char * const kBuiltPackagePath = "built_package_path";

// settings
const char * const kWorkingDirectory = "working_directory";
const char * const kDevModeOn = "dev_mode_on";
const char * const kPackratModeOn = "packrat_mode_on";
const char * const kRProfileOnRestore = "r_profile_on_restore";

// is the suspended session state compatible with the active R version?
std::string s_activeRVersion;
std::string s_suspendedRVersion;
bool s_isCompatibleSessionState = true;

// session callbacks
SessionStateCallbacks s_callbacks;

Error saveLibPaths(const FilePath& libPathsFile)
{
   std::string file = string_utils::utf8ToSystem(libPathsFile.getAbsolutePath());
   return r::exec::RFunction(".rs.saveLibPaths", file).call();
}

Error restoreLibPaths(const FilePath& libPathsFile)
{
   if (!libPathsFile.exists())
      return Success();

   std::string file = string_utils::utf8ToSystem(libPathsFile.getAbsolutePath());
   return r::exec::RFunction(".rs.restoreLibPaths", file).call();
}

#ifdef _WIN32

// TODO: This probably belongs somewhere else.
bool isDirectoryLockedWin32(const std::wstring& filePath)
{
   WIN32_FIND_DATAW data;
   std::wstring findQuery = fmt::format(L"{}/*", filePath);
   HANDLE hFind = FindFirstFileW(findQuery.c_str(), &data);
   if (hFind == INVALID_HANDLE_VALUE)
      return false;

   do
   {
      // skip '.' and '..'
      bool isDotFolder =
            (wcscmp(data.cFileName, L".") == 0) ||
            (wcscmp(data.cFileName, L"..") == 0);

      if (isDotFolder)
         continue;

      // get the child path
      std::wstring childPath = fmt::format(L"{}/{}", filePath, data.cFileName);
      // REprintf("[!] Checking file: '%s'\n", string_utils::wideToUtf8(childPath).c_str());

      // check if we can open it with exclusive access
      HANDLE hFile = CreateFileW(
               childPath.c_str(),
               GENERIC_READ | GENERIC_WRITE, // Check for both read + write access to files
               0,                            // No sharing (FILE_SHARE_READ/FILE_SHARE_WRITE would allow shared access)
               NULL,                         // Default security
               OPEN_EXISTING,                // Open existing file only
               FILE_ATTRIBUTE_NORMAL,        // Normal file
               NULL);                        // No template file

      if (hFile == INVALID_HANDLE_VALUE)
      {
         DWORD dwError = GetLastError();
         if (dwError == ERROR_SHARING_VIOLATION)
         {
            // REprintf("[!] %s is locked.\n", string_utils::wideToUtf8(childPath).c_str());
            return true;
         }
      }

      // File is not locked, close the handle
      CloseHandle(hFile);

      // If this file is a directory, recurse through it
      if (data.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)
      {
         if (isDirectoryLockedWin32(childPath))
         {
            // REprintf("[!] %s is locked.\n", string_utils::wideToUtf8(childPath).c_str());
            return true;
         }
      }
   }
   while (FindNextFileW(hFind, &data) != 0);

   FindClose(hFind);

   // if we get here, then we iterated through all the files within
   // the directory without finding a locked file
   return false;
}

#endif

bool isDirectoryLocked(const FilePath& filePath)
{
#ifndef _WIN32
   return false;
#else
   return isDirectoryLockedWin32(filePath.getAbsolutePathW());
#endif
}

bool isRLocationVariable(const std::string& name)
{
  return name == "LD_LIBRARY_PATH" ||
         name == "R_HOME" ||
         name == "R_DOC_DIR" ||
         name == "R_INCLUDE_DIR" ||
         name == "R_SHARE_DIR";
}

Error saveRVersion(const FilePath& filePath)
{
   Error error;
   
   // remove pre-existing file
   error = filePath.removeIfExists();
   if (error)
      return error;
   
   // ask R for the current version
   std::string version;
   error = RFunction(".rs.rVersionString").call(&version);
   if (error)
      return error;
   
   // write to file
   error = core::writeStringToFile(filePath, version);
   if (error)
      return error;
   
   // success!
   return Success();
}

Error saveEnvironmentVars(const FilePath& envFile, const std::string& ephemeralEnvVars)
{
   // remove then create settings file
   Error error = envFile.removeIfExists();
   if (error)
      return error;
   core::Settings envSettings;
   error = envSettings.initialize(envFile);
   if (error)
      return error;

   // build set of excluded environment variables
   std::vector<std::string> envEphemeral(core::algorithm::split(ephemeralEnvVars, ":"));
   std::unordered_set<std::string> ephemeral(envEphemeral.begin(), envEphemeral.end());

   // get environment and write it to the file
   core::system::Options env;
   core::system::environment(&env);
   envSettings.beginUpdate();
   for (const core::system::Option& var : env)
   {
      if (ephemeral.count(var.first) == 0)
         envSettings.set(var.first, var.second);
   }
   envSettings.endUpdate();

   return Success();
}

void setEnvVar(const std::string& name, const std::string& value)
{
   // don't restore R location variables if we already have them
   if (isRLocationVariable(name) && !core::system::getenv(name).empty())
      return;

   // don't overwrite route lock if already supplied (may change on resume)
   if (name == "RSTUDIO_SESSION_ROUTE" && !core::system::getenv(name).empty())
      return;

   // don't restore program mode value (should be set by session main initialization)
   if (name == "RSTUDIO_PROGRAM_MODE" && !core::system::getenv(name).empty())
      return;

   // don't restore the version of this session (should be set by main session initialization)
   if (name == "RSTUDIO_VERSION" && !core::system::getenv(name).empty())
      return;

   // don't restore the pid of this session (should be set by main session initialization)
   if (name == "RSTUDIO_SESSION_PID" && !core::system::getenv(name).empty())
      return;

   // don't restore socket path environment variables (should be set by main session initialization)
   if (name == "RS_SERVER_RPC_SOCKET_PATH" && !core::system::getenv(name).empty())
      return;

   if (name == "RS_SERVER_TMP_DIR" && !core::system::getenv(name).empty())
      return;

   if (name == "RS_SERVER_LOCAL_SOCKET_PATH" && !core::system::getenv(name).empty())
      return;

   if (name == "RS_MONITOR_SOCKET_PATH" && !core::system::getenv(name).empty())
      return;

   if (name == "RS_SESSION_TMP_DIR" && !core::system::getenv(name).empty())
      return;

   // don't restore misc launcher environment that is set when the session is launched
   if (name == "RSTUDIO_STANDALONE_PORT" && !core::system::getenv(name).empty())
      return;

   if (name == "RSTUDIO_SESSION_RSA_PRIVATE_KEY" && !core::system::getenv(name).empty())
      return;

   if (name == "RSTUDIO_PANDOC" && !FilePath(value).exists())
      return;
   
   // each R session gets a unique temporary directory (set by R itself)
   if (name == "R_SESSION_TMPDIR" && !core::system::getenv(name).empty())
      return;

   core::system::setenv(name, value);
}

Error restoreEnvironmentVars(const FilePath& envFile)
{
   if (!envFile.exists())
      return Success();

   // read settings file
   core::Settings envSettings;
   Error error = envSettings.initialize(envFile);
   if (error)
      return error;

   // set the environment vars
   envSettings.forEach(setEnvVar);

   return Success();
}

Error getAfterRestartCommand(const FilePath& afterRestartFile,
                             std::string* pCommand,
                             bool* pIsEager)
{
   std::string command;
   Error error = core::readStringFromFile(afterRestartFile, &command);
   if (error && !isFileNotFoundError(error))
      LOG_ERROR(error);
   
   bool eager = false;
   if (command.find("@") == 0)
   {
      eager = true;
      command = command.substr(1);
   }
   
   *pCommand = command;
   *pIsEager = eager;
   return Success();
}

Error getBuiltPackagePath(const FilePath& builtPackagePathFile,
                          std::string* pBuiltPackagePath)
{
   return core::readStringFromFile(builtPackagePathFile, pBuiltPackagePath);
}


struct AfterRestartCommandData
{
   SEXP elSEXP;
   SEXP resultSEXP;
   bool visible;
};

void executeAfterRestartCommandImpl(void* payload)
{
   AfterRestartCommandData* data = (AfterRestartCommandData*) payload;
   data->resultSEXP = Rf_eval(data->elSEXP, R_GlobalEnv);
   data->visible = R_Visible;
}

Error executeAfterRestartCommand(const std::string& command)
{
   if (command.empty())
      return Success();
   
   // simulate evaluation of code as though it were submitted to the console
   // this implies printing expression results as they're evaluated
   // TODO: should this be moved to RExec.hpp?
   
   // first, print the code we're going to evaluate
   s_callbacks.consoleWriteInput(core::string_utils::trimWhitespace(command));
   
   // parse the code we're trying to evaluate
   r::sexp::Protect protect;
   SEXP parsedSEXP = R_NilValue;
   Error error = r::exec::RFunction("base:::parse")
         .addParam("text", command)
         .call(&parsedSEXP, &protect);
   
   // R will report parse errors to the console, so don't report those
   // errors -- just exit early
   if (error)
      return Success();
   
   // iterate over the available expressions, evaluate them, and
   // print their results if appropriate
   for (int i = 0, n = r::sexp::length(parsedSEXP); i < n; i++)
   {
      AfterRestartCommandData data;
      data.elSEXP = VECTOR_ELT(parsedSEXP, i);
      data.resultSEXP = R_NilValue;
      data.visible = false;

      // NOTE: we use R_ToplevelExec to ensure R longjmps from errors don't
      // escape this context, and also so that we can capture whether evaluation
      // sets the R_Visible flag (and so results should be printed)
      int success = R_ToplevelExec(executeAfterRestartCommandImpl, &data);
      if (success)
      {
         if (data.visible)
         {
            r::sexp::printValue(data.resultSEXP);
         }
      }
      else
      {
         break;
      }
   }
   
   return Success();
}
   
Error restoreWorkingDirectory(const std::string& workingDirectory,
                              const FilePath& projectPath,
                              const FilePath& userHomePath)
{
   // resolve working dir
   FilePath workingDirPath = FilePath::resolveAliasedPath(workingDirectory, userHomePath);
   if (workingDirPath.exists())
      return workingDirPath.makeCurrentPath();
   
   // if that didn't work, use project directory
   if (projectPath.isDirectory())
      return projectPath.makeCurrentPath();
   
   // otehrwise, use home directory
   return utils::userHomePath().makeCurrentPath();
}

const char * const kSaving = "saving";
const char * const kRestoring = "restoring";
const char * const kCleaningUp = "cleaning up";
   
void reportError(const std::string& action,
                 const std::string& context, 
                 const Error& error,
                 const ErrorLocation& location,
                 const boost::function<void(const char*)>& reportFunction = 
                                       boost::function<void(const char*)>())
{
   // build the message
   std::string message = "Error " + action + " session";
   if (!context.empty())
      message += std::string(" (" + context + ")");
   
   // add context to error and log it
   Error serializationError = error;
   serializationError.addProperty("context", message);
   core::log::logError(serializationError, location);
   
   // notify end-user
   std::string report = message + ": " + error.getMessage() + "\n";
   if (reportFunction)
      reportFunction(report.c_str());
   else
      REprintf("%s", report.c_str());
}

struct ErrorRecorder
{
   ErrorRecorder(std::string* pMessages)
      : pMessages_(pMessages)
   {
   }
   
   void operator()(const char* message)
   {
      pMessages_->operator+=(message);
   }
   
   std::string* pMessages_;
};

void saveDevMode(Settings* pSettings)
{
   // check if dev-mode is on -- if it is then note this and turn it off
   // (so that at restore time we can explicitly re-enable it)
   bool devModeOn = false;
   Error error = r::exec::RFunction(".rs.devModeOn").call(&devModeOn);
   if (error)
      LOG_ERROR(error);
   if (devModeOn)
   {
      // set devmode bit in suspended settings
      pSettings->set(kDevModeOn, true);

      // turn dev mode off -- this is important so that dev mode undoes
      // its manipulations of the prompt and libpaths before they are saved
      // suppress output to eliminate dev_mode OFF message
      // ignore error on purpose -- will happen if devtools isn't installed
      r::session::utils::SuppressOutputInScope suppressOutput;
      error = r::exec::RFunction("devtools:::dev_mode", false).call();
   }

}

void initSaveContext(const FilePath& statePath,
                     Settings* pSettings,
                     bool* pSaved)
{
   // ensure the context exists
   Error error = statePath.ensureDirectory();
   if (error)
   {
      reportError(kSaving, "creating directory", error, ERROR_LOCATION);
      *pSaved = false;
   }

   // init session settings
   error = pSettings->initialize(statePath.completePath(kSettingsFile));
   if (error)
   {
      reportError(kSaving, kSettingsFile, error, ERROR_LOCATION);
      *pSaved = false;
   }
}

void saveWorkingContext(const FilePath& statePath,
                        Settings* pSettings,
                        bool* pSaved)
{
   // save history
   FilePath historyPath = statePath.completePath(kHistoryFile);
   Error error = consoleHistory().saveToFile(historyPath);
   if (error)
   {
      reportError(kSaving, kHistoryFile, error, ERROR_LOCATION);
      *pSaved = false;
   }

   // save client metrics
   client_metrics::save(pSettings);

   // save aliased path to current working directory
   std::string workingDirectory = FilePath::createAliasedPath(
                                       utils::safeCurrentPath(),
                                       r::session::utils::userHomePath());
   pSettings->set(kWorkingDirectory, workingDirectory);

   // save console actions
   FilePath consoleActionsPath = statePath.completePath(kConsoleActionsFile);
   error = consoleActions().saveToFile(consoleActionsPath);
   if (error)
   {
      reportError(kSaving, kConsoleActionsFile, error, ERROR_LOCATION);
      *pSaved = false;
   }
}

Error saveAfterRestartCommand(const FilePath& afterRestartCommandPath,
                              const std::string& afterRestartCommand)
{
   return core::writeStringToFile(afterRestartCommandPath, afterRestartCommand);
}

Error saveBuiltPackagePath(const FilePath& builtPackagePathPath,
                           const std::string& builtPackagePath)
{
   return core::writeStringToFile(builtPackagePathPath, builtPackagePath);
}


} // anonymous namespace
 
void initialize(SessionStateCallbacks callbacks)
{
   s_callbacks = callbacks;
}

bool save(const FilePath& statePath,
          const std::string& afterRestartCommand,
          const std::string& builtPackagePath,
          bool serverMode,
          bool excludePackages,
          bool disableSaveCompression,
          bool saveGlobalEnvironment,
          const std::string& ephemeralEnvVars)
{
   // initialize context
   Error error;
   Settings settings;
   bool saved = true;
   initSaveContext(statePath, &settings, &saved);
   
   // check and save packrat mode status
   bool packratModeOn = r::session::utils::isPackratModeOn();
   settings.set(kPackratModeOn, packratModeOn);

   // set r profile on restore (always run the .Rprofile in packrat mode)
   settings.set(kRProfileOnRestore, !excludePackages || packratModeOn);
   
   // save after restart command
   error = saveAfterRestartCommand(statePath.completePath(kAfterRestartCommand), afterRestartCommand);
   if (error)
   {
      reportError(kSaving, kAfterRestartCommand, error, ERROR_LOCATION);
   }
   
   // save build library path
   error = saveBuiltPackagePath(statePath.completePath(kBuiltPackagePath), builtPackagePath);
   if (error)
   {
      reportError(kSaving, kBuiltPackagePath, error, ERROR_LOCATION);
   }
   
   // save r version
   error = saveRVersion(statePath.completePath(kRVersion));
   if (error)
   {
      reportError(kSaving, kRVersion, error, ERROR_LOCATION);
      saved = false;
   }

   // save environment variables
   error = saveEnvironmentVars(statePath.completePath(kEnvironmentVars), ephemeralEnvVars);
   if (error)
   {
      reportError(kSaving, kEnvironmentVars, error, ERROR_LOCATION);
      saved = false;
   }

   // if we are in server mode then we just need to write the plot
   // state index (because the location of the graphics directory is stable)
   if (serverMode)
   {
      error = graphics::plotManager().savePlotsState();
      if (error)
      {
         reportError(kSaving, kPlotsFile, error, ERROR_LOCATION);
         saved = false;
      }
   }
   else
   {
      error = graphics::plotManager().serialize(statePath.completePath(kPlotsDir));
      if (error)
      {
         reportError(kSaving, kPlotsDir, error, ERROR_LOCATION);
         saved = false;
      }
   }

   // handle dev mode -- note that this MUST be executed before
   // save libpaths and save options because it manipulates them
   // (by disabling devmode)
   saveDevMode(&settings);

   // save libpaths
   error = saveLibPaths(statePath.completePath(kLibPathsFile));
   if (error)
   {
      reportError(kSaving, kLibPathsFile, error, ERROR_LOCATION);
      saved = false;
   }

   // save options 
   error = r::options::saveOptions(statePath.completePath(kOptionsFile));
   if (error)
   {
      reportError(kSaving, kOptionsFile, error, ERROR_LOCATION);
      saved = false;
   }
   
   // save working context
   saveWorkingContext(statePath, &settings, &saved);

   // save search path (disable save compression if requested)
   if (saveGlobalEnvironment && disableSaveCompression)
   {
      error = r::exec::RFunction(".rs.disableSaveCompression").call();
      if (error)
         LOG_ERROR(error);
   }

   if (saveGlobalEnvironment && !excludePackages)
   {
      error = search_path::save(statePath);
      if (error)
      {
         reportError(kSaving, kSearchPath, error, ERROR_LOCATION);
         saved = false;
      }
   }
   else if (saveGlobalEnvironment)
   {
      error = search_path::saveGlobalEnvironment(statePath);
      if (error)
      {
         reportError(kSaving, kGlobalEnvironment, error, ERROR_LOCATION);
         saved = false;
      }
   }

   // return status
   return saved;
}

bool saveMinimal(const core::FilePath& statePath,
                 const std::string& afterRestartCommand,
                 const std::string& builtPackagePath,
                 bool saveGlobalEnvironment)
{
   Error error;

   // initialize context
   Settings settings;
   bool saved = true;
   initSaveContext(statePath, &settings, &saved);

   // set r profile on restore
   settings.set(kRProfileOnRestore, true);

   // save packrat mode
   settings.set(kPackratModeOn, r::session::utils::isPackratModeOn());

   // handle dev mode
   saveDevMode(&settings);
   
   // save after restart command
   error = saveAfterRestartCommand(statePath.completeChildPath(kAfterRestartCommand), afterRestartCommand);
   if (error)
      LOG_ERROR(error);

   // save build library path
   error = saveBuiltPackagePath(statePath.completePath(kBuiltPackagePath), builtPackagePath);
   if (error)
      LOG_ERROR(error);

   // save working context
   saveWorkingContext(statePath, &settings, &saved);

   // save global environment if requested
   if (saveGlobalEnvironment)
   {
      // disable save compression
      Error error = r::exec::RFunction(".rs.disableSaveCompression").call();
      if (error)
         LOG_ERROR(error);

      error = search_path::saveGlobalEnvironment(statePath);
      if (error)
      {
         reportError(kSaving, kGlobalEnvironment, error, ERROR_LOCATION);
         saved = false;
      }
   }

   // return status
   return saved;
}

namespace {

bool getBoolSetting(const core::FilePath& statePath,
                    const std::string& name,
                    bool defaultValue)
{
   Settings settings;
   Error error = settings.initialize(statePath.completePath(kSettingsFile));
   if (error)
   {
      LOG_ERROR(error);
      return defaultValue;
   }

   return settings.getBool(name, defaultValue);
}

} // anonymous namespace

bool rProfileOnRestore(const core::FilePath& statePath)
{
   return getBoolSetting(statePath, kRProfileOnRestore, true);
}

bool packratModeEnabled(const core::FilePath& statePath)
{
   return getBoolSetting(statePath, kPackratModeOn, false);
}

namespace {

void useBuiltPackagePath(const FilePath& srcPath)
{
   // if we were unable to move the library path for some reason
   // (on Windows, this might be because a different R process is using that package still)
   // then just add the build library path to the libpaths for this session, and notify user
   //
   // this way, they can still use the installed package in that session, even if
   // it's not going to be available for other RStudio sessions
   Error error = r::exec::RFunction(".rs.prependLibraryPath")
         .addUtf8Param(srcPath.getParent())
         .call();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   FilePath homePath = core::system::userHomePath("R_USER|HOME");
   std::string aliasedPath = FilePath::createAliasedPath(srcPath.getParent(), homePath);
   REprintf(
            "\n"
            "- RStudio was unable to move '%s' into your package library.\n"
            "- '%s' will be loaded from the following library path for this session:\n"
            "- \"%s\"\n"
            "\n",
            srcPath.getFilename().c_str(),
            srcPath.getFilename().c_str(),
            aliasedPath.c_str());
}

Error finishPackageInstall(const std::string& builtPackagePath)
{
   Error error;

   // nothing to do if we have no library path
   if (builtPackagePath.empty())
      return Success();

   // check that the path exists; we just installed the package
   // to this directory so it would be surprising if it's suddenly gone!
   FilePath srcPath(builtPackagePath);
   if (!srcPath.exists())
      return fileNotFoundError(srcPath, ERROR_LOCATION);

   // the target library path is the parent of the source library path
   FilePath tgtPath(srcPath.getParent().getParent().completeChildPath(srcPath.getFilename()));

   // that path might already exist (e.g. because the package was previously installed)
   // move that directory out of the way. note that this can succeed on Windows even
   // if those files are open / in use by another application
   std::string backupName = fmt::format("{}-{}", tgtPath.getFilename(), core::system::generateShortenedUuid());
   FilePath backupDir = tgtPath.getParent().completeChildPath("_backup");
   error = backupDir.ensureDirectory();
   if (error)
      LOG_ERROR(error);

   FilePath backupPath = backupDir.completeChildPath(backupName);
   error = tgtPath.move(backupPath, core::FilePath::MoveDirect, true);
   if (error && !isFileNotFoundError(error))
   {
      useBuiltPackagePath(srcPath);
      return error;
   }

   // we can now move the installed package to its final location
   error = srcPath.move(tgtPath, core::FilePath::MoveDirect, true);
   if (error)
   {
      Error restoreError = backupPath.move(tgtPath, core::FilePath::MoveDirect, true);
      if (restoreError)
         LOG_ERROR(restoreError);

      useBuiltPackagePath(srcPath);
      return error;
   }

   // Take this opportunity to try and remove any folders within the backup directory.
   std::vector<FilePath> backupPaths;
   error = backupDir.getChildren(backupPaths);
   if (error)
      LOG_ERROR(error);

   for (const FilePath& backupPath : backupPaths)
   {
      if (!isDirectoryLocked(backupPath))
      {
         Error error = backupPath.remove();
         if (error)
            LOG_ERROR(error);
      }
   }

   return Success();
}

void removeBuildLibraryPath(const std::string& buildLibraryPath)
{
   Error error = r::exec::RFunction(".rs.removeBuildLibraryPath")
         .addUtf8Param(buildLibraryPath)
         .call();
   if (error)
      LOG_ERROR(error);
}

} // end anonymous namespace

Error deferredRestore(const FilePath& statePath, bool serverMode)
{
   Error error;

   // get current search path list -- need to do this before executing
   // the after restart command as that might load a package and modify
   // the search list
   std::vector<std::string> currentSearchPathList;
   error = r::exec::RFunction("base:::search").call(&currentSearchPathList);
   if (error)
      return error;
   
   // if we installed a package into a custom library path,
   // pull it out into the main library now
   std::string builtPackagePath;
   error = getBuiltPackagePath(statePath.completePath(kBuiltPackagePath), &builtPackagePath);
   if (error && !isFileNotFoundError(error))
      LOG_ERROR(error);

   Error installError = finishPackageInstall(builtPackagePath);
   if (installError)
      LOG_ERROR(error);

   // execute after restart command
   std::string command;
   bool isEagerCommand = false;
   error = getAfterRestartCommand(
            statePath.completePath(kAfterRestartCommand),
            &command,
            &isEagerCommand);
   if (error)
      LOG_ERROR(error);
   
   // execute eager (non-deferred) restart commands
   if (isEagerCommand)
   {
      Error error = executeAfterRestartCommand(command);
      if (error)
         LOG_ERROR(error);

      removeBuildLibraryPath(
               FilePath(builtPackagePath).getParent().getAbsolutePath());
   }
   
   {
      // restore search path
      utils::SuppressOutputInScope suppressOutput;
      error = search_path::restore(statePath, currentSearchPathList, s_isCompatibleSessionState);
      if (error)
         return error;
   }
   
   // execute deferred restart commands
   if (!isEagerCommand)
   {
      Error error = executeAfterRestartCommand(command);
      if (error)
         LOG_ERROR(error);

      removeBuildLibraryPath(
               FilePath(builtPackagePath).getParent().getAbsolutePath());
   }
   
   
   // if we are in server mode we just need to read the plots state
   // file (because the location of the graphics directory is stable)
   if (serverMode)
   {
      utils::SuppressOutputInScope suppressOutput;
      return graphics::plotManager().restorePlotsState();
   }
   else
   {
      utils::SuppressOutputInScope suppressOutput;
      FilePath plotsDir = statePath.completePath(kPlotsDir);
      if (plotsDir.exists())
         return graphics::plotManager().deserialize(plotsDir);
      else
         return Success();
   }
}

namespace {

bool validateRestoredRVersion(const FilePath& filePath)
{
   Error error;
   
   // assume we're okay if no file exists
   if (!filePath.exists())
      return !!Success();
   
   // read version from file
   std::string suspendedRVersion;
   error = core::readStringFromFile(
            filePath,
            &suspendedRVersion);
   if (error)
      return !!error;
   suspendedRVersion = core::string_utils::trimWhitespace(suspendedRVersion);
   s_suspendedRVersion = suspendedRVersion;
   
   // read active R version
   std::string activeRVersion;
   error = RFunction(".rs.rVersionString").call(&activeRVersion);
   if (error)
      return !!error;
   activeRVersion = core::string_utils::trimWhitespace(activeRVersion);
   s_activeRVersion = activeRVersion;
   
   // construct and compare versions
   core::Version suspended(suspendedRVersion);
   core::Version active(activeRVersion);
   
   // if both major and minor versions are equal, we're okay
   return
         suspended.versionMajor() == active.versionMajor() &&
         suspended.versionMinor() == active.versionMinor();
}

} // end anonymous namespace
   
bool restore(const FilePath& statePath,
             bool serverMode,
             boost::function<Error()>* pDeferredRestoreAction,
             std::string* pErrorMessages)
{
   Error error;
   
   // setup error buffer
   ErrorRecorder er(pErrorMessages);
   
   // detect incompatible r version (don't restore some parts of session state
   // in this case as it could cause a crash on startup)
   s_isCompatibleSessionState = validateRestoredRVersion(statePath.completePath(kRVersion));
   
   // init session settings (used below)
   Settings settings;
   error = settings.initialize(statePath.completePath(kSettingsFile));
   if (error)
      reportError(kRestoring, kSettingsFile, error, ERROR_LOCATION, er);
   
   // restore console actions
   FilePath consoleActionsPath = statePath.completePath(kConsoleActionsFile);
   error = consoleActions().loadFromFile(consoleActionsPath);
   if (error)
      reportError(kRestoring, kConsoleActionsFile, error, ERROR_LOCATION, er);
      
   // restore working directory
   std::string workingDir = settings.get(kWorkingDirectory);
   error = restoreWorkingDirectory(
            workingDir,
            r::session::utils::projectPath(),
            r::session::utils::userHomePath());
   
   if (error)
      reportError(kRestoring, kWorkingDirectory, error, ERROR_LOCATION, er);
   
   // restore options
   FilePath optionsPath = statePath.completePath(kOptionsFile);
   if (optionsPath.exists())
   {
      error = r::options::restoreOptions(optionsPath);
      if (error)
         reportError(kRestoring, kOptionsFile, error, ERROR_LOCATION, er);
   }
      
   if (s_isCompatibleSessionState)
   {
      // restore libpaths -- but only if packrat mode is off
      bool packratModeOn = settings.getBool(kPackratModeOn, false);
      if (!packratModeOn)
      {
         error = restoreLibPaths(statePath.completePath(kLibPathsFile));
         if (error)
            reportError(kRestoring, kLibPathsFile, error, ERROR_LOCATION, er);
      }

      // restore devmode
      if (settings.getBool(kDevModeOn, false))
      {
         // ignore error -- will occur if devtools isn't installed
         error = r::exec::RFunction("devtools:::dev_mode", true).call();
      }
   }

   // restore client_metrics (must execute after restore of options for
   // console width but prior to graphics::device for device size)
   client_metrics::restore(settings);

   // restore history
   FilePath historyFilePath = statePath.completePath(kHistoryFile);
   error = consoleHistory().loadFromFile(historyFilePath, false);
   if (error)
      reportError(kRestoring, kHistoryFile, error, ERROR_LOCATION, er);

   // restore environment vars
   error = restoreEnvironmentVars(statePath.completePath(kEnvironmentVars));
   if (error)
      reportError(kRestoring, kEnvironmentVars, error, ERROR_LOCATION, er);
   
   // set deferred restore action. this encapsulates parts of the restore
   // process that are potentially highly latent. this allows clients
   // to bring their UI up and then receive an event indicating that the
   // latent deserialization actions are taking place
   *pDeferredRestoreAction = boost::bind(deferredRestore, statePath, serverMode);
   
   // return true if there were no error messages
   return pErrorMessages->empty();
}

bool destroy(const FilePath& statePath)
{
   Error error = statePath.removeIfExists();
   if (error)
   {
      reportError(kCleaningUp, "", error, ERROR_LOCATION);
      return false;
   }
   else
   {
      return true;
   }
}

SessionStateInfo getSessionStateInfo()
{
   SessionStateInfo info;
   info.suspendedRVersion = core::Version(s_suspendedRVersion);
   info.activeRVersion = core::Version(s_activeRVersion);
   return info;
}

} // namespace state
} // namespace session   
} // namespace r
} // namespace rstudio
