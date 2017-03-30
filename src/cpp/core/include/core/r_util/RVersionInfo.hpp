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
#include <iostream>

#include <boost/regex.hpp>

#include <core/RegexUtils.hpp>
#include <core/SafeConvert.hpp>

#define kRVersionDefault   "Default"

#define kRVersionArch32    "32"
#define kRVersionArch64    "64"

namespace rstudio {
namespace core {
namespace r_util {

struct RVersionInfo
{
   explicit RVersionInfo(const std::string& number = kRVersionDefault,
                         const std::string& arch = std::string())
      : number(number), arch(arch)
   {
   }

   std::string number;
   std::string arch;

   bool isDefault() const { return number == kRVersionDefault; }
};

class RVersionNumber
{
public:
   static RVersionNumber parse(const std::string& number)
   {
      boost::regex re("(\\d+)\\.(\\d+)(?:.(\\d+))?");
      boost::smatch match;
      boost::match_flag_type flags = boost::match_default |
                                     boost::match_continuous;

      RVersionNumber ver;
      if (regex_utils::search(number, match, re, flags))
      {
         ver.versionMajor_ = safe_convert::stringTo<int>(match[1], 0);
         ver.versionMinor_ = safe_convert::stringTo<int>(match[2], 0);
         std::string match3 = match[3];
         if (!match3.empty())
            ver.versionPatch_ = safe_convert::stringTo<int>(match3, 0);
      }
      return ver;
   }

   RVersionNumber()
      : versionMajor_(0), versionMinor_(0), versionPatch_(0)
   {
   }

public:
   bool empty() const { return versionMajor_ != 0; }

   int versionMajor() const { return versionMajor_; }
   int versionMinor() const { return versionMinor_; }
   int versionPatch() const { return versionPatch_; }

   bool operator<(const RVersionNumber& other) const
   {
      if (versionMajor() == other.versionMajor() && versionMinor() == other.versionMinor())
         return versionPatch() < other.versionPatch();
      else if (versionMajor() == other.versionMajor())
         return versionMinor() < other.versionMinor();
      else
         return versionMajor() < other.versionMajor();
   }

   bool operator==(const RVersionNumber& other) const
   {
      return versionMajor() == other.versionMajor() &&
             versionMinor() == other.versionMinor() &&
             versionPatch() == other.versionPatch();
   }

   bool operator!=(const RVersionNumber& other) const
   {
      return !(*this == other);
   }

private:
   int versionMajor_;
   int versionMinor_;
   int versionPatch_;
};

inline std::ostream& operator<<(std::ostream& os, const RVersionNumber& ver)
{
   os << ver.versionMajor() << "." << ver.versionMinor() << "." << ver.versionPatch();
   return os;
}


} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_VERSION_INFO_HPP

