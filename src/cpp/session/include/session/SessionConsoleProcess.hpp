/*
 * SessionConsoleProcess.hpp
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
#ifndef SESSION_CONSOLE_PROCESS_HPP
#define SESSION_CONSOLE_PROCESS_HPP

#include <session/SessionConsoleProcessInfo.hpp>

#include <deque>

#include <boost/regex.hpp>
#include <boost/circular_buffer.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/thread/mutex.hpp>

#include <core/BoostSignals.hpp>
#include <core/system/Process.hpp>
#include <core/terminal/PrivateCommand.hpp>

#include <session/SessionConsoleProcessConnectionCallbacks.hpp>

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
const size_t kAutoFlushLength = 20;

class ConsoleProcess;
typedef boost::shared_ptr<ConsoleProcess> ConsoleProcessPtr;

/*
 * Note on multi-threading and locking: if connected to a terminal with websockets,
 * ConsoleProcess::onReceivedInput will be called on a separate thread. Thus, there
 * is inputOutputQueueMutex_ used to guard anything that can be modified by this call
 * but accessed elsewhere. Unpleasant. Be nice to refactor to contain this error-prone
 * complexity someday.
 */
class ConsoleProcess : boost::noncopyable,
                       public boost::enable_shared_from_this<ConsoleProcess>
{
private:
   // This constructor is only for resurrecting orphaned processes (i.e. for
   // suspend/resume scenarios)
   explicit ConsoleProcess(boost::shared_ptr<ConsoleProcessInfo> procInfo);

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

      bool interrupt;
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
   static ConsoleProcessPtr create(
         const std::string& command,
         core::system::ProcessOptions options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo);

   static ConsoleProcessPtr create(
         const std::string& program,
         const std::vector<std::string>& args,
         core::system::ProcessOptions options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo);

   static ConsoleProcessPtr createTerminalProcess(
         const std::string& command, // empty string for interactive shell
         core::system::ProcessOptions options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo,
         bool enableWebsockets);

   static ConsoleProcessPtr createTerminalProcess(
         core::system::ProcessOptions options,
         boost::shared_ptr<ConsoleProcessInfo> procInfo);

   static ConsoleProcessPtr createTerminalProcess(
         ConsoleProcessPtr proc);

   // Configure ProcessOptions for a terminal and return it. Also sets
   // the output param pSelectedShellType to indicate which shell type
   // was actually configured (e.g. what did 'default' get mapped to?).
   static core::system::ProcessOptions createTerminalProcOptions(
         const ConsoleProcessInfo& procInfo,
         TerminalShell::ShellType *pSelectedShellType);

   virtual ~ConsoleProcess() = default;

   // set a custom prompt handler -- return true to indicate the prompt
   // was handled and false to let it pass. return empty input to
   // indicate the user cancelled out of the prompt (in this case the
   // process will be terminated)
   void setPromptHandler(
         const boost::function<bool(const std::string&, Input*)>& onPrompt);

   RSTUDIO_BOOST_SIGNAL<void(int)>& onExit() { return onExit_; }

   std::string handle() const { return procInfo_->getHandle(); }
   InteractionMode interactionMode() const { return procInfo_->getInteractionMode(); }

   core::Error start();
   void enqueInput(const Input& input);
   void enqueInputInternalLock(const Input& input);
   Input dequeInput();
   void enquePrompt(const std::string& prompt);
   void interrupt();
   void interruptChild();
   void resize(int cols, int rows);
   bool isStarted() const { return started_; }
   void setCaption(std::string& caption) { procInfo_->setCaption(caption); }
   std::string getCaption() const { return procInfo_->getCaption(); }
   void setTitle(std::string& title) { procInfo_->setTitle(title); }
   std::string getTitle() const { return procInfo_->getTitle(); }
   void deleteLogFile(bool lastLineOnly = false) const;
   void deleteEnvFile() const;
   void setNotBusy() { procInfo_->setHasChildProcs(false); whitelistChildProc_ = false; }
   bool getIsBusy() const { return procInfo_->getHasChildProcs() || whitelistChildProc_; }
   bool getAllowRestart() const { return procInfo_->getAllowRestart(); }
   std::string getChannelMode() const;
   int getTerminalSequence() const { return procInfo_->getTerminalSequence(); }
   int getBufferLineCount() const { return procInfo_->getBufferLineCount(); }
   int getCols() const { return procInfo_->getCols(); }
   int getRows() const { return procInfo_->getRows(); }
   PidType getPid() const { return pid_; }
   bool getAltBufferActive() const { return procInfo_->getAltBufferActive(); }
   core::FilePath getCwd() const { return procInfo_->getCwd(); }
   bool getWasRestarted() const { return procInfo_->getRestarted(); }
   boost::optional<int> getExitCode() const { return procInfo_->getExitCode(); }

   std::string getShellName() const;
   TerminalShell::ShellType getShellType() const { return procInfo_->getShellType(); }

   // Used to downgrade to RPC mode after failed attempt to connect websocket
   void setRpcMode();

   // Get the given (0-based) chunk of the saved buffer; if more is available
   // after the requested chunk, *pMoreAvailable will be set to true
   std::string getSavedBufferChunk(int chunk, bool* pMoreAvailable) const;

   // Get the full terminal buffer
   std::string getBuffer() const;

   void setShowOnOutput(bool showOnOutput) const { procInfo_->setShowOnOutput(showOnOutput); }

   core::json::Object toJson(SerializationMode serialMode) const;
   static ConsoleProcessPtr fromJson(const core::json::Object& obj);

   void onReceivedInput(const std::string& input);

   void setZombie();
   static bool useWebsockets();

private:
   core::system::ProcessCallbacks createProcessCallbacks();
   bool onContinue(core::system::ProcessOperations& ops);
   void onStdout(core::system::ProcessOperations& ops,
                 const std::string& output);
   void onExit(int exitCode);
   void onHasSubprocs(bool hasNonWhitelistSubProcs, bool hasWhitelistSubprocs);
   void reportCwd(const core::FilePath& cwd);
   void processQueuedInput(core::system::ProcessOperations& ops);

   std::string bufferedOutput() const;
   void enqueOutputEvent(const std::string& output);
   void enquePromptEvent(const std::string& prompt);
   void handleConsolePrompt(core::system::ProcessOperations& ops,
                            const std::string& prompt);
   void maybeConsolePrompt(core::system::ProcessOperations& ops,
                           const std::string& output);

   ConsoleProcessSocketConnectionCallbacks createConsoleProcessSocketConnectionCallbacks();
   void onConnectionOpened();
   void onConnectionClosed();

   void saveEnvironment(const std::string& env);
   static void loadEnvironment(const std::string& handle, core::system::Options* pEnv);

private:
   // Command and options that will be used when start() is called
   std::string command_;
   std::string program_;
   std::vector<std::string> args_;
   core::system::ProcessOptions options_;
   boost::shared_ptr<ConsoleProcessInfo> procInfo_;

   // Whether the process should be stopped
   bool interrupt_ = false;

   // Whether to send pty interrupt
   bool interruptChild_ = false;
   
   // Whether the tty should be notified of a resize
   int newCols_ = -1; // -1 = no change
   int newRows_ = -1; // -1 = no change

   // Last known PID of associated process
   PidType pid_ = -1;

   // Has client been notified of state of childProcs_ at least once?
   bool childProcsSent_ = false;

   // Is there a child process matching the whitelist?
   bool whitelistChildProc_ = false;

   // Pending input (writes or ptyInterrupts)
   std::deque<Input> inputQueue_;
   int lastInputSequence_ = kIgnoreSequence;
   boost::mutex inputOutputQueueMutex_;

   boost::function<bool(const std::string&, Input*)> onPrompt_;
   RSTUDIO_BOOST_SIGNAL<void(int)> onExit_;

   // regex for prompt detection
   boost::regex controlCharsPattern_;
   boost::regex promptPattern_;

   // is the underlying process started?
   bool started_ = false;

   // cached pointer to process operations, for use in websocket thread callbacks
   boost::weak_ptr<core::system::ProcessOperations> pOps_;
   boost::mutex procOpsMutex_;

   // private command handler, used to capture environment variables during terminal idle time
   core::terminal::PrivateCommand envCaptureCmd_;
};

core::json::Array processesAsJson(SerializationMode serialMode);
core::Error initialize();

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_HPP
