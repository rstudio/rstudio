/*
 * EnvironmentMonitorTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <gtest/gtest.h>

#include <algorithm>

#include <r/RExec.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "../../SessionClientEventQueue.hpp"
#include "EnvironmentMonitor.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace environment {
namespace {

const char kHiddenName[] = ".rsEnvironmentMonitorTestHidden";
const char kVisibleName[] = "rsEnvironmentMonitorTestVisible";

void assignGlobal(const std::string& name)
{
   core::Error error = r::exec::executeString(
      "assign('" + name + "', 1, envir = globalenv())");
   ASSERT_FALSE(error);
}

void removeGlobal(const std::string& name)
{
   r::exec::executeString(
      "if (exists('" + name + "', envir = globalenv(), inherits = FALSE)) "
      "rm(list = '" + name + "', envir = globalenv())");
}

bool contains(const std::vector<std::string>& names, const std::string& name)
{
   return std::find(names.begin(), names.end(), name) != names.end();
}

// drains the client event queue, returning the names in assigned/removed events
std::vector<std::string> drainEnvironmentEventNames()
{
   std::vector<ClientEvent> events;
   clientEventQueue().remove(&events);

   std::vector<std::string> names;
   for (const ClientEvent& event : events)
   {
      if (event.type() == client_events::kEnvironmentRemoved)
         names.push_back(event.data().getString());
      else if (event.type() == client_events::kEnvironmentAssigned)
         names.push_back(event.data().getObject()["name"].getString());
   }
   return names;
}

// monitors the global environment with objects and prefs set up for a test,
// restoring both afterwards
class GlobalEnvironmentMonitorTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      savedShowHidden_ = prefs::userPrefs().showHiddenObjects();
      assignGlobal(kHiddenName);
      assignGlobal(kVisibleName);

      connection_ = module_context::events().onEnvironmentVariablesChanged.connect(
         [this](const module_context::EnvironmentVariablesChangedEvent& event)
         {
            signals_.push_back(event);
         });
   }

   void TearDown() override
   {
      removeGlobal(kHiddenName);
      removeGlobal(kVisibleName);
      prefs::userPrefs().setShowHiddenObjects(savedShowHidden_);
   }

   void startMonitoring(bool showHidden)
   {
      prefs::userPrefs().setShowHiddenObjects(showHidden);
      monitor_.setMonitoredEnvironment(R_GlobalEnv);
      clientEventQueue().clear();
      signals_.clear();
   }

   bool signalMentions(const std::string& name)
   {
      for (const auto& signal : signals_)
      {
         if (contains(signal.created, name) ||
             contains(signal.modified, name) ||
             contains(signal.deleted, name))
            return true;
      }
      return false;
   }

   EnvironmentMonitor monitor_;
   std::vector<module_context::EnvironmentVariablesChangedEvent> signals_;
   RSTUDIO_BOOST_SCOPED_CONNECTION connection_;
   bool savedShowHidden_ = false;
};

TEST_F(GlobalEnvironmentMonitorTest, ShowingHiddenObjectsReportsNoChanges)
{
   startMonitoring(false);

   prefs::userPrefs().setShowHiddenObjects(true);
   monitor_.resetBaseline();
   monitor_.checkForChanges();

   EXPECT_FALSE(contains(drainEnvironmentEventNames(), kHiddenName));
   EXPECT_TRUE(signals_.empty());
}

TEST_F(GlobalEnvironmentMonitorTest, HidingHiddenObjectsReportsNoChanges)
{
   startMonitoring(true);

   prefs::userPrefs().setShowHiddenObjects(false);
   monitor_.resetBaseline();
   monitor_.checkForChanges();

   EXPECT_FALSE(contains(drainEnvironmentEventNames(), kHiddenName));
   EXPECT_TRUE(signals_.empty());
}

TEST_F(GlobalEnvironmentMonitorTest, PrefChangeKeepsPendingChanges)
{
   startMonitoring(false);

   // R code can modify an object and change the pref in one evaluation,
   // before the monitor has checked for changes
   r::exec::executeString("assign('" + std::string(kVisibleName) + "', 2, envir = globalenv())");
   prefs::userPrefs().setShowHiddenObjects(true);
   monitor_.resetBaseline();
   monitor_.checkForChanges();

   std::vector<std::string> eventNames = drainEnvironmentEventNames();
   EXPECT_TRUE(contains(eventNames, kVisibleName));
   EXPECT_FALSE(contains(eventNames, kHiddenName));
   EXPECT_TRUE(signalMentions(kVisibleName));
}

TEST_F(GlobalEnvironmentMonitorTest, PrefChangeKeepsPendingPromiseEvaluation)
{
   r::exec::executeString(
      "delayedAssign('" + std::string(kVisibleName) + "', 3, assign.env = globalenv())");
   startMonitoring(false);

   r::exec::executeString("force(" + std::string(kVisibleName) + ")");
   prefs::userPrefs().setShowHiddenObjects(true);
   monitor_.resetBaseline();
   monitor_.checkForChanges();

   EXPECT_TRUE(contains(drainEnvironmentEventNames(), kVisibleName));
   EXPECT_TRUE(signalMentions(kVisibleName));
}

TEST_F(GlobalEnvironmentMonitorTest, AssistantSignalOmitsHiddenObjects)
{
   startMonitoring(true);

   // modify both objects so each is reported as changed
   r::exec::executeString("assign('" + std::string(kHiddenName) + "', 2, envir = globalenv())");
   r::exec::executeString("assign('" + std::string(kVisibleName) + "', 2, envir = globalenv())");
   monitor_.checkForChanges();

   // the pane lists both, but the assistant only hears about the visible one
   std::vector<std::string> eventNames = drainEnvironmentEventNames();
   EXPECT_TRUE(contains(eventNames, kHiddenName));
   EXPECT_TRUE(contains(eventNames, kVisibleName));
   EXPECT_TRUE(signalMentions(kVisibleName));
   EXPECT_FALSE(signalMentions(kHiddenName));
}

} // anonymous namespace
} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio
