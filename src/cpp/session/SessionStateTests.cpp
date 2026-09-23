/*
 * SessionStateTests.cpp
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

#include <r/session/RSessionState.hpp>

#include <string>

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {
namespace tests {

namespace {

const char* const kContentsFile = "contents";

class StateRestoreTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(root_));
      statePath_ = root_.completeChildPath("suspended-session-data");
      writeState("first");
   }

   void TearDown() override
   {
      EXPECT_FALSE(root_.removeIfExists());
   }

   void writeState(const std::string& contents) const
   {
      ASSERT_FALSE(statePath_.ensureDirectory());
      ASSERT_FALSE(writeStringToFile(statePath_.completeChildPath(kContentsFile), contents));
   }

   std::string readContents(const FilePath& statePath) const
   {
      std::string contents;
      EXPECT_FALSE(readStringFromFile(statePath.completeChildPath(kContentsFile), &contents));
      return contents;
   }

   FilePath setAsidePath(const std::string& suffix = std::string()) const
   {
      return root_.completeChildPath("suspended-session-data-unrestored" + suffix);
   }

   FilePath root_;
   FilePath statePath_;
};

} // anonymous namespace

TEST_F(StateRestoreTest, UnfinishedRestoreIsSetAside)
{
   state::restoreStarted(statePath_);

   std::string message = state::setAsideUnfinishedRestore(statePath_);

   EXPECT_FALSE(statePath_.exists());
   EXPECT_EQ("first", readContents(setAsidePath()));
   EXPECT_NE(std::string::npos, message.find(setAsidePath().getAbsolutePath()));
}

TEST_F(StateRestoreTest, FinishedRestoreIsLeftInPlace)
{
   // never restored
   EXPECT_EQ("", state::setAsideUnfinishedRestore(statePath_));
   EXPECT_TRUE(statePath_.exists());

   state::restoreStarted(statePath_);
   state::restoreFinished(statePath_);

   EXPECT_EQ("", state::setAsideUnfinishedRestore(statePath_));
   EXPECT_EQ("first", readContents(statePath_));
   EXPECT_FALSE(setAsidePath().exists());
}

TEST_F(StateRestoreTest, LaterSetAsideKeepsEarlierOne)
{
   state::restoreStarted(statePath_);
   ASSERT_NE("", state::setAsideUnfinishedRestore(statePath_));

   // state saved later fails to restore the same way
   writeState("second");
   state::restoreStarted(statePath_);
   std::string message = state::setAsideUnfinishedRestore(statePath_);

   EXPECT_EQ("first", readContents(setAsidePath()));
   EXPECT_EQ("second", readContents(setAsidePath("-2")));
   EXPECT_NE(std::string::npos, message.find(setAsidePath("-2").getAbsolutePath()));
}

TEST_F(StateRestoreTest, SavingStateClearsUnfinishedRestore)
{
   // the state is replaced by the save, so how an earlier restore of it went
   // no longer matters
   state::restoreStarted(statePath_);
   state::saveMinimal(statePath_, "", "", false);

   EXPECT_EQ("", state::setAsideUnfinishedRestore(statePath_));
   EXPECT_TRUE(statePath_.exists());
   EXPECT_FALSE(setAsidePath().exists());
}

} // namespace tests
} // namespace session
} // namespace r
} // namespace rstudio
