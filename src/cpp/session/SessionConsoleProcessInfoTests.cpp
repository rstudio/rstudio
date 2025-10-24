/*
 * SessionConsoleProcessInfoTests.cpp
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
#include <core/system/Process.hpp>

#include <boost/filesystem.hpp>

#include <session/SessionConsoleProcessInfo.hpp>

#include <boost/lexical_cast.hpp>
#include <boost/optional/optional_io.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace console_process {
namespace tests {

using namespace console_process;

namespace {

const std::string emptyStr;
const std::string caption("Terminal 1");
const std::string title("/Users/roger/R");
const std::string handle1("unit-test01");
const std::string bogusHandle1("unit-test03");
const int sequence = 1;
const InteractionMode mode = InteractionAlways;
const TerminalShell::ShellType shellType = TerminalShell::ShellType::Default;
const std::string channelId("some channel Id");
const bool altActive = false;

#ifdef _WIN32
const core::FilePath cwd("C:\\windows\\temp");
const core::FilePath altCwd("C:\\windows\\system32");
const core::FilePath cwdLink("C:\\windows\\temp\\cwd");
#else
const core::FilePath cwd("/usr/local");
const core::FilePath altCwd("/usr/stuff");
const core::FilePath cwdLink("/tmp/cwd");
#endif

const int cols = core::system::kDefaultCols;
const int rows = core::system::kDefaultRows;
const bool zombie = false;
const bool trackEnv = false;

const int maxLines = kDefaultTerminalMaxOutputLines;

bool testHandle(const std::string& handle)
{
   return handle == handle1;
}

// helper returns true if ConsoleProcessInfo objs have same field values
bool sameCpi(const ConsoleProcessInfo& first, const ConsoleProcessInfo& second)
{
   return (first.getCaption() == second.getCaption() &&
           first.getTitle() == second.getTitle() &&
           first.getHandle() == second.getHandle() &&
           first.getTerminalSequence() == second.getTerminalSequence() &&
           first.getAllowRestart() == second.getAllowRestart() &&
           first.getInteractionMode() == second.getInteractionMode() &&
           first.getMaxOutputLines() == second.getMaxOutputLines() &&
           first.getShowOnOutput() == second.getShowOnOutput() &&
           first.getExitCode()  == second.getExitCode() &&
           first.getHasChildProcs() == second.getHasChildProcs() &&
           first.getShellType() == second.getShellType() &&
           first.getChannelMode() == second.getChannelMode() &&
           first.getChannelId() == second.getChannelId() &&
           first.getAltBufferActive() == second.getAltBufferActive() &&
           first.getCwd() == second.getCwd() &&
           first.getCols() == second.getCols() &&
           first.getRows() == second.getRows() &&
           first.getRestarted() == second.getRestarted() &&
           first.getAutoClose() == second.getAutoClose() &&
           first.getZombie() == second.getZombie() &&
           first.getTrackEnv() == second.getTrackEnv());
}

} // anonymous namespace


TEST(ConsoleProcessInfoTest, CreationInitializesPropertiesCorrectly) {
   ConsoleProcessInfo cpi(caption, title, handle1, sequence,
                          shellType, altActive, cwd, cols, rows, zombie, trackEnv);

   EXPECT_EQ(caption, cpi.getCaption());
   EXPECT_EQ(title, cpi.getTitle());
   EXPECT_EQ(handle1, cpi.getHandle());
   EXPECT_EQ(sequence, cpi.getTerminalSequence());
   EXPECT_TRUE(cpi.getAllowRestart());
   EXPECT_EQ(InteractionAlways, cpi.getInteractionMode());
   EXPECT_EQ(maxLines, cpi.getMaxOutputLines());

   EXPECT_FALSE(cpi.getShowOnOutput());
   EXPECT_FALSE(cpi.getExitCode());
#ifdef _WIN32
   EXPECT_FALSE(cpi.getHasChildProcs());
#else
   EXPECT_TRUE(cpi.getHasChildProcs());
#endif
   EXPECT_EQ(shellType, cpi.getShellType());
   EXPECT_EQ(Rpc, cpi.getChannelMode());
   EXPECT_TRUE(cpi.getChannelId().empty());
   EXPECT_EQ(altActive, cpi.getAltBufferActive());
   EXPECT_EQ(cwd, cpi.getCwd());
   EXPECT_EQ(cols, cpi.getCols());
   EXPECT_EQ(rows, cpi.getRows());

   EXPECT_FALSE(cpi.getRestarted());
   cpi.setRestarted(true);
   EXPECT_TRUE(cpi.getRestarted());

   EXPECT_EQ(DefaultAutoClose, cpi.getAutoClose());
   cpi.setAutoClose(NeverAutoClose);
   EXPECT_EQ(NeverAutoClose, cpi.getAutoClose());

   EXPECT_EQ(zombie, cpi.getZombie());
   EXPECT_EQ(trackEnv, cpi.getTrackEnv());
}

TEST(ConsoleProcessInfoTest, EmptyHandleIsGeneratedWhenRequested) {
   ConsoleProcessInfo cpi(emptyStr /*caption*/, InteractionNever, 0 /*maxLines */);

   std::string handle = cpi.getHandle();
   EXPECT_TRUE(handle.empty());
   cpi.ensureHandle();
   handle = cpi.getHandle();
   EXPECT_FALSE(handle.empty());
}

