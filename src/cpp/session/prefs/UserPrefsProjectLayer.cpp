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

      // Start from public prefs
      if (publicPrefsCache_)
      {
         for (const auto& member : *publicPrefsCache_)
         {
            (*cache_)[member.getName()] = member.getValue();
         }
      }

      // Overlay private prefs (takes precedence)
      if (privatePrefsCache_)
      {
         for (const auto& member : *privatePrefsCache_)
         {
            (*cache_)[member.getName()] = member.getValue();
         }
      }
   }
   END_LOCK_MUTEX
}

void UserPrefsProjectLayer::onProjectConfigChanged()
{
   // Re-read public prefs from .Rproj
   publicPrefsCache_ = boost::make_shared<json::Object>(
      projects::projectContext().uiPrefs());

   // Re-merge with private prefs
   mergePrefs();

   // Pass new values to client
   json::Object dataJson;
   dataJson["name"] = kUserPrefsProjectLayer;
   dataJson["values"] = *cache_;
   ClientEvent event(client_events::kUserPrefsChanged, dataJson);
   module_context::enqueClientEvent(event);
}

void UserPrefsProjectLayer::onPrefsFileChanged()
{
   if (privatePrefsFile_.isEmpty())
      return;

   if (privatePrefsFile_.getLastWriteTime() <= lastSync_)
   {
      // No work to do; we wrote this update ourselves.
      return;
   }

   // Make a copy of the merged cache prior to reloading
   const json::Value oldVal = cache_->clone();
   const json::Object old = oldVal.getObject();

   // Reload the private prefs from file.
   // loadPrefsFromFile writes directly into cache_, so we steal it into privatePrefsCache_.
   FilePath schemaFile = options().rResourcesPath()
      .completePath("schema")
      .completePath(kUserPrefsSchemaFile);
   Error error = loadPrefsFromFile(privatePrefsFile_, schemaFile);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // Steal cache_ (which loadPrefsFromFile just populated) into privatePrefsCache_
   privatePrefsCache_ = cache_;

   // Rebuild the merged cache
   mergePrefs();

   // Diff against old merged cache and emit change notifications
   for (const auto& key : UserPrefValues::allKeys())
   {
      const auto itOld = old.find(key);
      const auto itNew = cache_->find(key);

      if (itNew != cache_->end() &&
          (itOld == old.end() || !((*itNew).getValue() == (*itOld).getValue())))
      {
         onChanged(key);
      }
   }
}

core::Error UserPrefsProjectLayer::readPrefs()
{
   if (projects::projectContext().hasProject())
   {
      // Read public prefs from .Rproj
      publicPrefsCache_ = boost::make_shared<json::Object>(
         projects::projectContext().uiPrefs());

      // Read private prefs from scratch path
      privatePrefsFile_ = projects::projectContext().scratchPath()
         .completePath(kUserPrefsFile);

      FilePath schemaFile = options().rResourcesPath()
         .completePath("schema")
         .completePath(kUserPrefsSchemaFile);

      // loadPrefsFromFile writes directly into cache_, so we use it then steal the result
      Error error = loadPrefsFromFile(privatePrefsFile_, schemaFile);
      if (error)
         LOG_ERROR(error);

      // Steal cache_ into privatePrefsCache_
      privatePrefsCache_ = cache_;

      // Build the merged cache
      mergePrefs();

      // Track the sync time for the private prefs file
      if (privatePrefsFile_.exists())
         lastSync_ = privatePrefsFile_.getLastWriteTime();

      // Keep pref cache in sync with project config (.Rproj) changes
      projects::projectContext().onConfigChanged.connect(
            boost::bind(&UserPrefsProjectLayer::onProjectConfigChanged, this));

      // After deferred init, start monitoring the private prefs file for changes
      module_context::events().onDeferredInit.connect([this](bool)
      {
         if (!privatePrefsFile_.isEmpty())
            monitorPrefsFile(privatePrefsFile_);
      });
   }
   else
   {
      publicPrefsCache_ = boost::make_shared<json::Object>();
      privatePrefsCache_ = boost::make_shared<json::Object>();
      cache_ = boost::make_shared<json::Object>();
   }
   return Success();
}

core::Error UserPrefsProjectLayer::writePrefs(const core::json::Object& prefs)
{
   if (privatePrefsFile_.isEmpty())
   {
      return fileNotFoundError(ERROR_LOCATION);
   }

   // Update the private prefs cache
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      privatePrefsCache_ = boost::make_shared<json::Object>(prefs);
   }
   END_LOCK_MUTEX

   // Write private prefs to disk
   Error error = writePrefsToFile(prefs, privatePrefsFile_);
   if (!error)
   {
      lastSync_ = privatePrefsFile_.getLastWriteTime();
   }

   // Rebuild the merged cache
   mergePrefs();

   return error;
}

core::Error UserPrefsProjectLayer::writePrivatePref(const std::string& name,
                                                    const json::Value& value)
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      if (!privatePrefsCache_)
         privatePrefsCache_ = boost::make_shared<json::Object>();

      (*privatePrefsCache_)[name] = value;
   }
   END_LOCK_MUTEX

   return writePrefs(*privatePrefsCache_);
}

core::json::Object UserPrefsProjectLayer::readPrivatePrefs()
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      if (privatePrefsCache_)
         return *privatePrefsCache_;
   }
   END_LOCK_MUTEX

   return json::Object();
}

} // namespace prefs
} // namespace session
} // namespace rstudio
