/*
 * UriHandler.hpp
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

#ifndef CORE_HTTP_URI_HANDLER_HPP
#define CORE_HTTP_URI_HANDLER_HPP

#include <string>
#include <vector>

#include <boost/function.hpp>
#include <boost/variant.hpp>

#include <core/http/AsyncConnection.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace core {
namespace http {

class Request;

typedef boost::function<void(Response*)> UriHandlerFunctionContinuation;

// UriHandlerFunction concept
typedef boost::function<void(const Request&,const UriHandlerFunctionContinuation&)>
                                                UriAsyncHandlerFunction;

// UriHandlerFunction concept
typedef boost::function<void(const Request&,Response*)> UriHandlerFunction;

// UriFilterFunction concept - return true if the filter handled the request
typedef boost::function<bool(const http::Request&, http::Response*)> 
                                                         UriFilterFunction;

typedef boost::function<bool(const Request&,
                             const std::string&,
                             bool,
                             const UriHandlerFunctionContinuation&)> UriAsyncUploadHandlerFunction;

typedef boost::variant<UriAsyncHandlerFunction,
            UriAsyncUploadHandlerFunction> UriAsyncHandlerFunctionVariant;

class UriHandler
{
public:
   UriHandler(const std::string& prefix, const UriAsyncHandlerFunction& function);
   UriHandler(const std::string& prefix, const UriHandlerFunction& function);
   UriHandler(const std::string& prefix, const UriAsyncUploadHandlerFunction& function);

   // COPYING: via compiler
   
   bool matches(const std::string& uri) const;
   
   UriAsyncHandlerFunctionVariant function() const;
  
   // implement UriHandlerFunction concept
   void operator()(const Request& request,
                   const UriHandlerFunctionContinuation& cont) const;

   void operator()(const Request& request,
                   const std::string& formData,
                   bool complete,
                   const UriHandlerFunctionContinuation& cont) const;
   
private:
   std::string prefix_;
   UriAsyncHandlerFunctionVariant function_;
};

class UriHandlers
{
public:
   UriHandlers() {}
   
   // COPYING: via compiler
   
   void add(const UriHandler& handler);
   
   boost::optional<UriAsyncHandlerFunctionVariant> handlerFor(const std::string& uri) const;
   
private:
   std::vector<UriHandler> uriHandlers_;
};

inline void notFoundHandler(const Request& request, Response* pResponse)
{
   pResponse->setNotFoundError(request);
}

void visitHandler(const UriAsyncHandlerFunctionVariant& variant,
                  const Request& request,
                  const UriHandlerFunctionContinuation& cont);

} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_URI_HANDLER_HPP


