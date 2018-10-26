/*
 * ServerSessionRpc.cpp
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
#include <server_core/http/SecureCookie.hpp>
#include <core/http/TcpIpAsyncServer.hpp>
#include <core/PeriodicCommand.hpp>
#include <core/json/Json.hpp>
#include <server_core/SecureKeyFile.hpp>
#include <server_core/SocketRpc.hpp>
#include <core/system/Crypto.hpp>

#include <core/system/PosixUser.hpp>

#include <server/ServerObject.hpp>
#include <server/ServerOptionsOverlay.hpp>
#include <server/ServerSessionManager.hpp>

#include <session/projects/SessionProjectSharing.hpp>

#include "load_balancer/LoadBalancer.hpp"
#include "ServerActivation.hpp"
#include "ServerSessionRpc.hpp"
#include "ServerJobLauncher.hpp"

#define kInvalidSecretEndpoint "/invalid_secret"

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace session_rpc {

namespace {

std::string s_sessionSharedSecret;
boost::shared_ptr<http::AsyncServer> s_pSessionRpcServer;

void sessionProfileFilter(core::r_util::SessionLaunchProfile* pProfile)
{
   pProfile->config.environment.push_back(
            std::make_pair(kServerRpcSecretEnvVar, s_sessionSharedSecret));
}

void disableSharingFilter(core::r_util::SessionLaunchProfile* pProfile)
{
   pProfile->config.environment.push_back(
      std::make_pair<std::string>(kRStudioDisableProjectSharing, "1"));
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
                          std::string* pUser)
{
   const http::Request& request = pConnection->request();

   std::string cookieValue = request.cookieValueFromHeader(kRstudioRpcCookieHeader);
   if (cookieValue.empty())
   {
      LOG_WARNING_MESSAGE("No auth cookie supplied for server RPC call");
      return false;
   }

   std::string user = core::http::secure_cookie::readSecureCookie(cookieValue);
   if (user.empty())
   {
      LOG_WARNING_MESSAGE("Invalid auth cookie supplied for server RPC call");
      return false;
   }

   *pUser = user;
   return true;
}

void validationHandler(
      const auth::SecureAsyncUriHandlerFunction& handler,
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   std::string username;

   // validate that the secret matches what we expect
   std::string secret =
         pConnection->request().headerValue(kServerRpcSecretHeader);

   // if there is no secret, check for a message signature instead
   if (secret.empty())
   {
      if (!validateSecureCookie(pConnection, &username))
      {
         writeInvalidRequest(pConnection);
         return;
      }
   }
   else
   {
      // used for traditional unix socket mode
      if (secret != s_sessionSharedSecret)
      {
         LOG_WARNING_MESSAGE("Session attempted to invoke server RPC with invalid "
                             "secret " + secret);
         writeInvalidRequest(pConnection);
         return;
      }

      // get user on the other end of the socket (if available)
      int uid = pConnection->request().remoteUid();
      if (uid != -1)
      {
         core::system::user::User user;
         Error error = core::system::user::userFromId(uid, &user);
         if (error)
         {
            LOG_WARNING_MESSAGE("Couldn't determine user for Server RPC request");
            LOG_ERROR(error);
            writeInvalidRequest(pConnection);
            return;
         }

         username = user.username;
      }
   }

   // invoke the wrapped async URI handler
   handler(username, pConnection);
}

} // anonymous namespace

void addHandler(const std::string &prefix,
                const auth::SecureAsyncUriHandlerFunction &handler)
{
   if (s_pSessionRpcServer)
   {
      s_pSessionRpcServer->addHandler(
               prefix, boost::bind(validationHandler, handler, _1));

      if (job_launcher::launcherSessionsEnabled(false /*checkLicense*/))
      {
         // if we're using job launcher sessions, we need to handle RPCs
         // from within the regular http server since sessions will be
         // on different machines - we add the handler to both because
         // at any time, the launcher licensing field could change, and
         // so sessions need to be able to communicate effectively in both
         // launcher and non-launcher modes
         server::server()->addHandler(
                  prefix, boost::bind(validationHandler, handler, _1));
      }
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
            core::system::EveryoneReadWriteMode);

   // initialize with path to our socket
   Error error = boost::static_pointer_cast<http::LocalStreamAsyncServer>(s_pSessionRpcServer)->init(
            FilePath(kServerRpcSocketPath));
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

   bool hasSharedStorage = !getServerPathOption(kSharedStoragePath).empty();
   bool projectSharingEnabled = getServerBoolOption(kServerProjectSharing);

   if (!hasSharedStorage || !projectSharingEnabled)
   {
      // disable project sharing
      sessionManager().addSessionLaunchProfileFilter(disableSharingFilter);
   }

   return Success();
}

} // namespace session_rpc
} // namespace server
} // namespace rstudio
