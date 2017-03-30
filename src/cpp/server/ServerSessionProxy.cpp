/*
 * ServerSessionProxy.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <server/ServerSessionProxy.hpp>

#include <vector>
#include <sstream>
#include <map>

#include <boost/regex.hpp>
#include <boost/algorithm/string/join.hpp>

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
#include <core/RegexUtils.hpp>

#include <core/http/SocketUtils.hpp>
#include <core/http/SocketProxy.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/LocalStreamAsyncClient.hpp>
#include <core/http/TcpIpAsyncClient.hpp>
#include <core/http/Util.hpp>
#include <core/http/URL.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/PosixUser.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <core/json/JsonRpc.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionLocalStreams.hpp>
#include <session/SessionInvalidScope.hpp>

#include <server/auth/ServerValidateUser.hpp>
#include <server/auth/ServerAuthHandler.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerErrorCategory.hpp>
#include <server/ServerSessionManager.hpp>

#include <server/ServerConstants.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace server {
namespace session_proxy {
   
namespace {

Error launchSessionRecovery(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const http::Request& request,
      bool firstAttempt,
      const r_util::SessionContext& context)
{
   // if this request is marked as requiring an existing
   // session then return session unavilable error
   if (requiresSession(request))
      return Error(server::errc::SessionUnavailableError, ERROR_LOCATION);

   // if the session scope is marked as invalid then return
   // invalid session scope error
   core::r_util::SessionScopeState state =
         session::collectInvalidScope(context);
   if (state != core::r_util::ScopeValid)
   {
       Error error(server::errc::InvalidSessionScopeError, ERROR_LOCATION);
       error.addProperty("state", state);
       return error;
   }

   // recreate streams dir if necessary
   Error error = session::local_streams::ensureStreamsDir();
   if (error)
      LOG_ERROR(error);

   // attempt to launch the session only if this is the first recovery attempt
   if (firstAttempt)
      return sessionManager().launchSession(ptrConnection->ioService(), 
            context);
   else
      return Success();
}

http::ConnectionRetryProfile sessionRetryProfile(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const r_util::SessionContext& context)
{
   http::ConnectionRetryProfile retryProfile;
   retryProfile.retryInterval = boost::posix_time::milliseconds(25);
   retryProfile.maxWait = boost::posix_time::seconds(10);
   retryProfile.recoveryFunction = boost::bind(launchSessionRecovery,
                                               ptrConnection, _1, _2, context);
   return retryProfile;
}

ProxyFilter s_proxyFilter;

ProxyRequestFilter s_proxyRequestFilter;

bool applyProxyFilter(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const r_util::SessionContext& context)
{
   if (s_proxyFilter)
      return s_proxyFilter(ptrConnection, context);
   else
      return false;
}

SessionContextSource s_sessionContextSource;

bool sessionContextForRequest(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const std::string& username,
      r_util::SessionContext* pSessionContext)
{
   if (s_sessionContextSource)
   {
      return s_sessionContextSource(ptrConnection, username, pSessionContext);
   }
   else
   {
      *pSessionContext = r_util::SessionContext(username);
      return true;
   }
}

void handleProxyResponse(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const r_util::SessionContext& context,
      const http::Response& response)
{
   // if there was a launch pending then remove it
   sessionManager().removePendingLaunch(context);

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
   // expects a close from the client end of the socket). however, don't
   // do this for Jetty (as it often doesn't send a Content-Length header)
   virtual bool stopReadingAndRespond()
   {
      std::string server = response_.headerValue("Server");
      if (boost::algorithm::contains(server, "Jetty"))
      {
         return false;
      }
      else
      {
         return response_.body().length() >= response_.contentLength();
      }
   }

   // ensure that we don't close the connection when a websockets
   // upgrade is taking place
   virtual bool keepConnectionAlive()
   {
      return response_.statusCode() == http::status::SwitchingProtocols;
   }
};


void rewriteLocalhostAddressHeader(const std::string& headerName,
                                   const http::Request& originalRequest,
                                   const std::string& port,
                                   http::Response* pResponse)
{
   // get the address and the proxied address
   std::string address = pResponse->headerValue(headerName);
   std::string proxiedAddress = "http://localhost:" + port;
   std::string portPath = "/p/" + port;

   // relative address, just prepend port
   if (boost::algorithm::starts_with(address, "/"))
   {
      address = portPath + address;
   }
   // proxied address, substitute base url
   else if (boost::algorithm::starts_with(address, proxiedAddress))
   {
      // find the base url from the original request
      std::string originalUri = originalRequest.absoluteUri();
      std::string::size_type pos = originalUri.find(portPath);
      if (pos != std::string::npos) // precaution, should always be true
      {
          // substitute the base url for the proxied address
         std::string baseUrl = originalUri.substr(0, pos + portPath.length());
         address = baseUrl + address.substr(proxiedAddress.length());
      }
   }

   // replace the header (no-op if both of the above tests fail)
   pResponse->replaceHeader(headerName, address);
}

bool isSparkUIResponse(const http::Response& response)
{
   using namespace boost::algorithm;
   return contains(response.headerValue("Server"), "Jetty") &&
          contains(response.body(), "<img src=\"/static/spark-logo") &&
          contains(response.body(), "<div class=\"navbar navbar-static-top\">");
}

void sendSparkUIResponse(
      const http::Response& response,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   std::string path = ptrConnection->request().path();
   size_t slashes = std::count(path.begin(), path.end(), '/');
   size_t up = slashes >= 4 ? (slashes - 3) : 0;
   std::vector<std::string> dirs;
   for (size_t i=0; i<up; i++)
      dirs.push_back("..");
   std::string prefix = boost::algorithm::join(dirs, "/");

   http::Response fixedResponse;
   fixedResponse.assign(response);
   std::string body = response.body();
   boost::algorithm::replace_all(body,
                              "href=\"/",
                              "href=\"" + prefix + "/");
   boost::algorithm::replace_all(body,
                                 "<script src=\"/",
                                 "<script src=\"" + prefix + "/");
   boost::algorithm::replace_all(body,
                                 "<img src=\"/",
                                 "<img src=\"" + prefix + "/");
   fixedResponse.setBody(body);
   ptrConnection->writeResponse(fixedResponse);
}

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
      const char * const kLocation = "Location";
      const char * const kRefresh = "Refresh";
      std::string location = response.headerValue(kLocation);
      std::string refresh = response.headerValue(kRefresh);
      if (!location.empty() || !refresh.empty())
      {
         // make a copy of the response to rewrite the headers into
         http::Response redirectResponse;
         redirectResponse.assign(response);

         // handle Location
         if (!location.empty())
         {
            rewriteLocalhostAddressHeader(kLocation,
                                          ptrConnection->request(),
                                          port,
                                          &redirectResponse);
         }

         // handle Refresh
         if (!refresh.empty())
         {
            rewriteLocalhostAddressHeader(kRefresh,
                                          ptrConnection->request(),
                                          port,
                                          &redirectResponse);
         }

         // write the copy
         ptrConnection->writeResponse(redirectResponse);
      }
      else
      {
         // fixup bad SparkUI URLs in responses (they use paths hard
         // coded to the root "/" and we are proxying them behind
         // a "/p/<port>/" URL)
         if (isSparkUIResponse(response))
         {         
            sendSparkUIResponse(response, ptrConnection);
         }
         else
         {
            ptrConnection->writeResponse(response);
         }
      }
   }
}

void handleLocalhostError(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const Error& error)
{
   // if this request required a session then return a standard 503
   if (http::isConnectionUnavailableError(error) &&
       requiresSession(ptrConnection->request()))
   {
      http::Response& response = ptrConnection->response();
      response.setStatusCode(http::status::ServiceUnavailable);
      ptrConnection->writeResponse();
   }
   else
   {
      ptrConnection->writeError(error);
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
      const r_util::SessionContext& context,
      const Error& error)
{   
   // if there was a launch pending then remove it
   sessionManager().removePendingLaunch(context);

   // check for authentication error
   if (server::isAuthenticationError(error))
   {
      http::Response& response = ptrConnection->response();
      response.setError(http::status::Unauthorized, "Unauthorized");
      ptrConnection->writeResponse();
      return;
   }

   if (server::isSessionUnavailableError(error) ||
       server::isInvalidSessionScopeError(error))
   {
      http::Response& response = ptrConnection->response();
      if (server::isInvalidSessionScopeError(error))
      {
         unsigned state = safe_convert::stringTo(error.getProperty("state"), 0);
         if (static_cast<r_util::SessionScopeState>(state) ==
             r_util::ScopeMissingProject)
            response.setError(http::status::NotFound, "Project not found");
         else
            response.setStatusCode(http::status::ServiceUnavailable);
      }
      else
      {
         response.setStatusCode(http::status::ServiceUnavailable);
      }
      ptrConnection->writeResponse();
      return;
   }

   // log if not connection terminated
   logIfNotConnectionTerminated(error, ptrConnection->request());

   // handle connection unavailable with sign out if session launches
   // require authentication, otherwise just return service unavailable
   if (http::isConnectionUnavailableError(error))
   {
      // write bad gateway
      http::Response& response = ptrConnection->response();
      response.setStatusCode(http::status::BadGateway);

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
      const r_util::SessionContext& context,
      const Error& error)
{
   // if there was a launch pending then remove it
   sessionManager().removePendingLaunch(context);

   // check for authentication error
   if (server::isAuthenticationError(error))
   {
      json::setJsonRpcError(json::errc::Unauthorized,
                            &(ptrConnection->response()));
      ptrConnection->writeResponse();
      return;
   }

   if (server::isSessionUnavailableError(error))
   {
      http::Response& response = ptrConnection->response();
      response.setStatusCode(http::status::ServiceUnavailable);
      ptrConnection->writeResponse();
      return;
   }

   if (server::isInvalidSessionScopeError(error))
   {
      // prepare client info
      json::Object clJson;
      clJson["scope_path"] = r_util::urlPathForSessionScope(context.scope);
      clJson["scope_state"] = safe_convert::numberToString(
               error.getProperty("state"));
      clJson["project"] = context.scope.project();
      clJson["id"] = context.scope.id();
      json::JsonRpcResponse jsonRpcResponse ;
      jsonRpcResponse.setError(json::errc::InvalidSession, clJson);
      json::setJsonRpcResponse(jsonRpcResponse, &(ptrConnection->response()));
      ptrConnection->writeResponse();
      return;
   }

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
      // if this request required a session then return a standard 503
      if (requiresSession(ptrConnection->request()))
      {
         http::Response& response = ptrConnection->response();
         response.setStatusCode(http::status::ServiceUnavailable);
      }
      else
      {
         json::setJsonRpcError(json::errc::Unavailable,
                              &(ptrConnection->response()));
      }
   }
   else if (server::isInvalidSessionScopeError(error))
   {
      json::setJsonRpcError(json::errc::Unavailable,
                           &(ptrConnection->response()));
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

Error userIdForUsername(const std::string& username, UidType* pUID)
{
   static core::thread::ThreadsafeMap<std::string, UidType> cache;

   if (cache.contains(username))
   {
      *pUID = cache.get(username);
   }
   else
   {
      core::system::user::User user;
      Error error = core::system::user::userFromUsername(username, &user);
      if (error)
         return error;

      *pUID = user.userId;
      cache.set(username, *pUID);
   }

   return Success();
}

void proxyRequest(
      const r_util::SessionContext& context,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const http::ErrorHandler& errorHandler,
      const http::ConnectionRetryProfile& connectionRetryProfile)
{
   // apply optional proxy filter
   if (applyProxyFilter(ptrConnection, context))
      return;

   // determine path to user stream
   std::string streamFile = r_util::sessionContextFile(context);
   FilePath streamPath = session::local_streams::streamPath(streamFile);

   // determine the uid for the username (for validation)
   UidType uid;
   Error error = userIdForUsername(context.username, &uid);
   if (error)
   {
      Error permissionError(boost::system::error_code(
                               boost::system::errc::permission_denied,
                               boost::system::get_system_category()),
                            error,
                            ERROR_LOCATION);
      errorHandler(permissionError);
      return;
   }

   // create client
   boost::shared_ptr<http::LocalStreamAsyncClient> pClient(
    new http::LocalStreamAsyncClient(ptrConnection->ioService(), streamPath, false, uid));

   // setup retry context
   if (!connectionRetryProfile.empty())
      pClient->setConnectionRetryProfile(connectionRetryProfile);

   // assign request
   pClient->request().assign(ptrConnection->request());

   // call request filter if we have one
   if (s_proxyRequestFilter)
      s_proxyRequestFilter(&(pClient->request()));

   // execute
   pClient->execute(
         boost::bind(handleProxyResponse, ptrConnection, context, _1),
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
   error = server::launchSession(r_util::SessionContext(user.username),
                                 args,
                                 &sessionPid);
   if (error)
      return error;

   // wait for exit
   return core::system::waitForProcessExit(sessionPid);
}

void proxyContentRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
      return;

   proxyRequest(context,
                ptrConnection,
                boost::bind(handleContentError, ptrConnection, context, _1),
                sessionRetryProfile(ptrConnection, context));
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

   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
      return;

   proxyRequest(context,
                ptrConnection,
                boost::bind(handleRpcError, ptrConnection, context, _1),
                sessionRetryProfile(ptrConnection, context));
}
   
void proxyEventsRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // validate the user
   if (!validateUser(ptrConnection, username))
      return;

   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
      return;

   proxyRequest(context,
                ptrConnection,
                boost::bind(handleEventsError, ptrConnection, _1),
                http::ConnectionRetryProfile());
}

void proxyLocalhostRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
      return;

   // apply optional proxy filter
   if (applyProxyFilter(ptrConnection, context))
      return;

   // make a copy of the request for forwarding
   http::Request request;
   request.assign(ptrConnection->request());

   // call request filter if we have one
   if (s_proxyRequestFilter)
      s_proxyRequestFilter(&request);

   // extract the port
   boost::regex re("/p/(\\d+)/");
   boost::smatch match;
   if (!regex_utils::search(request.uri(), match, re))
   {
      ptrConnection->response().setNotFoundError(request.uri());
      return;
   }
   std::string port = match[1];

   // strip the port part of the uri
   using namespace boost::algorithm;
   std::string portPath = match[0];
   std::string uri = replace_first_copy(request.uri(), portPath, "/");
   request.setUri(uri);

   // set the host
   request.setHost("localhost:" + port);

   // remove headers to be a correctly behaving proxy
   request.removeHeader("Keep-Alive");
   request.removeHeader("Proxy-Authenticate");
   request.removeHeader("Proxy-Authorization");
   request.removeHeader("Trailers");
   // spec says we should drop these but we're not sure if that's
   // true for our use case
   //request.removeHeader("TE");
   //request.removeHeader("Transfer-Encoding");

   // we had trouble with sending jetty accept-encoding of gzip
   // (it returns content w/o a Content-Length which foilis our
   // decoding code)
   request.removeHeader("Accept-Encoding");

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
         boost::bind(handleLocalhostError, ptrConnection, _1));
}

bool requiresSession(const http::Request& request)
{
   return !request.headerValue(kRStudioSessionRequiredHeader).empty();
}

void setProxyFilter(ProxyFilter filter)
{
   s_proxyFilter = filter;
}

void setProxyRequestFilter(ProxyRequestFilter filter)
{
   s_proxyRequestFilter = filter;
}

void setSessionContextSource(SessionContextSource source)
{
   s_sessionContextSource = source;
}

} // namespace session_proxy
} // namespace server
} // namespace rstudio


