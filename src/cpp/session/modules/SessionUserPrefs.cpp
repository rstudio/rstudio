/*
 * SessionUserPrefs.cpp
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

#include "SessionUserPrefs.hpp"

#include <core/Exec.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

Error setPreferences(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   json::Value val;
   Error error = json::readParams(request.params, &val);
   if (error)
      return error;

   return userPrefs().writeLayer(PREF_LAYER_USER, val.get_obj()); 
}

Error setState(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   json::Value val;
   Error error = json::readParams(request.params, &val);
   if (error)
      return error;

   userState().writeLayer(STATE_LAYER_USER, val.get_obj()); 

   module_context::events().onPreferencesSaved();

   return Success();
}

} // anonymous namespace

core::Error initialize()
{
   using namespace module_context;
   Error error = initializeSessionPrefs();
   if (error)
      return error;
   
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_user_prefs", setPreferences))
      (bind(registerRpcMethod, "set_state", setState));
   return initBlock.execute();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

