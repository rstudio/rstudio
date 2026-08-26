/*
 * FixedBufferProxyTests.cpp
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

// Coverage for the FixedBufferProxy client-facing framing decision (rstudio-pro-11740
// Step 2): once AsyncClient (Step 1) can feed FixedBufferProxy both chunked and
// Content-Length-delimited response bodies piece-wise, FixedBufferProxy must choose,
// once at header time, whether to forward Content-Length framing (raw body
// bytes, upstream Content-Length preserved) or fall back to chunked framing
// (enveloped pieces, Transfer-Encoding: chunked) -- and account buffer usage
// against the bytes actually enqueued in both modes.

#include <new>
#include <string>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/asio/error.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/asio/post.hpp>
#include <boost/make_shared.hpp>

#include <core/http/AsyncConnection.hpp>
#include <core/http/AsyncClient.hpp>
#include <core/http/FixedBufferProxy.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/Util.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

// A fake AsyncConnection that records the headers "written" and the raw bytes
// asyncWrite'd to the client, invoking handlers synchronously so the whole
// FixedBufferProxy state machine can be exercised without a real event loop.
class FakeAsyncConnection : public AsyncConnection
{
public:
   boost::asio::io_context& ioContext() override { return ioc_; }
   const http::Request& request() const override { return request_; }

   // Counts which execution context reaches in to populate the response, so a
   // test can pin that FixedBufferProxy keeps the whole assemble-then-write
   // pair on our strand -- an off-strand mutation would be racing this
   // connection's own writers over storage asio may be mid-read (see
   // AsyncConnection::response()'s threading note).
   http::Response& response() override
   {
      if (strand_.running_in_this_thread())
         responseAccessesOnStrand_++;
      else
         responseAccessesOffStrand_++;

      return response_;
   }

   void writeResponse(bool close, Socket::Handler handler) override
   {
      boost::asio::post(ioc_, [handler]() { handler(boost::system::error_code(), 0); });
   }

   void writeResponse(const http::Response&,
                       bool,
                       const http::Headers&,
                       Socket::Handler handler) override
   {
      boost::asio::post(ioc_, [handler]() { handler(boost::system::error_code(), 0); });
   }

   // NOTE: like real network I/O, the completion handler must not run
   // re-entrantly on the same call stack as the FixedBufferProxy method that
   // initiated the write -- FixedBufferProxy holds its own mutex_ across the call
   // into writeResponseHeaders()/asyncWrite(), and its completion handlers
   // (onHeadersWrote/onChunkWrote) re-acquire that same (non-recursive) mutex.
   // So these fakes post to the io_context instead of invoking the handler
   // inline; Fixture::deliver() drains the queue with ioc_.poll() once the
   // outer queueChunk() call (and its lock) has returned.
   void writeResponseHeaders(Socket::Handler handler) override
   {
      throwIfRequested();
      headersWritten_ = true;
      headersWrittenOnStrand_ = strand_.running_in_this_thread();
      writtenHeaders_.assign(response_);
      boost::system::error_code ec = takeNextWriteError();
      boost::asio::post(ioc_, [handler, ec]() { handler(ec, 0); });
   }

   // The overload FixedBufferProxy actually uses: it hands the assembled
   // response over instead of staging it in response_, so that nothing mutates
   // the response the connection writes from until the claim is won (see
   // AsyncConnection::response()). Mirror the real implementation and assign
   // it in here, so response()-based assertions still see what went out.
   void writeResponseHeaders(const http::Response& response, Socket::Handler handler) override
   {
      response_.assign(response);
      writeResponseHeaders(handler);
   }

   void writeError(const Error&) override {}

   void close() override
   {
      closed_ = true;

      if (throwOnNextClose_)
      {
         throwOnNextClose_ = false;
         throw std::bad_alloc();
      }
   }

   // One-shot injectable throw from close(). This is the first step of
   // failConnection()'s teardown, so it is the one whose failure would cost the
   // most if the steps were chained rather than attempted independently.
   void throwOnNextClose() { throwOnNextClose_ = true; }

   void continueParsing() override {}
   void setData(const boost::any& data) override { data_ = data; }
   boost::any getData() override { return data_; }
   const std::string& username() const override { return username_; }
   void setUsername(const std::string& username) override { username_ = username; }
   const std::string& handlerPrefix() const override { return handlerPrefix_; }
   void setHandlerPrefix(const std::string& prefix) override { handlerPrefix_ = prefix; }
   // A real strand over ioc_: FixedBufferProxy dispatches its header assembly
   // onto this (see FixedBufferProxy::queueChunk), and since the test thread is
   // never inside the strand, that dispatch posts -- Fixture::deliver()'s poll()
   // is what runs it, along with the write completions it chains into.
   boost::asio::io_context::strand& getStrand() override { return strand_; }

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}

   void asyncWrite(const boost::asio::const_buffer& buffer, Socket::Handler handler) override
   {
      throwIfRequested();
      boost::system::error_code ec = takeNextWriteError();
      std::size_t n = 0;
      if (!ec)
      {
         writtenBytes_.append(static_cast<const char*>(buffer.data()), buffer.size());
         n = buffer.size();
      }
      boost::asio::post(ioc_, [handler, ec, n]() { handler(ec, n); });
   }

   void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers,
                    Socket::Handler handler) override
   {
      throwIfRequested();
      boost::system::error_code ec = takeNextWriteError();
      std::size_t total = 0;
      if (!ec)
      {
         for (const auto& buffer : buffers)
         {
            writtenBytes_.append(static_cast<const char*>(buffer.data()), buffer.size());
            total += buffer.size();
         }
      }
      boost::asio::post(ioc_, [handler, ec, total]() { handler(ec, total); });
   }

   // One-shot injectable failure for the next header/body write, consumed by
   // whichever of writeResponseHeaders()/asyncWrite() runs next -- simulates
   // the downstream (browser) connection dropping mid-write.
   void failNextWrite(const boost::system::error_code& ec) { nextWriteError_ = ec; }

   // One-shot injectable *throw* from the next header/body write, as opposed to
   // failNextWrite()'s error_code. bad_alloc specifically: the realistic escape
   // from FixedBufferProxy's own locked regions is an allocation failure while
   // copying a piece up to maxChunkSize, and it enters the proxy through the
   // same two calls a write error does. Which of FixedBufferProxy's exception
   // guards catches it depends on the write: a header write is reached from
   // writeHeaders(), a body write from writeChunk() inside one of the three
   // locked regions.
   void throwOnNextWrite() { throwOnNextWrite_ = true; }

   // The client-facing request drives two of FixedBufferProxy's framing
   // decisions (HEAD => no body at all; HTTP/1.0 => no Transfer-Encoding), so
   // tests need to be able to vary it. Defaults match http::Request's own:
   // an empty method and HTTP/1.1, i.e. the ordinary chunked-capable client.
   void setRequestMethod(const std::string& method) { request_.setMethod(method); }
   void setRequestHttpVersion(int major, int minor) { request_.setHttpVersion(major, minor); }

   bool headersWritten_ = false;
   bool headersWrittenOnStrand_ = false;
   bool closed_ = false;
   int responseAccessesOnStrand_ = 0;
   int responseAccessesOffStrand_ = 0;
   std::string writtenBytes_;
   http::Response writtenHeaders_; // snapshot at header-write time

private:
   boost::system::error_code takeNextWriteError()
   {
      boost::system::error_code ec = nextWriteError_;
      nextWriteError_ = boost::system::error_code();
      return ec;
   }

   void throwIfRequested()
   {
      if (!throwOnNextWrite_)
         return;

      throwOnNextWrite_ = false;
      throw std::bad_alloc();
   }

   bool throwOnNextWrite_ = false;
   bool throwOnNextClose_ = false;
   boost::system::error_code nextWriteError_;
   boost::asio::io_context ioc_;
   boost::asio::io_context::strand strand_{ioc_};
   http::Request request_;
   http::Response response_;
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
};

// A fake IAsyncClient that just captures the FixedBufferHandler passed to
// setFixedBufferHandler() so the test can drive FixedBufferProxy::queueChunk() directly,
// and tracks close()/resumeChunkProcessing() calls.
class FakeAsyncClient : public IAsyncClient
{
public:
   http::Request& request() override { return request_; }
   void setConnectionRetryProfile(const http::ConnectionRetryProfile&) override {}
   void setRequestTimeout(const boost::posix_time::time_duration&) override {}
   void execute(const ResponseHandler&, const ErrorHandler&, const FixedBufferHandler&) override {}
   void setFixedBufferHandler(const FixedBufferHandler& fixedBufferHandler) override { fixedBufferHandler_ = fixedBufferHandler; }
   void setStreamNonChunkedResponses(bool) override {}
   void setBufferPredicate(const boost::function<bool(const http::Response&)>&) override {}
   void setFixedBufferHandlerSupportsPause(bool supportsPause) override { fixedBufferHandlerSupportsPause_ = supportsPause; }
   void setConnectHandler(const ConnectHandler&, const ConnectHandler&) override {}
   void resumeChunkProcessing() override { resumed_ = true; }
   void disableHandlers() override
   {
      disableHandlersCalled_ = true;

      // Before the detach below, so a test can keep the handler attached --
      // which is the only way an upstream can still deliver after a teardown.
      if (throwOnNextDisableHandlers_)
      {
         throwOnNextDisableHandlers_ = false;
         throw std::bad_alloc();
      }

      // Mirror the real AsyncClient::disableHandlers(): this is what breaks
      // the FixedBufferProxy <-> AsyncClient reference cycle (queueChunk()
      // was bound here via shared_from_this() in proxy()). Without actually
      // clearing this, a test could pass a "was it called" assertion while
      // the leak the call is meant to fix still silently persists.
      fixedBufferHandler_ = FixedBufferHandler();
   }
   void close() override
   {
      closed_ = true;

      if (throwOnNextClose_)
      {
         throwOnNextClose_ = false;
         throw std::bad_alloc();
      }
   }
   void setStrand(boost::asio::io_context::strand*) override {}

   // One-shot injectable throw from close(). This is the case that makes
   // FixedBufferProxy's failed_ flag load-bearing rather than belt-and-braces:
   // closeUpstream() calls close() before disableHandlers(), so a throw here
   // leaves fixedBufferHandler_ attached and the upstream still delivering.
   void throwOnNextClose() { throwOnNextClose_ = true; }

   // One-shot injectable throw from disableHandlers(). Paired with
   // throwOnNextClose() this simulates a teardown that fails at every step.
   void throwOnNextDisableHandlers() { throwOnNextDisableHandlers_ = true; }

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}
   void asyncWrite(const boost::asio::const_buffer&, Socket::Handler) override {}
   void asyncWrite(const std::vector<boost::asio::const_buffer>&, Socket::Handler) override {}

   // Deliver a piece the way the real AsyncClient does: invoke a COPY of the
   // stored handler, never the member itself. FixedBufferProxy's close paths
   // call disableHandlers() from within this very invocation, which detaches
   // fixedBufferHandler_ -- invoking the member directly would have it
   // destroyed out from under its own executing call, and the copy is also
   // what keeps the consumer alive through that reentrant detach (see
   // AsyncClient::disableHandlers()'s contract comment).
   bool deliverChunk(const http::Response& response, const std::string& chunk)
   {
      FixedBufferHandler handler = fixedBufferHandler_;
      return handler(response, chunk);
   }

   FixedBufferHandler fixedBufferHandler_;
   bool resumed_ = false;
   bool closed_ = false;
   bool disableHandlersCalled_ = false;
   bool fixedBufferHandlerSupportsPause_ = false;

private:
   bool throwOnNextClose_ = false;
   bool throwOnNextDisableHandlers_ = false;
   http::Request request_;
};

struct Fixture
{
   // preStamped goes onto the client connection's response before proxy() runs,
   // which is where FixedBufferProxy snapshots the Set-Cookie headers it has to
   // carry over -- and is the production ordering, since authentication stamps
   // refreshed cookies before dispatching to the handler that builds the proxy.
   Fixture(uint64_t maxBufferSize = 1024 * 1024,
           const http::Headers& preStamped = http::Headers())
   {
      pClientConnection = boost::make_shared<FakeAsyncConnection>();
      pServerConnection = boost::make_shared<FakeAsyncClient>();

      for (const http::Header& header : preStamped)
         pClientConnection->response().addHeader(header);

      pProxy = boost::make_shared<FixedBufferProxy>(pClientConnection, maxBufferSize);
      pProxy->proxy(pServerConnection);
   }

   // Deliver one piece the way AsyncClient's fixedBufferHandler_ invocation would,
   // then drain the fake connection's io_context so any posted
   // writeResponseHeaders/asyncWrite completion handlers (onHeadersWrote,
   // onChunkWrote, and whatever further writes/closes they chain into) run to
   // completion before returning -- mirroring how those completions would
   // eventually fire asynchronously in production.
   bool deliver(const http::Response& upstream, const std::string& chunk)
   {
      bool result = pServerConnection->deliverChunk(upstream, chunk);

      // io_context::poll() marks the context "stopped" once it runs out of
      // ready work; it must be restarted before it will process any handlers
      // posted by a later call (each deliver() call posts new work).
      boost::asio::io_context& ioc = pClientConnection->ioContext();
      ioc.restart();
      ioc.poll();

      return result;
   }

   // Run exactly one queued handler -- the dispatched writeHeaders(), or one
   // write completion -- leaving whatever it posts for the next call. This is
   // what lets a test arm an injected failure *between* two writes that
   // deliver() would otherwise run in a single drain, which is the only way to
   // reach the guards in onHeadersWrote()/onChunkWrote(): the write those
   // initiate is chained from the previous write's completion, never from a
   // queueChunk() call the test controls directly.
   void runOneHandler()
   {
      boost::asio::io_context& ioc = pClientConnection->ioContext();
      ioc.restart();
      ioc.poll_one();
   }

   void drain()
   {
      boost::asio::io_context& ioc = pClientConnection->ioContext();
      ioc.restart();
      ioc.poll();
   }

   boost::shared_ptr<FakeAsyncConnection> pClientConnection;
   boost::shared_ptr<FakeAsyncClient> pServerConnection;
   boost::shared_ptr<FixedBufferProxy> pProxy;
};

void makeContentLengthResponse(http::Response* pResponse, const std::string& body)
{
   pResponse->setStatusCode(200);
   pResponse->setHeader("Content-Length", static_cast<uintmax_t>(body.size()));
}

void makeChunkedResponse(http::Response* pResponse)
{
   pResponse->setStatusCode(200);
   pResponse->setHeader(kTransferEncoding, kChunkedTransferEncoding);
}

} // anonymous namespace

// FixedBufferProxy::proxy() must opt the connection into pause-aware completion
// handling (AsyncClient::closeAndRespond() otherwise discards a declined
// completion signal -- see completionPending_/setFixedBufferHandlerSupportsPause()
// in AsyncClient.hpp for the full rationale). Without this, a completion
// signal declined while FixedBufferProxy's own buffer is exactly full would never
// be retried, leaving both connections open indefinitely.
TEST(FixedBufferProxy, ProxyOptsIntoFixedBufferHandlerPauseSupport)
{
   Fixture fixture;
   EXPECT_TRUE(fixture.pServerConnection->fixedBufferHandlerSupportsPause_);
}

TEST(FixedBufferProxy, UsesContentLengthFramingWhenUpstreamLengthKnown)
{
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   EXPECT_TRUE(fixture.deliver(upstream, body));
   EXPECT_TRUE(fixture.deliver(upstream, "")); // completion signal

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length"), "5");
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());

   // Content-Length framing writes raw bytes -- no chunk envelope.
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, body);

   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

TEST(FixedBufferProxy, StreamsWithoutTouchingTheClientConnectionsResponse)
{
   // The client connection owns its response: it claims it before assigning
   // anything, which is the only ordering that is safe against its own earlier
   // writes -- those hand asio buffers pointing into that response's member
   // strings and stay in flight after the call that started them returns, so a
   // strand does not cover them. So FixedBufferProxy must assemble into a
   // response of its own and hand it over, never reach into that one, and its
   // one legitimate read of it (the Set-Cookie carry-over) happens back in
   // proxy(), before any of this can be racing.
   //
   // Pin both halves: nothing touches response() once streaming has started,
   // and the write is still initiated on the connection's strand.
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   // discount proxy()'s snapshot, which the fixture made on this thread
   fixture.pClientConnection->responseAccessesOnStrand_ = 0;
   fixture.pClientConnection->responseAccessesOffStrand_ = 0;

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, ""); // completion signal

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   EXPECT_TRUE(fixture.pClientConnection->headersWrittenOnStrand_);
   EXPECT_EQ(fixture.pClientConnection->responseAccessesOnStrand_, 0);
   EXPECT_EQ(fixture.pClientConnection->responseAccessesOffStrand_, 0);
}

TEST(FixedBufferProxy, WritesHeadersInlineWhenAlreadyOnTheClientConnectionsStrand)
{
   // The production wiring this path was built for shares one strand between
   // the two connections (proxyLocalhostRequest() hands the upstream
   // AsyncClient the client connection's own strand), so chunks arrive already
   // on it. dispatch() must then run the assembly inline rather than deferring
   // it -- deferring would add a hop per response and, more to the point, means
   // the headers are no longer written by the time queueChunk() returns, which
   // is the ordering the rest of this state machine was written against.
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   boost::asio::io_context& ioc = fixture.pClientConnection->ioContext();
   bool headersWrittenBeforeQueueChunkReturned = false;

   // discount proxy()'s snapshot, which the fixture made on this thread
   fixture.pClientConnection->responseAccessesOffStrand_ = 0;

   boost::asio::post(fixture.pClientConnection->getStrand(), [&]() {
      fixture.pServerConnection->deliverChunk(upstream, body);
      headersWrittenBeforeQueueChunkReturned = fixture.pClientConnection->headersWritten_;
   });

   ioc.restart();
   ioc.run();

   EXPECT_TRUE(headersWrittenBeforeQueueChunkReturned);
   EXPECT_EQ(fixture.pClientConnection->responseAccessesOffStrand_, 0);
}

TEST(FixedBufferProxy, SetsConnectionCloseSinceItAlwaysClosesAfterTheResponse)
{
   // FixedBufferProxy unconditionally closes both connections once the body
   // finishes (unlike AsyncConnectionImpl::writeResponse(), which only does
   // so -- and only then sets this header -- when called with close=true).
   // writeResponseHeaders(), which this path uses instead of writeResponse(),
   // never sets Connection on its own, so this must be set explicitly or the
   // client is never told the connection is about to close.
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, ""); // completion signal

   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue("Connection"), "close");
}

TEST(FixedBufferProxy, FallsBackToChunkedFramingWhenUpstreamLengthUnknown)
{
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeChunkedResponse(&upstream);

   EXPECT_TRUE(fixture.deliver(upstream, body));
   EXPECT_TRUE(fixture.deliver(upstream, "")); // completion signal

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding),
             kChunkedTransferEncoding);
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length").empty());

   std::string expected =
      http::util::formatMessageAsHttpChunk(body) + http::util::formatMessageAsHttpChunk("");
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, expected);

   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

// --- RFC 7230 3.3.1: "A server MUST NOT send a response containing
// Transfer-Encoding unless the corresponding request indicates HTTP/1.1 (or
// later)." ---------------------------------------------------------------
//
// An HTTP/1.0 client does not de-chunk. Because FixedBufferProxy always sends
// Connection: close and always closes, such a client applies 3.3.3 rule 7 and
// reads to EOF -- so emitting chunked framing at it does not merely violate the
// spec, it hands the hex chunk-size lines to the user as body content. The
// conformant framing for "length unknown, HTTP/1.0 client" is no framing header
// at all, with the close delimiting the body.

TEST(FixedBufferProxy, Http10ClientGetsCloseDelimitedFramingRatherThanChunked)
{
   Fixture fixture;
   fixture.pClientConnection->setRequestHttpVersion(1, 0);

   std::string body = "hello";
   http::Response upstream;
   makeChunkedResponse(&upstream); // upstream length unknown

   EXPECT_TRUE(fixture.deliver(upstream, body));
   EXPECT_TRUE(fixture.deliver(upstream, "")); // completion signal

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length").empty());

   // raw body bytes, no chunk envelope and no 0\r\n\r\n terminator
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, body);

   // the close is what delimits the body, so it must still happen
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

TEST(FixedBufferProxy, Http10ClientStillGetsContentLengthFramingWhenLengthIsKnown)
{
   // The HTTP/1.0 fallback must be scoped to the unknown-length case only:
   // Content-Length framing is perfectly valid for an HTTP/1.0 client and is
   // strictly better than close-delimiting (the client can tell a complete
   // response from a truncated one).
   Fixture fixture;
   fixture.pClientConnection->setRequestHttpVersion(1, 0);

   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length"), "5");
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, body);
}

TEST(FixedBufferProxy, Http10CloseDelimitedFramingDropsUnparseableUpstreamContentLength)
{
   // An unparseable Content-Length is why we fell back to a self-generated
   // framing in the first place (AsyncClient likewise reads to EOF in that
   // case). Forwarding the bogus value would leave the client acting on a
   // length this hop is not honoring.
   Fixture fixture;
   fixture.pClientConnection->setRequestHttpVersion(1, 0);

   std::string body = "hello";
   http::Response upstream;
   upstream.setStatusCode(200);
   upstream.setHeader("Content-Length", "not-a-number");

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length").empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, body);
}

TEST(FixedBufferProxy, ClientsNewerThanHttp11StillGetChunkedFraming)
{
   // The gate is "HTTP/1.1 or later", not "exactly HTTP/1.1".
   Fixture fixture;
   fixture.pClientConnection->setRequestHttpVersion(2, 0);

   std::string body = "hello";
   http::Response upstream;
   makeChunkedResponse(&upstream);

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding),
             kChunkedTransferEncoding);
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_,
             http::util::formatMessageAsHttpChunk(body) + http::util::formatMessageAsHttpChunk(""));
}

// --- RFC 7230 3.3.3 rule 1: responses to HEAD and 1xx/204/304 responses
// "cannot contain a message body" ------------------------------------------
//
// These are terminated by the first empty line after the headers regardless of
// what framing headers are present, so FixedBufferProxy must emit no framing
// header it would then have to terminate, and no body bytes. For 204 and 1xx,
// 3.3.1 additionally forbids Transfer-Encoding outright and 3.3.2 forbids
// Content-Length.

TEST(FixedBufferProxy, NoTransferEncodingOrBodyForNoContentResponse)
{
   Fixture fixture;
   http::Response upstream;
   upstream.setStatusCode(http::status::NoContent); // no Content-Length, as is common

   EXPECT_TRUE(fixture.deliver(upstream, "")); // completion signal

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length").empty());

   // no body at all -- in particular no 0\r\n\r\n chunked terminator
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());

   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

TEST(FixedBufferProxy, RemovesContentLengthFromNoContentResponse)
{
   // RFC 7230 3.3.2: "A server MUST NOT send a Content-Length header field in
   // any response with a status code of 1xx (Informational) or 204 (No
   // Content)." A Content-Length: 0 on a 204 is a common upstream habit.
   Fixture fixture;
   http::Response upstream;
   upstream.setStatusCode(http::status::NoContent);
   upstream.setHeader("Content-Length", "0");

   fixture.deliver(upstream, "");

   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length").empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
}

TEST(FixedBufferProxy, NoTransferEncodingOrBodyForInformationalResponse)
{
   // ResponseParser has no interim-response handling, so a 1xx an upstream
   // emits (103 Early Hints, or a 100 Continue we solicited) is parsed as *the*
   // response and streamed. 101 never reaches here -- the localhost proxy's
   // buffer predicate routes websocket upgrades to the buffered path.
   Fixture fixture;
   http::Response upstream;
   upstream.setStatusCode(103); // Early Hints

   fixture.deliver(upstream, "");

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length").empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
}

TEST(FixedBufferProxy, DiscardsBodyBytesOnAResponseThatCannotHaveABody)
{
   // A malformed upstream that sends a body on a 204: the client will not read
   // one (3.3.3 rule 1 terminates the message at the blank line), so anything
   // we relayed would sit on the wire as the start of a different message --
   // the response-smuggling shape. Drop it.
   Fixture fixture;
   http::Response upstream;
   upstream.setStatusCode(http::status::NoContent);
   upstream.setHeader("Content-Length", "5");

   EXPECT_TRUE(fixture.deliver(upstream, "hello"));
   EXPECT_TRUE(fixture.deliver(upstream, ""));

   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length").empty());
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

TEST(FixedBufferProxy, NotModifiedResponseKeepsContentLengthButWritesNoBody)
{
   // Unlike 1xx/204, a 304 MAY carry Content-Length (3.3.2) -- it describes the
   // body the equivalent GET would have returned, and caches want it. So it
   // stays; what must not appear is a body or a framing header we'd terminate.
   Fixture fixture;
   http::Response upstream;
   upstream.setStatusCode(http::status::NotModified);
   upstream.setHeader("Content-Length", "1000");
   upstream.setHeader("ETag", "\"abc\"");

   fixture.deliver(upstream, "");

   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length"), "1000");
   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue("ETag"), "\"abc\"");
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
   EXPECT_TRUE(fixture.pClientConnection->closed_);
}

TEST(FixedBufferProxy, HeadResponseKeepsContentLengthButWritesNoBody)
{
   Fixture fixture;
   fixture.pClientConnection->setRequestMethod("HEAD");

   http::Response upstream;
   upstream.setStatusCode(200);
   upstream.setHeader("Content-Length", "1000"); // length of the GET body

   fixture.deliver(upstream, "");

   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length"), "1000");
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
   EXPECT_TRUE(fixture.pClientConnection->closed_);
}

TEST(FixedBufferProxy, HeadResponseWithUnknownLengthEmitsNoTransferEncodingOrTerminator)
{
   // The status code alone would have selected chunked framing here; it is the
   // HEAD request method that makes the response body-less.
   Fixture fixture;
   fixture.pClientConnection->setRequestMethod("HEAD");

   http::Response upstream;
   makeChunkedResponse(&upstream);

   fixture.deliver(upstream, "");

   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
   EXPECT_TRUE(fixture.pClientConnection->closed_);
}

TEST(FixedBufferProxy, NoTransferEncodingOnASuccessfulResponseToConnect)
{
   // RFC 7230 3.3.1: "A server MUST NOT send a Transfer-Encoding header field
   // in any 2xx (Successful) response to a CONNECT request." Reachable rather
   // than theoretical: AsyncServerImpl's method allowlist (which would reject
   // CONNECT with a 405) is deliberately skipped for proxy handlers -- "proxy
   // handlers do not perform this checking as we flow all traffic like a
   // proxy" -- and /p/ and /p6/ are registered with addProxyHandler(). So a
   // CONNECT whose request-target routes to the port proxy is forwarded to the
   // user's app like any other method.
   Fixture fixture;
   fixture.pClientConnection->setRequestMethod("CONNECT");

   http::Response upstream;
   upstream.setStatusCode(200); // no Content-Length

   fixture.deliver(upstream, "");

   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
   EXPECT_TRUE(fixture.pClientConnection->closed_);
}

TEST(FixedBufferProxy, NonSuccessfulResponseToConnectKeepsOrdinaryFraming)
{
   // RFC 7231 4.3.6: any response to CONNECT other than a successful one means
   // no tunnel was formed and the connection "remains governed by HTTP" -- so
   // an error response to a CONNECT is framed like any other error response,
   // body and all. Scoping the rule above to 2xx is what keeps a 405/502 from
   // the upstream reaching the client with its body silently dropped.
   Fixture fixture;
   fixture.pClientConnection->setRequestMethod("CONNECT");

   std::string body = "method not allowed";
   http::Response upstream;
   upstream.setStatusCode(405);
   upstream.setHeader("Content-Length", static_cast<uintmax_t>(body.size()));

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, body);
   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length"),
             std::to_string(body.size()));
}

TEST(FixedBufferProxy, StripsUpstreamTransferEncodingWhenContentLengthFraming)
{
   Fixture fixture;
   std::string body = "hi";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);
   // Belt-and-suspenders: an upstream that (incorrectly) declares both should
   // never happen in practice (framing_ is decided from containsHeader +
   // !upstreamChunked), but confirm the outbound headers never carry the
   // hop-by-hop Transfer-Encoding regardless of framing chosen.
   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding).empty());
}

TEST(FixedBufferProxy, StripsHopByHopHeadersFromUpstreamResponse)
{
   // Regression test for PR #18541 review discussion r3786635259: only
   // Transfer-Encoding was being stripped from the upstream response, so
   // other hop-by-hop headers (e.g. an upstream backend's own
   // "Connection: keep-alive") were forwarded verbatim to the client on a
   // connection this proxy unconditionally closes after the body -- and
   // Connection can itself nominate additional per-message headers to strip.
   Fixture fixture;
   std::string body = "hi";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);
   upstream.setHeader("Connection", "keep-alive, X-Upstream-Only");
   upstream.setHeader("Keep-Alive", "timeout=5");
   upstream.setHeader("Proxy-Connection", "keep-alive");
   upstream.setHeader("Proxy-Authenticate", "Basic");
   upstream.setHeader("Proxy-Authorization", "Basic abc123");
   upstream.setHeader("TE", "trailers");
   upstream.setHeader("Trailer", "X-Checksum");
   upstream.setHeader("Upgrade", "websocket");
   upstream.setHeader("X-Upstream-Only", "should-be-stripped");
   upstream.setHeader("X-End-To-End", "should-survive");

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   const http::Response& written = fixture.pClientConnection->writtenHeaders_;
   // The proxy sets its own Connection: close (see
   // SetsConnectionCloseSinceItAlwaysClosesAfterTheResponse) after stripping
   // hop-by-hop headers, so its presence here must be that, not the
   // upstream's forwarded "keep-alive" value.
   EXPECT_EQ(written.headerValue("Connection"), "close");
   EXPECT_TRUE(written.headerValue("Keep-Alive").empty());
   EXPECT_TRUE(written.headerValue("Proxy-Connection").empty());
   EXPECT_TRUE(written.headerValue("Proxy-Authenticate").empty());
   EXPECT_TRUE(written.headerValue("Proxy-Authorization").empty());
   EXPECT_TRUE(written.headerValue("TE").empty());
   EXPECT_TRUE(written.headerValue("Trailer").empty());
   EXPECT_TRUE(written.headerValue("Upgrade").empty());
   EXPECT_TRUE(written.headerValue("X-Upstream-Only").empty());
   EXPECT_EQ(written.headerValue("X-End-To-End"), "should-survive");
}

TEST(FixedBufferProxy, HonorsHeadersNominatedByEveryConnectionFieldNotJustTheFirst)
{
   // Regression test: a response can carry more than one Connection header
   // field (each itself a comma-separated list per RFC 7230 6.1). Using
   // headerValue() (which only sees the first field) would miss header names
   // nominated by any later Connection field.
   Fixture fixture;
   std::string body = "hi";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);
   upstream.addHeader("Connection", "X-First-Nominated");
   upstream.addHeader("Connection", "X-Second-Nominated");
   upstream.setHeader("X-First-Nominated", "should-be-stripped");
   upstream.setHeader("X-Second-Nominated", "should-be-stripped");

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   const http::Response& written = fixture.pClientConnection->writtenHeaders_;
   EXPECT_TRUE(written.headerValue("X-First-Nominated").empty());
   EXPECT_TRUE(written.headerValue("X-Second-Nominated").empty());
}

TEST(FixedBufferProxy, UpstreamCannotNominateSetCookieToStripProxysOwnRefreshedCookie)
{
   // Regression test: hop-by-hop stripping must run against the upstream's
   // own headers *before* the proxy's preserved Set-Cookie (e.g. a refreshed
   // auth cookie stamped before the proxy request executed, see
   // PreservesSetCookieAlreadyStampedOnClientResponse) is restored. Otherwise
   // a misbehaving or malicious upstream could send "Connection: Set-Cookie"
   // and have the proxy's own re-added cookie stripped along with it.
   Fixture fixture(1024 * 1024, {http::Header("Set-Cookie", "auth=refreshed")});

   std::string body = "hi";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);
   upstream.setHeader("Connection", "Set-Cookie");

   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   bool found = false;
   for (const http::Header& header : fixture.pClientConnection->writtenHeaders_.headers())
   {
      if (boost::iequals(header.name, "Set-Cookie") && header.value == "auth=refreshed")
         found = true;
   }
   EXPECT_TRUE(found);
}

TEST(FixedBufferProxy, PreservesSetCookieAlreadyStampedOnClientResponse)
{
   Fixture fixture(1024 * 1024, {http::Header("Set-Cookie", "auth=refreshed")});

   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);
   fixture.deliver(upstream, body);
   fixture.deliver(upstream, "");

   bool found = false;
   for (const http::Header& header : fixture.pClientConnection->writtenHeaders_.headers())
   {
      if (boost::iequals(header.name, "Set-Cookie") && header.value == "auth=refreshed")
         found = true;
   }
   EXPECT_TRUE(found);
}

TEST(FixedBufferProxy, ClosesConnectionsForEmptyContentLengthZeroResponse)
{
   Fixture fixture;
   http::Response upstream;
   makeContentLengthResponse(&upstream, "");

   // The only queueChunk call is the empty final chunk (e.g. Content-Length: 0
   // or a 204). Headers must still be written and the connection closed even
   // though no body chunk is ever enqueued.
   EXPECT_TRUE(fixture.deliver(upstream, ""));

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length"), "0");
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, "");
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
   // This hits writeChunk()'s receivedFinal_-with-nothing-to-flush close
   // site -- confirm it, like every other close site, breaks the
   // FixedBufferProxy <-> AsyncClient reference cycle via disableHandlers(),
   // not just the raw close() pair.
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);
}

TEST(FixedBufferProxy, DoesNotDuplicateWriteWhenFinalSignalArrivesBeforeHeaderWriteCompletes)
{
   // Regression test for a race: in Content-Length framing the empty final
   // completion signal enqueues no new bytes (nothing to terminate), so if it
   // arrives before the *header write's* completion handler (onHeadersWrote)
   // has run, writeBuffer_ still shows exactly the one body chunk queued by
   // the first call. Without an explicit "write in progress" guard, FixedBufferProxy
   // would treat that as "the only chunk in the buffer, kick off a write" and
   // call writeChunk() a second time on the same still-unpopped front() entry
   // once onHeadersWrote() also calls writeChunk() -- duplicating the write
   // and eventually running onChunkWrote() against an already-emptied queue.
   //
   // Unlike Fixture::deliver(), which polls the io_context to completion after
   // every chunk (serializing header-write completion before the next chunk
   // is delivered), this test invokes the fixed buffer handler twice back-to-back
   // *before* draining anything, to reproduce the interleaving.
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   bool result1 = fixture.pServerConnection->deliverChunk(upstream, body);
   bool result2 = fixture.pServerConnection->deliverChunk(upstream, ""); // completion signal, before header write completes
   EXPECT_TRUE(result1);
   EXPECT_TRUE(result2);

   boost::asio::io_context& ioc = fixture.pClientConnection->ioContext();
   ioc.restart();
   ioc.poll();

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   // The body must be written exactly once -- not duplicated by a second,
   // concurrent asyncWrite of the same buffered chunk.
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, body);
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

TEST(FixedBufferProxy, AccountsBufferSizeAgainstFormattedNotRawBytesInChunkedMode)
{
   // "hi" is 2 raw bytes but its chunked envelope ("2\r\nhi\r\n") is 7 bytes.
   // queueChunk() now accepts unconditionally into an empty, idle buffer (see
   // AcceptsOversizedFirstChunkWhenBufferIsIdle below), so to exercise the
   // decline path this delivers a second piece while the first's header write
   // is still outstanding (non-idle) -- mirroring
   // DoesNotDuplicateWriteWhenFinalSignalArrivesBeforeHeaderWriteCompletes.
   // A maxBufferSize sized to exactly fit only the first piece's *formatted*
   // bytes must decline the second: if accounting instead used the smaller raw
   // chunk sizes, both would fit and the decline would never fire.
   std::string first = "hi";
   std::string firstFormatted = http::util::formatMessageAsHttpChunk(first);
   std::string second = "hello";
   ASSERT_GT(firstFormatted.size(), first.size());

   Fixture fixture(/* maxBufferSize = */ firstFormatted.size());
   http::Response upstream;
   makeChunkedResponse(&upstream);

   bool result1 = fixture.pServerConnection->deliverChunk(upstream, first);
   bool result2 = fixture.pServerConnection->deliverChunk(upstream, second);

   EXPECT_TRUE(result1);
   EXPECT_FALSE(result2);
}

