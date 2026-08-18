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

#include <string>
#include <vector>

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
   pConnection->response().setStatusCode(502);
   pConnection->writeResponse(true, [&](const boost::system::error_code&, std::size_t) {
      secondWriteHandlerCalled = true;
   });

   ioc.run();

   EXPECT_TRUE(headersWritten);
   // The guarded writeResponse() call must return without posting any
   // completion at all -- its handler must never fire.
   EXPECT_FALSE(secondWriteHandlerCalled);

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
   pConnection->response().setStatusCode(500);
   pConnection->writeResponseHeaders([&](const boost::system::error_code&, std::size_t) {
      secondWriteHandlerCalled = true;
   });

   ioc.run();

   EXPECT_TRUE(firstWritten);
   EXPECT_FALSE(secondWriteHandlerCalled);

   pConnection->close();

   std::vector<char> data(4096);
   boost::system::error_code readEc;
   std::size_t n = boost::asio::read(peer, boost::asio::buffer(data), readEc);
   std::string received(data.data(), n);

   EXPECT_NE(received.find("200"), std::string::npos);
   EXPECT_EQ(received.find("500"), std::string::npos);
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

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
