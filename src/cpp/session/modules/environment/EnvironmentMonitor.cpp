/*
 * EnvironmentMonitor.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "EnvironmentMonitor.hpp"

#include <r/RSexp.hpp>
#include <r/RInterface.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "EnvironmentUtils.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace environment {
namespace {

bool compareVarName(const r::sexp::Variable& var1,
                    const r::sexp::Variable& var2)
{
   return var1.first < var2.first;
}

void enqueRefreshEvent()
{
   ClientEvent refreshEvent(client_events::kEnvironmentRefresh);
   module_context::enqueClientEvent(refreshEvent);
}

// if the given variable is an unevaluated promise, add it to the given
// list of variables
void addUnevaledPromise(std::vector<r::sexp::Variable>* pEnv,
                        const r::sexp::Variable& var)
{
   if (isUnevaluatedPromise(var.second))
   {
      pEnv->push_back(var);
   }
}

// If the given variable exists in the given list, remove it. Compares on name
// only.
void removeVarFromList(std::vector<r::sexp::Variable>* pEnv,
                       const r::sexp::Variable& var)
{
   for (std::vector<r::sexp::Variable>::iterator iter = pEnv->begin();
        iter != pEnv->end(); iter++)
   {
      if (iter->first == var.first)
      {
         pEnv->erase(iter);
         break;
      }
   }
}

} // anonymous namespace

EnvironmentMonitor::EnvironmentMonitor() :
   initialized_(false),
   refreshOnInit_(false)
{}

void EnvironmentMonitor::enqueRemovedEvent(const r::sexp::Variable& variable)
{
   ClientEvent removedEvent(client_events::kEnvironmentRemoved, variable.first);
   module_context::enqueClientEvent(removedEvent);
}

void EnvironmentMonitor::enqueAssignedEvent(const r::sexp::Variable& variable)
{
   // get object info
   json::Value objInfo = varToJson(getMonitoredEnvironment(), variable);

   // enque event
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

void EnvironmentMonitor::listEnv(std::vector<r::sexp::Variable>* pEnv)
{
   if (!hasEnvironment())
      return;

   r::sexp::Protect rProtect;
   r::sexp::listEnvironment(getMonitoredEnvironment(),
                            false,
                            prefs::userPrefs().showLastDotValue(),
                            &rProtect,
                            pEnv);
}

void EnvironmentMonitor::checkForChanges()
{
   // information about the current environment
   std::vector<r::sexp::Variable> currentEnv;
   std::vector<r::sexp::Variable> currentPromises;

   // list of assigns/removes (includes both value changes and promise
   // evaluations)
   std::vector<r::sexp::Variable> addedVars;
   std::vector<r::sexp::Variable> removedVars;

   // get the set of variables and promises in the current environment
   listEnv(&currentEnv);

   // R returns an environment list sorted in dictionary order. Since the
   // set difference algorithms below use simple string comparisons to
   // establish order, we need to re-sort the list into canonical order
   // to avoid the algorithms detecting superfluous insertions.
   std::sort(currentEnv.begin(), currentEnv.end(), compareVarName);

   std::for_each(currentEnv.begin(), currentEnv.end(),
                 boost::bind(addUnevaledPromise, &currentPromises, _1));

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
                                compareVarName);

            // fire removed event for deletes
            std::for_each(removedVars.begin(),
                          removedVars.end(),
                          boost::bind(&EnvironmentMonitor::enqueRemovedEvent,
                                      this, _1));

            // remove deleted objects from the list of uneval'ed promises
            // so we'll stop monitoring them for evaluation
            std::for_each(removedVars.begin(),
                          removedVars.end(),
                          boost::bind(removeVarFromList, &unevaledPromises_, _1));

            // find adds & assigns (all variable name/value combinations in the
            // current environment but NOT in the previous environment)
            std::set_difference(currentEnv.begin(), currentEnv.end(),
                                lastEnv_.begin(), lastEnv_.end(),
                                std::back_inserter(addedVars));

            // remove assigned objects from the list of uneval'ed promises
            // (otherwise, we double-assign in the case where a promise SEXP
            // is simultaneously forced/evaluated and assigned a new value)
            std::for_each(addedVars.begin(),
                          addedVars.end(),
                          boost::bind(removeVarFromList, &unevaledPromises_, _1));

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
            std::set_difference(unevaledPromises_.begin(), unevaledPromises_.end(),
                                currentPromises.begin(), currentPromises.end(),
                                std::back_inserter(addedVars));
         }

         // fire assigned event for adds, assigns, and promise evaluations
         std::for_each(addedVars.begin(),
                       addedVars.end(),
                       boost::bind(&EnvironmentMonitor::enqueAssignedEvent,
                                    this, _1));
      }
   }

   unevaledPromises_ = currentPromises;
   lastEnv_ = currentEnv;
}

} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio
