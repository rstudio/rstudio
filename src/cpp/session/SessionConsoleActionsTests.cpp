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

#include <algorithm>
#include <random>
#include <string>
#include <vector>

#include <gtest/gtest.h>

#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/split.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include <core/AnsiEscapes.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/text/AnsiCodeParser.hpp>

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

TEST_F(ConsoleActionsTest, ReducingCapacityKeepsNewestActions)
{
   ConsoleActions& actions = consoleActions();
   int originalCapacity = actions.capacity();

   // one action per line, each identifiable by its content; asJson() flushes
   // the pending output buffer so the actions are stored before the capacity
   // changes (otherwise the flush itself would do the trimming)
   for (int i = 0; i < 20; i++)
      actions.add(kConsoleActionOutput, "line " + std::to_string(i) + "\n");

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   ASSERT_EQ(20u, actionsJson["data"].getArray().getSize());

   // the scrollback limit can be lowered while the session runs; the oldest
   // actions should be discarded, not the newest (boost's set_capacity()
   // would drop from the back)
   actions.setCapacity(10);
   EXPECT_EQ(10, actions.capacity());

   actions.asJson(&actionsJson);
   json::Array dataArray = actionsJson["data"].getArray();
   ASSERT_EQ(10u, dataArray.getSize());
   EXPECT_EQ("line 10\n", dataArray[0].getString());
   EXPECT_EQ("line 19\n", dataArray[9].getString());

   // raising the limit keeps what is there and makes room for more
   actions.setCapacity(originalCapacity);
   actions.add(kConsoleActionOutput, "line 20\n");
   actions.asJson(&actionsJson);
   EXPECT_EQ("line 10\nline 11\nline 12\nline 13\nline 14\n"
             "line 15\nline 16\nline 17\nline 18\nline 19\nline 20\n",
             consoleData(actionsJson));
}

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

TEST_F(ConsoleActionsTest, ContainsOutputMatchesRestoredMultilineText)
{
   ConsoleActions& actions = consoleActions();

   // stored as one action per line, and the long line in several chunks
   const std::string warning =
      "WARNING: first line\n"
      "\n"
      "    Path: /" + std::string(1200, 'x') + "\n"
      "last line";

   actions.add(kConsoleActionOutput, "earlier output\n");
   actions.add(kConsoleActionOutputError, warning + "\n");
   actions.add(kConsoleActionPrompt, "> ");

   ASSERT_FALSE(actions.saveToFile(tempFile_));
   actions.reset();
   EXPECT_FALSE(actions.containsOutput(warning));

   ASSERT_FALSE(actions.loadFromFile(tempFile_));
   EXPECT_TRUE(actions.containsOutput(warning));
   EXPECT_FALSE(actions.containsOutput("WARNING: another warning"));
}

TEST_F(ConsoleActionsTest, ContainsOutputIgnoresConditionHighlighting)
{
   ConsoleActions& actions = consoleActions();

   // condition highlighting marks up a "Warning:" prefix (matched without
   // regard to case) inside the text, as well as grouping the whole of it
   actions.add(
      kConsoleActionOutputError,
      kAnsiEscapeGroupStartWarning kAnsiEscapeHighlightStartWarning "WARNING:"
      kAnsiEscapeHighlightEnd " RStudio could not start this session in /a"
      kAnsiEscapeGroupEnd "\n");

   EXPECT_TRUE(actions.containsOutput(
      "WARNING: RStudio could not start this session in /a"));
}

TEST_F(ConsoleActionsTest, ContainsOutputIgnoresInput)
{
   ConsoleActions& actions = consoleActions();

   actions.add(kConsoleActionPrompt, "> ");
   actions.add(kConsoleActionInput, "message('WARNING: /a')");

   EXPECT_FALSE(actions.containsOutput("WARNING: /a"));
}

