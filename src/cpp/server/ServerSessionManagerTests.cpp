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

#include <algorithm>
#include <atomic>
#include <memory>
#include <set>
#include <string>
#include <thread>
#include <vector>

#include <signal.h>
#include <sys/wait.h>
#include <unistd.h>

#include <boost/asio/io_context.hpp>
#include <boost/optional.hpp>
#include <boost/optional/optional_io.hpp>
#include <boost/thread/barrier.hpp>
#include <boost/thread/thread.hpp>

#include <core/FileSerializer.hpp>
#include <core/http/Request.hpp>
#include <core/system/PosixSystem.hpp>

#include <shared_core/system/User.hpp>

#include <gtest/gtest.h>

using namespace rstudio::core;
using namespace boost::posix_time;

namespace rstudio {
namespace server {
namespace tests {

// installs a launch function that only counts invocations, so launchSession
// exercises the real pending-launch bookkeeping without spawning processes
class SessionManagerTest : public ::testing::Test
{
protected:
   SessionManagerTest()
      : now_(time_from_string("2026-01-01 00:00:00")),
        manager_(config())
   {
      manager_.setSessionLaunchFunction(launchFunction());
   }

   SessionManager::Config config()
   {
      SessionManager::Config config;
      config.now = [this] { return now_; };
      config.isProcessRunning = [this](PidType pid) { return isLive(pid); };
      return config;
   }

   SessionManager::SessionLaunchFunction launchFunction()
   {
      return [this](boost::asio::io_context&,
                    const r_util::SessionLaunchProfile& profile,
                    const http::Request&,
                    const http::ResponseHandler&,
                    const http::ErrorHandler&)
      {
         if (!launchDelay_.is_zero())
            boost::this_thread::sleep(launchDelay_);

         LOCK_MUTEX(stateMutex_)
         {
            launchedProfiles_.push_back(profile);
         }
         END_LOCK_MUTEX

         launchCount_++;
         return launchError_;
      };
   }

   std::unique_ptr<SessionManager> makeManager(const SessionManager::Config& config)
   {
      return std::unique_ptr<SessionManager>(new SessionManager(config));
   }

   std::unique_ptr<SessionManager> defaultManager()
   {
      std::unique_ptr<SessionManager> manager = makeManager(SessionManager::Config());
      manager->setSessionLaunchFunction(launchFunction());
      return manager;
   }

   // none => no pending launch; -1 => pending launch with no recorded pid
   static boost::optional<PidType> pendingLaunchPid(SessionManager& manager,
                                                    const r_util::SessionContext& context)
   {
      boost::optional<PidType> pid;
      LOCK_MUTEX(manager.launchesMutex_)
      {
         auto it = manager.pendingLaunches_.find(context);
         if (it != manager.pendingLaunches_.end())
            pid = it->second.pid;
      }
      END_LOCK_MUTEX

      return pid;
   }

   static std::size_t pendingLaunchCount(SessionManager& manager)
   {
      std::size_t count = 0;
      LOCK_MUTEX(manager.launchesMutex_)
      {
         count = manager.pendingLaunches_.size();
      }
      END_LOCK_MUTEX

      return count;
   }

   void markLive(PidType pid)
   {
      LOCK_MUTEX(stateMutex_)
      {
         livePids_.insert(pid);
      }
      END_LOCK_MUTEX
   }

   bool isLive(PidType pid)
   {
      bool live = false;
      LOCK_MUTEX(stateMutex_)
      {
         live = livePids_.count(pid) > 0;
      }
      END_LOCK_MUTEX

      return live;
   }

   std::vector<r_util::SessionLaunchProfile> launchedProfiles()
   {
      std::vector<r_util::SessionLaunchProfile> profiles;
      LOCK_MUTEX(stateMutex_)
      {
         profiles = launchedProfiles_;
      }
      END_LOCK_MUTEX

      return profiles;
   }

   static bool hasArg(const core::system::Options& args,
                      const std::string& name,
                      const std::string& value)
   {
      return std::find(args.begin(), args.end(), core::system::Option(name, value)) != args.end();
   }

   static bool hasArgNamed(const core::system::Options& args, const std::string& name)
   {
      return std::find_if(args.begin(), args.end(),
                          [&](const core::system::Option& arg) { return arg.first == name; }) != args.end();
   }

