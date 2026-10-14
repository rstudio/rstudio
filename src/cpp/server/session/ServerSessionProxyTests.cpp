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

#include <functional>
#include <limits>
#include <memory>
#include <string>
#include <vector>

#include <unistd.h>

#include <boost/algorithm/string/join.hpp>
#include <boost/any.hpp>
#include <boost/asio.hpp>
#include <boost/asio/error.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/make_shared.hpp>

#include <shared_core/system/User.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/http/FixedBufferProxy.hpp>
#include <core/http/HeaderCookieConstants.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/LocalStreamAsyncClient.hpp>
#include <core/http/TcpIpAsyncClient.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/tests/GatedResponder.hpp>

#include <core/http/SocketUtils.hpp>

#include <server_core/SocketOwnership.hpp>
#include <server_core/http/LocalhostAsyncClient.hpp>

#include <server/ServerErrorCategory.hpp>
#include <server/session/ServerSessionManager.hpp>
#include <server/session/ServerSessionProxy.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace tests {

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
      ioContext, context, jsonRequest, request, launched, core::system::Options());
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

namespace {

// A minimal AsyncConnection fake for driving handleLocalhostResponse() (via
// its handleLocalhostResponseForTest() passthrough) end-to-end: it just needs
// to hold a request/response pair and record whatever gets handed to
// writeResponse(), since that's the call the /p/ non-upgrade branch uses to
// deliver the proxied response and (after the rstudio-pro auth-cookie-drop
// fix) the refreshed auth cookies alongside it.
class FakeLocalhostConnection : public http::AsyncConnection
{
public:
   boost::asio::io_context& ioContext() override { return ioc_; }
   const http::Request& request() const override { return request_; }
   http::Response& response() override { return response_; }

   void writeResponse(bool close, http::Socket::Handler handler) override
   {
      handler(boost::system::error_code(), 0);
   }

   void writeResponse(const http::Response& response,
                       bool close,
                       const http::Headers& extraHeaders,
                       http::Socket::Handler handler) override
   {
      writtenResponse_.assign(response, extraHeaders);
      wroteResponse_ = true;
      handler(boost::system::error_code(), 0);
   }

   void writeResponseHeaders(http::Socket::Handler handler) override { handler(boost::system::error_code(), 0); }
   void writeResponseHeaders(const http::Response&, http::Socket::Handler handler) override
   {
      handler(boost::system::error_code(), 0);
   }

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
   void asyncReadSome(boost::asio::mutable_buffer, http::Socket::Handler) override {}
   void asyncWrite(const boost::asio::const_buffer&, http::Socket::Handler) override {}
   void asyncWrite(const std::vector<boost::asio::const_buffer>&, http::Socket::Handler) override {}

   http::Request request_;
   http::Response response_;       // staged response -- refreshAuthCookies() writes cookies here
   http::Response writtenResponse_; // what handleLocalhostResponse actually wrote to the client
   bool wroteResponse_ = false;

private:
   boost::asio::io_context ioc_;
   boost::asio::io_context::strand strand_{ioc_};
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
};

using core::tests::Gate;
using core::tests::GatedResponder;

// The ways an upstream can abandon a streamed body once the proxy has started
// writing the response's headers to the browser.
enum class UpstreamFailure
{
   // RST mid-body: the client's next read fails with connection_reset.
   Reset,

   // FIN mid-body, short of the declared Content-Length. The client currently
   // treats this as the end of the body rather than an error (see
   // AsyncClient::handleReadContent()), so the error handler is not reached;
   // it is kept here so that a future change reporting truncation as an error
   // is exercised against the error handlers too.
   Close,

