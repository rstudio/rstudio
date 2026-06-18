/*
 * ThreadTests.cpp
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

#include <atomic>
#include <chrono>
#include <thread>

#include <boost/thread.hpp>

#include <core/Thread.hpp>

namespace rstudio {
namespace core {
namespace thread {

TEST(ThreadTests, JoinOrAbandonNonJoinableIsNoOp)
{
   // A default-constructed thread was never started, so there is nothing to
   // join: the contract is a no-op that reports success.
   boost::thread thread;
   EXPECT_FALSE(thread.joinable());
   EXPECT_TRUE(joinOrAbandonThread(thread, "never-started", false));
}

TEST(ThreadTests, JoinOrAbandonJoinsAFinishingThread)
{
   // A thread that returns promptly should be joined within the timeout, leave
   // the handle non-joinable, and report success.
   boost::thread thread([] {});
   EXPECT_TRUE(joinOrAbandonThread(
      thread, "quick", false, boost::posix_time::seconds(5)));
   EXPECT_FALSE(thread.joinable());
}

TEST(ThreadTests, JoinOrAbandonDetachesOnTimeout)
{
   // A worker that ignores interruption (no interruption points) and runs past
   // the timeout must be detached: the call returns false and the handle is left
   // non-joinable so ~boost::thread does not terminate.
   std::atomic<bool> release(false);
   boost::thread thread([&release] {
      while (!release.load())
         boost::this_thread::yield();
   });

   EXPECT_FALSE(joinOrAbandonThread(
      thread, "stuck", false, boost::posix_time::milliseconds(100)));
   EXPECT_FALSE(thread.joinable());

   // let the abandoned worker exit before the test (and the atomic) goes away
   release.store(true);
}

TEST(ThreadTests, JoinOrAbandonInterruptsAndJoins)
{
   // A worker that polls an interruption point should be woken by interrupt=true
   // and then join well within the timeout, reporting success.
   boost::thread thread([] {
      try
      {
         while (true)
         {
            boost::this_thread::interruption_point();
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
         }
      }
      catch (const boost::thread_interrupted&)
      {
      }
   });

   EXPECT_TRUE(joinOrAbandonThread(
      thread, "interruptible", true, boost::posix_time::seconds(5)));
   EXPECT_FALSE(thread.joinable());
}

} // end namespace thread
} // end namespace core
} // end namespace rstudio
