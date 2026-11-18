/*
 * SessionConsoleHistoryTests.cpp
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

#include <boost/scope_exit.hpp>

#include <string>
#include <vector>

#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <r/session/RConsoleHistory.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace tests {

TEST(SessionConsoleHistoryTest, RHistsizeLimitsOnlyTheSavedHistory) {
   // restores R_HISTSIZE, set or not, when the test ends
   core::system::EnvironmentScope histsizeScope("R_HISTSIZE", "0");

   FilePath historyPath, longHistoryPath, savedPath;
   ASSERT_FALSE(FilePath::tempFilePath(historyPath));
   ASSERT_FALSE(FilePath::tempFilePath(longHistoryPath));
   ASSERT_FALSE(FilePath::tempFilePath(savedPath));
   BOOST_SCOPE_EXIT(&historyPath, &longHistoryPath, &savedPath)
   {
      historyPath.remove();
      longHistoryPath.remove();
      savedPath.remove();
   }
   BOOST_SCOPE_EXIT_END

   const std::vector<std::string> commands {"first()", "second()", "third()"};
   ASSERT_FALSE(core::writeStringVectorToFile(historyPath, commands));

   auto entriesOf = [](const r::session::ConsoleHistory& history)
   {
      return std::vector<std::string>(history.begin(), history.end());
   };

   // the file starts with a stale command each time, so a save that writes
   // nothing can't pass; readStringVectorFromFile() appends, so read into a
   // fresh vector
   auto savedBy = [&savedPath](const r::session::ConsoleHistory& history)
   {
      std::vector<std::string> saved;
      EXPECT_FALSE(core::writeStringToFile(savedPath, "stale()\n"));
      EXPECT_FALSE(history.saveToHistoryFile(savedPath));
      EXPECT_FALSE(core::readStringVectorFromFile(savedPath, &saved));
      return saved;
   };

   // R_HISTSIZE=0 asks R to save no history; the session's history works as
   // usual. loading a history file into a capacity of 0 used to crash
   r::session::ConsoleHistory zeroHistory;
   zeroHistory.setCapacityFromRHistsize();
   ASSERT_EQ(512, zeroHistory.capacity());
   ASSERT_FALSE(zeroHistory.loadFromFile(historyPath, true));
   EXPECT_EQ(commands, entriesOf(zeroHistory));
   EXPECT_TRUE(savedBy(zeroHistory).empty());

   // a small value doesn't limit the session's history either, only what is
   // saved, and like R it is read when saving
   core::system::setenv("R_HISTSIZE", "2");
   r::session::ConsoleHistory smallHistory;
   smallHistory.setCapacityFromRHistsize();
   EXPECT_EQ(512, smallHistory.capacity());
   ASSERT_FALSE(smallHistory.loadFromFile(historyPath, true));
   EXPECT_EQ(commands, entriesOf(smallHistory));

   const std::vector<std::string> lastTwo {"second()", "third()"};
   EXPECT_EQ(lastTwo, savedBy(smallHistory));
   EXPECT_EQ(lastTwo, savedBy(zeroHistory));

   // the session's own snapshot (saved on suspend) keeps everything
   std::vector<std::string> snapshot;
   ASSERT_FALSE(smallHistory.saveToFile(savedPath, true));
   ASSERT_FALSE(core::readStringVectorFromFile(savedPath, &snapshot));
   EXPECT_EQ(commands, snapshot);

   // a value larger than the default raises the session's capacity
   core::system::setenv("R_HISTSIZE", "1000");
   r::session::ConsoleHistory largeHistory;
   largeHistory.setCapacityFromRHistsize();
   EXPECT_EQ(1000, largeHistory.capacity());

   std::vector<std::string> longCommands;
   for (int i = 1; i <= 600; i++)
      longCommands.push_back("command" + std::to_string(i) + "()");
   ASSERT_FALSE(core::writeStringVectorToFile(longHistoryPath, longCommands));
   ASSERT_FALSE(largeHistory.loadFromFile(longHistoryPath, true));
   ASSERT_EQ(600, largeHistory.size());
   EXPECT_EQ(longCommands, savedBy(largeHistory));

   // unset, negative, and invalid values mean R's default of 512, for both the
   // session's capacity and the commands saved
   const std::vector<std::string> last512(longCommands.end() - 512, longCommands.end());
   for (const char* value : {"", "-1", "abc"})
   {
      if (*value)
         core::system::setenv("R_HISTSIZE", value);
      else
         core::system::unsetenv("R_HISTSIZE");

      r::session::ConsoleHistory defaultHistory;
      defaultHistory.setCapacityFromRHistsize();
      EXPECT_EQ(512, defaultHistory.capacity()) << "R_HISTSIZE=" << value;
      EXPECT_EQ(last512, savedBy(largeHistory)) << "R_HISTSIZE=" << value;
   }
}

} // namespace tests
} // namespace session
} // namespace rstudio
