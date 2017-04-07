/*
 * SessionConsoleProcess.hpp
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
#ifndef SESSION_CONSOLE_PROCESS_HPP
#define SESSION_CONSOLE_PROCESS_HPP

#include <session/SessionConsoleProcessInfo.hpp>

#include <deque>

#include <boost/regex.hpp>
#include <boost/signals.hpp>
#include <boost/circular_buffer.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/system/Process.hpp>

#include <session/SessionConsoleProcessSocket.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace console_process {

const int kFlushSequence = -2; // see ShellInput.FLUSH_SEQUENCE
const int kIgnoreSequence = -1; // see ShellInput.IGNORE_SEQUENCE
const int kAutoFlushLength = 20;

class ConsoleProcess : boost::noncopyable,
                       public boost::enable_shared_from_this<ConsoleProcess>
{
private:
   // This constructor is only for resurrecting orphaned processes (i.e. for
   // suspend/resume scenarios)
   ConsoleProcess(boost::shared_ptr<ConsoleProcessInfo> procInfo);

   ConsoleProcess(
         const std::string& command,
         const core::system::ProcessOptions& options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo);
  
   ConsoleProcess(
         const std::string& program,
         const std::vector<std::string>& args,
         const core::system::ProcessOptions& options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo);
   
   void regexInit();
   void commonInit();

public:
   struct Input
   {
      explicit Input(const std::string& text, bool echoInput = true)
         : interrupt(false), text(text), echoInput(echoInput),
           sequence(kIgnoreSequence)
      {
      }

      explicit Input(int sequence, const std::string& text, bool echoInput = true)
         : interrupt(false), text(text), echoInput(echoInput), sequence(sequence)
      {
      }

      Input() : interrupt(false), echoInput(false), sequence(kIgnoreSequence) {}

      bool empty() { return !interrupt && text.empty(); }

      bool interrupt ;
      std::string text;
      bool echoInput;

      // used to reassemble out-of-order input messages
      int sequence;
   };

public:
   // creating console processes with a command string is not supported on
   // Win32 because in order to implement the InteractionPossible/Always
   // modes we use the consoleio.exe proxy, which can only be invoked from
   // the runProgram codepath
   static boost::shared_ptr<ConsoleProcess> create(
         const std::string& command,
         core::system::ProcessOptions options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo);

   static boost::shared_ptr<ConsoleProcess> create(
         const std::string& program,
         const std::vector<std::string>& args,
         core::system::ProcessOptions options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo);

   static boost::shared_ptr<ConsoleProcess> createTerminalProcess(
         core::system::ProcessOptions options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo,
         bool enableWebsockets = true);
   
   virtual ~ConsoleProcess() {}

   // set a custom prompt handler -- return true to indicate the prompt
   // was handled and false to let it pass. return empty input to
   // indicate the user cancelled out of the prompt (in this case the
   // process will be terminated)
   void setPromptHandler(
         const boost::function<bool(const std::string&, Input*)>& onPrompt);

   boost::signal<void(int)>& onExit() { return onExit_; }

   std::string handle() const { return procInfo_->getHandle(); }
   InteractionMode interactionMode() const { return procInfo_->getInteractionMode(); }

   core::Error start();
   void enqueInput(const Input& input);
   Input dequeInput();
   void enquePrompt(const std::string& prompt);
   void interrupt();
   void resize(int cols, int rows);
   void onSuspend();
   bool isStarted() { return started_; }
   void setCaption(std::string& caption) { procInfo_->setCaption(caption); }
   void setTitle(std::string& title) { procInfo_->setTitle(title); }
   void deleteLogFile() const;
   void setNotBusy() { procInfo_->setHasChildProcs(false); }

   // Used to downgrade to RPC mode after failed attempt to connect websocket
   void setRpcMode();

   // Get the given (0-based) chunk of the saved buffer; if more is available
   // after the requested chunk, *pMoreAvailable will be set to true
   std::string getSavedBufferChunk(int chunk, bool* pMoreAvailable) const;

   void setShowOnOutput(bool showOnOutput) { procInfo_->setShowOnOutput(showOnOutput); }

   core::json::Object toJson() const;
   static boost::shared_ptr<ConsoleProcess> fromJson( core::json::Object& obj);

private:
   core::system::ProcessCallbacks createProcessCallbacks();
   bool onContinue(core::system::ProcessOperations& ops);
   void onStdout(core::system::ProcessOperations& ops,
                 const std::string& output);
   void onExit(int exitCode);
   void onHasSubprocs(bool hasSubProcs);
   void processQueuedInput(core::system::ProcessOperations& ops);

   std::string bufferedOutput() const;
   void enqueOutputEvent(const std::string& output);
   void enquePromptEvent(const std::string& prompt);
   void handleConsolePrompt(core::system::ProcessOperations& ops,
                            const std::string& prompt);
   void maybeConsolePrompt(core::system::ProcessOperations& ops,
                           const std::string& output);

   ConsoleProcessSocketConnectionCallbacks createConsoleProcessSocketConnectionCallbacks();
   void onReceivedInput(const std::string& input);
   void onConnectionOpened();
   void onConnectionClosed();

private:
   // Command and options that will be used when start() is called
   std::string command_;
   std::string program_;
   std::vector<std::string> args_;
   core::system::ProcessOptions options_;
   boost::shared_ptr<ConsoleProcessInfo> procInfo_;

   // Whether the process should be stopped
   bool interrupt_;
   
   // Whether the tty should be notified of a resize
   int newCols_; // -1 = no change
   int newRows_; // -1 = no change

   // Has client been notified of state of childProcs_ at least once?
   bool childProcsSent_;

   // Pending input (writes or ptyInterrupts)
   std::deque<Input> inputQueue_;
   int lastInputSequence_;

   boost::function<bool(const std::string&, Input*)> onPrompt_;
   boost::signal<void(int)> onExit_;

   // regex for prompt detection
   boost::regex controlCharsPattern_;
   boost::regex promptPattern_;

   // is the underlying process started?
   bool started_;

   // cached pointer to process options, for use in websocket thread callbacks
   boost::weak_ptr<core::system::ProcessOperations> pOps_;
   boost::mutex mutex_;
};


class PasswordManager : boost::noncopyable
{
public:
   typedef boost::function<bool(const std::string&, bool, std::string*, bool*)>
                                                               PromptHandler;

   explicit PasswordManager(const boost::regex& promptPattern,
                            const PromptHandler& promptHandler)
      : promptPattern_(promptPattern), promptHandler_(promptHandler)
   {
   }
   virtual ~PasswordManager() {}

   // COPYING: boost::noncopyable

public:
   // NOTE: if you don't showRememberOption then passwords from that
   // interaction will NOT be remembered after the parent console
   // process exits
   void attach(boost::shared_ptr<ConsoleProcess> pCP,
               bool showRememberOption = true);


private:
   bool handlePrompt(const std::string& cpHandle,
                     const std::string& prompt,
                     bool showRememberOption,
                     ConsoleProcess::Input* pInput);

   void onExit(const std::string& cpHandle, int exitCode);

   struct CachedPassword
   {
      CachedPassword() : remember(false) {}
      std::string cpHandle;
      std::string prompt;
      std::string password;
      bool remember;
   };

   static bool hasPrompt(const CachedPassword& cachedPassword,
                         const std::string& prompt);

   static bool hasHandle(const CachedPassword& cachedPassword,
                         const std::string& cpHandle);

   static bool forgetOnExit(const CachedPassword& cachedPassword,
                            const std::string& cpHandle);

private:
   boost::regex promptPattern_;
   PromptHandler promptHandler_;
   std::vector<CachedPassword> passwords_;
};

core::json::Array processesAsJson();
core::Error initialize();

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_HPP
