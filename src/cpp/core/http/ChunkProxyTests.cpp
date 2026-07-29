/*
 * ChunkProxyTests.cpp
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

// Coverage for the ChunkProxy client-facing framing decision (rstudio-pro-11740
// Step 2): once AsyncClient (Step 1) can feed ChunkProxy both chunked and
// Content-Length-delimited response bodies piece-wise, ChunkProxy must choose,
// once at header time, whether to forward Content-Length framing (raw body
// bytes, upstream Content-Length preserved) or fall back to chunked framing
// (enveloped pieces, Transfer-Encoding: chunked) -- and account buffer usage
// against the bytes actually enqueued in both modes.

#include <string>

#include <boost/algorithm/string/predicate.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/asio/post.hpp>
#include <boost/make_shared.hpp>

#include <core/http/AsyncConnection.hpp>
#include <core/http/AsyncClient.hpp>
#include <core/http/ChunkProxy.hpp>
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
// ChunkProxy state machine can be exercised without a real event loop.
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
   // re-entrantly on the same call stack as the ChunkProxy method that
   // initiated the write -- ChunkProxy holds its own mutex_ across the call
   // into writeResponseHeaders()/asyncWrite(), and its completion handlers
   // (onHeadersWrote/onChunkWrote) re-acquire that same (non-recursive) mutex.
   // So these fakes post to the io_context instead of invoking the handler
   // inline; Fixture::deliver() drains the queue with ioc_.poll() once the
   // outer queueChunk() call (and its lock) has returned.
   void writeResponseHeaders(Socket::Handler handler) override
   {
      headersWritten_ = true;
      writtenHeaders_.assign(response_);
      boost::asio::post(ioc_, [handler]() { handler(boost::system::error_code(), 0); });
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
      writtenBytes_.append(static_cast<const char*>(buffer.data()), buffer.size());
      std::size_t n = buffer.size();
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

   bool headersWritten_ = false;
   bool closed_ = false;
   std::string writtenBytes_;
   http::Response writtenHeaders_; // snapshot at header-write time

private:
   boost::asio::io_context ioc_;
   http::Request request_;
   http::Response response_;
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
   boost::asio::io_context::strand* pStrand_ = nullptr;
};

// A fake IAsyncClient that just captures the ChunkHandler passed to
// setChunkHandler() so the test can drive ChunkProxy::queueChunk() directly,
// and tracks close()/resumeChunkProcessing() calls.
class FakeAsyncClient : public IAsyncClient
{
public:
   http::Request& request() override { return request_; }
   void setConnectionRetryProfile(const http::ConnectionRetryProfile&) override {}
   void setRequestTimeout(const boost::posix_time::time_duration&) override {}
   void execute(const ResponseHandler&, const ErrorHandler&, const ChunkHandler&) override {}
   void setChunkHandler(const ChunkHandler& chunkHandler) override { chunkHandler_ = chunkHandler; }
   void setStreamNonChunkedResponses(bool) override {}
   void setBufferPredicate(const boost::function<bool(const http::Response&)>&) override {}
   void setChunkHandlerSupportsPause(bool supportsPause) override { chunkHandlerSupportsPause_ = supportsPause; }
   void setConnectHandler(const ConnectHandler&) override {}
   void resumeChunkProcessing() override { resumed_ = true; }
   void disableHandlers() override {}
   void close() override { closed_ = true; }
   void setStrand(boost::asio::io_context::strand*) override {}

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}
   void asyncWrite(const boost::asio::const_buffer&, Socket::Handler) override {}
   void asyncWrite(const std::vector<boost::asio::const_buffer>&, Socket::Handler) override {}

   ChunkHandler chunkHandler_;
   bool resumed_ = false;
   bool closed_ = false;
   bool chunkHandlerSupportsPause_ = false;

private:
   http::Request request_;
};

struct Fixture
{
   Fixture(uint64_t maxBufferSize = 1024 * 1024)
   {
      pClientConnection = boost::make_shared<FakeAsyncConnection>();
      pServerConnection = boost::make_shared<FakeAsyncClient>();
      pProxy = boost::make_shared<ChunkProxy>(pClientConnection, maxBufferSize);
      pProxy->proxy(pServerConnection);
   }

   // Deliver one piece the way AsyncClient's chunkHandler_ invocation would,
   // then drain the fake connection's io_context so any posted
   // writeResponseHeaders/asyncWrite completion handlers (onHeadersWrote,
   // onChunkWrote, and whatever further writes/closes they chain into) run to
   // completion before returning -- mirroring how those completions would
   // eventually fire asynchronously in production.
   bool deliver(const http::Response& upstream, const std::string& chunk)
   {
      bool result = pServerConnection->chunkHandler_(upstream, chunk);

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
   boost::shared_ptr<ChunkProxy> pProxy;
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

// ChunkProxy::proxy() must opt the connection into pause-aware completion
// handling (AsyncClient::closeAndRespond() otherwise discards a declined
// completion signal -- see completionPending_/setChunkHandlerSupportsPause()
// in AsyncClient.hpp for the full rationale). Without this, a completion
// signal declined while ChunkProxy's own buffer is exactly full would never
// be retried, leaving both connections open indefinitely.
TEST(ChunkProxy, ProxyOptsIntoChunkHandlerPauseSupport)
{
   Fixture fixture;
   EXPECT_TRUE(fixture.pServerConnection->chunkHandlerSupportsPause_);
}

TEST(ChunkProxy, UsesContentLengthFramingWhenUpstreamLengthKnown)
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

TEST(ChunkProxy, FallsBackToChunkedFramingWhenUpstreamLengthUnknown)
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

TEST(ChunkProxy, StripsUpstreamTransferEncodingWhenContentLengthFraming)
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

TEST(ChunkProxy, PreservesSetCookieAlreadyStampedOnClientResponse)
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

TEST(ChunkProxy, ClosesConnectionsForEmptyContentLengthZeroResponse)
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

TEST(ChunkProxy, DoesNotDuplicateWriteWhenFinalSignalArrivesBeforeHeaderWriteCompletes)
{
   // Regression test for a race: in Content-Length framing the empty final
   // completion signal enqueues no new bytes (nothing to terminate), so if it
   // arrives before the *header write's* completion handler (onHeadersWrote)
   // has run, writeBuffer_ still shows exactly the one body chunk queued by
   // the first call. Without an explicit "write in progress" guard, ChunkProxy
   // would treat that as "the only chunk in the buffer, kick off a write" and
   // call writeChunk() a second time on the same still-unpopped front() entry
   // once onHeadersWrote() also calls writeChunk() -- duplicating the write
   // and eventually running onChunkWrote() against an already-emptied queue.
   //
   // Unlike Fixture::deliver(), which polls the io_context to completion after
   // every chunk (serializing header-write completion before the next chunk
   // is delivered), this test invokes the chunk handler twice back-to-back
   // *before* draining anything, to reproduce the interleaving.
   Fixture fixture;
   std::string body = "hello";
   http::Response upstream;
   makeContentLengthResponse(&upstream, body);

   bool result1 = fixture.pServerConnection->chunkHandler_(upstream, body);
   bool result2 = fixture.pServerConnection->chunkHandler_(upstream, ""); // completion signal, before header write completes
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

TEST(ChunkProxy, AccountsBufferSizeAgainstFormattedNotRawBytesInChunkedMode)
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

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