   Error launch(SessionManager& manager,
                const r_util::SessionContext& context,
                const http::Request& request,
                bool* pLaunched)
   {
      boost::asio::io_context ioContext;
      return manager.launchSession(
               ioContext, context, request, *pLaunched, core::system::Options());
   }

   Error launch(SessionManager& manager, const r_util::SessionContext& context, bool* pLaunched)
   {
      return launch(manager, context, http::Request(), pLaunched);
   }

   Error launch(const r_util::SessionContext& context, bool* pLaunched)
   {
      return launch(manager_, context, pLaunched);
   }

   bool attemptLaunch(SessionManager& manager, const r_util::SessionContext& context)
   {
      bool launched = false;
      Error error = launch(manager, context, &launched);
      EXPECT_FALSE(error);

      return launched;
   }

   bool attemptLaunch(const r_util::SessionContext& context)
   {
      return attemptLaunch(manager_, context);
   }

   ptime now_;
   boost::mutex stateMutex_;
   std::set<PidType> livePids_;
   std::vector<r_util::SessionLaunchProfile> launchedProfiles_;
   std::atomic<int> launchCount_{0};
   time_duration launchDelay_ = time_duration();
   Error launchError_;
   SessionManager manager_;
};

// While a session is starting, more requests wait for it rather than start
// a second copy; once it connects, a later request can start it again.
TEST_F(SessionManagerTest, PendingLaunchSuppressesRelaunch)
{
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);
   EXPECT_EQ(PidType(-1), pendingLaunchPid(manager_, context));

   // a second request while the launch is pending piggybacks on it
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);

   // once the connection is made the pending launch is removed, so a
   // later request (e.g. after the session exits) launches again
   manager_.removePendingLaunch(context);
   EXPECT_FALSE(pendingLaunchPid(manager_, context));
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// If a starting session's process dies, the next request starts a new one;
// an unrelated process exiting changes nothing.
TEST_F(SessionManagerTest, DeadLaunchProcessClearsPendingLaunch)
{
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);
   manager_.notePendingLaunchPid(context, 1234);
   EXPECT_EQ(PidType(1234), pendingLaunchPid(manager_, context));

   // an exit notification for an unrelated process must not clear the
   // pending launch
   manager_.removePendingLaunchForPid(context, 9999, 0);
   EXPECT_EQ(PidType(1234), pendingLaunchPid(manager_, context));
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);

   // the launched process dying clears the pending launch, so the next
   // recovery attempt can relaunch instead of stalling behind it
   manager_.removePendingLaunchForPid(context, 1234, 1);
   EXPECT_FALSE(pendingLaunchPid(manager_, context));
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// When we don't know which process a starting session is (custom launchers),
// a process exiting doesn't make us start it again.
TEST_F(SessionManagerTest, ExitNotificationWithoutRecordedPidIsIgnored)
{
   // a pending launch whose pid was never recorded (custom session
   // launchers) keeps the previous behavior: only age or an explicit
   // removal clears it
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   manager_.removePendingLaunchForPid(context, 1234, 1);
   EXPECT_EQ(PidType(-1), pendingLaunchPid(manager_, context));
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);
}

// A failed request doesn't make us forget a session that is still starting
// and running, so we don't start a duplicate.
TEST_F(SessionManagerTest, RequestErrorKeepsPendingLaunchOfLiveProcess)
{
   // an RPC in flight to an exiting session dies with EOF right as its
   // replacement is spawned; that error must not erase the replacement's
   // pending launch while its process is alive, or the next recovery pass
   // double-launches and orphans the loser of the socket-bind race (#18572)
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);
   manager_.notePendingLaunchPid(context, 1234);
   markLive(1234);

   manager_.removePendingLaunch(context, false, "request error");
   EXPECT_EQ(PidType(1234), pendingLaunchPid(manager_, context));
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);

   // a successful proxied response (connection made) still clears it
   manager_.removePendingLaunch(context);
   EXPECT_FALSE(pendingLaunchPid(manager_, context));
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// A failed request does let us start again once the starting session's
// process has died.
TEST_F(SessionManagerTest, RequestErrorClearsPendingLaunchOfDeadProcess)
{
   // the liveness guard must not keep entries for processes that are gone
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);
   manager_.notePendingLaunchPid(context, 1234);

   manager_.removePendingLaunch(context, false, "request error");
   EXPECT_FALSE(pendingLaunchPid(manager_, context));
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// With the real "is this process running?" check, a failed request keeps a
// running session (this test's own process stands in for it).
TEST_F(SessionManagerTest, DefaultProcessProbeSeesLiveProcess)
{
   std::unique_ptr<SessionManager> manager = defaultManager();
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(*manager, context));
   EXPECT_EQ(1, launchCount_);
   manager->notePendingLaunchPid(context, ::getpid());

   manager->removePendingLaunch(context, false, "request error");
   EXPECT_EQ(::getpid(), pendingLaunchPid(*manager, context));
   EXPECT_FALSE(attemptLaunch(*manager, context));
   EXPECT_EQ(1, launchCount_);
}

