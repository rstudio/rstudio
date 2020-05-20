/*
 * UserPrefs.hpp
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

#ifndef SESSION_USER_PREFS_HPP
#define SESSION_USER_PREFS_HPP

#define kUserPrefsFile "rstudio-prefs.json"
#define kUserPrefsSchemaFile "user-prefs-schema.json"

#define kUserPrefsDefaultLayer  "default"
#define kUserPrefsComputedLayer "computed"
#define kUserPrefsSystemLayer   "system"
#define kUserPrefsUserLayer     "user"
#define kUserPrefsProjectLayer  "project"

#include "UserPrefValuesNative.hpp"

#include <shared_core/json/Json.hpp>

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace prefs {

enum PrefLayers
{
   PREF_LAYER_MIN      = 0,

   PREF_LAYER_DEFAULT  = PREF_LAYER_MIN,
   PREF_LAYER_COMPUTED = 1,
   PREF_LAYER_SYSTEM   = 2,
   PREF_LAYER_USER     = 3,
   PREF_LAYER_PROJECT  = 4,

   PREF_LAYER_MAX      = PREF_LAYER_PROJECT
};

UserPrefValuesNative& userPrefs();

core::json::Array allPrefLayers();

core::Error initializePrefs();

core::Error initializeSessionPrefs();

core::Error initializeProjectPrefs();

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif
