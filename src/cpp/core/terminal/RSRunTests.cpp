/*
 * RSRunTests.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <core/terminal/RSRunCmd.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace terminal {
namespace tests {

namespace {

} // anonymous namespace

context("RSRun Terminal Handling")
{
   test_that("initial parse state is normal")
   {
      RSRunCmd runCmd;
      expect_true(runCmd.getParseState() == RSRunCmd::ParseState::normal);
   }

   test_that("empty input is a no-op")
   {
      RSRunCmd runCmd;
      RSRunCmd::ParseState initialState = runCmd.getParseState();
      std::string input;
      std::string result = runCmd.processESC(input);
      expect_true(result == input);
      expect_true(initialState == runCmd.getParseState());
   }
   
   test_that("simple input with no ESC codes is a no-op")
   {
      RSRunCmd runCmd;
      RSRunCmd::ParseState initialState = runCmd.getParseState();
      std::string input = "Hello World, here is some simple text for you!";
      std::string result = runCmd.processESC(input);
      expect_true(result == input);
      expect_true(initialState == runCmd.getParseState());
   }

//   test_that("input containing a single ESC sequence parses")
//   {
//      RSRunCmd runCmd;
//      RSRunCmd::ParseState initialState = runCmd.getParseState();
//
//      std::string pipeId = "abcd";
//      std::string payload = "getwd()";
//      std::string input = RSRunCmd::createESC(pipeId, payload);
//      std::string result = runCmd.processESC(input);
//
//      expect_true(result.empty());
//      //expect_true(initialState == runCmd.getParseState());
//   }
}

} // end namespace tests
} // end namespace terminal
} // end namespace core
} // end namespace rstudio
