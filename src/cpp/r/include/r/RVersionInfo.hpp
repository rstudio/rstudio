/*
 * RVersionInfo.hpp
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

#ifndef R_R_VERSION_INFO_HPP
#define R_R_VERSION_INFO_HPP

#include <core/r_util/RVersionInfo.hpp>

namespace rstudio {
namespace r {
namespace version_info {

// Returns the current R version number (cached)
core::r_util::RVersionNumber currentRVersion();

} // namespace version_info
} // namespace r
} // namespace rstudio

#endif // R_R_VERSION_INFO_HPP
