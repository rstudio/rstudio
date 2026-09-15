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

} // namespace tests
} // namespace xdg
} // namespace system
} // namespace core
} // namespace rstudio

#endif // _WIN32