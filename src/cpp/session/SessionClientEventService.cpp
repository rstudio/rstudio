/*
 * SessionClientEventService.cpp
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

#include <algorithm>

#include <boost/function.hpp>

#include <core/BoostThread.hpp>
#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/BoostErrors.hpp>
#include <core/Thread.hpp>
#include <core/system/System.hpp>
#include <core/Macros.hpp>


#include <core/http/Request.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionHttpConnectionListener.hpp>
#include <session/SessionClientEventService.hpp>

#include "SessionClientEventQueue.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
   
namespace {

const int kLastChanceWaitSeconds = 4;

bool hasEventIdLessThanOrEqualTo(const json::Value& event, int targetId)
{
   const json::Object& eventJSON = event.getObject();
   int eventId = (*eventJSON.find("id")).getValue().getInt();
   return eventId <= targetId;
}
         
} // anonymous namespace

ClientEventService& clientEventService()
{
   static ClientEventService instance;
   return instance;
}

Error ClientEventService::start(const std::string& clientId)
{
   // set our clientid
   setClientId(clientId, false);
   
   // block all signals for launch of background thread (will cause it
   // to never receive signals)
   core::system::SignalBlocker signalBlocker;
   Error error = signalBlocker.blockAll();
   if (error)
      return error;
   
   // launch the service thread
   try
   {
      using boost::bind;
      boost::thread serviceThread(bind(&ClientEventService::run, this));
      serviceThread_ = MOVE_THREAD(serviceThread);
      
      return Success();
   }
   catch(const boost::thread_resource_error& e)
   {
      return Error(boost::thread_error::ec_from_exception(e), ERROR_LOCATION);
   }
}
   
void ClientEventService::stop()
{
   try
   {
      if (serviceThread_.joinable())
      {
         serviceThread_.interrupt();

         // wait for for the service thread to stop
         if (!serviceThread_.timed_join(
               boost::posix_time::seconds(kLastChanceWaitSeconds + 1)))
         {
            LOG_WARNING_MESSAGE("ClientEventService didn't stop on its own");
         }

         serviceThread_.detach();
      }
   }
   catch(const boost::thread_interrupted&)
   {
      // the main thread is the one who calls stop() and it should 
      // NEVER be interrupted for any reason
      LOG_WARNING_MESSAGE("thread interrupted during stop");
   }
}
   
void ClientEventService::setClientId(const std::string& clientId, bool clearEvents)
{
   LOCK_MUTEX(mutex_)
   {
      clientId_ = clientId.c_str(); // avoid ref count
      if (clearEvents)
         clientEvents_.clear();
   }
   END_LOCK_MUTEX

   if (clearEvents)
      clientEventQueue().clear();
}
   
std::string ClientEventService::clientId()
{
   LOCK_MUTEX(mutex_)
   {
      return std::string(clientId_.c_str()) ; // avoid ref-count
   }
   END_LOCK_MUTEX
   
   // keep compiler happy
   return std::string();
}

void ClientEventService::erasePreviouslyDeliveredEvents(int lastClientEventIdSeen)
{
   LOCK_MUTEX(mutex_)
   {
      clientEvents_.erase(
               std::remove_if(clientEvents_.begin(),
                              clientEvents_.end(),
                              boost::bind(hasEventIdLessThanOrEqualTo,
                                          _1,
                                          lastClientEventIdSeen)),
               clientEvents_.end());
   }
   END_LOCK_MUTEX
}

bool ClientEventService::havePendingClientEvents()
{
   LOCK_MUTEX(mutex_)
   {
      return !clientEvents_.isEmpty();
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return false;
}

void ClientEventService::addClientEvent(const json::Object& eventObject)
{
   LOCK_MUTEX(mutex_)
   {
      clientEvents_.push_back(eventObject);
   }
   END_LOCK_MUTEX
}

void ClientEventService::setClientEventResult(
                                       core::json::JsonRpcResponse* pResponse)
{
   LOCK_MUTEX(mutex_)
   {
      pResponse->setResult(clientEvents_);
   }
   END_LOCK_MUTEX
}


void ClientEventService::run()
{
   try
   {    
      // default time durations
      using namespace boost::posix_time;
      time_duration maxRequestSec = seconds(50);
      time_duration batchDelay = milliseconds(20);
      time_duration maxTotalBatchDelay = seconds(2);

      // make much shorter for desktop mode
      if (options().programMode() == kSessionProgramModeDesktop)
      {
         batchDelay = milliseconds(2);
         maxTotalBatchDelay = milliseconds(10);
      }
      
      // get alias to client event queue
      ClientEventQueue& clientEventQueue = session::clientEventQueue();
      
      // initialize state
      int nextEventId = 0;
      
      // accept loop
      bool stopServer = false;
      while (!stopServer || clientEventQueue.hasEvents())
      {
         boost::shared_ptr<HttpConnection> ptrConnection;
         try
         {
            // wait for up to 1 second for a connection
            long secondsToWait = stopServer ? kLastChanceWaitSeconds : 1;
            ptrConnection =
             httpConnectionListener().eventsConnectionQueue().dequeConnection(
                                             boost::posix_time::seconds(secondsToWait));

            // if we didn't get one then check for interruption requested
            // and then continue waiting
            if (!ptrConnection)
            {
               if (stopServer)
               {
                  // This was our last chance. There are still some events
                  // left in the queue, but we waited and nobody came.
                  break;
               }

               // check for interruption and set stopServer flag if we were
               if (boost::this_thread::interruption_requested())
                  throw boost::thread_interrupted();

               // accept next request (assuming we weren't interrupted)
               continue;
            }
         }
         catch(const boost::thread_interrupted&)
         {
            stopServer = true;
            continue;
         }

         // parse the json rpc request
         json::JsonRpcRequest request;
         Error error = json::parseJsonRpcRequest(ptrConnection->request().body(),
                                                 &request);
         if (error)
         {
            ptrConnection->sendJsonRpcError(error);
            continue;
         }

         // send an error back if this request came from the wrong client
         if (request.clientId != clientId())
         {
            Error error = Error(json::errc::InvalidClientId,
                                             ERROR_LOCATION);
            ptrConnection->sendJsonRpcError(error);
            continue;
         }

         // get the last event id seen by the client
         int lastClientEventIdSeen = -1;
         Error paramError = json::readParam(request.params, 
                                            0, 
                                            &lastClientEventIdSeen);
         if (paramError)
         {
            ptrConnection->sendJsonRpcError(paramError);
            continue;
         }
           
         // remove all events already seen by the client from our internal list
         erasePreviouslyDeliveredEvents(lastClientEventIdSeen);

         // sync next event id to client (required so that when we resume
         // from a suspend we provide client event ids in line with the 
         // client's expectations -- if we started with zero then the client
         // would never see any events!)
         nextEventId = std::max(nextEventId, lastClientEventIdSeen + 1);

         // check for events (and wait a specified internal if there are none)
         try
         {
            // wait for the specified maximum time
            if (havePendingClientEvents() || clientEventQueue.hasEvents() ||
                clientEventQueue.waitForEvent(maxRequestSec))
            {
               // ...got at least one event
               
               // wait for additional events that occur in rapid succession 
               // but don't wait for more than the specified maximum seconds
               boost::system_time maxBatchDelayTime = 
                              boost::get_system_time() + maxTotalBatchDelay;
               
               while ( clientEventQueue.waitForEvent(batchDelay) &&
                       (boost::get_system_time() < maxBatchDelayTime) )
               {
               }
           }
         }
         catch(const boost::thread_interrupted&)
         {
            // set flag so we terminate on the next accept loop iteration
            stopServer = true;
            
            // NOTE: even if we are interrupted we still want to allow the
            // response to be sent (e.g. need to send the client either
            // an empty list of events back or perhaps even the quit event!)
         }
         
         // if this is the correct client then remove events from the 
         // queue and send them. otherwise, send an InvalidClientId error
         // to this client. the currently active client will then pickup the
         // events on the next iteration of the accept loop
         if (request.clientId == clientId())
         {
            // deque the events
            std::vector<ClientEvent> events;
            clientEventQueue.remove(&events);
            
            // convert to json and add event id
            for (std::vector<ClientEvent>::const_iterator 
                 it = events.begin(); it != events.end(); ++it)
            {
               json::Object event;
               it->asJsonObject(nextEventId++, &event);
               addClientEvent(event);
            }

            // send them (pass false for kEventsPending b/c responses from the
            // event service shouldn't interact with automatic event service
            // starting/re-starting)
            json::JsonRpcResponse response;
            setClientEventResult(&response);
            response.setField(kEventsPending, "false");
            ptrConnection->sendJsonRpcResponse(response);
         }
         else
         {
            Error error(json::errc::InvalidClientId, ERROR_LOCATION);
            ptrConnection->sendJsonRpcError(error);
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}
      
} // namespace session
} // namespace rstudio
