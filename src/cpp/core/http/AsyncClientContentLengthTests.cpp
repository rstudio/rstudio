/*
 * AsyncClientContentLengthTests.cpp
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

// Regression coverage for rstudio#17807 and the related AsyncClient hardening:
//   - AsyncClient must terminate a Content-Length-delimited response once the
//     body is fully received, rather than depending on the server to close the
//     connection (EOF). A server or proxy that keeps the socket open after a
//     complete response would otherwise stall the read until the deadline,
//     discarding the received body.
//   - A response whose body is absent (no Content-Length, not chunked) or
//     chunked must still terminate as before -- via EOF and the chunk
//     terminator respectively -- so the Content-Length fast path does not
//     short-circuit an incomplete body or break chunked transfers.
//   - When an overall request deadline is configured (setRequestTimeout), a
//     peer that stalls after connecting must surface a timeout error rather
//     than keeping the request in flight indefinitely, while a request that
//     completes within the deadline must not see a spurious late timeout.

#include <atomic>
#include <chrono>
#include <sstream>
#include <string>
#include <thread>

#include <boost/asio/io_context.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/system_timer.hpp>
#include <boost/asio/write.hpp>
#include <boost/make_shared.hpp>
#include <boost/system/error_code.hpp>

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

// How the local server frames its response body.
enum class ResponseMode
{
   // Headers and a Content-Length-delimited body in a single write.
   ContentLength,

   // Headers, then the Content-Length body streamed across multiple writes
   // (with a pause between) so the client must assemble it in handleReadContent
   // across multiple TCP reads -- the actual proxy/NDJSON #17807 scenario.
   ContentLengthSplit,

   // A Transfer-Encoding: chunked body, terminated by the zero-length chunk
   // rather than Content-Length or EOF.
   Chunked,

   // A response with neither Content-Length nor chunked encoding; the body is
   // delimited by connection close (EOF).
   NoContentLength,

   // Accept the connection and never reply, simulating a peer that stalls after
   // the handshake.
   NoResponse
};

// A minimal blocking HTTP/1.1 server on its own thread. Accepts a single
// connection, reads the request headers, and writes back a response framed
// according to the requested ResponseMode. It never sends "Connection: close";
// when closeAfterResponse is false it holds the socket open, simulating an
// origin/proxy that keeps the connection alive after a complete response.
class LocalServer
{
public:
   LocalServer(ResponseMode mode, bool closeAfterResponse, std::string body)
      : acceptor_(ioc_, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0)),
        mode_(mode),
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

      writeResponse(socket, ec);

      if (mode_ != ResponseMode::NoResponse && closeAfterResponse_)
      {
         socket.shutdown(tcp::socket::shutdown_both, ec);
         socket.close(ec);
      }
      else
      {
         // hold the socket open (kept-alive response, or no reply at all) until
         // the test releases us
         while (!stop_.load())
            std::this_thread::sleep_for(std::chrono::milliseconds(25));
         socket.close(ec);
      }
   }

   void writeResponse(tcp::socket& socket, boost::system::error_code& ec)
   {
      switch (mode_)
      {
         case ResponseMode::NoResponse:
            break;

         case ResponseMode::ContentLength:
         {
            std::string resp =
               "HTTP/1.1 200 OK\r\n"
               "Content-Type: application/x-ndjson\r\n"
               "Content-Length: " + std::to_string(body_.size()) + "\r\n"
               "\r\n" + body_;
            boost::asio::write(socket, boost::asio::buffer(resp), ec);
            break;
         }

         case ResponseMode::ContentLengthSplit:
         {
            std::string headers =
               "HTTP/1.1 200 OK\r\n"
               "Content-Type: application/x-ndjson\r\n"
               "Content-Length: " + std::to_string(body_.size()) + "\r\n"
               "\r\n";
            boost::asio::write(socket, boost::asio::buffer(headers), ec);
            if (ec)
               return;

            // split the body across two writes with a pause between so it spans
            // separate reads on the client
            std::size_t half = body_.size() / 2;
            std::this_thread::sleep_for(std::chrono::milliseconds(25));
            boost::asio::write(socket, boost::asio::buffer(body_.data(), half), ec);
            if (ec)
               return;

            std::this_thread::sleep_for(std::chrono::milliseconds(25));
            boost::asio::write(
               socket, boost::asio::buffer(body_.data() + half, body_.size() - half), ec);
            break;
         }

         case ResponseMode::Chunked:
         {
            std::ostringstream resp;
            resp << "HTTP/1.1 200 OK\r\n"
                    "Content-Type: application/x-ndjson\r\n"
                    "Transfer-Encoding: chunked\r\n"
                    "\r\n";
            if (!body_.empty())
               resp << std::hex << body_.size() << "\r\n" << body_ << "\r\n";
            resp << "0\r\n\r\n";

            std::string bytes = resp.str();
            boost::asio::write(socket, boost::asio::buffer(bytes), ec);
            break;
         }

         case ResponseMode::NoContentLength:
         {
            std::string resp =
               "HTTP/1.1 200 OK\r\n"
               "Content-Type: text/plain\r\n"
               "\r\n" + body_;
            boost::asio::write(socket, boost::asio::buffer(resp), ec);
            break;
         }
      }
   }

   boost::asio::io_context ioc_;
   tcp::acceptor acceptor_;
   ResponseMode mode_;
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
   int errorCode = 0;
   std::string body;
   double elapsedSeconds = 0.0;
};

Outcome runScenario(ResponseMode mode,
                    bool closeAfterResponse,
                    const std::string& responseBody = "{\"name\":\"jsonlite\"}\n",
                    const boost::posix_time::time_duration& requestTimeout =
                       boost::posix_time::pos_infin)
{
   LocalServer server(mode, closeAfterResponse, responseBody);
   server.start();

   boost::asio::io_context ioc;

   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   if (!requestTimeout.is_special())
      pClient->setRequestTimeout(requestTimeout);

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
      [&](const core::Error& error) {
         outcome.gotError = true;
         outcome.errorCode = error.getCode();
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
   Outcome outcome = runScenario(ResponseMode::ContentLength, /*closeAfterResponse=*/true);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_EQ(outcome.body, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.timedOut);
}

