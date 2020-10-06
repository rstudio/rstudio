/*
 * RStdCallbacks.cpp
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

#define R_INTERNAL_FUNCTIONS

#include <gsl/gsl>

#include <boost/function.hpp>
#include <boost/regex.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RSourceManager.hpp>
#include <r/RUtil.hpp>
#include <r/session/RClientState.hpp>
#include <r/session/RConsoleActions.hpp>
#include <r/session/RConsoleHistory.hpp>
#include <r/session/RSession.hpp>
#include <r/session/RSessionState.hpp>

#include <core/FileSerializer.hpp>
#include <core/RegexUtils.hpp>

#include "RInit.hpp"
#include "REmbedded.hpp"
#include "RStdCallbacks.hpp"
#include "RQuit.hpp"
#include "RSuspend.hpp"

#include "graphics/RGraphicsDevDesc.hpp"
#include "graphics/RGraphicsUtils.hpp"
#include "graphics/RGraphicsDevice.hpp"
#include "graphics/RGraphicsPlotManager.hpp"

#include <Rembedded.h>

extern "C" {

#ifndef _WIN32
SA_TYPE SaveAction;
#else
__declspec(dllimport) SA_TYPE SaveAction;
#endif

}

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

namespace {

// have we completed our one-time initialization yet?
bool s_initialized = false;

// main callbacks; pointer to statically allocated memory
RCallbacks s_callbacks;

// internal callbacks for delegation
InternalCallbacks s_internalCallbacks;

// temporarily suppress output
bool s_suppressOutput = false;

class JumpToTopException
{
};

FilePath rSaveGlobalEnvironmentFilePath()
{
   FilePath rEnvironmentDir = utils::rEnvironmentDir();
   return rEnvironmentDir.completePath(".RData");
}

void rSuicideError(const Error& error)
{
   // provide error message if the error was unexpected
   std::string errorStr;
   if (!error.isExpected())
      errorStr = core::log::writeError(error);

   rSuicide(errorStr);
}

SA_TYPE saveAsk()
{
   try
   {
      // end user prompt
      std::string wsPath = FilePath::createAliasedPath(
            rSaveGlobalEnvironmentFilePath(), utils::userHomePath());
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
         RReadConsole(prompt.c_str(), &(inputBuffer[0]),
                      gsl::narrow_cast<int>(inputBuffer.size()), 0);
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
      throw r::exec::RErrorException(error.getMessage());
}
   
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
         REprintf("%s\n", "User cancelled quit operation");
         return false;
      }

      // on win32 we will actually assume responsibility for the
      // quit function entirely (so we can call our internal cleanup
      // handler code)
#ifdef _WIN32
      std::string quitErr;
      bool didQuit = win32Quit(input, &quitErr);
      if (!didQuit)
         REprintf("%s\n", quitErr.c_str());

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
   std::string path = string_utils::utf8ToSystem(globalEnvPath.getAbsolutePath());
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



} // anonymous namespace

bool imageIsDirty()
{
   return R_DirtyImage != 0;
}

void setImageDirty(bool imageDirty)
{
   R_DirtyImage = imageDirty ? 1 : 0;
}

void setRCallbacks(const RCallbacks& callbacks)
{
   s_callbacks = callbacks;
}

RCallbacks& rCallbacks()
{
   return s_callbacks;
}

InternalCallbacks* stdInternalCallbacks()
{
   return &s_internalCallbacks;
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
            LOG_ERROR(error);
            
            // terminate the session (use suicide so that no special
            // termination code runs -- i.e. call to setAbnormalEnd(false)
            // or call to client::quitSession)
            rSuicideError(error);
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
      if (s_callbacks.consoleRead(promptString, addToHistory, &consoleInput) )
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

         return 1;
      }
      else
      {
         // buffer not ready
         return 0;
      }
   }
   catch(r::exec::InterruptException&)
   {
      // set interrupts pending
      r::exec::setInterruptsPending(true);

      // call R interrupt handler. note that previously we used
      // r::exec::checkUserInterrupt(), but this also has the side effect of
      // calling R_ProcessEvents(). this means that modules with registered
      // event handlers may get a chance to handle our interrupt, and so the
      // interrupt will be handled in an incorrect context. this can lead to
      // issues like the process exiting prematurely. see:
      //
      //     https://github.com/rstudio/rstudio/issues/5108
      //
      // for one such example.
      //
      // note that on Windows we let the regular R_ProcessEvents() machinery
      // handle the interrupt as we noticed calling Rf_onintr() in that
      // environment could cause a crash
#ifndef _WIN32
      Rf_onintr();
#endif

      // buffer not ready
      return 0;
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
      
   return 0; // keep compiler happy
}
   
void RShowMessage(const char* msg)
{
   try 
   {
      s_callbacks.showMessage(msg);
   }
   CATCH_UNEXPECTED_EXCEPTION
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
         s_callbacks.consoleWrite(output, otype);
      }
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
   return 1;
}

void RBusy(int which)   
{
   try
   {
      // synchronize locale whenever R busy state changes
      // (done relatively eagerly to ensure synchronization
      // happens on each REPL iteration after user code is run)
      r::util::synchronizeLocale();

      // invoke callback
      s_callbacks.busy(which == 1);
   }
   CATCH_UNEXPECTED_EXCEPTION 
}

// NOTE: Win32 doesn't receive this callback
int RChooseFile (int newFile, char *buf, int len) 
{
   try 
   {
      FilePath filePath = s_callbacks.chooseFile(newFile == TRUE);
      if (!filePath.isEmpty())
      {
         // get absolute path
         std::string absolutePath = filePath.getAbsolutePath();
         
         // trunate file if it is too long
         std::string::size_type maxLen = len - 1;
         if (absolutePath.length() > maxLen)
            absolutePath.resize(maxLen);
         
         // copy the file to the buffer
         absolutePath.copy(buf, maxLen);
         buf[absolutePath.length()] = '\0';
         
         // return the length of the filepath buffer
         return gsl::narrow_cast<int>(absolutePath.length());
      }
      else
      {
         return 0;
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   // error if we got this far
   return 0;
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
         FilePath filePath = utils::safeCurrentPath().completePath(fixedPath);
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
      std::vector<std::string> commands;
      Error error = sexp::extract(CAR(args), &commands);
      if (error)
         throw r::exec::RErrorException(error.getMessage());
      
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


// NOTE: Win32 doesn't receive this callback
void RSuicide(const char* s)
{
   s_callbacks.suicide(s);
   s_internalCallbacks.suicide(s);
}

void rSuicide(const std::string& msg)
{
   // log abort message if we are in desktop mode
   if (!utils::isServerMode())
   {
      FilePath abendLogPath = utils::logPath().completePath(
         "rsession_abort_msg.log");
      Error error = core::writeStringToFile(abendLogPath, msg);
      if (error)
         LOG_ERROR(error);
   }

   R_Suicide(msg.c_str());
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
         saveact = SaveAction;
      
      // prompt user to resolve SA_SAVEASK into SA_SAVE or SA_NOSAVE
      if (saveact == SA_SAVEASK) 
      {
         if (imageIsDirty() || !utils::alwaysSaveHistory())
            saveact = saveAsk(); // can Rf_jump_to_toplevel()
         else
            saveact = SA_NOSAVE; // auto-resolve to no save when not dirty
      }

      // if the session was quit by the user then run our termination code
      bool sessionQuitByUser = (saveact != SA_SUICIDE) && !suspended();
      if (sessionQuitByUser)
      {
         // run last if requested (can throw error)
         if (runLast)
            R_dot_Last();
         
         // save history if we either always save history or saveact == SA_SAVE
         if (utils::alwaysSaveHistory() || saveact == SA_SAVE)
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
         if (!r::session::state::destroy(utils::suspendedSessionPath()))
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

namespace utils {

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

