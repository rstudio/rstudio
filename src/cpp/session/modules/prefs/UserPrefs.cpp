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

#include "UserPrefs.hpp"
#include "UserPrefsDefaultLayer.hpp"
#include "UserPrefsLayer.hpp"
#include "UserPrefsSystemLayer.hpp"
#include "UserPrefsProjectLayer.hpp"
#include "Preferences.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

class UserPrefs: public Preferences
{
   Error createLayers()
   {
      layers_.push_back(boost::make_shared<UserPrefsDefaultLayer>());  // PREF_LAYER_DEFAULT
      layers_.push_back(boost::make_shared<UserPrefsSystemLayer>());   // PREF_LAYER_USER
      layers_.push_back(boost::make_shared<UserPrefsLayer>());         // PREF_LAYER_SYSTEM
      layers_.push_back(boost::make_shared<UserPrefsProjectLayer>());  // PREF_LAYER_PROJECT
      return Success();
   }
} s_prefs;

Error setPreferences(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   json::Value val;
   Error error = json::readParams(request.params, &val);
   if (error)
      return error;

   s_prefs.writeLayer(PREF_LAYER_USER, val.get_obj()); 

   return Success();
}

} // anonymous namespace

json::Array userPrefs()
{
   return s_prefs.allLayers();
}

Error initializePrefs()
{
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_user_prefs", setPreferences));
   Error error = initBlock.execute();
   if (error)
      return error;

   error = s_prefs.initialize();
   if (error)
      return error;

   return Success();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio
