/*
 * Win32Pty.hpp
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

#ifndef CORE_SYSTEM_WIN32PTY_HPP
#define CORE_SYSTEM_WIN32PTY_HPP

#include <string>

#include <boost/noncopyable.hpp>

#include <core/system/Process.hpp>
#include <core/Error.hpp>
#include <core/Log.hpp>

#include <winpty/winpty.h>

namespace rstudio {
namespace core {
namespace system {

// Wrapper class for winpty library (https://github.com/rprichard/winpty)
// "A windows software package providing an interface similar to a Unix
// pty-master for communicating with Windows console programs."
class WinPty : boost::noncopyable
{
public:
   WinPty()
      : pPty_(NULL)
   {}

   virtual ~WinPty();

   void init(const std::string& exe,
             const std::vector<std::string> args,
             const ProcessOptions& options);

   // Start the pty and return HANDLEs. On success, caller is responsible for
   // closing returned HANDLEs; on failure, all returned HANDLEs will
   // contain NULL.
   Error startPty(HANDLE* pStdInWrite,
                  HANDLE* pStdOutRead,
                  HANDLE* pStdErrRead = NULL /*OPTIONAL*/);
   bool ptyRunning() const;

   // Start the process specified by init(); it will do I/O via the handles
   // returned by startPty. On success, caller is responsible for closing
   // returned HANDLE. On failure, pProcess will contain NULL.
   Error runProcess(HANDLE* pProcess = NULL /*OPTIONAL*/);

   // Change the size of the pseudoterminal
   Error setSize(int cols, int rows);

   // Send interrupt (Ctrl+C)
   Error interrupt();

   static Error writeToPty(HANDLE hPipe, const std::string& input);
   static Error readFromPty(HANDLE hPipe, std::string* pOutput);

private:
   void stopPty();

private:
   winpty_t *pPty_;
   std::string exe_;
   std::vector<std::string> args_;
   ProcessOptions options_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_WIN32PTY_HPP
