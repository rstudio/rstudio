/*
 * SessionOptions.cpp
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

#include <session/SessionOptions.hpp>

#include <boost/algorithm/string/trim.hpp>

#include <boost/property_tree/ptree.hpp>
#include <boost/property_tree/ini_parser.hpp>

#include <shared_core/FilePath.hpp>
#include <core/ProgramStatus.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Xdg.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>

#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RUserData.hpp>
#include <core/r_util/RSessionContext.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <core/r_util/RVersionsPosix.hpp>

#include <monitor/MonitorConstants.hpp>

#include <r/session/RSession.hpp>

#include <session/SessionConstants.hpp>
#include <session/SessionScopes.hpp>
#include <session/projects/SessionProjectSharing.hpp>

#include "session-config.h"

using namespace rstudio::core;

namespace rstudio {
namespace session {  

namespace {

void ensureDefaultDirectory(std::string* pDirectory,
                            const std::string& userHomePath)
{
   if (*pDirectory != "~")
   {
      FilePath dir = FilePath::resolveAliasedPath(*pDirectory,
                                                  FilePath(userHomePath));
      Error error = dir.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         *pDirectory = "~";
      }
   }
}

} // anonymous namespace

Options& options()
{
   static Options instance;
   return instance;
}

core::ProgramStatus Options::read(int argc, char * const argv[], std::ostream& osWarnings)
{
   using namespace boost::program_options;
   
   // get the shared secret
   monitorSharedSecret_ = core::system::getenv(kMonitorSharedSecretEnvVar);
   core::system::unsetenv(kMonitorSharedSecretEnvVar);

   // compute the resource path
   Error error = core::system::installPath("..", argv[0], &resourcePath_);
   if (error)
   {
      LOG_ERROR_MESSAGE("Unable to determine install path: "+error.getSummary());
      return ProgramStatus::exitFailure();
   }

   // detect running in OSX bundle and tweak resource path
#ifdef __APPLE__
   if (resourcePath_.completePath("Info.plist").exists())
      resourcePath_ = resourcePath_.completePath("Resources");
#endif

   // detect running in x86 directory and tweak resource path
#ifdef _WIN32
   if (resourcePath_.completePath("x86").exists())
   {
      resourcePath_ = resourcePath_.getParent();
   }
#endif

   // build options
   options_description runTests("tests");
   options_description runScript("script");
   options_description verify("verify");
   options_description program("program");
   options_description log("log");
   options_description docs("docs");
   options_description www("www");
   options_description session("session");
   options_description allow("allow");
   options_description r("r");
   options_description limits("limits");
   options_description external("external");
   options_description git("git");
   options_description user("user");
   options_description misc("misc");
   std::string saveActionDefault;
   int sameSite;

   program_options::OptionsDescription optionsDesc =
         buildOptions(&runTests, &runScript, &verify, &program, &log, &docs, &www,
                      &session, &allow, &r, &limits, &external, &git, &user, &misc,
                      &saveActionDefault, &sameSite);

   addOverlayOptions(&misc);

   optionsDesc.commandLine.add(verify);
   optionsDesc.commandLine.add(runTests);
   optionsDesc.commandLine.add(runScript);
   optionsDesc.commandLine.add(program);
   optionsDesc.commandLine.add(log);
   optionsDesc.commandLine.add(docs);
   optionsDesc.commandLine.add(www);
   optionsDesc.commandLine.add(session);
   optionsDesc.commandLine.add(allow);
   optionsDesc.commandLine.add(r);
   optionsDesc.commandLine.add(limits);
   optionsDesc.commandLine.add(external);
   optionsDesc.commandLine.add(git);
   optionsDesc.commandLine.add(user);
   optionsDesc.commandLine.add(misc);

   // define groups included in config-file processing
   optionsDesc.configFile.add(program);
   optionsDesc.configFile.add(log);
   optionsDesc.configFile.add(docs);
   optionsDesc.configFile.add(www);
   optionsDesc.configFile.add(session);
   optionsDesc.configFile.add(allow);
   optionsDesc.configFile.add(r);
   optionsDesc.configFile.add(limits);
   optionsDesc.configFile.add(external);
   optionsDesc.configFile.add(user);
   optionsDesc.configFile.add(misc);

   // read configuration
   ProgramStatus status = core::program_options::read(optionsDesc, argc,argv);
   if (status.exit())
      return status;
   
   // make sure the program mode is valid
   if (programMode_ != kSessionProgramModeDesktop &&
       programMode_ != kSessionProgramModeServer)
   {
      LOG_ERROR_MESSAGE("invalid program mode: " + programMode_);
      return ProgramStatus::exitFailure();
   }

   // resolve scope
   scope_ = r_util::SessionScope::fromProjectId(projectId_, scopeId_);
   scopeState_ = core::r_util::ScopeValid;

   sameSite_ = static_cast<rstudio::core::http::Cookie::SameSite>(sameSite);

   // call overlay hooks
   resolveOverlayOptions();
   std::string errMsg;
   if (!validateOverlayOptions(&errMsg, osWarnings))
   {
      program_options::reportError(errMsg, ERROR_LOCATION);
      return ProgramStatus::exitFailure();
   }

   // compute program identity
   programIdentity_ = "rsession-" + userIdentity_;

   // provide special home path in temp directory if we are verifying
   bool isLauncherSession = getBoolOverlayOption(kLauncherSessionOption);
   if (verifyInstallation_ && !isLauncherSession)
   {
      // we create a special home directory in server mode (since the
      // user we are running under might not have a home directory)
      // we do not do this for launcher sessions since launcher verification
      // must be run as a specific user with the normal home drive setup
      if (programMode_ == kSessionProgramModeServer)
      {
         verifyInstallationHomeDir_ = "/tmp/rstudio-verify-installation";
         Error error = FilePath(verifyInstallationHomeDir_).ensureDirectory();
         if (error)
         {
            LOG_ERROR(error);
            return ProgramStatus::exitFailure();
         }
         core::system::setenv("R_USER", verifyInstallationHomeDir_);
      }
   }

   // resolve home directory from env vars
   userHomePath_ = core::system::userHomePath("R_USER|HOME").getAbsolutePath();

   // use XDG data directory (usually ~/.local/share/rstudio, or LOCALAPPDATA
   // on Windows) as the scratch path
   userScratchPath_ = core::system::xdg::userDataDir().getAbsolutePath();

   // migrate data from old state directory to new directory
   error = core::r_util::migrateUserStateIfNecessary(
               programMode_ == kSessionProgramModeServer ?
                   core::r_util::SessionTypeServer :
                   core::r_util::SessionTypeDesktop);
   if (error)
   {
      LOG_ERROR(error);
   }


   // set HOME if we are in standalone mode (this enables us to reflect
   // R_USER back into HOME on Linux)
   if (standalone())
      core::system::setenv("HOME", userHomePath_);

   // ensure that default working dir and default project dir exist
   ensureDefaultDirectory(&defaultWorkingDir_, userHomePath_);
   ensureDefaultDirectory(&deprecatedDefaultProjectDir_, userHomePath_);

   // session timeout seconds is always -1 in desktop mode
   if (programMode_ == kSessionProgramModeDesktop)
      timeoutMinutes_ = 0;

   // convert string save action default to intenger
   if (saveActionDefault == "yes")
      saveActionDefault_ = r::session::kSaveActionSave;
   else if (saveActionDefault == "no")
      saveActionDefault_ = r::session::kSaveActionNoSave;
   else if (saveActionDefault == "ask" || saveActionDefault.empty())
      saveActionDefault_ = r::session::kSaveActionAsk;
   else
   {
      program_options::reportWarnings(
         "Invalid value '" + saveActionDefault + "' for "
         "session-save-action-default. Valid values are yes, no, and ask.",
         ERROR_LOCATION);
      saveActionDefault_ = r::session::kSaveActionAsk;
   }
   
   // convert relative paths by completing from the app resource path
   resolvePath(resourcePath_, &rResourcesPath_);
   resolvePath(resourcePath_, &wwwLocalPath_);
   resolvePath(resourcePath_, &wwwSymbolMapsPath_);
   resolvePath(resourcePath_, &coreRSourcePath_);
   resolvePath(resourcePath_, &modulesRSourcePath_);
   resolvePath(resourcePath_, &sessionLibraryPath_);
   resolvePath(resourcePath_, &sessionPackageArchivesPath_);
   resolvePostbackPath(resourcePath_, &rpostbackPath_);
#ifdef _WIN32
   resolvePath(resourcePath_, &consoleIoPath_);
   resolvePath(resourcePath_, &gnudiffPath_);
   resolvePath(resourcePath_, &gnugrepPath_);
   resolvePath(resourcePath_, &msysSshPath_);
   resolvePath(resourcePath_, &sumatraPath_);
   resolvePath(resourcePath_, &winutilsPath_);
   resolvePath(resourcePath_, &winptyPath_);

   // winpty.dll lives next to rsession.exe on a full install; otherwise
   // it lives in a directory named 32 or 64
   core::FilePath pty(winptyPath_);
   std::string completion;
   if (pty.isWithin(resourcePath_))
   {
#ifdef _WIN64
      completion = "winpty.dll";
#else
      completion = "x86/winpty.dll";
#endif
   }
   else
   {
#ifdef _WIN64
      completion = "64/bin/winpty.dll";
#else
      completion = "32/bin/winpty.dll";
#endif
   }
   winptyPath_ = pty.completePath(completion).getAbsolutePath();
#endif // _WIN32
   resolvePath(resourcePath_, &hunspellDictionariesPath_);
   resolvePath(resourcePath_, &mathjaxPath_);
   resolvePath(resourcePath_, &libclangHeadersPath_);
   resolvePandocPath(resourcePath_, &pandocPath_);

   // rsclang
   if (libclangPath_ != kDefaultRsclangPath)
   {
      libclangPath_ += "/5.0.2";
   }
   resolveRsclangPath(resourcePath_, &libclangPath_);

   // shared secret with parent
   secret_ = core::system::getenv("RS_SHARED_SECRET");
   /* SECURITY: Need RS_SHARED_SECRET to be available to
      rpostback. However, we really ought to communicate
      it in a more secure manner than this, at least on
      Windows where even within the same user session some
      processes can have different priviliges (integrity
      levels) than others. For example, using a named pipe
      with proper SACL to retrieve the shared secret, where
      the name of the pipe is in an environment variable. */
   //core::system::unsetenv("RS_SHARED_SECRET");

   // show user home page
   showUserHomePage_ = core::system::getenv(kRStudioUserHomePage) == "1";
   core::system::unsetenv(kRStudioUserHomePage);

   // multi session
   multiSession_ = (programMode_ == kSessionProgramModeDesktop) ||
                   (core::system::getenv(kRStudioMultiSession) == "1");

   // initial working dir override
   initialWorkingDirOverride_ = core::system::getenv(kRStudioInitialWorkingDir);
   core::system::unsetenv(kRStudioInitialWorkingDir);

   // initial environment file override
   initialEnvironmentFileOverride_ = core::system::getenv(kRStudioInitialEnvironment);
   core::system::unsetenv(kRStudioInitialEnvironment);

   // project sharing enabled
   projectSharingEnabled_ =
                core::system::getenv(kRStudioDisableProjectSharing).empty();

   // initial project (can either be a command line param or via env)
   r_util::SessionScope scope = sessionScope();
   if (!scope.empty())
   {
        scopeState_ = r_util::validateSessionScope(
                       scope,
                       userHomePath(),
                       userScratchPath(),
                       session::projectIdToFilePath(userScratchPath(), 
                                 FilePath(getOverlayOption(
                                       kSessionSharedStoragePath))),
                       projectSharingEnabled(),
                       &initialProjectPath_);
   }
   else
   {
      initialProjectPath_ = core::system::getenv(kRStudioInitialProject);
      core::system::unsetenv(kRStudioInitialProject);
   }

   // limit rpc client uid
   limitRpcClientUid_ = -1;
   std::string limitUid = core::system::getenv(kRStudioLimitRpcClientUid);
   if (!limitUid.empty())
   {
      limitRpcClientUid_ = core::safe_convert::stringTo<int>(limitUid, -1);
      core::system::unsetenv(kRStudioLimitRpcClientUid);
   }

   // get R versions path
   rVersionsPath_ = core::system::getenv(kRStudioRVersionsPath);
   core::system::unsetenv(kRStudioRVersionsPath);

   // capture default R version environment variables
   defaultRVersion_ = core::system::getenv(kRStudioDefaultRVersion);
   core::system::unsetenv(kRStudioDefaultRVersion);
   defaultRVersionHome_ = core::system::getenv(kRStudioDefaultRVersionHome);
   core::system::unsetenv(kRStudioDefaultRVersionHome);
   
   // capture auth environment variables
   authMinimumUserId_ = 0;
   if (programMode_ == kSessionProgramModeServer)
   {
      authRequiredUserGroup_ = core::system::getenv(kRStudioRequiredUserGroup);
      core::system::unsetenv(kRStudioRequiredUserGroup);

      authMinimumUserId_ = safe_convert::stringTo<unsigned int>(
                              core::system::getenv(kRStudioMinimumUserId), 100);

#ifndef _WIN32
      r_util::setMinUid(authMinimumUserId_);
#endif
      core::system::unsetenv(kRStudioMinimumUserId);
   }

   // signing key - used for verifying incoming RPC requests
   // in standalone mode
   signingKey_ = core::system::getenv(kRStudioSigningKey);

   if (verifySignatures_)
   {
      // generate our own signing key to be used when posting back to ourselves
      // this key is kept secret within this process and any child processes,
      // and only allows communication from this rsession process and its children
      error = core::system::crypto::generateRsaKeyPair(&sessionRsaPublicKey_, &sessionRsaPrivateKey_);
      if (error)
         LOG_ERROR(error);

      core::system::setenv(kRSessionRsaPublicKey, sessionRsaPublicKey_);
      core::system::setenv(kRSessionRsaPrivateKey, sessionRsaPrivateKey_);
   }

   // load cran options from repos.conf
   FilePath reposFile(rCRANReposFile());
   rCRANMultipleRepos_ = parseReposConfig(reposFile);

   // return status
   return status;
}

