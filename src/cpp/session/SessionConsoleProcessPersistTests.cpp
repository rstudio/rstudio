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

namespace rstudio {
namespace session {
namespace console_process {
namespace tests {

const std::string handle1("unit-test01");
const std::string handle2("unit-test02");
const size_t maxLines = 1000;

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

   SECTION("Write and load a buffer with no newlines")
   {
      std::string orig("hello how are you?");
      console_persist::appendToOutputBuffer(handle1, orig);
      std::string loaded = console_persist::getSavedBuffer(handle1, maxLines);
      CHECK(loaded.compare(orig) == 0);
   }


/*
// Add to the saved buffer for the given ConsoleProcess
void appendToOutputBuffer(const std::string& handle, const std::string& buffer);

// Delete the persisted saved buffer for the given ConsoleProcess
void deleteLogFile(const std::string& handle);

// Clean up ConsoleProcess buffer cache
// Takes a function to see if a given handle represents a known process.
void deleteOrphanedLogs(bool (*validHandle)(const std::string&));
*/
}

} // end namespace tests
} // end namespace console_process
} // end namespace session
} // end namespace rstudio
