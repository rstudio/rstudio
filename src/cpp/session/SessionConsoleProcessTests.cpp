/*
 * SessionConsoleProcessTests.cpp
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

#include <session/SessionConsoleProcess.hpp>

#include <gsl/gsl-lite.hpp>

#include <boost/lexical_cast.hpp>
#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace console_process {

#ifdef _WIN32
// Defined in SessionConsoleProcess.cpp; strips the conhost startup clear (only
// when preceded solely by leading control) from one post-restart output chunk.
// Returns true to keep watching later chunks, false to stop.
bool stripRestartClearFromChunk(std::string* pChunk);
// True if the output is solely leading control sequences (a clear may still be
// coming); false once visible content appears (the startup clear is absent).
bool isPureLeadingControl(const std::string& s);
#endif

namespace tests {

using namespace console_process;

// Setup for tests
boost::shared_ptr<ConsoleProcessInfo> pInfo = boost::make_shared<ConsoleProcessInfo>("Test Caption", InteractionNever, 0);
boost::shared_ptr<ConsoleProcess> pCP;
boost::shared_ptr<core::system::ProcessOptions> pOptions = boost::make_shared<core::system::ProcessOptions>();

void setUp() {
   std::string command = "echo";
   pCP = ConsoleProcess::create(command, *pOptions, pInfo);
}

class ConsoleProcessTest : public testing::Test {
protected:
   void SetUp() override {
      setUp();
   }
};


TEST_F(ConsoleProcessTest, EmptyQueueReturnsEmptyInput) {
   ConsoleProcess::Input next = pCP->dequeInput();
   EXPECT_TRUE(next.empty());
}

TEST_F(ConsoleProcessTest, InterruptSignalPassesThroughQueue) {
   ConsoleProcess::Input interrupt;
   interrupt.interrupt = true;
   pCP->enqueInput(interrupt);
   ConsoleProcess::Input next = pCP->dequeInput();
   EXPECT_FALSE(next.empty());
   EXPECT_TRUE(next.interrupt);
   next = pCP->dequeInput();
   EXPECT_TRUE(next.empty());
}

TEST_F(ConsoleProcessTest, UnsequencedInputFollowsFifoOrder) {
   const std::string orig = "abcdefghijklmnopqrstuvwxyz";

   for (char ch : orig) 
   {
      std::string oneChar;
      oneChar.push_back(ch);
      pCP->enqueInput(ConsoleProcess::Input(oneChar));
   }

   std::string result;
   ConsoleProcess::Input next = pCP->dequeInput();
   while (!next.empty()) 
   {
      EXPECT_FALSE(next.interrupt);
      EXPECT_TRUE(next.sequence == kIgnoreSequence);
      result += next.text;
      next = pCP->dequeInput();
   }

   EXPECT_EQ(orig, result);
}

TEST_F(ConsoleProcessTest, SequencedInputFollowsFifoOrder) {
   const std::string orig = "abcdefghijklmnopqrstuvwxyz";

   for (size_t i = 0; i < orig.length(); i++) 
   {
      std::string oneChar;
      oneChar.push_back(orig[i]);
      pCP->enqueInput(ConsoleProcess::Input(gsl::narrow_cast<int>(i), oneChar));
   }

   std::string result;
   ConsoleProcess::Input next = pCP->dequeInput();
   int seq = 0;
   while (!next.empty()) 
   {
      EXPECT_FALSE(next.interrupt);
      EXPECT_TRUE(next.sequence == seq++);
      result += next.text;
      next = pCP->dequeInput();
   }

   EXPECT_EQ(orig, result);
}

TEST_F(ConsoleProcessTest, MixedSequenceInputReordersCorrectly) {
   const std::string expected = "HELLO WORLD!";
   std::vector<ConsoleProcess::Input> input;
   input.emplace_back(1,  std::string("E"));
   input.emplace_back(0,  std::string("H"));
   input.emplace_back(2,  std::string("L"));
   input.emplace_back(3,  std::string("L"));
   input.emplace_back(4,  std::string("O"));
   input.emplace_back(5,  std::string(" "));
   input.emplace_back(7,  std::string("O"));
   input.emplace_back(6,  std::string("W"));
   input.emplace_back(8,  std::string("R"));
   input.emplace_back(9,  std::string("L"));
   input.emplace_back(10, std::string("D"));
   input.emplace_back(11, std::string("!"));

   for (const auto &cpi : input) 
   {
      pCP->enqueInput(cpi);
   }

   std::string result;
   ConsoleProcess::Input next = pCP->dequeInput();
   int seq = 0;
   while (!next.empty()) 
   {
      EXPECT_FALSE(next.interrupt);
      EXPECT_TRUE(next.sequence == seq++);
      result += next.text;
      next = pCP->dequeInput();
   }

   EXPECT_EQ(expected, result);
}

TEST_F(ConsoleProcessTest, AutoflushHandlesUnresolvedGaps) {
   std::vector<ConsoleProcess::Input> input;
   std::string expected;

   // intentionally skipping item "0" to prevent pulling from queueA
   int lastAdded = kIgnoreSequence;
   for (size_t i = 1; i < kAutoFlushLength + 5; i++) {
      std::string item = boost::lexical_cast<std::string>(i);
      expected += item;
      input.emplace_back(gsl::narrow_cast<int>(i), item);
      lastAdded = gsl::narrow_cast<int>(i);
   }

   for (const auto &cpi : input) 
   {
      pCP->enqueInput(cpi);
   }

   std::string result;
   ConsoleProcess::Input next = pCP->dequeInput();
   EXPECT_FALSE(next.empty());
   while (!next.empty()) 
   {
      EXPECT_FALSE(next.interrupt);
      result += next.text;
      next = pCP->dequeInput();
   }

   EXPECT_EQ(expected, result);

   // make sure we can resume adding and removing
   pCP->enqueInput(ConsoleProcess::Input(lastAdded + 1, "HELLO"));
   pCP->enqueInput(ConsoleProcess::Input(lastAdded + 2, " WORLD"));

   result.clear();
   next = pCP->dequeInput();
   EXPECT_FALSE(next.empty());
   while (!next.empty()) 
   {
      result += next.text;
      next = pCP->dequeInput();
   }

   EXPECT_EQ("HELLO WORLD", result);
}

TEST_F(ConsoleProcessTest, ExplicitFlushReturnsInputWithGapsAndResetsSequence) {
   std::vector<ConsoleProcess::Input> input;
   std::string expected;

   // intentionally skipping item "3" to prevent pulling from queue
   for (int i = 1; i < 10; i++) 
   {
      if (i != 3) 
      {
         std::string item = boost::lexical_cast<std::string>(i);
         expected += item;
         input.emplace_back(i, item);
      }
   }
   std::string flushText = "FLUSH"; // value not significant
   input.emplace_back(kFlushSequence, flushText);
   expected += flushText;
   std::string postFlushText = "post-flush"; // value not significant
   input.emplace_back(0, postFlushText);
   expected += postFlushText;

   for (const auto &cpi : input) 
   {
      pCP->enqueInput(cpi);
   }

   std::string result;
   ConsoleProcess::Input next = pCP->dequeInput();
   EXPECT_FALSE(next.empty());
   while (!next.empty()) 
   {
      EXPECT_FALSE(next.interrupt);
      result += next.text;

      if (next.text == postFlushText)
         EXPECT_TRUE(next.sequence == 0);
      else
         EXPECT_TRUE(next.sequence == kIgnoreSequence);

      next = pCP->dequeInput();
   }

   EXPECT_EQ(expected, result);
}

#ifdef _WIN32
TEST(ConsoleProcessConPty, StripsStartupClearAtHead)
{
   // Bytes a restarted Git Bash emits under ConPTY: hide cursor, clear screen,
   // reset SGR, home, then title + prompt (microsoft/terminal#4252). The clear
   // (preceded only by mode-sets) is removed; everything else is preserved.
   std::string out = "\x1b[?2004h\x1b[?25l\x1b[2J\x1b[m\x1b[H\x1b]0;t\x07PS1$ ";
   EXPECT_FALSE(stripRestartClearFromChunk(&out));   // a clear ends the window
   EXPECT_EQ(out, "\x1b[?2004h\x1b[?25l\x1b]0;t\x07PS1$ ");
   EXPECT_EQ(out.find("\x1b[2J"), std::string::npos);
}

TEST(ConsoleProcessConPty, KeepsUserClearAfterContent)
{
   // A prompt and a user `clear` coalesced into one chunk: the clear follows
   // visible content, so it is NOT the startup clear and must be left intact.
   std::string out = "\x1b[32m$ \x1b[mecho hi\r\nhi\r\n$ \x1b[2J\x1b[H";
   const std::string orig = out;
   EXPECT_FALSE(stripRestartClearFromChunk(&out));
   EXPECT_EQ(out, orig);                             // user clear preserved
}

TEST(ConsoleProcessConPty, KeepsWatchingPureLeadingControl)
{
   // Only mode-sets so far: the startup clear may still arrive in a later chunk.
   std::string out = "\x1b[?2004h\x1b[?25l";
   const std::string orig = out;
   EXPECT_TRUE(stripRestartClearFromChunk(&out));    // keep watching
   EXPECT_EQ(out, orig);
}

TEST(ConsoleProcessConPty, StopsOnContentWithoutClear)
{
   // A prompt arrives with no clear: the startup clear is absent, stop watching
   // so a later user clear is never stripped.
   std::string out = "\x1b[?2004h\x1b[32mtomto@pc$ ";
   const std::string orig = out;
   EXPECT_FALSE(stripRestartClearFromChunk(&out));
   EXPECT_EQ(out, orig);
}

TEST(ConsoleProcessConPty, StripsScrollbackClearVariant)
{
   // CSI 3J (clear scrollback) at the head is also removed.
   std::string out = "\x1b[3J\x1b[Hy";
   EXPECT_FALSE(stripRestartClearFromChunk(&out));
   EXPECT_EQ(out, "y");
}

TEST(ConsoleProcessConPty, LeadingControlIsRecognized)
{
   EXPECT_TRUE(isPureLeadingControl("\x1b[?2004h\x1b[?25l"));
   EXPECT_TRUE(isPureLeadingControl("\x1b[?2004h\x1b[")); // incomplete tail
   EXPECT_FALSE(isPureLeadingControl("\x1b[?2004h\x1b[32mtomto@pc$ "));
   EXPECT_FALSE(isPureLeadingControl("\x1b]0;title\x07")); // OSC is content
}

TEST(ConsoleProcessConPty, StripsStartupClearArrivingInLaterChunk)
{
   // The startup clear can land a chunk after the initial mode-sets. The first
   // chunk (pure leading control) must keep the window open without altering
   // anything; the clear in the following chunk is then stripped. This is the
   // cross-chunk path the "keep watching" return value exists for.
   std::string first = "\x1b[?2004h\x1b[?25l";
   const std::string firstOrig = first;
   EXPECT_TRUE(stripRestartClearFromChunk(&first));   // keep watching
   EXPECT_EQ(first, firstOrig);                        // nothing stripped yet

   std::string second = "\x1b[2J\x1b[H$ ";
   EXPECT_FALSE(stripRestartClearFromChunk(&second));  // clear handled; stop
   EXPECT_EQ(second, "$ ");
}
#endif // _WIN32

} // namespace tests
} // namespace console_process
} // namespace session
} // namespace rstudio