std::string Options::parseReposConfig(FilePath reposFile)
{
    using namespace boost::property_tree;

    if (!reposFile.exists())
      return "";

   std::shared_ptr<std::istream> pIfs;
   Error error = FilePath(reposFile).openForRead(pIfs);
   if (error)
   {
      core::program_options::reportError("Unable to open repos file: " + reposFile.getAbsolutePath(),
                  ERROR_LOCATION);

      return "";
   }

   try
   {
      ptree pt;
      ini_parser::read_ini(reposFile.getAbsolutePath(), pt);

      if (!pt.get_child_optional("CRAN"))
      {
         LOG_ERROR_MESSAGE("Repos file " + reposFile.getAbsolutePath() + " is missing CRAN entry.");
         return "";
      }

      std::stringstream ss;

      for (ptree::iterator it = pt.begin(); it != pt.end(); it++)
      {
         if (it != pt.begin())
         {
            ss << "|";
         }

         ss << it->first << "|" << it->second.get_value<std::string>();
      }

      return ss.str();
   }
   catch(const std::exception& e)
   {
      core::program_options::reportError(
         "Error reading " + reposFile.getAbsolutePath() + ": " + std::string(e.what()),
        ERROR_LOCATION);

      return "";
   }
}

bool Options::getBoolOverlayOption(const std::string& name)
{
   std::string optionValue = getOverlayOption(name);
   return boost::algorithm::trim_copy(optionValue) == "1";
}

