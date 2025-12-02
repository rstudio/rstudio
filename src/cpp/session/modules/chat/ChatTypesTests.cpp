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
