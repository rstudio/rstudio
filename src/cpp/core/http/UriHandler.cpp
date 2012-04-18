/*
 * UriHandler.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/http/UriHandler.hpp>

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/http/Request.hpp>

namespace core {
namespace http {

namespace {

void runSynchronousHandler(const UriHandlerFunction& function,
                           const Request& request,
                           const UriHandlerFunctionContinuation& cont)
{
   http::Response response;
   function(request, &response);
   cont(&response);
}

UriAsyncHandlerFunction adaptToAsync(const UriHandlerFunction& function)
{
   return boost::bind(runSynchronousHandler, function, _1, _2);
}

} // anonymous namespace

UriHandler::UriHandler(const std::string& prefix,
                       const UriAsyncHandlerFunction& function)
   : prefix_(prefix), function_(function)
{
}

UriHandler::UriHandler(const std::string& prefix,
                       const UriHandlerFunction& function)
   : prefix_(prefix), function_(adaptToAsync(function))
{
}

bool UriHandler::matches(const std::string& uri) const
{
   return boost::algorithm::starts_with(uri, prefix_);
}

UriAsyncHandlerFunction UriHandler::function() const
{
   return function_;
}

// implement UriHandlerFunction concept
void UriHandler::operator()(const Request& request,
                            const UriHandlerFunctionContinuation& cont) const
{
   function_(request, cont);
}
   
void UriHandlers::add(const UriHandler& handler) 
{
   uriHandlers_.push_back(handler);
}

UriAsyncHandlerFunction UriHandlers::handlerFor(const std::string& uri) const
{
   std::vector<UriHandler>::const_iterator handler = std::find_if(
                              uriHandlers_.begin(), 
                              uriHandlers_.end(), 
                              boost::bind(&UriHandler::matches, _1, uri));
   if ( handler != uriHandlers_.end() )
   {
      return handler->function();
   }
   else
   {
      return UriAsyncHandlerFunction();
   }
}
   
} // namespace http
} // namespace core

