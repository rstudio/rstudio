/*
 * SessionConsoleProcess.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <sstream>

#include <core/system/Interrupts.hpp>

#include <r/RExec.hpp>

#include <session/SessionConsoleProcess.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcessSocket.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/projects/SessionProjects.hpp>

#include "modules/SessionWorkbench.hpp"
#include "modules/SessionReticulate.hpp"

#include "SessionConsoleProcessTable.hpp"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace console_process {

namespace {

ConsoleProcessSocket s_terminalSocket;

// Posix-only, use is gated via getTrackEnv() always being false on Win32.
const std::string kEnvCommand = "/usr/bin/env";

// Environment variables that shouldn't be saved / restored when serializing a Terminal session.
std::set<std::string> s_ignoredEnvironmentVariables;

void initializeIgnoredEnvironmentVariables()
{
   s_ignoredEnvironmentVariables.clear();
   prefs::userPrefs().terminalIgnoredEnvironmentVariables().toSetString(s_ignoredEnvironmentVariables);
}

void onUserPrefsChanged(const std::string& layerName, const std::string& prefName)
{
   if (prefName == kTerminalIgnoredEnvironmentVariables)
      initializeIgnoredEnvironmentVariables();
}

// environment variables which should be ignored, e.g. because they represent
// session state that won't be valid after a restart
bool isIgnoredEnvironmentVariable(const std::string& name)
{
   return
         name == "_" ||
         boost::algorithm::starts_with(name, "R_") ||
         boost::algorithm::starts_with(name, "RSTUDIO_SESSION_") ||
         s_ignoredEnvironmentVariables.count(name);
}

} // anonymous namespace

void ConsoleProcess::setenv(const std::string& name,
                            const std::string& value)
{
   core::system::setenv(
            &*options_.environment,
            name,
            value);
}

// create process options for a terminal
core::system::ProcessOptions ConsoleProcess::createTerminalProcOptions(
      const ConsoleProcessInfo& procInfo,
      TerminalShell::ShellType* pSelectedShellType)
{
   // configure environment for shell, inheriting system environment
   //
   // note that we do this even if the process has a saved environment to be
   // used, as some environment variables might be tied to session state and so
   // need to be re-generated so that the correct value is reflected in the
   // terminal process
   core::system::Options shellEnv;
   core::system::environment(&shellEnv);
 
   // overlay saved environment variables
   if (procInfo.getTrackEnv() && !procInfo.getHandle().empty())
      loadEnvironment(procInfo.getHandle(), &shellEnv);

#ifdef __APPLE__
   // suppress macOS Catalina warning suggesting switching to zsh
   core::system::setenv(&shellEnv, "BASH_SILENCE_DEPRECATION_WARNING", "1");
#endif

   *pSelectedShellType = procInfo.getShellType();

#ifdef _WIN32
   core::system::setHomeToUserProfile(&shellEnv);
#endif
   
   // don't forward GIT_ASKPASS or SSH_ASKPASS if it's set to our rpostback
   // handlers -- those aren't required in a terminal, as the user can be
   // prompted for input via stdin, and it's likely that users will munge
   // the PATH within a terminal a way that makes our utilities unavailable
   //
   // TODO: could we just set an absolute path to the rpostback-askpass util?
   std::string gitAskpass = core::system::getenv("GIT_ASKPASS");
   if (gitAskpass == "rpostback-askpass")
      core::system::unsetenv(&shellEnv, "GIT_ASKPASS");

   std::string sshAskpass = core::system::getenv("SSH_ASKPASS");
   if (sshAskpass == "rpostback-askpass")
      core::system::unsetenv(&shellEnv, "SSH_ASKPASS");
   
   // amend shell paths as appropriate
   session::modules::workbench::amendShellPaths(&shellEnv);

   // set options
   core::system::ProcessOptions options;
   options.workingDir = procInfo.getCwd().isEmpty()
         ? module_context::shellWorkingDirectory()
         : procInfo.getCwd();
   
   options.smartTerminal = true;
#ifdef _WIN32
   options.reportHasSubprocs = false; // child process detection not supported on Windows
#else
   options.reportHasSubprocs = true;
#endif
   options.callbacksRequireMainThread = false; // to allow use of terminal from offline thread
   options.trackCwd = true;
   options.cols = procInfo.getCols();
   options.rows = procInfo.getRows();

   if (prefs::userPrefs().busyDetection() == kBusyDetectionList)
   {
      std::vector<std::string> exclusionList;
      prefs::userPrefs().busyExclusionList().toVectorString(exclusionList);
      options.ignoredSubprocs = exclusionList;
   }

   // set path to shell
   AvailableTerminalShells shells;
   TerminalShell shell;

   if (shells.getInfo(procInfo.getShellType(), &shell))
   {
      *pSelectedShellType = shell.type;
      options.shellPath = shell.path;
      options.args = shell.args;
   }

   // last-ditch, use system shell
   if (!options.shellPath.exists())
   {
      TerminalShell sysShell;
      if (AvailableTerminalShells::getSystemShell(&sysShell))
      {
         *pSelectedShellType = sysShell.type;
         options.shellPath = sysShell.path;
         options.args = sysShell.args;
      }
   }
   
#ifndef _WIN32
   // set xterm title to show current working directory after each command
   switch (shell.getEffectiveShellType())
   {
   
   case TerminalShell::ShellType::PosixBash:
   {
      // NOTE: We don't use `${string//pattern/replacement}`-style variable substitution
      // on Bash, as the way tilde is handled as a replacement character seems to depend
      // on some unknown factors (sometimes it needs to be escaped like '\~'?)
      const char* kPromptCommand = R"((_RS_PWD=$(dirs +0); echo -ne "\033]0;${_RS_PWD}\007"; unset _RS_PWD))"; 
      core::system::setenv(&shellEnv, "PROMPT_COMMAND", kPromptCommand);
      break;
   }
      
   default:
   {
      const char* kPromptCommand = R"(echo -ne "\033]0;${PWD/#${HOME}/~}\007")"; 
      core::system::setenv(&shellEnv, "PROMPT_COMMAND", kPromptCommand);
      break;
   }
      
   } // end switch

   // don't add commands starting with a space to shell history
   if (procInfo.getTrackEnv())
   {
      // HISTCONTROL is Bash-specific. In Zsh we rely on the shell having the 
      // HIST_IGNORE_SPACE option set, which we do via -g when we start Zsh. In the
      // future we could make environment-capture smarter and not set this variable
      // for shells that don't use it, but for now keeping it to avoid having to 
      // rework PrivateCommand class.
      core::system::setenv(&shellEnv, "HISTCONTROL", "ignoreboth");
   }
#endif

   // finally, set up environment
   options.environment = shellEnv;
   
   return options;
}

ConsoleProcess::ConsoleProcess(boost::shared_ptr<ConsoleProcessInfo> procInfo)
   : procInfo_(procInfo), envCaptureCmd_(kEnvCommand)
{
   regexInit();

   // When we retrieve from outputBuffer, we only want complete lines. Add a
   // dummy \n so we can tell the first line is a complete line.
   procInfo_->appendToOutputBuffer('\n');
}

ConsoleProcess::ConsoleProcess(const std::string& command,
                               const core::system::ProcessOptions& options,
                               boost::shared_ptr<ConsoleProcessInfo> procInfo)
   : command_(command), options_(options), procInfo_(procInfo),
     envCaptureCmd_(kEnvCommand)
{
   commonInit();
}

ConsoleProcess::ConsoleProcess(const std::string& program,
                               const std::vector<std::string>& args,
                               const core::system::ProcessOptions& options,
                               boost::shared_ptr<ConsoleProcessInfo> procInfo)
   : program_(program), args_(args), options_(options), procInfo_(procInfo),
     envCaptureCmd_(kEnvCommand)
{
   commonInit();
}

void ConsoleProcess::regexInit()
{
   controlCharsPattern_ = boost::regex("[\\r\\b]");
   promptPattern_ = boost::regex("^(.+)[\\W_]( +)$");
}

void ConsoleProcess::commonInit()
{
   regexInit();
   procInfo_->ensureHandle();

   // always redirect stderr to stdout so output is interleaved
   options_.redirectStdErrToStdOut = true;

   // ensure that we have an environment block to modify
   if (!options_.environment)
   {
      core::system::Options childEnv;
      core::system::environment(&childEnv);
      options_.environment = childEnv;
   }

   if (interactionMode() != InteractionNever || options_.smartTerminal)
   {
#ifdef _WIN32
      // NOTE: We use consoleio.exe here in order to make sure svn.exe password
      // prompting works properly

      FilePath consoleIoPath = session::options().consoleIoPath();

      // if this is as runProgram then fixup the program and args
      if (!program_.empty())
      {
         options_.createNewConsole = true;

         // build new args
         shell_utils::ShellArgs args;
         args << program_;
         args << args_;

         // fixup program_ and args_ so we run the consoleio.exe proxy
         program_ = consoleIoPath.getAbsolutePathNative();
         args_ = args;
      }
      // if this is a runCommand then prepend consoleio.exe to the command
      else if (!command_.empty())
      {
         options_.createNewConsole = true;
         command_ = shell_utils::escape(consoleIoPath) + " " + command_;
      }
      else // terminal
      {
         // undefine TERM, as it puts git-bash in a mode that winpty doesn't support;
         core::system::unsetenv(&(options_.environment.get()), "TERM");

         // request a pseudoterminal if this is an interactive console process
         options_.pseudoterminal = core::system::Pseudoterminal(
                  session::options().winptyPath(),
                  false /*plainText*/,
                  false /*conerr*/,
                  options_.cols,
                  options_.rows);
      }
