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

#include <sstream>

#include <core/FileUtils.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/System.hpp>
#include <shared_core/FilePath.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

// Test fixture for process creation and cleanup.
//
// The process runs inside a job object that kills everything in it when the
// job handle closes, so a test tears down its whole process tree. Terminating
// only the process we created is not enough: the tests that run cmd.exe leave
// its ping.exe grandchild alive, and an orphan goes on reporting the pid of
// its terminated parent as th32ParentProcessID. Windows recycles pids, so a
// later test that creates a process and asks getSubprocesses() for its
// children can be handed one of those orphans.
class Win32ProcessTest : public ::testing::Test
{
protected:
   STARTUPINFO si;
   PROCESS_INFORMATION pi;
   HANDLE hJob;

   void SetUp() override
   {
      ZeroMemory(&si, sizeof(si));
      si.cb = sizeof(si);
      ZeroMemory(&pi, sizeof(pi));
      hJob = nullptr;
   }

   // Runs command, leaving the process running on success. Started suspended
   // so that nothing can be spawned before the job is in place.
   bool StartProcess(const std::string& command)
   {
      std::vector<char> cmdBuf(command.begin(), command.end());
      cmdBuf.push_back('\0');

      hJob = ::CreateJobObject(nullptr, nullptr);
      if (hJob == nullptr)
         return false;

      JOBOBJECT_EXTENDED_LIMIT_INFORMATION limits;
      ZeroMemory(&limits, sizeof(limits));
      limits.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
      if (!::SetInformationJobObject(hJob,
                                     JobObjectExtendedLimitInformation,
                                     &limits,
                                     sizeof(limits)))
      {
         return false;
      }

      if (!::CreateProcess(
             nullptr,          // No module name (use command line)
             &(cmdBuf[0]),     // Command
             nullptr,          // Process handle not inheritable
             nullptr,          // Thread handle not inheritable
             FALSE,            // Set handle inheritance to FALSE
             CREATE_SUSPENDED, // Resumed once it is in the job
             nullptr,          // Use parent's environment block
             nullptr,          // Use parent's starting directory
             &si,              // Pointer to STARTUPINFO structure
             &pi))             // Pointer to PROCESS_INFORMATION structure
      {
         return false;
      }

      if (!::AssignProcessToJobObject(hJob, pi.hProcess))
         return false;

      return ::ResumeThread(pi.hThread) != static_cast<DWORD>(-1);
   }

   void CleanupProcess()
   {
      // takes the process and everything it spawned with it
      if (hJob != nullptr)
      {
         ::CloseHandle(hJob);
         hJob = nullptr;
      }

      if (pi.hProcess)
      {
         // the process is outside the job, and so still running, if it was
         // created but never assigned; without this the wait below hangs
         ::TerminateProcess(pi.hProcess, 1);
         ::WaitForSingleObject(pi.hProcess, INFINITE);
         ::CloseHandle(pi.hProcess);
         ::CloseHandle(pi.hThread);
         ZeroMemory(&pi, sizeof(pi));
      }
   }

   ~Win32ProcessTest()
   {
      CleanupProcess();
   }
};

// "Actual: false" says nothing about which process was mistaken for a child,
// and these failures are rare enough that a rerun will not show you.
static std::string Describe(const std::vector<SubprocInfo>& subprocs)
{
   std::ostringstream os;
   for (const SubprocInfo& info : subprocs)
      os << " " << info.exe << "(" << info.pid << ")";
   return os.str();
}

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

TEST(Win32SystemTest, ExecutablePathIsResolved)
{
   // a smoke test only: the test binary's own path is far short of MAX_PATH, so
   // the grow-and-retry loop never iterates here
   FilePath exePath;
   Error err = executablePath(nullptr, &exePath);
   ASSERT_FALSE(err);
   ASSERT_FALSE(exePath.isEmpty());
   ASSERT_TRUE(exePath.isRegularFile());
}

TEST(Win32SystemTest, FindProgramOnPath)
{
   FilePath cmdPath;
   Error err = findProgramOnPath("cmd.exe", &cmdPath);
   ASSERT_FALSE(err);
   ASSERT_TRUE(cmdPath.isRegularFile());
   ASSERT_TRUE(boost::algorithm::iequals(cmdPath.getFilename(), "cmd.exe"));
}

TEST(Win32SystemTest, FindProgramOnPathProbesExtensions)
{
   // no extension supplied, so .exe should be probed
   FilePath cmdPath;
   Error err = findProgramOnPath("cmd", &cmdPath);
   ASSERT_FALSE(err);
   ASSERT_TRUE(boost::algorithm::iequals(cmdPath.getFilename(), "cmd.exe"));
}

