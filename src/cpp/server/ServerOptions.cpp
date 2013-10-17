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

#include <monitor/MonitorConstants.hpp>

#include "ServerAppArmor.hpp"

using namespace core ;

namespace server {

namespace {

const char * const kDefaultProgramUser = "rstudio-server";

struct Deprecated
{
   Deprecated()
      : memoryLimitMb(0),
        stackLimitMb(0),
        userProcessLimit(0),
        authPamRequiresPriv(true)
   {
   }

   int memoryLimitMb;
   int stackLimitMb;
   int userProcessLimit;
   bool authPamRequiresPriv;
};

void reportDeprecationWarning(const std::string& option, std::ostream& os)
{
   os << "The option '" << option << "' is deprecated and will be discarded."
      << std::endl;
}

void reportDeprecationWarnings(const Deprecated& userOptions,
                               std::ostream& os)
{
   Deprecated defaultOptions;

   if (userOptions.memoryLimitMb != defaultOptions.memoryLimitMb)
      reportDeprecationWarning("rsession-memory-limit-mb", os);

   if (userOptions.stackLimitMb != defaultOptions.stackLimitMb)
      reportDeprecationWarning("rsession-stack-limit-mb", os);

   if (userOptions.userProcessLimit != defaultOptions.userProcessLimit)
      reportDeprecationWarning("rsession-process-limit", os);

   if (userOptions.authPamRequiresPriv != defaultOptions.authPamRequiresPriv)
      reportDeprecationWarning("auth-pam-requires-priv", os);
}

} // anonymous namespace

Options& options()
{
   static Options instance ;
   return instance ;
}


ProgramStatus Options::read(int argc,
                            char * const argv[],
                            std::ostream& osWarnings)
{
   using namespace boost::program_options ;

   // compute install path
   Error error = core::system::installPath("..", argv[0], &installPath_);
   if (error)
   {
      LOG_ERROR_MESSAGE("Unable to determine install path: "+error.summary());
      return ProgramStatus::exitFailure();
   }

   // compute the resource and binary paths
   FilePath resourcePath = installPath_;
   FilePath binaryPath = installPath_.childPath("bin");

   // detect running in OSX bundle and tweak paths
#ifdef __APPLE__
   if (installPath_.complete("Info.plist").exists())
   {
      resourcePath = installPath_.complete("Resources");
      binaryPath = installPath_.complete("MacOS");
   }
#endif

   // verify installation flag
   options_description verify("verify");
   verify.add_options()
     ("verify-installation",
     value<bool>(&verifyInstallation_)->default_value(false),
     "verify the current installation");

   // special program offline option (based on file existence at 
   // startup for easy bash script enable/disable of offline state)
   serverOffline_ = FilePath("/var/lib/rstudio-server/offline").exists();

   // generate monitor shared secret
   monitorSharedSecret_ = core::system::generateUuid();

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
         value<bool>(&serverDaemonize_)->default_value(
                                      core::system::effectiveUserIsRoot()),
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
         value<std::string>(&wwwPort_)->default_value(""),
         "port to listen on")
      ("www-local-path",
         value<std::string>(&wwwLocalPath_)->default_value("www"),
         "www files path")
      ("www-symbol-maps-path",
         value<std::string>(&wwwSymbolMapsPath_)->default_value(
                                                      "www-symbolmaps"),
        "www symbol maps path")
      ("www-use-emulated-stack",
       value<bool>(&wwwUseEmulatedStack_)->default_value(false),
       "use gwt emulated stack")
      ("www-thread-pool-size",
         value<int>(&wwwThreadPoolSize_)->default_value(2),
         "thread pool size")
      ("www-proxy-localhost",
         value<bool>(&wwwProxyLocalhost_)->default_value(true),
         "proxy requests to localhost ports over main server port");

