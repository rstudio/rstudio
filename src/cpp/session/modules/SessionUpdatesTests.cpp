/*
 * SessionUpdatesTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <gtest/gtest.h>

#include "SessionUpdates.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace updates {
namespace {

using namespace core;

TEST(SessionUpdatesTests, ParsesKeyValueLines)
{
   std::string output =
         "update-version=2026.09.1+401\n"
         "update-url=https://posit.co/download/?os=mac&arch=arm64\n"
         "update-urgent=0";

   json::Object result = parseUpdateCheckOutput(output);
   ASSERT_EQ(result.getSize(), 3u);
   EXPECT_EQ(result["update-version"].getString(), "2026.09.1+401");
   EXPECT_EQ(result["update-url"].getString(), "https://posit.co/download/?os=mac&arch=arm64");
   EXPECT_EQ(result["update-urgent"].getString(), "0");
}

TEST(SessionUpdatesTests, SkipsLinesWithoutKey)
{
   // a URL-decoded value that contains a newline arrives as several lines;
   // the lines after the first have no '=' and must not become keys
   std::string output =
         "update-message=first line\n"
         "second line\n"
         "=no key\n"
         "\n"
         "update-urgent=1\n";

   json::Object result = parseUpdateCheckOutput(output);
   ASSERT_EQ(result.getSize(), 2u);
   EXPECT_EQ(result["update-message"].getString(), "first line");
   EXPECT_EQ(result["update-urgent"].getString(), "1");
   EXPECT_FALSE(result.hasMember("second line"));
   EXPECT_FALSE(result.hasMember(""));
}

TEST(SessionUpdatesTests, EmptyOutputGivesEmptyObject)
{
   EXPECT_EQ(parseUpdateCheckOutput("").getSize(), 0u);
}

} // anonymous namespace
} // namespace updates
} // namespace modules
} // namespace session
} // namespace rstudio
