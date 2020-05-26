/*
 * UserPrefsLayer.hpp
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

#ifndef SESSION_USER_PREF_LAYER_HPP
#define SESSION_USER_PREF_LAYER_HPP

#include <session/prefs/PrefLayer.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace prefs {

class UserPrefsLayer: public PrefLayer
{
public:
   UserPrefsLayer();
   core::Error readPrefs() override;
   core::Error writePrefs(const core::json::Object &prefs) override;

protected:
   void onPrefsFileChanged() override;

private:
   // The time at which we last sync'ed the state of the prefs file with the prefs cache
   std::time_t lastSync_;

   // The path we're reading the preference file from
   core::FilePath prefsFile_;
};

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif
