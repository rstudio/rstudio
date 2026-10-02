/*
 * UserState.cpp
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

#include <shared_core/Memory.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/Exec.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>

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
   // intentionally leaked: other threads can read state while the process
   // exits, so this must never be destroyed during static teardown (#18318)
   static UserState& instance = make_leaked<UserState>();
   return instance;
}

Error initializeState()
{
   Error error = userState().initialize();
   if (error)
      return error;

   // this must happen before a project is opened, since the project's scratch
   // path is named after the context ID
   ensureContextId();

   return Success();
}

void ensureContextId()
{
   if (!userState().contextId().empty())
      return;

   std::string contextId = core::system::generateShortenedUuid();
   Error error = userState().setContextId(contextId);
   if (!error)
      return;

   // the state file can't be written (e.g. it was left owned by root by a sudo
   // run), so the ID wasn't kept. keep it for this session anyway: project
   // state is stored under it, and with an empty ID it would land in the
   // project's .Rproj.user directory itself
   LOG_ERROR(error);

   json::Object computed;
   computed[kContextId] = contextId;
   error = userState().writeLayer(STATE_LAYER_COMPUTED, computed);
   if (error)
      LOG_ERROR(error);
}

} // namespace prefs
} // namespace session
} // namespace rstudio