// Same, for a session owned by another user (as on a multi-user server): it
// must still count as running.
TEST_F(SessionManagerTest, DefaultProcessProbeSeesOtherUserProcess)
{
   // in a root-launched multi-user deployment rserver's request-handling
   // threads run privilege-dropped, so the kill(pid, 0) liveness probe
   // yields EPERM for a live rsession owned by another user; that must
   // still read as running, or the guard above is inert exactly where
   // #18572 occurs. pid 1 (init/launchd) stands in for a live process
   // this test cannot signal.
   std::unique_ptr<SessionManager> manager = defaultManager();
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(*manager, context));
   EXPECT_EQ(1, launchCount_);
   manager->notePendingLaunchPid(context, 1);

   manager->removePendingLaunch(context, false, "request error");
   EXPECT_EQ(PidType(1), pendingLaunchPid(*manager, context));
   EXPECT_FALSE(attemptLaunch(*manager, context));
   EXPECT_EQ(1, launchCount_);
}

// When we don't know which process is starting, a failed request lets the
// next request start the session again.
TEST_F(SessionManagerTest, RequestErrorClearsPendingLaunchWithoutRecordedPid)
{
   // custom session launchers never record a pid, and have no exit tracker
   // to clear a dead launch: the error paths must keep clearing for them
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);

   manager_.removePendingLaunch(context, false, "request error");
   EXPECT_FALSE(pendingLaunchPid(manager_, context));
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// A session that is running but slow to start gets 3 minutes before we
// start another.
TEST_F(SessionManagerTest, SlowLaunchOfLiveProcessIsNotRelaunched)
{
   ptime start = now_;
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   manager_.notePendingLaunchPid(context, 1234);
   markLive(1234);

   now_ = start + seconds(61);
   EXPECT_FALSE(attemptLaunch(context));

   now_ = start + minutes(3) - milliseconds(1);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);

   now_ = start + minutes(3);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// A starting session we can't check on gets 1 minute before a new request
// starts another.
TEST_F(SessionManagerTest, LaunchWindowEndsAfterOneMinute)
{
   ptime start = now_;
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));

   now_ = start + minutes(1) - milliseconds(1);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);

   now_ = start + minutes(1);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// A failed request during a slow start still doesn't cause a duplicate
// while the session is running.
TEST_F(SessionManagerTest, RequestErrorKeepsSlowLaunchOfLiveProcess)
{
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   manager_.notePendingLaunchPid(context, 1234);
   markLive(1234);

   manager_.removePendingLaunch(context, false, "request error");
   EXPECT_EQ(PidType(1234), pendingLaunchPid(manager_, context));
   now_ += seconds(61);
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);
}

