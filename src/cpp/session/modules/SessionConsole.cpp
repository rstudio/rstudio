/*
 * SessionConsole.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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


#include "SessionConsole.hpp"
#include "rmarkdown/SessionRmdNotebook.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>

#include <core/system/OutputCapture.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RConsoleActions.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace console {

namespace {   

bool suppressOutput(const std::string& output)
{
   // tokens to suppress
   const char * const kGlibWarningToken = "GLib-WARNING **:";
   const char * const kGlibCriticalToken = "GLib-CRITICAL **:";
   const char * const kGlibGObjectWarningToken = "GLib-GObject-WARNING **:";
   const char * const kAutoreleaseNoPool = "utoreleaseNoPool";
   const char * const kSelectInterrupted = "select: Interrupted system call";
   const char * const kNotAGitRepo = "Not a git repository";
   const char * const kIsOutsideRepo = "is outside repository";
   const char * const kCGContextError = "<Error>: CGContext";

   // check tokens
   if (boost::algorithm::contains(output, kGlibWarningToken) ||
       boost::algorithm::contains(output, kGlibCriticalToken) ||
       boost::algorithm::contains(output, kGlibGObjectWarningToken) ||
       boost::algorithm::contains(output, kAutoreleaseNoPool) ||
       boost::algorithm::contains(output, kSelectInterrupted) ||
       boost::algorithm::contains(output, kNotAGitRepo) ||
       boost::algorithm::contains(output, kIsOutsideRepo) ||
       boost::algorithm::contains(output, kCGContextError))
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
      module_context::activeSession().setWorkingDir(path);
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
   // check for working directory changed
   detectWorkingDirectoryChanged();
}

void onChunkExecCompleted()
{
   // notebook chunks restore the working directory when they're done executing,
   // so check when they're finished
   detectWorkingDirectoryChanged();
}

Error resetConsoleActions(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   r::session::consoleActions().reset();

   return Success();
}

SEXP rs_getPendingInput()
{
   r::sexp::Protect rProtect;
   return r::sexp::create(r::session::consoleActions().pendingInput(),
                          &rProtect);
}

} // anonymous namespace
   
Error initialize()
{    
   if (!(session::options().verifyInstallation() ||
         session::options().runTests()))
   {
      // capture standard streams
      Error error = initializeOutputCapture();
      if (error)
         return error;
   }

   // register routines
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_getPendingInput" ;
   methodDef.fun = (DL_FUNC) rs_getPendingInput ;
   methodDef.numArgs = 0;
   r::routines::addCallMethod(methodDef);

   
   // subscribe to events
   using boost::bind;
   using namespace module_context;
   events().onClientInit.connect(bind(onClientInit));
   events().onDetectChanges.connect(bind(onDetectChanges, _1));

   rmarkdown::notebook::events().onChunkExecCompleted.connect(
         bind(onChunkExecCompleted));

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
} // namespace session
} // namespace rstudio

