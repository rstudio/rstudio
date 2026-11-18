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

   EXPECT_FALSE(deadline.check(kQueued));
   EXPECT_FALSE(deadline.check(kQueued + milliseconds(1999)));
   EXPECT_FALSE(deadline.passed());
}

TEST(ChatExecutionDeadline, PassesOnceAtTheTimeout)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);

   EXPECT_TRUE(deadline.check(kQueued + milliseconds(2000)));
   EXPECT_TRUE(deadline.passed());

   // Later checks don't ask for a second interrupt
   EXPECT_FALSE(deadline.check(kQueued + milliseconds(5000)));
   EXPECT_TRUE(deadline.passed());
}

TEST(ChatExecutionDeadline, PassesOnALateFirstCheck)
{
   // Background processing polls R about every 50 ms, so the first check
   // usually comes after the deadline rather than exactly at it
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);

   EXPECT_FALSE(deadline.check(kQueued + milliseconds(1990)));
   EXPECT_TRUE(deadline.check(kQueued + milliseconds(2049)));
   EXPECT_FALSE(deadline.check(kQueued + milliseconds(2099)));
   EXPECT_TRUE(deadline.passed());
}

TEST(ChatExecutionDeadline, CountsFromWhenTheRequestWasQueued)
{
   // A request that waited 1.5 s in the queue has 0.5 s left to run
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);

   Clock::time_point started = kQueued + milliseconds(1500);
   EXPECT_FALSE(deadline.check(started + milliseconds(499)));
   EXPECT_TRUE(deadline.check(started + milliseconds(500)));
}

TEST(ChatExecutionDeadline, NonPositiveTimeoutSetsNoLimit)
{
   for (int timeout : {0, -1})
   {
      ExecutionDeadline deadline;
      deadline.arm(kQueued, timeout);

      EXPECT_FALSE(deadline.check(kQueued + std::chrono::hours(24))) << timeout;
      EXPECT_FALSE(deadline.passed()) << timeout;
   }
}

TEST(ChatExecutionDeadline, DisarmStopsEnforcing)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);

   EXPECT_FALSE(deadline.disarm());
   EXPECT_FALSE(deadline.check(kQueued + milliseconds(5000)));
}

TEST(ChatExecutionDeadline, DisarmReportsAPass)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   ASSERT_TRUE(deadline.check(kQueued + milliseconds(2000)));

   EXPECT_TRUE(deadline.disarm());
   EXPECT_TRUE(deadline.passed());
}

TEST(ChatExecutionDeadline, ArmForgetsAnEarlierPass)
{
   ExecutionDeadline deadline;
   deadline.arm(kQueued, 2000);
   ASSERT_TRUE(deadline.check(kQueued + milliseconds(2000)));
   deadline.disarm();

   Clock::time_point next = kQueued + milliseconds(10000);
   deadline.arm(next, 2000);
   EXPECT_FALSE(deadline.passed());
   EXPECT_FALSE(deadline.check(next + milliseconds(1000)));
   EXPECT_TRUE(deadline.check(next + milliseconds(2000)));
}

TEST(ChatExecutionDeadline, ExpiredInQueueOnceTheTimeoutIsUsedUp)
{
   EXPECT_FALSE(expiredInQueue(kQueued, 1000, kQueued + milliseconds(999)));
   EXPECT_TRUE(expiredInQueue(kQueued, 1000, kQueued + milliseconds(1000)));

   // A request dequeued long after its deadline
   EXPECT_TRUE(expiredInQueue(kQueued, 1000, kQueued + milliseconds(3200)));
}

TEST(ChatExecutionDeadline, NeverExpiredInQueueWithoutATimeout)
{
   EXPECT_FALSE(expiredInQueue(kQueued, 0, kQueued + std::chrono::hours(24)));
   EXPECT_FALSE(expiredInQueue(kQueued, -1, kQueued + std::chrono::hours(24)));
}