// After we give up on a stuck session and start a replacement, the old one
// exiting doesn't make us forget the replacement.
TEST_F(SessionManagerTest, OldProcessExitDoesNotClearRelaunch)
{
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   manager_.notePendingLaunchPid(context, 1234);
   markLive(1234);

   now_ += minutes(3);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
   EXPECT_EQ(PidType(-1), pendingLaunchPid(manager_, context));

   manager_.removePendingLaunchForPid(context, 1234, 1);
   EXPECT_EQ(PidType(-1), pendingLaunchPid(manager_, context));
   EXPECT_FALSE(attemptLaunch(context));

   manager_.notePendingLaunchPid(context, 5678);
   manager_.removePendingLaunchForPid(context, 1234, 1);
   EXPECT_EQ(PidType(5678), pendingLaunchPid(manager_, context));
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// If starting a session fails outright, nothing is left behind and the next
// request tries again.
TEST_F(SessionManagerTest, LaunchErrorClearsPendingLaunch)
{
   r_util::SessionContext context("user");
   launchError_ = Error("TestError", 1, ERROR_LOCATION);
   bool launched = false;
   EXPECT_TRUE(launch(context, &launched));
   EXPECT_FALSE(launched);
   EXPECT_FALSE(pendingLaunchPid(manager_, context));
   EXPECT_EQ(0u, pendingLaunchCount(manager_));

   launchError_ = Success();
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// Two sessions of the same user are tracked separately; finishing one
// doesn't affect the other.
TEST_F(SessionManagerTest, SessionsOfOneUserHaveSeparatePendingLaunches)
{
   r_util::SessionContext first("user", r_util::SessionScope::projectNone("aaaa1111"));
   r_util::SessionContext second("user", r_util::SessionScope::projectNone("bbbb2222"));
   EXPECT_TRUE(attemptLaunch(first));
   EXPECT_TRUE(attemptLaunch(second));
   EXPECT_EQ(2, launchCount_);
   EXPECT_EQ(2u, pendingLaunchCount(manager_));

   manager_.removePendingLaunch(first);
   EXPECT_FALSE(pendingLaunchPid(manager_, first));
   EXPECT_TRUE(pendingLaunchPid(manager_, second));
   EXPECT_TRUE(attemptLaunch(first));
   EXPECT_FALSE(attemptLaunch(second));
   EXPECT_EQ(3, launchCount_);
}

// Clearing a starting session by user and session ID only affects that
// session.
TEST_F(SessionManagerTest, RemovePendingSessionLaunchClearsOnlyMatchingSession)
{
   r_util::SessionContext context("user", r_util::SessionScope::projectNone("aaaa1111"));
   EXPECT_TRUE(attemptLaunch(context));
   manager_.notePendingLaunchPid(context, 1234);
   markLive(1234);

   manager_.removePendingSessionLaunch("other-user", "aaaa1111", false, "request error");
   manager_.removePendingSessionLaunch("user", "bbbb2222", false, "request error");
   EXPECT_EQ(PidType(1234), pendingLaunchPid(manager_, context));
   EXPECT_FALSE(attemptLaunch(context));
   EXPECT_EQ(1, launchCount_);

   manager_.removePendingSessionLaunch("user", "aaaa1111", false, "request error");
   EXPECT_FALSE(pendingLaunchPid(manager_, context));
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// A starting session we can't check on is started again after a minute.
TEST_F(SessionManagerTest, AgedLaunchWithoutRecordedPidIsRelaunched)
{
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));

   now_ += seconds(61);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// A starting session whose process is gone is started again after a minute.
TEST_F(SessionManagerTest, AgedLaunchOfDeadProcessIsRelaunched)
{
   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));
   manager_.notePendingLaunchPid(context, 1234);

   now_ += seconds(61);
   EXPECT_TRUE(attemptLaunch(context));
   EXPECT_EQ(2, launchCount_);
}

// Starting any session also forgets other sessions that have been starting
// for over 3 minutes, even if still running.
TEST_F(SessionManagerTest, LaunchSweepsStaleEntriesOfOtherContexts)
{
   // the sweep caps every entry at the stale age, even one whose process is
   // still alive: a launch that never connected to this server in that time
   // is presumed handled elsewhere (e.g. another load-balanced node)
   r_util::SessionContext first("user", r_util::SessionScope::projectNone("aaaa1111"));
   r_util::SessionContext second("user", r_util::SessionScope::projectNone("bbbb2222"));
   EXPECT_TRUE(attemptLaunch(first));
   manager_.notePendingLaunchPid(first, 1234);
   markLive(1234);

   now_ += minutes(3) + milliseconds(1);
   EXPECT_TRUE(attemptLaunch(second));
   EXPECT_FALSE(pendingLaunchPid(manager_, first));
   EXPECT_EQ(1u, pendingLaunchCount(manager_));
}

// That cleanup leaves alone sessions that have been starting for under
// 3 minutes.
TEST_F(SessionManagerTest, SweepKeepsEntriesInsideStaleAge)
{
   r_util::SessionContext first("user", r_util::SessionScope::projectNone("aaaa1111"));
   r_util::SessionContext second("user", r_util::SessionScope::projectNone("bbbb2222"));
   EXPECT_TRUE(attemptLaunch(first));

   now_ += minutes(2) + seconds(59);
   EXPECT_TRUE(attemptLaunch(second));
   EXPECT_EQ(PidType(-1), pendingLaunchPid(manager_, first));
   EXPECT_EQ(2u, pendingLaunchCount(manager_));
}

