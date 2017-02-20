/*
 * SessionConsoleProcessPersistTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#include <session/SessionConsoleProcessPersist.hpp>

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>
#include <sstream>

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
class CleanupPeristTest
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

TEST_CASE("ConsoleProcess Persistence")
{
   CleanupPeristTest cleanup;

   SECTION("Save and restore Console Process metadata")
   {
      std::string orig("Hello World this is some text.");
      console_persist::saveConsoleProcesses(orig);

      std::string loaded = console_persist::loadConsoleProcessMetadata();
      CHECK(loaded.compare(orig) == 0);
   }

   SECTION("Save and restore empty Console Process metadata")
   {
      std::string orig;
      console_persist::saveConsoleProcesses(orig);

      std::string loaded = console_persist::loadConsoleProcessMetadata();
      CHECK(loaded.compare(orig) == 0);
   }

   SECTION("Try to load a non-existent buffer")
   {
      std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(loaded.empty());
   }

   SECTION("Verify that a deleted buffer is empty")
   {
      std::string orig = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(orig.empty());

      orig = "Once upon a time";
      console_persist::appendToOutputBuffer(handle1, orig);

      std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(loaded.compare(orig) == 0);

      console_persist::deleteLogFile(handle1);
      loaded = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(loaded.empty());
   }

   SECTION("Write and load a buffer with no newlines")
   {
      std::string orig("hello how are you?");
      console_persist::appendToOutputBuffer(handle1, orig);
      std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(loaded.compare(orig) == 0);
   }

   SECTION("Write and load a buffer with one newline")
   {
      std::string orig("hello how are you?\n");
      console_persist::appendToOutputBuffer(handle1, orig);
      std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(loaded.compare(orig) == 0);
   }

   SECTION("Write and load several lines")
   {
      std::string orig("hello how are you?\nthat is good\nhave a nice day");
      console_persist::appendToOutputBuffer(handle1, orig);
      std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(loaded.compare(orig) == 0);
   }

   SECTION("Write more lines than maxLines then read it")
   {
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
      CHECK(loaded.compare(expect) == 0);
   }

   SECTION("Write more lines than maxLines then read it without trimming")
   {
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
      CHECK(loaded.compare(expect) == 0);
   }

   SECTION("Delete unknown log files")
   {
      std::string orig1("hello how are you?\nthat is good\nhave a nice day");
      std::string orig2("beware of ferret");
      std::string bogus1("doom");
      std::string bogus2("once upon a time");

      console_persist::appendToOutputBuffer(handle1, orig1);
      console_persist::appendToOutputBuffer(handle2, orig2);
      console_persist::appendToOutputBuffer(bogusHandle1, bogus1);
      console_persist::appendToOutputBuffer(bogusHandle2, bogus2);

      std::string loaded = console_persist::getSavedBuffer(bogusHandle1, maxLines);
      CHECK(loaded.compare(bogus1) == 0);
      loaded = console_persist::getSavedBuffer(bogusHandle2, maxLines);
      CHECK(loaded.compare(bogus2) == 0);

      console_persist::deleteOrphanedLogs(testHandle);
      loaded = console_persist::getSavedBuffer(bogusHandle1, maxLines);
      CHECK(loaded.empty());
      loaded = console_persist::getSavedBuffer(bogusHandle2, maxLines);
      CHECK(loaded.empty());
      loaded = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(loaded.compare(orig1) == 0);
      loaded = console_persist::getSavedBuffer(handle2, maxLines);
      CHECK(loaded.compare(orig2) == 0);
   }
}

} // end namespace tests
} // end namespace console_process
} // end namespace session
} // end namespace rstudio
