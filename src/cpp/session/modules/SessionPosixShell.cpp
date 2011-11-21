/*
 * SessionPosixShell.cpp
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

// TODO: set sane initial size for shell dialog

// TODO: ui chrome (consider console dialog style hanging from top)
// TODO: border for shell widget

// TODO: painting issue where ace below dialog paints above (Ctrl-C
// off of sleep 1000 repros)

// TODO: vim and more behave oddly -- any way to full disable?

// TODO: on termination we get this error message printed:
//   Cannot set tty process group (Inappropriate ioctl for device)
// is there any way to terminate cleanly (perhaps another ctrl signal)

// TODO: up-arrow history

// TODO: other editing shortcuts (completion manager?)

// TODO: cap output lines sent on the server


#include "SessionPosixShell.hpp"

#include <boost/shared_ptr.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace posix_shell {

namespace {

class PosixShell;
boost::shared_ptr<PosixShell> s_pActiveShell;

class PosixShell : boost::noncopyable
{
public:
   static Error create(int width,
                       int maxLines,
                       boost::shared_ptr<PosixShell>* ppPosixShell)
   {
      boost::shared_ptr<PosixShell> pShell(new PosixShell(maxLines));

      system::ProcessCallbacks cb;
      cb.onContinue = boost::bind(&PosixShell::onContinue, pShell, _1);
      cb.onStdout = boost::bind(&PosixShell::onStdout, pShell, _2);
      cb.onExit = boost::bind(&PosixShell::onExit, pShell, _1);

      system::ProcessOptions options;
      options.pseudoterminal = system::Pseudoterminal(width, 1);
      Error error = module_context::processSupervisor().runCommand("/bin/sh",
                                                                   options,
                                                                   cb);
      if (!error)
      {
         *ppPosixShell = pShell;
         return Success();
      }
      else
      {
         return error;
      }

   }

private:
   PosixShell(int maxLines)
      : maxLines_(maxLines), interrupt_(false), terminate_(false)
   {
   }

public:
   virtual ~PosixShell() {}

   void enqueueInput(const std::string &input)
   {
      inputQueue_.append(input);
   }

   void interrupt()
   {
      interrupt_ = true;
   }

   void terminate()
   {
      terminate_ = true;
   }


private:
   bool onContinue(core::system::ProcessOperations& ops)
   {
      if (terminate_)
         return false;

      if (!inputQueue_.empty())
      {
         Error error = ops.writeToStdin(inputQueue_, false);
         if (error)
            LOG_ERROR(error);

         inputQueue_.clear();
      }

      if (interrupt_)
      {
         Error error = ops.ptyInterrupt();
         if (error)
            LOG_ERROR(error);

         interrupt_ = false;
      }

      return true;
   }

   void onStdout(const std::string& output)
   {
      // if we are the active shell then emit output event
      if (s_pActiveShell.get() == this)
      {
         ClientEvent event(client_events::kPosixShellOutput, output);
         module_context::enqueClientEvent(event);
      }
   }

   void onExit(int exitCode)
   {
      // if we are the active shell then notify the client that we
      // are being terminated (if we aren't the active shell then
      // the client isn't (logically) listenting for exit
      if (s_pActiveShell.get() == this)
      {
         s_pActiveShell.reset();

         // notify client
         json::Object eventData;
         eventData["exit_code"] = exitCode;
         ClientEvent event(client_events::kPosixShellExit, eventData);
         module_context::enqueClientEvent(event);
      }
   }

private:
   int maxLines_;
   std::string inputQueue_;
   bool interrupt_;
   bool terminate_;
};


Error startPosixShell(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   // get parameters
   int width, maxLines;
   Error error = json::readParams(request.params, &width, &maxLines);
   if (error)
      return error;

   // terminate any existing shell
   if (s_pActiveShell)
   {
      s_pActiveShell->terminate();
      s_pActiveShell.reset();
   }

   // start a new shell
   return PosixShell::create(width, maxLines, &s_pActiveShell);
}

Error interruptPosixShell(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // validate we have an active shell
   if (!s_pActiveShell)
      return Error(json::errc::MethodUnexpected, ERROR_LOCATION);

   s_pActiveShell->interrupt();

   return Success();
}

Error sendInputToPosixShell(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   // read param
   std::string input;
   Error error = json::readParam(request.params, 0, &input);
   if (error)
      return error;

   // validate we have an active shell
   if (!s_pActiveShell)
      return Error(json::errc::MethodUnexpected, ERROR_LOCATION);

   // send input
   s_pActiveShell->enqueueInput(input);

   return Success();
}

Error terminatePosixShell(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // validate we have an active shell
   if (!s_pActiveShell)
      return Error(json::errc::MethodUnexpected, ERROR_LOCATION);

   s_pActiveShell->terminate();

   return Success();
}


void onClientInit()
{
   // terminate any active shell we have on client init
   if (s_pActiveShell)
   {
      s_pActiveShell->terminate();
      s_pActiveShell.reset();
   }
}


} // anonymous namespace


Error initialize()
{
   using namespace module_context;
   module_context::events().onClientInit.connect(boost::bind(onClientInit));

   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "start_posix_shell", startPosixShell))
      (bind(registerRpcMethod, "interrupt_posix_shell", interruptPosixShell))
      (bind(registerRpcMethod, "send_input_to_posix_shell", sendInputToPosixShell))
      (bind(registerRpcMethod, "terminate_posix_shell", terminatePosixShell));
   return initBlock.execute();

}
   
   
} // namespace posix_shell
} // namespace modules
} // namesapce session

