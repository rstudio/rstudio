/*
 * LocalStreamAsyncClientTests.cpp
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

#ifndef _WIN32

#include <core/http/LocalStreamAsyncClient.hpp>

#include <unistd.h>

#include <string>
#include <thread>

#include <boost/asio/io_context.hpp>
#include <boost/asio/local/stream_protocol.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/write.hpp>

#include <gtest/gtest.h>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

using boost::asio::local::stream_protocol;

// a minimal server on its own thread: accepts one connection over a local
// stream, reads the request headers, and either answers with a complete
// response or hangs up without one
class LocalStreamServer
{
public:
   LocalStreamServer(const FilePath& streamPath, bool respond)
      : acceptor_(ioc_, stream_protocol::endpoint(streamPath.getAbsolutePath())),
        socket_(ioc_),
        respond_(respond)
   {
   }

   ~LocalStreamServer()
   {
      // a client that never connected leaves the accept pending; give up on
      // it, so that the test fails on its assertions rather than hanging here
      ioc_.stop();

      if (thread_.joinable())
         thread_.join();
   }

   void start()
   {
      acceptor_.async_accept(socket_, [this](const boost::system::error_code& ec) { serve(ec); });
      thread_ = std::thread([this]() { ioc_.run(); });
   }

   // the number of request bytes the accepted connection delivered. Waits for
   // that connection to be served, so call it only once the client is done
   std::size_t requestBytes()
   {
      if (thread_.joinable())
         thread_.join();

      return requestBytes_;
   }

private:
   void serve(const boost::system::error_code& acceptError)
   {
      if (acceptError)
         return;

      boost::system::error_code ec;

      boost::asio::streambuf buf;
      boost::asio::read_until(socket_, buf, "\r\n\r\n", ec);
      requestBytes_ = buf.size();

      if (respond_)
      {
         std::string response =
            "HTTP/1.1 200 OK\r\n"
            "Content-Length: 2\r\n"
            "Connection: close\r\n"
            "\r\n"
            "ok";
         boost::asio::write(socket_, boost::asio::buffer(response), ec);
      }

      socket_.shutdown(stream_protocol::socket::shutdown_both, ec);
      socket_.close(ec);
   }

   boost::asio::io_context ioc_;
   stream_protocol::acceptor acceptor_;
   stream_protocol::socket socket_;
   bool respond_;
   std::size_t requestBytes_ = 0;
   std::thread thread_;
};

class LocalStreamAsyncClientTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(streamPath_));
      ASSERT_FALSE(streamPath_.removeIfExists());
   }

   void TearDown() override
   {
      streamPath_.removeIfExists();
   }

   FilePath streamPath_;
};

} // anonymous namespace

TEST_F(LocalStreamAsyncClientTest, PeerPidIsRecordedOnConnect)
{
   // the server runs in this process, so the client's peer is this process;
   // rserver relies on the pid to know which session process answered
   LocalStreamServer server(streamPath_, true);
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<LocalStreamAsyncClient> pClient(new LocalStreamAsyncClient(ioc, streamPath_));
   EXPECT_EQ(-1, pClient->peerPid());

   pClient->request().setMethod("GET");
   pClient->request().setUri("/");

   bool responded = false;
   Error responseError;
   pClient->execute(
      [&](const Response& response) {
         responded = true;
         EXPECT_EQ(200, response.statusCode());
      },
      [&](const Error& error) {
         responseError = error;
      });
   ioc.run();

   EXPECT_TRUE(responded);
   EXPECT_FALSE(responseError) << responseError.asString();
   EXPECT_EQ(::getpid(), pClient->peerPid());
}

TEST_F(LocalStreamAsyncClientTest, ErrorCarriesPeerPidOfProcessReached)
{
   // a hang-up after connecting is an outcome produced by the process that
   // was reached, and the error says which one; errors reach their handlers
   // without the client, so this is how rserver attributes them
   LocalStreamServer server(streamPath_, false);
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<LocalStreamAsyncClient> pClient(new LocalStreamAsyncClient(ioc, streamPath_));
   pClient->request().setMethod("GET");
   pClient->request().setUri("/");

   bool responded = false;
   Error responseError;
   pClient->execute(
      [&](const Response&) {
         responded = true;
      },
      [&](const Error& error) {
         responseError = error;
      });
   ioc.run();

   EXPECT_FALSE(responded);
   ASSERT_TRUE(responseError);
   EXPECT_EQ(std::to_string(::getpid()), responseError.getProperty(kLocalStreamPeerPidProperty));
}

TEST_F(LocalStreamAsyncClientTest, ConnectionFailureCarriesNoPeerPid)
{
   // nothing listens on the path: no process was reached, so the error
   // must not claim one
   boost::asio::io_context ioc;
   boost::shared_ptr<LocalStreamAsyncClient> pClient(new LocalStreamAsyncClient(ioc, streamPath_));
   pClient->request().setMethod("GET");
   pClient->request().setUri("/");

   Error responseError;
   pClient->execute(
      [&](const Response&) {
      },
      [&](const Error& error) {
         responseError = error;
      });
   ioc.run();

   ASSERT_TRUE(responseError);
   EXPECT_EQ("", responseError.getProperty(kLocalStreamPeerPidProperty));
   EXPECT_EQ(-1, pClient->peerPid());
}

TEST_F(LocalStreamAsyncClientTest, RequestIsSentToPeerRunningAsExpectedUser)
{
   // the server runs in this process, so the peer runs as this user
   LocalStreamServer server(streamPath_, true);
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<LocalStreamAsyncClient> pClient(new LocalStreamAsyncClient(ioc, streamPath_));
   pClient->setExpectedPeerUid(::geteuid());
   pClient->request().setMethod("GET");
   pClient->request().setUri("/");

   bool responded = false;
   Error responseError;
   pClient->execute(
      [&](const Response& response) {
         responded = true;
         EXPECT_EQ(200, response.statusCode());
      },
      [&](const Error& error) {
         responseError = error;
      });
   ioc.run();

   EXPECT_TRUE(responded);
   EXPECT_FALSE(responseError) << responseError.asString();
   EXPECT_GT(server.requestBytes(), 0u);
}

TEST_F(LocalStreamAsyncClientTest, RequestIsNotSentToPeerRunningAsAnotherUser)
{
   LocalStreamServer server(streamPath_, true);
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<LocalStreamAsyncClient> pClient(new LocalStreamAsyncClient(ioc, streamPath_));
   pClient->setExpectedPeerUid(::geteuid() + 1);
   pClient->request().setMethod("GET");
   pClient->request().setUri("/");

   bool responded = false;
   Error responseError;
   pClient->execute(
      [&](const Response&) {
         responded = true;
      },
      [&](const Error& error) {
         responseError = error;
      });
   ioc.run();

   EXPECT_FALSE(responded);
   ASSERT_TRUE(responseError);
   EXPECT_EQ(boost::system::errc::permission_denied, responseError.getCode());

   // the peer was refused, so the error is not an outcome it produced
   EXPECT_EQ("", responseError.getProperty(kLocalStreamPeerPidProperty));
   EXPECT_EQ(-1, pClient->peerPid());

   // and it was refused before any of the request reached it
   EXPECT_EQ(0u, server.requestBytes());
}

TEST_F(LocalStreamAsyncClientTest, PeerRunningAsAnotherUserIsNotRetried)
{
   // a retry profile is how rserver waits for a session to start; a peer
   // that is running as the wrong user is not one to wait for
   LocalStreamServer server(streamPath_, true);
   server.start();

   int recoveryAttempts = 0;
   ConnectionRetryProfile retryProfile(
      boost::posix_time::seconds(10),
      boost::posix_time::milliseconds(10),
      [&](const Request&, bool) {
         recoveryAttempts++;
         return Success();
      });

   boost::asio::io_context ioc;
   boost::shared_ptr<LocalStreamAsyncClient> pClient(new LocalStreamAsyncClient(ioc, streamPath_));
   pClient->setExpectedPeerUid(::geteuid() + 1);
   pClient->setConnectionRetryProfile(retryProfile);
   pClient->request().setMethod("GET");
   pClient->request().setUri("/");

   Error responseError;
   pClient->execute(
      [&](const Response&) {
      },
      [&](const Error& error) {
         responseError = error;
      });
   ioc.run();

   ASSERT_TRUE(responseError);
   EXPECT_EQ(boost::system::errc::permission_denied, responseError.getCode());
   EXPECT_EQ(0, recoveryAttempts);
   EXPECT_EQ(0u, server.requestBytes());
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio

#endif // _WIN32
