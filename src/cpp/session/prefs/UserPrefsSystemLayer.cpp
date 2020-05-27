/*
 * UserPrefsSystemLayer.cpp
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
   return loadPrefsFromFile(
      core::system::xdg::systemConfigFile(kUserPrefsFile),
      options().rResourcesPath().completePath("schema").completePath(kUserPrefsSchemaFile));
      
}

} // namespace prefs
} // namespace session
} // namespace rstudio