TEST(FixedBufferProxy, AcceptsOversizedFirstChunkWhenBufferIsIdle)
{
   // Regression test for PR #18541 review discussion r3786635238:
   // AsyncClient::breakChunks() caps upstream pieces at maxChunkSize (1MB),
   // which chunked-envelopes to slightly more than 1MB. If maxBufferSize_ is
   // at or below that enveloped size, the very first piece into an empty,
   // idle buffer must still be accepted -- there is no write completion that
   // will ever retry a piece declined with nothing in flight, so declining it
   // here would hang both connections until client timeout.
   std::string body(1024 * 1024, 'x'); // matches AsyncClient::maxChunkSize
   std::string formatted = http::util::formatMessageAsHttpChunk(body);

   Fixture fixture(/* maxBufferSize = */ formatted.size() - 1);
   http::Response upstream;
   makeChunkedResponse(&upstream);

   EXPECT_TRUE(fixture.deliver(upstream, body));
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, formatted);
}

TEST(FixedBufferProxy, RedeliveredCompletionSignalStillWritesTerminatorAfterOversizedChunk)
{
   // Regression test for a race introduced by the empty-buffer escape hatch in
   // queueChunk() (see AcceptsOversizedFirstChunkWhenBufferIsIdle above): once
   // an oversized first chunk is accepted into an idle buffer, a chunked
   // completion signal delivered immediately after -- before that chunk's
   // write completes -- itself exceeds maxBufferSize_ and must be declined,
   // not silently treated as received. If receivedFinal_ were latched on the
   // declined attempt, onChunkWrote() would close both connections as soon as
   // the oversized write drains, without ever writing the required
   // 0\r\n\r\n terminator or redelivering the completion signal.
   std::string body(1024 * 1024, 'x');
   std::string formatted = http::util::formatMessageAsHttpChunk(body);

   Fixture fixture(/* maxBufferSize = */ formatted.size() - 1);
   http::Response upstream;
   makeChunkedResponse(&upstream);

   // Deliver the oversized body and the completion signal back-to-back,
   // before draining, so the completion signal arrives while the oversized
   // chunk's header/body write is still outstanding.
   bool result1 = fixture.pServerConnection->deliverChunk(upstream, body);
   bool result2 = fixture.pServerConnection->deliverChunk(upstream, "");
   EXPECT_TRUE(result1);
   EXPECT_FALSE(result2);

   boost::asio::io_context& ioc = fixture.pClientConnection->ioContext();
   ioc.restart();
   ioc.poll();

   // The oversized chunk has drained; the declined completion signal must be
   // resumable, not dropped, and the connections must still be open.
   EXPECT_TRUE(fixture.pServerConnection->resumed_);
   EXPECT_FALSE(fixture.pClientConnection->closed_);

   // Simulate AsyncClient's real redelivery of the completion signal on resume.
   EXPECT_TRUE(fixture.pServerConnection->deliverChunk(upstream, ""));
   ioc.restart();
   ioc.poll();

   std::string expected = formatted + http::util::formatMessageAsHttpChunk("");
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, expected);
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

