/*
 * Preferences.cpp
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


#include <session/prefs/Preferences.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

core::json::Array Preferences::allLayers()
{
   json::Array layers;
   LOCK_MUTEX(mutex_)
   {
      for (auto layer: layers_)
      {
         layers.push_back(layer->allPrefs());
      }
   }
   END_LOCK_MUTEX
   return layers;
}

core::Error Preferences::readLayers()
{
   Error error;
   LOCK_MUTEX(mutex_)
   {
      for (auto layer: layers_)
      {
         error = layer->readPrefs();
         if (error)
         {
            LOG_ERROR(error);
         }
      }
   }
   END_LOCK_MUTEX
   return Success();
}

core::Error Preferences::initialize()
{
   Error error = createLayers();
   if (error)
      return error;

   error = readLayers();
   if (error)
      return error;

   LOCK_MUTEX(mutex_)
   {
      for (auto layer: layers_)
      {
         // Validate the layer and log errors for violations
         error = layer->validatePrefs();
         if (error)
            LOG_ERROR(error);

         // Subscribe for layer change notifications
         layer->onChanged.connect(boost::bind(&Preferences::onPrefLayerChanged, this));
      }
   }
   END_LOCK_MUTEX

   return Success();
}

core::Error Preferences::writeLayer(size_t layer, const core::json::Object& prefs)
{
   Error result;
   LOCK_MUTEX(mutex_)
   {
      // We cannot write the base layer or a non-existent layer.
      if (layer >= layers_.size() || layer < 1)
         return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

      // Write only the unique values in this layer.
      json::Object unique;
      for (const auto pref: prefs)
      {
         // Check to see whether the value for this preference (a) exists in some other lower layer,
         // and if so (b) whether it differs from the value in that layer.
         bool found = false;
         bool differs = false;
         for (size_t i = layer - 1; i >= 0; --i)
         {
            const auto val = layers_[i]->readValue(pref.name());
            if (val)
            {
               found = true;
               if (!(*val == pref.value()))
               {
                  differs = true;
               }
               break;
            }
         }

         if (!found || differs)
         {
            // If the preference doesn't exist in any other layer, or the value doesn't match the them,
            // record the unique value in this layer.
            unique[pref.name()] = pref.value();
         }
      }

      result = layers_[layer]->writePrefs(unique);
   }
   END_LOCK_MUTEX;

   if (result)
      return result;

   // Empty value indicates that we changed multiple prefs
   // TODO: fire event
   // onChanged("");
   return Success();
}

boost::optional<core::json::Value> Preferences::readValue(const std::string& name)
{
   // Work backwards through the layers, starting with the most specific (project or user-level
   // settings) and working towards the most general (basic defaults)
   LOCK_MUTEX(mutex_)
   {
      for (const auto layer: boost::adaptors::reverse(layers_))
      {
         boost::optional<core::json::Value> val = layer->readValue(name);
         if (val && val->type() != json::NullType && val->type() != json::UnknownType)
         {
            return val;
         }
      }
   }
   END_LOCK_MUTEX

   return boost::none;
}

core::Error Preferences::writeValue(const std::string& name, const core::json::Value& value)
{
   Error result;
   LOCK_MUTEX(mutex_)
   {
      auto layer = layers_[userLayer()];
      result = layer->writePref(name, value);
   }
   END_LOCK_MUTEX

   // Make sure to keep this outside the mutex lock since prefs are typically read after being
   // changed.
   onChanged(name);

   return result;
}

core::json::Object Preferences::userPrefLayer()
{
   core::json::Object layer;
   LOCK_MUTEX(mutex_)
   {
      layer = layers_[userLayer()]->allPrefs();
   }
   END_LOCK_MUTEX
   return layer;
}

Error Preferences::clearValue(const std::string &name)
{
   Error result;
   LOCK_MUTEX(mutex_)
   {
      result =layers_[userLayer()]->clearValue(name);
   }
   END_LOCK_MUTEX
   return result;
}

void Preferences::onPrefLayerChanged()
{
}

} // namespace prefs
} // namespace session
} // namespace rstudio
