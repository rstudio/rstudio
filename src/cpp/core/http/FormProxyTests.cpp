/*
 * FormProxyTests.cpp
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

// Coverage for how FormProxy reacts to the downstream (session) connection
// having already settled by the time it registers its connect handler.
//
// ServerSessionProxy::proxyRequest() posts its ClientHandler -- which is what
// calls FormProxy::initialize() -- to the io_context after execute(), so on
// rserver's multi-threaded io_context the client can error, complete, or be
// torn down by FixedBufferProxy before initialize() runs. FormProxy gates all
// of its writes on the connect notification, so it must be told when that
// notification can no longer arrive.

#include <string>

#include <boost/asio/io_context.hpp>
#include <boost/asio/post.hpp>
#include <boost/make_shared.hpp>

#include <core/http/AsyncClient.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/http/FormProxy.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

// The upstream (browser) connection. Only the three things FormProxy can do to
// it matter here: close it, resume its parsing, and carry the proxy in its
// data slot.
class FakeUpstreamConnection : public AsyncConnection
{
public:
   boost::asio::io_context& ioContext() override { return ioc_; }
   const http::Request& request() const override { return request_; }
   http::Response& response() override { return response_; }

   void writeResponse(bool, Socket::Handler) override {}
   void writeResponse(const http::Response&, bool, const http::Headers&, Socket::Handler) override {}
   void writeResponseHeaders(Socket::Handler) override {}
   void writeError(const Error&) override {}

   void close() override { closeCount_++; }
   void continueParsing() override { continueParsingCount_++; }

   void setData(const boost::any& data) override { data_ = data; }
   boost::any getData() override { return data_; }
   const std::string& username() const override { return username_; }
   void setUsername(const std::string& username) override { username_ = username; }
   const std::string& handlerPrefix() const override { return handlerPrefix_; }
   void setHandlerPrefix(const std::string& prefix) override { handlerPrefix_ = prefix; }
   boost::asio::io_context::strand& getStrand() override { return *pStrand_; }

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}
   void asyncWrite(const boost::asio::const_buffer&, Socket::Handler) override {}
   void asyncWrite(const std::vector<boost::asio::const_buffer>&, Socket::Handler) override {}

   int closeCount_ = 0;
   int continueParsingCount_ = 0;

private:
   boost::asio::io_context ioc_;
   http::Request request_;
   http::Response response_;
   boost::any data_;
   std::string username_;
   std::string handlerPrefix_;
   boost::asio::io_context::strand* pStrand_ = nullptr;
};

// The downstream (session) client. setConnectHandler() decides, per the
// `settled` flag, which of the two notifications the real
// AsyncClient::setConnectHandler() would deliver: the connect handler when the
// client is still live, the downstream-closed handler once handlersDisabled_
// is set. Both are invoked inline, exactly as the real one does (outside its
// lock, on the caller's stack).
class FakeDownstreamClient : public IAsyncClient
{
public:
   explicit FakeDownstreamClient(bool settled) : settled_(settled) {}

   http::Request& request() override { return request_; }
   void setConnectionRetryProfile(const http::ConnectionRetryProfile&) override {}
   void setRequestTimeout(const boost::posix_time::time_duration&) override {}
   void execute(const ResponseHandler&, const ErrorHandler&, const FixedBufferHandler&) override {}
   void setFixedBufferHandler(const FixedBufferHandler&) override {}
   void setStreamNonChunkedResponses(bool) override {}
   void setBufferPredicate(const boost::function<bool(const http::Response&)>&) override {}
   void setFixedBufferHandlerSupportsPause(bool) override {}
   void resumeChunkProcessing() override {}
   void disableHandlers() override {}
   void close() override { closeCount_++; }
   void setStrand(boost::asio::io_context::strand*) override {}

   void setConnectHandler(const ConnectHandler& connectHandler,
                          const ConnectHandler& downstreamClosedHandler) override
   {
      if (settled_)
         downstreamClosedHandler();
      else
         connectHandler();
   }

   // Socket
   void asyncReadSome(boost::asio::mutable_buffer, Socket::Handler) override {}

   // NOTE: like real network I/O, the completion handler must not run
   // re-entrantly on the same call stack as the FormProxy method that started
   // the write -- FormProxy calls writeData() with its (non-recursive) mutex_
   // held, and the completion handler onDataWrote() re-acquires it. So post
   // instead of invoking inline; drain() runs the queue once the outer
   // queueData() call and its lock have returned.
   void asyncWrite(const boost::asio::const_buffer& buffer, Socket::Handler handler) override
   {
      written_.append(static_cast<const char*>(buffer.data()), buffer.size());
      std::size_t n = buffer.size();
      boost::asio::post(ioc_, [handler, n]() { handler(boost::system::error_code(), n); });
   }

   void asyncWrite(const std::vector<boost::asio::const_buffer>& buffers,
                   Socket::Handler handler) override
   {
      std::size_t total = 0;
      for (const auto& buffer : buffers)
      {
         written_.append(static_cast<const char*>(buffer.data()), buffer.size());
         total += buffer.size();
      }
      boost::asio::post(ioc_, [handler, total]() { handler(boost::system::error_code(), total); });
   }

   // run every posted write completion (and anything they chain into) to
   // quiescence, the way they would eventually fire in production
   void drain()
   {
      ioc_.restart();
      ioc_.poll();
   }

   int closeCount_ = 0;
   std::string written_;

private:
   bool settled_;
   http::Request request_;
   boost::asio::io_context ioc_;
};

} // anonymous namespace

// Baseline: a live client reports connect, and form data is written straight
// through to it.
TEST(FormProxy, LiveDownstreamWritesFormData)
{
   auto pUpstream = boost::make_shared<FakeUpstreamConnection>();
   auto pDownstream = boost::make_shared<FakeDownstreamClient>(/*settled=*/false);
   auto pProxy = boost::make_shared<FormProxy>(pUpstream, pDownstream);

   pProxy->initialize();

   EXPECT_TRUE(pProxy->queueData("--boundary\r\n"));
   pDownstream->drain();
   EXPECT_TRUE(pProxy->queueData("file bytes"));
   pDownstream->drain();

   EXPECT_EQ(pDownstream->written_, "--boundary\r\nfile bytes");
   EXPECT_EQ(pUpstream->closeCount_, 0);
}

