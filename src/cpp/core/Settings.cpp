/*
 * Settings.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/Settings.hpp>

#include <boost/lexical_cast.hpp>

#include <core/Log.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

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
   settingsFile_ = filePath ;
   settingsMap_.clear() ;
   Error error = core::readStringMapFromFile(settingsFile_, &settingsMap_) ;
   if (error)
   {
      // we don't consider file-not-found and error because it is a 
      // common initialization case
      if (error.code() != boost::system::errc::no_such_file_or_directory)
      {
         error.addProperty("settings-file", settingsFile_);
         return error ;
      }
   }
   
   return Success() ;
}

void Settings::set(const std::string& name, const std::string& value)
{
   if (value != settingsMap_[name])
   {
      settingsMap_[name] = value ;
      isDirty_ = true;
      
      if (!updatePending_)
         writeSettings() ;
   }
}
   
void Settings::set(const std::string& name, int value)
{
   set(name, boost::lexical_cast<std::string>(value));
}

void Settings::set(const std::string& name, bool value)
{
   set(name, boost::lexical_cast<std::string>(value));
}
   
bool Settings::contains(const std::string& name) const
{
   return settingsMap_.find(name) != settingsMap_.end();
}
   
std::string Settings::get(const std::string& name, 
                          const std::string& defaultValue) const
{
   std::map<std::string,std::string>::const_iterator pos = 
                                                   settingsMap_.find(name) ;
   if (pos != settingsMap_.end())
      return (*pos).second ;
   else
      return defaultValue ;
}
   
int Settings::getInt(const std::string& name, int defaultValue) const
{
   std::string value = get(name) ;
   if (value.empty())
       return defaultValue ;
   else
       return boost::lexical_cast<int>(value);
}

int Settings::getBool(const std::string& name, bool defaultValue) const
{
   std::string value = get(name) ;
   if (value.empty())
      return defaultValue ;
   else
      return boost::lexical_cast<bool>(value);
}   
   
void Settings::beginUpdate()
{
   updatePending_ = true ;
}

void Settings::endUpdate()
{
   updatePending_ = false ;
   if (isDirty_)
      writeSettings();
}

void Settings::writeSettings() 
{
   isDirty_ = false;
   Error error = core::writeStringMapToFile(settingsFile_, settingsMap_) ; 
   if (error)
     LOG_ERROR(error);
}


}


