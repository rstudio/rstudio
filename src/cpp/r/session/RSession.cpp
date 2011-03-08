/*
 * RSession.cpp
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
#include <core/system/System.hpp>
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
#include "REmbedded.hpp"

#include "graphics/RGraphicsUtils.hpp"
#include "graphics/RGraphicsDevice.hpp"

#include <Rembedded.h>
#include <R_ext/Utils.h>
#include <R_ext/Rdynload.h>

// get rid of windows TRUE and FALSE definitions
#undef TRUE
#undef FALSE

using namespace core ;

namespace r {
namespace session {

namespace {
   
// options
ROptions s_options;

// callbacks
RCallbacks s_callbacks;
   
// client-state path
FilePath s_clientStatePath;
   
// session state path
FilePath s_suspendedSessionPath ; 
   
// function for deferred deserialization actions. this encapsulates parts of 
// the initialization process that are potentially highly latent. this allows
// clients to bring their UI up and then receive an event indicating that the
// latent deserialization actions are taking place
boost::function<Error()> s_deferredDeserializationAction;
   
// have we completed our one-time initialization yet?
bool s_initialized = false;

// are in the middle of servicing a suspend request?
bool s_suspended = false;

// temporarily suppress output
bool s_suppressOuput = false;
class SuppressOutputInScope
{
public:
   SuppressOutputInScope() { s_suppressOuput = true; }
   ~SuppressOutputInScope() { s_suppressOuput = false; }
};

// default save action
const SA_TYPE kDefaultSaveAction = SA_SAVEASK;

// R history file
const char * const kRHistory = ".Rhistory";
   
   
class SerializationCallbackScope : boost::noncopyable
{
public:
   SerializationCallbackScope(int action)
   {
      s_callbacks.serialization(action);
   }
   
   ~SerializationCallbackScope()
   {
      try {
         s_callbacks.serialization(kSerializationActionCompleted);
      } catch(...) {}
   }
};
   
 
bool saveSessionState()
{
   // notify client of serialization status
   SerializationCallbackScope cb(kSerializationActionSuspendSession);
   
   // suppress interrupts which occur during saving
   r::exec::IgnoreInterruptsScope ignoreInterrupts;
   
   // save 
   return r::session::state::save(s_suspendedSessionPath);
}
   
Error deferredRestoreSessionState(
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
   SuppressOutputInScope suppressOutput;
   
   // restore
   return deferredRestoreAction();
}

Error saveDefaultGlobalEnvironment()
{
   // notify client of serialization status
   SerializationCallbackScope cb(kSerializationActionSaveDefaultWorkspace);

   // suppress interrupts which occur during saving
   r::exec::IgnoreInterruptsScope ignoreInterrupts;
         
   // save global environment
   Error error = r::exec::executeSafely(boost::bind(R_SaveGlobalEnvToFile,
                                                       "~/.RData"));
   
   if (error)
   {
      return error;
   }
   else
   {
      return Success();
   }
}
   
Error restoreDefaultGlobalEnvironment()
{
   // notify client of serialization status
   SerializationCallbackScope cb(kSerializationActionLoadDefaultWorkspace);
   
   // ignore interrupts which occur during restoring of the global env
   // the restoration will run to completion in any case and then the
   // next thing the user does will be "interrupted" -- clearly not
   // what they intended
   r::exec::IgnoreInterruptsScope ignoreInterrupts;
   
   // restore the default global environment if there is one
   if (s_options.userHomePath.complete(".RData").exists())
   {
      Error error = r::exec::executeSafely(boost::bind(
                                                   R_RestoreGlobalEnvFromFile,
                                                   "~/.RData", 
                                                   TRUE));
      if (error)   
         return error;

      Rprintf("[Workspace restored from ~/.RData]\n\n");
   }

   return Success();
}

// save our session state when the quit function is called
void saveHistoryAndClientState()
{
   // save history (log errors)
   FilePath historyPath = s_options.userHomePath.complete(kRHistory);
   Error error = consoleHistory().saveToFile(historyPath);
   if (error)
      LOG_ERROR(error);

   // commit persistent client state
   r::session::clientState().commit(ClientStateCommitPersistentOnly,
                                    s_clientStatePath);
}


// automatically create R_LIBS_USER if it doesen't already exist
// and there is nowhere else writeable that we can install packages
// to (this enables users to override this behavior by adding an
// entry to their .libPaths is .Rprofile)
void initRLibsUser()
{
   // get the full r libs user path
   std::string rLibsUser = core::system::getenv("R_LIBS_USER");
   rLibsUser = r::util::expandFileName(rLibsUser);
   FilePath rLibsUserPath(rLibsUser);

   // if it already exists then we are done
   if (rLibsUserPath.exists())
      return;

   // auto-create R_LIBS_USER if the existing .libPaths are not writeable
   bool autoCreate = true;
   std::vector<std::string> libPaths;
   Error error = r::exec::RFunction(".libPaths").call(&libPaths);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // if we are on windows then auto-create if there is only one libpath
#ifdef _WIN32

   if (libPaths.size() <= 1)
      autoCreate = true;

   // on OSX & Unix we check for the existence of at least one writeable libpath
#else

   for (std::vector<std::string>::const_iterator
         it = libPaths.begin();
         it != libPaths.end();
         ++it)
   {
      FilePath libPath(*it);

      FilePath testWriteablePath = libPath.complete(".rstudio-test-writeable");
      Error error = writeStringToFile(testWriteablePath, "abc123");
      if (!error)
      {
         // found a writeable path, don't auto-create
         autoCreate = false;

         // cleanup the file
         Error error = testWriteablePath.remove();
         if (error)
            LOG_ERROR(error);

         break;
      }
   }

#endif

   if (autoCreate)
   {
      // create the path
      Error error = rLibsUserPath.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // add it to our libPaths
      error = r::exec::RFunction(".libPaths", rLibsUserPath.absolutePath()).call();
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }
}

} // anonymous namespace
  

const int kSerializationActionSaveDefaultWorkspace = 1;
const int kSerializationActionLoadDefaultWorkspace = 2;
const int kSerializationActionSuspendSession = 3;
const int kSerializationActionResumeSession = 4;
const int kSerializationActionCompleted = 5;

// forward declare win32QuitHook so we can register it
#ifdef _WIN32
SEXP win32QuitHook(SEXP call, SEXP op, SEXP args, SEXP rho);
#endif

// one-time per session initialization
Error initialize()
{
   // install R tools
   FilePath toolsFilePath = s_options.rSourcePath.complete("Tools.R");
   Error error = r::sourceManager().sourceLocal(toolsFilePath);
   if (error)
      return error ;

   // append any extra library to the libpaths
   if (!s_options.rLibsExtra.empty())
   {
      error = r::exec::RFunction(".rs.libPathsAppend",
                                 s_options.rLibsExtra.absolutePath()).call();
      if (error)
         return error;
   }

   // create R_LIBS_USER if there is nowhere else defined to install packages
   initRLibsUser();

   // initialize graphics device
   FilePath graphicsPath = s_options.userScratchPath.complete("graphics");
   error = graphics::device::initialize(graphicsPath, s_callbacks.locator);
   if (error) 
      return error;
   
   // restore client state
   session::clientState().restore(s_clientStatePath);
      
   // restore suspended session if we have one
   bool wasResumed = false;
   
   if (s_suspendedSessionPath.exists())
   {
      std::string errorMessages ;
      
      // restore session
      {
         // don't show output during deserialization (packages loaded
         // during deserialization sometimes print messages)
         SuppressOutputInScope suppressOutput;
         
         // deserialize session. if any part of this fails then the errors
         // will be logged and error messages will be returned in the passed
         // errorMessages buffer (this mechanism is used because we generally
         // suppress output during restore but we need a way for the error
         // messages to make their way back to the user)
         boost::function<Error()> deferredRestoreAction;
         r::session::state::restore(s_suspendedSessionPath, 
                                    &deferredRestoreAction, 
                                    &errorMessages);
         
         if (deferredRestoreAction)
         {
            s_deferredDeserializationAction = boost::bind(
                                                deferredRestoreSessionState, 
                                                deferredRestoreAction);
         }
      }
      
      // show any error messages
      if (!errorMessages.empty())
      {
         REprintf(errorMessages.c_str());
         REprintf("IMPORTANT: This error may have resulted in data loss. "
                  "Please report it immediately!\n");
      }
      
      // note we were resumed
      wasResumed = true;
   }  
   // new session
   else
   {  
      // restore console history (for serialized sessions this was already 
      // handled as part of deserializeSession)
      FilePath historyFilePath = s_options.userHomePath.complete(kRHistory);
      error = consoleHistory().loadFromFile(historyFilePath, false);
      if (error)
         LOG_ERROR(error);
      
      // defer loading of global environment
      s_deferredDeserializationAction = restoreDefaultGlobalEnvironment;
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

std::string historyInputFilter(const std::string& input)
{
   // we added this late in the feature parity cycle so want to make sure
   // there is no chance that it will result in an unexpected exception
   try
   {
      // filter password command line parameters (e.g for svn)

      if (input.find("--password") != std::string::npos)
      {
         const boost::regex pattern("--password[ =]+[\\\"A-Za-z0-9_]+");
         return boost::regex_replace(input, pattern, "--password XXXXXXXX");
      }
      else
      {
         return input;
      }
   }
   catch(...)
   {
      return input;
   }
}

int RReadConsole (const char *prompt,
                  CONSOLE_BUFFER_CHAR* buf,
                  int buflen,
                  int hist)
{
   try
   {
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
         
         // ensure only one initialization 
         s_initialized = true;
      }
         
      // get the next input
      bool addToHistory = (hist == 1);
      RConsoleInput consoleInput;
      if ( s_callbacks.consoleRead(prompt, addToHistory, &consoleInput) )
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
            // determine the actual input to return to R (default to input
            // recieved but can be distinct if we perform a shell escape)
            std::string rInput = consoleInput.text;

            if (s_options.shellEscape)
            {
               if (rInput.length() > 1 && rInput[0] == '!')
               {
                     // extract, trim, and escape command
                     std::string command = rInput.substr(1);
                     boost::algorithm::trim(command);
                     boost::algorithm::replace_all(command, "\\", "\\\\");
                     boost::algorithm::replace_all(command, "\"", "\\\"");
                     boost::algorithm::replace_all(command, "\n", "\\n");

                     // warn if this is a !cd
                     if (boost::algorithm::starts_with(command, "cd"))
                     {
                        r::exec::warning(
                              "!cd does not change the R working directory");
                     }

                     // form revised input
                     rInput = "system(\"" + command + "\")";
               }
            }

            // refresh source if necessary (no-op in production)
            r::sourceManager().reloadIfNecessary();
            
            // ensure that our input fits within the buffer
            std::string::size_type maxLen = buflen - 2; // for \n\0
            if (rInput.length() > maxLen)
               rInput.resize(maxLen);
            std::string::size_type inputLen = rInput.length();
            
            // see if we need to filter the input (e.g. to remove passwords)
            std::string filteredInput = historyInputFilter(consoleInput.text);

            // add to console actions and history (if requested). note that
            // we add the user's input rather than any tranformed input we
            // created as a result of a shell escape
            consoleActions().add(kConsoleActionInput, filteredInput);
            if (addToHistory)
               consoleHistory().add(filteredInput);

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
extern "C" void RBrowseURL(char ** url)
{
   try
   {
      // copy to std::string
      std::string URL(url[0]);
      
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
   
   SA_TYPE saveact = SA_SAVEASK;
   
   CONSOLE_BUFFER_CHAR buf[1024];
qask:
   // prompt the user 
   R_ClearerrConsole();
   R_FlushConsole();
   RReadConsole("Save workspace image? [y/n/c]: ",
                buf, 128, 0);
   
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
         saveact = kDefaultSaveAction ;
      
      // prompt user to resolve SA_SAVEASK into SA_SAVE or SA_NOSAVE
      if (saveact == SA_SAVEASK) 
         saveact = saveAsk();
      
      // if the session was quit by the user then run our termination code
      bool sessionQuitByUser = (saveact != SA_SUICIDE) && !s_suspended ;
      if (sessionQuitByUser)
      {
         // run last if requested (can throw error)
         if (runLast)
            R_dot_Last();
         
         // save environment and history 
         bool savedEnvironment = false;
         if (saveact == SA_SAVE)
         {
            // attempt save
            if (R_DirtyImage)
            {
               // attempt to save global environment. raise error (longjmp 
               // back to REPL) if there was a problem saving
               Error error = saveDefaultGlobalEnvironment();
               if (error)
                  r::exec::error(r::endUserErrorMessage(error));
            }
            
            // update state
            savedEnvironment = true; // flag for quit callback
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

         // save other persistent session state (history and client state)
         saveHistoryAndClientState();

         // clear display (closes the device). need to do this here
         // so that all of the graphics files are deleted
         r::session::graphics::display().clear();
         
         // print warnings (do this here even though R does it within 
         // RCleanUp because warnings which are enqued after the quit
         // callback (just below) are never seen by the client
         r::exec::printWarnings();
         
         // notify client that the session has been quit
         s_callbacks.quit(savedEnvironment);
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
#define CTXT_BROWSER 16 // from Defn.h
#define _(String) String
SEXP win32QuitHook(SEXP call, SEXP op, SEXP args, SEXP rho)
{
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

   // R_LIBS_USER
   if (!s_options.rLibsUser.empty())
      core::system::setenv("R_LIBS_USER", s_options.rLibsUser);
   
   // set compatible graphics engine version
   int engineVersion = s_options.rCompatibleGraphicsEngineVersion;
   graphics::setCompatibleEngineVersion(engineVersion);

   // set graphics and client state paths
   s_clientStatePath = s_options.userScratchPath.complete("client-state");
   
   // set source reloading behavior
   sourceManager().setAutoReload(options.autoReloadSource);
   
   // set console history size
   consoleHistory().setCapacity(options.consoleHistorySize);
   
   // initialize suspended session path
   FilePath userScratchPath = s_options.userScratchPath;
   s_suspendedSessionPath = userScratchPath.complete("suspended-session");  
   
   // register browseURL method
   R_CMethodDef browseURLMethod ;
   browseURLMethod.name = "rs_browseURL";
   browseURLMethod.fun = (DL_FUNC)&RBrowseURL;
   browseURLMethod.numArgs = 1;
   R_NativePrimitiveArgType types[1] = {STRSXP} ;
   browseURLMethod.types = types;
   browseURLMethod.styles = NULL;
   r::routines::addCMethod(browseURLMethod);

   // register createUUID method
   R_CallMethodDef createUUIDMethodDef ;
   createUUIDMethodDef.name = "rs_createUUID" ;
   createUUIDMethodDef.fun = (DL_FUNC) rs_createUUID ;
   createUUIDMethodDef.numArgs = 0;
   r::routines::addCallMethod(createUUIDMethodDef);

   // run R
   bool newSession = !s_suspendedSessionPath.exists();
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
                            newSession,
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
      Error error = s_deferredDeserializationAction();
      if (error)
      {
         // log error
         LOG_ERROR(error);
         
         // report to user
         std::string errMsg = r::endUserErrorMessage(error);
         REprintf((errMsg + "\n").c_str());
         REprintf("IMPORTANT: This error may have resulted in data loss. "
                  "Please report it immediately!\n");
      }
      
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
   
bool suspend(bool force)
{
   // commit all client state
   r::session::clientState().commit(ClientStateCommitAll, s_clientStatePath);
   
   // save the session state. errors are handled internally and reported
   // directly to the end user and written to the server log.
   bool suspend = saveSessionState();
      
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
   
void quit(bool saveWorkspace)
{   
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
   FilePath filePath(r::util::fixPath(filename) + "." + extension);
   return filePath;
}

} // namespace utils
   
} // namespace session
} // namespace r