TEST(Win32SystemTest, FindProgramOnPathMissing)
{
   FilePath programPath;
   ASSERT_TRUE(findProgramOnPath("oncetherewasafakeprogram374732.exe", &programPath));
   ASSERT_TRUE(findProgramOnPath("", &programPath));
}

TEST(Win32SystemTest, FindProgramOnPathQualified)
{
   FilePath cmdPath;
   ASSERT_FALSE(findProgramOnPath("cmd.exe", &cmdPath));

   // an already-qualified program isn't a PATH search, but should still resolve
   FilePath qualified;
   ASSERT_FALSE(findProgramOnPath(cmdPath.getAbsolutePath(), &qualified));
   ASSERT_TRUE(qualified == cmdPath);

   ASSERT_TRUE(findProgramOnPath(cmdPath.getAbsolutePath() + "-nope", &qualified));

   // a drive-relative name is rooted as far as FilePath is concerned; it must not
   // be joined onto each PATH entry
   ASSERT_TRUE(findProgramOnPath("C:oncetherewasafakeprogram374732.exe", &qualified));
}

TEST(Win32SystemTest, FindProgramOnPathQualifiedProbesExtensions)
{
   FilePath cmdPath;
   ASSERT_FALSE(findProgramOnPath("cmd.exe", &cmdPath));

   // a qualified name without an extension gets the same probing a bare name does,
   // so dropping the ".exe" here should still resolve
   std::string withoutExt = cmdPath.getParent().completeChildPath("cmd").getAbsolutePath();

   FilePath qualified;
   ASSERT_FALSE(findProgramOnPath(withoutExt, &qualified));
   ASSERT_TRUE(boost::algorithm::iequals(qualified.getFilename(), "cmd.exe"));
}

TEST(Win32SystemTest, FindProgramOnPathProbesExtensionsUnderDottedDirectories)
{
   // The extension of a qualified name has to be read from its last component. Taken over
   // the whole path, a dotted *directory* reads as an extension and suppresses the probing
   // above for a name that has none -- and dotted directories are the norm on Windows
   // ("C:/R/R-4.4.1/bin", "C:/Users/first.last"). The System32 case cannot catch this,
   // because none of its components contain a dot.
   const std::string kProbeProgram = "rs-probe-374732";

   FilePath probeDir;
   ASSERT_FALSE(FilePath::tempFilePath(probeDir));

   FilePath dottedDir = probeDir.completeChildPath("R-4.4.1").completeChildPath("bin");
   ASSERT_FALSE(dottedDir.ensureDirectory());
   ASSERT_FALSE(dottedDir.completeChildPath(kProbeProgram + ".exe").ensureFile());

   FilePath qualified;
   Error err = findProgramOnPath(
      dottedDir.completeChildPath(kProbeProgram).getAbsolutePath(),
      &qualified);

   // evaluate before cleanup, so a failure doesn't leave the temp directory behind
   bool foundProbe = !err &&
                     qualified.isRegularFile() &&
                     qualified.getFilename() == kProbeProgram + ".exe";

   probeDir.remove();

   ASSERT_FALSE(err);
   ASSERT_TRUE(foundProbe);
}

TEST(Win32SystemTest, FindProgramOnPathSearchesSystemDirectories)
{
   // clear PATH so the system directories are the only place cmd.exe can come from
   std::string savedPath = getenv("PATH");
   setenv("PATH", "");

   FilePath cmdPath;
   Error err = findProgramOnPath("cmd.exe", &cmdPath);

   setenv("PATH", savedPath);

   ASSERT_FALSE(err);
   ASSERT_TRUE(cmdPath.isRegularFile());
}

TEST(Win32SystemTest, FindProgramOnPathPrefersSystemDirectories)
{
   FilePath cmdPath;
   ASSERT_FALSE(findProgramOnPath("cmd.exe", &cmdPath));

   // A decoy cmd.exe at the front of PATH must not shadow the one in System32.
   // PathFindOnPath, which this replaced, searched the system directories before
   // PATH, and resolving cmd.exe is how we launch batch files.
   FilePath decoyDir;
   ASSERT_FALSE(FilePath::tempFilePath(decoyDir));
   ASSERT_FALSE(decoyDir.ensureDirectory());
   ASSERT_FALSE(decoyDir.completeChildPath("cmd.exe").ensureFile());

   std::string savedPath = getenv("PATH");
   setenv("PATH", decoyDir.getAbsolutePathNative() + ";" + savedPath);

   FilePath resolved;
   Error err = findProgramOnPath("cmd.exe", &resolved);

   setenv("PATH", savedPath);
   decoyDir.remove();

   ASSERT_FALSE(err);
   ASSERT_TRUE(resolved == cmdPath);
}