void Options::resolvePath(const FilePath& resourcePath,
                          std::string* pPath)
{
   if (!pPath->empty())
      *pPath = resourcePath.completePath(*pPath).getAbsolutePath();
}

#ifdef __APPLE__

void Options::resolvePostbackPath(const FilePath& resourcePath,
                                  std::string* pPath)
{
   // On OSX we keep the postback scripts over in the MacOS directory
   // rather than in the Resources directory -- make this adjustment
   // when the default postback path has been passed
   if (*pPath == kDefaultPostbackPath && programMode() == kSessionProgramModeDesktop)
   {
      FilePath path = resourcePath.getParent().completePath("MacOS/postback/rpostback");
      *pPath = path.getAbsolutePath();
   }
   else
   {
      resolvePath(resourcePath, pPath);
   }
}

void Options::resolvePandocPath(const FilePath& resourcePath,
                                std::string* pPath)
{
   if (*pPath == kDefaultPandocPath && programMode() == kSessionProgramModeDesktop)
   {
      FilePath path = resourcePath.getParent().completePath("MacOS/pandoc");
      *pPath = path.getAbsolutePath();
   }
   else
   {
      resolvePath(resourcePath, pPath);
   }
}

void Options::resolveRsclangPath(const FilePath& resourcePath,
                                 std::string* pPath)
{
   if (*pPath == kDefaultRsclangPath && programMode() == kSessionProgramModeDesktop)
   {
      FilePath path = resourcePath.getParent().completePath("MacOS/rsclang");
      *pPath = path.getAbsolutePath();
   }
   else
   {
      resolvePath(resourcePath, pPath);
   }
}

#else

void Options::resolvePostbackPath(const FilePath& resourcePath,
                                  std::string* pPath)
{
   resolvePath(resourcePath, pPath);
}

void Options::resolvePandocPath(const FilePath& resourcePath,
                                  std::string* pPath)
{
   resolvePath(resourcePath, pPath);
}

void Options::resolveRsclangPath(const FilePath& resourcePath,
                                 std::string* pPath)
{
   resolvePath(resourcePath, pPath);
}
#endif
   
} // namespace session
} // namespace rstudio
