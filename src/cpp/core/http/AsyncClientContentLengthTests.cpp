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
#include <functional>
#include <chrono>
#include <set>
#include <sstream>
#include <string>
#include <thread>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/asio/ip/tcp.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/system_timer.hpp>
#include <boost/asio/write.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/make_shared.hpp>
#include <boost/system/error_code.hpp>
#include <boost/thread/lock_guard.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/weak_ptr.hpp>

#include <core/http/AsyncConnection.hpp>
#include <core/http/FixedBufferProxy.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/TcpIpAsyncClient.hpp>
#include <core/http/Util.hpp>

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
   PartialBodyThenHardReset,

   // Writes the caller-supplied bytes verbatim as the entire response, headers
   // included. The only way to exercise the malformed/unusual framing headers
   // a user's own app behind /p/<port>/ can emit -- an exact wire image is the
   // point, so nothing here may normalize it.
   Raw
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

         case ResponseMode::Raw:
         {
            // body_ carries the whole response image, headers included
            boost::asio::write(socket, boost::asio::buffer(body_), ec);
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

// Minimal stand-in for FixedBufferProxy: registers itself into
// fixedBufferHandler_ via shared_from_this() (so it's kept alive solely by
// that binding, with no other strong reference -- matching the real
// production reference graph), and owns a mutex it locks around its
// close()/disableHandlers() call, mirroring FixedBufferProxy's own
// LOCK_MUTEX(mutex_)-protected close sites. Used to confirm disableHandlers()
// doesn't destroy a caller (and any lock it holds) out from under itself.
class FakeStreamingConsumer : public boost::enable_shared_from_this<FakeStreamingConsumer>
{
public:
   explicit FakeStreamingConsumer(const boost::shared_ptr<TcpIpAsyncClient>& pClient)
      : pClient_(pClient)
   {
   }

   void wire()
   {
      pClient_->setFixedBufferHandlerSupportsPause(true);
      boost::shared_ptr<FakeStreamingConsumer> self = shared_from_this();
      pClient_->setFixedBufferHandler(
         [self](const http::Response& response, const std::string& chunk) -> bool
         {
            return self->onChunk(response, chunk);
         });
   }

   bool onChunk(const http::Response&, const std::string&)
   {
      // Mirrors FixedBufferProxy::handleError()/closeConnections() exactly:
      // close(), then disableHandlers(), both synchronously, from within
      // this very call, while holding our own mutex -- the way
      // onChunkWrote()/writeChunk() do around their own close sites.
      boost::lock_guard<boost::mutex> lock(mutex_);
      pClient_->close();
      pClient_->disableHandlers();
      return true;
   }

   boost::shared_ptr<TcpIpAsyncClient> pClient_;
   boost::mutex mutex_;
};

// Pump the loop until `predicate` holds, bounded so a wrong expectation fails
// the test rather than hanging it. poll() rather than run(): the scenarios
// below use a server that accepts and never replies, so the client's read
// never completes and run() would block forever.
bool pollUntil(boost::asio::io_context& ioc, const std::function<bool()>& predicate)
{
   for (int i = 0; i < 2000; i++)
   {
      if (predicate())
         return true;
      ioc.restart();
      ioc.poll();
      std::this_thread::sleep_for(std::chrono::milliseconds(1));
   }
   return predicate();
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

// Regression test for a roborev finding on FixedBufferProxy::closeConnections():
// it calls pServerConnection_->close() followed by disableHandlers(), both
// synchronously, from within its own handleError() -- which itself runs from
// inside the very fixedBufferHandler_ call this AsyncClient is invoking (or,
// for the other three close sites, from inside a LOCK_MUTEX(mutex_) block
// reached via one of FixedBufferProxy's own asyncWrite completion handlers).
// disableHandlers() clears fixedBufferHandler_, the boost::function whose
// bound state holds the only shared_ptr keeping a consumer like
// FixedBufferProxy alive; clearing it inline, mid-call through it, would
// destroy that consumer (and any locked mutex it owns) out from under
// itself.
//
// FakeStreamingConsumer reproduces the real production reference graph
// exactly (no extra keep-alive shared_ptr the way capturing pClient by
// reference and a bare sentinel int would give): it is owned solely by the
// binding inside fixedBufferHandler_, and it locks its own mutex around the
// close()/disableHandlers() call the way FixedBufferProxy's own close sites
// do. A weak_ptr confirms it survives the synchronous call (not destroyed
// out from under its own still-executing method and locked mutex) and is
// only destroyed once the deferred cleanup actually runs.
TEST(AsyncClientContentLength, DisableHandlersIsSafeToCallFromWithinTheHandlerItClears)
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

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   boost::weak_ptr<FakeStreamingConsumer> weakConsumer;
   {
      boost::shared_ptr<FakeStreamingConsumer> pConsumer =
         boost::make_shared<FakeStreamingConsumer>(pClient);
      pConsumer->wire();
      weakConsumer = pConsumer;
      // pConsumer goes out of scope here -- the only remaining strong
      // reference is the one bound inside pClient's fixedBufferHandler_.
   }
   ASSERT_FALSE(weakConsumer.expired());

   pClient->execute([&](const http::Response&) {}, [&](const core::Error&) {});

   ioc.run();
   server.stop();

   EXPECT_TRUE(weakConsumer.expired());
}

// Concurrent-disable regression test, pinning down disableHandlers()'s
// cross-thread contract (see its declaration): called from a foreign thread
// while a handler invocation is in flight -- the /s/ shape, where
// FixedBufferProxy's write-completion path runs on the downstream
// connection's strand, not this client's -- it must (a) return promptly
// WITHOUT waiting for the in-flight invocation (a wait-based design
// deadlocks in production, since the callback can be blocked on a lock the
// disabling caller holds; here it would hang this test), (b) let that
// already-admitted invocation complete safely on its own copy of the
// handler, and (c) admit no further invocations afterward (in particular,
// the completion signal closeAndRespond() would otherwise deliver).
TEST(AsyncClientContentLength, DisableHandlersFromForeignThreadDoesNotBlockOnInFlightCallback)
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

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   std::atomic<bool> inCallback(false);
   std::atomic<bool> disableReturned(false);
   std::atomic<bool> callbackTimedOut(false);
   std::atomic<int> invocationsAfterDisable(0);

   pClient->execute(
      [&](const http::Response&) {},
      [&](const core::Error&) {},
      [&](const http::Response&, const std::string&) -> bool
      {
         if (disableReturned.load())
         {
            // admission after disableHandlers() returned -- contract breach
            ++invocationsAfterDisable;
            return true;
         }

         // signal the foreign thread, then block in-invocation until its
         // disableHandlers() call has returned (bounded so a regression
         // fails rather than hangs the suite)
         inCallback.store(true);
         for (int i = 0; i < 5000 && !disableReturned.load(); ++i)
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
         if (!disableReturned.load())
            callbackTimedOut.store(true);
         return true;
      });

   std::thread foreignThread([&]()
   {
      for (int i = 0; i < 5000 && !inCallback.load(); ++i)
         std::this_thread::sleep_for(std::chrono::milliseconds(1));
      if (!inCallback.load())
         return; // callback never ran; the main-thread assertions will fail

      // the invocation is in flight on the io thread; this must not block
      // waiting for it
      pClient->disableHandlers();
      disableReturned.store(true);
   });

   ioc.run();
   foreignThread.join();
   server.stop();

   EXPECT_TRUE(inCallback.load());
   EXPECT_TRUE(disableReturned.load());
   EXPECT_FALSE(callbackTimedOut.load());
   EXPECT_EQ(invocationsAfterDisable.load(), 0);
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
      writtenHeaders_.assign(response_);
      boost::asio::post(ioc_, [handler]() { handler(boost::system::error_code(), 0); });
   }

   // the claim-before-assign overload FixedBufferProxy uses; mirrors the real
   // implementation by assigning the handed-over response in before writing
   void writeResponseHeaders(const http::Response& response, Socket::Handler handler) override
   {
      response_.assign(response);
      writeResponseHeaders(handler);
   }

   void writeError(const Error& error) override
   {
      response_.setError(error);
      writeResponse(true, Socket::NullHandler);
   }

   void close() override
   {
      closed_ = true;

      // FixedBufferProxy closing this connection is the end of the exchange,
      // on both the success and error paths -- and it is the only completion
      // signal a test can rely on here, since closeConnections() also calls
      // disableHandlers(), which clears the ResponseHandler the site passed to
      // execute(). Waiting on that handler instead just waits out the timeout.
      if (onClosed_)
         onClosed_();
   }

   void continueParsing() override {}
   void setData(const boost::any& data) override { data_ = data; }
   boost::any getData() override { return data_; }
   const std::string& username() const override { return username_; }
   void setUsername(const std::string& username) override { username_ = username; }
   const std::string& handlerPrefix() const override { return handlerPrefix_; }
   void setHandlerPrefix(const std::string& prefix) override { handlerPrefix_ = prefix; }
   boost::asio::io_context::strand& getStrand() override { return strand_; }

   // invoked from close(); see there
   std::function<void()> onClosed_;

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}

   void asyncWrite(const boost::asio::const_buffer& buffer, Socket::Handler handler) override
   {
      std::size_t n = buffer.size();
      writtenBytes_.append(static_cast<const char*>(buffer.data()), n);
      boost::asio::post(ioc_, [handler, n]() { handler(boost::system::error_code(), n); });
   }

   void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers,
                    Socket::Handler handler) override
   {
      std::size_t total = 0;
      for (const auto& buffer : buffers)
      {
         writtenBytes_.append(static_cast<const char*>(buffer.data()), buffer.size());
         total += buffer.size();
      }
      boost::asio::post(ioc_, [handler, total]() { handler(boost::system::error_code(), total); });
   }

   int writeResponseCount_ = 0;
   int writeResponseHeadersCount_ = 0;
   bool closed_ = false;

   // the body bytes FixedBufferProxy actually put on the downstream wire, and
   // the headers it flushed ahead of them -- what the double-chunking tests
   // below have to inspect, since the defect is invisible from call counts
   std::string writtenBytes_;
   http::Response writtenHeaders_;

