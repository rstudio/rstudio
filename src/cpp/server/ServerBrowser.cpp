/*
 * ServerBrowser.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "ServerBrowser.hpp"

#include <boost/regex.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/SafeConvert.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <server/ServerOptions.hpp>

using namespace core;

namespace server {
namespace browser {

namespace {

int extractSafariVersion(const std::string& userAgent, int defaultVersion)
{
   // Example Safari 4 user-agent string:
   // Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; en-us) AppleWebKit/531.22.7 (KHTML, like Gecko) Version/4.0.5 Safari/531.22.7

   // Example Safari 5 user-agent string:
   // Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; en-us) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4

   try
   {
      boost::regex versionRegex(".*Version/([0-9])\\..*");
      boost::smatch match;
      if (regex_match(userAgent, match, versionRegex))
      {
         return safe_convert::stringTo<int>(match[1], defaultVersion);
      }
      else
      {
         LOG_WARNING_MESSAGE("Unable to parse user agent: " + userAgent);
         return defaultVersion;
      }
   }
   CATCH_UNEXPECTED_EXCEPTION

   return defaultVersion;

   // NOTE: try/catch above is unnecessary but we are being super defensive
   // because we are putting this in at the last minute during closedown
}

} // anonymous namespace


const char * const kBrowserUnsupported = "/browser.htm";

bool supportedBrowserFilter(const http::Request& request,
                            http::Response* pResponse)
{
	using namespace boost::algorithm;

   std::string userAgent = request.headerValue("User-Agent");

   if (contains(userAgent, "Chrome") ||
       contains(userAgent, "Firefox"))
   {
      return true;
	}
   else if (contains(userAgent, "Safari"))
   {
      // extract version (default to 5 to err on the side of not excluding)
      int safariVersion = extractSafariVersion(userAgent, 5);
      if (safariVersion < 5)
      {
         pResponse->setMovedTemporarily(request, kBrowserUnsupported);
         return false;
      }
      else
      {
         return true;
      }
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

