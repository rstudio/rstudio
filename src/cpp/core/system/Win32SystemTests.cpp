/*
 * Win32SystemTests.cpp
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

#include <core/system/System.hpp>
#include <shared_core/FilePath.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

// Test fixture for process creation and cleanup
class Win32ProcessTest : public ::testing::Test
{
protected:
   STARTUPINFO si;
   PROCESS_INFORMATION pi;
   std::string cmd;
   std::vector<char> cmdBuf;

   void SetUp() override
   {
      ZeroMemory(&si, sizeof(si));
      si.cb = sizeof(si);
      ZeroMemory(&pi, sizeof(pi));
   }

   void PrepareCommand(const std::string& command)
   {
      cmd = command;
      cmdBuf.resize(cmd.size() + 1, '\0');
      cmd.copy(&(cmdBuf[0]), cmd.size());
   }

   void CleanupProcess()
   {
      if (pi.hProcess)
      {
         TerminateProcess(pi.hProcess, 1);
         WaitForSingleObject(pi.hProcess, INFINITE);
         CloseHandle(pi.hProcess);
         CloseHandle(pi.hThread);
      }
   }

   ~Win32ProcessTest()
   {
      CleanupProcess();
   }
};

TEST(Win32SystemTest, TestWin7OrLater)
{
   ASSERT_TRUE(isWin7OrLater());
}

TEST(Win32SystemTest, ExpandEmptyEnvironmentVariable)
{
   std::string orig;
   std::string result;
   Error err = expandEnvironmentVariables(orig, &result);
   ASSERT_FALSE(err);
   ASSERT_TRUE(result.empty());
}

TEST(Win32SystemTest, ExpandBogusEnvVariable)
{
   std::string orig = "%oncetherewasafakevariable374732%";
   std::string result;
   Error err = expandEnvironmentVariables(orig, &result);
   ASSERT_FALSE(err);
   ASSERT_EQ(orig, result);
}

TEST(Win32SystemTest, ExpandRealEnvironmentVariable)
{
   std::string first = "RoadOftenTravelled=";
   std::string orig = first + "%path%";
   std::string result;
   Error err = expandEnvironmentVariables(orig, &result);
   ASSERT_FALSE(err);
   ASSERT_TRUE(boost::algorithm::starts_with(result, first));
   // assume non-empty path, seems safe
   ASSERT_TRUE(result.length() > first.length());
}

TEST(Win32SystemTest, ComSpec)
{
   FilePath command = expandComSpec();
   ASSERT_FALSE(command.isEmpty());
   ASSERT_TRUE(command.exists());
}

TEST(Win32SystemTest, WindowsArchitectureBitnessAssumptions)
{
   std::string windir;
   Error err = expandEnvironmentVariables("%windir%", &windir);
   FilePath windirPath(windir);
   ASSERT_TRUE(windirPath.exists());

   core::FilePath sysWowPath(windir + "\\" + "syswow64");
   core::FilePath sys32Path(windir + "\\" + "system32");

   ASSERT_TRUE(sys32Path.exists());
   ASSERT_TRUE(sysWowPath.exists());
}

TEST_F(Win32ProcessTest, CorrectDetectionOfNoChildProcesses)
{
   PrepareCommand("ping -n 8 posit.co");

   // Start the child process.
   ASSERT_TRUE(CreateProcess(
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
   ASSERT_TRUE(children.empty());
}

TEST_F(Win32ProcessTest, CorrectDetectionOfChildProcesses)
{
   PrepareCommand("cmd.exe /S /C \"ping -n 8 posit.co\" 1> nul");

   // Start the child process.
   ASSERT_TRUE(CreateProcess(
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
   ASSERT_TRUE(children.size() >= 1);
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
      ASSERT_TRUE(found);
   }
}

TEST_F(Win32ProcessTest, DetermineCurrentWorkingDirectoryOfAnotherProcess)
{
   FilePath emptyPath;
   FilePath startingDir = FilePath::safeCurrentPath(emptyPath);

   PrepareCommand("cmd.exe /S /C \"ping -n 8 posit.co\" 1> nul");

   // Start the child process.
   ASSERT_TRUE(CreateProcess(
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
   ASSERT_TRUE(cwd.isEmpty());
}

TEST_F(Win32ProcessTest, EmptySubprocListWhenNoChildProcesses)
{
   PrepareCommand("ping -n 8 posit.co");

   // Start the child process.
   ASSERT_TRUE(CreateProcess(
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
   ASSERT_TRUE(children.empty());
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