private:
   boost::asio::io_context& ioc_;
   boost::asio::io_context::strand strand_;
   http::Request request_;
   http::Response response_;
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
};

// --- RFC 7230 3.3.1: "A sender MUST NOT apply chunked more than once to a
// message body" ------------------------------------------------------------
//
// AsyncClient decides whether to de-chunk; FixedBufferProxy decides how to
// frame what it is handed. Those decisions used to be two independent string
// comparisons against "chunked", so an upstream could put them out of step and
// get a body that was chunk-framed on the wire, never de-chunked, and then
// wrapped in a second chunk layer on the way to the browser -- which de-chunks
// once and renders the inner chunk-size lines as content.
//
// These drive the whole real path (socket -> TcpIpAsyncClient -> the real
// FixedBufferProxy -> downstream connection) rather than calling queueChunk()
// directly, because the defect lives precisely in the handoff between the two.

namespace {

// Outcome of running one raw upstream response image through the real
// AsyncClient + FixedBufferProxy pair.
// (http::Response is noncopyable, so the two framing headers under test are
// lifted out by value rather than carrying the whole response object.)
struct ProxiedOutcome
{
   std::string body;             // bytes FixedBufferProxy wrote downstream
   std::string transferEncoding; // framing headers it flushed ahead of them
   std::string contentLength;
   int statusCode = 0;           // status of the response AsyncClient settled on
   bool gotError = false;        // the site's ErrorHandler fired
   bool timedOut = false;
};

ProxiedOutcome proxyRawUpstreamResponse(const std::string& rawResponse)
{
   ProxiedOutcome outcome;

   LocalServer server(ResponseMode::Raw, /*closeAfterResponse=*/true, rawResponse);
   server.start();

   boost::asio::io_context ioc;

   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   // mirrors the localhost port-proxy wiring in ServerSessionProxy, buffer
   // predicate included -- without it a 101 would take the streaming path here
   // and never reach the websocket-upgrade handling it gets in production
   pClient->setStreamNonChunkedResponses(true);
   pClient->setBufferPredicate([](const http::Response& response) {
      return response.statusCode() == http::status::SwitchingProtocols ||
             !response.headerValue("Location").empty() ||
             !response.headerValue("Refresh").empty() ||
             boost::algorithm::contains(response.headerValue("Server"), "Jetty");
   });

   boost::shared_ptr<FakeDownstreamConnection> pConnection =
      boost::make_shared<FakeDownstreamConnection>(ioc);
   pClient->setStrand(&pConnection->getStrand());

   boost::shared_ptr<FixedBufferProxy> pProxy =
      boost::make_shared<FixedBufferProxy>(pConnection);
   pProxy->proxy(pClient);

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");

   boost::shared_ptr<boost::asio::system_timer> pTimer =
      boost::make_shared<boost::asio::system_timer>(ioc, std::chrono::seconds(4));
   pTimer->async_wait([&](const boost::system::error_code& ec) {
      if (ec == boost::asio::error::operation_aborted)
         return;
      outcome.timedOut = true;
      pClient->close();
   });

   pConnection->onClosed_ = [&]() { pTimer->cancel(); };

   pClient->execute(
      [&](const http::Response& response) {
         outcome.statusCode = response.statusCode();
         pTimer->cancel();
      },
      [&](const core::Error&) { outcome.gotError = true; pTimer->cancel(); });

   ioc.run();
   server.stop();

   outcome.body = pConnection->writtenBytes_;
   outcome.transferEncoding = pConnection->writtenHeaders_.headerValue(kTransferEncoding);
   outcome.contentLength = pConnection->writtenHeaders_.headerValue("Content-Length");

   // Where the status comes from depends on which path ran. On the streaming
   // path FixedBufferProxy writes the headers itself and then calls
   // disableHandlers(), so the ResponseHandler set below never fires and the
   // only record of the status is what went downstream. On the buffered path
   // (the predicate above matched) FixedBufferProxy is never involved, and the
   // ResponseHandler is the only source.
   if (pConnection->writeResponseHeadersCount_ > 0)
      outcome.statusCode = pConnection->writtenHeaders_.statusCode();

   return outcome;
}

} // anonymous namespace