   // Stops sending mid-body until the client's request deadline fires, which
   // it reports as timed_out.
   Stall
};

const UpstreamFailure kUpstreamFailures[] = {
   UpstreamFailure::Reset,
   UpstreamFailure::Close,
   UpstreamFailure::Stall
};

std::string describe(UpstreamFailure failure)
{
   switch (failure)
   {
      case UpstreamFailure::Reset: return "upstream reset mid-body";
      case UpstreamFailure::Close: return "upstream closed mid-body";
      case UpstreamFailure::Stall: return "upstream stalled mid-body";
   }
   return std::string();
}

// What an upstream standing in for rsession (/s/) or a user's app (/p/) sends
// before abandoning the body: a response big enough for the proxy to stream
// (Content-Length over the 1MB threshold) and the first part of its body.
const char* const kInterruptedResponseHead =
   "HTTP/1.1 200 OK\r\n"
   "Content-Type: application/json\r\n"
   "Content-Length: 2097152\r\n"
   "\r\n";

// Abandons the body of the response begun with kInterruptedResponseHead as
// `failure` describes. For a stall, the connection is held open until pReleased
// opens.
GatedResponder::Finish abandonBody(UpstreamFailure failure, std::shared_ptr<Gate> pReleased)
{
   return [failure, pReleased](boost::asio::ip::tcp::socket& socket)
   {
      boost::system::error_code ec;
      switch (failure)
      {
         case UpstreamFailure::Reset:
            // a zero linger timeout makes close() send RST instead of FIN
            socket.set_option(boost::asio::socket_base::linger(true, 0), ec);
            break;

         case UpstreamFailure::Close:
            socket.shutdown(boost::asio::ip::tcp::socket::shutdown_send, ec);
            break;

         case UpstreamFailure::Stall:
            // hold the connection open until the client has given up on it
            pReleased->wait();
            break;
      }
   };
}

// Stands in for the browser's connection on the /s/ and /p/ streaming paths. Like
// AsyncConnectionImpl::claimResponse(), it lets the first write entry point
// claim its single response and refuses later ones without writing. Unlike a
// socket, it holds a streamed response's header write open until the test
// completes it, which makes the window deterministic in which asio would still
// be reading that write's buffers. AsyncConnectionImpl's buffers point straight
// into response_'s strings (see writeResponseHeadersImpl()), so
// headerBytesInFlight_ is what those strings must still hold when the write
// finishes.
class StreamingConnection : public http::AsyncConnection
{
public:
   explicit StreamingConnection(boost::asio::io_context& ioc)
      : ioc_(ioc),
        strand_(ioc)
   {
   }

   boost::asio::io_context& ioContext() override { return ioc_; }
   const http::Request& request() const override { return request_; }
   http::Response& response() override { return response_; }

   void writeResponse(bool close, http::Socket::Handler handler) override
   {
      if (claimResponse(handler))
         completeLater(handler, 0);
   }

   void writeResponse(const http::Response& response,
                       bool close,
                       const http::Headers& extraHeaders,
                       http::Socket::Handler handler) override
   {
      if (!claimResponse(handler))
         return;

      response_.assign(response, extraHeaders);
      completeLater(handler, 0);
   }

   void writeResponseHeaders(http::Socket::Handler handler) override
   {
      if (claimResponse(handler))
         startHeaderWrite(handler);
   }

   void writeResponseHeaders(const http::Response& response, http::Socket::Handler handler) override
   {
      if (!claimResponse(handler))
         return;

      response_.assign(response);
      startHeaderWrite(handler);
   }

   void writeError(const Error& error) override
   {
      if (claimResponse(http::Socket::Handler()))
         response_.setError(error);
   }

   void close() override { closed_ = true; }
   void continueParsing() override {}
   void setData(const boost::any& data) override { data_ = data; }
   boost::any getData() override { return data_; }
   const std::string& username() const override { return username_; }
   void setUsername(const std::string& username) override { username_ = username; }
   const std::string& handlerPrefix() const override { return handlerPrefix_; }
   void setHandlerPrefix(const std::string& prefix) override { handlerPrefix_ = prefix; }
   boost::asio::io_context::strand& getStrand() override { return strand_; }

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, http::Socket::Handler) override {}

   void asyncWrite(const boost::asio::const_buffer& buffer, http::Socket::Handler handler) override
   {
      completeLater(handler, buffer.size());
   }

