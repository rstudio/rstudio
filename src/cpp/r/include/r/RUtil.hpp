/*
 * RUtil.hpp
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

#ifndef R_UTIL_HPP
#define R_UTIL_HPP

#include <string>

namespace core {
   class FilePath;
   class Error;
}

namespace r {
namespace util {
   
std::string expandFileName(const std::string& name);
   
std::string fixPath(const std::string& path);

bool hasRequiredVersion(const std::string& version);

bool hasCapability(const std::string& capability);

std::string rconsole2utf8(const std::string& encoded);

core::Error iconvstr(const std::string& value,
                     const std::string& from,
                     const std::string& to,
                     bool allowSubstitution,
                     std::string* result);

} // namespace util   
} // namespace r


#endif // R_UTIL_HPP 