TEST(ConsoleProcessInfoTest, PropertiesCanBeModifiedAfterCreation) {
   ConsoleProcessInfo cpi(caption, title, handle1, sequence, shellType,
                          altActive, cwd, cols, rows, zombie, trackEnv);

   std::string altCaption("other caption");
   EXPECT_NE(caption, altCaption);
   cpi.setCaption(altCaption);
   EXPECT_EQ(altCaption, cpi.getCaption());

   std::string altTitle("other title");
   EXPECT_NE(title, altTitle);
   cpi.setTitle(altTitle);
   EXPECT_EQ(altTitle, cpi.getTitle());

   int altSequence = sequence + 1;
   cpi.setTerminalSequence(altSequence);
   EXPECT_EQ(altSequence, cpi.getTerminalSequence());

   bool altAllowRestart = false;
   cpi.setAllowRestart(altAllowRestart);
   EXPECT_EQ(altAllowRestart, cpi.getAllowRestart());

   InteractionMode altMode = InteractionNever;
   EXPECT_NE(altMode, mode);
   cpi.setInteractionMode(altMode);
   EXPECT_EQ(altMode, cpi.getInteractionMode());

   int altMax = maxLines + 1;
   cpi.setMaxOutputLines(altMax);
   EXPECT_EQ(altMax, cpi.getMaxOutputLines());

   bool altShowOnOutput = !cpi.getShowOnOutput();
   cpi.setShowOnOutput(altShowOnOutput);
   EXPECT_EQ(altShowOnOutput, cpi.getShowOnOutput());

   bool altHasChildProcs = !cpi.getHasChildProcs();
   cpi.setHasChildProcs(altHasChildProcs);
   EXPECT_EQ(altHasChildProcs, cpi.getHasChildProcs());

   ChannelMode altChannelMode = Websocket;
   std::string altChannelModeId = "Some other id";
   cpi.setChannelMode(altChannelMode, altChannelModeId);
   EXPECT_EQ(altChannelMode, cpi.getChannelMode());
   EXPECT_EQ(altChannelModeId, cpi.getChannelId());

   cpi.setCwd(altCwd);
   EXPECT_EQ(altCwd, cpi.getCwd());

   cpi.setAutoClose(AlwaysAutoClose);
   EXPECT_EQ(AlwaysAutoClose, cpi.getAutoClose());

   cpi.setZombie(true);
   EXPECT_TRUE(cpi.getZombie());

   cpi.setTrackEnv(true);
   EXPECT_TRUE(cpi.getTrackEnv());
}

TEST(ConsoleProcessInfoTest, ExitCodeCanBeUpdatedAndRetrieved) {
   ConsoleProcessInfo cpi(caption, title, handle1, sequence, shellType,
                          altActive, cwd, cols, rows, zombie, trackEnv);

   const int exitCode = 14;
   cpi.setExitCode(exitCode);
   EXPECT_TRUE(cpi.getExitCode());
   EXPECT_EQ(exitCode, *cpi.getExitCode());
}

TEST(ConsoleProcessInfoTest, MetadataCanBeSavedAndRestored) {
   std::string orig = ConsoleProcessInfo::loadConsoleProcessMetadata();
   std::string newMetadata = "once upon a time";
   ConsoleProcessInfo::saveConsoleProcesses(newMetadata);
   std::string newSaved = ConsoleProcessInfo::loadConsoleProcessMetadata();
   EXPECT_EQ(newMetadata, newSaved);
   ConsoleProcessInfo::saveConsoleProcesses(orig);
   newSaved = ConsoleProcessInfo::loadConsoleProcessMetadata();
   EXPECT_EQ(orig, newSaved);
}

