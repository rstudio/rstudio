/*
 * SessionTerminalShell.hpp
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

#ifndef SESSION_MODULES_TERMINAL_SHELL_HPP
#define SESSION_MODULES_TERMINAL_SHELL_HPP

#include <vector>

#include <core/json/JsonRpc.hpp>
#include <core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace console_process {

// Information describing one shell type for embedded interactive terminal
struct TerminalShell
{
   // Identifiers for discovered shell options; for Windows there are several
   // possibilities, depending on what's installed, and the bit-ness of the
   // OS. For all others, only bash is supported.
   enum TerminalShellType
   {
      DefaultShell = 0, // Selected by user

      GitBash      = 1, // Win32: Bash from Windows Git
      WSLBash      = 2, // Win32: Windows Services for Linux (64-bit Windows-10 only)
      Cmd32        = 3, // Win32: Windows command shell (32-bit)
      Cmd64        = 4, // Win32: Windows command shell (64-bit)
      PS32         = 5, // Win32: PowerShell (32-bit)
      PS64         = 6, // Win32: PowerShell (64-bit)

      PosixBash    = 7, // Posix: Bash

      Max          = PosixBash
   };

   TerminalShell()
      :
        type(DefaultShell)
   {}

   TerminalShell(
         TerminalShellType type,
         std::string name,
         core::FilePath path,
         std::vector<std::string> args)
      :
        type(type),
        name(name),
        path(path),
        args(args)
   {}

   TerminalShellType type;
   std::string name;
   core::FilePath path;
   std::vector<std::string> args;

   core::json::Object toJson() const;
};

class AvailableTerminalShells
{
public:
   AvailableTerminalShells();

   // JSON encode list of available types
   void toJson(core::json::Array* pArray) const;

   // Get details on one type; returns false if type not available
   bool getInfo(TerminalShell::TerminalShellType type, TerminalShell* pShellInfo) const;

   // Number of available shells (including pseudo-shell "default")
   inline size_t count() const { return shells_.size(); }

   // Get a standard system shell
   static bool getSystemShell(TerminalShell* pShellInfo);

private:
   std::vector<TerminalShell> shells_;
};

// If we are using git bash then return its path
core::FilePath getGitBashShell();

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_TERMINAL_SHELL_HPP
