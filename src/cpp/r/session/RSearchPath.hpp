/*
 * RSearchPath.hpp
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

#ifndef R_SESSION_SEARCH_PATH_HPP
#define R_SESSION_SEARCH_PATH_HPP

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
namespace search_path {

core::Error save(const core::FilePath& statePath);
core::Error saveGlobalEnvironment(const core::FilePath& statePath);
core::Error restore(const core::FilePath& statePath, bool isCompatibleSessionState = true);
   
} // namespace search_path
} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_SESSION_SEARCH_PATH_HPP 

