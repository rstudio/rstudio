/*
 * RVersionInfo.hpp
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

#ifndef CORE_R_UTIL_R_VERSION_INFO_HPP
#define CORE_R_UTIL_R_VERSION_INFO_HPP

#include <string>

#define kRVersionDefault   "Default"
#define kRVersionArch32    "32"
#define kRVersionArch64    "64"

namespace core {
namespace r_util {

struct RVersionInfo
{
   explicit RVersionInfo(const std::string& number,
                         const std::string& arch = std::string())
      : number(number), arch(arch)
   {
   }

   std::string number;
   std::string arch;

   bool isDefault() const { return number == kRVersionDefault; }
};


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_VERSION_INFO_HPP

