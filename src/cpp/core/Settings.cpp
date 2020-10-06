/*
 * Settings.cpp
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

#include <core/Settings.hpp>

#include <core/Log.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>

namespace rstudio {
namespace core {

Settings::Settings()
   : updatePending_(false),
     isDirty_(false)
{
}

Settings::~Settings()
{
}

Error Settings::initialize(const FilePath& filePath) 
{
   settingsFile_ = filePath;
   settingsMap_.clear();
   Error error = core::readStringMapFromFile(settingsFile_, &settingsMap_);
   if (error)
   {
      // we don't consider file-not-found and error because it is a 
      // common initialization case
      if (error != systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()))
      {
         error.addProperty("settings-file", settingsFile_);
         return error;
      }
   }
   
   return Success();
}

void Settings::set(const std::string& name, const std::string& value)
{
   if (value != settingsMap_[name])
   {
      settingsMap_[name] = value;
      isDirty_ = true;
      
      if (!updatePending_)
         writeSettings();
   }
}
   
void Settings::set(const std::string& name, int value)
{
   set(name, safe_convert::numberToString(value));
}

void Settings::set(const std::string& name, double value)
{
   set(name, safe_convert::numberToString(value));
}


void Settings::set(const std::string& name, bool value)
{
   set(name, safe_convert::numberToString(value));
}
   
bool Settings::contains(const std::string& name) const
{
   return settingsMap_.find(name) != settingsMap_.end();
}
   
std::string Settings::get(const std::string& name, 
                          const std::string& defaultValue) const
{
   std::map<std::string,std::string>::const_iterator pos = 
                                                   settingsMap_.find(name);
   if (pos != settingsMap_.end())
      return (*pos).second;
   else
      return defaultValue;
}
   
int Settings::getInt(const std::string& name, int defaultValue) const
{
   std::string value = get(name);
   if (value.empty())
       return defaultValue;
   else
       return safe_convert::stringTo<int>(value, defaultValue);
}

double Settings::getDouble(const std::string& name, double defaultValue) const
{
   std::string value = get(name);
   if (value.empty())
       return defaultValue;
   else
      return safe_convert::stringTo<double>(value, defaultValue);
}

bool Settings::getBool(const std::string& name, bool defaultValue) const
{
   std::string value = get(name);
   if (value.empty())
      return defaultValue;
   else
      return safe_convert::stringTo<bool>(value, defaultValue);
}   
   
void Settings::forEach(const boost::function<void(const std::string&,
                                                  const std::string&)>& func)
                                                                         const
{
   for (std::map<std::string,std::string>::const_iterator
         it = settingsMap_.begin(); it != settingsMap_.end(); ++it)
   {
      func(it->first, it->second);
   }
}

void Settings::beginUpdate()
{
   updatePending_ = true;
}

void Settings::endUpdate()
{
   updatePending_ = false;
   if (isDirty_)
      writeSettings();
}

void Settings::writeSettings() 
{
   isDirty_ = false;
   Error error = core::writeStringMapToFile(settingsFile_, settingsMap_);
   if (error)
     LOG_ERROR(error);
}


}
}


