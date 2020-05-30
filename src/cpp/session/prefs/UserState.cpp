/*
 * UserState.cpp
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

#include <core/json/JsonRpc.hpp>

#include <core/Exec.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include <session/prefs/UserStateValues.hpp>
#include <session/prefs/Preferences.hpp>
#include <session/prefs/UserState.hpp>

#include "UserStateDefaultLayer.hpp"
#include "UserStateComputedLayer.hpp"
#include "UserStateLayer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {
namespace {

class UserState: public UserStateValues
{
   Error createLayers() override
   {
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         layers_.push_back(boost::make_shared<UserStateDefaultLayer>()) ;  // STATE_LAYER_DEFAULT
         layers_.push_back(boost::make_shared<UserStateComputedLayer>());  // STATE_LAYER_COMPUTED
         layers_.push_back(boost::make_shared<UserStateLayer>());          // STATE_LAYER_USER
      }
      END_LOCK_MUTEX
      return Success();
   }

   int userLayer() override
   {
      return STATE_LAYER_USER;
   }

   int clientChangedEvent() override
   {
      return client_events::kUserStateChanged;
   }

   void onPrefLayerChanged(const std::string& layerName, const std::string& prefName) override
   {
      Preferences::onPrefLayerChanged(layerName, prefName);

      // Fire an event notifying the client that state has changed
      json::Object valueJson;
      auto val = readValue(layerName, prefName);
      if (val)
      {
         valueJson[prefName] = *val;
         json::Object dataJson;
         dataJson["name"] = layerName;
         dataJson["values"] = valueJson;
         ClientEvent event(client_events::kUserStateChanged, dataJson);
         module_context::enqueClientEvent(event);
      }
   }
};

} // anonymous namespace

json::Array allStateLayers()
{
   return userState().allLayers();
}

UserStateValues& userState()
{
   static UserState instance;
   return instance;
}

Error initializeState()
{
   return userState().initialize();
}

} // namespace prefs
} // namespace session
} // namespace rstudio
