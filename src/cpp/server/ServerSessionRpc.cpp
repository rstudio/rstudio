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
#include <core/http/TcpIpAsyncServer.hpp>
#include <core/PeriodicCommand.hpp>
#include <core/json/Json.hpp>
#include <core/SecureKeyFile.hpp>
#include <core/SocketRpc.hpp>
#include <core/system/Crypto.hpp>

#include <server/ServerObject.hpp>
#include <server/ServerOptionsOverlay.hpp>
#include <server/ServerSessionManager.hpp>

#include <session/projects/SessionProjectSharing.hpp>

#include "ServerSessionRpc.hpp"

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
   // give the session the shared secret
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
// ensures message signature is valid to ensure that the RPC sender
// is trusted while keeping the secret hidden
bool validateMessageSignature(boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   const http::Request& request = pConnection->request();

   // get the message signature from the header and fail out if it's not there
   std::string messageSignature = request.headerValue(kRstudioMessageSignature);
   if (messageSignature.empty())
   {
      LOG_WARNING_MESSAGE("Session attempted to invoke server RPC without a message signature");
      return false;
   }

   // get the date header and ensure that this request is not too old for us to accept
   // dates are to be specified in the official HTTP format (ex: Wed, 21 Oct 2015 07:28:00 GMT)
   std::string date = request.headerValue("Date");
   if (date.empty())
   {
      LOG_WARNING_MESSAGE("Signed RPC message did not specify a date. "
                          "A valid date must be present on all RPC requests");
      return false;
   }

   // verify the date is in the correct format
   // if not, we will fail out to ensure that an attacker can not supply
   // invalid dates that can exploit the integrity of the system
   if (!http::util::isValidDate(date))
   {
      LOG_WARNING_MESSAGE("Proxy request specified an invalid date: " + date +
                          ". Only HTTP Dates are supported (see HTTP spec for more details)");
      return false;
   }

   boost::posix_time::ptime requestTime = http::util::parseHttpDate(date);

   // check to ensure the request date is not more than 60 seconds apart from now
   // 60 seconds provides good security while still allowing for a bit of clock skew
   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
   boost::posix_time::time_duration timeDelta = now - requestTime;
   if (abs(timeDelta.total_seconds()) > 60)
   {
      LOG_WARNING_MESSAGE("Proxy request specified a date that was more than 60 seconds "
                          "different from now. Please ensure the proxy clock is synchronized "
                          "with the local clock.");
      return false;
   }

   // compute hmac for the message
   std::string payload = date +
                         "\n" +
                         request.body();
   std::vector<unsigned char> hmac;
   Error error = core::system::crypto::HMAC_SHA2(payload, s_sessionSharedSecret, &hmac);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // base 64 encode it
   std::string signature;
   error = core::system::crypto::base64Encode(hmac, &signature);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // ensure that the calculated signature matches that on the message
   if (messageSignature != signature)
   {
      LOG_WARNING_MESSAGE("Proxy auth calculated signature does not match supplied signature");
      return false;
   }

   return true;
}

void validationHandler(
      const core::http::AsyncUriHandlerFunction& handler,
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   // validate that the secret matches what we expect
   std::string secret =
         pConnection->request().headerValue(kServerRpcSecretHeader);

   // if there is no secret, check for a message signature instead
   if (secret.empty())
   {
      if (!validateMessageSignature(pConnection))
      {
         LOG_WARNING_MESSAGE("Session attempted to invoke server RPC with no secret or signature");
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
               prefix, boost::bind(validationHandler, handler, _1));
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
      if (getServerBoolOption(kSpawnerSessionsEnabled))
      {
         // if we're using spawner sessions, we need to handle RPCs
         // from within the regular http server since sessions will be
         // on different machines
         s_pSessionRpcServer = server::server();
      }
      else
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
      }

      s_pSessionRpcServer->setScheduledCommandInterval(
               boost::posix_time::milliseconds(kSessionRpcCmdPeriodMs));   

      // create the shared secret (if necessary)
      Error error = key_file::readSecureKeyFile("session-rpc-key",
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

} // namespace session_rpc
} // namespace server
} // namespace rstudio
