/*
 * ServerOptions.cpp
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

#include <server/ServerOptions.hpp>

#include <core/ProgramStatus.hpp>
#include <core/ProgramOptions.hpp>
#include <core/FilePath.hpp>

#include <core/system/System.hpp>

#include <server/util/system/System.hpp>
#include <server/util/system/User.hpp>

#include "config.h"

#ifdef __APPLE__
#define SHARED_LIB_EXT "dylib"
#else
#define SHARED_LIB_EXT "so"
#endif

using namespace core ;

namespace server {

namespace {

const char * const kDefaultProgramUser = "rstudio-server";

void resolvePath(const FilePath& installPath, std::string* pPath)
{
   *pPath = installPath.complete(*pPath).absolutePath();
}

void reportMissingRPathError(const std::string& name,
                      const std::string& path,
                      const core::ErrorLocation& location)
{
   program_options::reportError(
                        "Error: R " + name + " (" + path + ") does not exist",
                        ERROR_LOCATION);
}

} // anonymous namespace

Options& options()
{
   static Options instance ;
   return instance ;
}

bool Options::resolveRPaths()
{
   // resolve r-home
   bool customRHome = false;
   if (!rHome_.empty())
   {
      customRHome = true;
   }
   else
   {
      // probe for one of the standard R home paths
      std::vector<FilePath> rHomePaths;
      rHomePaths.push_back(FilePath("/usr/local/lib64/R/bin"));
      rHomePaths.push_back(FilePath("/usr/local/lib/R/bin"));
      rHomePaths.push_back(FilePath("/usr/lib64/R/bin"));
      rHomePaths.push_back(FilePath("/usr/lib/R/bin"));
      for(std::vector<FilePath>::const_iterator it = rHomePaths.begin();
          it != rHomePaths.end(); ++it)
      {
         if (it->exists())
         {
            rHome_ = it->parent().absolutePath();
            break;
         }
      }

      // if we still didn't find a home path then use configured value
      if (rHome_.empty())
         rHome_ = CONFIG_R_HOME_PATH;
   }

   // verify r-home
   FilePath rHomePath(rHome_);
   if (!rHomePath.exists())
   {
      reportMissingRPathError("home path", rHome_, ERROR_LOCATION);
      return false;
   }

   // if there was no explicit doc dir then complete off of detected home
   if (rDocDir_.empty())  
      rDocDir_ = rHomePath.complete("doc").absolutePath();

   // if that doesn't exist then last ditch is configured path
   // (note this is necessary for debian r-base since it locates
   // R_DOC_DIR in /usr/share)
   if (!FilePath(rDocDir_).exists())
      rDocDir_ = CONFIG_R_DOC_PATH;

   // check for doc path existing
   if (!FilePath(rDocDir_).exists())
   {
      reportMissingRPathError("doc dir", rDocDir_, ERROR_LOCATION);
      return false;
   }

   // resolve and verify rlibdir
   FilePath rLibDirPath = rHomePath.complete("lib");
   rLibDir_ = rLibDirPath.absolutePath();
   if (!rLibDirPath.exists())
   {
      reportMissingRPathError("lib dir", rLibDir_, ERROR_LOCATION);
      return false;
   }

   // verify that we have libR.so
   FilePath rLibRPath = rLibDirPath.complete("libR." SHARED_LIB_EXT);
   if (!rLibRPath.exists())
   {
      program_options::reportError("Error: libR.so not found in R lib path (" +
                                   rLibDir_ + "). Was R built with " +
                                   "--enable-R-shlib?",
                                   ERROR_LOCATION);
      return false;
   }

   return true;
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
         "run program as daemon");
   
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

   // r
   options_description r("r");
   r.add_options()
      ("r-home",
         value<std::string>(&rHome_)->default_value(""),
         "path to R home directory")
      ("r-doc-dir",
         value<std::string>(&rDocDir_)->default_value(""),
         "path to R docs directory ");

   // rsession
   options_description rsession("rsession");
   rsession.add_options()
      ("rsession-path", 
         value<std::string>(&rsessionPath_)->default_value("bin/rsession"),
         "path to rsession executable")
      ("rldpath-path",
         value<std::string>(&rldpathPath_)->default_value("bin/r-ldpath"),
         "path to r-ldpath script")
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
   
   // auth - auth options
   options_description auth("auth");
   auth.add_options()
      ("auth-validate-users",
        value<bool>(&authValidateUsers_)->default_value(true),
        "validate that authenticated users exist on the target system")
      ("auth-required-user-group",
        value<std::string>(&authRequiredUserGroup_)->default_value(""),
        "limit to users belonging to the specified group");

   // define program options
   FilePath defaultConfigPath("/etc/rstudio/rserver.conf");
   std::string configFile = defaultConfigPath.exists() ?
                                 defaultConfigPath.absolutePath() : "";
   program_options::OptionsDescription optionsDesc("rserver", configFile);
   optionsDesc.commandLine.add(server).add(www).add(r).add(rsession).add(auth);
   optionsDesc.configFile.add(server).add(www).add(r).add(rsession).add(auth);
 
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
      if (!util::system::realUserIsRoot())
      {
         serverUser_ = "";
      }
      // if there is a program user specified and it doesn't exist....
      else if (!util::system::user::exists(serverUser_))
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

   // resolve R paths
   if (!resolveRPaths())
      return ProgramStatus::exitFailure();

   // convert relative paths by completing from the system installation
   // path (this allows us to be relocatable)
   resolvePath(installPath, &wwwLocalPath_);
   resolvePath(installPath, &rsessionPath_);
   resolvePath(installPath, &rldpathPath_);
   resolvePath(installPath, &rsessionConfigFile_);

   // return status
   return status;
}

} // namespace server
