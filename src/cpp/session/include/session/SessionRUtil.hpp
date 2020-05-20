/*
 * SessionRUtil.hpp
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

#ifndef SESSION_R_UTIL_HPP
#define SESSION_R_UTIL_HPP

#include <set>
#include <string>

#include <session/SessionSourceDatabase.hpp>

namespace rstudio {
namespace core {
class Error;
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace session {
namespace r_utils {

core::Error extractRCode(const std::string& fileContents,
                         const std::string& documentType,
                         std::string* pCode);

std::set<std::string> implicitlyAvailablePackages(const core::FilePath& filePath);
std::set<std::string> implicitlyAvailablePackages(const core::FilePath& filePath,
                                                  const std::string& fileContents);

core::Error initialize();

} // namespace r_util
} // namespace session
} // namespace rstudio

#endif /* SESSION_R_UTIL_HPP */
