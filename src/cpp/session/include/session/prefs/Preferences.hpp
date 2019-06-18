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

#include <boost/range/adaptor/reversed.hpp>

#include <core/BoostSignals.hpp>
#include <core/Thread.hpp>

#include "PrefLayer.hpp"

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace prefs {

class Preferences
{
public:
   core::Error initialize();
   core::json::Array allLayers();
   core::json::Object userPrefLayer();
   core::Error writeLayer(size_t layer, const core::json::Object& prefs);

   template <typename T> T readPref(const std::string& name)
   {
      // Work backwards through the layers, starting with the most specific (project or user-level
      // settings) and working towards the most general (basic defaults)
      LOCK_MUTEX(mutex_)
      {
         for (auto layer: boost::adaptors::reverse(layers_))
         {
            boost::optional<T> val = layer->readPref<T>(name);
            if (val)
            {
               return *val;
            }
         }
      }
      END_LOCK_MUTEX
      
      // Every value must have a default (and we enforce this with tests), so it's an error to reach
      // this code. Return zero-initialized value in this case.
      core::Error error(core::json::errc::ParamMissing, ERROR_LOCATION);
      error.addProperty("description", "missing default value for preference '" + 
            name + "'");
      LOG_ERROR(error);
      return T();
   }


   template <typename T> core::Error writePref(const std::string& name, T value)
   {
      core::Error err;
      LOCK_MUTEX(mutex_)
      {
         auto layer = layers_[userLayer()];
         onChanged(name);
         err = layer->writePref(name, value);
      }
      END_LOCK_MUTEX

      return err;
   }

   // Read/write accessors for the underlying JSON property values
   boost::optional<core::json::Value> readValue(const std::string& name);
   core::Error writeValue(const std::string& name, const core::json::Value& value);
   core::Error clearValue(const std::string& name);

   virtual core::Error createLayers() = 0;
   virtual int userLayer() = 0;

   // Signal emitted when preferences change; can include the pref name when only one pref chagnes.
   RSTUDIO_BOOST_SIGNAL<void(const std::string&)> onChanged;

protected:
   core::Error readLayers();
   std::vector<boost::shared_ptr<PrefLayer>> layers_;
   boost::mutex mutex_;
};

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif

