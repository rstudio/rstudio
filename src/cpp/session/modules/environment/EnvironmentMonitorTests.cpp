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
#include "EnvironmentUtils.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace environment {
namespace {

const char kHiddenName[] = ".rsEnvironmentMonitorTestHidden";
const char kVisibleName[] = "rsEnvironmentMonitorTestVisible";
const char kCreatedName[] = "rsEnvironmentMonitorTestCreated";
const char kDeletedName[] = "rsEnvironmentMonitorTestDeleted";

// where ClearVisibleObjects stashes the global environment's visible objects
const char kSavedObjectsName[] = ".rsEnvironmentMonitorTestSaved";

core::Error evaluate(const std::string& code)
{
   return r::exec::executeString(code);
}

core::Error assignGlobal(const std::string& name, const std::string& value = "1")
{
   return evaluate("assign('" + name + "', " + value + ", envir = globalenv())");
}

core::Error removeGlobal(const std::string& name)
{
   return evaluate(
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
      savedShowLastValue_ = prefs::userPrefs().showLastDotValue();
      ASSERT_FALSE(assignGlobal(kHiddenName));
      ASSERT_FALSE(assignGlobal(kVisibleName));

      connection_ = module_context::events().onEnvironmentVariablesChanged.connect(
         [this](const module_context::EnvironmentVariablesChangedEvent& event)
         {
            signals_.push_back(event);
         });
   }

   void TearDown() override
   {
      EXPECT_FALSE(evaluate(
         "if (exists('" + std::string(kSavedObjectsName) + "', envir = globalenv())) "
         "{ list2env(" + kSavedObjectsName + ", envir = globalenv()); "
         "rm(list = '" + kSavedObjectsName + "', envir = globalenv()) }"));
      EXPECT_FALSE(removeGlobal(kHiddenName));
      EXPECT_FALSE(removeGlobal(kVisibleName));
      EXPECT_FALSE(removeGlobal(kCreatedName));
      EXPECT_FALSE(removeGlobal(kDeletedName));
      prefs::userPrefs().setShowHiddenObjects(savedShowHidden_);
      prefs::userPrefs().setShowLastDotValue(savedShowLastValue_);
   }

   void startMonitoring(bool showHidden)
   {
      prefs::userPrefs().setShowHiddenObjects(showHidden);
      prefs::userPrefs().setShowLastDotValue(false);
      monitor_.setMonitoredEnvironment(R_GlobalEnv);
      clearPending();
   }

   // discards events and signals emitted so far
   void clearPending()
   {
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

   bool signalResets()
   {
      return std::any_of(
         signals_.begin(), signals_.end(),
         [](const module_context::EnvironmentVariablesChangedEvent& signal)
         {
            return signal.reset;
         });
   }

   EnvironmentMonitor monitor_;
   std::vector<module_context::EnvironmentVariablesChangedEvent> signals_;
   RSTUDIO_BOOST_SCOPED_CONNECTION connection_;
   bool savedShowHidden_ = false;
   bool savedShowLastValue_ = false;
};

TEST_F(GlobalEnvironmentMonitorTest, ShowingHiddenObjectsReportsNoChanges)
{
   startMonitoring(false);

   prefs::userPrefs().setShowHiddenObjects(true);
   monitor_.checkForChanges();

   std::vector<std::string> eventNames = drainEnvironmentEventNames();
   EXPECT_FALSE(contains(eventNames, kHiddenName));
   EXPECT_FALSE(contains(eventNames, kVisibleName));
   EXPECT_TRUE(signals_.empty());
}

TEST_F(GlobalEnvironmentMonitorTest, HidingHiddenObjectsReportsNoChanges)
{
   startMonitoring(true);

   prefs::userPrefs().setShowHiddenObjects(false);
   monitor_.checkForChanges();

   std::vector<std::string> eventNames = drainEnvironmentEventNames();
   EXPECT_FALSE(contains(eventNames, kHiddenName));
   EXPECT_FALSE(contains(eventNames, kVisibleName));
   EXPECT_TRUE(signals_.empty());
}

TEST_F(GlobalEnvironmentMonitorTest, PrefChangeKeepsPendingChanges)
{
   ASSERT_FALSE(assignGlobal(kDeletedName));
   startMonitoring(false);

   // R code can change objects and the pref in one evaluation, before the
   // monitor has checked for changes
   ASSERT_FALSE(assignGlobal(kVisibleName, "2"));
   ASSERT_FALSE(assignGlobal(kCreatedName));
   ASSERT_FALSE(removeGlobal(kDeletedName));
   prefs::userPrefs().setShowHiddenObjects(true);
   monitor_.checkForChanges();

   std::vector<std::string> eventNames = drainEnvironmentEventNames();
   EXPECT_TRUE(contains(eventNames, kVisibleName));
   EXPECT_TRUE(contains(eventNames, kCreatedName));
   EXPECT_TRUE(contains(eventNames, kDeletedName));
   EXPECT_TRUE(signalMentions(kVisibleName));
   EXPECT_TRUE(signalMentions(kCreatedName));
   EXPECT_TRUE(signalMentions(kDeletedName));
}

TEST_F(GlobalEnvironmentMonitorTest, PrefChangeKeepsPendingPromiseEvaluation)
{
   ASSERT_FALSE(evaluate(
      "delayedAssign('" + std::string(kVisibleName) + "', 3, assign.env = globalenv())"));
   startMonitoring(false);

   ASSERT_FALSE(evaluate("force(" + std::string(kVisibleName) + ")"));
   prefs::userPrefs().setShowHiddenObjects(true);
   monitor_.checkForChanges();

   EXPECT_TRUE(contains(drainEnvironmentEventNames(), kVisibleName));
   EXPECT_TRUE(signalMentions(kVisibleName));
}

TEST_F(GlobalEnvironmentMonitorTest, HiddenObjectsUpdatePaneOnlyWhenShown)
{
   startMonitoring(false);

   ASSERT_FALSE(assignGlobal(kHiddenName, "2"));
   monitor_.checkForChanges();
   EXPECT_FALSE(contains(drainEnvironmentEventNames(), kHiddenName));

   prefs::userPrefs().setShowHiddenObjects(true);
   ASSERT_FALSE(assignGlobal(kHiddenName, "3"));
   monitor_.checkForChanges();
   EXPECT_TRUE(contains(drainEnvironmentEventNames(), kHiddenName));

   // the assistant never hears about hidden objects
   EXPECT_FALSE(signalMentions(kHiddenName));
}

TEST_F(GlobalEnvironmentMonitorTest, HiddenChangeMadeBeforeShowingIsReported)
{
   startMonitoring(false);

   ASSERT_FALSE(assignGlobal(kHiddenName, "2"));
   prefs::userPrefs().setShowHiddenObjects(true);
   monitor_.checkForChanges();

   EXPECT_TRUE(contains(drainEnvironmentEventNames(), kHiddenName));
}

TEST_F(GlobalEnvironmentMonitorTest, AssistantSignalOmitsHiddenObjects)
{
   startMonitoring(true);

   // modify both objects so each is reported as changed
   ASSERT_FALSE(assignGlobal(kHiddenName, "2"));
   ASSERT_FALSE(assignGlobal(kVisibleName, "2"));
   monitor_.checkForChanges();

   // the pane lists both, but the assistant only hears about the visible one
   std::vector<std::string> eventNames = drainEnvironmentEventNames();
   EXPECT_TRUE(contains(eventNames, kHiddenName));
   EXPECT_TRUE(contains(eventNames, kVisibleName));
   EXPECT_TRUE(signalMentions(kVisibleName));
   EXPECT_FALSE(signalMentions(kHiddenName));
}

TEST_F(GlobalEnvironmentMonitorTest, FirstVisibleObjectIsReportedToAssistant)
{
   // https://github.com/rstudio/rstudio/issues/18929
   startMonitoring(false);

   // with no visible objects left, the monitor takes its single-refresh path
   ASSERT_FALSE(evaluate(
      std::string(kSavedObjectsName) + " <- as.list(globalenv()); "
      "rm(list = ls(globalenv()), envir = globalenv())"));
   monitor_.checkForChanges();
   EXPECT_TRUE(signalResets());
   clearPending();

   ASSERT_FALSE(assignGlobal(kCreatedName));
   monitor_.checkForChanges();

   ASSERT_EQ(signals_.size(), 1u);
   EXPECT_FALSE(signals_[0].reset);
   EXPECT_TRUE(contains(signals_[0].created, kCreatedName));
}

TEST(EnvironmentListingPrefsTest, PaneListsNamesPerPrefs)
{
   bool savedShowHidden = prefs::userPrefs().showHiddenObjects();
   bool savedShowLastValue = prefs::userPrefs().showLastDotValue();

   prefs::userPrefs().setShowHiddenObjects(false);
   prefs::userPrefs().setShowLastDotValue(false);
   EXPECT_TRUE(isListedInPane("x"));
   EXPECT_FALSE(isListedInPane(".x"));
   EXPECT_FALSE(isListedInPane(".Last.value"));

   // .Last.value follows its own pref, not the hidden objects one
   prefs::userPrefs().setShowHiddenObjects(true);
   EXPECT_TRUE(isListedInPane(".x"));
   EXPECT_FALSE(isListedInPane(".Last.value"));

   prefs::userPrefs().setShowHiddenObjects(false);
   prefs::userPrefs().setShowLastDotValue(true);
   EXPECT_FALSE(isListedInPane(".x"));
   EXPECT_TRUE(isListedInPane(".Last.value"));

   prefs::userPrefs().setShowHiddenObjects(savedShowHidden);
   prefs::userPrefs().setShowLastDotValue(savedShowLastValue);
}

TEST(EnvironmentListingPrefsTest, LastValueIsListedByItsOwnPrefAlone)
{
   bool savedShowHidden = prefs::userPrefs().showHiddenObjects();
   bool savedShowLastValue = prefs::userPrefs().showLastDotValue();

   // .Last.value is a binding of baseenv itself, so listing all names there
   // would include it as a hidden object
   std::vector<std::string> names;
   prefs::userPrefs().setShowHiddenObjects(true);
   prefs::userPrefs().setShowLastDotValue(false);
   listEnvironmentForPane(R_BaseEnv, &names);
   EXPECT_FALSE(contains(names, ".Last.value"));

   prefs::userPrefs().setShowLastDotValue(true);
   listEnvironmentForPane(R_BaseEnv, &names);
   EXPECT_EQ(std::count(names.begin(), names.end(), ".Last.value"), 1);

   // the monitor tracks it whatever the prefs say
   prefs::userPrefs().setShowHiddenObjects(false);
   prefs::userPrefs().setShowLastDotValue(false);
   listEnvironmentForMonitor(R_GlobalEnv, &names);
   EXPECT_TRUE(contains(names, ".Last.value"));

   prefs::userPrefs().setShowHiddenObjects(savedShowHidden);
   prefs::userPrefs().setShowLastDotValue(savedShowLastValue);
}

} // anonymous namespace
} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio
