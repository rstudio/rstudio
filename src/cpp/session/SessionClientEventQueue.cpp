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

#include "modules/SessionConsole.hpp"

#include <core/BoostThread.hpp>
#include <core/Thread.hpp>
#include <shared_core/json/Json.hpp>
#include <core/StringUtils.hpp>

#include <r/session/RConsoleActions.hpp>

#include "SessionHttpMethods.hpp"

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
   
   if (event == client_events::kConsoleWriteOutput ||
       event == client_events::kConsoleWriteError)
   {
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
