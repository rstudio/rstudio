/*
 * SessionOfflineService.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include <session/SessionOptions.hpp>
#include "SessionConsoleInput.hpp"
#include <session/SessionHttpConnection.hpp>
#include <session/SessionHttpConnectionListener.hpp>
#include "SessionRpc.hpp"
#include "SessionOfflineService.hpp"
#include "SessionAsyncRpcConnection.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {
   static boost::posix_time::time_duration s_handleOfflineDuration;
   static boost::posix_time::time_duration s_asyncRpcDuration;
}

OfflineService& offlineService()
{
   static OfflineService instance;
   return instance;
}

Error OfflineService::start()
{
   if (!options().asyncRpcEnabled() && !options().handleOfflineEnabled())
   {
      LOG_DEBUG_MESSAGE("No async request processing - session-async-rpc-enabled=0 and handle-offline-enabled=0");
      return Success();
   }

   if ((!options().asyncRpcEnabled() || options().asyncRpcTimeoutMs() == 0) &&
       (!options().handleOfflineEnabled() || options().handleOfflineTimeoutMs() == 0))
   {
      if (options().asyncRpcEnabled())
         LOG_DEBUG_MESSAGE("Immediate asyncRpc enabled");
      if (options().handleOfflineEnabled())
         LOG_DEBUG_MESSAGE("Immediate offline request handling enabled");
      return Success();
   }


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
      boost::thread offlineThread(bind(&OfflineService::run, this));
      offlineThread_ = MOVE_THREAD(offlineThread);
      
      return Success();
   }
   catch(const boost::thread_resource_error& e)
   {
      return Error(boost::thread_error::ec_from_exception(e), ERROR_LOCATION);
   }
}
   
void OfflineService::stop()
{
   try
   {
      if (offlineThread_.joinable())
      {
         offlineThread_.interrupt();

         // wait for for the service thread to stop
         if (!offlineThread_.timed_join(
               boost::posix_time::seconds(2)))
         {
            LOG_WARNING_MESSAGE("OfflineService didn't stop on its own");
         }

         offlineThread_.detach();
      }
   }
   catch(const boost::thread_interrupted&)
   {
      // the main thread is the one who calls stop() and it should 
      // NEVER be interrupted for any reason
      LOG_WARNING_MESSAGE("thread interrupted during stop");
   }
}

// Called for each connection in mainConnectionQueue until an offlineable request is found.
bool offlineConnectionMatcher(boost::shared_ptr<HttpConnection> ptrHttpConn,
                              boost::posix_time::ptime now)
{
   if (!rpc::isOfflineableRequest(ptrHttpConn))
      return false;

   // Weird clock problem!
   if (now < ptrHttpConn->receivedTime())
      return false;

   // The R runtime is busy and we received this long enough ago
   if (now - ptrHttpConn->receivedTime() > s_handleOfflineDuration)
      return true;
   return false;
}

// Called for each connection in mainConnectionQueue - for any stale connections, convert to async
// and return the new AsyncRpcConnection
boost::shared_ptr<HttpConnection> asyncConnectionConverter(boost::shared_ptr<HttpConnection> ptrHttpConn,
                                                           boost::posix_time::ptime now)
{
   if (!ptrHttpConn->isAsyncRpc() &&
       http_methods::isJsonRpcRequest(ptrHttpConn) &&
       ptrHttpConn->receivedTime() + s_asyncRpcDuration < now)
   {
      boost::shared_ptr<HttpConnection> asyncConnection = http_methods::handleAsyncRpc(ptrHttpConn);

      if (http_methods::connectionDebugEnabled())
      {
         boost::posix_time::time_duration duration(now - ptrHttpConn->receivedTime());
         LOG_DEBUG_MESSAGE("Async reply:         " + ptrHttpConn->request().uri() +
                           " after: " + std::to_string(duration.seconds()) +
                           "." + std::to_string(duration.fractional_seconds()).substr(0,2));
      }

      return asyncConnection;
   }
   return boost::shared_ptr<HttpConnection>();
}

void OfflineService::run()
{
   try
   {    
      // default time durations
      int asyncRpcMillis = options().asyncRpcTimeoutMs();
      int handleOfflineMillis = options().handleOfflineTimeoutMs();
      if (!options().asyncRpcEnabled())
         asyncRpcMillis = 0;
      if (!options().handleOfflineEnabled())
         handleOfflineMillis = 0;

      if (asyncRpcMillis != 0 && asyncRpcMillis < 100)
      {
         asyncRpcMillis = 100;
      }
      if (handleOfflineMillis != 0 && handleOfflineMillis < 200)
      {
         handleOfflineMillis = 200;
      }

      int sleepMillis = asyncRpcMillis;

      //std::cout << "Running offlineService: " << asyncRpcMillis << " millis - handleOffline " << handleOfflineMillis << std::endl;

      s_asyncRpcDuration = boost::posix_time::milliseconds(asyncRpcMillis);
      s_handleOfflineDuration = boost::posix_time::milliseconds(handleOfflineMillis);

      LOG_DEBUG_MESSAGE("OfflineService started with session-async-rpc-timeout-ms=" + std::to_string(asyncRpcMillis) +
                        " handle-offline-timeout-ms=" + std::to_string(handleOfflineMillis));

      // Pick the smallest non-zero value to use in the sleep call
      // TODO: asyncRpc will run jobs later in some cases e.g. asyncRpcMillis=500 and handleOffline=750
      // runs offline requests every second, not 750. Keeping it simple for now
      if (sleepMillis == 0 || (handleOfflineMillis != 0 && handleOfflineMillis < sleepMillis))
         sleepMillis = handleOfflineMillis;

      BOOST_ASSERT(sleepMillis > 0); // If both are 0 this thread should not have been started

      boost::chrono::milliseconds sleepTime = boost::chrono::milliseconds(sleepMillis);

      // get alias to mainConnectionQueue
      HttpConnectionQueue& mainConnectionQueue = httpConnectionListener().mainConnectionQueue();
      bool serverStopped = false;
      while (!serverStopped)
      {
         if (boost::this_thread::interruption_requested())
         {
            serverStopped = true;
            break;
         }
         try
         {
            // Wake up using the minimum interval
            boost::this_thread::sleep_for(sleepTime);

            // Wait for the client to be initialized before any offline handling
            if (!httpConnectionListener().eventsActive())
               continue;

            // If R is not occupying the main thread, we'll continue to wait.
            // TODO: asyncRpc - other skippable operations, not from R - if so, we could remove this test
            if (!console_input::executing())
            {
               // TODO: asyncRpc log a warning here if mainConnectionQueue has more than 5 entries
               continue;
            }

            boost::posix_time::ptime now = boost::posix_time::microsec_clock::universal_time();

            if (handleOfflineMillis > 0)
            {
               boost::shared_ptr<HttpConnection> ptrConnection;
               do
               {
                  // Look for and handle any requests that can run offline that have been waiting too long
                  ptrConnection = mainConnectionQueue.dequeMatchingConnection(offlineConnectionMatcher, now);
                  if (ptrConnection)
                  {
                     if (http_methods::connectionDebugEnabled())
                     {
                        boost::posix_time::time_duration beforeTime(now - ptrConnection->receivedTime());
                        LOG_DEBUG_MESSAGE("- Handle offline:    " + ptrConnection->request().uri() +
                                          " after: " + std::to_string(beforeTime.seconds()) +
                                          "." + std::to_string(beforeTime.fractional_seconds()).substr(0,2));

                        http_methods::handleConnection(ptrConnection, http_methods::BackgroundConnection);

                        boost::posix_time::ptime endReq = boost::posix_time::microsec_clock::universal_time();
                        boost::posix_time::time_duration afterTime(endReq - now);
                        LOG_DEBUG_MESSAGE("--- complete:        " + ptrConnection->request().uri() +
                                          " in: " + std::to_string(afterTime.seconds()) +
                                          "." + std::to_string(afterTime.fractional_seconds()).substr(0,2));
                        now = endReq;
                     }
                     else
                     {
                        // TODO: asyncRpc - should we add a new mutex here so that only one handleConnection runs
                        // Offlineable Uris must be threadsafe to be runnable at the same time here and
                        // on the main thread
                        http_methods::handleConnection(ptrConnection, http_methods::BackgroundConnection);
                     }
                  }
               }
               while (ptrConnection); // Possible a queue has built up so flush them all out
            }

            if (asyncRpcMillis > 0)
            {
               // convert stale connections to async
               mainConnectionQueue.convertConnections(asyncConnectionConverter, now);
            }

            // TODO: asyncRpc - do we need to do anything to ensure session status events like
            // MemoryChangedEvent continue to be sent here? Looks like those are emitted on a separate
         }
         catch(const boost::thread_interrupted&)
         {
            serverStopped = true;
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION
}
      
} // namespace session
} // namespace rstudio
