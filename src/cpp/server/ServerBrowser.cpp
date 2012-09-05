/*
 * ServerBrowser.cpp
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

#include "ServerBrowser.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <server/ServerOptions.hpp>

using namespace core;

namespace server {
namespace browser {

const char * const kBrowserUnsupported = "/unsupported_browser.htm";

bool supportedBrowserFilter(const http::Request& request,
                            http::Response* pResponse)
{
	using namespace boost::algorithm;

   std::string userAgent = request.headerValue("User-Agent");

   if (contains(userAgent, "Chrome")      ||
       contains(userAgent, "chromeframe") ||
       contains(userAgent, "Firefox")     ||
       contains(userAgent, "Safari"))
   {
      return true;
	}
   else // unknown browser
   {
      pResponse->setMovedTemporarily(request, kBrowserUnsupported);
      return false;
   }
}


void handleBrowserUnsupportedRequest(const http::Request& request,
                                     http::Response* pResponse)
{
   // get the path to the browser file
   Options& options = server::options();
   FilePath wwwPath(options.wwwLocalPath());
   FilePath browserFilePath = wwwPath.complete(std::string(".") + kBrowserUnsupported);

   // return browser page
   pResponse->setNoCacheHeaders();
   pResponse->setFile(browserFilePath, request);
   pResponse->setContentType("text/html");
}
   
} // namespace browser
} // namespace server

