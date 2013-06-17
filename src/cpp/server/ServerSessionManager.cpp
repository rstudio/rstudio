/*
 * ServerSessionManager.cpp
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

#include "ServerSessionManager.hpp"

#include <vector>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/PosixUser.hpp>
#include <core/system/Environment.hpp>

#include <monitor/MonitorConstants.hpp>
#include <session/SessionConstants.hpp>

#include <server/ServerOptions.hpp>


#include <server/auth/ServerValidateUser.hpp>

#include "ServerREnvironment.hpp"


using namespace core;

namespace server {

namespace {

core::system::ProcessConfig sessionProcessConfig(
         const std::string& username,
         const core::system::Options& extraArgs = core::system::Options())
{
   // prepare command line arguments
   server::Options& options = server::options();
   core::system::Options args ;

   // check for options-specified config file and add to command
   // line if specified
   std::string rsessionConfigFile(options.rsessionConfigFile());
   if (!rsessionConfigFile.empty())
      args.push_back(std::make_pair("--config-file", rsessionConfigFile));

   // pass the user-identity
   args.push_back(std::make_pair("-" kUserIdentitySessionOptionShort,
                                 username));

   // allow session timeout to be overridden via environment variable
   std::string timeout = core::system::getenv("RSTUDIO_SESSION_TIMEOUT");
   if (!timeout.empty())
      args.push_back(std::make_pair("--" kTimeoutSessionOption, timeout));

   // pass our uid to instruct rsession to limit rpc clients to us and itself
   core::system::Options environment;
   uid_t uid = core::system::user::currentUserIdentity().userId;
   environment.push_back(std::make_pair(
                           kRStudioLimitRpcClientUid,
                           safe_convert::numberToString(uid)));

   // pass extra params
   std::copy(extraArgs.begin(), extraArgs.end(), std::back_inserter(args));

   // append R environment variables
   core::system::Options rEnvVars = r_environment::variables();
   environment.insert(environment.end(), rEnvVars.begin(), rEnvVars.end());

   // add monitor shared secret
   environment.push_back(std::make_pair(kMonitorSharedSecretEnvVar,
                                        options.monitorSharedSecret()));

   // build the config object and return it
   core::system::ProcessConfig config;
   config.args = args;
   config.environment = environment;
   config.stdStreamBehavior = core::system::StdStreamInherit;
   return config;
}

} // anonymous namespace

SessionManager& sessionManager()
{
   static SessionManager instance;
   return instance;
}

SessionManager::SessionManager()
{
   // set default session launcher
   sessionLaunchFunction_ = boost::bind(&SessionManager::launchAndTrackSession,
                                           this, _1);
}

Error SessionManager::launchSession(const std::string& username)
{
   using namespace boost::posix_time;

   // last ditch user validation -- an invalid user should very rarely
   // get to this point since we pre-emptively validate on client_init
   if (!server::auth::validateUser(username))
   {
      Error error = systemError(boost::system::errc::permission_denied,
                                ERROR_LOCATION);
      error.addProperty("username", username);
      return error;
   }

   LOCK_MUTEX(launchesMutex_)
   {
      // check whether we already have a launch pending
      LaunchMap::const_iterator pos = pendingLaunches_.find(username);
      if (pos != pendingLaunches_.end())
      {
         // if the launch is less than one minute old then return success
         if ( (pos->second + boost::posix_time::minutes(1))
               > microsec_clock::universal_time() )
         {
            return Success();
         }
         // otherwise erase it from pending launches and then
         // re-launch (immediately below)
         else
         {
            // very unexpected condition
            LOG_WARNING_MESSAGE("Very long session launch delay for "
                                "user " + username + " (aborting wait)");

            pendingLaunches_.erase(username);
         }
      }

      // record the launch
      pendingLaunches_[username] =  microsec_clock::universal_time();
   }
   END_LOCK_MUTEX

   // determine launch options
   r_util::SessionLaunchProfile profile;
   profile.username = username;
   profile.executablePath = server::options().rsessionPath();
   profile.config = sessionProcessConfig(username);

   // launch the session
   Error error = sessionLaunchFunction_(profile);
   if (error)
   {
      removePendingLaunch(username);
      return error;
   }

   return Success();
}

// default session launcher -- does the launch then tracks the pid
// for later reaping
Error SessionManager::launchAndTrackSession(
                           const core::r_util::SessionLaunchProfile& profile)
{
   // if we are root then assume the identity of the user
   using namespace core::system;
   std::string runAsUser = realUserIsRoot() ? profile.username : "";

   // launch the session
   PidType pid = 0;
   Error error = launchChildProcess(profile.executablePath,
                                    runAsUser,
                                    profile.config,
                                    &pid);
   if (error)
      return error;

   // track it for subsequent reaping
   processTracker_.addProcess(pid);

   // return success
   return Success();
}

void SessionManager::setSessionLaunchFunction(
                           const SessionLaunchFunction& launchFunction)
{
   sessionLaunchFunction_ = launchFunction;
}

void SessionManager::removePendingLaunch(const std::string& username)
{
   LOCK_MUTEX(launchesMutex_)
   {
      pendingLaunches_.erase(username);
   }
   END_LOCK_MUTEX
}

void SessionManager::notifySIGCHLD()
{
   processTracker_.notifySIGCHILD();
}

// helper function for verify-installation
Error launchSession(const std::string& username,
                    const core::system::Options& extraArgs,
                    PidType* pPid)
{
   // launch the session
   std::string rsessionPath = server::options().rsessionPath();
   std::string runAsUser = core::system::realUserIsRoot() ? username : "";
   core::system::ProcessConfig config = sessionProcessConfig(username,
                                                             extraArgs);

   *pPid = -1;
   return core::system::launchChildProcess(rsessionPath,
                                           runAsUser,
                                           config,
                                           pPid);
}


} // namespace server

