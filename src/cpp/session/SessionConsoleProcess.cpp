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
#include <boost/foreach.hpp>
#include <boost/range/adaptor/map.hpp>

#include <core/Algorithm.hpp>
#include <core/text/AnsiCodeParser.hpp>
#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

#include <r/RRoutines.hpp>

#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace console_process {

namespace {

typedef boost::shared_ptr<ConsoleProcess> ConsoleProcessPtr;
typedef std::map<std::string, ConsoleProcessPtr> ProcTable;

ProcTable s_procs;
ConsoleProcessSocket s_terminalSocket;

ConsoleProcessPtr findProcByHandle(const std::string& handle)
{
   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos != s_procs.end())
      return pos->second;
   else
      return ConsoleProcessPtr();
}

ConsoleProcessPtr findProcByCaption(const std::string& caption)
{
   BOOST_FOREACH(ConsoleProcessPtr& proc, s_procs | boost::adaptors::map_values)
   {
      if (proc->getCaption() == caption)
         return proc;
   }
   return ConsoleProcessPtr();
}

// Determine next terminal sequence, used when creating terminal name
// via rstudioapi: mimics what happens in client code.
std::string nextTerminalName()
{
   int maxNum = kNoTerminal;
   BOOST_FOREACH(ConsoleProcessPtr& proc, s_procs | boost::adaptors::map_values)
   {
      maxNum = std::max(maxNum, proc->getTerminalSequence());
   }
   maxNum++;

   return std::string("Terminal ") + safe_convert::numberToString(maxNum);
}

// Return vector of all terminal ids (captions)
SEXP rs_getAllTerminals()
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   std::vector<std::string> allCaptions;
   for (ProcTable::const_iterator it = s_procs.begin(); it != s_procs.end(); it++)
   {
      allCaptions.push_back(it->second->getCaption());
   }

   return r::sexp::create(allCaptions, &protect);
}

// Create a terminal with given id (caption). If null, create with automatically
// generated name. Returns resulting name in either case.
SEXP rs_createNamedTerminal(SEXP typeSEXP)
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   std::string terminalId = r::sexp::asString(typeSEXP);
   if (terminalId.empty())
   {
      terminalId = nextTerminalName();
   }

   json::Object eventData;
   eventData["id"] = terminalId;

   // send the event
   ClientEvent createNamedTerminalEvent(client_events::kCreateNamedTerminal, eventData);
   module_context::enqueClientEvent(createNamedTerminalEvent);

   return r::sexp::create(terminalId, &protect);
}

// Returns busy state of a terminal (i.e. does the shell have any child
// processes?)
SEXP rs_isTerminalBusy(SEXP terminalsSEXP)
{
   r::sexp::Protect protect;

   std::vector<std::string> terminalIds;
   if (!r::sexp::fillVectorString(terminalsSEXP, &terminalIds))
      return R_NilValue;

   std::vector<bool> isBusy;
   BOOST_FOREACH(const std::string& terminalId, terminalIds)
   {
      ConsoleProcessPtr proc = findProcByCaption(terminalId);
      if (proc == NULL)
      {
         isBusy.push_back(false);
         continue;
      }
      isBusy.push_back(proc->getIsBusy());
   }
   return r::sexp::create(isBusy, &protect);
}

// Returns bunch of metadata about terminal instance(s).
SEXP rs_getTerminalContext(SEXP terminalsSEXP)
{
   r::sexp::Protect protect;

   std::vector<std::string> terminalIds;
   if (!r::sexp::fillVectorString(terminalsSEXP, &terminalIds))
      return R_NilValue;

   r::sexp::ListBuilder outerBuilder(&protect);
   BOOST_FOREACH(const std::string& terminalId, terminalIds)
   {
      ConsoleProcessPtr proc = findProcByCaption(terminalId);
      if (proc == NULL)
      {
         continue;
      }

      r::sexp::ListBuilder builder(&protect);
      builder.add("handle", proc->handle());
      builder.add("caption", proc->getCaption());
      builder.add("title", proc->getTitle());
      builder.add("running", proc->isStarted());
      builder.add("busy", proc->getIsBusy());
      builder.add("connection", proc->getChannelMode());
      builder.add("sequence", proc->getTerminalSequence());
      builder.add("lines", proc->getBufferLineCount());
      builder.add("cols", proc->getCols());
      builder.add("rows", proc->getRows());
      builder.add("pid", proc->getPid());

      outerBuilder.add(proc->getCaption(), builder);
   }

   return r::sexp::create(outerBuilder, &protect);
}

// Ensure terminal is running (has started its process); a terminal can be
// unstarted if the session has previously been suspended and restarted,
// but user hasn't visited the terminal in the client.
SEXP rs_ensureTerminalRunning(SEXP idSEXP)
{
   r::sexp::Protect protect;

   std::string terminalId = r::sexp::asString(idSEXP);
   ConsoleProcessPtr proc = findProcByCaption(terminalId);
   if (proc == NULL)
      return R_NilValue;

   proc->start();
   return R_NilValue;
}

// Return buffer for a terminal, optionally stripping out Ansi codes.
SEXP rs_getTerminalBuffer(SEXP idSEXP, SEXP stripSEXP)
{
   r::sexp::Protect protect;

   std::string terminalId = r::sexp::asString(idSEXP);
   bool stripAnsi = r::sexp::asLogical(stripSEXP);

   ConsoleProcessPtr proc = findProcByCaption(terminalId);
   if (proc == NULL)
      return R_NilValue;

   std::string buffer = proc->getBuffer();

   if (stripAnsi)
      core::text::stripAnsiCodes(&buffer);
   string_utils::convertLineEndings(&buffer, string_utils::LineEndingPosix);
   return r::sexp::create(core::algorithm::split(buffer, "\n"), &protect);
}

