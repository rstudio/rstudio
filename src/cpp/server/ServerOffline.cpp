/*
 * ServerOffline.cpp
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

#include "ServerOffline.hpp"

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/gwt/GwtFileHandler.hpp>

#include <core/json/JsonRpc.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>

using namespace core;

namespace server {
namespace offline {
  
namespace {

void handleOfflineRequest(const http::Request& request,
                          http::Response* pResponse)
{
   // send error code for json responses
   if (request.acceptsContentType(json::kJsonContentType))
   {
      json::setJsonRpcError(json::errc::ServerOffline, pResponse);
   }
   
   // send error page for html responses
   else if (request.acceptsContentType("text/html"))
   {
      pResponse->setStatusCode(http::status::ServiceUnavailable);
      pResponse->setNoCacheHeaders();
      FilePath wwwPath(server::options().wwwLocalPath());
      pResponse->setFile(wwwPath.complete("offline.htm"), request);
   }
   
   // other content types just get a plain 503 with no content
   else
   {
       pResponse->setStatusCode(http::status::ServiceUnavailable);
   }
}
   
}
   
Error httpServerAddHandlers()
{
   // alias options
   Options& options = server::options();
   
   // use default gwt handling for image urls (required to render 
   // embedded images in offline page)
   uri_handlers::addBlocking("/images",
                             gwt::fileHandlerFunction(options.wwwLocalPath(),
                                                      "/"));
   
   // default handler sends back offline page or json error as appropriate
   uri_handlers::setBlockingDefault(handleOfflineRequest);
   
   // success
   return Success();
}

} // namespace offline
} // namespace server

