/*
 * ServerSessionRpc.cpp
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

#include <core/http/LocalStreamAsyncServer.hpp>
#include <core/http/Cookie.hpp>
#include <core/http/CSRFToken.hpp>
#include <server_core/http/SecureCookie.hpp>
#include <core/http/TcpIpAsyncServer.hpp>
#include <core/PeriodicCommand.hpp>
#include <shared_core/json/Json.hpp>
#include <server_core/SecureKeyFile.hpp>
#include <core/SocketRpc.hpp>
#include <core/system/Crypto.hpp>

#include <core/system/PosixUser.hpp>

#include <server/ServerConstants.hpp>
#include <server/ServerObject.hpp>
#include <server/ServerOptions.hpp>
#include <server/ServerPaths.hpp>
#include <server/ServerSessionManager.hpp>

#include <session/projects/SessionProjectSharing.hpp>

#include "ServerSessionRpc.hpp"

#define kInvalidSecretEndpoint "/invalid_secret"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace server {
namespace session_rpc {

namespace overlay {
   Error initialize();
   void addHttpProxyHandler();
}

namespace {

std::string s_sessionSharedSecret;
boost::shared_ptr<http::AsyncServer> s_pSessionRpcServer;

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

// used when clients are sending RPC via TCP
// ensures messages are sent from a trusted (logged in) user
bool validateSecureCookie(boost::shared_ptr<core::http::AsyncConnection> pConnection,
                          std::string* pUser,
                          bool fallbackAllowed)
{
   const http::Request& request = pConnection->request();

   std::string cookieValue = request.cookieValueFromHeader(kRstudioRpcCookieHeader);
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
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   std::string username;

   // validate that the secret matches what we expect
   std::string secret =
         pConnection->request().headerValue(kServerRpcSecretHeader);

   // if there is no secret, check for a message signature instead
   if (secret.empty())
   {
      if (!validateSecureCookie(pConnection, &username, fallbackAllowed))
      {
         unauthorizedResponseFunction(pConnection);
         return;
      }
   }
   else
   {
      // used for traditional unix socket mode
      if (secret != s_sessionSharedSecret)
      {
         if (!fallbackAllowed)
         {
            LOG_WARNING_MESSAGE("Session attempted to invoke server RPC with invalid "
                                "secret " + secret);
         }
         unauthorizedResponseFunction(pConnection);
         return;
      }

      // get user on the other end of the socket (if available)
      int uid = pConnection->request().remoteUid();
      if (uid != -1)
      {
         core::system::User user;
         Error error = core::system::User::getUserFromIdentifier(uid, user);
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
                                   _1));

      // check if we allow user access - meaning users do not need special user-hidden RPC
      // secret in order to call the RPC. some administrator protected RPCs should not be
      // invokable by regular users
      if (allowUserAccess)
      {
         // if we're using job launcher sessions, we need to handle RPCs
         // from within the regular http server since sessions will be
         // on different machines - we add the handler to both because
         // at any time, the launcher licensing field could change, and
         // so sessions need to be able to communicate effectively in both
         // launcher and non-launcher modes
         server::server()->addHandler(
                  prefix, boost::bind(validationHandler,
                                      handler,
                                      writeInvalidRequest,
                                      false /*fallbackAllowed*/,
                                      _1));
      }
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
                                   _1));

      overlay::addHttpProxyHandler();
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

   // inject the shared secret into the session
   sessionManager().addSessionLaunchProfileFilter(sessionProfileFilter);

   return overlay::initialize();
}

} // namespace session_rpc
} // namespace server
} // namespace rstudio
