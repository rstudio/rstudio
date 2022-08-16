/*
 * SessionHttpConnection.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_HTTP_CONNECTION_HPP
#define SESSION_HTTP_CONNECTION_HPP

#include <string>

#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>

#include <core/http/UriHandler.hpp>

/*
 HttpConnection plays two related roles in the system:

   1) It is a sink for asynchronous reading of http requests by
      HttpConnectionListener (this interface is private to HttpConnectionListener
      via friendship). This role occurs on a background thread.

   2) It provides an interface for HttpConnection::Handler to respond to
      requests. Responses are sent synchronously and are threfore disconnected
      from the HttpConnectionListener io_service and (most importantly) can
      therefore be sent on background threads.
*/

namespace rstudio {
namespace core {

   class Error;

   namespace http {
      class Request;
      class Response;
   }

   namespace json {
      class JsonRpcResponse;
   }
}
}

namespace rstudio {
namespace session {

// abstract base (insulate clients from knowledge of protocol-specifics)
class HttpConnection
{
public:
   virtual ~HttpConnection() {}

   virtual const core::http::Request& request() = 0;
   virtual void sendResponse(const core::http::Response& response) = 0;

   void sendJsonRpcError(const core::Error& error);
   void sendJsonRpcResponse();
   virtual void sendJsonRpcResponse(
                  core::json::JsonRpcResponse& jsonRpcResponse) = 0;

   // close (occurs automatically after writeResponse, here in case it
   // need to be closed in other circumstances
   virtual void close() = 0;

   // other useful introspection methods
   virtual std::string requestId() const = 0;

   virtual void setUploadHandler(const core::http::UriAsyncUploadHandlerFunction& uploadHandler) = 0;

   virtual bool isAsyncRpc() const = 0;

   virtual std::chrono::steady_clock::time_point receivedTime() const = 0;
};


} // namespace session
} // namespace rstudio

#endif // SESSION_HTTP_CONNECTION_HPP

