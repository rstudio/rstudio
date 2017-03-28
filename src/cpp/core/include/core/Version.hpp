/*
 * Version.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef CORE_VERSION_HPP
#define CORE_VERSION_HPP

#include <string>

#include <boost/regex.hpp>

#include <core/Algorithm.hpp>
#include <core/Error.hpp>
#include <core/SafeConvert.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {

class Version
{
   
public:
   
   Version()
   {
   }

   Version(std::string version)
   {
      try
      {
         // split version into components
         static boost::regex reVersion("[._-]+");
         version = core::string_utils::trimWhitespace(version);
         boost::sregex_token_iterator it(version.begin(), version.end(), reVersion, -1);
         boost::sregex_token_iterator end;
         for (; it != end; ++it)
         {
            std::string component = *it;
            int value = core::safe_convert::stringTo<int>(component, 0);
            pieces_.push_back(value);
         }
      }
      CATCH_UNEXPECTED_EXCEPTION;
   }
   
   Version(int versionMajor, int versionMinor, int versionPatch)
   {
      pieces_.push_back(versionMajor);
      pieces_.push_back(versionMinor);
      pieces_.push_back(versionPatch);
   }
   
   int versionMajor() const
   {
      return extractVersion(0);
   }
   
   int versionMinor() const
   {
      return extractVersion(1);
   }
   
   int versionPatch() const
   {
      return extractVersion(2);
   }
   
   int extractVersion(std::size_t index) const
   {
      if (index < pieces_.size())
         return pieces_[index];
      
      return 0;
   }
   
   int compare(const Version& other) const
   {
      return compare(*this, other);
   }
   
   operator std::string() const
   {
      if (pieces_.empty())
         return std::string();
      
      std::stringstream ss;
      ss << boost::lexical_cast<std::string>(pieces_[0]);
      for (std::size_t i = 1, n = pieces_.size(); i < n; ++i)
      {
         ss << ".";
         ss << boost::lexical_cast<std::string>(pieces_[i]);
      }
      
      return ss.str();
   }
   
private:
   
   int compare(Version lhs, Version rhs) const
   {
      std::size_t n = std::max(lhs.size(), rhs.size());
      
      for (std::size_t i = 0; i < n; ++i)
      {
         int vl = lhs.extractVersion(i);
         int vr = rhs.extractVersion(i);
         if (vl < vr)
            return -1;
         else if (vl > vr)
            return 1;
      }
      
      return 0;
   }
   
   std::size_t size() const { return pieces_.size(); }
   
   std::vector<int> pieces_;
};

inline bool operator  <(const Version& lhs, const Version& rhs)
{
   return lhs.compare(rhs) < 0;
}

inline bool operator <=(const Version& lhs, const Version& rhs)
{
   return lhs.compare(rhs) <= 0;
}

inline bool operator ==(const Version& lhs, const Version& rhs)
{
   return lhs.compare(rhs) == 0;
}

inline bool operator !=(const Version& lhs, const Version& rhs)
{
   return lhs.compare(rhs) != 0;
}

inline bool operator >=(const Version& lhs, const Version& rhs)
{
   return lhs.compare(rhs) >= 0;
}

inline bool operator  >(const Version& lhs, const Version& rhs)
{
   return lhs.compare(rhs) > 0;
}


} // end namespace core
} // end namespace rstudio

#endif /* CORE_VERSION_HPP */
