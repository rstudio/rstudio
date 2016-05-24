/*
 * SessionUriHandlers.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionUriHandlers.hpp"

#include <session/SessionConstants.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session { 
namespace uri_handlers {

http::UriHandlers& handlers()
{
   static http::UriHandlers instance;
   return instance;
}

} // namespace uri_handlers

namespace module_context {

Error registerAsyncUriHandler(
                         const std::string& name,
                         const http::UriAsyncHandlerFunction& handlerFunction)
{

   uri_handlers::handlers().add(http::UriHandler(name, handlerFunction));
   return Success();
}

Error registerUriHandler(const std::string& name,
                         const http::UriHandlerFunction& handlerFunction)
{

   uri_handlers::handlers().add(http::UriHandler(name, handlerFunction));
   return Success();
}


Error registerAsyncLocalUriHandler(
                         const std::string& name,
                         const http::UriAsyncHandlerFunction& handlerFunction)
{
   uri_handlers::handlers().add(http::UriHandler(kLocalUriLocationPrefix + name,
                                      handlerFunction));
   return Success();
}

Error registerLocalUriHandler(const std::string& name,
                              const http::UriHandlerFunction& handlerFunction)
{
   uri_handlers::handlers().add(http::UriHandler(kLocalUriLocationPrefix + name,
                                      handlerFunction));
   return Success();
}

} // namespace module_context
} // namespace session
} // namespace rstudio
