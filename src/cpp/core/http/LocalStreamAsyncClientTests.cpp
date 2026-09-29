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

// a minimal blocking server on its own thread: accepts one connection over
// a local stream, reads the request headers, and either answers with a
// complete response or hangs up without one
class LocalStreamServer
{
public:
   LocalStreamServer(const FilePath& streamPath, bool respond)
      : acceptor_(ioc_, stream_protocol::endpoint(streamPath.getAbsolutePath())),
        respond_(respond)
   {
   }

   ~LocalStreamServer()
   {
      if (thread_.joinable())
         thread_.join();
   }

   void start()
   {
      thread_ = std::thread([this]() { run(); });
   }

private:
   void run()
   {
      boost::system::error_code ec;

      stream_protocol::socket socket(ioc_);
      acceptor_.accept(socket, ec);
      if (ec)
         return;

      boost::asio::streambuf buf;
      boost::asio::read_until(socket, buf, "\r\n\r\n", ec);

      if (respond_)
      {
         std::string response =
            "HTTP/1.1 200 OK\r\n"
            "Content-Length: 2\r\n"
            "Connection: close\r\n"
            "\r\n"
            "ok";
         boost::asio::write(socket, boost::asio::buffer(response), ec);
      }

      socket.shutdown(stream_protocol::socket::shutdown_both, ec);
      socket.close(ec);
   }

   boost::asio::io_context ioc_;
   stream_protocol::acceptor acceptor_;
   bool respond_;
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

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio

#endif // _WIN32
