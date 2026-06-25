/*
 * AsyncServerTests.cpp
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

// Coverage for AsyncServerImpl's accept loop.
//
// The resource-exhaustion backoff (handleAccept -> scheduleAcceptRetry) can't
// be triggered deterministically without exhausting file descriptors, so it is
// covered in two narrower ways: a direct unit test of the classification
// predicate (isResourceExhaustionError) that selects the backoff path, and a
// normal accept/serve round-trip that forces the server template -- including
// the backoff branch -- to compile and confirms ordinary request handling was
// not broken by the refactor.

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

// The predicate that decides whether an accept error triggers the backoff path:
// only EMFILE/ENOMEM in the system category, and nothing else.
TEST(AsyncServerResourceExhaustion, ClassifiesResourceErrors)
{
   using boost::system::error_code;
   using boost::system::system_category;
   using boost::system::generic_category;
   namespace errc = boost::system::errc;

   // the two resource-exhaustion errors we back off on
   EXPECT_TRUE(isResourceExhaustionError(
      error_code(errc::too_many_files_open, system_category())));
   EXPECT_TRUE(isResourceExhaustionError(
      error_code(errc::not_enough_memory, system_category())));

   // unrelated system errors are not resource exhaustion
   EXPECT_FALSE(isResourceExhaustionError(
      error_code(errc::connection_reset, system_category())));
   EXPECT_FALSE(isResourceExhaustionError(
      error_code(errc::operation_canceled, system_category())));

   // a success code (no error) is not resource exhaustion
   EXPECT_FALSE(isResourceExhaustionError(error_code()));

   // the right value but the wrong category must not match
   EXPECT_FALSE(isResourceExhaustionError(
      error_code(errc::too_many_files_open, generic_category())));
}

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
