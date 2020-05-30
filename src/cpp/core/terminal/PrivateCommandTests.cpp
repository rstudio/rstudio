/*
 * PrivateCommandTests.cpp
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

#include <core/terminal/PrivateCommand.hpp>

#include <vector>

#include <tests/TestThat.hpp>

#include <core/BoostThread.hpp>

using namespace boost::posix_time;
using namespace boost::system;

namespace rstudio {
namespace core {
namespace terminal {
namespace tests {

namespace {

} // anonymous namespace

test_context("Private Terminal Command Tests")
{
   // ProcessOptions impl that tracks calls
   class OpsHarness : public system::ProcessOperations
   {
   public:
      OpsHarness() : colsSet(0), rowsSet(0), ptyInterrupted(false), terminated(false), pid(0)
      {
      }

      virtual Error writeToStdin(const std::string& input, bool eof)
      {
         writes.push_back(input);
         writesEof.push_back(eof);
         return Success();
      }

      virtual Error ptySetSize(int cols, int rows)
      {
         colsSet = cols;
         rowsSet = rows;
         return Success();
      }

      virtual Error ptyInterrupt()
      {
         ptyInterrupted = true;
         return Success();
      }

      virtual Error terminate()
      {
         terminated = true;
         return Success();
      }

      virtual PidType getPid()
      {
         return pid;
      }

      int colsSet;
      int rowsSet;
      std::vector<std::string> writes;
      std::vector<bool> writesEof;
      bool ptyInterrupted;
      bool terminated;
      int pid;

   } ops;

   const std::string kCommand = "sample_command";
   const std::string kEol = "\r\n";
   const std::string kPrompt = "fake-prompt someuser$ ";
   const bool kHasChildProcess = true;
   const bool kNoChildProcess = false;
   const int kPrivate = 25; // min delay between private commands
   const int kUser = 25; // min delay after user command
   const int kTimeout = 25; // private command timeout
   const int kPostTimeout = 25; // how long to suppress output after private command is done

   const bool kOncePerUserEnter = true;
   const bool kNotOncePerUserEnter = false;

   // this tests the overall functionality of the expected usage pattern
   test_that("command output successfully parsed and returned")
   {
      PrivateCommand cmd(kCommand, kPrivate * 2, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      std::string results = "one=two\nthree=four\nfive=six\n";

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));

      // mimic shell echoing back the command
      cmd.output(cmd.getFullCommand());
      cmd.output(kEol);
      cmd.output(cmd.getBOM());
      cmd.output(kEol);
      cmd.output("ignorespace");
      cmd.output(kEol);
      cmd.output(results);

      // verify we can't get results until EOM is seen
      expect_true(cmd.getPrivateOutput().empty());

      // close off the output
      cmd.output(cmd.getEOM());
      cmd.output(kEol);
      cmd.output(kPrompt);

      // trying to capture now should return true because final timeout hasn't expired
      expect_true(cmd.onTryCapture(ops, kNoChildProcess));

      boost::this_thread::sleep(milliseconds(kPostTimeout));

      // trying to capture now should return false, meaning we can get the output
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));

      // verify the captured output
      std::string captureResult = cmd.getPrivateOutput();
      expect_true(captureResult == results);

      // can only extract output one time per command
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("basic assumptions are true")
   {
      PrivateCommand cmd1(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

      expect_false(cmd1.hasCaptured());
      expect_true(cmd1.getPrivateOutput().empty());
      expect_false(cmd1.output("some output\n"));
      expect_true(cmd1.getPrivateOutput().empty());
      expect_false(cmd1.hasCaptured());

      PrivateCommand cmd2(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      expect_false(cmd2.hasCaptured());
      expect_true(cmd2.getPrivateOutput().empty());
      expect_false(cmd2.output("some output\n"));
      expect_true(cmd2.getPrivateOutput().empty());
      expect_false(cmd2.hasCaptured());
   }

   test_that("no capture if terminal has child process")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

      expect_false(cmd.onTryCapture(ops, kHasChildProcess));
      expect_false(cmd.hasCaptured());
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("(NotEnter) no capture if terminal has child process")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      expect_false(cmd.onTryCapture(ops, kHasChildProcess));
      expect_false(cmd.hasCaptured());
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("no capture if user hasn't hit <enter>")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

      cmd.userInput("partial user command");
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("no capture if user has entered a command but there is a child process")
   {
      PrivateCommand cmd(kCommand);

      cmd.userInput("user command\n");
      expect_false(cmd.onTryCapture(ops, kHasChildProcess));
   }

   test_that("(NotEnter) no capture if user has entered a command but there is a child process")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      cmd.userInput("user command\n");
      expect_false(cmd.onTryCapture(ops, kHasChildProcess));
   }

   test_that("no command issued if user has entered a command and no child process, but too soon")
   {
      PrivateCommand cmd(kCommand);

      cmd.userInput("user command\n");
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("(NotEnter) command not issued after user command, no child process, but too soon")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      cmd.userInput("user command\n");
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("command issued if user has entered a command and post-user-command timeout expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

      cmd.userInput("user command\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(cmd.hasCaptured());
      expect_true(ops.writes.size() == 1);
      expect_true(ops.writesEof.size() == ops.writes.size());
      std::string sentCommand = ops.writes[0];
      expect_true(*sentCommand.rbegin() == '\r' || *sentCommand.rbegin() == '\n');
   }

   test_that("(NotEnter) command issued, user has entered a command, post-user-cmd timeout expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      cmd.userInput("user command\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(ops.writes.size() == 1);
      expect_true(ops.writesEof.size() == ops.writes.size());
      std::string sentCommand = ops.writes[0];
      expect_true(*sentCommand.rbegin() == '\r' || *sentCommand.rbegin() == '\n');
   }

   test_that("(NotEnter) command issued immediately w/o user command entered")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(ops.writes.size() == 1);
      expect_true(ops.writesEof.size() == ops.writes.size());
   }

   test_that("command not issued if user hasn't entered a new command since last private comand")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

      cmd.userInput("user command one\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      cmd.terminateCapture();

      // no new user command entered, and enough time has passed for the private command delay
      boost::this_thread::sleep(milliseconds(kPrivate * 2));
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("(NotEnter) command reissued if private command delay has expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      cmd.terminateCapture();

      // no new user command entered, but enough time has passed for the private command delay
      boost::this_thread::sleep(milliseconds(kPrivate * 2));
      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("command not issued if private command interval hasn't expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

      cmd.userInput("user command one\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      cmd.terminateCapture();

      cmd.userInput("user command two\n");
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("(NotEnter) command not issued if private command interval hasn't expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      cmd.userInput("user command one\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      cmd.terminateCapture();

      cmd.userInput("user command two\n");
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("command issued if private command interval has expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

      cmd.userInput("user command one\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      cmd.terminateCapture();

      cmd.userInput("user command two\n");
      boost::this_thread::sleep(milliseconds(kPrivate * 2));
      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("(NotEnter) command issued if private command interval has expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      cmd.userInput("user command one\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      cmd.terminateCapture();

      cmd.userInput("user command two\n");
      boost::this_thread::sleep(milliseconds(kPrivate * 2));
      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
   }

   test_that("private command terminated if taking too long")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kOncePerUserEnter);

      cmd.userInput("user command\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));

      boost::this_thread::sleep(milliseconds(kTimeout * 2));
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(ops.ptyInterrupted);
      expect_false(cmd.hasCaptured());
   }

   test_that("(NotEnter) private command terminated if taking too long")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));

      boost::this_thread::sleep(milliseconds(kTimeout * 2));
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(ops.ptyInterrupted);
      expect_false(cmd.hasCaptured());
   }

   test_that("private command stops if wrong HISTCONTROL returned")
   {
      PrivateCommand cmd(kCommand, kPrivate * 2, kUser, kTimeout, kPostTimeout, kNotOncePerUserEnter);

      std::string results = "one=two\nthree=four\nfive=six\n";

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));

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
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));

      // verify the captured output matches expectation
      std::string captureResult = cmd.getPrivateOutput();
      expect_true(captureResult == results);

      // wait then try to do another capture
      boost::this_thread::sleep(milliseconds(kPrivate * 2));
      cmd.userInput("user command\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
      captureResult = cmd.getPrivateOutput();
      expect_true(captureResult.empty());
   }
}

} // end namespace tests
} // end namespace terminal
} // end namespace core
} // end namespace rstudio
