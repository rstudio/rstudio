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
   http::Response& response() override { return response_; }

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
      headersWritten_ = true;
      writtenHeaders_.assign(response_);
      boost::system::error_code ec = takeNextWriteError();
      boost::asio::post(ioc_, [handler, ec]() { handler(ec, 0); });
   }

   void writeError(const Error&) override {}

   void close() override { closed_ = true; }

   void continueParsing() override {}
   void setData(const boost::any& data) override { data_ = data; }
   boost::any getData() override { return data_; }
   const std::string& username() const override { return username_; }
   void setUsername(const std::string& username) override { username_ = username; }
   const std::string& handlerPrefix() const override { return handlerPrefix_; }
   void setHandlerPrefix(const std::string& prefix) override { handlerPrefix_ = prefix; }
   boost::asio::io_context::strand& getStrand() override { return *pStrand_; }

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}

   void asyncWrite(const boost::asio::const_buffer& buffer, Socket::Handler handler) override
   {
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

   bool headersWritten_ = false;
   bool closed_ = false;
   std::string writtenBytes_;
   http::Response writtenHeaders_; // snapshot at header-write time

private:
   boost::system::error_code takeNextWriteError()
   {
      boost::system::error_code ec = nextWriteError_;
      nextWriteError_ = boost::system::error_code();
      return ec;
   }

   boost::system::error_code nextWriteError_;
   boost::asio::io_context ioc_;
   http::Request request_;
   http::Response response_;
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
   boost::asio::io_context::strand* pStrand_ = nullptr;
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
   void setConnectHandler(const ConnectHandler&) override {}
   void resumeChunkProcessing() override { resumed_ = true; }
   void disableHandlers() override {}
   void close() override { closed_ = true; }
   void setStrand(boost::asio::io_context::strand*) override {}

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}
   void asyncWrite(const boost::asio::const_buffer&, Socket::Handler) override {}
   void asyncWrite(const std::vector<boost::asio::const_buffer>&, Socket::Handler) override {}

   FixedBufferHandler fixedBufferHandler_;
   bool resumed_ = false;
   bool closed_ = false;
   bool fixedBufferHandlerSupportsPause_ = false;

private:
   http::Request request_;
};

struct Fixture
{
   Fixture(uint64_t maxBufferSize = 1024 * 1024)
   {
      pClientConnection = boost::make_shared<FakeAsyncConnection>();
      pServerConnection = boost::make_shared<FakeAsyncClient>();
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
      bool result = pServerConnection->fixedBufferHandler_(upstream, chunk);

      // io_context::poll() marks the context "stopped" once it runs out of
      // ready work; it must be restarted before it will process any handlers
      // posted by a later call (each deliver() call posts new work).
      boost::asio::io_context& ioc = pClientConnection->ioContext();
      ioc.restart();
      ioc.poll();

      return result;
   }

   boost::shared_ptr<FakeAsyncConnection> pClientConnection;
   boost::shared_ptr<FakeAsyncClient> pServerConnection;
   boost::shared_ptr<FixedBufferProxy> pProxy;
};

void makeContentLengthResponse(http::Response* pResponse, const std::string& body)
{
   pResponse->setStatusCode(200);
   pResponse->setHeader("Content-Length", body.size());
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

TEST(FixedBufferProxy, PreservesSetCookieAlreadyStampedOnClientResponse)
{
   Fixture fixture;
   fixture.pClientConnection->response().setHeader(http::Header("Set-Cookie", "auth=refreshed"));

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

   bool result1 = fixture.pServerConnection->fixedBufferHandler_(upstream, body);
   bool result2 = fixture.pServerConnection->fixedBufferHandler_(upstream, ""); // completion signal, before header write completes
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
   // "hello" is 5 raw bytes but its chunked envelope ("5\r\nhello\r\n") is 10
   // bytes. A maxBufferSize between those two values must report bufferFull_
   // (queueChunk returns false) -- confirming currentBufferSize_ is accounted
   // against the formatted (enveloped) size, matching what onChunkWrote()
   // subtracts, not the smaller raw chunk size (eval P3).
   std::string body = "hello";
   std::string formatted = http::util::formatMessageAsHttpChunk(body);
   ASSERT_GT(formatted.size(), body.size());

   Fixture fixture(/* maxBufferSize = */ formatted.size() - 1);
   http::Response upstream;
   makeChunkedResponse(&upstream);

   EXPECT_FALSE(fixture.deliver(upstream, body));
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
   std::string body = "hello";

   Fixture fixture(/* maxBufferSize = */ body.size() - 1);
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   EXPECT_FALSE(fixture.deliver(upstream, body));
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

   bool r1 = fixture.pServerConnection->fixedBufferHandler_(upstream, "one-");
   bool r2 = fixture.pServerConnection->fixedBufferHandler_(upstream, "two-");
   bool r3 = fixture.pServerConnection->fixedBufferHandler_(upstream, "three");
   bool r4 = fixture.pServerConnection->fixedBufferHandler_(upstream, ""); // completion signal
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

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
