/*
 * SessionUserPrefs.cpp
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

#include "SessionCrashHandler.hpp"

#include <core/Exec.hpp>
#include <core/CrashHandler.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace crash_handler {
namespace {

void syncCrashHandlerPref()
{
   // Don't sync prefs if disabled by policy or we're not in desktop mode
   if (core::crash_handler::configSource() != core::crash_handler::ConfigSource::User ||
       session::options().programMode() != kSessionProgramModeDesktop)
      return;

   // Don't sync prefs if user has not been prompted yet
   if (!core::crash_handler::hasUserBeenPromptedForPermission())
      return;

   // Propagate the user prefs to the prefs file
   bool enable = prefs::userPrefs().submitCrashReports();
   if (enable != core::crash_handler::isHandlerEnabled())
   {
      // Pref out of sync; synchronize
      core::crash_handler::setUserHandlerEnabled(enable);
   }
   return;
}

void onUserPrefsChanged(const std::string& layer, const std::string& pref)
{
   if (pref == kSubmitCrashReports)
   {
      syncCrashHandlerPref();
   }
}


} // anonymous namespace

core::Error initialize()
{
   // Perform initial sync 
   syncCrashHandlerPref();

   prefs::userPrefs().onChanged.connect(onUserPrefsChanged);

   return Success();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

