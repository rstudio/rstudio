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

#include <grp.h>
#include <limits.h>
#include <pthread.h>
#include <signal.h>
#include <sys/wait.h>
#include <unistd.h>

#include <algorithm>
#include <atomic>
#include <cerrno>
#include <chrono>
#include <condition_variable>
#include <cstring>
#include <functional>
#include <limits>
#include <mutex>
#include <thread>

#include <boost/asio/ip/address.hpp>
#include <boost/asio/ip/address_v4.hpp>
#include <boost/scope_exit.hpp>

#include <gtest/gtest.h>

#ifdef __linux__
# include <sys/prctl.h>
#endif

#include <core/DateTime.hpp>
#include <core/Thread.hpp>
#include <core/system/ParentProcessMonitor.hpp>
#include <core/system/PosixChildProcessTracker.hpp>
#include <core/system/PosixGroup.hpp>

#include <shared_core/ILogDestination.hpp>
#include <shared_core/Logger.hpp>

#include <tests/fixtures/RequiresPrivilegeTestFixture.hpp>

namespace rstudio {
namespace core {
namespace system {

OSInfo parseOsReleaseContent(const std::string&);

namespace detail {

bool isLinkLocalIpv4(const boost::asio::ip::address_v4& addr);

std::string resolveBindAddressForAddresses(
      const std::string& address,
      const std::vector<posix::IpAddress>& addrs);

} // namespace detail

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

TEST(PosixTests, CreationTimeParsesCommandNameWithSpaces)
{
   // /proc/<pid>/stat wraps the command name in parentheses; a name with
   // spaces (or parentheses) must not shift the start-time field
   int ready[2];
   int done[2];
   ASSERT_EQ(0, ::pipe(ready));
   ASSERT_EQ(0, ::pipe(done));

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      ::close(ready[0]);
      ::close(done[1]);
      ::prctl(PR_SET_NAME, "a b (c) d");
      (void)::write(ready[1], "x", 1);
      char signal;
      (void)::read(done[0], &signal, 1);
      ::_exit(0);
   }

   ::close(ready[1]);
   ::close(done[0]);
   char signal = 0;
   ASSERT_EQ(1, ::read(ready[0], &signal, 1));

   ProcessInfo info;
   info.pid = child;
   boost::posix_time::ptime created;
   Error error = info.creationTime(&created);
   EXPECT_FALSE(error) << error.asString();

   double startSeconds = date_time::secondsSinceEpoch(created);
   double now = date_time::secondsSinceEpoch();
   EXPECT_LE(startSeconds, now + 5);
   EXPECT_GE(startSeconds, now - 120);

   ASSERT_EQ(1, ::write(done[1], "x", 1));
   int status;
   ASSERT_EQ(child, ::waitpid(child, &status, 0));
}

#endif

static posix::IpAddress ipAddress(const std::string& name,
                                  const std::string& address)
{
   posix::IpAddress ip;
   ip.Name = name;
   ip.Address = address;
   return ip;
}

TEST(PosixTests, FindProgramFindsWhich)
{
   FilePath whichPath;
   Error error = findProgramOnPath("which", &whichPath);
   EXPECT_FALSE(error);
   
   std::string resolvedPath = whichPath.getAbsolutePath();
   EXPECT_TRUE(resolvedPath == "/usr/bin/which" || resolvedPath == "/bin/which");
}

TEST(PosixTests, FindProgramResolvesQualifiedNames)
{
   FilePath whichPath;
   Error error = findProgramOnPath("which", &whichPath);
   ASSERT_FALSE(error);

   // an already-qualified name isn't a PATH search, but should still resolve. this
   // used to report success while handing back the PATH entry directory, because the
   // rooted name was passed to completeChildPath(), which rejects it. See #12806.
   FilePath qualified;
   error = findProgramOnPath(whichPath.getAbsolutePath(), &qualified);
   EXPECT_FALSE(error);
   EXPECT_TRUE(qualified == whichPath);

   // a qualified name that doesn't exist has to fail
   EXPECT_TRUE(findProgramOnPath(whichPath.getAbsolutePath() + "-nope", &qualified));

   // ...as does a directory, or a file that isn't executable
   EXPECT_TRUE(findProgramOnPath("/usr/bin", &qualified));
   EXPECT_TRUE(findProgramOnPath("/etc/hosts", &qualified));
}

TEST(PosixTests, FindProgramRejectsEmptyName)
{
   FilePath programPath;
   EXPECT_TRUE(findProgramOnPath("", &programPath));
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
      // we now have a subprocess, need a slight pause to allow the child
      // process to complete exec before pgrep can find it
      ::sleep(1);
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

TEST(PosixTests, ResolveBindAddressPassesThroughSpecificAddresses)
{
   EXPECT_EQ(resolveBindAddress("127.0.0.1"), std::string("127.0.0.1"));
   EXPECT_EQ(resolveBindAddress("192.168.1.1"), std::string("192.168.1.1"));
   EXPECT_EQ(resolveBindAddress("::1"), std::string("::1"));
   EXPECT_EQ(resolveBindAddress(""), std::string(""));
}

TEST(PosixTests, ResolveBindAddressHandlesIpv4Wildcard)
{
   // resolution may pick either family depending on the host's interfaces,
   // so just require some wildcard address back
   boost::system::error_code ec;
   boost::asio::ip::address addr =
      boost::asio::ip::make_address(resolveBindAddress("0.0.0.0"), ec);

   ASSERT_FALSE(ec);
   EXPECT_TRUE(addr.is_unspecified());
}

TEST(PosixTests, ResolveBindAddressHandlesIpv6Wildcard)
{
   boost::system::error_code ec;
   boost::asio::ip::address addr =
      boost::asio::ip::make_address(resolveBindAddress("::"), ec);

   ASSERT_FALSE(ec);
   EXPECT_TRUE(addr.is_unspecified());
}

TEST(PosixTests, ResolveBindAddressPrefersIpv6WhenOnlyIpv6Available)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("lo", "::1"),
      ipAddress("eth0", "2001:db8::10")
   };

