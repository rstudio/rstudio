/*
 * ServerBrowser.cpp
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

#include "ServerBrowser.hpp"

#include <core/Log.hpp>
#include <core/BrowserUtils.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <server/ServerOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace browser {

const char * const kBrowserUnsupported = "/unsupported_browser.htm";

bool supportedBrowserFilter(const http::Request& request,
                            http::Response* pResponse)
{
   if (options().wwwVerifyUserAgent())
   {
      std::string userAgent = request.headerValue("User-Agent");
      if (browser_utils::hasRequiredBrowser(userAgent))
      {
         return true;
      }
      else
      {
         pResponse->setMovedTemporarily(request, kBrowserUnsupported);
         return false;
      }
   }
   else
   {
      return true;
   }
}


void handleBrowserUnsupportedRequest(const http::Request& request,
                                     http::Response* pResponse)
{
   // get the path to the browser file
   Options& options = server::options();
   FilePath wwwPath(options.wwwLocalPath());
   FilePath browserFilePath = wwwPath.completePath(std::string(".") + kBrowserUnsupported);

   // return browser page
   pResponse->setNoCacheHeaders();
   pResponse->setFile(browserFilePath, request);
   pResponse->setContentType("text/html");
}
   
} // namespace browser
} // namespace server
} // namespace rstudio

