/*
 * SessionSuspend.cpp
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

#include <cstdlib>
#include <signal.h>

#include "SessionSuspend.hpp"
#include "SessionConsoleInput.hpp"

#include <session/SessionConstants.hpp>
#include <session/SessionHttpConnectionListener.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionConstants.hpp>

#include <shared_core/json/Json.hpp>

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

// keep track of what operations are blocking session from suspending
enum SuspendTimeoutState
{
   kWaitingForTimeout,
   kWaitingForNonBlocking
};

std::vector<bool> opsBlockingSuspend(kBlockingOpsCount, false);
boost::posix_time::ptime s_suspendTimeoutTime = boost::posix_time::second_clock::universal_time();
bool s_dirtyBlockingOps = false;
SuspendTimeoutState s_timeoutState = kWaitingForTimeout;

} // anonymous namespace

// convenience function for disallowing suspend (note still doesn't override
// the presence of s_forceSuspend = 1)
bool disallowSuspend()
{
   return false;
}

boost::posix_time::ptime timeoutTimeFromNow()
{
   int timeoutMinutes = options().timeoutMinutes();
   if (timeoutMinutes > 0)
   {
      return boost::posix_time::second_clock::universal_time() +
             boost::posix_time::minutes(options().timeoutMinutes());
   }
   else
   {
      return boost::posix_time::ptime(boost::posix_time::not_a_date_time);
   }
}

bool isTimedOut(const boost::posix_time::ptime& timeoutTime)
{
   using namespace boost::posix_time;

   // never time out in desktop mode
   if (options().programMode() == kSessionProgramModeDesktop)
      return false;

   // check for an client disconnection based timeout
   int disconnectedTimeoutMinutes = options().disconnectedTimeoutMinutes();
   if (disconnectedTimeoutMinutes > 0)
   {
      ptime lastEventConnection =
         httpConnectionListener().eventsConnectionQueue().lastConnectionTime();
      if (!lastEventConnection.is_not_a_date_time())
      {
         if ( (lastEventConnection + minutes(disconnectedTimeoutMinutes)
               < second_clock::universal_time()) )
         {
            return true;
         }
      }
   }

   // check for a foreground inactivity based timeout
   if (timeoutTime.is_not_a_date_time())
      return false;
   else
      return second_clock::universal_time() > timeoutTime;
}

void resetSuspendTimeout()
{
   s_suspendTimeoutTime = timeoutTimeFromNow();
}

core::json::Object blockingOpsToJson()
{
   core::json::Object blockingOps;

   if (opsBlockingSuspend[SuspendBlockingOps::kChildProcess])
      blockingOps.insert("active-child-process", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kExecuting])
      blockingOps.insert("executing", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kConnection])
      blockingOps.insert("active-connection", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kOverlay])
      blockingOps.insert("overlay", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kExternalPointer])
      blockingOps.insert("active-external-pointer", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kActiveJob])
      blockingOps.insert("active-job", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kCommandPrompt])
      blockingOps.insert("incomplete-command-prompt", core::json::Array());

   if (opsBlockingSuspend[SuspendBlockingOps::kWaitingForEditCompletion])
      blockingOps.insert("edit-completion", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kWaitingForChooseFileCompletion])
      blockingOps.insert("choose-file-completion", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kWaitingForLocatorCompletion])
      blockingOps.insert("locator-completion", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kWaitingForUnsavedHandlerCompletion])
      blockingOps.insert("unsaved-handler-completion", core::json::Array());
   if (opsBlockingSuspend[SuspendBlockingOps::kWaitingForUserPromptCompletion])
      blockingOps.insert("user-prompt-completion", core::json::Array());

   return blockingOps;
}

void storeBlockingOps(bool force = false)
{
   if (s_dirtyBlockingOps || force)
   {
      core::json::Object blockingOps = blockingOpsToJson();
      module_context::activeSession().setBlockingSuspend(blockingOps);
      module_context::enqueClientEvent(ClientEvent(rstudio::session::client_events::kSuspendBlocked, blockingOps));
      s_dirtyBlockingOps = false;
   }
}

void addBlockingOp(SuspendBlockingOps op)
{
   // If we're already tracking this op, nothing to do
   if (opsBlockingSuspend[op])
      return;

   opsBlockingSuspend[op] = true;
   s_dirtyBlockingOps = true;
}

void removeBlockingOp(SuspendBlockingOps op)
{
   if (!opsBlockingSuspend[op])
      return;

   opsBlockingSuspend[op] = false;
   s_dirtyBlockingOps = true;
}

/**
 * @brief Check if we're already tracking a blocking operation and update as
 * necessary
 *
 * @param blocking True if this operation is blocking suspension. False otherwise
 * @param op The operation type that's blocking session suspension
 *
 * @return The original blocking value, as a convenience passthrough value
 */
