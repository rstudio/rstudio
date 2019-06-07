/*
 * UserPrefsLayer.cpp
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

#include "UserPrefsLayer.hpp"

#include <core/system/Xdg.hpp>

#include <session/SessionOptions.hpp>
#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

core::Error UserPrefsLayer::readPrefs()
{
   prefsFile_ = core::system::xdg::userConfigDir().complete(kUserPrefsFile);
   return loadPrefsFromFile(prefsFile_);
}

core::Error UserPrefsLayer::writePrefs(const core::json::Object &prefs)
{
   if (prefsFile_.empty())
   {
      return fileNotFoundError(ERROR_LOCATION);
   }
   return writePrefsToFile(prefs, prefsFile_);
}

core::Error UserPrefsLayer::validatePrefs()
{
   return validatePrefsFromSchema(
      options().rResourcesPath().complete("schema").complete(kUserPrefsSchemaFile));
}

} // namespace prefs
} // namespace session
} // namespace rstudio

