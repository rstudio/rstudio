/*
 * SessionSuspend.cpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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
#include <unordered_set>

#include "SessionConsoleInput.hpp"

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionConstants.hpp>
#include <session/SessionHttpConnectionListener.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionPersistentState.hpp>
#include <session/SessionSuspend.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <r/RExec.hpp>
#include <r/session/RBusy.hpp>
#include <r/session/RSession.hpp>

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
   kWaitingForInactivity
};

std::unordered_set<std::string> opsBlockingSuspend;
boost::posix_time::ptime s_suspendTimeoutTime = boost::posix_time::second_clock::universal_time();
boost::posix_time::ptime s_blockingTimestamp = boost::posix_time::second_clock::universal_time();
bool s_dirtyBlockingOps = false;
bool s_initialNotificationSent = false;
bool s_timeoutLogSent = false;
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

void blockingTimestamp()
{
   s_blockingTimestamp = boost::posix_time::second_clock::universal_time();
}

void resetBlockingTimestamp()
{
   s_blockingTimestamp = boost::posix_time::not_a_date_time;
}

bool shouldNotify()
{
   if (!prefs::userPrefs().consoleSuspendBlockedNotice())
   {
      return false;
   }

   auto delay = boost::posix_time::seconds(prefs::userPrefs().consoleSuspendBlockedNoticeDelay());
   return s_blockingTimestamp + delay < boost::posix_time::second_clock::universal_time();
}

core::json::Array blockingOpsToJson()
{
   core::json::Array opsBlockingSuspendJson;

   for (const auto &op : opsBlockingSuspend)
   {
      opsBlockingSuspendJson.push_back(op);
   }

   return opsBlockingSuspendJson;
}

void addBlockingMetadata(const core::json::Array& blockingOps)
{
   module_context::activeSession().setBlockingSuspend(blockingOps);
}

void sendBlockingClientEvent(const core::json::Array& blockingOps)
{
   module_context::enqueClientEvent(ClientEvent(rstudio::session::client_events::kSuspendBlocked, blockingOps));
   s_initialNotificationSent = true;
}

void logBlockingOps(const core::json::Array& blockingOps)
{
   core::Error blocked("SessionTimeoutSuspendBlocked",
                       -1,
                       "Session attempted to suspend, due to timeout, but was blocked",
                       ERROR_LOCATION);

   blocked.addProperty("username", core::system::username());
   blocked.addProperty("session_id", module_context::activeSession().id());
   blocked.addProperty("suspend_timeout_minutes_setting", options().timeoutMinutes());
   blocked.addProperty("blocking_operations", blockingOps.writeFormatted());
   blocked.addProperty("blocking_ops_start_time", boost::posix_time::to_simple_string(s_blockingTimestamp));

   core::log::logErrorAsInfo(blocked);
}

/**
 * @brief Writes blocking OP metadata and creates an event, but only if
 * current blocking OPs haven't already been reported
 *
 * @param force If true, will store metadata and create an event, even if
 * the current blocking OPs have already been reported
 */
void storeBlockingOps(bool force = false)
{
   if (s_dirtyBlockingOps || force)
   {
      core::json::Array blockingOps = blockingOpsToJson();
      addBlockingMetadata(blockingOps);
      sendBlockingClientEvent(blockingOps);
      resetSuspendTimeout();
      s_dirtyBlockingOps = false;
   }
}

/**
 * @brief Logs a warning that auto/timeout suspend may be blocked
 */
void logBlockingOpsWarning()
{
   core::json::Array blockingOps = blockingOpsToJson();
   logBlockingOps(blockingOps);
}

void addBlockingOp(std::string op)
{
   // If we're already tracking this op, nothing to do
   if (opsBlockingSuspend.count(op))
      return;

   opsBlockingSuspend.insert(op);
   s_dirtyBlockingOps = true;
}

void addBlockingOp(std::string method, const boost::function<bool()>& allowSuspend)
{
   // Only a blocking op if the allowSuspend() is actually the disallowSuspend() method
   if (allowSuspend == disallowSuspend)
   {
      addBlockingOp(kGenericMethod + method);
   }
}

