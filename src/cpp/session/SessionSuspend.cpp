/*
 * SessionSuspend.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <cstdlib>
#include <signal.h>

#include "SessionSuspend.hpp"
#include "SessionConsoleInput.hpp"

#include <session/SessionConstants.hpp>
#include <session/SessionOptions.hpp>

#include <r/session/RSession.hpp>
#include <r/RExec.hpp>

namespace rstudio {
namespace session {
namespace suspend {

namespace {

// request suspends (cooperative and forced) using interrupts
volatile sig_atomic_t s_suspendRequested = 0;
volatile sig_atomic_t s_forceSuspend = 0;
volatile sig_atomic_t s_forceSuspendInterruptedR = 0;
bool s_suspendedFromTimeout = false;

// was the underlying r session resumed
bool s_rSessionResumed = false;

} // anonymous namespace

// convenience function for disallowing suspend (note still doesn't override
// the presence of s_forceSuspend = 1)
bool disallowSuspend() 
{ 
   return false; 
}

bool sessionResumed()
{
   return s_rSessionResumed;
}

void setSessionResumed(bool resumed)
{
   s_rSessionResumed = resumed;
}

bool suspendedFromTimeout()
{
   return s_suspendedFromTimeout;
}

void setSuspendedFromTimeout(bool suspended)
{
   s_suspendedFromTimeout = suspended;
}
   
bool suspendSession(bool force, int status)
{
   // need to make sure the global environment is loaded before we
   // attemmpt to save it!
   r::session::ensureDeserialized();

   // perform the suspend (does not return if successful)
   return r::session::suspend(force, status);
}

void suspendIfRequested(const boost::function<bool()>& allowSuspend)
{
   // never suspend in desktop mode
   if (options().programMode() == kSessionProgramModeDesktop)
      return;

   // check for forced suspend request
   if (s_forceSuspend)
   {
      // reset flag (if for any reason we fail we don't want to keep
      // hammering away on the failure case)
      s_forceSuspend = false;

      // did this force suspend interrupt R?
      if (s_forceSuspendInterruptedR)
      {
         // reset flag
         s_forceSuspendInterruptedR = false;

         // notify user
         rstudio::r::session::reportAndLogWarning(
            "Session forced to suspend due to system upgrade, restart, maintenance, "
            "or other issue. Your session data was saved however running "
            "computations may have been interrupted.");
      }

      // execute the forced suspend (does not return)
      suspendSession(true, EX_FORCE);
   }

   // cooperative suspend request
   else if (s_suspendRequested && allowSuspend())
   {
      // reset flag (if for any reason we fail we don't want to keep
      // hammering away on the failure case)
      s_suspendRequested = false;

      // attempt suspend -- if this succeeds it doesn't return; if it fails
      // errors will be logged/reported internally and we will move on
      suspendSession(false);
   }
}

// cooperative suspend -- the http server is forced to timeout and a flag 
// indicating that we should suspend at ourfirst valid opportunity is set
void handleUSR1(int unused)
{   
   // note that a suspend has been requested. the process will suspend
   // at the first instance that it is valid for it to do so 
   s_suspendRequested = 1;
}

// forced suspend -- R is interrupted, the http server is forced to timeout,
// and the 'force' flag is set
void handleUSR2(int unused)
{
   // note whether R was interrupted
   if (console_input::executing())
      s_forceSuspendInterruptedR = 1;

   // set the r interrupt flag (always)
   rstudio::r::exec::setInterruptsPending(true);

   // note that a suspend is being forced. 
   s_forceSuspend = 1;
}

} // namespace suspend
} // namespace session
} // namespace rstudio