TEST(ConsoleProcessInfoTest, DifferentExitCodesAreNotEqual) {
   ConsoleProcessInfo cpiFirst(caption, title, handle1, sequence, shellType,
                               altActive, cwd, cols, rows, zombie, trackEnv);
   ConsoleProcessInfo cpiSecond(caption, title, handle1, sequence, shellType,
                                altActive, cwd, cols, rows, zombie, trackEnv);

   cpiFirst.setExitCode(1);
   cpiSecond.setExitCode(12);
   EXPECT_FALSE(sameCpi(cpiFirst, cpiSecond));
}

TEST(ConsoleProcessInfoTest, InfoSerializesToAndFromJson) {
   ConsoleProcessInfo cpiOrig(caption, title, handle1, sequence, shellType,
                               altActive, cwd, cols, rows, zombie, trackEnv);

   core::json::Object origJson = cpiOrig.toJson(PersistentSerialization);
   boost::shared_ptr<ConsoleProcessInfo> pCpiRestored =
         ConsoleProcessInfo::fromJson(origJson);
   EXPECT_TRUE(pCpiRestored);
   EXPECT_TRUE(sameCpi(cpiOrig, *pCpiRestored));
}

#ifdef __linux__
TEST(ConsoleProcessInfoTest, SymlinkCwdResolvesCorrectlyWhenRestored) {
   // ensure file does not exist so symlink can be created
   cwdLink.remove();
   boost::filesystem::create_directory_symlink(cwd.getAbsolutePath(), cwdLink.getAbsolutePath());
   ConsoleProcessInfo cpiOrig(caption, title, handle1, sequence, shellType,
                          altActive, cwdLink, cols, rows, zombie, trackEnv);

   // blow away anything that might have been left over from a previous
   // failed run
   cpiOrig.deleteLogFile();

   EXPECT_EQ(cwdLink.getAbsolutePath(), cpiOrig.getCwd().getAbsolutePath());

   core::json::Object origJson = cpiOrig.toJson(PersistentSerialization);
   boost::shared_ptr<ConsoleProcessInfo> pCpiRestored =
         ConsoleProcessInfo::fromJson(origJson);
   EXPECT_TRUE(pCpiRestored);

   // restored cwd should be the symlink target
   EXPECT_EQ(cpiOrig.getCwd().resolveSymlink().getAbsolutePath(), pCpiRestored->getCwd().getAbsolutePath());
   cwdLink.remove();
}
#endif

TEST(ConsoleProcessInfoTest, NonTerminalBuffersAreStoredInJson) {
   // Non-terminal buffers are saved directly in this object and end up
   // persisted to the JSON. So to validate this we need to save some
   // output, persist to JSON, restore from JSON, and see that we get
   // back what we saved (trimmed based on maxLines). Be nice to abstract
   // all of this away, but bigger fish and all of that.
   ConsoleProcessInfo cpi(caption, mode, maxLines);

   // bufferedOutput is the accessor for the non-terminal buffer cache
   std::string orig = cpi.bufferedOutput();
   EXPECT_TRUE(orig.empty());

   std::string strOrig("one\ntwo\nthree\n");

   // kNoTerminal mode buffer returns everything after the first \n
   // in the buffer
   cpi.appendToOutputBuffer('\n');
   cpi.appendToOutputBuffer(strOrig);
   std::string loaded = cpi.bufferedOutput();
   EXPECT_EQ(strOrig, loaded);

   // add some more
   std::string orig2("four\nfive\nsix");
   cpi.appendToOutputBuffer(orig2);
   loaded = cpi.bufferedOutput();
   strOrig.append(orig2);
   EXPECT_EQ(strOrig, loaded);
}

TEST(ConsoleProcessInfoTest, TerminalBuffersAreStoredExternally) {
   // terminal sequence other than kNoTerminal triggers terminal
   // behavior where buffer are saved to an external file instead of
   // in the JSON. Same comment on the leaky abstractions here as for
   // previous non-terminal test.
   ConsoleProcessInfo cpi(caption, title, handle1, sequence, shellType,
                          altActive, cwd, cols, rows, zombie, trackEnv);

   // blow away anything that might have been left over from a previous
   // failed run
   cpi.deleteLogFile();

   // getSavedBufferChunk is the accessor for the terminal buffer cache
   bool moreAvailable;
   std::string loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(loaded.empty());
   EXPECT_FALSE(moreAvailable);

   std::string orig = "one\ntwo\nthree\nfour\nfive";
   EXPECT_LT(orig.length(), kOutputBufferSize);
   cpi.appendToOutputBuffer(orig);
   loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_FALSE(moreAvailable);
   EXPECT_EQ(orig, loaded);

   std::string orig2 = "\nsix\nseven\neight\nnine\nten\n";
   EXPECT_LT(orig.length() + orig2.length(), kOutputBufferSize);
   cpi.appendToOutputBuffer(orig2);
   loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   orig.append(orig2);
   EXPECT_FALSE(moreAvailable);
   EXPECT_EQ(orig, loaded);

   cpi.deleteLogFile();
   loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(loaded.empty());
   EXPECT_FALSE(moreAvailable);
}

