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
#include <core/json/JsonRpc.hpp>

namespace rstudio {
   namespace core {
      class Error;
   }
}

namespace rstudio {
namespace session {
namespace prefs {

class PrefLayer
{
public:
   virtual core::Error readPrefs() = 0;
   virtual core::Error writePrefs(const core::json::Object& prefs);
   virtual core::Error validatePrefs() = 0;
   virtual ~PrefLayer();

   template<typename T> boost::optional<T> readPref(const std::string& name)
   {
      // Ensure we have a cache from which to read preferences
      if (!cache_)
      {
         LOG_WARNING_MESSAGE("Attempt to look up preference '" + name + "' before preferences "
               "were read");
         return boost::none;
      }

      // Locate the preference in the cache
      auto it = cache_->find(name);
      if (it != cache_->end())
      {
         // Ensure the preference we found is of the correct type. 
         if (!core::json::isType<T>((*it).value()))
         {  
            core::Error error(core::json::errc::ParamTypeMismatch, ERROR_LOCATION);
            error.addProperty("description", "unexpected type "
                  "'" + core::json::typeAsString((*it).value()) + "'"
                  " for preference '" + name + "'");
            LOG_ERROR(error);
            return boost::none;
         }

         // Return the preference
         return (*it).value().get_value<T>();
      }
      return boost::none;
   };

   template <typename T> core::Error writePref(const std::string& name, T value)
   {
      // Ensure we have a cache to use as a baseline for writing
      if (!cache_)
      {
         core::Error error(core::json::errc::ParamMissing, ERROR_LOCATION);
         error.addProperty("description", "cannot write property '" + name + "' before reading it");
         return error;
      }
      (*cache_)[name] = value;
      return writePrefs(*cache_);
   }

   core::json::Object allPrefs();
   boost::optional<core::json::Value> readValue(const std::string& name);
   core::Error validatePrefsFromSchema(const core::FilePath& schemaFile);

protected:
   core::Error loadPrefsFromFile(const core::FilePath& prefsFile);
   core::Error loadPrefsFromSchema(const core::FilePath& schemaFile);
   core::Error writePrefsToFile(const core::json::Object& prefs, const core::FilePath& prefsFile);

   boost::shared_ptr<core::json::Object> cache_;
};

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif

