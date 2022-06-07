/*
 * SessionRpcActiveSessionsStorage.cpp
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


#include "SessionRpcActiveSessionsStorage.hpp"

#include <shared_core/json/Json.hpp>

#include <core/json/JsonRpc.hpp>
#include <core/r_util/RActiveSessions.hpp>

#include <session/SessionServerRpc.hpp>

#include "SessionRpcActiveSessionStorage.hpp"

using namespace rstudio::core;
using namespace rstudio::core::r_util;

namespace rstudio {
namespace session {
namespace storage {

RpcActiveSessionsStorage::RpcActiveSessionsStorage(const core::system::User& user) :
   _user(user)
{
}

core::Error RpcActiveSessionsStorage::initSessionProperties(const std::string& id, std::map<std::string, std::string> initialProperties)
{
   RpcActiveSessionStorage sessionStorage(_user, id);
   return sessionStorage.writeProperties(initialProperties);
}

std::vector<std::string> RpcActiveSessionsStorage::listSessionIds() const 
{   
   std::vector<std::string> ids;

   json::Array fields;
   fields.push_back(ActiveSession::kCreated);

   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   // We only really want the ID here, but an empty list will get all fields. Just ask for a single field instead.
   body[kSessionStorageFieldsField] = fields;
   body[kSessionStorageOperationField] = kSessionStroageReadAllOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;
   
   json::Value result;
   Error error = server_rpc::invokeServerRpc(request.method, request.toJsonObject(), &result);
   if (error)
   {
      LOG_ERROR(error);
      return ids;
   }

   json::JsonRpcResponse response;
   bool success = json::JsonRpcResponse::parse(result, &response);
   if (!success)
   {
      error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unable to parse the response from the server when listing all sessions owned by user " + _user.getUsername());
      error.addProperty("response", result.write());
      LOG_ERROR(error);
      return ids;
   }

   json::Array sessionsArr;
   if (!response.result().isObject())
   {
      LOG_ERROR_MESSAGE("Unexpected response  from the server when listing all sessions owned by user " + _user.getUsername() + ": " + result.write());
      return ids;
   }

   error = json::readObject(response.result().getObject(), kSessionStorageSessionsField, sessionsArr);
   if (error)
   {
      LOG_ERROR(error);
      return ids;
   }

   for (json::Array::Iterator itr = sessionsArr.begin(); itr != sessionsArr.end(); ++itr)
   {
      std::string id;
      error = json::readObject((*itr).getObject(), kSessionStorageIdField, id);
      if (error)
         LOG_ERROR(error);
      else
         ids.push_back(id);
   }

   return ids;
}

size_t RpcActiveSessionsStorage::getSessionCount() const 
{
   json::Object body;
   body[kSessionStorageUserIdField] = _user.getUserId();
   body[kSessionStorageOperationField] = kSessionStorageCountOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::Value result;
   Error error = server_rpc::invokeServerRpc(request.method, request.toJsonObject(), &result);
   if (error)
   {
      LOG_ERROR(error);
      return 0;
   }

   json::JsonRpcResponse response;
   bool success = json::JsonRpcResponse::parse(result, &response);
   if (!success)
   {
      error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unable to parse the response from the server when listing all sessions owned by user " + _user.getUsername());
      error.addProperty("response", result.write());
      LOG_ERROR(error);
      return 0;
   }

   if (!response.result().isObject())
   {
      LOG_ERROR_MESSAGE("Unexpected response  from the server when listing all sessions owned by user " + _user.getUsername() + ": " + result.write());
      return 0;
   }

   size_t count = 0;
   error = json::readObject(response.result().getObject(), kSessionStorageCountField, count);

   if (error)
      LOG_ERROR(error);
   
   return count;
}

std::shared_ptr<core::r_util::IActiveSessionStorage> RpcActiveSessionsStorage::getSessionStorage(const std::string& id) const 
{
   return std::shared_ptr<core::r_util::IActiveSessionStorage>(new RpcActiveSessionStorage(_user, id));
}

bool RpcActiveSessionsStorage::hasSessionId(const std::string& sessionId) const 
{
   bool empty = true;
   Error error = getSessionStorage(sessionId)->isEmpty(&empty);
   
   if (error)
      LOG_ERROR(error);
   return !empty;
}

} // namespace storage
} // namespace session
} // namespace rstudio