TEST(ConsoleProcessInfoTest, MultipleChunksAreStoredCorrectly) {
   ConsoleProcessInfo cpi(caption, title, handle1, sequence, shellType,
                          altActive, cwd, cols, rows, zombie, trackEnv);

   // blow away anything that might have been left over from a previous
   // failed run
   cpi.deleteLogFile();

   bool moreAvailable;
   std::string loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(loaded.empty());
   EXPECT_FALSE(moreAvailable);

   // fill buffer with less than one chunk
   std::string firstChunk(kOutputBufferSize - 1, 'a');
   EXPECT_LT(firstChunk.length(), kOutputBufferSize);
   cpi.appendToOutputBuffer(firstChunk);
   loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_FALSE(moreAvailable);
   EXPECT_EQ(firstChunk, loaded);

   // pad to exactly one chunk
   firstChunk += 'a';
   EXPECT_EQ(kOutputBufferSize, firstChunk.length());
   cpi.appendToOutputBuffer("a");
   loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_FALSE(moreAvailable);
   EXPECT_EQ(firstChunk, loaded);

   // add partial second chunk
   std::string secondChunk(2, 'b');
   EXPECT_LT(secondChunk.length(), kOutputBufferSize);
   cpi.appendToOutputBuffer(secondChunk);
   loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(moreAvailable);
   EXPECT_EQ(firstChunk, loaded);
   loaded = cpi.getSavedBufferChunk(1, &moreAvailable);
   EXPECT_FALSE(moreAvailable);
   EXPECT_EQ(secondChunk, loaded);

   // finish second chunk and add a single character as third chunk
   std::string finishSecond(kOutputBufferSize - secondChunk.length(), 'b');
   EXPECT_EQ(kOutputBufferSize, finishSecond.length() + secondChunk.length());
   cpi.appendToOutputBuffer(finishSecond);

   // try to read non-existent third chunk
   std::string thirdChunk = cpi.getSavedBufferChunk(3, &moreAvailable);
   EXPECT_FALSE(moreAvailable);
   EXPECT_EQ(0u, thirdChunk.length());

   // add a single character third chunk and read it
   thirdChunk = "c";
   cpi.appendToOutputBuffer(thirdChunk);
   loaded = cpi.getSavedBufferChunk(2, &moreAvailable);
   EXPECT_FALSE(moreAvailable);
   EXPECT_EQ(thirdChunk, loaded);

   // cleanup
   cpi.deleteLogFile();
   loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(loaded.empty());
   EXPECT_FALSE(moreAvailable);
}

TEST(ConsoleProcessInfoTest, OrphanedLogFilesAreDeleted) {
   ConsoleProcessInfo cpiGood(caption, title, handle1, sequence, shellType,
                              altActive, cwd, cols, rows, zombie, trackEnv);
   ConsoleProcessInfo cpiBad(caption, title, bogusHandle1, sequence, shellType,
                             altActive, cwd, cols, rows, zombie, trackEnv);

   std::string orig1("hello how are you?\nthat is good\nhave a nice day");
   EXPECT_LT(orig1.length(), kOutputBufferSize);
   std::string bogus1("doom");
   EXPECT_LT(bogus1.length(), kOutputBufferSize);

   cpiGood.appendToOutputBuffer(orig1);
   cpiBad.appendToOutputBuffer(bogus1);

   bool moreAvailable;
   std::string loadedGood = cpiGood.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_EQ(orig1, loadedGood);
   EXPECT_FALSE(moreAvailable);
   std::string loadedBad = cpiBad.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_EQ(bogus1, loadedBad);
   EXPECT_FALSE(moreAvailable);

   cpiGood.deleteOrphanedLogs(testHandle);
   cpiBad.deleteOrphanedLogs(testHandle);

   loadedGood = cpiGood.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_EQ(orig1, loadedGood);
   EXPECT_FALSE(moreAvailable);
   loadedBad = cpiBad.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_FALSE(moreAvailable);
   EXPECT_TRUE(loadedBad.empty());

   cpiGood.deleteLogFile();
   cpiBad.deleteLogFile();
}