// Kill terminal and its processes.
SEXP rs_killTerminal(SEXP terminalsSEXP)
{
   r::sexp::Protect protect;

   std::vector<std::string> terminalIds;
   if (!r::sexp::fillVectorString(terminalsSEXP, &terminalIds))
      return R_NilValue;

   BOOST_FOREACH(const std::string& terminalId, terminalIds)
   {
      ConsoleProcessPtr proc = findProcByCaption(terminalId);
      if (proc != NULL)
      {
         proc->interrupt();
      }
   }
   return R_NilValue;
}

} // anonymous namespace

void saveConsoleProcesses();

ConsoleProcess::ConsoleProcess(boost::shared_ptr<ConsoleProcessInfo> procInfo)
   : procInfo_(procInfo), interrupt_(false), newCols_(-1), newRows_(-1),
     cols_(-1), rows_(-1), pid_(-1), childProcsSent_(false),
     lastInputSequence_(kIgnoreSequence), started_(false)
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
     interrupt_(false), newCols_(-1), newRows_(-1), cols_(-1), rows_(-1), pid_(-1),
     childProcsSent_(false), lastInputSequence_(kIgnoreSequence), started_(false)
{
   commonInit();
}

ConsoleProcess::ConsoleProcess(const std::string& program,
                               const std::vector<std::string>& args,
                               const core::system::ProcessOptions& options,
                               boost::shared_ptr<ConsoleProcessInfo> procInfo)
   : program_(program), args_(args), options_(options), procInfo_(procInfo),
     interrupt_(false), newCols_(-1), newRows_(-1), cols_(-1), rows_(-1), pid_(-1),
     childProcsSent_(false),
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
      rows_ = options_.rows;
      cols_ = options_.cols;
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
      rows_ = newRows_;
      cols_ = newCols_;
      newCols_ = -1;
      newRows_ = -1;
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

std::string ConsoleProcess::getBuffer() const
{
   return procInfo_->getFullSavedBuffer();
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
   saveConsoleProcesses();

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

std::string ConsoleProcess::getChannelMode() const
{
   switch(procInfo_->getChannelMode())
   {
   case Rpc:
      return "rpc";
   case Websocket:
      return "websocket";
   default:
      return "unknown";
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
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != NULL)
   {
      return proc->start();
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
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != NULL)
   {
      proc->interrupt();
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

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != NULL)
   {
      proc->deleteLogFile();
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

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != NULL)
   {
      proc->enqueInput(input);
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
   
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != NULL)
   {
      proc->resize(cols, rows);
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
   
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
   {
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   }
   
   // make sure we don't have this name already
   if (findProcByCaption(caption) != NULL)
   {
      pResponse->setResult(false /*duplicate name*/);
      return Success();
   }

   proc->setCaption(caption);
   saveConsoleProcesses();
   pResponse->setResult(true /*successful*/);
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
   
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
   {
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   }
   
   proc->setTitle(title);
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

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
   {
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   }

   proc->deleteLogFile();
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

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   json::Object result;
   bool moreAvailable;
   std::string chunkContent = proc->getSavedBufferChunk(requestedChunk, &moreAvailable);

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

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   // Used to downgrade to Rpc after client was unable to connect to Websocket
   proc->setRpcMode();
   return Success();
}

// Determine if a given process handle exists in the table; used by client
// to detect stale consoleprocs.
Error procTestExists(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string handle;

   Error error = json::readParams(request.params,
                                  &handle);
   if (error)
      return error;

   bool exists = (findProcByHandle(handle) == NULL) ? false : true;
   pResponse->setResult(exists);
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
      ConsoleProcessPtr proc = findProcByHandle(procInfo->getHandle());
      if (proc != NULL && proc->isStarted())
      {
         // Jiggle the size of the pseudo-terminal, this will force the app
         // to refresh itself; this does rely on the host performing a second
         // resize to the actual available size. Clumsy, but so far this is
         // the best I've come up with.
         cp = proc;
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
   return findProcByHandle(handle) != NULL;
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

   // When shutting down, only preserve ConsoleProcesses that are marked
   // with allow_restart. Others should not survive a shutdown/restart.
   ProcTable::const_iterator nextIt = s_procs.begin();
   for (ProcTable::const_iterator it = s_procs.begin();
        it != s_procs.end();
        it = nextIt)
   {
      nextIt = it;
      ++nextIt;
      if (it->second->getAllowRestart() == false)
      {
         s_procs.erase(it->second->handle());
      }
   }

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

   RS_REGISTER_CALL_METHOD(rs_getAllTerminals, 0);
   RS_REGISTER_CALL_METHOD(rs_createNamedTerminal, 1);
   RS_REGISTER_CALL_METHOD(rs_isTerminalBusy, 1);
   RS_REGISTER_CALL_METHOD(rs_getTerminalContext, 1);
   RS_REGISTER_CALL_METHOD(rs_ensureTerminalRunning, 1);
   RS_REGISTER_CALL_METHOD(rs_getTerminalBuffer, 2);
   RS_REGISTER_CALL_METHOD(rs_killTerminal, 1);

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
      (bind(registerRpcMethod, "process_test_exists", procTestExists))
      (bind(registerRpcMethod, "process_use_rpc", procUseRpc));

   return initBlock.execute();
}

} // namespace console_process
} // namespace session
} // namespace rstudio