TEST_F(ConsoleActionsTest, ContainsOutputMissesTextTrimmedFromScrollback)
{
   ConsoleActions& actions = consoleActions();
   int originalCapacity = actions.capacity();
   actions.setCapacity(4);

   actions.add(kConsoleActionOutputError, "WARNING: first line\nsecond line\n");
   for (int i = 0; i < 3; i++)
      actions.add(kConsoleActionOutput, "line " + std::to_string(i) + "\n");

   // the warning's first line has been dropped, so it is no longer shown
   EXPECT_FALSE(actions.containsOutput("WARNING: first line\nsecond line"));
   EXPECT_TRUE(actions.containsOutput("second line"));

   actions.setCapacity(originalCapacity);
}

namespace {

typedef std::vector<std::string> Lines;

// the size of the chunks that long lines are stored in (kChunkSize in
// RConsoleActions.cpp)
const std::size_t kChunkSize = 512;

// whether the action at 'index' is a chunk of a long line, which continues in
// the action after it. a chunk is kChunkSize bytes, less the bytes of a UTF-8
// sequence that would otherwise have been cut in two
bool isChunk(json::Array& typeArray, json::Array& dataArray, std::size_t index)
{
   // the rest of a chunked line is always the next action, of the same type
   if (index + 1 == dataArray.getSize() ||
       typeArray[index].getInt() != typeArray[index + 1].getInt())
   {
      return false;
   }

   std::string data = dataArray[index].getString();
   if (data.size() == kChunkSize)
      return true;

   // cut a chunk from this action and the next, as if they were still one
   // piece of text: is this action what gets cut off?
   std::string text = data + dataArray[index + 1].getString();
   if (text.size() < kChunkSize)
      return false;

   std::string chunk = text.substr(0, kChunkSize);
   chunk.resize(chunk.size() - string_utils::utf8IncompleteSuffixLength(chunk));
   return chunk == data;
}

// every console line, computed the straightforward way: join all of the
// actions, strip escape sequences, then split. getConsoleLines() must return
// pages of exactly these lines, without doing all of this work
Lines referenceConsoleLines(json::Object& actionsJson)
{
   json::Array typeArray = actionsJson["type"].getArray();
   json::Array dataArray = actionsJson["data"].getArray();

   std::string content;
   for (std::size_t i = 0, n = dataArray.getSize(); i < n; i++)
   {
      std::string data = dataArray[i].getString();
      content.append(data);

      // input and output that doesn't end its line gets a newline; prompts
      // don't (input follows on the same line), nor do chunks (a long line
      // continues in the next action)
      if (typeArray[i].getInt() != kConsoleActionPrompt &&
          !data.empty() &&
          data.back() != '\n' &&
          !isChunk(typeArray, dataArray, i))
      {
         content.push_back('\n');
      }
   }

   text::stripAnsiCodes(&content);

   Lines lines;
   boost::algorithm::split(lines, content, boost::algorithm::is_any_of("\n"));
   if (!lines.empty() && lines.back().empty())
      lines.pop_back();

   return lines;
}

// the page of 'lines' that getConsoleLines() should return
Lines referencePage(const Lines& lines,
                    int limit,
                    int offset,
                    bool fromBottom,
                    int maxChars)
{
   long long total = static_cast<long long>(lines.size());
   long long begin, end;
   if (fromBottom)
   {
      end = total - offset;
      begin = std::max(0LL, end - limit);
   }
   else
   {
      begin = offset;
      end = std::min(total, begin + limit);
   }

   Lines page;
   long long chars = 0;
   for (long long i = std::max(0LL, begin); i < end && i < total; i++)
   {
      long long lineChars = static_cast<long long>(lines[i].size());
      if (chars + lineChars > maxChars)
         break;

      page.push_back(lines[i]);
      chars += lineChars;
   }

   return page;
}

// replace the console actions with exactly these; unlike add(), which
// splits what it is given into lines and chunks, this can store any shape
// of action (as a console actions file from another version might)
Error loadActions(const std::vector<ConsoleAction>& actions,
                  const FilePath& filePath)
{
   json::Array typeArray;
   json::Array dataArray;
   for (const ConsoleAction& action : actions)
   {
      typeArray.push_back(json::Value(action.type));
      dataArray.push_back(json::Value(action.data));
   }

   json::Object actionsJson;
   actionsJson["type"] = typeArray;
   actionsJson["data"] = dataArray;

   Error error = writeStringToFile(filePath, actionsJson.write());
   if (error)
      return error;

   consoleActions().reset();
   return consoleActions().loadFromFile(filePath);
}

} // anonymous namespace

