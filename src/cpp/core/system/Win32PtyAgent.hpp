/*
 * Win32PtyAgent.hpp
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

#ifndef CORE_SYSTEM_WIN32PTYAGENT_HPP
#define CORE_SYSTEM_WIN32PTYAGENT_HPP

#include "Win32Terminal.hpp"

namespace rstudio {
namespace core {
namespace system {

/**
 * Pseudo-terminal for Windows.
 *
 * Uses either WinPty (https://github.com/rprichard/winpty), or when available, the native
 * Windows ConPTY.
 *
 * By default will detect which flavor to use, preferring ConPTY, but can force use of
 * WinPty by calling setForceWinPty() before start().
 */
class WinPtyAgent : public WinTerminal
{
 public:
   virtual ~WinPtyAgent() = default;

   void setForceWinPty();
   bool usingConPty() const;
   static Error writeToPty(HANDLE hPipe, const std::string& input);
   static Error readFromPty(HANDLE hPipe, std::string* pOutput);

   // WinTerminal interface
   Error start(const std::string& exe,
               std::vector<std::string> args,
               const ProcessOptions& options,
               HANDLE* pStdInWrite,
               HANDLE* pStdOutRead,
               HANDLE* pStdErrRead,
               HANDLE* pProcess) override;
   bool ptyRunning() const override;
   Error setSize(int cols, int rows) override;
   Error interrupt() override;

 private:
   bool usingConPty_ = true;
   std::unique_ptr<WinTerminal> pPty_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_WIN32PTYAGENT_HPP