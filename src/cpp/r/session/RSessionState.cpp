/*
 * RSessionState.cpp
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

#include <r/session/RSessionState.hpp>

#include <algorithm>

#include <boost/function.hpp>
#include <boost/foreach.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
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

using namespace rstudio::core ;

namespace rstudio {
namespace r {
   
using namespace exec ;  
         
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

// settings
const char * const kWorkingDirectory = "working_directory";
const char * const kDevModeOn = "dev_mode_on";
const char * const kPackratModeOn = "packrat_mode_on";
const char * const kRProfileOnRestore = "r_profile_on_restore";

// is the suspended session state compatible with the active R version?
std::string s_activeRVersion;
std::string s_suspendedRVersion;
bool s_isCompatibleSessionState = true;

Error saveLibPaths(const FilePath& libPathsFile)
{
   std::string file = string_utils::utf8ToSystem(libPathsFile.absolutePath());
   return r::exec::RFunction(".rs.saveLibPaths", file).call();
}

Error restoreLibPaths(const FilePath& libPathsFile)
{
   if (!libPathsFile.exists())
      return Success();

   std::string file = string_utils::utf8ToSystem(libPathsFile.absolutePath());
   return r::exec::RFunction(".rs.restoreLibPaths", file).call();
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

Error saveEnvironmentVars(const FilePath& envFile)
{
   // remove then create settings file
   Error error = envFile.removeIfExists();
   if (error)
      return error;
   core::Settings envSettings;
   error = envSettings.initialize(envFile);
   if (error)
      return error;

   // get environment and write it to the file
   core::system::Options env;
   core::system::environment(&env);
   envSettings.beginUpdate();
   BOOST_FOREACH(const core::system::Option& var, env)
   {
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
   
Error restoreWorkingDirectory(const FilePath& userHomePath, 
                              const std::string& workingDirectory)
{
   // resolve working dir
   FilePath workingDirPath = FilePath::resolveAliasedPath(workingDirectory,
                                                          userHomePath);

   // restore working path if it exists (else revert to home)
   if (workingDirPath.exists())
      return workingDirPath.makeCurrentPath() ;
   else
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
   Error serializationError = error ;
   serializationError.addProperty("context", message);
   core::log::logError(serializationError, location);
   
   // notify end-user
   std::string report = message + ": " + error.code().message() + "\n";
   if (reportFunction)
      reportFunction(report.c_str());
   else
      REprintf(report.c_str());
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
   
   std::string* pMessages_ ;
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
      // ignore error on purpose -- will happen if devtools isn't intalled
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
   error = pSettings->initialize(statePath.complete(kSettingsFile));
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
   FilePath historyPath = statePath.complete(kHistoryFile);
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
   FilePath consoleActionsPath = statePath.complete(kConsoleActionsFile);
   error = consoleActions().saveToFile(consoleActionsPath);
   if (error)
   {
      reportError(kSaving, kConsoleActionsFile, error, ERROR_LOCATION);
      *pSaved = false;
   }
}

} // anonymous namespace
 
   

bool save(const FilePath& statePath,
          bool serverMode,
          bool excludePackages,
          bool disableSaveCompression)
{
   // initialize context
   Settings settings;
   bool saved = true;
   initSaveContext(statePath, &settings, &saved);
   
   // check and save packrat mode status
   bool packratModeOn = r::session::utils::isPackratModeOn();
   settings.set(kPackratModeOn, packratModeOn);

   // set r profile on restore (always run the .Rprofile in packrat mode)
   settings.set(kRProfileOnRestore, !excludePackages || packratModeOn);
   
   // save r version
   Error error = saveRVersion(statePath.complete(kRVersion));
   if (error)
   {
      reportError(kSaving, kRVersion, error, ERROR_LOCATION);
      saved = false;
   }

   // save environment variables
   error = saveEnvironmentVars(statePath.complete(kEnvironmentVars));
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
      error = graphics::plotManager().serialize(statePath.complete(kPlotsDir));
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
   error = saveLibPaths(statePath.complete(kLibPathsFile));
   if (error)
   {
      reportError(kSaving, kLibPathsFile, error, ERROR_LOCATION);
      saved = false;
   }

   // save options 
   error = r::options::saveOptions(statePath.complete(kOptionsFile));
   if (error)
   {
      reportError(kSaving, kOptionsFile, error, ERROR_LOCATION);
      saved = false;
   }
   
   // save working context
   saveWorkingContext(statePath, &settings, &saved);

   // save search path (disable save compression if requested)
   if (disableSaveCompression)
   {
      error = r::exec::RFunction(".rs.disableSaveCompression").call();
      if (error)
         LOG_ERROR(error);
   }

   if (!excludePackages)
   {
      error = search_path::save(statePath);
      if (error)
      {
         reportError(kSaving, kSearchPath, error, ERROR_LOCATION);
         saved = false;
      }
   }
   else
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
                 bool saveGlobalEnvironment)
{
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
   Settings settings ;
   Error error = settings.initialize(statePath.complete(kSettingsFile));
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

Error deferredRestore(const FilePath& statePath, bool serverMode)
{
   // search path
   Error error = search_path::restore(statePath, s_isCompatibleSessionState);
   if (error)
      return error;
   
   // if we are in server mode we just need to read the plots state
   // file (because the location of the graphics directory is stable)
   if (serverMode)
   {
      return graphics::plotManager().restorePlotsState();
   }
   else
   {
      FilePath plotsDir = statePath.complete(kPlotsDir);
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
      return Success();
   
   // read version from file
   std::string suspendedRVersion;
   error = core::readStringFromFile(
            filePath,
            &suspendedRVersion);
   if (error)
      return error;
   suspendedRVersion = core::string_utils::trimWhitespace(suspendedRVersion);
   s_suspendedRVersion = suspendedRVersion;
   
   // read active R version
   std::string activeRVersion;
   error = RFunction(".rs.rVersionString").call(&activeRVersion);
   if (error)
      return error;
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
   s_isCompatibleSessionState = validateRestoredRVersion(statePath.complete(kRVersion));
   
   // init session settings (used below)
   Settings settings ;
   error = settings.initialize(statePath.complete(kSettingsFile));
   if (error)
      reportError(kRestoring, kSettingsFile, error, ERROR_LOCATION, er);
   
   // restore console actions
   FilePath consoleActionsPath = statePath.complete(kConsoleActionsFile);
   error = consoleActions().loadFromFile(consoleActionsPath);
   if (error)
      reportError(kRestoring, kConsoleActionsFile, error, ERROR_LOCATION, er);
      
   // restore working directory
   std::string workingDir = settings.get(kWorkingDirectory);
   error = restoreWorkingDirectory(r::session::utils::userHomePath(), 
                                   workingDir);
   if (error)
      reportError(kRestoring, kWorkingDirectory, error, ERROR_LOCATION, er);
   
   // restore options
   FilePath optionsPath = statePath.complete(kOptionsFile);
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
         error = restoreLibPaths(statePath.complete(kLibPathsFile));
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
   FilePath historyFilePath = statePath.complete(kHistoryFile);
   error = consoleHistory().loadFromFile(historyFilePath, false);
   if (error)
      reportError(kRestoring, kHistoryFile, error, ERROR_LOCATION, er);

   // restore environment vars
   error = restoreEnvironmentVars(statePath.complete(kEnvironmentVars));
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
