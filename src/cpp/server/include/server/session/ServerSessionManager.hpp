/*
 * ServerSessionManager.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

#include <boost/asio/io_context.hpp>

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
   core::Error launchSession(boost::asio::io_context& ioContext,
                             const core::r_util::SessionContext& context,
                             const core::http::Request& request,
                             bool &launched,
                             core::system::Options environment,
                             const core::http::ResponseHandler& onLaunch = core::http::ResponseHandler(),
                             const core::http::ErrorHandler& onError = core::http::ErrorHandler(),
                             const std::string& openFile = "");
   void removePendingLaunch(const core::r_util::SessionContext& context, const bool success = true, const std::string& errorMsg = std::string());

   void removePendingSessionLaunch(const std::string& username, const std::string& sessionId, const bool success = true, const std::string& errorMsg = std::string());

   // associate the launched process with its pending launch so a launch whose
   // process dies before a client connection can be detected and cleared
   // (rather than suppressing relaunch attempts until it ages out)
   void notePendingLaunchPid(const core::r_util::SessionContext& context, PidType pid);

   // remove the pending launch if it still belongs to the given (now exited)
   // process; a no-op when the context has no pending launch or the pending
   // launch was recorded for a different process
   void removePendingLaunchForPid(const core::r_util::SessionContext& context, PidType pid, int exitStatus);

   // set a custom session launcher
   typedef boost::function<core::Error(
                           boost::asio::io_context&,
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
                        boost::asio::io_context&,
                        const core::r_util::SessionLaunchProfile& profile);

   int cleanStalePendingLaunches();

private:
   // pending launches; pid is -1 until the launched process is known (custom
   // session launchers may never report one)
   struct PendingLaunch
   {
      boost::posix_time::ptime launchTime;
      PidType pid = -1;
   };

   boost::mutex launchesMutex_;
   typedef std::map<core::r_util::SessionContext, PendingLaunch> LaunchMap;
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

