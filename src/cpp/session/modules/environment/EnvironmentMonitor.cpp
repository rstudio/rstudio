/*
 * EnvironmentMonitor.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include "EnvironmentUtils.hpp"

using namespace core;

namespace session {
namespace modules {
namespace environment {
namespace {

bool compareVarName(const r::sexp::Variable& var1,
                    const r::sexp::Variable& var2)
{
   return var1.first < var2.first;
}

void enqueRemovedEvent(const r::sexp::Variable& variable)
{
   ClientEvent removedEvent(client_events::kEnvironmentRemoved, variable.first);
   module_context::enqueClientEvent(removedEvent);
}

void enqueAssignedEvent(const r::sexp::Variable& variable)
{
   // get object info
   json::Value objInfo = varToJson(variable);

   // enque event
   ClientEvent assignedEvent(client_events::kEnvironmentAssigned, objInfo);
   module_context::enqueClientEvent(assignedEvent);
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

// if the given variable exists in the given list, remove it
void removeVarFromList(std::vector<r::sexp::Variable>* pEnv,
                       const r::sexp::Variable& var)
{
   std::vector<r::sexp::Variable>::iterator iter =
         std::find(pEnv->begin(), pEnv->end(), var);
   if (iter != pEnv->end())
   {
      pEnv->erase(iter);
   }
}

} // anonymous namespace

EnvironmentMonitor::EnvironmentMonitor() :
   initialized_(false)
{}

void EnvironmentMonitor::setMonitoredEnvironment(SEXP pEnvironment)
{
   environment_.set(pEnvironment);

   // init the environment by doing an initial check for changes
   initialized_ = false;
   checkForChanges();
}

SEXP EnvironmentMonitor::getMonitoredEnvironment()
{
   return environment_.get();
}

void EnvironmentMonitor::listEnv(std::vector<r::sexp::Variable>* pEnv)
{
   r::sexp::Protect rProtect;
   r::sexp::listEnvironment(getMonitoredEnvironment(), false, &rProtect, pEnv);
}

void EnvironmentMonitor::checkForChanges()
{
   // information about the current environment
   std::vector<r::sexp::Variable> currentEnv ;
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

   if (!initialized_)
   {
      if (getMonitoredEnvironment() == R_GlobalEnv)
      {
         enqueRefreshEvent();
      }
      initialized_ = true;
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
                          enqueRemovedEvent);

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
         }
      }
      if (!currentEnv.empty() && !lastEnv_.empty())
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
                       enqueAssignedEvent);
      }
   }

   unevaledPromises_ = currentPromises;
   lastEnv_ = currentEnv;
}


} // namespace environment
} // namespace modules
} // namespace session
