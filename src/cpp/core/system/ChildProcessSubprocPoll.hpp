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
//   typedef bool (*hasSubprocCheck)(PidType);

   ChildProcessSubprocPoll(PidType pid, boost::function<bool (PidType pid)> subProcCheck);

   virtual ~ChildProcessSubprocPoll() {}

   // returns true if subprocess polling was done
   bool poll(bool hadOutput);

   void stop();

   bool hasSubprocess() const;
   bool hasRecentOutput() const;

private:
   PidType pid_;
   boost::posix_time::ptime checkSubProcAfter_;
   boost::posix_time::ptime resetRecentOutputAfter_;
   bool hasSubprocess_;
   bool hasRecentOutput_;
   bool stopped_;
   boost::function<bool (PidType pid)> subProcCheck_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_CHILD_PROCESS_SUBPROC_POLL_HPP
