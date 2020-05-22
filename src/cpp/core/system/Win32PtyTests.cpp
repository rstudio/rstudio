/*
 * Win32PtyTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <gsl/gsl>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

#include <iostream>
#include <fstream>

#include <boost/algorithm/string/predicate.hpp>

#include <core/system/System.hpp>
#include <shared_core/FilePath.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>

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
            false /*connerr*/,
            options.cols,
            options.rows);

   FilePath cmd = expandComSpec();
   std::string cmdExe = cmd.getAbsolutePathNative();

   SECTION("Agent not running")
   {
      CHECK_FALSE(pty.ptyRunning());
   }

   SECTION("Finding winpty.dll")
   {
      CHECK(getWinPtyPath().exists());
   }

   SECTION("Start pty and a process")
   {
      HANDLE hInWrite = nullptr;
      HANDLE hOutRead = nullptr;
      HANDLE hErrRead = nullptr;
      HANDLE hProcess = nullptr;

      Error err = pty.start(cmdExe, emptyArgs, options,
                            &hInWrite, &hOutRead, &hErrRead, &hProcess);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK_FALSE(hErrRead);
      CHECK(hProcess);

      CHECK(::CloseHandle(hProcess));
      CHECK(::CloseHandle(hInWrite));
      CHECK(::CloseHandle(hOutRead));
   }

   SECTION("Start pty and a process with conerr")
   {
      HANDLE hInWrite = nullptr;
      HANDLE hOutRead = nullptr;
      HANDLE hErrRead = nullptr;
      HANDLE hProcess = nullptr;

      ProcessOptions conerrOptions = options;
      conerrOptions.pseudoterminal.get().conerr = true;
      Error err = pty.start(cmdExe, emptyArgs, conerrOptions,
                            &hInWrite, &hOutRead, &hErrRead, &hProcess);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK(hErrRead);
      CHECK(hProcess);

      CHECK(::CloseHandle(hProcess));
      CHECK(::CloseHandle(hInWrite));
      CHECK(::CloseHandle(hErrRead));
      CHECK(::CloseHandle(hOutRead));
   }

   SECTION("Start pty but fail to start process")
   {
      HANDLE hInWrite = nullptr;
      HANDLE hOutRead = nullptr;
      HANDLE hProcess = nullptr;

      Error err = pty.start(
               "C:\\NoWindows\\system08\\huhcmd.exe", emptyArgs, options,
               &hInWrite, &hOutRead, nullptr /*conerr*/, &hProcess);
      CHECK(err);
      CHECK_FALSE(hInWrite);
      CHECK_FALSE(hOutRead);
      CHECK_FALSE(hProcess);
   }

   SECTION("Capture output of a process")
   {
      HANDLE hInWrite = nullptr;
      HANDLE hOutRead = nullptr;
      HANDLE hProcess = nullptr;
      std::vector<std::string> args;

      args.push_back("/S");
      args.push_back("/C");
      args.push_back("\"echo Hello!\"");

      ProcessOptions plainOptions = options;
      plainOptions.pseudoterminal.get().plainText = true;
      Error err = pty.start(cmdExe, args, plainOptions,
                            &hInWrite, &hOutRead, nullptr /*conerr*/, &hProcess);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK(hProcess);

      // Obnoxious, but need to give child some time to spin up
      // and generate output.
      std::string stdOut;
      int tries = 10;
      while (tries && stdOut.empty())
      {
         ::Sleep(100);
         err = WinPty::readFromPty(hOutRead, &stdOut);
         CHECK(!err);
         tries--;
      }

      stdOut = string_utils::trimWhitespace(stdOut);
      CHECK_FALSE(stdOut.compare("Hello!"));
      CHECK(::CloseHandle(hProcess));
      CHECK(::CloseHandle(hInWrite));
      CHECK(::CloseHandle(hOutRead));
   }

   SECTION("Verify character-by-character send/receive")
   {
      HANDLE hInWrite = nullptr;
      HANDLE hOutRead = nullptr;
      HANDLE hProcess = nullptr;

      ProcessOptions plainOptions = options;

      // set simple prompt (>)
      core::system::Options shellEnv;
      core::system::setenv(&shellEnv, "PROMPT", "$G");
      plainOptions.environment = shellEnv;

      plainOptions.pseudoterminal.get().plainText = true;
      Error err = pty.start(cmdExe, emptyArgs, plainOptions,
                            &hInWrite, &hOutRead, nullptr /*conerr*/, &hProcess);
      CHECK(!err);
      CHECK(hInWrite);
      CHECK(hOutRead);
      CHECK(hProcess);

      // Obnoxious, but need to give child some time to spin up
      // and get to prompt.
      std::string stdOut;
      int tries = 10;
      while (tries)
      {
         ::Sleep(100);
         err = WinPty::readFromPty(hOutRead, &stdOut);
         CHECK(!err);
         tries--;
         if (stdOut.find('>') != std::string::npos)
            break;
      }

      stdOut.clear();
      std::string line1 = "echo This was once a place where words and letters and 32 numbers lived!";

      // console will word-wrap unless we set a large size
      err = pty.setSize(gsl::narrow_cast<int>(line1.length()) * 4, kRows);
      CHECK(!err);

      for (size_t i = 0; i < line1.length(); i++)
      {
         std::string typeThis;
         typeThis.push_back(line1[i]);
         err = WinPty::writeToPty(hInWrite,typeThis);
         CHECK(!err);
         ::Sleep(25);
         err = WinPty::readFromPty(hOutRead, &stdOut);
         CHECK(!err);
      }

      // try get all output
      tries = 10;
      while (tries && stdOut.length() < line1.length())
      {
         ::Sleep(100);
         err = WinPty::readFromPty(hOutRead, &stdOut);
         CHECK(!err);
         tries--;
      }

      CHECK_FALSE(stdOut.compare(line1));
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