TEST(AsyncClientContentLength, ChunkedUpstreamWithContentLengthIsNotChunkedTwice)
{
   // The header pair RFC 7230 3.3.3 rule 3 calls out as a possible smuggling
   // attempt: Transfer-Encoding must win, the Content-Length must be ignored
   // for framing and removed before forwarding. AsyncClient used to require
   // contentLength() == 0 before de-chunking, so it read the chunk-framed
   // bytes as an opaque Content-Length body while FixedBufferProxy -- seeing
   // the same "chunked" header -- wrapped them again.
   std::string payload = "{\"name\":\"jsonlite\"}\n";
   std::ostringstream raw;
   raw << "HTTP/1.1 200 OK\r\n"
          "Content-Type: application/x-ndjson\r\n"
          "Transfer-Encoding: chunked\r\n"
       << "Content-Length: " << payload.size() << "\r\n"
          "\r\n"
       << std::hex << payload.size() << "\r\n" << payload << "\r\n"
       << "0\r\n\r\n";

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);

   // exactly one chunk layer: the payload, then the terminator
   std::string expected = http::util::formatMessageAsHttpChunk(payload) +
                          http::util::formatMessageAsHttpChunk("");
   EXPECT_EQ(outcome.body, expected);

   // rule 3 again: the Content-Length must not travel on with the response
   EXPECT_TRUE(outcome.contentLength.empty());
   EXPECT_EQ(outcome.transferEncoding, kChunkedTransferEncoding);
}

TEST(AsyncClientContentLength, ChunkedUpstreamSpelledWithCapitalCIsNotChunkedTwice)
{
   // Transfer-coding names are case-insensitive tokens (RFC 7230 4). The old
   // exact compare read "Chunked" as "not chunked" on both sides at once, so
   // the raw chunk-framed bytes were relayed and then re-chunked.
   std::string payload = "hello chunked world";
   std::ostringstream raw;
   raw << "HTTP/1.1 200 OK\r\n"
          "Content-Type: text/plain\r\n"
          "Transfer-Encoding: Chunked\r\n"
          "\r\n"
       << std::hex << payload.size() << "\r\n" << payload << "\r\n"
       << "0\r\n\r\n";

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_EQ(outcome.body,
             http::util::formatMessageAsHttpChunk(payload) +
                http::util::formatMessageAsHttpChunk(""));
}

TEST(AsyncClientContentLength, ChunkedUpstreamWrittenWithoutOwsIsNotChunkedTwice)
{
   // "Transfer-Encoding:chunked" -- legal per RFC 7230 3.2, since the OWS after
   // the colon is optional. parseHeader() used to drop the field outright, so
   // nothing downstream knew the body was chunk-framed and it was re-chunked.
   std::string payload = "no optional whitespace here";
   std::ostringstream raw;
   raw << "HTTP/1.1 200 OK\r\n"
          "Content-Type: text/plain\r\n"
          "Transfer-Encoding:chunked\r\n"
          "\r\n"
       << std::hex << payload.size() << "\r\n" << payload << "\r\n"
       << "0\r\n\r\n";

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_EQ(outcome.body,
             http::util::formatMessageAsHttpChunk(payload) +
                http::util::formatMessageAsHttpChunk(""));
}

TEST(AsyncClientContentLength, UndecodableTransferCodingFailsClosedRatherThanCorrupting)
{
   // "gzip, chunked" is chunk-framed, but de-chunking leaves gzip-encoded
   // bytes rather than the payload. There is no way to hand those on honestly:
   // relabelling them as a plain body corrupts the response, and the old code
   // did worse still, re-chunking them under a header that claimed plain
   // chunked. Refuse the response instead.
   std::string payload = "not really gzip";
   std::ostringstream raw;
   raw << "HTTP/1.1 200 OK\r\n"
          "Content-Type: text/plain\r\n"
          "Transfer-Encoding: gzip, chunked\r\n"
          "\r\n"
       << std::hex << payload.size() << "\r\n" << payload << "\r\n"
       << "0\r\n\r\n";

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.gotError);
   EXPECT_TRUE(outcome.body.empty());
}

TEST(AsyncClientContentLength, BodyChunkedTwiceByTheUpstreamFailsClosed)
{
   // "chunked, chunked" is a body the upstream really did chunk twice, which
   // RFC 7230 3.3.1 forbids it from doing. Every signal short of counting the
   // codings says "ordinary chunked", so de-chunking once and re-framing would
   // hand the browser a body that still has a chunk layer inside it -- the same
   // corruption this whole change exists to prevent, arrived at from the other
   // direction. Refuse it.
   std::string payload = "chunked twice over";
   std::string innerBody = http::util::formatMessageAsHttpChunk(payload) +
                           http::util::formatMessageAsHttpChunk("");

   std::ostringstream raw;
   raw << "HTTP/1.1 200 OK\r\n"
          "Content-Type: text/plain\r\n"
          "Transfer-Encoding: chunked, chunked\r\n"
          "\r\n"
       << http::util::formatMessageAsHttpChunk(innerBody)
       << http::util::formatMessageAsHttpChunk("");

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.gotError);
   EXPECT_TRUE(outcome.body.empty());
}

TEST(AsyncClientContentLength, OrdinaryChunkedUpstreamStillGetsExactlyOneChunkLayer)
{
   // The control: the common, well-formed case must be unchanged by all of the
   // above -- de-chunked once by AsyncClient, chunked once by FixedBufferProxy.
   std::string payload = "{\"name\":\"jsonlite\"}\n";
   std::ostringstream raw;
   raw << "HTTP/1.1 200 OK\r\n"
          "Content-Type: application/x-ndjson\r\n"
          "Transfer-Encoding: chunked\r\n"
          "\r\n"
       << std::hex << payload.size() << "\r\n" << payload << "\r\n"
       << "0\r\n\r\n";

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_FALSE(outcome.gotError);
   EXPECT_EQ(outcome.body,
             http::util::formatMessageAsHttpChunk(payload) +
                http::util::formatMessageAsHttpChunk(""));
}

// A response is allowed to carry no header fields at all, and
// ResponseParser::parseStatusLine() has already consumed the status line's
// CRLF by the time the header block is read -- so all that remains of such a
// response is its lone terminating CRLF.

TEST(AsyncClientContentLength, ResponseWithNoHeaderFieldsAtAllIsParsed)
{
   // A response can legitimately carry zero header fields, and
   // parseStatusLine() has already consumed the status line's CRLF by the time
   // the header block is read -- so all that is left is the lone terminating
   // CRLF. Both the read (which searched for "\r\n\r\n") and the parse (which
   // skips a leading blank line as whitespace) used to run straight past it,
   // so this response never arrived at all: the read waited for a terminator
   // that never came and ended in an EOF error.
   //
   // Independent of the 1xx handling below, but that handling cannot work
   // without it, since a 100 Continue is normally exactly this shape.
   ProxiedOutcome outcome =
      proxyRawUpstreamResponse("HTTP/1.1 204 No Content\r\n\r\n");

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_FALSE(outcome.gotError);
   EXPECT_EQ(outcome.statusCode, 204);

   // 204 takes FixedBufferProxy's no-body framing: headers only
   EXPECT_TRUE(outcome.body.empty());
   EXPECT_TRUE(outcome.transferEncoding.empty());
}

