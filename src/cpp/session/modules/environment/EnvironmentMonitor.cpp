/*
 * EnvironmentMonitor.cpp
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

#include "EnvironmentMonitor.hpp"

#include <set>

#include <r/RSexp.hpp>
#include <r/RInterface.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "EnvironmentUtils.hpp"

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace environment {
namespace {

using BindingSnapshot = EnvironmentMonitor::BindingSnapshot;

bool compareSnapshotName(const BindingSnapshot& a, const BindingSnapshot& b)
{
   return a.name < b.name;
}

void enqueRefreshEvent()
{
   ClientEvent refreshEvent(client_events::kEnvironmentRefresh);
   module_context::enqueClientEvent(refreshEvent);
}

void removeNameFromList(std::vector<std::string>* pList,
                        const BindingSnapshot& snap)
{
   auto it = std::find(pList->begin(), pList->end(), snap.name);
   if (it != pList->end())
      pList->erase(it);
}

} // anonymous namespace

EnvironmentMonitor::EnvironmentMonitor() :
   initialized_(false),
   refreshOnInit_(false)
{}

void EnvironmentMonitor::enqueRemovedEvent(const std::string& name)
{
   ClientEvent removedEvent(client_events::kEnvironmentRemoved, name);
   module_context::enqueClientEvent(removedEvent);
}

void EnvironmentMonitor::enqueAssignedEvent(const std::string& name)
{
   json::Value objInfo = varToJson(name, getMonitoredEnvironment());
   ClientEvent assignedEvent(client_events::kEnvironmentAssigned, objInfo);
   module_context::enqueClientEvent(assignedEvent);
}

void EnvironmentMonitor::setMonitoredEnvironment(SEXP pEnvironment,
                                                 bool refresh)
{
   // ignore if we're already monitoring this environment
   if (getMonitoredEnvironment() == pEnvironment)
      return;

   environment_.set(pEnvironment);

   // init the environment by doing an initial check for changes
   initialized_ = false;
   refreshOnInit_ = refresh;
   checkForChanges();
}

SEXP EnvironmentMonitor::getMonitoredEnvironment()
{
   return environment_.get();
}

bool EnvironmentMonitor::hasEnvironment()
{
   SEXP envir = getMonitoredEnvironment();
   return envir != nullptr && r::sexp::isPrimitiveEnvironment(envir);
}

void EnvironmentMonitor::listEnv(std::vector<std::string>* pNames)
{
   if (!hasEnvironment())
      return;

   r::sexp::listEnvironment(getMonitoredEnvironment(),
                            false,
                            prefs::userPrefs().showLastDotValue(),
                            pNames);
}

void EnvironmentMonitor::snapshotBindings(
   SEXP env,
   const std::vector<std::string>& names,
   std::vector<BindingSnapshot>* pSnapshot)
{
   pSnapshot->clear();
   pSnapshot->reserve(names.size());
   for (const auto& name : names)
   {
      r::sexp::BindingType bt = r::sexp::getBindingType(name, env);
      SEXP token = r::sexp::getBindingIdentity(name, env, bt);
      pSnapshot->push_back({name, bt, token});
   }
}

void EnvironmentMonitor::checkForChanges()
{
   // information about the current environment
   std::vector<std::string> currentNames;
   std::vector<BindingSnapshot> currentEnv;
   std::vector<std::string> currentPromises;

   // list of assigns/removes (includes both value changes and promise
   // evaluations)
   std::vector<BindingSnapshot> addedVars;
   std::vector<BindingSnapshot> removedVars;

   // get the set of variable names in the current environment
   listEnv(&currentNames);

   // re-sort into canonical (C locale) order for set_difference
   std::sort(currentNames.begin(), currentNames.end());

   // build snapshots with opaque SEXP pointers for change detection
   SEXP monitoredEnv = getMonitoredEnvironment();
   snapshotBindings(monitoredEnv, currentNames, &currentEnv);

   // collect unevaluated promises
   for (const auto& name : currentNames)
   {
      if (isUnevaluatedPromise(name, monitoredEnv))
         currentPromises.push_back(name);
   }

   bool refreshEnqueued = false;
   if (!initialized_)
   {
      if (refreshOnInit_ ||
          getMonitoredEnvironment() == R_GlobalEnv)
      {
         enqueRefreshEvent();
         refreshEnqueued = true;
      }
      initialized_ = true;
      refreshOnInit_ = false;
   }
   else
   {
      if (currentEnv != lastEnv_)
      {
         // optimize for empty currentEnv (user reset workspace) or empty
         // lastEnv_ (startup) by just sending a single refresh event
         // only do this for the global environment--while debugging local
         // environments, the environment object list is sent down as part of
         // the context depth event.
         if ((currentEnv.empty() || lastEnv_.empty())
             && getMonitoredEnvironment() == R_GlobalEnv)
         {
            enqueRefreshEvent();
            refreshEnqueued = true;
         }
         else
         {
            std::set_difference(lastEnv_.begin(), lastEnv_.end(),
                                currentEnv.begin(), currentEnv.end(),
                                std::back_inserter(removedVars),
                                compareSnapshotName);

            // fire removed event for deletes
            for (const auto& snap : removedVars)
               enqueRemovedEvent(snap.name);

            // remove deleted objects from the list of uneval'ed promises
            std::for_each(removedVars.begin(),
                          removedVars.end(),
                          boost::bind(removeNameFromList, &unevaledPromises_, _1));

            // find adds & assigns (all snapshots in the current environment
            // but NOT in the previous environment -- detects both new names
            // and changed SEXP pointers)
            std::set_difference(currentEnv.begin(), currentEnv.end(),
                                lastEnv_.begin(), lastEnv_.end(),
                                std::back_inserter(addedVars));

            // remove assigned objects from the list of uneval'ed promises
            // (otherwise, we double-assign in the case where a promise SEXP
            // is simultaneously forced/evaluated and assigned a new value)
            std::for_each(addedVars.begin(),
                          addedVars.end(),
                          boost::bind(removeNameFromList, &unevaledPromises_, _1));
         }
      }
      // if a refresh is scheduled there's no need to emit add events one by one
      if (!refreshEnqueued)
      {
         // have any promises been evaluated since we last checked?
         if (currentPromises != unevaledPromises_)
         {
            // for each promise that is in the set of promises we are monitoring
            // for evaluation but not in the set of currently tracked promises,
            // we assume this to be an eval--process as an assign
            std::vector<std::string> evaluatedPromises;
            std::set_difference(unevaledPromises_.begin(), unevaledPromises_.end(),
                                currentPromises.begin(), currentPromises.end(),
                                std::back_inserter(evaluatedPromises));

            for (const auto& name : evaluatedPromises)
               addedVars.push_back({name, r::sexp::BindingType::Normal, R_NilValue});
         }

         // fire assigned event for adds, assigns, and promise evaluations
         for (const auto& snap : addedVars)
            enqueAssignedEvent(snap.name);
      }
   }

   // Emit environment variables changed signal for AI assistant integration
   // Only emit for global environment changes
   if (getMonitoredEnvironment() == R_GlobalEnv)
   {
      if (refreshEnqueued && currentEnv.empty())
      {
         // Environment was cleared - emit reset signal
         module_context::EnvironmentVariablesChangedEvent event;
         event.reset = true;
         module_context::events().onEnvironmentVariablesChanged(event);
      }
      else if (!addedVars.empty() || !removedVars.empty())
      {
         // Build a set of names from lastEnv_ for O(1) lookup to distinguish create vs modify
         std::set<std::string> lastEnvNames;
         for (const auto& snap : lastEnv_)
            lastEnvNames.insert(snap.name);

         module_context::EnvironmentVariablesChangedEvent event;
         event.reset = false;

         // Classify addedVars into created vs modified
         for (const auto& snap : addedVars)
         {
            if (lastEnvNames.count(snap.name) > 0)
               event.modified.push_back(snap.name);
            else
               event.created.push_back(snap.name);
         }

         // Extract names from removedVars
         for (const auto& snap : removedVars)
            event.deleted.push_back(snap.name);

         // Emit the signal
         module_context::events().onEnvironmentVariablesChanged(event);
      }
   }

   unevaledPromises_ = currentPromises;
   lastEnv_ = currentEnv;
}

} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio
