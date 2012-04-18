/*
 * ServerSessionProxy.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "ServerSessionProxy.hpp"

#include <vector>
#include <sstream>
#include <map>

#include <boost/date_time/posix_time/posix_time.hpp>

#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <boost/thread/thread_time.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/BoostErrors.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>
#include <core/WaitUtils.hpp>

#include <core/http/SocketUtils.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/LocalStreamAsyncClient.hpp>
#include <core/http/Util.hpp>
#include <core/system/System.hpp>
#include <core/system/PosixUser.hpp>

#include <core/json/JsonRpc.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionLocalStreams.hpp>

#include <server/util/system/System.hpp>
#include <server/util/system/User.hpp>

#include <server/auth/ServerValidateUser.hpp>

#include <server/ServerOptions.hpp>

#include "ServerSessionManager.hpp"

using namespace core ;

namespace server {
namespace session_proxy {
   
namespace {

void launchSessionRecovery(const std::string& username)
{
   Error error = sessionManager().launchSession(username);
   if (error)
      LOG_ERROR(error);
}


http::ConnectionRetryProfile sessionRetryProfile(const std::string& username)
{
   http::ConnectionRetryProfile retryProfile;
   retryProfile.retryInterval = boost::posix_time::milliseconds(25);
   retryProfile.maxWait = boost::posix_time::seconds(10);
   retryProfile.recoveryFunction = boost::bind(launchSessionRecovery,
                                               username);
   return retryProfile;
}


void handleProxyResponse(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      std::string username,
      const http::Response& response)
{
   // if there was a launch pending then remove it
   sessionManager().removePendingLaunch(username);

   // write the response
   ptrConnection->writeResponse(response);
}


void logIfNotConnectionTerminated(const Error& error,
                                  const http::Request& request)
{
   if (!http::isConnectionTerminatedError(error))
   {
      Error logError(error);
      logError.addProperty("request-uri", request.uri());
      LOG_ERROR(logError);
   }
}

void handleContentError(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      std::string username,
      const Error& error)
{   
   // if there was a launch pending then remove it
   sessionManager().removePendingLaunch(username);

   // log if not connection terminated
   logIfNotConnectionTerminated(error, ptrConnection->request());

   // convert connection unavailable to ServiceUnavailable http status
   if (http::isConnectionUnavailableError(error))
   {
      // write service unavailable
      http::Response& response = ptrConnection->response();
      response.setStatusCode(http::status::ServiceUnavailable);
      ptrConnection->writeResponse();
   }
   // otherwise just forward the error
   else
   {
      ptrConnection->writeError(error);
   }
}

void handleRpcError(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
       std::string username,
      const Error& error)
{
   // if there was a launch pending then remove it
   sessionManager().removePendingLaunch(username);

   // log if not connection terminated
   logIfNotConnectionTerminated(error, ptrConnection->request());

   // distinguish between connection and other error types
   if (http::isConnectionUnavailableError(error))
   {
      json::setJsonRpcError(json::errc::ConnectionError,
                            &(ptrConnection->response())) ;
   }
   else
   {
      json::setJsonRpcError(json::errc::TransmissionError,
                           &(ptrConnection->response())) ;
   }

   // write the response
   ptrConnection->writeResponse();
}

void handleEventsError(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const Error& error)
{
   // NOTE: events requests don't initiate session launches so
   // we don't call removePendingLaunch here

   // distinguish connection error as (expected) "Unavailable" error state
   if (http::isConnectionUnavailableError(error))
   {
      json::setJsonRpcError(json::errc::Unavailable,
                            &(ptrConnection->response())) ;
   }
   else
   {
      // log if not connection terminated
      logIfNotConnectionTerminated(error, ptrConnection->request());

      json::setJsonRpcError(json::errc::TransmissionError,
                           &(ptrConnection->response())) ;
   }

   // write the response
   ptrConnection->writeResponse();
}

void proxyRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const http::ErrorHandler& errorHandler,
      const http::ConnectionRetryProfile& connectionRetryProfile =
                                             http::ConnectionRetryProfile())
{
   // calculate stream path
   FilePath streamPath = session::local_streams::streamPath(username);

   // create async client
   boost::shared_ptr<http::LocalStreamAsyncClient> pClient(
    new http::LocalStreamAsyncClient(ptrConnection->ioService(), streamPath));

   // setup retry context
   if (!connectionRetryProfile.empty())
      pClient->setConnectionRetryProfile(connectionRetryProfile);

   // assign request
   pClient->request().assign(ptrConnection->request());

   // execute
   pClient->execute(
         boost::bind(handleProxyResponse, ptrConnection, username, _1),
         errorHandler);
}

// function used to periodically validate that the user is valid (has an
// account on the system and belongs to the required group if specified)
// we used to do this on every request but now do it on client_init and
// get_events. this will introduce less overhead and provide the same
// level of both usability (redirect the user to the login page) and
// and assurance that the user is legit (albeit every 50 seconds rather than
// continuously)  note that this check is placed here mostly to provide good
// feedback to the user that their credentials are no longer valid. the
// user is also validated when they sign in as well as right before
// a session is launched (however the usability factor will be much lower
// if they fail before during session launch since there isn't adequate
// http connection context at that level of the system to return
// json::errc::Unauthorized)
bool validateUser(boost::shared_ptr<http::AsyncConnection> ptrConnection,
                  const std::string& username)
{
   if (server::auth::validateUser(username))
   {
       return true;
   }
   else
   {
       json::setJsonRpcError(json::errc::Unauthorized,
                             &(ptrConnection->response()));
       ptrConnection->writeResponse();
       return false;
   }
}

} // anonymous namespace


Error initialize()
{ 
   return session::local_streams::createStreamsDir();
}

Error runVerifyInstallationSession()
{
   // get current user
   server::util::system::user::User user;
   Error error = currentUser(&user);
   if (error)
      return error;

   // launch verify installation session
   core::system::Options args;
   args.push_back(core::system::Option("--" kVerifyInstallationSessionOption, "1"));
   PidType sessionPid;
   error = server::launchSession(user.username, args, &sessionPid);
   if (error)
      return error;

   // wait for exit
   return util::system::waitForProcessExit(sessionPid);
}

void proxyContentRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   proxyRequest(username,
                ptrConnection,
                boost::bind(handleContentError, ptrConnection, username, _1),
                sessionRetryProfile(username));
}

void proxyRpcRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // validate the user if this is client_init
   if (boost::algorithm::ends_with(ptrConnection->request().uri(),
                                   "client_init"))
   {
      if (!validateUser(ptrConnection, username))
         return;
   }

   proxyRequest(username,
                ptrConnection,
                boost::bind(handleRpcError, ptrConnection, username, _1),
                sessionRetryProfile(username));
}
   
void proxyEventsRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // validate the user
   if (!validateUser(ptrConnection, username))
      return;

   proxyRequest(username,
                ptrConnection,
                boost::bind(handleEventsError, ptrConnection, _1));
}

} // namespace session_proxy
} // namespace server