// --- RFC 7231 6.2: "A client MUST be able to parse one or more 1xx responses
// received prior to a final response, even if the client does not expect one."
// ---------------------------------------------------------------------------
//
// A 1xx is interim: no body, and another response follows it on the same
// connection. AsyncClient used to take the first status line it saw as the
// final response, so the real response arrived as the interim one's body and
// was never delivered as a response at all.

TEST(AsyncClientContentLength, SkipsEarlyHintsAndDeliversTheFinalResponse)
{
   // 103 Early Hints, unprompted, followed by the real response -- the shape a
   // growing number of servers emit. Previously the caller got a 103 whose
   // "body" was the literal text of the 200.
   std::string payload = "{\"name\":\"jsonlite\"}\n";
   std::ostringstream raw;
   raw << "HTTP/1.1 103 Early Hints\r\n"
          "Link: </style.css>; rel=preload; as=style\r\n"
          "\r\n"
          "HTTP/1.1 200 OK\r\n"
          "Content-Type: application/x-ndjson\r\n"
       << "Content-Length: " << payload.size() << "\r\n"
          "\r\n"
       << payload;

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_FALSE(outcome.gotError);

   // the 200's body, framed by its own Content-Length -- and none of the
   // interim response's headers carried over onto it
   EXPECT_EQ(outcome.body, payload);
   EXPECT_EQ(outcome.contentLength, std::to_string(payload.size()));
   EXPECT_TRUE(outcome.transferEncoding.empty());
}

TEST(AsyncClientContentLength, SkipsContinueAndDeliversTheFinalResponse)
{
   // 100 Continue: reachable because nothing strips a client's
   // "Expect: 100-continue" before the request is proxied upstream.
   std::string payload = "created";
   std::ostringstream raw;
   raw << "HTTP/1.1 100 Continue\r\n"
          "\r\n"
          "HTTP/1.1 201 Created\r\n"
       << "Content-Length: " << payload.size() << "\r\n"
          "\r\n"
       << payload;

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_FALSE(outcome.gotError);
   EXPECT_EQ(outcome.body, payload);
}

TEST(AsyncClientContentLength, SkipsSeveralInterimResponsesInARow)
{
   // "one or more" -- the skip has to loop, not just handle a single interim
   // response. Also covers an interim response arriving with the final one in
   // the same read: everything after the first header block is already sitting
   // in the read buffer, so the re-read must find it there rather than waiting
   // on the socket for bytes that have already arrived.
   std::string payload = "after three hints";
   std::ostringstream raw;
   raw << "HTTP/1.1 103 Early Hints\r\nLink: </a.css>; rel=preload\r\n\r\n"
          "HTTP/1.1 103 Early Hints\r\nLink: </b.css>; rel=preload\r\n\r\n"
          "HTTP/1.1 103 Early Hints\r\nLink: </c.css>; rel=preload\r\n\r\n"
          "HTTP/1.1 200 OK\r\n"
       << "Content-Length: " << payload.size() << "\r\n"
          "\r\n"
       << payload;

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_FALSE(outcome.gotError);
   EXPECT_EQ(outcome.body, payload);
}

TEST(AsyncClientContentLength, InterimResponseBeforeAChunkedFinalResponseStillFramesCorrectly)
{
   // The interim response must leave no trace on the framing decision made for
   // the response that follows it.
   std::string payload = "streamed after hints";
   std::ostringstream raw;
   raw << "HTTP/1.1 103 Early Hints\r\nLink: </a.css>; rel=preload\r\n\r\n"
          "HTTP/1.1 200 OK\r\n"
          "Transfer-Encoding: chunked\r\n"
          "\r\n"
       << std::hex << payload.size() << "\r\n" << payload << "\r\n"
       << "0\r\n\r\n";

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_FALSE(outcome.gotError);
   EXPECT_EQ(outcome.body,
             http::util::formatMessageAsHttpChunk(payload) +
                http::util::formatMessageAsHttpChunk(""));
   EXPECT_EQ(outcome.transferEncoding, kChunkedTransferEncoding);
}

TEST(AsyncClientContentLength, EndlessInterimResponsesFailRatherThanLoopingForever)
{
   // An upstream that never commits to a final response must not be able to
   // keep us skipping for as long as it feels like sending 1xx headers.
   std::ostringstream raw;
   for (int i = 0; i < 50; i++)
      raw << "HTTP/1.1 103 Early Hints\r\nLink: </a.css>; rel=preload\r\n\r\n";
   raw << "HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nhi";

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_TRUE(outcome.gotError);
   EXPECT_TRUE(outcome.body.empty());
}

TEST(AsyncClientContentLength, SwitchingProtocolsIsFinalAndIsNotSkipped)
{
   // 101 shares the 1xx range but is the final HTTP response on the connection
   // -- what follows is the upgraded protocol, not another response. Skipping
   // it would discard the handshake and then try to parse websocket frames as
   // a status line.
   std::ostringstream raw;
   raw << "HTTP/1.1 101 Switching Protocols\r\n"
          "Upgrade: websocket\r\n"
          "Connection: Upgrade\r\n"
          "\r\n";

   ProxiedOutcome outcome = proxyRawUpstreamResponse(raw.str());

   ASSERT_FALSE(outcome.timedOut);
   EXPECT_FALSE(outcome.gotError);
   EXPECT_EQ(outcome.statusCode, 101);
}

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


// setConnectHandler() registered late -- after the exchange has already
// settled and disableHandlers() has detached everything -- must report that
// through the downstream-closed handler rather than dropping the
// notification. ServerSessionProxy's upload path registers exactly this late
// (it posts its ClientHandler to the io_context after execute()), and on
// rserver's multi-threaded io_context the settle can win that race; FormProxy
// gates its writes on the connect notification, so a dropped one leaves the
// upload buffering against a writer that can never appear.
TEST(AsyncClientContentLength, ConnectHandlerRegisteredAfterSettleReportsDownstreamClosed)
{
   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/true,
                      "{\"name\":\"jsonlite\"}\n");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   bool gotResponse = false;
   pClient->execute([&](const http::Response&) { gotResponse = true; },
                    [&](const core::Error&) {});

   ioc.run();
   server.stop();

   // the exchange has fully settled, so closeAndRespond() has run
   // disableHandlers() and no connect can ever be reported
   ASSERT_TRUE(gotResponse);

   bool connected = false;
   bool downstreamClosed = false;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { downstreamClosed = true; });

   EXPECT_FALSE(connected);
   EXPECT_TRUE(downstreamClosed);
}

