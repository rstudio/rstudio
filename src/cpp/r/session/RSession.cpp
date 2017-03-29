/*
 * RSession.cpp
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

#define R_INTERNAL_FUNCTIONS
#include <r/session/RSession.hpp>

#include <iostream>

#include <boost/regex.hpp>
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
#include <core/RegexUtils.hpp>
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
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RGraphics.hpp>
#include <r/session/RDiscovery.hpp>

#include "RClientMetrics.hpp"
#include "RRestartContext.hpp"
#include "REmbedded.hpp"

#include "graphics/RGraphicsDevDesc.hpp"
#include "graphics/RGraphicsUtils.hpp"
#include "graphics/RGraphicsDevice.hpp"
#include "graphics/RGraphicsPlotManager.hpp"

#include <Rembedded.h>
#include <R_ext/Utils.h>
#include <R_ext/Rdynload.h>
#include <R_ext/RStartup.h>
extern "C" SA_TYPE SaveAction;

#define CTXT_BROWSER 16

// get rid of windows TRUE and FALSE definitions
#undef TRUE
#undef FALSE

// constants for graphics scratch subdirectory
#define kGraphicsPath "graphics"

using namespace rstudio::core ;

namespace rstudio {
namespace r {
namespace session {

namespace {

// is this R 3.0 or greator
bool s_isR3 = false;

// is this R 3.3 or greator
bool s_isR3_3 = false;

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
   
// have we completed our one-time initialization yet?
bool s_initialized = false;

// are in the middle of servicing a suspend request?
bool s_suspended = false;

// temporarily suppress output
bool s_suppressOutput = false;

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
   FilePath toolsFilePath = s_options.rSourcePath.complete("Tools.R");
   Error error = r::sourceManager().sourceTools(toolsFilePath);
   if (error)
      return error ;

   // install RStudio API
   FilePath apiFilePath = s_options.rSourcePath.complete("Api.R");
   error = r::sourceManager().sourceTools(apiFilePath);
   if (error)
      return error;

   // initialize graphics device -- use a stable directory for server mode
   // and temp directory for desktop mode (so that we can support multiple
   // concurrent processes using the same project)
   FilePath graphicsPath;
   if (s_options.serverMode)
   {
      std::string path = kGraphicsPath;
      if (utils::isR3())
         path += "-r3";
      graphicsPath = s_options.sessionScratchPath.complete(path);
   }
   else
   {
      graphicsPath = r::session::utils::tempDir().complete(
                              "rs-graphics-" + core::system::generateUuid());
   }

   error = graphics::device::initialize(graphicsPath,
                                        s_callbacks.locator);
   if (error) 
      return error;
   
   // restore client state
   session::clientState().restore(s_clientStatePath,
                                  s_projectClientStatePath);
      
   // restore suspended session if we have one
   bool wasResumed = false;
   
   // first check for a pending restart
   if (restartContext().hasSessionState())
   {
      // restore session
      std::string errorMessages ;
      restoreSession(restartContext().sessionStatePath(), &errorMessages);

      // show any error messages
      if (!errorMessages.empty())
         REprintf(errorMessages.c_str());

      // note we were resumed
      wasResumed = true;
   }
   else if (s_suspendedSessionPath.exists())
   {  
      // restore session
      std::string errorMessages ;
      restoreSession(s_suspendedSessionPath, &errorMessages);
      
      // show any error messages
      if (!errorMessages.empty())
         REprintf(errorMessages.c_str());

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
   error = s_callbacks.init(rInitInfo);
   if (error)
      return error;

   // call resume hook if we were resumed
   if (wasResumed)
      s_callbacks.resumed();
   
   // now that all initialization code has had a chance to run we 
   // can register all external routines which were added to r::routines
   // during the init sequence
   r::routines::registerAll();
   
   // set default repository if requested
   if (!s_options.rCRANRepos.empty())
   {
      error = r::exec::RFunction(".rs.setCRANReposAtStartup",
                                 s_options.rCRANRepos).call();
      if (error)
         return error;
   }

   // initialize profile resources
   error = r::exec::RFunction(".rs.profileResources").call();
   if (error)
      return error;

   // complete embedded r initialization
   error = r::session::completeEmbeddedRInitialization(s_options.useInternet2);
   if (error)
      return error;

   // set global R options
   FilePath optionsFilePath = s_options.rSourcePath.complete("Options.R");
   error = r::sourceManager().sourceLocal(optionsFilePath);
   if (error)
      return error;

   // server specific R options options
   if (s_options.serverMode)
   {
#ifndef __APPLE__
      FilePath serverOptionsFilePath =  s_options.rSourcePath.complete(
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

void rSuicide(const std::string& msg)
{
   // log abort message if we are in desktop mode
   if (!s_options.serverMode)
   {
      FilePath abendLogPath = s_options.logPath.complete(
                                                 "rsession_abort_msg.log");
      Error error = core::writeStringToFile(abendLogPath, msg);
      if (error)
         LOG_ERROR(error);
   }


   R_Suicide(msg.c_str());
}

void rSuicide(const Error& error)
{
   // provide error message if the error was unexpected
   std::string msg;
   if (!error.expected())
      msg = core::log::errorAsLogEntry(error);

   rSuicide(msg);
}

// forward declare win32 quit handler and provide string based quit
// handler that parses the quit command from the console
#ifdef _WIN32
bool win32Quit(const std::string& saveAction,
               int status,
               bool runLast,
               std::string* pErrMsg);

bool win32Quit(const std::string& command, std::string* pErrMsg)
{
   // default values
   std::string saveAction = "default";
   double status = 0;
   bool runLast = true;

   // parse quit arguments
   SEXP argsSEXP;
   r::sexp::Protect rProtect;
   Error error = r::exec::RFunction(".rs.parseQuitArguments", command).call(
                                                                     &argsSEXP,
                                                                     &rProtect);
   if (!error)
   {
      error = r::sexp::getNamedListElement(argsSEXP,
                                           "save",
                                           &saveAction,
                                           saveAction);
      if (error)
         LOG_ERROR(error);

      error = r::sexp::getNamedListElement(argsSEXP,
                                           "status",
                                           &status,
                                           status);
      if (error)
         LOG_ERROR(error);

      error = r::sexp::getNamedListElement(argsSEXP,
                                           "runLast",
                                           &runLast,
                                           runLast);
      if (error)
         LOG_ERROR(error);
   }
   else
   {
      *pErrMsg = r::endUserErrorMessage(error);
      return false;
   }

   return win32Quit(saveAction, static_cast<int>(status), runLast, pErrMsg);
}

#endif

bool consoleInputHook(const std::string& prompt,
                      const std::string& input)
{
   // only check for quit when we're at the default prompt
   if (!r::session::utils::isDefaultPrompt(prompt))
      return true;

   // check for user quit invocation
    boost::regex re("^\\s*(q|quit)\\s*\\(.*$");
    boost::smatch match;
    if (regex_utils::match(input, match, re))
   {
      if (!s_callbacks.handleUnsavedChanges())
      {
         REprintf("User cancelled quit operation\n");
         return false;
      }

      // on win32 we will actually assume responsibility for the
      // quit function entirely (so we can call our internal cleanup
      // handler code)
#ifdef _WIN32
      std::string quitErr;
      bool didQuit = win32Quit(input, &quitErr);
      if (!didQuit)
         REprintf((quitErr + "\n").c_str());

      // always return false (since we take over the command fully)
      return false;
#else
      return true;
#endif
   }
   else
   {
      return true;
   }
}

bool isInjectedBrowserCommand(const std::string& cmd)
{
   return browserContextActive() &&
          (cmd == "c" || cmd == "Q" || cmd == "n" || cmd == "s" || cmd == "f");
}


int RReadConsole (const char *pmt,
                  CONSOLE_BUFFER_CHAR* buf,
                  int buflen,
                  int hist)
{
   try
   {
      // capture the prompt for later manipulation
      std::string prompt(pmt);

      // invoke one time initialization
      if (!s_initialized)
      {
         // ignore interrupts which occur during initialization. any
         // interrupt will cause the initialization to fail which will
         // then require the user to start over from the beginning. if the
         // user has to wait in any case we might as well help them out by
         // never having to start from scratch
         r::exec::IgnoreInterruptsScope ignoreInterrupts;
         
         // attempt to initialize 
         Error initError;
         Error error = r::exec::executeSafely<Error>(initialize, &initError);
         if (error || initError)
         {
            if (initError)
               error = initError;

            // log the error if it was unexpected
            if (!error.expected())
               LOG_ERROR(error);
            
            // terminate the session (use suicide so that no special
            // termination code runs -- i.e. call to setAbnormalEnd(false)
            // or call to client::quitSession)
            rSuicide(error);
         }
         
         // reset the prompt to whatever the default is after we've
         // fully initialized (and restored suspended options)
         prompt = r::options::getOption<std::string>("prompt");

         // ensure only one initialization 
         s_initialized = true;
      }

      std::string promptString(prompt);
      promptString = util::rconsole2utf8(promptString);

      // get the next input
      bool addToHistory = (hist == 1);
      RConsoleInput consoleInput("");
      if ( s_callbacks.consoleRead(promptString, addToHistory, &consoleInput) )
      {
         // add prompt to console actions (we do this after consoleRead
         // completes so that we don't send both a console prompt event
         // AND include the same prompt in the actions history)
         consoleActions().add(kConsoleActionPrompt, prompt);

         if (consoleInput.cancel)
         {
            // notify of interrupt
            consoleActions().notifyInterrupt();

            // escape out using exception so that we can allow normal
            // c++ stack unwinding to occur before jumping
            throw r::exec::InterruptException();
         }
         else
         {
            // determine the input to return to R
            std::string rInput = consoleInput.text;

            // refresh source if necessary (no-op in production)
            r::sourceManager().reloadIfNecessary();
            
            // ensure that our input fits within the buffer
            std::string::size_type maxLen = buflen - 2; // for \n\0
            rInput = string_utils::utf8ToSystem(rInput, true);
            if (rInput.length() > maxLen)
               rInput.resize(maxLen);
            std::string::size_type inputLen = rInput.length();
            
            // add to console actions and history (if requested). note that
            // we add the user's input rather than any tranformed input we
            // created as a result of a shell escape
            consoleActions().add(kConsoleActionInput, consoleInput.text);
            if (addToHistory && !isInjectedBrowserCommand(consoleInput.text))
               consoleHistory().add(consoleInput.text);

            // call console input hook and interrupt if the hook tells us to
            if (!consoleInputHook(prompt, consoleInput.text))
               throw r::exec::InterruptException();

            // copy to buffer and add terminators
            rInput.copy( (char*)buf, maxLen);
            buf[inputLen] = '\n';
            buf[inputLen+1] = '\0';
         }

         return 1 ;
      }
      else
      {
         return 0; // terminate
      }
   }
   catch(r::exec::InterruptException&)
   {
      // set interrupts pending
      r::exec::setInterruptsPending(true);

      // only issue an interrupt when not on Windows -- let the regular
      // event loop handle interrupts there. note that this will longjmp
#ifndef _WIN32
      r::exec::checkUserInterrupt();
#endif

      // return success
      return 1;
   }
   catch(const std::exception& e)
   {
      std::string msg = std::string("Unexpected exception: ") + e.what();
      LOG_ERROR_MESSAGE(msg);
      rSuicide(msg);
   }
   catch(...)
   {
      std::string msg = "Unknown exception";
      LOG_ERROR_MESSAGE(msg);
      rSuicide(msg);
   }
      
   return 0 ; // keep compiler happy
}
   
void RWriteConsoleEx (const char *buf, int buflen, int otype)
{
   try
   {
      if (!s_suppressOutput)
      {
         // get output
         std::string output = std::string(buf,buflen);
         output = util::rconsole2utf8(output);
         
         // add to console actions
         int type = otype == 1 ? kConsoleActionOutputError :
                                 kConsoleActionOutput;
         consoleActions().add(type, output);
         
         // write
         s_callbacks.consoleWrite(output, otype) ;
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}

void RShowMessage(const char* msg)
{
   try 
   {
      s_callbacks.showMessage(msg) ;
   }
   CATCH_UNEXPECTED_EXCEPTION
}

   
// NOTE: Win32 doesn't receive this callback
int REditFile(const char* file)
{
   try 
   {
      return s_callbacks.editFile(r::util::fixPath(file));
   }
   CATCH_UNEXPECTED_EXCEPTION
   
   // error if we got this far
   return 1 ;
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

// NOTE: Win32 doesn't receive this callback
int RShowFiles (int nfile, 
                const char **file, 
                const char **headers, 
                const char *wtitle, 
                Rboolean del, 
                const char *pager)
{
   try 
   {
      for (int i=0; i<nfile; i++)
      {
         // determine file path and title
         std::string fixedPath = r::util::fixPath(file[i]);
         FilePath filePath = utils::safeCurrentPath().complete(fixedPath);
         if (filePath.exists())
         {
            std::string title(headers[i]);
            if (title.empty())
               title = wtitle;
         
            // show file
            s_callbacks.showFile(title, filePath, del);
         }
         else
         {
            throw r::exec::RErrorException(
                               "File " + fixedPath + " does not exist.");
         }
      }
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::error(e.message());
   }

   CATCH_UNEXPECTED_EXCEPTION
   
   // NOTE: the documentation doesn't indicate what to return and do_fileshow
   // in platform.c doesn't check the return value s
   return 0;
}

// NOTE: Win32 doesn't receive this callback
int RChooseFile (int newFile, char *buf, int len) 
{
   try 
   {
      FilePath filePath = s_callbacks.chooseFile(newFile == TRUE);
      if (!filePath.empty())
      {
         // get absolute path
         std::string absolutePath = filePath.absolutePath();
         
         // trunate file if it is too long
         std::string::size_type maxLen = len - 1; 
         if (absolutePath.length() > maxLen)
            absolutePath.resize(maxLen);
         
         // copy the file to the buffer
         absolutePath.copy(buf, maxLen);
         buf[absolutePath.length()] = '\0';
         
         // return the length of the filepath buffer
         return absolutePath.length();
      }
      else
      {
         return 0;
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   // error if we got this far
   return 0 ;
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

void doHistoryFileOperation(SEXP args, 
                            boost::function<Error(const FilePath&)> fileOp)
{
   // validate filename argument
   SEXP file = CAR(args);
   if (!sexp::isString(file) || sexp::length(file) < 1)
      throw r::exec::RErrorException("invalid 'file' argument");
   
   // calculate full path to history file
   FilePath historyFilePath(R_ExpandFileName(Rf_translateChar(STRING_ELT(file, 
                                                                         0))));
   // perform operation
   Error error = fileOp(historyFilePath);
   if (error)
      throw r::exec::RErrorException(error.code().message());
}
   
void Rloadhistory(SEXP call, SEXP op, SEXP args, SEXP env)
{
   try
   {
      doHistoryFileOperation(args, boost::bind(&ConsoleHistory::loadFromFile,
                                               &consoleHistory(), _1, true));
      
      s_callbacks.consoleHistoryReset();
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::errorCall(call, e.message());
   }
}
   
void Rsavehistory(SEXP call, SEXP op, SEXP args, SEXP env)
{
   try
   {
      doHistoryFileOperation(args, boost::bind(&ConsoleHistory::saveToFile,
                                               &consoleHistory(), _1));
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::errorCall(call, e.message());
   }
}
   
void Raddhistory(SEXP call, SEXP op, SEXP args, SEXP env)
{
   try
   {
      // get commands
      std::vector<std::string> commands ;
      Error error = sexp::extract(CAR(args), &commands);
      if (error)
         throw r::exec::RErrorException(error.code().message());
      
      // append them
      ConsoleHistory& history = consoleHistory();
      std::for_each(commands.begin(), 
                    commands.end(),
                    boost::bind(&ConsoleHistory::add, &history, _1));
   }
   catch(r::exec::RErrorException& e)
   {
      r::exec::errorCall(call, e.message());
   }
}

void RBusy(int which)   
{
   try
   {
      s_callbacks.busy(which == 1) ;
   }
   CATCH_UNEXPECTED_EXCEPTION 
}


// track internal callbacks for delgation
r::session::InternalCallbacks s_internalCallbacks;

// NOTE: Win32 doesn't receive this callback
void RSuicide(const char* s)
{
   s_callbacks.suicide(s);
   s_internalCallbacks.suicide(s);
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

// NOTE: Win32 doesn't receive this function. As a result we only use
// it in server mode (where it is used to support suspending as well as
// notifying the browser of quit). In desktop mode we allow standard
// R_CleanUp processing to take place. There are two important implications
// of this:
//
//   (1) In desktop mode we override do_quit and use it as an indicator
//       that we should save history and persistent client state (an
//       alternative would be to more eagerly persist this data)
//
//   (2) In desktop mode we don't receive termination oriented events such
//       as quit, suicide, and cleanup. This means that the desktop process
//       must simply detect that we have exited and terminate itself. It also
//       means that cleanup of our http damons, file monitoring, etc. never
//       occurs (not an issue b/c our process is going away but worth noting)
//
void RCleanUp(SA_TYPE saveact, int status, int runLast)
{
   // perform cleanup that is coupled to our internal history,
   // environment-saving, client-state, and graphics implementations
   // and then delegate to internal R_CleanUp for the remainder
   // of processing
   try
   {
      // set to default if requested
      if (saveact == SA_DEFAULT)
         saveact = SaveAction ;
      
      // prompt user to resolve SA_SAVEASK into SA_SAVE or SA_NOSAVE
      if (saveact == SA_SAVEASK) 
      {
         if (imageIsDirty() || !s_options.alwaysSaveHistory())
            saveact = saveAsk(); // can Rf_jump_to_toplevel()
         else
            saveact = SA_NOSAVE; // auto-resolve to no save when not dirty
      }

      // if the session was quit by the user then run our termination code
      bool sessionQuitByUser = (saveact != SA_SUICIDE) && !s_suspended ;
      if (sessionQuitByUser)
      {
         // run last if requested (can throw error)
         if (runLast)
            R_dot_Last();
         
         // save history if we either always save history or saveact == SA_SAVE
         if (s_options.alwaysSaveHistory() || saveact == SA_SAVE)
         {
            FilePath historyPath = rHistoryFilePath();
            Error error = consoleHistory().saveToFile(historyPath);
            if (error)
               reportHistoryAccessError("write history to", historyPath, error);
         }

         // save environment and history
         if (saveact == SA_SAVE)
         {
            // attempt save if the image is dirty
            if (imageIsDirty())
            {
               // attempt to save global environment. raise error (longjmp 
               // back to REPL) if there was a problem saving
               Error error = saveDefaultGlobalEnvironment();
               if (error)
                  r::exec::error(r::endUserErrorMessage(error));
            }
            
            // update state
            saveact = SA_NOSAVE;     // prevent R from saving
         }
         
         // since we've successfully completed the session we can safely
         // remove any serialized session remaining on disk. note that if 
         // we do not successfully destroy the session then this would cause
         // data loss when the previous session is read rather than the
         // contents of .RData. therefore, we refuse to quit if we can't
         // successfully destroy the suspended session
         if (!r::session::state::destroy(s_suspendedSessionPath))
         {
            // this will cause us to jump back to the REPL loop
            r::exec::error("Unable to quit (session cleanup failure)\n");
         }

         // commit client state
         saveClientState(ClientStateCommitPersistentOnly);

         // clear display
         r::session::graphics::display().clear();
                
         // notify client that the session has been quit
         s_callbacks.quit();
      }

      // allow client to cleanup
      bool terminatedNormally = saveact != SA_SUICIDE;
      s_callbacks.cleanup(terminatedNormally);
      
      // call internal cleanup (never .runLast because we do it above)
      // NOTE: may want to replace RCleanUp entirely so that the client
      // can see any errors which occur during cleanup in the console
      // (they aren't seen now because the handling of quit obstructs them)
      s_internalCallbacks.cleanUp(saveact, status, FALSE);
   }
   CATCH_UNEXPECTED_EXCEPTION 
}

// Replace the quit function so we can call our R_CleanUp hook. Note
// that we need to take special measures to code this safely visa-vi
// Rf_error long jmps and C++ exceptions. Currently, the RCleanUp
// function can still long jump if runLast encounters an error. We
// should re-write the combination of this function and RCleanUp to
// be fully "error-safe" (not doing this now due to regression risk)
#ifdef _WIN32
bool win32Quit(const std::string& saveAction,
               int status,
               bool runLast,
               std::string* pErrMsg)
{
   if (r::session::browserContextActive())
   {
      *pErrMsg = "unable to quit when browser is active";
      return false;
   }

   // determine save action
   SA_TYPE action = SA_DEFAULT;
   if (saveAction == "ask")
      action = SA_SAVEASK;
   else if (saveAction == "no")
      action = SA_NOSAVE;
   else if (saveAction == "yes")
      action = SA_SAVE;
   else if (saveAction == "default")
      action = SA_DEFAULT;
   else
   {
      *pErrMsg = "Unknown save action: " + saveAction;
      return false;
   }

   // clean up
   Error error = r::exec::executeSafely(
                  boost::bind(&RCleanUp, action, status, runLast));
   if (error)
   {
      *pErrMsg = r::endUserErrorMessage(error);
      return false;
   }

   // failsafe in case we don't actually quit as a result of cleanup
   ::exit(0);

   // keep compiler happy
   return true;
}
#endif

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
   s_options = options ;
   s_callbacks = callbacks ;
   
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
      loadInitFile = restartContext().rProfileOnRestore();
   }
   else
   {
      loadInitFile = !s_suspendedSessionPath.exists()
                     || options.rProfileOnResume
                     || r::session::state::packratModeEnabled(
                                                s_suspendedSessionPath);
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
                            &s_internalCallbacks);

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

bool imageIsDirty()
{
   return R_DirtyImage != 0;
}

bool browserContextActive()
{
   return Rf_countContexts(CTXT_BROWSER, 1) > 0;
}
   
void quit(bool saveWorkspace, int status)
{
   // invoke quit
   std::string save = saveWorkspace ? "yes" : "no";
 #ifdef _WIN32
   std::string quitErr;
   bool didQuit = win32Quit(save, 0, true, &quitErr);
   if (!didQuit)
   {
      REprintf((quitErr + "\n").c_str());
      LOG_ERROR_MESSAGE(quitErr);
   }
 #else
   Error error = r::exec::RFunction("q", save, status, true).call();
   if (error)
   {
      REprintf((r::endUserErrorMessage(error) + "\n").c_str());
      LOG_ERROR(error);
   }
 #endif
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

SuppressOutputInScope::SuppressOutputInScope()
{
  s_suppressOutput = true;
}

SuppressOutputInScope::~SuppressOutputInScope()
{
   s_suppressOutput = false;
}

} // namespace utils
   
} // namespace session
} // namespace r
} // namespace rstudio
