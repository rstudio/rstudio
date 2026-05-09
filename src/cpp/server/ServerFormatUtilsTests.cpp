/*
 * ServerFormatUtilsTests.cpp
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

#include "ServerFormatUtils.hpp"

#include <gtest/gtest.h>

namespace rstudio {
namespace server {
namespace format_utils {
namespace tests {

TEST(ServerFormatUtilsTest, ExactDayBoundary)
{
   EXPECT_EQ("3 days", formatLoginTimeoutDuration(4320));
}

TEST(ServerFormatUtilsTest, AllThreeUnits)
{
   EXPECT_EQ("2 days, 23 hours, and 59 minutes",
             formatLoginTimeoutDuration(4319));
}

TEST(ServerFormatUtilsTest, HoursAndMinutes)
{
   EXPECT_EQ("1 hour and 30 minutes", formatLoginTimeoutDuration(90));
}

TEST(ServerFormatUtilsTest, MinutesOnly)
{
   EXPECT_EQ("45 minutes", formatLoginTimeoutDuration(45));
}

TEST(ServerFormatUtilsTest, SingleMinute)
{
   EXPECT_EQ("1 minute", formatLoginTimeoutDuration(1));
}

TEST(ServerFormatUtilsTest, SingleHour)
{
   EXPECT_EQ("1 hour", formatLoginTimeoutDuration(60));
}

TEST(ServerFormatUtilsTest, SingleDay)
{
   EXPECT_EQ("1 day", formatLoginTimeoutDuration(1440));
}

TEST(ServerFormatUtilsTest, DaysAndHoursNoMinutes)
{
   EXPECT_EQ("1 day and 1 hour", formatLoginTimeoutDuration(1500));
}

TEST(ServerFormatUtilsTest, DaysAndMinutesNoHours)
{
   EXPECT_EQ("1 day and 1 minute", formatLoginTimeoutDuration(1441));
}

TEST(ServerFormatUtilsTest, HoursOnly)
{
   EXPECT_EQ("2 hours", formatLoginTimeoutDuration(120));
}

TEST(ServerFormatUtilsTest, ZeroReturnsZeroMinutes)
{
   EXPECT_EQ("0 minutes", formatLoginTimeoutDuration(0));
}

TEST(ServerFormatUtilsTest, NegativeReturnsZeroMinutes)
{
   EXPECT_EQ("0 minutes", formatLoginTimeoutDuration(-5));
}

} // namespace tests
} // namespace format_utils
} // namespace server
} // namespace rstudio