// Hooks that customize how a session starts run in order, before the
// session is started.
TEST_F(SessionManagerTest, ProfileFiltersApplyBeforeLaunch)
{
   manager_.addSessionLaunchProfileFilter([](r_util::SessionLaunchProfile* pProfile)
   {
      pProfile->executablePath = "/filtered/rsession";
      pProfile->config.args.push_back({ "--first", "1" });
   });
   manager_.addSessionLaunchProfileFilter([](r_util::SessionLaunchProfile* pProfile)
   {
      pProfile->config.args.push_back({ "--second", pProfile->executablePath });
   });

   r_util::SessionContext context("user");
   EXPECT_TRUE(attemptLaunch(context));

   std::vector<r_util::SessionLaunchProfile> profiles = launchedProfiles();
   ASSERT_EQ(1u, profiles.size());
   EXPECT_EQ("/filtered/rsession", profiles[0].executablePath);

   const core::system::Options& args = profiles[0].config.args;
   ASSERT_GE(args.size(), 2u);
   EXPECT_EQ(core::system::Option("--first", "1"), args[args.size() - 2]);
   EXPECT_EQ(core::system::Option("--second", "/filtered/rsession"), args[args.size() - 1]);
}

// The "restore workspace" and "run .Rprofile" choices from the first page
// load are passed to the new session; other requests don't pass them.
TEST_F(SessionManagerTest, ClientInitArgsReachProfile)
{
   std::string body = R"({"method": "client_init", "params": [], "kwparams": {"restore_workspace": 0, "run_rprofile": 1}})";

   http::Request clientInit;
   clientInit.setUri("/rpc/client_init");
   clientInit.setBody(body);

   r_util::SessionContext context("user", r_util::SessionScope::projectNone("aaaa1111"));
   bool launched = false;
   EXPECT_FALSE(launch(manager_, context, clientInit, &launched));
   EXPECT_TRUE(launched);

   http::Request other;
   other.setUri("/rpc/console_input");
   other.setBody(body);

   r_util::SessionContext otherContext("user", r_util::SessionScope::projectNone("bbbb2222"));
   launched = false;
   EXPECT_FALSE(launch(manager_, otherContext, other, &launched));
   EXPECT_TRUE(launched);

   std::vector<r_util::SessionLaunchProfile> profiles = launchedProfiles();
   ASSERT_EQ(2u, profiles.size());
   EXPECT_TRUE(hasArg(profiles[0].config.args, "--r-restore-workspace", "0"));
   EXPECT_TRUE(hasArg(profiles[0].config.args, "--r-run-rprofile", "1"));
   EXPECT_FALSE(hasArgNamed(profiles[1].config.args, "--r-restore-workspace"));
   EXPECT_FALSE(hasArgNamed(profiles[1].config.args, "--r-run-rprofile"));
}

// Many simultaneous requests for the same session start it exactly once.
TEST_F(SessionManagerTest, ConcurrentLaunchesOfOneContextLaunchOnce)
{
   // the entry is inserted under the mutex before the launcher runs, so the
   // slow launcher only widens the window for the thread sanitizer
   launchDelay_ = milliseconds(20);

   const int kThreads = 16;
   r_util::SessionContext context("user");
   boost::barrier start(kThreads);
   std::vector<char> launched(kThreads, false);
   std::vector<std::thread> threads;
   for (int i = 0; i < kThreads; i++)
   {
      threads.emplace_back([&, i]
      {
         start.wait();
         launched[i] = attemptLaunch(context);
      });
   }
   for (std::thread& thread : threads)
      thread.join();

   EXPECT_EQ(1, launchCount_);
   EXPECT_EQ(1, std::count(launched.begin(), launched.end(), true));
   EXPECT_EQ(1u, pendingLaunchCount(manager_));
}

