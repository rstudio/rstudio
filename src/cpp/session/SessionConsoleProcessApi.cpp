/*
 * SessionConsoleProcessApi.cpp
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

#include "SessionConsoleProcessApi.hpp"

#include <gsl/gsl>

#include <boost/algorithm/string/replace.hpp>

#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/text/AnsiCodeParser.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>

#include <r/RRoutines.hpp>
#include <r/RExec.hpp>

#include "modules/SessionWorkbench.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace console_process {

namespace {

// findProcByHandle that reports an R error for unknown terminal handle
ConsoleProcessPtr findProcByHandleReportUnknown(const std::string& handle)
{
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == nullptr)
   {
      std::string error("Unknown terminal identifier '");
      error += handle;
      error += "'";
      r::exec::error(error);
   }
   return proc;
}

// Return buffer for a terminal, optionally stripping out Ansi codes.
std::string getTerminalBuffer(const ConsoleProcessPtr& proc, bool stripAnsi)
{
   std::string buffer = proc->getBuffer();

   if (stripAnsi)
   {
      core::text::stripAnsiCodes(&buffer);

      // remove <BEL> characters
      boost::algorithm::replace_all(buffer, "\x07", "");

      // process backspaces
      std::string::iterator iter = buffer.begin();
      std::string::iterator end = buffer.end();
      while (iter != end)
      {
         iter = std::find(iter, end, '\b');
         if (iter == end) break;
         if (iter == buffer.begin())
            iter = buffer.erase(iter);
         else
            iter = buffer.erase(iter-1, iter+1);
         end = buffer.end();
      }
   }
   string_utils::convertLineEndings(&buffer, string_utils::LineEndingPosix);
   return buffer;
}


// R APIs ---------------------------------------------------------------

// Return vector of all terminal ids (handles)
SEXP rs_terminalList()
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   return r::sexp::create(getAllHandles(), &protect);
}

// Create a terminal with given caption. If null, create with automatically
// generated name. Optionally specify shell type.
SEXP rs_terminalCreate(SEXP captionSEXP, SEXP showSEXP, SEXP shellTypeSEXP)
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   std::pair<int, std::string> termSequence = nextTerminalName();
   std::string terminalCaption;
   if (!r::sexp::isNull(captionSEXP))
      terminalCaption = r::sexp::asString(captionSEXP);
   if (terminalCaption.empty())
   {
      terminalCaption = termSequence.second;
   }
   else if (findProcByCaption(terminalCaption) != nullptr)
   {
      std::string msg = "Terminal caption already in use: '";
      msg += terminalCaption;
      msg += "'";
      r::exec::error(msg);
      return R_NilValue;
   }

   TerminalShell::ShellType shellType = TerminalShell::ShellType::Default;
   std::string terminalTypeStr;
   if (!r::sexp::isNull(shellTypeSEXP))
      terminalTypeStr = r::sexp::asString(shellTypeSEXP);
   if (!terminalTypeStr.empty())
   {
      shellType = TerminalShell::shellTypeFromString(terminalTypeStr);
   }
   bool show = r::sexp::asLogical(showSEXP);

   boost::shared_ptr<ConsoleProcessInfo> pCpi(
            new ConsoleProcessInfo(
               terminalCaption,
               std::string() /*title*/,
               std::string() /*handle*/,
               termSequence.first,
               shellType,
               false /*altBufferActive*/,
               core::FilePath() /*cwd*/,
               core::system::kDefaultCols, core::system::kDefaultRows,
               false /*zombie*/,
               prefs::userPrefs().terminalTrackEnvironment()));

   pCpi->setHasChildProcs(false);

   std::string handle;
   Error error = createTerminalConsoleProc(pCpi, &handle);
   if (error)
   {
      std::string msg = "Failed to create terminal: '";
      msg += error.getSummary();
      msg += "'";
      r::exec::error(msg);
      return R_NilValue;
   }

   ConsoleProcessPtr ptrProc = findProcByHandle(handle);
   if (!ptrProc)
   {
      r::exec::error("Unable to find created terminal");
      return R_NilValue;
   }

   error = ptrProc->start();
   if (error)
   {
      std::string msg = "Failed to start terminal: '";
      msg += error.getSummary();
      msg += "'";

      reapConsoleProcess(*ptrProc);

      r::exec::error(msg);
      return R_NilValue;
   }

   // notify the client so it adds this new terminal to the UI list and starts it
   json::Object eventData;
   eventData["process_info"] = ptrProc->toJson(ClientSerialization);
   eventData["show"] = show;
   ClientEvent addTerminalEvent(client_events::kAddTerminal, eventData);
   module_context::enqueClientEvent(addTerminalEvent);

   return r::sexp::create(handle, &protect);
}

