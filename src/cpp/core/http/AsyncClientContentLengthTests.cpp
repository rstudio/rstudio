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
#include <set>
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

#include <core/http/AsyncConnection.hpp>
#include <core/http/FixedBufferProxy.hpp>
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
   NoResponse,

   // Declares Content-Length for the full body but only ever writes the first
   // half of it, then closes the connection cleanly (shutdown + close) --
   // simulates a backend process (rsession/Shiny/Jetty) crashing or being
   // killed mid-response after emitting headers and part of the body.
   TruncatedContentLength,

   // Declares Content-Length for the intended body but actually writes extra
   // trailing bytes beyond it, with the connection kept open afterward --
   // simulates a backend whose Content-Length header is stale/miscalculated
   // relative to what it actually writes.
   OverLongContentLength,

   // Writes headers and half the declared Content-Length body, then aborts
   // the connection with a hard reset (SO_LINGER{on,0} + close, which causes
   // the kernel to send RST instead of FIN) rather than a clean shutdown --
   // simulates a backend process being killed (segfault, kill -9, OOM) after
   // the downstream client has already started receiving bytes.
   PartialBodyThenHardReset
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

      if (mode_ == ResponseMode::PartialBodyThenHardReset)
      {
         // Give the already-written partial body a moment to actually reach
         // the client before resetting -- SO_LINGER{0} can otherwise discard
         // unacknowledged data, silently turning "partial body then reset"
         // into "no body at all then reset".
         std::this_thread::sleep_for(std::chrono::milliseconds(25));

         // SO_LINGER{on, 0} makes close() send a hard RST instead of the
         // normal FIN/shutdown sequence, simulating a backend process dying
         // (segfault, kill -9, OOM) rather than closing cleanly.
         boost::asio::socket_base::linger option(true, 0);
         socket.set_option(option, ec);
         socket.close(ec);
      }
      else if (mode_ != ResponseMode::NoResponse && closeAfterResponse_)
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

         case ResponseMode::TruncatedContentLength:
         {
            std::string resp =
               "HTTP/1.1 200 OK\r\n"
               "Content-Type: application/x-ndjson\r\n"
               "Content-Length: " + std::to_string(body_.size()) + "\r\n"
               "\r\n" + body_.substr(0, body_.size() / 2);
            boost::asio::write(socket, boost::asio::buffer(resp), ec);
            break;
         }

         case ResponseMode::OverLongContentLength:
         {
            std::string extra = "EXTRA-BYTES-BEYOND-DECLARED-CONTENT-LENGTH";
            std::string resp =
               "HTTP/1.1 200 OK\r\n"
               "Content-Type: application/x-ndjson\r\n"
               "Content-Length: " + std::to_string(body_.size()) + "\r\n"
               "\r\n" + body_ + extra;
            boost::asio::write(socket, boost::asio::buffer(resp), ec);
            break;
         }

         case ResponseMode::PartialBodyThenHardReset:
         {
            std::string resp =
               "HTTP/1.1 200 OK\r\n"
               "Content-Type: application/x-ndjson\r\n"
               "Content-Length: " + std::to_string(body_.size()) + "\r\n"
               "\r\n" + body_.substr(0, body_.size() / 2);
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
                       boost::posix_time::pos_infin,
                    const std::string& requestMethod = "POST")
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
   request.setMethod(requestMethod);
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

// Streaming outcome: records the pieces delivered to a FixedBufferHandler (as opposed
// to the whole-body ResponseHandler used by runScenario above), plus whether
// each of the two handlers fired.
struct StreamingOutcome
{
   bool responseHandlerCalled = false;
   bool fixedBufferHandlerSawFinal = false;
   std::vector<std::string> chunks; // does not include the final empty chunk
   std::string responseBody; // populated only if responseHandlerCalled
   int statusCode = 0;
   bool timedOut = false;
   // Content-Length header value observed on the `response` argument passed to
   // the FixedBufferHandler for the first delivered piece -- confirms AsyncClient
   // relays the upstream Content-Length through to the fixed buffer handler (so
   // FixedBufferProxy can choose Content-Length framing downstream).
   std::string contentLengthHeaderOnFirstChunk;
};

