/*
 * RSession.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#define R_INTERNAL_FUNCTIONS
#include <r/session/RSession.hpp>

#include <iostream>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/replace.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/Scope.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/FileSerializer.hpp>
#include <core/FileUtils.hpp>
#include <core/http/Util.hpp>

#include <r/RExec.hpp>
#include <r/RUtil.hpp>
#include <r/RErrorCategory.hpp>
#include <r/ROptions.hpp>
#include <r/RSourceManager.hpp>
#include <r/RRoutines.hpp>
#include <r/RInterface.hpp>
#include <r/RFunctionHook.hpp>
#include <r/session/RSessionState.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RGraphics.hpp>
#include <r/session/RDiscovery.hpp>

#include "RClientMetrics.hpp"
#include "REmbedded.hpp"
#include "RQuit.hpp"
#include "RRestartContext.hpp"
#include "RStdCallbacks.hpp"

#include "graphics/RGraphicsDevDesc.hpp"
#include "graphics/RGraphicsUtils.hpp"
#include "graphics/RGraphicsDevice.hpp"
#include "graphics/RGraphicsPlotManager.hpp"

#include <Rembedded.h>
#include <R_ext/Utils.h>
#include <R_ext/Rdynload.h>
#include <R_ext/RStartup.h>

extern "C" {

#ifndef _WIN32
SA_TYPE SaveAction;
#else
__declspec(dllimport) SA_TYPE SaveAction;
#endif

}

#define CTXT_BROWSER 16

// get rid of windows TRUE and FALSE definitions
#undef TRUE
#undef FALSE

// constants for graphics scratch subdirectory
#define kGraphicsPath "graphics"

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

namespace {

// options
ROptions s_options;

// callbacks
RCallbacks s_callbacks;
   
// client-state paths
FilePath s_clientStatePath;
FilePath s_projectClientStatePath;
   
// session state path
FilePath s_suspendedSessionPath ; 
   
// function for deferred deserialization actions. this encapsulates parts of 
// the initialization process that are potentially highly latent. this allows
// clients to bring their UI up and then receive an event indicating that the
// latent deserialization actions are taking place
boost::function<void()> s_deferredDeserializationAction;
   
// are in the middle of servicing a suspend request?
bool s_suspended = false;

// script to run, if any
std::string s_runScript;

FilePath rHistoryFilePath()
{
   std::string histFile = core::system::getenv("R_HISTFILE");
   boost::algorithm::trim(histFile);
   if (histFile.empty())
      histFile = ".Rhistory";

   return s_options.rHistoryDir().complete(histFile);
}

   
FilePath rSaveGlobalEnvironmentFilePath()
{
   FilePath rEnvironmentDir = s_options.rEnvironmentDir();
   return rEnvironmentDir.complete(".RData");
}

std::string createAliasedPath(const FilePath& filePath)
{
   return FilePath::createAliasedPath(filePath, s_options.userHomePath);
}
   
class SerializationCallbackScope : boost::noncopyable
{
public:
   SerializationCallbackScope(int action,
                              const FilePath& targetPath = FilePath())
   {
      s_callbacks.serialization(action, targetPath);
   }
   
   ~SerializationCallbackScope()
   {
      try {
         s_callbacks.serialization(kSerializationActionCompleted,
                                   FilePath());
      } catch(...) {}
   }
};
   

void reportDeferredDeserializationError(const Error& error)
{
   // log error
   LOG_ERROR(error);

   // report to user
   std::string errMsg = r::endUserErrorMessage(error);
   REprintf((errMsg + "\n").c_str());
}

void completeDeferredSessionInit(bool newSession)
{
   // always cleanup any restart context here
   restartContext().removeSessionState();

   // call external hook
   if (s_callbacks.deferredInit)
      s_callbacks.deferredInit(newSession);
}

void saveClientState(ClientStateCommitType commitType)
{
   using namespace r::session;

   // save client state (note we don't explicitly restore this
   // in restoreWorkingState, rather it is restored during
   // initialize() so that the client always has access to it when
   // for client_init)
   r::session::clientState().commit(commitType,
                                    s_clientStatePath,
                                    s_projectClientStatePath);
}


 
bool saveSessionState(const RSuspendOptions& options,
                      const FilePath& suspendedSessionPath,
                      bool disableSaveCompression)
{
   // notify client of serialization status
   SerializationCallbackScope cb(kSerializationActionSuspendSession);
   
   // suppress interrupts which occur during saving
   r::exec::IgnoreInterruptsScope ignoreInterrupts;
   
   // save 
   if (options.saveMinimal)
   {
      // save minimal
      return r::session::state::saveMinimal(suspendedSessionPath,
                                            options.saveWorkspace);

   }
   else
   {
      return r::session::state::save(suspendedSessionPath,
                                     s_options.serverMode,
                                     options.excludePackages,
                                     disableSaveCompression);
   }
}
   
void deferredRestoreSuspendedSession(
                     const boost::function<Error()>& deferredRestoreAction)
{
   // notify client of serialization status
   SerializationCallbackScope cb(kSerializationActionResumeSession);
   
   // suppress interrupts which occur during restore
   r::exec::IgnoreInterruptsScope ignoreInterrupts;
 
   // suppress output which occurs during restore (packages can sometimes
   // print messages to the console indicating they have conflicts -- the
   // has already seen these messages and doesn't expect them now so 
   // we suppress them
   utils::SuppressOutputInScope suppressOutput;
   
   // restore action
   Error error = deferredRestoreAction();
   if (error)
      reportDeferredDeserializationError(error);

   // complete deferred init
   completeDeferredSessionInit(false);

}

Error saveDefaultGlobalEnvironment()
{
   // path to save to
   FilePath globalEnvPath = rSaveGlobalEnvironmentFilePath();

   // notify client of serialization status
   SerializationCallbackScope cb(kSerializationActionSaveDefaultWorkspace,
                                 globalEnvPath);

   // suppress interrupts which occur during saving
   r::exec::IgnoreInterruptsScope ignoreInterrupts;
         
   // save global environment
   std::string path = string_utils::utf8ToSystem(globalEnvPath.absolutePath());
   Error error = r::exec::executeSafely(
                    boost::bind(R_SaveGlobalEnvToFile, path.c_str()));
   
   if (error)
   {
      return error;
   }
   else
   {
      return Success();
   }
}

Error restoreGlobalEnvFromFile(const std::string& path, std::string* pErrMessage)
{
   r::exec::RFunction fn(".rs.restoreGlobalEnvFromFile");
   fn.addParam(path);
   return fn.call(pErrMessage);
}
   
void deferredRestoreNewSession()
{
   // restore the default global environment if there is one
   FilePath globalEnvPath = s_options.startupEnvironmentFilePath;
   if (s_options.restoreWorkspace && globalEnvPath.exists())
   {
      // notify client of serialization status
      SerializationCallbackScope cb(kSerializationActionLoadDefaultWorkspace,
                                    globalEnvPath);

      // ignore interrupts which occur during restoring of the global env
      // the restoration will run to completion in any case and then the
      // next thing the user does will be "interrupted" -- clearly not
      // what they intended
      r::exec::IgnoreInterruptsScope ignoreInterrupts;

      std::string path = string_utils::utf8ToSystem(globalEnvPath.absolutePath());
      std::string aliasedPath = createAliasedPath(globalEnvPath);
      
      std::string errMessage;
      Error error = restoreGlobalEnvFromFile(path, &errMessage);
      if (error)
      {
         ::REprintf(
                  "WARNING: Failed to restore workspace from '%s' "
                  "(an internal error occurred)\n",
                  aliasedPath.c_str());
         LOG_ERROR(error);
      }
      else if (!errMessage.empty())
      {
         std::stringstream ss;
         ss << "WARNING: Failed to restore workspace from "
            << "'" << aliasedPath << "'" << std::endl
            << "Reason: " << errMessage << std::endl;
         std::string message = ss.str();
         ::REprintf(message.c_str());
         LOG_ERROR_MESSAGE(message);
      }
      else
      {
         Rprintf(("[Workspace loaded from " + aliasedPath + "]\n\n").c_str());
      }
   }

   // mark image clean (we need to do this due to our delayed handling
   // of workspace restoration)
   setImageDirty(false);

   // complete deferred init
   completeDeferredSessionInit(true);
}

void reportHistoryAccessError(const std::string& context,
                              const FilePath& historyFilePath,
                              const Error& error)
{
   // always log
   LOG_ERROR(error);

   // default summary
   std::string summary = error.summary();

   // if the file exists and we still got no such file or directory
   // then it is almost always permission denied. this seems to happen
   // somewhat frequently on linux systems where the user was root for
   // an operation and ended up writing a .Rhistory
   if (historyFilePath.exists() &&
       (error.code() == boost::system::errc::no_such_file_or_directory))
   {
      summary = "permission denied (is the .Rhistory file owned by root?)";
   }

   // notify the user
   std::string path = createAliasedPath(historyFilePath);
   std::string errmsg = context + " " + path + ": " + summary;
   REprintf(("Error attempting to " + errmsg + "\n").c_str());
}

} // anonymous namespace
  

const int kSerializationActionSaveDefaultWorkspace = 1;
const int kSerializationActionLoadDefaultWorkspace = 2;
const int kSerializationActionSuspendSession = 3;
const int kSerializationActionResumeSession = 4;
const int kSerializationActionCompleted = 5;

void restoreSession(const FilePath& suspendedSessionPath,
                    std::string* pErrorMessages)
{
   // don't show output during deserialization (packages loaded
   // during deserialization sometimes print messages)
   utils::SuppressOutputInScope suppressOutput;

   // deserialize session. if any part of this fails then the errors
   // will be logged and error messages will be returned in the passed
   // errorMessages buffer (this mechanism is used because we generally
   // suppress output during restore but we need a way for the error
   // messages to make their way back to the user)
   boost::function<Error()> deferredRestoreAction;
   r::session::state::restore(suspendedSessionPath,
                              s_options.serverMode,
                              &deferredRestoreAction,
                              pErrorMessages);

   if (deferredRestoreAction)
   {
      s_deferredDeserializationAction = boost::bind(
                                          deferredRestoreSuspendedSession,
                                          deferredRestoreAction);
   }
}

bool isInjectedBrowserCommand(const std::string& cmd)
{
   return browserContextActive() &&
          (cmd == "c" || cmd == "Q" || cmd == "n" || cmd == "s" || cmd == "f");
}


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
      FilePath filePath = utils::safeCurrentPath().complete(file);
      if (!filePath.exists())
      {
          throw r::exec::RErrorException(
                             "File " + file + " does not exist.");
      }

      s_callbacks.showFile(r::sexp::asString(titleSEXP),
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
         s_callbacks.browseFile(filePath);
      }
      // urls with no protocol are assumed to be file references
      else if (URL.find("://") == std::string::npos)
      {
         std::string file = r::util::expandFileName(URL);
         FilePath filePath = utils::safeCurrentPath().complete(
                                                   r::util::fixPath(file));
         s_callbacks.browseFile(filePath);
      }
      else
      {
         s_callbacks.browseURL(URL) ;
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
      s_callbacks.consoleHistoryReset();
   return R_NilValue;
}

SEXP rs_saveHistory(SEXP sFile)
{
   std::string file = R_ExpandFileName(r::sexp::asString(sFile).c_str());
   consoleHistory().saveToFile(FilePath(file));
   return R_NilValue;
}

class JumpToTopException
{
};

SA_TYPE saveAsk()
{
   try
   {
      // end user prompt
      std::string wsPath = createAliasedPath(rSaveGlobalEnvironmentFilePath());
      std::string prompt = "Save workspace image to " + wsPath + "? [y/n";
      // The Rf_jump_to_top_level doesn't work (freezes the process) with
      // 64-bit mingw due to the way it does stack unwinding. Since this is
      // a farily obscure gesture (quit from command line then cancel the quit)
      // we just eliminate the possiblity of it on windows
#ifndef _WIN32
      prompt += "/c";
#endif
      prompt += "]: ";

      // input buffer
      std::vector<CONSOLE_BUFFER_CHAR> inputBuffer(512, 0);

      while(true)
      {
         // read input
         RReadConsole(prompt.c_str(), &(inputBuffer[0]), inputBuffer.size(), 0);
         std::string input(1, inputBuffer[0]);
         boost::algorithm::to_lower(input);

         // look for yes, no, or cancel
         if (input == "y")
            return SA_SAVE;
         else if (input == "n")
            return SA_NOSAVE;
#ifndef _WIN32
         else if (input == "c")
            throw JumpToTopException();
#endif
      }
   }
   catch(JumpToTopException)
   {
      Rf_jump_to_toplevel();
   }

   // keep compiler happy
   return SA_SAVE;
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

} // end anonymous namespace
   
Error run(const ROptions& options, const RCallbacks& callbacks) 
{   
   // copy options and callbacks
   s_options = options;
   s_callbacks = callbacks;
   setStdCallbacks(&s_callbacks);
   
   // set to default "C" numeric locale as-per R embedding docs
   setlocale(LC_NUMERIC, "C") ;
   
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
   
   // set compatible graphics engine version
   int engineVersion = s_options.rCompatibleGraphicsEngineVersion;
   graphics::setCompatibleEngineVersion(engineVersion);

   // set client state paths
   s_clientStatePath = s_options.userScratchPath.complete("client-state");
   s_projectClientStatePath = s_options.scopedScratchPath.complete("pcs");
   
   // set source reloading behavior
   sourceManager().setAutoReload(options.autoReloadSource);
     
   // initialize suspended session path
   FilePath userScratch = s_options.userScratchPath;
   FilePath oldSuspendedSessionPath = userScratch.complete("suspended-session");
   FilePath sessionScratch = s_options.sessionScratchPath;
   s_suspendedSessionPath = sessionScratch.complete("suspended-session-data");

   // one time migration of global suspend to default project suspend
   if (!s_suspendedSessionPath.exists() && oldSuspendedSessionPath.exists())
   {
     // try to move it first
     Error error = oldSuspendedSessionPath.move(s_suspendedSessionPath);
     if (error)
     {
        // log the move error
        LOG_ERROR(error);

        // try to copy it as a failsafe (eliminates cross-volume issues)
        error = file_utils::copyDirectory(oldSuspendedSessionPath,
                                                s_suspendedSessionPath);
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

   // register browseURL method
   R_CallMethodDef browseURLMethod ;
   browseURLMethod.name = "rs_browseURL";
   browseURLMethod.fun = (DL_FUNC)rs_browseURL;
   browseURLMethod.numArgs = 1;
   r::routines::addCallMethod(browseURLMethod);

   // register editFile method
   R_CallMethodDef editFileMethod;
   editFileMethod.name = "rs_editFile";
   editFileMethod.fun = (DL_FUNC)rs_editFile;
   editFileMethod.numArgs = 1;
   r::routines::addCallMethod(editFileMethod);

   // register showFile method
   R_CallMethodDef showFileMethod;
   showFileMethod.name = "rs_showFile";
   showFileMethod.fun = (DL_FUNC)rs_showFile;
   showFileMethod.numArgs = 3;
   r::routines::addCallMethod(showFileMethod);

   // register createUUID method
   R_CallMethodDef createUUIDMethodDef ;
   createUUIDMethodDef.name = "rs_createUUID" ;
   createUUIDMethodDef.fun = (DL_FUNC) rs_createUUID ;
   createUUIDMethodDef.numArgs = 0;
   r::routines::addCallMethod(createUUIDMethodDef);

   // register loadHistory method
   R_CallMethodDef loadHistoryMethodDef ;
   loadHistoryMethodDef.name = "rs_loadHistory" ;
   loadHistoryMethodDef.fun = (DL_FUNC) rs_loadHistory ;
   loadHistoryMethodDef.numArgs = 1;
   r::routines::addCallMethod(loadHistoryMethodDef);

   // register saveHistory method
   R_CallMethodDef saveHistoryMethodDef ;
   saveHistoryMethodDef.name = "rs_saveHistory" ;
   saveHistoryMethodDef.fun = (DL_FUNC) rs_saveHistory ;
   saveHistoryMethodDef.numArgs = 1;
   r::routines::addCallMethod(saveHistoryMethodDef);

   // register graphics methods
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
      loadInitFile = (!options.disableRProfileOnStart && (!s_suspendedSessionPath.exists() || options.rProfileOnResume))
                     || options.packratEnabled
                     || r::session::state::packratModeEnabled(s_suspendedSessionPath);
   }

   // quiet for resume cases
   bool quiet = restartContext().hasSessionState() ||
                s_suspendedSessionPath.exists();

   r::session::Callbacks cb;
   cb.showMessage = RShowMessage;
   cb.readConsole = RReadConsole;
   cb.writeConsoleEx = RWriteConsoleEx;
   cb.editFile = REditFile;
   cb.busy = RBusy;
   cb.chooseFile = RChooseFile;
   cb.showFiles = RShowFiles;
   cb.loadhistory = Rloadhistory;
   cb.savehistory = Rsavehistory;
   cb.addhistory = Raddhistory;
   cb.suicide = RSuicide;
   cb.cleanUp = RCleanUp;
   r::session::runEmbeddedR(FilePath(rLocations.homePath),
                            options.userHomePath,
                            quiet,
                            loadInitFile,
                            s_options.saveWorkspace,
                            cb,
                            stdInternalCallbacks());

   // keep compiler happy
   return Success() ;
}

void ensureDeserialized()
{
   if (s_deferredDeserializationAction)
   {
      // do the deferred action
      s_deferredDeserializationAction();
      s_deferredDeserializationAction.clear();
   }
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
      REprintf((errMsg + "\n").c_str());

      // restore previous values (but don't fire plotsChanged b/c
      // the reset doesn't result in a change in graphics state)
      r::exec::executeSafely(boost::bind(doSetClientMetrics, previousMetrics));
   }
}

void reportAndLogWarning(const std::string& warning)
{
   std::string msg = "WARNING: " + warning + "\n";
   RWriteConsoleEx(msg.c_str(), msg.length(), 1);
   LOG_WARNING_MESSAGE("(Reported to User) " + warning);
}

bool isSuspendable(const std::string& currentPrompt)
{
   // NOTE: active file graphics devices (e.g. png or pdf) are wiped out
   // during a suspend as are open connections. there may or may not be a
   // way to make this more robust.

   // are we not at the default prompt?
   std::string defaultPrompt = r::options::getOption<std::string>("prompt");
   if (currentPrompt != defaultPrompt)
      return false;
   else
      return true;
}
   

bool suspend(const RSuspendOptions& options,
             const FilePath& suspendedSessionPath,
             bool disableSaveCompression,
             bool force)
{
   // validate that force == true if disableSaveCompression is specified
   // this is because save compression is disabled and the previous options
   // are not restored, so it is only suitable to use this when we know
   // the process is going to go away completely
   if (disableSaveCompression)
      BOOST_ASSERT(force == true);

   // commit all client state
   saveClientState(ClientStateCommitAll);

   // if we are saving minimal then clear the graphics device
   if (options.saveMinimal)
   {
      r::session::graphics::display().clear();
   }

   // save the session state. errors are handled internally and reported
   // directly to the end user and written to the server log.
   bool suspend = saveSessionState(options,
                                   suspendedSessionPath,
                                   disableSaveCompression);
      
   // if we failed to save the data and are being forced then warn user
   if (!suspend && force)
   {
      reportAndLogWarning("Forcing suspend of process in spite of all session "
                          "data not being fully saved.");
      suspend = true;
   }
   
   // only continue with exiting the process if we actually succeed in saving
   if(suspend)
   {      
      // set suspended flag so cleanup code can act accordingly
      s_suspended = true;
      
      // call suspend hook
      s_callbacks.suspended(options);
   
      // clean up but don't save workspace or runLast because we have
      // been suspended
      RCleanUp(SA_NOSAVE, options.status, FALSE);
      
      // keep compiler happy (this line will never execute)
      return true;
   }
   else
   {
      return false;
   }
}

bool suspend(bool force, int status)
{
   return suspend(RSuspendOptions(status),
                  s_suspendedSessionPath,
                  false,
                  force);
}

void suspendForRestart(const RSuspendOptions& options)
{
   suspend(options,
           RestartContext::createSessionStatePath(s_options.scopedScratchPath,
                                                  s_options.sessionPort),
           true,  // disable save compression
           true);  // force suspend
}

// set save action
const int kSaveActionNoSave = 0;
const int kSaveActionSave = 1;
const int kSaveActionAsk = -1;

void setSaveAction(int saveAction)
{
   switch(saveAction)
   {
   case kSaveActionNoSave:
      SaveAction = SA_NOSAVE;
      break;
   case kSaveActionSave:
      SaveAction = SA_SAVE;
      break;
   case kSaveActionAsk:
   default:
      SaveAction = SA_SAVEASK;
      break;
   }

}

void setImageDirty(bool imageDirty)
{
   R_DirtyImage = imageDirty ? 1 : 0;
}

bool browserContextActive()
{
   return Rf_countContexts(CTXT_BROWSER, 1) > 0;
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

const FilePath& userHomePath()
{
   return s_options.userHomePath;
}

FilePath logPath()
{
   return s_options.logPath;
}

FilePath safeCurrentPath()
{
   return FilePath::safeCurrentPath(userHomePath());
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

FilePath tempDir()
{
   std::string tempDir;
   Error error = r::exec::RFunction("tempdir").call(&tempDir);
   if (error)
      LOG_ERROR(error);
   FilePath filePath(r::util::fixPath(tempDir));
   return filePath;
}

} // namespace utils
   
} // namespace session
} // namespace r
} // namespace rstudio
