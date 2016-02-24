/*
 * SharedSettings.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/Error.hpp>
#include <core/FilePath.hpp>
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

   std::string readSetting(const char * const settingName) const
   {
      using namespace rstudio::core;
      FilePath readPath = settingsPath_.complete(settingName);
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

   void writeSetting(const char * const settingName,
                     const std::string& value)
   {
      using namespace rstudio::core;
      FilePath writePath = settingsPath_.complete(settingName);
      Error error = core::writeStringToFile(writePath, value);
      if (error)
         LOG_ERROR(error);
   }

private:
   core::FilePath settingsPath_;
};


} // namespace core
} // namespace rstudio

#endif // CORE_SHARED_SETTINGS_HPP

