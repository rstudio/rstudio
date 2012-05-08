/*
 * SessionOptions.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <session/SessionOptions.hpp>

#include <boost/foreach.hpp>

#include <core/FilePath.hpp>
#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <session/SessionConstants.hpp>

using namespace core ;

namespace session {  

namespace {

const char* const kDefaultPostbackPath = "bin/postback/rpostback";
const char* const kDefaultRsPdfLatexPath = "bin/rspdflatex";

void resolvePath(const FilePath& resourcePath, std::string* pPath)
{
   if (!pPath->empty())
      *pPath = resourcePath.complete(*pPath).absolutePath();
}

#ifdef __APPLE__

void resolvePostbackPath(const FilePath& resourcePath, std::string* pPath)
{
   // On OSX we keep the postback scripts over in the MacOS directory
   // rather than in the Resources directory -- make this adjustment
   // when the default postback path has been passed
   if (*pPath == kDefaultPostbackPath)
   {
      FilePath path = resourcePath.parent().complete("MacOS/postback/rpostback");
      *pPath = path.absolutePath();
   }
   else
   {
      resolvePath(resourcePath, pPath);
   }
}

void resolveRsPdfLatexPath(const FilePath& resourcePath, std::string* pPath)
{
   // On OSX we keep the rspdflatex biary over in the MacOS directory
   // rather than in the Resources directory -- make this adjustment
   // when the default rspdflatex path has been passed
   if (*pPath == kDefaultRsPdfLatexPath)
   {
      FilePath path = resourcePath.parent().complete("MacOS/rspdflatex");
      *pPath = path.absolutePath();
   }
   else
   {
      resolvePath(resourcePath, pPath);
   }
}

#else

void resolvePostbackPath(const FilePath& resourcePath, std::string* pPath)
{
   resolvePath(resourcePath, pPath);
}

void resolveRsPdfLatexPath(const FilePath& resourcePath, std::string* pPath)
{
   resolvePath(resourcePath, pPath);

   FilePath exePath(*pPath);
   core::system::fixupExecutablePath(&exePath);
   *pPath = exePath.absolutePath();
}

#endif

}

Options& options()
{
   static Options instance ;
   return instance ;
}
   
core::ProgramStatus Options::read(int argc, char * const argv[])
{
   using namespace boost::program_options ;
   
   // compute the resource path
   FilePath resourcePath;
   Error error = core::system::installPath("..", argc, argv, &resourcePath);
   if (error)
   {
      LOG_ERROR_MESSAGE("Unable to determine install path: "+error.summary());
      return ProgramStatus::exitFailure();
   }

   // detect running in OSX bundle and tweak resource path
#ifdef __APPLE__
   if (resourcePath.complete("Info.plist").exists())
      resourcePath = resourcePath.complete("Resources");
#endif

   // detect running in x64 directory and tweak resource path
#ifdef _WIN32
   if (resourcePath.complete("x64").exists())
      resourcePath = resourcePath.parent();
#endif

   // verify installation flag
   options_description verify("verify");
   verify.add_options()
     (kVerifyInstallationSessionOption,
     value<bool>(&verifyInstallation_)->default_value(false),
     "verify the current installation");

   // program - name and execution
   options_description program("program");
   program.add_options()
      (kProgramModeSessionOption,
         value<std::string>(&programMode_)->default_value("server"),
         "program mode (desktop or server");
   
   // agreement
   options_description agreement("agreement");
   agreement.add_options()
      ("agreement-file",
      value<std::string>(&agreementFilePath_)->default_value(""),
      "agreement file");

   // docs url
   options_description docs("docs");
   docs.add_options()
      ("docs-url",
       value<std::string>(&docsURL_)->default_value(""),
       "custom docs url");

   // www options
   options_description www("www") ;
   www.add_options()
      ("www-local-path",
         value<std::string>(&wwwLocalPath_)->default_value("www"),
         "www local path")
      ("www-port",
         value<std::string>(&wwwPort_)->default_value("8787"),
         "port to listen on");

   // session options
   options_description session("session") ;
   session.add_options()
      ("session-timeout-minutes",
         value<int>(&timeoutMinutes_)->default_value(120),
         "session timeout (minutes)" )
      ("session-preflight-script",
         value<std::string>(&preflightScript_)->default_value(""),
         "session preflight script")
      ("session-create-public-folder",
         value<bool>(&createPublicFolder_)->default_value(false),
         "automatically create public folder");

   // r options
   bool rShellEscape; // no longer works but don't want to break any
                      // config files which formerly used it
                      // TODO: eliminate this option entirely
   options_description r("r") ;
   r.add_options()
      ("r-core-source",
         value<std::string>(&coreRSourcePath_)->default_value("R"),
         "Core R source path")
      ("r-modules-source", 
         value<std::string>(&modulesRSourcePath_)->default_value("R/modules"),
         "Modules R source path")
      ("r-session-packages",
         value<std::string>(&sessionPackagesPath_)->default_value("R/library"),
         "R packages path")
      ("r-libs-user",
         value<std::string>(&rLibsUser_)->default_value("~/R/library"),
         "R user library path")
      ("r-cran-repos",
         value<std::string>(&rCRANRepos_)->default_value(""),
         "Default CRAN repository")
      ("r-auto-reload-source",
         value<bool>(&autoReloadSource_)->default_value(false),
         "Reload R source if it changes during the session")
      ("r-compatible-graphics-engine-version",
         value<int>(&rCompatibleGraphicsEngineVersion_)->default_value(9),
         "Maximum graphics engine version we are compatible with")
      ("r-resources-path",
         value<std::string>(&rResourcesPath_)->default_value("resources"),
         "Directory containing external resources")
      ("r-shell-escape",
         value<bool>(&rShellEscape)->default_value(false),
         "Support shell escape (deprecated, no longer works)")
      ("r-home-dir-override",
         value<std::string>(&rHomeDirOverride_)->default_value(""),
         "Override for R_HOME (used for debug configurations)")
      ("r-doc-dir-override",
         value<std::string>(&rDocDirOverride_)->default_value(""),
         "Override for R_DOC_DIR (used for debug configurations)");

   // limits options
   options_description limits("limits");
   limits.add_options()
      ("limit-file-upload-size-mb",
       value<int>(&limitFileUploadSizeMb_)->default_value(0),
       "limit of file upload size")
      ("limit-cpu-time-minutes",
       value<int>(&limitCpuTimeMinutes_)->default_value(0),
       "limit on time of top level computations")
      ("limit-xfs-disk-quota",
       value<bool>(&limitXfsDiskQuota_)->default_value(false),
       "limit xfs disk quota");
   
   // external options
   options_description external("external");
   external.add_options()
      ("external-rpostback-path", 
       value<std::string>(&rpostbackPath_)->default_value(kDefaultPostbackPath),
       "Path to rpostback executable")
      ("external-rspdflatex-path",
       value<std::string>(&rspdflatexPath_)->default_value(kDefaultRsPdfLatexPath),
       "Path to tex scripts")
      ("external-consoleio-path",
       value<std::string>(&consoleIoPath_)->default_value("bin/consoleio.exe"),
       "Path to consoleio executable")
      ("external-gnudiff-path",
       value<std::string>(&gnudiffPath_)->default_value("bin/gnudiff"),
       "Path to gnudiff utilities (windows-only)")
      ("external-gnugrep-path",
       value<std::string>(&gnugrepPath_)->default_value("bin/gnugrep"),
       "Path to gnugrep utilities (windows-only)")
      ("external-msysssh-path",
       value<std::string>(&msysSshPath_)->default_value("bin/msys_ssh"),
       "Path to msys_ssh utilities (windows-only)")
      ("external-sumatra-path",
       value<std::string>(&sumatraPath_)->default_value("bin/sumatra"),
       "Path to SumatraPDF (windows-only)")
      ("external-hunspell-dictionaries-path",
       value<std::string>(&hunspellDictionariesPath_)->default_value("resources/dictionaries"),
       "Path to hunspell dictionaries")
      ("external-mathjax-path",
        value<std::string>(&mathjaxPath_)->default_value("resources/mathjax"),
        "Path to mathjax library");

   // user options (default user identity to current username)
   std::string currentUsername = core::system::username();
   options_description user("user") ;
   user.add_options()
      (kUserIdentitySessionOption "," kUserIdentitySessionOptionShort,
       value<std::string>(&userIdentity_)->default_value(currentUsername),
       "user identity" );
   
   // define program options
   FilePath defaultConfigPath("/etc/rstudio/rsession.conf");
   std::string configFile = defaultConfigPath.exists() ?
                                 defaultConfigPath.absolutePath() : "";
   core::program_options::OptionsDescription optionsDesc("rsession",
                                                         configFile);

   optionsDesc.commandLine.add(verify);
   optionsDesc.commandLine.add(program);
   optionsDesc.commandLine.add(agreement);
   optionsDesc.commandLine.add(docs);
   optionsDesc.commandLine.add(www);
   optionsDesc.commandLine.add(session);
   optionsDesc.commandLine.add(r);
   optionsDesc.commandLine.add(limits);
   optionsDesc.commandLine.add(external);
   optionsDesc.commandLine.add(user);

   // define groups included in config-file processing
   optionsDesc.configFile.add(program);
   optionsDesc.configFile.add(agreement);
   optionsDesc.configFile.add(docs);
   optionsDesc.configFile.add(www);
   optionsDesc.configFile.add(session);
   optionsDesc.configFile.add(r);
   optionsDesc.configFile.add(limits);
   optionsDesc.configFile.add(external);
   optionsDesc.configFile.add(user);

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

   // compute program identity
   programIdentity_ = "rsession-" + userIdentity_;

   // provide special home path in temp directory if we are verifying
   if (verifyInstallation_)
   {
      Error error = FilePath(kVerifyInstallationHomeDir).ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         return ProgramStatus::exitFailure();
      }
      core::system::setenv("R_USER", kVerifyInstallationHomeDir);
   }

   // compute user home path
   FilePath userHomePath = core::system::userHomePath("R_USER|HOME");

   userHomePath_ = userHomePath.absolutePath();

   // compute user scratch path
   std::string scratchPathName;
   if (programMode_ == kSessionProgramModeDesktop)
      scratchPathName = "RStudio-Desktop";
   else
      scratchPathName = "RStudio";
   userScratchPath_ = core::system::userSettingsPath(
                                       userHomePath,
                                       scratchPathName).absolutePath();

   // session timeout seconds is always -1 in desktop mode
   if (programMode_ == kSessionProgramModeDesktop)
      timeoutMinutes_ = 0;

   // if we are in desktop mode and no agreement file path was
   // specified then default to gpl-standalone
   if ( (programMode_ == kSessionProgramModeDesktop) &&
        agreementFilePath_.empty())
   {
      agreementFilePath_ = "resources/agpl-3.0-standalone.html";
   }

   // convert relative paths by completing from the app resource path
   resolvePath(resourcePath, &rResourcesPath_);
   resolvePath(resourcePath, &agreementFilePath_);
   resolvePath(resourcePath, &wwwLocalPath_);
   resolvePath(resourcePath, &coreRSourcePath_);
   resolvePath(resourcePath, &modulesRSourcePath_);
   resolvePath(resourcePath, &sessionPackagesPath_);
   resolvePostbackPath(resourcePath, &rpostbackPath_);
   resolveRsPdfLatexPath(resourcePath, &rspdflatexPath_);
#ifdef _WIN32
   resolvePath(resourcePath, &consoleIoPath_);
   resolvePath(resourcePath, &gnudiffPath_);
   resolvePath(resourcePath, &gnugrepPath_);
   resolvePath(resourcePath, &msysSshPath_);
   resolvePath(resourcePath, &sumatraPath_);
#endif
   resolvePath(resourcePath, &hunspellDictionariesPath_);
   resolvePath(resourcePath, &mathjaxPath_);

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

   // initial working dir override
   initialWorkingDirOverride_ = core::system::getenv("RS_INITIAL_WD");
   core::system::unsetenv("RS_INITIAL_WD");

   // initial environment file override
   initialEnvironmentFileOverride_ = core::system::getenv("RS_INITIAL_ENV");
   core::system::unsetenv("RS_INITIAL_ENV");

   // initial project
   initialProjectPath_ = core::system::getenv("RS_INITIAL_PROJECT");
   core::system::unsetenv("RS_INITIAL_PROJECT");

   // limit rpc client uid
   limitRpcClientUid_ = -1;
   std::string limitUid = core::system::getenv(kRStudioLimitRpcClientUid);
   if (!limitUid.empty())
   {
      limitRpcClientUid_ = core::safe_convert::stringTo<int>(limitUid, -1);
      core::system::unsetenv(kRStudioLimitRpcClientUid);
   }

   // return status
   return status;
}
   
} // namespace session
