/*
 * FileUtils.hpp
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

#ifndef CORE_FILEUTILS_HPP
#define CORE_FILEUTILS_HPP

#include <string>



namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace file_utils {

FilePath uniqueFilePath(const core::FilePath& parent,
                        const std::string& prefix = "");

std::string readFile(const core::FilePath& filePath);

#ifdef WIN32
bool isWindowsReservedName(const std::string& name);
#endif

Error copyDirectory(const FilePath& sourceDirectory,
                    const FilePath& targetDirectory);

} // namespace file_utils
} // namespace core
} // namespace rstudio

#endif // CORE_FILEUTILS_HPP
