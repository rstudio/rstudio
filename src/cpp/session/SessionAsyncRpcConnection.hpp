/*
 * SessionAsyncRpcConnection.hpp
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

#ifndef SESSION_ASYNC_RPC_CONNECTION_HPP
#define SESSION_ASYNC_RPC_CONNECTION_HPP

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <session/SessionHttpConnection.hpp>

#include "SessionHttpMethods.hpp"
#include "SessionRpc.hpp"

namespace rstudio {
namespace session {
namespace rpc {

/**
 * Created from an HttpConnectionImpl, then put into mainConnectionQueue for RPC requests handed asynchronously
 * by the rsession, where it first acks the http request, then later calls the rpc handler, then sends a kAsyncCompletion
 * event in the response.
 */
class AsyncRpcConnection :
        public HttpConnection,
        public boost::enable_shared_from_this<AsyncRpcConnection>,
        boost::noncopyable
{
public:
   AsyncRpcConnection(const boost::shared_ptr<HttpConnection>& ptrConnection,
                      const core::json::JsonRpcRequest& jsonRequest,
                      const std::string& asyncHandle)
      : ptrConnection_(ptrConnection), jsonRequest_(jsonRequest), asyncHandle_(asyncHandle)
   {
   }

   virtual ~AsyncRpcConnection() {}

   virtual const core::http::Request& request() { return ptrConnection_->request(); }

   // Not valid for async rpc since we already replied to http
   virtual void sendResponse(const core::http::Response& response)
   {
      BOOST_ASSERT(false);
   }

   // emit the kAsyncCompletion event with the response
   virtual void sendJsonRpcResponse(core::json::JsonRpcResponse& jsonRpcResponse)
   {
      rpc::endHandleRpcRequestIndirect(asyncHandle(), core::Success(), &jsonRpcResponse);
   }

   virtual void close()
   {
      // Not closing ptrConnection_ since it will already be closed
   }

   virtual std::string requestId() const
   {
      return ptrConnection_->requestId();
   }

   // This method only valid for uri handlers, not rpc requests
   virtual void setUploadHandler(const core::http::UriAsyncUploadHandlerFunction& uploadHandler)
   {
      BOOST_ASSERT(false);
   }

   // Differentiate from HttpConnectionImpl
   virtual bool isAsyncRpc() const
   {
      return true;
   }

   virtual std::chrono::steady_clock::time_point receivedTime() const
   {
      return ptrConnection_->receivedTime();
   }

   core::json::JsonRpcRequest jsonRpcRequest() const
   {
      return jsonRequest_;
   }

   // The uuid for the rpc ack and event response
   std::string asyncHandle() const
   {
      return asyncHandle_;
   }

private:
   // The (now closed) http connection including Request object
   boost::shared_ptr<HttpConnection> ptrConnection_;
   core::json::JsonRpcRequest jsonRequest_;
   std::string asyncHandle_;

};

} // namespace rpc
} // namespace session
} // namespace rstudio

#endif
