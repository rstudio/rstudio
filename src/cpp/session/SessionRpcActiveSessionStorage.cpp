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

#include <core/Log.hpp>
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
   LOG_DEBUG_MESSAGE("Reading property " + name + " from server for session " + _id);

   json::Array fields;
   fields.push_back(name);
   
   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageFieldsField] = fields;
   body[kSessionStorageOperationField] = kSessionStorageReadOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::Value result;
   Error error = server_rpc::invokeServerRpc(request.method, request.toJsonObject(), &result);
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

   if (!response.result().isObject())
   {
      error = Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unexpected type for result field in response when reading fields for session " + _id + " owned by user " + _user.getUsername());
      error.addProperty("response", result.write());

      LOG_ERROR(error);
      return error;
   }


   return json::readObject(response.result().getObject(), name, *pValue);
}

Error RpcActiveSessionStorage::readProperties(const std::set<std::string>& names, std::map<std::string, std::string>* pValues)
{   
   if (!names.empty())
      LOG_DEBUG_MESSAGE("Reading properties { " + boost::join(names, ", ") + " } from server for session " + _id);
      
   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageFieldsField] = json::toJsonArray(names);
   body[kSessionStorageOperationField] = kSessionStorageReadOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::Value result;
   Error error = server_rpc::invokeServerRpc(request.method, request.toJsonObject(), &result);
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

   json::Object resultObj;
   if (!response.result().isObject())
   {
      error = Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unexpected type for result field in response when reading fields for session " + _id + " owned by user " + _user.getUsername());
      error.addProperty("response", result.write());

      LOG_ERROR(error);
      return error;
   }

   resultObj = response.result().getObject();

   for (const auto& name: names)
   {
      std::string value;
      error = json::readObject(resultObj, name, value);
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
   LOG_DEBUG_MESSAGE("Reading properties all properties from server for session " + _id);
   return readProperties({}, pValues);
}

Error RpcActiveSessionStorage::writeProperty(const std::string& name, const std::string& value)
{
   LOG_DEBUG_MESSAGE("Writing property " + name + " with value " + value + " from server for session " + _id);
   json::Object fields;
   fields[name] = value;

   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageFieldsField] = fields;
   body[kSessionStorageOperationField] = kSessionStorageWriteOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::Value result;
   Error error = server_rpc::invokeServerRpc(request.method, request.toJsonObject(), &result);
   if (error)
      return error;

   // No need to wait for the response here.
   return Success();
}

Error RpcActiveSessionStorage::writeProperties(const std::map<std::string, std::string>& properties)
{
   std::string strProps;
   if (log::isLogLevel(log::LogLevel::DEBUG))
   {
      for (auto itr = properties.begin(); itr != properties.end(); ++itr)
      {
         if (!strProps.empty())
            strProps.append(", ");

         strProps.append(itr->first)
            .append(" = ")
            .append(itr->second);
      }
   }

   LOG_DEBUG_MESSAGE("Writing properties { " + strProps + " } from server for session " + _id);
   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageFieldsField] = json::toJsonValue(properties);
   body[kSessionStorageOperationField] = kSessionStorageWriteOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::Value result;
   return server_rpc::invokeServerRpc(request.method, request.toJsonObject(), &result);
}

Error RpcActiveSessionStorage::destroy()
{
   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageIdField] = _id;
   body[kSessionStorageOperationField] = kSessionStorageDeleteOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::Value result;
   return server_rpc::invokeServerRpc(request.method, request.toJsonObject(), &result);
}

Error RpcActiveSessionStorage::isValid(bool* pValue)
{
   LOG_DEBUG_MESSAGE("Checking whether session is valid for id: " + _id);
   // TODO: actual validation
   // We're within the session, so it must be valid.
   *pValue = true;
   return Success();
}

} // namespace storage
} // namespace session
} // namespace rstudio