// Returns busy state of a terminal (i.e. does the shell have any child
// processes?)
SEXP rs_terminalBusy(SEXP terminalsSEXP)
{
   r::sexp::Protect protect;

   std::vector<std::string> terminalIds;
   if (!r::sexp::fillVectorString(terminalsSEXP, &terminalIds))
      return R_NilValue;

   std::vector<bool> isBusy;
   for (const std::string& terminalId : terminalIds)
   {
      ConsoleProcessPtr proc = findProcByHandle(terminalId);
      if (proc == nullptr)
      {
         isBusy.push_back(false);
         continue;
      }
      isBusy.push_back(proc->getIsBusy());
   }
   return r::sexp::create(isBusy, &protect);
}

// Returns running state of a terminal (i.e. does the shell have a shell process?)
SEXP rs_terminalRunning(SEXP terminalsSEXP)
{
   r::sexp::Protect protect;

   std::vector<std::string> terminalIds;
   if (!r::sexp::fillVectorString(terminalsSEXP, &terminalIds))
      return R_NilValue;

   std::vector<bool> isRunning;
   for (const std::string& terminalId : terminalIds)
   {
      ConsoleProcessPtr proc = findProcByHandle(terminalId);
      if (proc == nullptr)
      {
         isRunning.push_back(false);
         continue;
      }
      isRunning.push_back(proc->isStarted());
   }
   return r::sexp::create(isRunning, &protect);
}

// Returns bunch of metadata about a terminal instance.
SEXP rs_terminalContext(SEXP terminalSEXP)
{
   r::sexp::Protect protect;

   std::string terminalId = r::sexp::asString(terminalSEXP);

   ConsoleProcessPtr proc = findProcByHandle(terminalId);
   if (proc == nullptr)
   {
      return R_NilValue;
   }

   r::sexp::ListBuilder builder(&protect);
   builder.add("handle", proc->handle());
   builder.add("caption", proc->getCaption());
   builder.add("title", proc->getTitle());
   builder.add("working_dir", module_context::createAliasedPath(proc->getCwd()));
   builder.add("shell", proc->getShellName());
   builder.add("running", proc->isStarted());
   builder.add("busy", proc->getIsBusy());

   if (proc->getExitCode())
      builder.add("exit_code", *proc->getExitCode());
   else
      builder.add("exit_code", R_NilValue);

   builder.add("connection", proc->getChannelMode());
   builder.add("sequence", proc->getTerminalSequence());
   builder.add("lines", proc->getBufferLineCount());
   builder.add("cols", proc->getCols());
   builder.add("rows", proc->getRows());
   builder.add("pid", gsl::narrow_cast<int>(proc->getPid()));
   builder.add("full_screen", proc->getAltBufferActive());
   builder.add("restarted", proc->getWasRestarted());

   return r::sexp::create(builder, &protect);
}

// Return buffer for a terminal, optionally stripping out Ansi codes.
SEXP rs_terminalBuffer(SEXP idSEXP, SEXP stripSEXP)
{
   r::sexp::Protect protect;

   std::string terminalId = r::sexp::asString(idSEXP);
   bool stripAnsi = r::sexp::asLogical(stripSEXP);

   ConsoleProcessPtr proc = findProcByHandleReportUnknown(terminalId);
   if (proc == nullptr)
      return R_NilValue;

   std::string buffer = getTerminalBuffer(proc, stripAnsi);
   return r::sexp::create(core::algorithm::split(buffer, "\n"), &protect);
}