#else
      // request a pseudoterminal if this is an interactive console process
      options_.pseudoterminal = core::system::Pseudoterminal(options_.cols,
                                                             options_.rows);

      // define TERM
      core::system::setenv(&(options_.environment.get()), "TERM",
                           options_.smartTerminal ? core::system::kSmartTerm :
                                                    core::system::kDumbTerm);

      // environment variables that can be used when configuring custom shells
      core::system::setenv(&(options_.environment.get()), "RSTUDIO_TERM", procInfo_->getHandle());

      core::system::setenv(&(options_.environment.get()), "RSTUDIO_PROJ_NAME",
                           projects::projectContext().file().getStem());
      core::system::setenv(&(options_.environment.get()), "RSTUDIO_SESSION_ID",
                           module_context::activeSession().id());
#endif
   }
   
   // this runs in the terminal pane as a child process of this process
   core::system::setenv(&(options_.environment.get()), "RSTUDIO_CHILD_PROCESS_PANE", "terminal");
   
   // When we retrieve from outputBuffer, we only want complete lines. Add a
   // dummy \n so we can tell the first line is a complete line.
   if (!options_.smartTerminal)
      procInfo_->appendToOutputBuffer('\n');
}

std::string ConsoleProcess::bufferedOutput() const
{
   if (options_.smartTerminal)
      return "";

   return procInfo_->bufferedOutput();
}

