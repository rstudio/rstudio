/*
 * SessionSuspendFilterTests.cpp
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

#include <session/SessionSuspendFilter.hpp>

#include <gtest/gtest.h>

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

TEST(SuspendFilterTest, DistributedEventConnectionsDoNotResetTimeout) {
   SessionSuspendFilters filters;
   boost::shared_ptr<HttpConnection> distEvent = boost::make_shared<TestConnection>("/distributed_events");
   EXPECT_FALSE(filters.shouldResetSuspendTimer(distEvent));
}

TEST(SuspendFilterTest, NonDistributedEventConnectionsResetTimeout) {
   SessionSuspendFilters filters;
   boost::shared_ptr<HttpConnection> notYourEvent = boost::make_shared<TestConnection>("/not_your_event");
   EXPECT_TRUE(filters.shouldResetSuspendTimer(notYourEvent));
}

TEST(SuspendFilterTest, CurrentlyEditingConnectionsDoNotResetTimeout) {
   SessionSuspendFilters filters;
   boost::shared_ptr<HttpConnection> currentlyEditing = boost::make_shared<TestConnection>("/rpc/set_currently_editing");
   EXPECT_FALSE(filters.shouldResetSuspendTimer(currentlyEditing));
}

TEST(SuspendFilterTest, NonCurrentlyEditingConnectionsResetTimeout) {
   SessionSuspendFilters filters;
   boost::shared_ptr<HttpConnection> notCurrentlyEditing = boost::make_shared<TestConnection>("/not_your_event");
   EXPECT_TRUE(filters.shouldResetSuspendTimer(notCurrentlyEditing));
}

} // namespace tests
} // namespace suspend
} // namespace session
} // namespace rstudio