TEST(FixedBufferProxy, RedeliveredCompletionSignalStillWritesTerminatorUnderOrdinaryBackpressure)
{
   // Regression test for PR #18541 review discussion r3786635248: the
   // originally-reported trigger was ordinary sustained backpressure mid-
   // stream (currentBufferSize_ a few bytes short of maxBufferSize_ from a
   // normal, non-oversized chunk already queued), not the oversized-single-
   // chunk edge case RedeliveredCompletionSignalStillWritesTerminatorAfter
   // OversizedChunk above covers. Confirms the same fix (only latching
   // receivedFinal_ once a piece is actually accepted) handles this more
   // realistic shape too.
   std::string body = "hello";
   std::string formatted = http::util::formatMessageAsHttpChunk(body);

   // Exactly 4 bytes of headroom remain after the body chunk is queued --
   // one byte short of the 5-byte "0\r\n\r\n" terminator.
   Fixture fixture(/* maxBufferSize = */ formatted.size() + 4);
   http::Response upstream;
   makeChunkedResponse(&upstream);

   // Deliver the body and the completion signal back-to-back, before
   // draining, so the completion signal arrives while the body chunk's
   // header/body write is still outstanding (not idle).
   bool result1 = fixture.pServerConnection->deliverChunk(upstream, body);
   bool result2 = fixture.pServerConnection->deliverChunk(upstream, "");
   EXPECT_TRUE(result1);
   EXPECT_FALSE(result2);

   boost::asio::io_context& ioc = fixture.pClientConnection->ioContext();
   ioc.restart();
   ioc.poll();

   // The body chunk has drained; the declined completion signal must be
   // resumable, not dropped, and the connections must still be open.
   EXPECT_TRUE(fixture.pServerConnection->resumed_);
   EXPECT_FALSE(fixture.pClientConnection->closed_);

   // Simulate AsyncClient's real redelivery of the completion signal on resume.
   EXPECT_TRUE(fixture.pServerConnection->deliverChunk(upstream, ""));
   ioc.restart();
   ioc.poll();

   std::string expected = formatted + http::util::formatMessageAsHttpChunk("");
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, expected);
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

