/*
 * Win32PtyTests.cpp
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

#ifdef _WIN32

#include "Win32Pty.hpp"

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

TEST_CASE("Win32PtyTests")
{
   WinPty pty;

   const int kCols = 80;
   const int kRows = 25;

   std::vector <std::string> args;
   ProcessOptions options;
   options.cols = 80;
   options.rows = 25;
   options.pseudoterminal = core::system::Pseudoterminal(options.cols,
                                                         options.rows);

   SECTION("Agent not running")
   {
      CHECK_FALSE(pty.ptyRunning());
   }

   SECTION("Initialize")
   {
      CHECK_FALSE(pty.ptyRunning());
      pty.init("cmd.exe", args, options);
      CHECK_FALSE(pty.ptyRunning());
   }

   SECTION("Start pty")
   {
      HANDLE hInWrite;
      HANDLE hOutRead;
      HANDLE hErrRead;

      pty.init("cmd.exe", args, options);
      Error err = pty.startPty(&hInWrite, &hOutRead, &hErrRead);
      CHECK(!err);
      CHECK_FALSE(hInWrite == INVALID_HANDLE_VALUE);
      CHECK_FALSE(hOutRead == INVALID_HANDLE_VALUE);
      CHECK_FALSE(hErrRead == INVALID_HANDLE_VALUE);
      ::CloseHandle(hInWrite);
      ::CloseHandle(hOutRead);
      ::CloseHandle(hErrRead);
   }

   }

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
