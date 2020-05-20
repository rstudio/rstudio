/*
 * PosixChildProcessTracker.hpp
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

#ifndef CORE_SYSTEM_CHILD_PROCESS_TRACKER_HPP
#define CORE_SYSTEM_CHILD_PROCESS_TRACKER_HPP

#include <map>

#include <boost/noncopyable.hpp>
#include <boost/function.hpp>

#include <core/Thread.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace system {

class ChildProcessTracker : boost::noncopyable
{
public:

  typedef boost::function<void(PidType,int)> ExitHandler;

  void addProcess(PidType pid, ExitHandler exitHandler = ExitHandler());

  void notifySIGCHILD();

private:
  void attemptToReapProcess(const std::pair<PidType,ExitHandler>& process);
  void removeProcess(PidType pid);
  std::map<PidType,ExitHandler> activeProcesses();

private:
   boost::mutex mutex_;
   std::map<PidType,ExitHandler> processes_;
};


} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_CHILD_PROCESS_TRACKER_HPP

