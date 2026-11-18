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
   std::string originalHistsize = core::system::getenv("R_HISTSIZE");
   BOOST_SCOPE_EXIT(&originalHistsize)
   {
      if (originalHistsize.empty())
         core::system::unsetenv("R_HISTSIZE");
      else
         core::system::setenv("R_HISTSIZE", originalHistsize);
   }
   BOOST_SCOPE_EXIT_END

   FilePath historyPath, savedPath;
   ASSERT_FALSE(FilePath::tempFilePath(historyPath));
   ASSERT_FALSE(FilePath::tempFilePath(savedPath));
   BOOST_SCOPE_EXIT(&historyPath, &savedPath)
   {
      historyPath.remove();
      savedPath.remove();
   }
   BOOST_SCOPE_EXIT_END

   const std::vector<std::string> commands {"first()", "second()", "third()"};
   ASSERT_FALSE(core::writeStringVectorToFile(historyPath, commands));

   // R_HISTSIZE=0 asks R to save no history; the session's history works as
   // usual. loading a history file into a capacity of 0 used to crash
   core::system::setenv("R_HISTSIZE", "0");
   r::session::ConsoleHistory history;
   history.setCapacityFromRHistsize();
   EXPECT_EQ(512, history.capacity());

   ASSERT_FALSE(history.loadFromFile(historyPath, true));
   EXPECT_EQ(commands, std::vector<std::string>(history.begin(), history.end()));

   std::vector<std::string> saved;
   ASSERT_FALSE(history.saveToHistoryFile(savedPath));
   ASSERT_FALSE(core::readStringVectorFromFile(savedPath, &saved));
   EXPECT_TRUE(saved.empty());

   // like R, R_HISTSIZE is read when saving, and keeps the most recent commands
   // (readStringVectorFromFile() appends, so clear before each read)
   core::system::setenv("R_HISTSIZE", "2");
   ASSERT_FALSE(history.saveToHistoryFile(savedPath));
   saved.clear();
   ASSERT_FALSE(core::readStringVectorFromFile(savedPath, &saved));
   EXPECT_EQ((std::vector<std::string> {"second()", "third()"}), saved);

   // unset, negative, and invalid values mean R's default of 512
   for (const char* value : {"", "-1", "abc"})
   {
      if (*value)
         core::system::setenv("R_HISTSIZE", value);
      else
         core::system::unsetenv("R_HISTSIZE");

      r::session::ConsoleHistory defaultHistory;
      defaultHistory.setCapacityFromRHistsize();
      EXPECT_EQ(512, defaultHistory.capacity()) << "R_HISTSIZE=" << value;

      ASSERT_FALSE(history.saveToHistoryFile(savedPath));
      saved.clear();
      ASSERT_FALSE(core::readStringVectorFromFile(savedPath, &saved));
      EXPECT_EQ(commands, saved) << "R_HISTSIZE=" << value;
   }

   // a value larger than the default also raises the session's capacity
   core::system::setenv("R_HISTSIZE", "1000");
   r::session::ConsoleHistory largeHistory;
   largeHistory.setCapacityFromRHistsize();
   EXPECT_EQ(1000, largeHistory.capacity());
}

} // namespace tests
} // namespace session
} // namespace rstudio
