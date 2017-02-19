/*
 * Win32PtyTests.cpp
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

#ifdef _WIN32

#include "Win32Pty.hpp"

#define RSTUDIO_NO_TESTTHAT_ALIASES
#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

TEST_CASE("Win32PtyTests")
{
   WinPty pty;

   const UINT64 kAgentFlags = 0x0000;
   const int kMouseMode = WINPTY_MOUSE_MODE_AUTO;
   const int kCols = 80;
   const int kRows = 25;
   const DWORD kTimeoutMs = 500;

   SECTION("Agent not running")
   {
      CHECK_FALSE(pty.agentRunning());
   }

   SECTION("Stopping agent when not running is ok")
   {
      CHECK_FALSE(pty.agentRunning());
      pty.stopAgent();
      CHECK_FALSE(pty.agentRunning());
   }

   SECTION("Start agent")
   {
      CHECK_FALSE(pty.agentRunning());
      pty.startAgent(kAgentFlags, kCols, kRows, kMouseMode, kTimeoutMs);
      CHECK(pty.agentRunning());
   }

   SECTION("Start and Stop agent")
   {
      pty.startAgent(kAgentFlags, kCols, kRows, kMouseMode, kTimeoutMs);
      CHECK(pty.agentRunning());
      pty.stopAgent();
      CHECK_FALSE(pty.agentRunning());
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // _WIN32
