/*
 * SessionStartupWarningTests.cpp
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

#include <session/SessionStartupWarning.hpp>

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace tests {

class StartupWarningTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(stateFile_));
   }

   void TearDown() override
   {
      EXPECT_FALSE(stateFile_.removeIfExists());
   }

   void suspend(const StartupWarning& warning)
   {
      Settings settings;
      ASSERT_FALSE(settings.initialize(stateFile_));
      warning.onSuspend(&settings);
   }

   void resume(StartupWarning* pWarning)
   {
      Settings settings;
      ASSERT_FALSE(settings.initialize(stateFile_));
      pWarning->onResume(settings);
   }

   FilePath stateFile_;
};

TEST_F(StartupWarningTest, UnchangedWarningIsNotRepeatedAcrossResumes)
{
   // Exercise the settings file round trip, including multiline warnings,
   // quoted paths, and backslashes as used by the Windows startup warnings.
   const std::string message = R"(WARNING: Cannot use "C:\Users\test".

Using a temporary directory instead.)";
   StartupWarning warning("warning");
   EXPECT_TRUE(warning.shouldShow(message));
   suspend(warning);

   for (int i = 0; i < 3; ++i)
   {
      StartupWarning resumed("warning");
      resume(&resumed);
      EXPECT_FALSE(resumed.shouldShow(message));
      suspend(resumed);
   }
}

TEST_F(StartupWarningTest, NewAndChangedWarningsAreShownAfterResume)
{
   StartupWarning warning("warning");
   EXPECT_FALSE(warning.shouldShow(""));
   suspend(warning);

   StartupWarning resumed("warning");
   resume(&resumed);
   EXPECT_TRUE(resumed.shouldShow("WARNING: Cannot use /first/path"));
   suspend(resumed);

   StartupWarning changed("warning");
   resume(&changed);
   EXPECT_TRUE(changed.shouldShow("WARNING: Cannot use /second/path"));
   suspend(changed);

   StartupWarning unchanged("warning");
   resume(&unchanged);
   EXPECT_FALSE(unchanged.shouldShow("WARNING: Cannot use /second/path"));
}

TEST_F(StartupWarningTest, ResolvedWarningIsShownIfItRecurs)
{
   StartupWarning warning("warning");
   EXPECT_TRUE(warning.shouldShow("WARNING: Cannot use /path"));
   suspend(warning);

   StartupWarning resolved("warning");
   resume(&resolved);
   EXPECT_FALSE(resolved.shouldShow(""));
   suspend(resolved);

   StartupWarning recurred("warning");
   resume(&recurred);
   EXPECT_TRUE(recurred.shouldShow("WARNING: Cannot use /path"));
}

TEST_F(StartupWarningTest, NewSessionDoesNotInheritOldWarnings)
{
   StartupWarning warning("warning");
   EXPECT_TRUE(warning.shouldShow("WARNING: Cannot use /path"));
   suspend(warning);

   // New sessions do not run the resume hook, even if settings still exist.
   StartupWarning newSession("warning");
   EXPECT_TRUE(newSession.shouldShow("WARNING: Cannot use /path"));
}

TEST_F(StartupWarningTest, ResumeWithoutSavedWarningStillShowsIt)
{
   StartupWarning warning("warning");
   resume(&warning);
   EXPECT_TRUE(warning.shouldShow("WARNING: Cannot use /path"));
}

TEST_F(StartupWarningTest, WarningConditionsAreIndependent)
{
   StartupWarning workingDirectory("working-directory");
   StartupWarning temporaryDataDirectory("temporary-data-directory");
   EXPECT_TRUE(workingDirectory.shouldShow("WARNING: Cannot use /path"));
   EXPECT_TRUE(temporaryDataDirectory.shouldShow("WARNING: Cannot use /path"));
   suspend(workingDirectory);
   suspend(temporaryDataDirectory);

   StartupWarning resumedWorkingDirectory("working-directory");
   StartupWarning resumedDataDirectory("temporary-data-directory");
   resume(&resumedWorkingDirectory);
   resume(&resumedDataDirectory);
   EXPECT_FALSE(resumedWorkingDirectory.shouldShow("WARNING: Cannot use /path"));
   EXPECT_TRUE(resumedDataDirectory.shouldShow("WARNING: Cannot use /other/path"));
   suspend(resumedWorkingDirectory);
   suspend(resumedDataDirectory);

   StartupWarning nextWorkingDirectory("working-directory");
   StartupWarning nextDataDirectory("temporary-data-directory");
   resume(&nextWorkingDirectory);
   resume(&nextDataDirectory);
   EXPECT_FALSE(nextWorkingDirectory.shouldShow("WARNING: Cannot use /path"));
   EXPECT_FALSE(nextDataDirectory.shouldShow("WARNING: Cannot use /other/path"));
}

} // namespace tests
} // namespace session
} // namespace rstudio
