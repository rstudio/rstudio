/*
 * ChildProcessSubprocPollTests.cpp
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

#include "ChildProcessSubprocPoll.hpp"

#include <boost/bind.hpp>
#include <boost/thread/thread.hpp>

#include <tests/TestThat.hpp>

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
const milliseconds kCheckCwdDelayExpired = milliseconds(45);

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

test_context("ChildProcess polling support class")
{
   test_that("initial state without subproc polling matches expectation")
   {
      PidType pid = 12345;
      NoSubProcPollingFixture test(pid);

      expect_true(test.poller_.hasRecentOutput());
      expect_true(test.poller_.hasNonWhitelistSubprocess());
      expect_false(test.poller_.hasWhitelistSubprocess());
   }

   test_that("recent input state without subproc polling doesn't change immediately")
   {
      PidType pid = 12345;
      NoSubProcPollingFixture test(pid);

      expect_false(test.poller_.poll(true));
      expect_true(test.poller_.hasRecentOutput());
      expect_false(test.poller_.poll(false));
      expect_true(test.poller_.hasRecentOutput()); // timeout hasn't expired
   }

   test_that("recent input state without subproc polling does change after timeout")
   {
      PidType pid = 12345;
      NoSubProcPollingFixture test(pid);

      test.poller_.poll(true);
      expect_true(test.poller_.hasRecentOutput());
      blockingwait(kResetRecentDelayExpired);
      test.poller_.poll(false);
      expect_false(test.poller_.hasRecentOutput()); // now it flips to false
      test.poller_.poll(true);
      expect_true(test.poller_.hasRecentOutput()); // but right back to true
   }

   test_that("initial state for subproc polling matches expectations")
   {
      PidType pid = 12345;
      SubProcPollingFixture test(pid);

      expect_true(test.poller_.hasRecentOutput());
      expect_true(test.poller_.hasNonWhitelistSubprocess());
      expect_false(test.poller_.hasWhitelistSubprocess());
   }

   test_that("childproc checked when recent output and timeout expires")
   {
      PidType pid = 12345;
      SubProcPollingFixture test(pid);

      test.checkReturns_ = false;
      test.poller_.poll(true);
      expect_false(test.checkCalled_); // polling timeout hasn't passed
      blockingwait(kCheckSubprocDelayExpired); // longer than the subproc timeout
      expect_true(test.poller_.poll(false)); // not longer than the recent output timeout!
      expect_true(test.checkCalled_);
      expect_true(test.poller_.hasNonWhitelistSubprocess() == test.checkReturns_);
      expect_true(test.poller_.hasRecentOutput());
      expect_true(test.callerPid_ == pid);
   }

   test_that("lack of recent output prevents childproc checking")
   {
      PidType pid = 12345;
      SubProcPollingFixture test(pid);

      test.checkReturns_ = false;
      test.poller_.poll(false);
      expect_false(test.checkCalled_);
      blockingwait(kCheckSubprocDelayExpired); // long enough for childproc polling
      test.poller_.poll(false); // not longer than the output timeout
      expect_true(test.checkCalled_);
      expect_true(test.poller_.hasNonWhitelistSubprocess() == test.checkReturns_);
      expect_true(test.poller_.hasRecentOutput());
      expect_true(test.callerPid_ == pid);

      test.clearFlags();
      test.poller_.poll(false); // no recent output
      blockingwait(kResetRecentDelayExpired);
      test.poller_.poll(false);
      expect_false(test.checkCalled_); // because of no recent output
      expect_false(test.poller_.hasRecentOutput());
      expect_true(test.poller_.hasNonWhitelistSubprocess() == test.checkReturns_);
   }

   test_that("initial state for cwd polling matches expectations")
   {
      PidType pid = 12345;
      CwdPollingFixture test(pid);

      expect_true(test.poller_.hasRecentOutput());
      expect_true(test.poller_.getCwd().isEmpty());
   }
}

} // namespace tests
} // namespace system
} // namespace core
} // namespace rstudio