// The same late registration against a client that failed to connect at all:
// handleError() also runs disableHandlers(), so this must report closed too
// rather than leaving the caller waiting.
TEST(AsyncClientContentLength, ConnectHandlerRegisteredAfterConnectFailureReportsDownstreamClosed)
{
   // bind and immediately release a port so connecting to it is refused
   unsigned short deadPort = 0;
   {
      boost::asio::io_context probeIoc;
      tcp::acceptor probe(probeIoc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
      deadPort = probe.local_endpoint().port();
   }

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(deadPort),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");

   bool gotError = false;
   pClient->execute([&](const http::Response&) {},
                    [&](const core::Error&) { gotError = true; });

   ioc.run();

   ASSERT_TRUE(gotError);

   bool connected = false;
   bool downstreamClosed = false;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { downstreamClosed = true; });

   EXPECT_FALSE(connected);
   EXPECT_TRUE(downstreamClosed);
}

// The other half of the race, from review: registration wins, but the client
// settles before handleWrite() can deliver the connect. disableHandlers()
// detaches the stored connect handler, so without reporting it the caller is
// left waiting on a notification that has just been made unreachable.
//
// Driven deterministically rather than by racing threads: the teardown runs
// synchronously after execute() but before the io_context is ever pumped, so
// requestWritten_ is provably still false and nothing has been delivered. The
// destination is a dead port purely so the abandoned connect attempt resolves
// and lets run() return. Note the report only arrives once the loop is pumped
// -- it is posted, not invoked, because the disabling caller may hold a
// consumer's own lock (FixedBufferProxy::closeConnections does).
TEST(AsyncClientContentLength, ConnectHandlerDetachedBeforeWriteCompletesReportsDownstreamClosed)
{
   unsigned short deadPort = 0;
   {
      boost::asio::io_context probeIoc;
      tcp::acceptor probe(probeIoc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
      deadPort = probe.local_endpoint().port();
   }

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(deadPort),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");

   bool connected = false;
   int downstreamClosedCount = 0;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { downstreamClosedCount++; });

   pClient->execute([&](const http::Response&) {}, [&](const core::Error&) {});

   // tear down the way FixedBufferProxy::closeConnections() does -- close()
   // then disableHandlers(), synchronously, in that order
   pClient->close();
   pClient->disableHandlers();

   // not delivered inline: it is queued on the strand, so the disabling caller
   // above returns (and would release its lock) before it can run
   EXPECT_EQ(downstreamClosedCount, 0);

   ioc.run();

   // the connect was never delivered, and the detach was reported rather than
   // dropped -- exactly once
   EXPECT_FALSE(connected);
   EXPECT_EQ(downstreamClosedCount, 1);

   // a second disable must not report again
   pClient->disableHandlers();
   ioc.restart();
   ioc.poll();
   EXPECT_EQ(downstreamClosedCount, 1);
}

// The same detach-before-write case, but torn down with a bare close() and no
// disableHandlers() call at all -- which is the only settle some paths perform
// (an embedder's destructor, FormProxy::handleError()). close() marks only
// closed_, so before this fix the stored handler was left attached: the aborted
// connect completion returns early from handleError() on closed_ and never
// reaches the disableHandlers() at the end of that function, so the one thing
// that would have reported the drop never ran.
TEST(AsyncClientContentLength, BareCloseBeforeWriteReportsDownstreamClosed)
{
   unsigned short deadPort = 0;
   {
      boost::asio::io_context probeIoc;
      tcp::acceptor probe(probeIoc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
      deadPort = probe.local_endpoint().port();
   }

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(deadPort),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");

   bool connected = false;
   int downstreamClosedCount = 0;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { downstreamClosedCount++; });

   pClient->execute([&](const http::Response&) {}, [&](const core::Error&) {});

   // bare close only -- no disableHandlers()
   pClient->close();

   // not delivered inline: close() posts it to the strand for the same reason
   // disableHandlers() does -- FixedBufferProxy::closeConnections() calls
   // close() with its own mutex_ held
   EXPECT_EQ(downstreamClosedCount, 0);

   ioc.run();

   EXPECT_FALSE(connected);
   EXPECT_EQ(downstreamClosedCount, 1);

   // and the pair does not double-report: close() detached the handler, so the
   // ordinary close()-then-disableHandlers() teardown adds nothing
   pClient->disableHandlers();
   ioc.restart();
   ioc.poll();
   EXPECT_EQ(downstreamClosedCount, 1);
}

// The registration-after-bare-close half. closed_ is set but handlersDisabled_
// is not, so keying setConnectHandler()'s terminal branch on handlersDisabled_
// alone sent this down the !requestWritten_ branch, storing a handler that
// nothing would ever deliver.
TEST(AsyncClientContentLength, ConnectHandlerRegisteredAfterBareCloseReportsDownstreamClosed)
{
   unsigned short deadPort = 0;
   {
      boost::asio::io_context probeIoc;
      tcp::acceptor probe(probeIoc, tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0));
      deadPort = probe.local_endpoint().port();
   }

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(deadPort),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");

   pClient->execute([&](const http::Response&) {}, [&](const core::Error&) {});
   pClient->close();

   bool connected = false;
   int downstreamClosedCount = 0;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { downstreamClosedCount++; });

   ioc.run();

   EXPECT_FALSE(connected);
   EXPECT_EQ(downstreamClosedCount, 1);
}

// A delivered connect must NOT also produce a downstream-closed report when
// the client is torn down afterwards: handleWrite() leaves connectHandler_ in
// place after invoking it, so disableHandlers() has to discriminate on
// requestWritten_ rather than on the handler being non-empty. Without that,
// every ordinary request would report closed on completion and FormProxy would
// discard form data mid-upload.
TEST(AsyncClientContentLength, DeliveredConnectIsNotAlsoReportedAsDownstreamClosed)
{
   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/true,
                      "{\"name\":\"jsonlite\"}\n");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   bool connected = false;
   int downstreamClosedCount = 0;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { downstreamClosedCount++; });

   pClient->execute([&](const http::Response&) {}, [&](const core::Error&) {});

   ioc.run();

   // closeAndRespond() has already run disableHandlers() by now; pump again in
   // case it posted anything, and call it once more to confirm idempotence
   pClient->disableHandlers();
   ioc.restart();
   ioc.poll();

   server.stop();

   EXPECT_TRUE(connected);
   EXPECT_EQ(downstreamClosedCount, 0);
}

