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

// Updates two polling-based flags for a process.
//
// hasSubprocess(): does the process have any child processes?
// hasRecentOutput(): did the process recently report receiving output
//
// Pass function to perform the actual subproc check to the constructor, or
// NULL to not do any subprocess checking.
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

   // How long does memory of "recent output" last? This state gets used to
   // keep the session alive when only communicating with terminal via
   // websockets and not RPC, and also to throttle-back subprocess checking
   // when there isn't any noticable activity taking place.
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
