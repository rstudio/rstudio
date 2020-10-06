/*
 * SessionConsoleInput.cpp
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

#include <signal.h>

#include <core/Algorithm.hpp>

#include "SessionConsoleInput.hpp"
#include "SessionClientEventQueue.hpp"
#include "SessionInit.hpp"
#include "SessionHttpMethods.hpp"
#include "SessionMainProcess.hpp"

#include "modules/SessionConsole.hpp"
#include "modules/SessionReticulate.hpp"

#include "modules/connections/SessionConnections.hpp"
#include "modules/environment/SessionEnvironment.hpp"
#include "modules/jobs/SessionJobs.hpp"
#include "modules/overlay/SessionOverlay.hpp"

#include <session/SessionModuleContext.hpp>

#include <r/session/RSession.hpp>
#include <r/ROptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace console_input {
namespace {

// queue of pending console input
using ConsoleInputQueue = std::queue<rstudio::r::session::RConsoleInput>;
ConsoleInputQueue s_consoleInputBuffer;

// manage global state indicating whether R is processing input
volatile sig_atomic_t s_rProcessingInput = 0;

// last prompt we issued
std::string s_lastPrompt;

void setExecuting(bool executing)
{
   s_rProcessingInput = executing;
   module_context::activeSession().setExecuting(executing);
}

void enqueueConsoleInput(const rstudio::r::session::RConsoleInput& input)
{
   json::Object data;
   data[kConsoleText] = input.text + "\n";
   data[kConsoleId]   = input.console;
   ClientEvent inputEvent(client_events::kConsoleWriteInput, data);
   clientEventQueue().add(inputEvent);
}

void consolePrompt(const std::string& prompt, bool addToHistory)
{
   // save the last prompt (for re-issuing)
   s_lastPrompt = prompt;

   // enque the event
   json::Object data;
   data["prompt"] = prompt;
   data["history"] = addToHistory;
   bool isDefaultPrompt = 
      prompt == rstudio::r::options::getOption<std::string>("prompt");
   data["default"] = isDefaultPrompt;
   data["language"] = modules::reticulate::isReplActive() ? "Python" : "R";
   
   ClientEvent consolePromptEvent(client_events::kConsolePrompt, data);
   clientEventQueue().add(consolePromptEvent);
   
   // allow modules to detect changes after execution of previous REPL
   module_context::events().onDetectChanges(module_context::ChangeSourceREPL);

   // call prompt hook
   module_context::events().onConsolePrompt(prompt);
}

bool canSuspend(const std::string& prompt)
{
   return !main_process::haveActiveChildren() && 
          modules::connections::isSuspendable() &&
          modules::overlay::isSuspendable() &&
          modules::environment::isSuspendable() &&
          modules::jobs::isSuspendable() &&
          rstudio::r::session::isSuspendable(prompt);
}

} // anonymous namespace

// extract console input -- can be either null (user hit escape) or a string
Error extractConsoleInput(const json::JsonRpcRequest& request)
{
   if (request.params.getSize() == 2)
   {
      // ensure the caller specified the requesting console
      std::string console;
      if (request.params[1].getType() == json::Type::STRING)
      {
         console = request.params[1].getString();
      }
      else
      {
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      }

      // extract the requesting console
      if (request.params[0].isNull())
      {
         addToConsoleInputBuffer(rstudio::r::session::RConsoleInput(console));
         return Success();
      }
      else if (request.params[0].getType() == json::Type::STRING)
      {
         // get console input to return to R
         std::string text = request.params[0].getString();
         addToConsoleInputBuffer(rstudio::r::session::RConsoleInput(text, console));

         // return success
         return Success();
      }
      else
      {
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      }
   }
   else
   {
      return Error(json::errc::ParamMissing, ERROR_LOCATION);
   }
}

bool executing()
{
   return s_rProcessingInput;
}

void reissueLastConsolePrompt()
{
   consolePrompt(s_lastPrompt, false);
}

void clearConsoleInputBuffer()
{
   s_consoleInputBuffer = ConsoleInputQueue();
}

namespace {

// this function takes the next chunk of (potentially multi-line) pending
// console input in the queue, then splits it into separate pieces of console
// input with one piece of input for each line.
//
// we also fix up indentation if we can determine that the
// code is going to be sent to the reticulate Python REPL.
void fixupPendingConsoleInput()
{
   // get next input
   auto input = s_consoleInputBuffer.front();
   
   // nothing to do if this is a cancel
   if (input.cancel)
      return;
   
   // if this has no newlines, then nothing to do
   auto index = input.text.find('\n');
   if (index == std::string::npos)
      return;
   
   // if we're about to send code to the Python REPL, then
   // we need to fix whitespace in the code before sending
   bool pyReplActive = modules::reticulate::isReplActive();
   
   // pop off current input (we're going to split and re-push now)
   s_consoleInputBuffer.pop();
   
   // does this Python line start an indented block?
   // NOTE: should consider using tokenizer here
   boost::regex reBlockStart(":\\s*(?:#|$)");
   
   // used to detect whitespace-only lines
   boost::regex reWhitespace("^\\s*$");
   
   // keep track of the indentation used for the current block
   // of Python code (default to no indent)
   std::string blockIndent;
   
   // split input into list of commands
   std::vector<std::string> lines = core::algorithm::split(input.text, "\n");
   for (std::size_t i = 0, n = lines.size(); i < n; i++)
   {
      // get current line
      std::string line = lines[i];
      
      // fix up indentation if necessary
      if (pyReplActive)
      {
         // if the line is empty, then replace it with the appropriate indent
         // (exclude last line in selection so that users submitting a whole
         // 'block' will see that block 'closed')
         if (line.empty() && i != n - 1)
         {
            line = blockIndent;
         }
         
         // if this line would exit the reticulate REPL, then update that state
         else if (line == "quit" || line == "exit")
         {
            blockIndent.clear();
            pyReplActive = false;
         }
         
         // if it looks like we're starting a new Python block,
         // then update our indent. perform a lookahead for the
         // next non-blank line, and use that line's indent
         else if (regex_utils::search(line, reBlockStart))
         {
            for (std::size_t j = i + 1; j < n; j++)
            {
               const std::string& lookahead = lines[j];
               
               // skip blank / whitespace-only lines, to allow
               // for cases like:
               //
               //    def foo():
               //
               //        x = 1
               //
               // where there might be empty whitespace between
               // the function definition and the start of its body
               if (regex_utils::match(lookahead, reWhitespace))
                  continue;
               
               blockIndent = string_utils::extractIndent(lookahead);
               break;
            }
         }
         
         // if the indent for this line has _decreased_, then we've
         // closed an inner block; e.g. for something like:
         //
         // def foo():
         //    def bar():
         //       "bar"
         //    "foo"         <--
         //
         // so update the indent in that case
         else if (!regex_utils::match(line, reWhitespace))
         {
            std::string lineIndent = string_utils::extractIndent(line);
            if (lineIndent.length() < blockIndent.length())
               blockIndent = lineIndent;
         }
      }
      else
      {
         // check for a line that would enter the Python REPL
         if (line == "reticulate::repl_python()" ||
             line == "repl_python()")
         {
            pyReplActive = true;
         }
      }
   
      // add to buffer
      s_consoleInputBuffer.push(rstudio::r::session::RConsoleInput(line, input.console));
      
   }
}

void popConsoleInput(rstudio::r::session::RConsoleInput* pConsoleInput)
{
   fixupPendingConsoleInput();
   *pConsoleInput = s_consoleInputBuffer.front();
   s_consoleInputBuffer.pop();
}

} // end anonymous namespace


bool rConsoleRead(const std::string& prompt,
                  bool addToHistory,
                  rstudio::r::session::RConsoleInput* pConsoleInput)
{
   // this is an invalid state in a forked (multicore) process
   if (main_process::wasForked())
   {
      LOG_WARNING_MESSAGE("rConsoleRead called in forked processs");
      return false;
   }

   // r is not processing input
   setExecuting(false);

   // if we have pending console input, send it directly
   if (!s_consoleInputBuffer.empty())
   {
      popConsoleInput(pConsoleInput);
   }
   
   // otherwise prompt and wait for console_input from the client
   else
   {
      // fire console_prompt event (unless we are just starting up, in which
      // case we will either prompt as part of the response to client_init or
      // we shouldn't prompt at all because we are resuming a suspended session)
      if (init::isSessionInitialized())
         consolePrompt(prompt, addToHistory);

      // wait for console_input
      json::JsonRpcRequest request;
      bool succeeded = http_methods::waitForMethod(
                        kConsoleInput,
                        boost::bind(consolePrompt, prompt, addToHistory),
                        boost::bind(canSuspend, prompt),
                        &request);

      // exit process if we failed
      if (!succeeded)
         return false;

      // extract console input. if there is an error during extraction we log it
      // but still return and empty string and true (returning false will cause R
      // to abort)
      Error error = extractConsoleInput(request);
      if (error)
      {
         LOG_ERROR(error);
         *pConsoleInput = rstudio::r::session::RConsoleInput("", "");
      }
      else
      {
         popConsoleInput(pConsoleInput);
      }
   }

   // fire onBeforeExecute and onConsoleInput events if this isn't a cancel
   if (!pConsoleInput->cancel)
   {
      module_context::events().onBeforeExecute();
      module_context::events().onConsoleInput(pConsoleInput->text);
   }

   // we are about to return input to r so set the flag indicating that state
   setExecuting(true);

   // ensure that output resulting from this input goes to the correct console
   if (clientEventQueue().setActiveConsole(pConsoleInput->console))
   {
      module_context::events().onActiveConsoleChanged(pConsoleInput->console,
            pConsoleInput->text);
   }

   ClientEvent promptEvent(client_events::kConsoleWritePrompt, prompt);
   clientEventQueue().add(promptEvent);
   enqueueConsoleInput(*pConsoleInput);

   // always return true (returning false causes the process to exit)
   return true;
}

void addToConsoleInputBuffer(const rstudio::r::session::RConsoleInput& consoleInput)
{
   s_consoleInputBuffer.push(consoleInput);
}

} // namespace console_input 
} // namespace session
} // namespace rstudio

