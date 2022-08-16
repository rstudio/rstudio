/*
 * FileUtils.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace file_utils {

std::string shortPathName(const std::string& string);

FilePath uniqueFilePath(const core::FilePath& parent,
                        const std::string& prefix = "",
                        const std::string& extension = "");

std::string readFile(const core::FilePath& filePath);
Error writeFile(const FilePath& filePath, const std::string& content);

#ifdef _WIN32
bool isWindowsReservedName(const std::string& name);
#endif

Error copyDirectory(const FilePath& sourceDirectory,
                    const FilePath& targetDirectory);

bool isDirectoryWriteable(const FilePath& directory);

} // namespace file_utils
} // namespace core
} // namespace rstudio

#endif // CORE_FILEUTILS_HPP
