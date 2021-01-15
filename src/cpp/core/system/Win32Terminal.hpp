/*
 * Win32Terminal.hpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#ifndef CORE_SYSTEM_WIN32TERMINAL_HPP
#define CORE_SYSTEM_WIN32TERMINAL_HPP

#include <string>
#include <core/system/Process.hpp>

namespace rstudio {
namespace core {
namespace system {

/**
 * Abstract interface for a psuedo-terminal API on Windows
 *
 * Uses either WinPty (https://github.com/rprichard/winpty), or when available, the native
 * Windows ConPTY. Use WinPtyAgent to get an instance of this interface.
 */
class WinTerminal
{
 public:
   // Start the process specified by exe; it will do I/O via the returned
   // handles. On success, caller is responsible for closing
   // returned handles. On failure, handles will contain nullptr.
   virtual Error start(const std::string& exe,
                       std::vector<std::string> args,
                       const ProcessOptions& options,
                       HANDLE* pStdInWrite,
                       HANDLE* pStdOutRead,
                       HANDLE* pStdErrRead,
                       HANDLE* pProcess) = 0;

   virtual bool ptyRunning() const = 0;

   // Change the size of the pseudoterminal
   virtual Error setSize(int cols, int rows) = 0;

   // Send interrupt (Ctrl+C)
   virtual Error interrupt() = 0;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_WIN32TERMINAL_HPP