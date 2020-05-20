/*
 * SessionTerminalShell.cpp
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

#include <session/SessionTerminalShell.hpp>

#include <core/Algorithm.hpp>
#include <core/system/System.hpp>
#include <core/StringUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include "SessionGit.hpp"
#include <session/prefs/UserPrefs.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileUtils.hpp>

namespace rstudio {
namespace session {
namespace console_process {

namespace {

void addShell(const core::FilePath& expectedPath,
              TerminalShell::ShellType type,
              const std::string& title,
              std::vector<std::string> args,
              std::vector<TerminalShell>* pShells,
              bool checkPathExists = true)
{
   if (!checkPathExists || expectedPath.exists())
      pShells->push_back(TerminalShell(type, title, expectedPath, args));
}

void scanPosixShells(std::vector<TerminalShell>* pShells)
{
   bool foundZsh = false;
   core::FilePath shellsFile("/etc/shells");
   if (shellsFile.exists())
   {
      std::string shells = core::file_utils::readFile(shellsFile);
      std::vector<std::string> lines;
      boost::algorithm::split(lines, shells, boost::algorithm::is_any_of("\n"));
      for (const std::string& line : lines)
      {
         std::string trimmedLine = core::string_utils::trimWhitespace(line);

         // skip comments and empty lines
         if (trimmedLine.empty() || (trimmedLine.size() > 0 && trimmedLine.at(0) == '#'))
            continue;

         if (!foundZsh && boost::algorithm::ends_with(trimmedLine, "/zsh"))
         {
            foundZsh = true;
            std::vector<std::string> args;
            args.emplace_back("-l"); // act like a login shell
            args.emplace_back("-g"); // don't add commands with leading space to history
            addShell(core::FilePath(trimmedLine), TerminalShell::ShellType::PosixZsh,
                     "Zsh", args, pShells);
         }
      }
   }
}

void scanAvailableShells(std::vector<TerminalShell>* pShells)
{
#ifdef _WIN32
   std::vector<std::string> args;

   const std::string sys32("system32\\");
   const std::string cmd("cmd.exe");
   const std::string ps("WindowsPowerShell\\v1.0\\powershell.exe");
   const std::string bash("bash.exe");
   const std::string pscore("PowerShell\\6\\pwsh.exe");

   std::string windir;
   core::Error err = core::system::expandEnvironmentVariables("%windir%\\", &windir);
   if (err)
   {
      LOG_ERROR(err);
      return;
   }
   std::string progfiles;
   err = core::system::expandEnvironmentVariables("%ProgramFiles%\\", &progfiles);
   if (err)
   {
      LOG_ERROR(err);
      return;
   }

   addShell(getGitBashShell(), TerminalShell::ShellType::GitBash, "Git Bash", args, pShells);

   core::FilePath cmd64 = core::FilePath(windir + sys32 + cmd);
   core::FilePath ps64 = core::FilePath(windir + sys32 + ps);
   core::FilePath bashWSL = core::FilePath(windir + sys32 + bash);
   core::FilePath psCore = core::FilePath(progfiles + pscore);

   addShell(cmd64, TerminalShell::ShellType::Cmd64, "Command Prompt", args, pShells);
   addShell(ps64, TerminalShell::ShellType::PS64, "Windows PowerShell", args, pShells);
   addShell(psCore, TerminalShell::ShellType::PSCore, "PowerShell Core", args, pShells);

   // Is there a better way to detect WSL? This will match any 64-bit
   // bash.exe found in same location as the WSL bash.
   addShell(bashWSL, TerminalShell::ShellType::WSLBash,
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

   // Additional Posix Shells
#ifndef _WIN32
   scanPosixShells(pShells);
#endif

   // Add user-selectable shell command option
   TerminalShell customShell;
   if (AvailableTerminalShells::getCustomShell(&customShell))
   {
      addShell(customShell.path, customShell.type, customShell.name,
               customShell.args, pShells,
               false /*checkPathExists*/);
   }
}

} // anonymous namespace

core::json::Object TerminalShell::toJson() const
{
   core::json::Object resultJson;
   resultJson["type"] = getShellId(type);
   resultJson["name"] = name;
   return resultJson;
}

// keep in sync with TerminalShellInfo::getShellName in client code
std::string TerminalShell::getShellName(ShellType type)
{
   switch (type)
   {
   case ShellType::Default:
      return "Default";
   case ShellType::GitBash:
      return "Git Bash";
   case ShellType::WSLBash:
      return "WSL";
   case ShellType::Cmd32:
   case ShellType::Cmd64:
      return "Command Prompt";
   case ShellType::PS32:
   case ShellType::PS64:
      return "PowerShell";
   case ShellType::PSCore:
      return "PowerShell Core";
   case ShellType::PosixBash:
      return "Bash";
   case ShellType::CustomShell:
      return "Custom";
   case ShellType::NoShell:
      return "User command";
   case ShellType::PosixZsh:
      return "Zsh";
   }
   return "Unknown";
}

