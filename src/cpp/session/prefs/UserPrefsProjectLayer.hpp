/*
 * UserPrefsProjectLayer.hpp
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

#ifndef SESSION_USER_PREF_PROJECT_LAYER_HPP
#define SESSION_USER_PREF_PROJECT_LAYER_HPP

#include <session/prefs/PrefLayer.hpp>

namespace rstudio {
namespace session {
namespace prefs {

// The project layer maintains two independent source caches and merges them
// into the base class cache_ that the rest of the preference system reads from:
//
//   publicPrefsCache_   UI prefs embedded in the .Rproj file (shared across users).
//   privatePrefsCache_  Per-user prefs stored in .Rproj.user/<ctx>/rstudio-prefs.json.
//   cache_              (inherited) The merged view: public prefs overlaid with private prefs.
//
// mergePrefs() rebuilds cache_ from the two source caches. Private prefs take
// precedence over public prefs when both define the same key. The source caches
// must be deep-copied into cache_ (via json::Value::clone()) because json::Value
// assignment is destructive (move semantics).
class UserPrefsProjectLayer: public PrefLayer
{
public:
   UserPrefsProjectLayer();
   core::Error readPrefs() override;
   core::Error writePrefs(const core::json::Object& prefs) override;

   core::Error writePrivatePref(const std::string& name, const core::json::Value& value);
   core::json::Object readPrivatePrefs();

private:
   void onProjectConfigChanged();
   void onPrefsFileChanged() override;
   void mergePrefs();

   core::FilePath privatePrefsFile_;
   boost::shared_ptr<core::json::Object> publicPrefsCache_;
   boost::shared_ptr<core::json::Object> privatePrefsCache_;
   std::time_t lastSync_;
};

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif
