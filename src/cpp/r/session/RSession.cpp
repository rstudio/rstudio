/*
 * RSession.cpp
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

#define R_INTERNAL_FUNCTIONS
#include <r/session/RSession.hpp>

#include <iostream>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/Scope.hpp>
#include <core/system/Architecture.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/FileSerializer.hpp>
#include <core/FileUtils.hpp>
#include <core/http/Util.hpp>
#include <core/http/URL.hpp>

#include <r/RCntxt.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RExec.hpp>
#include <r/RFunctionHook.hpp>
#include <r/RInterface.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RSourceManager.hpp>
#include <r/RUtil.hpp>
#include <r/session/RSessionState.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RDiscovery.hpp>
#include <r/session/RSuspend.hpp>

#include "RClientMetrics.hpp"
#include "REmbedded.hpp"
#include "RInit.hpp"
#include "RQuit.hpp"
#include "RRestartContext.hpp"
#include "RStdCallbacks.hpp"
#include "RScriptCallbacks.hpp"

#include "graphics/RGraphicsDevDesc.hpp"
#include "graphics/RGraphicsUtils.hpp"
#include "graphics/RGraphicsDevice.hpp"
#include "graphics/RGraphicsPlotManager.hpp"

#include <Rembedded.h>
#include <R_ext/Utils.h>
#include <R_ext/Rdynload.h>
#include <R_ext/RStartup.h>

#include <gsl/gsl-lite.hpp>

#define CTXT_BROWSER 16

// get rid of windows TRUE and FALSE definitions
#undef TRUE
#undef FALSE

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

namespace {

// options
ROptions s_options;

} // anonymous namespace
  

const int kSerializationActionSaveDefaultWorkspace = 1;
const int kSerializationActionLoadDefaultWorkspace = 2;
const int kSerializationActionSuspendSession = 3;
const int kSerializationActionResumeSession = 4;
const int kSerializationActionCompleted = 5;

SEXP rs_editFile(SEXP fileSEXP)
{
   try
   {
      std::string file = r::sexp::asString(fileSEXP);
      bool success = REditFile(file.c_str()) == 0;
      r::sexp::Protect rProtect;
      return r::sexp::create(success, &rProtect);
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION

   // keep compiler happy (this code is unreachable)
   return R_NilValue;
}

SEXP rs_showFile(SEXP titleSEXP, SEXP fileSEXP, SEXP delSEXP)
{
   try
   {
      std::string file = r::util::fixPath(r::sexp::asString(fileSEXP));
      FilePath filePath = utils::safeCurrentPath().completePath(file);
      if (!filePath.exists())
      {
          throw r::exec::RErrorException(
                             "File " + file + " does not exist.");
      }

      rCallbacks().showFile(r::sexp::asString(titleSEXP),
                           filePath,
                           r::sexp::asLogical(delSEXP));
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

// method called from browseUrl
SEXP rs_browseURL(SEXP urlSEXP)
{
   try
   {
      std::string URL = r::sexp::asString(urlSEXP);

      // file urls require special dispatching
      std::string filePrefix("file://");
      if (URL.find(filePrefix) == 0)
      {
         // also look for file:///c: style urls on windows
#ifdef _WIN32
         if (URL.find(filePrefix + "/") == 0)
             filePrefix = filePrefix + "/";
#endif

         // transform into FilePath
         std::string path = URL.substr(filePrefix.length());
         path = core::http::util::urlDecode(path);
         FilePath filePath(r::util::fixPath(path));

         // sometimes R passes short paths (like for files within the
         // R home directory). Convert these to long paths
#ifdef _WIN32
         core::system::ensureLongPath(&filePath);
#endif

         // fire browseFile
         rCallbacks().browseFile(filePath);
      }
      // urls with no protocol are assumed to be file references
      else if (URL.find("://") == std::string::npos)
      {
         std::string file = r::util::expandFileName(URL);
         FilePath filePath = utils::safeCurrentPath().completePath(
            r::util::fixPath(file));
         rCallbacks().browseFile(filePath);
      }
      else
      {
         rCallbacks().browseURL(URL);
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   return R_NilValue;
}

SEXP rs_createUUID()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(core::system::generateUuid(false), &rProtect);
}
   
SEXP rs_loadHistory(SEXP sFile)
{
   std::string file = R_ExpandFileName(r::sexp::asString(sFile).c_str());
   Error error = consoleHistory().loadFromFile(FilePath(file), true);
   if (error)
      LOG_ERROR(error);
   else
      rCallbacks().consoleHistoryReset();
   return R_NilValue;
}

SEXP rs_saveHistory(SEXP sFile)
{
   std::string file = R_ExpandFileName(r::sexp::asString(sFile).c_str());
   consoleHistory().saveToFile(FilePath(file));
   return R_NilValue;
}


SEXP rs_completeUrl(SEXP url, SEXP path)
{
   std::string completed = rstudio::core::http::URL::complete(
      r::sexp::asString(url), r::sexp::asString(path));

   r::sexp::Protect rProtect;
   return r::sexp::create(completed, &rProtect);
}

namespace {

SEXP rs_GEcopyDisplayList(SEXP fromDeviceSEXP)
{
   int fromDevice = r::sexp::asInteger(fromDeviceSEXP);
   GEcopyDisplayList(fromDevice);
   return Rf_ScalarLogical(1);
}

SEXP rs_GEplayDisplayList()
{
   graphics::device::playDisplayList();
   return Rf_ScalarLogical(1);
}

#ifdef __APPLE__

Error validateCompatible(const std::string& rHome)
{
   FilePath rsessionPath;
   Error error = core::system::executablePath(nullptr, &rsessionPath);
   if (error)
   {
      LOG_ERROR(error);
      return Success();
   }

   FilePath rLibPath = FilePath(rHome).completeChildPath("lib/libR.dylib");
   if (!rLibPath.exists())
   {
      LOG_ERROR(fileNotFoundError(rLibPath, ERROR_LOCATION));
      return Success();
   }

   std::string rsessionArchs = core::system::supportedArchitectures(rsessionPath);
   std::string rArchs = core::system::supportedArchitectures(rLibPath);

   for (auto arch : { "x86_64", "arm64" })
   {
      if (rsessionArchs.find(arch) != std::string::npos &&
          rArchs.find(arch) != std::string::npos)
      {
         return Success();
      }
   }

   Error formatError(boost::system::errc::executable_format_error, ERROR_LOCATION);
   formatError.addProperty("r-home", rHome);
   formatError.addProperty("r-archs", rArchs);
   formatError.addProperty("rsession-archs", rsessionArchs);
   return formatError;

}

#endif

} // end anonymous namespace
   
Error run(const ROptions& options, const RCallbacks& callbacks) 
{   
   // copy options and callbacks
   s_options = options;
   setRCallbacks(callbacks);
   
   // set to default "C" numeric locale as-per R embedding docs
   setlocale(LC_NUMERIC, "C");
   
   // perform R discovery
   r::session::RLocations rLocations;
   Error error = r::session::discoverR(&rLocations);
   if (error)
      return error;

   // R_HOME
   core::system::setenv("R_HOME", rLocations.homePath);
   
   // R_DOC_DIR (required by help-links.sh)
   core::system::setenv("R_DOC_DIR", rLocations.docPath);

   // R_LIBS_USER
   if (!s_options.rLibsUser.empty())
      core::system::setenv("R_LIBS_USER", s_options.rLibsUser);

#ifdef __APPLE__
   // validate compatible architecture
   error = validateCompatible(rLocations.homePath);
   if (error)
      return error;
#endif
   
   // set compatible graphics engine version
   int engineVersion = s_options.rCompatibleGraphicsEngineVersion;
   graphics::setCompatibleEngineVersion(engineVersion);

   // set source reloading behavior
   sourceManager().setAutoReload(options.autoReloadSource);
   
   // initialize suspended session path
   FilePath userScratch = s_options.userScratchPath;
   FilePath oldSuspendedSessionPath = userScratch.completePath("suspended-session");
   FilePath sessionScratch = s_options.sessionScratchPath;

   // set suspend paths
   setSuspendPaths(
      sessionScratch.completePath("suspended-session-data"),              // session data
      s_options.userScratchPath.completePath("client-state"),             // client state
      s_options.scopedScratchPath.completePath("pcs"));                   // project client state

   // one time migration of global suspend to default project suspend
   if (!suspendedSessionPath().exists() && oldSuspendedSessionPath.exists())
   {
     // try to move it first
     Error error = oldSuspendedSessionPath.move(suspendedSessionPath());
     if (error)
     {
        // log the move error
        LOG_ERROR(error);

        // try to copy it as a failsafe (eliminates cross-volume issues)
        error = file_utils::copyDirectory(oldSuspendedSessionPath,
                                                suspendedSessionPath());
        if (error)
           LOG_ERROR(error);

        // remove so this is always a one-time only thing
        error = oldSuspendedSessionPath.remove();
        if (error)
           LOG_ERROR(error);
     }
   }

   // initialize restart context
   restartContext().initialize(s_options.scopedScratchPath,
                               s_options.sessionPort);

   // register methods
   RS_REGISTER_CALL_METHOD(rs_browseURL);
   RS_REGISTER_CALL_METHOD(rs_editFile);
   RS_REGISTER_CALL_METHOD(rs_showFile);
   RS_REGISTER_CALL_METHOD(rs_createUUID);
   RS_REGISTER_CALL_METHOD(rs_loadHistory);
   RS_REGISTER_CALL_METHOD(rs_saveHistory);
   RS_REGISTER_CALL_METHOD(rs_completeUrl);
   RS_REGISTER_CALL_METHOD(rs_GEcopyDisplayList, 1);
   RS_REGISTER_CALL_METHOD(rs_GEplayDisplayList, 0);

   // run R

   // should we run .Rprofile?
   bool loadInitFile = false;
   if (restartContext().hasSessionState())
   {
      loadInitFile = restartContext().rProfileOnRestore() && !options.disableRProfileOnStart;
   }
   else
   {
      // we run the .Rprofile if this is a brand new session and
      // we are in a project and the DisableExecuteProfile setting is not set, or we are not in a project
      // alternatively, if we are resuming a session and the option is set to possibly run the .Rprofile
      // we will only run it if the DisableExecuteProfile project setting is not set (or we are not in a project)
      // finally, if this is a packrat project, we always run the Rprofile as it is required for correct operation
      loadInitFile = (!options.disableRProfileOnStart && (!suspendedSessionPath().exists() || options.rProfileOnResume))
                     || options.packratEnabled
                     || r::session::state::packratModeEnabled(suspendedSessionPath());
   }

   // quiet for resume cases
   bool quiet = restartContext().hasSessionState() ||
                suspendedSessionPath().exists();

   r::session::Callbacks cb;
   if (options.runScript.empty())
   {
      // normal session: read/write from browser
      cb.readConsole = RReadConsole;
      cb.writeConsoleEx = RWriteConsoleEx;
      cb.cleanUp = RCleanUp;
      cb.resetConsole = RResetConsole;
   }
   else
   {
      // headless script execution: read/write from script and output to stdout
      setRunScript(options.runScript);
      cb.readConsole = RReadScript;
      cb.writeConsoleEx = RWriteStdout;
      cb.cleanUp = RScriptCleanUp;
   }

   cb.showMessage = RShowMessage;
   cb.editFile = REditFile;
   cb.busy = RBusy;
   cb.chooseFile = RChooseFile;
   cb.showFiles = RShowFiles;
   cb.loadhistory = Rloadhistory;
   cb.savehistory = Rsavehistory;
   cb.addhistory = Raddhistory;
   cb.suicide = RSuicide;
   r::session::runEmbeddedR(FilePath(rLocations.homePath),
                            options.userHomePath,
                            quiet,
                            loadInitFile,
                            s_options.saveWorkspace,
                            cb,
                            stdInternalCallbacks());

   // keep compiler happy
   return Success();
}

namespace {

void doSetClientMetrics(const RClientMetrics& metrics)
{
   // set the metrics
   client_metrics::set(metrics);
}
   
} // anonymous namespace
   
void setClientMetrics(const RClientMetrics& metrics)
{
   // get existing values in case this results in an error
   RClientMetrics previousMetrics = client_metrics::get();
   
   // attempt to set the metrics
   Error error = r::exec::executeSafely(boost::bind(doSetClientMetrics, 
                                                    metrics));
   
   if (error)
   {
      // report to user
      std::string errMsg = r::endUserErrorMessage(error);
      REprintf("%s\n", errMsg.c_str());

      // restore previous values (but don't fire plotsChanged b/c
      // the reset doesn't result in a change in graphics state)
      r::exec::executeSafely(boost::bind(doSetClientMetrics, previousMetrics));
   }
}

void reportWarningToConsole(const std::string& warning)
{
   std::string msg = "WARNING: " + warning + "\n";
   RWriteConsoleEx(msg.c_str(), gsl::narrow_cast<int>(msg.length()), 1);
}

void reportAndLogWarning(const std::string& warning)
{
   std::string msg = "WARNING: " + warning + "\n";
   RWriteConsoleEx(msg.c_str(), gsl::narrow_cast<int>(msg.length()), 1);
   LOG_WARNING_MESSAGE("(Reported to User) " + warning);
}

bool isSuspendable(const std::string& currentPrompt)
{
   // NOTE: active file graphics devices (e.g. png or pdf) are wiped out
   // during a suspend as are open connections. there may or may not be a
   // way to make this more robust.
   
   if (s_options.suspendOnIncompleteStatement)
   {
      // Always allow suspending, even if the statement is not complete.
      return true;
   }
   else
   {
      // Avoid suspending when the prompt is not at its default value.  This mostly prevents us from
      // suspending if the user hasn't finished an R statement, since R's prompt changes from > to +
      // when a statement is incomplete. It is an option since some environments prefer more
      // aggressive suspension behavior.
      std::string defaultPrompt = r::options::getOption<std::string>("prompt");
      if (currentPrompt != defaultPrompt)
      {
         return false;
      }
   }
    
   return true;
}


bool browserContextActive()
{
   using namespace r::context;
   for (auto it = RCntxt::begin(); it != RCntxt::end(); ++it)
   {
      if (it->callflag() & CTXT_BROWSER)
      {
         return true;
      }
   }
   
   return false;
}
   
namespace utils {
   
bool isPackratModeOn()
{
   return !core::system::getenv("R_PACKRAT_MODE").empty();
}

bool isDevtoolsDevModeOn()
{
   bool isDevtoolsDevModeOn = false;
   Error error = r::exec::RFunction(".rs.devModeOn").call(&isDevtoolsDevModeOn);
   if (error)
      LOG_ERROR(error);
   return isDevtoolsDevModeOn;
}

bool isDefaultPrompt(const std::string& prompt)
{
   return prompt == r::options::getOption<std::string>("prompt");
}

bool isServerMode()
{
   return s_options.serverMode;
}

const FilePath& projectPath()
{
   return s_options.projectPath;
}

const FilePath& userHomePath()
{
   return s_options.userHomePath;
}

FilePath logPath()
{
   return s_options.logPath;
}

FilePath sessionScratchPath()
{
   return s_options.sessionScratchPath;
}

FilePath scopedScratchPath()
{
   return s_options.scopedScratchPath;
}

FilePath safeCurrentPath()
{
   return FilePath::safeCurrentPath(userHomePath());
}

FilePath rHistoryDir()
{
   return s_options.rHistoryDir();
}

FilePath rEnvironmentDir()
{
   return s_options.rEnvironmentDir();
}

FilePath startupEnvironmentFilePath()
{
   return s_options.startupEnvironmentFilePath;
}

FilePath rSourcePath()
{
   return s_options.rSourcePath;
}

bool restoreWorkspace()
{
   return s_options.restoreWorkspace;
}

std::string sessionPort()
{
   return s_options.sessionPort;
}

std::string rCRANUrl()
{
   return s_options.rCRANUrl;
}

std::string rCRANSecondary()
{
   return s_options.rCRANSecondary;
}

bool alwaysSaveHistory()
{
   return s_options.alwaysSaveHistory();
}

bool restoreEnvironmentOnResume()
{
   return s_options.restoreEnvironmentOnResume;
}

FilePath tempFile(const std::string& prefix, const std::string& extension)
{
   std::string filename;
   Error error = r::exec::RFunction("tempfile", prefix).call(&filename);
   if (error)
      LOG_ERROR(error);
   FilePath filePath(string_utils::systemToUtf8(r::util::fixPath(filename)) + "." + extension);
   return filePath;
}

namespace {

FilePath tempDirImpl()
{
   return FilePath(
       string_utils::systemToUtf8(
           r::util::fixPath(R_TempDir)));
}

} // end anonymous namespace

FilePath tempDir()
{
   static FilePath instance = tempDirImpl();
   return instance;
}

} // namespace utils
   
} // namespace session
} // namespace r
} // namespace rstudio
