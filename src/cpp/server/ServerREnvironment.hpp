/*
 * ServerREnvironment.hpp
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

#ifndef SERVER_R_ENVIRONMENT_HPP
#define SERVER_R_ENVIRONMENT_HPP

#include <string>
#include <vector>

#include <core/r_util/REnvironment.hpp>
#include <core/r_util/RVersionsPosix.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace server {
namespace r_environment {

bool hasFallbackVersion();
void setFallbackVersion(const core::r_util::RVersion& version);

bool initialize(std::string* pErrMsg);

core::r_util::RVersion rVersion();
void setRVersion(const core::r_util::RVersion& version);

bool detectRVersion(const core::FilePath& rScriptPath,
                    core::r_util::RVersion* pVersion,
                    std::string* pErrMsg);

} // namespace r_environment
} // namespace server
} // namespace rstudio

#endif // SERVER_R_ENVIRONMENT_HPP

