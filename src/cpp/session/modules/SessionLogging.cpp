/*
 * SessionLogging.cpp
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

#include "SessionLogging.hpp"

#include <map>

namespace rstudio {
namespace session {
namespace logging {

namespace {

std::map<std::string, int> s_stderrLogLevels;

} // anonymous namespace

int stderrLogLevel(const std::string& section)
{
   auto it = s_stderrLogLevels.find(section);
   if (it != s_stderrLogLevels.end())
      return it->second;
   return 0;
}

void setStderrLogLevel(const std::string& section, int level)
{
   if (level <= 0)
      s_stderrLogLevels.erase(section);
   else
      s_stderrLogLevels[section] = level;
}

} // namespace logging
} // namespace session
} // namespace rstudio
