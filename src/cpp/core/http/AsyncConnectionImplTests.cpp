/*
 * AsyncConnectionImplTests.cpp
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

// Regression coverage for the sendingResponse_ double-response guard: once a
// response (or just its headers, e.g. via writeResponseHeaders() -- what
// FixedBufferProxy calls directly when streaming a proxied body) has started
// on a connection, a further attempt to write a response (e.g. an upstream
// error handler calling writeResponse()/writeError() after the streaming
// proxy already flushed headers/body to the same connection) must be a
// no-op, not a second response interleaved on the wire.
//
// "No-op" means nothing reaches the wire and response_ is left alone -- not
// that the losing caller is forgotten: the claim settles its handler with
// already_started so it can tear down instead of waiting forever on a
// completion that cannot arrive. And because the racing writers need not be
// on the same strand, one and only one of them may ever win the claim.

#include <atomic>
#include <string>
#include <thread>
#include <vector>

#include <boost/asio/error.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <boost/asio/read.hpp>
#include <boost/make_shared.hpp>

#include <core/http/AsyncConnectionImpl.hpp>
#include <core/http/Response.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

using boost::asio::ip::tcp;
typedef AsyncConnectionImpl<tcp::socket> TcpAsyncConnection;

} // anonymous namespace

TEST(AsyncConnectionImpl, WriteResponseAfterHeadersAlreadyWrittenIsANoOp)
{
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler());

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   // FixedBufferProxy's first queueChunk() call writes only the headers
   // (the body streams separately via asyncWrite()).
   pConnection->response().setStatusCode(200);

   bool headersWritten = false;
   pConnection->writeResponseHeaders([&](const boost::system::error_code&, std::size_t) {
      headersWritten = true;
   });

   // Simulate an upstream error handler (e.g. ServerSessionProxy::
   // handleLocalhostError) attempting to write a whole, different response
   // on the same connection after that.
   bool secondWriteHandlerCalled = false;
   boost::system::error_code secondWriteEc;
   pConnection->response().setStatusCode(502);
   pConnection->writeResponse(true, [&](const boost::system::error_code& ec, std::size_t) {
      secondWriteHandlerCalled = true;
      secondWriteEc = ec;
   });

   ioc.run();

   EXPECT_TRUE(headersWritten);
   // The guarded writeResponse() writes nothing, but it must still settle its
   // caller with an error rather than swallow the completion -- a caller left
   // waiting on a completion that can never arrive wedges: FixedBufferProxy
   // would keep clientWriteInProgress_ latched, never flush queued chunks,
   // never resume backpressure, and leak both sockets.
   EXPECT_TRUE(secondWriteHandlerCalled);
   EXPECT_EQ(secondWriteEc, boost::asio::error::already_started);

   pConnection->close();

   // Read everything the peer actually received off the wire: it must be
   // exactly the one (200) status line, never a second (502) one appended.
   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   EXPECT_NE(received.find("200"), std::string::npos);
   EXPECT_EQ(received.find("502"), std::string::npos);
}

TEST(AsyncConnectionImpl, WriteResponseHeadersSetsNosniffHeader)
{
   // writeResponseHeaders() (what FixedBufferProxy calls to stream a proxied
   // response) is a thinner path than writeResponse(), which has long set
   // X-Content-Type-Options: nosniff unconditionally on every response.
   // Confirm that parity is retained here too.
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler());

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);

   bool headersWritten = false;
   pConnection->writeResponseHeaders([&](const boost::system::error_code&, std::size_t) {
      headersWritten = true;
   });

   ioc.run();
   EXPECT_TRUE(headersWritten);

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   EXPECT_NE(received.find("X-Content-Type-Options: nosniff"), std::string::npos);
}

// RFC 7230 2.6: "Intermediaries that process HTTP messages (i.e., all
// intermediaries other than those acting as tunnels) MUST send their own
// HTTP-version in forwarded messages." A proxied response arrives here having
// had Response::assign() copy the *upstream's* version onto it, so without
// normalization an HTTP/1.0 backend's version ends up on the status line of a
// response this server framed as HTTP/1.1 -- telling the client not to expect
// chunked framing or a persistent connection regardless of what the headers
// below the status line actually say.

TEST(AsyncConnectionImpl, WriteResponseHeadersSendsOurOwnHttpVersionNotTheUpstreams)
{
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler());

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);
   pConnection->response().setHttpVersion(1, 0); // as an HTTP/1.0 upstream's would be

   bool headersWritten = false;
   pConnection->writeResponseHeaders([&](const boost::system::error_code&, std::size_t) {
      headersWritten = true;
   });

   ioc.run();
   EXPECT_TRUE(headersWritten);

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   EXPECT_EQ(received.compare(0, 12, "HTTP/1.1 200"), 0) << "status line was: " << received.substr(0, 32);
}

TEST(AsyncConnectionImpl, WriteResponseSendsOurOwnHttpVersionNotTheUpstreams)
{
   // Same normalization on the buffered path, so the streaming and buffered
   // proxy paths cannot disagree about what version this hop speaks.
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler());

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   // mirrors what a proxy handler does: assign() an upstream response (which
   // copies its version) onto the client-facing response, then write it
   http::Response upstream;
   upstream.setStatusCode(200);
   upstream.setHttpVersion(1, 0);
   upstream.setBody("hi");

   bool responseWritten = false;
   pConnection->writeResponse(upstream, true, http::Headers(),
                              [&](const boost::system::error_code&, std::size_t) {
      responseWritten = true;
   });

   ioc.run();
   EXPECT_TRUE(responseWritten);

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   EXPECT_EQ(received.compare(0, 12, "HTTP/1.1 200"), 0) << "status line was: " << received.substr(0, 32);
}

TEST(AsyncConnectionImpl, WriteResponseHeadersAfterHeadersAlreadyWrittenIsANoOp)
{
   // writeResponseHeaders() must guard against being called a second time the
   // same way writeResponse()/writeError() do (see
   // WriteResponseAfterHeadersAlreadyWrittenIsANoOp above): an in-flight
   // asyncWrite from the first call may still hold buffers pointing into
   // response_'s current backing storage, so a second call must not mutate
   // response_ (e.g. via the Date/nosniff header logic) or write again.
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler());

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);

   bool firstWritten = false;
   pConnection->writeResponseHeaders([&](const boost::system::error_code&, std::size_t) {
      firstWritten = true;
   });

   // Simulate a second attempt to write headers on the same connection
   // (e.g. a caller that doesn't itself track whether it already wrote
   // headers) before the first write has run.
   bool secondWriteHandlerCalled = false;
   boost::system::error_code secondWriteEc;
   pConnection->response().setStatusCode(500);
   pConnection->writeResponseHeaders([&](const boost::system::error_code& ec, std::size_t) {
      secondWriteHandlerCalled = true;
      secondWriteEc = ec;
   });

   ioc.run();

   EXPECT_TRUE(firstWritten);
   // guarded, but still settled with an error so the caller can tear down
   // instead of waiting forever -- see the same expectation in
   // WriteResponseAfterHeadersAlreadyWrittenIsANoOp above.
   EXPECT_TRUE(secondWriteHandlerCalled);
   EXPECT_EQ(secondWriteEc, boost::asio::error::already_started);

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   EXPECT_NE(received.find("200"), std::string::npos);
   EXPECT_EQ(received.find("500"), std::string::npos);
}

TEST(AsyncConnectionImpl, ConcurrentResponseWritersElectExactlyOneWinner)
{
   // The claim is a check-then-act, and the writers it arbitrates do not all
   // run on this connection's strand: a streaming proxy writes from the
   // *upstream* AsyncClient's completion handlers (proxyRequest does not share
   // our strand with them the way proxyLocalhostRequest does), while the
   // connection's own read path can turn a request-parse error into
   // writeResponse(BadRequest) at the same moment. Read-then-set of a plain
   // bool lets both observe "not started" and both write; a single atomic
   // exchange cannot.
   //
   // So race several writers at once and pin the invariant: exactly one wins
   // and writes, and every loser is settled with already_started rather than
   // being left waiting on a completion that will never come.
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler());

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);

   const int writerCount = 8;
   std::atomic<int> won{0};
   std::atomic<int> lost{0};
   std::atomic<int> unexpectedError{0};

   // release all the writers from the same spin barrier so their claims
   // actually overlap rather than being serialized by thread startup
   std::atomic<int> ready{0};
   std::atomic<bool> go{false};

   std::vector<std::thread> writers;
   for (int i = 0; i < writerCount; i++)
   {
      writers.emplace_back([&]() {
         ready++;
         while (!go)
            std::this_thread::yield();

         pConnection->writeResponse(true, [&](const boost::system::error_code& ec, std::size_t) {
            if (!ec)
               won++;
            else if (ec == boost::asio::error::already_started)
               lost++;
            else
               unexpectedError++;
         });
      });
   }

   while (ready < writerCount)
      std::this_thread::yield();
   go = true;

   for (std::thread& writer : writers)
      writer.join();

   ioc.run();

   EXPECT_EQ(won.load(), 1);
   EXPECT_EQ(lost.load(), writerCount - 1);
   EXPECT_EQ(unexpectedError.load(), 0);

   // and only one response reached the wire
   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   std::size_t statusLines = 0;
   for (std::size_t at = received.find("HTTP/1.1");
        at != std::string::npos;
        at = received.find("HTTP/1.1", at + 1))
   {
      statusLines++;
   }
   EXPECT_EQ(statusLines, 1u) << "wire was: " << received;
}

TEST(AsyncConnectionImpl, WriteResponseHeadersCollapsesDuplicateUpstreamNosniffHeader)
{
   // FixedBufferProxy assigns the upstream response's headers verbatim before
   // writeResponseHeaders() adds its own X-Content-Type-Options -- if the
   // upstream already sent one (or, per HTTP, more than one field with that
   // name, or a differently-cased duplicate), setHeader() only replaces the
   // *first* match, so the wire must not end up carrying two instances.
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler());

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);
   pConnection->response().addHeader("X-Content-Type-Options", "sniff-allowed");
   pConnection->response().addHeader("x-content-type-options", "also-stale");

   bool headersWritten = false;
   pConnection->writeResponseHeaders([&](const boost::system::error_code&, std::size_t) {
      headersWritten = true;
   });

   ioc.run();
   EXPECT_TRUE(headersWritten);

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   // Both upstream-supplied duplicates must be gone entirely -- not just the
   // first -- and our own value must be the only one present.
   EXPECT_EQ(received.find("sniff-allowed"), std::string::npos);
   EXPECT_EQ(received.find("also-stale"), std::string::npos);
   EXPECT_NE(received.find("X-Content-Type-Options: nosniff"), std::string::npos);
}

// writeResponse() has always run responseFilter_ before sending, and
// AsyncServerImpl::acceptNextConnection() installs one on every connection it
// accepts -- so the filter is not an optional hook that only some deployments
// configure. It is what stamps "Server" and every server-add-header header
// (documented as applying to all responses; in practice HSTS /
// X-Frame-Options), and in rstudio-pro it also rewrites Location/Refresh for
// multi-session URI prefixes. writeResponseHeaders() -- the path
// FixedBufferProxy streams a proxied response through -- must run it too, or
// the /p/ content path silently loses all of that.

TEST(AsyncConnectionImpl, WriteResponseHeadersRunsTheResponseFilter)
{
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   // stand in for AsyncServerImpl::connectionResponseFilter: the "Server"
   // stamp plus a server-add-header-style header.
   int filterCalls = 0;
   const http::Request* pRequestSeen = nullptr;
   ResponseFilter filter = [&](const http::Request& request, http::Response* pResponse) {
      ++filterCalls;
      pRequestSeen = &request;
      pResponse->setHeader("Server", "RStudio");
      pResponse->setHeader("X-Frame-Options", "SAMEORIGIN");
   };

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler(),
         RequestFilter(),
         filter);

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);

   bool headersWritten = false;
   pConnection->writeResponseHeaders([&](const boost::system::error_code&, std::size_t) {
      headersWritten = true;
   });

   ioc.run();
   EXPECT_TRUE(headersWritten);
   EXPECT_EQ(filterCalls, 1);

   // the filter must see the request as it originally arrived (what
   // writeResponse() passes), not the live request_ a request filter may have
   // rewritten -- pro's multi-session filter derives the URI prefix from it
   EXPECT_NE(pRequestSeen, &pConnection->request());

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   // the filter mutated the very response whose header buffers went out
   EXPECT_NE(received.find("Server: RStudio"), std::string::npos);
   EXPECT_NE(received.find("X-Frame-Options: SAMEORIGIN"), std::string::npos);
}

TEST(AsyncConnectionImpl, WriteResponseHeadersRunsTheResponseFilterLast)
{
   // Ordering parity with writeResponse(), which runs the filter after its own
   // Date/Connection/nosniff handling: a filter that deliberately overrides one
   // of those headers must win on this path too, not be overwritten by it.
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   ResponseFilter filter = [](const http::Request&, http::Response* pResponse) {
      pResponse->setHeader("X-Content-Type-Options", "filtered");
   };

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler(),
         RequestFilter(),
         filter);

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);

   bool headersWritten = false;
   pConnection->writeResponseHeaders([&](const boost::system::error_code&, std::size_t) {
      headersWritten = true;
   });

   ioc.run();
   EXPECT_TRUE(headersWritten);

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   EXPECT_NE(received.find("X-Content-Type-Options: filtered"), std::string::npos);
   EXPECT_EQ(received.find("X-Content-Type-Options: nosniff"), std::string::npos);
}

TEST(AsyncConnectionImpl, WriteResponseHeadersRunsTheResponseFilterExactlyOnce)
{
   // The responseAlreadyStarted() guard runs before every other mutation of
   // response_, and the filter must be inside it: a second header write, or a
   // late writeResponse() from an upstream error handler, must not re-run the
   // filter over a response whose header buffers an asyncWrite may still hold.
   // Re-running connectionResponseFilter would append duplicate
   // server-add-header fields for any header it does not already replace.
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   int filterCalls = 0;
   ResponseFilter filter = [&](const http::Request&, http::Response* pResponse) {
      ++filterCalls;
      pResponse->addHeader("X-Frame-Options", "SAMEORIGIN");
   };

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler(),
         RequestFilter(),
         filter);

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);
   pConnection->writeResponseHeaders([](const boost::system::error_code&, std::size_t) {});

   // a second header write, then a whole-response write (what an upstream
   // error handler does) - both guarded, so neither reaches the filter
   pConnection->writeResponseHeaders([](const boost::system::error_code&, std::size_t) {});
   pConnection->writeResponse();

   ioc.run();

   EXPECT_EQ(filterCalls, 1);

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   // exactly one instance on the wire -- the filter uses addHeader(), so a
   // second invocation would have appended a duplicate field
   std::size_t occurrences = 0;
   for (std::size_t pos = received.find("X-Frame-Options");
        pos != std::string::npos;
        pos = received.find("X-Frame-Options", pos + 1))
   {
      ++occurrences;
   }
   EXPECT_EQ(occurrences, 1u);
}

// A filter runs as the last mutation on both paths, so on both it can override
// the framing headers set before it -- including Content-Length,
// Transfer-Encoding and Connection, which an admin's server-add-header reaches
// like any other. The pair of tests below pin that the two paths agree about
// it, which is the property that matters: the streaming path is not more
// exposed than the buffered one, and a future change that hardens either must
// fail here until it hardens both.
//
// The blast radius is one malformed response, not cross-request desync: every
// terminal path in FixedBufferProxy closes the client connection
// (closeConnections()), and writeResponse()'s default close=true does the same,
// so a client misled by a bogus length reads what it believes is the body and
// then hits EOF rather than parsing the next response out of leftover bytes.

TEST(AsyncConnectionImpl, WriteResponseHeadersLetsTheFilterOverrideFramingHeaders)
{
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   ResponseFilter filter = [](const http::Request&, http::Response* pResponse) {
      pResponse->setHeader("Content-Length", "999");
      pResponse->setHeader("Transfer-Encoding", "gzip");
      pResponse->setHeader("Connection", "keep-alive");
   };

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler(),
         RequestFilter(),
         filter);

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   // the framing headers FixedBufferProxy would have chosen for a chunked
   // response, set before the header write exactly as it sets them
   pConnection->response().setStatusCode(200);
   pConnection->response().setHeader("Transfer-Encoding", "chunked");
   pConnection->response().setHeader("Connection", "close");

   pConnection->writeResponseHeaders([](const boost::system::error_code&, std::size_t) {});

   ioc.run();
   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   EXPECT_NE(received.find("Content-Length: 999"), std::string::npos);
   EXPECT_NE(received.find("Transfer-Encoding: gzip"), std::string::npos);
   EXPECT_EQ(received.find("Transfer-Encoding: chunked"), std::string::npos);
   EXPECT_NE(received.find("Connection: keep-alive"), std::string::npos);
}

TEST(AsyncConnectionImpl, WriteResponseLetsTheFilterOverrideFramingHeadersToo)
{
   // The parity half of the test above: the long-standing buffered path allows
   // exactly the same overrides, which is why running the filter after framing
   // on the streaming path is not a new hazard there.
   boost::asio::io_context ioc;

   tcp::acceptor acceptor(ioc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
   unsigned short port = acceptor.local_endpoint().port();

   ResponseFilter filter = [](const http::Request&, http::Response* pResponse) {
      pResponse->setHeader("Content-Length", "999");
      pResponse->setHeader("Transfer-Encoding", "gzip");
      pResponse->setHeader("Connection", "keep-alive");
   };

   boost::shared_ptr<TcpAsyncConnection> pConnection =
      boost::make_shared<TcpAsyncConnection>(
         ioc,
         boost::shared_ptr<boost::asio::ssl::context>(),
         /*requestSequence=*/1,
         TcpAsyncConnection::HeadersParsedHandler(),
         TcpAsyncConnection::Handler(),
         TcpAsyncConnection::ClosedHandler(),
         RequestFilter(),
         filter);

   boost::system::error_code ec;
   pConnection->socket().connect(
      tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), port), ec);
   ASSERT_FALSE(ec);

   tcp::socket peer(ioc);
   acceptor.accept(peer, ec);
   ASSERT_FALSE(ec);

   pConnection->response().setStatusCode(200);
   pConnection->response().setBody("hello");

   // close=true, so writeResponse() sets Connection: close before the filter
   pConnection->writeResponse(true, [](const boost::system::error_code&, std::size_t) {});

   ioc.run();
   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   // the filter's values reach the wire here as well -- note the declared
   // length (999) does not match the body actually sent ("hello"), which is
   // precisely the malformed-response shape the streaming path is accused of
   // being uniquely vulnerable to
   EXPECT_NE(received.find("Content-Length: 999"), std::string::npos);
   EXPECT_NE(received.find("Transfer-Encoding: gzip"), std::string::npos);
   EXPECT_NE(received.find("Connection: keep-alive"), std::string::npos);
   EXPECT_EQ(received.find("Connection: close"), std::string::npos);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