TEST(FixedBufferProxy, FallsBackToChunkedWhenUpstreamContentLengthHeaderIsMalformed)
{
   // A non-numeric (or otherwise unparseable) Content-Length must not be
   // trusted for framing -- AsyncClient falls back to EOF-delimited reading
   // when Content-Length doesn't parse (see responseBodyComplete()/
   // streamedBodyComplete() in AsyncClient.hpp), so FixedBufferProxy's framing
   // decision must apply the same validity rule rather than choosing
   // Content-Length framing from bare header presence and forwarding the
   // invalid value verbatim to the browser.
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   upstream.setStatusCode(200);
   upstream.setHeader("Content-Length", "abc");

   EXPECT_TRUE(fixture.deliver(upstream, body));
   EXPECT_TRUE(fixture.deliver(upstream, "")); // completion signal

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);
   EXPECT_EQ(fixture.pClientConnection->writtenHeaders_.headerValue(kTransferEncoding),
             kChunkedTransferEncoding);
   EXPECT_TRUE(fixture.pClientConnection->writtenHeaders_.headerValue("Content-Length").empty());

   std::string expected =
      http::util::formatMessageAsHttpChunk(body) + http::util::formatMessageAsHttpChunk("");
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, expected);
}

TEST(FixedBufferProxy, QueueChunkDeclinesWhenContentLengthFramedBodyExceedsBufferSize)
{
   // Mirrors AccountsBufferSizeAgainstFormattedNotRawBytesInChunkedMode, but
   // for Content-Length framing, where the bytes enqueued are raw (no
   // envelope). This is likely the more common way large /p/<port>/ downloads
   // fill the buffer in practice, since Content-Length is preferred over
   // chunked whenever the upstream declares a length -- but only the chunked
   // buffer-full path had coverage.
   //
   // As above, queueChunk() now accepts unconditionally into an empty, idle
   // buffer, so the decline is exercised on a second piece delivered while
   // the first's header write is still outstanding (non-idle).
   std::string first = "a";
   std::string second = "hello";

   Fixture fixture(/* maxBufferSize = */ first.size());
   http::Response upstream;
   makeContentLengthResponse(&upstream, first + second);

   bool result1 = fixture.pServerConnection->deliverChunk(upstream, first);
   bool result2 = fixture.pServerConnection->deliverChunk(upstream, second);

   EXPECT_TRUE(result1);
   EXPECT_FALSE(result2);
}