   EXPECT_EQ(detail::resolveBindAddressForAddresses("0.0.0.0", addrs), std::string("::"));
}

TEST(PosixTests, ResolveBindAddressKeepsIpv4OnDualStackHosts)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("lo", "127.0.0.1"),
      ipAddress("eth0", "192.168.1.10"),
      ipAddress("eth0", "2001:db8::10")
   };

   EXPECT_EQ(detail::resolveBindAddressForAddresses("0.0.0.0", addrs), std::string("0.0.0.0"));
}

TEST(PosixTests, ResolveBindAddressIgnoresScopedIpv6ForIpv4Wildcard)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("lo", "127.0.0.1"),
      ipAddress("eth0", "fe80::1%eth0")
   };

   EXPECT_EQ(detail::resolveBindAddressForAddresses("0.0.0.0", addrs), std::string("0.0.0.0"));
}

TEST(PosixTests, ResolveBindAddressIgnoresLinkLocalIpv6ForIpv4Wildcard)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("lo", "127.0.0.1"),
      ipAddress("eth0", "fe80::1")
   };

   EXPECT_EQ(detail::resolveBindAddressForAddresses("0.0.0.0", addrs), std::string("0.0.0.0"));
}

TEST(PosixTests, IsLinkLocalIpv4IdentifiesLinkLocalAddresses)
{
   EXPECT_TRUE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("169.254.0.0")));
   EXPECT_TRUE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("169.254.0.1")));
   EXPECT_TRUE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("169.254.255.255")));
   EXPECT_TRUE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("169.254.1.100")));
}

TEST(PosixTests, IsLinkLocalIpv4RejectsNonLinkLocalAddresses)
{
   EXPECT_FALSE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("127.0.0.1")));
   EXPECT_FALSE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("192.168.1.1")));
   EXPECT_FALSE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("10.0.0.1")));
   EXPECT_FALSE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("169.253.0.1")));
   EXPECT_FALSE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("169.255.0.1")));
   EXPECT_FALSE(detail::isLinkLocalIpv4(
      boost::asio::ip::make_address_v4("0.0.0.0")));
}

TEST(PosixTests, ResolveBindAddressIgnoresLinkLocalIpv4ForIpv4Wildcard)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("lo", "127.0.0.1"),
      ipAddress("eth0", "169.254.1.100"),
      ipAddress("eth0", "2001:db8::10")
   };

   EXPECT_EQ(detail::resolveBindAddressForAddresses("0.0.0.0", addrs), std::string("::"));
}

