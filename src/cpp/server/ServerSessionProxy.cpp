/*
 * ServerSessionProxy.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <boost/foreach.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <boost/thread/thread_time.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <core/BoostErrors.hpp>
#include <core/Log.hpp>
#include <core/Thread.hpp>
#include <core/WaitUtils.hpp>
#include <core/RegexUtils.hpp>

#include <core/http/CSRFToken.hpp>
#include <core/http/SocketUtils.hpp>
#include <core/http/SocketProxy.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/LocalStreamAsyncClient.hpp>
#include <core/http/Util.hpp>
#include <core/http/URL.hpp>
#include <core/http/ChunkProxy.hpp>
#include <core/http/FormProxy.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/PosixGroup.hpp>
#include <core/system/PosixUser.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <core/json/JsonRpc.hpp>

#include <server_core/http/HeaderConstants.hpp>
#include <server_core/http/LocalhostAsyncClient.hpp>
#include <server_core/sessions/SessionLocalStreams.hpp>
#include <server_core/UrlPorts.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionInvalidScope.hpp>

#include <server/auth/ServerValidateUser.hpp>
#include <server/auth/ServerAuthHandler.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerErrorCategory.hpp>

#include <server/ServerSessionManager.hpp>

#include <server/ServerConstants.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace session_proxy {

// forward declare overlay methods
namespace overlay {

bool proxyRequest(int requestType,
                  const boost::shared_ptr<http::Request>& pRequest,
                  const r_util::SessionContext& context,
                  boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
                  const http::ErrorHandler& errorHandler,
                  const ClientHandler& clientHandler);

void proxyJupyterRequest(const r_util::SessionContext& context,
                         boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
                         const http::ErrorHandler& errorHandler);

void proxyVSCodeRequest(const r_util::SessionContext& context,
                        boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
                        const http::ErrorHandler& errorHandler);

bool proxyLocalhostRequest(http::Request& request,
                           const std::string& port,
                           const r_util::SessionContext& context,
                           boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
                           const LocalhostResponseHandler& responseHandler,
                           const http::ErrorHandler& errorHandler);

Error runVerifyInstallationSession(core::system::User& user,
                                   bool* pHandled);

} // namespace overlay
   
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
   Error error = server_core::sessions::local_streams::ensureStreamsDir();
   if (error)
      LOG_ERROR(error);

   // attempt to launch the session only if this is the first recovery attempt
   if (firstAttempt)
      return sessionManager().launchSession(ptrConnection->ioService(), 
            context, request);
   else
      return Success();
}

http::ConnectionRetryProfile sessionRetryProfile(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const r_util::SessionContext& context)
{
   server::Options& options = server::options();
   http::ConnectionRetryProfile retryProfile;
   retryProfile.retryInterval = boost::posix_time::milliseconds(25);
   retryProfile.maxWait = boost::posix_time::seconds(options.rsessionProxyMaxWaitSeconds());
   retryProfile.recoveryFunction = boost::bind(launchSessionRecovery,
                                               ptrConnection, _1, _2, context);
   return retryProfile;
}

ProxyFilter s_proxyFilter;

ProxyRequestFilter s_proxyRequestFilter;

void invokeRequestFilter(http::Request* pRequest)
{
   if (s_proxyRequestFilter)
      s_proxyRequestFilter(pRequest);
}

bool applyProxyFilter(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const r_util::SessionContext& context,
      const ClientHandler& clientHandler = ClientHandler())
{
   if (s_proxyFilter)
      return s_proxyFilter(ptrConnection, context, clientHandler);
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

   // ensure authorization cookies that were automatically refreshed as part of this
   // request are stamped on the response
   ptrConnection->writeResponse(response, true, getAuthCookies(ptrConnection->response()));
}

void rewriteLocalhostAddressHeader(const std::string& headerName,
                                   const http::Request& originalRequest,
                                   const std::string& port,
                                   const std::string& baseAddress,
                                   bool ipv6,
                                   http::Response* pResponse)
{
   // represents the port identifier in the URL
   std::string portId(port);

   auto portNum = safe_convert::stringTo<int>(portId);
   if (portNum)
   {
      // for numeric ports, use the port token to translate them to opaque identifiers
      std::string portToken = originalRequest.cookieValue(kPortTokenCookie);
      if (portToken.empty())
      {
         // we'll try the default token if no token was supplied on the request
         portToken = kDefaultPortToken;
      }

      portId = server_core::transformPort(portToken, *portNum);
   }

   // get the address and the proxied address
   std::string address = pResponse->headerValue(headerName);
   std::string proxiedAddress = "http://" + baseAddress + ":" + portId;
   std::string portPath = ipv6 ? ("/p6/" + portId) : ("/p/" + portId);

   // relative address, just prepend port
   if (boost::algorithm::starts_with(address, "/"))
   {
      address = portPath + address;
   }
   // proxied address, substitute base url
   else if (boost::algorithm::starts_with(address, proxiedAddress))
   {
      // find the base url from the original request
      std::string baseUri = originalRequest.baseUri();
      std::string::size_type pos = baseUri.find(portPath);
      if (pos != std::string::npos) // precaution, should always be true
      {
          // substitute the base url for the proxied address
         std::string baseUrl = baseUri.substr(0, pos + portPath.length());
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
      boost::shared_ptr<http::IAsyncClient> ptrLocalhost,
      const std::string& port,
      const std::string& baseAddress,
      bool ipv6,
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
                                          baseAddress,
                                          ipv6,
                                          &redirectResponse);
         }

         // handle Refresh
         if (!refresh.empty())
         {
            rewriteLocalhostAddressHeader(kRefresh,
                                          ptrConnection->request(),
                                          port,
                                          baseAddress,
                                          ipv6,
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

bool handleLicenseError(
      boost::shared_ptr<http::AsyncConnection> ptrConnection,
      const Error& error)
{
   return false;
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
      return;
   }

   if (handleLicenseError(ptrConnection, error))
   {
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
      // if regular content is somehow unauthorized, we should redirect
      // the user to sign in fully once again - however, if this is a session scope
      // workspaces request, then this was caused by manipulation of routing
      // via ServerMultiSession, and to properly route back to the session we will need
      // to redirect to the root of the application
      if (ptrConnection->request().uri().find("/workspaces/default_session_scope") != std::string::npos)
      {
         const_cast<http::Request&>(ptrConnection->request()).setUri("/");

         // for calls to default_session_scope, we want to prevent
         // ServerMultiSession from  transforming the redirect which would
         // cause the browser to inadvertently load that URL
         ptrConnection->response().setHeader(kRStudioNoTransformRedirect, "1");
      }

      auth::handler::signInThenContinue(ptrConnection->request(), &ptrConnection->response());
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
   else if (handleLicenseError(ptrConnection, error))
   {
      ptrConnection->writeResponse();
   }
   else
   {
      // otherwise just forward the error
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
      json::setJsonRpcError(Error(json::errc::Unauthorized, ERROR_LOCATION),
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
      json::JsonRpcResponse jsonRpcResponse;
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
      json::setJsonRpcError(Error(json::errc::ConnectionError, ERROR_LOCATION),
                            &(ptrConnection->response()));
   }
   else if (!handleLicenseError(ptrConnection, error))
   {
      json::setJsonRpcError(Error(json::errc::TransmissionError, ERROR_LOCATION),
                           &(ptrConnection->response()));
   }

   // write the response
   ptrConnection->writeResponse();
}

void handleEventsError(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const r_util::SessionContext& context,
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
         json::setJsonRpcError(Error(json::errc::Unavailable, ERROR_LOCATION),
                              &(ptrConnection->response()));
      }
   }
   else if (server::isInvalidSessionScopeError(error))
   {
      json::setJsonRpcError(Error(json::errc::Unavailable, ERROR_LOCATION),
                           &(ptrConnection->response()));
   }
   else if (!handleLicenseError(ptrConnection, error))
   {
      // log if not connection terminated
      logIfNotConnectionTerminated(error, ptrConnection->request());

      json::setJsonRpcError(Error(json::errc::TransmissionError, ERROR_LOCATION),
                           &(ptrConnection->response()));
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
      core::system::User user;
      Error error = core::system::User::getUserFromIdentifier(username, user);
      if (error)
         return error;

      *pUID = user.getUserId();
      cache.set(username, *pUID);
   }

   return Success();
}

void proxyRequest(
      int requestType,
      const r_util::SessionContext& context,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const http::ErrorHandler& errorHandler,
      const http::ConnectionRetryProfile& connectionRetryProfile,
      const ClientHandler& clientHandler = ClientHandler())
{
   // apply optional proxy filter
   if (applyProxyFilter(ptrConnection, context, clientHandler))
      return;

   // modify request
   boost::shared_ptr<http::Request> pRequest(new http::Request());
   pRequest->assign(ptrConnection->request());

   // add username
   pRequest->setHeader(kRStudioUserIdentityDisplay, context.username);

   // call request filter if we have one
   invokeRequestFilter(pRequest.get());

   // see if the request should be handled by the overlay
   if (overlay::proxyRequest(requestType, pRequest, context, ptrConnection, errorHandler, clientHandler))
   {
      // request handled by the overlay
      return;
   }

   // determine path to user stream
   std::string streamFile = r_util::sessionContextFile(context);
   FilePath streamPath = server_core::sessions::local_streams::streamPath(streamFile);

   // determine the uid for the username (for validation)
   UidType uid;
   boost::optional<UidType> validateUid;
   Error error = userIdForUsername(context.username, &uid);
   if (!error)
   {
      // if the user exists on the system, do uid validation
      validateUid = uid;
   }
   else
   {
      if (error != systemError(boost::system::errc::permission_denied, ErrorLocation()))
      {
         // if the error returned was permission_denied then no user was found
         // we consider user not found to be an acceptable error as it should
         // be created later by PAM profiles
         //
         // other errors indicate potential issues enumerating the passwd file
         // so reject access since we cannot verify the identity of the user
         Error permissionError(boost::system::error_code(
                                  boost::system::errc::permission_denied,
                                  boost::system::system_category()),
                               error,
                               ERROR_LOCATION);
         errorHandler(permissionError);
         return;
      }
   }

   // create client
   // if the user is available on the system pass in the uid for validation to ensure
   // that we only connect to the socket if it was created by the user
   boost::shared_ptr<http::IAsyncClient> pClient(new http::LocalStreamAsyncClient(
                                                    ptrConnection->ioService(),
                                                    streamPath, false, validateUid));

   // setup retry context
   if (!connectionRetryProfile.empty())
      pClient->setConnectionRetryProfile(connectionRetryProfile);

   // assign request
   pClient->request().assign(*pRequest);

   // proxy the request
   boost::shared_ptr<http::ChunkProxy> chunkProxy(new http::ChunkProxy(ptrConnection));
   chunkProxy->proxy(pClient);
   pClient->execute(boost::bind(handleProxyResponse, ptrConnection, context, _1),
                    errorHandler);

   if (clientHandler)
   {
      // invoke the client handler on the threadpool - we cannot do this
      // from this thread because that will cause ordering issues for the caller
      ptrConnection->ioService().post(boost::bind(clientHandler, pClient));
   }
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
       json::setJsonRpcError(Error(json::errc::Unauthorized, ERROR_LOCATION),
                             &(ptrConnection->response()));
       ptrConnection->writeResponse();
       return false;
   }
}

bool shouldRefreshCredentials(const http::Request& request)
{
   // determines whether or not credentials should automatically
   // be refreshed for the given RPC URI - in most cases, an RPC
   // is the direct result of a user action so credentials should
   // be refreshed, but in some cases RPCs are fired automatically
   // on a timer, and thus should not renew credentials
   //
   // we will only not refresh credentials if explicitly told not to
   return (request.headerValue(kRStudioRpcRefreshAuthCreds) != "0");
}

} // anonymous namespace

http::Headers getAuthCookies(const http::Response& response)
{
   http::Headers authCookies;
   for (const http::Header& cookie : response.getCookies({ 
      kCSRFTokenCookie,
      kUserIdCookie,
      kUserListCookie,
      kPersistAuthCookie }))
   {
      authCookies.push_back(cookie);
   }
   return authCookies;
}

Error initialize()
{ 
   return server_core::sessions::local_streams::ensureStreamsDir();
}

Error runVerifyInstallationSession()
{
   // get current user
   core::system::User user;
   Error error = core::system::User::getCurrentUser(user);
   if (error)
      return error;

   bool handled = false;
   error = overlay::runVerifyInstallationSession(user, &handled);
   if (error)
      return error;

   if (!handled)
   {
      // launch verify installation session
      core::system::Options args;
      args.push_back(core::system::Option("--" kVerifyInstallationSessionOption, "1"));
      PidType sessionPid;
      error = server::launchSession(r_util::SessionContext(user.getUsername()),
                                    args,
                                    &sessionPid);
      if (error)
         return error;

      // wait for exit
      return core::system::waitForProcessExit(sessionPid);
   }

   return Success();
}

void proxyContentRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
      return;

   proxyRequest(RequestType::Content,
                context,
                ptrConnection,
                boost::bind(handleContentError, ptrConnection, context, _1),
                sessionRetryProfile(ptrConnection, context));
}

bool proxyUploadRequest(
      const std::string& username,
      const std::string& userIdentifier,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const std::string& formData,
      bool complete)
{
   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
   {
      ptrConnection->close();
      return true;
   }

   // see if we have a form proxy for this request already
   boost::shared_ptr<http::FormProxy> proxy;
   boost::any connectionData = ptrConnection->getData();
   if (!connectionData.empty())
   {
      try
      {
         proxy = boost::any_cast<boost::shared_ptr<http::FormProxy>>(connectionData);
      }
      catch (boost::bad_any_cast&)
      {
      }
   }

   if (!proxy)
   {
      // no proxy yet - we need to proxy this request and create
      // a form proxy to write form data to
      auto onClientCreated = [=](const boost::shared_ptr<http::IAsyncClient>& proxyClient)
      {
         boost::shared_ptr<http::FormProxy> proxy =
               boost::make_shared<http::FormProxy>(ptrConnection, proxyClient);

         proxy->initialize();
         ptrConnection->setData(proxy);

         // continue handling form data
         ptrConnection->continueParsing();
      };

      proxyRequest(RequestType::Content,
                   context,
                   ptrConnection,
                   boost::bind(handleContentError, ptrConnection, context, _1),
                   sessionRetryProfile(ptrConnection, context),
                   onClientCreated);

      // because the client creation is asynchronous, we need to stop handling form pieces
      // until we actually have a client to write the data to
      return false;
   }
   else
   {
      return proxy->queueData(formData);
   }
}

void proxyRpcRequest(
      const std::string& username,
      const std::string& userIdentifier,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // validate the user if this is client_init
   bool isClientInit = boost::algorithm::ends_with(ptrConnection->request().uri(),
                                                   "client_init");
   if (isClientInit)
   {
      if (!validateUser(ptrConnection, username))
         return;
   }

   // refresh auth credentials automatically if this RPC is the result of a user action
   bool refreshCredentials = shouldRefreshCredentials(ptrConnection->request());
   if (refreshCredentials)
   {
      auth::handler::refreshAuthCookies(userIdentifier,
                                        ptrConnection->request(),
                                        &ptrConnection->response());
   }

   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
      return;

   proxyRequest(isClientInit ? RequestType::ClientInit : RequestType::Rpc,
                context,
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

   proxyRequest(RequestType::Events,
                context,
                ptrConnection,
                boost::bind(handleEventsError, ptrConnection, context, _1),
                http::ConnectionRetryProfile());
}

void proxyJupyterRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
      return;

   overlay::proxyJupyterRequest(context,
                                ptrConnection,
                                boost::bind(handleContentError, ptrConnection, context, _1));
}

void proxyVSCodeRequest(
      const std::string& username,
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection)
{
   // get session context
   r_util::SessionContext context;
   if (!sessionContextForRequest(ptrConnection, username, &context))
      return;

   overlay::proxyVSCodeRequest(context,
                               ptrConnection,
                               boost::bind(handleContentError, ptrConnection, context, _1));
}

void proxyLocalhostRequest(
      bool ipv6,
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
   invokeRequestFilter(&request);

   // extract the (scrambled) port, which consists of 8 or 9 hex digits 
   // (an additional prefix digit may exist for server routing)
   std::string pMap = ipv6 ? "/p6/" : "/p/";
   boost::regex re(pMap + "([a-fA-F0-9]{8,9})(/|$)");
   boost::smatch match;
   if (!regex_utils::search(request.uri(), match, re))
   {
      ptrConnection->response().setNotFoundError(request);
      return;
   }

   // extract the port token
   std::string portToken = ptrConnection->request().cookieValue(kPortTokenCookie);
   if (portToken.empty())
   {
      // we'll try the default token if no token was supplied on the request
      portToken = kDefaultPortToken;
   }

   // unscramble the port using the token
   bool server = false;
   int portNum = server_core::detransformPort(portToken, match[1], server);
   if (portNum < 0)
   {
      // act as though there's no content here if we can't determine the correct port
      ptrConnection->response().setNotFoundError(request);
      return;
   }

   std::string port = safe_convert::numberToString(portNum);

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

   // we had trouble with sending jetty accept-encoding of gzip
   // (it returns content w/o a Content-Length which foilis our
   // decoding code)
   request.removeHeader("Accept-Encoding");

   // specify closing of the connection after the request unless this is
   // an attempt to upgrade to websockets
   if (!http::util::isWSUpgradeRequest(request))
   {
      request.setHeader("Connection", "close");
   }

   LocalhostResponseHandler onResponse =
         boost::bind(handleLocalhostResponse, ptrConnection, _3, port, _2, ipv6, _1);
   http::ErrorHandler onError = boost::bind(handleLocalhostError, ptrConnection, _1);

   // see if the request should be handled by the overlay (unless it should be handled by the server)
   if (!server && overlay::proxyLocalhostRequest(request, port, context, ptrConnection, onResponse, onError))
   {
      // request handled by the overlay
      return;
   }

   // set the host
   std::string address;
   if (!ipv6)
   {
      address = "localhost";
      request.setHost(address + ":" + port);
   }
   else
   {
      address = "::1";
      request.setHost("[" + address + "]" + ":" + port);
   }

   // create async tcp/ip client and assign request
   boost::shared_ptr<http::IAsyncClient> pClient(
      new server_core::http::LocalhostAsyncClient(ptrConnection->ioService(), address, port));
   pClient->request().assign(request);

   // execute request
   pClient->execute(
            boost::bind(handleLocalhostResponse, ptrConnection, pClient, port, address, ipv6, _1),
            onError);
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


