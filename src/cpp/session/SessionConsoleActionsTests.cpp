/*
 * SessionConsoleActionsTests.cpp
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

#include <r/session/RConsoleActions.hpp>

#include <gtest/gtest.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {
namespace tests {

namespace {

// concatenate the data portion of the console actions wire format
std::string consoleData(json::Object& actionsJson)
{
   std::string data;
   for (const json::Value& value : actionsJson["data"].getArray())
      data.append(value.getString());
   return data;
}

} // anonymous namespace

TEST(ConsoleActionsTest, ChunkingDoesNotSplitUtf8Sequences)
{
   ConsoleActions& actions = consoleActions();
   actions.reset();

   // a single long line of 3-byte UTF-8 characters; because the chunk
   // size is not a multiple of 3, naive chunking would split a character
   // at every chunk boundary
   std::string line;
   for (int i = 0; i < 600; i++)
      line.append("\xE2\x96\x88");
   line.push_back('\n');

   actions.add(kConsoleActionOutput, line);

   json::Object actionsJson;
   actions.asJson(&actionsJson);

   // make sure the line was actually chunked
   json::Array dataArray = actionsJson["data"].getArray();
   ASSERT_GT(dataArray.getSize(), 1u);

   // every chunk should be individually valid UTF-8
   std::string all;
   for (std::size_t i = 0, n = dataArray.getSize(); i < n; i++)
   {
      std::string data = dataArray[i].getString();
      std::size_t distance = 0;
      Error error = string_utils::utf8Distance(data.begin(), data.end(), &distance);
      EXPECT_FALSE(error);
      all.append(data);
   }

   // concatenating the chunks should reproduce the original output
   EXPECT_EQ(line, all);

   actions.reset();
}

TEST(ConsoleActionsTest, InvalidUtf8OutputSurvivesSaveAndLoad)
{
   ConsoleActions& actions = consoleActions();
   actions.reset();

   // raw bytes that are not valid UTF-8, as produced by e.g. cat("\xff")
   actions.add(kConsoleActionOutput, "before \xFF\xFE after\n");

   FilePath tempFile;
   ASSERT_FALSE(FilePath::tempFilePath(tempFile));
   EXPECT_FALSE(actions.saveToFile(tempFile));

   actions.reset();
   EXPECT_FALSE(actions.loadFromFile(tempFile));

   // invalid bytes should have been sanitized when the action was captured
   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ("before ?? after\n", consoleData(actionsJson));

   actions.reset();
   tempFile.removeIfExists();
}

TEST(ConsoleActionsTest, UnparseableConsoleActionsFileIsDiscarded)
{
   ConsoleActions& actions = consoleActions();
   actions.reset();

   // simulate a console_actions file written by an older release, with a
   // multi-byte UTF-8 sequence split across two chunks (see issue #18382);
   // the invalid UTF-8 within causes a JSON parse error on load
   std::string contents = "{\"type\": [2, 2], \"data\": [\"output \xE2\x96\", \"\x88\\n\"]}";

   FilePath tempFile;
   ASSERT_FALSE(FilePath::tempFilePath(tempFile));
   ASSERT_FALSE(writeStringToFile(tempFile, contents));

   // the corrupt file should be discarded, not reported as an error
   EXPECT_FALSE(actions.loadFromFile(tempFile));

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ(0u, actionsJson["data"].getArray().getSize());

   actions.reset();
   tempFile.removeIfExists();
}

TEST(ConsoleActionsTest, MalformedConsoleActionsFileIsDiscarded)
{
   ConsoleActions& actions = consoleActions();
   actions.reset();

   FilePath tempFile;
   ASSERT_FALSE(FilePath::tempFilePath(tempFile));
   ASSERT_FALSE(writeStringToFile(tempFile, "{\"type\": 42}"));

   EXPECT_FALSE(actions.loadFromFile(tempFile));

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ(0u, actionsJson["data"].getArray().getSize());

   actions.reset();
   tempFile.removeIfExists();
}

} // namespace tests
} // namespace session
} // namespace r
} // namespace rstudio
