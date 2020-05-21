/*
 * SessionConsoleProcessTests.cpp
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

#include <session/SessionConsoleProcess.hpp>

#include <gsl/gsl>

#include <boost/lexical_cast.hpp>
#include <tests/TestThat.hpp>

namespace rstudio {
namespace session {
namespace console_process {
namespace tests {

using namespace console_process;

test_context("queue and fetch input")
{
   core::system::ProcessOptions procOptions;
   core::FilePath cwd("/usr/local");

   boost::shared_ptr<ConsoleProcessInfo> pCPI =
         boost::shared_ptr<ConsoleProcessInfo>(new ConsoleProcessInfo(
            "test caption", "test title", "fakehandle", 9999 /*terminal*/,
            TerminalShell::ShellType::Default, false /*altBuffer*/, cwd,
            core::system::kDefaultCols, core::system::kDefaultRows,
            false /*zombie*/, false /*trackEnv*/));

   std::string command;
   boost::shared_ptr<ConsoleProcess> pCP =
         ConsoleProcess::createTerminalProcess(command, procOptions, pCPI, false /*enableWebsockets*/);

   test_that("empty queue returns nothing")
   {
      ConsoleProcess::Input next = pCP->dequeInput();
      expect_true(next.empty());
   }

   test_that("can queue and deque an interrupt")
   {
      ConsoleProcess::Input interrupt;
      interrupt.interrupt = true;
      pCP->enqueInput(interrupt);
      ConsoleProcess::Input next = pCP->dequeInput();
      expect_false(next.empty());
      expect_true(next.interrupt);
      next = pCP->dequeInput();
      expect_true(next.empty());
   }

   test_that("unsequenced input is FIFO")
   {
      const std::string orig = "abcdefghijklmnopqrstuvwxyz";

      for (char ch : orig)
      {
         std::string oneChar;
         oneChar.push_back(ch);
         pCP->enqueInput(ConsoleProcess::Input(oneChar));
      }

      std::string result;
      ConsoleProcess::Input next = pCP->dequeInput();
      while (!next.empty())
      {
         expect_false(next.interrupt);
         expect_true(next.sequence == kIgnoreSequence);
         result += next.text;
         next = pCP->dequeInput();
      }

      expect_true(orig == result);
   }

   test_that("in-order sequenced input is FIFO")
   {
      const std::string orig = "abcdefghijklmnopqrstuvwxyz";

      for (size_t i = 0; i < orig.length(); i++)
      {
         std::string oneChar;
         oneChar.push_back(orig[i]);
         pCP->enqueInput(ConsoleProcess::Input(gsl::narrow_cast<int>(i), oneChar));
      }

      std::string result;
      ConsoleProcess::Input next = pCP->dequeInput();
      int seq = 0;
      while (!next.empty())
      {
         expect_false(next.interrupt);
         expect_true(next.sequence == seq++);
         result += next.text;
         next = pCP->dequeInput();
      }

      expect_true(orig == result);
   }

   test_that("mixed-up input is returned in correct order")
   {
      const std::string expected = "HELLO WORLD!";
      std::vector<ConsoleProcess::Input> input;
      input.emplace_back(1,  std::string("E"));
      input.emplace_back(0,  std::string("H"));
      input.emplace_back(2,  std::string("L"));
      input.emplace_back(3,  std::string("L"));
      input.emplace_back(4,  std::string("O"));
      input.emplace_back(5,  std::string(" "));
      input.emplace_back(7,  std::string("O"));
      input.emplace_back(6,  std::string("W"));
      input.emplace_back(8,  std::string("R"));
      input.emplace_back(9,  std::string("L"));
      input.emplace_back(10, std::string("D"));
      input.emplace_back(11, std::string("!"));

      for (const auto &cpi : input)
      {
         pCP->enqueInput(cpi);
      }

      std::string result;
      ConsoleProcess::Input next = pCP->dequeInput();
      int seq = 0;
      while (!next.empty())
      {
         expect_false(next.interrupt);
         expect_true(next.sequence == seq++);
         result += next.text;
         next = pCP->dequeInput();
      }

      expect_true(expected == result);
   }

   test_that("Autoflush kicks in when too much input with unresolved gap")
   {
      std::vector<ConsoleProcess::Input> input;
      std::string expected;

      // intentionally skipping item "0" to prevent pulling from queueA
      int lastAdded = kIgnoreSequence;
      for (size_t i = 1; i < kAutoFlushLength + 5; i++)
      {
         std::string item = boost::lexical_cast<std::string>(i);
         expected += item;
         input.emplace_back(gsl::narrow_cast<int>(i), item);
         lastAdded = gsl::narrow_cast<int>(i);
      }

      for (const auto &cpi : input)
      {
         pCP->enqueInput(cpi);
      }

      std::string result;
      ConsoleProcess::Input next = pCP->dequeInput();
      expect_false(next.empty());
      while (!next.empty())
      {
         expect_false(next.interrupt);
         result += next.text;
         next = pCP->dequeInput();
      }

      expect_true(expected == result);

      // make sure we can resume adding and removing
      pCP->enqueInput(ConsoleProcess::Input(lastAdded + 1, "HELLO"));
      pCP->enqueInput(ConsoleProcess::Input(lastAdded + 2, " WORLD"));

      result.clear();
      next = pCP->dequeInput();
      expect_false(next.empty());
      while (!next.empty())
      {
         result += next.text;
         next = pCP->dequeInput();
      }

      expect_true(result == "HELLO WORLD");
   }

   test_that("Explicit flush returns input with gaps and resets sequence count")
   {
      std::vector<ConsoleProcess::Input> input;
      std::string expected;

      // intentionally skipping item "3" to prevent pulling from queue
      for (int i = 1; i < 10; i++)
      {
         if (i != 3)
         {
            std::string item = boost::lexical_cast<std::string>(i);
            expected += item;
            input.emplace_back(i, item);
         }
      }
      std::string flushText = "FLUSH"; // value not significant
      input.emplace_back(kFlushSequence, flushText);
      expected += flushText;
      std::string postFlushText = "post-flush"; // value not significant
      input.emplace_back(0, postFlushText);
      expected += postFlushText;

      for (const auto &cpi : input)
      {
         pCP->enqueInput(cpi);
      }

      std::string result;
      ConsoleProcess::Input next = pCP->dequeInput();
      expect_false(next.empty());
      while (!next.empty())
      {
         expect_false(next.interrupt);
         result += next.text;

         if (next.text == postFlushText)
            expect_true(next.sequence == 0);
         else
            expect_true(next.sequence == kIgnoreSequence);

         next = pCP->dequeInput();
      }

      expect_true(expected == result);
   }
}

} // namespace tests
} // namespace console_process
} // namespace session
} // namespace rstudio
