/*
 * ChildProcessSubprocPoll.cpp
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

#include "ChildProcessSubprocPoll.hpp"

namespace rstudio {
namespace core {
namespace system {

namespace {

boost::posix_time::ptime now()
{
   return boost::posix_time::microsec_clock::universal_time();
}

const int kThrottleSubProcFactor = 4;

} // anonymous namespace

ChildProcessSubprocPoll::ChildProcessSubprocPoll(
      PidType pid,
      boost::posix_time::milliseconds resetRecentDelay,
      boost::posix_time::milliseconds checkSubprocDelay,
      boost::posix_time::milliseconds checkCwdDelay,
      boost::function<std::vector<SubprocInfo> (PidType pid)> subProcCheck,
      const std::vector<std::string>& subProcWhitelist,
      boost::function<core::FilePath (PidType pid)> cwdCheck)
   :
     pid_(pid),
     checkSubProcAfter_(boost::posix_time::not_a_date_time),
     resetRecentOutputAfter_(boost::posix_time::not_a_date_time),
     checkCwdAfter_(boost::posix_time::not_a_date_time),
     hasSubprocess_(true),
     hasWhitelistSubprocess_(false),
     hasRecentOutput_(true),
     didThrottleSubprocCheck_(false),
     stopped_(false),
     resetRecentDelay_(resetRecentDelay),
     checkSubprocDelay_(checkSubprocDelay),
     checkCwdDelay_(checkCwdDelay),
     subProcCheck_(subProcCheck),
     subProcWhitelist_(subProcWhitelist),
     cwdCheck_(cwdCheck)
{
}

bool ChildProcessSubprocPoll::poll(bool hadOutput)
{
   if (stopped_)
      return false;

   boost::posix_time::ptime currentTime = now();

   // Update state of "hasRecentOutput". We remember that we saw output for
   // up to "resetRecentDelay_" milliseconds.
   if (hadOutput)
   {
      hasRecentOutput_ = true;
      resetRecentOutputAfter_ = boost::posix_time::not_a_date_time;
   }

   if (resetRecentOutputAfter_.is_not_a_date_time())
   {
      resetRecentOutputAfter_ = currentTime + resetRecentDelay_;
   }
   else if (currentTime > resetRecentOutputAfter_)
   {
      hasRecentOutput_ = false;
      resetRecentOutputAfter_ = currentTime + resetRecentDelay_;
   }

   bool didCheckSubProc = pollSubproc(currentTime);
   bool didCheckCwd = pollCwd(currentTime);
   return didCheckSubProc || didCheckCwd;
}

bool ChildProcessSubprocPoll::pollSubproc(boost::posix_time::ptime currentTime)
{
   if (!subProcCheck_)
      return false;

   // Update state of "hasSubprocesses". We do this no more often than every
   // "checkSubprocDelay_" milliseconds, and less often if we haven't seen any
   // recent output. The latter is to reduce load when nothing is happening,
   // under the assumption that if all child processes are terminated, we
   // will always see output in the form of the command prompt.
   if (!hasRecentOutput() && !didThrottleSubprocCheck_)
   {
      checkSubProcAfter_ = currentTime + checkSubprocDelay_ * kThrottleSubProcFactor;
      didThrottleSubprocCheck_ = true;
      return false;
   }

   if (checkSubProcAfter_.is_not_a_date_time())
   {
      checkSubProcAfter_ = currentTime + checkSubprocDelay_;
      return false;
   }

   if (currentTime <= checkSubProcAfter_)
      return false;

   // Enough time has passed, update whether "pid" has subprocesses
   // and restart the timer.
   hasSubprocess_ = false;
   hasWhitelistSubprocess_ = false;
   std::vector<SubprocInfo> children = subProcCheck_(pid_);
   for (const SubprocInfo& proc : children)
   {
      bool isWhitelistItem = false;
      for (const auto& whitelistItem : subProcWhitelist_)
      {
         if (proc.exe == whitelistItem)
         {
            isWhitelistItem = true;
            hasWhitelistSubprocess_ = true;
            break;
         }
      }
      if (!isWhitelistItem)
      {
         hasSubprocess_ = true;
      }
   }

   checkSubProcAfter_ = currentTime + checkSubprocDelay_;
   didThrottleSubprocCheck_ = false;
   return true;
}

bool ChildProcessSubprocPoll::pollCwd(boost::posix_time::ptime currentTime)
{
   if (!cwdCheck_)
      return false;

   // Update state of "getCwd". We do this no more often than every
   // "checkCwdDelay_" milliseconds.
   if (checkCwdAfter_.is_not_a_date_time())
   {
      checkCwdAfter_ = currentTime + checkCwdDelay_;
      return false;
   }

   if (currentTime <= checkCwdAfter_)
      return false;

   // Enough time has passed, query current working directory and restart timer
   cwd_ = cwdCheck_(pid_);
   checkCwdAfter_ = currentTime + checkCwdDelay_;
   return true;
}

void ChildProcessSubprocPoll::stop()
{
   stopped_ = true;
}

bool ChildProcessSubprocPoll::hasNonWhitelistSubprocess() const
{
   return hasSubprocess_;
}

bool ChildProcessSubprocPoll::hasWhitelistSubprocess() const
{
   return hasWhitelistSubprocess_;
}

bool ChildProcessSubprocPoll::hasRecentOutput() const
{
   return hasRecentOutput_;
}

core::FilePath ChildProcessSubprocPoll::getCwd() const
{
   return cwd_;
}

} // namespace system
} // namespace core
} // namespace rstudio
