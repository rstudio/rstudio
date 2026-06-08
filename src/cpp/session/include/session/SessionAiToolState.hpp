/*
 * SessionAiToolState.hpp
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

#ifndef SESSION_AI_TOOL_STATE_HPP
#define SESSION_AI_TOOL_STATE_HPP

#include <set>
#include <string>
#include <vector>

namespace rstudio {
namespace session {

// Project-local directory (relative to the project root) used by the Posit
// Assistant to store its state. RStudio adds this to a project's ignore files
// (.gitignore, .Rbuildignore, svn:ignore) when it is present.
//
// We ignore the ".posit/assistant" subdirectory specifically rather than the
// whole ".posit" directory: ".posit" is shared with other Posit tools (e.g.
// the Posit Publisher extension writes ".posit/publisher", whose files are
// meant to be committed), so ".posit" itself must stay tracked.
constexpr const char* kPositAssistantStateDir = ".posit/assistant";

// Directory used by Posit Assistant releases prior to the ".posit/assistant"
// rename. Still recognized so that projects which already contain it continue
// to have it ignored.
constexpr const char* kPositAssistantStateDirLegacy = ".positai";

// All Posit Assistant project-state directory names RStudio recognizes, in
// preference order (current name first, then legacy). Paths are relative to
// the project root and use '/' as the separator.
inline std::vector<std::string> aiAssistantStateDirs()
{
   return { kPositAssistantStateDir, kPositAssistantStateDirLegacy };
}

// Project-relative directories that the file monitor must allow through (and
// whose creation should trigger an ignore-file update). This is each state
// directory plus, for any nested one, its ancestor directories -- the
// ancestors must be monitored so the creation of the nested directory is
// observed, even though the ancestors themselves are not ignored (e.g.
// ".posit" must be monitored to observe ".posit/assistant", but ".posit" is
// shared with the Posit Publisher extension and stays tracked).
inline std::vector<std::string> aiAssistantMonitorPaths()
{
   std::set<std::string> paths;
   for (const std::string& dir : aiAssistantStateDirs())
   {
      std::string prefix;
      for (char ch : dir)
      {
         if (ch == '/')
            paths.insert(prefix);
         prefix += ch;
      }
      paths.insert(prefix);
   }

   return std::vector<std::string>(paths.begin(), paths.end());
}

// Returns an anchored regular expression matching exactly the given directory
// path, suitable for use as an entry in .Rbuildignore (which interprets
// entries as regular expressions). Regex metacharacters in the path are
// escaped; the '/' path separator is left as-is.
inline std::string aiAssistantStateDirRegex(const std::string& dir)
{
   std::string escaped;
   for (char ch : dir)
   {
      if (ch == '.')
         escaped += "\\.";
      else
         escaped += ch;
   }

   return "^" + escaped + "$";
}

} // namespace session
} // namespace rstudio

#endif // SESSION_AI_TOOL_STATE_HPP
