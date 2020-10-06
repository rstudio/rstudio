/*
 * SessionUserState.hpp
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

#ifndef SESSION_USER_STATE_HPP
#define SESSION_USER_STATE_HPP

#define kUserStateFileDesktop "rstudio-desktop.json"
#define kUserStateFileServer "rstudio-server.json"
#define kUserStateSchemaFile "user-state-schema.json"

#include "UserStateValues.hpp"

#include <shared_core/json/Json.hpp>

#define kUserStateDefaultLayer  "default"
#define kUserStateComputedLayer "computed"
#define kUserStateUserLayer     "user"

enum StateLayer
{
   STATE_LAYER_MIN      = 0,

   STATE_LAYER_DEFAULT  = STATE_LAYER_MIN,
   STATE_LAYER_COMPUTED = 1,
   STATE_LAYER_USER     = 2,

   STATE_LAYER_MAX      = STATE_LAYER_USER
};

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace prefs {

UserStateValues& userState();

core::json::Array allStateLayers();

core::Error initializeState();

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif
