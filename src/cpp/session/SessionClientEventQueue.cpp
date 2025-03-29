/*
 * SessionClientEventQueue.cpp
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

#include "SessionClientEventQueue.hpp"

#include <shared_core/json/Json.hpp>

#include <core/AnsiEscapes.hpp>
#include <core/BoostThread.hpp>
#include <core/Thread.hpp>
#include <core/StringUtils.hpp>

#include <r/RExec.hpp>

#include <r/session/RConsoleActions.hpp>

#include <session/prefs/UserPrefs.hpp>

#include "SessionHttpMethods.hpp"
#include "modules/SessionConsole.hpp"


#define kNeverMatch "^(?!)$"

using namespace rstudio::core;

namespace rstudio {
namespace session {
 
namespace {

ClientEventQueue* s_pClientEventQueue = nullptr;

std::string s_highlightConditionsPref;

boost::regex s_reErrorPrefix(kNeverMatch);
boost::regex s_reWarningPrefix(kNeverMatch);
boost::regex s_reInAdditionPrefix(kNeverMatch);

bool annotateErrors()
{
   return s_highlightConditionsPref != kConsoleHighlightConditionsNone;
}

bool annotateWarnings()
{
   return
         s_highlightConditionsPref == kConsoleHighlightConditionsErrorsWarnings ||
         s_highlightConditionsPref == kConsoleHighlightConditionsErrorsWarningsMessages;
}

} // end anonymous namespace

void initializeClientEventQueue()
{
   BOOST_ASSERT(s_pClientEventQueue == nullptr);
   s_pClientEventQueue = new ClientEventQueue();
}

void finishInitializeClientEventQueue()
{
   s_highlightConditionsPref = prefs::userPrefs().consoleHighlightConditions();
   if (s_highlightConditionsPref != kConsoleHighlightConditionsNone)
   {
      Error error;

      std::string reErrorPrefix;
      error = r::exec::RFunction(".rs.reErrorPrefix")
            .call(&reErrorPrefix);
      if (error)
         LOG_ERROR(error);

      std::string reWarningPrefix;
      error = r::exec::RFunction(".rs.reWarningPrefix")
            .call(&reWarningPrefix);
      if (error)
         LOG_ERROR(error);

      std::string reInAdditionPrefix;
      error = r::exec::RFunction(".rs.reInAdditionPrefix")
            .call(&reInAdditionPrefix);
      if (error)
         LOG_ERROR(error);

      s_reErrorPrefix      = boost::regex(reErrorPrefix);
      s_reWarningPrefix    = boost::regex(reWarningPrefix);
      s_reInAdditionPrefix = boost::regex(reInAdditionPrefix);
   }
}

ClientEventQueue& clientEventQueue()
{
   return *s_pClientEventQueue;
}
   
ClientEventQueue::ClientEventQueue()
   :  pMutex_(new boost::mutex()),
      pWaitForEventCondition_(new boost::condition()),
      lastEventAddTime_(boost::posix_time::not_a_date_time),
      consoleOutput_(client_events::kConsoleWriteOutput, true),
      consoleErrors_(client_events::kConsoleWriteError, true),
      buildOutput_(client_events::kBuildOutput, false)
{
   // buffered outputs (required for parts that might overflow)
   bufferedOutputs_.push_back(&consoleOutput_);
   bufferedOutputs_.push_back(&consoleErrors_);
   bufferedOutputs_.push_back(&buildOutput_);
}

bool ClientEventQueue::setActiveConsole(const std::string& console)
{
   bool changed = false;
   LOCK_MUTEX(*pMutex_)
   {
      if (activeConsole_ != console)
      {
         // flush events to the previous console
         flushAllBufferedOutput();
         
         // switch to the new one
         activeConsole_ = console;
         changed = true;
      }
   }
   END_LOCK_MUTEX
   return changed;
}

namespace {

void annotateError(std::string* pOutput, bool allowGroupAll)
{
   boost::smatch match;
   if (boost::regex_search(*pOutput, match, s_reErrorPrefix))
   {
      // Insert highlight markers around 'Error'.
      // Note that, because the word may have been translated, we just look
      // for the first colon or space following the location where the error
      // prefix was matched.
      auto matchEnd = match[0].second - pOutput->begin();
      auto highlightStart = match[0].first - pOutput->begin();
      auto highlightEnd = pOutput->find_first_of(": ", highlightStart);
      if (highlightEnd != std::string::npos)
      {
         pOutput->insert(highlightEnd, kAnsiEscapeHighlightEnd);
         pOutput->insert(highlightStart, kAnsiEscapeGroupStartError kAnsiEscapeHighlightStartError);
      }
      else
      {
         pOutput->insert(highlightStart, kAnsiEscapeGroupStartError);
      }

      // If options(warn = 0) is set, it's possible that errors will
      // be printed as part of processing the error.
      // Try to detect this case, and split the outputs.
      auto searchBegin = pOutput->cbegin() + matchEnd;
      auto searchEnd = pOutput->cend();
      if (boost::regex_search(searchBegin, searchEnd, match, s_reInAdditionPrefix))
      {
         auto index = match[0].begin() - pOutput->begin();
         pOutput->insert(index, kAnsiEscapeGroupEnd kAnsiEscapeGroupStartWarning);
      }

      pOutput->append(kAnsiEscapeGroupEnd);

   }
   else if (allowGroupAll)
   {
      pOutput->insert(0, kAnsiEscapeGroupStartError);
      pOutput->append(kAnsiEscapeGroupEnd);
   }
}

} // end anonymous namespace

void ClientEventQueue::annotateOutput(int event,
                                      std::string* pOutput)
{
   if (errorOutputPending_)
   {
      errorOutputPending_ = false;
      annotateError(pOutput, true);
      return;
   }

   if (annotateErrors())
   {
      annotateError(pOutput, false);
   }

   if (annotateWarnings())
   {
      // R will write warning output on stdout in response to warnings(),
      // but on stderr if just emitted on its own. ¯\_(._.)_/¯
      boost::smatch match;
      if (boost::regex_search(*pOutput, match, s_reWarningPrefix))
      {
         pOutput->insert(match[0].first - pOutput->begin(), kAnsiEscapeGroupStartWarning);
         pOutput->append(kAnsiEscapeGroupEnd);

         // Include a code execution hyperlink as well
         boost::algorithm::replace_all(
                  *pOutput,
                  "warnings()",
                  ANSI_HYPERLINK("ide:run", "warnings()", "warnings()"));
      }
   }
}

void ClientEventQueue::setErrorOutputPending()
{
   flushAllBufferedOutput();
   errorOutputPending_ = true;
}

void ClientEventQueue::add(const ClientEvent& event)
{
   if (http_methods::protocolDebugEnabled() && event.type() != client_events::kConsoleWriteError)
   {
      if (event.data().getType() == json::Type::STRING)
      {
         LOG_DEBUG_MESSAGE("Queued event: " + event.typeName() + ": " + event.data().getString());
      }
      else if (event.typeName() == "busy")
      {
         bool val = event.data().getObject()["value"].getBool();
         LOG_DEBUG_MESSAGE("Queued event: " + event.typeName() + ": " + safe_convert::numberToString(val));
      }
      else
      {
         LOG_DEBUG_MESSAGE("Queued event: " + event.typeName());
      }
   }
   
   LOCK_MUTEX(*pMutex_)
   {
      // console output and errors are batched up for compactness / efficiency
      //
      // note that 'errors' are really just anything written to stderr, and this
      // includes things like output from 'message()' and so it's feasible that
      // stderr could become overwhelmed in the same way stdout might.
      if (event.type() == client_events::kConsoleWriteOutput &&
          event.data().getType() == json::Type::STRING)
      {
         flushBufferedOutput(&consoleErrors_);
         consoleOutput_.append(event.data().getString());
      }
      else if (event.type() == client_events::kConsoleWriteError &&
               event.data().getType() == json::Type::STRING)
      {
         flushBufferedOutput(&consoleOutput_);
         consoleErrors_.append(event.data().getString());
      }
      else if (event.type() == client_events::kBuildOutput &&
               event.data().getType() == json::Type::OBJECT)
      {
         // read output -- don't log errors as this routine is called very frequently
         // during build and we don't want to overload the logs
         auto jsonData = event.data().getObject();
         std::string output;
         json::readObject(jsonData, "output", output);
         buildOutput_.append(output);
      }
      else
      {
         // flush existing console output prior to adding an action of another type
         flushAllBufferedOutput();
         
         // add event to queue
         pendingEvents_.push_back(event);
      }
      
      lastEventAddTime_ = boost::posix_time::microsec_clock::universal_time();
   }
   END_LOCK_MUTEX
   
   // notify listeners that an event has been added
   pWaitForEventCondition_->notify_all();
}
   
bool ClientEventQueue::hasEvents() 
{
   LOCK_MUTEX(*pMutex_)
   {
      if (pendingEvents_.size() > 0)
         return true;
      
      for (BufferedOutput* pOutput : bufferedOutputs_)
         if (!pOutput->empty())
            return true;
      
      return false;
   }
   END_LOCK_MUTEX
   
   // keep compiler happy
   return false;
}
  
void ClientEventQueue::remove(std::vector<ClientEvent>* pEvents)
{
   LOCK_MUTEX(*pMutex_)
   {
      // flush any buffered output
      flushAllBufferedOutput();
      
      // copy the events to the caller
      pEvents->insert(pEvents->begin(), 
                      pendingEvents_.begin(), 
                      pendingEvents_.end());
   
      // clear pending events
      pendingEvents_.clear();
   }
   END_LOCK_MUTEX
}

void ClientEventQueue::clear()
{
   LOCK_MUTEX(*pMutex_)
   {
      for (BufferedOutput* pOutput : bufferedOutputs_)
         pOutput->clear();
      
      pendingEvents_.clear();
   }
   END_LOCK_MUTEX
}
  
   
bool ClientEventQueue::waitForEvent(
                        const boost::posix_time::time_duration& waitDuration)
{
   using namespace boost;
   try
   {
      unique_lock<mutex> lock(*pMutex_);
      system_time timeoutTime = get_system_time() + waitDuration;
      return pWaitForEventCondition_->timed_wait(lock, timeoutTime);
   }
   catch(const thread_resource_error& e) 
   { 
      Error waitError(boost::thread_error::ec_from_exception(e), 
                        ERROR_LOCATION);
      LOG_ERROR(waitError);
      return false;
   }
}
   

bool ClientEventQueue::eventAddedSince(const boost::posix_time::ptime& time)
{
   LOCK_MUTEX(*pMutex_)
   {
      if (lastEventAddTime_.is_not_a_date_time())
         return false;
      else
         return lastEventAddTime_ >= time;
   }
   END_LOCK_MUTEX
   
   // keep compiler happy
   return false;
}

void ClientEventQueue::flushAllBufferedOutput()
{
   // NOTE: Private helper so no lock required (mutex is not recursive)
   //
   // NOTE: Order shouldn't matter here as long as we ensure that
   // stdout is flushed whenever stderr is received, and vice versa.
   // This happens as events are received so we should be safe.
   for (BufferedOutput* pOutput : bufferedOutputs_)
      flushBufferedOutput(pOutput);
}

void ClientEventQueue::flushBufferedOutput(BufferedOutput* pBuffer)
{
   // NOTE: Private helper so no lock required (mutex is not recursive)
   if (pBuffer->empty())
      return;
   
   int event = pBuffer->event();
   std::string output = pBuffer->output();
   if (pBuffer->useConsoleActionLimit())
   {
      int limit = r::session::consoleActions().capacity() + 1;
      string_utils::trimLeadingLines(limit, &output);
   }

   annotateOutput(event, &output);
   
   if (event == client_events::kConsoleWriteOutput ||
       event == client_events::kConsoleWriteError)
   {
      // fire events
      auto type = (event == client_events::kConsoleWriteOutput)
            ? module_context::ConsoleOutputNormal
            : module_context::ConsoleOutputError;

      module_context::events().onConsoleOutput(type, output);

      // add to console actions
      if (event == client_events::kConsoleWriteOutput)
      {
         r::session::consoleActions().add(kConsoleActionOutput, output);
      }
      else if (event == client_events::kConsoleWriteError)
      {
         r::session::consoleActions().add(kConsoleActionOutputError, output);
      }

      // send client event
      json::Object payload;
      payload[kConsoleText] = output;
      payload[kConsoleId]   = activeConsole_;
      pendingEvents_.push_back(ClientEvent(event, payload));
   }
   else if (event == client_events::kBuildOutput)
   {
      using namespace module_context;
      CompileOutput compileOutput(kCompileOutputNormal, output);
      json::Object payload = compileOutputAsJson(compileOutput);
      pendingEvents_.push_back(ClientEvent(event, payload));
   }
   else
   {
      ELOGF("internal error: don't know how to flush buffer for event '{}'", event);
   }
   
   pBuffer->clear();
}

} // namespace session
} // namespace rstudio