TEST_F(ConsoleActionsTest, GetConsoleLinesOfEmptyConsole)
{
   ConsoleActions& actions = consoleActions();

   EXPECT_EQ(Lines(), actions.getConsoleLines(50));
   EXPECT_EQ(Lines(), actions.getConsoleLines(50, 0, false));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesPagesFromBottom)
{
   ConsoleActions& actions = consoleActions();

   for (int i = 0; i < 10; i++)
      actions.add(kConsoleActionOutput, "line " + std::to_string(i) + "\n");

   // the most recent lines, oldest first
   EXPECT_EQ(Lines({"line 7", "line 8", "line 9"}), actions.getConsoleLines(3));

   // the offset skips lines at the bottom
   EXPECT_EQ(Lines({"line 5", "line 6", "line 7"}), actions.getConsoleLines(3, 2));
   EXPECT_EQ(Lines({"line 0"}), actions.getConsoleLines(1, 9));

   // a page that runs off the top is cut short
   EXPECT_EQ(Lines({"line 0", "line 1"}), actions.getConsoleLines(3, 8));
   EXPECT_EQ(Lines(), actions.getConsoleLines(3, 10));
   EXPECT_EQ(Lines(), actions.getConsoleLines(3, 25));

   EXPECT_EQ(10u, actions.getConsoleLines(50).size());
   EXPECT_EQ(Lines(), actions.getConsoleLines(0));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesPagesFromTop)
{
   ConsoleActions& actions = consoleActions();

   for (int i = 0; i < 10; i++)
      actions.add(kConsoleActionOutput, "line " + std::to_string(i) + "\n");

   EXPECT_EQ(Lines({"line 0", "line 1", "line 2"}), actions.getConsoleLines(3, 0, false));

   // the offset skips lines at the top
   EXPECT_EQ(Lines({"line 2", "line 3", "line 4"}), actions.getConsoleLines(3, 2, false));
   EXPECT_EQ(Lines({"line 9"}), actions.getConsoleLines(1, 9, false));

   // a page that runs off the bottom is cut short
   EXPECT_EQ(Lines({"line 8", "line 9"}), actions.getConsoleLines(3, 8, false));
   EXPECT_EQ(Lines(), actions.getConsoleLines(3, 10, false));
   EXPECT_EQ(Lines(), actions.getConsoleLines(3, 25, false));

   EXPECT_EQ(10u, actions.getConsoleLines(50, 0, false).size());
   EXPECT_EQ(Lines(), actions.getConsoleLines(0, 0, false));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesReadsWrappedScrollback)
{
   ConsoleActions& actions = consoleActions();
   int originalCapacity = actions.capacity();
   actions.setCapacity(4);

   // more lines than the scrollback holds, so the oldest are overwritten
   for (int i = 0; i < 10; i++)
      actions.add(kConsoleActionOutput, "line " + std::to_string(i) + "\n");

   EXPECT_EQ(Lines({"line 6", "line 7", "line 8", "line 9"}), actions.getConsoleLines(50));
   EXPECT_EQ(Lines({"line 7", "line 8"}), actions.getConsoleLines(2, 1));
   EXPECT_EQ(Lines({"line 7", "line 8"}), actions.getConsoleLines(2, 1, false));

   actions.setCapacity(originalCapacity);
}