// Kill terminal and its processes.
SEXP rs_terminalKill(SEXP terminalsSEXP)
{
   std::vector<std::string> terminalIds;
   if (!r::sexp::fillVectorString(terminalsSEXP, &terminalIds))
      return R_NilValue;

   std::string handle;
   for (const std::string& terminalId : terminalIds)
   {
      ConsoleProcessPtr proc = findProcByHandle(terminalId);
      if (proc != nullptr)
      {
         handle = proc->handle();
         proc->interrupt();
         reapConsoleProcess(*proc);
      }
   }

   // Notify the client so it removes this terminal from the UI list.
   if (!handle.empty())
   {
      json::Object eventData;
      eventData["handle"] = handle;
      ClientEvent removeTerminalEvent(client_events::kRemoveTerminal, eventData);
      module_context::enqueClientEvent(removeTerminalEvent);
   }

   return R_NilValue;
}

SEXP rs_terminalVisible()
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   ConsoleProcessPtr proc = getVisibleProc();
   if (proc == nullptr)
      return R_NilValue;

   return r::sexp::create(proc->handle(), &protect);
}

SEXP rs_terminalClear(SEXP idSEXP)
{
   std::string terminalId = r::sexp::asString(idSEXP);

   ConsoleProcessPtr proc = findProcByHandleReportUnknown(terminalId);
   if (proc == nullptr)
      return R_NilValue;

   // clear the server-side log directly
   proc->deleteLogFile();

   // send the event to the client; if not connected, it will get the cleared
   // server-side buffer next time it connects.
   json::Object eventData;
   eventData["id"] = proc->getCaption();

   ClientEvent clearNamedTerminalEvent(client_events::kClearTerminal, eventData);
   module_context::enqueClientEvent(clearNamedTerminalEvent);

   return R_NilValue;
}

// Send text to the terminal
SEXP rs_terminalSend(SEXP idSEXP, SEXP textSEXP)
{
   std::string terminalId = r::sexp::asString(idSEXP);
   std::string text = r::sexp::asString(textSEXP);

   ConsoleProcessPtr proc = findProcByHandleReportUnknown(terminalId);
   if (!proc)
      return R_NilValue;

   if (!proc->isStarted())
   {
      r::exec::error("Terminal is not running and cannot accept input");
      return R_NilValue;
   }

   proc->onReceivedInput(text);
   return R_NilValue;
}

// Activate a terminal to ensure it is running (and optionally visible).
SEXP rs_terminalActivate(SEXP idSEXP, SEXP showSEXP)
{
   std::string terminalId;
   if (!r::sexp::isNull(idSEXP))
      terminalId = r::sexp::asString(idSEXP);
   bool show = r::sexp::asLogical(showSEXP);

   if (!session::options().allowShell())
      return R_NilValue;

   std::string caption;
   if (!terminalId.empty())
   {
      ConsoleProcessPtr proc = findProcByHandleReportUnknown(terminalId);
      if (!proc)
         return R_NilValue;

      if (!proc->isStarted())
      {
         // start the process
         proc = proc->createTerminalProcess(proc);
         if (!proc)
         {
            LOG_ERROR_MESSAGE("Unable to create consoleproc for terminal via activateTerminal");
            return R_NilValue;
         }
         Error err = proc->start();
         if (err)
         {
            LOG_ERROR(err);
            reapConsoleProcess(*proc);
            r::exec::error(err.getSummary());
            return R_NilValue;
         }
      }
      caption = proc->getCaption();
   }

   if (show)
   {
      json::Object eventData;
      eventData["id"] = caption;

      ClientEvent activateTerminalEvent(client_events::kActivateTerminal, eventData);
      module_context::enqueClientEvent(activateTerminalEvent);
   }
   return R_NilValue;
}

