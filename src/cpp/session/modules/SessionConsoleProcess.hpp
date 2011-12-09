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

#include <boost/circular_buffer.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/system/Process.hpp>
#include <core/Log.hpp>

#include <core/json/Json.hpp>

namespace core {
   class Error;
}

namespace session {
namespace modules {
namespace console_process {

class ConsoleProcess : boost::noncopyable,
                       public boost::enable_shared_from_this<ConsoleProcess>
{
private:
   // This constructor is only for resurrecting orphaned processes (i.e. for
   // suspend/resume scenarios)
   ConsoleProcess();

   ConsoleProcess(
         const std::string& command,
         const core::system::ProcessOptions& options,
         const std::string& caption,
         bool dialog,
         bool interactive,
         const boost::function<void()>& onExit);

   ConsoleProcess(
         const std::string& program,
         const std::vector<std::string>& args,
         const core::system::ProcessOptions& options,
         const std::string& caption,
         bool dialog,
         bool interactive,
         const boost::function<void()>& onExit);

   void commonInit();

public:
   static boost::shared_ptr<ConsoleProcess> create(
         const std::string& command,
         core::system::ProcessOptions options,
         const std::string& caption,
         bool dialog,
         bool interactive,
         const boost::function<void()>& onExit=boost::function<void()>());

   static boost::shared_ptr<ConsoleProcess> create(
         const std::string& program,
         const std::vector<std::string>& args,
         core::system::ProcessOptions options,
         const std::string& caption,
         bool dialog,
         bool interactive,
         const boost::function<void()>& onExit=boost::function<void()>());

   virtual ~ConsoleProcess() {}

   std::string handle() const { return handle_; }
   bool interactive() const { return interactive_; }
   std::string bufferedOutput() const;

   core::Error start();
   void enqueueInput(const std::string& input);

   void ptyInterrupt();

   void interrupt();

   core::system::ProcessCallbacks createProcessCallbacks();

   bool onContinue(core::system::ProcessOperations& ops);
   void onStdout(core::system::ProcessOperations& ops,
                 const std::string& output);
   void onStderr(core::system::ProcessOperations& ops,
                 const std::string& output);
   void onExit(int exitCode);

   core::json::Object toJson() const;

   static boost::shared_ptr<ConsoleProcess> fromJson(
                                              core::json::Object& obj);

private:
   // Command and options that will be used when start() is called
   std::string command_;
   std::string program_;
   std::vector<std::string> args_;
   core::system::ProcessOptions options_;

   std::string caption_;
   bool dialog_;
   bool interactive_;

   // The handle that the client can use to refer to this process
   std::string handle_;

   // Whether the process has been successfully started
   bool started_;
   // Whether the process should issue a pty interrupt
   bool ptyInterrupt_;
   // Whether the process should be stopped
   bool interrupt_;

   // Pending writes to stdin
   std::string inputQueue_;

   // Buffer output in case client disconnects/reconnects and needs
   // to recover some history
   boost::circular_buffer<char> outputBuffer_;

   boost::optional<int> exitCode_;

   boost::function<void()> onExit_;
};

core::json::Array processesAsJson();
core::Error initialize();

} // namespace console_process
} // namespace modules
} // namespace session

#endif // SESSION_CONSOLE_PROCESS_HPP
