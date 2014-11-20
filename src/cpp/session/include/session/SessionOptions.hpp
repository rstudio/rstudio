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

#include <R_ext/RStartup.h>

#include <session/SessionConstants.hpp>

namespace rscore {
   class ProgramStatus;
}

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
   rscore::ProgramStatus read(int argc, char * const argv[]);
   virtual ~Options() {}
   
   bool verifyInstallation() const
   {
      return verifyInstallation_;
   }

   rscore::FilePath verifyInstallationHomeDir() const
   {
      if (!verifyInstallationHomeDir_.empty())
         return rscore::FilePath(verifyInstallationHomeDir_.c_str());
      else
         return rscore::FilePath();
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
   rscore::FilePath agreementFilePath() const
   { 
      if (!agreementFilePath_.empty())
         return rscore::FilePath(agreementFilePath_.c_str());
      else
         return rscore::FilePath();
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

   rscore::FilePath wwwSymbolMapsPath() const
   {
      return rscore::FilePath(wwwSymbolMapsPath_.c_str());
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

   rscore::FilePath preflightScriptPath() const
   {
      return rscore::FilePath(preflightScript_.c_str());
   }

   int timeoutMinutes() const { return timeoutMinutes_; }

   int disconnectedTimeoutMinutes() { return disconnectedTimeoutMinutes_; }

   bool createProfile() const { return createProfile_; }

   bool createPublicFolder() const { return createPublicFolder_; }

   bool rProfileOnResumeDefault() const { return rProfileOnResumeDefault_; }

   int saveActionDefault() const { return saveActionDefault_; }

   unsigned int minimumUserId() const { return 100; }
   
   rscore::FilePath coreRSourcePath() const
   { 
      return rscore::FilePath(coreRSourcePath_.c_str());
   }
   
   rscore::FilePath modulesRSourcePath() const
   { 
      return rscore::FilePath(modulesRSourcePath_.c_str());
   }

   rscore::FilePath sessionLibraryPath() const
   {
      return rscore::FilePath(sessionLibraryPath_.c_str());
   }
   
   rscore::FilePath sessionPackagesPath() const
   {
      return rscore::FilePath(sessionPackagesPath_.c_str());
   }

   rscore::FilePath sessionPackageArchivesPath() const
   {
      return rscore::FilePath(sessionPackageArchivesPath_.c_str());
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

   rscore::FilePath rResourcesPath() const
   {
      return rscore::FilePath(rResourcesPath_.c_str());
   }

   std::string rHomeDirOverride()
   {
      return std::string(rHomeDirOverride_.c_str());
   }

   std::string rDocDirOverride()
   {
      return std::string(rDocDirOverride_.c_str());
   }

   bool autoReloadSource() const { return autoReloadSource_; }

   // limits
   int limitFileUploadSizeMb() const { return limitFileUploadSizeMb_; }
   int limitCpuTimeMinutes() const { return limitCpuTimeMinutes_; }

   int limitRpcClientUid() const { return limitRpcClientUid_; }

   bool limitXfsDiskQuota() const { return limitXfsDiskQuota_; }
   
   // external
   rscore::FilePath rpostbackPath() const
   {
      return rscore::FilePath(rpostbackPath_.c_str());
   }

   rscore::FilePath consoleIoPath() const
   {
      return rscore::FilePath(consoleIoPath_.c_str());
   }

   rscore::FilePath gnudiffPath() const
   {
      return rscore::FilePath(gnudiffPath_.c_str());
   }

   rscore::FilePath gnugrepPath() const
   {
      return rscore::FilePath(gnugrepPath_.c_str());
   }

   rscore::FilePath msysSshPath() const
   {
      return rscore::FilePath(msysSshPath_.c_str());
   }

   rscore::FilePath sumatraPath() const
   {
      return rscore::FilePath(sumatraPath_.c_str());
   }
   
   rscore::FilePath hunspellDictionariesPath() const
   {
      return rscore::FilePath(hunspellDictionariesPath_.c_str());
   }

   rscore::FilePath mathjaxPath() const
   {
      return rscore::FilePath(mathjaxPath_.c_str());
   }

   rscore::FilePath pandocPath() const
   {
      return rscore::FilePath(pandocPath_.c_str());
   }

   rscore::FilePath libclangPath() const
   {
      return rscore::FilePath(libclangPath_.c_str());
   }

   rscore::FilePath libclangHeadersPath() const
   {
      return rscore::FilePath(libclangHeadersPath_.c_str());
   }

   bool allowFileDownloads() const
   {
      return allowOverlay() || allowFileDownloads_;
   }

   bool allowShell() const
   {
      return allowOverlay() || allowShell_;
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

   // user info
   std::string userIdentity() const 
   { 
      return std::string(userIdentity_.c_str()); 
   }
   
   bool showUserIdentity() const
   {
      return showUserIdentity_;
   }

   rscore::FilePath userHomePath() const
   { 
      return rscore::FilePath(userHomePath_.c_str());
   }
   
   rscore::FilePath userScratchPath() const
   { 
      return rscore::FilePath(userScratchPath_.c_str());
   }

   rscore::FilePath userLogPath() const
   {
      return userScratchPath().childPath("log");
   }

   rscore::FilePath initialWorkingDirOverride()
   {
      if (!initialWorkingDirOverride_.empty())
         return rscore::FilePath(initialWorkingDirOverride_.c_str());
      else
         return rscore::FilePath();
   }

   rscore::FilePath initialEnvironmentFileOverride()
   {
      if (!initialEnvironmentFileOverride_.empty())
         return rscore::FilePath(initialEnvironmentFileOverride_.c_str());
      else
         return rscore::FilePath();
   }

   rscore::FilePath initialProjectPath()
   {
      if (!initialProjectPath_.empty())
         return rscore::FilePath(initialProjectPath_.c_str());
      else
         return rscore::FilePath();
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
   rscore::string_utils::LineEnding sourceLineEnding() const
   {
      return rscore::string_utils::LineEndingPosix;
   }

   // The line ending we persist to disk with. This could potentially
   // be a per-user or even per-file option.
   rscore::string_utils::LineEnding sourcePersistLineEnding() const
   {
      return rscore::string_utils::LineEndingNative;
   }

   std::string monitorSharedSecret() const
   {
      return monitorSharedSecret_.c_str();
   }

   bool standalone() const
   {
      return standalone_;
   }

   std::string getOverlayOption(const std::string& name)
   {
      return overlayOptions_[name];
   }

   bool getBoolOverlayOption(const std::string& name);

private:
   void resolvePath(const rscore::FilePath& resourcePath,
                    std::string* pPath);
   void resolvePostbackPath(const rscore::FilePath& resourcePath,
                            std::string* pPath);
   void resolvePandocPath(const rscore::FilePath& resourcePath, std::string* pPath);

   void resolveRsclangPath(const rscore::FilePath& resourcePath, std::string* pPath);

   void addOverlayOptions(boost::program_options::options_description* pOpt);
   bool validateOverlayOptions(std::string* pErrMsg);
   void resolveOverlayOptions();
   bool allowOverlay() const;

private:
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

   // r
   std::string coreRSourcePath_;
   std::string modulesRSourcePath_;
   std::string sessionLibraryPath_;
   std::string sessionPackagesPath_;
   std::string sessionPackageArchivesPath_;
   std::string rLibsUser_;
   std::string rCRANRepos_;
   bool autoReloadSource_ ;
   int rCompatibleGraphicsEngineVersion_;
   std::string rResourcesPath_;
   std::string rHomeDirOverride_;
   std::string rDocDirOverride_;
   
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
   std::string hunspellDictionariesPath_;
   std::string mathjaxPath_;
   std::string pandocPath_;
   std::string libclangPath_;
   std::string libclangHeadersPath_;

   bool allowFileDownloads_;
   bool allowShell_;
   bool allowPackageInstallation_;
   bool allowVcs_;
   bool allowCRANReposEdit_;
   bool allowVcsExecutableEdit_;
   bool allowRemovePublicFolder_;
   bool allowRpubsPublish_;

   // user info
   bool showUserIdentity_;
   std::string userIdentity_;
   std::string userHomePath_;
   std::string userScratchPath_;   

   // overrides
   std::string initialWorkingDirOverride_;
   std::string initialEnvironmentFileOverride_;

   // initial project
   std::string initialProjectPath_;

   // monitor
   std::string monitorSharedSecret_;

   // overlay options
   std::map<std::string,std::string> overlayOptions_;
};
  
} // namespace session

#endif // SESSION_SESSION_OPTIONS_HPP

