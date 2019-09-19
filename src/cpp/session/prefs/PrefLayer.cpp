/*
 * PrefLayer.cpp
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

#include <session/prefs/PrefLayer.hpp>

#include <core/FileSerializer.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {
namespace {

bool prefsFileFilter(const core::FilePath& prefsFile, const core::FileInfo& fileInfo)
{
   return prefsFile.absolutePath() == fileInfo.absolutePath();
}

} // anonymous namespace


PrefLayer::PrefLayer(const std::string& layerName):
   layerName_(layerName)
{
}

PrefLayer::~PrefLayer()
{
   // End file monitoring if not already terminated
   if (handle_)
   {
      core::system::file_monitor::unregisterMonitor(*handle_);
      handle_ = boost::none;
   }
}

core::json::Object PrefLayer::allPrefs()
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      return *cache_;
   }
   END_LOCK_MUTEX;

   return json::Object();
}

Error PrefLayer::writePrefs(const core::json::Object &prefs)
{
   // Most preference layers aren't writable, so the default implementation throws an error.
   return systemError(boost::system::errc::function_not_supported, ERROR_LOCATION);
}

Error PrefLayer::loadPrefsFromFile(const core::FilePath &prefsFile)
{
   json::Value val;
   std::string contents;
   Error error = readStringFromFile(prefsFile, &contents);
   if (error)
   {
      // No prefs file; use an empty cache
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         cache_ = boost::make_shared<json::Object>();
      }
      END_LOCK_MUTEX

      if (!isNotFoundError(error))
      {
         // If we hit an unexpected error (e.g. permission denied), it's still not fatal (we can live
         // without a prefs file) but users might like to know.
         LOG_ERROR(error);
      }
      return Success();
   }

   error = json::parse(contents, ERROR_LOCATION, &val);
   if (error)
   {
      // Couldn't parse prefs JSON
      return error;
   }
   else if (val.type() == json::ObjectType)
   {
      // Successful parse of prefs object
      RECURSIVE_LOCK_MUTEX(mutex_)
      {
         cache_ = boost::make_shared<json::Object>(val.get_obj());
      }
      END_LOCK_MUTEX
   }
   else
   {
      // We parsed but got a non-object JSON value (this is exceedingly unlikely)
      return Error(rapidjson::kParseErrorValueInvalid, ERROR_LOCATION);
   }

   return Success();
}

Error PrefLayer::loadPrefsFromSchema(const core::FilePath &schemaFile)
{
   std::string contents;
   Error error = readStringFromFile(schemaFile, &contents);
   if (error)
      return error;

   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      cache_ = boost::make_shared<json::Object>();
      error = json::getSchemaDefaults(contents, cache_.get());
   }
   END_LOCK_MUTEX

   return error;
}

Error PrefLayer::validatePrefsFromSchema(const core::FilePath &schemaFile)
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      if (cache_ && cache_->type() == json::ObjectType)
      {
         std::string contents;
         Error error = readStringFromFile(schemaFile, &contents);
         if (error)
            return error;

         return json::validate(*cache_, contents, ERROR_LOCATION);
      }
      else
      {
         // We won't technically fail validation here, but we shouldn't try to validate before reading.
         LOG_WARNING_MESSAGE("Attempt to validate prefs before they were read.");
      }
   }
   END_LOCK_MUTEX

   return Success();
}

Error PrefLayer::writePrefsToFile(const core::json::Object& prefs, const core::FilePath& prefsFile)
{
   Error error;

   // If the preferences file doesn't exist, ensure its directory does.
   if (!prefsFile.exists())
   {
      error = prefsFile.parent().ensureDirectory();
      if (error)
         return error;
   }

   // Save the new preferences to file
   std::ostringstream oss;
   json::writeFormatted(prefs, oss);
   error = writeStringToFile(prefsFile, oss.str());

   return error;
}

boost::optional<core::json::Value> PrefLayer::readValue(const std::string& name)
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      const auto it = cache_->find(name);
      if (it == cache_->end())
      {
         // The value doesn't exist in this layer.
         return boost::none;
      }
      return (*it).value().clone();
   }
   END_LOCK_MUTEX

   return boost::none;
}

Error PrefLayer::clearValue(const std::string& name)
{
   RECURSIVE_LOCK_MUTEX(mutex_)
   {
      auto it = cache_->find(name);
      if (it == cache_->end())
      {
         // No value to clear!
         return Success();
      }

      cache_->erase(it);
      return writePrefs(*cache_);
   }
   END_LOCK_MUTEX

   return Success();
}

void PrefLayer::onPrefsFileChanged()
{
   // No-op for override
}

void PrefLayer::fileMonitorRegistered(core::system::file_monitor::Handle handle,
                                      const tree<core::FileInfo>& files)
{
   // Save handle to unregister later
   handle_ = handle;
}

void PrefLayer::fileMonitorFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   if (events.size() > 0)
   {
      // No need to introspect the event since we discard all events not associated with the
      // preference file.
      onPrefsFileChanged();
   }
}

void PrefLayer::fileMonitorTermination(const Error& error)
{
   if (error)
      LOG_ERROR(error);

   // Clear file monitoring handle
   handle_ = boost::none; 
}

void PrefLayer::monitorPrefsFile(const core::FilePath& prefsFile)
{
   // Sanity check: end any existing monitoring before initiating another
   if (handle_)
   {
      core::system::file_monitor::unregisterMonitor(*handle_);
      handle_ = boost::none;
   }

   // Kickoff file monitoring for the directory containing the file
   using boost::bind;
   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = bind(&PrefLayer::fileMonitorRegistered, this, _1, _2);
   cb.onRegistrationError = bind(&PrefLayer::fileMonitorTermination, this, _1);
   cb.onMonitoringError = bind(&PrefLayer::fileMonitorTermination, this, _1);
   cb.onFilesChanged = bind(&PrefLayer::fileMonitorFilesChanged, this, _1);
   cb.onUnregistered = bind(&PrefLayer::fileMonitorTermination, this, Success());
   core::system::file_monitor::registerMonitor(prefsFile.parent(), 
         false /* recursive */, 
         bind(prefsFileFilter, prefsFile, _1),
         cb);
}

std::string PrefLayer::layerName()
{
   return layerName_;
}

} // namespace prefs
} // namespace session
} // namespace rstudio

