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

#include <iostream>
#include <fstream>

#include <boost/algorithm/string/predicate.hpp>

#include <core/system/System.hpp>
#include <core/FilePath.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

namespace {

std::string ptyPath;

FilePath getWinPtyPath()
{
   // Find the rdesktop-dev.conf, so we can find the external-winpty-path
   // which gives us the absolute path to the winpty location. This assumes
   // we are running the unit test from the root of the CPP build folder.
   if (!ptyPath.empty())
      return FilePath(ptyPath);

#ifdef _WIN64
   std::string suffix("/64/bin/winpty.dll");
#else
   std::string suffix("/32/bin/winpty.dll");
#endif
   std::string prefix("external-winpty-path=");
   std::ifstream in("./conf/rdesktop-dev.conf");

   std::string line;
   while (std::getline(in, line))
   {
      if (boost::algorithm::starts_with(line, prefix))
      {
         ptyPath = line.substr(prefix.length());
         ptyPath += suffix;
         return FilePath(ptyPath);
      }
   }
   return FilePath();
}

Error readPipe(HANDLE hPipe, std::string* pOutput)
{
   DWORD dwAvail = 0;
   if (!::PeekNamedPipe(hPipe, NULL, 0, NULL, &dwAvail, NULL))
   {
      if (::GetLastError() == ERROR_BROKEN_PIPE)
         return Success();
      else
         return systemError(::GetLastError(), ERROR_LOCATION);
   }

   // no data available
   if (dwAvail == 0)
      return Success();

   // read data which is available
   DWORD nBytesRead;
   std::vector<CHAR> buffer(dwAvail, 0);
   if (!::ReadFile(hPipe, &(buffer[0]), dwAvail, &nBytesRead, NULL))
      return systemError(::GetLastError(), ERROR_LOCATION);

   // append to output
   pOutput->append(&(buffer[0]), nBytesRead);

   // success
   return Success();
}

} // anonymous namespace

TEST_CASE("Win32PtyTests")
{
   WinPty pty;

   const int kCols = 80;
   const int kRows = 25;

   std::vector <std::string> emptyArgs;

   ProcessOptions options;
   options.cols = kCols;
   options.rows = kRows;
   options.pseudoterminal = core::system::Pseudoterminal(
            getWinPtyPath(),
            false /*plainText*/,
            options.cols,
            options.rows);

   ProcessOptions plainOptions;
   plainOptions.cols = kCols;
   plainOptions.rows = kRows;
   plainOptions.pseudoterminal = core::system::Pseudoterminal(
            getWinPtyPath(),
            true /*plainText*/,
            plainOptions.cols,
            plainOptions.rows);

   SECTION("Agent not running")
   {
      CHECK_FALSE(pty.ptyRunning());
   }

   SECTION("Initialize")
   {
      CHECK_FALSE(pty.ptyRunning());
      pty.init("cmd.exe", emptyArgs, options);
      CHECK_FALSE(pty.ptyRunning());
   }

   SECTION("Finding winpty.dll")
   {
      CHECK(getWinPtyPath().exists());
   }

   SECTION("Start pty and get handles")
   {
      HANDLE hInWrite;
      HANDLE hOutRead;
      HANDLE hErrRead;

      pty.init("cmd.exe", emptyArgs, options);
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

      pty.init(exe, emptyArgs, options);
      Error err = pty.startPty(&hInWrite, &hOutRead, &hErrRead);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK_FALSE(hErrRead);

      HANDLE hProcess;
      err = pty.runProcess(&hProcess);
      CHECK(!err);
      CHECK(hProcess);

      CHECK(::CloseHandle(hProcess));
      CHECK(::CloseHandle(hInWrite));
      CHECK(::CloseHandle(hOutRead));
   }

   SECTION("Start pty but fail to start process")
   {
      HANDLE hInWrite;
      HANDLE hOutRead;
      HANDLE hErrRead;

      pty.init("C:\\NoWindows\\system08\\huhcmd.exe", emptyArgs, options);
      Error err = pty.startPty(&hInWrite, &hOutRead, &hErrRead);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK_FALSE(hErrRead);

      HANDLE hProcess;
      err = pty.runProcess(&hProcess);
      CHECK(err);
      CHECK_FALSE(hProcess);
      CHECK(::CloseHandle(hInWrite));
      CHECK(::CloseHandle(hOutRead));
   }

   SECTION("Capture output of a process")
   {
      HANDLE hInWrite;
      HANDLE hOutRead;
      HANDLE hErrRead;
      std::vector<std::string> args;

      FilePath cmd = expandComSpec();
      CHECK(cmd.exists());
      std::string exe = cmd.absolutePathNative();

      args.push_back("/S");
      args.push_back("/C");
      args.push_back("\"echo Hello!\"");

      pty.init(exe, args, plainOptions);
      Error err = pty.startPty(&hInWrite, &hOutRead, &hErrRead);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK_FALSE(hErrRead);

      HANDLE hProcess;
      err = pty.runProcess(&hProcess);
      CHECK(!err);
      CHECK(hProcess);

      // Obnoxious, but need to give child some time to spin up
      // and generate output.
      std::string stdOut;
      int tries = 10;
      while (tries && stdOut.empty())
      {
         ::Sleep(100);
         err = readPipe(hOutRead, &stdOut);
         CHECK(!err);
         tries--;
      }

      stdOut = string_utils::trimWhitespace(stdOut);
      CHECK_FALSE(stdOut.compare("Hello!"));
      CHECK(::CloseHandle(hProcess));
      CHECK(::CloseHandle(hInWrite));
      CHECK(::CloseHandle(hOutRead));
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
