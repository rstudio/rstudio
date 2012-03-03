/*
 * SessionConsole.cpp
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


#include "SessionConsole.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>

#include <core/system/OutputCapture.hpp>

#include <r/RExec.hpp>
#include <r/session/RConsoleActions.hpp>

#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace modules { 
namespace console {

namespace {   

bool suppressOutput(const std::string& output)
{
   // tokens to suppress
   const char * const kGlibWarningToken = "GLib-WARNING **: getpwuid_r()";
   const char * const kAutoreleaseNoPool = "utoreleaseNoPool";
   const char * const kSelectInterrupted = "select: Interrupted system call";
   const char * const kNotAGitRepo = "Not a git repository";
   const char * const kIsOutsideRepo = "is outside repository";

   // check tokens
   if (boost::algorithm::contains(output, kGlibWarningToken) ||
       boost::algorithm::contains(output, kAutoreleaseNoPool) ||
       boost::algorithm::contains(output, kSelectInterrupted) ||
       boost::algorithm::contains(output, kNotAGitRepo) ||
       boost::algorithm::contains(output, kIsOutsideRepo))
   {
      return true;
   }
   else
   {
      return false;
   }
}

void writeStandardOutput(const std::string& output)
{  
   module_context::consoleWriteOutput(output);
}

void writeStandardError(const std::string& output)
{
   if (!suppressOutput(output))
      module_context::consoleWriteError(output);
}


Error initializeOutputCapture()
{
   // only capture stderr if it isn't connected to a  terminal
   boost::function<void(const std::string&)> stderrHandler;
   if (!core::system::stderrIsTerminal())
      stderrHandler = writeStandardError;

   // initialize
   return core::system::captureStandardStreams(writeStandardOutput,
                                               stderrHandler);
}

FilePath s_lastWorkingDirectory;

void detectWorkingDirectoryChanged()
{
   FilePath currentWorkingDirectory = module_context::safeCurrentPath();
   if ( s_lastWorkingDirectory.empty() ||
        (currentWorkingDirectory != s_lastWorkingDirectory) )
   {
      // fire event
      std::string path = module_context::createAliasedPath(currentWorkingDirectory);
      ClientEvent event(client_events::kWorkingDirChanged, path);
      module_context::enqueClientEvent(event);

      // update state
      s_lastWorkingDirectory = currentWorkingDirectory;
   }
}

void onClientInit()
{
   // reset state to force wd changed event
   s_lastWorkingDirectory = FilePath();
   detectWorkingDirectoryChanged();
}

void onDetectChanges(module_context::ChangeSource source)
{
   // print warnings after all RPC and URI handlers (not required
   // for REPL because R already does this for us)
   if (source != module_context::ChangeSourceREPL)
      r::exec::printWarnings();

   // check for working directory changed
   detectWorkingDirectoryChanged();
}

Error resetConsoleActions(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   r::session::consoleActions().reset();

   return Success();
}

} // anonymous namespace
   
Error initialize()
{    
   if (!session::options().verifyInstallation())
   {
      // capture standard streams
      Error error = initializeOutputCapture();
      if (error)
         return error;
   }
   
   // subscribe to events
   using boost::bind;
   using namespace module_context;
   events().onClientInit.connect(bind(onClientInit));
   events().onDetectChanges.connect(bind(onDetectChanges, _1));

   // more initialization 
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionConsole.R"))
      (bind(registerRpcMethod, "reset_console_actions", resetConsoleActions));

   return initBlock.execute();
}


} // namespace console
} // namespace modules
} // namesapce session

