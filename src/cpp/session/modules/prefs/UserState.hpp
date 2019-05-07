/*
 * SessionUserState.hpp
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

#ifndef SESSION_USER_STATE_HPP
#define SESSION_USER_STATE_HPP

#include <core/json/Json.hpp>

#define kUserStateFile "rstudio-state.json"
#define kUserStateSchemaFile "user-state-schema.json"

enum StateLayer
{
   STATE_LAYER_MIN     = 0,

   STATE_LAYER_DEFAULT = STATE_LAYER_MIN,
   STATE_LAYER_USER    = 1,

   STATE_LAYER_MAX     = STATE_LAYER_USER
};

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {

core::json::Array userState();

core::Error initializeState();

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif

