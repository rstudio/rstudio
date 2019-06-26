/*
 * UserPrefsLayer.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#include "UserPrefsLayer.hpp"

#include <core/system/Xdg.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserPrefsLayer::UserPrefsLayer():
   PrefLayer(kUserPrefsUserLayer)
{
}

Error UserPrefsLayer::readPrefs()
{
   Error err;
   prefsFile_ = core::system::xdg::userConfigDir().complete(kUserPrefsFile);

   // After deferred init, start monitoring the prefs file for changes
   module_context::events().onDeferredInit.connect([&](bool)
   {
      monitorPrefsFile(prefsFile_);
   });

   // Mark the last sync time 
   lastSync_ = prefsFile_.lastWriteTime();

   return loadPrefsFromFile(prefsFile_);
}

void UserPrefsLayer::onPrefsFileChanged()
{
   if (prefsFile_.lastWriteTime() <= lastSync_)
   {
      // No work to do; we wrote this update ourselves.
      return;
   }

   // Make a copy of the prefs prior to reloading, so we can diff against the old copy
   const json::Object old = cache_->clone().get_obj();

   // Reload the prefs from the file
   Error error = loadPrefsFromFile(prefsFile_);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // Figure out what prefs changed in the file
   for (const auto key: UserPrefValues::allKeys())
   {
      const auto itOld = old.find(key);
      const auto itNew = cache_->find(key);

      // A pref is considered to be changed if it (a) exists in the new set of prefs, and (b) either
      // didn't exist in the old set, or existed there with a new value. This does not currently
      // emit events for pref values that have been removed.
      if (itNew != cache_->end() &&
          (itOld == old.end() || !((*itNew).value() == (*itOld).value())))
      {
         onChanged(key);
      }
   }
}

Error UserPrefsLayer::writePrefs(const core::json::Object &prefs)
{
   if (prefsFile_.empty())
   {
      return fileNotFoundError(ERROR_LOCATION);
   }
   Error error;

   LOCK_MUTEX(mutex_)
   {
      *cache_ = prefs;
   }
   END_LOCK_MUTEX

   error = writePrefsToFile(*cache_, prefsFile_);
   if (!error)
   {
      // If we successfully wrote the contents, mark the last sync time
      lastSync_ = prefsFile_.lastWriteTime();
   }

   return error;
}

Error UserPrefsLayer::validatePrefs()
{
   return validatePrefsFromSchema(
      options().rResourcesPath().complete("schema").complete(kUserPrefsSchemaFile));
}

} // namespace prefs
} // namespace session
} // namespace rstudio

