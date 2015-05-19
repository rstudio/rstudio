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

#include <core/FilePath.hpp>

#include <core/r_util/RSessionContext.hpp>

namespace rstudio {
namespace session {
namespace projects {

class ProjectsSettings
{
public:
   explicit ProjectsSettings(const core::FilePath& userScratchPath)
      : settingsPath_(core::r_util::projectsSettingsPath(userScratchPath))
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

   std::string readSetting(const char * const settingName) const
   {
      return core::r_util::readProjectsSetting(settingsPath_, settingName);
   }

   void writeSetting(const char * const settingName, const std::string& value)
   {
      core::r_util::writeProjectsSetting(settingsPath_, settingName, value);
   }

private:
   core::FilePath settingsPath_;
};


} // namepspace projects
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_PROJECTS_SETTINGS_HPP

