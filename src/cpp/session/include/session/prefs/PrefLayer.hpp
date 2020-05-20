/*
 * PrefLayer.hpp
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

#ifndef SESSION_PREF_LAYER_HPP
#define SESSION_PREF_LAYER_HPP

#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#include <core/system/FileMonitor.hpp>
#include <core/system/FileChangeEvent.hpp>

#include <core/BoostSignals.hpp>
#include <core/FileInfo.hpp>
#include <core/Thread.hpp>

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
   PrefLayer(const std::string& layerName);
   virtual core::Error readPrefs() = 0;
   virtual core::Error writePrefs(const core::json::Object& prefs);
   virtual ~PrefLayer();
   virtual void destroy();
   std::string layerName();

   template<typename T> boost::optional<T> readPref(const std::string& name)
   {
      // Ensure we have a cache from which to read preferences
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         if (!cache_)
         {
            LOG_WARNING_MESSAGE("Attempt to look up preference '" + name + "' before preference "
                  "layer '" + layerName() + "' was read");
            return boost::none;
         }

         // Locate the preference in the cache
         const auto it = cache_->find(name);
         if (it != cache_->end())
         {
            // Ensure the preference we found is of the correct type. 
            if (!core::json::isType<T>((*it).getValue()))
            {  
               core::Error error(core::json::errc::ParamTypeMismatch, ERROR_LOCATION);
               error.addProperty("description", "unexpected type "
                     "'" + core::json::typeAsString((*it).getValue()) + "'"
                     " for preference '" + name + "' in layer '" + layerName() + "'");
               LOG_ERROR(error);
               return boost::none;
            }

            // Return the preference
            return (*it).getValue().getValue<T>();
         }
      }
      END_LOCK_MUTEX;

      return boost::none;
   };

   template <typename T> core::Error writePref(const std::string& name, T value)
   {
      // Ensure we have a cache to use as a baseline for writing
      core::Error error;
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         if (!cache_)
         {
            core::Error error(core::json::errc::ParamMissing, ERROR_LOCATION);
            error.addProperty("description", "cannot write property '" + name + "' in layer '" +
                  layerName() + "' before reading it");
            return error;
         }
         (*cache_)[name] = value;
      }
      END_LOCK_MUTEX;

      // WritePrefs does its own mutex locking
      error = writePrefs(*cache_);

      // Notify listeners that the pref has a new value
      onChanged(name);

      return error;
   }

   // Signal emitted when a pref changes
   RSTUDIO_BOOST_SIGNAL<void(const std::string&)> onChanged;

   core::json::Object allPrefs();
   boost::optional<core::json::Value> readValue(const std::string& name);
   core::Error clearValue(const std::string& name);
   core::Error validatePrefsFromSchema(const core::FilePath& schemaFile);

protected:
   // I/O methods
   core::Error loadPrefsFromFile(const core::FilePath& prefsFile, const core::FilePath& schemaFile);
   core::Error loadPrefsFromSchema(const core::FilePath& schemaFile);
   core::Error writePrefsToFile(const core::json::Object& prefs, const core::FilePath& prefsFile);

   // File registration for automatic update
   void monitorPrefsFile(const core::FilePath& prefsFile);
   virtual void onPrefsFileChanged();

   // The actual cache of preference values, and a mutex that guards access
   boost::shared_ptr<core::json::Object> cache_;
   boost::recursive_mutex mutex_;

   // The name of this pref layer
   std::string layerName_;

private:
   // File monitor event handlers
   boost::optional<core::system::file_monitor::Handle> handle_;
   void fileMonitorRegistered(core::system::file_monitor::Handle handle,
                              const tree<core::FileInfo>& files);
   void fileMonitorFilesChanged(
                   const std::vector<core::system::FileChangeEvent>& events);
   void fileMonitorTermination(const core::Error& error);

};

} // namespace prefs
} // namespace session
} // namespace rstudio

#endif

