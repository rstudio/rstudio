/*
 * UserState.cpp
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

#include <core/json/JsonRpc.hpp>

#include <core/Exec.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include <session/prefs/UserStateValues.hpp>
#include <session/prefs/Preferences.hpp>

#include "UserStateDefaultLayer.hpp"
#include "UserStateComputedLayer.hpp"
#include "UserStateLayer.hpp"
#include "UserState.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {
namespace {

class UserState: public UserStateValues
{
   Error createLayers()
   {
      layers_.push_back(boost::make_shared<UserStateDefaultLayer>()) ;  // STATE_LAYER_DEFAULT
      layers_.push_back(boost::make_shared<UserStateComputedLayer>());  // STATE_LAYER_COMPUTED
      layers_.push_back(boost::make_shared<UserStateLayer>());          // STATE_LAYER_USER
      return Success();
   }

   int userLayer()
   {
      return STATE_LAYER_USER;
   }
} s_state;

Error setState(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   json::Value val;
   Error error = json::readParams(request.params, &val);
   if (error)
      return error;

   s_state.writeLayer(STATE_LAYER_USER, val.get_obj()); 

   return Success();
}
} // anonymous namespace

json::Array allStateLayers()
{
   return s_state.allLayers();
}

Error initializeState()
{
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_state", setState));
   Error error = initBlock.execute();
   if (error)
      return error;

   error = s_state.initialize();
   if (error)
      return error;

   return Success();
}

} // namespace state
} // namespace session
} // namespace rstudio
