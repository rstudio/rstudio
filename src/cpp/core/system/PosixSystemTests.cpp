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
#include <gtest/gtest.h>

#include <tests/fixtures/RequiresPrivilegeTestFixture.hpp>

namespace rstudio {
namespace core {
namespace system {

OSInfo parseOsReleaseContent(const std::string&);

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

   EXPECT_FALSE(group.empty());
   return group;
}

#endif

TEST(PosixTests, FindProgramFindsWhich)
{
   FilePath whichPath;
   Error error = findProgramOnPath("which", &whichPath);
   EXPECT_FALSE(error);
   
   std::string resolvedPath = whichPath.getAbsolutePath();
   EXPECT_TRUE(resolvedPath == "/usr/bin/which" || resolvedPath == "/bin/which");
}

TEST(PosixTests, NoSubprocessesViaPgrep)
{
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(1);
      _exit(0);
   }
   else
   {
      // process we started doesn't have a subprocess
      std::vector<SubprocInfo> children = getSubprocessesViaPgrep(pid);
      EXPECT_TRUE(children.empty());

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

TEST(PosixTests, FindSubprocessNameViaPgrep)
{
   std::string exe = "sleep";

   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      execlp(exe.c_str(), exe.c_str(), "100", nullptr);
      EXPECT_TRUE(false); // shouldn't get here!
   }
   else
   {
      // we now have a subprocess
      std::vector<SubprocInfo> children = getSubprocessesViaPgrep(getpid());
      EXPECT_TRUE(children.size() >= 1u);
      if (children.size() >= 1u)
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
         EXPECT_TRUE(found);
      }

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

#ifdef __APPLE__ // Mac-specific subprocess detection

TEST(PosixTests, NoSubprocessesMac)
{
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(1);
      _exit(0);
   }
   else
   {
      // process we started doesn't have a subprocess
      std::vector<SubprocInfo> children = getSubprocessesMac(pid);
      EXPECT_TRUE(children.empty());

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

TEST(PosixTests, FindSubprocessPidMac)
{
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(1);
      _exit(0);
   }
   else
   {
      // we now have a subprocess
      std::vector<SubprocInfo> children = getSubprocessesMac(getpid());
      EXPECT_TRUE(children.size() == 1u);
      EXPECT_TRUE(children.at(0).pid == pid);

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

TEST(PosixTests, FindSubprocessNameMac)
{
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);
   std::string exe = "sleep";

   if (pid == 0)
   {
      execlp(exe.c_str(), exe.c_str(), "100", nullptr);
      EXPECT_TRUE(false); // shouldn't get here!
   }
   else
   {
      // we now have a subprocess, need a slight pause to allow system tables to
      // catch up
      ::sleep(1);
      std::vector<SubprocInfo> children = getSubprocessesMac(getpid());
      EXPECT_TRUE(children.size() == 1u);
      if (children.size() == 1u)
         EXPECT_TRUE(children[0].exe.compare(exe) == 0);

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

TEST(PosixTests, WorkingDirMac)
{
   FilePath emptyPath;
   FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(1);
      _exit(0);
   }
   else
   {
      // we now have a subprocess
      FilePath cwd = currentWorkingDirMac(pid);
      EXPECT_FALSE(cwd.isEmpty());
      EXPECT_TRUE(cwd.exists());
      EXPECT_EQ(startingDir, cwd);

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

#else

TEST(PosixTests, NoSubprocessesProcFs)
{
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(1);
      _exit(0);
   }
   else
   {
      // process we started doesn't have a subprocess
      std::vector<SubprocInfo> children = getSubprocessesViaProcFs(pid);
      EXPECT_TRUE(children.empty());

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

TEST(PosixTests, FindSubprocessProcFs)
{
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);
   std::string exe = "sleep";

   if (pid == 0)
   {
      execlp(exe.c_str(), exe.c_str(), "10000", nullptr);
      EXPECT_TRUE(false); // shouldn't get here!
   }
   else
   {
      // we now have a subprocess
      ::sleep(1);
      std::vector<SubprocInfo> children = getSubprocessesViaProcFs(getpid());
      EXPECT_TRUE(children.size() >= 1u);
      if (children.size() >= 1u)
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
         EXPECT_TRUE(found);
      }

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}
#endif // !__APPLE__

TEST(PosixTests, NoSubprocessesGeneric)
{
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(1);
      _exit(0);
   }
   else
   {
      // process we started doesn't have a subprocess
      std::vector<SubprocInfo> children = getSubprocesses(pid);
      EXPECT_TRUE(children.empty());

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

TEST(PosixTests, WorkingDirGeneric)
{
   FilePath emptyPath;
   FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(1);
      _exit(0);
   }
   else
   {
      // we now have a subprocess
      FilePath cwd = currentWorkingDir(pid);
      EXPECT_FALSE(cwd.isEmpty());
      EXPECT_TRUE(cwd.exists());
      EXPECT_EQ(startingDir, cwd);

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

#ifndef __APPLE__

TEST(PosixTests, WorkingDirLsof)
{
   FilePath lsofPath;
   Error error = findProgramOnPath("lsof", &lsofPath);
   EXPECT_FALSE(error);
   
   std::string resolvedPath = lsofPath.getAbsolutePath();
   EXPECT_TRUE(resolvedPath.find("lsof") != std::string::npos);

   FilePath emptyPath;
   FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(10); // 1 sec was not enough time for lsof to run all the time in jenkins
      _exit(0);
   }
   else
   {
      // we now have a subprocess
      FilePath cwd;
      error = currentWorkingDirViaLsof(pid, &cwd);
      EXPECT_FALSE(error);
      EXPECT_FALSE(cwd.isEmpty());
      EXPECT_TRUE(cwd.exists());
      EXPECT_EQ(startingDir, cwd);

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

TEST(PosixTests, ParseOsReleaseEmpty)
{
   const auto content = "";
   OSInfo info = parseOsReleaseContent(content);
   EXPECT_TRUE(info.osId.empty());
   EXPECT_TRUE(info.osVersion.empty());
   EXPECT_TRUE(info.osVersionCodename.empty());
}

TEST(PosixTests, ParseOsReleaseUnquoted)
{
   std::string content = R"(
ID=ubuntu
VERSION_ID=20.04
VERSION_CODENAME=focal
)";
   OSInfo info = parseOsReleaseContent(content);
   EXPECT_EQ(info.osId, "ubuntu");
   EXPECT_EQ(info.osVersion, "20.04");
   EXPECT_EQ(info.osVersionCodename, "focal");
}

TEST(PosixTests, WorkingDirProcFs)
{
   FilePath emptyPath;
   FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
   pid_t pid = fork();
   EXPECT_FALSE(pid == -1);

   if (pid == 0)
   {
      ::sleep(1);
      _exit(0);
   }
   else
   {
      // we now have a subprocess
      FilePath cwd = currentWorkingDirViaProcFs(pid);
      EXPECT_FALSE(cwd.isEmpty());
      EXPECT_TRUE(cwd.exists());
      EXPECT_EQ(startingDir, cwd);

      ::kill(pid, SIGKILL);
      ::waitpid(pid, nullptr, 0);
   }
}

#endif // !__APPLE__

// Test fixture for privilege tests with user and group handling
class PosixTestsRequiresPrivilege : public rstudio::tests::fixtures::RequiresPrivilegeTestFixture
{
protected:
   User testUser;
   group::Group testGroup;
   group::Group testNonMemberGroup;

   void SetUp() override
   {
      // Call parent SetUp to check for root privileges
      rstudio::tests::fixtures::RequiresPrivilegeTestFixture::SetUp();
      
      // Initialize the test user and groups if we didn't skip the test
#ifdef __linux__
      initUserAndGroup("nobody", getNoGroupName(), "users");
#endif // __linux__

#ifdef __APPLE__
      initUserAndGroup("nobody", "nobody", "daemon");
#endif // __APPLE__
   }

   // Platform-specific helper functions
#ifdef __linux__
   static std::string getNobodyUsername() { return "nobody"; }
   static std::string getNobodyGroup() { return tests::getNoGroupName(); }
   static std::string getNonMemberGroup() { return "users"; }
#endif

#ifdef __APPLE__
   static std::string getNobodyUsername() { return "nobody"; }
   static std::string getNobodyGroup() { return "nobody"; }
   static std::string getNonMemberGroup() { return "daemon"; }
#endif

private:
#ifdef __linux__
   std::string getNoGroupName()
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

      EXPECT_FALSE(group.empty());
      return group;
   }
#endif

   void initUserAndGroup(std::string username, std::string groupname, std::string nonmember_groupname)
   {
      // get user info
      Error error = User::getUserFromIdentifier(username, testUser);
      EXPECT_FALSE(error);
      if (error)
      {
         LOG_ERROR(error);
         GTEST_SKIP() << "Could not get user information for " << username;
         return;
      }

      // get group info. user should be a member of this group.
      error = group::groupFromName(groupname, &testGroup);
      EXPECT_FALSE(error);
      if (error)
      {
         LOG_ERROR(error);
         GTEST_SKIP() << "Could not get group information for " << groupname;
         return;
      }

      // get secondary group info. user should not be a member of this group.
      error = group::groupFromName(nonmember_groupname, &testNonMemberGroup);
      EXPECT_FALSE(error);
      if (error)
      {
         LOG_ERROR(error);
         GTEST_SKIP() << "Could not get group information for " << nonmember_groupname;
         return;
      }
   }
};

TEST_F(PosixTestsRequiresPrivilege, TemporarilyDropPrivUsesPrimaryGroup)
{
   // drop privs to the unprivileged user
   Error error = temporarilyDropPriv(testUser.getUsername().c_str(), false);
   EXPECT_FALSE(error);

   // check real and effective user
   uid_t ruid = getuid();
   uid_t euid = geteuid();
   EXPECT_EQ(ruid, 0u);
   EXPECT_EQ(euid, testUser.getUserId());

   // check real and effective group
   gid_t rgid = getgid();
   gid_t egid = getegid();
   EXPECT_EQ(rgid, 0u);
   // since we didn't provide a target group, we expect the target user's primary group
   EXPECT_EQ(egid, testUser.getGroupId());

   error = restorePriv();
   EXPECT_FALSE(error);
}

TEST_F(PosixTestsRequiresPrivilege, TemporarilyDropPrivUsesAlternateGroup)
{
   // drop privs to the unprivileged user
   Error error = temporarilyDropPriv(testUser.getUsername().c_str(),
                                    testGroup.name,
                                    false);
   EXPECT_FALSE(error);

   // check real and effective user
   uid_t ruid = getuid();
   uid_t euid = geteuid();
   EXPECT_EQ(ruid, 0u);
   EXPECT_EQ(euid, testUser.getUserId());

   // check real and effective group
   gid_t rgid = getgid();
   gid_t egid = getegid();
   EXPECT_EQ(rgid, 0u);
   // we provided a target group, so we expect that group's ID
   EXPECT_EQ(egid, testGroup.groupId);

   error = restorePriv();
   EXPECT_FALSE(error);
}

TEST_F(PosixTestsRequiresPrivilege, TemporarilyDropPrivChecksGroupMembership)
{
   // drop privs to the unprivileged user, but specify a group that the user is not in
   Error error = temporarilyDropPriv(testUser.getUsername().c_str(),
                                    testNonMemberGroup.name,
                                    false);
   EXPECT_TRUE(error);
}

TEST_F(PosixTestsRequiresPrivilege, DISABLED_PermanentlyDropPrivUsesPrimaryGroup)
{
   // drop privs to the unprivileged user
   Error error = permanentlyDropPriv(testUser.getUsername().c_str());
   EXPECT_FALSE(error);

   // check real and effective user
   uid_t ruid = getuid();
   uid_t euid = geteuid();
   EXPECT_EQ(ruid, testUser.getUserId());
   EXPECT_EQ(euid, testUser.getUserId());

   // check real and effective group
   gid_t rgid = getgid();
   gid_t egid = getegid();
   // since we didn't provide a target group, we expect the target user's primary group
   EXPECT_EQ(rgid, testUser.getGroupId());
   EXPECT_EQ(egid, testUser.getGroupId());
}

TEST_F(PosixTestsRequiresPrivilege, DISABLED_PermanentlyDropPrivUsesAlternateGroup)
{
   // try dropping privs to a target group that target user does not belong to
   Error error = permanentlyDropPriv(testUser.getUsername().c_str(), testNonMemberGroup.name);
   EXPECT_EQ(error.getCode(), boost::system::errc::permission_denied);
}

TEST_F(PosixTestsRequiresPrivilege, DISABLED_PermanentlyDropPrivChecksGroupMembership)
{
   // drop privs to the unprivileged user and target group
   Error error = permanentlyDropPriv(testUser.getUsername().c_str(), testGroup.name);
   EXPECT_FALSE(error);

   // check real and effective user
   uid_t ruid = getuid();
   uid_t euid = geteuid();
   EXPECT_EQ(ruid, testUser.getUserId());
   EXPECT_EQ(euid, testUser.getUserId());

   // check real and effective group
   gid_t rgid = getgid();
   gid_t egid = getegid();
   // since we provided a target group, we now expect the target group
   EXPECT_EQ(rgid, testGroup.groupId);
   EXPECT_EQ(egid, testGroup.groupId);
}

TEST(PosixTests, ParseOsReleaseQuoted)
{
   std::string content = R"(
ID="ubuntu"
VERSION_ID="20.04"
VERSION_CODENAME="focal"
)";
   OSInfo info = parseOsReleaseContent(content);
   EXPECT_EQ(info.osId, "ubuntu");
   EXPECT_EQ(info.osVersion, "20.04");
   EXPECT_EQ(info.osVersionCodename, "focal");
}

} // namespace tests
} // namespace system
} // namespace core
} // namespace rstudio

#endif // _WIN32