// Simultaneous requests for different sessions each start their own session.
TEST_F(SessionManagerTest, ConcurrentLaunchesOfDistinctContextsEachLaunch)
{
   launchDelay_ = milliseconds(20);

   const int kThreads = 16;
   boost::barrier start(kThreads);
   std::vector<char> launched(kThreads, false);
   std::vector<std::thread> threads;
   for (int i = 0; i < kThreads; i++)
   {
      threads.emplace_back([&, i]
      {
         r_util::SessionContext context("user", r_util::SessionScope::projectNone("scope" + std::to_string(i)));
         start.wait();
         launched[i] = attemptLaunch(context);
      });
   }
   for (std::thread& thread : threads)
      thread.join();

   EXPECT_EQ(kThreads, launchCount_);
   EXPECT_EQ(kThreads, std::count(launched.begin(), launched.end(), true));
   EXPECT_EQ(std::size_t(kThreads), pendingLaunchCount(manager_));
}

// Starting, tracking, and clearing sessions from many threads at once
// doesn't crash or leave bad data behind.
TEST_F(SessionManagerTest, ConcurrentRemoveNoteAndLaunchStayConsistent)
{
   // a weak end-state check: this test mainly gives the thread sanitizer
   // every entry point racing on the same few contexts
   const int kThreads = 8;
   const int kIterations = 200;
   std::vector<r_util::SessionContext> contexts;
   for (const char* id : { "aaaa1111", "bbbb2222", "cccc3333" })
      contexts.emplace_back("user", r_util::SessionScope::projectNone(id));

   boost::mutex notedMutex;
   std::set<PidType> notedPids;

   boost::barrier start(kThreads);
   std::vector<std::thread> threads;
   for (int t = 0; t < kThreads; t++)
   {
      threads.emplace_back([&, t]
      {
         start.wait();
         for (int i = 0; i < kIterations; i++)
         {
            const r_util::SessionContext& context = contexts[(t + i) % contexts.size()];
            PidType pid = 1000 * (t + 1) + i;
            switch ((t + i) % 4)
            {
            case 0:
               attemptLaunch(context);
               break;
            case 1:
               LOCK_MUTEX(notedMutex)
               {
                  notedPids.insert(pid);
               }
               END_LOCK_MUTEX
               if (i % 2 == 0)
                  markLive(pid);
               manager_.notePendingLaunchPid(context, pid);
               break;
            case 2:
               manager_.removePendingLaunch(context, false, "request error");
               break;
            case 3:
               manager_.removePendingLaunchForPid(context, pid - 2, 1);
               break;
            }
         }
      });
   }
   for (std::thread& thread : threads)
      thread.join();

   for (const r_util::SessionContext& context : contexts)
   {
      boost::optional<PidType> pid = pendingLaunchPid(manager_, context);
      if (pid && *pid != -1)
         EXPECT_EQ(1u, notedPids.count(*pid)) << "unexpected pid " << *pid;
   }
   EXPECT_LE(pendingLaunchCount(manager_), contexts.size());
}

// launches real child processes through the default launcher, with a profile
// filter swapping rsession for a /bin/sh stub that runs script_. the stub must
// be a system binary: on macOS the launcher sets DYLD_INSERT_LIBRARIES, which
// dyld drops only for SIP-protected executables
class SessionManagerProcessTest : public SessionManagerTest
{
protected:
   void SetUp() override
   {
      core::system::User user;
      Error error = core::system::User::getCurrentUser(user);
      ASSERT_FALSE(error);
      username_ = user.getUsername();
   }

   void TearDown() override
   {
      // stub children get their own process group and outlive this binary;
      // kill any the test left running. a pid the tracker already reaped
      // reports ECHILD and is skipped, since it may have been reused
      for (PidType pid : spawnedPids_)
      {
         int status = 0;
         if (::waitpid(pid, &status, WNOHANG) == 0)
         {
            ::kill(pid, SIGKILL);
            ::waitpid(pid, &status, 0);
         }
      }
   }

   std::unique_ptr<SessionManager> makeProcessManager()
   {
      SessionManager::Config config;
      config.now = [this] { return now_; };

      std::unique_ptr<SessionManager> manager = makeManager(config);
      manager->addSessionLaunchProfileFilter([this](r_util::SessionLaunchProfile* pProfile)
      {
         core::system::Options args = { { "-c", script_ } };
         if (forwardSessionArgs_)
         {
            // sh -c <script> <$0> <$1...>: the session args become "$@"
            args.push_back({ "stub", "" });
            args.insert(args.end(), pProfile->config.args.begin(), pProfile->config.args.end());
         }

         pProfile->executablePath = executablePath_;
         pProfile->config.args = args;
      });
      return manager;
   }