// Drives a client opted into setStreamNonChunkedResponses(true), with an
// optional buffer predicate and an optional hook to simulate the fixed buffer
// handler applying backpressure (returning false) on any of a set of 0-based
// chunk indices, requiring the test to call resumeChunkProcessing() to
// continue. Each index in pauseOnChunkIndices is a raw call count (including
// redeliveries), which -- since that counter only ever increases -- can never
// revisit the same value twice, so a plain set membership check pauses each
// requested index exactly once without extra bookkeeping.
// pauseOnFinalSignal additionally simulates backpressure landing on the
// completion signal itself (the empty chunk) rather than on any data chunk --
// see BackpressureOnCompletionSignalStillCompletes below.
StreamingOutcome runStreamingScenario(
   ResponseMode mode,
   bool closeAfterResponse,
   const std::string& responseBody,
   const boost::function<bool(const http::Response&)>& bufferPredicate =
      boost::function<bool(const http::Response&)>(),
   std::set<int> pauseOnChunkIndices = {},
   bool pauseOnFinalSignal = false)
{
   LocalServer server(mode, closeAfterResponse, responseBody);
   server.start();

   boost::asio::io_context ioc;

   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   pClient->setStreamNonChunkedResponses(true);
   // production wiring (FixedBufferProxy::proxy()) always sets both flags together;
   // this scenario simulates that consumer's pause/resume contract, so the
   // completion signal's `false` return here means backpressure, not "done".
   pClient->setFixedBufferHandlerSupportsPause(true);
   if (bufferPredicate)
      pClient->setBufferPredicate(bufferPredicate);

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   StreamingOutcome outcome;
   int chunkIndex = 0;

   boost::shared_ptr<boost::asio::system_timer> pTimer =
      boost::make_shared<boost::asio::system_timer>(ioc, std::chrono::seconds(4));
   pTimer->async_wait([&](const boost::system::error_code& ec) {
      if (ec == boost::asio::error::operation_aborted)
         return;
      outcome.timedOut = true;
      pClient->close();
   });

   bool finalSignalPaused = false;
   FixedBufferHandler fixedBufferHandler = [&](const http::Response& response, const std::string& chunk) -> bool
   {
      outcome.statusCode = response.statusCode();

      if (chunk.empty())
      {
         if (pauseOnFinalSignal && !finalSignalPaused)
         {
            // simulate FixedBufferProxy's outbound buffer being exactly full at the
            // instant the completion signal arrives: this call bypasses
            // deliverChunks()/chunkState_ entirely (see closeAndRespond()),
            // so declining it here exercises AsyncClient's own
            // completionPending_ tracking rather than the ordinary
            // chunkState_ pause/resume path already covered above.
            finalSignalPaused = true;
            boost::asio::post(ioc, [&]() {
               pClient->resumeChunkProcessing();
            });
            return false;
         }

         outcome.fixedBufferHandlerSawFinal = true;
         pTimer->cancel();
         return true;
      }

      if (outcome.contentLengthHeaderOnFirstChunk.empty() && chunkIndex == 0)
         outcome.contentLengthHeaderOnFirstChunk = response.headerValue("Content-Length");

      bool pauseHere = pauseOnChunkIndices.count(chunkIndex) > 0;
      chunkIndex++;

      if (pauseHere)
      {
         // simulate backpressure: signal "not consumed" without recording the
         // chunk. AsyncClient retains this same (unconsumed) chunk in
         // chunkState_ and will redeliver it via resumeChunkProcessing() below
         // -- that redelivery is what actually records it (chunkIndex has
         // moved past every index in pauseOnChunkIndices, so it is accepted
         // then, even if that later index is also in the set).
         boost::asio::post(ioc, [&]() {
            pClient->resumeChunkProcessing();
         });
         return false;
      }

      outcome.chunks.push_back(chunk);
      return true;
   };

   pClient->execute(
      [&](const http::Response& response) {
         outcome.responseHandlerCalled = true;
         outcome.responseBody = response.body();
         outcome.statusCode = response.statusCode();
         pTimer->cancel();
      },
      [&](const core::Error& error) {
         pTimer->cancel();
      },
      fixedBufferHandler);

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

// --- Step 1 coverage: streaming non-chunked bodies through fixedBufferHandler_ ---
//
// Regression coverage for the rstudio-pro-11740 streaming change: when a
// wiring site opts in via setStreamNonChunkedResponses(true), a non-chunked
// (Content-Length) body must be delivered piece-wise to the FixedBufferHandler
// (with the empty-chunk completion signal), rather than accumulated and
// delivered whole via the ResponseHandler. A header-time buffer predicate
// must still force the legacy whole-body path when set. Backpressure
// (fixedBufferHandler_ returning false) must be respected and resumeChunkProcessing()
// must correctly complete a paused final piece -- the eval-P1 fix.

// The core streaming behavior: a Content-Length body split across multiple
// reads, on a socket the server keeps open (so only the Content-Length byte
// count -- not EOF -- can signal completion, exercising the P1 fix), is
// delivered piece-wise to the fixed buffer handler and completes with the empty
// final chunk. The ResponseHandler must not fire.
TEST(AsyncClientContentLength, StreamsNonChunkedBodyPieceWiseWhenOptedIn)
{
   StreamingOutcome outcome = runStreamingScenario(
      ResponseMode::ContentLengthSplit, /*closeAfterResponse=*/false,
      /*responseBody=*/"{\"name\":\"jsonlite\"}\n");

   EXPECT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.fixedBufferHandlerSawFinal);
   EXPECT_FALSE(outcome.responseHandlerCalled);
   EXPECT_EQ(outcome.statusCode, 200);

   std::string assembled;
   for (const std::string& chunk : outcome.chunks)
      assembled += chunk;
   EXPECT_EQ(assembled, "{\"name\":\"jsonlite\"}\n");

   // the response handed to the fixed buffer handler must still carry the upstream
   // Content-Length header so FixedBufferProxy can choose Content-Length framing.
   EXPECT_EQ(outcome.contentLengthHeaderOnFirstChunk,
             std::to_string(std::string("{\"name\":\"jsonlite\"}\n").size()));
}