TEST(ConsoleProcessInfoTest, TrimmableBuffersRespectMaxLineLimit) {
   const int smallMaxLines = 5;
   ConsoleProcessInfo cpi(caption, title, handle1, sequence, shellType,
                          altActive, cwd, cols, rows, zombie, trackEnv);
   cpi.setMaxOutputLines(smallMaxLines);

   // blow away anything that might have been left over from a previous
   // failed run
   cpi.deleteLogFile();

   bool moreAvailable;
   std::string loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(loaded.empty());
   EXPECT_FALSE(moreAvailable);

   // fill buffer with more than one chunk and more lines than the
   // maximum specified; each line is one-chunk in length, and filled
   // with digits corresponding to the line #, "0000...", "1111..."
   for (int i = 0; i < smallMaxLines + 2; i++) {
      std::string str = boost::lexical_cast<std::string>(i);
      std::string line(kOutputBufferSize - 1, str[0]);
      line += "\n";
      cpi.appendToOutputBuffer(line);
   }

   // now when we request the first chunk, we expect the buffer to
      // be trimmed to have smallMaxLines + 1 before first chunk is returned
      std::string expected(kOutputBufferSize -1, '2');
      expected = std::string("\n") + expected;
      loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(moreAvailable);
   EXPECT_EQ(expected, loaded);

      // cleanup
      cpi.deleteLogFile();
      loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(loaded.empty());
   EXPECT_FALSE(moreAvailable);
}

TEST(ConsoleProcessInfoTest, FullBufferLoadsWithMaxLineLimit) {
   const int smallMaxLines = 3;
   ConsoleProcessInfo cpi(caption, title, handle1, sequence, shellType,
                          altActive, cwd, cols, rows, zombie, trackEnv);
   cpi.setMaxOutputLines(smallMaxLines);

   // blow away anything that might have been left over from a previous
   // failed run
   cpi.deleteLogFile();

   bool moreAvailable;
   std::string loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(loaded.empty());
   EXPECT_FALSE(moreAvailable);

   // fill buffer with more than one chunk and more lines than the
   // maximum specified; each line is one-chunk in length, and filled
   // with digits corresponding to the line #, "0000...", "1111..."
   for (int i = 0; i < smallMaxLines + 2; i++) 
   {
      std::string str = boost::lexical_cast<std::string>(i);
      std::string line(kOutputBufferSize - 1, str[0]);
      line += "\n";
      cpi.appendToOutputBuffer(line);
   }

   // now when we request the full buffer, we expect the buffer to
   // be trimmed to have smallMaxLines + 1
   std::string expected2(kOutputBufferSize -1, '2');
   std::string expected3(kOutputBufferSize -1, '3');
   std::string expected4(kOutputBufferSize -1, '4');
   std::string expected = std::string("\n") + expected2 + std::string("\n") +
         expected3 + std::string("\n") + expected4 + std::string("\n");
   loaded = cpi.getFullSavedBuffer();
   EXPECT_EQ(expected, loaded);

   // cleanup
   cpi.deleteLogFile();
   loaded = cpi.getSavedBufferChunk(0, &moreAvailable);
   EXPECT_TRUE(loaded.empty());
   EXPECT_FALSE(moreAvailable);
}

TEST(ConsoleProcessInfoTest, LineCountingHandlesPartialLines) {
   const int lines = 10;
   ConsoleProcessInfo cpi(caption, title, handle1, sequence, shellType,
                          altActive, cwd, cols, rows, zombie, trackEnv);
   cpi.setMaxOutputLines(lines * 2);

   // blow away anything that might have been left over from a previous
   // failed run
   cpi.deleteLogFile();

   // fill buffer with several lines of text
   // maximum specified; each line is one-chunk in length, and filled
   // with digits corresponding to the line #, "0000...", "1111..."
   for (int i = 0; i < lines; i++) 
   {
      std::string str = boost::lexical_cast<std::string>(i);
      std::string line(10, str[0]);
      line += "\n";
      cpi.appendToOutputBuffer(line);
   }

   // line count includes any partial lines at the end (even a zero-
   // length line after a final \n)
   EXPECT_EQ(lines + 1, cpi.getBufferLineCount());

   // cleanup
   cpi.deleteLogFile();
}

} // namespace tests
} // namespace console_process
} // namespace session
} // namespace rstudio
