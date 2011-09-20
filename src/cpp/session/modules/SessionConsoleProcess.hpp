/*
 * SessionConsoleProcess.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#ifndef SESSION_CONSOLE_PROCESS_HPP
#define SESSION_CONSOLE_PROCESS_HPP

#include <core/system/Process.hpp>

namespace core {
   class Error;
}

namespace session {
namespace modules {
namespace console_process {

class ConsoleProcess
{
public:
   ConsoleProcess(const std::string& command,
                  const core::system::ProcessOptions& options);

   std::string handle() const { return handle_; }

   core::Error start();
   void enqueueInput(const std::string& input);
   void interrupt();

   core::system::ProcessCallbacks createProcessCallbacks();

   bool onContinue(core::system::ProcessOperations& ops);
   void onStdout(core::system::ProcessOperations& ops,
                 const std::string& output);
   void onStderr(core::system::ProcessOperations& ops,
                 const std::string& output);
   void onExit(int exitCode);

private:
   // Command and options that will be used when start() is called
   std::string command_;
   core::system::ProcessOptions options_;

   // The handle that the client can use to refer to this process
   std::string handle_;

   // Whether the process has been successfully started
   bool started_;
   // Whether the process should be stopped
   bool interrupt_;

   // Pending writes to stdin
   std::string inputQueue_;
};

boost::shared_ptr<ConsoleProcess> createProcess(const std::string& command);

core::Error initialize();

} // namespace console_process
} // namespace modules
} // namespace session

#endif // SESSION_CONSOLE_PROCESS_HPP