// A header-observable buffer predicate (e.g. simulating the SparkUI/Jetty
// gate) must force the legacy full-buffering path even when the wiring site
// opted into streaming: the ResponseHandler fires with the whole body, and
// the FixedBufferHandler is never invoked (not even with the completion signal).
TEST(AsyncClientContentLength, BufferPredicateForcesFullBufferingWhenStreamingOptedIn)
{
   auto alwaysBuffer = [](const http::Response&) { return true; };

   StreamingOutcome outcome = runStreamingScenario(
      ResponseMode::ContentLength, /*closeAfterResponse=*/true,
      /*responseBody=*/"{\"name\":\"jsonlite\"}\n",
      /*bufferPredicate=*/alwaysBuffer);

   EXPECT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.responseHandlerCalled);
   EXPECT_EQ(outcome.responseBody, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(outcome.fixedBufferHandlerSawFinal);
   EXPECT_TRUE(outcome.chunks.empty());
}

// Backpressure: the fixed buffer handler returning false on an interior piece must
// pause delivery; resumeChunkProcessing() must resume and, on the final
// piece, the paused response must still route to completion (the empty
// chunk) rather than another read -- confirming `complete` correctly threads
// through chunkState_ across a pause/resume cycle.
TEST(AsyncClientContentLength, BackpressurePauseAndResumeCompletesStreamedBody)
{
   StreamingOutcome outcome = runStreamingScenario(
      ResponseMode::ContentLengthSplit, /*closeAfterResponse=*/false,
      /*responseBody=*/"{\"name\":\"jsonlite\"}\n",
      /*bufferPredicate=*/boost::function<bool(const http::Response&)>(),
      /*pauseOnChunkIndices=*/{0});

   EXPECT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.fixedBufferHandlerSawFinal);
   EXPECT_FALSE(outcome.responseHandlerCalled);

   std::string assembled;
   for (const std::string& chunk : outcome.chunks)
      assembled += chunk;
   EXPECT_EQ(assembled, "{\"name\":\"jsonlite\"}\n");
}