bool checkBlockingOp(bool blocking, SuspendBlockingOps op)
{
   if (opsBlockingSuspend[op] != blocking)
   {
      opsBlockingSuspend[op] = blocking;
      s_dirtyBlockingOps = true;
   }

   return blocking;
}

/**
 * @brief Clears any tracking of blocking ops
 *
 * @param store Also stores the cleared results if set to true
 */
void clearBlockingOps(bool store)
{
   std::fill(opsBlockingSuspend.begin(), opsBlockingSuspend.end(), false);
   if (store)
   {
      // set to ensure blank info is written
      storeBlockingOps(true);
   }

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

   // If we're suspending then clear list of blocking ops
   clearBlockingOps(true);
   s_timeoutState = kWaitingForTimeout;

   // perform the suspend (does not return if successful)
   return r::session::suspend(force, status, session::options().ephemeralEnvVars());
}

void checkForTimeout(const boost::function<bool()>& allowSuspend)
{
   bool canSuspend = allowSuspend();

   // If there's no suspend-blocking activity, just wait
   // for the inactivity timer to expire
   if (s_timeoutState == kWaitingForTimeout)
   {
      if (canSuspend)
      {
         if (isTimedOut(s_suspendTimeoutTime))
         {
            // note that we timed out
            suspend::setSuspendedFromTimeout(true);

            if (!options().timeoutSuspend())
            {
               // configuration dictates that we should quit the
               // session instead of suspending when timeout occurs
               //
               // the conditions for the quit must be the same as those
               // for a regular suspend
               rstudio::r::session::quit(false, EXIT_SUCCESS); // does not return
            }

            // attempt to suspend (does not return if it succeeds)
            if (!suspend::suspendSession(false))
            {
               // reset timeout flag
               suspend::setSuspendedFromTimeout(false);

               // if it fails then reset the timeout timer so we don't keep
               // hammering away on the failure case
               resetSuspendTimeout();
               s_timeoutState = kWaitingForTimeout;
            }
         }
      }
      else
      {
         s_timeoutState = kWaitingForNonBlocking;
      }
   }

   // If there is suspend-blocking activity, wait for it to end while also
   // notifying user that the session is blocked from suspending
   if (s_timeoutState == kWaitingForNonBlocking)
   {
      if (canSuspend)
      {
         // reset the inactivity timeout before switching waiting modes
         s_timeoutState = kWaitingForTimeout;
         resetSuspendTimeout();
      }

      storeBlockingOps();
   }
}

void checkForSuspend(const boost::function<bool()>& allowSuspend)
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

   // timeout suspend
   checkForTimeout(allowSuspend);
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

   bool isLauncherSession = options().getBoolOverlayOption(kLauncherSessionOption);

   if (!isLauncherSession || console_input::executing())
   {
      // interrupt R
      // for launcher sessions, we only want to interrupt user code to fix
      // an excess logging issue caused by interrupting RStudio code that
      // takes longer to execute in docker containers
      // when not in launcher session mode, always interrupt
      rstudio::r::exec::setInterruptsPending(true);
   }

   // note that a suspend is being forced. 
   s_forceSuspend = 1;
}

} // namespace suspend
} // namespace session
} // namespace rstudio
