/*
 * SessionSystemResources.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionSystemResources.hpp"
#include "../SessionMainProcess.hpp"
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionMain.hpp>
#include "../SessionConsoleInput.hpp"
#include <session/SessionConstants.hpp>

#include <chrono>

#include <core/Exec.hpp>
#include <core/system/Interrupts.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RJson.hpp>

#include <boost/make_shared.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace system_resources {
namespace {

// Keep track of the time at which the active memory query was issued; this is
// used to debounce memory queries. We use a steady_clock to avoid being
// affected by system wall clock adjustments.
std::chrono::steady_clock::time_point s_activeQuery;
boost::mutex s_queryMutex;
bool s_memoryLimitErrorSeen = false;
bool s_memoryLimitWarningSeen = false;

// The interval, in seconds, at which we will query for memory statistics
std::atomic<int> s_queryInterval;


/**
 * Performs a previously scheduled query for available memory.
 */
void performScheduledMemoryQuery(std::chrono::steady_clock::time_point originQuery)
{
   // Only perform this query if it hasn't been superseded by a newer one.
   LOCK_MUTEX(s_queryMutex)
   {
      if (originQuery == s_activeQuery)
      {
         // Only perform this query if pref is enabled; it's possible for queries
         // to get scheduled even with the pref off
         if (prefs::userPrefs().showMemoryUsage())
         {
            emitMemoryChangedEvent();
         }
      }
   }
   END_LOCK_MUTEX
}

/**
 * Schedules a memory changed event to be emitted. 
 */
void scheduleMemoryChangedEvent()
{
   // Memory queries aren't very expensive, but aren't free either, so it's
   // wasteful to repeatedly query the OS for memory stats multiple times a
   // second (which can happen as queries are bound to UI triggers). 
   //
   // To debounce memory queries, we place them in a queue for 500ms, and
   // ignore any query execution that isn't at the top of the queue.
   LOCK_MUTEX(s_queryMutex)
   {
      std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
      s_activeQuery = now;
      module_context::scheduleDelayedWork(
               boost::posix_time::milliseconds(500),
               boost::bind(performScheduledMemoryQuery, now));
   }
   END_LOCK_MUTEX
}

/**
 * Performs periodic re-query for memory stats.
 * 
 * We do this regularly if prefs indicate we should do so. This helps us
 * update memory usage in the Environment pane that isn't necessarily the
 * result of user actions (long-running code, background jobs, non-R
 * processes on the system, etc.)
 */
void performPeriodicWork(bool refreshStats)
{
   // Schedule a re-query if requested
   if (refreshStats)
   {
      scheduleMemoryChangedEvent();
   }

   // Schedule the next re-query; we re-read the pref every time so it can be
   // adjusted without a restart. Note that setting the pref to 0 will cause
   // automatic requeries to cease entirely.
   s_queryInterval = prefs::userPrefs().memoryQueryIntervalSeconds();
   if (s_queryInterval > 0)
   {
      module_context::scheduleDelayedWork(
         boost::posix_time::seconds(s_queryInterval.load()),
         boost::bind(performPeriodicWork, true));
   }
}

void onUserSettingsChanged(const std::string& layer, const std::string& pref)
{
   if (pref == kShowMemoryUsage)
   {
      // If the pref was just turned on, compute and show memory usage immediately
      if (prefs::userPrefs().showMemoryUsage())
      {
         emitMemoryChangedEvent();
      }
   }
   else if (pref == kMemoryQueryIntervalSeconds)
   {
      if (s_queryInterval == 0 &&
          prefs::userPrefs().memoryQueryIntervalSeconds() > 0)
      {
         // If we were previously not querying at all, start doing so now.
         performPeriodicWork(true /* refresh stats */);
      }
   }
}

void onDeferredInit(bool newSession)
{
   // Schedule the initial refresh of memory statistics
   performPeriodicWork(false);
}

void onDetectChanges(module_context::ChangeSource source)
{
   // Wait a few ms before actually computing usage; this gives memory
   // counters time to catch up with whatever just happened and allows for
   // debouncing.
   scheduleMemoryChangedEvent();
}

/**
 * Gets a memory usage report for the client; this is a summary of both system
 * and R memory usage and used to build the visualization in Tools -> Memory ->
 * Memory Usage Report.
 */
Error getMemoryUsageReport(const json::JsonRpcRequest& , json::JsonRpcResponse* pResponse)
{
   json::Object report;

   // Get system-level memory stats
   boost::shared_ptr<MemoryUsage> pMemUsage;
   Error error = getMemoryUsage(&pMemUsage);
   if (error)
   {
      return error;
   }

   // Get R memory stats and convert result to JSON
   SEXP rMemUsage;
   r::sexp::Protect protect;
   error = r::exec::RFunction(".rs.getRMemoryUsed").call(&rMemUsage, &protect);
   if (error)
   {
      return error;
   }
   json::Value rMemUsageVal;
   error = r::json::jsonValueFromList(rMemUsage, &rMemUsageVal);
   if (error)
   {
      return error;
   }
   
   // Emit report to client
   report["system"] = pMemUsage->toJson();
   report["r"] = rMemUsageVal;
   pResponse->setResult(report);

   return Success();
}

} // anonymous namespace

json::Object MemoryStat::toJson()
{
   json::Object stat;
   stat["kb"] = static_cast<int64_t>(kb);
   stat["provider"] = static_cast<int>(provider);
   return stat;
}

