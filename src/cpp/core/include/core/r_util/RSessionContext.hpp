/*
 * RSessionContext.hpp
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

#ifndef CORE_R_UTIL_R_SESSION_CONTEXT_HPP
#define CORE_R_UTIL_R_SESSION_CONTEXT_HPP

#include <string>

#include <core/FilePath.hpp>

#include <core/r_util/RVersionInfo.hpp>

#define kUserSettings                  "monitored/user-settings/user-settings"
#define kAlwaysRestoreLastProject      "restoreLastProject"

#define kProjectsSettings              "projects_settings"
#define kNextSessionProject            "next-session-project"
#define kNextSessionProjectNone        "none"
#define kLastProjectPath               "last-project-path"

#define kRStudioInitialWorkingDir      "RS_INITIAL_WD"
#define kRStudioInitialEnvironment     "RS_INITIAL_ENV"
#define kRStudioInitialProject         "RS_INITIAL_PROJECT"

namespace rstudio {
namespace core {
namespace r_util {

enum SessionType
{
   SessionTypeDesktop,
   SessionTypeServer
};

struct UserDirectories
{
   std::string homePath;
   std::string scratchPath;
};

UserDirectories userDirectories(SessionType sessionType,
                                const std::string& homePath = std::string());


FilePath projectsSettingsPath(const FilePath& userScratchPath);

std::string readProjectsSetting(const FilePath& settingsPath,
                                const char * const settingName);

void writeProjectsSetting(const FilePath& settingsPath,
                          const char * const settingName,
                          const std::string& value);

FilePath nextSessionProject(SessionType sessionType,
                            const std::string& homePath = std::string());

RVersionInfo nextSessionRVersion(SessionType sessionType,
                                 const std::string& homePath = std::string());

} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_SESSION_CONTEXT_HPP

