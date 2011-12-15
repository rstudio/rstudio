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

#include <queue>

#include <boost/signals.hpp>
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

enum InteractionMode
{
   InteractionNever = 0,
   InteractionPossible = 1,
   InteractionAlways = 2
};

extern const int kDefaultMaxOutputLines;

class ConsoleProcess : boost::noncopyable,
                       public boost::enable_shared_from_this<ConsoleProcess>
{
private:
   // This constructor is only for resurrecting orphaned processes (i.e. for
   // suspend/resume scenarios)
   ConsoleProcess();

#ifndef _WIN32
   ConsoleProcess(
         const std::string& command,
         const core::system::ProcessOptions& options,
         const std::string& caption,
         bool dialog,
         InteractionMode mode,
         int maxOutputLines);
#endif

   ConsoleProcess(
         const std::string& program,
         const std::vector<std::string>& args,
         const core::system::ProcessOptions& options,
         const std::string& caption,
         bool dialog,
         InteractionMode mode,
         int maxOutputLines);

   void commonInit();

public:
   struct Input
   {
      Input() : interrupt(false), echoInput(false) {}

      bool empty() { return !interrupt && text.empty(); }

      bool interrupt ;
      std::string text;
      bool echoInput;
   };

public:
   // creating console processes with a command string is not supported on
   // Win32 because in order to implement the InteractionPossible/Always
   // modes we use the consoleio.exe proxy, which can only be invoked from
   // the runProgram codepath
#ifndef _WIN32
   static boost::shared_ptr<ConsoleProcess> create(
         const std::string& command,
         core::system::ProcessOptions options,
         const std::string& caption,
         bool dialog,
         InteractionMode mode,
         int maxOutputLines = kDefaultMaxOutputLines);
#endif

   static boost::shared_ptr<ConsoleProcess> create(
         const std::string& program,
         const std::vector<std::string>& args,
         core::system::ProcessOptions options,
         const std::string& caption,
         bool dialog,
         InteractionMode mode,
         int maxOutputLines = kDefaultMaxOutputLines);

   virtual ~ConsoleProcess() {}

   void setPromptHandler(
         const boost::function<Input(const std::string&)>& onPrompt);

   boost::signal<void(int)>& onExit() { return onExit_; }

   std::string handle() const { return handle_; }
   InteractionMode interactionMode() const { return interactionMode_; }

   core::Error start();
   void enqueInput(const Input& input);
   void interrupt();

   core::json::Object toJson() const;
   static boost::shared_ptr<ConsoleProcess> fromJson(
                                              core::json::Object& obj);

private:
   core::system::ProcessCallbacks createProcessCallbacks();
   bool onContinue(core::system::ProcessOperations& ops);
   void onStdout(core::system::ProcessOperations& ops,
                 const std::string& output);
   void onExit(int exitCode);

   std::string bufferedOutput() const;
   void appendToOutputBuffer(const std::string& str);
   void enqueOutputEvent(const std::string& output, bool error);
   void handleConsolePrompt(const std::string& prompt);
   void maybeConsolePrompt(const std::string& output);

private:
   // Command and options that will be used when start() is called
   std::string command_;
   std::string program_;
   std::vector<std::string> args_;
   core::system::ProcessOptions options_;

   std::string caption_;
   bool dialog_;
   InteractionMode interactionMode_;
   int maxOutputLines_;

   // The handle that the client can use to refer to this process
   std::string handle_;

   // Whether the process has been successfully started
   bool started_;

   // Whether the process should be stopped
   bool interrupt_;

   // Pending input (writes or ptyInterrupts)
   std::queue<Input> inputQueue_;

   // Buffer output in case client disconnects/reconnects and needs
   // to recover some history
   boost::circular_buffer<char> outputBuffer_;

   boost::optional<int> exitCode_;

   boost::function<Input(const std::string&)> onPrompt_;
   boost::signal<void(int)> onExit_;
};


class PasswordManager : boost::noncopyable
{
public:
   PasswordManager() {}
   virtual ~PasswordManager() {}

   // COPYING: boost::noncopyable

public:
   void attach(boost::shared_ptr<ConsoleProcess> pCP);


private:
   ConsoleProcess::Input handlePrompt(const std::string& cpHandle,
                                      const std::string& prompt);

   void onExit(const std::string& cpHandle, int exitCode);

private:
   struct CachedPassword
   {
      std::string cpHandle;
      std::string prompt;
      std::string password;
   };

   std::vector<CachedPassword> passwords_;
};

core::json::Array processesAsJson();
core::Error initialize();

} // namespace console_process
} // namespace modules
} // namespace session

#endif // SESSION_CONSOLE_PROCESS_HPP
