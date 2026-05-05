/*
 * ServerSessionRpc.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <server/session/ServerSessionRpc.hpp>

#include <shared_core/json/Json.hpp>

#include <core/SocketRpc.hpp>
#include <core/http/LocalStreamAsyncServer.hpp>
#include <core/system/User.hpp>

#include <server_core/http/SecureCookie.hpp>
#include <server_core/SecureKeyFile.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerPaths.hpp>
#include <server/session/ServerSessionManager.hpp>

#define kInvalidSecretEndpoint "/invalid_secret"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace server {
namespace session_rpc {

namespace overlay {

typedef boost::function<void(
   const auth::SecureAsyncUriHandlerFunction&,
   http::AsyncUriHandlerFunction,
   bool /*fallbackAllowed*/,
   bool /*requireAdminAuth*/,
   boost::shared_ptr<core::http::AsyncConnection>)> ValidationHandler;

Error initialize(
   const boost::shared_ptr<http::AsyncServer>& pSessionRpcServer,
   const std::string& sessionSharedSecret,
   ValidationHandler validationHandler,
   http::AsyncUriHandlerFunction invalidRequestHandler);

void addHandler(
   const std::string& prefix,
   const auth::SecureAsyncUriHandlerFunction& handler,
   bool allowUserAccess);

void addHttpProxyHandler(
   const std::string& prefix,
   const auth::SecureAsyncUriHandlerFunction& handler);

}

namespace {

std::string s_sessionSharedSecret;
boost::shared_ptr<http::AsyncServer> s_pSessionRpcServer;

// Cached uid of the configured server-user (e.g. "rstudio-server"), resolved
// once at session_rpc::initialize(). Used by the admin-endpoint authorization
// gate in validationHandler so the decision is driven by SO_PEERCRED, not by
// any cookie-derived username. -1 means lookup failed/uninitialized; in that
// case only uid=0 (root) callers can satisfy the gate.
// int64_t avoids the uid_t->int narrowing that would misidentify UID >= 2^31 or
// the nobody sentinel (4294967295) as the uninitialized sentinel.
int64_t s_serverUserUid = -1;

void sessionProfileFilter(core::r_util::SessionLaunchProfile* pProfile)
{
   pProfile->config.environment.push_back(
            std::make_pair(kServerRpcSecretEnvVar, s_sessionSharedSecret));
}

void writeInvalidRequest(boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   http::Response& response = pConnection->response();
   response.setStatusCode(core::http::status::BadRequest);
   response.setStatusMessage("Invalid request.");
   pConnection->writeResponse();
}

// Used by the admin-auth gate in validationHandler. The caller is authenticated
// (or anonymous on the cookie-less path) but not authorized for an admin RPC.
// 403 is the right status here - the request itself is well-formed.
void writeForbiddenResponse(boost::shared_ptr<core::http::AsyncConnection> pConnection,
                            const std::string& reason)
{
   http::Response& response = pConnection->response();
   response.setStatusCode(core::http::status::Forbidden);
   response.setStatusMessage(reason);
   pConnection->writeResponse();
}

// used when clients are sending RPC via TCP
// ensures messages are sent from a trusted (logged in) user
bool validateSecureCookie(boost::shared_ptr<core::http::AsyncConnection> pConnection,
                          std::string* pUser,
                          bool fallbackAllowed)
{
   const http::Request& request = pConnection->request();

   std::string cookieValue = request.cookieValueFromHeader(kRStudioRpcCookieHeader);
   if (cookieValue.empty())
   {
      if (!fallbackAllowed)
         LOG_WARNING_MESSAGE("No auth cookie supplied for server RPC call");
      return false;
   }

   std::string user = core::http::secure_cookie::readSecureCookie(cookieValue);
   if (user.empty())
   {
      if (!fallbackAllowed)
         LOG_WARNING_MESSAGE("Invalid auth cookie supplied for server RPC call");
      return false;
   }

   *pUser = user;
   return true;
}

// validation requiring Rpc secrets
void validationHandler(
      const auth::SecureAsyncUriHandlerFunction& handler,
      http::AsyncUriHandlerFunction unauthorizedResponseFunction,
      bool fallbackAllowed, // failure should not be logged
      bool requireAdminAuth, // endpoint registered with allowUserAccess=false
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   std::string username;

   // if there is no secret, check for a message signature instead
   if (!pConnection->request().containsHeader(kServerRpcSecretHeader))
   {
      if (!validateSecureCookie(pConnection, &username, fallbackAllowed))
      {
         LOG_DEBUG_MESSAGE("validateSecure cookie failed for: " + pConnection->request().uri());
         unauthorizedResponseFunction(pConnection);
         return;
      }
   }
   else
   {
      // used for traditional unix socket mode
      // validate that the secret matches what we expect
      std::string secret = pConnection->request().headerValue(kServerRpcSecretHeader);
      if (secret != s_sessionSharedSecret)
      {
         if (!fallbackAllowed)
         {
            LOG_WARNING_MESSAGE("Session attempted to invoke server RPC with invalid "
                                "secret " + secret);
         }
         LOG_DEBUG_MESSAGE("invalid shared secret - auth failed for: " + pConnection->request().uri());
         unauthorizedResponseFunction(pConnection);
         return;
      }

      // get user on the other end of the socket (if available)
      int64_t uid = pConnection->request().remoteUid();
      if (uid != -1)
      {
         core::system::User user;
         Error error = system::getUserFromUserId(uid, user);
         if (error)
         {
            LOG_WARNING_MESSAGE("Couldn't determine user for Server RPC request");
            LOG_ERROR(error);
            unauthorizedResponseFunction(pConnection);
            return;
         }

         username = user.getUsername();
      }
   }
   pConnection->setUsername(username);

   // API-level authorization for endpoints registered with allowUserAccess=false:
   // require the caller to be root or the configured server-user. Decision is
   // driven by SO_PEERCRED (not by `username`, which can come from a cookie).
   if (requireAdminAuth)
   {
      const int64_t uid = pConnection->request().remoteUid();
      if (uid == -1)
      {
         LOG_WARNING_MESSAGE("Admin RPC " + pConnection->request().uri() +
                             " rejected: no peer credentials");
         writeForbiddenResponse(pConnection,
            "Forbidden: admin operations require local socket transport");
         return;
      }
      if (uid != 0 && uid != s_serverUserUid)
      {
         LOG_WARNING_MESSAGE("Admin RPC " + pConnection->request().uri() +
                             " rejected: caller uid=" + std::to_string(uid) +
                             " is not root or " + server::options().serverUser());
         writeForbiddenResponse(pConnection,
            "Forbidden: admin operations require root or " +
            server::options().serverUser());
         return;
      }
      LOG_DEBUG_MESSAGE("Admin RPC " + pConnection->request().uri() +
                        " invoked by uid=" + std::to_string(uid));
   }

   LOG_DEBUG_MESSAGE("Handling session rpc: " + pConnection->request().debugInfo());

   // invoke the wrapped async URI handler
   handler(username, pConnection);
}

} // anonymous namespace