void ConsoleProcess::setPromptHandler(
      const boost::function<bool(const std::string&, Input*)>& onPrompt)
{
   onPrompt_ = onPrompt;
}

Error ConsoleProcess::start()
{
   if (started_ || procInfo_->getZombie())
      return Success();

   Error error;
   if (!command_.empty())
   {
      error = module_context::processSupervisor().runCommand(
                                 command_, options_, createProcessCallbacks());
   }
   else if (!program_.empty())
   {
      error = module_context::processSupervisor().runProgram(
                          program_, args_, options_, createProcessCallbacks());
   }
   else
   {
      error = module_context::processSupervisor().runTerminal(
                          options_, createProcessCallbacks());
   }
   if (!error)
      started_ = true;

   return error;
}

void ConsoleProcess::enqueInputInternalLock(const Input& input)
{
   LOCK_MUTEX(inputOutputQueueMutex_)
   {
      enqueInput(input);
   }
   END_LOCK_MUTEX
}

void ConsoleProcess::enqueInput(const Input& input)
{
   if (input.sequence == kIgnoreSequence)
   {
      inputQueue_.push_back(input);
      return;
   }

   if (input.sequence == kFlushSequence)
   {
      inputQueue_.push_back(input);

      // set everything in queue to "ignore" so it will be pulled from
      // queue as-is, even with gaps
      for (auto &it : inputQueue_)
      {
         it.sequence = kIgnoreSequence;
      }
      lastInputSequence_ = kIgnoreSequence;
      return;
   }

   // insert in order by sequence
   for (auto it = inputQueue_.begin(); it != inputQueue_.end(); it++)
   {
      if (input.sequence < (*it).sequence)
      {
         inputQueue_.insert(it, input);
         return;
      }
   }
   inputQueue_.push_back(input);
}

ConsoleProcess::Input ConsoleProcess::dequeInput()
{
   // Pull next available Input from queue; return an empty Input
   // if none available or if an out-of-sequence entry is
   // reached; assumption is the missing item(s) will eventually
   // arrive and unblock the backlog.
   if (inputQueue_.empty())
      return Input();

   Input input = inputQueue_.front();
   if (input.sequence == kIgnoreSequence || input.sequence == kFlushSequence)
   {
      inputQueue_.pop_front();
      return input;
   }

   if (input.sequence == lastInputSequence_ + 1)
   {
      lastInputSequence_++;
      inputQueue_.pop_front();
      return input;
   }

   // Getting here means input is out of sequence. We want to prevent
   // getting permanently stuck if a message gets lost and the
   // gap(s) never get filled in. So we'll flush it if input piles up.
   if (inputQueue_.size() >= kAutoFlushLength)
   {
      // set everything in queue to "ignore" so it will be pulled from
      // queue as-is, even with gaps
      for (auto &it : inputQueue_)
      {
         lastInputSequence_ = it.sequence;
         it.sequence = kIgnoreSequence;
      }

      input.sequence = kIgnoreSequence;
      inputQueue_.pop_front();
      return input;
   }

   return Input();
}

