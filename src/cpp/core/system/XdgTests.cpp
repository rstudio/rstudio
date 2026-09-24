/*
 * XdgTests.cpp
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

#include <gtest/gtest.h>

#ifndef _WIN32

#include <boost/filesystem.hpp>

#include <sys/stat.h>
#include <unistd.h>

#include <vector>

#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>
#include <core/system/Environment.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace xdg {
namespace tests {

namespace {

boost::optional<std::string> s_defaultUser("default");
boost::optional<FilePath>    s_defaultHome("/tmp/default");

// RAII guard that unsets a set of environment variables, restoring their original values (or
// leaving them unset) when the guard goes out of scope. Counterpart to EnvironmentScope (in
// core/system/Environment.hpp), which overrides a variable to a specific value - this instead
// guarantees a variable is absent for the scope's duration, regardless of what the ambient
// environment happens to have set.
class ScopedEnvUnset : boost::noncopyable
{
public:
   explicit ScopedEnvUnset(std::vector<std::string> variables) : variables_(std::move(variables))
   {
      for (const auto& variable : variables_)
      {
         std::string previousValue;
         hadValue_.push_back(core::system::getenv(variable, &previousValue));
         previousValues_.push_back(previousValue);
         core::system::unsetenv(variable);
      }
   }

   ~ScopedEnvUnset()
   {
      for (size_t i = 0; i < variables_.size(); ++i)
      {
         if (hadValue_[i])
            core::system::setenv(variables_[i], previousValues_[i]);
         else
            core::system::unsetenv(variables_[i]);
      }
   }

private:
   std::vector<std::string> variables_;
   std::vector<std::string> previousValues_;
   std::vector<bool> hadValue_;
};

} // end anonymous namespace

TEST(XdgTest, DirectoryResolution)
{
   // userConfigDir/userDataDir/userCacheDir consult these env vars ahead of any explicitly-supplied
   // home directory (see EnvironmentOverrides below), so clear them here - otherwise this test's
   // explicit-home-dir assertions are at the mercy of whatever the ambient environment happens to
   // have set for them.
   ScopedEnvUnset scopedEnvUnset({"RSTUDIO_CONFIG_HOME", "XDG_CONFIG_HOME", "RSTUDIO_DATA_HOME",
                                  "XDG_DATA_HOME", "RSTUDIO_CACHE_HOME", "XDG_CACHE_HOME"});

   FilePath homePath(core::system::getenv("HOME"));

   EXPECT_EQ(homePath.completeChildPath(".config/rstudio"), userConfigDir());
   EXPECT_EQ(homePath.completeChildPath(".config/rstudio"), userConfigDir(s_defaultUser));
   EXPECT_EQ(FilePath("/tmp/default/.config/rstudio"), userConfigDir(s_defaultUser, s_defaultHome));
   
   EXPECT_EQ(homePath.completeChildPath(".local/share/rstudio"), userDataDir());
   EXPECT_EQ(homePath.completeChildPath(".local/share/rstudio"), userDataDir(s_defaultUser));
   EXPECT_EQ(FilePath("/tmp/default/.local/share/rstudio"), userDataDir(s_defaultUser, s_defaultHome));

   EXPECT_EQ(homePath.completeChildPath(".cache/rstudio"), userCacheDir());
   EXPECT_EQ(homePath.completeChildPath(".cache/rstudio"), userCacheDir(s_defaultUser));
   EXPECT_EQ(FilePath("/tmp/default/.cache/rstudio"), userCacheDir(s_defaultUser, s_defaultHome));
}
   

TEST(XdgTest, EnvironmentOverrides)
{
   // Test config home overrides
   {
      EnvironmentScope scope("RSTUDIO_CONFIG_HOME", "/tmp/rstudio/config");
      EXPECT_EQ(FilePath("/tmp/rstudio/config"), userConfigDir());
      EXPECT_EQ(FilePath("/tmp/rstudio/config"), userConfigDir(s_defaultUser));
      EXPECT_EQ(FilePath("/tmp/rstudio/config"), userConfigDir(s_defaultUser, s_defaultHome));
   }

   // Test data home overrides
   {
      EnvironmentScope scope("RSTUDIO_DATA_HOME", "/tmp/rstudio/data");
      EXPECT_EQ(FilePath("/tmp/rstudio/data"), userDataDir());
      EXPECT_EQ(FilePath("/tmp/rstudio/data"), userDataDir(s_defaultUser));
      EXPECT_EQ(FilePath("/tmp/rstudio/data"), userDataDir(s_defaultUser, s_defaultHome));
   }

   // Test cache home overrides
   {
      EnvironmentScope scope("RSTUDIO_CACHE_HOME", "/tmp/rstudio/cache");
      EXPECT_EQ(FilePath("/tmp/rstudio/cache"), userCacheDir());
      EXPECT_EQ(FilePath("/tmp/rstudio/cache"), userCacheDir(s_defaultUser));
      EXPECT_EQ(FilePath("/tmp/rstudio/cache"), userCacheDir(s_defaultUser, s_defaultHome));
   }
}
   

TEST(XdgTest, UsesExistingXdgConfigDir)
{
   // set up XDG directories in tempdir
   char templateString[] = "/tmp/rstudio-XXXXXX";
   char* testDir = ::mkdtemp(templateString);
   std::string xdgConfigA = fmt::format("{}/xdg-a", testDir);
   std::string xdgConfigB = fmt::format("{}/xdg-b", testDir);
   std::string xdgConfigDirs = fmt::format("{}:{}", xdgConfigA, xdgConfigB);
   
   EnvironmentScope scope("XDG_CONFIG_DIRS", xdgConfigDirs.c_str());
   // None of the XDG_CONFIG_DIRS exist; use fallback default
   EXPECT_EQ(FilePath("/etc/rstudio"), systemConfigDir());
   
   // An rstudio directory exists in the XDG_CONFIG_DIRS path; use it
   EXPECT_TRUE(boost::filesystem::create_directories(xdgConfigB + "/rstudio"));
   EXPECT_EQ(FilePath(xdgConfigB + "/rstudio"), systemConfigDir());
   
   // An rstudio directory exists in the XDG_CONFIG_DIRS path; use it
   EXPECT_TRUE(boost::filesystem::create_directories(xdgConfigA + "/rstudio"));
   EXPECT_EQ(FilePath(xdgConfigA + "/rstudio"), systemConfigDir());
   
   // clean up
   boost::filesystem::remove_all(testDir);
}

TEST(XdgTest, SystemConfigFileSearch)
{
   // set up XDG directories in tempdir
   char templateString[] = "/tmp/rstudio-XXXXXX";
   char* testDir = ::mkdtemp(templateString);
   std::string xdgConfigA = fmt::format("{}/xdg-a", testDir);
   std::string xdgConfigB = fmt::format("{}/xdg-b", testDir);
   std::string xdgConfigDirs = fmt::format("{}:{}", xdgConfigA, xdgConfigB);

   EnvironmentScope scope("XDG_CONFIG_DIRS", xdgConfigDirs.c_str());

   // Find logging.conf in default path
   EXPECT_EQ(FilePath("/etc/rstudio/logging.conf"), systemConfigFile("logging.conf"));

   // Even if one of the XDG directories exist, we ignore it since it doesn't contain
   // the logging.conf file we're looking for.
   boost::filesystem::create_directories(xdgConfigB);
   EXPECT_EQ(FilePath("/etc/rstudio/logging.conf"), systemConfigFile("logging.conf"));

   // The 'rstudio' directory exists in one of the XDG config directories, but it
   // does not contain the system config file we're searching for, so it's skipped
   FilePath rstudioXdgConfigB = FilePath(xdgConfigB).completePath("rstudio");
   rstudioXdgConfigB.ensureDirectory();
   EXPECT_EQ(FilePath("/etc/rstudio/logging.conf"), systemConfigFile("logging.conf"));

   // If we create the file now, it should be used.
   FilePath logFile = FilePath(xdgConfigB).completePath("rstudio/logging.conf");
   EXPECT_EQ(Success(), logFile.getParent().ensureDirectory());
   EXPECT_EQ(Success(), logFile.ensureFile());
   EXPECT_EQ(logFile, systemConfigFile("logging.conf"));

   // If RSTUDIO_CONFIG_DIR is set, then that should be used, even if the logging.conf
   // file does not yet exist in that directory. That is, RSTUDIO_CONFIG_DIR overrides.
   EnvironmentScope scope2("RSTUDIO_CONFIG_DIR", testDir);
   EXPECT_EQ(FilePath(testDir).completePath("logging.conf"), systemConfigFile("logging.conf"));

   // clean up
   boost::filesystem::remove_all(testDir);
}

namespace {

// A unique directory for a single test, removed when the test finishes.
class ScopedTestDir : boost::noncopyable
{
public:
   ScopedTestDir()
   {
      EXPECT_FALSE(FilePath::tempFilePath(path_));
      EXPECT_FALSE(path_.ensureDirectory());
   }

   ~ScopedTestDir()
   {
      EXPECT_FALSE(path_.removeIfExists());
   }

   const FilePath& path() const
   {
      return path_;
   }

private:
   FilePath path_;
};

mode_t fileMode(const FilePath& path)
{
   struct stat info;
   EXPECT_EQ(0, ::lstat(path.getAbsolutePath().c_str(), &info));
   return info.st_mode & 07777;
}

} // anonymous namespace

TEST(XdgTest, CheckDirectoryWritable)
{
   ScopedTestDir testDir;

   EXPECT_FALSE(checkDirectoryWritable(testDir.path()));

   // a missing directory is created, and no probe file is left behind
   FilePath missing = testDir.path().completePath("a/b");
   EXPECT_FALSE(checkDirectoryWritable(missing));
   EXPECT_TRUE(missing.isDirectory());
   std::vector<FilePath> children;
   EXPECT_FALSE(missing.getChildren(children));
   EXPECT_TRUE(children.empty());

   FilePath file = testDir.path().completePath("file");
   ASSERT_FALSE(file.ensureFile());
   EXPECT_TRUE(checkDirectoryWritable(file));

   // root can write regardless of permission bits
   if (::geteuid() != 0)
   {
      FilePath readOnly = testDir.path().completePath("read-only");
      ASSERT_FALSE(readOnly.ensureDirectory());
      ASSERT_EQ(0, ::chmod(readOnly.getAbsolutePath().c_str(), 0555));
      EXPECT_TRUE(checkDirectoryWritable(readOnly));
      ASSERT_EQ(0, ::chmod(readOnly.getAbsolutePath().c_str(), 0755));
   }
}

TEST(XdgTest, TemporaryUserDataDir)
{
   ScopedTestDir testDir;
   EnvironmentScope scope("TMPDIR", testDir.path().getAbsolutePath().c_str());
   FilePath expected = testDir.path().completePath("rstudio-data-" + username());

   FilePath temporaryDir;
   ASSERT_FALSE(temporaryUserDataDir(&temporaryDir));
   EXPECT_EQ(expected.getAbsolutePath(), temporaryDir.getAbsolutePath());
   EXPECT_TRUE(temporaryDir.isDirectory());
   EXPECT_EQ(0700, fileMode(temporaryDir));

   // an existing directory is reused, and made private again
   ASSERT_EQ(0, ::chmod(expected.getAbsolutePath().c_str(), 0755));
   ASSERT_FALSE(temporaryUserDataDir(&temporaryDir));
   EXPECT_EQ(expected.getAbsolutePath(), temporaryDir.getAbsolutePath());
   EXPECT_EQ(0700, fileMode(temporaryDir));

   // anything else at that path is refused, including a symlink to a directory
   ASSERT_FALSE(expected.remove());
   FilePath target = testDir.path().completePath("target");
   ASSERT_FALSE(target.ensureDirectory());
   ASSERT_EQ(0, ::symlink(target.getAbsolutePath().c_str(), expected.getAbsolutePath().c_str()));
   EXPECT_TRUE(temporaryUserDataDir(&temporaryDir));

   ASSERT_EQ(0, ::unlink(expected.getAbsolutePath().c_str()));
   ASSERT_FALSE(expected.ensureFile());
   EXPECT_TRUE(temporaryUserDataDir(&temporaryDir));
}

TEST(XdgTest, RedirectUnwritableUserDataDir)
{
   // root can write regardless of permission bits
   if (::geteuid() == 0)
      GTEST_SKIP() << "permission checks don't apply to root";

   ScopedTestDir testDir;
   FilePath dataDir = testDir.path().completePath("data");
   ASSERT_FALSE(dataDir.ensureDirectory());
   EnvironmentScope tmpScope("TMPDIR", testDir.path().getAbsolutePath().c_str());

   {
      EnvironmentScope dataScope("RSTUDIO_DATA_HOME", dataDir.getAbsolutePath().c_str());

      // a writable data directory is left alone
      FilePath temporaryDir;
      Error temporaryDirError;
      EXPECT_FALSE(redirectUnwritableUserDataDir(&temporaryDir, &temporaryDirError));
      EXPECT_TRUE(temporaryDir.isEmpty());
      EXPECT_EQ(dataDir.getAbsolutePath(), userDataDir().getAbsolutePath());
      EXPECT_FALSE(isUserDataDirTemporary());

      // an unwritable one is replaced by the temporary directory
      ASSERT_EQ(0, ::chmod(dataDir.getAbsolutePath().c_str(), 0555));
      Error error = redirectUnwritableUserDataDir(&temporaryDir, &temporaryDirError);
      ASSERT_EQ(0, ::chmod(dataDir.getAbsolutePath().c_str(), 0755));

      EXPECT_TRUE(error);
      EXPECT_FALSE(temporaryDirError);
      FilePath expected = testDir.path().completePath("rstudio-data-" + username());
      EXPECT_EQ(expected.getAbsolutePath(), temporaryDir.getAbsolutePath());
      EXPECT_EQ(expected.getAbsolutePath(), userDataDir().getAbsolutePath());
      EXPECT_TRUE(isUserDataDirTemporary());
   }

   // the redirect ends with the environment that carried it, so it doesn't
   // outlive this test
   EXPECT_FALSE(isUserDataDirTemporary());
}

} // namespace tests
} // namespace xdg
} // namespace system
} // namespace core
} // namespace rstudio

#endif // _WIN32