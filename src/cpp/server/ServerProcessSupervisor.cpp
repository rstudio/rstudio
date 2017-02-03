/*
 * ServerProcessSupervisor.cpp
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

#include <server/ServerProcessSupervisor.hpp>

#include <core/Error.hpp>
#include <core/DateTime.hpp>
#include <core/Thread.hpp>
#include <core/PeriodicCommand.hpp>
#include <core/system/Process.hpp>

#include <server/ServerScheduler.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace server {
namespace process_supervisor {

namespace {

// mutex that protects access to the process supervisor's methods
boost::mutex s_mutex;

core::system::ProcessSupervisor& processSupervisor()
{
   static core::system::ProcessSupervisor instance;
   return instance;
}

bool pollProcessSupervisor()
{
   LOCK_MUTEX(s_mutex)
   {
      processSupervisor().poll();
   }
   END_LOCK_MUTEX

   return true;
}

} // anonymous namespace

Error runProgram(
  const std::string& executable,
  const std::vector<std::string>& args,
  const std::string& input,
  const core::system::ProcessOptions& options,
  const boost::function<void(const core::system::ProcessResult&)>& onCompleted)
{
   LOCK_MUTEX(s_mutex)
   {
      return processSupervisor().runProgram(executable,
                                            args,
                                            input,
                                            options,
                                            onCompleted);
   }
   END_LOCK_MUTEX

   // fulfill closure and keep compiler happy
   core::system::ProcessResult result;
   result.exitStatus = EXIT_FAILURE;
   result.stdErr = "Thread resource error occurred while running program " +
                   executable;
   onCompleted(result);
   return Success();
}

core::Error runProgram(
  const std::string& executable,
  const std::vector<std::string>& args,
  const core::system::ProcessOptions& options,
  const core::system::ProcessCallbacks& cb)
{
   LOCK_MUTEX(s_mutex)
   {
      return processSupervisor().runProgram(
            executable, args, options, cb);
   }
   END_LOCK_MUTEX
   return Success();
}


Error initialize()
{
   // periodically poll process supervisor
   scheduler::addCommand(
      boost::shared_ptr<ScheduledCommand>(new PeriodicCommand(
         boost::posix_time::milliseconds(500), pollProcessSupervisor, false))
   );

   return Success();
}

} // namespace process_supervisor
} // namespace server
} // namespace rstudio
