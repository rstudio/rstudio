/*
 * REnvironment.hpp
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

#ifndef CORE_R_UTIL_R_ENVIRONMENT_HPP
#define CORE_R_UTIL_R_ENVIRONMENT_HPP

#include <string>
#include <vector>

#include <core/system/Types.hpp>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace r_util {

typedef std::vector<std::pair<std::string,std::string> > EnvironmentVars;

bool detectREnvironment(const FilePath& whichRScript,
                        const FilePath& ldPathsScript,
                        const std::string& ldLibraryPath,
                        std::string* pRScriptPath,
                        std::string* pVersion,
                        EnvironmentVars* pVars,
                        std::string* pErrMsg);

void setREnvironmentVars(const EnvironmentVars& vars);
void setREnvironmentVars(const EnvironmentVars& vars,
                         core::system::Options* pEnv);

std::string rLibraryPath(const FilePath& rHomePath,
                         const FilePath& rLibPath,
                         const FilePath& ldPathsScript,
                         const std::string& ldLibraryPath);

Error rVersion(const FilePath& rHomePath,
               const FilePath& rScriptPath,
               const std::string& ldLibraryPath,
               std::string* pVersion);

void ensureLang();

} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_ENVIRONMENT_HPP

