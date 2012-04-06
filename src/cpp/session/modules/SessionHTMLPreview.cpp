/*
 * SessionHTMLPreview.cpp
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


#include "SessionHTMLPreview.hpp"

#include <boost/bind.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/http/Util.hpp>
#include <core/text/TemplateFilter.hpp>

#include <core/markdown/Markdown.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace html_preview {

namespace {

const char * const kHTMLPreviewLocation = "/html_preview/";

FilePath s_currentPreviewDir;

void setDocumentResponse(const std::string& htmlOutput,
                         http::Response* pResponse)
{
   // determine location of template
   FilePath resourcesPath = session::options().rResourcesPath();
   FilePath htmlPreviewFile = resourcesPath.childPath("html_preview.htm");

   // setup template filter
   std::map<std::string,std::string> vars;
   vars["html_output"] = htmlOutput;
   text::TemplateFilter filter(vars);

   // send response
   pResponse->setNoCacheHeaders();
   pResponse->setBody(htmlPreviewFile, filter);
}

void handlePreviewRequest(const http::Request& request,
                          http::Response* pResponse)
{
   // get the requested path
   std::string path = http::util::pathAfterPrefix(request,
                                                  kHTMLPreviewLocation);

   // if it is empty then this is the main request
   if (path.empty())
   {
      // get the file
      std::string file = request.queryParamValue("source_file");
      if (file.empty())
      {
         pResponse->setError(http::status::NotFound, "No file requested");
         return;
      }

      // resolve alias and ensure that it exists
      FilePath filePath = module_context::resolveAliasedPath(file);
      if (!filePath.exists())
      {
         pResponse->setError(http::status::NotFound, "File doesn't exist");
         return;
      }

      // run markdown processor on it
      std::string htmlOutput;
      Error error = markdownToHTML(filePath,
                                   markdown::Extensions(),
                                   markdown::HTMLOptions(),
                                   &htmlOutput);
      if (error)
      {
         LOG_ERROR(error);
         pResponse->setError(http::status::InternalServerError,
                             "Error rendering markdown");
         return;
      }

      // set the current preview dir (for resolving relative references)
      s_currentPreviewDir = filePath.parent();

      // return html
      setDocumentResponse(htmlOutput, pResponse);
   }

   // request for dependent file
   else
   {
      // ensure the request is in sequence
      if (s_currentPreviewDir.empty())
      {
         pResponse->setError(http::status::BadRequest, "No active preview");
         return;
      }

      // return the file
      FilePath filePath = s_currentPreviewDir.childPath(path);
      pResponse->setCacheableFile(filePath, request);
   }
}

   
} // anonymous namespace
   
Error initialize()
{  
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerUriHandler, kHTMLPreviewLocation, handlePreviewRequest))
   ;
   return initBlock.execute();
}
   


} // namespace html_preview
} // namespace modules
} // namesapce session

