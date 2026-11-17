/*
 * SessionConsoleInputTests.cpp
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

#include "SessionConsoleInput.hpp"

#include <string>
#include <vector>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace console_input {
namespace tests {

TEST(ConsoleInputLanguageTest, ReplEntryAndExitAreFollowed)
{
   EXPECT_EQ("Python", languageAfterInput("R", "reticulate::repl_python()"));
   EXPECT_EQ("Python", languageAfterInput("R", "repl_python()"));
   EXPECT_EQ("Julia", languageAfterInput("R", ".rs.julia.repl()"));

   for (const char* exit : { "quit", "exit", "quit()", "exit()" })
   {
      EXPECT_EQ("R", languageAfterInput("Python", exit));
      EXPECT_EQ("R", languageAfterInput("Julia", exit));
      EXPECT_EQ("R", languageAfterInput("R", exit));
   }

   EXPECT_EQ("R", languageAfterInput("R", "1 + 1"));
   EXPECT_EQ("Julia", languageAfterInput("Julia", "1 + 1"));
}

TEST(ConsoleInputLanguageTest, ReplEntryIsOnlyRecognizedInR)
{
   // inside a REPL, the other's entry is that REPL's own input
   EXPECT_EQ("Python", languageAfterInput("Python", ".rs.julia.repl()"));
   EXPECT_EQ("Julia", languageAfterInput("Julia", "reticulate::repl_python()"));
}

TEST(ConsoleInputLanguageTest, InputIsFollowedLineByLine)
{
   EXPECT_EQ("Julia", languageAfterInput("Python", "quit\n.rs.julia.repl()"));
   EXPECT_EQ("R", languageAfterInput("R", ".rs.julia.repl()\nx = 1\nexit()"));
   EXPECT_EQ("Python", languageAfterInput("Julia", "exit\nrepl_python()\nx = 1"));
}

TEST(ConsoleInputLanguageTest, SwitchInputGoesByWayOfR)
{
   EXPECT_EQ("", languageSwitchInput("R", "R"));
   EXPECT_EQ("", languageSwitchInput("Julia", "Julia"));
   EXPECT_EQ("reticulate::repl_python()", languageSwitchInput("R", "Python"));
   EXPECT_EQ(".rs.julia.repl()", languageSwitchInput("R", "Julia"));
   EXPECT_EQ("quit", languageSwitchInput("Python", "R"));
   EXPECT_EQ("quit", languageSwitchInput("Julia", "R"));
   EXPECT_EQ("quit\n.rs.julia.repl()", languageSwitchInput("Python", "Julia"));
   EXPECT_EQ("quit\nreticulate::repl_python()", languageSwitchInput("Julia", "Python"));

   // no language the console has
   EXPECT_EQ("", languageSwitchInput("R", "Stan"));
   EXPECT_EQ("", languageSwitchInput("Julia", "Stan"));
}

TEST(ConsoleInputLanguageTest, SwitchInputReachesItsTarget)
{
   std::vector<std::string> languages = { "R", "Python", "Julia" };
   for (const std::string& from : languages)
   {
      for (const std::string& to : languages)
         EXPECT_EQ(to, languageAfterInput(from, languageSwitchInput(from, to))) << from << " -> " << to;
   }
}

} // namespace tests
} // namespace console_input
} // namespace session
} // namespace rstudio
