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

#include <chrono>
#include <future>
#include <string>

#include <boost/asio/io_context.hpp>
#include <boost/asio/local/stream_protocol.hpp>
#include <boost/asio/read_until.hpp>
#include <boost/asio/steady_timer.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/write.hpp>
#include <boost/function.hpp>

#include <gtest/gtest.h>

#include <shared_core/FilePath.hpp>

#include <core/system/PosixUser.hpp>

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
        deadline_(ioc_),
        respond_(respond)
   {
   }

   ~LocalStreamServer()
   {
      // a test that is over has no more use for a server still waiting on
      // its client
      ioc_.stop();

      if (finished_.valid())
         finished_.wait();

      if (timedOut_)
         ADD_FAILURE() << "the server gave up waiting on its client";
   }

   void start()
   {
      acceptor_.async_accept(socket_, [this](const boost::system::error_code& ec) { onAccept(ec); });

      // a client that never connects, or that stalls partway through its
      // request, would otherwise wait on the server, and the test on both
      deadline_.expires_after(std::chrono::seconds(10));
      deadline_.async_wait([this](const boost::system::error_code& ec) { onDeadline(ec); });

      finished_ = std::async(std::launch::async, [this]() { ioc_.run(); });
   }

   // whether a client connected. Like requestBytes(), this waits for the
   // server to be done with its client, so call it only once the client is
   bool accepted()
   {
      waitUntilFinished();
      return accepted_;
   }

   // the number of request bytes the accepted connection delivered
   std::size_t requestBytes()
   {
      waitUntilFinished();
      return requestBytes_;
   }

private:
   void waitUntilFinished()
   {
      if (finished_.valid())
         finished_.wait();
   }

   void onAccept(const boost::system::error_code& ec)
   {
      if (ec)
         return;

      accepted_ = true;
      boost::asio::async_read_until(
         socket_,
         request_,
         "\r\n\r\n",
         [this](const boost::system::error_code& ec, std::size_t) { onRequest(ec); });
   }

   void onRequest(const boost::system::error_code& ec)
   {
      requestBytes_ = request_.size();

      if (ec || !respond_)
      {
         hangUp();
         return;
      }

      boost::asio::async_write(
         socket_,
         boost::asio::buffer(response_),
         [this](const boost::system::error_code&, std::size_t) { hangUp(); });
   }

   void onDeadline(const boost::system::error_code& ec)
   {
      // cancelled: the server finished with its client in time
      if (ec)
         return;

      timedOut_ = true;
      hangUp();
   }

   // ends whatever the server is waiting on, which lets its thread finish
   void hangUp()
   {
      boost::system::error_code ec;
      deadline_.cancel();
      acceptor_.close(ec);
      socket_.shutdown(stream_protocol::socket::shutdown_both, ec);
      socket_.close(ec);
   }

   boost::asio::io_context ioc_;
   stream_protocol::acceptor acceptor_;
   stream_protocol::socket socket_;
   boost::asio::steady_timer deadline_;
   boost::asio::streambuf request_;
   const std::string response_ =
      "HTTP/1.1 200 OK\r\n"
      "Content-Length: 2\r\n"
      "Connection: close\r\n"
      "\r\n"
      "ok";
   bool respond_;
   bool accepted_ = false;
   bool timedOut_ = false;
   std::size_t requestBytes_ = 0;
   std::future<void> finished_;
};

// a client whose peer's identity is whatever the test says it is, for the
// outcomes that would take a second user or a failing lookup to produce
class StubbedPeerClient : public LocalStreamAsyncClient
{
public:
   using LocalStreamAsyncClient::LocalStreamAsyncClient;

   boost::function<Error(core::system::user::UserIdentity*)> peerIdentity;

protected:
   Error connectedPeerIdentity(core::system::user::UserIdentity* pIdentity) override
   {
      return peerIdentity(pIdentity);
   }
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
   // the server runs in this process, so this user both owns the path and
   // is who the peer runs as
   LocalStreamServer server(streamPath_, true);
   server.start();

   boost::asio::io_context ioc;
   boost::shared_ptr<LocalStreamAsyncClient> pClient(new LocalStreamAsyncClient(ioc, streamPath_, false, ::geteuid()));
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

   // and it was refused once connected, before any of the request reached it
   EXPECT_TRUE(server.accepted());
   EXPECT_EQ(0u, server.requestBytes());
}

TEST_F(LocalStreamAsyncClientTest, RequestIsNotSentWhenPathOwnerMatchesButPeerDoesNot)
{
   // the two checks look at different things: the path can belong to the
   // expected user while the process connected to runs as another
   LocalStreamServer server(streamPath_, true);
   server.start();

   UidType uid = ::geteuid();

   boost::asio::io_context ioc;
   boost::shared_ptr<StubbedPeerClient> pClient(new StubbedPeerClient(ioc, streamPath_, false, uid));
   pClient->setExpectedPeerUid(uid);
   pClient->peerIdentity = [&](core::system::user::UserIdentity* pIdentity) -> Error {
      pIdentity->userId = uid + 1;
      pIdentity->groupId = ::getegid();
      return Success();
   };
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
   EXPECT_TRUE(server.accepted());
   EXPECT_EQ(0u, server.requestBytes());
}

TEST_F(LocalStreamAsyncClientTest, RequestIsNotSentWhenPeerCannotBeIdentified)
{
   // not knowing which user the peer runs as is no reason to trust it, nor
   // to wait for it, even on a path that belongs to the expected user
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
   boost::shared_ptr<StubbedPeerClient> pClient(new StubbedPeerClient(ioc, streamPath_, false, ::geteuid()));
   pClient->setExpectedPeerUid(::geteuid());
   pClient->setConnectionRetryProfile(retryProfile);
   pClient->peerIdentity = [](core::system::user::UserIdentity*) -> Error {
      return systemError(boost::system::errc::not_connected, ERROR_LOCATION);
   };
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
   EXPECT_EQ(0, recoveryAttempts);
   EXPECT_TRUE(server.accepted());
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
   EXPECT_TRUE(server.accepted());
   EXPECT_EQ(0u, server.requestBytes());
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio

#endif // _WIN32
