/*
 * SessionTrustTests.cpp
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

#include "SessionTrust.hpp"

#include <memory>
#include <string>
#include <vector>

#ifndef _WIN32
#include <sys/stat.h>
#include <unistd.h>
#endif

#include <gtest/gtest.h>

#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace trust {
namespace {

// trust.json lives in the user data directory, so point that at a
// temporary directory for the duration of each test
class SessionTrustTest : public testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(dataHome_));
      ASSERT_FALSE(dataHome_.ensureDirectory());
      dataHomeScope_.reset(new core::system::EnvironmentScope(
         "RSTUDIO_DATA_HOME",
         dataHome_.getAbsolutePath().c_str()));

      project_ = dataHome_.completePath("project");
      ASSERT_FALSE(project_.ensureDirectory());
   }

   void TearDown() override
   {
      dataHomeScope_.reset();
      EXPECT_FALSE(dataHome_.removeIfExists());
   }

   FilePath trustFile() const
   {
      return dataHome_.completePath("trust.json");
   }

   std::vector<std::string> readList(const std::string& key) const
   {
      std::vector<std::string> result;

      std::string contents;
      Error error = readStringFromFile(trustFile(), &contents);
      EXPECT_FALSE(error);

      json::Value value;
      EXPECT_FALSE(value.parse(contents));
      if (!value.isObject())
         return result;

      json::Object object = value.getObject();
      auto it = object.find(key);
      if (it == object.end() || !(*it).getValue().isArray())
         return result;

      for (const json::Value& entry : (*it).getValue().getArray())
      {
         if (entry.isString())
            result.push_back(entry.getString());
      }
      return result;
   }

   FilePath dataHome_;
   std::unique_ptr<core::system::EnvironmentScope> dataHomeScope_;
   FilePath project_;
};

TEST_F(SessionTrustTest, GrantReplacesUnparseableTrustFile)
{
   const std::string invalid = "{ \"trustedDirectories\": [";
   ASSERT_FALSE(writeStringToFile(trustFile(), invalid));

   EXPECT_FALSE(grantTrust(project_));

   std::vector<std::string> expected = { project_.getCanonicalPath() };
   EXPECT_EQ(expected, readList("trustedDirectories"));
   EXPECT_TRUE(readList("untrustedDirectories").empty());

   // the replaced contents are kept, to be recovered by hand
   std::string backup;
   EXPECT_FALSE(readStringFromFile(dataHome_.completePath("trust.json.invalid"), &backup));
   EXPECT_EQ(invalid, backup);
}

TEST_F(SessionTrustTest, SuccessiveRecoveriesKeepEarlierCopies)
{
   ASSERT_FALSE(writeStringToFile(trustFile(), "first"));
   EXPECT_FALSE(grantTrust(project_));

   ASSERT_FALSE(writeStringToFile(trustFile(), "second"));
   EXPECT_FALSE(grantTrust(project_));

   std::string backup;
   EXPECT_FALSE(readStringFromFile(dataHome_.completePath("trust.json.invalid"), &backup));
   EXPECT_EQ("first", backup);
   EXPECT_FALSE(readStringFromFile(dataHome_.completePath("trust.json.invalid-2"), &backup));
   EXPECT_EQ("second", backup);
}

#ifndef _WIN32
TEST_F(SessionTrustTest, GrantLeavesInvalidTrustFileItCannotKeep)
{
   if (::geteuid() == 0)
      GTEST_SKIP() << "root bypasses file permissions";

   const std::string invalid = "{ \"trustedDirectories\": [";
   ASSERT_FALSE(writeStringToFile(trustFile(), invalid));

   // the backup can't be created next to the trust file
   ASSERT_EQ(0, ::chmod(dataHome_.getAbsolutePath().c_str(), 0555));
   Error error = grantTrust(project_);
   ASSERT_EQ(0, ::chmod(dataHome_.getAbsolutePath().c_str(), 0755));

   EXPECT_TRUE(error);
   std::string contents;
   EXPECT_FALSE(readStringFromFile(trustFile(), &contents));
   EXPECT_EQ(invalid, contents);
}
#endif

TEST_F(SessionTrustTest, RevokeReplacesTrustFileWithWrongShape)
{
   ASSERT_FALSE(writeStringToFile(trustFile(), "[]"));

   EXPECT_FALSE(revokeTrust(project_));

   std::vector<std::string> expected = { project_.getCanonicalPath() };
   EXPECT_EQ(expected, readList("untrustedDirectories"));
   EXPECT_TRUE(readList("trustedDirectories").empty());
}

TEST_F(SessionTrustTest, ResetReplacesEmptyTrustFile)
{
   ASSERT_FALSE(writeStringToFile(trustFile(), ""));

   EXPECT_FALSE(resetTrust(project_));

   EXPECT_TRUE(readList("trustedDirectories").empty());
   EXPECT_TRUE(readList("untrustedDirectories").empty());
}

TEST_F(SessionTrustTest, GrantKeepsExistingEntries)
{
   std::string other = dataHome_.completePath("other").getAbsolutePath();
   ASSERT_FALSE(writeStringToFile(
      trustFile(),
      "{ \"trustedDirectories\": [\"" + other + "\"], \"untrustedDirectories\": [] }"));

   EXPECT_FALSE(grantTrust(project_));

   std::vector<std::string> expected = { other, project_.getCanonicalPath() };
   EXPECT_EQ(expected, readList("trustedDirectories"));
}

} // anonymous namespace
} // namespace trust
} // namespace modules
} // namespace session
} // namespace rstudio
