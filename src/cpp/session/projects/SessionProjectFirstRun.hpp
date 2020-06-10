/*
 * SessionProjectFirstRun.hpp
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

#ifndef SESSION_PROJECTS_PROJECT_FIRST_RUN_HPP
#define SESSION_PROJECTS_PROJECT_FIRST_RUN_HPP

#include <string>
#include <vector>

namespace rstudio {
namespace core {
   class FilePath;
}
}
 
namespace rstudio {
namespace session {
namespace projects {

void addFirstRunDoc(const core::FilePath& projectScratchPath, const std::string& doc);
std::vector<std::string> collectFirstRunDocs(const core::FilePath& projectScratchPath);

} // namespace projects
} // namespace session
} // namespace rstudio

#endif // SESSION_PROJECTS_PROJECT_FIRST_RUN_HPP
