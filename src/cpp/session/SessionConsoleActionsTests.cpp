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

// NOTE: these tests drive the process-wide consoleActions() singleton, so
// state is reset around each test to keep them order-independent
class ConsoleActionsTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      consoleActions().reset();
      ASSERT_FALSE(FilePath::tempFilePath(tempFile_));
   }

   void TearDown() override
   {
      consoleActions().reset();
      tempFile_.removeIfExists();
   }

   FilePath tempFile_;
};

TEST_F(ConsoleActionsTest, ChunkingDoesNotSplitUtf8Sequences)
{
   ConsoleActions& actions = consoleActions();

   // a single long line cycling 1- to 4-byte UTF-8 characters, so that chunk
   // boundaries fall at every possible offset within a multi-byte sequence
   // regardless of the chunk size in use
   std::string line;
   for (int i = 0; i < 200; i++)
      line.append("a\xC3\xA9\xE2\x96\x88\xF0\x9F\x98\x80");
   line.push_back('\n');

   actions.add(kConsoleActionOutput, line);

   json::Object actionsJson;
   actions.asJson(&actionsJson);

   // make sure the line was actually chunked
   json::Array dataArray = actionsJson["data"].getArray();
   ASSERT_GT(dataArray.getSize(), 1u);

   // every chunk should be individually valid UTF-8
   for (std::size_t i = 0, n = dataArray.getSize(); i < n; i++)
   {
      std::string data = dataArray[i].getString();
      std::size_t distance = 0;
      Error error = string_utils::utf8Distance(data.begin(), data.end(), &distance);
      EXPECT_FALSE(error);
   }

   // concatenating the chunks should reproduce the original output
   EXPECT_EQ(line, consoleData(actionsJson));

   // the actions should also survive a save / load round trip byte-identically;
   // this is the layer where issue #18382 lived, as the JSON parser validates
   // UTF-8 on load
   ASSERT_FALSE(actions.saveToFile(tempFile_));
   actions.reset();
   ASSERT_FALSE(actions.loadFromFile(tempFile_));

   json::Object restoredJson;
   actions.asJson(&restoredJson);
   EXPECT_EQ(line, consoleData(restoredJson));
}

TEST_F(ConsoleActionsTest, Utf8SequenceSplitAcrossAddCallsIsRejoined)
{
   ConsoleActions& actions = consoleActions();

   // pipe reads are arbitrary byte windows over the output stream, so a
   // multi-byte character can be torn across two add() calls with a flush
   // in between (e.g. session state serialized just as output arrives)
   actions.add(kConsoleActionOutput, "abc\xE2\x96");

   // the incomplete suffix should be held back, not sanitized to '?'
   json::Object firstJson;
   actions.asJson(&firstJson);
   EXPECT_EQ("abc", consoleData(firstJson));

   // the continuation byte should rejoin the held-back prefix
   actions.add(kConsoleActionOutput, "\x88 done\n");

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ("abc\xE2\x96\x88 done\n", consoleData(actionsJson));
}

TEST_F(ConsoleActionsTest, InvalidUtf8IsSanitizedBeforeSaving)
{
   ConsoleActions& actions = consoleActions();

   // raw bytes that are not valid UTF-8, as produced by e.g. cat("\xff")
   actions.add(kConsoleActionOutput, "before \xFF\xFE after\n");

   EXPECT_FALSE(actions.saveToFile(tempFile_));

   actions.reset();
   EXPECT_FALSE(actions.loadFromFile(tempFile_));

   // invalid bytes are sanitized when the buffer is flushed into an action,
   // so the file both parses and round-trips
   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ("before ?? after\n", consoleData(actionsJson));
}

TEST_F(ConsoleActionsTest, UnserializableActionsAreDiscardedAtSave)
{
   ConsoleActions& actions = consoleActions();

   // a UTF-16 surrogate half passes the lenient utf8Clean() sanitizer but is
   // rejected by the JSON parser; saveToFile() should detect this and save an
   // empty replay rather than a file that would be discarded on restore
   actions.add(kConsoleActionOutput, "surrogate \xED\xA0\x80\n");

   ASSERT_FALSE(actions.saveToFile(tempFile_));
   actions.reset();
   EXPECT_FALSE(actions.loadFromFile(tempFile_));

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ(0u, actionsJson["data"].getArray().getSize());
}

TEST_F(ConsoleActionsTest, UnparseableConsoleActionsFileIsDiscarded)
{
   ConsoleActions& actions = consoleActions();

   // simulate a console_actions file with a multi-byte UTF-8 sequence split
   // across two chunks, as written by any build predating the UTF-8-aware
   // chunking in flush() (see issue #18382); the invalid UTF-8 within causes
   // a JSON parse error on load
   std::string contents = "{\"type\": [2, 2], \"data\": [\"output \xE2\x96\", \"\x88\\n\"]}";
   ASSERT_FALSE(writeStringToFile(tempFile_, contents));

   // the corrupt file should be discarded, not reported as an error
   EXPECT_FALSE(actions.loadFromFile(tempFile_));

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ(0u, actionsJson["data"].getArray().getSize());
}

TEST_F(ConsoleActionsTest, MalformedConsoleActionsFileIsDiscarded)
{
   ConsoleActions& actions = consoleActions();

   ASSERT_FALSE(writeStringToFile(tempFile_, "{\"type\": 42}"));

   EXPECT_FALSE(actions.loadFromFile(tempFile_));

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ(0u, actionsJson["data"].getArray().getSize());
}

TEST_F(ConsoleActionsTest, MismatchedTypeAndDataArraysAreDiscarded)
{
   ConsoleActions& actions = consoleActions();

   ASSERT_FALSE(writeStringToFile(tempFile_, "{\"type\": [2], \"data\": []}"));

   EXPECT_FALSE(actions.loadFromFile(tempFile_));

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ(0u, actionsJson["data"].getArray().getSize());
}

TEST_F(ConsoleActionsTest, MalformedEntriesAreSkippedOnLoad)
{
   ConsoleActions& actions = consoleActions();

   ASSERT_FALSE(writeStringToFile(
      tempFile_,
      "{\"type\": [2, \"bogus\", 2], \"data\": [\"a\", \"b\", \"c\"]}"));

   EXPECT_FALSE(actions.loadFromFile(tempFile_));

   // the malformed entry is skipped; the well-formed ones still load
   json::Object actionsJson;
   actions.asJson(&actionsJson);
   EXPECT_EQ(2u, actionsJson["data"].getArray().getSize());
   EXPECT_EQ("ac", consoleData(actionsJson));
}

} // namespace tests
} // namespace session
} // namespace r
} // namespace rstudio