void ConsoleProcess::enquePromptEvent(const std::string& prompt)
{
   // enque a prompt event
   json::Object data;
   data["handle"] = handle();
   data["prompt"] = prompt;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessPrompt, data));
}

void ConsoleProcess::enquePrompt(const std::string& prompt)
{
   enquePromptEvent(prompt);
}

void ConsoleProcess::interrupt()
{
   core::system::interrupt(pid_);
   interruptCount_ += 1;
}

void ConsoleProcess::interruptChild()
{
   interruptChild_ = true;
}

void ConsoleProcess::resize(int cols, int rows)
{
   newCols_ = cols;
   newRows_ = rows;
}

bool ConsoleProcess::onContinue(core::system::ProcessOperations& ops)
{
   // if we've attempted to interrupt this process multiple times,
   // but this process still appears to be running, then exit
   if (interruptCount_ >= 3)
   {
      return false;
   }

   // send SIGINT to children of the shell
   if (interruptChild_)
   {
      Error error = ops.ptyInterrupt();
      if (error)
         LOG_ERROR(error);
      interruptChild_ = false;
   }

   LOCK_MUTEX(inputOutputQueueMutex_)
   {
      if (procInfo_->getTrackEnv())
      {
         // try to capture and persist the environment
         if (envCaptureCmd_.onTryCapture(ops, getIsBusy()))
            return true;

         saveEnvironment(envCaptureCmd_.getPrivateOutput());
      }

      // For RPC-based communication, this is where input is always dispatched; for websocket
      // communication, it is normally dispatched inside onReceivedInput, but this call is needed
      // to deal with input built-up during a privateCommandLoop.
      processQueuedInput(ops);

      // capture weak reference to the callbacks so websocket callback
      // can use them
      pOps_ = ops.weak_from_this();
   }
   END_LOCK_MUTEX

   if (newCols_ != -1 && newRows_ != -1)
   {
      ops.ptySetSize(newCols_, newRows_);
      procInfo_->setCols(newCols_);
      procInfo_->setRows(newRows_);
      newCols_ = -1;
      newRows_ = -1;
      saveConsoleProcesses();
   }

   pid_ = ops.getPid();

   // continue
   return true;
}

void ConsoleProcess::processQueuedInput(core::system::ProcessOperations& ops)
{
   // process input queue
   Input input = dequeInput();
   while (!input.empty())
   {
      // pty interrupt
      if (input.interrupt)
      {
         Error error = ops.ptyInterrupt();
         if (error)
            LOG_ERROR(error);

         if (input.echoInput)
            procInfo_->appendToOutputBuffer("^C");
      }

      // text input
      else
      {
         std::string inputText = input.text;

         envCaptureCmd_.userInput(inputText);

#ifdef _WIN32
         if (!options_.smartTerminal)
         {
            string_utils::convertLineEndings(&inputText, string_utils::LineEndingWindows);
         }
#endif
         Error error = ops.writeToStdin(inputText, false);
         if (error)
            LOG_ERROR(error);

         if (!options_.smartTerminal) // smart terminal does echo via pty
         {
            if (input.echoInput)
               procInfo_->appendToOutputBuffer(inputText);
            else
               procInfo_->appendToOutputBuffer("\n");
         }
      }

      input = dequeInput();
   }
}

void ConsoleProcess::deleteLogFile(bool lastLineOnly) const
{
   procInfo_->deleteLogFile(lastLineOnly);
}

void ConsoleProcess::deleteEnvFile() const
{
   procInfo_->deleteEnvFile();
}

std::string ConsoleProcess::getSavedBufferChunk(int chunk, bool* pMoreAvailable) const
{
   return procInfo_->getSavedBufferChunk(chunk, pMoreAvailable);
}

std::string ConsoleProcess::getBuffer() const
{
   return procInfo_->getFullSavedBuffer();
}

