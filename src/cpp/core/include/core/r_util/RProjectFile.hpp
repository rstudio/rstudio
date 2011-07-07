/*
 * RProjectFile.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_R_UTIL_R_PROJECT_FILE_HPP
#define CORE_R_UTIL_R_PROJECT_FILE_HPP

#include <string>

namespace core {

class Error;
class FilePath;

namespace r_util {

core::Error writeProjectFile(const std::string& uuid,
                             const FilePath& filePath);

core::FilePath projectFromDirectory(const core::FilePath& directoryPath);

} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_PROJECT_FILE_HPP

