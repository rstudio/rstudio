/*
 * ServerOffline.cpp
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

#include "ServerOffline.hpp"

#include <shared_core/Error.hpp>
#include <core/gwt/GwtFileHandler.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/Log.hpp>
#include <core/text/TemplateFilter.hpp>

#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace offline {
  
namespace {

void handleOfflineRequest(const http::Request& request,
                          http::Response* pResponse)
{
   // send error code for json responses
   if (request.acceptsContentType(json::kJsonContentType))
   {
      json::setJsonRpcError(Error(json::errc::ServerOffline, ERROR_LOCATION), pResponse);
   }
   
   // send error page for html responses
   else if (request.acceptsContentType("text/html"))
   {
      std::ostringstream os;
      std::map<std::string, std::string> vars;
      vars["request_uri"] = string_utils::jsLiteralEscape(request.uri());
      vars["base_uri"] = string_utils::jsLiteralEscape(request.baseUri(core::http::BaseUriUse::External));

      FilePath offlineTemplate = FilePath(options().wwwLocalPath()).completeChildPath("offline.htm");
      core::Error err = core::text::renderTemplate(offlineTemplate, vars, os);

      if (err)
      {
         // if we cannot display the page log the error
         // note: this should never happen in a proper deployment
         LOG_ERROR(err);
      }
      else
      {
         std::string body = os.str();
         pResponse->setContentType("text/html");
         pResponse->setBodyUnencoded(body);
      }

      // set 503 status even if there was an error showing the page
      pResponse->setNoCacheHeaders();
      pResponse->setStatusCode(core::http::status::ServiceUnavailable);
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
} // namespace rstudio

