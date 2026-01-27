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

// RStudioVersion implementation

RStudioVersion::RStudioVersion()
   : major(0), minor(0), patch(0), suffix(""), dailyBuildNumber(-1)
{
}

bool RStudioVersion::parse(const std::string& versionStr)
{
   // Reset to defaults
   major = 0;
   minor = 0;
   patch = 0;
   suffix = "";
   dailyBuildNumber = -1;

   if (versionStr.empty())
      return false;

   // Find where suffix starts
   // Formats:
   //   Pre-release: "2026.04.0-daily+172" or "2026.04.0-preview"
   //   Release:     "2026.01.0+392" or "2026.01.0"
   std::string baseVersion = versionStr;
   size_t dashPos = versionStr.find('-');
   size_t plusPos = versionStr.find('+');

   if (dashPos != std::string::npos)
   {
      // Has a dash - suffix starts at dash (e.g., "-daily+172")
      baseVersion = versionStr.substr(0, dashPos);
      suffix = versionStr.substr(dashPos);

      // Extract build number if present from -daily+N, -dev+N, or -hourly+N suffixes
      size_t suffixPlusPos = suffix.find('+');
      if (suffixPlusPos != std::string::npos &&
          (suffix.find("-daily") == 0 || suffix.find("-dev") == 0 || suffix.find("-hourly") == 0))
      {
         std::string buildNumStr = suffix.substr(suffixPlusPos + 1);
         dailyBuildNumber = safe_convert::stringTo<int>(buildNumStr, -1);
      }
   }
   else if (plusPos != std::string::npos)
   {
      // No dash but has plus - release build with build number (e.g., "2026.01.0+392")
      baseVersion = versionStr.substr(0, plusPos);
      suffix = versionStr.substr(plusPos);
      // Extract build number for release comparison
      std::string buildNumStr = suffix.substr(1); // Skip the '+'
      dailyBuildNumber = safe_convert::stringTo<int>(buildNumStr, -1);
   }
   // else: no suffix at all (e.g., "2026.01.0")

   // Parse base version (YYYY.MM.P)
   std::vector<std::string> parts;
   boost::split(parts, baseVersion, boost::is_any_of("."));

   if (parts.size() < 3)
      return false;

   major = safe_convert::stringTo<int>(parts[0], -1);
   if (major < 0)
      return false;

   minor = safe_convert::stringTo<int>(parts[1], -1);
   if (minor < 0)
      return false;

   patch = safe_convert::stringTo<int>(parts[2], -1);
   if (patch < 0)
      return false;

   return true;
}

bool RStudioVersion::operator<(const RStudioVersion& other) const
{
   // Compare major.minor.patch first
   if (major != other.major)
      return major < other.major;
   if (minor != other.minor)
      return minor < other.minor;
   if (patch != other.patch)
      return patch < other.patch;

   // Same base version - compare by suffix type and build number
   // Ordering (oldest to newest): pre-release builds < release builds
   //
   // Release: suffix is empty OR starts with "+" (e.g., "", "+392")
   // Pre-release: suffix starts with "-" (e.g., "-daily+172", "-preview")
   bool thisIsRelease = suffix.empty() || (!suffix.empty() && suffix[0] == '+');
   bool otherIsRelease = other.suffix.empty() || (!other.suffix.empty() && other.suffix[0] == '+');

   // Pre-release builds with build numbers (daily/dev/hourly)
   bool thisHasBuildNum = (dailyBuildNumber >= 0);
   bool otherHasBuildNum = (other.dailyBuildNumber >= 0);

   // Release versions are newest
   if (thisIsRelease && !otherIsRelease)
      return false; // Release > pre-release
   if (!thisIsRelease && otherIsRelease)
      return true;  // Pre-release < Release

   // Both are releases - compare by build number
   if (thisIsRelease && otherIsRelease)
   {
      // Both empty suffix - equal
      if (suffix.empty() && other.suffix.empty())
         return false;
      // One empty, one with build number - treat empty as "latest" or equal
      if (suffix.empty() || other.suffix.empty())
         return false;
      // Both have build numbers - compare numerically
      return dailyBuildNumber < other.dailyBuildNumber;
   }

   // Both are pre-release - compare by build number presence
   // Versions with build numbers (daily/dev/hourly) are older than those without (preview, etc.)
   if (thisHasBuildNum && !otherHasBuildNum)
      return true;  // daily/dev < preview/other
   if (!thisHasBuildNum && otherHasBuildNum)
      return false; // preview/other > daily/dev

   // Both have build numbers - compare build numbers
   if (thisHasBuildNum && otherHasBuildNum)
      return dailyBuildNumber < other.dailyBuildNumber;

   // Neither has build numbers (both have suffixes like -preview, -beta, etc.)
   // Compare suffixes lexically as a fallback
   return suffix < other.suffix;
}

bool RStudioVersion::operator>(const RStudioVersion& other) const
{
   return other < *this;
}

bool RStudioVersion::operator<=(const RStudioVersion& other) const
{
   return !(*this > other);
}

bool RStudioVersion::operator>=(const RStudioVersion& other) const
{
   return !(*this < other);
}

bool RStudioVersion::operator==(const RStudioVersion& other) const
{
   return major == other.major &&
          minor == other.minor &&
          patch == other.patch &&
          suffix == other.suffix;
}

bool RStudioVersion::operator!=(const RStudioVersion& other) const
{
   return !(*this == other);
}

} // namespace types
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