   void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers, http::Socket::Handler handler) override
   {
      completeLater(handler, boost::asio::buffer_size(buffers));
   }

   bool headerWriteInFlight() const { return !pendingHeaderWrite_.empty(); }

   void completeHeaderWrite(const boost::system::error_code& ec)
   {
      http::Socket::Handler handler;
      handler.swap(pendingHeaderWrite_);
      if (handler)
         boost::asio::post(strand_, boost::bind(handler, ec, std::size_t(0)));
   }

   std::string serializedHeaders() const
   {
      std::string bytes;
      for (const boost::asio::const_buffer& buffer : response_.headerBuffers())
         bytes.append(static_cast<const char*>(buffer.data()), buffer.size());
      return bytes;
   }

   http::Request request_;
   http::Response response_;
   std::string headerBytesInFlight_;
   std::function<void()> onHeaderWriteStarted_;
   int refusedWrites_ = 0;
   bool closed_ = false;

private:
   bool claimResponse(const http::Socket::Handler& handler)
   {
      if (!responseClaimed_)
      {
         responseClaimed_ = true;
         return true;
      }

      ++refusedWrites_;
      if (handler)
      {
         boost::system::error_code ec = boost::asio::error::already_started;
         boost::asio::post(strand_, boost::bind(handler, ec, std::size_t(0)));
      }
      return false;
   }

   void startHeaderWrite(const http::Socket::Handler& handler)
   {
      headerBytesInFlight_ = serializedHeaders();
      pendingHeaderWrite_ = handler;
      if (onHeaderWriteStarted_)
         onHeaderWriteStarted_();
   }

   void completeLater(const http::Socket::Handler& handler, std::size_t bytes)
   {
      if (handler)
         boost::asio::post(strand_, boost::bind(handler, boost::system::error_code(), bytes));
   }

   boost::asio::io_context& ioc_;
   boost::asio::io_context::strand strand_;
   http::Socket::Handler pendingHeaderWrite_;
   bool responseClaimed_ = false;
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
};

typedef boost::function<void(boost::shared_ptr<http::AsyncConnection>, const Error&)> InterruptedStreamErrorHandler;

// Which proxy site's upstream wiring the harness reproduces.
enum class ProxySite
{
   // /s/, as proxyRequest() wires it (with a TCP client standing in for the
   // local stream client; both share AsyncClient's read and error paths)
   LocalStream,

   // /p/, as proxyLocalhostRequest() wires it
   Localhost
};

struct InterruptedStreamOutcome
{
   bool sawUpstreamError = false;
   Error upstreamError;
   bool headerWriteInFlightAtError = false;
   std::string headerBytesInFlight;
   std::string headerBytesAfterError;
   int refusedWrites = 0;
};

