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

#include <chrono>
#include <condition_variable>
#include <memory>
#include <mutex>
#include <string>
#include <thread>

#include <boost/asio.hpp>
#include <boost/make_shared.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <server_core/http/LocalhostAsyncClient.hpp>

namespace rstudio {
namespace server_core {
namespace http {

namespace {

// One-shot signal from the client's io_context thread to the responder thread.
class Gate
{
public:
   void open()
   {
      std::lock_guard<std::mutex> lock(mutex_);
      open_ = true;
      cv_.notify_all();
   }

   // Bounded, so a gate that never opens fails the test rather than hanging it.
   bool wait()
   {
      std::unique_lock<std::mutex> lock(mutex_);
      return cv_.wait_for(lock, std::chrono::seconds(2), [this]() { return open_; });
   }

private:
   std::mutex mutex_;
   std::condition_variable cv_;
   bool open_ = false;
};

// Serves one response with no Content-Length and no Transfer-Encoding, so its
// body is delimited by the connection closing. The headers go out together with
// the first part of the body; the rest is only written once the client has
// parsed the headers, so the client must read again to see it.
class EofDelimitedResponder
{
public:
   EofDelimitedResponder(const std::string& firstPart,
                         const std::string& secondPart,
                         std::shared_ptr<Gate> pHeadersParsed)
      : acceptor_(ioc_, boost::asio::ip::tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0)),
        firstPart_(firstPart),
        secondPart_(secondPart),
        pHeadersParsed_(pHeadersParsed)
   {
   }

   ~EofDelimitedResponder()
   {
      if (thread_.joinable())
         thread_.join();
   }

   unsigned short port() { return acceptor_.local_endpoint().port(); }

   void start() { thread_ = std::thread([this]() { run(); }); }

private:
   void run()
   {
      boost::system::error_code ec;
      boost::asio::ip::tcp::socket socket(ioc_);
      acceptor_.accept(socket, ec);
      if (ec)
         return;

      boost::asio::streambuf request;
      boost::asio::read_until(socket, request, "\r\n\r\n", ec);
      if (ec)
         return;

      std::string head =
         "HTTP/1.1 200 OK\r\n"
         "Content-Type: application/octet-stream\r\n"
         "\r\n" + firstPart_;
      boost::asio::write(socket, boost::asio::buffer(head), ec);
      if (ec)
         return;

      // A client that already gave up on the body just makes this write fail.
      pHeadersParsed_->wait();
      boost::asio::write(socket, boost::asio::buffer(secondPart_), ec);

      socket.shutdown(boost::asio::ip::tcp::socket::shutdown_both, ec);
      socket.close(ec);
   }

   boost::asio::io_context ioc_;
   boost::asio::ip::tcp::acceptor acceptor_;
   std::string firstPart_;
   std::string secondPart_;
   std::shared_ptr<Gate> pHeadersParsed_;
   std::thread thread_;
};

} // anonymous namespace

// proxyLocalhostRequest() (/p/) streams every response its buffer predicate
// doesn't hold, including one with no Content-Length (see
// BufferingPolicyTests.StreamsResponseWithNoContentLength). A streamed body is
// never accumulated into response_.body(), so stopReadingAndRespondImpl()'s
// `body().length() >= contentLength()` evaluates `0 >= 0` on the first read
// after the headers and ends the response right there. The browser gets only
// the bytes that arrived with the headers.
//
// AsyncClient::readSomeContent() guards against this for every subclass by
// never consulting stopReadingAndRespond() for a streamed (or chunked) body;
// see AsyncClientContentLengthTests' StopReadingOnContentLengthAsyncClient.
// This test checks the guard covers LocalhostAsyncClient's own override.
TEST(LocalhostAsyncClientTest, StreamsEofDelimitedBodyToCompletion)
{
   const std::string firstPart(4096, 'a');
   const std::string secondPart(4096, 'b');

   auto pHeadersParsed = std::make_shared<Gate>();
   EofDelimitedResponder responder(firstPart, secondPart, pHeadersParsed);
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
