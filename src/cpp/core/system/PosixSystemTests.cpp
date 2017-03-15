/*
 * PosixSystemTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef _WIN32

#include <core/system/System.hpp>
#include <signal.h>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

context("PosixSystemTests")
{

   test_that("No subprocess detected correctly")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         execlp("sleep", "sleep", "5", NULL);
         expect_true(false); // shouldn't get here!
      }
      else
      {
         // process we started doesn't have a subprocess
         expect_false(hasSubprocesses(pid));

         kill(pid, SIGKILL);
      }
   }

   test_that("Subprocess detected correctly")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         execlp("sleep", "sleep", "2", NULL);
         expect_true(false); // shouldn't get here!
      }
      else
      {
         // we now have a subprocess
         expect_true(hasSubprocesses(getpid()));

         kill(pid, SIGKILL);
      }
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // !_WIN32
