/*
 * ServerFormatUtils.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "ServerFormatUtils.hpp"

#include <string>
#include <vector>

namespace rstudio {
namespace server {

namespace {

void appendPart(std::vector<std::string>& parts, int n, const char* singular, const char* plural)
{
   if (n <= 0)
      return;
   parts.push_back(std::to_string(n) + " " + (n == 1 ? singular : plural));
}

// The empty-vector arm is unreachable from formatLoginTimeoutDuration (which
// returns early for n < 1 and always pushes at least one part for n >= 1),
// but is defined for safety so future callers passing an empty vector get a
// silent empty string rather than UB.
std::string oxfordJoin(const std::vector<std::string>& parts)
{
   if (parts.empty())
      return std::string();
   if (parts.size() == 1)
      return parts[0];
   if (parts.size() == 2)
      return parts[0] + " and " + parts[1];

   std::string out;
   for (std::size_t i = 0; i < parts.size(); ++i)
   {
      if (i == parts.size() - 1)
         out += "and " + parts[i];
      else
         out += parts[i] + ", ";
   }
   return out;
}

} // anonymous namespace

std::string formatLoginTimeoutDuration(int minutes)
{
   if (minutes < 1)
      return "0 minutes";

   const int days = minutes / 1440;
   const int hours = (minutes % 1440) / 60;
   const int mins = minutes % 60;

   std::vector<std::string> parts;
   appendPart(parts, days, "day", "days");
   appendPart(parts, hours, "hour", "hours");
   appendPart(parts, mins, "minute", "minutes");

   return oxfordJoin(parts);
}

} // namespace server
} // namespace rstudio
