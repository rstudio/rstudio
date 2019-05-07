/*
 * SessionUserPrefs.hpp
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

#ifndef SESSION_USER_PREFS_HPP
#define SESSION_USER_PREFS_HPP

#include <core/json/Json.hpp>

#define kUserPrefsFile "rstudio-prefs.json"
#define kUserPrefsSchemaFile "user-prefs-schema.json"

enum PrefLayer
{
   PREF_LAYER_MIN     = 0,

   PREF_LAYER_DEFAULT = PREF_LAYER_MIN,
   PREF_LAYER_SYSTEM  = 1,
   PREF_LAYER_USER    = 2,
   PREF_LAYER_PROJECT = 3,

   PREF_LAYER_MAX     = PREF_LAYER_PROJECT
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

core::json::Array userPrefs();

core::Error initializePrefs();

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif

