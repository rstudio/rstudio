/*
 * PosixChildProcess.hpp
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

#ifndef CORE_SYSTEM_POSIX_CHILD_PROCESS_HPP
#define CORE_SYSTEM_POSIX_CHILD_PROCESS_HPP

#include <boost/asio/io_service.hpp>

#include <core/system/ChildProcess.hpp>
#include <core/system/Process.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace system {

// interface for AsioAsyncChildProcess
// primarly used for testing
class IAsioAsyncChildProcess
{
public:
   // begins running the process
   virtual Error run(const core::system::ProcessCallbacks& callbacks) = 0;

   // asynchronously writes to the process's standard input stream
   virtual void asyncWriteToStdin(const std::string& input, bool eof = false) = 0;

   // returns whether or not the process has exited
   virtual bool exited() = 0;

   // terminates the process gracefully by sending SIGTERM
   virtual Error terminate() = 0;
};

// AsioAsyncChildProcess
// uses boost ASIO to read/write std streams
class AsioAsyncChildProcess : public IAsioAsyncChildProcess, public AsyncChildProcess
{
public:
   AsioAsyncChildProcess(boost::asio::io_service& ioService,
                         const std::string& exe,
                         const std::vector<std::string>& args,
                         const core::system::ProcessOptions& options);
   AsioAsyncChildProcess(boost::asio::io_service& ioService,
                         const std::string& command,
                         const core::system::ProcessOptions& options);

   virtual ~AsioAsyncChildProcess();

   Error run(const core::system::ProcessCallbacks& callbacks);
   void asyncWriteToStdin(const std::string& input, bool eof = false);

   virtual bool exited();

   virtual Error terminate();

   virtual bool hasNonWhitelistSubprocess() const;
   virtual bool hasWhitelistSubprocess() const;
   virtual core::FilePath getCwd() const;
   virtual bool hasRecentOutput() const;

   pid_t pid() const;

private:
   struct Impl;
   boost::shared_ptr<Impl> pAsioImpl_;
};

// forks a child process and runs the specified method within it
// optionally as a specific user to run as, or current user if none specified
// note: runAs requires root privileges (real user id of caller must be root)
Error forkAndRun(const boost::function<int(void)>& func,
                 const std::string& runAs = std::string());

// forks a child process and runs the specified method within it as the root user
// running as root requires the caller's real user id to be root
// if this is not the case, the method will be run as the calling user
Error forkAndRunPrivileged(const boost::function<int(void)>& func);

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_POSIX_CHILD_PROCESS_HPP
