/*
 * UserPrefs.cpp
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

#include <core/system/Xdg.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <shared_core/json/rapidjson/schema.h>

#include <core/Exec.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "UserPrefsDefaultLayer.hpp"
#include "UserPrefsComputedLayer.hpp"
#include "UserPrefsLayer.hpp"
#include "UserPrefsSystemLayer.hpp"
#include "UserPrefsProjectLayer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {
namespace {

class UserPrefs: public UserPrefValuesNative
{
   Error createLayers() override
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         // Create the initial layers (which just rely on files on disk)
         layers_.push_back(boost::make_shared<UserPrefsDefaultLayer>());  // PREF_LAYER_DEFAULT
         layers_.push_back(boost::make_shared<UserPrefsSystemLayer>());   // PREF_LAYER_SYSTEM
         layers_.push_back(boost::make_shared<UserPrefsLayer>());         // PREF_LAYER_USER
      }
      END_LOCK_MUTEX

      return Success();
   }

   int userLayer() override
   {
      return PREF_LAYER_USER;
   }

   int clientChangedEvent() override
   {
      return client_events::kUserPrefsChanged;
   }

   void onPrefLayerChanged(const std::string& layerName, const std::string& prefName) override
   {
      Preferences::onPrefLayerChanged(layerName, prefName);

      // Fire an event notifying the client that prefs have changed
      json::Object valueJson;
      auto val = readValue(layerName, prefName);
      if (val)
      {
         valueJson[prefName] = *val;
         json::Object dataJson;
         dataJson["name"] = layerName;
         dataJson["values"] = valueJson;
         ClientEvent event(client_events::kUserPrefsChanged, dataJson);
         module_context::enqueClientEvent(event);
      }
   }

public:

   Error createComputedLayer()
   {
      // The computed layer is created later since computations may involve evaluating R code
      // (which we can't do in early init)
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         auto computed = boost::make_shared<UserPrefsComputedLayer>();
         Error error = computed->readPrefs();
         if (error)
            return error;

         layers_.insert(layers_.begin() + PREF_LAYER_COMPUTED, computed);

      }
      END_LOCK_MUTEX

      return Success();
   }

   Error createProjectLayer()
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         auto project = boost::make_shared<UserPrefsProjectLayer>();
         Error error = project->readPrefs();
         if (error)
            return error;

         layers_.push_back(project);

      }
      END_LOCK_MUTEX

      return Success();
   }
};

} // anonymous namespace

json::Array allPrefLayers()
{
   return userPrefs().allLayers();
}

UserPrefValuesNative& userPrefs()
{
   static UserPrefs instance;
   return instance;
}

Error initializePrefs()
{
   // Ensure that the user folder (where we'll store the preferences) exists; this is non-fatal so
   // don't return it
   Error error = core::system::xdg::userConfigDir().ensureDirectory();
   if (error)
      LOG_ERROR(error);

   return userPrefs().initialize();
}

Error initializeSessionPrefs()
{
   UserPrefs& instance = static_cast<UserPrefs&>(userPrefs());
   return instance.createComputedLayer();
}

Error initializeProjectPrefs()
{
   UserPrefs& instance = static_cast<UserPrefs&>(userPrefs());
   return instance.createProjectLayer();
}

} // namespace prefs
} // namespace session
} // namespace rstudio
