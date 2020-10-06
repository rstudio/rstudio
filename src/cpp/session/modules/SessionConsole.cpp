/*
 * SessionConsole.cpp
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


#include "SessionConsole.hpp"
#include "rmarkdown/SessionRmdNotebook.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <shared_core/FilePath.hpp>

#include <core/system/OutputCapture.hpp>

#include <core/text/AnsiCodeParser.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/session/RConsoleActions.hpp>

#include <session/SessionModuleContext.hpp>

#include <session/prefs/UserPrefs.hpp>

#define kMinConsoleLines 10

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace console {

namespace {   

bool suppressOutput(const std::string& output)
{
   if (options().getBoolOverlayOption(kLauncherSessionOption))
   {
      // in launcher session mode, we log normal program errors to stderr so they
      // can be recorded in the launcher job's logs, but we don't want them to show
      // up in the RStudio console, so we filter them out here
      // note: all log messages will contain a tag like [rsession-username]
      if (boost::algorithm::contains(output, "[rsession"))
         return true;
   }

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
   return core::system::captureStandardStreams(writeStandardOutput,
                                               writeStandardError,
                                               options().logStderr());
}

FilePath s_lastWorkingDirectory;

void detectWorkingDirectoryChanged()
{
   FilePath currentWorkingDirectory = module_context::safeCurrentPath();
   if ( s_lastWorkingDirectory.isEmpty() ||
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

text::AnsiCodeMode modeFromPref(const std::string& pref)
{
   if (pref == kAnsiConsoleModeOn)
      return core::text::AnsiColorOn;
   else if (pref == kAnsiConsoleModeOff)
      return core::text::AnsiColorOff;
   return core::text::AnsiColorStrip;
}

} // anonymous namespace
   

void syncConsoleColorEnv() 
{
   // Use rsession alias to avoid collision with 'session'
   // object brought in by Catch
   namespace rsession = rstudio::session;
   using namespace rsession;

   // Mirror ansi_color_mode user preference with RSTUDIO_CONSOLE_COLOR
   // environment variable so it can be inherited by spawned R processes, such
   // as package builds, which cannot query RStudio IDE settings.
   if (modeFromPref(prefs::userPrefs().ansiConsoleMode()) == core::text::AnsiColorOn)
   {
      core::system::setenv("RSTUDIO_CONSOLE_COLOR", "256");

      if (rsession::options().defaultConsoleTerm().length() > 0)
         core::system::setenv("TERM", rsession::options().defaultConsoleTerm());
      if (rsession::options().defaultCliColorForce())
         core::system::setenv("CLICOLOR_FORCE", "1");
   }
   else
   {
      core::system::unsetenv("RSTUDIO_CONSOLE_COLOR");
      core::system::unsetenv("TERM");
      core::system::unsetenv("CLICOLOR_FORCE");
   }
}

Error initialize()
{    
   if (!(session::options().verifyInstallation() ||
         session::options().runTests() ||
         !session::options().runScript().empty()))
   {
      // capture standard streams
      Error error = initializeOutputCapture();
      if (error)
         return error;
   }

   // set console action capacity from user pref, presuming the value is reasonable
   int maxLines = prefs::userPrefs().consoleMaxLines();
   if (maxLines < kMinConsoleLines)
   {
      LOG_WARNING_MESSAGE("Console must have at least " + 
            safe_convert::numberToString(kMinConsoleLines) + 
            " lines; ignoring invalid max line setting '" +
            safe_convert::numberToString(maxLines) + "'");
   }
   else
   {
      r::session::consoleActions().setCapacity(maxLines);
   }

   // register routines
   RS_REGISTER_CALL_METHOD(rs_getPendingInput);
   
   // subscribe to events
   using boost::bind;
   using namespace module_context;
   events().onClientInit.connect(bind(onClientInit));
   events().onDetectChanges.connect(bind(onDetectChanges, _1));

   rmarkdown::notebook::events().onChunkExecCompleted.connect(
         bind(onChunkExecCompleted));

   // more initialization 
   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionConsole.R"))
      (bind(registerRpcMethod, "reset_console_actions", resetConsoleActions));

   return initBlock.execute();
}


} // namespace console
} // namespace modules
} // namespace session
} // namespace rstudio

