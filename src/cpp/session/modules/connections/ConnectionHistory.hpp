/*
 * ConnectionHistory.hpp
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

#ifndef SESSION_CONNECTIONS_CONNECTION_HISTORY_HPP
#define SESSION_CONNECTIONS_CONNECTION_HISTORY_HPP

#include <boost/noncopyable.hpp>

#include <core/FilePath.hpp>
#include <core/json/Json.hpp>

#include "Connection.hpp"

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace connections {
   
class ConnectionHistory;
ConnectionHistory& connectionHistory();

class ConnectionHistory : boost::noncopyable
{
private:
   ConnectionHistory();
   friend ConnectionHistory& connectionHistory();

public:
   core::Error initialize();

   void update(const Connection& connection);
   void remove(const ConnectionId& id);

   core::json::Array connectionsAsJson();

private:
   void onConnectionsChanged();
   core::Error readConnections(core::json::Array* pConnections);
   core::Error writeConnections(const core::json::Array& connectionsJson);

private:
   core::FilePath connectionsDir_;
};
                       
} // namespace connections
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CONNECTIONS_CONNECTION_HISTORY_HPP
