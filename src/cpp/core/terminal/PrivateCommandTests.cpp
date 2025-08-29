/*
 * PrivateCommandTests.cpp
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

#include <core/terminal/PrivateCommand.hpp>

#include <vector>
#include <memory>

#include <gtest/gtest.h>

#include <core/BoostThread.hpp>

using namespace boost::posix_time;
using namespace boost::system;

namespace rstudio {
namespace core {
namespace terminal {
namespace tests {

namespace {

// Constants used in tests
const std::string kCommand = "test-command";
const std::string kEol = "\r\n";
const std::string kPrompt = "$ ";
const int kPrivate = 100; // milliseconds
const int kUser = 200; // milliseconds
const int kTimeout = 300; // milliseconds
const int kPostTimeout = 50; // milliseconds
const bool kOncePerUserEnter = true;
const bool kNotOncePerUserEnter = false;
const bool kHasChildProcess = true;
const bool kNoChildProcess = false;

// Mock operations
struct MockOps : public core::system::ProcessOperations
{
   std::vector<std::string> writes;
   std::vector<bool> writesEof;
   bool ptyInterrupted = false;

   Error writeToStdin(const std::string& input, bool eof) override
   {
      writes.push_back(input);
      writesEof.push_back(eof);
      return Success();
   }

   Error ptyInterrupt() override
   {
      ptyInterrupted = true;
      return Success();
   }
   
   Error ptySetSize(int cols, int rows) override
   {
      return Success();
   }
   
   Error terminate() override
   {
      return Success();
   }
   
   PidType getPid() override
   {
      return 0;
   }
};

} // anonymous namespace

// Test fixture for terminal tests that ensures proper cleanup between tests
class TerminalTest : public ::testing::Test {
protected:
   void SetUp() override {
      ops_ = std::make_unique<MockOps>();
   }
   
   void TearDown() override {
      ops_.reset();
   }

   std::unique_ptr<MockOps> ops_;
};

TEST_F(TerminalTest, CommandOutputSuccessfullyParsedAndReturned)
{
   PrivateCommand cmd(kCommand, kPrivate * 2, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   std::string results = "one=two\nthree=four\nfive=six\n";

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));

   // mimic shell echoing back the command
   cmd.output(cmd.getFullCommand());
   cmd.output(kEol);
   cmd.output(cmd.getBOM());
   cmd.output(kEol);
   cmd.output("ignorespace");
   cmd.output(kEol);
   cmd.output(results);

   // verify we can't get results until EOM is seen
   ASSERT_TRUE(cmd.getPrivateOutput().empty());

   // close off the output
   cmd.output(cmd.getEOM());
   cmd.output(kEol);
   cmd.output(kPrompt);

   // trying to capture now should return true because final timeout hasn't expired
   ASSERT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));

   boost::this_thread::sleep(milliseconds(kPostTimeout));

   // trying to capture now should return false, meaning we can get the output
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));

   // verify the captured output
   std::string captureResult = cmd.getPrivateOutput();
   EXPECT_EQ(results, captureResult);

   // can only extract output one time per command
   EXPECT_TRUE(cmd.getPrivateOutput().empty());
}

TEST_F(TerminalTest, BasicAssumptionsAreTrue)
{
   PrivateCommand cmd1(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

   EXPECT_FALSE(cmd1.hasCaptured());
   EXPECT_TRUE(cmd1.getPrivateOutput().empty());
   EXPECT_FALSE(cmd1.output("some output\n"));
   ASSERT_TRUE(cmd1.getPrivateOutput().empty());
   EXPECT_FALSE(cmd1.hasCaptured());

   PrivateCommand cmd2(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   EXPECT_FALSE(cmd2.hasCaptured());
   ASSERT_TRUE(cmd2.getPrivateOutput().empty());
   EXPECT_FALSE(cmd2.output("some output\n"));
   ASSERT_TRUE(cmd2.getPrivateOutput().empty());
   EXPECT_FALSE(cmd2.hasCaptured());
}

TEST_F(TerminalTest, NoCaptureIfTerminalHasChildProcess)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

   EXPECT_FALSE(cmd.onTryCapture(*ops_, kHasChildProcess));
   EXPECT_FALSE(cmd.hasCaptured());
   EXPECT_TRUE(cmd.getPrivateOutput().empty());
}

TEST_F(TerminalTest, NotEnterNoCaptureIfTerminalHasChildProcess)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   EXPECT_FALSE(cmd.onTryCapture(*ops_, kHasChildProcess));
   EXPECT_FALSE(cmd.hasCaptured());
   EXPECT_TRUE(cmd.getPrivateOutput().empty());
}

TEST_F(TerminalTest, NoCaptureIfUserHasntHitEnter)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

   cmd.userInput("partial user command");
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, NoCaptureIfUserHasEnteredACommandButThereIsAChildProcess)
{
   PrivateCommand cmd(kCommand);

   cmd.userInput("user command\n");
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kHasChildProcess));
}

TEST_F(TerminalTest, NotEnterNoCaptureIfUserHasEnteredACommandButThereIsAChildProcess)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   cmd.userInput("user command\n");
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kHasChildProcess));
}

TEST_F(TerminalTest, NoCommandIssuedIfUserHasEnteredACommandAndNoChildProcessButTooSoon)
{
   PrivateCommand cmd(kCommand);

   cmd.userInput("user command\n");
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, NotEnterCommandNotIssuedAfterUserCommandNoChildProcessButTooSoon)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   cmd.userInput("user command\n");
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, CommandIssuedIfUserHasEnteredACommandAndPostUserCommandTimeoutExpired)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

   cmd.userInput("user command\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   ASSERT_TRUE(cmd.hasCaptured());
   EXPECT_EQ(2u, ops_->writes.size());
   EXPECT_TRUE(ops_->writesEof.size() == ops_->writes.size());
   std::string sentCommand = ops_->writes[0];
   EXPECT_TRUE((*sentCommand.rbegin() == '\r' || *sentCommand.rbegin() == '\n'));
}

TEST_F(TerminalTest, NotEnterCommandIssuedUserHasEnteredACommandPostUserCmdTimeoutExpired)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   cmd.userInput("user command\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   EXPECT_EQ(2u, ops_->writes.size());
   EXPECT_TRUE(ops_->writesEof.size() == ops_->writes.size());
   std::string sentCommand = ops_->writes[0];
   EXPECT_TRUE((*sentCommand.rbegin() == '\r' || *sentCommand.rbegin() == '\n'));
}

TEST_F(TerminalTest, NotEnterCommandIssuedImmediatelyWithoutUserCommandEntered)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   EXPECT_EQ(2u, ops_->writes.size());
   EXPECT_TRUE(ops_->writesEof.size() == ops_->writes.size());
}

TEST_F(TerminalTest, CommandNotIssuedIfUserHasntEnteredANewCommandSinceLastPrivateCommand)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

   cmd.userInput("user command one\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   cmd.terminateCapture();

   // no new user command entered, and enough time has passed for the private command delay
   boost::this_thread::sleep(milliseconds(kPrivate * 2));
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, NotEnterCommandReissuedIfPrivateCommandDelayHasExpired)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   cmd.terminateCapture();

   // no new user command entered, but enough time has passed for the private command delay
   boost::this_thread::sleep(milliseconds(kPrivate * 2));
   ASSERT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, CommandNotIssuedIfPrivateCommandIntervalHasntExpired)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

   cmd.userInput("user command one\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   cmd.terminateCapture();

   cmd.userInput("user command two\n");
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, NotEnterCommandNotIssuedIfPrivateCommandIntervalHasntExpired)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   cmd.userInput("user command one\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   cmd.terminateCapture();

   cmd.userInput("user command two\n");
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, CommandIssuedIfPrivateCommandIntervalHasExpired)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

   cmd.userInput("user command one\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   cmd.terminateCapture();

   cmd.userInput("user command two\n");
   boost::this_thread::sleep(milliseconds(kPrivate * 2));
   ASSERT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, NotEnterCommandIssuedIfPrivateCommandIntervalHasExpired)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   cmd.userInput("user command one\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
   cmd.terminateCapture();

   cmd.userInput("user command two\n");
   boost::this_thread::sleep(milliseconds(kPrivate * 2));
   ASSERT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));
}

TEST_F(TerminalTest, PrivateCommandTerminatedIfTakingTooLong)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

   cmd.userInput("user command\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));

   boost::this_thread::sleep(milliseconds(kTimeout * 2));
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
   ASSERT_TRUE(ops_->ptyInterrupted);
   EXPECT_FALSE(cmd.hasCaptured());
}

TEST_F(TerminalTest, NotEnterPrivateCommandTerminatedIfTakingTooLong)
{
   PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));

   boost::this_thread::sleep(milliseconds(kTimeout * 2));
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
   ASSERT_TRUE(ops_->ptyInterrupted);
   EXPECT_FALSE(cmd.hasCaptured());
}

TEST_F(TerminalTest, PrivateCommandStopsIfWrongHistcontrolReturned)
{
   PrivateCommand cmd(kCommand, kPrivate * 2, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

   std::string results = "one=two\nthree=four\nfive=six\n";

   EXPECT_TRUE(cmd.onTryCapture(*ops_, kNoChildProcess));

   // mimic shell echoing back the command
   cmd.output(cmd.getFullCommand());
   cmd.output(kEol);
   cmd.output(cmd.getBOM());
   cmd.output(kEol);
   cmd.output("ignoredups");
   cmd.output(kEol);
   cmd.output(results);
   cmd.output(cmd.getEOM());
   cmd.output(kEol);
   cmd.output(kPrompt);

   boost::this_thread::sleep(milliseconds(kPostTimeout));
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));

   // verify the captured output matches expectation
   std::string captureResult = cmd.getPrivateOutput();
   EXPECT_EQ(results, captureResult);

   // wait then try to do another capture
   boost::this_thread::sleep(milliseconds(kPrivate * 2));
   cmd.userInput("user command\n");
   boost::this_thread::sleep(milliseconds(kUser * 2));
   EXPECT_FALSE(cmd.onTryCapture(*ops_, kNoChildProcess));
   captureResult = cmd.getPrivateOutput();
   EXPECT_TRUE(captureResult.empty());
}

} // namespace tests
} // namespace terminal
} // namespace core
} // namespace rstudio