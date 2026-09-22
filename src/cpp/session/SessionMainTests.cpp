/*
 * SessionMainTests.cpp
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

#include <session/SessionMain.hpp>

#include <gtest/gtest.h>

#ifndef _WIN32

#include <cerrno>
#include <csignal>
#include <cstdlib>

#include <sys/wait.h>
#include <unistd.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileLock.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace tests {

namespace {

const int kRequestedStatus = 42;
const int kTeardownRanStatus = 99;
const int kChildFailedStatus = 98;
const unsigned int kChildTimeoutSeconds = 30;

// Runs in the forked child, and only returns on failure.
void exitFromBackgroundThreadInChild(const FilePath& root)
{
   // don't hang the suite if the exit never happens
   ::alarm(kChildTimeoutSeconds);

   // The child is a copy of a multithreaded process, so the logger's locks
   // may have been snapshotted held; route lock logging around them.
   FileLock::setLogFileForTesting(root.completePath("lock.log"));

   // Reached only if exit-time teardown runs; it replaces the status so
   // that the parent can tell exit() from _Exit().
   std::atexit([]() { ::_exit(kTeardownRanStatus); });

   // Taken here rather than before the fork, since a forked child starts
   // with an empty lock registry. A hard-linked lock keeps its public entry
   // when released, so the parent sees the release rather than a missing
   // file.
   FileLock::setUseSymlinksForTesting(false);
   LinkBasedFileLock lock;
   if (lock.acquire(root.completePath("lock")))
      return;

   detail::exitFromBackgroundThread(kRequestedStatus);
}

} // anonymous namespace

// exit() runs atexit handlers and static destructors on the calling thread,
// underneath a main thread that is still running. A background caller must
// therefore leave without any exit-time teardown, but still release the
// link-based locks of a load-balanced session, which cannot be recognized
// as stale by their owner's pid.
//
// The rest of exitEarly() joins worker threads that do not exist in a
// forked child, so only the background tail is driven from here.
TEST(SessionMainTest, ExitFromBackgroundThreadSkipsTeardown)
{
   FileLock::initialize();

   FilePath root;
   ASSERT_FALSE(FilePath::tempFilePath(root));
   ASSERT_FALSE(root.ensureDirectory());
   FilePath lockFilePath = root.completePath("lock");

   // load-balanced, so that neither side can treat the lock as released
   // merely because its owner is gone
   bool wasLoadBalanced = FileLock::isLoadBalanced();
   FileLock::setLoadBalancedForTesting(true);

   pid_t child = ::fork();
   if (child == 0)
   {
      // The child is a copy of a live session. It must never get back into
      // the rest of the suite, which ends in a real rCleanup() and exit().
      try
      {
         exitFromBackgroundThreadInChild(root);
      }
      catch (...)
      {
      }

      ::_exit(kChildFailedStatus);
   }

   int status = 0;
   pid_t waited = -1;
   if (child != -1)
   {
      do
      {
         waited = ::waitpid(child, &status, 0);
      } while (waited == -1 && errno == EINTR);

      if (waited != child)
      {
         ::kill(child, SIGKILL);
         ::waitpid(child, nullptr, 0);
      }
   }

   bool locked = LinkBasedFileLock().isLocked(lockFilePath);

   FileLock::setLoadBalancedForTesting(wasLoadBalanced);
   EXPECT_FALSE(root.removeIfExists());

   ASSERT_NE(child, -1);
   ASSERT_EQ(waited, child);
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(WEXITSTATUS(status), kRequestedStatus);
   EXPECT_FALSE(locked);
}

} // namespace tests
} // namespace session
} // namespace rstudio

#endif // _WIN32
