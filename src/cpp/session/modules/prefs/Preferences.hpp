/*
 * Preferences.hpp
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

#ifndef SESSION_PREFERENCES_HPP
#define SESSION_PREFERENCES_HPP

#include <core/json/Json.hpp>

#include "PrefLayer.hpp"

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {

class Preferences
{
public:
   core::Error initialize();
   core::json::Array allLayers();
   core::Error writeLayer(size_t layer, const core::json::Object& prefs);

   virtual core::Error createLayers() = 0;
protected:
   core::Error readLayers();
   std::vector<boost::shared_ptr<PrefLayer>> layers_;
};

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

#endif

