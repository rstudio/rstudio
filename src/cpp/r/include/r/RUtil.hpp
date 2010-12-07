/*
 * RUtil.hpp
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

#ifndef R_UTIL_HPP
#define R_UTIL_HPP

#include <string>

namespace core {
   class FilePath;
}

namespace r {
namespace util {
   
std::string expandFileName(const std::string& name);
   
std::string fixPath(const std::string& path);   

 
} // namespace util   
} // namespace r


#endif // R_UTIL_HPP 

