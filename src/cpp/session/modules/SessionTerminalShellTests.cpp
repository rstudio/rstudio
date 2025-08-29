/*
 * SessionTerminalShellTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <session/SessionTerminalShell.hpp>

#include <core/system/System.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace console_process {
namespace tests {

using namespace console_process;

TEST(SessionTerminalShellTest, CanGenerateJsonArrayOfShells) 
{
   AvailableTerminalShells shells;
   size_t origCount = shells.count();
   EXPECT_GT(origCount, 0u);
   core::json::Array arr;
   shells.toJson(&arr);
   EXPECT_EQ(arr.getSize(), origCount);
}

#ifdef _WIN32

TEST(SessionTerminalShellTest, Windows64HasCommandPrompt64) 
{
   AvailableTerminalShells shells;
   EXPECT_GT(shells.count(), 0u);
   TerminalShell shell;
   EXPECT_TRUE(shells.getInfo(TerminalShell::ShellType::Cmd64, &shell));
   EXPECT_EQ(TerminalShell::ShellType::Cmd64, shell.type);
   EXPECT_TRUE(shell.path.exists());
}

TEST(SessionTerminalShellTest, Windows64HasPowershell64) 
{
   AvailableTerminalShells shells;
   EXPECT_GT(shells.count(), 0u);
   TerminalShell shell;
   EXPECT_TRUE(shells.getInfo(TerminalShell::ShellType::PS64, &shell));
   EXPECT_EQ(TerminalShell::ShellType::PS64, shell.type);
   EXPECT_TRUE(shell.path.exists());
}

TEST(SessionTerminalShellTest, WSLBashDetectedIfInstalled) 
{
   AvailableTerminalShells shells;
   EXPECT_GT(shells.count(), 0u);
   TerminalShell shell;
   if (shells.getInfo(TerminalShell::ShellType::WSLBash, &shell))
   {
   EXPECT_EQ(TerminalShell::ShellType::WSLBash, shell.type);
      EXPECT_TRUE(shell.path.exists());
   }
}

TEST(SessionTerminalShellTest, GitBashDetectedIfInstalled) 
{
   AvailableTerminalShells shells;
   TerminalShell shell;
   if (shells.getInfo(TerminalShell::ShellType::GitBash, &shell))
   {
      EXPECT_EQ(TerminalShell::ShellType::GitBash, shell.type);
      EXPECT_TRUE(shell.path.exists());
   }
}

// Only uncomment this test on a system known to have Windows Services
// for Linux installed.
#if 0
TEST(SessionTerminalShellTest, WSLBashIsDetected) 
{
   AvailableTerminalShells shells;
   TerminalShell shell;
   EXPECT_TRUE(shells.getInfo(TerminalShell::ShellType::WSLBash, &shell));
   EXPECT_TRUE(shell.type == TerminalShell::ShellType::WSLBash);
   EXPECT_TRUE(shell.path.exists());
}

TEST(SessionTerminalShellTest, NoPosixBashOnWindows) 
{
   AvailableTerminalShells shells;
   TerminalShell shell;
   EXPECT_FALSE(shells.getInfo(TerminalShell::ShellType::PosixBash, &shell));
}
#endif

#else // Posix

TEST(SessionTerminalShellTest, PosixBashFound) 
{
   AvailableTerminalShells shells;
   TerminalShell shell;
   EXPECT_TRUE(shells.getInfo(TerminalShell::ShellType::PosixBash, &shell));
   EXPECT_EQ(TerminalShell::ShellType::PosixBash, shell.type);
   EXPECT_TRUE(shell.path.exists());
   EXPECT_FALSE(shell.args.empty());
}

#endif

} // namespace tests
} // namespace console_process
} // namespace session
} // namespace rstudio