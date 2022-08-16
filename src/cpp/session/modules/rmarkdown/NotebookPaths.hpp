/*
 * NotebookPaths.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_MODULES_RMARKDOWN_NOTEBOOK_PATHS_HPP
#define SESSION_MODULES_RMARKDOWN_NOTEBOOK_PATHS_HPP

#include <string>

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

core::Error notebookPathToId(const core::FilePath& path, std::string* pId);

core::Error notebookIdToPath(const std::string& id, core::FilePath* pPath);

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif /* SESSION_MODULES_RMARKDOWN_NOTEBOOK_PATHS_HPP */