TEST(PosixTests, ResolveBindAddressSkipsUnparseableAddresses)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("bad0", "not-an-address"),
      ipAddress("eth0", "2001:db8::10")
   };

   EXPECT_EQ(detail::resolveBindAddressForAddresses("0.0.0.0", addrs), std::string("::"));
}

TEST(PosixTests, ResolveBindAddressFallsBackToIpv4WithoutIpv6)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("lo", "127.0.0.1"),
      ipAddress("eth0", "192.168.1.10")
   };

   EXPECT_EQ(detail::resolveBindAddressForAddresses("::", addrs), std::string("0.0.0.0"));
}

TEST(PosixTests, ResolveBindAddressPassesThroughHostNames)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("lo", "127.0.0.1"),
      ipAddress("eth0", "192.168.1.10")
   };

   EXPECT_EQ(detail::resolveBindAddressForAddresses("localhost", addrs), std::string("localhost"));
   EXPECT_EQ(detail::resolveBindAddressForAddresses("", addrs), std::string(""));
}

TEST(PosixTests, ResolveBindAddressTreatsAlternateWildcardSpellingsAsWildcard)
{
   std::vector<posix::IpAddress> addrs = {
      ipAddress("lo", "127.0.0.1"),
      ipAddress("eth0", "192.168.1.10")
   };

   // alternate spellings of the IPv6 wildcard resolve like "::" itself
   EXPECT_EQ(detail::resolveBindAddressForAddresses("::0", addrs), std::string("0.0.0.0"));
   EXPECT_EQ(detail::resolveBindAddressForAddresses("0:0:0:0:0:0:0:0", addrs), std::string("0.0.0.0"));
}

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

TEST(PosixTests, SafeLaunchThreadLeavesFaultSignalsUnblocked)
{
   sigset_t threadMask;
   sigemptyset(&threadMask);

   boost::thread thread;
   core::thread::safeLaunchThread([&threadMask]()
   {
      ::pthread_sigmask(SIG_BLOCK, nullptr, &threadMask);
   }, &thread);
   thread.join();

   // synchronous fault signals must stay unblocked: a blocked fault signal
   // makes the kernel bypass any crash handler and kill the process silently
   EXPECT_FALSE(sigismember(&threadMask, SIGSEGV));
   EXPECT_FALSE(sigismember(&threadMask, SIGBUS));
   EXPECT_FALSE(sigismember(&threadMask, SIGILL));
   EXPECT_FALSE(sigismember(&threadMask, SIGFPE));

   // asynchronous signals stay blocked on background threads
   EXPECT_TRUE(sigismember(&threadMask, SIGINT));
   EXPECT_TRUE(sigismember(&threadMask, SIGTERM));
   EXPECT_TRUE(sigismember(&threadMask, SIGHUP));
}

TEST(PosixTests, WaitForParentTerminationNoParentWithoutFds)
{
   using namespace parent_process_monitor;

   EXPECT_EQ(ParentTerminationNoParent, waitForParentTermination(-1, -1));
}

TEST(PosixTests, WaitForParentTerminationReadsPipeFds)
{
   using namespace parent_process_monitor;

   // parent writes before exiting: normal termination
   int fds[2];
   ASSERT_EQ(0, ::pipe(fds));
   ASSERT_EQ(4, ::write(fds[1], "done", 4));
   EXPECT_EQ(ParentTerminationNormal, waitForParentTermination(fds[0], fds[1]));
   ::close(fds[0]);

   // parent exits without writing (write end closed): abnormal termination
   ASSERT_EQ(0, ::pipe(fds));
   EXPECT_EQ(ParentTerminationAbnormal, waitForParentTermination(fds[0], fds[1]));
   ::close(fds[0]);
}

TEST(PosixTests, IsProcessRunningDetectsLiveAndDeadProcesses)
{
   // own process is trivially running
   EXPECT_TRUE(isProcessRunning(::getpid()));

   // pid 1 (init/launchd) always exists but is owned by root: when this
   // test runs unprivileged the kill(pid, 0) probe fails with EPERM, which
   // must still count as running -- rserver's privilege-dropped threads
   // probe rsession processes owned by other users this way (#18572)
   EXPECT_TRUE(isProcessRunning(1));

   // a pid no process can have reports not running
   EXPECT_FALSE(isProcessRunning(std::numeric_limits<pid_t>::max()));
}

