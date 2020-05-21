/*
 * UriHandler.cpp
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

#include <core/http/UriHandler.hpp>

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/http/Request.hpp>

namespace rstudio {
namespace core {
namespace http {

namespace {

class UriAsyncHandlerFunctionVariantVisitor : public boost::static_visitor<void>
{
public:
   UriAsyncHandlerFunctionVariantVisitor(const Request& request,
                                         const UriHandlerFunctionContinuation& cont) :
      cont_(cont)
   {
      request_.assign(request);
   }

   UriAsyncHandlerFunctionVariantVisitor(const Request& request,
                                         const std::string& formData,
                                         bool complete,
                                         const UriHandlerFunctionContinuation& cont) :
      cont_(cont),
      formData_(formData),
      isComplete_(complete)
   {
      request_.assign(request);
   }

   void operator()(const UriAsyncHandlerFunction& func)
   {
      func(request_, cont_);
   }

   void operator()(const UriAsyncUploadHandlerFunction& func)
   {
      func(request_, formData_, isComplete_, cont_);
   }

private:
   Request request_;
   UriHandlerFunctionContinuation cont_;
   std::string formData_;
   bool isComplete_;
};

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

UriHandler::UriHandler(const std::string& prefix,
                       const UriAsyncUploadHandlerFunction& function)
   : prefix_(prefix), function_(function)
{
}

bool UriHandler::matches(const std::string& uri) const
{
   return boost::algorithm::starts_with(uri, prefix_);
}

UriAsyncHandlerFunctionVariant UriHandler::function() const
{
   return function_;
}

// implement UriHandlerFunction concept
void UriHandler::operator()(const Request& request,
                            const UriHandlerFunctionContinuation& cont) const
{
   UriAsyncHandlerFunctionVariantVisitor visitor(request, cont);
   boost::apply_visitor(visitor, function_);
}

void UriHandler::operator()(const Request& request,
                            const std::string& formData,
                            bool complete,
                            const UriHandlerFunctionContinuation& cont) const
{
   UriAsyncHandlerFunctionVariantVisitor visitor(request, formData, complete, cont);
   boost::apply_visitor(visitor, function_);
}
   
void UriHandlers::add(const UriHandler& handler) 
{
   uriHandlers_.push_back(handler);
}

boost::optional<UriAsyncHandlerFunctionVariant> UriHandlers::handlerFor(const std::string& uri) const
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
      return boost::optional<UriAsyncHandlerFunctionVariant>();
   }
}

void visitHandler(const UriAsyncHandlerFunctionVariant& variant,
                  const Request& request,
                  const UriHandlerFunctionContinuation& cont)
{
   UriAsyncHandlerFunctionVariantVisitor visitor(request, cont);
   boost::apply_visitor(visitor, variant);
}
   
} // namespace http
} // namespace core
} // namespace rstudio

