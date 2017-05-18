/*
 * SessionConsoleProcessTable.cpp
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

#include "SessionConsoleProcessTable.hpp"

#include <boost/foreach.hpp>
#include <boost/range/adaptor/map.hpp>

#include <core/Algorithm.hpp>
#include <core/Exec.hpp>
#include <core/SafeConvert.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

#include <r/RRoutines.hpp>
#include <r/RExec.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace console_process {

namespace {

// terminal currently visible in the client
std::string s_visibleTerminalHandle;

typedef std::map<std::string, ConsoleProcessPtr> ProcTable;

ProcTable s_procs;

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
      ConsoleProcessPtr proc = ConsoleProcess::fromJson(it->get_obj());

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

void onSuspend(core::Settings* /*pSettings*/)
{
   serializeConsoleProcs();
   s_visibleTerminalHandle.clear();
}

void onResume(const core::Settings& /*settings*/)
{
}

void loadConsoleProcesses()
{
   std::string contents = ConsoleProcessInfo::loadConsoleProcessMetadata();
   if (contents.empty())
      return;
   deserializeConsoleProcs(contents);
   ConsoleProcessInfo::deleteOrphanedLogs(isKnownProcHandle);
}

} // anonymous namespace--------------

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

ConsoleProcessPtr getVisibleProc()
{
   return findProcByHandle(s_visibleTerminalHandle);
}

std::vector<std::string> getAllCaptions()
{
   std::vector<std::string> allCaptions;
   for (ProcTable::const_iterator it = s_procs.begin(); it != s_procs.end(); it++)
   {
      allCaptions.push_back(it->second->getCaption());
   }
   return allCaptions;
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

   return std::string("Terminal ") + core::safe_convert::numberToString(maxNum);
}

void saveConsoleProcesses()
{
   ConsoleProcessInfo::saveConsoleProcesses(serializeConsoleProcs());
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

   s_visibleTerminalHandle.clear();
   saveConsoleProcesses();
}

void addConsoleProcess(const ConsoleProcessPtr& proc)
{
   s_procs[proc->handle()] = proc;
}

Error reapConsoleProcess(const ConsoleProcess& proc)
{
   proc.deleteLogFile();
   if (s_procs.erase(proc.handle()))
   {
      saveConsoleProcesses();
   }

   // don't report errors if tried to reap something that isn't in the
   // table; there are cases where we do reaping on the server-side and
   // the client may also try to reap the same thing after-the-fact
   return Success();
}

core::json::Array allProcessesAsJson()
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

namespace api { // R API

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

// Return vector of all terminal ids (captions)
SEXP rs_getAllTerminals()
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   return r::sexp::create(getAllCaptions(), &protect);
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

// Returns running state of a terminal (i.e. does the shell have a shell process?)
SEXP rs_isTerminalRunning(SEXP terminalsSEXP)
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
SEXP rs_getTerminalContext(SEXP terminalSEXP)
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
SEXP rs_getTerminalBuffer(SEXP idSEXP, SEXP stripSEXP)
{
   r::sexp::Protect protect;

   std::string terminalId = r::sexp::asString(idSEXP);
   bool stripAnsi = r::sexp::asLogical(stripSEXP);

   ConsoleProcessPtr proc = findProcByCaptionReportUnknown(terminalId);
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

SEXP rs_getVisibleTerminal()
{
   r::sexp::Protect protect;

   if (!session::options().allowShell())
      return R_NilValue;

   ConsoleProcessPtr proc = getVisibleProc();
   if (proc == NULL)
      return R_NilValue;

   return r::sexp::create(proc->getCaption(), &protect);
}

SEXP rs_clearTerminal(SEXP idSEXP)
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
SEXP rs_sendToTerminal(SEXP idSEXP, SEXP textSEXP)
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
SEXP rs_activateTerminal(SEXP idSEXP, SEXP showSEXP)
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

} // namespace api ----------------

namespace rpc { // RPC API

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
      proc->enqueInput(input);
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
      s_visibleTerminalHandle.clear();
      return Success();
   }

   // make sure this handle actually exists
   ConsoleProcessPtr proc = findProcByHandle(handle);
   if (proc == NULL)
   {
      s_visibleTerminalHandle.clear();
      return systemError(boost::system::errc::invalid_argument,
                         "Error notifying selected terminal",
                         ERROR_LOCATION);
   }

   s_visibleTerminalHandle = handle;
   return Success();
}


} // namespace rpc ----------------

Error internalInitialize()
{
   using boost::bind;
   using namespace module_context;

   events().onShutdown.connect(saveConsoleProcessesAtShutdown);
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));

   loadConsoleProcesses();

   RS_REGISTER_CALL_METHOD(api::rs_activateTerminal, 2);
   RS_REGISTER_CALL_METHOD(api::rs_createNamedTerminal, 1);
   RS_REGISTER_CALL_METHOD(api::rs_clearTerminal, 1);
   RS_REGISTER_CALL_METHOD(api::rs_getAllTerminals, 0);
   RS_REGISTER_CALL_METHOD(api::rs_getTerminalContext, 1);
   RS_REGISTER_CALL_METHOD(api::rs_getTerminalBuffer, 2);
   RS_REGISTER_CALL_METHOD(api::rs_getVisibleTerminal, 0);
   RS_REGISTER_CALL_METHOD(api::rs_isTerminalBusy, 1);
   RS_REGISTER_CALL_METHOD(api::rs_isTerminalRunning, 1);
   RS_REGISTER_CALL_METHOD(api::rs_killTerminal, 1);
   RS_REGISTER_CALL_METHOD(api::rs_sendToTerminal, 2);

   // install rpc methods
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "process_start", rpc::procStart))
      (bind(registerRpcMethod, "process_interrupt", rpc::procInterrupt))
      (bind(registerRpcMethod, "process_reap", rpc::procReap))
      (bind(registerRpcMethod, "process_write_stdin", rpc::procWriteStdin))
      (bind(registerRpcMethod, "process_set_size", rpc::procSetSize))
      (bind(registerRpcMethod, "process_set_caption", rpc::procSetCaption))
      (bind(registerRpcMethod, "process_set_title", rpc::procSetTitle))
      (bind(registerRpcMethod, "process_erase_buffer", rpc::procEraseBuffer))
      (bind(registerRpcMethod, "process_get_buffer_chunk", rpc::procGetBufferChunk))
      (bind(registerRpcMethod, "process_test_exists", rpc::procTestExists))
      (bind(registerRpcMethod, "process_use_rpc", rpc::procUseRpc))
      (bind(registerRpcMethod, "process_notify_visible", rpc::procNotifyVisible));

   return initBlock.execute();
}

} // namespace console_process
} // namespace session
} // namespace rstudio