TEST_F(ConsoleActionsTest, GetConsoleLinesJoinsChunksOfLongLines)
{
   ConsoleActions& actions = consoleActions();

   // lines longer than the chunk size are stored as several actions; cover a
   // line ending mid-chunk, and lines that are an exact multiple of the chunk
   // size (whose last chunk then also carries the newline)
   const std::string ragged(2 * kChunkSize + 176, 'x');
   const std::string twoChunks(2 * kChunkSize, 'y');
   const std::string oneChunk(kChunkSize, 'z');

   actions.add(kConsoleActionOutput, "start\n");
   actions.add(kConsoleActionOutput, ragged + "\n" + twoChunks + "\n" + oneChunk + "\n");
   actions.add(kConsoleActionOutput, "end\n");

   // make sure the long lines really were chunked
   json::Object actionsJson;
   actions.asJson(&actionsJson);
   ASSERT_EQ(8u, actionsJson["data"].getArray().getSize());

   EXPECT_EQ(Lines({"start", ragged, twoChunks, oneChunk, "end"}), actions.getConsoleLines(50));
   EXPECT_EQ(Lines({"start", ragged, twoChunks, oneChunk, "end"}), actions.getConsoleLines(50, 0, false));

   // each long line counts as one line when paging, in either direction
   EXPECT_EQ(Lines({twoChunks, oneChunk}), actions.getConsoleLines(2, 1));
   EXPECT_EQ(Lines({"start", ragged}), actions.getConsoleLines(2, 3));
   EXPECT_EQ(Lines({ragged, twoChunks}), actions.getConsoleLines(2, 1, false));
   EXPECT_EQ(Lines({oneChunk, "end"}), actions.getConsoleLines(2, 3, false));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesStripsAnsiCodesSpanningChunks)
{
   ConsoleActions& actions = consoleActions();

   // place an escape sequence so that it straddles the first chunk boundary
   const std::string head(kChunkSize - 2, 'a');
   const std::string tail(600, 'b');

   actions.add(kConsoleActionOutput, "before\n");
   actions.add(kConsoleActionOutput, head + "\x1b[31mred\x1b[0m" + tail + "\n");
   actions.add(kConsoleActionOutput, "\x1b[1mafter\x1b[22m\n");

   // make sure the first chunk really does end partway through the sequence
   json::Object actionsJson;
   actions.asJson(&actionsJson);
   json::Array dataArray = actionsJson["data"].getArray();
   ASSERT_EQ(5u, dataArray.getSize());
   ASSERT_EQ(head + "\x1b[", dataArray[1].getString());

   const std::string stripped = head + "red" + tail;
   EXPECT_EQ(Lines({"before", stripped, "after"}), actions.getConsoleLines(50));
   EXPECT_EQ(Lines({"before", stripped, "after"}), actions.getConsoleLines(50, 0, false));
   EXPECT_EQ(Lines({stripped}), actions.getConsoleLines(1, 1));
   EXPECT_EQ(Lines({stripped}), actions.getConsoleLines(1, 1, false));
   EXPECT_EQ(Lines({"before"}), actions.getConsoleLines(1, 2));
   EXPECT_EQ(Lines({"after"}), actions.getConsoleLines(1, 2, false));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesJoinsChunksOfLongNonAsciiLines)
{
   ConsoleActions& actions = consoleActions();

   // a chunk is cut short rather than split a multi-byte character, so the
   // chunks of these lines come in different sizes: cycling 1- to 4-byte
   // characters cuts chunks 1 and 2 bytes short, and the first chunk of the
   // second line has room for all but the last byte of a 4-byte character
   std::string mixed;
   for (int i = 0; i < 200; i++)
      mixed.append("a\xC3\xA9\xE2\x96\x88\xF0\x9F\x98\x80");

   std::string wide = "a";
   for (int i = 0; i < 200; i++)
      wide.append("\xF0\x9F\x98\x80");

   actions.add(kConsoleActionOutput, "before\n" + mixed + "\n" + wide + "\nafter\n");

   // make sure chunks of each of those sizes really are involved
   json::Object actionsJson;
   actions.asJson(&actionsJson);
   json::Array dataArray = actionsJson["data"].getArray();
   ASSERT_EQ(8u, dataArray.getSize());
   ASSERT_EQ(kChunkSize - 1, dataArray[1].getString().size());
   ASSERT_EQ(kChunkSize, dataArray[2].getString().size());
   ASSERT_EQ(kChunkSize - 2, dataArray[3].getString().size());
   ASSERT_EQ(kChunkSize - 3, dataArray[5].getString().size());

   EXPECT_EQ(Lines({"before", mixed, wide, "after"}), actions.getConsoleLines(50));
   EXPECT_EQ(Lines({"before", mixed, wide, "after"}), actions.getConsoleLines(50, 0, false));
   EXPECT_EQ(Lines({mixed}), actions.getConsoleLines(1, 2));
   EXPECT_EQ(Lines({wide}), actions.getConsoleLines(1, 2, false));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesStripsAnsiCodesSpanningShortenedChunks)
{
   ConsoleActions& actions = consoleActions();

   // a window title of 3-byte characters, placed so that the first chunk ends
   // partway through it, 2 bytes short of the chunk size
   std::string title;
   for (int i = 0; i < 10; i++)
      title.append("\xE2\x96\x88");

   const std::string head(kChunkSize - 12, 'a');
   actions.add(kConsoleActionOutput, head + "\x1b]0;" + title + "\x07visible\n");

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   json::Array dataArray = actionsJson["data"].getArray();
   ASSERT_EQ(2u, dataArray.getSize());
   ASSERT_EQ(kChunkSize - 2, dataArray[0].getString().size());

   EXPECT_EQ(Lines({head + "visible"}), actions.getConsoleLines(50));
   EXPECT_EQ(Lines({head + "visible"}), actions.getConsoleLines(50, 0, false));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesKeepsOutputOfDifferentTypesApart)
{
   ConsoleActions& actions = consoleActions();

   // an unfinished line that happens to be nearly a chunk long, followed by
   // a multi-byte character, isn't a chunk when what follows is other output
   const std::string unfinished(kChunkSize - 2, 'x');
   actions.add(kConsoleActionOutput, unfinished);
   actions.add(kConsoleActionOutputError, "\xE2\x96\x88 error\n");

   EXPECT_EQ(Lines({unfinished, "\xE2\x96\x88 error"}), actions.getConsoleLines(50));
   EXPECT_EQ(Lines({unfinished, "\xE2\x96\x88 error"}), actions.getConsoleLines(50, 0, false));

   // nor is one of exactly a chunk's length: input that long, and then its
   // output, or an unfinished line of output that long, and then a prompt
   actions.reset();
   const std::string input(kChunkSize, 'i');
   const std::string output(kChunkSize, 'o');
   actions.add(kConsoleActionPrompt, "> ");
   actions.add(kConsoleActionInput, input);
   actions.add(kConsoleActionOutput, output);
   actions.add(kConsoleActionPrompt, "> ");

   json::Object actionsJson;
   actions.asJson(&actionsJson);
   json::Array dataArray = actionsJson["data"].getArray();
   ASSERT_EQ(4u, dataArray.getSize());
   ASSERT_EQ(input, dataArray[1].getString());
   ASSERT_EQ(output, dataArray[2].getString());

   EXPECT_EQ(Lines({"> " + input, output, "> "}), actions.getConsoleLines(50));
   EXPECT_EQ(Lines({"> " + input, output, "> "}), actions.getConsoleLines(50, 0, false));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesJoinsPromptsAndInput)
{
   ConsoleActions& actions = consoleActions();

   // input arrives without a newline, and belongs on the line of its prompt
   actions.add(kConsoleActionPrompt, "> ");
   actions.add(kConsoleActionInput, "x <- c(1,");
   actions.add(kConsoleActionPrompt, "+ ");
   actions.add(kConsoleActionInput, "2)");
   actions.add(kConsoleActionPrompt, "> ");
   actions.add(kConsoleActionInput, "cat(x)");

   // output that doesn't end in a newline is still its own line
   actions.add(kConsoleActionOutput, "1 2");
   actions.add(kConsoleActionPrompt, "> ");
   actions.add(kConsoleActionInput, "message('oops')");
   actions.add(kConsoleActionOutputError, "oops\n");

   // the pending prompt is the last line
   actions.add(kConsoleActionPrompt, "> ");

   Lines expected = {
      "> x <- c(1,",
      "+ 2)",
      "> cat(x)",
      "1 2",
      "> message('oops')",
      "oops",
      "> ",
   };

   EXPECT_EQ(expected, actions.getConsoleLines(50));
   EXPECT_EQ(expected, actions.getConsoleLines(50, 0, false));

   EXPECT_EQ(Lines({"oops", "> "}), actions.getConsoleLines(2));
   EXPECT_EQ(Lines({"> cat(x)", "1 2"}), actions.getConsoleLines(2, 3));
   EXPECT_EQ(Lines({"> x <- c(1,"}), actions.getConsoleLines(2, 6));
   EXPECT_EQ(Lines({"+ 2)", "> cat(x)"}), actions.getConsoleLines(2, 1, false));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesKeepsBlankLinesButDropsTrailingEmptyLine)
{
   ConsoleActions& actions = consoleActions();

   // only the empty "line" after the final newline is dropped
   actions.add(kConsoleActionOutput, "a\n\nb\n\n");
   EXPECT_EQ(Lines({"a", "", "b", ""}), actions.getConsoleLines(50));
   EXPECT_EQ(Lines({"a", "", "b", ""}), actions.getConsoleLines(50, 0, false));
   EXPECT_EQ(Lines({"b", ""}), actions.getConsoleLines(2));
   EXPECT_EQ(Lines({"", "b"}), actions.getConsoleLines(2, 1));
   EXPECT_EQ(Lines({"", "b"}), actions.getConsoleLines(2, 1, false));

   // a line that hasn't been terminated yet is returned
   actions.add(kConsoleActionOutput, "partial");
   EXPECT_EQ(Lines({"", "partial"}), actions.getConsoleLines(2));
   EXPECT_EQ(Lines({"", "partial"}), actions.getConsoleLines(2, 3, false));

   // a last line with nothing in it but escape sequences is dropped as well
   // (prompts aren't given a newline, so this one ends the console contents)
   actions.add(kConsoleActionPrompt, "\x1b[0m");
   EXPECT_EQ(Lines({"", "partial"}), actions.getConsoleLines(2));
   EXPECT_EQ(Lines({"", "partial"}), actions.getConsoleLines(2, 3, false));
   EXPECT_EQ(Lines({"a", "", "b", "", "partial"}), actions.getConsoleLines(50));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesRespectsMaxChars)
{
   ConsoleActions& actions = consoleActions();

   // escape sequences don't count towards the limit
   actions.add(kConsoleActionOutput, "aaaa\n\x1b[31mbbbb\x1b[0m\ncccc\ndddd\n");

   // lines are taken from the top of the page for as long as they fit
   EXPECT_EQ(Lines({"aaaa", "bbbb", "cccc", "dddd"}), actions.getConsoleLines(50, 0, true, 16));
   EXPECT_EQ(Lines({"aaaa", "bbbb", "cccc"}), actions.getConsoleLines(50, 0, true, 15));
   EXPECT_EQ(Lines({"aaaa", "bbbb"}), actions.getConsoleLines(50, 0, true, 8));
   EXPECT_EQ(Lines({"aaaa"}), actions.getConsoleLines(50, 0, true, 7));
   EXPECT_EQ(Lines(), actions.getConsoleLines(50, 0, true, 3));
   EXPECT_EQ(Lines(), actions.getConsoleLines(50, 0, true, 0));

   // ... which, paging from the bottom, is the oldest line of that page
   EXPECT_EQ(Lines({"cccc"}), actions.getConsoleLines(2, 0, true, 7));
   EXPECT_EQ(Lines({"bbbb"}), actions.getConsoleLines(2, 1, true, 7));

   EXPECT_EQ(Lines({"aaaa", "bbbb"}), actions.getConsoleLines(50, 0, false, 8));
   EXPECT_EQ(Lines({"bbbb"}), actions.getConsoleLines(2, 1, false, 7));
   EXPECT_EQ(Lines(), actions.getConsoleLines(50, 0, false, 3));

   // a line that is too long ends the page, even if later lines would fit
   actions.reset();
   actions.add(kConsoleActionOutput, "ab\n" + std::string(100, 'x') + "\ncd\n");
   EXPECT_EQ(Lines({"ab"}), actions.getConsoleLines(50, 0, true, 50));
   EXPECT_EQ(Lines({"ab"}), actions.getConsoleLines(50, 0, false, 50));
   EXPECT_EQ(Lines(), actions.getConsoleLines(2, 0, true, 50));
   EXPECT_EQ(Lines({"cd"}), actions.getConsoleLines(1, 0, true, 50));
}