void addHandler(const std::string& prefix,
                const auth::SecureAsyncUriHandlerFunction& handler,
                bool allowUserAccess)
{
   if (s_pSessionRpcServer)
   {
      s_pSessionRpcServer->addHandler(
               prefix, boost::bind(validationHandler,
                                   handler,
                                   writeInvalidRequest,
                                   false /*fallbackAllowed*/,
                                   !allowUserAccess /*requireAdminAuth*/,
                                   _1));

      overlay::addHandler(prefix, handler, allowUserAccess);
   }
}

void addHttpProxyHandler(const std::string &prefix,
                         const auth::SecureAsyncUriHandlerFunction &handler)
{
   if (s_pSessionRpcServer)
   {
      s_pSessionRpcServer->addHandler(
               prefix, boost::bind(validationHandler,
                                   handler,
                                   writeInvalidRequest,
                                   false /*fallbackAllowed*/,
                                   false /*requireAdminAuth - proxy handlers are not admin-only*/,
                                   _1));

      overlay::addHttpProxyHandler(prefix, handler);
   }
}

void addPeriodicCommand(boost::shared_ptr<PeriodicCommand> pCmd)
{
   if (s_pSessionRpcServer)
   {
      s_pSessionRpcServer->addScheduledCommand(pCmd);
   }
}

Error startup()
{
   // start the server (it might not exist if project sharing isn't on)
   if (s_pSessionRpcServer)
      return s_pSessionRpcServer->run();

   return Success();
}

Error initialize()
{
   // create the async server instance
   s_pSessionRpcServer = boost::make_shared<http::LocalStreamAsyncServer>(
            "Session RPCs",
            std::string(),
            core::FileMode::ALL_READ_WRITE);

   // initialize with path to our socket
   Error error = boost::static_pointer_cast<http::LocalStreamAsyncServer>(s_pSessionRpcServer)->init(
            serverRpcSocketPath());
   if (error)
      return error;

   s_pSessionRpcServer->setScheduledCommandInterval(
            boost::posix_time::milliseconds(kSessionRpcCmdPeriodMs));

   // create the shared secret
   error = key_file::readSecureKeyFile("session-rpc-key",
                                       &s_sessionSharedSecret);
   if (error)
      return error;

   // resolve the configured server-user's uid so the admin-RPC gate in
   // validationHandler can compare peer-creds numerically rather than via
   // a (cookie-derived) username
   {
      core::system::User saUser;
      core::Error saErr = core::system::User::getUserFromIdentifier(
            server::options().serverUser(), saUser);
      if (saErr)
         LOG_ERROR(saErr);
      else
         s_serverUserUid = static_cast<int64_t>(saUser.getUserId());
   }

   // inject the shared secret into the session
   sessionManager().addSessionLaunchProfileFilter(sessionProfileFilter);

   return overlay::initialize(
      s_pSessionRpcServer,
      s_sessionSharedSecret,
      validationHandler,
      writeInvalidRequest);
}

} // namespace session_rpc
} // namespace server
} // namespace rstudio