// The fix: a complete Content-Length response on a socket the server keeps
// open must be delivered promptly, not stalled until the timeout.
TEST(AsyncClientContentLength, DeliversWhenServerKeepsConnectionOpen)
{
   Outcome outcome = runScenario(ResponseMode::ContentLength, /*closeAfterResponse=*/false);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_EQ(outcome.body, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.timedOut);
}

// An empty body with Content-Length: 0 on a kept-open socket must also respond
// immediately rather than waiting for EOF.
TEST(AsyncClientContentLength, DeliversEmptyBodyWhenServerKeepsConnectionOpen)
{
   Outcome outcome =
      runScenario(ResponseMode::ContentLength, /*closeAfterResponse=*/false, /*responseBody=*/"");

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_TRUE(outcome.body.empty());
   EXPECT_FALSE(outcome.timedOut);
}

// The Content-Length termination must assemble a body that arrives across
// multiple TCP reads (handleReadContent), not just one that lands with the
// headers (handleReadHeaders). The server holds the socket open, so only the
// Content-Length check -- not EOF -- can complete the response.
TEST(AsyncClientContentLength, AssemblesBodyStreamedAcrossMultipleReads)
{
   Outcome outcome =
      runScenario(ResponseMode::ContentLengthSplit, /*closeAfterResponse=*/false);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_EQ(outcome.body, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.timedOut);
}

// A chunked response (no Content-Length) terminates on the zero-length chunk,
// not EOF. The server keeps the socket open, so the Content-Length fast path
// must correctly defer to chunked handling rather than short-circuiting.
TEST(AsyncClientContentLength, DeliversChunkedBodyWhenServerKeepsConnectionOpen)
{
   Outcome outcome = runScenario(ResponseMode::Chunked, /*closeAfterResponse=*/false);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_EQ(outcome.body, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.timedOut);
}

// A response that declares neither Content-Length nor chunked encoding is
// delimited by connection close. responseBodyComplete() must return false here
// so the body is read until EOF rather than being short-circuited.
TEST(AsyncClientContentLength, DeliversBodyWithoutContentLengthViaEof)
{
   Outcome outcome = runScenario(ResponseMode::NoContentLength, /*closeAfterResponse=*/true);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_EQ(outcome.body, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.timedOut);
}

// rstudio#17807 (general gap): when setRequestTimeout is configured, a peer
// that connects and then never responds must surface a timeout error rather
// than keeping the request in flight forever. The client's own 300ms deadline
// should fire (delivering a timed_out error) well before the test's 4s backstop.
TEST(AsyncClientContentLength, RequestTimeoutFiresWhenServerNeverResponds)
{
   Outcome outcome = runScenario(ResponseMode::NoResponse,
                                 /*closeAfterResponse=*/false,
                                 /*responseBody=*/"",
                                 /*requestTimeout=*/boost::posix_time::milliseconds(300));

   EXPECT_FALSE(outcome.gotResponse);
   EXPECT_TRUE(outcome.gotError);
   EXPECT_EQ(outcome.errorCode, static_cast<int>(boost::system::errc::timed_out));
   EXPECT_FALSE(outcome.timedOut);

   // the client deadline (300ms), not the 4s backstop, must have fired
   EXPECT_GE(outcome.elapsedSeconds, 0.1);
   EXPECT_LT(outcome.elapsedSeconds, 2.0);
}

// The complement of the timeout test: with a deadline set but the server
// responding right away, the request must complete normally and the deadline
// must be cancelled cleanly -- no spurious late timed_out error.
TEST(AsyncClientContentLength, CompletesPromptlyWhenDeadlineSetAndServerResponds)
{
   Outcome outcome = runScenario(ResponseMode::ContentLength,
                                 /*closeAfterResponse=*/true,
                                 /*responseBody=*/"{\"name\":\"jsonlite\"}\n",
                                 /*requestTimeout=*/boost::posix_time::seconds(5));

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_EQ(outcome.statusCode, 200);
   EXPECT_EQ(outcome.body, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.gotError);
   EXPECT_FALSE(outcome.timedOut);
   EXPECT_LT(outcome.elapsedSeconds, 1.0);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
