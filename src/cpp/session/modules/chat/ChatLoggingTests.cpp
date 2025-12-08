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

#include <gtest/gtest.h>

using namespace rstudio::session::modules::chat::logging;

TEST(ChatLogging, GetLogLevelPriorityReturnsCorrectPrioritiesForAllLevels)
{
   EXPECT_EQ(getLogLevelPriority("trace"), 0);
   EXPECT_EQ(getLogLevelPriority("debug"), 1);
   EXPECT_EQ(getLogLevelPriority("info"), 2);
   EXPECT_EQ(getLogLevelPriority("warn"), 3);
   EXPECT_EQ(getLogLevelPriority("error"), 4);
   EXPECT_EQ(getLogLevelPriority("fatal"), 5);
}

TEST(ChatLogging, GetLogLevelPriorityTreatsUnknownLevelsAsHighestPriority)
{
   EXPECT_EQ(getLogLevelPriority("unknown"), 999);
   EXPECT_EQ(getLogLevelPriority("invalid"), 999);
   EXPECT_EQ(getLogLevelPriority(""), 999);
   EXPECT_EQ(getLogLevelPriority("TRACE"), 999); // Case-sensitive
}

TEST(ChatLogging, ShouldLogBackendMessageReturnsTrueForNonLoggerLogNotifications)
{
   // Regular notification
   std::string msg = R"({"jsonrpc":"2.0","method":"other/notification","params":{}})";
   EXPECT_TRUE(shouldLogBackendMessage(msg));

   // Request (has id)
   msg = R"({"jsonrpc":"2.0","id":1,"method":"some/method","params":{}})";
   EXPECT_TRUE(shouldLogBackendMessage(msg));
}

TEST(ChatLogging, ShouldLogBackendMessageReturnsTrueForMalformedJson)
{
   EXPECT_TRUE(shouldLogBackendMessage("not json"));
   EXPECT_TRUE(shouldLogBackendMessage("{invalid"));
   EXPECT_TRUE(shouldLogBackendMessage(""));
}

TEST(ChatLogging, ShouldLogBackendMessageFiltersLoggerLogAtLevel2)
{
   // Save original level
   int originalLevel = chatLogLevel();

   // Set to level 2
   setChatLogLevel(2);

   std::string msg = R"({"jsonrpc":"2.0","method":"logger/log","params":{"level":"info","message":"test"}})";
   EXPECT_FALSE(shouldLogBackendMessage(msg));

   // Restore original level
   setChatLogLevel(originalLevel);
}

TEST(ChatLogging, ShouldLogBackendMessageShowsLoggerLogAtLevel3Plus)
{
   // Save original level
   int originalLevel = chatLogLevel();
   std::string originalMinLevel = getBackendMinLogLevel();

   // Set to level 3 and backend min level to error
   setChatLogLevel(3);
   setBackendMinLogLevel("error");

   // Error level should be shown
   std::string errorMsg = R"({"jsonrpc":"2.0","method":"logger/log","params":{"level":"error","message":"test"}})";
   EXPECT_TRUE(shouldLogBackendMessage(errorMsg));

   // Debug level should not be shown (below error threshold)
   std::string debugMsg = R"({"jsonrpc":"2.0","method":"logger/log","params":{"level":"debug","message":"test"}})";
   EXPECT_FALSE(shouldLogBackendMessage(debugMsg));

   // Restore original levels
   setChatLogLevel(originalLevel);
   setBackendMinLogLevel(originalMinLevel);
}