// From review (job 59): the third registration case. When requestWritten_ is
// already true, handleWrite() has been and gone, so setConnectHandler() has to
// deliver the notification itself -- and deciding that under socketMutex_ then
// calling out after releasing it is a decision another thread can invalidate
// in between. A caller acts on a connect notification by writing, so a stale
// one has FormProxy write to a closed socket and take its error path, which
// closes the upstream connection and resets whatever response FixedBufferProxy
// was mid-write of. The delivery is therefore deferred to the strand and the
// state re-read there.
//
// Deterministic: the server accepts and never replies, so the request is
// written (requestWritten_ true) while the exchange is still live, which is
// the only state this branch is reachable from. The settle is then made to win
// the race by running before the loop is pumped.
TEST(AsyncClientContentLength, LateConnectRegistrationSettledBeforeDeliveryReportsClosed)
{
   LocalServer server(ResponseMode::NoResponse, /*closeAfterResponse=*/false, "");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");

   // a first registration purely to observe when the request has been written,
   // i.e. when requestWritten_ has become true and nothing has settled
   bool written = false;
   pClient->setConnectHandler([&]() { written = true; }, []() {});
   pClient->execute([](const http::Response&) {}, [](const core::Error&) {});
   ASSERT_TRUE(pollUntil(ioc, [&]() { return written; }));

   // the late registration under test
   bool connected = false;
   int closedCount = 0;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { closedCount++; });

   // nothing delivered yet -- it is queued on the strand, which is what gives
   // the settle below a chance to be seen before the notification is decided
   EXPECT_FALSE(connected);
   EXPECT_EQ(closedCount, 0);

   // the settle wins, the way FixedBufferProxy::closeConnections() tears down
   pClient->close();
   pClient->disableHandlers();

   ioc.restart();
   ioc.poll();

   // reported as closed, not as a connect the caller would write against.
   // exactly once: requestWritten_ is true here, so disableHandlers() does not
   // also report the stored handler from the first registration
   EXPECT_FALSE(connected);
   EXPECT_EQ(closedCount, 1);

   server.stop();
}

// The same late registration with nothing settling: deferring delivery to the
// strand must not turn into never delivering, or swap the notification on a
// perfectly live client.
TEST(AsyncClientContentLength, LateConnectRegistrationOnLiveClientStillReportsConnect)
{
   LocalServer server(ResponseMode::NoResponse, /*closeAfterResponse=*/false, "");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");

   bool written = false;
   pClient->setConnectHandler([&]() { written = true; }, []() {});
   pClient->execute([](const http::Response&) {}, [](const core::Error&) {});
   ASSERT_TRUE(pollUntil(ioc, [&]() { return written; }));

   bool connected = false;
   int closedCount = 0;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { closedCount++; });

   ASSERT_TRUE(pollUntil(ioc, [&]() { return connected; }));
   EXPECT_EQ(closedCount, 0);

   pClient->close();
   server.stop();
}

// Control for the two above: registered before execute() -- the ordinary
// ordering -- the connect handler is what fires, and the downstream-closed
// handler stays untouched. Exactly one of the two is ever delivered.
TEST(AsyncClientContentLength, ConnectHandlerRegisteredBeforeExecuteStillReportsConnect)
{
   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/true,
                      "{\"name\":\"jsonlite\"}\n");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   bool connected = false;
   bool downstreamClosed = false;
   pClient->setConnectHandler([&]() { connected = true; },
                              [&]() { downstreamClosed = true; });

   pClient->execute([&](const http::Response&) {}, [&](const core::Error&) {});

   ioc.run();
   server.stop();

   EXPECT_TRUE(connected);
   EXPECT_FALSE(downstreamClosed);
}

// --- rstudio#18625: a failed handler snapshot must take an error path ---
//
// AsyncClient copies its handler members out from under socketMutex_ before
// invoking them (see snapshotHandlers()). Those copies are fallible: copying a
// boost::function whose target is too large for its small-object buffer
// allocates, and allocation failure under the memory pressure streaming exists
// to relieve is the realistic way one fails. Every snapshot site would
// otherwise read the resulting empty local as "no handler installed" and act on
// it, so each of the three below is driven through that failure.

namespace {

// A callback target whose copy constructor throws once armed -- the fault
// injection those scenarios need, delivered deterministically rather than by
// trying to exhaust memory. std::bad_alloc specifically, since that is what a
// real boost::function copy raises. One-shot: the point is that the client
// recovers, not that it survives an endless fault.
//
// The shared_ptr and boost::function members also keep this comfortably too
// large (and too non-trivial) for boost::function's small-object buffer, so the
// snapshot really does clone it on the heap rather than storing it inline.
template <typename F>
class ThrowOnCopy
{
public:
   ThrowOnCopy(const boost::shared_ptr<bool>& pArmed, const F& callback)
      : pArmed_(pArmed), callback_(callback)
   {
   }

   ThrowOnCopy(const ThrowOnCopy& other)
      : pArmed_(other.pArmed_), callback_(other.callback_)
   {
      if (!*pArmed_)
         return;

      *pArmed_ = false;
      throw std::bad_alloc();
   }

   template <typename... Args>
   typename F::result_type operator()(Args&&... args) const
   {
      return callback_(std::forward<Args>(args)...);
   }

private:
   boost::shared_ptr<bool> pArmed_;
   F callback_;
};

} // anonymous namespace

// deliverChunks(): the snapshot fails between two body pieces. The empty local
// is indistinguishable from an absent fixed buffer handler, and acting on it
// appends the piece to response_ -- which nothing in streaming mode ever reads
// -- and then goes on to tell the consumer the body is complete, one piece
// short. The request must fail instead.
TEST(AsyncClientContentLength, FixedBufferHandlerSnapshotFailureMidBodyFailsRatherThanDroppingPiece)
{
   const std::string body = "{\"line\":1}\n{\"line\":2}\n{\"line\":3}\n{\"line\":4}\n";

   LocalServer server(ResponseMode::ContentLengthSplit, /*closeAfterResponse=*/true, body);
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   pClient->setStreamNonChunkedResponses(true);
   pClient->setFixedBufferHandlerSupportsPause(true);

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   std::vector<std::string> pieces;
   bool sawFinalSignal = false;
   bool gotResponse = false;
   bool gotError = false;

   boost::shared_ptr<boost::asio::system_timer> pTimer =
      boost::make_shared<boost::asio::system_timer>(ioc, std::chrono::seconds(4));
   pTimer->async_wait([&](const boost::system::error_code& ec) {
      if (ec == boost::asio::error::operation_aborted)
         return;
      pClient->close();
   });

   boost::shared_ptr<bool> pArmed = boost::make_shared<bool>(false);
   FixedBufferHandler fixedBufferHandler =
      ThrowOnCopy<boost::function<bool(const http::Response&, const std::string&)>>(
         pArmed,
         [&](const http::Response&, const std::string& piece) -> bool
         {
            if (piece.empty())
            {
               sawFinalSignal = true;
               return true;
            }

            pieces.push_back(piece);

            // arm from inside the delivery of the first piece, so the failure
            // lands on the snapshot taken for the piece after this one -- the
            // mid-body case, with a consumer already streaming
            *pArmed = true;
            return true;
         });

   pClient->execute(
      [&](const http::Response& response) { gotResponse = true; },
      [&](const core::Error&) { gotError = true; pTimer->cancel(); },
      fixedBufferHandler);

   ioc.run();
   server.stop();

   // the failure is reported rather than logged and stepped over
   EXPECT_TRUE(gotError);

   // and the consumer is never told the body finished, which -- having received
   // only the first piece -- would have been a lie
   EXPECT_FALSE(sawFinalSignal);
   EXPECT_FALSE(gotResponse);
   ASSERT_EQ(pieces.size(), 1u);
   EXPECT_LT(pieces[0].size(), body.size());
}

