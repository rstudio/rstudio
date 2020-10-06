/*
 * SessionClientEventService.hpp
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

#ifndef SESSION_CLIENT_EVENT_SERVICE_HPP
#define SESSION_CLIENT_EVENT_SERVICE_HPP

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
   void stop();
   
   void setClientId(const std::string& clientId, bool clearEvents);

   std::string clientId();

private:
   void run();

   void erasePreviouslyDeliveredEvents(int lastClientEventIdSeen);
   bool havePendingClientEvents();
   void addClientEvent(const core::json::Object& eventObject);
   void setClientEventResult(core::json::JsonRpcResponse* pResponse);

  
private:
   boost::mutex mutex_;
   boost::thread serviceThread_;

   std::string clientId_;
   core::json::Array clientEvents_;
};
   
  
} // namespace session
} // namespace rstudio

#endif // SESSION_CLIENT_EVENT_SERVICE_HPP
