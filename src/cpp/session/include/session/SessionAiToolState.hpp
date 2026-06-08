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

#include <string>
#include <vector>

namespace rstudio {
namespace session {

// Project-local directory used by the Posit Assistant to store its state.
//
// When this directory is present in a project, RStudio adds it to the
// project's ignore files (.gitignore, .Rbuildignore, svn:ignore). The
// Assistant stores its state under ".posit/assistant"; for now we ignore the
// parent ".posit" directory rather than that specific subdirectory.
//
// To ignore the more specific subdirectory instead, change this to
// ".posit/assistant". Note, however, that the file-monitor logic which
// detects mid-session creation (in SessionProjectContext, SessionGit, and
// SessionSVN) assumes a single path component at the project root, and that
// svn:ignore matches only immediate children of the directory it is set on --
// both would need revisiting before a nested path could be used here.
constexpr const char* kPositAssistantStateDir = ".posit";

// Directory used by Posit Assistant releases prior to the ".posit" rename.
// Still recognized so that projects which already contain it continue to have
// it ignored.
constexpr const char* kPositAssistantStateDirLegacy = ".positai";

// All Posit Assistant project-state directory names RStudio recognizes, in
// preference order (current name first, then legacy).
inline std::vector<std::string> aiAssistantStateDirs()
{
   return { kPositAssistantStateDir, kPositAssistantStateDirLegacy };
}

// Returns an anchored regular expression matching exactly the given directory
// name, suitable for use as an entry in .Rbuildignore or svn:ignore (both of
// which interpret entries as regular expressions). Regex metacharacters in
// the name are escaped.
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