// Streams a large response through the same wiring the given proxy site sets
// up, has the upstream abandon the body (as `failure` describes) while the
// response headers are still being written to the browser, and hands any
// resulting error to errorHandler.
InterruptedStreamOutcome streamThenInterruptUpstream(ProxySite site,
                                                     UpstreamFailure failure,
                                                     const InterruptedStreamErrorHandler& errorHandler)
{
   // the upstream abandons the body once the proxy starts writing the
   // response's headers to the browser
   auto pHeaderWriteStarted = std::make_shared<Gate>();
   auto pReleased = std::make_shared<Gate>();
   GatedResponder upstream(kInterruptedResponseHead + std::string(16384, 'x'),
                           pHeaderWriteStarted,
                           abandonBody(failure, pReleased));
   upstream.start();

   boost::asio::io_context ioc;
   auto pConnection = boost::make_shared<StreamingConnection>(ioc);
   pConnection->request_.setMethod("POST");
   pConnection->request_.setUri("/rpc/large_result");
   pConnection->onHeaderWriteStarted_ = [pHeaderWriteStarted]() { pHeaderWriteStarted->open(); };

   const std::string port = std::to_string(upstream.port());
   boost::shared_ptr<http::IAsyncClient> pClient;
   if (site == ProxySite::Localhost)
   {
      auto pLocalhost = boost::make_shared<server_core::http::LocalhostAsyncClient>(ioc, "127.0.0.1", port);

      // run the port-ownership check proxyLocalhostRequest() enables; this
      // process owns the upstream, so the check passes and the request goes out
      pLocalhost->setExpectedPeerUid(::getuid());
      pClient = pLocalhost;
      pClient->setStrand(&pConnection->getStrand());
   }
   else
   {
      pClient = boost::make_shared<http::TcpIpAsyncClient>(ioc, "127.0.0.1", port);
   }

   boost::posix_time::time_duration requestTimeout = boost::posix_time::seconds(5);
   if (failure == UpstreamFailure::Stall)
      requestTimeout = boost::posix_time::milliseconds(500);
   pClient->setRequestTimeout(requestTimeout);
   pClient->request().setMethod("POST");
   pClient->request().setUri("/rpc/large_result");

   auto pProxy = boost::make_shared<http::FixedBufferProxy>(pConnection);
   if (site == ProxySite::Localhost)
   {
      pProxy->proxy(pClient);
      pClient->setBufferPredicate(session_proxy::shouldBufferLocalhostResponseForTest);
   }
   else
   {
      pProxy->proxy(pClient, session_proxy::getAuthCookies(pConnection->response()));
      pClient->setBufferPredicate(session_proxy::shouldBufferLocalStreamResponseForTest);
   }
   pClient->setStreamNonChunkedResponses(true);

   InterruptedStreamOutcome outcome;
   pClient->execute(
      [](const http::Response&) {},
      [&](const Error& error)
      {
         outcome.sawUpstreamError = true;
         outcome.upstreamError = error;
         outcome.headerWriteInFlightAtError = pConnection->headerWriteInFlight();
         errorHandler(pConnection, error);
      });
   ioc.run();
   pReleased->open();

   outcome.headerBytesInFlight = pConnection->headerBytesInFlight_;
   outcome.headerBytesAfterError = pConnection->serializedHeaders();
   outcome.refusedWrites = pConnection->refusedWrites_;

   // finish the held header write so FixedBufferProxy tears itself down
   pConnection->completeHeaderWrite(boost::asio::error::operation_aborted);
   ioc.restart();
   ioc.run();

   return outcome;
}

// handleContentError() and handleLocalhostError() still populate the
// connection's own response() in their special-case branches before calling
// writeResponse(). That is only safe because none of those branches can match
// an error raised after the upstream has started responding -- by which point
// a streaming FixedBufferProxy may own the response and be writing straight
// from its storage. Each check those handlers make ahead of their catch-all
// writeError() branch (which claims the connection before mutating anything)
// is listed here; returns the ones `error` matches.
std::vector<std::string> mutatingBranchesMatching(const Error& error)
{
   std::vector<std::string> matches;
   if (server::isAuthenticationError(error))
      matches.push_back("isAuthenticationError");
   if (server::isSessionUnavailableError(error))
      matches.push_back("isSessionUnavailableError");
   if (server::isInvalidSessionScopeError(error))
      matches.push_back("isInvalidSessionScopeError");
   if (http::isConnectionUnavailableError(error))
      matches.push_back("isConnectionUnavailableError");
   if (!error.getProperty(server_core::socket_utils::kPortOwnershipRejectedProperty).empty())
      matches.push_back(server_core::socket_utils::kPortOwnershipRejectedProperty);

   http::Response scratch;
   if (session_proxy::handleLicenseErrorForTest(error, &scratch))
      matches.push_back("handleLicenseError");

   return matches;
}

void expectReachesNoMutatingBranch(const Error& error)
{
   std::vector<std::string> matches = mutatingBranchesMatching(error);
   EXPECT_TRUE(matches.empty())
      << "a mid-body error would reach an error-handler branch that mutates the "
      << "connection's response in place (" << boost::algorithm::join(matches, ", ")
      << "): " << error.asString();
}

