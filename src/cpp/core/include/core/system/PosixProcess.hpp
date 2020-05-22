/*
 * PosixProcess.hpp
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

#ifndef CORE_SYSTEM_POSIX_PROCESS_HPP
#define CORE_SYSTEM_POSIX_PROCESS_HPP

#include <boost/asio/io_service.hpp>

#include <core/system/Process.hpp>
#include <core/system/PosixChildProcess.hpp>

namespace rstudio {
namespace core {
namespace system {

// AsioProcessSupervisor
// Supervisor which allows asynchronous reading/writing of process streams
// intended to be more efficient by not having to poll
// utilizes AsioAsyncChildProcess instead of regular AsyncChildProcess
class AsioProcessSupervisor : boost::noncopyable
{
public:
   AsioProcessSupervisor(boost::asio::io_service& ioService);
   virtual ~AsioProcessSupervisor();

   core::Error runProgram(const std::string& executable,
                          const std::vector<std::string>& args,
                          const ProcessOptions& options,
                          const ProcessCallbacks& callbacks,
                          boost::shared_ptr<AsioAsyncChildProcess>* pOutChild = nullptr);

   core::Error runCommand(const std::string& command,
                          const ProcessOptions& options,
                          const ProcessCallbacks& callbacks,
                          boost::shared_ptr<AsioAsyncChildProcess>* pOutChild = nullptr);

   // Check whether any children are currently running
   bool hasRunningChildren();

   // Terminate all running children
   void terminateAll(bool killChildProcs);

   // Wait for all children to exit. Returns false if the operation timed out
   bool wait(const boost::posix_time::time_duration& maxWait =
                boost::posix_time::time_duration(boost::posix_time::not_a_date_time));

private:
   struct Impl;
   friend struct Impl;
   boost::shared_ptr<Impl> pImpl_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_POSIX_PROCESS_HPP
