/*
 * SessionOptions.hpp
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

#ifndef SESSION_SESSION_OPTIONS_HPP
#define SESSION_SESSION_OPTIONS_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/SafeConvert.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/StringUtils.hpp>
#include <core/ProgramOptions.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <R_ext/RStartup.h>

#include <session/SessionConstants.hpp>

namespace rstudio {
namespace core {
   class ProgramStatus;
}
}

namespace rstudio {
namespace session {
 

// singleton
class Options;
Options& options();
   
class Options : boost::noncopyable
{
private:
   Options()
   {
   }
   friend Options& options() ;
   
   // COPYING: boost::noncopyable

public:
   // read options  
   core::ProgramStatus read(int argc, char * const argv[]);   
   virtual ~Options() {}
   
   bool runTests() const
   {
      return runTests_;
   }
   
   bool verifyInstallation() const
   {
      return verifyInstallation_;
   }

   core::FilePath verifyInstallationHomeDir() const
   {
      if (!verifyInstallationHomeDir_.empty())
         return core::FilePath(verifyInstallationHomeDir_.c_str());
      else
         return core::FilePath();
   }

   std::string programIdentity() const 
   { 
      return std::string(programIdentity_.c_str()); 
   }
   
   std::string programMode() const 
   { 
      return std::string(programMode_.c_str()); 
   }

   bool logStderr() const
   {
      return logStderr_;
   }
   
   // agreement
   core::FilePath agreementFilePath() const
   { 
      if (!agreementFilePath_.empty())
         return core::FilePath(agreementFilePath_.c_str());
      else
         return core::FilePath();
   }

   // docs
   std::string docsURL() const
   {
      return std::string(docsURL_.c_str());
   }
   
   // www
   std::string wwwLocalPath() const
   {
      return std::string(wwwLocalPath_.c_str());
   }

   core::FilePath wwwSymbolMapsPath() const
   {
      return core::FilePath(wwwSymbolMapsPath_.c_str());
   }

   std::string wwwPort() const
   {
      return std::string(wwwPort_.c_str());
   }

   std::string wwwAddress() const
   {
      return std::string(wwwAddress_.c_str());
   }

   std::string sharedSecret() const
   {
      return std::string(secret_.c_str());
   }

   core::FilePath preflightScriptPath() const
   {
      return core::FilePath(preflightScript_.c_str());
   }

   int timeoutMinutes() const { return timeoutMinutes_; }

   int disconnectedTimeoutMinutes() { return disconnectedTimeoutMinutes_; }

   bool createProfile() const { return createProfile_; }

   bool createPublicFolder() const { return createPublicFolder_; }

   bool rProfileOnResumeDefault() const { return rProfileOnResumeDefault_; }

   int saveActionDefault() const { return saveActionDefault_; }

   unsigned int authMinimumUserId() const { return authMinimumUserId_; }

   std::string authRequiredUserGroup() const { return authRequiredUserGroup_; }

   std::string defaultWorkingDir() const { return defaultWorkingDir_.c_str(); }

   std::string defaultProjectDir() const { return defaultProjectDir_.c_str(); }

   bool showHelpHome() const { return showHelpHome_; }

   bool showUserHomePage() const { return showUserHomePage_; }
   
   std::string defaultConsoleTerm() const { return defaultConsoleTerm_; }
   bool defaultCliColorForce() const { return defaultCliColorForce_; }

   core::FilePath coreRSourcePath() const 
   { 
      return core::FilePath(coreRSourcePath_.c_str());
   }
   
   core::FilePath modulesRSourcePath() const 
   { 
      return core::FilePath(modulesRSourcePath_.c_str()); 
   }

   core::FilePath sessionLibraryPath() const
   {
      return core::FilePath(sessionLibraryPath_.c_str());
   }
   
   core::FilePath sessionPackageArchivesPath() const
   {
      return core::FilePath(sessionPackageArchivesPath_.c_str());
   }

   
   std::string rLibsUser() const
   {
      return std::string(rLibsUser_.c_str());
   }

   std::string rCRANRepos() const
   {
      return std::string(rCRANRepos_.c_str());
   }

   int rCompatibleGraphicsEngineVersion() const
   {
      return rCompatibleGraphicsEngineVersion_;
   }

   core::FilePath rResourcesPath() const
   {
      return core::FilePath(rResourcesPath_.c_str());
   }

   std::string rHomeDirOverride()
   {
      return std::string(rHomeDirOverride_.c_str());
   }

   std::string rDocDirOverride()
   {
      return std::string(rDocDirOverride_.c_str());
   }
   
   std::string defaultRVersion()
   {
      return std::string(defaultRVersion_.c_str());
   }
   
   std::string defaultRVersionHome()
   {
      return std::string(defaultRVersionHome_.c_str());
   }
   
   bool autoReloadSource() const { return autoReloadSource_; }

   // limits
   int limitFileUploadSizeMb() const { return limitFileUploadSizeMb_; }
   int limitCpuTimeMinutes() const { return limitCpuTimeMinutes_; }

   int limitRpcClientUid() const { return limitRpcClientUid_; }

   bool limitXfsDiskQuota() const { return limitXfsDiskQuota_; }
   
   // external
   core::FilePath rpostbackPath() const
   {
      return core::FilePath(rpostbackPath_.c_str());
   }

   core::FilePath consoleIoPath() const
   {
      return core::FilePath(consoleIoPath_.c_str());
   }

   core::FilePath gnudiffPath() const
   {
      return core::FilePath(gnudiffPath_.c_str());
   }

   core::FilePath gnugrepPath() const
   {
      return core::FilePath(gnugrepPath_.c_str());
   }

   core::FilePath msysSshPath() const
   {
      return core::FilePath(msysSshPath_.c_str());
   }

   core::FilePath sumatraPath() const
   {
      return core::FilePath(sumatraPath_.c_str());
   }

   core::FilePath winutilsPath() const
   {
      return core::FilePath(winutilsPath_.c_str());
   }
   
   core::FilePath winptyPath() const
   {
      return core::FilePath(winptyPath_.c_str());
   }

   core::FilePath hunspellDictionariesPath() const
   {
      return core::FilePath(hunspellDictionariesPath_.c_str());
   }

   core::FilePath mathjaxPath() const
   {
      return core::FilePath(mathjaxPath_.c_str());
   }

   core::FilePath pandocPath() const
   {
      return core::FilePath(pandocPath_.c_str());
   }

   core::FilePath libclangPath() const
   {
      return core::FilePath(libclangPath_.c_str());
   }

   core::FilePath libclangHeadersPath() const
   {
      return core::FilePath(libclangHeadersPath_.c_str());
   }

   bool allowFileDownloads() const
   {
      return allowOverlay() || allowFileDownloads_;
   }

   bool allowFileUploads() const
   {
      return allowOverlay() || allowFileUploads_;
   }

   bool allowShell() const
   {
      return allowOverlay() || allowShell_;
   }

   bool allowTerminalWebsockets() const
   {
      return allowOverlay() || allowTerminalWebsockets_;
   }

   bool allowPackageInstallation() const
   {
      return allowOverlay() || allowPackageInstallation_;
   }

   bool allowVcs() const
   {
      return allowOverlay() || allowVcs_;
   }

   bool allowCRANReposEdit() const
   {
      return allowOverlay() || allowCRANReposEdit_;
   }

   bool allowVcsExecutableEdit() const
   {
      return allowOverlay() || allowVcsExecutableEdit_;
   }

   bool allowRemovePublicFolder() const
   {
      return allowOverlay() || allowRemovePublicFolder_;
   }

   bool allowRpubsPublish() const
   {
      return allowOverlay() || allowRpubsPublish_;
   }

   bool allowExternalPublish() const
   {
      return allowOverlay() || allowExternalPublish_;
   }

   bool allowPublish() const
   {
      return allowOverlay() || allowPublish_;
   }

   bool supportsDriversLicensing() const
   {
      return !allowOverlay();
   }

   bool allowPresentationCommands() const
   {
      return allowPresentationCommands_;
   }

   // user info
   std::string userIdentity() const 
   { 
      return std::string(userIdentity_.c_str()); 
   }
   
   bool showUserIdentity() const
   {
      return showUserIdentity_;
   }

   core::r_util::SessionScope sessionScope() const
   {
      return scope_;
   }

   core::r_util::SessionScopeState scopeState() const
   {
      return scopeState_;
   }

   core::r_util::SessionContext sessionContext() const
   {
      return core::r_util::SessionContext(userIdentity(), sessionScope());
   }

   bool multiSession() const
   {
      return multiSession_;
   }

   bool projectSharingEnabled() const
   {
      return projectSharingEnabled_;
   }

   bool switchProjectsWithUrl() const
   {
      return programMode() == kSessionProgramModeServer &&
             options().multiSession() == true;
   }

   core::FilePath userHomePath() const 
   { 
      return core::FilePath(userHomePath_.c_str());
   }
   
   core::FilePath userScratchPath() const 
   { 
      return core::FilePath(userScratchPath_.c_str()); 
   }

   core::FilePath userLogPath() const
   {
      return userScratchPath().childPath("log");
   }

   core::FilePath initialWorkingDirOverride()
   {
      if (!initialWorkingDirOverride_.empty())
         return core::FilePath(initialWorkingDirOverride_.c_str());
      else
         return core::FilePath();
   }

   core::FilePath initialEnvironmentFileOverride()
   {
      if (!initialEnvironmentFileOverride_.empty())
         return core::FilePath(initialEnvironmentFileOverride_.c_str());
      else
         return core::FilePath();
   }

   core::FilePath initialProjectPath()
   {
      if (!initialProjectPath_.empty())
         return core::FilePath(initialProjectPath_.c_str());
      else
         return core::FilePath();
   }

   core::FilePath rVersionsPath()
   {
      if (!rVersionsPath_.empty())
         return core::FilePath(rVersionsPath_.c_str());
      else
         return core::FilePath();
   }

   void clearInitialContextSettings()
   {
      initialWorkingDirOverride_.clear();
      initialEnvironmentFileOverride_.clear();
      initialProjectPath_.clear();
   }

   // The line ending we use when working with source documents
   // in memory. This doesn't really make sense for the user to
   // change.
   core::string_utils::LineEnding sourceLineEnding() const
   {
      return core::string_utils::LineEndingPosix;
   }

   std::string monitorSharedSecret() const
   {
      return monitorSharedSecret_.c_str();
   }

   bool standalone() const
   {
      return standalone_;
   }

   std::string launcherToken() const
   {
      return launcherToken_;
   }

   std::string getOverlayOption(const std::string& name)
   {
      return overlayOptions_[name];
   }

   bool getBoolOverlayOption(const std::string& name);

private:
   void resolvePath(const core::FilePath& resourcePath,
                    std::string* pPath);
   void resolvePostbackPath(const core::FilePath& resourcePath,
                            std::string* pPath);
   void resolvePandocPath(const core::FilePath& resourcePath, std::string* pPath);

   void resolveRsclangPath(const core::FilePath& resourcePath, std::string* pPath);

   void addOverlayOptions(boost::program_options::options_description* pOpt);
   bool validateOverlayOptions(std::string* pErrMsg);
   void resolveOverlayOptions();
   bool allowOverlay() const;

private:
   // tests
   bool runTests_;
   
   // verify
   bool verifyInstallation_;
   std::string verifyInstallationHomeDir_;

   // program
   std::string programIdentity_;
   std::string programMode_;

   // log
   bool logStderr_;

   // agreement
   std::string agreementFilePath_;

   // docs
   std::string docsURL_;
   
   // www
   std::string wwwLocalPath_;
   std::string wwwSymbolMapsPath_;
   std::string wwwPort_;
   std::string wwwAddress_;

   // session
   std::string secret_;
   std::string preflightScript_;
   int timeoutMinutes_;
   int disconnectedTimeoutMinutes_;
   bool createProfile_;
   bool createPublicFolder_;
   bool rProfileOnResumeDefault_;
   int saveActionDefault_;
   bool standalone_;
   std::string authRequiredUserGroup_;
   unsigned int authMinimumUserId_;
   std::string defaultWorkingDir_;
   std::string defaultProjectDir_;
   bool showHelpHome_;
   bool showUserHomePage_;
   std::string defaultConsoleTerm_;
   bool defaultCliColorForce_;

   // r
   std::string coreRSourcePath_;
   std::string modulesRSourcePath_;
   std::string sessionLibraryPath_;
   std::string sessionPackageArchivesPath_;
   std::string rLibsUser_;
   std::string rCRANRepos_;
   bool autoReloadSource_ ;
   int rCompatibleGraphicsEngineVersion_;
   std::string rResourcesPath_;
   std::string rHomeDirOverride_;
   std::string rDocDirOverride_;
   std::string defaultRVersion_;
   std::string defaultRVersionHome_;
   
   // limits
   int limitFileUploadSizeMb_;
   int limitCpuTimeMinutes_;
   int limitRpcClientUid_;
   bool limitXfsDiskQuota_;
   
   // external
   std::string rpostbackPath_;
   std::string consoleIoPath_;
   std::string gnudiffPath_;
   std::string gnugrepPath_;
   std::string msysSshPath_;
   std::string sumatraPath_;
   std::string winutilsPath_;
   std::string hunspellDictionariesPath_;
   std::string mathjaxPath_;
   std::string pandocPath_;
   std::string libclangPath_;
   std::string libclangHeadersPath_;
   std::string winptyPath_;

   // root directory for locating resources
   core::FilePath resourcePath_;

   bool allowFileDownloads_;
   bool allowFileUploads_;
   bool allowShell_;
   bool allowTerminalWebsockets_;
   bool allowPackageInstallation_;
   bool allowVcs_;
   bool allowCRANReposEdit_;
   bool allowVcsExecutableEdit_;
   bool allowRemovePublicFolder_;
   bool allowRpubsPublish_;
   bool allowExternalPublish_;
   bool allowPublish_;
   bool allowPresentationCommands_;

   // user info
   bool showUserIdentity_;
   std::string userIdentity_;
   core::r_util::SessionScope scope_;
   core::r_util::SessionScopeState scopeState_;
   bool multiSession_;
   bool projectSharingEnabled_;
   std::string userHomePath_;
   std::string userScratchPath_;   
   std::string launcherToken_;

   // overrides
   std::string initialWorkingDirOverride_;
   std::string initialEnvironmentFileOverride_;

   // initial project
   std::string initialProjectPath_;
   std::string rVersionsPath_;

   // monitor
   std::string monitorSharedSecret_;

   // overlay options
   std::map<std::string,std::string> overlayOptions_;
};
  
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_OPTIONS_HPP

