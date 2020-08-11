/*
 * AsyncConnection.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef CORE_HTTP_ASYNC_CONNECTION_HPP
#define CORE_HTTP_ASYNC_CONNECTION_HPP

#include <boost/any.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/asio/io_service.hpp>

#include <core/http/Response.hpp>
#include <core/http/Socket.hpp>

namespace rstudio {
namespace core {

class Error;

namespace http {

class Request;
class Response;

class AsyncConnection;

typedef boost::function<void(boost::shared_ptr<Response>)> RequestFilterContinuation;

typedef boost::function<void(boost::asio::io_service&,
                             Request*,
                             RequestFilterContinuation)> RequestFilter;

typedef boost::function<void(const Request& request, Response*)> ResponseFilter;

// abstract base (insulate clients from knowledge of protocol-specifics)
class AsyncConnection : public Socket
{
public:
   virtual ~AsyncConnection() {}

   // io service for initiating dependent async network operations
   virtual boost::asio::io_service& ioService() = 0;

   // request
   virtual const http::Request& request() const = 0;

   // populate or set response then call writeResponse when done
   virtual http::Response& response() = 0;
   virtual void writeResponse(bool close = true) = 0;

   // simple wrappers for writing an existing response or error
   virtual void writeResponse(const http::Response& response,
                              bool close = true,
                              const http::Headers& extraHeaders = http::Headers()) = 0;

   // writes only the headers and not any body data
   // useful for chunked encoding (streaming)
   // after successful write, the handler callback is invoked
   // allowing you to start writing to the raw socket for streaming purposes
   virtual void writeResponseHeaders(Socket::Handler handler) = 0;

   virtual void writeError(const Error& error) = 0;

   virtual void close() = 0;

   // resume parsing the connection data if previously paused
   virtual void continueParsing() = 0;

   // set and get arbitrary connection-related data
   virtual void setData(const boost::any& data) = 0;
   virtual boost::any getData() = 0;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_CONNECTION_HPP
