/*
 * ServerSessionProxyTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <gtest/gtest.h>

#include <limits>

#include <unistd.h>

#include <boost/asio/error.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/make_shared.hpp>

#include <shared_core/system/User.hpp>

#include <core/http/AsyncConnection.hpp>
#include <core/http/LocalStreamAsyncClient.hpp>
#include <core/http/Request.hpp>
#include <core/json/JsonRpc.hpp>

#include <server/session/ServerSessionManager.hpp>
#include <server/session/ServerSessionProxy.hpp>

using namespace rstudio::core;
using namespace rstudio::server;

namespace {

// a connection that goes nowhere: the error handlers write their response to
// it, but the tests only look at what they reported to the session manager
class NullAsyncConnection : public http::AsyncConnection
{
public:
   NullAsyncConnection()
      : strand_(ioc_)
   {
   }

   boost::asio::io_context& ioContext() override { return ioc_; }
   const http::Request& request() const override { return request_; }
   http::Response& response() override { return response_; }

   void writeResponse(bool, Socket::Handler) override {}
   void writeResponse(const http::Response&, bool, const http::Headers&, Socket::Handler) override {}
   void writeResponseHeaders(Socket::Handler) override {}
   void writeResponseHeaders(const http::Response&, Socket::Handler) override {}
   void writeError(const Error&) override {}
   void close() override {}
   void continueParsing() override {}

   void setData(const boost::any& data) override { data_ = data; }
   boost::any getData() override { return data_; }
   const std::string& username() const override { return username_; }
   void setUsername(const std::string& username) override { username_ = username; }
   const std::string& handlerPrefix() const override { return handlerPrefix_; }
   void setHandlerPrefix(const std::string& prefix) override { handlerPrefix_ = prefix; }
   boost::asio::io_context::strand& getStrand() override { return strand_; }

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}
   void asyncWrite(const boost::asio::const_buffer&, Socket::Handler) override {}
   void asyncWrite(const std::vector<boost::asio::const_buffer>&, Socket::Handler) override {}

private:
   boost::asio::io_context ioc_;
   boost::asio::io_context::strand strand_;
   http::Request request_;
   http::Response response_;
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
};

Error noopLaunchFunction(boost::asio::io_context&,
                         const r_util::SessionLaunchProfile&,
                         const json::JsonRpcRequest&,
                         const http::Request&,
                         const http::ResponseHandler&,
                         const http::ErrorHandler&)
{
   return Success();
}

// false when a launch is already pending for the context
bool attemptLaunch(const r_util::SessionContext& context)
{
   boost::asio::io_context ioContext;
   json::JsonRpcRequest jsonRequest;
   http::Request request;
   bool launched = false;

   Error error = sessionManager().launchSession(
      ioContext, context, jsonRequest, request, launched, system::Options());
   EXPECT_FALSE(error);

   return launched;
}

// what a request ends with when the process it reached hangs up, as
// LocalStreamAsyncClient reports it
Error hangUpError(PidType peerPid)
{
   Error error(boost::asio::error::make_error_code(boost::asio::error::eof), ERROR_LOCATION);
   error.addProperty(http::kLocalStreamPeerPidProperty, peerPid);
   return error;
}

typedef void (*ProxyErrorHandler)(boost::shared_ptr<http::AsyncConnection>,
                                  const r_util::SessionContext&,
                                  const Error&);

// the launched pid in each half is chosen so that the liveness fallback, which
// is what applies to an error carrying no peer pid, would decide the opposite
void expectErrorAttributedToPeer(ProxyErrorHandler handleError, const std::string& username)
{
   sessionManager().setSessionLaunchFunction(noopLaunchFunction);
   boost::shared_ptr<http::AsyncConnection> pConnection = boost::make_shared<NullAsyncConnection>();
   r_util::SessionContext context(username);

   // an error from another process keeps the launch pending, though the
   // launched process isn't running
   ASSERT_TRUE(attemptLaunch(context));
   sessionManager().notePendingLaunchPid(context, std::numeric_limits<PidType>::max());

   handleError(pConnection, context, hangUpError(::getpid()));
   EXPECT_FALSE(attemptLaunch(context));
   sessionManager().removePendingLaunch(context);

   // an error from the launched process ends the launch, though that
   // process is running
   ASSERT_TRUE(attemptLaunch(context));
   sessionManager().notePendingLaunchPid(context, ::getpid());

   handleError(pConnection, context, hangUpError(::getpid()));
   EXPECT_TRUE(attemptLaunch(context));
   sessionManager().removePendingLaunch(context);
}

} // anonymous namespace

// proxyLocalhostRequest() (in ServerSessionProxy.cpp) enforces that a
// localhost-proxy request (/p/ and /p6/) may only reach a destination port
// owned by the requesting user (rstudio-pro#11470). It does this by resolving
// the caller's uid and calling setExpectedPeerUid() on the
// LocalhostAsyncClient, failing closed (setNotFoundError) on *any* resolution
// error -- because unlike other uid checks in this file, the destination port
// here is attacker-controlled (decoded from a client-supplied token), so the
// uid check is the sole access-control boundary on this path.
//
// The resolution helper (userIdForUsername) has internal linkage (anonymous
// namespace) and the surrounding function needs a live HTTP connection/request
// to drive end-to-end, so it isn't practical to unit test the full function
// here. These tests instead exercise the resolve-success / resolve-failure
// decision inputs directly, via the userIdForUsernameForTest() passthrough
// hook, asserting the same success/failure outcomes that
// proxyLocalhostRequest() branches on:
//   - resolution succeeds -> proceeds to setExpectedPeerUid(uid)
//   - resolution fails (for *any* reason)  -> fails closed / rejects the request
//
// This complements (does not replace) the existing rserver-tests /
// rstudio-server-core-tests regression suites and e2e coverage, which
// exercise this code path indirectly but don't specifically assert the
// ownership-enforcement decision.
TEST(ProxyLocalhostUidResolutionTests, ResolvesKnownUserToExpectedUid)
{
   system::User currentUser;
   Error userError = system::User::getCurrentUser(currentUser);
   ASSERT_FALSE(userError) << "Could not determine current user for test setup: "
                            << userError.asString();

   UidType uid = static_cast<UidType>(-1);
   Error error = session_proxy::userIdForUsernameForTest(currentUser.getUsername(), &uid);

   // resolution succeeded => proxyLocalhostRequest() would proceed to call
   // setExpectedPeerUid(uid) and allow the proxy to continue.
   EXPECT_FALSE(error) << error.asString();
   EXPECT_EQ(uid, currentUser.getUserId());
}

TEST(ProxyLocalhostUidResolutionTests, FailsClosedForUnknownUser)
{
   // A username that should never exist on any system running this test.
   const std::string unknownUser = "rstudio-pro-11470-nonexistent-user";

   UidType uid = static_cast<UidType>(-1);
   Error error = session_proxy::userIdForUsernameForTest(unknownUser, &uid);

   // resolution failed (for any reason) => proxyLocalhostRequest() must fail
   // closed and reject the request (setNotFoundError) rather than proceed
   // without an ownership check.
   EXPECT_TRUE(error);
}

TEST(ProxyLocalhostResponseTests, NormalizesChunkedRedirectBeforeRewritingHeaders)
{
   http::Request request;
   request.setUri("/p/port-token/source");

   http::Response response;
   response.setStatusCode(http::status::MovedTemporarily);
   response.setHeader("Location", "/location-target");
   response.setHeader("Refresh", "/refresh-target");
   response.setHeader("Transfer-Encoding", "Chunked");
   response.setBody("decoded redirect body");

   http::Response preparedResponse;
   session_proxy::prepareLocalhostResponseForTest(
      request,
      "port-token",
      "localhost",
      false,
      response,
      &preparedResponse);

   EXPECT_TRUE(preparedResponse.headerValue("Transfer-Encoding").empty());
   EXPECT_EQ(preparedResponse.headerValue("Content-Length"),
             std::to_string(response.body().size()));
   EXPECT_EQ(preparedResponse.headerValue("Location"),
             "/p/port-token/location-target");
   EXPECT_EQ(preparedResponse.headerValue("Refresh"),
             "/p/port-token/refresh-target");
   EXPECT_EQ(preparedResponse.body(), response.body());
}

TEST(ProxyLocalhostResponseTests, NormalizesChunkedSparkUiBeforeRewritingBody)
{
   http::Request request;
   request.setUri("/p/port-token/jobs/job-id");

   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.setHeader("Server", "Jetty(9.4.57)");
   response.setHeader("Transfer-Encoding", "chunked");
   response.setBody(
      "<div class=\"navbar navbar-static-top\">"
      "<a href=\"/jobs\">Jobs</a>"
      "<script src=\"/static/app.js\"></script>"
      "<img src=\"/static/spark-logo-77x50px-hd.png\">"
      "</div>");

   http::Response preparedResponse;
   session_proxy::prepareLocalhostResponseForTest(
      request,
      "port-token",
      "localhost",
      false,
      response,
      &preparedResponse);

   EXPECT_TRUE(preparedResponse.headerValue("Transfer-Encoding").empty());
   EXPECT_EQ(preparedResponse.headerValue("Content-Length"),
             std::to_string(preparedResponse.body().size()));
   EXPECT_NE(preparedResponse.body().find("href=\"../jobs\""), std::string::npos);
   EXPECT_NE(preparedResponse.body().find("<script src=\"../static/app.js\""),
             std::string::npos);
   EXPECT_NE(preparedResponse.body().find("<img src=\"../static/spark-logo"),
             std::string::npos);
}

// The proxy reports each request's outcome to the session manager along with
// the process that produced it (#18963). The client stamps that pid on the
// errors it raises (see LocalStreamAsyncClientTests.cpp) and the session
// manager decides what it means (see ServerSessionManagerTests.cpp); these
// cover the step between, where the error handlers read it off the error.
// Losing it there would go unnoticed otherwise, since the outcome would then
// be handled as one that couldn't be attributed.
TEST(ProxyPendingLaunchTests, RpcErrorIsAttributedToItsPeerProcess)
{
   expectErrorAttributedToPeer(session_proxy::handleRpcErrorForTest,
                               "proxy-rpc-error-peer-pid-user");
}

TEST(ProxyPendingLaunchTests, ContentErrorIsAttributedToItsPeerProcess)
{
   expectErrorAttributedToPeer(session_proxy::handleContentErrorForTest,
                               "proxy-content-error-peer-pid-user");
}
