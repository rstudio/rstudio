/*
 * ServerSessionProxy.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "ServerSessionProxy.hpp"

#include <vector>
#include <sstream>
#include <map>

#include <boost/regex.hpp>

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
#include <core/http/SocketProxy.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/LocalStreamAsyncClient.hpp>
#include <core/http/TcpIpAsyncClient.hpp>
#include <core/http/Util.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/PosixUser.hpp>

#include <core/json/JsonRpc.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionLocalStreams.hpp>

#include <server/auth/ServerValidateUser.hpp>
#include <server/auth/ServerAuthHandler.hpp>

#include <server/ServerOptions.hpp>

#include "ServerSessionManager.hpp"

using namespace core ;

namespace server {
namespace session_proxy {
   
namespace {

void launchSessionRecovery(const std::string& username)
{
   // recreate streams dir if necessary
   Error error = session::local_streams::ensureStreamsDir();
   if (error)
      LOG_ERROR(error);

   error = sessionManager().launchSession(username);
   if (error)
      LOG_ERROR(error);
}

http::ConnectionRetryProfile sessionRetryProfile(const std::string& username)
{
   http::ConnectionRetryProfile retryProfile;
   retryProfile.retryInterval = boost::posix_time::milliseconds(25);
   retryProfile.maxWait = boost::posix_time::seconds(10);
   retryProfile.recoveryFunction = boost::bind(launchSessionRecovery, username);
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

class LocalhostAsyncClient : public http::TcpIpAsyncClient
{
public:
   LocalhostAsyncClient(boost::asio::io_service& ioService,
                        const std::string& address,
                        const std::string& port)
      : http::TcpIpAsyncClient(ioService, address, port)
   {
   }

private:
   // detect when we've got the whole response and force a response and a
   // close of the socket (this is because the current version of httpuv
   // expects a close from the client end of the socket)
   virtual bool stopReadingAndRespond()
   {
      return response_.body().length() >= response_.contentLength();
   }

   // ensure that we don't close the connection when a websockets
   // upgrade is taking place
   virtual bool keepConnectionAlive()
   {
      return response_.statusCode() == http::status::SwitchingProtocols;
   }
};

void handleLocalhostResponse(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      boost::shared_ptr<LocalhostAsyncClient> ptrLocalhost,
      const std::string& port,
      const http::Response& response)
{
   // check for upgrade to websockets
   if (response.statusCode() == http::status::SwitchingProtocols)
   {
      // write the response but don't close the connection
      ptrConnection->writeResponse(response, false);

      // cast to generic socket types
      boost::shared_ptr<http::Socket> ptrClient =
         boost::static_pointer_cast<http::Socket>(ptrConnection);
      boost::shared_ptr<http::Socket> ptrServer =
         boost::static_pointer_cast<http::Socket>(ptrLocalhost);

      // connect the sockets
      http::SocketProxy::create(ptrClient, ptrServer);
   }
   // normal response, write and close (handle redirects if necessary)
   else
   {   
      // re-write location headers if necessary
      std::string location = response.headerValue("Location");
      if (!location.empty())
      {
         location = "/p/" + port + location;
         http::Response redirectResponse;
         redirectResponse.assign(response);
         redirectResponse.setHeader(http::Header("Location", location));
         ptrConnection->writeResponse(redirectResponse);
      }
      else
      {
         ptrConnection->writeResponse(response);
      }
   }
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

   // handle connection unavailable with sign out if session launches
   // require authentication, otherwise just return service unavailable
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
                            &(ptrConnection->response()));
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
   return session::local_streams::ensureStreamsDir();
}

Error runVerifyInstallationSession()
{
   // get current user
   core::system::user::User user;
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
   return core::system::waitForProcessExit(sessionPid);
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

void proxyLocalhostRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // make a copy of the request for forwarding
   http::Request request;
   request.assign(ptrConnection->request());

   // extract the port
   boost::regex re("/p/(\\d+)/");
   boost::smatch match;
   if (!boost::regex_search(request.uri(), match, re))
   {
      ptrConnection->response().setError(http::status::NotFound,
                                         request.uri() + " not found");
      return;
   }
   std::string port = match[1];

   // strip the port part of the uri
   using namespace boost::algorithm;
   std::string portPath = match[0];
   std::string uri = replace_first_copy(request.uri(), portPath, "/");
   request.setUri(uri);

   // remove headers to be a correctly behaving proxy
   request.removeHeader("Keep-Alive");
   request.removeHeader("Proxy-Authenticate");
   request.removeHeader("Proxy-Authorization");
   request.removeHeader("Trailers");
   // spec says we should drop these but we're not sure if that's
   // true for our use case
   //request.removeHeader("TE");
   //request.removeHeader("Transfer-Encoding");

   // specify closing of the connection after the request unless this is
   // an attempt to upgrade to websockets
   if (!boost::algorithm::iequals(request.headerValue("Connection"), "Upgrade"))
      request.setHeader("Connection", "close");

   // create async tcp/ip client and assign request
   boost::shared_ptr<LocalhostAsyncClient> pClient(
      new LocalhostAsyncClient(ptrConnection->ioService(), "localhost", port));
   pClient->request().assign(request);

   // execute request
   pClient->execute(
         boost::bind(handleLocalhostResponse, ptrConnection, pClient, port, _1),
         boost::bind(&core::http::AsyncConnection::writeError,
                     ptrConnection, _1));
}

} // namespace session_proxy
} // namespace server


