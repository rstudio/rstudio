/*
 * LocalhostAsyncClientTests.cpp
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

#include <memory>
#include <string>

#include <boost/asio.hpp>
#include <boost/make_shared.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/tests/GatedResponder.hpp>
#include <server_core/http/LocalhostAsyncClient.hpp>

namespace rstudio {
namespace server_core {
namespace http {

// proxyLocalhostRequest() (/p/) streams every response its buffer predicate
// doesn't hold, including one with no Content-Length (see
// BufferingPolicyTests.StreamsResponseWithNoContentLength). A streamed body is
// never accumulated into response_.body(), so stopReadingAndRespondImpl()'s
// `body().length() >= contentLength()` evaluates `0 >= 0` on the first read
// after the headers and ends the response right there. The browser gets only
// the bytes that arrived with the headers.
//
// AsyncClient::isStreamingResponse() exists for this override shape (see
// AsyncClientContentLengthTests' GuardedStopReadingAsyncClient); both
// LocalhostAsyncClient overrides need to consult it.
TEST(LocalhostAsyncClientTest, StreamsEofDelimitedBodyToCompletion)
{
   const std::string firstPart(4096, 'a');
   const std::string secondPart(4096, 'b');

   // No Content-Length and no Transfer-Encoding, so the body is delimited by
   // the connection closing. The headers go out together with the first part
   // of the body; the rest is only written once the client has parsed the
   // headers, so the client must read again to see it.
   const std::string head =
      "HTTP/1.1 200 OK\r\n"
      "Content-Type: application/octet-stream\r\n"
      "\r\n" + firstPart;

   auto pHeadersParsed = std::make_shared<core::tests::Gate>();
   core::tests::GatedResponder responder(
      head, pHeadersParsed, [&](boost::asio::ip::tcp::socket& socket)
      {
         // a client that already gave up on the body just makes this write fail
         boost::system::error_code ec;
         boost::asio::write(socket, boost::asio::buffer(secondPart), ec);
         socket.shutdown(boost::asio::ip::tcp::socket::shutdown_both, ec);
      });
   responder.start();

   boost::asio::io_context ioc;
   auto pClient = boost::make_shared<LocalhostAsyncClient>(ioc, "127.0.0.1", std::to_string(responder.port()));
   pClient->setRequestTimeout(boost::posix_time::seconds(5));

   // the streaming configuration proxyLocalhostRequest() applies
   pClient->setStreamNonChunkedResponses(true);
   pClient->setFixedBufferHandlerSupportsPause(true);

   pClient->setResponseHeadersHandler(
      [pHeadersParsed](const core::http::Response&) { pHeadersParsed->open(); });

   pClient->request().setMethod("GET");
   pClient->request().setUri("/download");
   pClient->request().setHeader("Connection", "close");

   std::string received;
   bool sawFinal = false;
   bool sawError = false;
   pClient->execute(
      [](const core::http::Response&) {},
      [&](const core::Error&) { sawError = true; },
      [&](const core::http::Response&, const std::string& chunk)
      {
         if (chunk.empty())
            sawFinal = true;
         else
            received += chunk;
         return true;
      });
   ioc.run();

   EXPECT_FALSE(sawError);
   EXPECT_TRUE(sawFinal);
   EXPECT_EQ(firstPart.size() + secondPart.size(), received.size())
      << "the streamed body ended after " << received.size() << " bytes";
   EXPECT_TRUE(received == firstPart + secondPart);
}

} // namespace http
} // namespace server_core
} // namespace rstudio
