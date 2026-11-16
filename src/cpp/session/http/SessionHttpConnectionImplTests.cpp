/*
 * SessionHttpConnectionImplTests.cpp
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

#include "SessionHttpConnectionImpl.hpp"

#include <algorithm>
#include <string>

#include <boost/asio/ip/tcp.hpp>
#include <boost/asio/read.hpp>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/write.hpp>
#include <boost/make_shared.hpp>
#include <boost/weak_ptr.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace tests {

namespace {

using boost::asio::ip::tcp;
typedef HttpConnectionImpl<tcp> TcpConnection;

struct UploadOutcome
{
   int uploadCalls = 0;
   bool lastCallComplete = false;
   std::size_t lastCallSize = 0;
   bool dispatched = false;
   bool connectionFreed = false;
   boost::system::error_code writeError;
   std::string received;
};

// POST a multipart body of `bodySize` bytes to a session connection whose
// upload handler responds and stops after the first chunk (keepGoing false),
// or reads every chunk and responds after the last (keepGoing true). The
// client stops sending after `sentSize` bytes of the body.
UploadOutcome runUpload(std::size_t bodySize,
                        bool keepGoing,
                        std::size_t sentSize = std::string::npos)
{
   UploadOutcome outcome;
   boost::asio::io_context ioContext;

   tcp::acceptor acceptor(ioContext, tcp::endpoint(boost::asio::ip::address_v4::loopback(), 0));
   tcp::socket client(ioContext);
   client.connect(acceptor.local_endpoint());

   core::http::UriAsyncUploadHandlerFunction uploadHandler =
         [&](const core::http::Request&,
             const std::string& formData,
             bool complete,
             const core::http::UriHandlerFunctionContinuation& cont)
   {
      ++outcome.uploadCalls;
      outcome.lastCallComplete = complete;
      outcome.lastCallSize = formData.size();
      if (!keepGoing || complete)
      {
         core::http::Response response;
         response.setStatusCode(keepGoing ? core::http::status::Ok : core::http::status::BadRequest);
         cont(&response);
      }

      return keepGoing;
   };

   auto pConnection = boost::make_shared<TcpConnection>(
         ioContext,
         boost::shared_ptr<boost::asio::ssl::context>(),
         [&](boost::shared_ptr<TcpConnection> pParsed)
         {
            pParsed->setUploadHandler(uploadHandler);
         },
         [&](boost::shared_ptr<TcpConnection>)
         {
            outcome.dispatched = true;
         });

   acceptor.accept(pConnection->socket());
   pConnection->startReading();

   std::string request =
         "POST /upload HTTP/1.1\r\n"
         "Content-Type: multipart/form-data; boundary=b\r\n"
         "Content-Length: " + std::to_string(bodySize) + "\r\n"
         "\r\n" + std::string(std::min(bodySize, sentSize), 'x');
   boost::asio::async_write(
            client,
            boost::asio::buffer(request),
            [&](const boost::system::error_code& ec, std::size_t)
            {
               outcome.writeError = ec;
               if (sentSize < bodySize)
               {
                  boost::system::error_code shutdownError;
                  client.shutdown(tcp::socket::shutdown_send, shutdownError);
               }
            });

   // only pending operations keep the connection alive from here
   boost::weak_ptr<TcpConnection> pWeakConnection = pConnection;
   pConnection.reset();
   ioContext.run();

   outcome.connectionFreed = pWeakConnection.expired();

   boost::asio::streambuf received;
   boost::system::error_code ec;
   boost::asio::read(client, received, ec);
   outcome.received.assign(boost::asio::buffers_begin(received.data()),
                           boost::asio::buffers_end(received.data()));

   return outcome;
}

} // anonymous namespace

TEST(SessionHttpConnectionTest, UploadHandlerGivingUpOnTheLastChunkEndsTheRequest)
{
   UploadOutcome outcome = runUpload(100, false);
   EXPECT_EQ(1, outcome.uploadCalls);
   EXPECT_FALSE(outcome.dispatched);
   EXPECT_TRUE(outcome.connectionFreed);
   EXPECT_EQ(0u, outcome.received.find("HTTP/1.1 400"));
}

TEST(SessionHttpConnectionTest, UploadHandlerGivingUpOnAnEarlierChunkEndsTheRequest)
{
   UploadOutcome outcome = runUpload(3 * 1024 * 1024 / 2, false);
   EXPECT_EQ(1, outcome.uploadCalls);
   EXPECT_FALSE(outcome.dispatched);
   EXPECT_TRUE(outcome.connectionFreed);
}

TEST(SessionHttpConnectionTest, UploadHandlerGivingUpEarlyStillDeliversItsReply)
{
   // far more of the body than socket buffers hold is still unsent when the
   // handler answers; closing with it unread would reset the connection
   UploadOutcome outcome = runUpload(8 * 1024 * 1024, false);
   EXPECT_EQ(1, outcome.uploadCalls);
   EXPECT_TRUE(outcome.connectionFreed);
   EXPECT_FALSE(outcome.writeError) << outcome.writeError.message();
   EXPECT_EQ(0u, outcome.received.find("HTTP/1.1 400"));
}

TEST(SessionHttpConnectionTest, ClientLeavingMidUploadEndsTheUpload)
{
   UploadOutcome outcome = runUpload(3 * 1024 * 1024, true, 3 * 1024 * 1024 / 2);
   EXPECT_FALSE(outcome.dispatched);
   EXPECT_TRUE(outcome.connectionFreed);

   // the handler hears that no more is coming
   EXPECT_EQ(2, outcome.uploadCalls);
   EXPECT_TRUE(outcome.lastCallComplete);
   EXPECT_EQ(0u, outcome.lastCallSize);
}

TEST(SessionHttpConnectionTest, CompletedUploadFreesTheConnection)
{
   UploadOutcome outcome = runUpload(3 * 1024 * 1024 / 2, true);
   EXPECT_EQ(2, outcome.uploadCalls);
   EXPECT_FALSE(outcome.dispatched);
   EXPECT_TRUE(outcome.connectionFreed);
   EXPECT_EQ(0u, outcome.received.find("HTTP/1.1 200"));
}

} // namespace tests
} // namespace session
} // namespace rstudio
