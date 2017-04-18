/*
 * ActiveConnections.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "ActiveConnections.hpp"


#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {

namespace {


void enqueActiveConnectionsChanged()
{
   ClientEvent event(client_events::kActiveConnectionsChanged,
                     activeConnections().activeConnectionsAsJson());
   module_context::enqueClientEvent(event);
}


} // anonymous namespace


ActiveConnections& activeConnections()
{
   static ActiveConnections instance;
   return instance;
}

ActiveConnections::ActiveConnections()
{
}

bool ActiveConnections::empty() const
{
   return activeConnections_.empty();
}

void ActiveConnections::add(const ConnectionId& id)
{
   activeConnections_.insert(id);
   enqueActiveConnectionsChanged();
}

void ActiveConnections::remove(const ConnectionId& id)
{
   activeConnections_.erase(id);
   enqueActiveConnectionsChanged();
}

json::Array ActiveConnections::activeConnectionsAsJson()
{
   json::Array connectionsJson;
   for (std::set<ConnectionId>::const_iterator it = activeConnections_.begin();
        it != activeConnections_.end(); ++it)
   {
      connectionsJson.push_back(connectionIdJson(*it));
   }
   return connectionsJson;
}


void ActiveConnections::broadcastToClient()
{
   enqueActiveConnectionsChanged();
}





} // namespace connections
} // namespace modules
} // namespace session
} // namespace rstudio

