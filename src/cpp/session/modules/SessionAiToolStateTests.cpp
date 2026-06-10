/*
 * SessionAiToolStateTests.cpp
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

#include <session/SessionAiToolState.hpp>

#include <algorithm>

#include <boost/regex.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace tests {

TEST(SessionAiToolState, StateDirsCurrentBeforeLegacy)
{
   std::vector<std::string> expected = { ".posit/assistant", ".positai" };
   EXPECT_EQ(aiAssistantStateDirs(), expected);
}

TEST(SessionAiToolState, MonitorPathsIncludeNestedAncestors)
{
   // each state dir plus, for the nested ".posit/assistant", its parent
   // ".posit" -- the parent must be monitored so the nested dir's creation is
   // observed. Returned sorted (backed by std::set).
   std::vector<std::string> expected = { ".posit", ".posit/assistant", ".positai" };
   EXPECT_EQ(aiAssistantMonitorPaths(), expected);
}

TEST(SessionAiToolState, MonitorPathsHaveNoEmptyEntry)
{
   std::vector<std::string> paths = aiAssistantMonitorPaths();
   EXPECT_EQ(std::find(paths.begin(), paths.end(), std::string()), paths.end());
}

TEST(SessionAiToolState, RegexEscapesDotsKeepsSlash)
{
   // '.' is a regex metacharacter and must be escaped; '/' is a literal path
   // separator and must be left as-is. These strings are written verbatim into
   // .Rbuildignore and used as svn:ignore presence checks.
   EXPECT_EQ(aiAssistantStateDirRegex(".posit/assistant"), "^\\.posit/assistant$");
   EXPECT_EQ(aiAssistantStateDirRegex(".positai"), "^\\.positai$");
   EXPECT_EQ(aiAssistantStateDirRegex("assistant"), "^assistant$");
}

TEST(SessionAiToolState, RegexAnchoredSoPrefixDoesNotCollide)
{
   // ".posit/assistant" and ".positai" share the ".posit" prefix; the anchors
   // must keep their regexes from matching one another.
   boost::regex assistant(aiAssistantStateDirRegex(".posit/assistant"));
   boost::regex positai(aiAssistantStateDirRegex(".positai"));

   EXPECT_TRUE(boost::regex_search(std::string(".posit/assistant"), assistant));
   EXPECT_FALSE(boost::regex_search(std::string(".positai"), assistant));
   EXPECT_TRUE(boost::regex_search(std::string(".positai"), positai));
   EXPECT_FALSE(boost::regex_search(std::string(".posit/assistant"), positai));
}

} // namespace tests
} // namespace session
} // namespace rstudio
