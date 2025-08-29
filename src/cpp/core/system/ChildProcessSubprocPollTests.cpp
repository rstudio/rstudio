/*
 * ChildProcessSubprocPollTests.cpp
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

#include "ChildProcessSubprocPoll.hpp"

#include <boost/bind/bind.hpp>
#include <boost/thread/thread.hpp>

#include <gtest/gtest.h>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace system {
namespace tests {

namespace {
using boost::posix_time::milliseconds;

const milliseconds kResetRecentDelay = milliseconds(100);
const milliseconds kResetRecentDelayExpired = milliseconds(110);

const milliseconds kCheckSubprocDelay = milliseconds(25);
const milliseconds kCheckSubprocDelayExpired = milliseconds(35);

const milliseconds kCheckCwdDelay = milliseconds(35);

void blockingwait(milliseconds ms)
{
   boost::this_thread::sleep(ms);
}

class NoSubProcPollingFixture
{
public:
   NoSubProcPollingFixture(PidType pid)
      :
        poller_(pid, kResetRecentDelay, kCheckSubprocDelay, kCheckCwdDelay,
                0, std::vector<std::string>(), 0)
   {}

   ChildProcessSubprocPoll poller_;
};

class SubProcPollingFixture
{
public:
   SubProcPollingFixture(PidType pid)
      :
        poller_(pid, kResetRecentDelay, kCheckSubprocDelay, kCheckCwdDelay,
                boost::bind(&SubProcPollingFixture::checkSubproc, this, _1),
                std::vector<std::string>(),
                0),
        checkReturns_(false),
        callerPid_(0),
        checkCalled_(false)
   {}

   std::vector<SubprocInfo> checkSubproc(PidType pid)
   {
      callerPid_ = pid;
      checkCalled_ = true;
      std::vector<SubprocInfo> result;
      if (checkReturns_)
      {
         SubprocInfo info;
         info.exe = "some_exe";
         info.pid = 123;
         result.push_back(info);
      }
      return result;
   }

   void clearFlags()
   {
      callerPid_ = 0;
      checkCalled_ = false;
   }

   ChildProcessSubprocPoll poller_;
   bool checkReturns_;

   PidType callerPid_;
   bool checkCalled_;
};

class CwdPollingFixture
{
public:
   CwdPollingFixture(PidType pid)
      :
        poller_(pid, kResetRecentDelay, kCheckSubprocDelay, kCheckCwdDelay,
                0, std::vector<std::string>(),
                boost::bind(&CwdPollingFixture::checkCwd, this, _1)),
        callerPid_(0),
        checkCalled_(false)
   {}

   core::FilePath checkCwd(PidType pid)
   {
      callerPid_ = pid;
      checkCalled_ = true;
      return checkReturns_;
   }

   void clearFlags()
   {
      callerPid_ = 0;
      checkCalled_ = false;
   }

   ChildProcessSubprocPoll poller_;
   core::FilePath checkReturns_;

   PidType callerPid_;
   bool checkCalled_;
};

} // anonymous namespace

TEST(ChildprocessTest, ChildprocessPollingSupportClassInitialStateWithoutSubprocPollingMatchesExpectation)
{
   PidType pid = 12345;
   NoSubProcPollingFixture test(pid);

   EXPECT_TRUE(test.poller_.hasRecentOutput());
   ASSERT_TRUE(test.poller_.hasNonIgnoredSubprocess());
   EXPECT_FALSE(test.poller_.hasIgnoredSubprocess());
}

TEST(ChildprocessTest, ChildprocessPollingSupportClassRecentInputStateWithoutSubprocPollingDoesntChangeImmediately)
{
   PidType pid = 12345;
   NoSubProcPollingFixture test(pid);

   EXPECT_FALSE(test.poller_.poll(true));
   EXPECT_TRUE(test.poller_.hasRecentOutput());
   EXPECT_FALSE(test.poller_.poll(false));
   ASSERT_TRUE(test.poller_.hasRecentOutput()); // timeout hasn't expired
}

TEST(ChildprocessTest, ChildprocessPollingSupportClassRecentInputStateWithoutSubprocPollingDoesChangeAfterTimeout)
{
   PidType pid = 12345;
   NoSubProcPollingFixture test(pid);

   test.poller_.poll(true);
   EXPECT_TRUE(test.poller_.hasRecentOutput());
   blockingwait(kResetRecentDelayExpired);
   test.poller_.poll(false);
   EXPECT_FALSE(test.poller_.hasRecentOutput()); // now it flips to false
   test.poller_.poll(true);
   ASSERT_TRUE(test.poller_.hasRecentOutput()); // but right back to true
}

TEST(ChildprocessTest, ChildprocessPollingSupportClassInitialStateForSubprocPollingMatchesExpectations)
{
   PidType pid = 12345;
   SubProcPollingFixture test(pid);

   EXPECT_TRUE(test.poller_.hasRecentOutput());
   ASSERT_TRUE(test.poller_.hasNonIgnoredSubprocess());
   EXPECT_FALSE(test.poller_.hasIgnoredSubprocess());
}

TEST(ChildprocessTest, ChildprocessPollingSupportClassChildprocCheckedWhenRecentOutputAndTimeoutExpires)
{
   PidType pid = 12345;
   SubProcPollingFixture test(pid);

   test.checkReturns_ = false;
   test.poller_.poll(true);
   EXPECT_FALSE(test.checkCalled_); // polling timeout hasn't passed
   blockingwait(kCheckSubprocDelayExpired); // longer than the subproc timeout
   EXPECT_TRUE(test.poller_.poll(false)); // not longer than the recent output timeout!
   EXPECT_TRUE(test.checkCalled_);
   EXPECT_EQ(test.checkReturns_, test.poller_.hasNonIgnoredSubprocess());
   EXPECT_TRUE(test.poller_.hasRecentOutput());
   EXPECT_EQ(pid, test.callerPid_);
}

TEST(ChildprocessTest, ChildprocessPollingSupportClassLackOfRecentOutputPreventsChildprocChecking)
{
   PidType pid = 12345;
   SubProcPollingFixture test(pid);

   test.checkReturns_ = false;
   test.poller_.poll(false);
   EXPECT_FALSE(test.checkCalled_);
   blockingwait(kCheckSubprocDelayExpired); // long enough for childproc polling
   test.poller_.poll(false); // not longer than the output timeout
   EXPECT_TRUE(test.checkCalled_);
   EXPECT_EQ(test.checkReturns_, test.poller_.hasNonIgnoredSubprocess());
   EXPECT_TRUE(test.poller_.hasRecentOutput());
   EXPECT_EQ(pid, test.callerPid_);

   test.clearFlags();
   test.poller_.poll(false); // no recent output
   blockingwait(kResetRecentDelayExpired);
   test.poller_.poll(false);
   EXPECT_FALSE(test.checkCalled_); // because of no recent output
   EXPECT_FALSE(test.poller_.hasRecentOutput());
   EXPECT_EQ(test.checkReturns_, test.poller_.hasNonIgnoredSubprocess());


TEST(ChildprocessTest, InitialStateForCwdPollingMatchesExpectations)
{
   PidType pid = 12345;
   CwdPollingFixture test(pid);

   EXPECT_TRUE(test.poller_.hasRecentOutput());
   EXPECT_TRUE(test.poller_.getCwd().isEmpty());
}

} // namespace tests
} // namespace system
} // namespace core
} // namespace rstudio