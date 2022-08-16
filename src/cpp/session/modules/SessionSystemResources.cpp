/*
 * SessionSystemResources.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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

#include <chrono>

#include <core/Exec.hpp>

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


/**
 * Computes memory usage and emits it to the client as a client event.
 */
void emitMemoryChangedEvent()
{
   // Ensure we don't emit memory stats from child processes
   if (main_process::wasForked())
   {
      return;
   }

   boost::shared_ptr<MemoryUsage> pUsage;
   Error error = getMemoryUsage(&pUsage);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      ClientEvent event(client_events::kMemoryUsageChanged, pUsage->toJson());
      module_context::enqueClientEvent(event);
   }
}

}  // namespace system_resources
}  // namespace modules
}  // namespace session
}  // namespace rstudio
