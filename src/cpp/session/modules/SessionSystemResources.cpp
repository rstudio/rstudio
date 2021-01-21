/*
 * SessionSystemResources.cpp
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

#include "SessionSystemResources.hpp"
#include "../SessionMainProcess.hpp"
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

#include <boost/make_shared.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace system_resources {
namespace {

std::atomic<time_t> s_activeQuery;

/**
 * Computes memory usage and emits it to the client as a client event.
 */
void emitMemoryChangedEvent()
{
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

/**
 * Performs a previously scheduled query for available memory.
 */
void performScheduledMemoryQuery(time_t originQuery)
{
   // Only perform this query if it hasn't been superseded by a newer one.
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
   time_t now = std::time(nullptr);
   s_activeQuery = now;
   module_context::scheduleDelayedWork(
            boost::posix_time::milliseconds(500),
            boost::bind(performScheduledMemoryQuery, now));
}

/**
 * Performs periodic re-query for memory stats
 */
bool performPeriodicWork()
{
   scheduleMemoryChangedEvent();

   // Continue querying for memory stats
   return true;
}

void onUserSettingsChanged(const std::string& layer, const std::string& pref)
{
   if (pref != kShowMemoryUsage)
      return;
   
   // If the pref was just turned on, compute and show memory usage immediately
   if (prefs::userPrefs().showMemoryUsage())
   {
      emitMemoryChangedEvent();
   }
}

void onDetectChanges(module_context::ChangeSource source)
{
   // Don't emit memory stats from child processes
   if (main_process::wasForked())
   {
      return;
   }

   // Wait a few ms before actually computing usage; this gives memory
   // counters time to catch up with whatever just happened and allows for
   // debouncing.
   scheduleMemoryChangedEvent();
}

} // anonymous namespace

json::Object MemoryStat::toJson()
{
   json::Object stat;
   stat["kb"] = kb;
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
   int kb;
   core::system::MemoryProvider provider;

   error = core::system::getTotalMemory(&kb, &provider);
   if (error)
      return error;
   pStats->total = MemoryStat(kb, provider);

   error = core::system::getMemoryUsed(&kb, &provider);
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
   prefs::userPrefs().onChanged.connect(onUserSettingsChanged);

   // Monitor memory usage every 10 seconds
   module_context::schedulePeriodicWork(
      boost::posix_time::seconds(10),
      performPeriodicWork);

   module_context::events().onDetectChanges.connect(onDetectChanges);

   return Success();
}

}  // namespace system_resources
}  // namespace modules
}  // namespace session
}  // namespace rstudio