   // launches the stub and records its pid for teardown; -1 when the child
   // exited and was reaped by the tracker before the launch returned
   PidType launchChild(SessionManager& manager, const r_util::SessionContext& context)
   {
      bool launched = false;
      Error error = launch(manager, context, &launched);
      EXPECT_FALSE(error);
      EXPECT_TRUE(launched);

      boost::optional<PidType> pid = pendingLaunchPid(manager, context);
      if (!pid)
         return -1;

      if (*pid > 0)
         spawnedPids_.push_back(*pid);
      return *pid;
   }

   // stands in for rserver's SIGCHLD thread, which the test binary lacks
   bool waitForReap(SessionManager& manager,
                    const r_util::SessionContext& context,
                    time_duration timeout = seconds(5))
   {
      ptime deadline = microsec_clock::universal_time() + timeout;
      while (pendingLaunchPid(manager, context))
      {
         if (microsec_clock::universal_time() > deadline)
            return false;

         manager.notifySIGCHLD();
         boost::this_thread::sleep(milliseconds(10));
      }
      return true;
   }

   // SIGKILL a child and wait for the tracker to reap it (and so run its
   // exit handler), whatever becomes of the context's pending launch
   bool killAndReap(SessionManager& manager, PidType pid, time_duration timeout = seconds(5))
   {
      if (::kill(pid, SIGKILL) == -1)
         return false;

      ptime deadline = microsec_clock::universal_time() + timeout;
      while (core::system::isProcessRunning(pid))
      {
         if (microsec_clock::universal_time() > deadline)
            return false;

         manager.notifySIGCHLD();
         boost::this_thread::sleep(milliseconds(10));
      }
      return true;
   }

   std::string username_;
   std::string executablePath_ = "/bin/sh";
   std::string script_ = "exec sleep 30";
   bool forwardSessionArgs_ = false;
   std::vector<PidType> spawnedPids_;
};

// Starting a real process records which process it is, and it is running.
TEST_F(SessionManagerProcessTest, RealLaunchRecordsChildPid)
{
   std::unique_ptr<SessionManager> manager = makeProcessManager();
   r_util::SessionContext context(username_);

   PidType pid = launchChild(*manager, context);
   EXPECT_GT(pid, 0);
   EXPECT_NE(::getpid(), pid);
   EXPECT_TRUE(core::system::isProcessRunning(pid));
}

// When a real session process exits right away, we notice and can start it
// again.
TEST_F(SessionManagerProcessTest, ChildExitClearsPendingLaunch)
{
   // a fast exit can be reaped by addProcess's probe before the launch
   // returns, so the entry may already be gone here
   script_ = "exit 3";
   std::unique_ptr<SessionManager> manager = makeProcessManager();
   r_util::SessionContext context(username_);

   launchChild(*manager, context);
   EXPECT_TRUE(waitForReap(*manager, context));
   EXPECT_FALSE(pendingLaunchPid(*manager, context));

   launchChild(*manager, context);
   EXPECT_TRUE(waitForReap(*manager, context));
}

// If the session program can't run at all, that shows up as the process
// exiting, so we don't wait on it.
TEST_F(SessionManagerProcessTest, ExecFailureSurfacesAsChildExit)
{
   // the forked child _exits when execve fails, so the launch itself succeeds
   executablePath_ = "/nonexistent/rsession";
   std::unique_ptr<SessionManager> manager = makeProcessManager();
   r_util::SessionContext context(username_);

   launchChild(*manager, context);
   EXPECT_TRUE(waitForReap(*manager, context));
}

// With a real running process, a failed request doesn't cause a duplicate
// session; once it is killed, we can start again.
TEST_F(SessionManagerProcessTest, RequestErrorKeepsLiveChild)
{
   // #18572: a request error must not clear the pending launch of a live
   // session process, or the next recovery pass launches a second one
   std::unique_ptr<SessionManager> manager = makeProcessManager();
   r_util::SessionContext context(username_);

   PidType pid = launchChild(*manager, context);
   ASSERT_GT(pid, 0);

   manager->removePendingLaunch(context, false, "request error");
   EXPECT_EQ(pid, pendingLaunchPid(*manager, context));
   EXPECT_FALSE(attemptLaunch(*manager, context));

   ASSERT_EQ(0, ::kill(pid, SIGKILL));
   EXPECT_TRUE(waitForReap(*manager, context));
   EXPECT_GT(launchChild(*manager, context), 0);
}

