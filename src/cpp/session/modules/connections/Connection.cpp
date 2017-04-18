/*
 * Connection.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#include <boost/foreach.hpp>

#include <core/Base64.hpp>
#include <core/StringUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>

// max icon size is 20k; this prevents packages that haven't saved/scaled
// their icons properly from causing performance trouble downstream
#define kMaxIconSize 20 * 1048

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace connections {

// determines the icon data; this could be either a path on disk (if we have
// a suitable icon locally), base64-encoded icon data (if the icon is embedded
// in an R package), or nothing (if we cannot determine an icon at all)
std::string iconData(const std::string& iconGroup, 
      const std::string& iconName,
      const std::string& iconPath)
{
   if (iconPath.empty())
   {
      // the package did not supply an icon; see if there's one baked in
      FilePath path = options().rResourcesPath().childPath("connections")
         .childPath(iconGroup)
         .childPath(string_utils::toLower(iconName) + ".png");
      if (path.exists())
         return std::string("connections/") + iconGroup + "/" + 
            string_utils::toLower(iconName) +
            ".png";

      // didn't find anything
      return std::string();
   }

   // expand the path 
   FilePath icon = module_context::resolveAliasedPath(iconPath);
   std::string iconData;

   // ensure that the icon file exists and is a small GIF, JPG, or PNG image
   if (icon.exists() && icon.size() < kMaxIconSize &&
       (icon.hasExtensionLowerCase(".gif") ||
        icon.hasExtensionLowerCase(".png") ||
        icon.hasExtensionLowerCase(".jpg") ||
        icon.hasExtensionLowerCase(".jpeg")))
   {
      Error error = base64::encode(icon, &iconData);
      if (error)
         LOG_ERROR(error);
      else
      {
         iconData = "data:" + icon.mimeContentType("image/png") + 
                    ";base64," + iconData;
      }
   }
   return iconData;
}

json::Object connectionIdJson(const ConnectionId& id)
{
   json::Object idJson;
   idJson["type"] = id.type;
   idJson["host"] = id.host;
   return idJson;
}

json::Object connectionActionJson(const ConnectionAction& action) 
{
   json::Object actionJson;
   actionJson["name"]      = action.name;
   actionJson["icon_path"] = action.icon;
   actionJson["icon_data"] = iconData("actions", action.name, action.icon);
   return actionJson;
}

json::Object connectionObjectTypeJson(const ConnectionObjectType& type) 
{
   json::Object objectTypeJson;
   objectTypeJson["name"]      = type.name;
   objectTypeJson["contains"]  = type.contains;
   objectTypeJson["icon_path"] = type.icon;
   objectTypeJson["icon_data"] = iconData("objects", type.name, type.icon);
   return objectTypeJson;
}

json::Object connectionJson(const Connection& connection)
{
   // form the action array
   json::Array actions;
   BOOST_FOREACH(const ConnectionAction& action, connection.actions)
   {
      actions.push_back(connectionActionJson(action));
   }

   // form the object type array
   json::Array objectTypes;
   BOOST_FOREACH(const ConnectionObjectType& type, connection.objectTypes)
   {
      objectTypes.push_back(connectionObjectTypeJson(type));
   }

   json::Object connectionJson;
   connectionJson["id"]           = connectionIdJson(connection.id);
   connectionJson["connect_code"] = connection.connectCode;
   connectionJson["display_name"] = connection.displayName;
   connectionJson["last_used"]    = connection.lastUsed;
   connectionJson["actions"]      = actions;
   connectionJson["object_types"] = objectTypes;
   connectionJson["icon_path"]    = connection.icon;
   connectionJson["icon_data"]    = iconData("drivers", connection.id.type,
         connection.icon);

   return connectionJson;
}

Error actionFromJson(const json::Object& actionJson,
                     ConnectionAction* pAction)
{
   return json::readObject(actionJson,
         "name", &(pAction->name),
         "icon_path", &(pAction->icon));
}

Error objectTypeFromJson(const json::Object& objectTypeJson,
                         ConnectionObjectType* pObjectType)
{
   return json::readObject(objectTypeJson,
         "name", &(pObjectType->name),
         "contains", &(pObjectType->contains),
         "icon_path", &(pObjectType->icon));
}

Error connectionIdFromJson(const json::Object& connectionIdJson,
                           ConnectionId* pConnectionId)
{
   return json::readObject(connectionIdJson,
         "type", &(pConnectionId->type),
         "host", &(pConnectionId->host));
}

Error connectionFromJson(const json::Object& connectionJson,
                         Connection* pConnection)
{
   // read id fields
   json::Object idJson;
   Error error = json::readObject(connectionJson, "id", &idJson);
   if (error)
      return error;
   error = connectionIdFromJson(idJson, &(pConnection->id));

   // read remaining fields
   json::Array actions;
   json::Array objectTypes;
   error = json::readObject(
            connectionJson,
            "connect_code", &(pConnection->connectCode),
            "display_name", &(pConnection->displayName),
            "actions", &actions,
            "object_types", &objectTypes,
            "icon_path", &(pConnection->icon),
            "last_used", &(pConnection->lastUsed));

   // read each action
   BOOST_FOREACH(const json::Value& action, actions) 
   {
      if (action.type() != json::ObjectType)
         continue;
      ConnectionAction act;
      error = actionFromJson(action.get_obj(), &act);
      if (error)
      {
         // be fault-tolerant here (we can still use the connection even if the
         // actions aren't well-formed)
         LOG_ERROR(error);
         continue;
      }
      pConnection->actions.push_back(act);
   }

   // read each object type
   BOOST_FOREACH(const json::Value& objectType, objectTypes) 
   {
      if (objectType.type() != json::ObjectType)
         continue;
      ConnectionObjectType type;
      error = objectTypeFromJson(objectType.get_obj(), &type);
      if (error)
      {
         LOG_ERROR(error);
         continue;
      }
      pConnection->objectTypes.push_back(type);
   }
   return error;
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
} // namespace session
} // namespace rstudio

