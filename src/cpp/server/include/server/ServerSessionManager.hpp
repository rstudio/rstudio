/*
 * ServerSessionManager.hpp
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

#ifndef SERVER_SESSION_MANAGER_HPP
#define SERVER_SESSION_MANAGER_HPP

#include <string>
#include <vector>
#include <map>

#include <boost/asio/io_service.hpp>

#include <core/BoostSignals.hpp>
#include <core/http/AsyncClient.hpp>
#include <core/http/Request.hpp>
#include <core/Thread.hpp>

#include <core/system/PosixSystem.hpp>
#include <core/system/PosixChildProcessTracker.hpp>

#include <core/r_util/RSessionContext.hpp>
#include <core/r_util/RSessionLaunchProfile.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
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
   SessionManager();
   friend SessionManager& sessionManager();

public:
   // launching
   core::Error launchSession(boost::asio::io_service& ioService,
                             const core::r_util::SessionContext& context,
                             const core::http::Request& request,
                             const core::http::ResponseHandler& onLaunch = core::http::ResponseHandler(),
                             const core::http::ErrorHandler& onError = core::http::ErrorHandler());
   void removePendingLaunch(const core::r_util::SessionContext& context);

   // set a custom session launcher
   typedef boost::function<core::Error(
                           boost::asio::io_service&,
                           const core::r_util::SessionLaunchProfile&,
                           const core::http::Request&,
                           const core::http::ResponseHandler& onLaunch,
                           const core::http::ErrorHandler& onError)>
                                                  SessionLaunchFunction;
   void setSessionLaunchFunction(const SessionLaunchFunction& launchFunction);

   // set a launch profile filter
   typedef boost::function<void(
                           core::r_util::SessionLaunchProfile*)>
                                                  SessionLaunchProfileFilter;
   void addSessionLaunchProfileFilter(const SessionLaunchProfileFilter& filter);

   // get current session launch profile filters
   const std::vector<SessionLaunchProfileFilter>& getSessionLaunchProfileFilters() { return sessionLaunchProfileFilters_; }

   // notification that a SIGCHLD was received
   void notifySIGCHLD();

private:
   // default session launcher -- runs the process then uses the
   // ChildProcessTracker to track it's pid for later reaping
   core::Error launchAndTrackSession(
                        boost::asio::io_service&,
                        const core::r_util::SessionLaunchProfile& profile);

private:
   // pending launches
   boost::mutex launchesMutex_;
   typedef std::map<core::r_util::SessionContext,
                    boost::posix_time::ptime> LaunchMap;
   LaunchMap pendingLaunches_;

   // session launch function
   SessionLaunchFunction sessionLaunchFunction_;

   // session launch profile filters
   std::vector<SessionLaunchProfileFilter> sessionLaunchProfileFilters_;

   // child process tracker
   core::system::ChildProcessTracker processTracker_;
};

// set a process config filter
void setProcessConfigFilter(const core::system::ProcessConfigFilter& filter);

// Lower-level global functions for launching sessions. These are used
// internally by the SessionManager as well as for verify-installation
core::Error launchSession(const core::r_util::SessionContext& context,
                          const core::system::Options& extraArgs,
                          PidType* pPid);

core::r_util::SessionLaunchProfile createSessionLaunchProfile(const core::r_util::SessionContext& context,
                                                              const core::system::Options& extraArgs);


} // namespace server
} // namespace rstudio

#endif // SERVER_SESSION_MANAGER_HPP

