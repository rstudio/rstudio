/*
 * Win32SystemTests.cpp
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

#include <core/system/System.hpp>
#include <shared_core/FilePath.hpp>
#include <boost/algorithm/string/predicate.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

TEST_CASE("Win32SystemTests")
{
   SECTION("Test Win7 or Later")
   {
      CHECK(isWin7OrLater());
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
      CHECK_FALSE(command.isEmpty());
      CHECK(command.exists());
   }

   SECTION("Windows architecture bitness assumptions")
   {
      std::string windir;
      Error err = expandEnvironmentVariables("%windir%", &windir);
      FilePath windirPath(windir);
      CHECK(windirPath.exists());

      core::FilePath sysWowPath(windir + "\\" + "syswow64");
      core::FilePath sys32Path(windir + "\\" + "system32");

      CHECK(sys32Path.exists());
      CHECK(sysWowPath.exists());
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
               nullptr,       // No module name (use command line)
               &(cmdBuf[0]),  // Command
               nullptr,       // Process handle not inheritable
               nullptr,       // Thread handle not inheritable
               FALSE,         // Set handle inheritance to FALSE
               0,             // No creation flags
               nullptr,       // Use parent's environment block
               nullptr,       // Use parent's starting directory
               &si,           // Pointer to STARTUPINFO structure
               &pi));         // Pointer to PROCESS_INFORMATION structure

      std::vector<SubprocInfo> children = getSubprocesses(pi.dwProcessId);
      CHECK(children.empty());

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
               nullptr,       // No module name (use command line)
               &(cmdBuf[0]),  // Command
               nullptr,       // Process handle not inheritable
               nullptr,       // Thread handle not inheritable
               FALSE,         // Set handle inheritance to FALSE
               0,             // No creation flags
               nullptr,       // Use parent's environment block
               nullptr,       // Use parent's starting directory
               &si,           // Pointer to STARTUPINFO structure
               &pi));         // Pointer to PROCESS_INFORMATION structure

      ::Sleep(100); // give child time to start

      std::string exe = "PING.EXE";
      std::vector<SubprocInfo> children = getSubprocesses(pi.dwProcessId);
      CHECK(children.size() >= 1);
      if (children.size() >= 1)
      {
         bool found = false;
         for (SubprocInfo info : children)
         {
            if (info.exe.compare(exe) == 0)
            {
               found = true;
               break;
            }
         }
         CHECK(found);
      }

      CHECK(TerminateProcess(pi.hProcess, 1));

      WaitForSingleObject(pi.hProcess, INFINITE);
      CloseHandle(pi.hProcess);
      CloseHandle(pi.hThread);
   }

   SECTION("Determine current-working-directory of another process")
   {
      FilePath emptyPath;
      FilePath startingDir = FilePath::safeCurrentPath(emptyPath);

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
               nullptr,       // No module name (use command line)
               &(cmdBuf[0]),  // Command
               nullptr,       // Process handle not inheritable
               nullptr,       // Thread handle not inheritable
               FALSE,         // Set handle inheritance to FALSE
               0,             // No creation flags
               nullptr,       // Use parent's environment block
               nullptr,       // Use parent's starting directory
               &si,           // Pointer to STARTUPINFO structure
               &pi));         // Pointer to PROCESS_INFORMATION structure

      ::Sleep(100); // give child time to start

      FilePath cwd = currentWorkingDir(pi.dwProcessId);

      // API is not implemented on Windows and should always return an empty
      // FilePath. See currentWorkingDir in Win32System.cpp for more info.
      CHECK(cwd.isEmpty());

      TerminateProcess(pi.hProcess, 1);
      WaitForSingleObject(pi.hProcess, INFINITE);
      CloseHandle(pi.hProcess);
      CloseHandle(pi.hThread);
   }

   SECTION("Empty subproc list when no child processes")
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
               nullptr,       // No module name (use command line)
               &(cmdBuf[0]),  // Command
               nullptr,       // Process handle not inheritable
               nullptr,       // Thread handle not inheritable
               FALSE,         // Set handle inheritance to FALSE
               0,             // No creation flags
               nullptr,       // Use parent's environment block
               nullptr,       // Use parent's starting directory
               &si,           // Pointer to STARTUPINFO structure
               &pi));         // Pointer to PROCESS_INFORMATION structure

      std::vector<SubprocInfo> children = getSubprocesses(pi.dwProcessId);
      CHECK(children.empty());

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