TEST(FixedBufferProxy, HandlesDownstreamWriteFailureDuringBodyWrite)
{
   // Real-world trigger: the browser's connection to rserver drops mid-
   // download (tab closed, network drop, laptop sleeps) while a body chunk
   // is queued for write. FixedBufferProxy::handleError() exists for exactly
   // this, but was previously never exercised by any test -- every fake
   // write unconditionally succeeded.
   Fixture fixture;
   std::string body = "helloworld";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   // First piece writes successfully (headers + first chunk).
   EXPECT_TRUE(fixture.deliver(upstream, "hello"));
   ASSERT_TRUE(fixture.pClientConnection->headersWritten_);
   ASSERT_FALSE(fixture.pClientConnection->closed_);

   // The next body chunk's write fails.
   fixture.pClientConnection->failNextWrite(boost::asio::error::broken_pipe);
   fixture.deliver(upstream, "world");

   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);

   // Regression coverage for a reference-cycle leak: proxy() bound queueChunk()
   // into AsyncClient's fixedBufferHandler_ via shared_from_this(), and
   // AsyncClient::close() alone never clears it (its own internal
   // disableHandlers() call is unreachable once we've already closed it
   // ourselves). Without handleError() calling disableHandlers() explicitly,
   // this FixedBufferProxy, pClientConnection_, and any buffered chunks would
   // leak forever on every routine mid-download browser disconnect.
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);
   // The Fixture's own pProxy is the only remaining reference once the fake's
   // fixedBufferHandler_ has actually been cleared.
   EXPECT_EQ(fixture.pProxy.use_count(), 1);
}

