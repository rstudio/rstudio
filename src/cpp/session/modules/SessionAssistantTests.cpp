/*
 * SessionAssistantTests.cpp
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

#include "SessionAssistant.hpp"

#include <memory>
#include <vector>

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>
#include <core/system/Process.hpp>

namespace rstudio {
namespace session {
namespace tests {

using core::FilePath;
using modules::assistant::addPendingAgentRequestForTesting;
using modules::assistant::AgentStderrTail;
using modules::assistant::agentRequestTimeout;
using modules::assistant::agentStderrTail;
using modules::assistant::nesLanguageServerPath;
using modules::assistant::queueAgentNotificationForTesting;
using modules::assistant::queueAgentReplyForTesting;
using modules::assistant::resetForNewAgentForTesting;
using modules::assistant::runBackgroundProcessingForTesting;
using modules::assistant::ServicedClock;
using modules::assistant::writeQueuedAgentRequestsForTesting;
using std::chrono::milliseconds;
using std::chrono::seconds;
using std::chrono::steady_clock;

TEST(SessionAssistant, StderrTailKeepsShortTextUnchanged)
{
   EXPECT_EQ(agentStderrTail("Error: boom", 100), "Error: boom");
}

TEST(SessionAssistant, StderrTailAtLimitUnchanged)
{
   std::string text(10, 'x');
   EXPECT_EQ(agentStderrTail(text, 10), text);
}

TEST(SessionAssistant, StderrTailKeepsTailAndNotesDroppedByteCount)
{
   // Node.js prints the (minified, huge) offending source line before the
   // error and stack trace, so the tail is the part worth keeping.
   std::string text = std::string(90, 'x') + "Error: boom";
   EXPECT_EQ(agentStderrTail(text, 11),
             "[... 90 bytes truncated ...] Error: boom");
}

TEST(SessionAssistant, StderrTailDoesNotStartMidUtf8Character)
{
   // U+00E9 is 0xC3 0xA9 in UTF-8. A limit of 3 would start the tail on the
   // continuation byte 0xA9; the tail must skip forward past it so the
   // result stays valid UTF-8 (it is later embedded in JSON).
   std::string text = "aa\xC3\xA9zz";
   EXPECT_EQ(agentStderrTail(text, 3), "[... 4 bytes truncated ...] zz");
}

TEST(SessionAssistant, StderrAccumulatorUnderLimitHasNoMarker)
{
   AgentStderrTail acc(10);
   acc.append("abc");
   acc.append("def");
   EXPECT_EQ(acc.text(), "abcdef");
}

TEST(SessionAssistant, StderrAccumulatorCountsDroppedBytesCumulatively)
{
   // The marker's count must track raw bytes dropped across appends -- it
   // must not count previously-added marker text as stderr, nor reset when a
   // later chunk arrives.
   AgentStderrTail acc(10);
   acc.append(std::string(25, 'x') + "END");
   EXPECT_EQ(acc.text(), "[... 18 bytes truncated ...] xxxxxxxEND");
   acc.append("12345");
   EXPECT_EQ(acc.text(), "[... 23 bytes truncated ...] xxEND12345");
}

TEST(SessionAssistant, StderrAccumulatorSetReplacesAndResetsCount)
{
   AgentStderrTail acc(10);
   acc.append(std::string(20, 'x'));
   acc.set("boom");
   EXPECT_EQ(acc.text(), "boom");
}

TEST(SessionAssistant, StderrAccumulatorClearEmpties)
{
   AgentStderrTail acc(10);
   acc.append(std::string(20, 'x'));
   acc.clear();
   EXPECT_EQ(acc.text(), "");
   acc.append("ok");
   EXPECT_EQ(acc.text(), "ok");
}

TEST(SessionAssistant, StderrAccumulatorSkipsUtf8ContinuationAtCut)
{
   // U+00E9 is 0xC3 0xA9 in UTF-8; the cut lands on the continuation byte
   // and must move past it, counting it as dropped.
   AgentStderrTail acc(3);
   acc.append("aa\xC3\xA9zz");
   EXPECT_EQ(acc.text(), "[... 4 bytes truncated ...] zz");
}

namespace {

// Creates a NES language server script named `fileName` under a fresh
// temporary installation directory, and returns that directory.
FilePath createNesInstallation(const std::string& fileName)
{
   FilePath installDir;
   FilePath::tempFilePath(installDir);

   FilePath script = installDir.completeChildPath("dist/nes").completeChildPath(fileName);
   script.getParent().ensureDirectory();
   core::writeStringToFile(script, "// mock language server");

   return installDir;
}

} // anonymous namespace

TEST(SessionAssistant, NesLanguageServerPathEmptyWhenNoInstallationLocated)
{
   EXPECT_TRUE(nesLanguageServerPath(FilePath()).isEmpty());
}

TEST(SessionAssistant, NesLanguageServerPathEmptyWhenInstallationHasNoNesScript)
{
   FilePath installDir;
   FilePath::tempFilePath(installDir);
   installDir.ensureDirectory();

   EXPECT_TRUE(nesLanguageServerPath(installDir).isEmpty());

   installDir.removeIfExists();
}

TEST(SessionAssistant, NesLanguageServerPathResolvesWithinGivenInstallation)
{
   // The script must be found under the installation that was passed in --
   // e.g. a system-wide or bundled install -- rather than under a fixed
   // location.
   FilePath installDir = createNesInstallation("language-server.cjs");

   EXPECT_EQ(nesLanguageServerPath(installDir).getAbsolutePath(),
             installDir.completeChildPath("dist/nes/language-server.cjs").getAbsolutePath());

   installDir.removeIfExists();
}

TEST(SessionAssistant, NesLanguageServerPathFallsBackToJsScript)
{
   FilePath installDir = createNesInstallation("language-server.js");

   EXPECT_EQ(nesLanguageServerPath(installDir).getAbsolutePath(),
             installDir.completeChildPath("dist/nes/language-server.js").getAbsolutePath());

   installDir.removeIfExists();
}

TEST(SessionAssistant, NesLanguageServerPathPrefersCjsScript)
{
   FilePath installDir = createNesInstallation("language-server.cjs");
   core::writeStringToFile(
      installDir.completeChildPath("dist/nes/language-server.js"), "// mock");

   EXPECT_EQ(nesLanguageServerPath(installDir).getAbsolutePath(),
             installDir.completeChildPath("dist/nes/language-server.cjs").getAbsolutePath());

   installDir.removeIfExists();
}

TEST(SessionAssistant, ServicedClockCountsShortGaps)
{
   auto start = steady_clock::now();
   ServicedClock clock(seconds(1));
   clock.tick(start);
   EXPECT_EQ(clock.elapsed(), steady_clock::duration::zero());

   clock.tick(start + milliseconds(500));
   clock.tick(start + milliseconds(1500));
   EXPECT_EQ(clock.elapsed(), milliseconds(1500));
}

TEST(SessionAssistant, ServicedClockCapsLongGaps)
{
   auto start = steady_clock::now();
   ServicedClock clock(seconds(1));
   clock.tick(start);
   clock.tick(start + seconds(30));
   EXPECT_EQ(clock.elapsed(), seconds(1));
}

TEST(SessionAssistant, ServicedClockIgnoresTimeGoingBackwards)
{
   auto start = steady_clock::now();
   ServicedClock clock(seconds(1));
   clock.tick(start);
   clock.tick(start - milliseconds(500));
   EXPECT_EQ(clock.elapsed(), steady_clock::duration::zero());

   clock.tick(start);
   EXPECT_EQ(clock.elapsed(), milliseconds(500));
}

TEST(SessionAssistant, InitializeGetsLongerTimeoutThanOtherRequests)
{
   EXPECT_EQ(agentRequestTimeout("initialize"), seconds(60));
   EXPECT_EQ(agentRequestTimeout("textDocument/inlineCompletion"), seconds(10));
}

namespace {

const char* const kCompletion = "textDocument/inlineCompletion";

// Simulated time for the background-processing passes below. It runs ahead of
// the real clock so each test's passes follow the previous test's.
steady_clock::time_point s_now = steady_clock::now();

// Run busy passes 100ms apart, as while R runs code that services events.
void serviceEventsFor(steady_clock::duration span)
{
   for (auto end = s_now + span; s_now < end; )
   {
      s_now += milliseconds(100);
      runBackgroundProcessingForTesting(false, s_now);
   }
}

void runIdlePass()
{
   runBackgroundProcessingForTesting(true, s_now);
}

// Register a pending request and collect its results. The results are shared
// with the request so that one a failed test leaves pending can still be
// resolved safely by a later test's sweep.
std::shared_ptr<std::vector<bool>> addRequest(const std::string& requestId,
                                              const std::string& method,
                                              bool written)
{
   auto results = std::make_shared<std::vector<bool>>();
   addPendingAgentRequestForTesting(requestId, method, written, s_now,
                                    [results](bool cancelled) { results->push_back(cancelled); });
   return results;
}

void queueReply(const std::string& requestId)
{
   queueAgentReplyForTesting(
      R"({"jsonrpc":"2.0","id":")" + requestId + R"(","result":{}})");
}

} // anonymous namespace

TEST(SessionAssistant, IdlePassKeepsRequestWithinTimeout)
{
   auto results = addRequest("within-timeout", kCompletion, true);

   serviceEventsFor(seconds(9));
   runIdlePass();
   EXPECT_TRUE(results->empty());

   queueReply("within-timeout");
   runIdlePass();
   EXPECT_EQ(*results, std::vector<bool>({ false }));
}

TEST(SessionAssistant, IdlePassCancelsRequestJustPastTimeout)
{
   auto results = addRequest("at-timeout", kCompletion, true);

   serviceEventsFor(seconds(10));
   runIdlePass();
   EXPECT_TRUE(results->empty());

   serviceEventsFor(milliseconds(100));
   runIdlePass();
   EXPECT_EQ(*results, std::vector<bool>({ true }));
}

TEST(SessionAssistant, IdlePassDeliversQueuedReplyToExpiredRequest)
{
   // A reply that arrived while R was busy must reach its request even
   // though the request is past its timeout by the time R is idle.
   auto results = addRequest("expired-with-reply", kCompletion, true);

   serviceEventsFor(seconds(11));
   queueReply("expired-with-reply");
   runIdlePass();

   EXPECT_EQ(*results, std::vector<bool>({ false }));
}

TEST(SessionAssistant, IdlePassCancelsExpiredRequestWithoutReply)
{
   auto results = addRequest("expired-without-reply", kCompletion, true);

   serviceEventsFor(seconds(11));
   runIdlePass();

   EXPECT_EQ(*results, std::vector<bool>({ true }));
}

TEST(SessionAssistant, BusyPassLeavesExpiredRequestPending)
{
   auto results = addRequest("expired-while-busy", kCompletion, true);

   serviceEventsFor(seconds(11));
   EXPECT_TRUE(results->empty());

   // An idle pass then cancels it, which also removes it from the pending map.
   runIdlePass();
   EXPECT_EQ(*results, std::vector<bool>({ true }));
}

TEST(SessionAssistant, TimeBlockedWithoutServicingEventsIsNotChargedToRequest)
{
   // While R is blocked the agent's output isn't drained, so a large reply
   // can stall the agent until the first pass afterwards; the request must
   // still be pending when the reply follows on the next pass.
   auto results = addRequest("blocked", kCompletion, true);

   s_now += seconds(30);
   runIdlePass();
   EXPECT_TRUE(results->empty());

   queueReply("blocked");
   serviceEventsFor(milliseconds(100));
   EXPECT_EQ(*results, std::vector<bool>({ false }));
}

TEST(SessionAssistant, RequestWrittenAfterBlockGetsFullTimeout)
{
   // The first pass after R was blocked writes queued requests before the
   // background handler runs; the gap before the write mustn't count.
   runBackgroundProcessingForTesting(false, s_now);
   s_now += seconds(30);
   auto results = addRequest("written-after-block", kCompletion, true);

   serviceEventsFor(milliseconds(9900));
   runIdlePass();
   EXPECT_TRUE(results->empty());

   queueReply("written-after-block");
   runIdlePass();
   EXPECT_EQ(*results, std::vector<bool>({ false }));
}

TEST(SessionAssistant, NewAgentCancelsPreviousAgentsRequests)
{
   auto held = addRequest("previous-agent-held", kCompletion, false);
   auto written = addRequest("previous-agent-written", kCompletion, true);

   resetForNewAgentForTesting();
   EXPECT_EQ(*held, std::vector<bool>({ true }));
   EXPECT_EQ(*written, std::vector<bool>({ true }));

   // A late reply for a cancelled request isn't delivered a second time.
   queueReply("previous-agent-written");
   runIdlePass();
   EXPECT_EQ(*written, std::vector<bool>({ true }));
}

// Records what would be written to the agent's stdin.
class RecordingAgentOperations : public core::system::ProcessOperations
{
public:
   core::Error writeToStdin(const std::string& input, bool) override
   {
      writes.push_back(input);
      return core::Success();
   }
   core::Error ptySetSize(int, int) override { return core::Success(); }
   core::Error ptyInterrupt() override { return core::Success(); }
   core::Error terminate() override { return core::Success(); }
   PidType getPid() override { return 0; }

   std::vector<std::string> writes;
};

TEST(SessionAssistant, NewAgentIsNotSentPreviousAgentsQueue)
{
   // The previous agent answered 'initialize' but exited before its
   // 'initialized' was written; the new agent must not receive it.
   queueAgentNotificationForTesting("initialized");
   resetForNewAgentForTesting();

   RecordingAgentOperations operations;
   writeQueuedAgentRequestsForTesting(operations);
   EXPECT_TRUE(operations.writes.empty());
}

TEST(SessionAssistant, InitializeOutlastsRequestTimeout)
{
   auto results = addRequest("initialize", "initialize", true);

   serviceEventsFor(seconds(30));
   runIdlePass();
   EXPECT_TRUE(results->empty());

   serviceEventsFor(seconds(31));
   runIdlePass();
   EXPECT_EQ(*results, std::vector<bool>({ true }));
}

TEST(SessionAssistant, HeldRequestOutlastsRequestTimeout)
{
   // A request held unwritten while the agent initializes is timed as
   // generously as 'initialize' itself.
   auto results = addRequest("held", kCompletion, false);

   serviceEventsFor(seconds(30));
   runIdlePass();
   EXPECT_TRUE(results->empty());

   serviceEventsFor(seconds(31));
   runIdlePass();
   EXPECT_EQ(*results, std::vector<bool>({ true }));
}

} // end namespace tests
} // end namespace session
} // end namespace rstudio
