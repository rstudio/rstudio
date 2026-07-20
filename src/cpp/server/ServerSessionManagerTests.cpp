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

#include <boost/asio/io_context.hpp>

#include <core/http/Request.hpp>

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
                             const http::Request&,
                             const http::ResponseHandler&,
                             const http::ErrorHandler&)
{
   s_launchCount++;
   return Success();
}

bool attemptLaunch(const r_util::SessionContext& context)
{
   boost::asio::io_context ioContext;
   http::Request request;
   bool launched = false;

   Error error = sessionManager().launchSession(
            ioContext, context, request, launched, core::system::Options());
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

} // namespace tests
} // namespace server
} // namespace rstudio
