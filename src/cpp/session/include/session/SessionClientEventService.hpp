/*
 * SessionClientEventService.hpp
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

#ifndef SESSION_CLIENT_EVENT_SERVICE_HPP
#define SESSION_CLIENT_EVENT_SERVICE_HPP

#include <atomic>
#include <string>

#include <boost/utility.hpp>

#include <core/BoostThread.hpp>

#include <core/json/JsonRpc.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace session {

// singleton
class ClientEventService;
ClientEventService& clientEventService();

class ClientEventService : boost::noncopyable
{
private:
   ClientEventService() {}
   friend ClientEventService& clientEventService();

public:
   // COPYING: boost::noncopyable

   core::Error start(const std::string& clientId);

   // stop the service. when flushPendingEvents is true (the default, used for
   // user-initiated quits) the service thread is given a brief last-chance
   // window to deliver any remaining events to a connected client. when false
   // (e.g. a forced suspend from a server shutdown, where the client is no
   // longer reachable) that window is skipped so shutdown is not delayed.
   void stop(bool flushPendingEvents = true);

   void setClientId(const std::string& clientId, bool clearEvents);

   std::string clientId();

private:
   void run();

   // how long the run loop should wait for a client connection to retrieve
   // final events while stopping. zero when there is no point waiting (a
   // forced suspend, or no client has been active recently).
   long lastChanceWaitSeconds() const;

   void erasePreviouslyDeliveredEvents(int lastClientEventIdSeen);
   bool havePendingClientEvents();
   void addClientEvent(const core::json::Object& eventObject);
   void setClientEventResult(core::json::JsonRpcResponse* pResponse);

  
private:
   boost::mutex mutex_;
   boost::thread serviceThread_;

   // when false, the service thread skips its last-chance event-delivery wait
   // on stop (set by stop() before the thread is interrupted)
   std::atomic<bool> flushPendingEventsOnStop_{true};

   std::string clientId_;
   core::json::Array clientEvents_;
};
   
  
} // namespace session
} // namespace rstudio

#endif // SESSION_CLIENT_EVENT_SERVICE_HPP
