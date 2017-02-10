/*
 * SessionLibPathsIndexer.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef SESSION_MODULES_LIB_PATHS_INDEXER_HPP
#define SESSION_MODULES_LIB_PATHS_INDEXER_HPP

#include <vector>
#include <map>

namespace rstudio {
namespace core {

class Error;
class FilePath;

} // end namespace core
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace libpaths {

const std::vector<core::FilePath>& getInstalledPackages();
core::Error initialize();

} // end namespace libpaths
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_MODULES_LIB_PATHS_INDEXER_HPP */
