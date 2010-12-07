/*
 * UriHandler.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_HTTP_URI_HANDLER_HPP
#define CORE_HTTP_URI_HANDLER_HPP

#include <string>
#include <vector>

#include <boost/function.hpp>

#include <core/http/Response.hpp>

namespace core {
namespace http {

class Request;

// UriHandlerFunction concept
typedef boost::function<void(const Request&,Response*)> UriHandlerFunction ;
   
// UriFilterFunction concept - return true if the filter handled the request
typedef boost::function<bool(const http::Request&, http::Response*)> 
                                                         UriFilterFunction; 
   
class UriHandler
{
public:
   UriHandler(const std::string& prefix, const UriHandlerFunction& function);
   
   // COPYING: via compiler
   
   bool matches(const std::string& uri) const;
   
   UriHandlerFunction function() const;
  
   // implement UriHandlerFunction concept
   void operator()(const Request& request, Response* pResponse) const;
   
private:
   std::string prefix_;
   UriHandlerFunction function_ ;
};

class UriHandlers
{
public:
   UriHandlers() {}
   
   // COPYING: via compiler
   
   void add(const UriHandler& handler);
   
   UriHandlerFunction handlerFor(const std::string& uri) const;
   
private:
   std::vector<UriHandler> uriHandlers_;
};

inline void notFoundHandler(const Request& request, Response* pResponse)
{
   pResponse->setStatusCode(http::status::NotFound);
   pResponse->setContentType("text/plain");
   pResponse->setBody(request.uri() + " not found");
}

   
} // namespace http
} // namespace core

#endif // CORE_HTTP_URI_HANDLER_HPP