TEST(FixedBufferProxy, HandlesDownstreamWriteFailureDuringHeaderWrite)
{
   // Same real-world trigger as HandlesDownstreamWriteFailureDuringBodyWrite,
   // but the disconnect happens even earlier -- before any body byte is sent.
   // onHeadersWrote() must short-circuit via handleError() before writeChunk()
   // ever runs.
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   fixture.pClientConnection->failNextWrite(boost::asio::error::broken_pipe);
   fixture.deliver(upstream, body);

   EXPECT_TRUE(fixture.pClientConnection->headersWritten_); // attempted, even though it failed
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);

   // See HandlesDownstreamWriteFailureDuringBodyWrite: same reference-cycle
   // leak, but via onHeadersWrote()'s call into handleError() instead.
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);
   EXPECT_EQ(fixture.pProxy.use_count(), 1);
}

TEST(FixedBufferProxy, LosingTheResponseClaimDetachesWithoutClosingTheClientConnection)
{
   // already_started is not a transport failure like the broken_pipe above: it
   // is AsyncConnectionImpl::claimResponse() reporting that another writer on
   // this connection got to its one response first (its read path turning a
   // request-parse error into writeResponse(BadRequest), say). That winner's
   // write may still be in flight, so unlike every other error path here we
   // must NOT close the client connection -- doing so would truncate or reset
   // the very response the claim exists to protect. Detach from the upstream
   // and leave the client to its winner.
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   fixture.pClientConnection->failNextWrite(boost::asio::error::already_started);
   fixture.deliver(upstream, body);

   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
   EXPECT_FALSE(fixture.pClientConnection->closed_);

   // but we do stop and let go of the upstream, or the buffered body and the
   // FixedBufferProxy <-> AsyncClient reference cycle leak (see
   // HandlesDownstreamWriteFailureDuringHeaderWrite)
   EXPECT_TRUE(fixture.pServerConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);
   EXPECT_EQ(fixture.pProxy.use_count(), 1);
}

