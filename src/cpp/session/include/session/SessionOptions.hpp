/*
 * SessionOptions.hpp
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

#ifndef SESSION_SESSION_OPTIONS_HPP
#define SESSION_SESSION_OPTIONS_HPP

#include <string>

#include <boost/utility.hpp>

#include <shared_core/SafeConvert.hpp>
#include <shared_core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/StringUtils.hpp>
#include <core/ProgramOptions.hpp>

#include <core/r_util/RSessionContext.hpp>


#include <session/SessionOptions.gen.hpp>

namespace rstudio {
namespace core {
   class ProgramStatus;
   class FilePath;
}
}

namespace rstudio {
namespace session {
 

// singleton
class Options;
Options& options();
   
class Options : public GeneratedOptions,
                boost::noncopyable
{
private:
   Options()
   {
   }
   friend Options& options();
   
   // COPYING: boost::noncopyable

public:
   // read options  
   core::ProgramStatus read(int argc, char * const argv[], std::ostream& osWarnings) override;
   virtual ~Options() {}

   void addOverlayOptions(boost::program_options::options_description* pOpt);

   static std::string parseReposConfig(core::FilePath reposFile);

   std::string getOverlayOption(const std::string& name)
   {
      return overlayOptions_[name];
   }

   bool getBoolOverlayOption(const std::string& name);

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
      return userScratchPath().completeChildPath("log");
   }

   bool multiSession() const
   {
      return multiSession_;
   }

   bool showUserHomePage() const
   {
      return showUserHomePage_;
   }

   bool supportsDriversLicensing() const;

   core::r_util::SessionScope sessionScope() const
   {
      return scope_;
   }

   core::FilePath initialWorkingDirOverride()
   {
      if (!initialWorkingDirOverride_.empty())
         return core::FilePath(initialWorkingDirOverride_.c_str());
      else
         return core::FilePath();
   }

   const std::string& signingKey() const
   {
      return signingKey_;
   }

   std::string sessionRsaPublicKey() const
   {
      return sessionRsaPublicKey_;
   }

   std::string sessionRsaPrivateKey() const
   {
      return sessionRsaPrivateKey_;
   }

   bool switchProjectsWithUrl() const
   {
      return programMode() == kSessionProgramModeServer &&
             options().multiSession() == true;
   }

   core::FilePath verifyInstallationHomeDir() const
   {
      if (!verifyInstallationHomeDir_.empty())
         return core::FilePath(verifyInstallationHomeDir_.c_str());
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

   std::string programIdentity() const
   {
      return programIdentity_;
   }

   core::r_util::SessionContext sessionContext() const
   {
      return core::r_util::SessionContext(userIdentity(), sessionScope());
   }

   unsigned int authMinimumUserId() const
   {
      return authMinimumUserId_;
   }

   // The line ending we use when working with source documents
   // in memory. This doesn't really make sense for the user to
   // change.
   core::string_utils::LineEnding sourceLineEnding() const
   {
      return core::string_utils::LineEndingPosix;
   }

   bool projectSharingEnabled() const
   {
      return projectSharingEnabled_;
   }

   std::string monitorSharedSecret() const
   {
      return monitorSharedSecret_;
   }

   core::r_util::SessionScopeState scopeState() const
   {
      return scopeState_;
   }

   std::string rCRANMultipleRepos() const
   {
      return rCRANMultipleRepos_;
   }

   std::string sharedSecret() const
   {
      return secret_;
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

   int limitRpcClientUid() const
   {
      return limitRpcClientUid_;
   }

   core::FilePath rVersionsPath() const
   {
      if (!rVersionsPath_.empty())
         return core::FilePath(rVersionsPath_);
      else
         return core::FilePath();
   }

   std::string defaultRVersion() const
   {
      return defaultRVersion_;
   }

   std::string defaultRVersionHome() const
   {
      return defaultRVersionHome_;
   }

   std::string authRequiredUserGroup() const
   {
      return authRequiredUserGroup_;
   }

private:
   bool multiSession_;
   bool showUserHomePage_;
   bool projectSharingEnabled_;

   core::r_util::SessionScope scope_;
   core::r_util::SessionScopeState scopeState_;

   std::string userScratchPath_;
   std::string userHomePath_;
   std::string initialWorkingDirOverride_;
   std::string signingKey_;
   std::string verifyInstallationHomeDir_;
   std::string initialEnvironmentFileOverride_;
   std::string monitorSharedSecret_;
   std::string programIdentity_;
   std::string rCRANMultipleRepos_;
   std::string secret_;
   std::string initialProjectPath_;
   std::string rVersionsPath_;
   std::string defaultRVersion_;
   std::string defaultRVersionHome_;
   std::string authRequiredUserGroup_;

   // root directory for locating resources
   core::FilePath resourcePath_;

   unsigned int authMinimumUserId_;
   int limitRpcClientUid_;

   // in-session generated RSA keys
   std::string sessionRsaPublicKey_;
   std::string sessionRsaPrivateKey_;

   // overlay options
   std::map<std::string,std::string> overlayOptions_;

   bool validateOverlayOptions(std::string* pErrMsg, std::ostream& osWarnings);

   void resolvePath(const core::FilePath& resourcePath, std::string* pPath);
   void resolvePostbackPath(const core::FilePath& resourcePath, std::string* pPath);
   void resolvePandocPath(const core::FilePath& resourcePath, std::string* pPath);
   void resolveRsclangPath(const core::FilePath& resourcePath, std::string* pPath);

   void resolveOverlayOptions();
   bool allowOverlay() const;
};
  
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_OPTIONS_HPP

