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

#include <winpty/winpty.h>

#include <string>

#include <boost/noncopyable.hpp>

namespace rstudio {
namespace core {
namespace system {

// Wrapper class for winpty library (https://github.com/rprichard/winpty)
// "A windows software package providing an interface similar to a Unix
// pty-master for communicating with Windows console programs.
class WinPty : boost::noncopyable
{
public:
   WinPty()
      : pPty_(NULL)
   {}

   virtual ~WinPty();

   // Start the winpty agent process (safe to call if already running).
   // Returns true if successful.
   bool startAgent(UINT64 agentFlags,
                   int cols, int rows,
                   int mousemode,
                   DWORD timeoutMs);

   // Stop the winpty agent process (safe to call if it is not running).
   void stopAgent();

   // Is the agent currently running?
   bool agentRunning() const;

private:
   winpty_t *pPty_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_WIN32PTY_HPP
