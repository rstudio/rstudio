/*
 * SessionTerminalShell.cpp
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

#include <session/SessionTerminalShell.hpp>

#include <boost/foreach.hpp>

#include <core/system/System.hpp>
#include <core/Log.hpp>
#include <core/Error.hpp>

#include <session/SessionUserSettings.hpp>
#include "SessionGit.hpp"

namespace rstudio {
namespace session {
namespace console_process {

namespace {

void addShell(const core::FilePath& expectedPath,
              TerminalShell::TerminalShellType type,
              const std::string& title,
              std::vector<std::string> args,
              std::vector<TerminalShell>* pShells)
{
   if (expectedPath.exists())
      pShells->push_back(TerminalShell(type, title, expectedPath, args));
}

void scanAvailableShells(std::vector<TerminalShell>* pShells)
{
#ifdef _WIN32
   // On Vista and above virtual directories allow processes to get at both
   // 32 and 64-bit system binaries (cmd.exe and powershell.exe).
   //
   // * On a 32-bit OS, %windir%\system32 contains 32-bit, and there are
   //   no 64-bit bins.
   //
   // * On a 64-bit OS, a 32-bit process sees 32-bit bins in %windir%\system32,
   //   and 64-bit bins in %windir%\sysnative.
   //
   // * On a 64-bit OS, a 64-bit process sees 32-bit bins in %windir%\syswow64,
   //   and 64-bit bins in %windir%\system32.

   std::vector<std::string> args;

   if (!core::system::isVistaOrLater())
   {
      // Below Vista, just use %comspec%
      addShell(core::system::expandComSpec(),
               TerminalShell::Cmd32, "Command Prompt", args, pShells);
   }
   else
   {
      const std::string sysnative("sysnative\\");
      const std::string sys32("system32\\");
      const std::string wow64("syswow64\\");
      const std::string cmd("cmd.exe");
      const std::string ps("WindowsPowerShell\\v1.0\\powershell.exe");
      const std::string bash("bash.exe");

      std::string windir;
      core::Error err = core::system::expandEnvironmentVariables("%windir%\\", &windir);
      if (err)
      {
         LOG_ERROR(err);
         return;
      }

      addShell(getGitBashShell(), TerminalShell::GitBash, "Git Bash", args, pShells);

      core::FilePath cmd32;
      core::FilePath cmd64;
      core::FilePath ps32;
      core::FilePath ps64;
      core::FilePath bashWSL; // Windows Subsystem for Linux

      if (core::system::isWin64())
      {
         if (core::system::isCurrentProcessWin64())
         {
            cmd32 = core::FilePath(windir + wow64 + cmd);
            cmd64 = core::FilePath(windir + sys32 + cmd);
            ps32 = core::FilePath(windir + wow64 + ps);
            ps64 = core::FilePath(windir + sys32 + ps);
            bashWSL = core::FilePath(windir + sys32 + bash);
         }
         else
         {
            cmd32 = core::FilePath(windir + sys32 + cmd);
            cmd64 = core::FilePath(windir + sysnative + cmd);
            ps32 = core::FilePath(windir + sys32 + ps);
            ps64 = core::FilePath(windir + sysnative + ps);
            bashWSL = core::FilePath(windir + sysnative + bash);
         }
      }
      else // 32-bit windows
      {
         cmd32 = core::FilePath(windir + sys32 + cmd);
         ps32 = core::FilePath(windir + sys32 + ps);
      }

      addShell(cmd32, TerminalShell::Cmd32, "Command Prompt (32-bit)", args, pShells);
      addShell(cmd64, TerminalShell::Cmd64, "Command Prompt (64-bit)", args, pShells);
      addShell(ps32, TerminalShell::PS32, "Windows PowerShell (32-bit)", args, pShells);
      addShell(ps64, TerminalShell::PS64, "Windows PowerShell (64-bit)", args, pShells);

      // Is there a better way to detect WSL? This will match any 64-bit
      // bash.exe found in same location as the WSL bash.
      if (core::system::isWin64())
      {
         addShell(bashWSL, TerminalShell::WSLBash,
                  "Bash (Windows Subsystem for Linux)", args, pShells);
      }
   }
#endif

   // If nothing found add standard shell (%comspec% on Windows, Bash on Posix)
   if (pShells->empty())
   {
      TerminalShell sysShell;
      if (AvailableTerminalShells::getSystemShell(&sysShell))
      {
         addShell(sysShell.path, sysShell.type, sysShell.name, sysShell.args, pShells);
      }
   }
}

} // anonymous namespace

core::json::Object TerminalShell::toJson() const
{
   core::json::Object resultJson;
   resultJson["type"] = type;
   resultJson["name"] = name;
   return resultJson;
}

AvailableTerminalShells::AvailableTerminalShells()
{
   scanAvailableShells(&shells_);
}

void AvailableTerminalShells::toJson(core::json::Array* pArray) const
{
   BOOST_FOREACH(const TerminalShell& shell, shells_)
   {
      pArray->push_back(shell.toJson());
   }
}

bool AvailableTerminalShells::getInfo(TerminalShell::TerminalShellType type,
                                      TerminalShell* pShellInfo) const
{
   if (type == TerminalShell::DefaultShell)
   {
      type = userSettings().defaultTerminalShellValue();
      if (type == TerminalShell::DefaultShell)
      {
         // Preference never set; pick first one available
         if (!shells_.empty())
         {
            type = shells_.at(0).type;
         }
      }
   }
   BOOST_FOREACH(const TerminalShell& shell, shells_)
   {
      if (shell.type == type)
      {
         *pShellInfo = shell;
         return true;
      }
   }
   return false;
}

core::FilePath getGitBashShell()
{
   core::FilePath gitExePath = modules::git::detectedGitExePath();
   if (!gitExePath.empty())
   {
      core::FilePath gitBashPath =
            gitExePath.parent().parent().complete("usr/bin/bash.exe");
      if (gitBashPath.exists())
         return gitBashPath;
      else
         return core::FilePath();
   }
   else
       return core::FilePath();
}

bool AvailableTerminalShells::getSystemShell(TerminalShell* pShellInfo)
{
#ifdef _WIN32
   pShellInfo->path = core::system::expandComSpec();
   if (core::system::isWin64())
   {
      pShellInfo->type = TerminalShell::Cmd64;
      pShellInfo->name = "Command Prompt (64-bit)";
   }
   else
   {
      pShellInfo->type = TerminalShell::Cmd32;
      pShellInfo->name = "Command Prompt (32-bit)";
   }
#else
   pShellInfo->name = "Bash";
   pShellInfo->type = TerminalShell::PosixBash;
   pShellInfo->path = core::FilePath("/usr/bin/env");
   pShellInfo->args.push_back("bash");
   pShellInfo->args.push_back("-l"); // act like a login shell
#endif
   return true;
}

} // namespace console_process
} // namespace session
} // namespace rstudio

