/*
 * Win32ConPty.hpp
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
#ifndef CORE_SYSTEM_WIN32CONPTY_HPP
#define CORE_SYSTEM_WIN32CONPTY_HPP

#include "Win32Terminal.hpp"

namespace rstudio {
namespace core {
namespace system {

class WinConPty : public WinTerminal
{
 public:
   WinConPty() {}

   virtual ~WinConPty();

   // Start the process specified by exe; it will do I/O via the returned
   // handles. On success, caller is responsible for closing
   // returned handles. On failure, handles will contain nullptr.
   Error start(const std::string& exe,
               const std::vector<std::string> args,
               const ProcessOptions& options,
               HANDLE* pStdInWrite,
               HANDLE* pStdOutRead,
               HANDLE* pStdErrRead,
               HANDLE* pProcess) override;

   bool ptyRunning() const override;

   // Change the size of the pseudoterminal
   Error setSize(int cols, int rows) override;

   // Send interrupt (Ctrl+C)
   Error interrupt() override;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_WIN32CONPTY_HPP