// Real-world trigger: a large /p/<port>/ download to a slow/congested browser
// connection, where the downstream TCP send buffer fills repeatedly over the
// life of one response rather than just once. Pausing on both delivered data
// pieces of the split body (not just one, as the test above exercises) must
// still thread `complete` through chunkState_ correctly and complete once
// resumed.
TEST(AsyncClientContentLength, MultipleBackpressurePauseResumeCyclesAcrossLongBody)
{
   StreamingOutcome outcome = runStreamingScenario(
      ResponseMode::ContentLengthSplit, /*closeAfterResponse=*/false,
      /*responseBody=*/"{\"name\":\"jsonlite\"}\n",
      /*bufferPredicate=*/boost::function<bool(const http::Response&)>(),
      /*pauseOnChunkIndices=*/{0, 1});

   EXPECT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.fixedBufferHandlerSawFinal);
   EXPECT_FALSE(outcome.responseHandlerCalled);

   std::string assembled;
   for (const std::string& chunk : outcome.chunks)
      assembled += chunk;
   EXPECT_EQ(assembled, "{\"name\":\"jsonlite\"}\n");
}

// Opt-out sites (the default): setting a FixedBufferHandler without opting into
// setStreamNonChunkedResponses must preserve legacy behavior for a
// non-chunked response -- it buffers fully and is delivered via the
// ResponseHandler; the FixedBufferHandler is never invoked.
TEST(AsyncClientContentLength, NonStreamingSiteWithFixedBufferHandlerKeepsLegacyBuffering)
{
   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/true,
                      "{\"name\":\"jsonlite\"}\n");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   // note: setStreamNonChunkedResponses is deliberately NOT called here --
   // this reproduces the /s/ and launcher wiring, which only calls
   // setFixedBufferHandler()/passes a fixedBufferHandler to execute().

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   bool responseHandlerCalled = false;
   bool fixedBufferHandlerCalled = false;
   std::string responseBody;

   pClient->execute(
      [&](const http::Response& response) {
         responseHandlerCalled = true;
         responseBody = response.body();
      },
      [&](const core::Error& error) {},
      [&](const http::Response&, const std::string&) {
         fixedBufferHandlerCalled = true;
         return true;
      });

   ioc.run();
   server.stop();

   EXPECT_TRUE(responseHandlerCalled);
   EXPECT_EQ(responseBody, "{\"name\":\"jsonlite\"}\n");
   EXPECT_FALSE(fixedBufferHandlerCalled);
}

// --- Step 4 coverage: filling gaps left by Step 1's early test additions ---

// The eval-P1 regression guard, specifically for backpressure landing on the
// *final* piece (as opposed to BackpressurePauseAndResumeCompletesStreamedBody
// above, which pauses on an interior piece): a single-write Content-Length
// response over a kept-alive upstream is delivered to the fixed buffer handler as
// exactly one data chunk, which is therefore also the final data chunk before
// the empty completion signal. Pausing there and resuming must still route to
// closeAndRespond() (completion) rather than another readSomeContent() -- the
// `complete` flag must have threaded through chunkState_ across the pause.
TEST(AsyncClientContentLength, BackpressureOnFinalPieceCompletesOverKeptAliveConnection)
{
   StreamingOutcome outcome = runStreamingScenario(
      ResponseMode::ContentLength, /*closeAfterResponse=*/false,
      /*responseBody=*/"{\"name\":\"jsonlite\"}\n",
      /*bufferPredicate=*/boost::function<bool(const http::Response&)>(),
      /*pauseOnChunkIndices=*/{0});

   EXPECT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.fixedBufferHandlerSawFinal);
   EXPECT_FALSE(outcome.responseHandlerCalled);

   std::string assembled;
   for (const std::string& chunk : outcome.chunks)
      assembled += chunk;
   EXPECT_EQ(assembled, "{\"name\":\"jsonlite\"}\n");
}

