/*
 * Preferences.hpp
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

#ifndef SESSION_PREFERENCES_HPP
#define SESSION_PREFERENCES_HPP

#include <shared_core/json/Json.hpp>

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
   Preferences();
   core::Error initialize();
   bool initialized();
   core::json::Array allLayers();
   core::json::Object userPrefLayer();
   core::json::Object getLayer(const std::string& name);
   core::Error writeLayer(int layer, const core::json::Object& prefs);

   template <typename T> T readPref(const std::string& name)
   {
      // Work backwards through the layers, starting with the most specific (project or user-level
      // settings) and working towards the most general (basic defaults)
      RECURSIVE_LOCK_MUTEX(mutex_)
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
      auto layer = layers_[userLayer()];
      if (!layer)
      {
         core::Error error(core::json::errc::ParamMissing, ERROR_LOCATION);
         error.addProperty("description", "missing user layer for preference '" + 
               name + "'");
         return error;
      }
      if (layer->readPref<T>(name) != value)
      {
         // Write a new value when changing the value
         err = layer->writePref(name, value);
         onChanged(layer->layerName(), name);
      }

      return err;
   }

   // Read/write accessors for the underlying JSON property values
   boost::optional<core::json::Value> readValue(const std::string& layerName, 
         const std::string& name);
   boost::optional<core::json::Value> readValue(const std::string& name,
         std::string* pLayerName = nullptr);
   core::Error writeValue(const std::string& name, const core::json::Value& value);
   core::Error clearValue(const std::string& name);

   void notifyClient(const std::string& layer, const std::string& pref);

   virtual core::Error createLayers() = 0;
   virtual int userLayer() = 0;
   virtual int clientChangedEvent() = 0;
   void destroyLayers();

   // Signal emitted when preferences change; includes the layer name and value name
   RSTUDIO_BOOST_SIGNAL<void(const std::string&, const std::string&)> onChanged;

protected:
   virtual void onPrefLayerChanged(const std::string& layerName, const std::string& prefName);
   core::Error readLayers();
   std::vector<boost::shared_ptr<PrefLayer>> layers_;
   boost::recursive_mutex mutex_;
   bool initialized_;
};

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif

