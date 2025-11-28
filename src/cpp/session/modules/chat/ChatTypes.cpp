/*
 * ChatTypes.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "ChatTypes.hpp"

#include <vector>
#include <boost/algorithm/string.hpp>
#include <shared_core/SafeConvert.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace types {

SemanticVersion::SemanticVersion() : major(0), minor(0), patch(0) {}

bool SemanticVersion::parse(const std::string& versionStr)
{
   // Match format: major.minor.patch (with optional "v" prefix)
   std::string cleanVersion = versionStr;
   if (!cleanVersion.empty() && cleanVersion[0] == 'v')
      cleanVersion = cleanVersion.substr(1);

   // Split on dots
   std::vector<std::string> parts;
   boost::split(parts, cleanVersion, boost::is_any_of("."));

   if (parts.size() < 1)
      return false;

   // Parse major (required)
   major = safe_convert::stringTo<int>(parts[0], -1);
   if (major < 0)
      return false;

   // Parse minor (optional, default to 0)
   if (parts.size() >= 2)
   {
      minor = safe_convert::stringTo<int>(parts[1], -1);
      if (minor < 0)
         return false;
   }

   // Parse patch (optional, default to 0)
   if (parts.size() >= 3)
   {
      patch = safe_convert::stringTo<int>(parts[2], -1);
      if (patch < 0)
         return false;
   }

   return true;
}

bool SemanticVersion::operator>(const SemanticVersion& other) const
{
   if (major != other.major)
      return major > other.major;
   if (minor != other.minor)
      return minor > other.minor;
   return patch > other.patch;
}

bool SemanticVersion::operator<(const SemanticVersion& other) const
{
   return other > *this;
}

bool SemanticVersion::operator>=(const SemanticVersion& other) const
{
   return !(*this < other);
}

bool SemanticVersion::operator<=(const SemanticVersion& other) const
{
   return !(*this > other);
}

bool SemanticVersion::operator==(const SemanticVersion& other) const
{
   return major == other.major &&
          minor == other.minor &&
          patch == other.patch;
}

bool SemanticVersion::operator!=(const SemanticVersion& other) const
{
   return !(*this == other);
}

} // namespace types
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
