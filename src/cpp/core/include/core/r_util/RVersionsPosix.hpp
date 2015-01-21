/*
 * RVersions.hpp
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

#ifndef CORE_R_UTIL_R_VERSIONS_HPP
#define CORE_R_UTIL_R_VERSIONS_HPP

#include <vector>
#include <iosfwd>

#include <core/FilePath.hpp>

#include <core/system/Environment.hpp>

#include <core/r_util/RVersionInfo.hpp>

namespace rstudio {
namespace core {
namespace r_util {

struct RVersion
{
   RVersion() : isDefault(false) {}
   bool isDefault;
   std::string number;
   core::system::Options environment;

   FilePath homeDir() const
   {
      return FilePath(core::system::getenv(environment, "R_HOME"));
   }
};


std::ostream& operator<<(std::ostream& os, const RVersion& version);

std::vector<RVersion> enumerateRVersions(
                              const std::vector<FilePath>& otherRHomes,
                              const FilePath& ldPathsScript,
                              const std::string& ldLibraryPath);

RVersion selectVersion(const RVersionInfo& matchVersion,
                       std::vector<RVersion> versions);

} // namespace r_util
} // namespace core 
} // namespace rstudio

#endif // CORE_R_UTIL_R_VERSIONS_HPP