// Interrupts a streamed response every way an upstream can, and checks that
// errorHandler leaves the response whose headers are in flight alone.
void expectInterruptionsLeaveInFlightHeadersAlone(ProxySite site,
                                                  const InterruptedStreamErrorHandler& errorHandler)
{
   for (UpstreamFailure failure : kUpstreamFailures)
   {
      SCOPED_TRACE(describe(failure));
      InterruptedStreamOutcome outcome = streamThenInterruptUpstream(site, failure, errorHandler);

      EXPECT_FALSE(outcome.headerBytesInFlight.empty())
         << "the response never started streaming, so nothing was exercised";
      EXPECT_EQ(outcome.headerBytesInFlight, outcome.headerBytesAfterError)
         << "the error handler rewrote the response whose headers were still being written";

      // see UpstreamFailure::Close
      if (failure == UpstreamFailure::Close && !outcome.sawUpstreamError)
         continue;

      EXPECT_TRUE(outcome.sawUpstreamError);
      if (!outcome.sawUpstreamError)
         continue;

      EXPECT_TRUE(outcome.headerWriteInFlightAtError);
      EXPECT_EQ(1, outcome.refusedWrites);
      expectReachesNoMutatingBranch(outcome.upstreamError);
   }
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

// Regression test for the /p/ localhost-proxy auth-cookie drop: the
// non-websocket-upgrade branch of handleLocalhostResponse() used to call
// ptrConnection->writeResponse(preparedResponse) with no extraHeaders, which
// clobbers whatever refreshAuthCookies() had staged on the connection's own
// response() -- unlike handleProxyResponse (the /s/ buffered path) and the
// /s/ streamed path, which both pass getAuthCookies(ptrConnection->response())
// through. This drives handleLocalhostResponse's normal branch end-to-end via
// the handleLocalhostResponseForTest() passthrough and asserts the refreshed
// auth cookie lands on what actually gets written to the client, alongside
// the upstream (rsession) response's own headers.
TEST(ProxyLocalhostResponseTests, PreservesRefreshedAuthCookiesOnNormalResponse)
{
   FakeLocalhostConnection connection;
   connection.request_.setUri("/p/port-token/source");

   // Simulate refreshAuthCookies() having already staged a refreshed auth
   // cookie on the connection's response before the proxy call ran.
   connection.response_.addHeader("Set-Cookie", std::string(kUserIdCookie) + "=refreshed-value");

   // The response coming back from the localhost-proxied (rsession) process.
   http::Response upstreamResponse;
   upstreamResponse.setStatusCode(http::status::Ok);
   upstreamResponse.setHeader("Content-Type", "text/plain");
   upstreamResponse.setBody("hello from rsession");

   session_proxy::handleLocalhostResponseForTest(
      boost::shared_ptr<http::AsyncConnection>(&connection, [](http::AsyncConnection*) {}),
      "port-token",
      "localhost",
      false,
      upstreamResponse);

   ASSERT_TRUE(connection.wroteResponse_);

   bool foundRefreshedAuthCookie = false;
   for (const http::Header& header : connection.writtenResponse_.getCookies({ kUserIdCookie }))
   {
      if (header.value.find(std::string(kUserIdCookie) + "=refreshed-value") != std::string::npos)
         foundRefreshedAuthCookie = true;
   }
   EXPECT_TRUE(foundRefreshedAuthCookie)
      << "refreshed auth cookie was dropped from the /p/ proxy response";

   // The upstream response's own content must still make it through.
   EXPECT_EQ(connection.writtenResponse_.body(), "hello from rsession");
}

// proxyRequest() streams any /s/ response of 1MB or more through a
// FixedBufferProxy, and proxyLocalhostRequest() does the same for /p/. Either
// way the proxy claims the connection's response and writes its headers from
// response()'s own storage. If the upstream then abandons the body, the
// upstream client calls the site's error handler while that write may still be
// in flight. Whatever the handler does, it must not mutate response() then,
// which is what AsyncConnection::response()'s threading note forbids; on a real
// socket it is a use-after-free.
//
// handleRpcError() and handleEventsError() assemble their error in a response
// of their own. handleContentError() and handleLocalhostError() reach a mid-body
// failure through their catch-all writeError() branch, which claims the
// connection first; their other branches still populate response() in place,
// so these also guard against a mid-body error ever being routed into one of
// those (see mutatingBranchesMatching()).
TEST(StreamedLocalStreamProxyTests, ContentErrorMidBodyLeavesInFlightHeadersAlone)
{
   r_util::SessionContext context("test-user");
   expectInterruptionsLeaveInFlightHeadersAlone(
      ProxySite::LocalStream,
      [&](boost::shared_ptr<http::AsyncConnection> ptrConnection, const Error& error)
      {
         session_proxy::handleContentErrorForTest(ptrConnection, context, error);
      });
}

TEST(StreamedLocalStreamProxyTests, RpcErrorMidBodyLeavesInFlightHeadersAlone)
{
   r_util::SessionContext context("test-user");
   expectInterruptionsLeaveInFlightHeadersAlone(
      ProxySite::LocalStream,
      [&](boost::shared_ptr<http::AsyncConnection> ptrConnection, const Error& error)
      {
         session_proxy::handleRpcErrorForTest(ptrConnection, context, http::Headers(), error);
      });
}

TEST(StreamedLocalStreamProxyTests, EventsErrorMidBodyLeavesInFlightHeadersAlone)
{
   r_util::SessionContext context("test-user");
   expectInterruptionsLeaveInFlightHeadersAlone(
      ProxySite::LocalStream,
      [&](boost::shared_ptr<http::AsyncConnection> ptrConnection, const Error& error)
      {
         session_proxy::handleEventsErrorForTest(ptrConnection, context, http::Headers(), error);
      });
}

TEST(StreamedLocalhostProxyTests, LocalhostErrorMidBodyLeavesInFlightHeadersAlone)
{
   expectInterruptionsLeaveInFlightHeadersAlone(
      ProxySite::Localhost,
      [](boost::shared_ptr<http::AsyncConnection> ptrConnection, const Error& error)
      {
         session_proxy::handleLocalhostErrorForTest(ptrConnection, error);
      });
}

// The errors the upstream clients raise once a response is under way, built
// with the properties they attach (AsyncClient/LocalStreamAsyncClient
// addErrorProperties()), checked against the same branch list. This covers
// transport errors the harness above can't readily provoke, and fails fast if
// a classifier is widened to match one -- e.g. connection_reset being added to
// isConnectionUnavailableError() so that a reset reads as a session to relaunch.
TEST(StreamedProxyErrorClassificationTests, MidBodyTransportErrorsReachNoMutatingBranch)
{
   const Error midBodyErrors[] = {
      Error(boost::asio::error::eof, ERROR_LOCATION),
      Error(boost::asio::error::connection_reset, ERROR_LOCATION),
      Error(boost::asio::error::connection_aborted, ERROR_LOCATION),
      Error(boost::asio::error::broken_pipe, ERROR_LOCATION),
      Error(boost::asio::error::operation_aborted, ERROR_LOCATION),
      Error(boost::asio::error::timed_out, ERROR_LOCATION),
      systemError(boost::system::errc::timed_out, ERROR_LOCATION),
   };

   for (const Error& bare : midBodyErrors)
   {
      SCOPED_TRACE(bare.asString());
      expectReachesNoMutatingBranch(bare);

      Error tagged = bare;
      tagged.addProperty("path", "/tmp/rstudio-test-stream");
      tagged.addProperty(http::kLocalStreamPeerPidProperty, 4242);
      tagged.addProperty("user-id", 1000);
      expectReachesNoMutatingBranch(tagged);
   }
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
   expectErrorAttributedToPeer(
      [](boost::shared_ptr<http::AsyncConnection> ptrConnection,
         const r_util::SessionContext& context,
         const Error& error)
      {
         session_proxy::handleRpcErrorForTest(ptrConnection, context, http::Headers(), error);
      },
      "proxy-rpc-error-peer-pid-user");
}

TEST(ProxyPendingLaunchTests, ContentErrorIsAttributedToItsPeerProcess)
{
   expectErrorAttributedToPeer(session_proxy::handleContentErrorForTest,
                               "proxy-content-error-peer-pid-user");
}

} // namespace tests
} // namespace server
} // namespace rstudio