TEST(PosixTests, ChildProcessTrackerReapsChildThatExitedBeforeRegistration)
{
   pid_t pid = ::fork();
   ASSERT_NE(-1, pid);

   if (pid == 0)
      ::_exit(42);

   // Observe the exit without reaping it. This reproduces the rserver race
   // where SIGCHLD is handled before the launched rsession pid is registered.
   siginfo_t childInfo;
   ::memset(&childInfo, 0, sizeof(childInfo));
   int result;
   do
   {
      result = ::waitid(P_PID, pid, &childInfo, WEXITED | WNOWAIT);
   }
   while (result == -1 && errno == EINTR);
   ASSERT_EQ(0, result);
   ASSERT_EQ(pid, childInfo.si_pid);

   bool exitHandled = false;
   int exitStatus = -1;
   ChildProcessTracker tracker;
   tracker.addProcess(pid, [&](PidType reapedPid, int status)
   {
      EXPECT_EQ(pid, reapedPid);
      exitHandled = true;
      exitStatus = status;
   });

   EXPECT_TRUE(exitHandled);

   // Keep the child from leaking if the assertion above regresses.
   tracker.notifySIGCHILD();

   ASSERT_TRUE(WIFEXITED(exitStatus));
   EXPECT_EQ(42, WEXITSTATUS(exitStatus));

   errno = 0;
   EXPECT_EQ(-1, ::waitpid(pid, nullptr, WNOHANG));
   EXPECT_EQ(ECHILD, errno);
}

TEST(PosixTests, ChildProcessTrackerLeavesRunningChildAlone)
{
   pid_t pid = ::fork();
   ASSERT_NE(-1, pid);

   if (pid == 0)
   {
      // sleep rather than block forever, so that a regression which drops
      // WNOHANG from waitPid surfaces as the failed expectation below rather
      // than as a hung test
      ::sleep(30);
      ::_exit(0);
   }

   bool exitHandled = false;
   ChildProcessTracker tracker;
   tracker.addProcess(pid, [&](PidType, int)
   {
      exitHandled = true;
   });

   // registration must neither block on nor reap a child that is still
   // running: waitpid returning 0 means the pid is alive and unreaped
   EXPECT_FALSE(exitHandled);
   EXPECT_EQ(0, ::waitpid(pid, nullptr, WNOHANG));

   // once the child really does exit, the tracker still reaps it
   ASSERT_EQ(0, ::kill(pid, SIGKILL));

   siginfo_t childInfo;
   ::memset(&childInfo, 0, sizeof(childInfo));
   int result;
   do
   {
      result = ::waitid(P_PID, pid, &childInfo, WEXITED | WNOWAIT);
   }
   while (result == -1 && errno == EINTR);
   ASSERT_EQ(0, result);

   tracker.notifySIGCHILD();
   EXPECT_TRUE(exitHandled);

   errno = 0;
   EXPECT_EQ(-1, ::waitpid(pid, nullptr, WNOHANG));
   EXPECT_EQ(ECHILD, errno);
}

namespace {

// generous budgets for the forked children below, well past anything a
// healthy child needs; a child that blows one has hung
const std::chrono::milliseconds kChildTimeout(10000);

bool waitUntil(const std::function<bool()>& predicate, std::chrono::milliseconds timeout)
{
   auto deadline = std::chrono::steady_clock::now() + timeout;
   while (!predicate())
   {
      if (std::chrono::steady_clock::now() >= deadline)
         return false;
      std::this_thread::sleep_for(std::chrono::milliseconds(10));
   }

   return true;
}

// reaps the child, killing it first if it hasn't exited within the timeout
bool waitForChildExit(pid_t child, int* pStatus, std::chrono::milliseconds timeout)
{
   bool exited = waitUntil([&]() { return ::waitpid(child, pStatus, WNOHANG) == child; }, timeout);
   if (!exited)
   {
      ::kill(child, SIGKILL);
      ::waitpid(child, nullptr, 0);
   }

   return exited;
}

// a log destination whose writes block until released, so that a thread
// writing to it holds the logger's read lock for as long as a test needs
class BlockingLogDestination : public log::ILogDestination
{
public:
   BlockingLogDestination()
      : log::ILogDestination("blocking-log-destination",
                             log::LogLevel::ERR,
                             log::LogMessageFormatType::PRETTY,
                             false)
   {
   }

   void refresh(const log::RefreshParams&) override
   {
   }

