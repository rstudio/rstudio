/*
 * RActiveSessionsStorage.cpp
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

#include <core/r_util/RActiveSessionsStorage.hpp>

#include <algorithm>

#include <boost/algorithm/string.hpp>

#include <core/SocketRpc.hpp>

#include <core/r_util/RActiveSessionStorage.hpp>
#include <core/r_util/RActiveSessions.hpp>

#include <core/system/Environment.hpp>


namespace rstudio {
namespace core {
namespace r_util {

namespace
{

Error sessionIdFromFolder(const FilePath& folder, std::string* pSessionId)
{
   std::string baseName = folder.getFilename();
   std::vector<std::string> result;
   boost::split(result, baseName, boost::is_any_of("-"));

   if (result.size() != 2)
   {
      Error error = Error("SessionMigrationError", 1, ERROR_LOCATION);
      error.addProperty("description", "Unexpected file in active sessions: " + folder.getAbsolutePath());
      return error;
   }

   *pSessionId = result[1];
   return Success();
}

FilePath getSessionDirPath(const FilePath& storagePath, const std::string& sessionId)
{
   return storagePath.completeChildPath(kSessionDirPrefix + sessionId);
}

} // anonymous namespace

FileActiveSessionsStorage::FileActiveSessionsStorage(const FilePath& rootStoragePath)
{
   storagePath_ = ActiveSessions::storagePath(rootStoragePath);
   Error error = storagePath_.ensureDirectory();
   if (error)
      LOG_ERROR(error);
}

Error FileActiveSessionsStorage::hasSessionId(const std::string& sessionId, bool* pHasSessionId) const
{
   FilePath dir = getSessionDirPath(storagePath_, sessionId);
   *pHasSessionId = dir.exists();

   return Success();
}

std::vector<std::string> FileActiveSessionsStorage::listSessionIds() const
{
   // list to return
   std::vector<std::string> sessions;

   // enumerate children and check for sessions
   std::vector<FilePath> children;
   Error error = storagePath_.getChildren(children);
   if (error)
   {
      LOG_ERROR(error);
      return sessions;
   }
   std::string prefix = kSessionDirPrefix;
   for (const FilePath& child : children)
   {
      if (boost::algorithm::starts_with(child.getFilename(), prefix))
      {
         std::string id = child.getFilename().substr(prefix.length());
         sessions.push_back(id);
      }

   }

   // return
   return sessions;
}

size_t FileActiveSessionsStorage::getSessionCount() const
{
   return listSessionIds().size();
}

// Returns a shared pointer to the session storage, or an empty session session storage pointer if it does not exist
std::shared_ptr<IActiveSessionStorage> FileActiveSessionsStorage::getSessionStorage(const std::string& id) const
{
   FilePath scratchPath = storagePath_.completeChildPath(kSessionDirPrefix + id);
   return std::make_shared<FileActiveSessionStorage>(scratchPath);
}


RpcActiveSessionsStorage::RpcActiveSessionsStorage(const core::system::User& user, const FilePath& rootStoragePath, InvokeRpc invokeRpcFunc) :
   user_(user),
   storagePath_(rootStoragePath),
   invokeRpcFunc_(std::move(invokeRpcFunc))
{
}

InvokeRpc RpcActiveSessionsStorage::getDefaultRpcFunc() 
{
   return [](const json::JsonRpcRequest& request, json::JsonRpcResponse* pResponse)
   {
      FilePath rpcSocket(core::system::getenv(kServerRpcSocketPathEnvVar));
      LOG_DEBUG_MESSAGE("Invoking rserver RPC '" + request.method + "' on socket " + 
            rpcSocket.getAbsolutePath());

      json::Value result;
      Error error = socket_rpc::invokeRpc(rpcSocket, request.method, request.toJsonObject(), &result);
      if (error)
         return error;

      
      bool success = json::JsonRpcResponse::parse(result, pResponse);
      if (!success)
      {
         error = Error(json::errc::ParseError, ERROR_LOCATION);
         error.addProperty(
            "description",
            "Unable to parse the response for RPC request: " + request.toJsonObject().write());
         error.addProperty("response", result.write());
      }

      return error;
   };
}


std::vector<std::string> RpcActiveSessionsStorage::listSessionIds() const 
{   
   // First attempt to ensure all sessions have been migrated.
   migrateSessions();

   std::vector<std::string> ids;

   json::Array fields;
   fields.push_back(ActiveSession::kCreated);

   json::Object body;
   body[kSessionStorageUserIdField] = user_.getUserId();
   // We only really want the ID here, but an empty list will get all fields. Just ask for a single field instead.
   body[kSessionStorageFieldsField] = fields;
   body[kSessionStorageOperationField] = kSessionStroageReadAllOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;
   
   json::JsonRpcResponse response;
   Error error = invokeRpcFunc_(request, &response);
   if (error)
   {
      LOG_ERROR(error);
      return ids;
   }

   json::Array sessionsArr;
   if (!response.result().isObject())
   {
      LOG_ERROR_MESSAGE("Unexpected response from the server when listing all sessions owned by user " + user_.getUsername() + ": " + response.result().write());
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
   body[kSessionStorageUserIdField] = user_.getUserId();
   body[kSessionStorageOperationField] = kSessionStorageCountOp;

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::JsonRpcResponse response;
   Error error = invokeRpcFunc_(request, &response);
   if (error)
   {
      LOG_ERROR(error);
      return 0;
   }

   if (!response.result().isObject())
   {
      LOG_ERROR_MESSAGE("Unexpected response from the server when listing all sessions owned by user " + user_.getUsername() + ": " + response.result().write());
      return 0;
   }

   uint64_t count = 0;
   error = json::readObject(response.result().getObject(), kSessionStorageCountField, count);

   if (error)
      LOG_ERROR(error);
   
   return (size_t) count;
}

std::shared_ptr<core::r_util::IActiveSessionStorage> RpcActiveSessionsStorage::getSessionStorage(const std::string& id) const 
{
   FilePath scratchPath = storagePath_.completeChildPath(kSessionDirPrefix + id);
   return std::shared_ptr<core::r_util::IActiveSessionStorage>(new RpcActiveSessionStorage(user_, id, scratchPath, invokeRpcFunc_));
}

Error RpcActiveSessionsStorage::hasSessionId(const std::string& sessionId, bool* pHasSessionId) const 
{   
   LOG_DEBUG_MESSAGE("Checking whether session id " + sessionId + " is in use.");
   json::Object body;
   body[kSessionStorageUserIdField] = user_.getUserId();
   body[kSessionStorageIdField] = sessionId;
   body[kSessionStorageOperationField] = kSessionStorageCountOp; 

   json::JsonRpcRequest request;
   request.method = kSessionStorageRpc;
   request.kwparams = body;

   json::JsonRpcResponse response;
   Error error = invokeRpcFunc_(request, &response);
   if (error)
      return error;

   if (!response.result().isObject())
   {
      error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty(
         "description",
         "Unexpected to JSON value in the response from the server when checking whether session with id " + sessionId + " is empty.");
      error.addProperty("response", response.result().write());

      LOG_ERROR(error);
      return error;
   }

   int count = 0;
   error = json::readObject(response.result().getObject(), kSessionStorageCountField, count);
   if (error)
      return error;

   *pHasSessionId = count > 0;

   return error;
}

void RpcActiveSessionsStorage::migrateSessions() const
{
   static const std::string migratedFileName = ".migrated";
   static const std::string properites = "properites";

   FilePath rootMigratedFile = storagePath_.completeChildPath(migratedFileName);
   if (!rootMigratedFile.exists())
   {
      std::vector<FilePath> children;
      Error error = storagePath_.getChildren(children);
      if (error)
      {
         error.addProperty("operation", "Attempting to migrate old sessions for user " + user_.getUsername());
         LOG_ERROR(error);
         return;
      }

      for (const FilePath& child: children)
      {
         FilePath migratedFile = child.completeChildPath(migratedFileName);
         if (!migratedFile.exists())
         {
            if (child.completeChildPath(properites).exists())
            {
               std::string sessionId;
               Error error = sessionIdFromFolder(child, &sessionId);
               if (error)
               {
                  error.addProperty("operation", "Attempting to migrate old sessions for user " + user_.getUsername());
                  LOG_ERROR(error);
                  continue;
               }

               ActiveSession fileActiveSession(
                  sessionId,
                  child,
                  std::shared_ptr<core::r_util::IActiveSessionStorage>(
                     new FileActiveSessionStorage(child)));

               ActiveSession rpcActiveSession(
                  sessionId,
                  child,               
                  std::shared_ptr<core::r_util::IActiveSessionStorage>(
                     new RpcActiveSessionStorage(user_, sessionId, child, invokeRpcFunc_)));

               std::map<std::string, std::string> props;
               error = fileActiveSession.readProperties({}, &props);
               if (error)
               {
                  error.addProperty("operation", "Attempting to migrate old sessions for user " + user_.getUsername());
                  error.addProperty("sessionId", sessionId);
                  LOG_ERROR(error);
                  continue;
               }

               error = rpcActiveSession.writeProperties(props);
               if (error)
               {
                  error.addProperty("operation", "Attempting to migrate old sessions for user " + user_.getUsername());
                  error.addProperty("sessionId", sessionId);
                  LOG_ERROR(error);
                  continue;
               }
            }

            migratedFile.ensureFile();
         }
      }

      rootMigratedFile.ensureFile();
   }
}

} // namespace r_util
} // namsepace core
} // namespace rstudio