// Regression guard for Step 1's useFixedBufferHandler()/deliverChunks() refactor: a
// chunked-encoding upstream, with a real FixedBufferHandler set and streaming opted
// in, must still be delivered piece-wise via the pre-existing chunked path
// (processChunks()) exactly as before this change -- streamNonChunkedResponses_
// only gates the *non-chunked* delivery decision (streamResponse_ requires
// `!chunkedEncoding_`), so chunked responses are unaffected by the flag.
TEST(AsyncClientContentLength, ChunkedUnchangedWithFixedBufferHandlerWhenStreamingOptedIn)
{
   StreamingOutcome outcome = runStreamingScenario(
      ResponseMode::Chunked, /*closeAfterResponse=*/false,
      /*responseBody=*/"{\"name\":\"jsonlite\"}\n");

   EXPECT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.fixedBufferHandlerSawFinal);
   EXPECT_FALSE(outcome.responseHandlerCalled);

   std::string assembled;
   for (const std::string& chunk : outcome.chunks)
      assembled += chunk;
   EXPECT_EQ(assembled, "{\"name\":\"jsonlite\"}\n");
}

// Regression guard for the completionPending_ fix (rstudio-pro-11740
// follow-up): closeAndRespond()'s completion signal (fixedBufferHandler_(response_,
// "")) is the one chunk delivery that bypasses deliverChunks()/chunkState_,
// calling the handler directly and, before this fix, discarding its return
// value. If the consumer (FixedBufferProxy, in production) declines that call under
// backpressure -- e.g. its outbound buffer happens to be exactly full at the
// instant the body finishes -- the pause request was previously dropped on
// the floor: nothing recorded that completion still needed to be sent, so a
// later resumeChunkProcessing() call had no saved state to act on and the
// response never completed, leaving both proxied connections open forever.
// This test simulates exactly that: the fixed buffer handler declines only the
// empty completion chunk (accepting all real data normally), then signals
// "drained" via resumeChunkProcessing() as FixedBufferProxy would once its write
// buffer empties. Before the fix, this hangs until the test's own timeout
// fires (outcome.timedOut) with fixedBufferHandlerSawFinal still false; after the
// fix, the completion signal is retried and delivered exactly once.
TEST(AsyncClientContentLength, BackpressureOnCompletionSignalStillCompletes)
{
   StreamingOutcome outcome = runStreamingScenario(
      ResponseMode::ContentLength, /*closeAfterResponse=*/false,
      /*responseBody=*/"{\"name\":\"jsonlite\"}\n",
      /*bufferPredicate=*/boost::function<bool(const http::Response&)>(),
      /*pauseOnChunkIndices=*/{},
      /*pauseOnFinalSignal=*/true);

   EXPECT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.fixedBufferHandlerSawFinal);
   EXPECT_FALSE(outcome.responseHandlerCalled);

   std::string assembled;
   for (const std::string& chunk : outcome.chunks)
      assembled += chunk;
   EXPECT_EQ(assembled, "{\"name\":\"jsonlite\"}\n");
}

