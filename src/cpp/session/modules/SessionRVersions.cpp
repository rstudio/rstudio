/*
 * SessionRVersions.cpp
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

#include "SessionRVersions.hpp"

#include <core/Exec.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/RVersionSettings.hpp>

#include <r/RExec.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace r_versions {
namespace {

void syncRVersionPref()
{
   RVersionSettings versionSettings(module_context::userScratchPath(),
                                 FilePath(options().getOverlayOption(
                                             kSessionSharedStoragePath)));

   auto value = prefs::userPrefs().readValue(kDefaultRVersion);
   if (!value || value->getType() != json::Type::OBJECT)
   {
      // No work to do if preference isn't actually defined
      return;
   }

   // Load values from prefs
   std::string version, home, label;
   Error error = json::readObject(value->getObject(),
         kDefaultRVersionVersion, version,
         kDefaultRVersionRHome,   home,
         kDefaultRVersionLabel,   label);
   if (error)
   {
      // Should not happen when preferences are valid according to schema
      LOG_ERROR(error);
      return;
   }

   if (versionSettings.defaultRVersion() == version &&
       versionSettings.defaultRVersionHome() == home &&
       versionSettings.defaultRVersionLabel() == label)
   {
      // No work to do; prefs file and versions settings file are already in sync
      return;
   }

   // Write new setting
   versionSettings.setDefaultRVersion(version, home, label);
   return;
}

void syncRestoreRVersionPref()
{
   RVersionSettings versionSettings(module_context::userScratchPath(),
                                 FilePath(options().getOverlayOption(
                                             kSessionSharedStoragePath)));

   bool restore = prefs::userPrefs().restoreProjectRVersion();

   if (restore == versionSettings.restoreProjectRVersion())
   {
      // No work to do; prefs already in sync
      return;
   }

   versionSettings.setRestoreProjectRVersion(restore);
   return;
}

void onUserPrefsChanged(const std::string& layer, const std::string& pref)
{
   if (pref == kDefaultRVersion)
   {
      syncRVersionPref();
   }
   else if (pref == kRestoreProjectRVersion)
   {
      syncRestoreRVersionPref();
   }
}


} // anonymous namespace

core::Error initialize()
{
   // Perform initial sync 
   syncRVersionPref();

   prefs::userPrefs().onChanged.connect(onUserPrefsChanged);

   return Success();
}

} // namespace r_versions
} // namespace modules
} // namespace session
} // namespace rstudio