void ConsoleProcess::enqueOutputEvent(const std::string &output)
{
   if (envCaptureCmd_.output(output))
      return;

   // normal output processing
   bool currentAltBufferStatus = procInfo_->getAltBufferActive();

   // copy to output buffer
   procInfo_->appendToOutputBuffer(output);

   if (procInfo_->getAltBufferActive() != currentAltBufferStatus)
      saveConsoleProcesses();

   // If there's more output than the client can even show, then
   // truncate it to the amount that the client can show. Too much
   // output can overwhelm the client, making it unresponsive.
   std::string trimmedOutput = output;
   if (!prefs::userPrefs().limitVisibleConsole())
      string_utils::trimLeadingLines(procInfo_->getMaxOutputLines(), &trimmedOutput);

   if (procInfo_->getChannelMode() == Websocket)
   {
      s_terminalSocket.sendText(procInfo_->getHandle(), output);
      return;
   }

   // Rpc
   json::Object data;
   data["handle"] = handle();
   data["output"] = trimmedOutput;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessOutput, data));
}

void ConsoleProcess::onStdout(core::system::ProcessOperations& ops,
                              const std::string& output)
{
   if (options_.smartTerminal)
   {
      LOCK_MUTEX(inputOutputQueueMutex_)
      {
         enqueOutputEvent(output);
      }
      END_LOCK_MUTEX
      return;
   }

   // convert line endings to posix
   std::string posixOutput = output;
   string_utils::convertLineEndings(&posixOutput,
                                    string_utils::LineEndingPosix);

   // process as normal output or detect a prompt if there is one
   if (boost::algorithm::ends_with(posixOutput, "\n"))
   {
      enqueOutputEvent(posixOutput);
   }
   else
   {
      // look for the last newline and take the content after
      // that as the prompt
      std::size_t lastLoc = posixOutput.find_last_of("\n\f");
      if (lastLoc != std::string::npos)
      {
         enqueOutputEvent(posixOutput.substr(0, lastLoc));
         maybeConsolePrompt(ops, posixOutput.substr(lastLoc + 1));
      }
      else
      {
         maybeConsolePrompt(ops, posixOutput);
      }
   }
}

void ConsoleProcess::maybeConsolePrompt(core::system::ProcessOperations& ops,
                                        const std::string& output)
{
   boost::smatch smatch;

   // treat special control characters as output rather than a prompt
   if (regex_utils::search(output, smatch, controlCharsPattern_))
      enqueOutputEvent(output);

   // make sure the output matches our prompt pattern
   if (!regex_utils::match(output, smatch, promptPattern_))
      enqueOutputEvent(output);

   // it is a prompt
   else
      handleConsolePrompt(ops, output);
}

void ConsoleProcess::handleConsolePrompt(core::system::ProcessOperations& ops,
                                         const std::string& prompt)
{
   // if there is a custom prompt handler then give it a chance to
   // handle the prompt first
   if (onPrompt_)
   {
      Input input;
      if (onPrompt_(prompt, &input))
      {
         if (!input.empty())
         {
            enqueInput(input);
         }
         else
         {
            Error error = ops.terminate();
            if (error)
              LOG_ERROR(error);
         }

         return;
      }
   }

   enquePromptEvent(prompt);
}

void ConsoleProcess::onExit(int exitCode)
{
   procInfo_->setExitCode(exitCode);
   procInfo_->setHasChildProcs(false);

   if (procInfo_->getAutoClose() == DefaultAutoClose)
   {
      procInfo_->setAutoClose(
            ConsoleProcessInfo::closeModeFromPref(prefs::userPrefs().terminalCloseBehavior()));
   }

   if (procInfo_->getAutoClose() == NeverAutoClose)
   {
      setZombie();
   }
   saveConsoleProcesses();

   json::Object data;
   data["handle"] = handle();
   data["exitCode"] = exitCode;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessExit, data));

   onExit_(exitCode);
}

void ConsoleProcess::onHasSubprocs(bool hasNonIgnoredSubprocs, bool hasIgnoredSubprocs)
{
   ignoredChildProc_ = hasIgnoredSubprocs;
   if (hasNonIgnoredSubprocs != procInfo_->getHasChildProcs() || !childProcsSent_)
   {
      procInfo_->setHasChildProcs(hasNonIgnoredSubprocs);

      json::Object subProcs;
      subProcs["handle"] = handle();
      subProcs["subprocs"] = procInfo_->getHasChildProcs();
      module_context::enqueClientEvent(
            ClientEvent(client_events::kTerminalSubprocs, subProcs));
      childProcsSent_ = true;
   }
}

void ConsoleProcess::reportCwd(const core::FilePath& cwd)
{
   if (procInfo_->getCwd() != cwd && cwd.exists())
   {
      FilePath resolvedCwd = cwd.resolveSymlink();
      procInfo_->setCwd(resolvedCwd);

      json::Object termCwd;
      termCwd["handle"] = handle();
      termCwd["cwd"] = module_context::createAliasedPath(resolvedCwd);
      module_context::enqueClientEvent(
            ClientEvent(client_events::kTerminalCwd, termCwd));
      childProcsSent_ = true;

      saveConsoleProcesses();
   }
}