   void writeLog(log::LogLevel, const std::string&) override
   {
      std::unique_lock<std::mutex> lock(mutex_);
      writing_ = true;
      condition_.notify_all();
      condition_.wait(lock, [this] { return released_; });
   }

   bool waitUntilWriting(std::chrono::milliseconds timeout)
   {
      std::unique_lock<std::mutex> lock(mutex_);
      return condition_.wait_for(lock, timeout, [this] { return writing_; });
   }

   void release()
   {
      std::lock_guard<std::mutex> lock(mutex_);
      released_ = true;
      condition_.notify_all();
   }

private:
   std::mutex mutex_;
   std::condition_variable condition_;
   bool writing_ = false;
   bool released_ = false;
};

// notes that a fork has begun. registered by the test below, after the
// logger's handlers, so it runs before them and thus before any wait they impose
std::atomic<bool> s_forkPrepared(false);
std::once_flag s_forkPreparedRegistration;

void noteForkPrepared()
{
   s_forkPrepared = true;
}

// how the scenario below ended, as the exit status of the process it runs in
enum ForkDuringLogWriteResult
{
   kForkScenarioPassed = 0,
   kForkScenarioWriterNeverWrote,
   kForkScenarioForkNeverBegan,
   kForkScenarioForkFailed,
   kForkScenarioChildHung,
   kForkScenarioChildFailed
};

const char* const kForkDuringLogWriteResults[] = {
   "passed",
   "the writer never reached the log destination",
   "the fork never began",
   "fork() failed",
   "the child hung logging after the fork",
   "the child could not log after the fork"
};

// forks while another thread is mid log write, then logs in both the child and
// the parent. runs in a process of its own (see the test below): a regression
// hangs the fork, the child, or the parent's next log write, and none of those
// can be abandoned from inside the process that hit them
int runForkDuringLogWrite()
{
   s_forkPrepared = false;

   const std::string section = "fork-during-log-write";
   auto pDestination = std::make_shared<BlockingLogDestination>();
   log::addLogDestination(pDestination, section);

   std::thread writer([&]()
   {
      log::logErrorMessage("holding the logger's read lock", section);
   });

   bool writing = pDestination->waitUntilWriting(kChildTimeout);
   if (!writing)
   {
      pDestination->release();
      writer.join();
      return kForkScenarioWriterNeverWrote;
   }

   // fork on another thread, so this one can release the writer the fork is waiting for
   pid_t child = -1;
   std::thread forker([&]()
   {
      child = ::fork();
      if (child == 0)
      {
         // both of these hang in a child forked before the write finished: the
         // write blocks on this destination's release, which it would have
         // inherited unreleased, and the refresh on the inherited read lock
         log::logErrorMessage("logging in the child", section);
         log::refreshAllLogDestinations();
         ::_exit(0);
      }
   });

   bool forkStarted = waitUntil([]() { return s_forkPrepared.load(); }, kChildTimeout);
   pDestination->release();
   writer.join();
   forker.join();

   // reap the child before reporting anything, so that a hung one doesn't outlive us
   int status = 0;
   bool exited = false;
   if (child > 0)
      exited = waitForChildExit(child, &status, kChildTimeout);

   if (!forkStarted)
      return kForkScenarioForkNeverBegan;

   if (child == -1)
      return kForkScenarioForkFailed;

   if (!exited)
      return kForkScenarioChildHung;

   if (!WIFEXITED(status) || WEXITSTATUS(status) != 0)
      return kForkScenarioChildFailed;

   // the parent must have its lock back as well; a logger left locked hangs here
   log::logErrorMessage("logging in the parent after the fork", section);
   log::refreshAllLogDestinations();
   log::removeLogDestination(pDestination->getId(), section);

   return kForkScenarioPassed;
}

} // anonymous namespace

