/*
 * SessionConsoleProcessTests.cpp
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

#include <session/SessionConsoleProcess.hpp>

#include <boost/lexical_cast.hpp>
#include <tests/TestThat.hpp>

namespace rstudio {
namespace session {
namespace console_process {
namespace tests {

using namespace console_process;

context("queue and fetch input")
{
   core::system::ProcessOptions procOptions;

   boost::shared_ptr<ConsoleProcessInfo> pCPI =
         boost::make_shared<ConsoleProcessInfo>(
            "test caption", "test title", "fakehandle", 9999 /*terminal*/,
            TerminalShell::DefaultShell, console_process::Rpc, "");

   boost::shared_ptr<ConsoleProcess> pCP =
         ConsoleProcess::createTerminalProcess(procOptions, pCPI, false /*enableWebsockets*/);

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

      for (size_t i = 0; i < orig.length(); i++)
      {
         std::string oneChar;
         oneChar.push_back(orig[i]);
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

      expect_true(!orig.compare(result));
   }

   test_that("in-order sequenced input is FIFO")
   {
      const std::string orig = "abcdefghijklmnopqrstuvwxyz";

      for (size_t i = 0; i < orig.length(); i++)
      {
         std::string oneChar;
         oneChar.push_back(orig[i]);
         pCP->enqueInput(ConsoleProcess::Input(i, oneChar));
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

      expect_true(!orig.compare(result));
   }

   test_that("mixed-up input is returned in correct order")
   {
      const std::string expected = "HELLO WORLD!";
      std::vector<ConsoleProcess::Input> input;
      input.push_back(ConsoleProcess::Input(1,  std::string("E")));
      input.push_back(ConsoleProcess::Input(0,  std::string("H")));
      input.push_back(ConsoleProcess::Input(2,  std::string("L")));
      input.push_back(ConsoleProcess::Input(3,  std::string("L")));
      input.push_back(ConsoleProcess::Input(4,  std::string("O")));
      input.push_back(ConsoleProcess::Input(5,  std::string(" ")));
      input.push_back(ConsoleProcess::Input(7,  std::string("O")));
      input.push_back(ConsoleProcess::Input(6,  std::string("W")));
      input.push_back(ConsoleProcess::Input(8,  std::string("R")));
      input.push_back(ConsoleProcess::Input(9,  std::string("L")));
      input.push_back(ConsoleProcess::Input(10, std::string("D")));
      input.push_back(ConsoleProcess::Input(11, std::string("!")));

      for (size_t i = 0; i < input.size(); i++)
      {
         pCP->enqueInput(input[i]);
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

      expect_true(!expected.compare(result));
   }

   test_that("Autoflush kicks in when too much input with unresolved gap")
   {
      std::vector<ConsoleProcess::Input> input;
      std::string expected;

      // intentionally skipping item "0" to prevent pulling from queueA
      int lastAdded = kIgnoreSequence;
      for (int i = 1; i < kAutoFlushLength + 5; i++)
      {
         std::string item = boost::lexical_cast<std::string>(i);
         expected += item;
         input.push_back(ConsoleProcess::Input(i, item));
         lastAdded = i;
      }

      for (size_t i = 0; i < input.size(); i++)
      {
         pCP->enqueInput(input[i]);
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

      expect_true(!expected.compare(result));

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

      expect_true(!result.compare("HELLO WORLD"));
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
            input.push_back(ConsoleProcess::Input(i, item));
         }
      }
      std::string flushText = "FLUSH"; // value not significant
      input.push_back(ConsoleProcess::Input(kFlushSequence, flushText));
      expected += flushText;
      std::string postFlushText = "post-flush"; // value not significant
      input.push_back(ConsoleProcess::Input(0, postFlushText));
      expected += postFlushText;

      for (size_t i = 0; i < input.size(); i++)
      {
         pCP->enqueInput(input[i]);
      }

      std::string result;
      ConsoleProcess::Input next = pCP->dequeInput();
      expect_false(next.empty());
      int itemCount = 1;
      while (!next.empty())
      {
         expect_false(next.interrupt);
         result += next.text;

         if (!next.text.compare(postFlushText))
            expect_true(next.sequence == 0);
         else
            expect_true(next.sequence == kIgnoreSequence);

         next = pCP->dequeInput();
      }

      expect_true(!expected.compare(result));
   }
}

} // namespace tests
} // namespace console_process
} // namespace session
} // namespace rstudio
