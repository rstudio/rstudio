/*
 * SessionConsoleProcessApi.cpp
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

#include "SessionConsoleProcessApi.hpp"

#include <boost/foreach.hpp>

#include <core/Algorithm.hpp>
#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

#include <r/RRoutines.hpp>
#include <r/RExec.hpp>

#include "modules/SessionWorkbench.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace console_process {

namespace {

// findProcByCaption that reports an R error for unknown caption
ConsoleProcessPtr findProcByCaptionReportUnknown(const std::string& caption)
{
   ConsoleProcessPtr proc = findProcByCaption(caption);
   if (proc == NULL)
   {
      std::string error("Unknown terminal '");
      error += caption;
      error += "'";
      r::exec::error(error);
   }
   return proc;
}

// Return buffer for a terminal, optionally stripping out Ansi codes.
std::string getTerminalBuffer(ConsoleProcessPtr proc, bool stripAnsi)
{
   std::string buffer = proc->getBuffer();

   if (stripAnsi)
      core::text::stripAnsiCodes(&buffer);
   string_utils::convertLineEndings(&buffer, string_utils::LineEndingPosix);
   return buffer;
}


// R APIs ---------------------------------------------------------------

// Return vector of all terminal ids (captions)
SEXP rs_terminalList()
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   return r::sexp::create(getAllCaptions(), &protect);
}

// Create a terminal with given id (caption). If null, create with automatically
// generated name. Returns resulting name in either case.
SEXP rs_terminalCreate(SEXP typeSEXP)
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
SEXP rs_terminalBusy(SEXP terminalsSEXP)
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

// Returns running state of a terminal (i.e. does the shell have a shell process?)
SEXP rs_terminalRunning(SEXP terminalsSEXP)
{
   r::sexp::Protect protect;

   std::vector<std::string> terminalIds;
   if (!r::sexp::fillVectorString(terminalsSEXP, &terminalIds))
      return R_NilValue;

   std::vector<bool> isRunning;
   BOOST_FOREACH(const std::string& terminalId, terminalIds)
   {
      ConsoleProcessPtr proc = findProcByCaption(terminalId);
      if (proc == NULL)
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

   ConsoleProcessPtr proc = findProcByCaption(terminalId);
   if (proc == NULL)
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
   builder.add("connection", proc->getChannelMode());
   builder.add("sequence", proc->getTerminalSequence());
   builder.add("lines", proc->getBufferLineCount());
   builder.add("cols", proc->getCols());
   builder.add("rows", proc->getRows());
   builder.add("pid", static_cast<int>(proc->getPid()));
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

   ConsoleProcessPtr proc = findProcByCaptionReportUnknown(terminalId);
   if (proc == NULL)
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

   BOOST_FOREACH(const std::string& terminalId, terminalIds)
   {
      ConsoleProcessPtr proc = findProcByCaption(terminalId);
      if (proc != NULL)
      {
         proc->interrupt();
         reapConsoleProcess(*proc);
      }
   }
   return R_NilValue;
}

SEXP rs_terminalVisible()
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   ConsoleProcessPtr proc = getVisibleProc();
   if (proc == NULL)
      return R_NilValue;

   return r::sexp::create(proc->getCaption(), &protect);
}

SEXP rs_terminalClear(SEXP idSEXP)
{
   std::string terminalId = r::sexp::asString(idSEXP);

   ConsoleProcessPtr proc = findProcByCaptionReportUnknown(terminalId);
   if (proc == NULL)
      return R_NilValue;

   // clear the server-side log directly
   proc->deleteLogFile();

   // send the event to the client; if not connected, it will get the cleared
   // server-side buffer next time it connects.
   json::Object eventData;
   eventData["id"] = terminalId;

   ClientEvent clearNamedTerminalEvent(client_events::kClearTerminal, eventData);
   module_context::enqueClientEvent(clearNamedTerminalEvent);

   return R_NilValue;
}

// Send text to the terminal
SEXP rs_terminalSend(SEXP idSEXP, SEXP textSEXP)
{
   std::string terminalId = r::sexp::asString(idSEXP);
   std::string text = r::sexp::asString(textSEXP);

   ConsoleProcessPtr proc = findProcByCaptionReportUnknown(terminalId);
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
   std::string terminalId = r::sexp::asString(idSEXP);
   bool show = r::sexp::asLogical(showSEXP);

   if (!session::options().allowShell())
      return R_NilValue;

   if (!terminalId.empty())
   {
      ConsoleProcessPtr proc = findProcByCaptionReportUnknown(terminalId);
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
            r::exec::error(err.summary());
            return R_NilValue;
         }
      }
   }

   if (show)
   {
      json::Object eventData;
      eventData["id"] = terminalId;

      ClientEvent activateTerminalEvent(client_events::kActivateTerminal, eventData);
      module_context::enqueClientEvent(activateTerminalEvent);
   }
   return R_NilValue;
}

// RPC APIs ---------------------------------------------------------------

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
                         "Error starting consoleProc",
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
                         "Error interrupting consoleProc",
                         ERROR_LOCATION);
   }
}

Error procInterruptChild(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != NULL)
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
               json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc != NULL)
      return reapConsoleProcess(*proc);
   else
      return systemError(boost::system::errc::invalid_argument,
                         "Error reaping consoleProc",
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
   if (proc == NULL)
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Error setting terminal caption",
                         ERROR_LOCATION);
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
      return systemError(boost::system::errc::invalid_argument,
                         "Error setting terminal title",
                         ERROR_LOCATION);
   }

   proc->setTitle(title);
   return Success();
}

Error procEraseBuffer(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string handle;
   bool lastLineOnly;

   Error error = json::readParams(request.params,
                                  &handle,
                                  &lastLineOnly);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
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
   if (proc == NULL)
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
   if (proc == NULL)
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
                 json::JsonRpcResponse* pResponse)
{
   std::string handle;

   Error error = json::readParams(request.params,
                                  &handle);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
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

   bool exists = (findProcByHandle(handle) == NULL) ? false : true;
   pResponse->setResult(exists);
   return Success();
}

// Notification from client of currently-selected terminal.
Error procNotifyVisible(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
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
   if (proc == NULL)
   {
      clearVisibleProc();
      return systemError(boost::system::errc::invalid_argument,
                         "Error notifying selected terminal",
                         ERROR_LOCATION);
   }

   setVisibleProc(handle);
   return Success();
}

Error procSetZombie(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string handle;

   Error error = json::readParams(request.params,
                                  &handle);
   if (error)
      return error;

   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
      return systemError(boost::system::errc::invalid_argument,
                         "Error setting process to zombie mode",
                         ERROR_LOCATION);

   // Set a process to zombie mode meaning we keep showing it and its buffer, but don't
   // start its process
   proc->setZombie();
   return Success();
}

} // anonymous namespace

Error initializeApi()
{
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_terminalActivate, 2);
   RS_REGISTER_CALL_METHOD(rs_terminalCreate, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalClear, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalList, 0);
   RS_REGISTER_CALL_METHOD(rs_terminalContext, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalBuffer, 2);
   RS_REGISTER_CALL_METHOD(rs_terminalVisible, 0);
   RS_REGISTER_CALL_METHOD(rs_terminalBusy, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalRunning, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalKill, 1);
   RS_REGISTER_CALL_METHOD(rs_terminalSend, 2);

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
      (bind(registerRpcMethod, "process_use_rpc", procUseRpc))
      (bind(registerRpcMethod, "process_notify_visible", procNotifyVisible))
      (bind(registerRpcMethod, "process_set_zombie", procSetZombie))
      (bind(registerRpcMethod, "process_interrupt_child", procInterruptChild))
      (bind(registerRpcMethod, "process_get_buffer", procGetBuffer));

   return initBlock.execute();
}

} // namespace console_process
} // namespace session
} // namespace rstudio
