/*
 * RSessionContext.cpp
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

#include <core/r_util/RSessionContext.hpp>

#include <iostream>

#include <boost/algorithm/string/trim.hpp>

#include <core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/FileSerializer.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <core/r_util/RProjectFile.hpp>

namespace rstudio {
namespace core {
namespace r_util {

namespace {


} // anonymous namespace


UserDirectories userDirectories(SessionType sessionType,
                                const std::string& homePath)
{
   UserDirectories dirs;
   if (!homePath.empty())
      dirs.homePath = homePath;
   else
      dirs.homePath = core::system::userHomePath("R_USER|HOME").absolutePath();

   // compute user scratch path
   std::string scratchPathName;
   if (sessionType == SessionTypeDesktop)
      scratchPathName = "RStudio-Desktop";
   else
      scratchPathName = "RStudio";

   dirs.scratchPath = core::system::userSettingsPath(
                                          FilePath(dirs.homePath),
                                          scratchPathName).absolutePath();

   return dirs;
}

FilePath projectsSettingsPath(const FilePath& userScratchPath)
{
   FilePath settingsPath = userScratchPath.complete(kProjectsSettings);
   Error error = settingsPath.ensureDirectory();
   if (error)
      LOG_ERROR(error);

   return settingsPath;
}

std::string readProjectsSetting(const FilePath& settingsPath,
                                const char * const settingName)
{
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

void writeProjectsSetting(const FilePath& settingsPath,
                          const char * const settingName,
                          const std::string& value)
{
   FilePath writePath = settingsPath.complete(settingName);
   Error error = core::writeStringToFile(writePath, value);
   if (error)
      LOG_ERROR(error);
}

// Determine the project for the next RStudio session. This is
// used principally to inspect .Rproj and packrat configuration files
// to determine whether we need to use an alternate version of R. Note that
// the logic here must be synchronized with the logic in the startup method
// of session::projects
FilePath nextSessionProject(SessionType sessionType,
                            const std::string& homePath)
{
   // get the user scratch path and projects settings path
   UserDirectories dirs = userDirectories(sessionType, homePath);
   FilePath userScratchPath(dirs.scratchPath);
   FilePath projectsSettings = r_util::projectsSettingsPath(userScratchPath);

   // read the startup oriented project options
   std::string nextSessionProject = readProjectsSetting(projectsSettings,
                                                        kNextSessionProject);
   std::string lastProjectPath = readProjectsSetting(projectsSettings,
                                                     kLastProjectPath);

   // read environment variables derived from startup file associations
   std::string initialProjPath = core::system::getenv(kRStudioInitialProject);
   std::string initialWDPath = core::system::getenv(kRStudioInitialWorkingDir);

   // read the always restore last project user setting
   bool alwaysRestoreLastProject = false;
   core::Settings uSettings;
   FilePath userSettingsPath = userScratchPath.childPath(kUserSettings);
   Error error = uSettings.initialize(userSettingsPath);
   if (error)
      LOG_ERROR(error);
   else
      alwaysRestoreLastProject = uSettings.getBool(kAlwaysRestoreLastProject,
                                                   true);

   // apply logic required to determine the next project (if any)

   if (!nextSessionProject.empty())
   {
      if (nextSessionProject == kNextSessionProjectNone)
         return FilePath();
      else
         return FilePath::resolveAliasedPath(nextSessionProject,
                                             FilePath(dirs.homePath));
   }
   else if (!initialProjPath.empty())
   {
      return FilePath(initialProjPath);
   }
   else if (!initialWDPath.empty())
      // this is here just to prevent the next case
      // (b/c we are opening based on a file association)
   {
      return FilePath();
   }
   else if (alwaysRestoreLastProject && !lastProjectPath.empty())
   {
      return FilePath(lastProjectPath);
   }
   else
   {
      return FilePath();
   }
}

RVersionInfo nextSessionRVersion(SessionType sessionType,
                                 const std::string& homePath)
{
   // first determine the project path -- if there is none then we return
   // an empty RVersionInfo (which will result in just using the default)
   FilePath nextProject = nextSessionProject(sessionType, homePath);
   if (nextProject.empty())
      return RVersionInfo();

   // read the project file
   std::string errMsg;
   r_util::RProjectConfig projectConfig;
   Error error = r_util::readProjectFile(nextProject, &projectConfig, &errMsg);
   if (error)
   {
      error.addProperty("message", errMsg);
      LOG_ERROR(error);
      return RVersionInfo();
   }

   // TODO: look for packrat version as well

   // return the version
   return projectConfig.rVersion;
}

} // namespace r_util
} // namespace core 
} // namespace rstudio