// Run a process in a terminal
SEXP rs_terminalExecute(SEXP commandSEXP,
                        SEXP dirSEXP,
                        SEXP envSEXP,
                        SEXP showSEXP)
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   std::string command = r::sexp::asString(commandSEXP);
   if (command.empty())
   {
      r::exec::error("No command specified");
      return R_NilValue;
   }

   std::string currentDir;
   if (!r::sexp::isNull(dirSEXP))
      currentDir = r::sexp::asString(dirSEXP);
   if (!currentDir.empty())
   {
      FilePath cwd = module_context::resolveAliasedPath(currentDir);
      if (!cwd.exists() || !cwd.isDirectory())
      {
         std::string message = "Invalid directory: '";
         message += cwd.getAbsolutePathNative();
         message += "'";
         r::exec::error(message);
         return R_NilValue;
      }
   }

   std::vector<std::string> env;
   if (!r::sexp::fillVectorString(envSEXP, &env))
   {
      r::exec::error("Invalid environment");
      return R_NilValue;
   }

   core::system::Options customEnv;
   for (const std::string& str : env)
   {
      core::system::Option envVar;
      if (!core::system::parseEnvVar(str, &envVar))
      {
         std::string msg = "Invalid environment: '";
         msg += str;
         msg += "'";
         r::exec::error(msg);
         return R_NilValue;
      }
      customEnv.push_back(envVar);
   }

   bool show = r::sexp::asLogical(showSEXP);

   std::string handle;
   Error error = createTerminalExecuteConsoleProc(
            command /*title*/,
            command,
            currentDir,
            customEnv,
            &handle);
   if (error)
   {
      std::string msg = "Failed to create terminal for job execution: '";
      msg += error.getSummary();
      msg += "'";
      r::exec::error(msg);
      return R_NilValue;
   }

   ConsoleProcessPtr ptrProc = findProcByHandle(handle);
   if (!ptrProc)
   {
      r::exec::error("Unable to find terminal for job execution");
      return R_NilValue;
   }

   error = ptrProc->start();
   if (error)
   {
      std::string msg = "Failed to start job in terminal: '";
      msg += error.getSummary();
      msg += "'";

      reapConsoleProcess(*ptrProc);

      r::exec::error(msg);
      return R_NilValue;
   }

   // notify the client so it adds this new terminal to the UI list
   json::Object eventData;
   eventData["process_info"] = ptrProc->toJson(ClientSerialization);
   eventData["show"] = show;
   ClientEvent addTerminalEvent(client_events::kAddTerminal, eventData);
   module_context::enqueClientEvent(addTerminalEvent);

   return r::sexp::create(handle, &protect);
}

// Return terminal exit codes
SEXP rs_terminalExitCode(SEXP idSEXP)
{
   r::sexp::Protect protect;

   std::string terminalId = r::sexp::asString(idSEXP);

   ConsoleProcessPtr proc = findProcByHandle(terminalId);
   if (proc == nullptr)
   {
      return R_NilValue;
   }

   if (proc->getExitCode())
      return r::sexp::create(*proc->getExitCode(), &protect);
   else
      return R_NilValue;
}

// RPC APIs ---------------------------------------------------------------

Error procStart(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != nullptr)
   {
      return proc->start();
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error starting consoleProc",
                         ERROR_LOCATION);
   }
}

Error procInterrupt(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != nullptr)
   {
      proc->interrupt();
      return Success();
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error interrupting consoleProc",
                         ERROR_LOCATION);
   }
}

Error procInterruptChild(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != nullptr)
   {
      proc->interruptChild();
      return Success();
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error interrupting consoleProc",
                         ERROR_LOCATION);
   }
}

Error procReap(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != nullptr)
      return reapConsoleProcess(*proc);
   else
      return systemError(boost::system::errc::invalid_argument,
                         "Error reaping consoleProc",
                         ERROR_LOCATION);
}

Error procWriteStdin(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* /*pResponse*/)
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
   if (proc != nullptr)
   {
      proc->enqueInputInternalLock(input);
      return Success();
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error writing to consoleProc",
                         ERROR_LOCATION);
   }
}

Error procSetSize(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* /*pResponse*/)
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
   if (proc != nullptr)
   {
      proc->resize(cols, rows);
      return Success();

   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error setting consoleProc terminal size",
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
   if (proc == nullptr)
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error setting terminal caption",
                         ERROR_LOCATION);
   }

   // make sure we don't have this name already
   if (findProcByCaption(caption) != nullptr)
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
                         json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;
   std::string title;

   Error error = json::readParams(request.params,
                                  &handle,
                                  &title);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == nullptr)
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error setting terminal title",
                         ERROR_LOCATION);
   }

   proc->setTitle(title);
   return Success();
}

Error procEraseBuffer(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;
   bool lastLineOnly;

   Error error = json::readParams(request.params,
                                  &handle,
                                  &lastLineOnly);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == nullptr)
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error erasing terminal buffer",
                         ERROR_LOCATION);
   }

   proc->deleteLogFile(lastLineOnly);
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
      return systemError(boost::system::errc::invalid_argument,
                         "Invalid buffer chunk requested",
                         ERROR_LOCATION);

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == nullptr)
      return systemError(boost::system::errc::invalid_argument,
                         "Error getting buffer chunk",
                         ERROR_LOCATION);

   json::Object result;
   bool moreAvailable;
   std::string chunkContent = proc->getSavedBufferChunk(requestedChunk, &moreAvailable);

   result["chunk"] = chunkContent;
   result["chunk_number"] = requestedChunk;
   result["more_available"] = moreAvailable;
   pResponse->setResult(result);

   return Success();
}

Error procGetBuffer(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string handle;
   bool stripAnsi;

   Error error = json::readParams(request.params, &handle, &stripAnsi);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == nullptr)
      return systemError(boost::system::errc::invalid_argument,
                         "Error getting buffer chunk",
                         ERROR_LOCATION);

   json::Object result;
   std::string buffer = getTerminalBuffer(proc, stripAnsi);

   result["chunk"] = buffer;
   result["chunk_number"] = 0;
   result["more_available"] = false;
   pResponse->setResult(result);

   return Success();
}

Error procUseRpc(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;

   Error error = json::readParams(request.params,
                                  &handle);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == nullptr)
      return systemError(boost::system::errc::invalid_argument,
                         "Error switching terminal to RPC",
                         ERROR_LOCATION);

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

   bool exists = !(findProcByHandle(handle) == nullptr);
   pResponse->setResult(exists);
   return Success();
}

// Notification from client of currently-selected terminal.
Error procNotifyVisible(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;

   Error error = json::readParams(request.params, &handle);

   if (error)
      return error;

   if (handle.empty())
   {
      // nothing selected in client
      clearVisibleProc();
      return Success();
   }

   // make sure this handle actually exists
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == nullptr)
   {
      clearVisibleProc();
      return systemError(boost::system::errc::invalid_argument,
                         "Error notifying selected terminal",
                         ERROR_LOCATION);
   }

   setVisibleProc(handle);
   return Success();
}

Error getTerminalShells(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   console_process::AvailableTerminalShells availableShells;
   json::Array shells;
   availableShells.toJson(&shells);
   pResponse->setResult(shells);
   return Success();
}

Error startTerminal(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   using namespace session::module_context;
   using namespace session::console_process;

   json::Object jsObject;
   Error error = json::readParam(request.params, 0, &jsObject);
   if (error)
      return error;

   boost::shared_ptr<ConsoleProcessInfo> cpi = ConsoleProcessInfo::fromJson(jsObject);

   std::string handle;
   error = createTerminalConsoleProc(cpi, &handle);
   if (error)
      return error;

   ConsoleProcessPtr ptrProc = findProcByHandle(handle);
   if (!ptrProc)
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Failed to create terminal",
                         ERROR_LOCATION);
   }

   pResponse->setResult(ptrProc->toJson(ClientSerialization));

   return Success();
}

} // anonymous namespace

Error initializeApi()
{
   using namespace module_context;
   using boost::bind;

   RS_REGISTER_CALL_METHOD(rs_terminalActivate, 2);
   RS_REGISTER_CALL_METHOD(rs_terminalCreate, 3);
   RS_REGISTER_CALL_METHOD(rs_terminalClear, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalList, 0);
   RS_REGISTER_CALL_METHOD(rs_terminalContext, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalBuffer, 2);
   RS_REGISTER_CALL_METHOD(rs_terminalVisible, 0);
   RS_REGISTER_CALL_METHOD(rs_terminalBusy, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalRunning, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalKill, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalSend, 2);
   RS_REGISTER_CALL_METHOD(rs_terminalExecute, 4);
   RS_REGISTER_CALL_METHOD(rs_terminalExitCode, 1);

   ExecBlock initBlock;
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
      (bind(registerRpcMethod, "process_use_rpc", procUseRpc))
      (bind(registerRpcMethod, "process_notify_visible", procNotifyVisible))
      (bind(registerRpcMethod, "process_interrupt_child", procInterruptChild))
      (bind(registerRpcMethod, "process_get_buffer", procGetBuffer))
      (bind(registerRpcMethod, "get_terminal_shells", getTerminalShells))
      (bind(registerRpcMethod, "start_terminal", startTerminal));

   return initBlock.execute();
}

} // namespace console_process
} // namespace session
} // namespace rstudio
