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
   const size_t OUTPUT_BUFFER_SIZE = 8192;
   typedef std::map<std::string, boost::shared_ptr<ConsoleProcess> > ProcTable;
   ProcTable s_procs;

   core::system::ProcessOptions procOptions()
   {
      core::system::ProcessOptions options;
      options.detachSession = true;
      return options;
   }
} // anonymous namespace

ConsoleProcess::ConsoleProcess(const std::string& command,
                               const core::system::ProcessOptions& options,
                               const std::string& caption,
                               bool dialog,
                               const boost::function<void()>& onExit)
   : command_(command), options_(options), caption_(caption), dialog_(dialog),
     started_(false), interrupt_(false), outputBuffer_(OUTPUT_BUFFER_SIZE),
     onExit_(onExit)
{
   handle_ = core::system::generateUuid(false);

   // When we retrieve from outputBuffer, we only want complete lines. Add a
   // dummy \n so we can tell the first line is a complete line.
   outputBuffer_.push_back('\n');
}

std::string ConsoleProcess::bufferedOutput() const
{
   boost::circular_buffer<char>::const_iterator pos =
         std::find(outputBuffer_.begin(), outputBuffer_.end(), '\n');

   std::string result;
   if (pos != outputBuffer_.end())
      pos++;
   std::copy(pos, outputBuffer_.end(), std::back_inserter(result));
   // Will be empty if the buffer was overflowed by a single line
   return result;
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
   std::copy(output.begin(), output.end(),
             std::back_inserter(outputBuffer_));

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
   exitCode_.reset(exitCode);

   json::Object data;
   data["handle"] = handle_;
   data["exitCode"] = exitCode;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessExit, data));

   if (onExit_)
      onExit_();
}

core::json::Object ConsoleProcess::toJson() const
{
   json::Object result;
   result["handle"] = handle_;
   result["caption"] = caption_;
   result["dialog"] = dialog_;
   result["buffered_output"] = bufferedOutput();
   if (exitCode_)
      result["exit_code"] = *exitCode_;
   else
      result["exit_code"] = json::Value();
   return result;
}

core::system::ProcessCallbacks ConsoleProcess::createProcessCallbacks()
{
   core::system::ProcessCallbacks cb;
   cb.onContinue = boost::bind(&ConsoleProcess::onContinue, ConsoleProcess::shared_from_this(), _1);
   cb.onStdout = boost::bind(&ConsoleProcess::onStdout, ConsoleProcess::shared_from_this(), _1, _2);
   cb.onStderr = boost::bind(&ConsoleProcess::onStderr, ConsoleProcess::shared_from_this(), _1, _2);
   cb.onExit = boost::bind(&ConsoleProcess::onExit, ConsoleProcess::shared_from_this(), _1);
   return cb;
}

// Creates the ConsoleProcess object and returns the handle, but doesn't
// actually launch the process. This is so that the client gets a chance
// to hook up any necessary event listeners before the process starts
// causing events to be fired.
Error procInit(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   std::string command, caption;
   bool dialog;
   Error error = json::readParams(request.params, &command, &caption, &dialog);
   if (error)
      return error;

   boost::shared_ptr<ConsoleProcess> ptrProc = ConsoleProcess::create(
                                                       command,
                                                       procOptions(),
                                                       caption,
                                                       dialog);
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

Error procReap(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;

   if (!s_procs.erase(handle))
   {
      return systemError(boost::system::errc::invalid_argument,
                         ERROR_LOCATION);
   }
   else
   {
      return Success();
   }
}

boost::shared_ptr<ConsoleProcess> ConsoleProcess::create(
      const std::string &command,
      core::system::ProcessOptions options,
      const std::string& caption,
      bool dialog,
      const boost::function<void()>& onExit)
{
   options.terminateChildren = true;
   boost::shared_ptr<ConsoleProcess> ptrProc(
         new ConsoleProcess(command, options, caption, dialog, onExit));
   s_procs[ptrProc->handle()] = ptrProc;
   return ptrProc;
}

const std::map<std::string, boost::shared_ptr<ConsoleProcess> >& processes()
{
   return s_procs;
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
      (bind(registerRpcMethod, "process_interrupt", procInterrupt))
      (bind(registerRpcMethod, "process_reap", procReap));

   return initBlock.execute();
}

} // namespace console_process
} // namespace modules
} // namespace session
