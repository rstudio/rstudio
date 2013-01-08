/*
 * ServerOptions.cpp
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

#include <server/ServerOptions.hpp>

#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>
#include <core/FilePath.hpp>

#include <core/system/PosixUser.hpp>
#include <core/system/PosixSystem.hpp>

#include "ServerAppArmor.hpp"

using namespace core ;

namespace server {

namespace {

const char * const kDefaultProgramUser = "rstudio-server";

void resolvePath(const FilePath& installPath, std::string* pPath)
{
   if (!pPath->empty())
      *pPath = installPath.complete(*pPath).absolutePath();
}


} // anonymous namespace

Options& options()
{
   static Options instance ;
   return instance ;
}

ProgramStatus Options::read(int argc, char * const argv[])
{
   using namespace boost::program_options ;

   // compute install path
   FilePath installPath;
   Error error = core::system::installPath("..", argc, argv, &installPath);
   if (error)
   {
      LOG_ERROR_MESSAGE("Unable to determine install path: "+error.summary());
      return ProgramStatus::exitFailure();
   }

   // verify installation flag
   options_description verify("verify");
   verify.add_options()
     ("verify-installation",
     value<bool>(&verifyInstallation_)->default_value(false),
     "verify the current installation");

   // special program offline option (based on file existence at 
   // startup for easy bash script enable/disable of offline state)
   serverOffline_ = FilePath("/etc/rstudio/offline").exists();

   // program - name and execution
   options_description server("server");
   server.add_options()
      ("server-working-dir",
         value<std::string>(&serverWorkingDir_)->default_value("/"),
         "program working directory")
      ("server-user",
         value<std::string>(&serverUser_)->default_value(kDefaultProgramUser),
         "program user")
      ("server-daemonize",
         value<bool>(&serverDaemonize_)->default_value(1),
         "run program as daemon")
      ("server-app-armor-enabled",
         value<bool>(&serverAppArmorEnabled_)->default_value(1),
         "is app armor enabled for this session");

   // www - web server options
   options_description www("www") ;
   www.add_options()
      ("www-address",
         value<std::string>(&wwwAddress_)->default_value("0.0.0.0"),
         "server address")
      ("www-port",
         value<std::string>(&wwwPort_)->default_value("8787"),
         "port to listen on")
      ("www-local-path",
         value<std::string>(&wwwLocalPath_)->default_value("www"),
         "www files path")
      ("www-thread-pool-size",
         value<int>(&wwwThreadPoolSize_)->default_value(2),
         "thread pool size");

   // rsession
   options_description rsession("rsession");
   rsession.add_options()
      ("rsession-which-r",
         value<std::string>(&rsessionWhichR_)->default_value(""),
         "path to main R program (e.g. /usr/bin/R)")
      ("rsession-path", 
         value<std::string>(&rsessionPath_)->default_value("bin/rsession"),
         "path to rsession executable")
      ("rldpath-path",
         value<std::string>(&rldpathPath_)->default_value("bin/r-ldpath"),
         "path to r-ldpath script")
      ("rsession-ld-library-path",
         value<std::string>(&rsessionLdLibraryPath_)->default_value(""),
         "default LD_LIBRARY_PATH for rsession")
      ("rsession-config-file",
         value<std::string>(&rsessionConfigFile_)->default_value(""),
         "path to rsession config file")
      ("rsession-memory-limit-mb",
         value<int>(&rsessionMemoryLimitMb_)->default_value(0),
         "rsession memory limit (mb)")
      ("rsession-stack-limit-mb",
         value<int>(&rsessionStackLimitMb_)->default_value(0),
         "rsession stack limit (mb)")
      ("rsession-process-limit",
         value<int>(&rsessionUserProcessLimit_)->default_value(0),
         "rsession user process limit");
   
   // still read depracated options (so we don't break config files)
   bool deprecatedAuthPamRequiresPriv;
   options_description auth("auth");
   auth.add_options()
      ("auth-validate-users",
        value<bool>(&authValidateUsers_)->default_value(true),
        "validate that authenticated users exist on the target system")
      ("auth-required-user-group",
        value<std::string>(&authRequiredUserGroup_)->default_value(""),
        "limit to users belonging to the specified group")
      ("auth-pam-helper-path",
        value<std::string>(&authPamHelperPath_)->default_value("bin/rserver-pam"),
       "path to PAM helper binary")
      ("auth-pam-requires-priv",
        value<bool>(&deprecatedAuthPamRequiresPriv)->default_value(true),
        "deprecated: will always be true");

   // define program options
   FilePath defaultConfigPath("/etc/rstudio/rserver.conf");
   std::string configFile = defaultConfigPath.exists() ?
                                 defaultConfigPath.absolutePath() : "";
   program_options::OptionsDescription optionsDesc("rserver", configFile);
   optionsDesc.commandLine.add(verify).add(server).add(www).add(rsession).add(auth);
   optionsDesc.configFile.add(server).add(www).add(rsession).add(auth);
 
   // read options
   ProgramStatus status = core::program_options::read(optionsDesc, argc, argv);
   if (status.exit())
      return status;
    
   // if specified, confirm that the program user exists. however, if the
   // program user is the default and it doesn't exist then allow that to pass,
   // this just means that the user did a simple make install and hasn't setup
   // an rserver user yet. in this case the program will run as root
   if (!serverUser_.empty())
   {
      // if we aren't running as root then forget the programUser
      if (!core::system::realUserIsRoot())
      {
         serverUser_ = "";
      }
      // if there is a program user specified and it doesn't exist....
      else if (!core::system::user::exists(serverUser_))
      {
         if (serverUser_ == kDefaultProgramUser)
         {
            // administrator hasn't created an rserver system account yet
            // so run as root
            serverUser_ = "";
         }
         else
         {
            LOG_ERROR_MESSAGE("Server user "+ serverUser_ +" does not exist");
            return ProgramStatus::exitFailure();
         }
      }
   }

   // if app armor is enabled do a further check to see whether
   // the profile exists. if it doesn't then disable it
   if (serverAppArmorEnabled_)
   {
      if (!FilePath("/etc/apparmor.d/rstudio-server").exists())
         serverAppArmorEnabled_ = false;
   }

   // convert relative paths by completing from the system installation
   // path (this allows us to be relocatable)
   resolvePath(installPath, &wwwLocalPath_);
   resolvePath(installPath, &authPamHelperPath_);
   resolvePath(installPath, &rsessionPath_);
   resolvePath(installPath, &rldpathPath_);
   resolvePath(installPath, &rsessionConfigFile_);

   // return status
   return status;
}

} // namespace server
