/*
 * SessionConsoleProcessPersistTests.cpp
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
#include <session/SessionConsoleProcessPersist.hpp>


#include <gtest/gtest.h>

#include <sstream>

#include <core/system/Environment.hpp>

namespace rstudio {
namespace session {
namespace console_process {
namespace tests {

const std::string handle1("unit-test01");
const std::string handle2("unit-test02");
const std::string bogusHandle1("unit-test03");
const std::string bogusHandle2("unit-test04");

const size_t maxLines = 1000;

namespace {

bool testHandle(const std::string& handle)
{
   return !handle.compare(handle1) || !handle.compare(handle2);
}

// These tests are running against an actual session, so try to save and
// restore the Console persistence metadata so local client usage isn't
// impacted by running the tests.
class CleanupPeristTest : public ::testing::Test
{
public:
   CleanupPeristTest()
      : origMetadata_(console_persist::loadConsoleProcessMetadata())
   {
      console_persist::deleteLogFile(handle1);
      console_persist::deleteLogFile(handle2);
   }

   ~CleanupPeristTest()
   {
      console_persist::saveConsoleProcesses(origMetadata_);
      console_persist::deleteLogFile(handle1);
      console_persist::deleteLogFile(handle2);
   }

private:
   std::string origMetadata_;
};

} // anonymous namespace

TEST_F(CleanupPeristTest, MetadataIsSavedAndRestoredCorrectly) {
   std::string orig("Hello World this is some text.");
   console_persist::saveConsoleProcesses(orig);

   std::string loaded = console_persist::loadConsoleProcessMetadata();
   EXPECT_EQ(orig, loaded);
}

TEST_F(CleanupPeristTest, EmptyMetadataIsSavedAndRestoredCorrectly) {
   std::string orig;
   console_persist::saveConsoleProcesses(orig);

   std::string loaded = console_persist::loadConsoleProcessMetadata();
   EXPECT_EQ(orig, loaded);
}

TEST_F(CleanupPeristTest, LoadingNonExistentBufferReturnsEmpty) {
   std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
   EXPECT_TRUE(loaded.empty());
}

TEST_F(CleanupPeristTest, DeletedBufferIsEmpty) {
   std::string orig = console_persist::getSavedBuffer(handle1, maxLines);
   EXPECT_TRUE(orig.empty());

   orig = "Once upon a time";
   console_persist::appendToOutputBuffer(handle1, orig);

   std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
   EXPECT_EQ(orig, loaded);

   console_persist::deleteLogFile(handle1);
   loaded = console_persist::getSavedBuffer(handle1, maxLines);
   EXPECT_TRUE(loaded.empty());
}

TEST_F(CleanupPeristTest, BufferWithoutNewlinesIsPreserved) {
   std::string orig("hello how are you?");
   console_persist::appendToOutputBuffer(handle1, orig);
   std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
   EXPECT_EQ(orig, loaded);
}

TEST_F(CleanupPeristTest, BufferWithSingleNewlineIsPreserved) {
   std::string orig("hello how are you?\n");
   console_persist::appendToOutputBuffer(handle1, orig);
   std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
   EXPECT_EQ(orig, loaded) << loaded << "!=" << orig;
}

TEST_F(CleanupPeristTest, BufferWithMultipleLinesIsPreserved) {
   std::string orig("hello how are you?\nthat is good\nhave a nice day");
   console_persist::appendToOutputBuffer(handle1, orig);
   std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
   EXPECT_EQ(orig, loaded) << loaded << "!=" << orig;
}

TEST_F(CleanupPeristTest, BufferExceedingMaxLinesIsTrimmed) {
   std::stringstream ss;
   std::stringstream ss_expect;
   ss_expect << '\n';
   for (size_t i = 0; i < maxLines * 2; i++) 
   {
      ss << i << '\n';
      if (i >= maxLines)
         ss_expect << i << '\n';
   }
   std::string expect = ss_expect.str();
   std::string orig = ss.str();
   console_persist::appendToOutputBuffer(handle2, orig);
   std::string loaded = console_persist::getSavedBuffer(handle2, maxLines);
   EXPECT_EQ(expect, loaded);
}

TEST_F(CleanupPeristTest, BufferIsNotTrimmedWithZeroMaxLines) {
   std::stringstream ss;
   std::stringstream ss_expect;
   for (size_t i = 0; i < maxLines * 2; i++) 
   {
      ss << i << '\n';
   }
   std::string expect = ss.str();
   std::string orig = ss.str();
   console_persist::appendToOutputBuffer(handle2, orig);
   std::string loaded = console_persist::getSavedBuffer(handle2, 0);
   EXPECT_EQ(expect, loaded) << loaded << "!=" << expect;
}

TEST_F(CleanupPeristTest, UnknownLogFilesAreDeleted) {
   std::string orig1("hello how are you?\nthat is good\nhave a nice day");
   std::string orig2("beware of ferret");
   std::string bogus1("doom");
   std::string bogus2("once upon a time");

   console_persist::appendToOutputBuffer(handle1, orig1);
   console_persist::appendToOutputBuffer(handle2, orig2);
   console_persist::appendToOutputBuffer(bogusHandle1, bogus1);
   console_persist::appendToOutputBuffer(bogusHandle2, bogus2);

   std::string loaded = console_persist::getSavedBuffer(bogusHandle1, maxLines);
   EXPECT_EQ(bogus1, loaded);
   loaded = console_persist::getSavedBuffer(bogusHandle2, maxLines);
   EXPECT_EQ(bogus2, loaded);

   console_persist::deleteOrphanedLogs(testHandle);
   loaded = console_persist::getSavedBuffer(bogusHandle1, maxLines);
   EXPECT_TRUE(loaded.empty());
   loaded = console_persist::getSavedBuffer(bogusHandle2, maxLines);
   EXPECT_TRUE(loaded.empty());
   loaded = console_persist::getSavedBuffer(handle1, maxLines);
   EXPECT_EQ(orig1, loaded) << loaded << "!=" << orig1;
   loaded = console_persist::getSavedBuffer(handle2, maxLines);
   EXPECT_EQ(orig2, loaded) << loaded << "!=" << orig2;
}

#ifndef _WIN32
TEST_F(CleanupPeristTest, EnvironmentIsSavedAndRestored) {
   core::system::Options env;
   core::system::environment(&env);
   EXPECT_FALSE(env.empty());

   console_persist::saveConsoleEnvironment(handle1, env);

   core::system::Options loadEnv;
   console_persist::loadConsoleEnvironment(handle1, &loadEnv);
   EXPECT_TRUE(loadEnv.size() == env.size());

   for (const core::system::Option& varOrig : env)
   {
      bool match = false;
      for (const core::system::Option& varLoaded : loadEnv)
      {
         if (varLoaded.first == varOrig.first)
         {
            if (varLoaded.second == varOrig.second)
            {
               match = true;
               break;
            }
         }
      }
      EXPECT_TRUE(match);
      if (!match)
         break;
   }

   console_persist::deleteEnvFile(handle1);
}
#endif

} // namespace tests
} // namespace console_process
} // namespace session
} // namespace rstudio
