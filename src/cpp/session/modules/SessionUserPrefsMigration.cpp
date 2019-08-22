/*
 * SessionUserPrefsMigration.cpp
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

#include "SessionUserPrefsMigration.hpp"

#include <core/json/Json.hpp>
#include <core/Settings.hpp>

#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

void migrateUiPrefs(const json::Object& uiPrefs, json::Object* dest)
{
   std::vector<std::string> keys = userPrefs().allKeys();
   json::Value val;
   for (const auto &key: keys)
   {
      // Migrate pane locations
      if (key == kPanes)
      {
         json::Object::iterator it = uiPrefs.find("pane_config");
         if (it != uiPrefs.end())
         {
            json::Object old = (*it).value().get_obj();
            json::Object panes;
            panes[kPanesQuadrants] = old["panes"];
            panes[kPanesTabSet1] = old["tabSet1"];
            panes[kPanesTabSet2] = old["tabSet2"];
            panes[kPanesConsoleLeftOnTop] = old["consoleLeftOnTop"];
            panes[kPanesConsoleRightOnTop] = old["consoleRightOnTop"];

            (*dest)[kPanes] = panes;
         }
      }
   }
}

} // anonymous namespace


core::Error migratePrefs(const FilePath& src)
{
   json::Object srcPrefs;
   json::Object destPrefs;

   // Read the old settings file
   Settings settings; 
   Error err = settings.initialize(src);
   if (err)
      return err;

   // Migrate UI prefs if present.
   std::string uiPrefs = settings.get("uiPrefs");
   if (!uiPrefs.empty())
   {
      json::Value val;
      err = json::parse(uiPrefs, ERROR_LOCATION, &val);
      if (err)
      {
         LOG_ERROR(err);
      }
      else if (val.type() == json::ObjectType)
      {
         migrateUiPrefs(val.get_obj(), &destPrefs);
      }
   }

   std::stringstream oss;
   json::writeFormatted(destPrefs, oss);

   return userPrefs().writeLayer(PREF_LAYER_USER, destPrefs); 
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

