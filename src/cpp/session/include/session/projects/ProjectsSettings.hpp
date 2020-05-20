/*
 * ProjectsSettings.hpp
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

#ifndef SESSION_SESSION_PROJECTS_SETTINGS_HPP
#define SESSION_SESSION_PROJECTS_SETTINGS_HPP

// header-only file for access to projects settings from many contexts
// (main session thread, background threads, other processes, etc.)

#include <core/SharedSettings.hpp>

#include <core/r_util/RSessionContext.hpp>

#define kProjectsSettings              "projects_settings"
#define kNextSessionProject            "next-session-project"
#define kSwitchToProject               "switch-to-project"
#define kLastProjectPath               "last-project-path"
#define kAlwaysRestoreLastProject      "restoreLastProject"

namespace rstudio {
namespace session {
namespace projects {

class ProjectsSettings : public core::SharedSettings
{
public:
   explicit ProjectsSettings(const core::FilePath& userScratchPath)
      : core::SharedSettings(projectsSettingsPath(userScratchPath))
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
      if (!lastProjectPath.isEmpty())
         writeSetting(kLastProjectPath, lastProjectPath.getAbsolutePath());
      else
         writeSetting(kLastProjectPath, "");
   }

private:

   static core::FilePath projectsSettingsPath(
                     const core::FilePath& userScratchPath)
   {
      using namespace rstudio::core;
      FilePath settingsPath = userScratchPath.completePath(kProjectsSettings);
      Error error = settingsPath.ensureDirectory();
      if (error)
         LOG_ERROR(error);

      return settingsPath;
   }
};


} // namepspace projects
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_PROJECTS_SETTINGS_HPP

