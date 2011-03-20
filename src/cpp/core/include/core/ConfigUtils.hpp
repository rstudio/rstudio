/*
 * ConfigUtils.hpp
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

#ifndef CORE_CONFIG_UTILS_HPP
#define CORE_CONFIG_UTILS_HPP

#include <string>
#include <vector>

namespace core {

class Error;
class FilePath;

namespace config_utils {
   
typedef std::vector<std::pair<std::string,std::string> > Variables;

Error extractVariables(const FilePath& file, Variables* pVariables);

} // namespace config_utils
} // namespace core 


#endif // CORE_CONFIG_UTILS_HPP

