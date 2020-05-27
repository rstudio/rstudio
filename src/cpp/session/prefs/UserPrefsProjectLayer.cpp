/*
 * UserPrefsLayer.cpp
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

#include "UserPrefsProjectLayer.hpp"

#include <core/system/Xdg.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserPrefsProjectLayer::UserPrefsProjectLayer():
   PrefLayer(kUserPrefsProjectLayer)
{
}

void UserPrefsProjectLayer::onProjectConfigChanged()
{
   // Update cache
   cache_ = boost::make_shared<json::Object>(projects::projectContext().uiPrefs());
   
   // Pass new values to client
   json::Object dataJson;
   dataJson["name"] = kUserPrefsProjectLayer;
   dataJson["values"] = *cache_;
   ClientEvent event(client_events::kUserPrefsChanged, dataJson);
   module_context::enqueClientEvent(event);
}

core::Error UserPrefsProjectLayer::readPrefs()
{
   if (projects::projectContext().hasProject())
   {
      // Populate initial cache with project preferences
      cache_ = boost::make_shared<json::Object>(projects::projectContext().uiPrefs());

      // Keep pref cache in sync with project
      projects::projectContext().onConfigChanged.connect(
            boost::bind(&UserPrefsProjectLayer::onProjectConfigChanged, this));

   }
   else
   {
      cache_ = boost::make_shared<json::Object>();
   }
   return Success();
}

} // namespace prefs
} // namespace session
} // namespace rstudio

