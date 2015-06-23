/*
 * RVersionSettings.hpp
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

#ifndef SESSION_SESSION_RVERSION_SETTINGS_HPP
#define SESSION_SESSION_RVERSION_SETTINGS_HPP

// header-only file for access to r version settings from many contexts

#include <core/SharedSettings.hpp>

#define kRVersionSettings              "rversion-settings"
#define kDefaultRVersion               "defaultRVersion"
#define kDefaultRVersionHome           "defaultRVersionHome"
#define kRestoreProjectRVersion        "restoreProjectRVersion"

namespace rstudio {
namespace session {

class RVersionSettings : public core::SharedSettings
{
public:
   explicit RVersionSettings(const core::FilePath& userScratchPath)
      : core::SharedSettings(rVersionSettingsPath(userScratchPath))
   {
   }

   std::string defaultRVersion()
   {
      return readSetting(kDefaultRVersion);
   }

   std::string defaultRVersionHome()
   {
      return readSetting(kDefaultRVersionHome);
   }

   void setDefaultRVersion(const std::string& version,
                           const std::string& versionHome)
   {
      writeSetting(kDefaultRVersion, version);
      writeSetting(kDefaultRVersionHome, versionHome);
   }

   bool restoreProjectRVersion()
   {
      return readSetting(kRestoreProjectRVersion) != "0";
   }

   void setRestoreProjectRVersion(bool restoreProjectRVersion)
   {
      writeSetting(kRestoreProjectRVersion, restoreProjectRVersion ? "1" : "0");
   }

private:

   static core::FilePath rVersionSettingsPath(
                     const core::FilePath& userScratchPath)
   {
      using namespace rstudio::core;
      FilePath settingsPath = userScratchPath.complete(kRVersionSettings);
      Error error = settingsPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);

      return settingsPath;
   }
};


} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_RVERSION_SETTINGS_HPP

