/*
 * DataViewerTests.cpp
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

#include <string>

#include "DataViewer.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace data {
namespace viewer {
namespace {

// isSearchSubset: the global search is a bare literal substring query, so a
// new search's rows are a subset of the working copy's whenever the working
// search occurs within the new one.

TEST(DataViewerTest, SearchSubset_IdenticalSearch)
{
   EXPECT_TRUE(detail::isSearchSubset("abc", "abc"));
   EXPECT_TRUE(detail::isSearchSubset("", ""));
}

TEST(DataViewerTest, SearchSubset_ExtendedSearchIsSubset)
{
   // typing more characters can only narrow the matches
   EXPECT_TRUE(detail::isSearchSubset("abc", "abcd"));
   EXPECT_TRUE(detail::isSearchSubset("walnut", "walnuts"));
}

TEST(DataViewerTest, SearchSubset_ContainedSearchIsSubset)
{
   // any row containing "xabcy" contains "abc", wherever it was typed
   EXPECT_TRUE(detail::isSearchSubset("abc", "xabcy"));
}

TEST(DataViewerTest, SearchSubset_EmptySearchIsSupersetOfAll)
{
   // a working copy built with no search holds every (filtered) row
   EXPECT_TRUE(detail::isSearchSubset("", "anything"));
}

TEST(DataViewerTest, SearchSubset_BackspacedSearchIsNotSubset)
{
   EXPECT_FALSE(detail::isSearchSubset("abcd", "abc"));
   EXPECT_FALSE(detail::isSearchSubset("abc", ""));
}

TEST(DataViewerTest, SearchSubset_FilterLookalikeIsLiteralText)
{
   // a search that merely looks like a "type|value" filter must be compared
   // as text: rows containing "numeric|12_18" are unrelated to rows
   // containing "numeric|10_20", even though 12-18 is inside 10-20
   EXPECT_FALSE(detail::isSearchSubset("numeric|10_20", "numeric|12_18"));
   EXPECT_TRUE(detail::isSearchSubset("numeric|1", "numeric|12_18"));
}

// isFilterSubset: column filters are structured "type|value" strings.

TEST(DataViewerTest, FilterSubset_IdenticalFilter)
{
   EXPECT_TRUE(detail::isFilterSubset("character|abc", "character|abc"));
   EXPECT_TRUE(detail::isFilterSubset("", ""));
}

TEST(DataViewerTest, FilterSubset_CharacterExtensionIsSubset)
{
   EXPECT_TRUE(detail::isFilterSubset("character|walnut", "character|walnuts"));
   EXPECT_FALSE(detail::isFilterSubset("character|walnuts", "character|walnut"));
}

TEST(DataViewerTest, FilterSubset_NumericRangeInclusion)
{
   EXPECT_TRUE(detail::isFilterSubset("numeric|2_30", "numeric|5_10"));
   EXPECT_FALSE(detail::isFilterSubset("numeric|5_10", "numeric|2_30"));
}

TEST(DataViewerTest, FilterSubset_FactorMustBeIdentical)
{
   EXPECT_TRUE(detail::isFilterSubset("factor|2", "factor|2"));
   EXPECT_FALSE(detail::isFilterSubset("factor|2", "factor|3"));
}

TEST(DataViewerTest, FilterSubset_UnparseableFilterIsNotSubset)
{
   // bare strings (no "type|value" separator) can't be proven subsets
   EXPECT_FALSE(detail::isFilterSubset("abc", "abcd"));
}

} // anonymous namespace
} // namespace viewer
} // namespace data
} // namespace modules
} // namespace session
} // namespace rstudio
