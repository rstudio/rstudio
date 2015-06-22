/*
 * ProjectsSettings.hpp
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

#ifndef SESSION_SESSION_PROJECTS_SETTINGS_HPP
#define SESSION_SESSION_PROJECTS_SETTINGS_HPP

// header-only file for access to projects settings from many contexts
// (main session thread, background threads, other processes, etc.)

#include <string>

#include <boost/algorithm/string/trim.hpp>

#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

#include <core/r_util/RSessionContext.hpp>

#define kProjectsSettings              "projects_settings"
#define kNextSessionProject            "next-session-project"
#define kSwitchToProject               "switch-to-project"
#define kLastProjectPath               "last-project-path"
#define kAlwaysRestoreLastProject      "restoreLastProject"
#define kDefaultRVersion               "defaultRVersion"
#define kDefaultRVersionHome           "defaultRVersionHome"
#define kRestoreProjectRVersion        "restoreProjectRVersion"

namespace rstudio {
namespace session {
namespace projects {

class ProjectsSettings
{
public:
   explicit ProjectsSettings(const core::FilePath& userScratchPath)
      : settingsPath_(projectsSettingsPath(userScratchPath))
   {
   }

   std::string nextSessionProject() const
   {
      return readSetting(kNextSessionProject);
   }

   void setNextSessionProject(const std::string& nextSessionProject)
   {
      writeSetting(kNextSessionProject, nextSessionProject);
   }

   std::string switchToProjectPath() const
   {
      return readSetting(kSwitchToProject);
   }

   void setSwitchToProjectPath(const std::string& switchToProjectPath)
   {
      writeSetting(kSwitchToProject, switchToProjectPath);
   }

   core::FilePath lastProjectPath() const
   {
      std::string path = readSetting(kLastProjectPath);
      if (!path.empty())
         return core::FilePath(path);
      else
         return core::FilePath();
   }

   void setLastProjectPath(const core::FilePath& lastProjectPath)
   {
      if (!lastProjectPath.empty())
         writeSetting(kLastProjectPath, lastProjectPath.absolutePath());
      else
         writeSetting(kLastProjectPath, "");
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

   std::string readSetting(const char * const settingName) const
   {
      return readProjectsSetting(settingsPath_, settingName);
   }

   void writeSetting(const char * const settingName, const std::string& value)
   {
      writeProjectsSetting(settingsPath_, settingName, value);
   }

private:

   static core::FilePath projectsSettingsPath(
                     const core::FilePath& userScratchPath)
   {
      using namespace rstudio::core;
      FilePath settingsPath = userScratchPath.complete(kProjectsSettings);
      Error error = settingsPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);

      return settingsPath;
   }

   static std::string readProjectsSetting(const core::FilePath& settingsPath,
                                          const char * const settingName)
   {
      using namespace rstudio::core;
      FilePath readPath = settingsPath.complete(settingName);
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

   static void writeProjectsSetting(const core::FilePath& settingsPath,
                                    const char * const settingName,
                                    const std::string& value)
   {
      using namespace rstudio::core;
      FilePath writePath = settingsPath.complete(settingName);
      Error error = core::writeStringToFile(writePath, value);
      if (error)
         LOG_ERROR(error);
   }

private:
   core::FilePath settingsPath_;
};


} // namepspace projects
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_PROJECTS_SETTINGS_HPP

