/*
 * ChildProcessSubprocPoll.hpp
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

#ifndef CORE_SYSTEM_CHILD_PROCESS_SUBPROC_POLL_HPP
#define CORE_SYSTEM_CHILD_PROCESS_SUBPROC_POLL_HPP

#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/function.hpp>

#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace system {

// Maintains and updates two polling-based flags for a process.
//
// hasSubprocess(): does the process have any child processes? This can be
// fairly expensive to compute so we don't do it more often than specified by
// "checkSubProcDelay". Can be turned off completely by passing a
// NULL "subProcCheck" function.
//
// hasRecentOutput(): did the process recently report receiving output? Recent
// is controlled by "resetRecentDelay", i.e. if we've seen output within the
// last "resetRecentDelay" millisedconds.
//
// Tracking recent-output state serves two purposes:
//
// (1) the rsession checks this to see if there has been
// recent activity in order to keep the rsession alive (the websocket channel
// for terminals doesn't automatically do this like the RPC channel does)
//
// (2) to throttle back the subprocess checks if there hasn't been any recent
// output, the notion being that when the final subprocess stops we expect
// to see output in the form of the prompt.
//
class ChildProcessSubprocPoll : boost::noncopyable
{
public:
   ChildProcessSubprocPoll(
         PidType pid,
         boost::posix_time::milliseconds resetRecentDelay,
         boost::posix_time::milliseconds checkSubprocDelay,
         boost::function<bool (PidType pid)> subProcCheck);

   virtual ~ChildProcessSubprocPoll() {}

   // returns true if subprocess polling was done
   bool poll(bool hadOutput);

   void stop();

   bool hasSubprocess() const;
   bool hasRecentOutput() const;

  boost::posix_time::milliseconds getResetRecentDelay() const;

   boost::posix_time::milliseconds getCheckSubprocDelay() const;

private:
   // process whose subprocesses we are tracking
   PidType pid_;

   // when to next perform the checks
   boost::posix_time::ptime checkSubProcAfter_;
   boost::posix_time::ptime resetRecentOutputAfter_;

   // results of most recent checks
   bool hasSubprocess_;
   bool hasRecentOutput_;

   // misc. state
   bool didThrottleSubprocCheck_;
   bool stopped_;

   // How long does memory of "recent output" last?
   boost::posix_time::milliseconds resetRecentDelay_;

   // what is the minimum length of time before we check for subprocesses?
   boost::posix_time::milliseconds checkSubprocDelay_;

   // function used to check for subprocesses; if NULL then subprocess
   // checking will not be done
   boost::function<bool (PidType pid)> subProcCheck_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_CHILD_PROCESS_SUBPROC_POLL_HPP
