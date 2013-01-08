/*
 * SessionClientEventQueue.cpp
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

#include "SessionClientEventQueue.hpp"

#include <boost/foreach.hpp>


#include <core/BoostThread.hpp>
#include <core/Thread.hpp>
#include <core/json/Json.hpp>
#include <core/StringUtils.hpp>

#include <r/session/RConsoleActions.hpp>

using namespace core ;

namespace session {
 
namespace {
ClientEventQueue* s_pClientEventQueue = NULL;
}

void initializeClientEventQueue()
{
   BOOST_ASSERT(s_pClientEventQueue == NULL);
   s_pClientEventQueue = new ClientEventQueue();
}

ClientEventQueue& clientEventQueue()
{
   return *s_pClientEventQueue;
}
   
ClientEventQueue::ClientEventQueue()
   :  pMutex_(new boost::mutex()),
      pWaitForEventCondition_(new boost::condition()),
      lastEventAddTime_(boost::posix_time::not_a_date_time)
{
}

void ClientEventQueue::add(const ClientEvent& event)
{ 
   LOCK_MUTEX(*pMutex_)
   {
      // console output is batched up for compactness/efficiency.
      if (event.type() == client_events::kConsoleWriteOutput)
      {
         if (event.data().type() == json::StringType)
            pendingConsoleOutput_ += event.data().get_str();
      }
      else
      {
         // flush existing console output prior to adding an 
         // action of another type
         flushPendingConsoleOutput() ;
         
         // add event to queue
         pendingEvents_.push_back(event) ;
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
      return pendingEvents_.size() > 0 || pendingConsoleOutput_.length() > 0;
   }
   END_LOCK_MUTEX
   
   // keep compiler happy
   return false ;
}
  
void ClientEventQueue::remove(std::vector<ClientEvent>* pEvents)
{
   LOCK_MUTEX(*pMutex_)
   {
      // flush any pending output
      flushPendingConsoleOutput();
      
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
      pendingConsoleOutput_.clear();
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
                        ERROR_LOCATION) ; 
      LOG_ERROR(waitError);
      return false ;
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
   

void ClientEventQueue::flushPendingConsoleOutput()
{
   // NOTE: private helper so no lock required (mutex is not recursive) 
   
   if ( !pendingConsoleOutput_.empty() )
   {
      // If there's more console output than the client can even show, then
      // truncate it to the amount that the client can show. Too much output
      // can overwhelm the client, causing it to become unresponsive.
      int limit = r::session::consoleActions().capacity() + 1;
      string_utils::trimLeadingLines(limit, &pendingConsoleOutput_);

      pendingEvents_.push_back(ClientEvent(client_events::kConsoleWriteOutput, 
                                           pendingConsoleOutput_)); 
      pendingConsoleOutput_.clear() ;
   }
}

} // namespace session
