/*
 * SessionApiPrefs.hpp
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

#ifndef SESSION_MODULE_API_PREFS_HPP
#define SESSION_MODULE_API_PREFS_HPP

#include <session/prefs/Preferences.hpp>

#define kApiPrefsFile "rstudioapi-prefs.json"

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {

/**
 * This class represents the preferences set from the RStudio API (rstudioapi package). These
 * preferences don't have a schema; they are arbitrary key/value pairs defined by rstudioapi package
 * users. API preferences are stored in a separate file from core IDE preferences. 
 */
class ApiPrefs: public session::prefs::Preferences
{
public:
   core::Error createLayers();
   int userLayer();
   int clientChangedEvent();
};

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
