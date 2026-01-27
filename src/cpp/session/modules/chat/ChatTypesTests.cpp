/*
 * ChatTypesTests.cpp
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

#include "ChatTypes.hpp"

#include <gtest/gtest.h>

using namespace rstudio::session::modules::chat::types;

TEST(SemanticVersion, ParseValidFullVersions)
{
   SemanticVersion v;

   EXPECT_TRUE(v.parse("1.2.3"));
   EXPECT_EQ(v.major, 1);
   EXPECT_EQ(v.minor, 2);
   EXPECT_EQ(v.patch, 3);
}

TEST(SemanticVersion, ParseVersionsWithVPrefix)
{
   SemanticVersion v;

   EXPECT_TRUE(v.parse("v2.1.0"));
   EXPECT_EQ(v.major, 2);
   EXPECT_EQ(v.minor, 1);
   EXPECT_EQ(v.patch, 0);
}

TEST(SemanticVersion, ParsePartialVersions)
{
   SemanticVersion v1;
   EXPECT_TRUE(v1.parse("3"));
   EXPECT_EQ(v1.major, 3);
   EXPECT_EQ(v1.minor, 0);
   EXPECT_EQ(v1.patch, 0);

   SemanticVersion v2;
   EXPECT_TRUE(v2.parse("2.5"));
   EXPECT_EQ(v2.major, 2);
   EXPECT_EQ(v2.minor, 5);
   EXPECT_EQ(v2.patch, 0);
}

TEST(SemanticVersion, RejectInvalidVersions)
{
   SemanticVersion v;

   EXPECT_FALSE(v.parse(""));           // Empty string
   EXPECT_FALSE(v.parse("abc"));        // Non-numeric
   EXPECT_FALSE(v.parse("1.a.3"));      // Non-numeric minor
   EXPECT_FALSE(v.parse("1.2.x"));      // Non-numeric patch
   EXPECT_FALSE(v.parse("-1.2.3"));     // Negative major
   EXPECT_FALSE(v.parse("1.-2.3"));     // Negative minor
   EXPECT_FALSE(v.parse("1.2.-3"));     // Negative patch
}

TEST(SemanticVersion, CompareMajorVersions)
{
   SemanticVersion v1, v2;

   v1.parse("2.0.0");
   v2.parse("1.9.9");
   EXPECT_TRUE(v1 > v2);

   v1.parse("1.0.0");
   v2.parse("2.0.0");
   EXPECT_FALSE(v1 > v2);
}

TEST(SemanticVersion, CompareMinorVersions)
{
   SemanticVersion v1, v2;

   v1.parse("1.5.0");
   v2.parse("1.4.9");
   EXPECT_TRUE(v1 > v2);

   v1.parse("1.3.0");
   v2.parse("1.4.0");
   EXPECT_FALSE(v1 > v2);
}

TEST(SemanticVersion, ComparePatchVersions)
{
   SemanticVersion v1, v2;

   v1.parse("1.2.5");
   v2.parse("1.2.4");
   EXPECT_TRUE(v1 > v2);

   v1.parse("1.2.3");
   v2.parse("1.2.4");
   EXPECT_FALSE(v1 > v2);
}

TEST(SemanticVersion, EqualVersionsAreNotGreaterThanEachOther)
{
   SemanticVersion v1, v2;

   v1.parse("1.2.3");
   v2.parse("1.2.3");
   EXPECT_FALSE(v1 > v2);
   EXPECT_FALSE(v2 > v1);
}

TEST(SemanticVersion, LessThanOperatorWorksCorrectly)
{
   SemanticVersion v1, v2;

   v1.parse("1.2.3");
   v2.parse("2.0.0");
   EXPECT_TRUE(v1 < v2);
   EXPECT_FALSE(v2 < v1);

   v1.parse("1.2.3");
   v2.parse("1.2.3");
   EXPECT_FALSE(v1 < v2);
}

TEST(SemanticVersion, GreaterThanOrEqualOperatorWorksCorrectly)
{
   SemanticVersion v1, v2;

   v1.parse("2.0.0");
   v2.parse("1.9.9");
   EXPECT_TRUE(v1 >= v2);

   v1.parse("1.2.3");
   v2.parse("1.2.3");
   EXPECT_TRUE(v1 >= v2);

   v1.parse("1.0.0");
   v2.parse("2.0.0");
   EXPECT_FALSE(v1 >= v2);
}

TEST(SemanticVersion, LessThanOrEqualOperatorWorksCorrectly)
{
   SemanticVersion v1, v2;

   v1.parse("1.0.0");
   v2.parse("2.0.0");
   EXPECT_TRUE(v1 <= v2);

   v1.parse("1.2.3");
   v2.parse("1.2.3");
   EXPECT_TRUE(v1 <= v2);

   v1.parse("2.0.0");
   v2.parse("1.9.9");
   EXPECT_FALSE(v1 <= v2);
}

TEST(SemanticVersion, EqualityOperatorWorksCorrectly)
{
   SemanticVersion v1, v2;

   v1.parse("1.2.3");
   v2.parse("1.2.3");
   EXPECT_TRUE(v1 == v2);

   v1.parse("1.2.3");
   v2.parse("1.2.4");
   EXPECT_FALSE(v1 == v2);

   v1.parse("1.2.3");
   v2.parse("1.3.3");
   EXPECT_FALSE(v1 == v2);

   v1.parse("1.2.3");
   v2.parse("2.2.3");
   EXPECT_FALSE(v1 == v2);
}

TEST(SemanticVersion, InequalityOperatorWorksCorrectly)
{
   SemanticVersion v1, v2;

   v1.parse("1.2.3");
   v2.parse("1.2.4");
   EXPECT_TRUE(v1 != v2);

   v1.parse("1.2.3");
   v2.parse("1.2.3");
   EXPECT_FALSE(v1 != v2);
}

// =============================================================================
// RStudioVersion Tests
// =============================================================================

TEST(RStudioVersion, ParseValidDailyBuild)
{
   RStudioVersion v;

   EXPECT_TRUE(v.parse("2026.04.0-daily+172"));
   EXPECT_EQ(v.major, 2026);
   EXPECT_EQ(v.minor, 4);
   EXPECT_EQ(v.patch, 0);
   EXPECT_EQ(v.suffix, "-daily+172");
   EXPECT_EQ(v.dailyBuildNumber, 172);
}

TEST(RStudioVersion, ParseValidReleaseVersion)
{
   RStudioVersion v;

   EXPECT_TRUE(v.parse("2026.04.0"));
   EXPECT_EQ(v.major, 2026);
   EXPECT_EQ(v.minor, 4);
   EXPECT_EQ(v.patch, 0);
   EXPECT_EQ(v.suffix, "");
   EXPECT_EQ(v.dailyBuildNumber, -1);
}

TEST(RStudioVersion, ParseValidPreviewVersion)
{
   RStudioVersion v;

   EXPECT_TRUE(v.parse("2026.04.0-preview"));
   EXPECT_EQ(v.major, 2026);
   EXPECT_EQ(v.minor, 4);
   EXPECT_EQ(v.patch, 0);
   EXPECT_EQ(v.suffix, "-preview");
   EXPECT_EQ(v.dailyBuildNumber, -1);
}

TEST(RStudioVersion, ParseValidDevBuild)
{
   RStudioVersion v;

   EXPECT_TRUE(v.parse("2026.01.999-dev+999"));
   EXPECT_EQ(v.major, 2026);
   EXPECT_EQ(v.minor, 1);
   EXPECT_EQ(v.patch, 999);
   EXPECT_EQ(v.suffix, "-dev+999");
   EXPECT_EQ(v.dailyBuildNumber, 999);
}

TEST(RStudioVersion, ParseValidHourlyBuild)
{
   RStudioVersion v;

   EXPECT_TRUE(v.parse("2026.04.0-hourly+178"));
   EXPECT_EQ(v.major, 2026);
   EXPECT_EQ(v.minor, 4);
   EXPECT_EQ(v.patch, 0);
   EXPECT_EQ(v.suffix, "-hourly+178");
   EXPECT_EQ(v.dailyBuildNumber, 178);
}

TEST(RStudioVersion, ParseReleaseBuildWithNumber)
{
   RStudioVersion v;

   EXPECT_TRUE(v.parse("2026.01.0+392"));
   EXPECT_EQ(v.major, 2026);
   EXPECT_EQ(v.minor, 1);
   EXPECT_EQ(v.patch, 0);
   EXPECT_EQ(v.suffix, "+392");
   EXPECT_EQ(v.dailyBuildNumber, 392);
}

TEST(RStudioVersion, RejectInvalidVersions)
{
   RStudioVersion v;

   EXPECT_FALSE(v.parse(""));              // Empty string
   EXPECT_FALSE(v.parse("2026.04"));       // Missing patch
   EXPECT_FALSE(v.parse("abc.04.0"));      // Non-numeric major
   EXPECT_FALSE(v.parse("2026.abc.0"));    // Non-numeric minor
   EXPECT_FALSE(v.parse("2026.04.abc"));   // Non-numeric patch
}

TEST(RStudioVersion, CompareDailyBuildNumbers)
{
   RStudioVersion v1, v2;

   v1.parse("2026.04.0-daily+172");
   v2.parse("2026.04.0-daily+100");
   EXPECT_TRUE(v1 > v2);
   EXPECT_TRUE(v2 < v1);

   v1.parse("2026.04.0-daily+100");
   v2.parse("2026.04.0-daily+172");
   EXPECT_TRUE(v1 < v2);
   EXPECT_TRUE(v2 > v1);
}

TEST(RStudioVersion, CompareMajorVersions)
{
   RStudioVersion v1, v2;

   v1.parse("2027.01.0-daily+1");
   v2.parse("2026.12.0-daily+999");
   EXPECT_TRUE(v1 > v2);
}

TEST(RStudioVersion, CompareMinorVersions)
{
   RStudioVersion v1, v2;

   v1.parse("2026.05.0-daily+1");
   v2.parse("2026.04.0-daily+999");
   EXPECT_TRUE(v1 > v2);
}

TEST(RStudioVersion, ComparePatchVersions)
{
   RStudioVersion v1, v2;

   v1.parse("2026.04.1-daily+1");
   v2.parse("2026.04.0-daily+999");
   EXPECT_TRUE(v1 > v2);
}

TEST(RStudioVersion, DailyBuildOlderThanRelease)
{
   RStudioVersion daily, release;

   daily.parse("2026.04.0-daily+999");
   release.parse("2026.04.0");

   // Daily builds are considered older than release with same base version
   EXPECT_TRUE(daily < release);
   EXPECT_TRUE(release > daily);
}

TEST(RStudioVersion, DevBuildOlderThanRelease)
{
   RStudioVersion dev, release;

   dev.parse("2026.04.0-dev+999");
   release.parse("2026.04.0");

   // Dev builds are considered older than release with same base version
   EXPECT_TRUE(dev < release);
   EXPECT_TRUE(release > dev);
}

TEST(RStudioVersion, PreviewOlderThanRelease)
{
   RStudioVersion preview, release;

   preview.parse("2026.04.0-preview");
   release.parse("2026.04.0");

   // Preview builds are older than release with same base version
   EXPECT_TRUE(preview < release);
   EXPECT_TRUE(release > preview);
}

TEST(RStudioVersion, DailyOlderThanPreview)
{
   RStudioVersion daily, preview;

   daily.parse("2026.04.0-daily+999");
   preview.parse("2026.04.0-preview");

   // Daily/dev builds (with build numbers) are older than preview (no build number)
   EXPECT_TRUE(daily < preview);
   EXPECT_TRUE(preview > daily);
}

TEST(RStudioVersion, DevAndDailyCompareByBuildNumber)
{
   RStudioVersion dev, daily;

   // Both have build numbers, so compare by build number
   dev.parse("2026.04.0-dev+100");
   daily.parse("2026.04.0-daily+200");

   EXPECT_TRUE(dev < daily);
   EXPECT_TRUE(daily > dev);

   // Reverse
   dev.parse("2026.04.0-dev+300");
   daily.parse("2026.04.0-daily+200");

   EXPECT_TRUE(daily < dev);
   EXPECT_TRUE(dev > daily);
}

TEST(RStudioVersion, DailyOlderThanReleaseWithBuildNumber)
{
   RStudioVersion daily, release;

   daily.parse("2026.04.0-daily+999");
   release.parse("2026.04.0+392");

   // Daily builds are older than release builds with same base version
   EXPECT_TRUE(daily < release);
   EXPECT_TRUE(release > daily);
}

TEST(RStudioVersion, CompareReleaseBuildNumbers)
{
   RStudioVersion r1, r2;

   r1.parse("2026.01.0+392");
   r2.parse("2026.01.0+500");

   EXPECT_TRUE(r1 < r2);
   EXPECT_TRUE(r2 > r1);

   // Test numeric comparison (not lexical)
   r1.parse("2026.01.0+50");
   r2.parse("2026.01.0+392");

   EXPECT_TRUE(r1 < r2);  // 50 < 392 numerically
   EXPECT_TRUE(r2 > r1);
}

TEST(RStudioVersion, EqualVersions)
{
   RStudioVersion v1, v2;

   v1.parse("2026.04.0-daily+172");
   v2.parse("2026.04.0-daily+172");
   EXPECT_TRUE(v1 == v2);
   EXPECT_FALSE(v1 != v2);
   EXPECT_FALSE(v1 < v2);
   EXPECT_FALSE(v1 > v2);
   EXPECT_TRUE(v1 <= v2);
   EXPECT_TRUE(v1 >= v2);
}

TEST(RStudioVersion, ComparisonOperatorsComplete)
{
   RStudioVersion v1, v2;

   // Test all comparison operators
   v1.parse("2026.04.0-daily+100");
   v2.parse("2026.04.0-daily+200");

   EXPECT_TRUE(v1 < v2);
   EXPECT_FALSE(v1 > v2);
   EXPECT_TRUE(v1 <= v2);
   EXPECT_FALSE(v1 >= v2);
   EXPECT_FALSE(v1 == v2);
   EXPECT_TRUE(v1 != v2);
}
