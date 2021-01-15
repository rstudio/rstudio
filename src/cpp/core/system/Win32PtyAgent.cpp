/*
 * Win32PtyAgent.cpp
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

#include "Win32PtyAgent.hpp"

#include "Win32Pty.hpp"
#include "Win32ConPty.hpp"

namespace rstudio {
namespace core {
namespace system {

void WinPtyAgent::setForceWinPty()
{
   usingConPty_ = false;
}

bool WinPtyAgent::usingConPty() const
{
   return usingConPty_;
}

/*static*/
Error WinPtyAgent::writeToPty(HANDLE hPipe, const std::string& input)
{
   if (input.empty())
      return Success();

   OVERLAPPED over;
   memset(&over, 0, sizeof(over));

   DWORD dwWritten;
   BOOL bSuccess = ::WriteFile(hPipe,
                               input.data(),
                               static_cast<DWORD>(input.length()),
                               &dwWritten,
                               &over);
   auto lastErr = ::GetLastError();
   if (!bSuccess && lastErr == ERROR_IO_PENDING)
   {
      bSuccess = GetOverlappedResult(hPipe,
                                     &over,
                                     &dwWritten,
                                     TRUE /*wait*/);
      lastErr = ::GetLastError();
   }
   if (!bSuccess)
      return systemError(lastErr, ERROR_LOCATION);

   return Success();
}

/*static*/
Error WinPtyAgent::readFromPty(HANDLE hPipe, std::string* pOutput)
{
   // check for available bytes
   DWORD dwAvail = 0;
   if (!::PeekNamedPipe(hPipe, nullptr, 0, nullptr, &dwAvail, nullptr))
   {
      auto lastErr = ::GetLastError();
      if (lastErr == ERROR_BROKEN_PIPE)
         return Success();
      else
         return systemError(lastErr, ERROR_LOCATION);
   }

   // no data available
   if (dwAvail == 0)
      return Success();

   // read data which is available
   DWORD nBytesRead = dwAvail;
   std::vector<CHAR> buffer(dwAvail, 0);
   OVERLAPPED over;
   memset(&over, 0, sizeof(over));
   BOOL bSuccess = ::ReadFile(hPipe, &(buffer[0]), dwAvail, nullptr, &over);
   auto lastErr = ::GetLastError();
   if (!bSuccess && lastErr == ERROR_IO_PENDING)
   {
      bSuccess = GetOverlappedResult(hPipe,
                                     &over,
                                     &nBytesRead,
                                     TRUE /*wait*/);
      lastErr = ::GetLastError();
   }

   if (!bSuccess)
      return systemError(lastErr, ERROR_LOCATION);

   // append to output
   pOutput->append(&(buffer[0]), nBytesRead);

   // success
   return Success();
}

Error WinPtyAgent::start(const std::string& exe,
                         std::vector<std::string> args,
                         const ProcessOptions& options,
                         HANDLE* pStdInWrite,
                         HANDLE* pStdOutRead,
                         HANDLE* pStdErrRead,
                         HANDLE* pProcess)
{
   // Is ConPTY available for use? This horrible check is necessary because ConPTY shipped
   // in some earlier releases of Windows-10 but was too unstable for use. VSCode makes the
   // same check for its terminal via the node.pty package.
   if (usingConPty_ && getWinBuildNumber() < 18309)
      usingConPty_ = false;

   if (usingConPty_)
      pPty_.reset(static_cast<WinTerminal*>(new WinConPty()));
   else
      pPty_.reset(static_cast<WinTerminal*>(new WinPty()));

   return pPty_->start(exe, args, options, pStdInWrite, pStdOutRead, pStdErrRead, pProcess);
}

bool WinPtyAgent::ptyRunning() const
{
   return pPty_->ptyRunning();
}

Error WinPtyAgent::setSize(int cols, int rows)
{
   return pPty_->setSize(cols, rows);
}

Error WinPtyAgent::interrupt()
{
   return pPty_->interrupt();
}

} // namespace system
} // namespace core
} // namespace rstudio