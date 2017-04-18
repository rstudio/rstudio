/*
 * SessionRMarkdown.hpp
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

#ifndef SESSION_SESSION_RMARKDOWN_HPP
#define SESSION_SESSION_RMARKDOWN_HPP

#include <string>

#define kRenderTypeStatic   0
#define kRenderTypeShiny    1
#define kRenderTypeNotebook 2

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules {      
namespace rmarkdown {

bool rmarkdownPackageAvailable();

bool knitParamsAvailable();

bool knitWorkingDirAvailable();

core::Error evaluateRmdParams(const std::string& docId);

core::Error initialize();

} // namespace rmarkdown
} // namepace handlers
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_RMARKDOWN_HPP
