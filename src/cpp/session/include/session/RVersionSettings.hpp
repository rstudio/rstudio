/*
 * RVersionSettings.hpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <session/SessionScopes.hpp>

#define kRVersionSettings              "rversion-settings"
#define kDefaultRVersion               "defaultRVersion"
#define kDefaultRVersionHome           "defaultRVersionHome"
#define kDefaultRVersionLabel          "defaultRVersionLabel"
#define kRestoreProjectRVersion        "restoreProjectRVersion"
#define kRVersionSuffix                "-RVersion"
#define kRVersionHomeSuffix            "-RVersionHome"
#define kRVersionLabelSuffix           "-RVersionLabel"
#define kRVersionProjectFile           "RVersion"

namespace rstudio {
namespace session {

class RVersionSettings : public core::SharedSettings
{
public:
   explicit RVersionSettings(const core::FilePath& userScratchPath,
                             const core::FilePath& sharedStoragePath)
      : core::SharedSettings(rVersionSettingsPath(userScratchPath)),
        userScratchPath_(userScratchPath),
        sharedStoragePath_(sharedStoragePath)
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

   std::string defaultRVersionLabel()
   {
      return readSetting(kDefaultRVersionLabel);
   }

   void setDefaultRVersion(const std::string& version,
                           const std::string& versionHome,
                           const std::string& versionLabel)
   {
      writeSetting(kDefaultRVersion, version);
      writeSetting(kDefaultRVersionHome, versionHome);
      writeSetting(kDefaultRVersionLabel, versionLabel);
   }

   bool restoreProjectRVersion()
   {
      return readSetting(kRestoreProjectRVersion) != "0";
   }

   void setRestoreProjectRVersion(bool restoreProjectRVersion)
   {
      writeSetting(kRestoreProjectRVersion, restoreProjectRVersion ? "1" : "0");
   }

   void setProjectLastRVersion(const std::string& projectDir,
                               const core::FilePath& sharedProjectScratchPath,
                               const std::string& version,
                               const std::string& versionHome,
                               const std::string& versionLabel)
   {
      // get a project id
      core::r_util::FilePathToProjectId filePathToProjectId =
                           session::filePathToProjectId(userScratchPath_,
                                                        sharedStoragePath_);
      core::r_util::ProjectId projectId = filePathToProjectId(projectDir);

      // save the version
      writeProjectSetting(projectId.asString(), kRVersionSuffix, version);
      writeProjectSetting(projectId.asString(), kRVersionHomeSuffix, versionHome);
      writeProjectSetting(projectId.asString(), kRVersionLabelSuffix, versionLabel);

      // save R version in the project itself; used as a hint on preferred R version
      // when opening a project for the first time on a different machine/container
      if (!sharedProjectScratchPath.empty())
      {
         writeSettingToPath(sharedProjectScratchPath, kRVersionProjectFile, version);
      }
   }

   void readProjectLastRVersion(const std::string& projectDir,
                                const core::FilePath& sharedProjectScratchPath,
                                std::string* pVersion,
                                std::string* pVersionHome,
                                std::string* pVersionLabel)
   {
      // get a project id
      core::r_util::FilePathToProjectId filePathToProjectId =
                           session::filePathToProjectId(userScratchPath_,
                                                        sharedStoragePath_);
      core::r_util::ProjectId projectId = filePathToProjectId(projectDir);

      *pVersion = readProjectSetting(projectId.asString(),
                                     kRVersionSuffix);
      *pVersionHome = readProjectSetting(projectId.asString(),
                                         kRVersionHomeSuffix);
      *pVersionLabel = readProjectSetting(projectId.asString(),
                                          kRVersionLabelSuffix);

      // if no local setting for R version, check for hint in .Rproj.user
      if ((pVersion->empty() || pVersionHome->empty()) && !sharedProjectScratchPath.empty())
      {
         *pVersion = readSettingFromPath(sharedProjectScratchPath, kRVersionProjectFile);
         pVersionHome->clear();
      }
   }

private:
   void writeProjectSetting(const std::string& projectId,
                            const char* name,
                            const std::string& value)
   {
      writeSetting((projectId + name).c_str(), value);
   }

   std::string readProjectSetting(const std::string& projectId,
                                  const char* name)

   {
      return readSetting((projectId + name).c_str());
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

   core::FilePath userScratchPath_;
   core::FilePath sharedStoragePath_;
};


} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_RVERSION_SETTINGS_HPP
