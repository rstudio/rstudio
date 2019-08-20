/*
 * UserStateLayer.cpp
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

#include "UserStateLayer.hpp"

#include <core/system/Xdg.hpp>

#include <session/SessionOptions.hpp>
#include <session/prefs/UserState.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserStateLayer::UserStateLayer():
   PrefLayer(kUserStateUserLayer)
{
}

core::Error UserStateLayer::readPrefs()
{
   prefsFile_ = core::system::xdg::userDataDir().complete(kUserStateFile);

   return loadPrefsFromFile(prefsFile_);
}

core::Error UserStateLayer::writePrefs(const core::json::Object &prefs)
{
   if (prefsFile_.empty())
   {
      return fileNotFoundError(ERROR_LOCATION);
   }

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      *cache_ = prefs;
   }
   END_LOCK_MUTEX

   return writePrefsToFile(*cache_, prefsFile_);
}

core::Error UserStateLayer::validatePrefs()
{
   return validatePrefsFromSchema(
      options().rResourcesPath().complete("schema").complete(kUserStateSchemaFile));
}

} // namespace prefs
} // namespace session
} // namespace rstudio