// With real processes: after giving up on a stuck session and starting a
// replacement, the stuck one exiting doesn't make us forget the replacement.
TEST_F(SessionManagerProcessTest, StaleChildExitDoesNotClearReplacement)
{
   ptime start = now_;
   std::unique_ptr<SessionManager> manager = makeProcessManager();
   r_util::SessionContext context(username_);

   PidType first = launchChild(*manager, context);
   ASSERT_GT(first, 0);

   now_ = start + seconds(61);
   EXPECT_FALSE(attemptLaunch(*manager, context));
   EXPECT_EQ(first, pendingLaunchPid(*manager, context));

   // past the stale age the live first child no longer holds off a relaunch,
   // leaving two live children for one context
   now_ = start + minutes(3);
   PidType second = launchChild(*manager, context);
   ASSERT_GT(second, 0);
   EXPECT_NE(first, second);
   EXPECT_TRUE(core::system::isProcessRunning(first));

   EXPECT_TRUE(killAndReap(*manager, first));
   EXPECT_EQ(second, pendingLaunchPid(*manager, context));

   EXPECT_TRUE(killAndReap(*manager, second));
   EXPECT_FALSE(pendingLaunchPid(*manager, context));
}

// A session that connected and later exits doesn't make us forget a newer
// session that is still starting.
TEST_F(SessionManagerProcessTest, SessionExitAfterConnectKeepsRelaunch)
{
   std::unique_ptr<SessionManager> manager = makeProcessManager();
   r_util::SessionContext context(username_);

   PidType first = launchChild(*manager, context);
   ASSERT_GT(first, 0);

   // connection made: the entry clears while the session keeps running
   manager->removePendingLaunch(context);
   EXPECT_FALSE(pendingLaunchPid(*manager, context));
   EXPECT_TRUE(core::system::isProcessRunning(first));

   PidType second = launchChild(*manager, context);
   ASSERT_GT(second, 0);
   EXPECT_NE(first, second);

   // the connected session's eventual exit leaves its replacement's entry
   EXPECT_TRUE(killAndReap(*manager, first));
   EXPECT_EQ(second, pendingLaunchPid(*manager, context));
}

// A process that has exited but not yet been cleaned up still looks running
// until the cleanup happens.
TEST_F(SessionManagerProcessTest, ZombieChildCountsAsLive)
{
   // kill(pid, 0) succeeds on an unreaped zombie, so the liveness guard
   // relies on prompt SIGCHLD reaping to let go of a dead launch
   std::unique_ptr<SessionManager> manager = makeProcessManager();
   r_util::SessionContext context(username_);

   PidType pid = launchChild(*manager, context);
   ASSERT_GT(pid, 0);

   ASSERT_EQ(0, ::kill(pid, SIGKILL));
   siginfo_t info;
   ASSERT_EQ(0, ::waitid(P_PID, pid, &info, WEXITED | WNOWAIT));

   manager->removePendingLaunch(context, false, "request error");
   EXPECT_EQ(pid, pendingLaunchPid(*manager, context));

   manager->notifySIGCHLD();
   EXPECT_FALSE(pendingLaunchPid(*manager, context));
}

// The started process receives the expected user, login token, and session
// ID.
TEST_F(SessionManagerProcessTest, StubReceivesSessionArgsAndEnv)
{
   FilePath outputPath;
   ASSERT_FALSE(FilePath::tempFilePath(outputPath));
   std::string output = outputPath.getAbsolutePath();

   forwardSessionArgs_ = true;
   script_ = "printf '%s\\n' \"$@\" > '" + output + "'; env >> '" + output + "'";
   std::unique_ptr<SessionManager> manager = makeProcessManager();
   r_util::SessionContext context(username_, r_util::SessionScope::projectNone("aaaa1111"));

   launchChild(*manager, context);
   ASSERT_TRUE(waitForReap(*manager, context));

   std::string contents;
   ASSERT_FALSE(core::readStringFromFile(outputPath, &contents));
   EXPECT_NE(std::string::npos, contents.find("-u\n" + username_ + "\n"));
   EXPECT_NE(std::string::npos, contents.find("--launcher-token\n"));
   EXPECT_NE(std::string::npos, contents.find("\nRSTUDIO_SESSION_SCOPE_ID=aaaa1111\n"));

   outputPath.remove();
}

} // namespace tests
} // namespace server
} // namespace rstudio
