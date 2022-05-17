/*
 * SessionSuspendFilterTests.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <session/SessionSuspendFilter.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace session {
namespace suspend {
namespace tests {

/**
 * Dummy test implementation for testing HttpConnections with specific URIs
 */ 
class TestConnection : public HttpConnection
{
public:
   TestConnection(std::string uri)
      : m_request()
   {
      m_request.setUri(uri);
   }
   virtual const core::http::Request& request() override { return m_request; }

   virtual void sendResponse(const core::http::Response& response) override {}
   virtual void sendJsonRpcResponse(
         core::json::JsonRpcResponse& jsonRpcResponse) override {}
   virtual void close() override {}
   virtual std::string requestId() const override {return std::string();}
   virtual void setUploadHandler(const core::http::UriAsyncUploadHandlerFunction& uploadHandler) override {}
   virtual bool isAsyncRpc() const override {return false;}
   virtual std::chrono::steady_clock::time_point receivedTime() const override {return std::chrono::steady_clock::now();}

   core::http::Request m_request;
};

test_context("Session Suspend Filters")
{
   SessionSuspendFilters filters = SessionSuspendFilters();
   boost::shared_ptr<HttpConnection> notYourEvent = boost::make_shared<TestConnection>("This is not the event you are looking for");

   boost::shared_ptr<HttpConnection> distEvent = boost::make_shared<TestConnection>("/distributed_events");
   test_that("Connections with /distributed_event URI should not reset a session's suspend timeout")
   {
      expect_false(filters.shouldResetSuspendTimer(distEvent));
   }
   test_that("Connections without /distributed_event URI can reset a session's suspend timeout")
   {
      expect_true(filters.shouldResetSuspendTimer(notYourEvent));
   }

   boost::shared_ptr<HttpConnection> currentlyEditing = boost::make_shared<TestConnection>("/rpc/set_currently_editing");
   test_that("Connections with /rpc/set_currently_editing URI should not reset a session's suspend timeout")
   {
      expect_false(filters.shouldResetSuspendTimer(currentlyEditing));
   }
   test_that("Connections without /rpc/set_currently_editing URI can reset a session's suspend timeout")
   {
      expect_true(filters.shouldResetSuspendTimer(notYourEvent));
   }
}

} // namespace tests
} // namespace suspend
} // namespace session
} // namespace rstudio

