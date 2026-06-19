/*
 * AsyncClientContentLengthTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

// Regression coverage for rstudio#17807: AsyncClient must terminate a
// Content-Length-delimited response once the body is fully received, rather
// than depending on the server to close the connection (EOF). A server or
// proxy that keeps the socket open after a complete response would otherwise
// stall the read until the caller's timeout, discarding the received body.

#include <atomic>
#include <chrono>
#include <string>
#include <thread>

#include <boost/asio/io_context.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/system_timer.hpp>
#include <boost/asio/write.hpp>
#include <boost/make_shared.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/TcpIpAsyncClient.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

using boost::asio::ip::tcp;

// A minimal blocking HTTP/1.1 server on its own thread. Accepts a single
// connection, reads the request headers, and writes back a Content-Length
// response. It never sends "Connection: close"; when closeAfterResponse is
// false it holds the socket open, simulating an origin/proxy that keeps the
// connection alive.
class LocalServer
{
public:
   LocalServer(bool closeAfterResponse, std::string body)
      : acceptor_(ioc_, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0)),
        closeAfterResponse_(closeAfterResponse),
        body_(std::move(body))
   {
   }

   ~LocalServer()
   {
      stop_ = true;
      if (thread_.joinable())
         thread_.join();
   }

   unsigned short port() { return acceptor_.local_endpoint().port(); }
   void start() { thread_ = std::thread([this]() { run(); }); }
   void stop() { stop_ = true; }

private:
   void run()
   {
      boost::system::error_code ec;

      tcp::socket socket(ioc_);
      acceptor_.accept(socket, ec);
      if (ec)
         return;

      boost::asio::streambuf buf;
      boost::asio::read_until(socket, buf, "\r\n\r\n", ec);

      std::string resp =
         "HTTP/1.1 200 OK\r\n"
         "Content-Type: application/x-ndjson\r\n"
         "Content-Length: " + std::to_string(body_.size()) + "\r\n"
         "\r\n" + body_;

      boost::asio::write(socket, boost::asio::buffer(resp), ec);

      if (closeAfterResponse_)
      {
         socket.shutdown(tcp::socket::shutdown_both, ec);
         socket.close(ec);
      }
      else
      {
         while (!stop_.load())
            std::this_thread::sleep_for(std::chrono::milliseconds(25));
         socket.close(ec);
      }
   }

   boost::asio::io_context ioc_;
   tcp::acceptor acceptor_;
   bool closeAfterResponse_;
   std::string body_;
   std::atomic<bool> stop_{false};
   std::thread thread_;
};

struct Outcome
{
   bool gotResponse = false;
   bool gotError = false;
   bool timedOut = false;
   int statusCode = 0;
   std::string body;
   double elapsedSeconds = 0.0;
};

Outcome runScenario(bool closeAfterResponse,
                    const std::string& responseBody = "{\"name\":\"jsonlite\"}\n")
{
   LocalServer server(closeAfterResponse, responseBody);
   server.start();

   boost::asio::io_context ioc;

   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("POST");
   request.setUri("/__api__/filter/packages");
   request.setHeader("Connection", "close");
   request.setContentType("application/json");
   request.setBody("{\"repo\":\"cran\",\"names\":[\"jsonlite\"]}");

   Outcome outcome;
   auto start = std::chrono::steady_clock::now();

   // generous backstop; the fix should complete well under this
   boost::shared_ptr<boost::asio::system_timer> pTimer =
      boost::make_shared<boost::asio::system_timer>(ioc, std::chrono::seconds(4));

   pTimer->async_wait([&](const boost::system::error_code& ec) {
      if (ec == boost::asio::error::operation_aborted)
         return;
      outcome.timedOut = true;
      pClient->close();
   });

   pClient->execute(
      [&](const http::Response& response) {
         outcome.gotResponse = true;
         outcome.statusCode = response.statusCode();
         outcome.body = response.body();
         outcome.elapsedSeconds =
            std::chrono::duration<double>(std::chrono::steady_clock::now() - start).count();
         pTimer->cancel();
      },
      [&](const core::Error&) {
         outcome.gotError = true;
         outcome.elapsedSeconds =
            std::chrono::duration<double>(std::chrono::steady_clock::now() - start).count();
         pTimer->cancel();
      });

   ioc.run();

   server.stop();
   return outcome;
}

} // anonymous namespace

// Baseline: a server that closes after the response delivers it via EOF.
TEST(AsyncClientContentLength, DeliversWhenServerClosesConnection)
{
   Outcome outcome = runScenario(/*closeAfterResponse=*/true);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_EQ(outcome.body, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.timedOut);
}

// The fix: a complete Content-Length response on a socket the server keeps
// open must be delivered promptly, not stalled until the timeout.
TEST(AsyncClientContentLength, DeliversWhenServerKeepsConnectionOpen)
{
   Outcome outcome = runScenario(/*closeAfterResponse=*/false);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_EQ(outcome.body, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.timedOut);
}

// An empty body with Content-Length: 0 on a kept-open socket must also respond
// immediately rather than waiting for EOF.
TEST(AsyncClientContentLength, DeliversEmptyBodyWhenServerKeepsConnectionOpen)
{
   Outcome outcome = runScenario(/*closeAfterResponse=*/false, /*responseBody=*/"");

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_TRUE(outcome.body.empty());
   EXPECT_FALSE(outcome.timedOut);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
