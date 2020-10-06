/*
 * TemplateFilter.cpp
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

#include <core/text/TemplateFilter.hpp>

#include <shared_core/FilePath.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace core {
namespace text {

void handleTemplateRequest(const FilePath& templatePath,
                           const http::Request& request,
                           http::Response* pResponse)
{
   // setup template variables
   std::map<std::string,std::string> variables;
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

Error renderTemplate(const core::FilePath& templateFile,
                     const std::map<std::string, std::string> &vars,
                     std::ostream& os)
{
   // open input stream to template
   std::shared_ptr<std::istream> pIfs;
   Error error = templateFile.openForRead(pIfs);
   if (error)
      return error;

   try
   {
      // ensure that errors are reported with exceptions (compatible
      // with behavior of boost::iostreams::copy)
      pIfs->exceptions(std::istream::failbit | std::istream::badbit);
      os.exceptions(std::istream::failbit | std::istream::badbit);

      // create a filtered stream w/ the template filter and std::ostream
      boost::iostreams::filtering_ostream filteredStream;
      text::TemplateFilter templateFilter(vars);
      filteredStream.push(templateFilter);
      filteredStream.push(os);

      // process the template
      boost::iostreams::copy(*pIfs, filteredStream, 128);
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("template-path", templateFile);
      return error;
   }

   return Success();
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
} // namespace rstudio