json::Object MemoryUsage::toJson()
{
   json::Object usage;
   usage["total"] = total.toJson();
   usage["used"] = used.toJson();
   usage["process"] = process.toJson();
   usage["limit"] = limit.toJson();
   usage["abort"] = abort;
   usage["limitWarning"] = limitWarning;
   usage["overLimit"] = overLimit;
   return usage;
}

Error getMemoryUsage(boost::shared_ptr<MemoryUsage> *pMemUsage)
{
   boost::shared_ptr<MemoryUsage> pStats = boost::make_shared<MemoryUsage>();

   Error error;
   long kb;
   core::system::MemoryProvider provider;

   error = core::system::getTotalMemory(&kb, &provider);
   if (error)
      return error;
   pStats->total = MemoryStat(kb, provider);

   error = core::system::getTotalMemoryUsed(&kb, &provider);
   if (error)
      return error;
   pStats->used = MemoryStat(kb, provider);

   error = core::system::getProcessMemoryUsed(&kb, &provider);
   if (error)
      return error;
   pStats->process = MemoryStat(kb, provider);

   error = core::system::getProcessMemoryLimit(&kb, &provider);
   if (error)
      return error;
   pStats->limit = MemoryStat(kb, provider);

   if (pStats->limit.kb != 0)
   {
      uint64_t limit = pStats->limit.kb;
      uint64_t process = pStats->process.kb;
      long freeMem = pStats->total.kb - pStats->used.kb;
      if (freeMem < 0)
         freeMem = 0;
      int freeMemPercent = freeMem * 100 / pStats->total.kb;

      if (process > limit)
      {
	 int overBy = process - limit;
	 int abortFreeMemPercent = options().abortFreeMemPercent();
         pStats->overLimit = true;
         // Treat memory limit as only an error unless instructed to abort the session:
	 //  1. Let sys-admin's override abort
	 //  2. Give a small grace limit, especially since RSS is fuzzy and memory gets reclaimed
	 //  as you near the limit in cgroups.
	 //  3. Don't kill sessions until the system needs memory: less than 100mb or 5% free (where 5 is configurable, useful especially for testing)
         pStats->abort = !options().allowOverLimitSessions() && overBy > 50*1024 && (freeMem < 100*1024 || freeMemPercent < abortFreeMemPercent);
         std::string statusMessage = "Session process using: " + std::to_string(process) + "kb over the limit of: " + std::to_string(limit) + "kb.";
         std::string freeMessage = std::to_string(freeMem) + "kb free (" + std::to_string(freeMemPercent) + "%)";

         if (pStats->abort)
            LOG_ERROR_MESSAGE(statusMessage + " Stopping session due to low system memory (< 100mb or 5%): " + freeMessage);
         else if (!s_memoryLimitErrorSeen)
         {
            LOG_ERROR_MESSAGE(statusMessage + " Showing user an error but not stopping session with sufficient free system memory (> 100mb and 5%): " + freeMessage);
            s_memoryLimitErrorSeen = true;
         }
      }
      // The client will produce a warning the first time but this will be nice to see how fast memory is increasing
      else if (process + process * 0.15 > limit)
      {
         pStats->limitWarning = true;
         if (!s_memoryLimitWarningSeen)
         {
            LOG_WARNING_MESSAGE("Warning user: system memory usage: " + std::to_string(process) + " within 15% of the limit: " +
                                 std::to_string(limit) + " before session will be stopped");
            s_memoryLimitWarningSeen = true;
         }
      }
      else
         LOG_DEBUG_MESSAGE("System memory limit check: " + std::to_string(process) + " / " + std::to_string(limit));
   }

   *pMemUsage = pStats;
   return core::Success();
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   s_queryInterval = 0;

   // Listen for user settings changes and change events so we can perform
   // memory statistic refreshes as needed.
   prefs::userPrefs().onChanged.connect(onUserSettingsChanged);
   module_context::events().onDetectChanges.connect(onDetectChanges);
   module_context::events().onDeferredInit.connect(onDeferredInit);

   core::ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_memory_usage_report", getMemoryUsageReport))
      (bind(module_context::sourceModuleRFile, "SessionSystemResources.R"));

   return initBlock.execute();
}

void exitForMemoryLimit()
{
   controlledExit(SESSION_EXIT_EXCEEDED_MEMORY_LIMIT);
}

void startShutdownForMemoryLimit()
{
   bool busy = session::console_input::executing();
   if (busy)
   {
      LOG_DEBUG_MESSAGE("Interrupting session for memory limit shutdown");
      r::exec::setInterruptsPending(true);
      core::system::interrupt();
   }
   // Give the interrupt a chance to finish, the event to get to the client and then exit cleanly with a failure status
   module_context::scheduleDelayedWork(boost::posix_time::milliseconds(100), boost::bind(exitForMemoryLimit), false);
}

/**
 * Computes memory usage and emits it to the client as a client event.
 */
void emitMemoryChangedEvent()
{
   // Ensure we don't emit memory stats from child processes
   if (main_process::wasForked())
      return;

   boost::shared_ptr<MemoryUsage> pUsage;
   Error error = getMemoryUsage(&pUsage);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      json::Object usageJson = pUsage->toJson();
      ClientEvent event(client_events::kMemoryUsageChanged, usageJson);
      module_context::enqueClientEvent(event);
   }

   if (pUsage && pUsage->overLimit && pUsage->abort)
   {
      // We've sent the event that the client will detect as memory limit exceeded so this will
      // just exit after a pause to let the event get through, interrupt R cleanly, then exit as
      // cleanly as possible but with an error status.
      startShutdownForMemoryLimit();
   }
}

}  // namespace system_resources
}  // namespace modules
}  // namespace session
}  // namespace rstudio
