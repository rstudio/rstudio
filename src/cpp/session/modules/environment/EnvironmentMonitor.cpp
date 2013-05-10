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
   return var1.first < var2.first ;
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
   std::cerr << "Tracked assignment to " << variable.first << std::endl;

   // enque event
   ClientEvent assignedEvent(client_events::kEnvironmentAssigned, objInfo);
   module_context::enqueClientEvent(assignedEvent);
}

} // anonymous namespace


void EnvironmentMonitor::setMonitoredEnvironment(SEXP pEnvironment)
{
   std::cerr << "Creating monitor for environment "
             << environment_.get() << std::endl;
   environment_.set(pEnvironment);
   initialized_ = false;
}

void EnvironmentMonitor::listEnv(std::vector<r::sexp::Variable>* pEnv)
{
   r::sexp::Protect rProtect;
   std::cerr << "Listing environment " << environment_.get() << std::endl;
   r::sexp::listEnvironment(environment_.get(), false, &rProtect, pEnv);
}

void EnvironmentMonitor::checkForChanges()
{
   // get the current environment
   std::vector<r::sexp::Variable> currentEnv ;
   listEnv(&currentEnv);

   std::cerr << "Checking for changes to environment " << environment_.get() << std::endl;

   if (!initialized_)
   {
      lastEnv_ = currentEnv;
      initialized_ = true;
      return;
   }

   // if there are changes
   if (currentEnv != lastEnv_)
   {
      std::vector<r::sexp::Variable> removedVars ;
      std::set_difference(lastEnv_.begin(), lastEnv_.end(),
                          currentEnv.begin(), currentEnv.end(),
                          std::back_inserter(removedVars),
                          compareVarName);

      // fire removed event for deletes
      std::for_each(removedVars.begin(),
                    removedVars.end(),
                    enqueRemovedEvent);

      // find adds & assigns (all variable name/value combinations in the
      // current environment but NOT in the previous environment)
      std::vector<r::sexp::Variable> addedVars;
      std::set_difference(currentEnv.begin(), currentEnv.end(),
                          lastEnv_.begin(), lastEnv_.end(),
                          std::back_inserter(addedVars));

      // fire assigned event for adds & assigns
      std::for_each(addedVars.begin(),
                    addedVars.end(),
                    enqueAssignedEvent);
   }

   lastEnv_ = currentEnv;
}


} // namespace environment
} // namespace modules
} // namespace session
