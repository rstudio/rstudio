/*
 * GwtFileHandler.cpp
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

#include <core/gwt/GwtFileHandler.hpp>

#include <boost/regex.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/FilePath.hpp>
#include <core/RegexUtils.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/system/System.hpp>
#include <core/http/CSRFToken.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include "config.h"

namespace rstudio {
namespace core {
namespace gwt {
   
namespace {
   
struct FileRequestOptions
{
   std::string wwwLocalPath;
   std::string baseUri;
   core::http::UriFilterFunction mainPageFilter;
   std::string initJs;
   std::string gwtPrefix;
   bool useEmulatedStack;
   std::string frameOptions;
};

void handleFileRequest(const FileRequestOptions& options,
                       const http::Request& request, 
                       http::Response* pResponse)
{
   // get the uri and strip the query string
   std::string uri = request.uri();
   std::size_t pos = uri.find("?");
   if (pos != std::string::npos)
      uri.erase(pos);
            
   // request for one-character short of root location redirects to root
   if (uri == options.baseUri.substr(0, options.baseUri.size()-1))
   {
      pResponse->setMovedPermanently(request, options.baseUri);
      return;
   }
   
   // request for a URI not within our location scope
   if (uri.find(options.baseUri) != 0)
   {
      pResponse->setNotFoundError(request);
      return;
   }
   
   // auto-append index.htm to request for root location
   const char * const kIndexFile = "index.htm";
   if (uri == options.baseUri)
      uri += kIndexFile;
   
   // if this is main page and we have a filter then then give it a crack
   // at the request
   std::string mainPage = options.baseUri + kIndexFile;
   if (uri == mainPage)
   {
      // run filter if we have one
      if (options.mainPageFilter)
      {
         // if the filter returns false it means we should stop processing
         if (!options.mainPageFilter(request, pResponse))
            return;
      }

      // apply browser compatibility headers
      pResponse->setBrowserCompatible(request);
   }
   
   // get the requested file 
   std::string relativePath = uri.substr(options.baseUri.length());
   FilePath filePath = http::util::requestedFile(options.wwwLocalPath, relativePath);
   if (filePath.isEmpty())
   {
      pResponse->setNotFoundError(request);
      return;
   }
   else if (filePath.isDirectory())
   {
      // deny directory listings
      pResponse->setError(http::status::Forbidden, "Forbidden");
      return;
   }
   
   // case: files designated to be cached "forever"
   if (regex_utils::match(uri, boost::regex(".*\\.cache\\..*")))
   {
      pResponse->setCacheForeverHeaders();
      pResponse->setFile(filePath, request);
   }
   
   // case: files designated to never be cached 
   else if (regex_utils::match(uri, boost::regex(".*\\.nocache\\..*")))
   {
      pResponse->setNoCacheHeaders();
      pResponse->setFile(filePath, request);
   }
   // case: main page -- don't cache and dynamically set compiler stack mode
   else if (uri == mainPage)
   {
      // check for emulated stack
      std::map<std::string,std::string> vars;
      bool useEmulatedStack = options.useEmulatedStack ||
                         (request.queryParamValue("emulatedStack") == "1");
      vars["compiler_stack_mode"] = useEmulatedStack ? "emulated" : "native";

      // polyfill for IE11 (only)
      std::string polyfill = "<script type=\"text/javascript\" language=\"javascript\" src=\"js/core-js/minified.js\"></script>\n";
      if (regex_utils::match(request.userAgent(), boost::regex(".*Trident.*"))) {
         vars["head_tags"] = polyfill;
      } else {
         vars["head_tags"] = std::string();
      }

      // check for initJs
      if (!options.initJs.empty())
         vars["head_tags"] = vars["head_tags"] + "<script>" + options.initJs + "</script>";

      // gwt prefix
      vars["gwt_prefix"] = options.gwtPrefix;

#ifndef RSTUDIO_SERVER
      vars["viewport_tag"] = R"(<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />)";
#else
      vars["viewport_tag"] = std::string();
#endif

      // read existing CSRF token
      std::string csrfToken = request.cookieValue(kCSRFTokenCookie);
      vars["csrf_token"] = string_utils::htmlEscape(csrfToken, true /* isAttribute */);

      // don't allow main page to be framed by other domains (clickjacking
      // defense)
      pResponse->setFrameOptionHeaders(options.frameOptions);

      // return the page
      pResponse->setNoCacheHeaders();
      pResponse->setFile(filePath, request, text::TemplateFilter(vars));
   }
   // case: normal cacheable file
   else
   {
      // since these are application components we force revalidation (default behavior of
      // setCacheableFile)
      pResponse->setCacheableFile(filePath, request);
   }
}
   
} // anonymous namespace
   
http::UriHandlerFunction fileHandlerFunction(
                                       const std::string& wwwLocalPath,
                                       const std::string& baseUri,
                                       http::UriFilterFunction mainPageFilter,
                                       const std::string& initJs,
                                       const std::string& gwtPrefix,
                                       bool useEmulatedStack,
                                       const std::string& frameOptions)
{
   FileRequestOptions options { wwwLocalPath, baseUri, mainPageFilter, initJs,
                                gwtPrefix, useEmulatedStack, frameOptions };

   return boost::bind(handleFileRequest,
                      options,
                      _1,
                      _2);
}

} // namespace gwt
} // namespace core
} // namespace rstudio

