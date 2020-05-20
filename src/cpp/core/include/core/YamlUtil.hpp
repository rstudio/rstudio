/*
 * YamlUtil.hpp
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

#ifndef CORE_YAML_UTIL_HPP
#define CORE_YAML_UTIL_HPP

#include <string>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace yaml {

bool hasYamlHeader(const core::FilePath& filePath);
bool hasYamlHeader(const std::string& content);

std::string extractYamlHeader(const core::FilePath& filePath);
std::string extractYamlHeader(const std::string& content);

} // namespace yaml
} // namespace core
} // namespace rstudio

#endif /* CORE_YAML_UTIL_HPP */
