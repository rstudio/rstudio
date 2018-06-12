/*
 * SessionTerminalShell.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <core/Algorithm.hpp>
#include <core/system/System.hpp>
#include <core/StringUtils.hpp>

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
              std::vector<TerminalShell>* pShells,
              bool checkPathExists = true)
{
   if (!checkPathExists || expectedPath.exists())
      pShells->push_back(TerminalShell(type, title, expectedPath, args));
}

void scanAvailableShells(std::vector<TerminalShell>* pShells)
{
#ifdef _WIN32
   std::vector<std::string> args;

   const std::string sys32("system32\\");
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

   core::FilePath cmd64;
   core::FilePath ps64;
   core::FilePath bashWSL; // Windows Subsystem for Linux

   cmd64 = core::FilePath(windir + sys32 + cmd);
   ps64 = core::FilePath(windir + sys32 + ps);
   bashWSL = core::FilePath(windir + sys32 + bash);

   addShell(cmd64, TerminalShell::Cmd64, "Command Prompt", args, pShells);
   addShell(ps64, TerminalShell::PS64, "Windows PowerShell", args, pShells);

   // Is there a better way to detect WSL? This will match any 64-bit
   // bash.exe found in same location as the WSL bash.
   addShell(bashWSL, TerminalShell::WSLBash,
            "Bash (Windows Subsystem for Linux)", args, pShells);
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

#ifndef _WIN32
   // Add user-selectable shell command option
   TerminalShell customShell;
   if (AvailableTerminalShells::getCustomShell(&customShell))
   {
      addShell(customShell.path, customShell.type, customShell.name,
               customShell.args, pShells,
               false /*checkPathExists*/);
   }
#endif // !_WIN32
}

} // anonymous namespace

core::json::Object TerminalShell::toJson() const
{
   core::json::Object resultJson;
   resultJson["type"] = type;
   resultJson["name"] = name;
   return resultJson;
}

// keep in sync with TerminalShellInfo::getShellName in client code
std::string TerminalShell::getShellName(TerminalShellType type)
{
   switch (type)
   {
   case DefaultShell:
      return "Default";
   case GitBash:
      return "Git Bash";
   case WSLBash:
      return "WSL";
   case Cmd32:
      return "Command Prompt (32-bit)";
   case Cmd64:
      return "Command Prompt (64-bit)";
   case PS32:
      return "PowerShell (32-bit)";
   case PS64:
      return "PowerShell (64-bit)";
   case PosixBash:
      return "Bash";
   case CustomShell:
      return "Custom";
   case NoShell:
      return "User command";
   default:
      return "Unknown";
   }
}

// keep in sync with values supported by terminalCreate "shellType" argument in 
// rstudioapi and rs_terminalCreate
TerminalShell::TerminalShellType TerminalShell::shellTypeFromString(const std::string& str)
{
   std::string typeStr = core::string_utils::toLower(str);
   if (typeStr == "win-cmd")
   {
      return TerminalShell::TerminalShellType::Cmd64;
   }
   else if (typeStr == "win-ps")
   {
      return TerminalShell::TerminalShellType::PS64;
   }
   else if (typeStr == "win-git-bash")
   {
      return TerminalShell::TerminalShellType::GitBash;
   }
   else if (typeStr == "win-wsl-bash")
   {
      return TerminalShell::TerminalShellType::WSLBash;
   }
   else if (typeStr == "custom")
   {
      return TerminalShell::TerminalShellType::CustomShell;
   }
   else // implicity includes "default"
   {
      return TerminalShell::TerminalShellType::DefaultShell;
   }
}

AvailableTerminalShells::AvailableTerminalShells()
{
   scanAvailableShells(&shells_);
}

void AvailableTerminalShells::toJson(core::json::Array* pArray) const
{
   for (const auto& shell : shells_)
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
   for (const auto& shell : shells_)
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
   pShellInfo->type = TerminalShell::Cmd64;
   pShellInfo->name = "Command Prompt";
#else
   pShellInfo->name = "Bash";
   pShellInfo->type = TerminalShell::PosixBash;
   pShellInfo->path = core::FilePath("/usr/bin/env");
   pShellInfo->args.emplace_back("bash");
   pShellInfo->args.emplace_back("-l"); // act like a login shell
#endif
   return true;
}

bool AvailableTerminalShells::getCustomShell(TerminalShell* pShellInfo)
{
   pShellInfo->name = "Custom";
   pShellInfo->type = TerminalShell::CustomShell;
   pShellInfo->path = userSettings().customShellCommand();

   // arguments are space separated, currently no way to represent a literal space
   std::vector<std::string> args;
   if (!userSettings().customShellOptions().empty())
   {
      args = core::algorithm::split(userSettings().customShellOptions(), " ");
   }
   pShellInfo->args = args;
   return true;
}

} // namespace console_process
} // namespace session
} // namespace rstudio