TEST(FixedBufferProxy, MultipleQueuedChunksDrainInOrderAndAccountBufferCorrectly)
{
   // Real-world trigger: a fast localhost backend (Jetty/Shiny serving from
   // memory) emits several TCP reads' worth of body in quick succession
   // before the slower downstream browser write completes even once.
   // Existing tests only ever queue at most one chunk before draining --
   // this exercises writeBuffer_ actually holding multiple entries at once.
   Fixture fixture;
   std::string body = "one-two-three";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   bool r1 = fixture.pServerConnection->deliverChunk(upstream, "one-");
   bool r2 = fixture.pServerConnection->deliverChunk(upstream, "two-");
   bool r3 = fixture.pServerConnection->deliverChunk(upstream, "three");
   bool r4 = fixture.pServerConnection->deliverChunk(upstream, ""); // completion signal
   EXPECT_TRUE(r1);
   EXPECT_TRUE(r2);
   EXPECT_TRUE(r3);
   EXPECT_TRUE(r4);

   boost::asio::io_context& ioc = fixture.pClientConnection->ioContext();
   ioc.restart();
   ioc.poll();

   // All three pieces must arrive in order, with none skipped or duplicated.
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, body);
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
}

// An exception escaping the header assembly must not unwind out of the proxy.
// writeHeaders() runs from the client connection's strand whenever
// queueChunk()'s dispatch is not inline (as here -- the test thread is never in
// the strand, so deliver()'s poll() is what runs it), which in production means
// unwinding through boost::asio and out of AsyncServerImpl::runServiceThread()'s
// ioContext().run(), retiring a server worker thread for good. Instead the
// proxy must log, close both ends, and detach.
TEST(FixedBufferProxy, HeaderWriteThrowTearsDownInsteadOfEscaping)
{
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   fixture.pClientConnection->throwOnNextWrite();

   // The throw happens inside poll(), which deliver() calls -- so this asserts
   // the guard is what caught it, not the test harness.
   EXPECT_NO_THROW(fixture.deliver(upstream, body));

   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);

   // Nothing partial reached the client: the body write is only initiated by
   // onHeadersWrote, which never ran.
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());

   // disableHandlers() broke the cycle, so the fixture holds the only reference
   // -- without it this proxy, both connections and every buffered chunk leak.
   EXPECT_EQ(fixture.pProxy.use_count(), 1);
}

