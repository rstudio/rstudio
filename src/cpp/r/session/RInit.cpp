/*
 * RInit.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <boost/bind.hpp>

#include <core/system/Environment.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RSourceManager.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RSessionState.hpp>

#include "REmbedded.hpp"
#include "RInit.hpp"
#include "RSuspend.hpp"
#include "RStdCallbacks.hpp"
#include "RRestartContext.hpp"

#include "graphics/RGraphicsDevice.hpp"

// constants for graphics scratch subdirectory
#define kGraphicsPath "graphics"

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

namespace {

// is this R 3.0 or greater
bool s_isR3 = false;

// is this R 3.3 or greater
bool s_isR3_3 = false;

// function for deferred deserialization actions. this encapsulates parts of 
// the initialization process that are potentially highly latent. this allows
// clients to bring their UI up and then receive an event indicating that the
// latent deserialization actions are taking place
boost::function<void()> s_deferredDeserializationAction;
   
void reportDeferredDeserializationError(const Error& error)
{
   // log error
   LOG_ERROR(error);

   // report to user
   std::string errMsg = r::endUserErrorMessage(error);
   REprintf("%s\n", errMsg.c_str());
}

std::string createAliasedPath(const FilePath& filePath)
{
   return FilePath::createAliasedPath(filePath, utils::userHomePath());
}
   
Error restoreGlobalEnvFromFile(const std::string& path, std::string* pErrMessage)
{
   r::exec::RFunction fn(".rs.restoreGlobalEnvFromFile");
   fn.addParam(path);
   return fn.call(pErrMessage);
}

void completeDeferredSessionInit(bool newSession)
{
   // always cleanup any restart context here
   restartContext().removeSessionState();

   // call external hook
   if (rCallbacks().deferredInit)
      rCallbacks().deferredInit(newSession);
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

void deferredRestoreNewSession()
{
   // restore the default global environment if there is one
   FilePath globalEnvPath = utils::startupEnvironmentFilePath();
   if (utils::restoreWorkspace() && globalEnvPath.exists())
   {
      // notify client of serialization status
      SerializationCallbackScope cb(kSerializationActionLoadDefaultWorkspace,
                                    globalEnvPath);

      // ignore interrupts which occur during restoring of the global env
      // the restoration will run to completion in any case and then the
      // next thing the user does will be "interrupted" -- clearly not
      // what they intended
      r::exec::IgnoreInterruptsScope ignoreInterrupts;

      std::string path = string_utils::utf8ToSystem(globalEnvPath.getAbsolutePath());
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
         
         ::REprintf("%s\n", message.c_str());
         LOG_ERROR_MESSAGE(message);
      }
      else
      {
         const char* fmt = "[Workspace loaded from %s]\n\n";
         Rprintf(fmt, aliasedPath.c_str());
      }
   }

   // mark image clean (we need to do this due to our delayed handling
   // of workspace restoration)
   setImageDirty(false);

   // complete deferred init
   completeDeferredSessionInit(true);
}

} // anonymous namespace

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
                              utils::isServerMode(),
                              &deferredRestoreAction,
                              pErrorMessages);

   if (deferredRestoreAction)
   {
      s_deferredDeserializationAction = boost::bind(
                                          deferredRestoreSuspendedSession,
                                          deferredRestoreAction);
   }
}

// one-time per session initialization
Error initialize()
{
   // ensure that the utils package is loaded (it might not be loaded
   // if R is attempting to recover from a library loading error which
   // occurs during .Rprofile)
   Error libError = r::exec::RFunction("library", "utils").call();
   if (libError)
      LOG_ERROR(libError);

   // check whether this is R 3.3 or greater
   Error r33Error = r::exec::evaluateString("getRversion() >= '3.3.0'", &s_isR3_3);
   if (r33Error)
      LOG_ERROR(r33Error);

   if (s_isR3_3)
   {
      s_isR3 = true;
   }
   else
   {
      // check whether this is R 3.0 or greater
      Error r3Error = r::exec::evaluateString("getRversion() >= '3.0.0'", &s_isR3);
      if (r3Error)
         LOG_ERROR(r3Error);
   }

   // initialize console history capacity
   r::session::consoleHistory().setCapacityFromRHistsize();

   // install R tools
   FilePath toolsFilePath = utils::rSourcePath().completePath("Tools.R");
   Error error = r::sourceManager().sourceTools(toolsFilePath);
   if (error)
      return error;

   // install RStudio API
   FilePath apiFilePath = utils::rSourcePath().completePath("Api.R");
   error = r::sourceManager().sourceTools(apiFilePath);
   if (error)
      return error;

   // initialize graphics device -- use a stable directory for server mode
   // and temp directory for desktop mode (so that we can support multiple
   // concurrent processes using the same project)
   FilePath graphicsPath;
   if (utils::isServerMode())
   {
      std::string path = kGraphicsPath;
      if (utils::isR3())
         path += "-r3";
      graphicsPath = utils::sessionScratchPath().completePath(path);
   }
   else
   {
      graphicsPath = r::session::utils::tempDir().completePath(
         "rs-graphics-" + core::system::generateUuid());
   }

   error = graphics::device::initialize(graphicsPath,
                                        rCallbacks().locator);
   if (error) 
      return error;
   
   // restore client state
   session::clientState().restore(utils::clientStatePath(),
                                  utils::projectClientStatePath());
      
   // restore suspended session if we have one
   bool wasResumed = false;
   
   // first check for a pending restart
   if (restartContext().hasSessionState())
   {
      // restore session
      std::string errorMessages;
      restoreSession(restartContext().sessionStatePath(), &errorMessages);

      // show any error messages
      if (!errorMessages.empty())
         REprintf("%s\n", errorMessages.c_str());

      // note we were resumed
      wasResumed = true;
   }
   else if (suspendedSessionPath().exists())
   {  
      // restore session
      std::string errorMessages;
      restoreSession(suspendedSessionPath(), &errorMessages);
      
      // show any error messages
      if (!errorMessages.empty())
         REprintf("%s\n", errorMessages.c_str());

      // note we were resumed
      wasResumed = true;
   }  
   // new session
   else
   {  
      // restore console history
      FilePath historyPath = rHistoryFilePath();
      error = consoleHistory().loadFromFile(historyPath, false);
      if (error)
         reportHistoryAccessError("read history from", historyPath, error);

      // defer loading of global environment
      s_deferredDeserializationAction = deferredRestoreNewSession;
   }
   
   // initialize client
   RInitInfo rInitInfo(wasResumed);
   error = rCallbacks().init(rInitInfo);
   if (error)
      return error;

   // call resume hook if we were resumed
   if (wasResumed)
      rCallbacks().resumed();
   
   // now that all initialization code has had a chance to run we 
   // can register all external routines which were added to r::routines
   // during the init sequence
   r::routines::registerAll();
   
   // set default repository if requested
   if (!utils::rCRANUrl().empty() || !utils::rCRANSecondary().empty())
   {
      error = r::exec::RFunction(".rs.setCRANReposAtStartup",
                                 utils::rCRANUrl(),
                                 utils::rCRANSecondary()).call();
      if (error)
         return error;
   }

   // initialize profile resources
   error = r::exec::RFunction(".rs.profileResources").call();
   if (error)
      return error;

   // complete embedded r initialization
   error = r::session::completeEmbeddedRInitialization(utils::useInternet2());
   if (error)
      return error;

   // set global R options
   FilePath optionsFilePath = utils::rSourcePath().completePath("Options.R");
   error = r::sourceManager().sourceLocal(optionsFilePath);
   if (error)
      return error;

   // server specific R options options
   if (utils::isServerMode())
   {
#ifndef __APPLE__
      FilePath serverOptionsFilePath = utils::rSourcePath().completePath(
         "ServerOptions.R");
      return r::sourceManager().sourceLocal(serverOptionsFilePath);
#else
      return Success();
#endif
   }
   else
   {
      return Success();
   }
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
   
FilePath rHistoryFilePath()
{
   std::string histFile = core::system::getenv("R_HISTFILE");
   boost::algorithm::trim(histFile);
   if (histFile.empty())
      histFile = ".Rhistory";

   return utils::rHistoryDir().completePath(histFile);
}

void reportHistoryAccessError(const std::string& context,
                              const FilePath& historyFilePath,
                              const Error& error)
{
   // always log
   LOG_ERROR(error);

   // default summary
   std::string summary = error.getSummary();

   // if the file exists and we still got no such file or directory
   // then it is almost always permission denied. this seems to happen
   // somewhat frequently on linux systems where the user was root for
   // an operation and ended up writing a .Rhistory
   if (historyFilePath.exists() &&
       (error == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation())))
   {
      summary = "permission denied (is the .Rhistory file owned by root?)";
   }

   // notify the user
   std::string path = createAliasedPath(historyFilePath);
   std::string errmsg = context + " " + path + ": " + summary;
   REprintf("Error attempting to %s\n", errmsg.c_str());
}
   
namespace utils {

bool isR3()
{
   return s_isR3;
}

bool isR3_3()
{
   return s_isR3_3;
}

}

} // namespace session
} // namespace r
} // namespace rstudio