std::string ConsoleProcess::getChannelMode() const
{
   switch(procInfo_->getChannelMode())
   {
   case Rpc:
      return "rpc";
   case Websocket:
      return "websocket";
   }
   return "unknown";
}

void ConsoleProcess::setRpcMode()
{
   s_terminalSocket.stopListening(handle());
   procInfo_->setChannelMode(Rpc, "");
}

void ConsoleProcess::setZombie()
{
   procInfo_->setZombie(true);
   procInfo_->setHasChildProcs(false);
   saveConsoleProcesses();
}

core::json::Object ConsoleProcess::toJson(SerializationMode serialMode) const
{
   return procInfo_->toJson(serialMode);
}

ConsoleProcessPtr ConsoleProcess::fromJson(const core::json::Object &obj)
{
   boost::shared_ptr<ConsoleProcessInfo> pProcInfo(ConsoleProcessInfo::fromJson(obj));
   ConsoleProcessPtr pProc(new ConsoleProcess(pProcInfo));
   return pProc;
}

core::system::ProcessCallbacks ConsoleProcess::createProcessCallbacks()
{
   core::system::ProcessCallbacks cb;
   cb.onContinue = boost::bind(&ConsoleProcess::onContinue, ConsoleProcess::shared_from_this(), _1);
   cb.onStdout = boost::bind(&ConsoleProcess::onStdout, ConsoleProcess::shared_from_this(), _1, _2);
   cb.onExit = boost::bind(&ConsoleProcess::onExit, ConsoleProcess::shared_from_this(), _1);
   if (options_.reportHasSubprocs)
   {
      cb.onHasSubprocs = boost::bind(&ConsoleProcess::onHasSubprocs, ConsoleProcess::shared_from_this(), _1, _2);
   }
   if (options_.trackCwd)
   {
      cb.reportCwd = boost::bind(&ConsoleProcess::reportCwd, ConsoleProcess::shared_from_this(), _1);
   }
   return cb;
}

ConsoleProcessPtr ConsoleProcess::create(
      const std::string& command,
      core::system::ProcessOptions options,
      boost::shared_ptr<ConsoleProcessInfo> procInfo)
{
   options.terminateChildren = true;
   ConsoleProcessPtr ptrProc(
         new ConsoleProcess(command, options, procInfo));
   addConsoleProcess(ptrProc);
   saveConsoleProcesses();
   return ptrProc;
}

ConsoleProcessPtr ConsoleProcess::create(
      const std::string& program,
      const std::vector<std::string>& args,
      core::system::ProcessOptions options,
      boost::shared_ptr<ConsoleProcessInfo> procInfo)
{
   options.terminateChildren = true;
   ConsoleProcessPtr ptrProc(
         new ConsoleProcess(program, args, options, procInfo));
   addConsoleProcess(ptrProc);
   saveConsoleProcesses();
   return ptrProc;
}

