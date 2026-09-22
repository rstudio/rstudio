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
#include <cstdio>
#include <cstdlib>
#include <string>
#include <thread>

#include <sys/wait.h>
#include <unistd.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileLock.hpp>
#include <core/FileSerializer.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace tests {

namespace {

const int kRequestedStatus = 42;
const int kTeardownRanStatus = 99;
const int kChildFailedStatus = 98;
const unsigned int kChildTimeoutSeconds = 30;
const char* const kBufferedOutput = "buffered output\n";

// Runs in the forked child, and only returns on failure.
void exitEarlyFromThreadInChild(const FilePath& root)
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

   // left in the stream's buffer, as R leaves writes to a file() connection
   std::string outputPath = root.completePath("output").getAbsolutePath();
   FILE* pOutput = std::fopen(outputPath.c_str(), "w");
   if (pOutput == nullptr || std::fputs(kBufferedOutput, pOutput) == EOF)
      return;

   // never returns, since exitEarly() ends the process
   std::thread([]()
   {
      exitEarly(kRequestedStatus);
   }).join();
}

} // anonymous namespace

// exit() runs atexit handlers and static destructors on the calling thread,
// underneath a main thread that is still running. A background caller of
// exitEarly() must therefore leave without any exit-time teardown, but still
// release its link-based locks and flush stdio, as exit() would have.
TEST(SessionMainTest, ExitEarlyFromBackgroundThreadSkipsTeardown)
{
   FileLock::initialize();

   FilePath root;
   ASSERT_FALSE(FilePath::tempFilePath(root));
   ASSERT_FALSE(root.ensureDirectory());
   FilePath lockFilePath = root.completePath("lock");

   // load-balanced, so that the parent cannot treat the lock as released
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
         exitEarlyFromThreadInChild(root);
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

   std::string output;
   Error outputError = readStringFromFile(root.completePath("output"), &output);

   FileLock::setLoadBalancedForTesting(wasLoadBalanced);
   EXPECT_FALSE(root.removeIfExists());

   ASSERT_NE(child, -1);
   ASSERT_EQ(waited, child);
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(WEXITSTATUS(status), kRequestedStatus);
   EXPECT_FALSE(locked);
   EXPECT_FALSE(outputError);
   EXPECT_EQ(output, kBufferedOutput);
}

} // namespace tests
} // namespace session
} // namespace rstudio

#endif // _WIN32
