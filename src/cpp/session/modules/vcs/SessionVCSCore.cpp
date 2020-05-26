/*
 * SessionVCSCore.cpp
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
#include "SessionVCSCore.hpp"

#include <shared_core/FilePath.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace source_control {

VCSStatus StatusResult::getStatus(const FilePath& fileOrDirectory) const
{
   std::map<std::string, VCSStatus>::const_iterator found =
         this->filesByPath_.find(fileOrDirectory.getAbsolutePath());
   if (found != this->filesByPath_.end())
      return found->second;

   return VCSStatus();
}

} // namespace source_control
} // namespace modules
} // namespace session
} // namespace rstudio
