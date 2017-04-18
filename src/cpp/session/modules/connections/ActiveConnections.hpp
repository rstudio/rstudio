/*
 * ActiveConnections.hpp
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

#ifndef SESSION_CONNECTIONS_ACTIVE_CONNECTIONS_HPP
#define SESSION_CONNECTIONS_ACTIVE_CONNECTIONS_HPP

#include <set>

#include <boost/noncopyable.hpp>

#include "Connection.hpp"

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {
   
class ActiveConnections;
ActiveConnections& activeConnections();

class ActiveConnections : boost::noncopyable
{
private:
   ActiveConnections();
   friend ActiveConnections& activeConnections();

public:

   bool empty() const;

   void add(const ConnectionId& id);
   void remove(const ConnectionId& id);

   core::json::Array activeConnectionsAsJson();

   void broadcastToClient();

private:
   std::set<ConnectionId> activeConnections_;
};


                       
} // namespace connections
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CONNECTIONS_ACTIVE_CONNECTIONS_HPP
