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

#include <string>
#include <utility>

#include <boost/regex/v5/regex.hpp>

#include <shared_core/json/Json.hpp>

#include <core/AnsiEscapes.hpp>
#include <core/BoostThread.hpp>
#include <core/Thread.hpp>
#include <core/StringUtils.hpp>
#include <core/regex/RegexDebug.hpp>
#include <core/regex/RegexSearch.hpp>

#include <r/RExec.hpp>
#include <r/session/RConsoleActions.hpp>

#include <session/SessionConsoleOutput.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "SessionHttpMethods.hpp"
#include "modules/SessionConsole.hpp"


using namespace rstudio::core;

namespace rstudio {
namespace session {
 
namespace {

ClientEventQueue* s_pClientEventQueue = nullptr;

} // end anonymous namespace

void initializeClientEventQueue()
{
   BOOST_ASSERT(s_pClientEventQueue == nullptr);
   s_pClientEventQueue = new ClientEventQueue();
}

ClientEventQueue& clientEventQueue()
{
   return *s_pClientEventQueue;
}
   
ClientEventQueue::ClientEventQueue()
   : pMutex_(new boost::mutex()),
     pWaitForEventCondition_(new boost::condition()),
     lastEventAddTime_(boost::posix_time::not_a_date_time)
{
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
   END_LOCK_MUTEX;

   return changed;
}

namespace {

bool isConsoleOutputEvent(int event)
{
   return
         event == client_events::kConsoleWriteOutput ||
         event == client_events::kConsoleWriteError ||
         event == client_events::kConsoleWritePendingError ||
         event == client_events::kConsoleWritePendingWarning;
}

void annotateError(std::string* pOutput, bool allowGroupAll)
{
   using namespace console_output;

   boost::smatch match;
   if (regex_utils::search(*pOutput, match, reErrorPrefix()))
   {
      // we want to mutate the input string, but the match object uses
      // iterators into the original string, so we need to instead
      // compute offsets from the match

      // Check for a match in one of our capturing groups.
      for (std::size_t i = 1; i < match.size(); i++)
      {
         if (match[i].matched)
         {
            // If we found a match, we'll insert highlight markers around the match.
            auto lhs = match.position(i);
            auto rhs = lhs + match.length(i);

            pOutput->insert(rhs, kAnsiEscapeHighlightEnd);
            pOutput->insert(lhs, kAnsiEscapeHighlightStartError);
            break;
         }
      }

      // Insert our group markers.
      pOutput->insert(0, kAnsiEscapeGroupStartError);
      pOutput->append(kAnsiEscapeGroupEnd);
   }
   else if (allowGroupAll)
   {
      pOutput->insert(0, kAnsiEscapeGroupStartError);
      pOutput->append(kAnsiEscapeGroupEnd);
   }
}

void annotateWarning(std::string* pOutput, bool allowGroupAll)
{
   using namespace console_output;

   boost::smatch match;
   
   auto offset = 0;
   auto lhs = pOutput->cbegin();
   auto rhs = pOutput->cend();

   // Skip over an 'In addition: ' prefix, if there is one.
   if (regex_utils::search(lhs, rhs, match, reInAdditionPrefix()))
   {
      offset = match.position() + match.length();
   }

   if (regex_utils::search(lhs + offset, rhs, match, reWarningPrefix()))
   {
      // Check for a match in one of our capturing groups.
      for (std::size_t i = 1; i < match.size(); i++)
      {
         if (match[i].matched)
         {
            // If we found a match, we'll insert highlight markers around the match.
            auto lhs = match.position(i) + offset;
            auto rhs = lhs + match.length(i);
            pOutput->insert(rhs, kAnsiEscapeHighlightEnd);
            pOutput->insert(lhs, kAnsiEscapeHighlightStartWarning);
            break;
         }
      }

      // Insert our group markers.
      pOutput->insert(0, kAnsiEscapeGroupStartWarning);
      pOutput->append(kAnsiEscapeGroupEnd);
   }
   else if (allowGroupAll)
   {
      pOutput->insert(0, kAnsiEscapeGroupStartWarning);
      pOutput->append(kAnsiEscapeGroupEnd);
   }

   // Include a code execution hyperlink as well
   boost::algorithm::replace_all(
            *pOutput,
            "warnings()",
            ANSI_HYPERLINK("ide:run", "warnings()", "warnings()"));
}

} // end anonymous namespace

void ClientEventQueue::annotateOutput(int event,
                                      std::string* pOutput)
{
   using namespace console_output;

   if (isErrorAnnotationEnabled())
   {
      if (event == client_events::kConsoleWritePendingError)
      {
         annotateError(pOutput, true);
      }
      else
      {
         annotateError(pOutput, false);
      }
   }

   if (isWarningAnnotationEnabled())
   {
      if (event == client_events::kConsoleWritePendingWarning)
      {
         annotateWarning(pOutput, true);
      }
      else
      {
         annotateWarning(pOutput, false);
      }
   }
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
      if (isConsoleOutputEvent(event.type()))
      {
         if (event.data().getType() == json::Type::STRING)
         {
            flushAllBufferedOutputExcept(event.type());
            bufferedOutputs_[event.type()].append(event.data().getString());
         }
      }
      else if (event.type() == client_events::kBuildOutput &&
               event.data().getType() == json::Type::OBJECT)
      {
         // read output -- don't log errors as this routine is called very frequently
         // during build and we don't want to overload the logs
         auto jsonData = event.data().getObject();
         std::string output;
         json::readObject(jsonData, "output", output);

         flushAllBufferedOutputExcept(event.type());
         bufferedOutputs_[event.type()].append(output);
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
   END_LOCK_MUTEX;
   
   // notify listeners that an event has been added
   pWaitForEventCondition_->notify_all();
}
   
bool ClientEventQueue::hasEvents() 
{
   LOCK_MUTEX(*pMutex_)
   {
      if (pendingEvents_.size() > 0)
         return true;
      
      for (auto&& entry : bufferedOutputs_)
      {
         if (!entry.second.empty())
         {
            return true;
         }
      }
      
      return false;
   }
   END_LOCK_MUTEX;
   
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
   END_LOCK_MUTEX;
}

void ClientEventQueue::clear()
{
   LOCK_MUTEX(*pMutex_)
   {
      for (auto&& entry : bufferedOutputs_)
      {
         entry.second.clear();
      }
      
      pendingEvents_.clear();
   }
   END_LOCK_MUTEX;
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
   END_LOCK_MUTEX;
   
   // keep compiler happy
   return false;
}

void ClientEventQueue::flush()
{
   LOCK_MUTEX(*pMutex_)
   {
      flushAllBufferedOutput();
   }
   END_LOCK_MUTEX;
}

void ClientEventQueue::flushAllBufferedOutput()
{
   // NOTE: Private helper so no lock required (mutex is not recursive)
   //
   // NOTE: Order shouldn't matter here as long as we ensure that
   // stdout is flushed whenever stderr is received, and vice versa.
   // This happens as events are received so we should be safe.
   for (auto&& entry : bufferedOutputs_)
   {
      flushBufferedOutput(entry.first, &entry.second);
   }
}

void ClientEventQueue::flushAllBufferedOutputExcept(int event)
{
   // NOTE: Private helper so no lock required (mutex is not recursive)
   //
   // NOTE: Order shouldn't matter here as long as we ensure that
   // stdout is flushed whenever stderr is received, and vice versa.
   // This happens as events are received so we should be safe.
   for (auto&& entry : bufferedOutputs_)
   {
      if (entry.first != event)
      {
         flushBufferedOutput(entry.first, &entry.second);
      }
   }
}

void ClientEventQueue::flushBufferedOutput(int event, std::string* pOutput)
{
   // NOTE: Private helper so no lock required (mutex is not recursive)
   if (pOutput->empty())
      return;
   
   if (isConsoleOutputEvent(event))
   {
      int limit = r::session::consoleActions().capacity() + 1;
      string_utils::trimLeadingLines(limit, pOutput);
   }

   annotateOutput(event, pOutput);

   if (isConsoleOutputEvent(event))
   {
      if (event == client_events::kConsoleWriteOutput)
      {
         auto type = module_context::ConsoleOutputNormal;
         module_context::events().onConsoleOutput(type, *pOutput);
         r::session::consoleActions().add(kConsoleActionOutput, *pOutput);
      }
      else
      {
         auto type = module_context::ConsoleOutputError;
         module_context::events().onConsoleOutput(type, *pOutput);
         r::session::consoleActions().add(kConsoleActionOutputError, *pOutput);
      }

      // send client event
      json::Object payload;
      payload[kConsoleText] = *pOutput;
      payload[kConsoleId]   = activeConsole_;
      int normalizedEvent = (event == client_events::kConsoleWriteOutput)
            ? client_events::kConsoleWriteOutput
            : client_events::kConsoleWriteError;
      pendingEvents_.push_back(ClientEvent(normalizedEvent, payload));
   }
   else if (event == client_events::kBuildOutput)
   {
      using namespace module_context;
      CompileOutput compileOutput(kCompileOutputNormal, *pOutput);
      json::Object payload = compileOutputAsJson(compileOutput);
      pendingEvents_.push_back(ClientEvent(event, payload));
   }
   else
   {
      ELOGF("internal error: don't know how to flush buffer for event '{}'", event);
   }
   
   pOutput->clear();
}

} // namespace session
} // namespace rstudio
