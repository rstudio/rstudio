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

#include <core/SafeConvert.hpp>

#include <session/SessionModuleContext.hpp>

#include "SessionConsoleProcessApi.hpp"
#include "modules/SessionVCS.hpp"

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

void clearVisibleProc()
{
   s_visibleTerminalHandle.clear();
}

void setVisibleProc(const std::string& handle)
{
   s_visibleTerminalHandle = handle;
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

// Determine next terminal sequence and name
std::pair<int, std::string> nextTerminalName()
{
   int maxNum = kNoTerminal;
   BOOST_FOREACH(ConsoleProcessPtr& proc, s_procs | boost::adaptors::map_values)
   {
      maxNum = std::max(maxNum, proc->getTerminalSequence());
   }
   maxNum++;

   return std::pair<int, std::string>(
            maxNum,
            std::string("Terminal ") + core::safe_convert::numberToString(maxNum));
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
   proc.deleteEnvFile();
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

Error internalInitialize()
{
   using boost::bind;
   using namespace module_context;

   events().onShutdown.connect(saveConsoleProcessesAtShutdown);
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));

   loadConsoleProcesses();

   return initializeApi();
}

Error createTerminalConsoleProc(
      TerminalShell::TerminalShellType shellType,
      int cols,
      int rows,
      const std::string& termHandle, // empty if starting a new terminal
      const std::string& termCaption,
      const std::string& termTitle,
      int termSequence,
      bool altBufferActive,
      const std::string& currentDir,
      bool zombie,
      bool trackEnv,
      std::string* pHandle)
{
   using namespace session::module_context;
   using namespace session::console_process;

#if defined(_WIN32)
   trackEnv = false;
#endif

   std::string computedCaption = termCaption;

   if (termSequence == kNewTerminal)
   {
      std::pair<int, std::string> sequenceInfo = nextTerminalName();
      termSequence = sequenceInfo.first;
      if (computedCaption.empty())
         computedCaption = sequenceInfo.second;
   }

   if (termSequence == kNoTerminal)
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Unsupported terminal sequence",
                         ERROR_LOCATION);
   }

   if (computedCaption.empty())
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Empty terminal caption not supported",
                         ERROR_LOCATION);
   }

   FilePath cwd;
   if (!currentDir.empty())
   {
      cwd = module_context::resolveAliasedPath(currentDir);
   }

   TerminalShell::TerminalShellType actualShellType;
   core::system::ProcessOptions options = ConsoleProcess::createTerminalProcOptions(
         shellType, cols, rows, termSequence, cwd, trackEnv, termHandle, &actualShellType);

   boost::shared_ptr<ConsoleProcessInfo> ptrProcInfo =
         boost::shared_ptr<ConsoleProcessInfo>(new ConsoleProcessInfo(
            computedCaption, termTitle, termHandle, termSequence, actualShellType,
            altBufferActive, cwd, cols, rows, zombie, trackEnv));

   boost::shared_ptr<ConsoleProcess> ptrProc =
               ConsoleProcess::createTerminalProcess(options, ptrProcInfo);

   ptrProc->onExit().connect(boost::bind(
                              &modules::source_control::enqueueRefreshEvent));

   *pHandle = ptrProc->handle();

   return Success();
}

Error createTerminalExecuteConsoleProc(
      const std::string& title,
      const std::string& command,
      const std::vector<std::string>& args,
      const std::string& currentDir,
      std::string* pHandle)
{
   using namespace session::module_context;
   using namespace session::console_process;

   std::string termHandle;
   int cols = core::system::kDefaultCols;
   int rows = core::system::kDefaultRows;

   std::pair<int, std::string> sequenceInfo = nextTerminalName();
   int termSequence = sequenceInfo.first;
   std::string caption = sequenceInfo.second;

   FilePath cwd;
   if (!currentDir.empty())
   {
      cwd = module_context::resolveAliasedPath(currentDir);
   }

   core::system::ProcessOptions options;

   core::system::Options childEnv;
   core::system::environment(&childEnv);
   options.environment = childEnv;

   options.smartTerminal = true;

   std::string actualCommand;

#ifndef _WIN32
   options.detachSession = true;

   shell_utils::ShellCommand cmdWithArgs(command);
   cmdWithArgs << shell_utils::EscapeFilesOnly;
   BOOST_FOREACH(const std::string& arg, args)
   {
      cmdWithArgs << arg;
   }
   actualCommand = cmdWithArgs;
#endif

#ifdef _WIN32
   options.detachProcess = true;

   // TODO (gary) construct command
   // actualCommand =
   // commandArgs =
#endif

   options.reportHasSubprocs = true;
   options.trackCwd = false;
   options.workingDir = cwd;

   boost::shared_ptr<ConsoleProcessInfo> ptrProcInfo =
         boost::shared_ptr<ConsoleProcessInfo>(new ConsoleProcessInfo(
            caption, title, termHandle, termSequence, TerminalShell::NoShell,
            false /*altBuffer*/, cwd, cols, rows, false /*zombie*/, false /*trackEnv*/));

   // TODO (gary) ensure these are roundtripping successfully
   ptrProcInfo->setInteractionMode(InteractionNever);
   ptrProcInfo->setAutoClose(NeverAutoClose);
   ptrProcInfo->setHasChildProcs(false);

   boost::shared_ptr<ConsoleProcess> ptrProc =
               ConsoleProcess::createTerminalExecuteProcess(cmdWithArgs, options, ptrProcInfo);

   *pHandle = ptrProc->handle();

   return Success();
}

} // namespace console_process
} // namespace session
} // namespace rstudio
