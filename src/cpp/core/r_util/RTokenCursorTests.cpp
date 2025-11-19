/*
 * SessionRTokenCursorTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
#include <core/r_util/RTokenCursor.hpp>

namespace rstudio {
namespace core {
namespace unit_tests {

using namespace core::r_util::token_cursor;

bool isPipeOperator(const std::wstring& string)
{
   static const boost::wregex rePipe(L"^(%[^>]*>+[^>]*%)|([|]>)$");
   return regex_utils::match(string.begin(), string.end(), rePipe);
}

TEST(RutilTest, TokenCursorsDetectEndOfStatements)
{
   RTokens rTokens(L"1 + 2\n");
   RTokenCursor cursor(rTokens);
   
   EXPECT_TRUE(cursor.isType(RToken::NUMBER));
   ASSERT_TRUE(cursor.moveToNextSignificantToken());
   ASSERT_TRUE(cursor.isType(RToken::OPER));
   ASSERT_TRUE(cursor.moveToNextToken());
   ASSERT_TRUE(cursor.isType(RToken::WHITESPACE));
   ASSERT_TRUE(cursor.moveToNextToken());
   ASSERT_TRUE(cursor.isType(RToken::NUMBER));
   ASSERT_TRUE(cursor.isAtEndOfStatement(false));
}

TEST(RutilTest, TokenCursorIgnoresEolInParentheses)
{
   RTokens rTokens(L"(1\n+2)");
   RTokenCursor cursor(rTokens);
   EXPECT_TRUE(cursor.isType(RToken::LPAREN));
   ASSERT_TRUE(cursor.moveToNextSignificantToken());
   ASSERT_TRUE(cursor.isType(RToken::NUMBER));
   ASSERT_TRUE(cursor.nextToken().isType(RToken::WHITESPACE));
   ASSERT_TRUE(cursor.nextToken().contentEquals(L"\n"));
   EXPECT_FALSE(cursor.isAtEndOfStatement(true));
}

TEST(RutilTest, MoveToPositionWorks)
{
   RTokens rTokens(L"\n\napple + 2");
   RTokenCursor cursor(rTokens);
   
   EXPECT_TRUE(cursor.moveToPosition(2, 0));
   ASSERT_TRUE(cursor.isType(RToken::ID));
   
   ASSERT_TRUE(cursor.moveToPosition(2, 1));
   ASSERT_TRUE(cursor.isType(RToken::ID));
   
   ASSERT_TRUE(cursor.moveToPosition(2, 5));
   ASSERT_TRUE(cursor.isType(RToken::WHITESPACE));
}

TEST(RutilTest, PipeChainHeadsExtraction)
{
   EXPECT_TRUE(isPipeOperator(L"%>%"));
   ASSERT_TRUE(isPipeOperator(L"%>>%"));
   ASSERT_TRUE(isPipeOperator(L"%T>%"));
   ASSERT_TRUE(isPipeOperator(L"|>"));
   EXPECT_FALSE(isPipeOperator(L"%!%"));
   RTokens rTokens(L"mtcars %>% first_level() %>% second_level(1");
   RTokenCursor cursor(rTokens);
   cursor.moveToEndOfTokenStream();
   EXPECT_TRUE((cursor.isType(RToken::NUMBER) && cursor.contentEquals(L"1")));
   ASSERT_TRUE(cursor.moveToOpeningParenAssociatedWithCurrentFunctionCall());
   ASSERT_TRUE(cursor.isType(RToken::LPAREN));
   ASSERT_TRUE(cursor.moveToPreviousSignificantToken());
   ASSERT_TRUE(cursor.contentEquals(L"second_level"));
   ASSERT_TRUE(cursor.moveToStartOfEvaluation());
   ASSERT_TRUE(cursor.contentEquals(L"second_level"));
   EXPECT_EQ("mtcars", cursor.getHeadOfPipeChain());
}

TEST(RutilTest, PipeChainLookupsFailOutsideChain)
{
   RTokens rTokens(L"mtcars %>% foo\nbar");
   RTokenCursor cursor(rTokens);
   cursor.moveToEndOfTokenStream();
   EXPECT_TRUE(cursor.getHeadOfPipeChain().empty());
}

TEST(RutilTest, EvaluationLookaroundsWork)
{
   RTokens rTokens(L"foo$bar$baz[[1]]$bam");
   RTokenCursor cursor(rTokens);
   
   EXPECT_TRUE(cursor.contentEquals(L"foo"));
   ASSERT_TRUE(cursor.moveToEndOfEvaluation());
   ASSERT_TRUE(cursor.contentEquals(L"baz"));
   ASSERT_TRUE(cursor.moveToStartOfEvaluation());
   ASSERT_TRUE(cursor.contentEquals(L"foo"));
   
   ASSERT_TRUE(cursor.moveToEndOfStatement(false));
   ASSERT_TRUE(cursor.contentEquals(L"bam"));
}

TEST(RutilTest, PreviousSignificantTokenAtEnd)
{
   RTokens rTokens(L"a <- b");
   RTokenCursor cursor(rTokens);
   cursor.moveToEndOfTokenStream();
   auto result = cursor.previousSignificantToken();
   EXPECT_TRUE(result.isType(RToken::OPER));
   ASSERT_TRUE(result.contentEquals(L"<-"));
}

TEST(RutilTest, PreviousSignificantTokenInMiddle)
{
   RTokens rTokens(L"a <- b");
   RTokenCursor cursor(rTokens);
   cursor.moveToEndOfTokenStream();
   cursor.moveToPreviousSignificantToken();
   auto result = cursor.previousSignificantToken();
   EXPECT_TRUE(result.isType(RToken::ID));
   ASSERT_TRUE(result.contentEquals(L"a"));
}

TEST(RutilTest, PreviousSignificantTokenAtBeginning)
{
   RTokens rTokens(L"a <- b");
   RTokenCursor cursor(rTokens);
   const auto& result = cursor.previousSignificantToken();
   EXPECT_TRUE(result.isType(RToken::ERR));
   ASSERT_TRUE(result.content().empty());
   EXPECT_EQ(0u, result.length());

   // isPipeOperator exercises the token's begin/end iterators
   EXPECT_FALSE(token_utils::isPipeOperator(result));
}

TEST(RutilTest, OneColonTwoColonThreeParsing)
{
   RTokens rTokens(L"1:2:3");
   EXPECT_EQ(5u, rTokens.size());
   EXPECT_TRUE(rTokens.at(0).isType(RToken::NUMBER));
   EXPECT_TRUE(rTokens.at(1).isType(RToken::OPER));
   EXPECT_TRUE(rTokens.at(2).isType(RToken::NUMBER));
   EXPECT_TRUE(rTokens.at(3).isType(RToken::OPER));
   EXPECT_TRUE(rTokens.at(4).isType(RToken::NUMBER));
}

} // namespace unit_tests
} // namespace core
} // namespace rstudio
