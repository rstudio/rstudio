/*
 * PrefLayer.hpp
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

#ifndef SESSION_PREF_LAYER_HPP
#define SESSION_PREF_LAYER_HPP

#include <core/json/Json.hpp>

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {

class PrefLayer
{
public:
   virtual core::Error readPrefs() = 0;
   virtual core::Error writePrefs(const core::json::Object& prefs);
   virtual core::Error validatePrefs() = 0;
   virtual ~PrefLayer();

   core::json::Object allPrefs();
   core::Error validatePrefsFromSchema(const core::FilePath& schemaFile);

protected:
   core::Error loadPrefsFromFile(const core::FilePath& prefsFile);
   core::Error loadPrefsFromSchema(const core::FilePath& schemaFile);
   core::Error writePrefsToFile(const core::json::Object& prefs, const core::FilePath& prefsFile);

   boost::shared_ptr<core::json::Object> cache_;
};

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif

