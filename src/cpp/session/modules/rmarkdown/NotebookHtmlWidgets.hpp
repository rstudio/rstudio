/*
 * NotebookHtmlWidgets.hpp
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


#ifndef SESSION_NOTEBOOK_HTML_WIGETS_HPP
#define SESSION_NOTEBOOK_HTML_WIGETS_HPP

#include "NotebookCapture.hpp"

namespace rstudio {
namespace core {
   class FilePath;
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

class HtmlCapture : public NotebookCapture
{
public:
   HtmlCapture();
   ~HtmlCapture();
   core::Error connectHtmlCapture(
         const core::FilePath& outputFolder,
         const core::FilePath& libraryFolder,
         const rstudio::core::json::Object& chunkOptions);
   void disconnect();
private:
};

core::Error initHtmlWidgets();

core::Error mergeLib(const core::FilePath& source, 
                     const core::FilePath& target);

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
