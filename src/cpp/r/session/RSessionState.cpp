/*
 * RSessionState.cpp
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

#include "RSessionState.hpp"

#include <algorithm>

#include <boost/function.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/Log.hpp>
#include <core/FileSerializer.hpp>

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

using namespace core ;

namespace r {
   
using namespace exec ;  
         
namespace session {
namespace state {
  
namespace {
   
// file names
const char * const kSettingsFile = "settings";
const char * const kConsoleActionsFile = "console_actions";
const char * const kOptionsFile = "options";
const char * const kHistoryFile = "history";
const char * const kPlotsFile = "plots";
const char * const kPlotsDir = "plots_dir";
const char * const kSearchPath = "search_path";

// settings
const char * const kWorkingDirectory = "working_directory"; 
   
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
   
} // anonymous namespace
 
   
bool save(const FilePath& statePath, bool serverMode)
{
   // flag indicating whether we succeeded saving
   bool saved = true;
   
   // ensure the context exists (if we fail this is fatal)
   Error error = statePath.ensureDirectory();
   if (error)
   {
      reportError(kSaving, "creating directory", error, ERROR_LOCATION);
      saved = false;
   }   
      
   // init session settings (used below)
   Settings settings ;
   error = settings.initialize(statePath.complete(kSettingsFile));
   if (error)
   {
      reportError(kSaving, kSettingsFile, error, ERROR_LOCATION);
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

   // save options 
   error = r::options::saveOptions(statePath.complete(kOptionsFile));
   if (error)
   {
      reportError(kSaving, kOptionsFile, error, ERROR_LOCATION);
      saved = false;
   }
   
   // save history
   FilePath historyPath = statePath.complete(kHistoryFile);
   error = consoleHistory().saveToFile(historyPath);
   if (error)
   {
      reportError(kSaving, kHistoryFile, error, ERROR_LOCATION);
      saved = false;
   }

   // save client metrics
   client_metrics::save(&settings);
   
   // save aliased path to current working directory
   std::string workingDirectory = FilePath::createAliasedPath(
                                       utils::safeCurrentPath(),
                                       r::session::utils::userHomePath());
   settings.set(kWorkingDirectory, workingDirectory);
   
   // save console actions
   FilePath consoleActionsPath = statePath.complete(kConsoleActionsFile);
   error = consoleActions().saveToFile(consoleActionsPath);
   if (error)
   {
      reportError(kSaving, kConsoleActionsFile, error, ERROR_LOCATION);
      saved = false;
   }
   
   // save search path
   error = search_path::save(statePath);
   if (error)
   {
      reportError(kSaving, kSearchPath, error, ERROR_LOCATION);
      saved = false;
   }

   // return status
   return saved;
}

Error deferredRestore(const FilePath& statePath, bool serverMode)
{
   // search path
   Error error = search_path::restore(statePath);
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
   
bool restore(const FilePath& statePath,
             bool serverMode,
             boost::function<Error()>* pDeferredRestoreAction,
             std::string* pErrorMessages)
{
   // setup error buffer
   ErrorRecorder er(pErrorMessages);
   
   // init session settings (used below)
   Settings settings ;
   Error error = settings.initialize(statePath.complete(kSettingsFile));
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
   error = r::options::restoreOptions(statePath.complete(kOptionsFile));
   if (error)
      reportError(kRestoring, kOptionsFile, error, ERROR_LOCATION, er);
   
   // restore client_metrics (must execute after restore of options for
   // console width but prior to graphics::device for device size)
   client_metrics::restore(settings);

   // restore history
   FilePath historyFilePath = statePath.complete(kHistoryFile);
   error = consoleHistory().loadFromFile(historyFilePath, false);
   if (error)
      reportError(kRestoring, kHistoryFile, error, ERROR_LOCATION, er);

   // set deferred restore action. this encapsulates parts of the restore
   // process that are potentially highly latent. this allows clients
   // to bring their UI up and then receive an event indicating that the
   // latent deserialization actions are taking place
   *pDeferredRestoreAction = boost::bind(deferredRestore,
                                             statePath, serverMode);
   
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
   
   
      
} // namespace state
} // namespace session   
} // namespace r
