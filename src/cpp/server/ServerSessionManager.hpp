/*
 * ServerSessionManager.hpp
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

#ifndef SERVER_SESSION_MANAGER_HPP
#define SERVER_SESSION_MANAGER_HPP

#include <string>
#include <vector>
#include <map>

#include <boost/signals.hpp>

#include <core/Thread.hpp>
#include <core/system/PosixSystem.hpp>

namespace core {
   class Error;
}

namespace server {

// singleton
class SessionManager;
SessionManager& sessionManager();

// Session manager for launching managed sessions. This includes
// automatically waiting for other pending launches (rather than
// attempting to launch the same session twice) as well as reaping
// of session child processes
class SessionManager
{
private:
   // singleton
   SessionManager() {}
   friend SessionManager& sessionManager();

public:
   // launching
   core::Error launchSession(const std::string& username);
   void removePendingLaunch(const std::string& username);

   // notificatio that a SIGCHLD was received
   void notifySIGCHLD();

private:
   void addActivePid(PidType pid);
   void removeActivePid(PidType pid);
   std::vector<PidType> activePids();

private:
   // pending launches
   boost::mutex launchesMutex_;
   typedef std::map<std::string,boost::posix_time::ptime> LaunchMap;
   LaunchMap pendingLaunches_;

   // pids we have launched
   boost::mutex pidsMutex_;
   std::vector<PidType> activePids_;
};

// Lower-level global functions for launching sessions. These are used
// internally by the SessionManager as well as for verify-installation
//
core::Error launchSession(const std::string& username, PidType* pPid);

core::Error launchSession(const std::string& username,
                          const core::system::Options& extraArgs,
                          PidType* pPid);

} // namespace server

#endif // SERVER_SESSION_MANAGER_HPP

