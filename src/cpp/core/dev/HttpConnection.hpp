/*
 * HttpConnection.hpp
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

#ifndef HTTP_CONNECTION_HPP
#define HTTP_CONNECTION_HPP

namespace core {
   namespace http {
      class Request;
      class Response;
   }
}

// abstract base (insulate clients from knowledge of protocol-specifics)
class HttpConnection
{
public:
   virtual ~HttpConnection() {}

   virtual const core::http::Request& request() = 0;
   virtual void sendResponse(const core::http::Response& response) = 0;

   // close (occurs automatically after writeResponse, here in case it
   // need to be closed in other circumstances
   virtual void close() = 0;
};


#endif // HTTP_CONNECTION_HPP

