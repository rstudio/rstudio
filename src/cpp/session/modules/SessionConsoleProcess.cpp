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
#include <core/system/ShellUtils.hpp>
#include <core/Exec.hpp>
#include <core/SafeConvert.hpp>
#include <core/Settings.hpp>

#include <core/system/Environment.hpp>

#include <session/SessionModuleContext.hpp>

#include "config.h"

#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif

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
#ifndef _WIN32
      options.detachSession = true;
#endif
      return options;
   }
} // anonymous namespace

const int kDefaultMaxOutputLines = 500;

ConsoleProcess::ConsoleProcess()
   : dialog_(false), interactionMode_(InteractionNever),
     maxOutputLines_(kDefaultMaxOutputLines), started_(true),
     interrupt_(false), outputBuffer_(OUTPUT_BUFFER_SIZE)
{
   // When we retrieve from outputBuffer, we only want complete lines. Add a
   // dummy \n so we can tell the first line is a complete line.
   outputBuffer_.push_back('\n');
}

#ifndef _WIN32
ConsoleProcess::ConsoleProcess(const std::string& command,
                               const core::system::ProcessOptions& options,
                               const std::string& caption,
                               bool dialog,
                               InteractionMode interactionMode,
                               int maxOutputLines,
                               const boost::function<void()>& onExit)
   : command_(command), options_(options), caption_(caption), dialog_(dialog),
     interactionMode_(interactionMode), maxOutputLines_(maxOutputLines),
     started_(false), interrupt_(false),
     outputBuffer_(OUTPUT_BUFFER_SIZE),
     onExit_(onExit)
{
   commonInit();
}
#endif

ConsoleProcess::ConsoleProcess(const std::string& program,
                               const std::vector<std::string>& args,
                               const core::system::ProcessOptions& options,
                               const std::string& caption,
                               bool dialog,
                               InteractionMode interactionMode,
                               int maxOutputLines,
                               const boost::function<void()>& onExit)
   : program_(program), args_(args), options_(options), caption_(caption), dialog_(dialog),
     interactionMode_(interactionMode), maxOutputLines_(maxOutputLines),
     started_(false),  interrupt_(false),
     outputBuffer_(OUTPUT_BUFFER_SIZE),
     onExit_(onExit)
{
   commonInit();
}

void ConsoleProcess::commonInit()
{
   handle_ = core::system::generateUuid(false);

   // always redirect stderr to stdout so output is interleaved
   options_.redirectStdErrToStdOut = true;

   if (interactionMode() != InteractionNever)
   {
#ifdef _WIN32
      // NOTE: We use consoleio.exe here in order to make sure svn.exe password
      // prompting works properly
      options_.createNewConsole = true;

      // build new args
      shell_utils::ShellArgs args;
      args << program_;
      args << args_;

      // fixup program_ and args_ so we run the consoleio.exe proxy
      FilePath consoleIoPath = session::options().consoleIoPath();
      program_ = consoleIoPath.absolutePathNative();
      args_ = args;
#else
      // request a pseudoterminal if this is an interactive console process
      options_.pseudoterminal = core::system::Pseudoterminal(80, 1);

      // define TERM to dumb (but first make sure we have an environment
      // block to modify)
      if (!options_.environment)
      {
         core::system::Options childEnv;
         core::system::environment(&childEnv);
         options_.environment = childEnv;
      }
      core::system::setenv(&(options_.environment.get()), "TERM", "dumb");
#endif
   }


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

   Error error;
   if (!command_.empty())
   {
      error = module_context::processSupervisor().runCommand(
                                 command_, options_, createProcessCallbacks());
   }
   else
   {
      error = module_context::processSupervisor().runProgram(
                          program_, args_, options_, createProcessCallbacks());
   }
   if (!error)
      started_ = true;
   return error;
}

void ConsoleProcess::enqueInput(const Input& input)
{
   inputQueue_.push(input);
}

void ConsoleProcess::interrupt()
{
   interrupt_ = true;
}

bool ConsoleProcess::onContinue(core::system::ProcessOperations& ops)
{
   // full stop interrupt if requested
   if (interrupt_)
      return false;

   // process input queue
   while (!inputQueue_.empty())
   {
      // pop input
      Input input = inputQueue_.front();
      inputQueue_.pop();

      // pty interrupt
      if (input.interrupt)
      {
         Error error = ops.ptyInterrupt();
         if (error)
            LOG_ERROR(error);

         if (input.echoInput)
            appendToOutputBuffer("^C");
      }

      // text input
      else
      {
         Error error = ops.writeToStdin(input.text, false);
         if (error)
            LOG_ERROR(error);

         if (input.echoInput)
            appendToOutputBuffer(input.text);
         else
            appendToOutputBuffer("\n");
      }
   }

   // continue
   return true;
}

void ConsoleProcess::appendToOutputBuffer(const std::string &str)
{
   std::copy(str.begin(), str.end(), std::back_inserter(outputBuffer_));
}

void ConsoleProcess::enqueOutputEvent(const std::string &output, bool error)
{
   // copy to output buffer
   appendToOutputBuffer(output);

   // If there's more output than the client can even show, then
   // truncate it to the amount that the client can show. Too much
   // output can overwhelm the client, making it unresponsive.
   std::string trimmedOutput = output;
   string_utils::trimLeadingLines(maxOutputLines_, &trimmedOutput);

   json::Object data;
   data["handle"] = handle_;
   data["error"] = error;
   data["output"] = trimmedOutput;
   module_context::enqueClientEvent(
         ClientEvent(client_events::kConsoleProcessOutput, data));
}

