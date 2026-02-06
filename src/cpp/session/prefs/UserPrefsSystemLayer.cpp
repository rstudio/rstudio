/*
 * UserPrefsSystemLayer.cpp
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

#include "UserPrefsSystemLayer.hpp"

#include <core/system/Xdg.hpp>

#include <session/SessionOptions.hpp>
#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserPrefsSystemLayer::UserPrefsSystemLayer():
   PrefLayer(kUserPrefsSystemLayer)
{
}

core::Error UserPrefsSystemLayer::readPrefs()
{
   json::Object prefs;
   Error error = loadPrefsFromFile(
      core::system::xdg::systemConfigFile(kUserPrefsFile),
      options().rResourcesPath().completePath("schema").completePath(kUserPrefsSchemaFile),
      &prefs);

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      cache_ = boost::make_shared<json::Object>(std::move(prefs));
   }
   END_LOCK_MUTEX

   return error;
}

} // namespace prefs
} // namespace session
} // namespace rstudio