// closeAndRespond(): the snapshot fails on the last word of the request, where
// an empty local costs the most. The fixed buffer handler copy is the one that
// fails here, so the surviving response handler would be invoked instead --
// handing a whole-body callback a response_ that streaming never populated,
// i.e. reporting a successful, empty response for a body that was in fact
// delivered in full.
TEST(AsyncClientContentLength, ResponseHandlerSnapshotFailureAtCompletionFailsRatherThanRespondingEmpty)
{
   const std::string body = "{\"name\":\"jsonlite\"}\n";

   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/true, body);
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   pClient->setStreamNonChunkedResponses(true);
   pClient->setFixedBufferHandlerSupportsPause(true);

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   std::vector<std::string> pieces;
   bool sawFinalSignal = false;
   bool gotResponse = false;
   std::string responseBody;
   bool gotError = false;

   boost::shared_ptr<boost::asio::system_timer> pTimer =
      boost::make_shared<boost::asio::system_timer>(ioc, std::chrono::seconds(4));
   pTimer->async_wait([&](const boost::system::error_code& ec) {
      if (ec == boost::asio::error::operation_aborted)
         return;
      pClient->close();
   });

   boost::shared_ptr<bool> pArmed = boost::make_shared<bool>(false);
   FixedBufferHandler fixedBufferHandler =
      ThrowOnCopy<boost::function<bool(const http::Response&, const std::string&)>>(
         pArmed,
         [&](const http::Response&, const std::string& piece) -> bool
         {
            if (piece.empty())
            {
               sawFinalSignal = true;
               return true;
            }

            pieces.push_back(piece);

            // the whole body arrives in this one piece, so the next snapshot
            // taken is closeAndRespond()'s
            *pArmed = true;
            return true;
         });

   pClient->execute(
      [&](const http::Response& response)
      {
         gotResponse = true;
         responseBody = response.body();
         pTimer->cancel();
      },
      [&](const core::Error&) { gotError = true; pTimer->cancel(); },
      fixedBufferHandler);

   ioc.run();
   server.stop();

   EXPECT_TRUE(gotError);

   // no completion of any kind was invented from the failed copies
   EXPECT_FALSE(gotResponse);
   EXPECT_TRUE(responseBody.empty());
   EXPECT_FALSE(sawFinalSignal);

   // the body itself did stream through before the failure -- what went wrong
   // was only the copy taken to announce that it had finished
   ASSERT_EQ(pieces.size(), 1u);
   EXPECT_EQ(pieces[0], body);
}

// A completion retry is necessarily post-close: closeAndRespond() closes the
// socket before it sends the empty completion chunk, and a backpressured
// consumer asks resumeChunkProcessing() to re-enter closeAndRespond() later.
// A snapshot failure on that retry must bypass handleError()'s ordinary
// "purposefully closed" early return and still release the handler cycle.
TEST(AsyncClientContentLength, ResponseHandlerSnapshotFailureOnClosedCompletionRetryStillSettles)
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
   pClient->setFixedBufferHandlerSupportsPause(true);

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   bool gotResponse = false;
   bool gotError = false;
   bool timedOut = false;
   int completionCalls = 0;

   boost::shared_ptr<boost::asio::system_timer> pTimer =
      boost::make_shared<boost::asio::system_timer>(ioc, std::chrono::seconds(2));
   pTimer->async_wait([&](const boost::system::error_code& ec) {
      if (ec == boost::asio::error::operation_aborted)
         return;
      timedOut = true;
      pClient->close();
   });

   boost::shared_ptr<bool> pArmed = boost::make_shared<bool>(false);
   boost::weak_ptr<int> weakHandlerState;
   {
      boost::shared_ptr<int> pHandlerState = boost::make_shared<int>(0);
      weakHandlerState = pHandlerState;

      FixedBufferHandler fixedBufferHandler =
         ThrowOnCopy<boost::function<bool(const http::Response&, const std::string&)>>(
            pArmed,
            [&, pHandlerState](const http::Response&, const std::string& piece) -> bool
            {
               if (!piece.empty())
                  return true;

               ++completionCalls;
               *pArmed = true;
               boost::asio::post(ioc, [&]() { pClient->resumeChunkProcessing(); });
               return false;
            });

      pClient->execute(
         [&](const http::Response&) { gotResponse = true; pTimer->cancel(); },
         [&](const core::Error&) { gotError = true; pTimer->cancel(); },
         fixedBufferHandler);
   }

   ioc.run();
   server.stop();

   EXPECT_FALSE(timedOut);
   EXPECT_FALSE(gotResponse);
   EXPECT_TRUE(gotError);
   EXPECT_EQ(completionCalls, 1);
   EXPECT_TRUE(weakHandlerState.expired());
}

// handleError() snapshots errorHandler_ too. If the allocation pressure that
// broke the first snapshot also breaks this one, the callback cannot be
// delivered, but cleanup must still run rather than reading an empty local as
// "no handler" and leaking the installed callbacks.
TEST(AsyncClientContentLength, ErrorHandlerSnapshotFailureStillDisablesHandlers)
{
   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/true,
                      "{\"name\":\"jsonlite\"}\n");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   pClient->setStreamNonChunkedResponses(true);
   pClient->setFixedBufferHandlerSupportsPause(true);

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   bool gotResponse = false;
   bool gotError = false;
   boost::shared_ptr<bool> pFixedArmed = boost::make_shared<bool>(false);
   boost::shared_ptr<bool> pErrorArmed = boost::make_shared<bool>(false);
   boost::weak_ptr<int> weakFixedState;
   boost::weak_ptr<int> weakErrorState;

   {
      boost::shared_ptr<int> pFixedState = boost::make_shared<int>(0);
      boost::shared_ptr<int> pErrorState = boost::make_shared<int>(0);
      weakFixedState = pFixedState;
      weakErrorState = pErrorState;

      pClient->execute(
         [&](const http::Response&) { gotResponse = true; },
         ThrowOnCopy<boost::function<void(const core::Error&)>>(
            pErrorArmed,
            [&, pErrorState](const core::Error&) { gotError = true; }),
         ThrowOnCopy<boost::function<bool(const http::Response&, const std::string&)>>(
            pFixedArmed,
            [&, pFixedState](const http::Response&, const std::string& piece) -> bool
            {
               if (!piece.empty())
               {
                  // The next fixed-buffer copy (at completion) and the error
                  // handler copy used to report it both fail once.
                  *pFixedArmed = true;
                  *pErrorArmed = true;
               }
               return true;
            }));
   }

   ioc.run();
   server.stop();

   EXPECT_FALSE(gotResponse);
   EXPECT_FALSE(gotError);
   EXPECT_TRUE(weakFixedState.expired());
   EXPECT_TRUE(weakErrorState.expired());
}

// handleWrite(): the connect notification's snapshot fails. A caller gated on
// that notification (FormProxy) buffers its upload until it hears one way or
// the other, so dropping it hangs the upload; the client must report the
// still-undeliverable notification through the downstream-closed handler, which
// is what requestWritten_ staying false on this path arranges.
TEST(AsyncClientContentLength, ConnectHandlerSnapshotFailureReportsDownstreamClosedRatherThanDropping)
{
   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/true,
                      "{\"name\":\"jsonlite\"}\n");
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   bool connected = false;
   bool downstreamClosed = false;
   bool gotResponse = false;
   bool gotError = false;

   boost::shared_ptr<bool> pArmed = boost::make_shared<bool>(false);
   pClient->setConnectHandler(
      ThrowOnCopy<boost::function<void()>>(pArmed, [&]() { connected = true; }),
      [&]() { downstreamClosed = true; });

   pClient->execute([&](const http::Response&) { gotResponse = true; },
                    [&](const core::Error&) { gotError = true; });

   // armed only now: registering the handler above copies it twice (into the
   // argument, then into connectHandler_), and the copy under test is the one
   // handleWrite() takes to invoke it
   *pArmed = true;

   ioc.run();
   server.stop();

   EXPECT_FALSE(connected);
   EXPECT_TRUE(downstreamClosed);
   EXPECT_TRUE(gotError);

   // the request is failed rather than read to completion behind the caller's
   // back: whoever registered the connect handler is waiting to write to this
   // connection, so its response is not theirs to consume
   EXPECT_FALSE(gotResponse);
}

// --- rstudio#18625: a failed handler install must leave the client unchanged ---
//
// The other half of the same exception-safety problem: the sites that *store*
// handlers copied them under socketMutex_ interleaved with the assignments, so
// a copy that threw left the client half-configured -- and LOCK_MUTEX stepped
// over it, leaving the caller to find out later, or never. Every copy now
// happens before the lock, with only swaps under it, so either the whole
// install lands or nothing does and the exception reaches the caller.

// execute(): the response handler copies, the error handler does not. A request
// still must not start, but "nothing changed" is insufficient when an embedder
// has already installed a streaming handler: FixedBufferProxy does that before
// execute(), and its handler closes a shared_ptr cycle around the client. With
// no request in motion, only execute()'s failure path remains to detach it.
TEST(AsyncClientContentLength, ExecuteHandlerInstallFailureBreaksPrewiredStreamingCycle)
{
   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", "1",
         boost::posix_time::seconds(5));

   boost::weak_ptr<FakeStreamingConsumer> weakConsumer;
   {
      boost::shared_ptr<FakeStreamingConsumer> pConsumer =
         boost::make_shared<FakeStreamingConsumer>(pClient);
      pConsumer->wire();
      weakConsumer = pConsumer;
   }
   ASSERT_FALSE(weakConsumer.expired());

   boost::shared_ptr<bool> pArmed = boost::make_shared<bool>(false);
   ResponseHandler doomedResponseHandler =
      ThrowOnCopy<boost::function<void(const http::Response&)>>(
         pArmed, [](const http::Response&) {});

   // armed only now, so the copy that fails is the one execute() takes
   *pArmed = true;
   EXPECT_THROW(pClient->execute(doomedResponseHandler, [](const core::Error&) {}),
                std::bad_alloc);

   // The request never reached connectAndWriteRequest(), and the already-wired
   // consumer was detached synchronously. Its bound self-reference and its
   // reference back to pClient can no longer survive as a cycle.
   EXPECT_TRUE(weakConsumer.expired());
}

// setFixedBufferHandler(): the installed handler was detached before the
// replacement was copied into its place, so a failed copy left no fixed buffer
// handler at all -- which silently turns a streaming consumer into an
// accumulate-into-response_ one, for a body nothing then reads.
TEST(AsyncClientContentLength, FixedBufferHandlerInstallFailureKeepsTheInstalledHandler)
{
   const std::string body = "{\"line\":1}\n{\"line\":2}\n";

   LocalServer server(ResponseMode::ContentLength, /*closeAfterResponse=*/true, body);
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(server.port()),
         boost::posix_time::seconds(5));

   pClient->setStreamNonChunkedResponses(true);
   pClient->setFixedBufferHandlerSupportsPause(true);

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/file");
   request.setHeader("Connection", "close");

   // the wiring FixedBufferProxy::proxy() does: a streaming consumer installed
   // through the setter rather than through execute()
   std::vector<std::string> pieces;
   bool sawFinalSignal = false;
   pClient->setFixedBufferHandler(
      [&](const http::Response&, const std::string& piece) -> bool
      {
         if (piece.empty())
            sawFinalSignal = true;
         else
            pieces.push_back(piece);

         return true;
      });

   boost::shared_ptr<bool> pArmed = boost::make_shared<bool>(false);
   FixedBufferHandler doomedFixedBufferHandler =
      ThrowOnCopy<boost::function<bool(const http::Response&, const std::string&)>>(
         pArmed, [](const http::Response&, const std::string&) { return true; });

   *pArmed = true;
   EXPECT_THROW(pClient->setFixedBufferHandler(doomedFixedBufferHandler), std::bad_alloc);

   bool gotResponse = false;
   pClient->execute([&](const http::Response&) { gotResponse = true; },
                    [](const core::Error&) {});

   ioc.run();
   server.stop();

   // the consumer installed before the failed replacement is still the one
   // streaming the body -- not displaced by a handler that never landed
   EXPECT_TRUE(sawFinalSignal);
   ASSERT_EQ(pieces.size(), 1u);
   EXPECT_EQ(pieces[0], body);

   // and the body did not fall back to being buffered for the whole-body handler
   EXPECT_FALSE(gotResponse);
}

// setConnectHandler(): the two handlers are stored as a pair, and the second
// copy is the one that fails here. Storing a connect notification while losing
// the downstream-closed handler that reports its loss leaves a caller with no
// way to be told either way -- the hang the pair exists to prevent.
TEST(AsyncClientContentLength, ConnectHandlerInstallFailureKeepsTheInstalledPair)
{
   // never executed: what is under test is the registration itself, and a bare
   // close() is enough to make the client report the pending notification
   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", "1", boost::posix_time::seconds(5));

   bool connectedA = false;
   bool downstreamClosedA = false;
   pClient->setConnectHandler([&]() { connectedA = true; },
                              [&]() { downstreamClosedA = true; });

   bool connectedB = false;
   bool downstreamClosedB = false;
   boost::shared_ptr<bool> pArmed = boost::make_shared<bool>(false);
   ConnectHandler doomedDownstreamClosedHandler =
      ThrowOnCopy<boost::function<void()>>(pArmed, [&]() { downstreamClosedB = true; });

   // armed so the connect half of the pair copies and the downstream-closed
   // half does not, which is what used to strand the pair half-replaced
   *pArmed = true;
   EXPECT_THROW(pClient->setConnectHandler([&]() { connectedB = true; },
                                           doomedDownstreamClosedHandler),
                std::bad_alloc);

   // the registration that was already in place is intact, so the settle below
   // still has somewhere to report to
   pClient->close();
   ioc.run();

   EXPECT_TRUE(downstreamClosedA);
   EXPECT_FALSE(connectedA);

   // and nothing from the failed registration took effect
   EXPECT_FALSE(connectedB);
   EXPECT_FALSE(downstreamClosedB);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
