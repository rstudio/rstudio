/*
 * PosixChildProcessTracker.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/system/PosixChildProcessTracker.hpp>

#include <sys/wait.h>

#include <boost/format.hpp>
#include <boost/bind.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

// wraper for waitPid which tries again for EINTR
int waitPid(PidType pid, int* pStatus)
{
   for (;;)
   {
      int result = ::waitpid(pid, pStatus, WNOHANG);
      if (result == -1 && errno == EINTR)
         continue;
      return result;
   }
}

} // anonymous namespace


void ChildProcessTracker::addProcess(PidType pid, ExitHandler exitHandler)
{
   LOCK_MUTEX(mutex_)
   {
      processes_.insert(std::make_pair(pid, exitHandler));
   }
   END_LOCK_MUTEX
}

void ChildProcessTracker::notifySIGCHILD()
{
   // We make a copy of hte active pids so that we can do the reaping
   // outside of the pidsMutex_. This is an extra conservative precaution
   // in case there is ever an issue with waitpid blocking.
   std::map<PidType,ExitHandler> processes = activeProcesses();

   // attempt to reap each process
   std::for_each(processes.begin(),
                 processes.end(),
                 boost::bind(&ChildProcessTracker::attemptToReapProcess,
                             this, _1));
}

void ChildProcessTracker::attemptToReapProcess(
                              const std::pair<PidType,ExitHandler>& process)
{
   // non-blocking wait for the child
   int pid = process.first;
   int status;
   int result = waitPid(pid, &status);

   // reaped the child
   if (result == pid)
   {
      // confirm this was a real exit
      bool exited = false;
      if (WIFEXITED(status))
      {
         exited = true;
         status = WEXITSTATUS(status);
      }
      else if (WIFSIGNALED(status))
      {
         exited = true;
      }

      // if it was a real exit (as opposed to a SIGSTOP or SIGCONT)
      // then remove the pid from our table and fire the event
      if (exited)
      {
         // all done with this pid
         removeProcess(pid);

         // call exit handler if we have one
         ExitHandler exitHandler = process.second;
         if (exitHandler)
            exitHandler(pid, status);
      }
      else
      {
         boost::format fmt("Received SIGCHLD when child did not "
                           "actually exit (pid=%1%, status=%2%");
         LOG_WARNING_MESSAGE(boost::str(fmt % pid % status));
      }
   }
   // error occured
   else if (result == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("pid", pid);
      LOG_ERROR(error);
   }
}

void ChildProcessTracker::removeProcess(PidType pid)
{
   LOCK_MUTEX(mutex_)
   {
      processes_.erase(pid);
   }
   END_LOCK_MUTEX
}

std::map<PidType,ChildProcessTracker::ExitHandler>
                            ChildProcessTracker::activeProcesses()
{
   LOCK_MUTEX(mutex_)
   {
      return processes_;
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return std::map<PidType,ExitHandler>();
}

} // namespace system
} // namespace core
} // namespace rstudio