// Guards against a second bug the completionPending_ fix above could have
// introduced: a `false` return from the completion (empty) chunk is
// overloaded across FixedBufferHandler consumers. FixedBufferProxy returns false to mean
// "temporary backpressure, resume me later" and leaves the connection open.
// But RPC-style streaming consumers elsewhere in the codebase -- e.g.
// LauncherClient::fixedBufferHandler and sendMethodToSession's onChunk wrapper in
// SessionPidToContext.hpp -- also return false on the empty chunk, but to
// mean "the stream is done," and they close the connection themselves,
// synchronously, as part of that very call. Those consumers never call
// resumeChunkProcessing() (there is no FixedBufferProxy in that path to do so), so
// if closeAndRespond() treated their "done" the same as FixedBufferProxy's "pause,"
// completionPending_ would be set and never cleared, permanently skipping
// disableHandlers() on every ordinary completion of this kind of streaming
// RPC call. The fix requires opting in via setFixedBufferHandlerSupportsPause()
// (which only FixedBufferProxy::proxy() does) rather than inferring intent from
// connection state -- a `false` return is otherwise discarded exactly as it
// was before this fix, which is what this test exercises: no call to
// setFixedBufferHandlerSupportsPause() here, deliberately, to reproduce the
// RPC-style consumer's wiring (close() from within the handler, then return
// false). Confirms disableHandlers() still runs promptly -- observed via a
// sentinel captured alongside the fixed buffer handler lambda, whose reference
// count should drop back to 1 once fixedBufferHandler_ is cleared, rather than
// lingering for as long as pClient itself stays alive.
TEST(AsyncClientContentLength, CompletionSignalDeclinedWithoutPauseSupportStillReleasesHandlers)
{
   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/false,
                      "{\"name\":\"jsonlite\"}\n");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   pClient->setStreamNonChunkedResponses(true);
   // deliberately NOT calling setFixedBufferHandlerSupportsPause(true) -- this
   // reproduces LauncherClient/sendMethodToSession's wiring, which only ever
   // calls setFixedBufferHandler()

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   boost::shared_ptr<int> pSentinel = boost::make_shared<int>(0);

   pClient->execute(
      [&](const http::Response&) {},
      [&](const core::Error&) {},
      [&pClient, pSentinel](const http::Response&, const std::string& chunk) -> bool
      {
         if (chunk.empty())
         {
            // mirrors LauncherClient::fixedBufferHandler / sendMethodToSession's
            // onChunk wrapper: close synchronously, then signal "done".
            pClient->close();
            return false;
         }
         return true;
      });

   ioc.run();
   server.stop();

   EXPECT_EQ(pSentinel.use_count(), 1);
}

// --- Known gaps, characterized rather than fixed for now ---
//
// The following tests document CURRENT (accepted, not ideal) AsyncClient
// behavior for backends that misbehave relative to their own declared
// Content-Length. They are deliberately written to assert what the code
// actually does today, not what it ideally should do, so a future fix has a
// clear baseline and these gaps stay visible in the suite rather than silent.
// See the project plan for the full rationale on why these are left as-is
// for now rather than fixed alongside the other tests in this file.

// Real-world trigger: an rsession or localhost app process (Shiny, Jetty)
// crashes or is OOM-killed mid-response after emitting headers and part of a
// Content-Length-declared body; the OS delivers an orderly FIN on process
// exit. AsyncClient::handleReadContent's EOF branch (predates this streaming
// work -- confirmed via git history) calls closeAndRespond() unconditionally
// on EOF without checking responseBodyComplete(), so this is silently
// accepted as a complete, successful (if short) response today.
TEST(AsyncClientContentLength, TruncatedContentLengthBodyReportsErrorNotSilentSuccess)
{
   std::string body = "{\"name\":\"jsonlite\"}\n";
   Outcome outcome = runScenario(ResponseMode::TruncatedContentLength,
                                 /*closeAfterResponse=*/true,
                                 body);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_FALSE(outcome.gotError);
   EXPECT_EQ(outcome.body, body.substr(0, body.size() / 2));
   EXPECT_FALSE(outcome.timedOut);
}

// Same gap as above, but on the streaming code path proxyLocalhostRequest()
// actually uses live today for /p/<port>/ downloads: the fixed buffer handler
// sees a normal completion signal, not an error, even though
// contentLengthStreamed_ never reached the declared Content-Length.
TEST(AsyncClientContentLength, StreamedTruncatedContentLengthBodyReportsErrorNotSilentSuccess)
{
   std::string body = "{\"name\":\"jsonlite\"}\n";
   StreamingOutcome outcome = runStreamingScenario(
      ResponseMode::TruncatedContentLength, /*closeAfterResponse=*/true, body);

   EXPECT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.fixedBufferHandlerSawFinal);
   EXPECT_FALSE(outcome.responseHandlerCalled);

   std::string delivered;
   for (const std::string& chunk : outcome.chunks)
      delivered += chunk;
   EXPECT_EQ(delivered, body.substr(0, body.size() / 2));
}

