/*
 * SessionCodeSearchTests.cpp
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

#include "SessionCodeSearch.hpp"

#include <gtest/gtest.h>

using namespace rstudio::session::modules::code_search;

namespace {

// The client's SourceItem.java constants, which these values must match.
const int kNone = 0;
const int kSection = 7;
const int kFigure = 8;
const int kTable = 9;
const int kMath = 10;

} // anonymous namespace

TEST(CodeSearchXRef, TheoremStyleTypesAreMath)
{
   for (const char* type : {"thm", "lem", "cor", "prp", "cnj", "def", "exm", "exr", "eq"})
      EXPECT_EQ(kMath, sourceItemTypeForXRef(type)) << type;
}

TEST(CodeSearchXRef, TablesAndListingsAreTables)
{
   for (const char* type : {"tab", "tbl", "lst"})
      EXPECT_EQ(kTable, sourceItemTypeForXRef(type)) << type;
}

TEST(CodeSearchXRef, HeadingsAreSections)
{
   for (const char* type : {"h1", "h2", "h3", "h4", "h5", "h6", "sec"})
      EXPECT_EQ(kSection, sourceItemTypeForXRef(type)) << type;
}

TEST(CodeSearchXRef, FiguresAreFigures)
{
   EXPECT_EQ(kFigure, sourceItemTypeForXRef("fig"));
}

// e.g. a Quarto callout or remark, which the client shows by its label alone
TEST(CodeSearchXRef, OtherTypesAreNone)
{
   for (const char* type : {"nte", "rem", ""})
      EXPECT_EQ(kNone, sourceItemTypeForXRef(type)) << type;
}

TEST(CodeSearchXRef, TheoremStyleResultShowsItsName)
{
   XRefResult result = xrefResult("thm", "pyth", "Pythagoras", false);
   EXPECT_EQ("thm:pyth", result.text);
   EXPECT_EQ("Pythagoras", result.extraInfo);
}

TEST(CodeSearchXRef, UnnamedTheoremStyleResultHasNoName)
{
   XRefResult result = xrefResult("exm", "fizz", "", false);
   EXPECT_EQ("exm:fizz", result.text);
   EXPECT_EQ("", result.extraInfo);
}

// A figure's or table's title is its caption, which isn't shown.
TEST(CodeSearchXRef, FigureResultShowsOnlyItsLabel)
{
   XRefResult result = xrefResult("fig", "plot", "A scatter plot", false);
   EXPECT_EQ("fig:plot", result.text);
   EXPECT_EQ("", result.extraInfo);
}

TEST(CodeSearchXRef, HeadingResultIsItsTitle)
{
   XRefResult result = xrefResult("h1", "intro", "Introduction", false);
   EXPECT_EQ("Introduction", result.text);
   EXPECT_EQ("", result.extraInfo);
}

TEST(CodeSearchXRef, QuartoResultShowsOnlyItsLabel)
{
   XRefResult result = xrefResult("thm", "pyth", "Pythagoras", true);
   EXPECT_EQ("thm-pyth", result.text);
   EXPECT_EQ("", result.extraInfo);
}

TEST(CodeSearchXRef, TheoremIsFoundByLabelOrName)
{
   XRefResult result = xrefResult("thm", "pyth", "Pythagoras", false);
   EXPECT_TRUE(xrefMatches(result, "thm:pyth"));
   EXPECT_TRUE(xrefMatches(result, "pyth"));
   EXPECT_TRUE(xrefMatches(result, "Pythagoras"));
   EXPECT_TRUE(xrefMatches(result, "goras"));
   EXPECT_FALSE(xrefMatches(result, "Euclid"));
}

TEST(CodeSearchXRef, UnnamedResultIsNotFoundByEmptyName)
{
   XRefResult result = xrefResult("exm", "fizz", "", false);
   EXPECT_TRUE(xrefMatches(result, "exm:fizz"));
   EXPECT_FALSE(xrefMatches(result, "buzz"));
}

// Without the name, "Pythagoras" leaves most of its characters unmatched
// in "thm:pyth" and ranks far below other results.
TEST(CodeSearchXRef, NameMatchRanksByTheName)
{
   EXPECT_EQ(0, scoreXRefMatch("thm:pyth", "Pythagoras", "Pythagoras"));
   EXPECT_LT(scoreXRefMatch("thm:pyth", "Pythagoras", "Pythagoras"),
             scoreXRefMatch("thm:pyth", "", "Pythagoras"));
}

TEST(CodeSearchXRef, LabelMatchRanksByTheLabel)
{
   EXPECT_EQ(0, scoreXRefMatch("thm:pyth", "Pythagoras", "thm:pyth"));
}

// "Pythagoras" lacks "q", yet its score for "pq" (two characters unmatched,
// at most) beats the label's, whose matches come late.
TEST(CodeSearchXRef, UnmatchedNameDoesNotAffectRank)
{
   const std::string label = "thm:very-long-label-pq";
   EXPECT_EQ(scoreXRefMatch(label, "", "pq"),
             scoreXRefMatch(label, "Pythagoras", "pq"));
}
