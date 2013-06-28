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

namespace core {
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
   core::ProgramStatus read(int argc, char * const argv[]);   
   virtual ~Options() {}
   
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

   std::string sharedSecret() const
   {
      return std::string(secret_.c_str());
   }

   core::FilePath preflightScriptPath() const
   {
      return core::FilePath(preflightScript_.c_str());
   }

   int timeoutMinutes() const { return timeoutMinutes_; }

   bool createPublicFolder() const { return createPublicFolder_; }

   bool rProfileOnResumeDefault() const { return rProfileOnResumeDefault_; }

   unsigned int minimumUserId() const { return 100; }
   
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
   
   core::FilePath sessionPackagesPath() const
   {
      return core::FilePath(sessionPackagesPath_.c_str());
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
   
   core::FilePath hunspellDictionariesPath() const
   {
      return core::FilePath(hunspellDictionariesPath_.c_str());
   }

   core::FilePath mathjaxPath() const
   {
      return core::FilePath(mathjaxPath_.c_str());
   }

   bool allowFileDownloads() const
   {
      return allowFileDownloads_;
   }

   bool allowShell() const
   {
      return allowShell_;
   }

   bool allowPackageInstallation() const
   {
      return allowPackageInstallation_;
   }

   bool allowVcs() const
   {
      return allowVcs_;
   }

   bool allowCRANReposEdit() const
   {
      return allowCRANReposEdit_;
   }

   bool allowVcsExecutableEdit() const
   {
      return allowVcsExecutableEdit_;
   }

   bool allowRemovePublicFolder() const
   {
      return allowRemovePublicFolder_;
   }

   // user info
   std::string userIdentity() const 
   { 
      return std::string(userIdentity_.c_str()); 
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

   // The line ending we persist to disk with. This could potentially
   // be a per-user or even per-file option.
   core::string_utils::LineEnding sourcePersistLineEnding() const
   {
      return core::string_utils::LineEndingNative;
   }

   std::string monitorSharedSecret() const
   {
      return monitorSharedSecret_.c_str();
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


   void addOverlayOptions(boost::program_options::options_description* pOpt);
   bool validateOverlayOptions(std::string* pErrMsg);
   void resolveOverlayOptions();

private:
   // verify
   bool verifyInstallation_;
   std::string verifyInstallationHomeDir_;

   // program
   std::string programIdentity_;
   std::string programMode_;

   // agreement
   std::string agreementFilePath_;

   // docs
   std::string docsURL_;
   
   // www
   std::string wwwLocalPath_;
   std::string wwwSymbolMapsPath_;
   std::string wwwPort_;

   // session
   std::string secret_;
   std::string preflightScript_;
   int timeoutMinutes_;
   bool createPublicFolder_;
   bool rProfileOnResumeDefault_;

   // r
   std::string coreRSourcePath_;
   std::string modulesRSourcePath_;
   std::string sessionLibraryPath_;
   std::string sessionPackagesPath_;
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

   bool allowFileDownloads_;
   bool allowShell_;
   bool allowPackageInstallation_;
   bool allowVcs_;
   bool allowCRANReposEdit_;
   bool allowVcsExecutableEdit_;
   bool allowRemovePublicFolder_;

   // user info
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

