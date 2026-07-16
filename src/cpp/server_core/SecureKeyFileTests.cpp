/*
 * SecureKeyFileTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <string>
#include <utility>

#include <sys/stat.h>

#include <server_core/SecureKeyFile.hpp>

#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/Xdg.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server_core {

namespace {

// Sets an env var for the test body and restores the prior value on scope exit.
class ScopedEnv
{
public:
   ScopedEnv(std::string name, const std::string& value)
      : name_(std::move(name))
   {
      previous_ = core::system::getenv(name_);
      had_ = !previous_.empty();
      core::system::setenv(name_, value);
   }

   ~ScopedEnv()
   {
      if (had_)
         core::system::setenv(name_, previous_);
      else
         core::system::unsetenv(name_);
   }

private:
   std::string name_;
   std::string previous_;
   bool had_ = false;
};

} // anonymous namespace

// End-to-end regression for #18255. Skipped as root, where permission bits are
// not enforced and the fallback branch cannot be exercised.
TEST(SecureKeyFileTest, UnreadableSystemKeyFallsBackToCache)
{
   if (core::system::effectiveUserIsRoot())
      GTEST_SKIP() << "permission bits are not enforced for root";

   FilePath tempDir;
   ASSERT_FALSE(FilePath::tempFilePath(tempDir));
   ASSERT_FALSE(tempDir.ensureDirectory());

   FilePath configDir = tempDir.completeChildPath("config");
   ASSERT_FALSE(configDir.ensureDirectory());
   FilePath cacheDir = tempDir.completeChildPath("cache");
   ASSERT_FALSE(cacheDir.ensureDirectory());

   FilePath systemKey = configDir.completeChildPath("session-rpc-key");
   ASSERT_FALSE(writeStringToFile(systemKey, "unreadable-system-key"));
   ASSERT_EQ(0, ::chmod(systemKey.getAbsolutePath().c_str(), 0000));

   {
      ScopedEnv config("RSTUDIO_CONFIG_DIR", configDir.getAbsolutePath());
      ScopedEnv cache("XDG_CACHE_HOME", cacheDir.getAbsolutePath());
      // userCacheDir() resolves RSTUDIO_CACHE_HOME before XDG_CACHE_HOME.
      ScopedEnv cacheHome("RSTUDIO_CACHE_HOME", cacheDir.getAbsolutePath());

      std::string contents, hash, pathUsed;
      Error error = key_file::readSecureKeyFile(
            "session-rpc-key", &contents, &hash, &pathUsed);

      // Resolve the expected cache path through the same XDG logic the code
      // uses, so the comparison is immune to symlink canonicalization.
      FilePath expectedCacheKey =
            core::system::xdg::userCacheDir().completePath("session-rpc-key");

      EXPECT_FALSE(error);
      EXPECT_FALSE(contents.empty());
      EXPECT_NE(pathUsed, systemKey.getAbsolutePath());
      EXPECT_EQ(pathUsed, expectedCacheKey.getAbsolutePath());
   }

   ::chmod(systemKey.getAbsolutePath().c_str(), 0600);
   tempDir.removeIfExists();
}

// A readable system key is still shared, preserving #17543 behavior for
// non-root service-account deployments.
TEST(SecureKeyFileTest, ReadableSystemKeyIsUsed)
{
   FilePath tempDir;
   ASSERT_FALSE(FilePath::tempFilePath(tempDir));
   ASSERT_FALSE(tempDir.ensureDirectory());

   FilePath configDir = tempDir.completeChildPath("config");
   ASSERT_FALSE(configDir.ensureDirectory());
   FilePath cacheDir = tempDir.completeChildPath("cache");
   ASSERT_FALSE(cacheDir.ensureDirectory());

   FilePath systemKey = configDir.completeChildPath("session-rpc-key");
   const std::string sharedKey = "shared-service-account-key";
   ASSERT_FALSE(writeStringToFile(systemKey, sharedKey));

   {
      ScopedEnv config("RSTUDIO_CONFIG_DIR", configDir.getAbsolutePath());
      ScopedEnv cache("XDG_CACHE_HOME", cacheDir.getAbsolutePath());
      // userCacheDir() resolves RSTUDIO_CACHE_HOME before XDG_CACHE_HOME.
      ScopedEnv cacheHome("RSTUDIO_CACHE_HOME", cacheDir.getAbsolutePath());

      std::string contents, hash, pathUsed;
      Error error = key_file::readSecureKeyFile(
            "session-rpc-key", &contents, &hash, &pathUsed);

      EXPECT_FALSE(error);
      EXPECT_EQ(contents, sharedKey);
      EXPECT_EQ(pathUsed, systemKey.getAbsolutePath());
   }

   tempDir.removeIfExists();
}

} // namespace server_core
} // namespace rstudio
