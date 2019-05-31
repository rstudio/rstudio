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
namespace modules {
namespace prefs {

core::json::Array Preferences::allLayers()
{
   json::Array layers;
   for (auto layer: layers_)
   {
      layers.push_back(layer->allPrefs());
   }
   return layers;
}

core::Error Preferences::readLayers()
{
   Error error;
   for (auto layer: layers_)
   {
      error = layer->readPrefs();
      if (error)
      {
         LOG_ERROR(error);
      }
   }
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

   for (auto layer: layers_)
   {
      error = layer->validatePrefs();
      if (error)
         LOG_ERROR(error);
   }

   return Success();
}

core::Error Preferences::writeLayer(size_t layer, const core::json::Object& prefs)
{
   if (layer >= layers_.size())
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

   Error result =  layers_[layer]->writePrefs(prefs);
   if (result)
      return result;

   // Empty value indicates that we changed multiple prefs
   onChanged("");
   return Success();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio
