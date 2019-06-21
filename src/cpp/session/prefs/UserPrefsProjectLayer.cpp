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

#include "UserPrefsProjectLayer.hpp"

#include <core/system/Xdg.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserPrefsProjectLayer::UserPrefsProjectLayer():
   PrefLayer(kUserPrefsProjectLayer)
{
}

core::Error UserPrefsProjectLayer::readPrefs()
{
   if (projects::projectContext().hasProject())
   {
      cache_ = boost::make_shared<json::Object>(projects::projectContext().uiPrefs());
   }
   else
   {
      cache_ = boost::make_shared<json::Object>();
   }
   return Success();
}

core::Error UserPrefsProjectLayer::validatePrefs()
{
   // Project level prefs can't be invalid.
   return Success();
}

} // namespace prefs
} // namespace session
} // namespace rstudio

