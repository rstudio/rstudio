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

#include <shared_core/system/User.hpp>
#include <core/http/HeaderCookieConstants.hpp>

#include <server/session/ServerSessionProxy.hpp>

using namespace rstudio::core;
using namespace rstudio::server;

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

// shouldBufferLocalhostResponse() (ServerSessionProxy.cpp) is the /p/ path's
// named buffering policy, handed to setBufferPredicate() in
// proxyLocalhostRequest(). It ORs the header-observable always-buffer cases
// (websocket upgrade, redirect, SparkUI/Jetty) with the shared size gate
// (isBelowStreamingThreshold, FixedBufferProxy.hpp) added by this step. These
// tests exercise the policy directly via the shouldBufferLocalhostResponseForTest()
// passthrough hook.
TEST(BufferingPolicyTests, AlwaysBuffersSwitchingProtocolsRegardlessOfSize)
{
   http::Response response;
   response.setStatusCode(http::status::SwitchingProtocols);

   EXPECT_TRUE(session_proxy::shouldBufferLocalhostResponseForTest(response));
}

TEST(BufferingPolicyTests, AlwaysBuffersLocationRedirectRegardlessOfSize)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.setHeader("Location", "/redirect-target");
   response.setContentLength(8388608);

   EXPECT_TRUE(session_proxy::shouldBufferLocalhostResponseForTest(response));
}

TEST(BufferingPolicyTests, AlwaysBuffersRefreshRegardlessOfSize)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.setHeader("Refresh", "5; url=/refresh-target");
   response.setContentLength(8388608);

   EXPECT_TRUE(session_proxy::shouldBufferLocalhostResponseForTest(response));
}

TEST(BufferingPolicyTests, AlwaysBuffersJettyServerRegardlessOfSize)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.setHeader("Server", "Jetty(9.4.57)");
   response.setContentLength(8388608);

   EXPECT_TRUE(session_proxy::shouldBufferLocalhostResponseForTest(response));
}

TEST(BufferingPolicyTests, StreamsLargeResponseWithNoOtherBufferCondition)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.setContentLength(8388608);

   EXPECT_FALSE(session_proxy::shouldBufferLocalhostResponseForTest(response));
}

TEST(BufferingPolicyTests, BuffersSmallResponseOnSizeGate)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.setContentLength(512);

   EXPECT_TRUE(session_proxy::shouldBufferLocalhostResponseForTest(response));
}

TEST(BufferingPolicyTests, StreamsResponseWithNoContentLength)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);

   EXPECT_FALSE(session_proxy::shouldBufferLocalhostResponseForTest(response));
}

// The local-stream /s/ proxy path's named buffering policy
// (shouldBufferLocalStreamResponse, ServerSessionProxy.cpp), handed to
// setBufferPredicate() in proxyRequest. proxyRequest is the delivery path for
// every plain (non-launcher) RStudio session once the request reaches the
// node the session lives on. Unlike /p/, launcher, or the load balancer, /s/
// has no header-observable always-buffer condition of its own -- only the
// shared size gate (isBelowStreamingThreshold, FixedBufferProxy.hpp). These
// tests exercise the policy directly via the
// shouldBufferLocalStreamResponseForTest() passthrough hook.
TEST(BufferingPolicyTests, ShouldBufferLocalStreamResponseStreamsLargeResponse)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.setContentLength(8388608);

   EXPECT_FALSE(session_proxy::shouldBufferLocalStreamResponseForTest(response));
}

TEST(BufferingPolicyTests, ShouldBufferLocalStreamResponseBuffersSmallResponse)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.setContentLength(512);

   EXPECT_TRUE(session_proxy::shouldBufferLocalStreamResponseForTest(response));
}

TEST(BufferingPolicyTests, ShouldBufferLocalStreamResponseStreamsResponseWithNoContentLength)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);

   EXPECT_FALSE(session_proxy::shouldBufferLocalStreamResponseForTest(response));
}

TEST(BufferingPolicyTests, ShouldBufferLocalStreamResponseDoesNotAlwaysBufferSwitchingProtocols)
{
   // Unlike /p/, /s/ never upgrades to a websocket, so a 101 here is not a
   // condition this policy needs to special-case -- it falls through to the
   // size gate like any other status.
   http::Response response;
   response.setStatusCode(http::status::SwitchingProtocols);
   response.setContentLength(8388608);

   EXPECT_FALSE(session_proxy::shouldBufferLocalStreamResponseForTest(response));
}

// Cookie-parity regression test: proxyRequest's streamed /s/ path passes
// getAuthCookies(ptrConnection->response()) as FixedBufferProxy::proxy()'s
// preservedCookiesOverride, so it must land exactly the auth-cookie whitelist
// getAuthCookies() computes -- not a blind copy of every Set-Cookie already on
// that response. handleProxyResponse, the buffered-path completion handler,
// already calls
// writeResponse(response, true, getAuthCookies(ptrConnection->response())),
// so the streamed path must compute the identical filtered set or the two
// delivery strategies would diverge on which cookies survive.
TEST(BufferingPolicyTests, GetAuthCookiesFiltersToWhitelistNotBlindCopyForLocalStreamCookieParity)
{
   http::Response response;
   response.setStatusCode(http::status::Ok);
   response.addHeader("Set-Cookie", std::string(kUserIdCookie) + "=refreshed-value");
   response.addHeader("Set-Cookie", "some-other-cookie=should-not-be-carried-over");

   http::Headers authCookies = session_proxy::getAuthCookies(response);

   bool foundAuthCookie = false;
   bool foundNonAuthCookie = false;
   for (const http::Header& header : authCookies)
   {
      if (header.value.find(std::string(kUserIdCookie) + "=refreshed-value") != std::string::npos)
         foundAuthCookie = true;
      if (header.value.find("some-other-cookie") != std::string::npos)
         foundNonAuthCookie = true;
   }
   EXPECT_TRUE(foundAuthCookie);
   EXPECT_FALSE(foundNonAuthCookie);
}
