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

#include <core/system/PosixSystem.hpp>
#include <signal.h>
#include <sys/wait.h>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

context("PosixSystemTests")
{

   test_that("No subprocess detected correctly with generic method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         expect_false(hasSubprocesses(pid));

         expect_true(::kill(pid, SIGKILL) == 0);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Subprocess detected correctly with generic method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         expect_true(hasSubprocesses(getpid()));

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("No subprocess detected correctly with pgrep method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         expect_false(hasSubprocessesViaPgrep(pid));

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Subprocess detected correctly with pgrep method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         expect_true(hasSubprocessesViaPgrep(getpid()));

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Empty subprocess list returned correctly with pgrep method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesViaPgrep(pid);
         expect_true(children.empty());

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Subprocess name detected correctly with pgrep method")
   {
      std::string exe = "sleep";

      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         execlp(exe.c_str(), exe.c_str(), "100", NULL);
         expect_true(false); // shouldn't get here!
      }
      else
      {
         // we now have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesViaPgrep(getpid());
         expect_true(children.size() == 1);
         if (children.size() == 1)
            expect_true(children[0].exe.compare(exe) == 0);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

#ifdef __APPLE__ // Mac-specific subprocess detection

   test_that("No subprocess detected correctly with Mac method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         expect_false(hasSubprocessesMac(pid));

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Subprocess detected correctly with Mac method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         expect_true(hasSubprocessesMac(getpid()));

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Subprocess list correctly empty with Mac method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesMac(pid);
         expect_true(children.empty());

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Subprocess count and pid detected correctly with Mac method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         std::vector<SubprocInfo> children = getSubprocessesMac(getpid());
         expect_true(children.size() == 1);
         expect_true(children.at(0).pid == pid);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Subprocess name detected correctly with Mac method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);
      std::string exe = "sleep";

      if (pid == 0)
      {
         execlp(exe.c_str(), exe.c_str(), "100", NULL);
         expect_true(false); // shouldn't get here!
      }
      else
      {
         // we now have a subprocess, need a slight pause to allow system tables to
         // catch up
         ::sleep(1);
         std::vector<SubprocInfo> children = getSubprocessesMac(getpid());
         expect_true(children.size() == 1);
         if (children.size() == 1)
            expect_true(children[0].exe.compare(exe) == 0);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }



#endif // __APPLE__

#ifdef HAVE_PROCSELF
   test_that("No subprocess detected correctly with procfs method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         expect_false(hasSubprocessesViaProcFs(pid));

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Subprocess detected correctly with procfs method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         expect_true(hasSubprocessesViaProcFs(getpid()));

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }
#endif // HAVE_PROCSELF

   test_that("Empty list of subprocesses returned correctly with generic method")
   {
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // process we started doesn't have a subprocess
         std::vector<SubprocInfo> children = getSubprocesses(pid);
         expect_true(children.empty());

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Current working directory determined correctly with generic method")
   {
      FilePath emptyPath;
      FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         FilePath cwd = currentWorkingDir(pid);
         expect_false(cwd.empty());
         expect_true(cwd.exists());
         expect_true(startingDir == cwd);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

   test_that("Current working directory determined correctly with lsof method")
   {
      FilePath emptyPath;
      FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         FilePath cwd = currentWorkingDirViaLsof(pid);
         expect_false(cwd.empty());
         expect_true(cwd.exists());
         expect_true(startingDir == cwd);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }

#ifdef HAVE_PROCSELF
   test_that("Current working directory determined correctly with procfs method")
   {
      FilePath emptyPath;
      FilePath startingDir = FilePath::safeCurrentPath(emptyPath);
      pid_t pid = fork();
      expect_false(pid == -1);

      if (pid == 0)
      {
         ::sleep(1);
         _exit(0);
      }
      else
      {
         // we now have a subprocess
         FilePath cwd = currentWorkingDirViaProcFs(pid);
         expect_false(cwd.empty());
         expect_true(cwd.exists());
         expect_true(startingDir == cwd);

         ::kill(pid, SIGKILL);
         ::waitpid(pid, NULL, 0);
      }
   }
#endif // HAVE_PROCSELF
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // !_WIN32
