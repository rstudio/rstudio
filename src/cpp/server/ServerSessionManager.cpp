/*
 * ServerSessionManager.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <server/ServerSessionManager.hpp>

#include <vector>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/PosixUser.hpp>
#include <core/system/Environment.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <monitor/MonitorClient.hpp>
#include <session/SessionConstants.hpp>

#include <server/ServerOptions.hpp>

#include <server/ServerErrorCategory.hpp>

#include <server/auth/ServerValidateUser.hpp>

#include "ServerREnvironment.hpp"


using namespace rstudio::core;

namespace rstudio {
namespace server {

namespace {

static std::string s_launcherToken;

core::system::ProcessConfig sessionProcessConfig(
         r_util::SessionContext context,
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
                                 context.username));

   // pass the project if specified
   if (!context.scope.project().empty())
   {
      args.push_back(std::make_pair("-" kProjectSessionOptionShort,
                                    context.scope.project()));
   }

   // pass the scope id if specified
   if (!context.scope.id().empty())
   {
      args.push_back(std::make_pair("-" kScopeSessionOptionShort,
                                    context.scope.id()));
   }

   // create launch token if we haven't already
   if (s_launcherToken.empty())
      s_launcherToken = core::system::generateShortenedUuid();
   args.push_back(std::make_pair("--launcher-token", s_launcherToken));

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

   // set session scope project if we have one
   if (!context.scope.project().empty())
   {
      environment.push_back(std::make_pair(
                              kRStudioSessionScopeProject,
                              context.scope.project()));
   }

   // set session scope id if we have one
   if (!context.scope.id().empty())
   {
      environment.push_back(std::make_pair(
                              kRStudioSessionScopeId,
                              context.scope.id()));
   }

   // log to stderr if we aren't daemonized
   if (!options.serverDaemonize())
      args.push_back(std::make_pair("--log-stderr", "1"));

   // pass extra params
   std::copy(extraArgs.begin(), extraArgs.end(), std::back_inserter(args));

   // append R environment variables
   r_util::RVersion rVersion = r_environment::rVersion();
   core::system::Options rEnvVars = rVersion.environment();
   environment.insert(environment.end(), rEnvVars.begin(), rEnvVars.end());
   
   // mark this as the system default R version
   core::system::setenv(&environment,
                        kRStudioDefaultRVersion,
                        rVersion.number());
   core::system::setenv(&environment,
                        kRStudioDefaultRVersionHome,
                        rVersion.homeDir().absolutePath());

   // forward the auth options
   core::system::setenv(&environment,
                        kRStudioRequiredUserGroup,
                        options.authRequiredUserGroup());
   core::system::setenv(&environment,
                        kRStudioMinimumUserId,
                        safe_convert::numberToString(
                                 options.authMinimumUserId()));
   
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

void onProcessExit(const std::string& username, PidType pid)
{
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
                                           this, _1, _2);
}

Error SessionManager::launchSession(boost::asio::io_service& ioService,
      const r_util::SessionContext& context)
{
   using namespace boost::posix_time;
   LOCK_MUTEX(launchesMutex_)
   {
      // check whether we already have a launch pending
      LaunchMap::const_iterator pos = pendingLaunches_.find(context);
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
                                "user " + context.username +" (aborting wait)");

            pendingLaunches_.erase(context);
         }
      }

      // record the launch
      pendingLaunches_[context] =  microsec_clock::universal_time();
   }
   END_LOCK_MUTEX

   // determine launch options
   r_util::SessionLaunchProfile profile;
   profile.context = context;
   profile.executablePath = server::options().rsessionPath();
   profile.config = sessionProcessConfig(context);

   // pass the profile to any filters we have
   BOOST_FOREACH(SessionLaunchProfileFilter f, sessionLaunchProfileFilters_)
   {
      f(&profile);
   }

   // launch the session
   Error error = sessionLaunchFunction_(ioService, profile);
   if (error)
   {
      removePendingLaunch(context);
      return error;
   }

   return Success();
}

namespace {

core::system::ProcessConfigFilter s_processConfigFilter;

} // anonymous namespace


void setProcessConfigFilter(const core::system::ProcessConfigFilter& filter)
{
   s_processConfigFilter = filter;
}

// default session launcher -- does the launch then tracks the pid
// for later reaping
Error SessionManager::launchAndTrackSession(
                           boost::asio::io_service&,
                           const core::r_util::SessionLaunchProfile& profile)
{
   // if we are root then assume the identity of the user
   using namespace rstudio::core::system;
   std::string runAsUser = realUserIsRoot() ? profile.context.username : "";

   // launch the session
   PidType pid = 0;
   Error error = launchChildProcess(profile.executablePath,
                                    runAsUser,
                                    profile.config,
                                    s_processConfigFilter,
                                    &pid);
   if (error)
      return error;

   // track it for subsequent reaping
   processTracker_.addProcess(pid, boost::bind(onProcessExit,
                                               profile.context.username,
                                               pid));

   // return success
   return Success();
}

void SessionManager::setSessionLaunchFunction(
                           const SessionLaunchFunction& launchFunction)
{
   sessionLaunchFunction_ = launchFunction;
}

void SessionManager::addSessionLaunchProfileFilter(
                              const SessionLaunchProfileFilter& filter)
{
   sessionLaunchProfileFilters_.push_back(filter);
}

void SessionManager::removePendingLaunch(const r_util::SessionContext& context)
{
   LOCK_MUTEX(launchesMutex_)
   {
      pendingLaunches_.erase(context);
   }
   END_LOCK_MUTEX
}

void SessionManager::notifySIGCHLD()
{
   processTracker_.notifySIGCHILD();
}

// helper function for verify-installation
Error launchSession(const r_util::SessionContext& context,
                    const core::system::Options& extraArgs,
                    PidType* pPid)
{
   // launch the session
   std::string username = context.username;
   std::string rsessionPath = server::options().rsessionPath();
   std::string runAsUser = core::system::realUserIsRoot() ? username : "";
   core::system::ProcessConfig config = sessionProcessConfig(context,
                                                             extraArgs);

   *pPid = -1;
   return core::system::launchChildProcess(rsessionPath,
                                           runAsUser,
                                           config,
                                           core::system::ProcessConfigFilter(),
                                           pPid);
}


} // namespace server
} // namespace rstudio

