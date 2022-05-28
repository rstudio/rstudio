/*
 * SessionSessionMetadataRpc.cpp
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

#include <server/ServerSessionMetadataRpc.hpp>

#include <string>
#include <set>
#include <map>
#include <memory>

#include <shared_core/json/Json.hpp>
#include <shared_core/system/User.hpp>

#include <core/Log.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <core/system/Xdg.hpp>

#include <server/ServerOptions.hpp>
#include <server/auth/ServerAuthHandler.hpp>

#include "ServerSessionRpc.hpp"

using namespace rstudio::core;
using namespace rstudio::core::r_util;

namespace rstudio {
namespace server {
namespace session_metadata {

namespace {


typedef std::function<Error (boost::system::error_code, const Error&, const ErrorLocation&)> BaseError;

inline FilePath userDataDir(const system::User& user)
{
   return system::xdg::userDataDir(user.getUsername(), user.getHomePath());
}

inline std::unique_ptr<r_util::ActiveSessions> getActiveSessions(const system::User& user)
{
   return std::unique_ptr<r_util::ActiveSessions>(new r_util::ActiveSessions(userDataDir(user)));
}

inline boost::shared_ptr<r_util::ActiveSession> getActiveSession(const system::User& user, const std::string& sessionId)
{
   return getActiveSessions(user)->get(sessionId);
}

Error missingFieldError(
   const BaseError& baseErrorFun,
   const std::string& field,
   const std::string& body,
   const ErrorLocation& errorLocation)
{
   Error error = baseErrorFun(json::errc::ParamMissing, Success(), errorLocation);
   error.addProperty("description", "Session metadata RPC request is missing required field \"" + field + "\": " + body);
   error.addProperty("field", field);
   LOG_ERROR(error);
   return error;
}

Error noUserIdError(
   const BaseError& baseErrorFun,
   const std::string& username,
   const std::string& sessionId,
   const ErrorLocation& errorLocation)
{
   Error error = baseErrorFun(json::errc::ParamMissing, Success(), errorLocation);
   error.addProperty("description", "User " + username + " has made a request for a session " + sessionId + " without the required " + kSessionStorageUserIdField + " field.");
   error.addProperty("sessionId", sessionId);
   LOG_ERROR(error);
   return error;
}

Error handleRead(
   const system::User& user,
   const std::string& sessionId,
   const std::set<std::string>& fields,
   std::map<std::string, std::string>* pValues)
{
   return getActiveSession(user, sessionId)->readProperties(fields, pValues);
}

Error handleReadAll(
   const boost::optional<system::User>& user,
   const std::set<std::string>& fields,
   std::vector<std::map<std::string, std::string>>* pValues)
{
   if (user)
   {
      std::unique_ptr<ActiveSessions> aciveSessions = getActiveSessions(user.get());
      std::vector<boost::shared_ptr<ActiveSession> > sessions = 
         aciveSessions->list(
            userDataDir(user.get()),
            options().getOverlayOption("server-project-sharing") == "1");

      for (const auto& session: sessions)
      {
         std::map<std::string, std::string> sessionProps;
         Error error = session->readProperties(fields, &sessionProps);
         if (error)
         {
            error.addProperty("session-id", session->id());
            LOG_ERROR(error);
         }
         else
         {
            sessionProps[kSessionStorageIdField] = session->id();
            pValues->push_back(sessionProps);
         }
      }
   }

   // Because we only log errors and always return success, the user may experience silent 
   // failures. Depending on the use case of this call it may be fine.
   return Success();
}

Error handleWrite(
   const system::User& user,
   const std::string& sessionId,
   const std::map<std::string, std::string>& values)
{
   return getActiveSession(user, sessionId)->writeProperties(values);
}

Error authorizeRequest(
   const BaseError& baseErrorFun,
   const std::string& requester,
   boost::optional<system::UidType> sessionOwner,
   system::User* pRequesterUser,
   boost::optional<system::User>* pSessionUser,
   bool* pRequesterIsAdmin)
{
   using namespace rstudio::core::system;

   Error error = User::getUserFromIdentifier(requester, *pRequesterUser);
   if (error)
      return error;

   if (sessionOwner)
   {
      User sessionUser;
      error = User::getUserFromIdentifier(sessionOwner.get(), sessionUser);
      if (error)
         return error;

      *pSessionUser = sessionUser;
   }

   *pRequesterIsAdmin = auth::handler::overlay::isUserAdmin(pRequesterUser->getUsername());

   // If the user is not an admin and there is no user-id specified in the request (which means read for all users),
   // or the user in the request is not the same as the request initiator, disallow access
   if (!*pRequesterIsAdmin && (!sessionOwner || (*pRequesterUser != pSessionUser->get())))
   {
      std::string desc = "User " + pRequesterUser->getUsername() + " has made a request for ";
      if (sessionOwner)
         desc += "a session owned by " + pSessionUser->get().getUsername() + " and is not an admin.";
      else
         desc += "sessions owned by any user and is not an admin.";
         
      error = baseErrorFun(json::errc::Unauthorized, Success(), ERROR_LOCATION);
      error.addProperty("description", desc);
      error.addProperty("user", requester);
      
      if (sessionOwner)
         error.addProperty("sessionOwner", pSessionUser->get().getUsername());
      LOG_ERROR(error);
   }

   return error;
}

void handleMetadataRpcImpl(const std::string& username, boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   json::JsonRpcResponse response;
   json::JsonRpcRequest rpcRequest;
   Error error = json::parseJsonRpcRequest(pConnection->request().body(), &rpcRequest);
   if (error)
   {
      error.addProperty("user", username);
      error.addProperty("body",  pConnection->request().body());
      error.addOrUpdateProperty("description", "Failed to parse Session Metadata RPC request: " + pConnection->request().body());
      LOG_ERROR(error);
      return json::setJsonRpcError(error, &pConnection->response(), true);
   }

   const std::string& body =  pConnection->request().body();

   system::User requester;
   boost::optional<system::UidType> sessionOwnerUid;
   boost::optional<system::User> sessionOwner;
   bool isAdmin;
   
   std::string operation;
   error = json::readObject(rpcRequest.kwparams, std::string(kSessionStorageOperationField), operation);
   if (error)
   {
      std::string description = "Invalid type supplied for required field \"" + std::string(kSessionStorageOperationField) + "\".";
      json::errc::errc_t err = json::errc::ParamTypeMismatch;
      if (json::isMissingMemberError(error))
      {
         err = json::errc::ParamMissing;
         description = "Required field \"" + std::string(kSessionStorageOperationField) + "\" is missing.";
      }

      error = Error(err, error, ERROR_LOCATION);
      error.addProperty("user", username);
      error.addProperty("body", pConnection->request().body());
      error.addProperty("description", description);
      LOG_ERROR(error);

      return json::setJsonRpcError(error, &pConnection->response(), true);
   } 

   const BaseError baseError = [operation, username, body](
      boost::system::error_code errorCode,
      const Error& cause,
      const ErrorLocation& errorLocation)
   {

      Error error = Error(errorCode, cause, errorLocation);
      error.addProperty("user", username);
      error.addProperty("operation", operation);
      error.addProperty("body", body);
      return error;
   };

   if (rpcRequest.kwparams.hasMember(kSessionStorageUserIdField))
   {
      error = readObject(rpcRequest.kwparams, kSessionStorageUserIdField, sessionOwnerUid);
      if (error)
      {
         error = baseError(json::errc::ParamInvalid, error, ERROR_LOCATION);
         error.addProperty("field", "kSessionStorageUserField");
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }
   }

   error = authorizeRequest(baseError, username, sessionOwnerUid, &requester, &sessionOwner, &isAdmin);
   if (error)
      return json::setJsonRpcError(error, &pConnection->response(), true);

   if ((operation != kSessionStroageReadAllOp) && !rpcRequest.kwparams.hasMember(kSessionStorageIdField))
      return json::setJsonRpcError(
         missingFieldError(baseError, username, kSessionStorageIdField, ERROR_LOCATION), &pConnection->response(), true);

   if (!rpcRequest.kwparams.hasMember(kSessionStorageFieldsField))
      return json::setJsonRpcError(
         missingFieldError(baseError, username, kSessionStorageFieldsField, ERROR_LOCATION), &pConnection->response(), true);

   if (operation == kSessionStorageWriteOp)
   {
      std::string sessionId;
      std::map<std::string, std::string> fields;
      error = json::readObject(rpcRequest.kwparams, kSessionStorageFieldsField, fields, kSessionStorageIdField, sessionId);
      if (error)
      {
         json::errc::errc_t err = json::errc::ParamTypeMismatch;
         if (json::isMissingMemberError(error))
            err = json::errc::ParamMissing;
         error = baseError(err, error, ERROR_LOCATION);
         LOG_ERROR(error);
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }

      if (!sessionOwner)
      {
         // This should be impossible because we already did validation that this field exists.
         assert(false);
         error = noUserIdError(baseError, username, sessionId, ERROR_LOCATION);
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }

      error = handleWrite(sessionOwner.get(), sessionId, fields);
      if (error)
      {
         error = baseError(json::errc::ExecutionError, error, ERROR_LOCATION);
         LOG_ERROR(error);
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }
      else
      {
         response.setResult(true);
      }
   }
   else if (operation == kSessionStorageReadOp)
   {
      std::string sessionId;
      std::set<std::string> fields;
      error = json::readObject(rpcRequest.kwparams, kSessionStorageFieldsField, fields, kSessionStorageIdField, sessionId);
      if (error)
      {
         json::errc::errc_t err = json::errc::ParamTypeMismatch;
         if (json::isMissingMemberError(error))
            err = json::errc::ParamMissing;
         error = baseError(err, error, ERROR_LOCATION);
         LOG_ERROR(error);
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }

      if (!sessionOwner)
      {
         // This should be impossible because we already did validation that this field exists.
         assert(false);
         error = noUserIdError(baseError, username, sessionId, ERROR_LOCATION);
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }

      std::map<std::string, std::string> result;
      error = handleRead(sessionOwner.get(), sessionId, fields, &result);
      if (error)
      {
         error = baseError(json::errc::ExecutionError, error, ERROR_LOCATION);
         LOG_ERROR(error);
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }
      else
         response.setResult(json::toJsonValue(result));
   }
   else if (operation == kSessionStroageReadAllOp)
   {
      std::set<std::string> fields;
      error = json::readObject(rpcRequest.kwparams, kSessionStorageFieldsField, fields);
      if (error)
      {
         json::errc::errc_t err = json::errc::ParamTypeMismatch;
         if (json::isMissingMemberError(error))
            err = json::errc::ParamMissing;
         error = baseError(err, error, ERROR_LOCATION);
         LOG_ERROR(error);
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }
      

      std::vector<std::map<std::string, std::string>> result;
      error = handleReadAll(sessionOwner, fields, &result);
      if (error)
      {
         error = baseError(json::errc::ExecutionError, error, ERROR_LOCATION);
         LOG_ERROR(error);
         return json::setJsonRpcError(error, &pConnection->response(), true);
      }
      else
      {
         json::Array sessionArray;

         for (const auto& val: result)
            sessionArray.push_back(json::toJsonValue(val));

         json::Object sessionsObj;
         sessionsObj.insert(kSessionStorageSessionsField, sessionArray);
         response.setResult(sessionsObj);
      }
   }
   else 
   {
      error = Error(json::errc::ParamInvalid, ERROR_LOCATION);
      error.addOrUpdateProperty("description", "Invalid operation requested for Session Metadata RPC: " + operation);
      error.addProperty("user", username);
      error.addProperty("operation", operation);
      LOG_ERROR(error);
      return json::setJsonRpcError(error, &pConnection->response(), true);
   }

   json::setJsonRpcResponse(response, &pConnection->response());
}

void handleMetadataRpc(const std::string& username, boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   handleMetadataRpcImpl(username, pConnection);
   pConnection->writeResponse();
}

} // anonymous namespace

Error initialize()
{
   session_rpc::addHandler(kSessionStorageRpc, handleMetadataRpc);

   return Success();
}

} // namespace session_metadata
} // namespace server
} // namespace rstudio
