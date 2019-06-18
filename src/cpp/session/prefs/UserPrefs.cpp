/*
 * UserPrefs.cpp
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

#include <core/system/Xdg.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/json/rapidjson/schema.h>

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
   Error createLayers()
   {
      LOCK_MUTEX(mutex_)
      {
         layers_.push_back(boost::make_shared<UserPrefsDefaultLayer>());  // PREF_LAYER_DEFAULT
         layers_.push_back(boost::make_shared<UserPrefsSystemLayer>());   // PREF_LAYER_SYSTEM
         layers_.push_back(boost::make_shared<UserPrefsLayer>());         // PREF_LAYER_USER
         layers_.push_back(boost::make_shared<UserPrefsProjectLayer>());  // PREF_LAYER_PROJECT
      }
      END_LOCK_MUTEX

      return Success();
   }

   int userLayer()
   {
      return PREF_LAYER_USER;
   }

public:

   Error createComputedLayer()
   {
      // The computed layer is created later since computations may involve evaluating R code (which
      // we can't do in early init)
      LOCK_MUTEX(mutex_)
      {
         auto layer = boost::make_shared<UserPrefsComputedLayer>();
         Error error = layer->readPrefs();
         if (error)
            return error;

         layers_.insert(layers_.begin() + PREF_LAYER_COMPUTED, layer);
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
   return userPrefs().initialize();
}

Error initializeSessionPrefs()
{
   UserPrefs& instance = static_cast<UserPrefs&>(userPrefs());
   return instance.createComputedLayer();
}

} // namespace prefs
} // namespace session
} // namespace rstudio