namespace {

bool augmentTerminalProcessPython(ConsoleProcessPtr cp)
{
   // forward RETICULATE_PYTHON_FALLBACK if set
   std::string reticulatePythonFallback = modules::reticulate::reticulatePython();

   if (!reticulatePythonFallback.empty())
      cp->setenv("RETICULATE_PYTHON_FALLBACK", reticulatePythonFallback);
   
   // forward CONDA_PREFIX if set
   // use custom environment variable name since the user profile
   // might override this to use the 'base' environment by default;
   // our terminal hooks ensure we 'clean up' after whatever the
   // user profile might've done
   std::string condaPrefix = core::system::getenv("CONDA_PREFIX");
   if (!condaPrefix.empty())
      cp->setenv("_RS_CONDA_PREFIX", condaPrefix);

   // return true if we have a configured version of python
   // (indicating that we want terminal hooks to be installed)
   return !reticulatePythonFallback.empty();
}

void useTerminalHooks(ConsoleProcessPtr cp)
{
   // enable terminal hooks for bash
   auto isBashShell = [&]()
   {
      switch (cp->getShellType())
      {

      case TerminalShell::ShellType::PosixBash:
      case TerminalShell::ShellType::GitBash:
      {
         return true;
      }

      case TerminalShell::ShellType::CustomShell:
      {
         FilePath shellPath = cp->getShellPath();
         std::string shellName = shellPath.getFilename();

#ifdef _WIN32
         if (boost::algorithm::ends_with(shellName, "bash.exe"))
            return true;
#endif

         if (boost::algorithm::ends_with(shellName, "bash"))
            return true;

         return false;
      }

      default:
      {
         return false;
      }

      }
   };

   if (isBashShell())
   {
      core::FilePath bashDotDir = session::options().rResourcesPath().completeChildPath("terminal/bash");
      core::FilePath bashProfile = bashDotDir.completeChildPath(".bash_profile");

#ifdef __APPLE__
      if (cp->getShellType() == TerminalShell::ShellType::PosixBash)
      {
         // use --rcfile to set startup scripts when using default bash shell
         auto&& options = cp->getProcessOptions();
         options.args.push_back("--rcfile");
         options.args.push_back(bashProfile.getAbsolutePath());
      }
      else
      {
         // use ENV with other custom shells (presumedly a modern Bash shell)
         const char* env = ::getenv("ENV");
         cp->setenv("_REALENV", env ? env : "<unset>");
         cp->setenv("ENV", bashProfile.getAbsolutePath());
      }
#else
      // set ENV so that our terminal hooks are run
      const char* env = ::getenv("ENV");
      cp->setenv("_REALENV", env ? env : "<unset>");
      cp->setenv("ENV", bashProfile.getAbsolutePath());
#endif
   }
   
   // let terminals know if python integration is enabled
   cp->setenv(
            "_RS_TERMINAL_PYTHON_INTEGRATION_ENABLED_",
            prefs::userPrefs().terminalPythonIntegration() ? "TRUE" : "FALSE");
   
   // make sure terminals see R temporary directory
   // (older versions of R didn't set R_SESSION_TMPDIR; newer ones do)
   cp->setenv("R_SESSION_TMPDIR", module_context::tempDir().getAbsolutePath());

   // set RSTUDIO_TERMINAL_HOOKS (to be sourced on startup by supported shells)
   FilePath hooksPath = session::options().rResourcesPath().completeChildPath("terminal/hooks");
   cp->setenv("RSTUDIO_TERMINAL_HOOKS", hooksPath.getAbsolutePath());
}
   
void augmentTerminalProcess(ConsoleProcessPtr cp)
{
   if (prefs::userPrefs().terminalHooks())
   {
      if (prefs::userPrefs().terminalPythonIntegration())
         augmentTerminalProcessPython(cp);
      
      useTerminalHooks(cp);
   }
}

} // end anonymous namespace

// supports reattaching to a running process, or creating a new process with
// previously used handle
ConsoleProcessPtr ConsoleProcess::createTerminalProcess(
      const std::string& command,
      core::system::ProcessOptions options,
      boost::shared_ptr<ConsoleProcessInfo> procInfo,
      bool enableWebsockets)
{
   ConsoleProcessPtr cp;

   // only true if we create a new process with a previously used handle
   procInfo->setRestarted(false);

   // Use websocket as preferred communication channel; it can fail
   // here if unable to establish the server-side of things, in which case
   // fallback to using Rpc.
   //
   // It can also fail later when client tries to connect; fallback for that
   // happens from the client-side via RPC call procUseRpc.
   if (enableWebsockets)
   {
      Error error = s_terminalSocket.ensureServerRunning();
      if (error)
      {
         procInfo->setChannelMode(Rpc, "");
         LOG_ERROR(error);
      }
      else
      {
         std::string port = safe_convert::numberToString(s_terminalSocket.port());
         procInfo->setChannelMode(Websocket, port);
      }
   }
   else
   {
      procInfo->setChannelMode(Rpc, "");
   }

   if (procInfo->getAllowRestart() && !procInfo->getHandle().empty())
   {
      // return existing ConsoleProcess if it is still running
      ConsoleProcessPtr proc = findProcByHandle(procInfo->getHandle());
      if (proc != nullptr && proc->isStarted())
      {
         cp = proc;
         cp->procInfo_->setRestarted(false);
      }
      else
      {
         // Create new process with previously used handle
         procInfo->setRestarted(true);

         // previous terminal session might have been killed while a full-screen
         // program was running
         procInfo->setAltBufferActive(false);

         if (!procInfo->getZombie())
            procInfo->resetExitCode();

         options.terminateChildren = true;
         cp.reset(new ConsoleProcess(command, options, procInfo));
         augmentTerminalProcess(cp);
         addConsoleProcess(cp);

         // Windows Command Prompt and PowerShell don't support reloading
         // buffers, so delete the buffer before we start the new process.
         if (cp->getShellType() == TerminalShell::ShellType::Cmd32 ||
             cp->getShellType() == TerminalShell::ShellType::Cmd64 ||
             cp->getShellType() == TerminalShell::ShellType::PS32 ||
             cp->getShellType() == TerminalShell::ShellType::PS64 ||
#ifdef _WIN32
             // Ditto for custom shell on Windows.
             cp->getShellType() == TerminalShell::ShellType::CustomShell ||
#endif
             cp->getShellType() == TerminalShell::ShellType::PSCore)
         {
            cp->deleteLogFile();
         }

         saveConsoleProcesses();
      }
   }
   else
   {
      // otherwise create a new one
      cp = create(command, options, procInfo);
      augmentTerminalProcess(cp);
   }

   if (cp->procInfo_->getChannelMode() == Websocket)
   {
      // start watching for websocket callbacks
      s_terminalSocket.listen(cp->procInfo_->getHandle(),
                              cp->createConsoleProcessSocketConnectionCallbacks());
   }
   
   return cp;
}

