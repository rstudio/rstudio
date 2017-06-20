/*
 * PrivateCommandTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

context("Private Terminal Command Tests")
{
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
   const bool kHasChildProcess = true;
   const bool kNoChildProcess = false;
   const int kPrivate = 10;
   const int kUser = 10;
   const int kTimeout = 10;

   test_that("no capture if terminal has child process")
   {
      PrivateCommand cmd(kCommand);

      expect_false(cmd.hasCaptured());
      expect_false(cmd.onTryCapture(ops, kHasChildProcess));
      expect_false(cmd.hasCaptured());
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("no capture if user is in the middle of typing but hasn't hit <enter>")
   {
      PrivateCommand cmd(kCommand);

      expect_false(cmd.hasCaptured());
      cmd.userInput("partial user command");
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
      expect_false(cmd.hasCaptured());
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("no capture if user is in the middle of typing but hasn't hit <enter> in child proc")
   {
      PrivateCommand cmd(kCommand);

      expect_false(cmd.hasCaptured());
      cmd.userInput("partial user command");
      expect_false(cmd.onTryCapture(ops, kHasChildProcess));
      expect_false(cmd.hasCaptured());
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("no capture if user has entered a command but there is a child process")
   {
      PrivateCommand cmd(kCommand);

      expect_false(cmd.hasCaptured());
      cmd.userInput("user command\n");
      expect_false(cmd.onTryCapture(ops, kHasChildProcess));
      expect_false(cmd.hasCaptured());
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("no command issued if user has entered a command and no child process, but too soon")
   {
      PrivateCommand cmd(kCommand);

      expect_false(cmd.hasCaptured());
      cmd.userInput("user command\n");
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
      expect_false(cmd.hasCaptured());
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("command issued if user has entered a command and post-user-command timeout expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout);

      expect_false(cmd.hasCaptured());
      cmd.userInput("user command\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(cmd.hasCaptured());
      expect_true(ops.writes.size() == 1);
      expect_true(ops.writesEof.size() == ops.writes.size());
      std::string sentCommand = ops.writes[0];
      expect_true(*sentCommand.rbegin() == '\r' || *sentCommand.rbegin() == '\n');
      expect_true(cmd.getPrivateOutput().empty());
   }

   test_that("command not issued if user hasn't entered a new command since last private comand")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout);

      expect_false(cmd.hasCaptured());
      cmd.userInput("user command one\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(cmd.hasCaptured());
      cmd.endCapture();

      // no new user command entered, and enough time has passed for the private command delay
      boost::this_thread::sleep(milliseconds(kPrivate * 2));
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
      expect_false(cmd.hasCaptured());
   }

   test_that("command not issued if private command interval hasn't expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout);

      expect_false(cmd.hasCaptured());
      cmd.userInput("user command one\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(cmd.hasCaptured());
      cmd.endCapture();

      cmd.userInput("user command two\n");
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
      expect_false(cmd.hasCaptured());
   }

   test_that("command issued if private command interval has expired")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout);

      expect_false(cmd.hasCaptured());
      cmd.userInput("user command one\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(cmd.hasCaptured());
      cmd.endCapture();

      cmd.userInput("user command two\n");
      boost::this_thread::sleep(milliseconds(kPrivate * 2));
      expect_true(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(cmd.hasCaptured());
   }

   test_that("private command terminated if taking too long")
   {
      PrivateCommand cmd(kCommand, kPrivate, kUser, kTimeout);

      expect_false(cmd.hasCaptured());
      cmd.userInput("user command\n");
      boost::this_thread::sleep(milliseconds(kUser * 2));

      expect_true(cmd.onTryCapture(ops, kNoChildProcess));

      boost::this_thread::sleep(milliseconds(kTimeout * 2));
      expect_false(cmd.onTryCapture(ops, kNoChildProcess));
      expect_true(ops.ptyInterrupted);
      expect_false(cmd.hasCaptured());
   }
}

} // end namespace tests
} // end namespace terminal
} // end namespace core
} // end namespace rstudio