// The race this change exists for. A settled downstream reports closed instead
// of dropping the notification, and every subsequent piece of form data is
// accepted (discarded) rather than buffered: returning false here is what
// pauses the upstream connection's parsing, and with no writer that pause is
// never lifted -- the upload hangs until the client times out.
TEST(FormProxy, SettledDownstreamKeepsAcceptingFormDataInsteadOfStalling)
{
   auto pUpstream = boost::make_shared<FakeUpstreamConnection>();
   auto pDownstream = boost::make_shared<FakeDownstreamClient>(/*settled=*/true);

   // a deliberately tiny buffer: were the data still being buffered, this much
   // form data would trip the buffer-full pause several times over
   auto pProxy = boost::make_shared<FormProxy>(pUpstream, pDownstream, /*maxBufferSize=*/16);

   pProxy->initialize();

   for (int i = 0; i < 100; i++)
   {
      EXPECT_TRUE(pProxy->queueData(std::string(64, 'x')));
      pDownstream->drain();
   }

   // nothing was written anywhere -- there was nowhere to write it
   EXPECT_EQ(pDownstream->written_, "");
}

// Tearing down the upstream connection is NOT FormProxy's to do here. Whoever
// settled the downstream client already owns finishing it: the error path has
// run the client's ErrorHandler, and the ordinary path has handed a complete
// response (a 401/403/413/redirect the session sent without reading the whole
// body) to FixedBufferProxy, which closes both connections once it has written
// it. Closing from here would race that write and turn a clean status code
// into a connection reset.
TEST(FormProxy, SettledDownstreamDoesNotCloseTheUpstreamConnection)
{
   auto pUpstream = boost::make_shared<FakeUpstreamConnection>();
   auto pDownstream = boost::make_shared<FakeDownstreamClient>(/*settled=*/true);
   auto pProxy = boost::make_shared<FormProxy>(pUpstream, pDownstream);

   pProxy->initialize();
   pProxy->queueData("file bytes");
   pDownstream->drain();

   EXPECT_EQ(pUpstream->closeCount_, 0);
   EXPECT_EQ(pDownstream->closeCount_, 0);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
