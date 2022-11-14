/*
 * PosixSystemTests.cpp
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

#ifndef _WIN32

#include <core/system/PosixSystem.hpp>
#include <core/system/PosixGroup.hpp>
#include <signal.h>
#include <sys/wait.h>
#include <unistd.h>
#include <grp.h>
#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

#ifdef __linux__

static std::string getNoGroupName()
{
   std::string group;

   // Fun with groups:
   //
   // - Debian/Ubuntu have nobody user in the group "nogroup" and the "nobody" group doesn't exist
   // - RHEL/CentOS have nobody in the "nobody" group, and "nogroup" doesn't exist
   // - OpenSUSE has both groups, but nobody belongs to "nobody"
   //
   if (getgrnam("nobody"))
      group = "nobody"; // RHEL/CentOS/OpenSUSE
   else if (getgrnam("nogroup"))
      group = "nogroup"; // Debian/Ubuntu

   expect_false(group.empty());
   return group;
}

#endif

test_context("PosixSystemTests")
{
   test_that("findProgramOnPath can find core utils")
   {
      FilePath whichPath;
      Error error = findProgramOnPath("which", &whichPath);
      expect_false(error);
      
      std::string resolvedPath = whichPath.getAbsolutePath();
      expect_true(resolvedPath == "/usr/bin/which" || resolvedPath == "/bin/which");
   }
   
   test_that("Empty subprocess list returned correctly with pgrep method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesViaPgrep(pid);
         expect_true(children.empty());

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

   test_that("Subprocess name detected correctly with pgrep method")
   {
      std::string exe = "sleep";

      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         execlp(exe.c_str(), exe.c_str(), "100", nullptr);
         expect_true(false); // shouldn't get here!
      }
      else
      {
         // we now have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesViaPgrep(getpid());
         expect_true(children.size() >= 1);
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
            expect_true(found);
         }

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

#ifdef __APPLE__ // Mac-specific subprocess detection

   test_that("Subprocess list correctly empty with Mac method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesMac(pid);
         expect_true(children.empty());

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

   test_that("Subprocess count and pid detected correctly with Mac method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesMac(getpid());
         expect_true(children.size() == 1);
         expect_true(children.at(0).pid == pid);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

   test_that("Subprocess name detected correctly with Mac method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);
      std::string exe = "sleep";

      if (pid == 0)
      {
         execlp(exe.c_str(), exe.c_str(), "100", nullptr);
         expect_true(false); // shouldn't get here!
      }
      else
      {
         // we now have a subprocess, need a slight pause to allow system tables to
         // catch up
         ::sleep(1);
         std::vector<SubprocInfo> children = getSubprocessesMac(getpid());
         expect_true(children.size() == 1);
         if (children.size() == 1)
            expect_true(children[0].exe.compare(exe) == 0);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

   test_that("Current working directory determined correctly with Mac method")
   {
      FilePath emptyPath;
      FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         FilePath cwd = currentWorkingDirMac(pid);
         expect_false(cwd.isEmpty());
         expect_true(cwd.exists());
         expect_true(startingDir == cwd);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

#else

   test_that("No subprocesses detected correctly with procfs method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesViaProcFs(pid);
         expect_true(children.empty());

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

   test_that("Subprocess detected correctly with procfs method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);
      std::string exe = "sleep";

      if (pid == 0)
      {
         execlp(exe.c_str(), exe.c_str(), "10000", nullptr);
         expect_true(false); // shouldn't get here!
      }
      else
      {
         // we now have a subprocess
         ::sleep(1);
         std::vector<SubprocInfo> children = getSubprocessesViaProcFs(getpid());
         expect_true(children.size() >= 1);
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
            expect_true(found);
         }

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }
#endif // !__APPLE__

   test_that("Empty list of subprocesses returned correctly with generic method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         std::vector<SubprocInfo> children = getSubprocesses(pid);
         expect_true(children.empty());

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

   test_that("Current working directory determined correctly with generic method")
   {
      FilePath emptyPath;
      FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         FilePath cwd = currentWorkingDir(pid);
         expect_false(cwd.isEmpty());
         expect_true(cwd.exists());
         expect_true(startingDir == cwd);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

#ifndef __APPLE__

   test_that("Current working directory determined correctly with lsof method")
   {
      FilePath lsofPath;
      Error error = findProgramOnPath("lsof", &lsofPath);
      expect_false(error);
      
      std::string resolvedPath = lsofPath.getAbsolutePath();
      expect_true(resolvedPath.find("lsof") != std::string::npos);

      FilePath emptyPath;
      FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         FilePath cwd;
         error = currentWorkingDirViaLsof(pid, &cwd);
         expect_false(error);
         expect_false(cwd.isEmpty());
         expect_true(cwd.exists());
         expect_true(startingDir == cwd);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }

   test_that("Current working directory determined correctly with procfs method")
   {
      FilePath emptyPath;
      FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         FilePath cwd = currentWorkingDirViaProcFs(pid);
         expect_false(cwd.isEmpty());
         expect_true(cwd.exists());
         expect_true(startingDir == cwd);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, nullptr, 0);
      }
   }
#endif // !__APPLE__
}

User s_testUser;
group::Group s_testGroup;
group::Group s_testNonMemberGroup;

bool initUserAndGroup(std::string username, std::string groupname, std::string nonmember_groupname)
{
   bool isRoot = core::system::effectiveUserIsRoot();
   expect_true(isRoot);
   if (!isRoot)
      return false;

   // get user info
   Error error = User::getUserFromIdentifier(username, s_testUser);
   expect_false(error);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // get group info. user should be a member of this group.
   error = group::groupFromName(groupname, &s_testGroup);
   expect_false(error);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   // get secondary group info. user should not be a member of this group.
   error = group::groupFromName(nonmember_groupname, &s_testNonMemberGroup);
   expect_false(error);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   return true;
}

TEST_CASE("TemporarilyDropPrivTests", "[requiresRoot]")
{
#ifdef __linux__
      expect_true(initUserAndGroup("nobody", getNoGroupName(), "users"));
#endif // __linux__
#ifdef __APPLE__
      expect_true(initUserAndGroup("nobody", "nobody", "daemon"));
#endif // __APPLE__

   test_that("temporarilyDropPriv uses primary group")
   {
      // drop privs to the unprivileged user
      Error error = temporarilyDropPriv(s_testUser.getUsername().c_str(), false);
      expect_false(error);

      // check real and effective user
      uid_t ruid = getuid();
      uid_t euid = geteuid();
      expect_true(ruid == 0);
      expect_true(euid == s_testUser.getUserId());

      // check real and effective group
      gid_t rgid = getgid();
      gid_t egid = getegid();
      expect_true(rgid == 0);
      // since we didn't provide a target group, we expect the target user's primary group
      expect_true(egid == s_testUser.getGroupId());

      error = restorePriv();
      expect_false(error);
   }

   test_that("temporarilyDropPriv uses alternate group")
   {
      // drop privs to the unprivileged user and target group
      Error error = temporarilyDropPriv(s_testUser.getUsername().c_str(), s_testGroup.name, false);
      expect_false(error);

      // check real and effective user
      uid_t ruid = getuid();
      uid_t euid = geteuid();
      expect_true(ruid == 0);
      expect_true(euid == s_testUser.getUserId());

      // check real and effective group
      gid_t rgid = getgid();
      gid_t egid = getegid();
      expect_true(rgid == 0);
      // since we provided a target group, we now expect the target group
      expect_true(egid == s_testGroup.groupId);

      error = restorePriv();
      expect_false(error);
   }

   test_that("temporarilyDropPriv checks group membership with alternate group")
   {
      // try dropping privs to a target group that target user does not belong to
      Error error = temporarilyDropPriv(s_testUser.getUsername().c_str(), s_testNonMemberGroup.name, false);
      expect_true(error.getCode() == boost::system::errc::permission_denied);

      error = restorePriv();
      expect_false(error);
   }
}


TEST_CASE("PermanentlyDropPrivPrimaryTests", "[requiresRoot]")
{
#ifdef __linux__
   expect_true(initUserAndGroup("nobody", getNoGroupName(), "users"));
#endif // __linux__
#ifdef __APPLE__
   expect_true(initUserAndGroup("nobody", "nobody", "daemon"));
#endif // __APPLE__

   test_that("permanentlyDropPriv uses primary group")
   {
      // drop privs to the unprivileged user
      Error error = permanentlyDropPriv(s_testUser.getUsername().c_str());
      expect_false(error);

      // check real and effective user
      uid_t ruid = getuid();
      uid_t euid = geteuid();
      expect_true(ruid == s_testUser.getUserId());
      expect_true(euid == s_testUser.getUserId());

      // check real and effective group
      gid_t rgid = getgid();
      gid_t egid = getegid();
      // since we didn't provide a target group, we expect the target user's primary group
      expect_true(rgid == s_testUser.getGroupId());
      expect_true(egid == s_testUser.getGroupId());
   }
}

TEST_CASE("PermanentlyDropPrivAlternateTests", "[requiresRoot]")
{
#ifdef __linux__
   expect_true(initUserAndGroup("nobody", getNoGroupName(), "users"));
#endif // __linux__
#ifdef __APPLE__
   expect_true(initUserAndGroup("nobody", "nobody", "daemon"));
#endif // __APPLE__

   test_that("permanentlyDropPriv checks group membership with alternate group")
   {
      // try dropping privs to a target group that target user does not belong to
      Error error = permanentlyDropPriv(s_testUser.getUsername().c_str(), s_testNonMemberGroup.name);
      expect_true(error.getCode() == boost::system::errc::permission_denied);
   }

   test_that("permanentlyDropPriv uses alternate group")
   {
      // drop privs to the unprivileged user and target group
      Error error = permanentlyDropPriv(s_testUser.getUsername().c_str(), s_testGroup.name);
      expect_false(error);

      // check real and effective user
      uid_t ruid = getuid();
      uid_t euid = geteuid();
      expect_true(ruid == s_testUser.getUserId());
      expect_true(euid == s_testUser.getUserId());

      // check real and effective group
      gid_t rgid = getgid();
      gid_t egid = getegid();
      // since we provided a target group, we now expect the target group
      expect_true(rgid == s_testGroup.groupId);
      expect_true(egid == s_testGroup.groupId);
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // !_WIN32
