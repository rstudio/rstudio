/*
 * Connection.cpp
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

#include "Connection.hpp"


#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {

namespace {




} // anonymous namespace


json::Object connectionIdJson(const ConnectionId& id)
{
   json::Object idJson;
   idJson["type"] = id.type;
   idJson["host"] = id.host;
   return idJson;
}

json::Object connectionJson(const Connection& connection)
{
   json::Object connectionJson;
   connectionJson["id"] = connectionIdJson(connection.id);
   connectionJson["finder"] = connection.finder;
   connectionJson["connect_code"] = connection.connectCode;
   connectionJson["disconnect_code"] = connection.disconnectCode;
   connectionJson["lastUsed"] = connection.lastUsed;
   return connectionJson;
}

bool hasConnectionId(const ConnectionId& id,
                     const core::json::Object& connectionJson)
{
   json::Object idJson;
   Error error = json::readObject(connectionJson, "id", &idJson);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   std::string type, host;
   error = json::readObject(idJson, "type", &type, "host", &host);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return id == ConnectionId(type, host);
}

} // namespace connections
} // namespace modules
} // namesapce session
} // namespace rstudio

