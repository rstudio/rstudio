/*
 * AsyncUriHandler.hpp
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

#ifndef CORE_HTTP_ASYNC_URI_HANDLER_HPP
#define CORE_HTTP_ASYNC_URI_HANDLER_HPP

#include <string>
#include <vector>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/http/UriHandler.hpp>
#include <core/http/AsyncConnection.hpp>


namespace core {
namespace http {

// AsyncUriHandlerFunction concept
typedef boost::function<void(
            boost::shared_ptr<AsyncConnection>)> AsyncUriHandlerFunction;

class AsyncUriHandler
{
public:

public:
   AsyncUriHandler(const std::string& prefix,
                   AsyncUriHandlerFunction function)
       : prefix_(prefix), function_(function)
   {
   }

   // COPYING: via compiler

   bool matches(const std::string& uri) const
   {
      return boost::algorithm::starts_with(uri, prefix_);
   }

   AsyncUriHandlerFunction function() const
   {
      return function_;
   }


   // implement AsyncUriHandlerFunction concept
   void operator()(boost::shared_ptr<AsyncConnection> pConnection) const
   {
      function_(pConnection);
   }

private:
   std::string prefix_;
   AsyncUriHandlerFunction function_ ;

};

class AsyncUriHandlers
{
   // COPYING: via compiler

public:
   void add(AsyncUriHandler handler)
   {
      uriHandlers_.push_back(handler);
   }

   AsyncUriHandlerFunction handlerFor(const std::string& uri) const
   {
      std::vector<AsyncUriHandler>::const_iterator handler =
            std::find_if(
              uriHandlers_.begin(),
              uriHandlers_.end(),
              boost::bind(&AsyncUriHandler::matches, _1, uri));
      if ( handler != uriHandlers_.end() )
      {
         return handler->function();
      }
      else
      {
         return AsyncUriHandlerFunction();
      }
   }

private:
   std::vector<AsyncUriHandler> uriHandlers_;
};

} // namespace http
} // namespace core


#endif // CORE_HTTP_ASYNC_URI_HANDLER_HPP


