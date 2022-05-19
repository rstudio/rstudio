/*
 * SessionRpcActiveSessionStorage.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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


#include "SessionRpcActiveSessionStorage.hpp"

#include <boost/algorithm/string/join.hpp>

#include <shared_core/json/Json.hpp>

#include <core/json/JsonRpc.hpp>
#include <core/r_util/RActiveSessions.hpp>

#include <session/SessionServerRpc.hpp>

using namespace rstudio::core;
using namespace rstudio::core::r_util;

namespace rstudio {
namespace session {
namespace storage {

RpcActiveSessionStorage::RpcActiveSessionStorage(const system::User& user, std::string sessionId) :
   _user(user),
   _id(std::move(sessionId))
{
}

Error RpcActiveSessionStorage::readProperty(const std::string& name, std::string* pValue)
{
   json::Array fields;
   fields.push_back(name);
   
   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageFieldsField] = fields;
   body[kSessionStorageOperationField] = kSessionStorageReadOp;

   json::Value result;
   Error error = server_rpc::invokeServerRpc(kSessionStorageRpc, body, &result);
   if (error)
      return error;

   json::JsonRpcResponse response;
   bool success = json::JsonRpcResponse::parse(result, &response);
   if (!success)
   {
      error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unable to parse the response from the server when reading the " + name + " field for session " + _id + " owned by user " + _user.getUsername());
      error.addProperty("response", result.write());
      LOG_ERROR(error);
      return error;
   }

   return response.getField(name, pValue);
}

Error RpcActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{   
   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageFieldsField] = json::toJsonArray(names);
   body[kSessionStorageOperationField] = kSessionStorageReadOp;

   json::Value result;
   Error error = server_rpc::invokeServerRpc(kSessionStorageRpc, body, &result);
   if (error)
      return error;

   json::JsonRpcResponse response;
   bool success = json::JsonRpcResponse::parse(result, &response);
   if (!success)
   {
      error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unable to parse the response from the server when reading the fields for session " + _id + " owned by user " + _user.getUsername());
      error.addProperty("response", result.write());

      if (!names.empty())
         error.addProperty("fields", boost::algorithm::join(names, ", "));

      LOG_ERROR(error);
      return error;
   }

   for (const auto& name: names)
   {
      std::string value;
      error = response.getField(name, &value);
      if (error)
         LOG_ERROR(error);
      else
         (*pValues)[name] = value;
   }

   if (names.empty())
   {
      if (!response.result().isObject())
      {
         error = Error(json::errc::ParseError, ERROR_LOCATION);
         error.addProperty(
            "description",
            "Unable to parse the response from the server when reading the fields for session " + _id + " owned by user " + _user.getUsername());
         error.addProperty("response", result.write());

         if (!names.empty())
            error.addProperty("fields", boost::algorithm::join(names, ", "));

         LOG_ERROR(error);
         return error;
      }

      json::Object resultObj = response.result().getObject();
      for (auto itr = resultObj.begin(); itr != resultObj.end(); ++itr)
         if ((*itr).getValue().isString())
            (*pValues)[(*itr).getName()] = (*itr).getValue().getString();
   }

   return Success();
}

Error RpcActiveSessionStorage::readProperties(std::map<std::string, std::string>* pValues)
{
   return readProperties({}, pValues);
}

Error RpcActiveSessionStorage::writeProperty(const std::string& name, const std::string& value)
{
   json::Object fields;
   fields[name] = value;

   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageFieldsField] = fields;
   body[kSessionStorageOperationField] = kSessionStorageWriteOp;

   json::Value result;
   Error error = server_rpc::invokeServerRpc(kSessionStorageRpc, body, &result);
   if (error)
      return error;

   // No need to wait for the response here.
   return Success();
}

Error RpcActiveSessionStorage::writeProperties(const std::map<std::string, std::string>& properties)
{
   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageFieldsField] = json::toJsonValue(properties);
   body[kSessionStorageOperationField] = kSessionStorageWriteOp;

   json::Value result;
   Error error = server_rpc::invokeServerRpc(kSessionStorageRpc, body, &result);
   if (error)
      return error;

   // No need to wait for the response here.
   return Success();
}

} // namespace storage
} // namespace session
} // namespace rstudio