   // rsession
   Deprecated dep;
   options_description rsession("rsession");
   rsession.add_options()
      ("rsession-which-r",
         value<std::string>(&rsessionWhichR_)->default_value(""),
         "path to main R program (e.g. /usr/bin/R)")
      ("rsession-path", 
         value<std::string>(&rsessionPath_)->default_value("rsession"),
         "path to rsession executable")
      ("rldpath-path",
         value<std::string>(&rldpathPath_)->default_value("r-ldpath"),
         "path to r-ldpath script")
      ("rsession-ld-library-path",
         value<std::string>(&rsessionLdLibraryPath_)->default_value(""),
         "default LD_LIBRARY_PATH for rsession")
      ("rsession-config-file",
         value<std::string>(&rsessionConfigFile_)->default_value(""),
         "path to rsession config file")
      ("rsession-memory-limit-mb",
         value<int>(&dep.memoryLimitMb)->default_value(dep.memoryLimitMb),
         "rsession memory limit (mb) - DEPRECATED")
      ("rsession-stack-limit-mb",
         value<int>(&dep.stackLimitMb)->default_value(dep.stackLimitMb),
         "rsession stack limit (mb) - DEPRECATED")
      ("rsession-process-limit",
         value<int>(&dep.userProcessLimit)->default_value(dep.userProcessLimit),
         "rsession user process limit - DEPRECATED");
   
   // still read depracated options (so we don't break config files)
   options_description auth("auth");
   auth.add_options()
      ("auth-none",
        value<bool>(&authNone_)->default_value(
                                 !core::system::effectiveUserIsRoot()),
        "don't do any authentication")
      ("auth-validate-users",
        value<bool>(&authValidateUsers_)->default_value(
                                 core::system::effectiveUserIsRoot()),
        "validate that authenticated users exist on the target system")
      ("auth-required-user-group",
        value<std::string>(&authRequiredUserGroup_)->default_value(""),
        "limit to users belonging to the specified group")
      ("auth-pam-helper-path",
        value<std::string>(&authPamHelperPath_)->default_value("rserver-pam"),
       "path to PAM helper binary")
      ("auth-pam-requires-priv",
        value<bool>(&dep.authPamRequiresPriv)->default_value(
                                                   dep.authPamRequiresPriv),
        "deprecated: will always be true");

   options_description monitor("monitor");
   monitor.add_options()
      (kMonitorIntervalSeconds,
       value<int>(&monitorIntervalSeconds_)->default_value(300),
       "monitoring interval");

   // define program options
   FilePath defaultConfigPath("/etc/rstudio/rserver.conf");
   std::string configFile = defaultConfigPath.exists() ?
                                 defaultConfigPath.absolutePath() : "";
   program_options::OptionsDescription optionsDesc("rserver", configFile);

   // overlay hook
   addOverlayOptions(&server, &www, &rsession, &auth, &monitor);

   optionsDesc.commandLine.add(verify).add(server).add(www).add(rsession).add(auth).add(monitor);
   optionsDesc.configFile.add(server).add(www).add(rsession).add(auth).add(monitor);
 
   // read options
   bool help = false;
   ProgramStatus status = core::program_options::read(optionsDesc,
                                                      argc,
                                                      argv,
                                                      &help);

   // terminate if this was a help request
   if (help)
      return ProgramStatus::exitSuccess();

   // report deprecation warnings
   reportDeprecationWarnings(dep, osWarnings);

   // call overlay hooks
   resolveOverlayOptions();
   std::string errMsg;
   if (!validateOverlayOptions(&errMsg, osWarnings))
   {
      program_options::reportError(errMsg, ERROR_LOCATION);
      return ProgramStatus::exitFailure();
   }

   // exit if the call to read indicated we should -- note we don't do this
   // immediately so that we can allow overlay validation to occur (otherwise
   // a --test-config wouldn't test overlay options)
   if (status.exit())
      return status;
    
   // rationalize auth settings
   if (authNone_)
      authValidateUsers_ = false;

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
            // so we'll end up running as root
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
   resolvePath(resourcePath, &wwwLocalPath_);
   resolvePath(resourcePath, &wwwSymbolMapsPath_);
   resolvePath(binaryPath, &authPamHelperPath_);
   resolvePath(binaryPath, &rsessionPath_);
   resolvePath(binaryPath, &rldpathPath_);
   resolvePath(resourcePath, &rsessionConfigFile_);

   // return status
   return status;
}

void Options::resolvePath(const FilePath& basePath,
                          std::string* pPath) const
{
   if (!pPath->empty())
      *pPath = basePath.complete(*pPath).absolutePath();
}

} // namespace server
