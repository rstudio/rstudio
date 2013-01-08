/*
 * SessionViewPdf.cpp
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

#include "SessionViewPdf.hpp"

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Exec.hpp>

#include <core/http/Util.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace tex {
namespace view_pdf {

namespace {

void handleViewPdf(const http::Request& request, http::Response* pResponse)
{
   // get the file path
   FilePath filePath(request.queryParamValue("path"));
   if (!filePath.exists())
   {
      pResponse->setError(http::status::NotFound, "File not found");
      return;
   }

   // send it back
   pResponse->setNoCacheHeaders();
   pResponse->setFile(filePath, request);
   pResponse->setContentType("application/pdf");
}

} // anonymous namespace

std::string createViewPdfUrl(const core::FilePath& filePath)
{
   return "view_pdf?path=" + http::util::urlEncode(filePath.absolutePath(),
                                                   true);
}

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerUriHandler, "/view_pdf", handleViewPdf))
   ;
   return initBlock.execute();
}

} // namespace view_pdf
} // namespace tex
} // namespace modules
} // namesapce session

