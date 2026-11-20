/*
 * ChatExecutionDeadlineTests.cpp
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

#include "ChatExecutionDeadline.hpp"

#include <climits>
#include <cmath>

#include <gtest/gtest.h>

using namespace rstudio::session::modules::chat::execution;

namespace {

using std::chrono::milliseconds;

// A fixed origin, so the tests never depend on how long they take to run
const Clock::time_point kQueued = Clock::time_point() + std::chrono::hours(1);

} // anonymous namespace

TEST(ChatExecutionDeadline, DoesNotPassBeforeTheTimeout)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);

   EXPECT_FALSE(deadline.takeInterrupt(kQueued));
   EXPECT_FALSE(deadline.expired(kQueued + milliseconds(1999)));
   EXPECT_FALSE(deadline.takeInterrupt(kQueued + milliseconds(1999)));
   EXPECT_FALSE(deadline.passed());
   EXPECT_FALSE(deadline.interrupted());
}

TEST(ChatExecutionDeadline, InterruptsAtTheTimeout)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);

   EXPECT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(2000)));
   EXPECT_TRUE(deadline.passed());
   EXPECT_TRUE(deadline.interrupted());
}

TEST(ChatExecutionDeadline, InterruptsAgainAtLaterChecks)
{
   // The agent's code can catch an interrupt (e.g. tryCatch(interrupt = ))
   // and carry on, so each check past the deadline asks for another
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);

   EXPECT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(2000)));
   EXPECT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(2050)));
   EXPECT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(5000)));
   EXPECT_TRUE(deadline.passed());
   EXPECT_TRUE(deadline.interrupted());
}

TEST(ChatExecutionDeadline, InterruptsOnALateFirstCheck)
{
   // Background processing polls R about every 50 ms, so the first check
   // usually comes after the deadline rather than exactly at it
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);

   EXPECT_FALSE(deadline.takeInterrupt(kQueued + milliseconds(1990)));
   EXPECT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(2049)));
   EXPECT_TRUE(deadline.passed());
}

TEST(ChatExecutionDeadline, DisarmStopsFurtherInterrupts)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);
   ASSERT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(2000)));

   deadline.disarm();
   EXPECT_FALSE(deadline.takeInterrupt(kQueued + milliseconds(2050)));
}

TEST(ChatExecutionDeadline, DoesNotInterruptOutsideTheAgentsCode)
{
   // After the agent's code returns, RStudio's own R calls run (the warning
   // formatter, the expression boundary hook); an interrupt there would cut
   // them short
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);

   EXPECT_FALSE(deadline.takeInterrupt(kQueued + milliseconds(2000)));

   deadline.setAgentCodeRunning(true);
   EXPECT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(2050)));

   deadline.setAgentCodeRunning(false);
   EXPECT_FALSE(deadline.takeInterrupt(kQueued + milliseconds(2100)));
   EXPECT_TRUE(deadline.passed());
}

TEST(ChatExecutionDeadline, ArmStartsOutsideTheAgentsCode)
{
   // A request that starts inside another one's readline() arms the deadline
   // while the other one's code is running
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);

   deadline.arm(kQueued, 2000);
   EXPECT_FALSE(deadline.takeInterrupt(kQueued + milliseconds(2000)));
}

TEST(ChatExecutionDeadline, ExpiredRecordsAPassWithoutAnInterrupt)
{
   // Checking between expressions finds the pass without interrupting R
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);

   EXPECT_TRUE(deadline.expired(kQueued + milliseconds(2010)));
   EXPECT_TRUE(deadline.passed());
   EXPECT_FALSE(deadline.interrupted());
}

TEST(ChatExecutionDeadline, CountsFromWhenTheRequestWasQueued)
{
   // A request that waited 1.5 s in the queue has 0.5 s left to run
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);

   Clock::time_point started = kQueued + milliseconds(1500);
   EXPECT_FALSE(deadline.takeInterrupt(started + milliseconds(499)));
   EXPECT_TRUE(deadline.takeInterrupt(started + milliseconds(500)));
}

TEST(ChatExecutionDeadline, NonPositiveTimeoutSetsNoLimit)
{
   for (int timeout : {0, -1})
   {
      ExecutionDeadline deadline;
      deadline.arm(kQueued, timeout);
      deadline.setAgentCodeRunning(true);

      EXPECT_FALSE(deadline.expired(kQueued + std::chrono::hours(24))) << timeout;
      EXPECT_FALSE(deadline.takeInterrupt(kQueued + std::chrono::hours(24))) << timeout;
      EXPECT_FALSE(deadline.passed()) << timeout;
   }
}

TEST(ChatExecutionDeadline, ArmWithoutALimitReplacesAnActiveDeadline)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);
   deadline.arm(kQueued, 0);
   deadline.setAgentCodeRunning(true);

   EXPECT_FALSE(deadline.expired(kQueued + milliseconds(5000)));
   EXPECT_FALSE(deadline.takeInterrupt(kQueued + milliseconds(5000)));
}

TEST(ChatExecutionDeadline, DisarmStopsEnforcing)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);

   EXPECT_FALSE(deadline.disarm());
   EXPECT_FALSE(deadline.expired(kQueued + milliseconds(5000)));
   EXPECT_FALSE(deadline.takeInterrupt(kQueued + milliseconds(5000)));
}

TEST(ChatExecutionDeadline, DisarmReportsAPass)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);
   ASSERT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(2000)));

   EXPECT_TRUE(deadline.disarm());
   EXPECT_TRUE(deadline.passed());
   EXPECT_TRUE(deadline.interrupted());
}

TEST(ChatExecutionDeadline, ArmForgetsAnEarlierPass)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   deadline.setAgentCodeRunning(true);
   ASSERT_TRUE(deadline.takeInterrupt(kQueued + milliseconds(2000)));
   deadline.disarm();

   Clock::time_point next = kQueued + milliseconds(10000);
   deadline.arm(next, 2000);
   deadline.setAgentCodeRunning(true);
   EXPECT_FALSE(deadline.passed());
   EXPECT_FALSE(deadline.interrupted());
   EXPECT_FALSE(deadline.takeInterrupt(next + milliseconds(1000)));
   EXPECT_TRUE(deadline.takeInterrupt(next + milliseconds(2000)));
}

TEST(ChatExecutionDeadline, ExpiredInQueueOnceTheTimeoutIsUsedUp)
{
   EXPECT_FALSE(expiredInQueue(kQueued, 1000, kQueued + milliseconds(999)));
   EXPECT_TRUE(expiredInQueue(kQueued, 1000, kQueued + milliseconds(1000)));

   // A request dequeued long after its deadline
   EXPECT_TRUE(expiredInQueue(kQueued, 1000, kQueued + milliseconds(3200)));
}

TEST(ChatExecutionDeadline, TimeoutFromOptionKeepsWholeMilliseconds)
{
   EXPECT_EQ(timeoutFromOption(2000), 2000);
   EXPECT_EQ(timeoutFromOption(1), 1);
}

TEST(ChatExecutionDeadline, TimeoutFromOptionRoundsAFractionUp)
{
   // Rounding down would turn 0.5 into "no limit"
   EXPECT_EQ(timeoutFromOption(1500.5), 1501);
   EXPECT_EQ(timeoutFromOption(0.5), 1);
}

TEST(ChatExecutionDeadline, TimeoutFromOptionClampsToIntRange)
{
   EXPECT_EQ(timeoutFromOption(2147483648.0), INT_MAX);
   EXPECT_EQ(timeoutFromOption(1e12), INT_MAX);
}

TEST(ChatExecutionDeadline, TimeoutFromOptionReadsNonPositiveAsNoLimit)
{
   EXPECT_EQ(timeoutFromOption(0), 0);
   EXPECT_EQ(timeoutFromOption(-5), 0);
   EXPECT_EQ(timeoutFromOption(std::nan("")), 0);
}

TEST(ChatExecutionDeadline, RequestTimeoutLimitsTheAgentsCode)
{
   EXPECT_EQ(requestTimeout(30000, true, false), 30000);
   EXPECT_EQ(requestTimeout(1500.5, true, false), 1501);
}

TEST(ChatExecutionDeadline, RequestTimeoutLeavesRunInConsoleUnlimited)
{
   // Every 11.x Assistant sends Run in Console with captureOutput: false and
   // timeout: 30000
   EXPECT_EQ(requestTimeout(30000, false, false), 0);
}

TEST(ChatExecutionDeadline, RequestTimeoutLeavesSilentRequestsUnlimited)
{
   // Every 11.x Assistant sends its interleaved setup and teardown silently,
   // with the agent code's timeout, and its best-effort teardown with 5000
   EXPECT_EQ(requestTimeout(30000, true, true), 0);
   EXPECT_EQ(requestTimeout(5000, true, true), 0);
}

TEST(ChatExecutionDeadline, NeverExpiredInQueueWithoutATimeout)
{
   EXPECT_FALSE(expiredInQueue(kQueued, 0, kQueued + std::chrono::hours(24)));
   EXPECT_FALSE(expiredInQueue(kQueued, -1, kQueued + std::chrono::hours(24)));
}

TEST(ChatBusyHandoff, ReleasesAfterTheLastRequestRuns)
{
   BusyHandoff handoff;
   handoff.started();

   EXPECT_TRUE(handoff.finished(true, true));
}

TEST(ChatBusyHandoff, HandsTheBusyStateToTheNextRequest)
{
   BusyHandoff handoff;
   handoff.started();
   EXPECT_FALSE(handoff.finished(true, false));

   handoff.started();
   EXPECT_TRUE(handoff.finished(true, true));
}

TEST(ChatBusyHandoff, ReleasesWhenTheLastRequestExpiresAfterAHandoff)
{
   // A ran with B queued behind it, and B expired before it could start
   BusyHandoff handoff;
   handoff.started();
   EXPECT_FALSE(handoff.finished(true, false));

   EXPECT_TRUE(handoff.finished(false, true));
}

TEST(ChatBusyHandoff, KeepsHoldingWhileExpiriesLeaveRequestsQueued)
{
   BusyHandoff handoff;
   handoff.started();
   EXPECT_FALSE(handoff.finished(true, false));

   EXPECT_FALSE(handoff.finished(false, false));
   EXPECT_TRUE(handoff.finished(false, true));
}

TEST(ChatBusyHandoff, KeepsTheBusyStateOfWorkItDidNotStart)
{
   // An expiry in idle work inside a user command waiting in file.choose()
   BusyHandoff handoff;

   EXPECT_FALSE(handoff.finished(false, true));
}

TEST(ChatBusyHandoff, KeepsTheBusyStateOfARunningRequest)
{
   // A ran with B queued and handed the busy state to B, which waits in
   // readline(); C expires in idle work inside that read, and B, not the
   // handoff, still holds the console
   BusyHandoff handoff;
   handoff.started();
   EXPECT_FALSE(handoff.finished(true, false));
   handoff.started();

   EXPECT_FALSE(handoff.finished(false, true));
}