TEST_F(ConsoleActionsTest, GetConsoleLinesMatchesWholeScrollbackReference)
{
   ConsoleActions& actions = consoleActions();

   // pieces that action data is assembled from; chosen to produce newlines
   // within and at the end of actions, blank lines, and escape sequences that
   // are whole, truncated, or split between neighbouring actions
   const std::vector<std::string> pieces = {
      "a",
      "bc",
      "def ",
      "\n",
      "\n",
      "\n\n",
      "\r",
      "\x1b[31m",
      "\x1b[0m",
      "\x1b",
      "\x1b[",
      "[1m",
      "31m",
      "\x1b]8;;http://example.com\x07",
      "\x1b]0;title",
      "\x1b\\",
      "\xC3\xA9",
      "\xE2\x96\x88",
      "\xF0\x9F\x98\x80",
   };

   // characters of 2, 3 and 4 bytes
   const std::vector<std::string> wide = {
      "\xC3\xA9",
      "\xE2\x96\x88",
      "\xF0\x9F\x98\x80",
   };

   // out of range values included: they come straight from the caller
   const std::vector<int> limits = {-1, 0, 1, 2, 3, 5, 50};
   const std::vector<int> offsets = {-2, -1, 0, 1, 2, 3, 7};
   const std::vector<int> maxCharses = {-1, 0, 1, 5, 600, 8000};

   // fixed seed, so a failure can be reproduced
   std::mt19937 rng(19750);
   auto pick = [&](std::size_t n) { return static_cast<std::size_t>(rng() % n); };

   for (int iteration = 0; iteration < 100; iteration++)
   {
      // the character that the previous action, if cut short like a chunk,
      // is followed by
      std::string carried;
      int carriedType = 0;

      std::vector<ConsoleAction> input(pick(13));
      for (ConsoleAction& action : input)
      {
         action.type = static_cast<int>(pick(4));
         for (std::size_t i = 0, n = pick(5); i < n; i++)
            action.data.append(pieces[pick(pieces.size())]);

         // usually in an action of the same type, as it is for a real chunk
         if (!carried.empty())
         {
            action.data.insert(0, carried);
            if (pick(4) != 0)
               action.type = carriedType;
            carried.clear();
         }

         // make some actions one chunk long: ending in ordinary text, in a
         // newline, or in the first byte of an escape sequence; or 1 to 3
         // bytes short of that and followed by a multi-byte character, which
         // makes the action a chunk only if the character wouldn't have fit
         switch (pick(12))
         {
         case 3:
         case 4:
            action.data.resize(kChunkSize - 1 - pick(3), 'x');
            carried = wide[pick(wide.size())];
            carriedType = action.type;
            break;
         case 0:
            action.data.resize(kChunkSize, 'x');
            break;
         case 1:
            action.data.resize(kChunkSize - 1, 'x');
            action.data.push_back('\n');
            break;
         case 2:
            action.data.resize(kChunkSize - 1, 'x');
            action.data.push_back('\x1b');
            break;
         default:
            break;
         }
      }

      ASSERT_FALSE(loadActions(input, tempFile_));

      // asJson() also flushes, so the reference sees what getConsoleLines() does
      json::Object actionsJson;
      actions.asJson(&actionsJson);
      const Lines allLines = referenceConsoleLines(actionsJson);

      // loadFromFile() quietly drops what it can't read, which would leave
      // the pages below to compare less than was generated
      ASSERT_EQ(input.size(), actionsJson["data"].getArray().getSize());

      // a sample of the pages that could be asked for
      for (int page = 0; page < 60; page++)
      {
         int limit = limits[pick(limits.size())];
         int offset = offsets[pick(offsets.size())];
         int maxChars = maxCharses[pick(maxCharses.size())];
         bool fromBottom = pick(2) == 0;

         ASSERT_EQ(
            referencePage(allLines, limit, offset, fromBottom, maxChars),
            actions.getConsoleLines(limit, offset, fromBottom, maxChars))
            << "iteration " << iteration
            << ", limit " << limit
            << ", offset " << offset
            << ", fromBottom " << fromBottom
            << ", maxChars " << maxChars
            << ", actions " << actionsJson.write();
      }
   }
}

} // namespace tests
} // namespace session
} // namespace r
} // namespace rstudio
