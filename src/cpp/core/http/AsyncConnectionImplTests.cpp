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

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