TEST(PosixTests, ForkWaitsForInFlightLogWriteAndChildCanLog)
{
   // rserver forks the child that becomes a session while other threads log,
   // and that child drops privilege and logs before it execs. fork() would
   // copy a mid-write thread's read lock into the child with no thread left to
   // release it, so the logger holds its lock across the fork instead: the fork
   // waits for the write to finish, and the child starts with a usable logger
   std::call_once(s_forkPreparedRegistration, []()
   {
      ASSERT_EQ(0, ::pthread_atfork(noteForkPrepared, nullptr, nullptr));
   });

   // the scenario hangs rather than fails when the fix regresses, so run it in
   // a process this test can kill; the scenario's own waits fit inside this budget
   pid_t scenario = ::fork();
   ASSERT_NE(-1, scenario);
   if (scenario == 0)
      ::_exit(runForkDuringLogWrite());

   int status = 0;
   ASSERT_TRUE(waitForChildExit(scenario, &status, 4 * kChildTimeout))
      << "the scenario hung: a fork or a log write after it never returned";
   ASSERT_TRUE(WIFEXITED(status)) << "scenario status: " << status;

   int result = WEXITSTATUS(status);
   ASSERT_LT(result, static_cast<int>(sizeof(kForkDuringLogWriteResults) / sizeof(*kForkDuringLogWriteResults)));
   EXPECT_EQ(kForkScenarioPassed, result) << kForkDuringLogWriteResults[result];
}

TEST(PosixTests, ResolveUserReturnsCurrentUserAndGroups)
{
   User user;
   ASSERT_FALSE(User::getCurrentUser(user));

   ResolvedUser resolved;
   ASSERT_FALSE(resolveUser(user.getUsername(), &resolved));
   EXPECT_EQ(user.getUserId(), resolved.user.getUserId());
   EXPECT_EQ(user.getGroupId(), resolved.user.getGroupId());

   // getgrouplist(3) always includes the primary group
   auto begin = resolved.groupIds.begin();
   auto end = resolved.groupIds.end();
   EXPECT_NE(end, std::find(begin, end, user.getGroupId()));
}

TEST(PosixTests, QueryUserGroupIdsPublishesNothingWhenTheLookupFails)
{
   // getgrouplist(3) can fail without touching the count or the buffer, and when
   // no larger buffer helps, the zeroed buffer must not then be handed back as
   // membership in gid 0
   User user;
   ASSERT_FALSE(User::getCurrentUser(user));

   std::vector<GidType> groupIds = { 12345 };
   Error error = group::queryUserGroupIds(user, &groupIds, [](const char*, gid_t, group::GroupListGidType*, int*)
   {
      errno = EIO;
      return -1;
   });

   ASSERT_TRUE(error);
   EXPECT_EQ(EIO, error.getCode());
   EXPECT_TRUE(groupIds.empty());
}

TEST(PosixTests, QueryUserGroupIdsGrowsTheBufferToFit)
{
   // a lookup that reports a larger count is retried with room for it, and only
   // the entries it reports come back
   User user;
   ASSERT_FALSE(User::getCurrentUser(user));

   const int numGroups = 150;
   int calls = 0;
   auto lookup = [&calls](const char*, gid_t, group::GroupListGidType* groups, int* pNumGroups) -> int
   {
      calls++;
      if (*pNumGroups < numGroups)
      {
         *pNumGroups = numGroups;
         return -1;
      }

      for (int i = 0; i < numGroups; i++)
         groups[i] = static_cast<group::GroupListGidType>(1000 + i);
      *pNumGroups = numGroups;
      return 0;
   };

   std::vector<GidType> groupIds;
   ASSERT_FALSE(group::queryUserGroupIds(user, &groupIds, lookup));
   EXPECT_EQ(2, calls);
   ASSERT_EQ(static_cast<std::size_t>(numGroups), groupIds.size());
   EXPECT_EQ(1000u, groupIds.front());
   EXPECT_EQ(1000u + numGroups - 1, groupIds.back());
}

TEST(PosixTests, QueryUserGroupIdsGrowsTheBufferWhenTheCountIsUnchanged)
{
   // macOS's getgrouplist(3) fills a short buffer and fails without saying how
   // many groups it needs, so the buffer is grown until they all fit
   User user;
   ASSERT_FALSE(User::getCurrentUser(user));

   const int numGroups = 150;
   int calls = 0;
   auto lookup = [&calls, numGroups](const char*, gid_t, group::GroupListGidType* groups, int* pNumGroups) -> int
   {
      calls++;
      int numFilled = std::min(*pNumGroups, numGroups);
      for (int i = 0; i < numFilled; i++)
         groups[i] = static_cast<group::GroupListGidType>(1000 + i);

      if (numFilled < numGroups)
         return -1;

      *pNumGroups = numGroups;
      return 0;
   };

   std::vector<GidType> groupIds;
   ASSERT_FALSE(group::queryUserGroupIds(user, &groupIds, lookup));
   EXPECT_EQ(2, calls);
   ASSERT_EQ(static_cast<std::size_t>(numGroups), groupIds.size());
   EXPECT_EQ(1000u, groupIds.front());
   EXPECT_EQ(1000u + numGroups - 1, groupIds.back());
}

