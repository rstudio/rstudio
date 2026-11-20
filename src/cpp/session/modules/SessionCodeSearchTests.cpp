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
