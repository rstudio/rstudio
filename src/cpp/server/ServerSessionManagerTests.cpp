/*
 * ServerSessionManagerTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <server/session/ServerSessionManager.hpp>

#include <limits>
#include <stdexcept>

#include <unistd.h>

#include <boost/asio/io_context.hpp>

#include <core/http/Request.hpp>
#include <core/json/JsonRpc.hpp>

#include <gtest/gtest.h>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace tests {

namespace {

// installs a launch function that only counts invocations, so launchSession
// exercises the real pending-launch bookkeeping without spawning processes
int s_launchCount = 0;

Error countingLaunchFunction(boost::asio::io_context&,
                             const r_util::SessionLaunchProfile&,
                             const json::JsonRpcRequest&,
                             const http::Request&,
                             const http::ResponseHandler&,
                             const http::ErrorHandler&)
{
   s_launchCount++;
   return Success();
}

// while it runs, other request threads report outcomes for the context: an
// error (e.g. an EOF from the exiting session a restart replaces) and a
// success (a last response from that same session)
Error requestsEndDuringLaunchFunction(boost::asio::io_context& ioContext,
                                      const r_util::SessionLaunchProfile& profile,
                                      const json::JsonRpcRequest& jsonRequest,
                                      const http::Request& request,
                                      const http::ResponseHandler& onLaunch,
                                      const http::ErrorHandler& onError)
{
   sessionManager().removePendingLaunch(profile.context, false, "request error");
   sessionManager().removePendingLaunch(profile.context);
   return countingLaunchFunction(ioContext, profile, jsonRequest, request, onLaunch, onError);
}

// a launcher session RPC (Workbench) reports outcomes by username and
// session id rather than by context; this one ends during the launch
Error sessionRpcEndsDuringLaunchFunction(boost::asio::io_context& ioContext,
                                         const r_util::SessionLaunchProfile& profile,
                                         const json::JsonRpcRequest& jsonRequest,
                                         const http::Request& request,
                                         const http::ResponseHandler& onLaunch,
                                         const http::ErrorHandler& onError)
{
   sessionManager().removePendingSessionLaunch(profile.context.username, profile.context.scope.id(), false, "request error");
   sessionManager().removePendingSessionLaunch(profile.context.username, profile.context.scope.id());
   return countingLaunchFunction(ioContext, profile, jsonRequest, request, onLaunch, onError);
}

Error throwingLaunchFunction(boost::asio::io_context&,
                             const r_util::SessionLaunchProfile&,
                             const json::JsonRpcRequest&,
                             const http::Request&,
                             const http::ResponseHandler&,
                             const http::ErrorHandler&)
{
   s_launchCount++;
   throw std::runtime_error("launch function threw");
}

Error failingLaunchFunction(boost::asio::io_context&,
                            const r_util::SessionLaunchProfile&,
                            const json::JsonRpcRequest&,
                            const http::Request&,
                            const http::ResponseHandler&,
                            const http::ErrorHandler&)
{
   s_launchCount++;
   return systemError(boost::system::errc::resource_unavailable_try_again, ERROR_LOCATION);
}

bool attemptLaunch(const r_util::SessionContext& context)
{
   boost::asio::io_context ioContext;
   json::JsonRpcRequest jsonRequest;
   http::Request request;
   bool launched = false;

   Error error = sessionManager().launchSession(
            ioContext, context, jsonRequest, request, launched, core::system::Options());
   EXPECT_FALSE(error);

   return launched;
}

} // anonymous namespace

TEST(SessionManagerTest, PendingLaunchSuppressesRelaunch)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   r_util::SessionContext context("pending-launch-dedupe-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   // a second request while the launch is pending piggybacks on it
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   // once the connection is made the pending launch is removed, so a
   // later request (e.g. after the session exits) launches again
   sessionManager().removePendingLaunch(context);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, DeadLaunchProcessClearsPendingLaunch)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   r_util::SessionContext context("pending-launch-dead-process-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   sessionManager().notePendingLaunchPid(context, 1234);

   // an exit notification for an unrelated process must not clear the
   // pending launch
   sessionManager().removePendingLaunchForPid(context, 9999, 0);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   // the launched process dying clears the pending launch, so the next
   // recovery attempt can relaunch instead of stalling behind it
   sessionManager().removePendingLaunchForPid(context, 1234, 1);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, ExitNotificationWithoutRecordedPidIsIgnored)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // a pending launch whose pid was never recorded (custom session
   // launchers) keeps the previous behavior: only age or an explicit
   // removal clears it
   r_util::SessionContext context("pending-launch-no-pid-user");
   EXPECT_TRUE(attemptLaunch(context));
   sessionManager().removePendingLaunchForPid(context, 1234, 1);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, RequestErrorKeepsPendingLaunchOfLiveProcess)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // an RPC in flight to an exiting session dies with EOF right as its
   // replacement is spawned; that error must not erase the replacement's
   // pending launch while its process is alive, or the next recovery pass
   // double-launches and orphans the loser of the socket-bind race (#18572).
   // this test process stands in for the live session process.
   r_util::SessionContext context("pending-launch-live-process-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   sessionManager().notePendingLaunchPid(context, ::getpid());

   sessionManager().removePendingLaunch(context, false, "request error");
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   // a successful proxied response (connection made) still clears it
   sessionManager().removePendingLaunch(context);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, RequestErrorKeepsPendingLaunchOfOtherUserProcess)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // in a root-launched multi-user deployment rserver's request-handling
   // threads run privilege-dropped, so the kill(pid, 0) liveness probe
   // yields EPERM for a live rsession owned by another user; that must
   // still read as running, or the guard above is inert exactly where
   // #18572 occurs. pid 1 (init/launchd) stands in for a live process
   // this test cannot signal.
   r_util::SessionContext context("pending-launch-other-user-process-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   sessionManager().notePendingLaunchPid(context, 1);

   sessionManager().removePendingLaunch(context, false, "request error");
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, RequestErrorClearsPendingLaunchOfDeadProcess)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // the liveness guard must not keep entries for processes that are gone:
   // a pid no process can have stands in for a dead session process
   r_util::SessionContext context("pending-launch-dead-pid-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   sessionManager().notePendingLaunchPid(
      context, std::numeric_limits<PidType>::max());

   sessionManager().removePendingLaunch(context, false, "request error");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, RequestErrorClearsPendingLaunchWithoutRecordedPid)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // custom session launchers never record a pid, and have no exit tracker
   // to clear a dead launch: the error paths must keep clearing for them
   r_util::SessionContext context("pending-launch-error-no-pid-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   sessionManager().removePendingLaunch(context, false, "request error");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, RequestsEndingDuringLaunchKeepPendingLaunch)
{
   sessionManager().setSessionLaunchFunction(requestsEndDuringLaunchFunction);
   s_launchCount = 0;

   // requests that end before the launched process even exists can't be
   // about it; clearing its entry then let a retrying request launch a
   // second session for the context, which locked the user out (#18941)
   r_util::SessionContext context("pending-launch-requests-during-launch-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   // once the launch has been made, request outcomes count again
   sessionManager().removePendingLaunch(context);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, OutcomeFromAnotherProcessKeepsPendingLaunch)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // after the launch has returned, the session a restart is replacing can
   // still answer or fail a request; attributed to that process (by the
   // connection's peer pid), the outcome says nothing about the launched
   // one and must not clear its entry (#18963). this test process stands
   // in for the launched session, pid 1 for the one being replaced.
   r_util::SessionContext context("pending-launch-other-process-outcome-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   sessionManager().notePendingLaunchPid(context, ::getpid());

   sessionManager().removePendingLaunch(context, true, std::string(), 1);
   EXPECT_FALSE(attemptLaunch(context));
   sessionManager().removePendingLaunch(context, false, "request error", 1);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   // an outcome from the launched process itself ends the launch
   sessionManager().removePendingLaunch(context, true, std::string(), ::getpid());
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, ErrorFromLaunchedProcessClearsPendingLaunch)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // the liveness guard is for outcomes that can't be attributed: an error
   // the launched process itself produced means it was reached, so the
   // launch is over even though the process is still alive
   r_util::SessionContext context("pending-launch-own-error-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   sessionManager().notePendingLaunchPid(context, ::getpid());

   sessionManager().removePendingLaunch(context, false, "request error", ::getpid());
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, OutcomeWithoutRecordedPidIgnoresPeerPid)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // with no pid recorded for the launch there is nothing to attribute the
   // outcome against, so a success clears the entry as before
   r_util::SessionContext context("pending-launch-peer-no-pid-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   sessionManager().removePendingLaunch(context, true, std::string(), 1);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, SessionIdOutcomeAppliesSameGuards)
{
   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   s_launchCount = 0;

   // the by-session-id variant used to erase unconditionally, ignoring the
   // guards the by-context variant applies (#18963)
   r_util::SessionContext context("pending-launch-session-id-guards-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   sessionManager().notePendingLaunchPid(context, ::getpid());

   sessionManager().removePendingSessionLaunch(context.username, context.scope.id(), false, "request error");
   EXPECT_FALSE(attemptLaunch(context));
   sessionManager().removePendingSessionLaunch(context.username, context.scope.id(), true, std::string(), 1);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   sessionManager().removePendingSessionLaunch(context.username, context.scope.id());
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, SessionIdOutcomesDuringLaunchKeepPendingLaunch)
{
   sessionManager().setSessionLaunchFunction(sessionRpcEndsDuringLaunchFunction);
   s_launchCount = 0;

   r_util::SessionContext context("pending-launch-session-id-during-launch-user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, ThrowingLaunchFunctionClearsPendingLaunch)
{
   sessionManager().setSessionLaunchFunction(throwingLaunchFunction);
   s_launchCount = 0;

   // a launch function that throws used to leave the entry marked as
   // launching, which no outcome could clear until it aged out
   r_util::SessionContext context("pending-launch-throwing-launch-user");
   boost::asio::io_context ioContext;
   json::JsonRpcRequest jsonRequest;
   http::Request request;
   bool launched = false;
   EXPECT_THROW(sessionManager().launchSession(
                   ioContext, context, jsonRequest, request, launched, core::system::Options()),
                std::runtime_error);
   EXPECT_EQ(1, s_launchCount);

   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

TEST(SessionManagerTest, FailedLaunchClearsPendingLaunch)
{
   sessionManager().setSessionLaunchFunction(failingLaunchFunction);
   s_launchCount = 0;

   // a launch that fails is over: nothing may be left waiting on it
   r_util::SessionContext context("pending-launch-failed-launch-user");
   boost::asio::io_context ioContext;
   json::JsonRpcRequest jsonRequest;
   http::Request request;
   bool launched = false;
   Error error = sessionManager().launchSession(
            ioContext, context, jsonRequest, request, launched, core::system::Options());
   EXPECT_TRUE(error);
   EXPECT_FALSE(launched);

   sessionManager().setSessionLaunchFunction(countingLaunchFunction);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, s_launchCount);

   sessionManager().removePendingLaunch(context);
}

} // namespace tests
} // namespace server
} // namespace rstudio
