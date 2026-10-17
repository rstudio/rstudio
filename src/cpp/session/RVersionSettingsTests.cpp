/*
 * RVersionSettingsTests.cpp
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


#include <session/RVersionSettings.hpp>

#include <core/system/Environment.hpp>
#include <core/tests/ScratchDir.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace {

class RVersionSettingsTests : public testing::Test
{
protected:
   void SetUp() override
   {
      settingsPath_ = core::tests::scratchDir();
   }

   void TearDown() override
   {
      EXPECT_FALSE(settingsPath_.remove());
   }

   void write(const std::string& id, const char* name, const std::string& value)
   {
      ASSERT_FALSE(core::writeStringToFile(settingsPath_.completePath(id + name), value));
   }

   void writeLegacy()
   {
      write(legacyId_, kRVersionSuffix, "4.4.3");
      write(legacyId_, kRVersionHomeSuffix, "/opt/R/4.4.3/lib/R");
      write(legacyId_, kRVersionLabelSuffix, "production");
   }

   void read(const std::string& legacyId)
   {
      detail::readProjectRVersionSettings(settingsPath_, currentId_, legacyId,
            &version_, &home_, &label_);
   }

   core::FilePath settingsPath_;
   const std::string currentId_ = "fffff12345678";
   const std::string legacyId_ = "1003e12345678";
   std::string version_, home_, label_;
};

TEST_F(RVersionSettingsTests, ReadsCompleteLegacyRecordAfterUidBoundaryChange)
{
   core::system::EnvironmentScope minUidScope("RSTUDIO_MINIMUM_USER_ID", "1000");
   const uid_t uid = 1000 + 0xFFFFF;
   ASSERT_EQ(currentId_, core::r_util::ProjectId("12345678", uid).asString());
   ASSERT_EQ(legacyId_, core::r_util::legacyObfuscatedUserId(uid) + "12345678");

   writeLegacy();
   read(legacyId_);
   EXPECT_EQ(version_, "4.4.3");
   EXPECT_EQ(home_, "/opt/R/4.4.3/lib/R");
   EXPECT_EQ(label_, "production");
   EXPECT_FALSE(settingsPath_.completePath(currentId_ + kRVersionSuffix).exists());
}

TEST_F(RVersionSettingsTests, CurrentRecordWinsIncludingEmptyLabel)
{
   writeLegacy();
   write(currentId_, kRVersionSuffix, "4.5.1");
   write(currentId_, kRVersionHomeSuffix, "/opt/R/4.5.1/lib/R");
   write(currentId_, kRVersionLabelSuffix, "");
   read(legacyId_);
   EXPECT_EQ(version_, "4.5.1");
   EXPECT_EQ(home_, "/opt/R/4.5.1/lib/R");
   EXPECT_TRUE(label_.empty());
}

TEST_F(RVersionSettingsTests, DoesNotMixPartialCurrentRecordWithLegacyFields)
{
   writeLegacy();
   write(currentId_, kRVersionSuffix, "4.5.1");
   read(legacyId_);
   EXPECT_EQ(version_, "4.5.1");
   EXPECT_TRUE(home_.empty());
   EXPECT_TRUE(label_.empty());
}

TEST_F(RVersionSettingsTests, EmptyCurrentSettingDoesNotRestoreLegacyRecord)
{
   writeLegacy();
   write(currentId_, kRVersionLabelSuffix, "");
   read(legacyId_);
   EXPECT_TRUE(version_.empty());
   EXPECT_TRUE(home_.empty());
   EXPECT_TRUE(label_.empty());
}

TEST_F(RVersionSettingsTests, UnchangedIdDoesNotReadAnotherUsersLegacyRecord)
{
   writeLegacy();
   read(currentId_);
   EXPECT_TRUE(version_.empty());
   EXPECT_TRUE(home_.empty());
   EXPECT_TRUE(label_.empty());
}

TEST_F(RVersionSettingsTests, MissingRecordsClearOutputs)
{
   version_ = home_ = label_ = "stale";
   read(legacyId_);
   EXPECT_TRUE(version_.empty());
   EXPECT_TRUE(home_.empty());
   EXPECT_TRUE(label_.empty());
}

TEST_F(RVersionSettingsTests, PublicReaderRetainsProjectFileFallback)
{
   core::FilePath projectScratch = settingsPath_.completePath("project-scratch");
   ASSERT_FALSE(projectScratch.ensureDirectory());
   ASSERT_FALSE(core::writeStringToFile(projectScratch.completePath(kRVersionProjectFile), "4.3.3"));

   RVersionSettings settings(settingsPath_, core::FilePath());
   settings.readProjectLastRVersion("test-project", projectScratch, &version_, &home_, &label_);
   EXPECT_EQ(version_, "4.3.3");
   EXPECT_TRUE(home_.empty());
   EXPECT_TRUE(label_.empty());
}

} // anonymous namespace
} // namespace session
} // namespace rstudio
