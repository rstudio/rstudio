/*
 * RMarkdownPresentation.hpp
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

#ifndef SESSION_SESSION_RMARKDOWN_PRESENTATION_HPP
#define SESSION_SESSION_RMARKDOWN_PRESENTATION_HPP

#include <string>

namespace rstudio {
namespace core {
   class FilePath;
}
}

#include <shared_core/json/Json.hpp>
 
namespace rstudio {
namespace session {
namespace modules {      
namespace rmarkdown {
namespace presentation {

void ammendResults(const std::string& formatName,
                   core::FilePath& targetFile,
                   int sourceLine,
                   core::json::Object* pResultsJson);


} // namespace presentation
} // namespace rmarkdown
} // namepace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_RMARKDOWN_PRESENTATION_HPP
