/*
 * SessionConsoleProcess.cpp
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

#include <session/SessionConsoleProcess.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace console_process {

namespace {
   typedef std::map<std::string, boost::shared_ptr<ConsoleProcess> > ProcTable;
   ProcTable s_procs;
   ConsoleProcessSocket s_terminalSocket;
} // anonymous namespace

void saveConsoleProcesses();

ConsoleProcess::ConsoleProcess(boost::shared_ptr<ConsoleProcessInfo> procInfo)
   : procInfo_(procInfo), interrupt_(false), newCols_(-1), newRows_(-1),
     childProcsSent_(false), lastInputSequence_(kIgnoreSequence), started_(false)
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
     interrupt_(false), newCols_(-1), newRows_(-1), childProcsSent_(false),
     lastInputSequence_(kIgnoreSequence), started_(false)
{
   commonInit();
}

ConsoleProcess::ConsoleProcess(const std::string& program,
                               const std::vector<std::string>& args,
                               const core::system::ProcessOptions& options,
                               boost::shared_ptr<ConsoleProcessInfo> procInfo)
   : program_(program), args_(args), options_(options), procInfo_(procInfo),
     interrupt_(false), newCols_(-1), newRows_(-1), childProcsSent_(false),
     lastInputSequence_(kIgnoreSequence), started_(false)
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

   if (interactionMode() != InteractionNever)
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
         program_ = consoleIoPath.absolutePathNative();
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
         // undefine TERM, as it puts git-bash in a mode that winpty doesn't
         // support; was set in SessionMain.cpp::main to support color in
         // the R Console
         if (!options_.environment)
         {
            core::system::Options childEnv;
            core::system::environment(&childEnv);
            options_.environment = childEnv;
         }
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

      // define TERM (but first make sure we have an environment
      // block to modify)
      if (!options_.environment)
      {
         core::system::Options childEnv;
         core::system::environment(&childEnv);
         options_.environment = childEnv;
      }
      
      core::system::setenv(&(options_.environment.get()), "TERM",
                           options_.smartTerminal ? core::system::kSmartTerm :
                                                    core::system::kDumbTerm);
#endif
   }


   // When we retrieve from outputBuffer, we only want complete lines. Add a
   // dummy \n so we can tell the first line is a complete line.
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
   if (started_)
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
      for (std::deque<Input>::iterator it = inputQueue_.begin();
           it != inputQueue_.end(); it++)
      {
         (*it).sequence = kIgnoreSequence;
      }
      lastInputSequence_ = kIgnoreSequence;
      return;
   }

   // insert in order by sequence
   for (std::deque<Input>::iterator it = inputQueue_.begin();
        it != inputQueue_.end(); it++)
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
      for (std::deque<Input>::iterator it = inputQueue_.begin();
           it != inputQueue_.end(); it++)
      {
         lastInputSequence_ = (*it).sequence;
         (*it).sequence = kIgnoreSequence;
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
   interrupt_ = true;
}

void ConsoleProcess::resize(int cols, int rows)
{
   newCols_ = cols;
   newRows_ = rows;
}

bool ConsoleProcess::onContinue(core::system::ProcessOperations& ops)
{
   // full stop interrupt if requested
   if (interrupt_)
      return false;


   if (procInfo_->getChannelMode() == Rpc)
   {
      processQueuedInput(ops);
   }
   else
   {
      // capture weak reference to the callbacks so websocket callback
      // can use them
      LOCK_MUTEX(mutex_)
      {
         pOps_ = ops.weak_from_this();
      }
      END_LOCK_MUTEX
   }

   if (newCols_ != -1 && newRows_ != -1)
   {
      ops.ptySetSize(newCols_, newRows_);
      newCols_ = -1;
      newRows_ = -1;
   }
   
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

void ConsoleProcess::deleteLogFile() const
{
   procInfo_->deleteLogFile();
}

std::string ConsoleProcess::getSavedBufferChunk(int chunk, bool* pMoreAvailable) const
{
   return procInfo_->getSavedBufferChunk(chunk, pMoreAvailable);
}

void ConsoleProcess::enqueOutputEvent(const std::string &output)
{
   // copy to output buffer
   procInfo_->appendToOutputBuffer(output);

   // If there's more output than the client can even show, then
   // truncate it to the amount that the client can show. Too much
   // output can overwhelm the client, making it unresponsive.
   std::string trimmedOutput = output;
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
      enqueOutputEvent(output);
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
   // if there is a custom prmopt handler then give it a chance to
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

   json::Object data;
   data["handle"] = handle();
   data["exitCode"] = exitCode;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessExit, data));

   onExit_(exitCode);
}

void ConsoleProcess::onHasSubprocs(bool hasSubprocs)
{
   if (hasSubprocs != procInfo_->getHasChildProcs() || !childProcsSent_)
   {
      procInfo_->setHasChildProcs(hasSubprocs);

      json::Object subProcs;
      subProcs["handle"] = handle();
      subProcs["subprocs"] = procInfo_->getHasChildProcs();
      module_context::enqueClientEvent(
            ClientEvent(client_events::kTerminalSubprocs, subProcs));
      childProcsSent_ = true;
   }
}

void ConsoleProcess::setRpcMode()
{
   s_terminalSocket.stopListening(handle());
   procInfo_->setChannelMode(Rpc, "");
}

core::json::Object ConsoleProcess::toJson() const
{
   return procInfo_->toJson();
}

boost::shared_ptr<ConsoleProcess> ConsoleProcess::fromJson(
                                             core::json::Object &obj)
{
   boost::shared_ptr<ConsoleProcessInfo> pProcInfo(ConsoleProcessInfo::fromJson(obj));
   boost::shared_ptr<ConsoleProcess> pProc(new ConsoleProcess(pProcInfo));
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
      cb.onHasSubprocs = boost::bind(&ConsoleProcess::onHasSubprocs, ConsoleProcess::shared_from_this(), _1);
   }
   return cb;
}

Error procStart(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;
   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos != s_procs.end())
   {
      return pos->second->start();
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         ERROR_LOCATION);
   }
}

Error procInterrupt(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;
   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos != s_procs.end())
   {
      pos->second->interrupt();
      return Success();
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         ERROR_LOCATION);
   }
}

Error procReap(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;

   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos != s_procs.end())
   {
      pos->second->deleteLogFile();
      if (s_procs.erase(handle))
      {
         saveConsoleProcesses();
         return Success();
      }
   }
   
   return systemError(boost::system::errc::invalid_argument,
                      ERROR_LOCATION);
}

Error procWriteStdin(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParam(request.params, 0, &handle);
   if (error)
      return error;

   ConsoleProcess::Input input;
   error = json::readObjectParam(request.params, 1,
                                 "sequence", &input.sequence,
                                 "interrupt", &input.interrupt,
                                 "text", &input.text,
                                 "echo_input", &input.echoInput);
   if (error)
      return error;

   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos != s_procs.end())
   {
      pos->second->enqueInput(input);
      return Success();
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         ERROR_LOCATION);
   }
}

Error procSetSize(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   std::string handle;
   int cols, rows;
   Error error = json::readParams(request.params,
                                  &handle,
                                  &cols,
                                  &rows);
   if (error)
      return error;
   
   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos != s_procs.end())
   {
      pos->second->resize(cols, rows);
      return Success();

   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         ERROR_LOCATION);
   }
}

Error procSetCaption(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string handle;
   std::string caption;
   
   Error error = json::readParams(request.params,
                                  &handle,
                                  &caption);
   if (error)
      return error;
   
   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos == s_procs.end())
   {
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   }
   
   pos->second->setCaption(caption);
   saveConsoleProcesses();
   return Success();
}

Error procSetTitle(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string handle;
   std::string title;
   
   Error error = json::readParams(request.params,
                                  &handle,
                                  &title);
   if (error)
      return error;
   
   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos == s_procs.end())
   {
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   }
   
   pos->second->setTitle(title);
   return Success();
}

Error procEraseBuffer(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string handle;

   Error error = json::readParams(request.params,
                                  &handle);
   if (error)
      return error;

   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos == s_procs.end())
   {
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   }

   pos->second->deleteLogFile();
   return Success();
}

Error procGetBufferChunk(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string handle;
   int requestedChunk;

   Error error = json::readParams(request.params,
                                  &handle,
                                  &requestedChunk);
   if (error)
      return error;
   if (requestedChunk < 0)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos == s_procs.end())
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   json::Object result;
   bool moreAvailable;
   std::string chunkContent = pos->second->getSavedBufferChunk(
            requestedChunk, &moreAvailable);

   result["chunk"] = chunkContent;
   result["chunk_number"] = requestedChunk;
   result["more_available"] = moreAvailable;
   pResponse->setResult(result);

   return Success();
}

Error procUseRpc(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   std::string handle;

   Error error = json::readParams(request.params,
                                  &handle);
   if (error)
      return error;

   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos == s_procs.end())
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   // Used to downgrade to Rpc after client was unable to connect to Websocket
   pos->second->setRpcMode();
   return Success();
}

boost::shared_ptr<ConsoleProcess> ConsoleProcess::create(
      const std::string& command,
      core::system::ProcessOptions options,
      boost::shared_ptr<ConsoleProcessInfo> procInfo)
{
   options.terminateChildren = true;
   boost::shared_ptr<ConsoleProcess> ptrProc(
         new ConsoleProcess(command, options, procInfo));
   s_procs[ptrProc->handle()] = ptrProc;
   saveConsoleProcesses();
   return ptrProc;
}

boost::shared_ptr<ConsoleProcess> ConsoleProcess::create(
      const std::string& program,
      const std::vector<std::string>& args,
      core::system::ProcessOptions options,
      boost::shared_ptr<ConsoleProcessInfo> procInfo)
{
   options.terminateChildren = true;
   boost::shared_ptr<ConsoleProcess> ptrProc(
         new ConsoleProcess(program, args, options, procInfo));
   s_procs[ptrProc->handle()] = ptrProc;
   saveConsoleProcesses();
   return ptrProc;
}

// supports reattaching to a running process, or creating a new process with
// previously used handle
boost::shared_ptr<ConsoleProcess> ConsoleProcess::createTerminalProcess(
      core::system::ProcessOptions options,
      boost::shared_ptr<ConsoleProcessInfo> procInfo,
      bool enableWebsockets)
{
   boost::shared_ptr<ConsoleProcess> cp;

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

   std::string command;
   if (procInfo->getAllowRestart() && !procInfo->getHandle().empty())
   {
      // return existing ConsoleProcess if it is still running
      ProcTable::const_iterator pos = s_procs.find(procInfo->getHandle());
      if (pos != s_procs.end() && pos->second->isStarted())
      {
         // Jiggle the size of the pseudo-terminal, this will force the app
         // to refresh itself; this does rely on the host performing a second
         // resize to the actual available size. Clumsy, but so far this is
         // the best I've come up with.
         cp = pos->second;
         cp->resize(25, 5);
      }
      else
      {
         // Create new process with previously used handle
         options.terminateChildren = true;
         cp.reset(new ConsoleProcess(command, options, procInfo));
         s_procs[cp->handle()] = cp;
         saveConsoleProcesses();
      }
   }
   else
   {
      // otherwise create a new one
      cp =  create(command, options, procInfo);
   }

   if (cp->procInfo_->getChannelMode() == Websocket)
   {
      // start watching for websocket callbacks
      s_terminalSocket.listen(cp->procInfo_->getHandle(),
                              cp->createConsoleProcessSocketConnectionCallbacks());
   }
   return cp;
}

ConsoleProcessSocketConnectionCallbacks ConsoleProcess::createConsoleProcessSocketConnectionCallbacks()
{
   ConsoleProcessSocketConnectionCallbacks cb;
   cb.onReceivedInput = boost::bind(&ConsoleProcess::onReceivedInput, ConsoleProcess::shared_from_this(), _1);
   cb.onConnectionOpened = boost::bind(&ConsoleProcess::onConnectionOpened, ConsoleProcess::shared_from_this());
   cb.onConnectionClosed = boost::bind(&ConsoleProcess::onConnectionClosed, ConsoleProcess::shared_from_this());
   return cb;
}

// received input from websocket (e.g. user typing on client); called on
// different thread
void ConsoleProcess::onReceivedInput(const std::string& input)
{
   LOCK_MUTEX(mutex_)
   {
      enqueInput(Input(input));
      boost::shared_ptr<core::system::ProcessOperations> ops = pOps_.lock();
      if (ops)
      {
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

void PasswordManager::attach(
                  boost::shared_ptr<console_process::ConsoleProcess> pCP,
                  bool showRememberOption)
{
   pCP->setPromptHandler(boost::bind(&PasswordManager::handlePrompt,
                                       this,
                                       pCP->handle(),
                                       _1,
                                       showRememberOption,
                                       _2));

   pCP->onExit().connect(boost::bind(&PasswordManager::onExit,
                                       this,
                                       pCP->handle(),
                                       _1));
}

bool PasswordManager::handlePrompt(const std::string& cpHandle,
                                   const std::string& prompt,
                                   bool showRememberOption,
                                   ConsoleProcess::Input* pInput)
{
   // is this a password prompt?
   boost::smatch match;
   if (regex_utils::match(prompt, match, promptPattern_))
   {
      // see if it matches any of our existing cached passwords
      std::vector<CachedPassword>::const_iterator it =
                  std::find_if(passwords_.begin(),
                               passwords_.end(),
                               boost::bind(&hasPrompt, _1, prompt));
      if (it != passwords_.end())
      {
         // cached password
         *pInput = ConsoleProcess::Input(it->password + "\n", false);
      }
      else
      {
         // prompt for password
         std::string password;
         bool remember;
         if (promptHandler_(prompt, showRememberOption, &password, &remember))
         {

            // cache the password (but also set the remember flag so it
            // will be removed from the cache when the console process
            // exits if the user chose not to remember).
            CachedPassword cachedPassword;
            cachedPassword.cpHandle = cpHandle;
            cachedPassword.prompt = prompt;
            cachedPassword.password = password;
            cachedPassword.remember = remember;
            passwords_.push_back(cachedPassword);

            // interactively entered password
            *pInput = ConsoleProcess::Input(password + "\n", false);
         }
         else
         {
            // user cancelled
            *pInput = ConsoleProcess::Input();
         }
      }

      return true;
   }
   // not a password prompt so ignore
   else
   {
      return false;
   }
}

void PasswordManager::onExit(const std::string& cpHandle,
                             int exitCode)
{
   // if a process exits with an error then remove any cached
   // passwords which originated from that process
   if (exitCode != EXIT_SUCCESS)
   {
      passwords_.erase(std::remove_if(passwords_.begin(),
                                      passwords_.end(),
                                      boost::bind(&hasHandle, _1, cpHandle)),
                       passwords_.end());
   }

   // otherwise remove any cached password for this process which doesn't
   // have its remember flag set
   else
   {
      passwords_.erase(std::remove_if(passwords_.begin(),
                                      passwords_.end(),
                                      boost::bind(&forgetOnExit, _1, cpHandle)),
                       passwords_.end());
   }
}


bool PasswordManager::hasPrompt(const CachedPassword& cachedPassword,
                                const std::string& prompt)
{
   return cachedPassword.prompt == prompt;
}

bool PasswordManager::hasHandle(const CachedPassword& cachedPassword,
                                const std::string& cpHandle)
{
   return cachedPassword.cpHandle == cpHandle;
}

bool PasswordManager::forgetOnExit(const CachedPassword& cachedPassword,
                                   const std::string& cpHandle)
{
   return hasHandle(cachedPassword, cpHandle) && !cachedPassword.remember;
}

core::json::Array processesAsJson()
{
   json::Array procInfos;
   for (ProcTable::const_iterator it = s_procs.begin();
        it != s_procs.end();
        it++)
   {
      procInfos.push_back(it->second->toJson());
   }
   return procInfos;
}

std::string serializeConsoleProcs()
{
   json::Array array;
   for (ProcTable::const_iterator it = s_procs.begin();
        it != s_procs.end();
        it++)
   {
      array.push_back(it->second->toJson());
   }

   std::ostringstream ostr;
   json::write(array, ostr);
   return ostr.str();
}
   
void deserializeConsoleProcs(const std::string& jsonStr)
{
   if (jsonStr.empty())
      return;
   json::Value value;
   if (!json::parse(jsonStr, &value))
   {
      LOG_WARNING_MESSAGE("invalid console process json: " + jsonStr);
      return;
   }

   json::Array procs = value.get_array();
   for (json::Array::iterator it = procs.begin();
        it != procs.end();
        it++)
   {
      boost::shared_ptr<ConsoleProcess> proc =
                                    ConsoleProcess::fromJson(it->get_obj());

      // Deserializing consoleprocs list only happens during session
      // initialization, therefore they do not represent an actual running
      // async process, therefore are not busy. Mark as such, otherwise we
      // can get false "busy" indications on the client after a restart, for
      // example if a session was closed with busy terminal(s), then
      // restarted. This is not hit if reconnecting to a still-running
      // session.
      proc->setNotBusy();

      s_procs[proc->handle()] = proc;
   }
}

bool isKnownProcHandle(const std::string& handle)
{
   ProcTable::const_iterator pos = s_procs.find(handle);
   return pos != s_procs.end();
}

void loadConsoleProcesses()
{
   std::string contents = ConsoleProcessInfo::loadConsoleProcessMetadata();
   if (contents.empty())
      return;
   deserializeConsoleProcs(contents);
   ConsoleProcessInfo::deleteOrphanedLogs(isKnownProcHandle);
}

void saveConsoleProcessesAtShutdown(bool terminatedNormally)
{
   if (!terminatedNormally)
      return;
   saveConsoleProcesses();
}

void saveConsoleProcesses()
{
   ConsoleProcessInfo::saveConsoleProcesses(serializeConsoleProcs());
}
   
void onSuspend(core::Settings* /*pSettings*/)
{
   serializeConsoleProcs();
}

void onResume(const core::Settings& /*settings*/)
{
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   events().onShutdown.connect(saveConsoleProcessesAtShutdown);
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));

   loadConsoleProcesses();

   // install rpc methods
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "process_start", procStart))
      (bind(registerRpcMethod, "process_interrupt", procInterrupt))
      (bind(registerRpcMethod, "process_reap", procReap))
      (bind(registerRpcMethod, "process_write_stdin", procWriteStdin))
      (bind(registerRpcMethod, "process_set_size", procSetSize))
      (bind(registerRpcMethod, "process_set_caption", procSetCaption))
      (bind(registerRpcMethod, "process_set_title", procSetTitle))
      (bind(registerRpcMethod, "process_erase_buffer", procEraseBuffer))
      (bind(registerRpcMethod, "process_get_buffer_chunk", procGetBufferChunk))
      (bind(registerRpcMethod, "process_use_rpc", procUseRpc));

   return initBlock.execute();
}

} // namespace console_process
} // namespace session
} // namespace rstudio
