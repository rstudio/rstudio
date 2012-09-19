/*
 * RSession.cpp
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

#include <r/RExec.hpp>
#include <r/RUtil.hpp>
#include <r/RErrorCategory.hpp>
#include <r/ROptions.hpp>
#include <r/RSourceManager.hpp>
#include <r/RRoutines.hpp>
#include <r/RInterface.hpp>
#include <r/RFunctionHook.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RGraphics.hpp>
#include <r/session/RDiscovery.hpp>

#include "RClientMetrics.hpp"
#include "RSessionState.hpp"
#include "RRestartContext.hpp"
#include "REmbedded.hpp"

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

using namespace core ;

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
   
// have we completed our one-time initialization yet?
bool s_initialized = false;

// are in the middle of servicing a suspend request?
bool s_suspended = false;

// is any quit we receive interactive (i.e. from the invocation of the
// q() function by user)
bool s_quitIsInteractive = true;

// temporarily suppress output
bool s_suppressOuput = false;

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
      Error error = r::exec::executeSafely(boost::bind(
                                        R_RestoreGlobalEnvFromFile,
                                        path.c_str(),
                                        TRUE));
      if (error)   
      {
         reportDeferredDeserializationError(error);
      }
      else
      {
         // print path to console
         std::string aliasedPath = createAliasedPath(globalEnvPath);
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

// forward declare quit hooks so we can register them
#ifdef _WIN32
SEXP win32QuitHook(SEXP call, SEXP op, SEXP args, SEXP rho);
#else
CCODE s_originalPosixQuitFunction;
SEXP posixQuitHook(SEXP call, SEXP op, SEXP args, SEXP rho);
#endif

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

   // initialize console history capacity
   r::session::consoleHistory().setCapacityFromRHistsize();

   // install R tools
   FilePath toolsFilePath = s_options.rSourcePath.complete("Tools.R");
   Error error = r::sourceManager().sourceTools(toolsFilePath);
   if (error)
      return error ;

   // make sure the extra lib paths are at the end
   if (!s_options.rLibsExtra.empty())
   {
      error = r::exec::RFunction(".rs.libPathsAppend",
         core::string_utils::utf8ToSystem(s_options.rLibsExtra.absolutePath()))
                                                                        .call();
      if (error)
         return error;
   }

   // initialize graphics device -- use a stable directory for server mode
   // and temp directory for desktop mode (so that we can support multiple
   // concurrent processes using the same project)
   FilePath graphicsPath;
   if (s_options.serverMode)
   {
      graphicsPath = s_options.scopedScratchPath.complete(kGraphicsPath);
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
      error = r::exec::RFunction(".rs.setCRANRepos",
                                 s_options.rCRANRepos).call();
      if (error)
         return error;
   }

   // hook the quit function if we are on win32 (used so we can
   // call our RCleanUp function which otherwise couldn't be called on
   // Windows where no cleanup hook is supported)
#ifdef _WIN32
   error = r::function_hook::registerReplaceHook("quit", win32QuitHook, NULL);
   if (error)
      return error;
#else
   error = r::function_hook::registerReplaceHook("quit",
                                                 posixQuitHook,
                                                 &s_originalPosixQuitFunction);
#endif


   // complete embedded r initialization
   error = r::session::completeEmbeddedRInitialization();
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
      FilePath serverOptionsFilePath =  s_options.rSourcePath.complete(
                                                         "ServerOptions.R");
      return r::sourceManager().sourceLocal(serverOptionsFilePath);
   }
   else
   {
      return Success();
   }
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

            // log the error
            LOG_ERROR(error);
            
            // terminate the session (use suicide so that no special
            // termination code runs -- i.e. call to setAbnormalEnd(false)
            // or call to client::quitSession)
            R_Suicide(error.code().message().c_str());
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
      RConsoleInput consoleInput;
      if ( s_callbacks.consoleRead(promptString, addToHistory, &consoleInput) )
      {
         // add prompt to console actions (we do this after consoleRead
         // completes so that we don't send both a console prompt event
         // AND include the same prompt in the actions history)
         consoleActions().add(kConsoleActionPrompt, prompt);

         if (consoleInput.cancel)
         {
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
            if (addToHistory)
               consoleHistory().add(consoleInput.text);

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
   catch(r::exec::InterruptException)
   {
      r::exec::setInterruptsPending(true);
      r::exec::checkUserInterrupt();
   }
   CATCH_UNEXPECTED_EXCEPTION

   // if we get this far then this was an unexpected error
   R_Suicide("Unexpected error reading console input");
      
   return 0 ; // keep compiler happy
}
   
void RWriteConsoleEx (const char *buf, int buflen, int otype)
{
   try
   {
      if (!s_suppressOuput)
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
         // transform into FilePath
         std::string path = URL.substr(filePrefix.length());
         FilePath filePath(r::util::fixPath(path));
         
         // fire browseFile
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

// prompt to save changes (will Rf_jump_to_toplevel if there is an error)
SA_TYPE saveAsk()
{
   // NOTE: we don't check R_Interactive (as Rstd_CleanUp does) here because 
   // we are always interactive. If we ever support a non-interactive mode
   // then we need to update the logic to reflect this

   // TODO: this will leak if the user executes a cancel. we should wrap
   // this in a try/catch(JumpToTopException) construct
   std::string prompt = "Save workspace image to " +
                        createAliasedPath(rSaveGlobalEnvironmentFilePath()) +
                        "? [y/n/c]: ";

   SA_TYPE saveact = SA_SAVEASK;
   
   CONSOLE_BUFFER_CHAR buf[1024];
qask:
   // prompt the user 
   R_ClearerrConsole();
   R_FlushConsole();
   RReadConsole(prompt.c_str(), buf, 128, 0);
   
   switch (buf[0]) 
   {
      case 'y':
      case 'Y':
         saveact = SA_SAVE;
         break;
      case 'n':
      case 'N':
         saveact = SA_NOSAVE;
         break;
      case 'c':
      case 'C':
         // NOTE: this does a non-local jump to the top-level context,
         // bypassing normal c++ stack-unwinding. therefore, no 
         // reliance on c++ destructors for cleanup is possible
         // within this function or any other function in our codebase
         // which calls it w/ a SA_SAVEASK or SA_DEFAULT parameter
         Rf_jump_to_toplevel();
         break;
      default:
         goto qask;
   }
   
   return saveact;
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
//
void RCleanUp(SA_TYPE saveact, int status, int runLast)
{
   // NOTE: This routine partially duplicates the code from the internal
   // R_CleanUp routine. This is done so that we can inject our own 
   // cleanup code into the shutdown sequence. we may wish to consider
   // porting all of R_CleanUp into this function so we can make all of
   // the logic more clear and transparent
   try
   {
      // set to default if requested
      if (saveact == SA_DEFAULT)
         saveact = SaveAction ;
      
      // prompt user to resolve SA_SAVEASK into SA_SAVE or SA_NOSAVE
      if (saveact == SA_SAVEASK) 
      {
         if (imageIsDirty() || !s_options.alwaysSaveHistory())
            saveact = saveAsk();
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
         
         // print warnings (do this here even though R does it within 
         // RCleanUp because warnings which are enqued after the quit
         // callback (just below) are never seen by the client
         r::exec::printWarnings();
         
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

// NOTE: on Win32 we fully replace the quit function so that
// we can call our R_CleanUp hook (Windows R doesn't allow hooking
// of the cleanup function). The implementation below is identical
// to do_quit in main.c save for calling our RCleanUp function rather
// than R's R_CleanUp function (which will ultimatley be called by our
// function after it does it's work)
#ifdef _WIN32
extern "C" Rboolean R_Interactive;/* TRUE during interactive use*/
#define _(String) String
SEXP win32QuitHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   if (s_quitIsInteractive)
   {
      if (!s_callbacks.handleUnsavedChanges())
         r::exec::error("User cancelled quit operation");
   }

   const char *tmp;
   SA_TYPE ask=SA_DEFAULT;
   int status, runLast;

   /* if there are any browser contexts active don't quit */
   if(Rf_countContexts(CTXT_BROWSER, 1)) {
   Rf_warning(_("cannot quit from browser"));
   return R_NilValue;
   }
   if( !Rf_isString(CAR(args)) )
   Rf_errorcall(call, _("one of \"yes\", \"no\", \"ask\" or \"default\" expected."));
   tmp = CHAR(STRING_ELT(CAR(args), 0)); /* ASCII */
   if( !strcmp(tmp, "ask") ) {
   ask = SA_SAVEASK;
   if(!R_Interactive)
     Rf_warning(_("save=\"ask\" in non-interactive use: command-line default will be used"));
   } else if( !strcmp(tmp, "no") )
   ask = SA_NOSAVE;
   else if( !strcmp(tmp, "yes") )
   ask = SA_SAVE;
   else if( !strcmp(tmp, "default") )
   ask = SA_DEFAULT;
   else
   Rf_errorcall(call, _("unrecognized value of 'save'"));
   status = Rf_asInteger(CADR(args));
   if (status == NA_INTEGER) {
   Rf_warning(_("invalid 'status', 0 assumed"));
   runLast = 0;
   }
   runLast = Rf_asLogical(CADDR(args));
   if (runLast == NA_LOGICAL) {
   Rf_warning(_("invalid 'runLast', FALSE assumed"));
   runLast = 0;
   }
   /* run the .Last function. If it gives an error, will drop back to main
     loop. */

   // run our cleanup function rather than R's
   RCleanUp(ask, status, runLast);

   exit(0);
   /*NOTREACHED*/
}
#else
SEXP posixQuitHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
   if (s_quitIsInteractive)
   {
      if (!s_callbacks.handleUnsavedChanges())
         r::exec::error("User cancelled quit operation");
   }

   return s_originalPosixQuitFunction(call, op, args, rho);
}
#endif

   
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

   // Append our special extra lib path to R_LIBS
   if (!s_options.rLibsExtra.empty())
   {
      std::string rLibs = core::system::getenv("R_LIBS");
      if (!rLibs.empty())
   #ifdef _WIN32
         rLibs.append(";");
   #else
         rLibs.append(":");
   #endif
      rLibs.append(core::string_utils::utf8ToSystem(
                                    s_options.rLibsExtra.absolutePath()));
      core::system::setenv("R_LIBS", rLibs);
   }

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
   FilePath userScratchPath = s_options.userScratchPath;
   s_suspendedSessionPath = userScratchPath.complete("suspended-session");  
   
   // initialize restart context
   restartContext().initialize(s_options.scopedScratchPath,
                               s_options.sessionPort);

   // register browseURL method
   R_CallMethodDef browseURLMethod ;
   browseURLMethod.name = "rs_browseURL";
   browseURLMethod.fun = (DL_FUNC)rs_browseURL;
   browseURLMethod.numArgs = 1;
   r::routines::addCallMethod(browseURLMethod);

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

   // run R
   bool loadInitFile = restartContext().hasSessionState()
                       || !s_suspendedSessionPath.exists()
                       || options.rProfileOnResume;

   bool quiet =  !restartContext().hasSessionState() &&
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
      s_callbacks.suspended();
   
      // clean up but don't save workspace or runLast because we have
      // been suspended
      RCleanUp(SA_NOSAVE, 0, FALSE);
      
      // keep compiler happy (this line will never execute)
      return true;
   }
   else
   {
      return false;
   }
}

bool suspend(bool force)
{
   return suspend(RSuspendOptions(), s_suspendedSessionPath, false, force);
}

void suspendForRestart(const RSuspendOptions& options)
{
   suspend(options,
           RestartContext::createSessionStatePath(s_options.scopedScratchPath,
                                                  s_options.sessionPort),
           true,  // disable save compression
           true); // force suspend
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
   
void quit(bool saveWorkspace)
{
   // denote this as a non-interactive quit (so we don't need to
   // prompt for unsaved changes)
   s_quitIsInteractive = false;
   core::scope::SetOnExit<bool> resetOnExit(&s_quitIsInteractive, true);

   // invoke quit
   std::string save = saveWorkspace ? "yes" : "no";
   Error error = r::exec::RFunction("q", save, 0, true).call();
   if (error)
      LOG_ERROR(error);
}
   
namespace utils {
   
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
  s_suppressOuput = true;
}

SuppressOutputInScope::~SuppressOutputInScope()
{
   s_suppressOuput = false;
}

} // namespace utils
   
} // namespace session
} // namespace r
