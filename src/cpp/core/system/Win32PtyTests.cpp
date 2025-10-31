/*
 * Win32PtyTests.cpp
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

#ifdef _WIN32

#include "Win32Pty.hpp"

#include <gsl/gsl-lite.hpp>

#include <gtest/gtest.h>

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

// Test fixture for Win32Pty tests
class Win32PtyTest : public ::testing::Test
{
protected:
   WinPty pty;
   const int kCols = 80;
   const int kRows = 25;
   std::vector<std::string> emptyArgs;
   ProcessOptions options;
   std::string cmdExe;

   void SetUp() override
   {
      // Setup process options with pseudoterminal
      options.cols = kCols;
      options.rows = kRows;
      options.pseudoterminal = core::system::Pseudoterminal(
               getWinPtyPath(),
               false /*plainText*/,
               false /*connerr*/,
               options.cols,
               options.rows);

      // Get command shell path
      FilePath cmd = expandComSpec();
      cmdExe = cmd.getAbsolutePathNative();
      
      // Verify pty is valid
      ASSERT_FALSE(pty.ptyRunning());
      ASSERT_TRUE(getWinPtyPath().exists());
   }
   
   void CloseHandles(HANDLE hProcess, HANDLE hInWrite, HANDLE hOutRead, HANDLE hErrRead = nullptr)
   {
      if (hProcess) ASSERT_TRUE(::CloseHandle(hProcess));
      if (hInWrite) ASSERT_TRUE(::CloseHandle(hInWrite));
      if (hOutRead) ASSERT_TRUE(::CloseHandle(hOutRead));
      if (hErrRead) ASSERT_TRUE(::CloseHandle(hErrRead));
   }
};

TEST_F(Win32PtyTest, StartPtyAndProcess)
{
   HANDLE hInWrite = nullptr;
   HANDLE hOutRead = nullptr;
   HANDLE hErrRead = nullptr;
   HANDLE hProcess = nullptr;

   Error err = pty.start(cmdExe, emptyArgs, options,
                      &hInWrite, &hOutRead, &hErrRead, &hProcess);
   ASSERT_FALSE(err);
   ASSERT_TRUE(hInWrite);
   ASSERT_TRUE(hOutRead);
   ASSERT_FALSE(hErrRead);
   ASSERT_TRUE(hProcess);

   CloseHandles(hProcess, hInWrite, hOutRead);
}

TEST_F(Win32PtyTest, StartPtyWithConerr)
{
   HANDLE hInWrite = nullptr;
   HANDLE hOutRead = nullptr;
   HANDLE hErrRead = nullptr;
   HANDLE hProcess = nullptr;

   ProcessOptions conerrOptions = options;
   conerrOptions.pseudoterminal.get().conerr = true;
   Error err = pty.start(cmdExe, emptyArgs, conerrOptions,
                      &hInWrite, &hOutRead, &hErrRead, &hProcess);
   ASSERT_FALSE(err);
   ASSERT_TRUE(hInWrite);
   ASSERT_TRUE(hOutRead);
   ASSERT_TRUE(hErrRead);
   ASSERT_TRUE(hProcess);

   CloseHandles(hProcess, hInWrite, hOutRead, hErrRead);
}

TEST_F(Win32PtyTest, StartPtyButFailToStartProcess)
{
   HANDLE hInWrite = nullptr;
   HANDLE hOutRead = nullptr;
   HANDLE hProcess = nullptr;

   Error err = pty.start(
            "C:\\NoWindows\\system08\\huhcmd.exe", emptyArgs, options,
            &hInWrite, &hOutRead, nullptr /*conerr*/, &hProcess);
   ASSERT_TRUE(err);
   ASSERT_FALSE(hInWrite);
   ASSERT_FALSE(hOutRead);
   ASSERT_FALSE(hProcess);
}

TEST_F(Win32PtyTest, CaptureOutputOfProcess)
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
   ASSERT_FALSE(err);
   ASSERT_TRUE(hInWrite);
   ASSERT_TRUE(hOutRead);
   ASSERT_TRUE(hProcess);

   // Obnoxious, but need to give child some time to spin up
   // and generate output.
   std::string stdOut;
   int tries = 10;
   while (tries && stdOut.empty())
   {
      ::Sleep(100);
      err = WinPty::readFromPty(hOutRead, &stdOut);
      ASSERT_FALSE(err);
      tries--;
   }

   stdOut = string_utils::trimWhitespace(stdOut);
   ASSERT_EQ(std::string("Hello!"), stdOut);
   
   CloseHandles(hProcess, hInWrite, hOutRead);
}

TEST_F(Win32PtyTest, VerifyCharacterByCharacterSendReceive)
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
   ASSERT_FALSE(err);
   ASSERT_TRUE(hInWrite);
   ASSERT_TRUE(hOutRead);
   ASSERT_TRUE(hProcess);

   // Obnoxious, but need to give child some time to spin up
   // and get to prompt.
   std::string stdOut;
   int tries = 10;
   while (tries)
   {
      ::Sleep(100);
      err = WinPty::readFromPty(hOutRead, &stdOut);
      ASSERT_FALSE(err);
      tries--;
      if (stdOut.find('>') != std::string::npos)
         break;
   }

   stdOut.clear();
   std::string line1 = "echo This was once a place where words and letters and 32 numbers lived!";

   // console will word-wrap unless we set a large size
   err = pty.setSize(gsl::narrow_cast<int>(line1.length()) * 4, kRows);
   ASSERT_FALSE(err);

   for (size_t i = 0; i < line1.length(); i++)
   {
      std::string typeThis;
      typeThis.push_back(line1[i]);
      err = WinPty::writeToPty(hInWrite,typeThis);
      ASSERT_FALSE(err);
      ::Sleep(25);
      err = WinPty::readFromPty(hOutRead, &stdOut);
      ASSERT_FALSE(err);
   }

   // try get all output
   tries = 10;
   while (tries && stdOut.length() < line1.length())
   {
      ::Sleep(100);
      err = WinPty::readFromPty(hOutRead, &stdOut);
      ASSERT_FALSE(err);
      tries--;
   }

   ASSERT_EQ(line1, stdOut);
   
   CloseHandles(hProcess, hInWrite, hOutRead);
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
