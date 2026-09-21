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

#include <cstdlib>
#include <thread>

#include <sys/wait.h>
#include <unistd.h>

namespace rstudio {
namespace session {
namespace tests {

namespace {

const int kRequestedStatus = 42;
const int kTeardownRanStatus = 99;
const unsigned int kChildTimeoutSeconds = 30;

} // anonymous namespace

// exit() runs atexit handlers and static destructors on the calling thread,
// underneath a main thread that is still running. A background caller must
// therefore leave without any exit-time teardown.
//
// Only this direction is checked. The main-thread path joins worker threads
// that do not exist in a forked child, so it cannot be driven from here.
TEST(SessionMainTest, ExitEarlyFromBackgroundThreadSkipsTeardown)
{
   pid_t child = ::fork();
   ASSERT_NE(child, -1);
   if (child == 0)
   {
      // Reached only if exit-time teardown runs; it replaces the status so
      // that the parent can tell exit() from _Exit().
      std::atexit([]() { ::_exit(kTeardownRanStatus); });

      // don't hang the suite if the exit never happens
      ::alarm(kChildTimeoutSeconds);

      // the forking thread is the child's main thread; keep it alive, as the
      // session's main thread would be, while a background thread exits
      std::thread exiter([]() { exitEarly(kRequestedStatus); });
      for (;;)
         ::pause();
   }

   int status = 0;
   ASSERT_EQ(::waitpid(child, &status, 0), child);
   ASSERT_TRUE(WIFEXITED(status));
   EXPECT_EQ(WEXITSTATUS(status), kRequestedStatus);
}

} // namespace tests
} // namespace session
} // namespace rstudio

#endif // _WIN32