// Real-world trigger: a backend's Content-Length header is stale or
// miscalculated relative to what it actually writes (e.g. a template engine
// appending trailing bytes after computing the header). ResponseParser::
// appendToBody() appends whatever is currently in responseBuffer_
// unconditionally, without truncating to the declared Content-Length, so any
// excess bytes that arrive in the same read as the declared body are
// included in the delivered body rather than stopped at the declared
// boundary.
TEST(AsyncClientContentLength, BodyLongerThanDeclaredContentLengthStopsAtDeclaredBoundary)
{
   std::string body = "{\"name\":\"jsonlite\"}\n";
   Outcome outcome = runScenario(ResponseMode::OverLongContentLength,
                                 /*closeAfterResponse=*/false,
                                 body);

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_FALSE(outcome.timedOut);
   // Documents today's actual behavior: the excess bytes beyond the declared
   // Content-Length are forwarded as part of the body, not dropped.
   EXPECT_EQ(outcome.body, body + "EXTRA-BYTES-BEYOND-DECLARED-CONTENT-LENGTH");
}

// A minimal fake standing in for the downstream (browser) AsyncConnection,
// used only by UpstreamErrorArrivesAfterFixedBufferProxyHasFlushedHeaders
// below. Posts its write completions to the same (real) io_context driving
// the real FixedBufferProxy + TcpIpAsyncClient under test, so everything
// drains together via a single ioc.run(). Deliberately does NOT implement
// the AsyncConnectionImpl::sendingResponse_ double-response guard -- that
// guard is unit-tested directly against the real AsyncConnectionImpl class
// in AsyncConnectionImplTests.cpp (WriteResponseAfterHeadersAlreadyWrittenIsANoOp).
// This fake exists only to prove the hazardous *interaction* actually
// reaches a second write attempt in the first place.
class FakeDownstreamConnection : public AsyncConnection
{
public:
   explicit FakeDownstreamConnection(boost::asio::io_context& ioc) : ioc_(ioc), strand_(ioc) {}

   boost::asio::io_context& ioContext() override { return ioc_; }
   const http::Request& request() const override { return request_; }
   http::Response& response() override { return response_; }

   void writeResponse(bool, Socket::Handler handler) override
   {
      ++writeResponseCount_;
      boost::asio::post(ioc_, [handler]() { handler(boost::system::error_code(), 0); });
   }

   void writeResponse(const http::Response&, bool, const http::Headers&,
                       Socket::Handler handler) override
   {
      ++writeResponseCount_;
      boost::asio::post(ioc_, [handler]() { handler(boost::system::error_code(), 0); });
   }

   void writeResponseHeaders(Socket::Handler handler) override
   {
      ++writeResponseHeadersCount_;
      boost::asio::post(ioc_, [handler]() { handler(boost::system::error_code(), 0); });
   }

   void writeError(const Error& error) override
   {
      response_.setError(error);
      writeResponse(true, Socket::NullHandler);
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
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}

   void asyncWrite(const boost::asio::const_buffer& buffer, Socket::Handler handler) override
   {
      std::size_t n = buffer.size();
      boost::asio::post(ioc_, [handler, n]() { handler(boost::system::error_code(), n); });
   }

   void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers,
                    Socket::Handler handler) override
   {
      std::size_t total = 0;
      for (const auto& buffer : buffers)
         total += buffer.size();
      boost::asio::post(ioc_, [handler, total]() { handler(boost::system::error_code(), total); });
   }

   int writeResponseCount_ = 0;
   int writeResponseHeadersCount_ = 0;
   bool closed_ = false;

private:
   boost::asio::io_context& ioc_;
   boost::asio::io_context::strand strand_;
   http::Request request_;
   http::Response response_;
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
};

