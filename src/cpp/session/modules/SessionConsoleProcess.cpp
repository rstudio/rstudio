/*
 * SessionConsoleProcess.cpp
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
#include "SessionConsoleProcess.hpp"

#include <core/json/JsonRpc.hpp>
#include <core/system/Process.hpp>
#include <core/Exec.hpp>
#include <core/SafeConvert.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules {
namespace console_process {

namespace {
   typedef std::map<std::string, boost::shared_ptr<ConsoleProcess> > ProcTable;
   ProcTable s_procs;

   core::system::ProcessOptions procOptions()
   {
      core::system::ProcessOptions options;
#ifdef __linux__
      options.onAfterFork = &::setsid;
#endif
      return options;
   }
} // anonymous namespace

ConsoleProcess::ConsoleProcess(const std::string& command,
                               const core::system::ProcessOptions& options,
                               const boost::function<void()>& onExit)
   : command_(command), options_(options), started_(false),
     interrupt_(false), onExit_(onExit)
{
   handle_ = core::system::generateUuid(false);
}

Error ConsoleProcess::start()
{
   if (started_)
      return Success();

   Error error = module_context::processSupervisor().runCommand(
                                 command_, options_, createProcessCallbacks());
   if (!error)
      started_ = true;
   return error;
}

void ConsoleProcess::enqueueInput(const std::string &input)
{
   inputQueue_.append(input);
}

void ConsoleProcess::interrupt()
{
   interrupt_ = true;
}

bool ConsoleProcess::onContinue(core::system::ProcessOperations& ops)
{
   if (!inputQueue_.empty())
   {
      Error error = ops.writeToStdin(inputQueue_, false);
      if (error)
         LOG_ERROR(error);

      inputQueue_.clear();
   }

   return !interrupt_;
}

void ConsoleProcess::onStdout(core::system::ProcessOperations& ops,
                                     const std::string& output)
{
   json::Object data;
   data["handle"] = handle_;
   data["error"] = false;
   data["output"] = output;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessOutput, data));
}

void ConsoleProcess::onStderr(core::system::ProcessOperations& ops,
                                     const std::string& output)
{
   json::Object data;
   data["handle"] = handle_;
   data["error"] = true;
   data["output"] = output;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessOutput, data));
}

void ConsoleProcess::onExit(int exitCode)
{
   json::Object data;
   data["handle"] = handle_;
   data["exitCode"] = exitCode;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessExit, data));

   s_procs.erase(handle_);

   if (onExit_)
      onExit_();
}

core::system::ProcessCallbacks ConsoleProcess::createProcessCallbacks()
{
   core::system::ProcessCallbacks cb;
   cb.onContinue = boost::bind(&ConsoleProcess::onContinue, this, _1);
   cb.onStdout = boost::bind(&ConsoleProcess::onStdout, this, _1, _2);
   cb.onStderr = boost::bind(&ConsoleProcess::onStderr, this, _1, _2);
   cb.onExit = boost::bind(&ConsoleProcess::onExit, this, _1);
   return cb;
}

// Creates the ConsoleProcess object and returns the handle, but doesn't
// actually launch the process. This is so that the client gets a chance
// to hook up any necessary event listeners before the process starts
// causing events to be fired.
Error procInit(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   std::string command;
   Error error = json::readParams(request.params, &command);
   if (error)
      return error;

   boost::shared_ptr<ConsoleProcess> ptrProc = ConsoleProcess::create(
                                                       command, procOptions());
   pResponse->setResult(ptrProc->handle());
   return Success();
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

boost::shared_ptr<ConsoleProcess> ConsoleProcess::create(
      const std::string &command,
      core::system::ProcessOptions options,
      const boost::function<void()>& onExit)
{
   options.terminateChildren = true;
   boost::shared_ptr<ConsoleProcess> ptrProc(
         new ConsoleProcess(command, options, onExit));
   s_procs[ptrProc->handle()] = ptrProc;
   return ptrProc;
}

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "process_prepare", procInit))
      (bind(registerRpcMethod, "process_start", procStart))
      (bind(registerRpcMethod, "process_interrupt", procInterrupt));

   return initBlock.execute();
}

} // namespace console_process
} // namespace modules
} // namespace session