// The same requirement for a throw from a body write, which arrives inside
// queueChunk()'s locked region (via writeChunk()) rather than from
// writeHeaders(). Falling through to `return true` there would report this
// chunk as delivered when it was dropped, leaving the client a
// Content-Length-framed body with a hole in it; and because
// clientWriteInProgress_ is latched by writeChunk() before asyncWrite(), no
// completion handler would ever run to clear it.
TEST(FixedBufferProxy, BodyWriteThrowTearsDownInsteadOfReportingChunkDelivered)
{
   Fixture fixture;
   http::Response upstream;
   makeContentLengthResponse(&upstream, "firstsecond");

   // First piece goes out cleanly, so headers are written and no write is
   // outstanding -- which is what puts the *next* queueChunk() on the path that
   // calls writeChunk() itself.
   EXPECT_TRUE(fixture.deliver(upstream, "first"));
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, "first");

   fixture.pClientConnection->throwOnNextWrite();

   // queueChunk() still returns true: `false` means "pause and redeliver", and
   // after the teardown below nothing will ever resume this proxy.
   bool queued = false;
   EXPECT_NO_THROW(queued = fixture.deliver(upstream, "second"));
   EXPECT_TRUE(queued);

   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, "first"); // "second" dropped
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);
   EXPECT_EQ(fixture.pProxy.use_count(), 1);
}

// A teardown whose every step throws must still attempt every step, and must
// still contain what it could not tear down. Closing either connection can
// throw (they are virtual calls into asio), and chaining the steps -- as
// closeConnections()/closeUpstream() do on the ordinary paths -- would let the
// first failure abandon the rest. disableHandlers() is the costly one to skip:
// it breaks the FixedBufferProxy <-> AsyncClient reference cycle, and failed_
// means nothing will ever retry it.
TEST(FixedBufferProxy, FailedTeardownStillAttemptsEveryStepAndDiscardsLaterChunks)
{
   Fixture fixture;
   http::Response upstream;
   makeContentLengthResponse(&upstream, "firstsecondthird");

   EXPECT_TRUE(fixture.deliver(upstream, "first"));

   // Fail the body write, and then every step of the teardown it triggers --
   // including the client close, which runs first and so would take the other
   // two down with it if the steps were chained.
   fixture.pClientConnection->throwOnNextWrite();
   fixture.pClientConnection->throwOnNextClose();
   fixture.pServerConnection->throwOnNextClose();
   fixture.pServerConnection->throwOnNextDisableHandlers();
   EXPECT_NO_THROW(fixture.deliver(upstream, "second"));

   // Every step was still attempted, in spite of all of them throwing.
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);

   // That attempt threw before detaching, though, so the handler is still live
   // and the upstream delivers again. Accepted (true, not a pause -- nothing
   // will ever resume this proxy) and dropped, with nothing further written.
   bool queued = false;
   EXPECT_NO_THROW(queued = fixture.deliver(upstream, "third"));
   EXPECT_TRUE(queued);
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, "first");

   // Including the completion signal, which must not be able to write a
   // terminator or re-close either end.
   EXPECT_NO_THROW(queued = fixture.deliver(upstream, ""));
   EXPECT_TRUE(queued);
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, "first");
}

// Losing the response claim must leave the client connection alone even when
// the upstream teardown it does perform fails. handleErrorImpl() declines to
// close that connection because another writer owns its one response and may
// still be writing it; a throw on the way out of that path must not turn into
// the close it just declined, truncating the winner's response.
TEST(FixedBufferProxy, FailedUpstreamTeardownAfterLosingTheClaimSparesTheClient)
{
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   fixture.pClientConnection->failNextWrite(boost::asio::error::already_started);
   fixture.pServerConnection->throwOnNextClose();
   EXPECT_NO_THROW(fixture.deliver(upstream, body));

   // The whole point: still not ours to close, failing or not.
   EXPECT_FALSE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());

   // The upstream half proceeds as far as it can, including the detach the
   // throwing close would otherwise have skipped.
   EXPECT_TRUE(fixture.pServerConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);
   EXPECT_EQ(fixture.pProxy.use_count(), 1);
}

// The first of the two completion-handler guards. onHeadersWrote() clears
// clientWriteInProgress_ and then calls writeChunk(), so a throw from that
// write is caught by that function's locked region rather than by
// writeHeaders() or queueChunk(). Falling through instead would leave
// clientWriteInProgress_ latched by writeChunk() with no write in flight and
// no completion handler left to clear it: the upstream stays paused, neither
// connection is ever closed, and the proxy plus its buffered chunk leak.
TEST(FixedBufferProxy, FirstBodyWriteThrowFromHeaderCompletionTearsDown)
{
   Fixture fixture;
   http::Response upstream;
   makeContentLengthResponse(&upstream, "first");

   // Queue without draining, so the header write is still pending.
   EXPECT_TRUE(fixture.pServerConnection->deliverChunk(upstream, "first"));

   // Run just the dispatched writeHeaders(); its completion stays queued.
   fixture.runOneHandler();
   EXPECT_TRUE(fixture.pClientConnection->headersWritten_);

   // So the next write is the body write onHeadersWrote() initiates.
   fixture.pClientConnection->throwOnNextWrite();
   EXPECT_NO_THROW(fixture.drain());

   EXPECT_TRUE(fixture.pClientConnection->writtenBytes_.empty());
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);
   EXPECT_EQ(fixture.pProxy.use_count(), 1);
}

// The second one: with more than one piece queued, onChunkWrote() pops the
// piece it just wrote and calls writeChunk() for the next. A throw there is
// caught by onChunkWrote()'s own locked region, and must not leave the already
// popped-and-written bytes reported as progress on a stalled proxy.
TEST(FixedBufferProxy, SubsequentBodyWriteThrowFromChunkCompletionTearsDown)
{
   Fixture fixture;
   http::Response upstream;
   makeContentLengthResponse(&upstream, "firstsecond");

   // Both pieces queue up behind the still-pending header write.
   EXPECT_TRUE(fixture.pServerConnection->deliverChunk(upstream, "first"));
   EXPECT_TRUE(fixture.pServerConnection->deliverChunk(upstream, "second"));

   fixture.runOneHandler(); // writeHeaders()
   fixture.runOneHandler(); // onHeadersWrote() -> writes "first"
   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, "first");

   // The next write is the one onChunkWrote() initiates for "second".
   fixture.pClientConnection->throwOnNextWrite();
   EXPECT_NO_THROW(fixture.drain());

   EXPECT_EQ(fixture.pClientConnection->writtenBytes_, "first");
   EXPECT_TRUE(fixture.pClientConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->closed_);
   EXPECT_TRUE(fixture.pServerConnection->disableHandlersCalled_);
   EXPECT_EQ(fixture.pProxy.use_count(), 1);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
