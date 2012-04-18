/*
 * ServerSessionManager.cpp
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

#include "ServerSessionManager.hpp"

#include <sys/wait.h>

#include <vector>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <core/system/PosixUser.hpp>

#include <session/SessionConstants.hpp>

#include <server/ServerOptions.hpp>

#include <server/util/system/User.hpp>

#include <server/auth/ServerValidateUser.hpp>

#include "ServerREnvironment.hpp"


using namespace core;

namespace server {

SessionManager& sessionManager()
{
   static SessionManager instance;
   return instance;
}

Error SessionManager::launchSession(const std::string& username)
{
   using namespace boost::posix_time;

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

   // launch the session
   PidType pid;
   Error error = server::launchSession(username, &pid);
   if (error)
   {
      removePendingLaunch(username);
      return error;
   }
   else
   {
      // add it to our active pids
      addActivePid(pid);

      // return success
      return Success();
   }
}

void SessionManager::removePendingLaunch(const std::string& username)
{
   LOCK_MUTEX(launchesMutex_)
   {
      pendingLaunches_.erase(username);
   }
   END_LOCK_MUTEX
}

namespace {

// wraper for waitPid which tries again for EINTR
int waitPid(PidType pid, int* pStatus)
{
   for (;;)
   {
      int result = ::waitpid(pid, pStatus, WNOHANG);
      if (result == -1 && errno == EINTR)
         continue;
      return result;
   }
}

} // anonymous namespace

void SessionManager::notifySIGCHLD()
{
   // We make a copy of hte active pids so that we can do the reaping
   // outside of the pidsMutex_. This is an extra conservative precaution
   // in case there is ever an issue with waitpid blocking.
   std::vector<PidType> pids = activePids();

   // reap all that we can
   BOOST_FOREACH(PidType pid, pids)
   {
      // non-blocking wait for the child
      int status;
      int result = waitPid(pid, &status);

      // reaped the child
      if (result == pid)
      {
         // confirm this was a real exit
         bool exited = false;
         if (WIFEXITED(status))
         {
            exited = true;
            status = WEXITSTATUS(status);
         }
         else if (WIFSIGNALED(status))
         {
            exited = true;
         }

         // if it was a real exit (as opposed to a SIGSTOP or SIGCONT)
         // then remove the pid from our table and fire the event
         if (exited)
         {
            // all done with this pid
            removeActivePid(pid);
         }
         else
         {
            boost::format fmt("Received SIGCHLD when child did not "
                              "actually exit (pid=%1%, status=%2%");
            LOG_WARNING_MESSAGE(boost::str(fmt % pid % status));
         }
      }
      // error occured
      else if (result == -1)
      {
         Error error = systemError(errno, ERROR_LOCATION);
         error.addProperty("pid", pid);
         LOG_ERROR(error);
      }
   }
}

void SessionManager::addActivePid(PidType pid)
{
   LOCK_MUTEX(pidsMutex_)
   {
      activePids_.push_back(pid);
   }
   END_LOCK_MUTEX
}

void SessionManager::removeActivePid(PidType pid)
{
   LOCK_MUTEX(pidsMutex_)
   {
      activePids_.erase(std::remove(activePids_.begin(),
                                    activePids_.end(),
                                    pid),
                        activePids_.end());
   }
   END_LOCK_MUTEX
}

std::vector<PidType> SessionManager::activePids()
{
   LOCK_MUTEX(pidsMutex_)
   {
      return activePids_;
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return std::vector<PidType>();
}


Error launchSession(const std::string& username,
                    const core::system::Options& extraArgs,
                    PidType* pPid)
{
   // last ditch user validation -- an invalid user should very rarely
   // get to this point since we pre-emptively validate on client_init
   if (!server::auth::validateUser(username))
   {
      Error error = systemError(boost::system::errc::permission_denied,
                                ERROR_LOCATION);
      error.addProperty("username", username);
      return error;
   }

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

   // pass our uid to instruct rsession to limit rpc clients to us and itself
   core::system::Options environment;
   uid_t uid = core::system::user::currentUserIdentity().userId;
   environment.push_back(std::make_pair(
                           kRStudioLimitRpcClientUid,
                           boost::lexical_cast<std::string>(uid)));

   // pass extra params
   std::copy(extraArgs.begin(), extraArgs.end(), std::back_inserter(args));

   // append R environment variables
   core::system::Options rEnvVars = r_environment::variables();
   environment.insert(environment.end(), rEnvVars.begin(), rEnvVars.end());

   // launch the session
   *pPid = -1;
   std::string runAsUser = util::system::realUserIsRoot() ? username : "";
   util::system::ProcessConfig config;
   config.args = args;
   config.environment = environment;
   config.stdStreamBehavior = util::system::StdStreamInherit;
   config.memoryLimitBytes = static_cast<RLimitType>(
                               options.rsessionMemoryLimitMb() * 1024L * 1024L);
   config.stackLimitBytes = static_cast<RLimitType>(
                               options.rsessionStackLimitMb() * 1024L * 1024L);
   config.userProcessesLimit = static_cast<RLimitType>(
                               options.rsessionUserProcessLimit());
   return util::system::launchChildProcess(options.rsessionPath(),
                                           runAsUser,
                                           config,
                                           pPid) ;
}

Error launchSession(const std::string& username, PidType* pPid)
{
   return launchSession(username, core::system::Options(), pPid);
}



} // namespace server

