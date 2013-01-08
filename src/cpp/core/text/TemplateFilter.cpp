/*
 * TemplateFilter.cpp
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

#include <core/text/TemplateFilter.hpp>

#include <core/FilePath.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

namespace core {
namespace text {

void handleTemplateRequest(const FilePath& templatePath,
                           const http::Request& request,
                           http::Response* pResponse)
{
   // setup template variables
   std::map<std::string,std::string> variables ;
   http::Fields queryParams = request.queryParams();
   for (http::Fields::const_iterator it = queryParams.begin();
        it != queryParams.end();
        ++it)
   {
      variables[it->first] = it->second;
   }

   // return browser page (processing template)
   pResponse->setNoCacheHeaders();
   text::TemplateFilter templateFilter(variables);
   pResponse->setFile(templatePath, request, templateFilter);
   pResponse->setContentType("text/html");

}

void handleSecureTemplateRequest(const std::string& username,
                                 const FilePath& progressPagePath,
                                 const http::Request& request,
                                 http::Response* pResponse)
{
   handleTemplateRequest(progressPagePath, request, pResponse);
}


} // namespace text
} // namespace core