// Real-world trigger: an rsession/Shiny process behind /p/<port>/ is killed
// mid-stream (segfault, kill -9, OOM) after the browser has already started
// receiving bytes. By the time AsyncClient::handleError() (triggered here by
// the hard reset, not a clean EOF) invokes the site's ErrorHandler,
// FixedBufferProxy has already called writeResponseHeaders() directly on the
// downstream connection. In production, the ErrorHandler
// (ServerSessionProxy::handleLocalhostError) calls
// writeResponse()/writeError() on that same connection unconditionally --
// this proves that hazard is real and reachable (both writes are actually
// attempted); AsyncConnectionImplTests.cpp separately proves the
// sendingResponse_ guard added to the real connection class prevents it from
// becoming a second, malformed response on the wire.
TEST(AsyncClientContentLength, UpstreamErrorArrivesAfterFixedBufferProxyHasFlushedHeaders)
{
   std::string body = "{\"name\":\"jsonlite\"}\n";
   LocalServer server(ResponseMode::PartialBodyThenHardReset, /*closeAfterResponse=*/true, body);
   server.start();

   boost::asio::io_context ioc;

   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));
   pClient->setStreamNonChunkedResponses(true);

   boost::shared_ptr<FakeDownstreamConnection> pConnection =
      boost::make_shared<FakeDownstreamConnection>(ioc);
   pClient->setStrand(&pConnection->getStrand());

   boost::shared_ptr<FixedBufferProxy> pProxy =
      boost::make_shared<FixedBufferProxy>(pConnection);
   pProxy->proxy(pClient);

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   bool timedOut = false;
   boost::shared_ptr<boost::asio::system_timer> pTimer =
      boost::make_shared<boost::asio::system_timer>(ioc, std::chrono::seconds(4));
   pTimer->async_wait([&](const boost::system::error_code& ec) {
      if (ec == boost::asio::error::operation_aborted)
         return;
      timedOut = true;
      pClient->close();
   });

   pClient->execute(
      [&](const http::Response&) { pTimer->cancel(); },
      // mirrors ServerSessionProxy::handleLocalhostError: on an upstream
      // error, write an error response directly to the downstream connection,
      // unconditionally.
      [&](const core::Error& error) {
         pConnection->writeError(error);
         pTimer->cancel();
      });

   ioc.run();
   server.stop();

   EXPECT_FALSE(timedOut);
   // FixedBufferProxy already started a response (headers written) before
   // the reset was detected, and the ErrorHandler's writeError() call above
   // is genuinely reached and attempts a second, whole response on the same
   // connection -- confirming this fake (without a guard) would produce a
   // malformed double response. The real AsyncConnectionImpl prevents this;
   // see AsyncConnectionImplTests.cpp.
   EXPECT_EQ(pConnection->writeResponseHeadersCount_, 1);
   EXPECT_EQ(pConnection->writeResponseCount_, 1);
}

// Real-world trigger: /p/<port>/ proxies real HEAD requests against Jetty/
// Shiny content, some of which may have framework bugs that still emit a
// body. AsyncClient has no method-awareness (confirmed: no reference to
// "HEAD" anywhere in AsyncClient.hpp) -- this predates the streaming feature
// and applies identically to the buffered path, so a body accompanying a
// HEAD response is delivered like any other Content-Length body rather than
// suppressed.
TEST(AsyncClientContentLength, HeadRequestWithStrayUpstreamBodyBytesIsNotForwarded)
{
   std::string body = "{\"name\":\"jsonlite\"}\n";
   Outcome outcome = runScenario(ResponseMode::ContentLength,
                                 /*closeAfterResponse=*/true,
                                 body,
                                 /*requestTimeout=*/boost::posix_time::pos_infin,
                                 /*requestMethod=*/"HEAD");

   EXPECT_TRUE(outcome.gotResponse);
   EXPECT_FALSE(outcome.timedOut);
   // Documents today's actual behavior: the stray body is forwarded despite
   // the request being HEAD.
   EXPECT_EQ(outcome.body, body);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
