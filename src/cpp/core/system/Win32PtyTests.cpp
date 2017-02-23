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

   SECTION("Start pty and get handles")
   {
      HANDLE hInWrite;
      HANDLE hOutRead;
      HANDLE hErrRead;

      pty.init("cmd.exe", args, options);
      Error err = pty.startPty(&hInWrite, &hOutRead, &hErrRead);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK_FALSE(hErrRead);
      CHECK(::CloseHandle(hInWrite));
      CHECK(::CloseHandle(hOutRead));
   }

   SECTION("Start pty and a process")
   {
      HANDLE hInWrite;
      HANDLE hOutRead;
      HANDLE hErrRead;

      FilePath cmd = expandComSpec();
      CHECK(cmd.exists());
      std::string exe = cmd.absolutePathNative();

      pty.init(exe, args, options);
      Error err = pty.startPty(&hInWrite, &hOutRead, &hErrRead);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK_FALSE(hErrRead);

      HANDLE hProcess;
      err = pty.runProcess(&hProcess);
      CHECK(!err);
      CHECK(hProcess);

      CHECK(::CloseHandle(hInWrite));
      CHECK(::CloseHandle(hOutRead));
   }

   SECTION("Start pty but fail to start process")
   {
      HANDLE hInWrite;
      HANDLE hOutRead;
      HANDLE hErrRead;

      pty.init("C:\\NoWindows\\system08\\huhcmd.exe", args, options);
      Error err = pty.startPty(&hInWrite, &hOutRead, &hErrRead);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK_FALSE(hErrRead);

      HANDLE hProcess;
      err = pty.runProcess(&hProcess);
      CHECK(err);
      CHECK_FALSE(hProcess);
   }

   SECTION("Capture output of a process")
   {
      CHECK(true);
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