void removeBlockingOp(std::string op)
{
   if (opsBlockingSuspend.count(op) == 0)
      return;

   opsBlockingSuspend.erase(op);
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
bool checkBlockingOp(bool blocking, std::string op)
{
   if (opsBlockingSuspend.count(op) && !blocking)
   {
      opsBlockingSuspend.erase(op);
      s_dirtyBlockingOps = true;
   }
   else if (opsBlockingSuspend.count(op) == 0 && blocking)
   {
      opsBlockingSuspend.insert(op);
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
   opsBlockingSuspend.clear();
   if (store)
   {
      addBlockingMetadata(core::json::Array());
      sendBlockingClientEvent(core::json::Array());
      s_dirtyBlockingOps = false;
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

   // record time of suspension
   module_context::activeSession().setSuspensionTime();

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
         s_timeoutState = kWaitingForInactivity;
         blockingTimestamp(); // Record when the blocking started
         resetSuspendTimeout();
         s_initialNotificationSent = false; // Set flag so at least one notification is sent to frontend
         s_timeoutLogSent = false;
      }
   }

   // If there is suspend-blocking activity, wait for it to end while also
   // notifying user that the session is blocked from suspending
   if (s_timeoutState == kWaitingForInactivity)
   {
      if (canSuspend)
      {
         // reset the inactivity timeout before switching waiting modes
         s_timeoutState = kWaitingForTimeout;
         resetSuspendTimeout();
         resetBlockingTimestamp();
         clearBlockingOps(true);

         return;
      }

      // Send RPC warning user after a few seconds
      if (shouldNotify())
         storeBlockingOps(!s_initialNotificationSent);

      // If we've been waiting for inactivity long enough that we
      // would have normally suspended, create a single warning log
      if (!s_timeoutLogSent && isTimedOut(s_suspendTimeoutTime))
      {
         logBlockingOpsWarning();
         s_timeoutLogSent = true;
      }
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

std::string mostSignificantTimeAgo(const boost::posix_time::ptime& before, const boost::posix_time::ptime& after = boost::posix_time::second_clock::universal_time())
{
   if (before == boost::posix_time::not_a_date_time || after == boost::posix_time::not_a_date_time)
      return "";

   boost::posix_time::time_duration diff = after - before;
   if (diff.is_negative())
   {
       diff.invert_sign();
   }

   // determine the most significant non-zero time resolution
   if (diff < boost::posix_time::minutes(2))
      return ""; // Stay quiet if not enough time has passed
   if (diff < boost::posix_time::hours(1))
      return std::to_string(diff.minutes()) + " minutes ago";

   if (diff < boost::posix_time::hours(2))
      return std::to_string(diff.hours()) + " hour ago";
   if (diff < boost::posix_time::hours(24))
      return std::to_string(diff.hours()) + " hours ago";

   if (diff < boost::posix_time::hours(48))
      return std::to_string(diff.hours() / 24l) + " day ago";
   if (diff < boost::posix_time::hours(24 * 365))
      return std::to_string(diff.hours() / 24l) + " days ago";

   if (diff < boost::posix_time::hours(24 * 365 * 2))
       return std::to_string(diff.hours() / (24l * 365l)) + " year ago";
   else
      return std::to_string(diff.hours() / (24l * 365l)) + " years ago";
}

std::string getResumedMessage()
{
   boost::posix_time::ptime suspensionTimestamp = module_context::activeSession().suspensionTime();

   // Assume suspension never occurred if there's no suspensionTimestamp
   if (suspensionTimestamp == boost::posix_time::not_a_date_time)
   {
      boost::posix_time::ptime lastResumed = module_context::activeSession().lastResumed();

      std::string xAmountOfTimeAgo = mostSignificantTimeAgo(lastResumed);
      if (xAmountOfTimeAgo.empty())
         return "";

      std::string blockedTime = boost::posix_time::to_simple_string(lastResumed);
      return "Connected to your session in progress, last started "
             + blockedTime
             + " UTC ("
             + xAmountOfTimeAgo
             + ")\n";
   }
   else
   {
      std::string xAmountOfTimeAgo = mostSignificantTimeAgo(suspensionTimestamp);
      if (xAmountOfTimeAgo.empty())
         return "";

      std::string suspendedTime = boost::posix_time::to_simple_string(suspensionTimestamp);
      return "Session restored from your saved work on "
             + suspendedTime
            + " UTC ("
            + xAmountOfTimeAgo
            + ")\n";
   }

   return "";
}

void initFromResume()
{
   module_context::activeSession().setLastResumed();
   module_context::activeSession().setSuspensionTime(boost::posix_time::not_a_date_time);

   s_timeoutState = kWaitingForTimeout;
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
   // if R is busy executing code, we'll need to interrupt and force suspend
   //
   // for launcher sessions, we only want to interrupt user code to fix
   // an excess logging issue caused by interrupting RStudio code that
   // takes longer to execute in docker containers
   if (console_input::executing() || r::session::isBusy())
   {
      s_forceSuspendInterruptedR = 1;
      rstudio::r::exec::setInterruptsPending(true);
   }

   // note that a suspend is being forced
   s_forceSuspend = 1;
}

} // namespace suspend
} // namespace session
} // namespace rstudio
