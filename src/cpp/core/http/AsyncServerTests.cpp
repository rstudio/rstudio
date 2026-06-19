/*
 * AsyncServerTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

// Smoke coverage for AsyncServerImpl's accept loop. The resource-exhaustion
// backoff (handleAccept -> scheduleAcceptRetry) can't be triggered
// deterministically without exhausting file descriptors, so this instead
// exercises the normal accept/serve path end-to-end -- which also forces the
// server template (including the backoff branch) to compile and confirms the
// refactor did not break ordinary request handling.

#include <chrono>
#include <string>

#include <boost/asio/io_context.hpp>
#include <boost/asio/system_timer.hpp>
#include <boost/make_shared.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/http/TcpIpAsyncClient.hpp>
#include <core/http/TcpIpAsyncServer.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

void handlePing(const http::Request& request, http::Response* pResponse)
{
   pResponse->setStatusCode(http::status::Ok);
   pResponse->setContentType("text/plain");
   pResponse->setBody("pong");
}

} // anonymous namespace

TEST(AsyncServerSmoke, ServesABasicRequest)
{
   TcpIpAsyncServer server("test-server");

   Error error = server.init("127.0.0.1", "0"); // 0 => ephemeral port
   ASSERT_FALSE(error) << error.getSummary();

   server.addBlockingHandler("/", handlePing);

   error = server.run(1);
   ASSERT_FALSE(error) << error.getSummary();

   unsigned short port = server.localEndpoint().port();

   boost::asio::io_context ioc;
   boost::shared_ptr<TcpIpAsyncClient> pClient =
      boost::make_shared<TcpIpAsyncClient>(
         ioc, "127.0.0.1", std::to_string(port),
         boost::posix_time::seconds(5));

   http::Request& request = pClient->request();
   request.setMethod("GET");
   request.setUri("/");

   bool gotResponse = false;
   bool gotError = false;
   int statusCode = 0;
   std::string body;

   // backstop so a failure can't hang the suite
   boost::asio::system_timer deadline(ioc, std::chrono::seconds(5));
   deadline.async_wait([&](const boost::system::error_code& ec) {
      if (ec == boost::asio::error::operation_aborted)
         return;
      pClient->close();
   });

   pClient->execute(
      [&](const http::Response& response) {
         gotResponse = true;
         statusCode = response.statusCode();
         body = response.body();
         deadline.cancel();
      },
      [&](const core::Error&) {
         gotError = true;
         deadline.cancel();
      });

   ioc.run();

   server.stop();
   server.waitUntilStopped();

   EXPECT_TRUE(gotResponse);
   EXPECT_FALSE(gotError);
   EXPECT_EQ(statusCode, 200);
   EXPECT_EQ(body, "pong");
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
