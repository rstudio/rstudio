/*
 * ChatLoggingTests.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "ChatLogging.hpp"

#include <tests/TestThat.hpp>

using namespace rstudio::session::modules::chat::logging;

test_context("ChatLogging")
{
   test_that("GetLogLevelPriority returns correct priorities for all levels")
   {
      expect_equal(getLogLevelPriority("trace"), 0);
      expect_equal(getLogLevelPriority("debug"), 1);
      expect_equal(getLogLevelPriority("info"), 2);
      expect_equal(getLogLevelPriority("warn"), 3);
      expect_equal(getLogLevelPriority("error"), 4);
      expect_equal(getLogLevelPriority("fatal"), 5);
   }

   test_that("GetLogLevelPriority treats unknown levels as highest priority")
   {
      expect_equal(getLogLevelPriority("unknown"), 999);
      expect_equal(getLogLevelPriority("invalid"), 999);
      expect_equal(getLogLevelPriority(""), 999);
      expect_equal(getLogLevelPriority("TRACE"), 999); // Case-sensitive
   }

   test_that("ShouldLogBackendMessage returns true for non-logger/log notifications")
   {
      // Regular notification
      std::string msg = R"({"jsonrpc":"2.0","method":"other/notification","params":{}})";
      expect_true(shouldLogBackendMessage(msg));

      // Request (has id)
      msg = R"({"jsonrpc":"2.0","id":1,"method":"some/method","params":{}})";
      expect_true(shouldLogBackendMessage(msg));
   }

   test_that("ShouldLogBackendMessage returns true for malformed JSON")
   {
      expect_true(shouldLogBackendMessage("not json"));
      expect_true(shouldLogBackendMessage("{invalid"));
      expect_true(shouldLogBackendMessage(""));
   }

   test_that("ShouldLogBackendMessage filters logger/log at level 2")
   {
      // Save original level
      int originalLevel = chatLogLevel();

      // Set to level 2
      setChatLogLevel(2);

      std::string msg = R"({"jsonrpc":"2.0","method":"logger/log","params":{"level":"info","message":"test"}})";
      expect_false(shouldLogBackendMessage(msg));

      // Restore original level
      setChatLogLevel(originalLevel);
   }

   test_that("ShouldLogBackendMessage shows logger/log at level 3+")
   {
      // Save original level
      int originalLevel = chatLogLevel();
      std::string originalMinLevel = getBackendMinLogLevel();

      // Set to level 3 and backend min level to error
      setChatLogLevel(3);
      setBackendMinLogLevel("error");

      // Error level should be shown
      std::string errorMsg = R"({"jsonrpc":"2.0","method":"logger/log","params":{"level":"error","message":"test"}})";
      expect_true(shouldLogBackendMessage(errorMsg));

      // Debug level should not be shown (below error threshold)
      std::string debugMsg = R"({"jsonrpc":"2.0","method":"logger/log","params":{"level":"debug","message":"test"}})";
      expect_false(shouldLogBackendMessage(debugMsg));

      // Restore original levels
      setChatLogLevel(originalLevel);
      setBackendMinLogLevel(originalMinLevel);
   }
}
