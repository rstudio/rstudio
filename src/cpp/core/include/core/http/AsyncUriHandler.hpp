/*
 * AsyncUriHandler.hpp
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

#ifndef CORE_HTTP_ASYNC_URI_HANDLER_HPP
#define CORE_HTTP_ASYNC_URI_HANDLER_HPP

#include <string>
#include <vector>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/variant.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/http/UriHandler.hpp>
#include <core/http/AsyncConnection.hpp>

namespace rstudio {
namespace core {
namespace http {

// AsyncUriHandlerFunction concept
typedef boost::function<void(
            boost::shared_ptr<AsyncConnection>)> AsyncUriHandlerFunction;

typedef boost::function<bool(
            boost::shared_ptr<AsyncConnection>, const std::string&, bool)> AsyncUriUploadHandlerFunction;

typedef boost::variant<AsyncUriHandlerFunction,
            AsyncUriUploadHandlerFunction> AsyncUriHandlerFunctionVariant;

class AsyncUriHandlerFunctionVariantVisitor : public boost::static_visitor<void>
{
public:
   AsyncUriHandlerFunctionVariantVisitor(boost::shared_ptr<AsyncConnection> pConnection) :
      pConnection_(pConnection)
   {
   }

   AsyncUriHandlerFunctionVariantVisitor(boost::shared_ptr<AsyncConnection> pConnection,
                                         const std::string& formData,
                                         bool isComplete) :
      pConnection_(pConnection),
      formData_(formData),
      isComplete_(isComplete)
   {
   }

   void operator()(const AsyncUriHandlerFunction& func)
   {
      func(pConnection_);
   }

   void operator()(const AsyncUriUploadHandlerFunction& func)
   {
      func(pConnection_, formData_, isComplete_);
   }

private:
   boost::shared_ptr<AsyncConnection> pConnection_;
   std::string formData_;
   bool isComplete_;
};

class AsyncUriHandler
{
public:
   AsyncUriHandler() : isProxyHandler_(false) {} // other members default initialized

   AsyncUriHandler(const std::string& prefix,
                   AsyncUriHandlerFunctionVariant function,
                   bool isProxyHandler = false)
       : prefix_(prefix), function_(function), isProxyHandler_(isProxyHandler)
   {
   }

   // COPYING: via compiler

   bool matches(const std::string& uri) const
   {
      return boost::algorithm::starts_with(uri, prefix_);
   }

   boost::optional<AsyncUriHandlerFunctionVariant> function() const
   {
      return function_;
   }


   // implement AsyncUriHandlerFunction concept
   void operator()(boost::shared_ptr<AsyncConnection> pConnection) const
   {
      if (!function_)
         return;

      AsyncUriHandlerFunctionVariantVisitor visitor(pConnection);
      boost::apply_visitor(visitor, function_.get());
   }

   void operator()(boost::shared_ptr<AsyncConnection> pConnection,
                   const std::string& formData,
                   bool isComplete) const
   {
      if (!function_)
         return;

      AsyncUriHandlerFunctionVariantVisitor visitor(pConnection, formData, isComplete);
      boost::apply_visitor(visitor, function_.get());
   }

   bool isProxyHandler() const
   {
      return isProxyHandler_;
   }

private:
   std::string prefix_;
   boost::optional<AsyncUriHandlerFunctionVariant> function_;
   bool isProxyHandler_;

};

inline void visitHandler(const AsyncUriHandlerFunctionVariant& variant,
                         boost::shared_ptr<AsyncConnection> pConnection)
{
   AsyncUriHandlerFunctionVariantVisitor visitor(pConnection);
   boost::apply_visitor(visitor, variant);
}

class AsyncUriHandlers
{
   // COPYING: via compiler

public:
   void add(AsyncUriHandler handler)
   {
      uriHandlers_.push_back(handler);
   }

   AsyncUriHandler handlerFor(const std::string& uri) const
   {
      std::vector<AsyncUriHandler>::const_iterator handler =
            std::find_if(
              uriHandlers_.begin(),
              uriHandlers_.end(),
              boost::bind(&AsyncUriHandler::matches, _1, uri));
      if ( handler != uriHandlers_.end() )
      {
         return *handler;
      }
      else
      {
         return AsyncUriHandler();
      }
   }

private:
   std::vector<AsyncUriHandler> uriHandlers_;
};

} // namespace http
} // namespace core
} // namespace rstudio


#endif // CORE_HTTP_ASYNC_URI_HANDLER_HPP


