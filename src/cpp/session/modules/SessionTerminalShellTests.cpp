/*
 * SessionTerminalShellTests.cpp
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

#include <session/SessionTerminalShell.hpp>

#include <core/system/System.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace session {
namespace console_process {
namespace tests {

using namespace console_process;

context("session terminal shell tests")
{
#ifdef _WIN32

   test_that("Can generate json array of shells")
   {
      AvailableTerminalShells shells;
      size_t origCount = shells.count();
      expect_true(origCount > 0);
      core::json::Array arr;
      shells.toJson(&arr);
      expect_true(origCount == arr.size());
   }

   test_that("Windows has 32-bit command prompt")
   {
      AvailableTerminalShells shells;
      expect_true(shells.count() > 0);
      TerminalShell shell;
      expect_true(shells.getInfo(TerminalShell::Cmd32, &shell));
      expect_true(shell.type == TerminalShell::Cmd32);
      expect_true(shell.path.exists());
   }

   test_that("Windows has 32-bit powershell")
   {
      AvailableTerminalShells shells;
      expect_true(shells.count() > 0);
      TerminalShell shell;
      expect_true(shells.getInfo(TerminalShell::PS32, &shell));
      expect_true(shell.type == TerminalShell::PS32);
      expect_true(shell.path.exists());
   }

   test_that("64-bit Windows has 64-bit command prompt")
   {
      if (core::system::isWin64())
      {
         AvailableTerminalShells shells;
         expect_true(shells.count() > 0);
         TerminalShell shell;
         expect_true(shells.getInfo(TerminalShell::Cmd64, &shell));
         expect_true(shell.type == TerminalShell::Cmd64);
         expect_true(shell.path.exists());
      }
   }

   test_that("64-bit Windows has 64-bit powershell")
   {
      if (core::system::isWin64())
      {
         AvailableTerminalShells shells;
         expect_true(shells.count() > 0);
         TerminalShell shell;
         expect_true(shells.getInfo(TerminalShell::PS64, &shell));
         expect_true(shell.type == TerminalShell::PS64);
         expect_true(shell.path.exists());
      }
   }

   test_that("WSL Bash is detected if installed")
   {
         AvailableTerminalShells shells;
         expect_true(shells.count() > 0);
         TerminalShell shell;
         if (shells.getInfo(TerminalShell::WSLBash, &shell))
         {
            expect_true(shell.type == TerminalShell::WSLBash);
            expect_true(shell.path.exists());
         }
   }

   test_that("Git Bash is detected if installed")
   {
      AvailableTerminalShells shells;
      TerminalShell shell;
      if (shells.getInfo(TerminalShell::GitBash, &shell))
      {
         expect_true(shell.type == TerminalShell::GitBash);
         expect_true(shell.path.exists());
      }
   }

   // Only uncomment this test on a system known to have Windows Services
   // for Linux installed.
#if 0
   test_that("WSL Bash is detected")
   {
      AvailableTerminalShells shells;
      TerminalShell shell;
      expect_true(shells.getInfo(TerminalShell::WSLBash, &shell));
      expect_true(shell.type == TerminalShell::WSLBash);
      expect_true(shell.path.exists());
   }

   test_that("No Posix Bash on Windows")
   {
      AvailableTerminalShells shells;
      TerminalShell shell;
      expect_false(shells.getInfo(TerminalShell::PosixBash, &shell));
    }

#endif


#else // Posix
   test_that("One shell (bash) on Posix")
   {
      AvailableTerminalShells shells;
      TerminalShell shell;
      expect_true(shells.getInfo(TerminalShell::PosixBash, &shell));
      expect_true(shell.type == TerminalShell::PosixBash);
      expect_true(shell.path.exists());
      expect_true(shell.args.size() > 0);
   }

#endif
}

} // namespace tests
} // namespace console_process
} // namespace session
} // namespace rstudio
