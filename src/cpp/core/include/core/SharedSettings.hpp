/*
 * SharedSettings.hpp
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

#ifndef CORE_SHARED_SETTINGS_HPP
#define CORE_SHARED_SETTINGS_HPP

#include <string>

#include <boost/algorithm/string/trim.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

namespace rstudio {
namespace core {

class SharedSettings
{
public:
   explicit SharedSettings(const core::FilePath& settingsPath)
      : settingsPath_(settingsPath)
   {
      Error error = settingsPath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }

   // read setting from a folder
   static std::string readSettingFromPath(const core::FilePath& settingsPath, const std::string& settingName)
   {
      using namespace rstudio::core;
      FilePath readPath = settingsPath.completePath(settingName);
      if (readPath.exists())
      {
         std::string value;
         Error error = core::readStringFromFile(readPath, &value);
         if (error)
         {
            LOG_ERROR(error);
            return std::string();
         }
         boost::algorithm::trim(value);
         return value;
      }
      else
      {
         return std::string();
      }
   }

   // write setting to a folder
   static void writeSettingToPath(const core::FilePath& settingsPath,
                                  const std::string& settingName,
                                  const std::string& value)
   {
      using namespace rstudio::core;
      FilePath writePath = settingsPath.completePath(settingName);
      Error error = core::writeStringToFile(writePath, value);
      if (error)
         LOG_ERROR(error);
   }

   std::string readSetting(const char * const settingName) const
   {
      return readSettingFromPath(settingsPath_, settingName);
   }

   void writeSetting(const char * const settingName,
                     const std::string& value)
   {
      writeSettingToPath(settingsPath_, settingName, value);
   }

private:
   core::FilePath settingsPath_;
};


} // namespace core
} // namespace rstudio

#endif // CORE_SHARED_SETTINGS_HPP

