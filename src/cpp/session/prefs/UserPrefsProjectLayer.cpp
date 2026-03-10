/*
 * UserPrefsProjectLayer.cpp
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

#include "UserPrefsProjectLayer.hpp"

#include <core/system/Xdg.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserPrefsProjectLayer::UserPrefsProjectLayer():
   PrefLayer(kUserPrefsProjectLayer),
   lastSync_(0)
{
}

void UserPrefsProjectLayer::mergePrefs()
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      cache_ = boost::make_shared<json::Object>();

      // Start from shared prefs
      if (sharedPrefsCache_)
      {
         for (const auto& member : *sharedPrefsCache_)
         {
            (*cache_)[member.getName()] = member.getValue().clone();
         }
      }

      // Overlay local prefs (takes precedence)
      if (localPrefsCache_)
      {
         for (const auto& member : *localPrefsCache_)
         {
            (*cache_)[member.getName()] = member.getValue().clone();
         }
      }
   }
   END_LOCK_MUTEX
}

void UserPrefsProjectLayer::onProjectConfigChanged()
{
   json::Object dataJson;
   dataJson["name"] = kUserPrefsProjectLayer;

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      sharedPrefsCache_ = boost::make_shared<json::Object>(
         projects::projectContext().uiPrefs());
      mergePrefs();
      dataJson["values"] = cache_->clone();
   }
   END_LOCK_MUTEX

   ClientEvent event(client_events::kUserPrefsChanged, dataJson);
   module_context::enqueClientEvent(event);
}

void UserPrefsProjectLayer::onPrefsFileChanged()
{
   if (localPrefsFile_.isEmpty())
      return;

   json::Object cacheOld;
   json::Object cacheNew;

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      if (localPrefsFile_.getLastWriteTime() <= lastSync_)
      {
         // No work to do; we wrote this update ourselves.
         return;
      }

      // Snapshot the merged cache prior to reloading
      cacheOld = cache_->clone().getObject();

      // Reload local prefs from file
      FilePath schemaFile = options().rResourcesPath()
         .completePath("schema")
         .completePath(kUserPrefsSchemaFile);
      json::Object prefs;
      Error error = loadPrefsFromFile(localPrefsFile_, schemaFile, &prefs);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      // Update the local prefs cache and rebuild the merged cache
      localPrefsCache_ = boost::make_shared<json::Object>(std::move(prefs));
      mergePrefs();
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

core::Error UserPrefsProjectLayer::readPrefs()
{
   if (projects::projectContext().hasProject())
   {
      // Read local prefs from scratch path
      FilePath localPrefsFile = projects::projectContext().scratchPath()
         .completePath(kUserPrefsFile);

      FilePath schemaFile = options().rResourcesPath()
         .completePath("schema")
         .completePath(kUserPrefsSchemaFile);

      json::Object prefs;
      Error error = loadPrefsFromFile(localPrefsFile, schemaFile, &prefs);
      if (error)
         LOG_ERROR(error);

      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         sharedPrefsCache_ = boost::make_shared<json::Object>(
            projects::projectContext().uiPrefs());
         localPrefsCache_ = boost::make_shared<json::Object>(std::move(prefs));
         localPrefsFile_ = localPrefsFile;
         mergePrefs();
         if (localPrefsFile_.exists())
            lastSync_ = localPrefsFile_.getLastWriteTime();
      }
      END_LOCK_MUTEX

      // Keep pref cache in sync with project config (.Rproj) changes
      projects::projectContext().onConfigChanged.connect(
            boost::bind(&UserPrefsProjectLayer::onProjectConfigChanged, this));

      // After deferred init, start monitoring the local prefs file for changes
      module_context::events().onDeferredInit.connect([this](bool)
      {
         if (!localPrefsFile_.isEmpty())
            monitorPrefsFile(localPrefsFile_);
      });
   }
   else
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         sharedPrefsCache_ = boost::make_shared<json::Object>();
         localPrefsCache_ = boost::make_shared<json::Object>();
         cache_ = boost::make_shared<json::Object>();
      }
      END_LOCK_MUTEX
   }
   return Success();
}

core::Error UserPrefsProjectLayer::writePrefs(const core::json::Object& prefs)
{
   if (localPrefsFile_.isEmpty())
   {
      return fileNotFoundError(ERROR_LOCATION);
   }

   // Filter to only keys in the local project allowlist. This guards against
   // the base class writePref<T>() path, which passes the full merged cache_.
   auto allowed = UserPrefValues::localProjectPrefs();
   json::Object localPrefs;
   for (const auto& member : prefs)
   {
      if (allowed.find(member.getName()) != allowed.end())
         localPrefs[member.getName()] = member.getValue().clone();
   }

   // Write to disk first; only update in-memory state on success
   Error error = writePrefsToFile(localPrefs, localPrefsFile_);
   if (error)
      return error;

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      lastSync_ = localPrefsFile_.getLastWriteTime();
      localPrefsCache_ = boost::make_shared<json::Object>(std::move(localPrefs));
      mergePrefs();
   }
   END_LOCK_MUTEX

   return Success();
}

core::Error UserPrefsProjectLayer::writeLocalPref(const std::string& name,
                                                    const json::Value& value)
{
   json::Object prefs;
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      if (localPrefsCache_)
         prefs = *localPrefsCache_;
   }
   END_LOCK_MUTEX

   prefs[name] = value;
   return writePrefs(prefs);
}

core::json::Object UserPrefsProjectLayer::readLocalPrefs()
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      if (localPrefsCache_)
      {
         return localPrefsCache_->clone().getObject();
      }
   }
   END_LOCK_MUTEX

   return json::Object();
}

} // namespace prefs
} // namespace session
} // namespace rstudio