TEST(PosixTests, SetProcessLimitsReportsFailuresThroughCallback)
{
   // a child between fork and exec hands setProcessLimits a syslog sink, so a
   // limit it cannot set has to reach that sink rather than the logger
   struct rlimit files;
   ASSERT_EQ(0, ::getrlimit(RLIMIT_NOFILE, &files));

   // the probes below only ever raise the soft limit, so this can put it back
   BOOST_SCOPE_EXIT(&files)
   {
      ::setrlimit(RLIMIT_NOFILE, &files);
   }
   BOOST_SCOPE_EXIT_END

   // both probes need a finite hard limit: the first sets the soft limit to it
   // (which some systems refuse for RLIM_INFINITY), and the second exceeds it
   if (files.rlim_max == RLIM_INFINITY)
      GTEST_SKIP() << "the hard file limit is unlimited here";

   std::vector<Error> reported;
   auto report = [&reported](const Error& error) { reported.push_back(error); };

   // raising the soft limit to the hard one is allowed of any process
   ProcessLimits limits;
   limits.filesLimit = files.rlim_max;
   setProcessLimits(limits, report);
   EXPECT_TRUE(reported.empty());

   // raising the hard limit is not, unless privileged
   if (::geteuid() == 0)
      GTEST_SKIP() << "raising the hard file limit would succeed here";

   limits.filesLimit = files.rlim_max + 1;
   setProcessLimits(limits, report);
   ASSERT_EQ(1u, reported.size());
   EXPECT_EQ(EPERM, reported[0].getCode());
}

TEST(PosixTests, PermanentlyDropPrivAfterForkReturnsInChild)
{
   // the drop must return in a forked child without any lookup; unprivileged,
   // the drop itself fails with EPERM and returning at all is what counts
   User user;
   ASSERT_FALSE(User::getCurrentUser(user));

   ResolvedUser resolved;
   ASSERT_FALSE(resolveUser(user.getUsername(), &resolved));

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      Error error = permanentlyDropPrivAfterFork(resolved);
      ::_exit(error && effectiveUserIsRoot() ? 1 : 0);
   }

   int status = 0;
   ASSERT_TRUE(waitForChildExit(child, &status, kChildTimeout)) << "the child hung dropping privilege";
   EXPECT_TRUE(WIFEXITED(status) && WEXITSTATUS(status) == 0) << "child status: " << status;
}

TEST_F(PosixTestsRequiresPrivilege, PermanentlyDropPrivAfterForkSetsIdsAndGroups)
{
   ResolvedUser resolved;
   ASSERT_FALSE(resolveUser(testUser.getUsername(), &resolved));
   ASSERT_FALSE(resolved.groupIds.empty());

   pid_t child = ::fork();
   ASSERT_NE(-1, child);
   if (child == 0)
   {
      if (permanentlyDropPrivAfterFork(resolved))
         ::_exit(1);

      if (::getuid() != testUser.getUserId() || ::geteuid() != testUser.getUserId())
         ::_exit(2);

      if (::getgid() != testUser.getGroupId() || ::getegid() != testUser.getGroupId())
         ::_exit(3);

      // the supplementary groups are the resolved ones (as many as the kernel takes)
      gid_t groups[NGROUPS_MAX];
      int numGroups = ::getgroups(NGROUPS_MAX, groups);
      if (numGroups < 0)
         ::_exit(4);

      std::size_t numExpected = std::min<std::size_t>(resolved.groupIds.size(), NGROUPS_MAX);
      for (std::size_t i = 0; i < numExpected; i++)
      {
         if (std::find(groups, groups + numGroups, resolved.groupIds[i]) == groups + numGroups)
            ::_exit(5);
      }

      ::_exit(0);
   }

   int status = 0;
   ASSERT_TRUE(waitForChildExit(child, &status, kChildTimeout)) << "the child hung dropping privilege";
   EXPECT_TRUE(WIFEXITED(status) && WEXITSTATUS(status) == 0) << "child status: " << status;
}

} // namespace tests
} // namespace system
} // namespace core
} // namespace rstudio

#endif // _WIN32
