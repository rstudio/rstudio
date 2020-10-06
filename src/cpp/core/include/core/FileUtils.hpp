/*
 * FileUtils.hpp
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

#ifndef CORE_FILEUTILS_HPP
#define CORE_FILEUTILS_HPP

#include <string>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace file_utils {

FilePath uniqueFilePath(const core::FilePath& parent,
                        const std::string& prefix = "",
                        const std::string& extension = "");

std::string readFile(const core::FilePath& filePath);

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
