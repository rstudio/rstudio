/*
 * SessionTerminal.cpp
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

#include "SessionTerminal.hpp"
#include "SessionWorkbench.hpp"

#include <core/Exec.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionConsoleProcess.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace terminal {
namespace {

Error getTerminalOptions(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   json::Object optionsJson;

   FilePath terminalPath;

   // Shell type is needed on Windows to make additional shell-type
   // specific tweaks, see DesktopGwtCallback.cpp::openTerminal
   console_process::TerminalShell::ShellType shellType =
         console_process::TerminalShell::ShellType::Default;

#if defined(_WIN32)

   // Use default terminal setting for shell
   console_process::AvailableTerminalShells shells;
   console_process::TerminalShell shell;

   if (shells.getInfo(prefs::userPrefs().defaultTerminalShellValue(), &shell))
   {
      shellType = shell.type;
      terminalPath = shell.path;
   }

   // last-ditch, use system shell
   if (!terminalPath.exists())
   {
      console_process::TerminalShell sysShell;
      if (console_process::AvailableTerminalShells::getSystemShell(&sysShell))
      {
         shellType = shell.type;
         terminalPath = sysShell.path;
      }
   }

#elif defined(__APPLE__)

   // do nothing (we always launch Terminal.app)

#else

   // read from prefs (if no pref, the computed pref layer supplies one)
   terminalPath = FilePath(prefs::userPrefs().terminalPath());

#endif

   // append shell paths as appropriate
   std::string extraPathEntries;
   session::modules::workbench::ammendShellPaths(&extraPathEntries);

   optionsJson["terminal_path"] = terminalPath.getAbsolutePath();
   optionsJson["working_directory"] =
      module_context::shellWorkingDirectory().getAbsolutePath();
   optionsJson["extra_path_entries"] = extraPathEntries;
   optionsJson["shell_type"] = console_process::TerminalShell::getShellId(shellType);
   pResponse->setResult(optionsJson);

   return Success();
}


} // anonymous namespace

core::Error initialize()
{
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_terminal_options", getTerminalOptions));
   return initBlock.execute();
}

} // namespace terminal
} // namespace modules
} // namespace session
} // namespace rstudio

