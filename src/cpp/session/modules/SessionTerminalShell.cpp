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

#include "SessionTerminalShell.hpp"

#include <boost/foreach.hpp>

#include <core/system/System.hpp>
#include <core/Log.hpp>
#include <core/Error.hpp>

#include <session/SessionUserSettings.hpp>
#include "SessionGit.hpp"

namespace rstudio {
namespace session {
namespace modules { 
namespace workbench {

namespace {

#ifdef _WIN32
void addShell(const core::FilePath& expectedPath,
              TerminalShell::TerminalShellType type,
              const std::string& title,
              std::vector<TerminalShell>* pShells)
{
   if (expectedPath.exists())
      pShells->push_back(TerminalShell(type, title, expectedPath));
}

void addShell(const std::string& expectedPath,
              TerminalShell::TerminalShellType type,
              const std::string& title,
              std::vector<TerminalShell>* pShells)
{
   addShell(core::FilePath(expectedPath), type, title, pShells);
}
#endif

void scanAvailableShells(std::vector<TerminalShell>* pShells)
{
   pShells->push_back(TerminalShell());

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

   if (!core::system::isVistaOrLater())
   {
      // Below Vista, just use %comspec%
      addShell(core::system::expandComSpec(),
               TerminalShell::Cmd32, "Command Prompt", pShells);
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

      std::string cmd32;
      std::string cmd64;
      std::string ps32;
      std::string ps64;
      std::string bashWSL; // Windows Subsystem for Linux

      if (core::system::isWin64())
      {
         if (core::system::isCurrentProcessWin64())
         {
            cmd32 = windir + wow64 + cmd;
            cmd64 = windir + sys32 + cmd;
            ps32 = windir + wow64 + ps;
            ps64 = windir + sys32 + ps;
            bashWSL = windir + sys32 + bash;
         }
         else
         {
            cmd32 = windir + sys32 + cmd;
            cmd64 = windir + sysnative + cmd;
            ps32 = windir + sys32 + ps;
            ps64 = windir + sysnative + ps;
            bashWSL = windir + sysnative + bash;
         }
      }
      else // 32-bit windows
      {
         cmd32 = windir + sys32 + cmd;
         cmd64.clear();
         ps32 = windir + sys32 + ps;
         ps64.clear();
         bashWSL.clear();
      }

      addShell(cmd32, TerminalShell::Cmd32, "Command Prompt (32-bit)", pShells);
      addShell(cmd64, TerminalShell::Cmd64, "Command Prompt (64-bit)", pShells);
      addShell(ps32, TerminalShell::PS32, "Windows PowerShell (32-bit)", pShells);
      addShell(ps64, TerminalShell::PS64, "Windows PowerShell (64-bit)", pShells);

      if (core::system::isWin10OrLater())
      {
         // Is there a better way to detect WSL? This will treat any 64-bit
         // bash.exe found in same location as the WSL bash.
         addShell(bashWSL, TerminalShell::WSLBash,
                  "Bash (Windows Subsystem for Linux)", pShells);
      }

      addShell(getGitBashShell(), TerminalShell::GitBash, "Git Bash", pShells);
   }

   // If nothing found try to add %comspec% so there's something
   // available on Windows.
   if (pShells->size() < 2)
   {
      addShell(core::system::expandComSpec(),
               TerminalShell::Cmd32, "Command Prompt", pShells);
   }

#endif
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
   core::FilePath gitExePath = git::detectedGitExePath();
   if (!gitExePath.empty())
      return gitExePath.parent().childPath("sh.exe");
   else
       return core::FilePath();
}

} // namespace workbench
} // namespace modules
} // namesapce session
} // namespace rstudio

