/*
 * SessionHelpHome.cpp
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

#include "SessionHelpHome.hpp"

#include <core/text/TemplateFilter.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace session {
namespace modules { 
namespace help {

namespace {

} // anonymous namespace

void handleHelpHomeRequest(const core::http::Request& request,
                                const std::string& jsCallbacks,
                                core::http::Response* pResponse)
{
   // get the resource path
   FilePath helpResPath = options().rResourcesPath().complete("help_resources");

   // resolve the file reference
   std::string path = http::util::pathAfterPrefix(request,
                                                  "/help/doc/home/");

   // if it's empty then this is the root template
   if (path.empty())
   {

      std::map<std::string,std::string> variables;
      variables["js_callbacks"] = jsCallbacks;
      text::TemplateFilter templateFilter(variables);
      pResponse->setNoCacheHeaders();
      pResponse->setFile(helpResPath.childPath("index.htm"),
                         request,
                         templateFilter);

   }
   // otherwise it's just a file reference
   else
   {
      FilePath filePath = helpResPath.complete(path);
      pResponse->setCacheableFile(filePath, request);
   }
}



} // namepsace help
} // namespace modules
} // namespace session
} // namespace rstudio

