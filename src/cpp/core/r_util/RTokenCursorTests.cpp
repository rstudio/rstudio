/*
 * SessionRTokenCursorTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <tests/TestThat.hpp>
#include <core/r_util/RTokenCursor.hpp>

namespace rstudio {
namespace core {
namespace unit_tests {

using namespace core::r_util::token_cursor;

bool isPipeOperator(const std::wstring& string)
{
   static const boost::wregex rePipe(L"^%[^>]*>+[^>]*%$");
   return regex_utils::match(string.begin(), string.end(), rePipe);
}

test_context("RTokenCursor")
{
   test_that("Token cursors properly detect end of statements")
   {
      RTokens rTokens(L"1 + 2\n");
      RTokenCursor cursor(rTokens);
      
      expect_true(cursor.isType(RToken::NUMBER));
      expect_true(cursor.moveToNextSignificantToken());
      expect_true(cursor.isType(RToken::OPER));
      expect_true(cursor.moveToNextToken());
      expect_true(cursor.isType(RToken::WHITESPACE));
      expect_true(cursor.moveToNextToken());
      expect_true(cursor.isType(RToken::NUMBER));
      expect_true(cursor.isAtEndOfStatement(false));
   }
   
   test_that("Token cursor ignores EOL when in parenthetical scope")
   {
      RTokens rTokens(L"(1\n+2)");
      RTokenCursor cursor(rTokens);
      expect_true(cursor.isType(RToken::LPAREN));
      expect_true(cursor.moveToNextSignificantToken());
      expect_true(cursor.isType(RToken::NUMBER));
      expect_true(cursor.nextToken().isType(RToken::WHITESPACE));
      expect_true(cursor.nextToken().contentEquals(L"\n"));
      expect_false(cursor.isAtEndOfStatement(true));
   }
   
   test_that("Move to position functions as expected")
   {
      RTokens rTokens(L"\n\napple + 2");
      RTokenCursor cursor(rTokens);
      
      expect_true(cursor.moveToPosition(2, 0));
      expect_true(cursor.isType(RToken::ID));
      
      expect_true(cursor.moveToPosition(2, 1));
      expect_true(cursor.isType(RToken::ID));
      
      expect_true(cursor.moveToPosition(2, 5));
      expect_true(cursor.isType(RToken::WHITESPACE));
   }
   
   test_that("pipe / chain operation heads are extracted successfully")
   {
      expect_true(isPipeOperator(L"%>%"));
      expect_true(isPipeOperator(L"%>>%"));
      expect_true(isPipeOperator(L"%T>%"));
      
      RTokens rTokens(L"mtcars %>% first_level() %>% second_level(1");
      RTokenCursor cursor(rTokens);
      cursor.moveToEndOfTokenStream();
      expect_true(cursor.isType(RToken::NUMBER) &&
                  cursor.contentEquals(L"1"));
      
      expect_true(cursor.moveToOpeningParenAssociatedWithCurrentFunctionCall());
      expect_true(cursor.isType(RToken::LPAREN));
      expect_true(cursor.moveToPreviousSignificantToken());
      expect_true(cursor.contentEquals(L"second_level"));
      expect_true(cursor.moveToStartOfEvaluation());
      expect_true(cursor.contentEquals(L"second_level"));
      expect_true(cursor.getHeadOfPipeChain() == "mtcars");
   }
   
   test_that("pipe / chain operation lookups fail when not within associated chain")
   {
      RTokens rTokens(L"mtcars %>% foo\nbar");
      RTokenCursor cursor(rTokens);
      cursor.moveToEndOfTokenStream();
      expect_true(cursor.getHeadOfPipeChain().empty());
   }
   
   test_that("evaluation lookarounds work")
   {
      RTokens rTokens(L"foo$bar$baz[[1]]$bam");
      RTokenCursor cursor(rTokens);
      
      expect_true(cursor.contentEquals(L"foo"));
      expect_true(cursor.moveToEndOfEvaluation());
      expect_true(cursor.contentEquals(L"baz"));
      expect_true(cursor.moveToStartOfEvaluation());
      expect_true(cursor.contentEquals(L"foo"));
      
      expect_true(cursor.moveToEndOfStatement(false));
      expect_true(cursor.contentEquals(L"bam"));
   }

   test_that("previousSignificantToken works at end of token list")
   {
      RTokens rTokens(L"a <- b");
      RTokenCursor cursor(rTokens);
      cursor.moveToEndOfTokenStream();
      auto result = cursor.previousSignificantToken();
      expect_true(result.isType(RToken::OPER));
      expect_true(result.contentEquals(L"<-"));
   }

   test_that("previousSignificantToken works in middle of token list")
   {
      RTokens rTokens(L"a <- b");
      RTokenCursor cursor(rTokens);
      cursor.moveToEndOfTokenStream();
      cursor.moveToPreviousSignificantToken();
      auto result = cursor.previousSignificantToken();
      expect_true(result.isType(RToken::ID));
      expect_true(result.contentEquals(L"a"));
   }

   test_that("previousSignificantToken returns dummy token when already at beginning")
   {
      RTokens rTokens(L"a <- b");
      RTokenCursor cursor(rTokens);
      const auto& result = cursor.previousSignificantToken();
      expect_true(result.isType(RToken::ERR));
      expect_true(result.content().empty());
      expect_true(result.length() == 0);

      // isPipeOperator exercises the token's begin/end iterators
      expect_false(token_utils::isPipeOperator(result));
   }
}

} // namespace unit_tests
} // namespace core
} // namespace rstudio