// keep in sync with values supported by terminalCreate "shellType" argument in 
// rstudioapi and rs_terminalCreate
TerminalShell::ShellType TerminalShell::shellTypeFromString(const std::string& str)
{
   std::string typeStr = core::string_utils::toLower(str);
   if (typeStr == kWindowsTerminalShellWinCmd)
   {
      return TerminalShell::ShellType::Cmd64;
   }
   else if (typeStr == kWindowsTerminalShellWinPs)
   {
      return TerminalShell::ShellType::PS64;
   }
   else if (typeStr == kWindowsTerminalShellPsCore)
   {
      return TerminalShell::ShellType::PSCore;
   }
   else if (typeStr == kWindowsTerminalShellWinGitBash)
   {
      return TerminalShell::ShellType::GitBash;
   }
   else if (typeStr == kWindowsTerminalShellWinWslBash)
   {
      return TerminalShell::ShellType::WSLBash;
   }
   else if (typeStr == kWindowsTerminalShellCustom)
   {
      return TerminalShell::ShellType::CustomShell;
   }
   else if (typeStr == kPosixTerminalShellZsh)
   {
      return TerminalShell::ShellType::PosixZsh;
   }
   else // implicitly includes "default"
   {
      return TerminalShell::ShellType::Default;
   }
}

std::string TerminalShell::getShellId(ShellType type)
{
   switch(type)
   {
      case TerminalShell::ShellType::Default:
         return kWindowsTerminalShellDefault;
      case TerminalShell::ShellType::Cmd32:
      case TerminalShell::ShellType::Cmd64:
         return kWindowsTerminalShellWinCmd;
      case TerminalShell::ShellType::PS32:
      case TerminalShell::ShellType::PS64:
         return kWindowsTerminalShellWinPs;
      case TerminalShell::ShellType::PSCore:
         return kWindowsTerminalShellPsCore;
      case TerminalShell::ShellType::GitBash:
         return kWindowsTerminalShellWinGitBash;
      case TerminalShell::ShellType::WSLBash:
         return kWindowsTerminalShellWinWslBash;
      case TerminalShell::ShellType::CustomShell:
         return kWindowsTerminalShellCustom;
      case TerminalShell::ShellType::PosixBash:
         return kPosixTerminalShellBash;
      case TerminalShell::ShellType::NoShell:
         return kPosixTerminalShellNone;
      case TerminalShell::ShellType::PosixZsh:
         return kPosixTerminalShellZsh;
   }
   return kWindowsTerminalShellDefault;
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

bool AvailableTerminalShells::getInfo(TerminalShell::ShellType type,
                                      TerminalShell* pShellInfo) const
{
   if (type == TerminalShell::ShellType::Default)
   {
      type = prefs::userPrefs().defaultTerminalShellValue();
      if (type == TerminalShell::ShellType::Default)
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
   if (!gitExePath.isEmpty())
   {
      // Prefer git-for-windows bash wrapper; it does some setup before it invokes MSYS2's
      // usr/bin/bash.exe; not using this wrapper causes problems if other Cygwin-based
      // Posix environments are on path (such as those in RTools4)
      core::FilePath gitBashPath = gitExePath.getParent().completePath("bash.exe");
      if (gitBashPath.exists())
         return gitBashPath;

      // fall back to old detection behavior in case there's an older version that only
      // has this installation pattern
      gitBashPath = gitExePath.getParent().getParent().completePath("usr/bin/bash.exe");
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
   pShellInfo->type = TerminalShell::ShellType::Cmd64;
   pShellInfo->name = "Command Prompt";
#else
   pShellInfo->name = "Bash";
   pShellInfo->type = TerminalShell::ShellType::PosixBash;
   pShellInfo->path = core::FilePath("/usr/bin/env");
   pShellInfo->args.emplace_back("bash");
   pShellInfo->args.emplace_back("-l"); // act like a login shell
#endif
   return true;
}

bool AvailableTerminalShells::getCustomShell(TerminalShell* pShellInfo)
{
   pShellInfo->name = "Custom";
   pShellInfo->type = TerminalShell::ShellType::CustomShell;
   pShellInfo->path = module_context::resolveAliasedPath(
         prefs::userPrefs().customShellCommand());

   // arguments are space separated, currently no way to represent a literal space
   std::vector<std::string> args;
   if (!prefs::userPrefs().customShellOptions().empty())
   {
      args = core::algorithm::split(prefs::userPrefs().customShellOptions(), " ");
   }
   pShellInfo->args = args;
   return true;
}

} // namespace console_process
} // namespace session
} // namespace rstudio

