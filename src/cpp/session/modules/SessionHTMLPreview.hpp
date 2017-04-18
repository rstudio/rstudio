/*
 * SessionHTMLPreview.hpp
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

#ifndef SESSION_HTML_PREVIEW_HPP
#define SESSION_HTML_PREVIEW_HPP

#define kQtMathJaxConfigScript "<script type=\"text/x-mathjax-config\">" \
   "MathJax.Hub.Config({" \
   "  \"HTML-CSS\": { minScaleAdjust: 125, availableFonts: [] } " \
   " });" \
   "</script>"

#include <core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
   namespace http {
      class Response;
   }
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace html_preview {

core::json::Object capabilitiesAsJson();
void addFileSpecificHeaders(const core::FilePath& filePath,
                            core::http::Response* pResponse);

core::Error initialize();
                       
} // namespace html_preview
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_HTML_PREVIEW_HPP