ConsoleProcessPtr ConsoleProcess::createTerminalProcess(
      core::system::ProcessOptions options,
      boost::shared_ptr<ConsoleProcessInfo> procInfo)
{
   std::string command;
   return createTerminalProcess(command, options, procInfo, useWebsockets());
}

ConsoleProcessPtr ConsoleProcess::createTerminalProcess(
      ConsoleProcessPtr proc)
{
   TerminalShell::ShellType actualShellType;
   core::system::ProcessOptions options = ConsoleProcess::createTerminalProcOptions(
            *proc->procInfo_,
            &actualShellType);
   proc->procInfo_->setShellType(actualShellType);
   return createTerminalProcess(options, proc->procInfo_);
}

ConsoleProcessSocketConnectionCallbacks ConsoleProcess::createConsoleProcessSocketConnectionCallbacks()
{
   ConsoleProcessSocketConnectionCallbacks cb;
   cb.onReceivedInput = boost::bind(&ConsoleProcess::onReceivedInput, ConsoleProcess::shared_from_this(), _1);
   cb.onConnectionOpened = boost::bind(&ConsoleProcess::onConnectionOpened, ConsoleProcess::shared_from_this());
   cb.onConnectionClosed = boost::bind(&ConsoleProcess::onConnectionClosed, ConsoleProcess::shared_from_this());
   return cb;
}

// received input from websocket (e.g. user typing on client), or from
// rstudioapi, may be called on different thread
void ConsoleProcess::onReceivedInput(const std::string& input)
{
   LOCK_MUTEX(inputOutputQueueMutex_)
   {
      enqueInput(Input(input));
      boost::shared_ptr<core::system::ProcessOperations> ops = pOps_.lock();
      if (ops)
      {
         if (!envCaptureCmd_.hasCaptured())
            processQueuedInput(*ops);
      }
   }
   END_LOCK_MUTEX
}

// websocket connection closed; called on different thread
void ConsoleProcess::onConnectionClosed()
{
   s_terminalSocket.stopListening(handle());
}

// websocket connection opened; called on different thread
void ConsoleProcess::onConnectionOpened()
{
}

void ConsoleProcess::saveEnvironment(const std::string& env)
{
   if (env.empty())
      return;

   std::string normalized = env;
   string_utils::convertLineEndings(&normalized, string_utils::LineEndingNative);

   core::system::Options environment;
   std::istringstream iss(normalized);
   for (std::string line; std::getline(iss, line); )
   {
      size_t equalSign = line.find_first_of('=');
      if (equalSign == std::string::npos)
         return;

      std::string name = line.substr(0, equalSign);
      if (isIgnoredEnvironmentVariable(name))
         continue;
      
      std::string value = line.substr(equalSign + 1);
      core::system::setenv(&environment, name, value);
   }
   
   if (environment.empty())
      return;

   procInfo_->saveConsoleEnvironment(environment);
}

void ConsoleProcess::loadEnvironment(const std::string& handle, core::system::Options* pEnv)
{
   ConsoleProcessInfo::loadConsoleEnvironment(handle, pEnv);
}

FilePath ConsoleProcess::getShellPath() const
{
   return options_.shellPath;
}

std::string ConsoleProcess::getShellName() const
{
   return TerminalShell::getShellName(procInfo_->getShellType());
}

bool ConsoleProcess::useWebsockets()
{
   return session::options().allowTerminalWebsockets() &&
                     prefs::userPrefs().terminalWebsockets();
}

core::json::Array processesAsJson(SerializationMode serialMode)
{
   return allProcessesAsJson(serialMode);
}

Error initialize()
{
   initializeIgnoredEnvironmentVariables();
   prefs::userPrefs().onChanged.connect(onUserPrefsChanged);
   return internalInitialize();
}

} // namespace console_process
} // namespace session
} // namespace rstudio
