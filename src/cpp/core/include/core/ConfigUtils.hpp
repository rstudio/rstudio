/*
 * ConfigUtils.hpp
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

#ifndef CORE_CONFIG_UTILS_HPP
#define CORE_CONFIG_UTILS_HPP

#include <string>
#include <map>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace config_utils {
   
typedef std::map<std::string,std::string> Variables;

void extractVariables(const std::string& vars, Variables* pVariables);
Error extractVariables(const FilePath& file, Variables* pVariables);

} // namespace config_utils
} // namespace core 
} // namespace rstudio


#endif // CORE_CONFIG_UTILS_HPP

