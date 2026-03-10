/*
 * UserPrefsLayer.cpp
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
   prefsFile_ = core::system::xdg::userConfigDir().completePath(kUserPrefsFile);

   // After deferred init, start monitoring the prefs file for changes
   module_context::events().onDeferredInit.connect([&](bool)
   {
      monitorPrefsFile(prefsFile_);
   });

   // Mark the last sync time 
   lastSync_ = prefsFile_.getLastWriteTime();

   json::Object prefs;
   Error error = loadPrefsFromFile(prefsFile_,
       options().rResourcesPath().completePath("schema").completePath(kUserPrefsSchemaFile),
       &prefs);

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      cache_ = boost::make_shared<json::Object>(std::move(prefs));
   }
   END_LOCK_MUTEX

   return error;
}

void UserPrefsLayer::onPrefsFileChanged()
{
   json::Object cacheOld;
   json::Object cacheNew;

   // Reload the prefs from the file
   json::Object prefs;
   Error error = loadPrefsFromFile(prefsFile_,
       options().rResourcesPath().completePath("schema").completePath(kUserPrefsSchemaFile),
       &prefs);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      if (prefsFile_.getLastWriteTime() <= lastSync_)
      {
         // No work to do; we wrote this update ourselves.
         return;
      }

      // Snapshot the old cache prior to replacing it
      cacheOld = cache_->clone().getObject();
      cache_ = boost::make_shared<json::Object>(std::move(prefs));
      cacheNew = cache_->clone().getObject();
   }
   END_LOCK_MUTEX

   // Diff against old cache and notify listeners of changed keys.
   // This does not currently emit events for pref values that have been removed.
   for (const auto& key : UserPrefValues::allKeys())
   {
      const auto itOld = cacheOld.find(key);
      const auto itNew = cacheNew.find(key);

      bool existsInNew = itNew != cacheNew.end();
      bool isNew = itOld == cacheOld.end();
      bool isChanged = !isNew && !((*itOld).getValue() == (*itNew).getValue());

      if (existsInNew && (isNew || isChanged))
      {
         onChanged(key);
      }
   }
}

Error UserPrefsLayer::writePrefs(const core::json::Object &prefs)
{
   if (prefsFile_.isEmpty())
   {
      return fileNotFoundError(ERROR_LOCATION);
   }

   // Write to disk first; only update in-memory state on success
   Error error = writePrefsToFile(prefs, prefsFile_);
   if (error)
   {
      // Modify the error to be more descriptive
      if (isFileNotFoundError(error))
      {
         error = Error(
            error.getName(),
            error.getCode(),
            "Unable to save preferences. Please verify that " +
               prefsFile_.getAbsolutePath() +
               " exists and is owned by the user '" +
               system::username() + "'.",
            error,
            ERROR_LOCATION);
      }
      return error;
   }

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      *cache_ = prefs;
      lastSync_ = prefsFile_.getLastWriteTime();
   }
   END_LOCK_MUTEX

   return Success();
}

} // namespace prefs
} // namespace session
} // namespace rstudio

