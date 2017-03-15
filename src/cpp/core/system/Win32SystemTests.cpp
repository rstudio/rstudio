/*
 * Win32SystemTests.cpp
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

#include <core/system/System.hpp>
#include <core/FilePath.hpp>
#include <boost/algorithm/string/predicate.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

TEST_CASE("Win32SystemTests")
{
   SECTION("Test Vista or Later")
   {
      CHECK(isVistaOrLater());
   }

   SECTION("Test Win7 or Later")
   {
      if (isWin7OrLater())
      {
         CHECK(isVistaOrLater());
      }
   }

   SECTION("Expand Empty Environment Variable")
   {
      std::string orig;
      std::string result;
      Error err = expandEnvironmentVariables(orig, &result);
      CHECK(!err);
      CHECK(result.empty());
   }

   SECTION("Expand Bogus Env Variable")
   {
      std::string orig = "%oncetherewasafakevariable374732%";
      std::string result;
      Error err = expandEnvironmentVariables(orig, &result);
      CHECK(!err);
      CHECK_FALSE(result.compare(orig));
   }

   SECTION("Expand Real Environment Variable")
   {
      std::string first = "RoadOftenTravelled=";
      std::string orig = first + "%path%";
      std::string result;
      Error err = expandEnvironmentVariables(orig, &result);
      CHECK(!err);
      CHECK(boost::algorithm::starts_with(result, first));
      // assume non-empty path, seems safe
      CHECK(result.length() > first.length());
   }

   SECTION("ComSpec")
   {
      FilePath command = expandComSpec();
      CHECK_FALSE(command.empty());
      CHECK(command.exists());
   }

   SECTION("Windows architecture bitness assumptions")
   {
      std::string windir;
      Error err = expandEnvironmentVariables("%windir%", &windir);
      FilePath windirPath(windir);
      CHECK(windirPath.exists());

      core::FilePath sysWowPath(windir + "\\" + "syswow64");
      core::FilePath sysNativePath(windir + "\\" + "sysnative");
      core::FilePath sys32Path(windir + "\\" + "system32");

      CHECK(sys32Path.exists());

      if (!isWin64())
      {
         CHECK_FALSE(isCurrentProcessWin64());
         CHECK_FALSE(sysWowPath.exists());
         CHECK_FALSE(sysNativePath.exists());
      }
      else
      {
         if (isCurrentProcessWin64())
         {
            CHECK(sysWowPath.exists());
         }
         else
         {
            CHECK(sysNativePath.exists());
         }
      }
   }

   SECTION("Correct detection of no child processes")
   {
      STARTUPINFO si;
      PROCESS_INFORMATION pi;

      ZeroMemory(&si, sizeof(si));
      si.cb = sizeof(si);
      ZeroMemory(&pi, sizeof(pi));

      std::string cmd = "ping -n 8 www.rstudio.com";
      std::vector<char> cmdBuf(cmd.size() + 1, '\0');
      cmd.copy(&(cmdBuf[0]), cmd.size());

      // Start the child process.
      CHECK(CreateProcess(
               NULL,          // No module name (use command line)
               &(cmdBuf[0]),  // Command
               NULL,          // Process handle not inheritable
               NULL,          // Thread handle not inheritable
               FALSE,         // Set handle inheritance to FALSE
               0,             // No creation flags
               NULL,          // Use parent's environment block
               NULL,          // Use parent's starting directory
               &si,           // Pointer to STARTUPINFO structure
               &pi));         // Pointer to PROCESS_INFORMATION structure

      CHECK_FALSE(hasSubprocesses(pi.dwProcessId));

      CHECK(TerminateProcess(pi.hProcess, 1));

      WaitForSingleObject(pi.hProcess, INFINITE);
      CloseHandle(pi.hProcess);
      CloseHandle(pi.hThread);
   }

   SECTION("Correct detection of child processes")
   {
      STARTUPINFO si;
      PROCESS_INFORMATION pi;

      ZeroMemory(&si, sizeof(si));
      si.cb = sizeof(si);
      ZeroMemory(&pi, sizeof(pi));

      std::string cmd = "cmd.exe /S /C \"ping -n 8 www.rstudio.com\" 1> nul";
      std::vector<char> cmdBuf(cmd.size() + 1, '\0');
      cmd.copy(&(cmdBuf[0]), cmd.size());

      // Start the child process.
      CHECK(CreateProcess(
               NULL,          // No module name (use command line)
               &(cmdBuf[0]),  // Command
               NULL,          // Process handle not inheritable
               NULL,          // Thread handle not inheritable
               FALSE,         // Set handle inheritance to FALSE
               0,             // No creation flags
               NULL,          // Use parent's environment block
               NULL,          // Use parent's starting directory
               &si,           // Pointer to STARTUPINFO structure
               &pi));         // Pointer to PROCESS_INFORMATION structure

      ::Sleep(100); // give child time to start
      CHECK(hasSubprocesses(pi.dwProcessId));

      CHECK(TerminateProcess(pi.hProcess, 1));

      WaitForSingleObject(pi.hProcess, INFINITE);
      CloseHandle(pi.hProcess);
      CloseHandle(pi.hThread);
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
