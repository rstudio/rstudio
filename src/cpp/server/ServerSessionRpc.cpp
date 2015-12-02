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
#include <core/PeriodicCommand.hpp>
#include <core/json/Json.hpp>

#include <server/ServerOptionsOverlay.hpp>
#include <server/ServerSessionManager.hpp>
#include <server/ServerSecureKeyFile.hpp>

#include <session/SessionServerRpc.hpp>
#include <session/projects/SessionProjectSharing.hpp>

#include "ServerSessionRpc.hpp"

#define kInvalidSecretEndpoint "/invalid_secret"

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace session_rpc {

namespace {

std::string s_sessionSharedSecret;
boost::shared_ptr<http::LocalStreamAsyncServer> s_pSessionRpcServer;

void sessionProfileFilter(core::r_util::SessionLaunchProfile* pProfile)
{
   // give the session the shared secret
   pProfile->config.environment.push_back(
         std::make_pair(kServerRpcSecretEnvVar, s_sessionSharedSecret));
}

void disableSharingFilter(core::r_util::SessionLaunchProfile* pProfile)
{
   pProfile->config.environment.push_back(
      std::make_pair<std::string>(kRStudioDisableProjectSharing, "1"));
}

void secretValidatingHandler(
      const core::http::AsyncUriHandlerFunction& handler,
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   // validate that the secret matches what we expect
   core::http::Response* pResponse = &(pConnection->response());
   std::string secret =
         pConnection->request().headerValue(kServerRpcSecretHeader);
   if (secret != s_sessionSharedSecret)
   {
      LOG_WARNING_MESSAGE("Session attempted to invoke server RPC with invalid "
                          "secret " + secret);
      pResponse->setStatusCode(core::http::status::BadRequest);
      pResponse->setStatusMessage("Invalid request.");
      pConnection->writeResponse();
      return;
   }

   // invoke the wrapped async URI handler
   handler(pConnection);
}

} // anonymous namespace

void addHandler(const std::string &prefix,
                const core::http::AsyncUriHandlerFunction &handler)
{
   if (s_pSessionRpcServer)
   {
      s_pSessionRpcServer->addHandler(
               prefix, boost::bind(secretValidatingHandler, handler, _1));
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
   // currently only shared projects perform session RPCs
   if (getServerBoolOption(kServerProjectSharing) &&
       !getServerPathOption(kSharedStoragePath).empty())
   {
      // create the async server instance
      s_pSessionRpcServer = boost::make_shared<http::LocalStreamAsyncServer>(
               "Session RPCs",
               std::string(),
               core::system::EveryoneReadWriteMode);

      s_pSessionRpcServer->setScheduledCommandInterval(
               boost::posix_time::milliseconds(kSessionRpcCmdPeriodMs));

      // initialize with path to our scoket
      Error error = s_pSessionRpcServer->init(FilePath(kServerRpcSocketPath));
      if (error)
         return error;

      // create the shared secret (if necessary)
      error = key_file::readSecureKeyFile("session-rpc-key",
                                          &s_sessionSharedSecret);
      if (error)
         return error;

      // inject the shared secret into the session
      sessionManager().addSessionLaunchProfileFilter(sessionProfileFilter);
   }
   else
   {
      // add filter to disable project sharing
      sessionManager().addSessionLaunchProfileFilter(disableSharingFilter);
   }

   return Success();
}

} // namespace acls
} // namespace server
} // namespace rstudio
