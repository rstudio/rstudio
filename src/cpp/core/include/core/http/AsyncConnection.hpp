/*
 * AsyncConnection.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <boost/shared_ptr.hpp>
#include <boost/asio/io_service.hpp>

#include <core/http/Socket.hpp>

namespace rstudio {
namespace core {

class Error;

namespace http {

class Request;
class Response;

class AsyncConnection;

typedef boost::function<void(
            http::Request*,
            boost::function<void()> continuation)> RequestFilter;

typedef boost::function<void(const std::string&,
                             http::Response*)> ResponseFilter;

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
                              bool close = true) = 0;
   virtual void writeError(const Error& error) = 0;
};

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_ASYNC_CONNECTION_HPP
