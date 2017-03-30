/*
 * GwtFileHandler.cpp
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

#include <core/gwt/GwtFileHandler.hpp>

#include <boost/regex.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/FilePath.hpp>
#include <core/RegexUtils.hpp>
#include <core/text/TemplateFilter.hpp>
#include <core/system/System.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>


namespace rstudio {
namespace core {
namespace gwt {   
   
namespace {
   

void handleFileRequest(const std::string& wwwLocalPath,
                       const std::string& baseUri,
                       core::http::UriFilterFunction mainPageFilter,
                       const std::string& initJs,
                       const std::string& gwtPrefix,
                       bool useEmulatedStack,
                       const std::string& frameOptions,
                       const http::Request& request, 
                       http::Response* pResponse)
{
   // get the uri and strip the query string
   std::string uri = request.uri();
   std::size_t pos = uri.find("?");
   if (pos != std::string::npos)
      uri.erase(pos);
            
   // request for one-character short of root location redirects to root
   if (uri == baseUri.substr(0, baseUri.size()-1))
   {
      pResponse->setMovedPermanently(request, baseUri);
      return;
   }
   
   // request for a URI not within our location scope
   if (uri.find(baseUri) != 0)
   {
      pResponse->setNotFoundError(request.uri());
      return;
   }
   
   // auto-append index.htm to request for root location
   const char * const kIndexFile = "index.htm";
   if (uri == baseUri)
      uri += kIndexFile;
   
   // if this is main page and we have a filter then then give it a crack
   // at the request
   std::string mainPage = baseUri + kIndexFile;
   if (uri == mainPage)
   {
      // run filter if we have one
      if (mainPageFilter)
      {
         // if the filter returns false it means we should stop processing
         if (!mainPageFilter(request, pResponse))
            return ;
      }

      // apply browser compatibility headers
      pResponse->setBrowserCompatible(request);
   }
   
   // get the requested file 
   std::string relativePath = uri.substr(baseUri.length());
   FilePath filePath = http::util::requestedFile(wwwLocalPath, relativePath);
   if (filePath.empty())
   {
      pResponse->setNotFoundError(request.uri());
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
      useEmulatedStack = useEmulatedStack ||
                         (request.queryParamValue("emulatedStack") == "1");
      vars["compiler_stack_mode"] = useEmulatedStack ? "emulated" : "native";

      // check for initJs
      if (!initJs.empty())
         vars["head_tags"] = "<script>" + initJs + "</script>";
      else
         vars["head_tags"] = std::string();

      // gwt prefix
      vars["gwt_prefix"] = gwtPrefix;

      // don't allow main page to be framed by other domains (clickjacking
      // defense)
      pResponse->setFrameOptionHeaders(frameOptions);

      // return the page
      pResponse->setNoCacheHeaders();
      pResponse->setFile(filePath, request, text::TemplateFilter(vars));
   }
   
   // case: normal cacheable file
   else
   {
      // since these are application components we force revalidation
      pResponse->setCacheWithRevalidationHeaders();
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
   return boost::bind(handleFileRequest,
                      wwwLocalPath,
                      baseUri,
                      mainPageFilter,
                      initJs,
                      gwtPrefix,
                      useEmulatedStack,
                      frameOptions,
                      _1,
                      _2);
}  

} // namespace gwt
} // namespace core
} // namespace rstudio

