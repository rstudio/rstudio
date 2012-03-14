/*
 * SessionOptions.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

   unsigned int minimumUserId() const { return 100; }
   
   core::FilePath coreRSourcePath() const 
   { 
      return core::FilePath(coreRSourcePath_.c_str());
   }
   
   core::FilePath modulesRSourcePath() const 
   { 
      return core::FilePath(modulesRSourcePath_.c_str()); 
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

   core::FilePath rHelpCssFilePath() const
   {
      return core::FilePath(rHelpCssFilePath_.c_str());
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

   core::FilePath rspdflatexPath() const
   {
      return core::FilePath(rspdflatexPath_.c_str());
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
   
   core::FilePath hunspellDictionariesPath() const
   {
      return core::FilePath(hunspellDictionariesPath_.c_str());
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

private:
   // verify
   bool verifyInstallation_;

   // program
   std::string programIdentity_;
   std::string programMode_;

   // agreement
   std::string agreementFilePath_;

   // docs
   std::string docsURL_;
   
   // www
   std::string wwwLocalPath_;
   std::string wwwPort_;

   // session
   std::string secret_;
   std::string preflightScript_;
   int timeoutMinutes_;
   bool createPublicFolder_;

   // r
   std::string coreRSourcePath_;
   std::string modulesRSourcePath_;
   std::string sessionPackagesPath_;
   std::string rLibsUser_;
   std::string rCRANRepos_;
   bool autoReloadSource_ ;
   int rCompatibleGraphicsEngineVersion_;
   std::string rHelpCssFilePath_;
   std::string rHomeDirOverride_;
   std::string rDocDirOverride_;
   
   // limits
   int limitFileUploadSizeMb_;
   int limitCpuTimeMinutes_;
   int limitRpcClientUid_;
   bool limitXfsDiskQuota_;
   
   // external
   std::string rpostbackPath_;
   std::string rspdflatexPath_;
   std::string consoleIoPath_;
   std::string gnudiffPath_;
   std::string gnugrepPath_;
   std::string msysSshPath_;
   std::string hunspellDictionariesPath_;

   // user info
   std::string userIdentity_;
   std::string userHomePath_;
   std::string userScratchPath_;   

   // overrides
   std::string initialWorkingDirOverride_;
   std::string initialEnvironmentFileOverride_;

   // initial project
   std::string initialProjectPath_;
};
  
} // namespace session

#endif // SESSION_SESSION_OPTIONS_HPP