TEST(Win32SystemTest, FindProgramOnPathTrimsQuotedEntries)
{
   // The program has to be one the system directories cannot supply. Those are searched
   // ahead of PATH, so probing for something like cmd.exe would resolve from System32
   // and return before the quoted PATH entry was ever examined -- leaving the trimming
   // this test exists to cover unexercised.
   const std::string kProbeProgram = "rs-probe-374732.exe";

   FilePath probeDir;
   ASSERT_FALSE(FilePath::tempFilePath(probeDir));
   ASSERT_FALSE(probeDir.ensureDirectory());
   ASSERT_FALSE(probeDir.completeChildPath(kProbeProgram).ensureFile());

   std::string savedPath = getenv("PATH");
   setenv("PATH", "  \"" + probeDir.getAbsolutePathNative() + "\"  ");

   FilePath quotedPath;
   Error err = findProgramOnPath(kProbeProgram, &quotedPath);

   // evaluate before cleanup, so a failure doesn't leave the environment or temp dir behind
   bool foundProbe = !err &&
                     quotedPath.isRegularFile() &&
                     quotedPath.getFilename() == kProbeProgram;

   setenv("PATH", savedPath);
   probeDir.remove();

   ASSERT_FALSE(err);
   ASSERT_TRUE(foundProbe);
}

TEST(Win32SystemTest, RunCommandReachesTheShell)
{
   // runCommand() resolves cmd.exe through findProgramOnPath() and then builds the
   // CreateProcessW command line by hand. cmd parses that line itself and stops
   // scanning an unquoted program name at the first '/', so a forward-slash path to
   // cmd.exe leaves it reading "/Windows/..." as switches: the shell starts, prints a
   // usage complaint and exits, and the command never runs. That failure is silent at
   // every layer above -- runCommand() succeeds, only the exit status says otherwise.
   ProcessResult result;
   Error err = runCommand("echo rstudio-shell-probe", ProcessOptions(), &result);
   ASSERT_FALSE(err);
   ASSERT_EQ(0, result.exitStatus);
   ASSERT_TRUE(boost::algorithm::contains(result.stdOut, "rstudio-shell-probe"));
}

TEST(Win32SystemTest, LongPathNameRoundTrip)
{
   FilePath tempDir;
   Error err = FilePath::tempFilePath(tempDir);
   ASSERT_FALSE(err);

   err = tempDir.ensureDirectory();
   ASSERT_FALSE(err);

   std::string longPath = tempDir.getAbsolutePath();
   std::string shortPath = file_utils::shortPathName(longPath);
   if (shortPath == longPath)
   {
      // 8.3 name generation can be disabled per-volume, in which case there's no
      // short name to expand back
      tempDir.remove();
      GTEST_SKIP() << "no 8.3 short name available for the temporary directory";
   }

   // expanding the short form should recover the long form
   ASSERT_TRUE(boost::algorithm::iequals(file_utils::longPathName(shortPath), longPath));

   tempDir.remove();
}

TEST(Win32SystemTest, LongPathNamePassesThroughMissingPaths)
{
   // GetLongPathNameW returns 0 for a path that doesn't exist, and we hand back the
   // input. Note this does not cover the buffer-too-small case (a non-zero return with
   // the buffer left unwritten), which is what the old fixed-size code misread as
   // success; reproducing that needs a path longer than its 520-byte buffer.
   std::string missing = "C:\\oncetherewasafakedirectory374732\\nope.txt";
   ASSERT_EQ(missing, file_utils::longPathName(missing));
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
   ASSERT_TRUE(StartProcess("ping -n 8 posit.co"));

   std::vector<SubprocInfo> children = getSubprocesses(pi.dwProcessId);
   ASSERT_TRUE(children.empty()) << "unexpected children:" << Describe(children);
}

TEST_F(Win32ProcessTest, CorrectDetectionOfChildProcesses)
{
   ASSERT_TRUE(StartProcess("cmd.exe /S /C \"ping -n 8 posit.co\" 1> nul"));

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

   ASSERT_TRUE(StartProcess("cmd.exe /S /C \"ping -n 8 posit.co\" 1> nul"));

   ::Sleep(100); // give child time to start

   FilePath cwd = currentWorkingDir(pi.dwProcessId);

   // API is not implemented on Windows and should always return an empty
   // FilePath. See currentWorkingDir in Win32System.cpp for more info.
   ASSERT_TRUE(cwd.isEmpty());
}

TEST_F(Win32ProcessTest, EmptySubprocListWhenNoChildProcesses)
{
   ASSERT_TRUE(StartProcess("ping -n 8 posit.co"));

   std::vector<SubprocInfo> children = getSubprocesses(pi.dwProcessId);
   ASSERT_TRUE(children.empty()) << "unexpected children:" << Describe(children);
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