void ConsoleProcess::onStdout(core::system::ProcessOperations& ops,
                              const std::string& output)
{
   enqueOutputEvent(output, false);
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
   result["interaction_mode"] = static_cast<int>(interactionMode_);
   result["max_output_lines"] = maxOutputLines_;
   result["buffered_output"] = bufferedOutput();
   if (exitCode_)
      result["exit_code"] = *exitCode_;
   else
      result["exit_code"] = json::Value();
   return result;
}

boost::shared_ptr<ConsoleProcess> ConsoleProcess::fromJson(
                                             core::json::Object &obj)
{
   boost::shared_ptr<ConsoleProcess> pProc(new ConsoleProcess());
   pProc->handle_ = obj["handle"].get_str();
   pProc->caption_ = obj["caption"].get_str();
   pProc->dialog_ = obj["dialog"].get_bool();

   json::Value mode = obj["interaction_mode"];
   if (!mode.is_null())
      pProc->interactionMode_ = static_cast<InteractionMode>(mode.get_int());
   else
      pProc->interactionMode_ = InteractionNever;

   json::Value maxLines = obj["max_output_lines"];
   if (!maxLines.is_null())
      pProc->maxOutputLines_ = maxLines.get_int();
   else
      pProc->maxOutputLines_ = kDefaultMaxOutputLines;

   std::string bufferedOutput = obj["buffered_output"].get_str();
   std::copy(bufferedOutput.begin(), bufferedOutput.end(),
             std::back_inserter(pProc->outputBuffer_));
   json::Value exitCode = obj["exit_code"];
   if (exitCode.is_null())
      pProc->exitCode_.reset();
   else
      pProc->exitCode_.reset(exitCode.get_int());

   return pProc;
}

core::system::ProcessCallbacks ConsoleProcess::createProcessCallbacks()
{
   core::system::ProcessCallbacks cb;
   cb.onContinue = boost::bind(&ConsoleProcess::onContinue, ConsoleProcess::shared_from_this(), _1);
   cb.onStdout = boost::bind(&ConsoleProcess::onStdout, ConsoleProcess::shared_from_this(), _1, _2);
   cb.onExit = boost::bind(&ConsoleProcess::onExit, ConsoleProcess::shared_from_this(), _1);
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

Error procWriteStdin(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParam(request.params, 0, &handle);
   if (error)
      return error;

   ConsoleProcess::Input input;
   error = json::readObjectParam(request.params, 1,
                                 "interrupt", &input.interrupt,
                                 "text", &input.text,
                                 "echo_input", &input.echoInput);
   if (error)
      return error;

   ProcTable::const_iterator pos = s_procs.find(handle);
   if (pos != s_procs.end())
   {
#ifdef RSTUDIO_SERVER
      if (!input.interrupt)
      {
         error = core::system::crypto::rsaPrivateDecrypt(input.text,
                                                         &input.text);
         if (error)
            return error;
      }
#endif

      pos->second->enqueInput(input);

      return Success();
   }
   else
   {
      return systemError(boost::system::errc::invalid_argument,
                         ERROR_LOCATION);
   }
}

#ifndef _WIN32
boost::shared_ptr<ConsoleProcess> ConsoleProcess::create(
      const std::string& command,
      core::system::ProcessOptions options,
      const std::string& caption,
      bool dialog,
      InteractionMode interactionMode,
      int maxOutputLines,
      const boost::function<void()>& onExit)
{
   options.terminateChildren = true;
   boost::shared_ptr<ConsoleProcess> ptrProc(
         new ConsoleProcess(command,
                            options,
                            caption,
                            dialog,
                            interactionMode,
                            maxOutputLines,
                            onExit));
   s_procs[ptrProc->handle()] = ptrProc;
   return ptrProc;
}
#endif

boost::shared_ptr<ConsoleProcess> ConsoleProcess::create(
      const std::string& program,
      const std::vector<std::string>& args,
      core::system::ProcessOptions options,
      const std::string& caption,
      bool dialog,
      InteractionMode interactionMode,
      int maxOutputLines,
      const boost::function<void()>& onExit)
{
   options.terminateChildren = true;
   boost::shared_ptr<ConsoleProcess> ptrProc(
         new ConsoleProcess(program,
                            args,
                            options,
                            caption,
                            dialog,
                            interactionMode,
                            maxOutputLines,
                            onExit));
   s_procs[ptrProc->handle()] = ptrProc;
   return ptrProc;
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

void onSuspend(core::Settings* pSettings)
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
   pSettings->set("console_procs", ostr.str());
}

void onResume(const core::Settings& settings)
{
   std::string strVal = settings.get("console_procs");
   if (strVal.empty())
      return;

   json::Value value;
   if (!json::parse(strVal, &value))
      return;

   json::Array procs = value.get_array();
   for (json::Array::iterator it = procs.begin();
        it != procs.end();
        it++)
   {
      boost::shared_ptr<ConsoleProcess> proc =
                                    ConsoleProcess::fromJson(it->get_obj());
      s_procs[proc->handle()] = proc;
   }
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // add suspend/resume handler
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   // install rpc methods
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "process_start", procStart))
      (bind(registerRpcMethod, "process_interrupt", procInterrupt))
      (bind(registerRpcMethod, "process_reap", procReap))
      (bind(registerRpcMethod, "process_write_stdin", procWriteStdin));

   return initBlock.execute();
}

} // namespace console_process
} // namespace modules
} // namespace session
