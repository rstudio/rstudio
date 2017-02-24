/*
 * SessionConsoleInput.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include "SessionConsoleInput.hpp"
#include "SessionClientEventQueue.hpp"
#include "SessionInit.hpp"
#include "SessionHttpMethods.hpp"
#include "SessionMainProcess.hpp"

#include "modules/SessionConsole.hpp"

#include "modules/connections/SessionConnections.hpp"
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
std::queue<rstudio::r::session::RConsoleInput> s_consoleInputBuffer;

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
   json::Object data ;
   data["prompt"] = prompt ;
   data["history"] = addToHistory;
   bool isDefaultPrompt = 
      prompt == rstudio::r::options::getOption<std::string>("prompt");
   data["default"] = isDefaultPrompt;
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
          rstudio::r::session::isSuspendable(prompt);
}

} // anonymous namespace

// extract console input -- can be either null (user hit escape) or a string
Error extractConsoleInput(const json::JsonRpcRequest& request)
{
   if (request.params.size() == 2)
   {
      // ensure the caller specified the requesting console
      std::string console;
      if (request.params[1].type() == json::StringType)
      {
         console = request.params[1].get_str();
      }
      else
      {
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      }

      // extract the requesting console
      if (request.params[0].is_null())
      {
         addToConsoleInputBuffer(rstudio::r::session::RConsoleInput(console));
         return Success();
      }
      else if (request.params[0].type() == json::StringType)
      {
         // get console input to return to R
         std::string text = request.params[0].get_str();
         addToConsoleInputBuffer(rstudio::r::session::RConsoleInput(text, 
                  console));

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
   // Discard any buffered input
   while (!s_consoleInputBuffer.empty())
      s_consoleInputBuffer.pop();
}

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

   if (!s_consoleInputBuffer.empty())
   {
      *pConsoleInput = s_consoleInputBuffer.front();
      s_consoleInputBuffer.pop();
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
      json::JsonRpcRequest request ;
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
      *pConsoleInput = s_consoleInputBuffer.front();
      s_consoleInputBuffer.pop();
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
   if (consoleInput.cancel || consoleInput.text.find('\n') == std::string::npos)
   {
      s_consoleInputBuffer.push(consoleInput);
      return;
   }

   // split input into list of commands
   boost::char_separator<char> lineSep("\n", "", boost::keep_empty_tokens);
   boost::tokenizer<boost::char_separator<char> > lines(consoleInput.text, lineSep);
   for (boost::tokenizer<boost::char_separator<char> >::iterator
        lineIter = lines.begin();
        lineIter != lines.end();
        ++lineIter)
   {
      // get line
      std::string line(*lineIter);

      // add to buffer
      s_consoleInputBuffer.push(rstudio::r::session::RConsoleInput(
               line, consoleInput.console));
   }
}

} // namespace console_input 
} // namespace session
} // namespace rstudio

