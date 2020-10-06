/*
 * RTokenizerTests.cpp
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

#define RSTUDIO_ENABLE_DEBUG_MACROS

#include <core/r_util/RTokenizer.hpp>

#include <iostream>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace r_util {

namespace {

class Verifier
{
public:
   Verifier(wchar_t defaultTokenType,
            const std::wstring& prefix,
            const std::wstring& suffix)
      : defaultTokenType_(defaultTokenType),
        prefix_(prefix),
        suffix_(suffix)
   {
   }

   void verify(const std::wstring& value)
   {
      verify(defaultTokenType_, value);
   }

   void verify(wchar_t tokenType, const std::wstring& value)
   {

      RTokenizer rt(prefix_ + value + suffix_);
      RToken t;
      while ((t = rt.nextToken()))
      {
         if (t.offset() == prefix_.length())
         {
            if (tokenType != t.type())
            {
               std::wcerr << value << L" : " << t.content() << std::endl;
            }
            expect_true(tokenType == t.type());
            expect_true(value.length() == t.length());
            expect_true(value == t.content());
            return;
         }
      }

   }

   void verify(const std::deque<std::wstring>& values)
   {
      verify(defaultTokenType_, values);
   }

   void verify(int tokenType, const std::deque<std::wstring>& values)
   {
      for (const std::wstring& value : values)
         verify(tokenType, value);
   }

private:
   const wchar_t defaultTokenType_;
   const std::wstring prefix_;
   const std::wstring suffix_;

};


void testVoid()
{
   RTokenizer rt(L"");
   expect_true(!rt.nextToken());
}

void testSimple()
{
   Verifier v(RToken::ERR, L" ", L" ");
   v.verify(RToken::LPAREN, L"(");
   v.verify(RToken::RPAREN, L")");
   v.verify(RToken::LBRACKET, L"[");
   v.verify(RToken::RBRACKET, L"]");
   v.verify(RToken::LBRACE, L"{");
   v.verify(RToken::RBRACE, L"}");
   v.verify(RToken::COMMA, L",");
   v.verify(RToken::SEMI, L";");
}

void testError()
{
   Verifier v(RToken::ERR, L" ", L" ");
}

void testComment()
{
   Verifier v(RToken::COMMENT, L" ", L"\n");
   v.verify(L"#");
   v.verify(L"# foo #");

   Verifier v2(RToken::COMMENT, L" ", L"\r\n");
   v2.verify(L"#");
   v2.verify(L"# foo #");
}


void testNumbers()
{
   Verifier v(RToken::NUMBER, L" ", L" ");
   v.verify(L"1");
   v.verify(L"10");
   v.verify(L"0.1");
   v.verify(L"1.");
   v.verify(L".2");
   v.verify(L"1e-7");
   v.verify(L"1.2e+7");
   v.verify(L"2e");
   v.verify(L"3e+");
   v.verify(L"0x");
   v.verify(L"0x0");
   v.verify(L"0xDEADBEEF");
   v.verify(L"0xcafebad");
   v.verify(L"1L");
   v.verify(L"0x10L");
   v.verify(L"1000000L");
   v.verify(L"1e6L");
   v.verify(L"1.1L");
   v.verify(L"1e-3L");
   v.verify(L"2i");
   v.verify(L"4.1i");
   v.verify(L"1e-2i");
}


void testOperators()
{
   Verifier v(RToken::OPER, L" ", L" ");
   v.verify(L"+");
   v.verify(L"-");
   v.verify(L"*");
   v.verify(L"/");
   v.verify(L"^");
   v.verify(L">");
   v.verify(L">=");
   v.verify(L"<");
   v.verify(L"<=");
   v.verify(L"==");
   v.verify(L"!=");
   v.verify(L"!");
   v.verify(L"&");
   v.verify(L"|");
   v.verify(L"~");
   v.verify(L"->");
   v.verify(L"<-");
   v.verify(L"->>");
   v.verify(L"<<-");
   v.verify(L"$");
   v.verify(L":");
   v.verify(L"=");
   v.verify(L":=");
}

void testUOperators()
{
   Verifier v(RToken::UOPER, L" ", L" ");
   v.verify(L"%%");
   v.verify(L"%test test%");
}

void testStrings()
{
   Verifier v(RToken::STRING, L" ", L" ");
   v.verify(L"\"test\"");
   v.verify(L"\" '$\t\r\n\\\"\"");
   v.verify(L"\"\"");
   v.verify(L"''");
   v.verify(L"'\"'");
   v.verify(L"'\\\"'");
   v.verify(L"'\n'");
   v.verify(L"'foo bar \\U654'");
}

void testIdentifiers()
{
   Verifier v(RToken::ID, L" ", L" ");
   v.verify(L".");
   v.verify(L"...");
   v.verify(L"..1");
   v.verify(L"..2");
   v.verify(L"foo");
   v.verify(L"FOO");
   v.verify(L"f1");
   v.verify(L"a_b");
   v.verify(L"ab_");
   v.verify(L"`foo`");
   v.verify(L"`$@!$@#$`");
   v.verify(L"`a\n\"'b`");

   v.verify(L"\x00C1" L"qc1");
   v.verify(L"\x00C1" L"qc1" L"\x00C1");
}

void testWhitespace()
{
   Verifier v(RToken::WHITESPACE, L"a", L"z");
   v.verify(L" ");
   v.verify(L"      ");
   v.verify(L"\t\n");
   v.verify(L"\x00A0");
   v.verify(L"  \x3000  ");
   v.verify(L" \x00A0\t\x3000\r  ");
}


} // anonymous namespace


test_context("RTokenizer")
{
   test_that("We tokenize various strings correctly")
   {
      testVoid();
      testComment();
      testSimple();
      testError();
      testNumbers();
      testOperators();
      testUOperators();
      testStrings();
      testIdentifiers();
      testWhitespace();
   }
   
   test_that("Comments are tokenized without a trailing newline")
   {
      RTokens rTokens(L"## this is a comment\n1");
      expect_true(rTokens.size() == 3);
      expect_true(rTokens.at(0).isType(RToken::COMMENT));
      expect_true(rTokens.at(1).isType(RToken::WHITESPACE));
      expect_true(rTokens.at(1).contentEquals(L"\n"));
      expect_true(rTokens.at(2).isType(RToken::NUMBER));
   }
   
   test_that("'**' is properly tokenized as a single operator")
   {
      RTokens rTokens(L"1 ** 2");
      expect_true(rTokens.size() == 5);
      expect_true(rTokens.at(0).isType(RToken::NUMBER));
      expect_true(rTokens.at(1).isType(RToken::WHITESPACE));
      expect_true(rTokens.at(2).isType(RToken::OPER));
      expect_true(rTokens.at(2).contentEquals(L"**"));
   }
   
   test_that("plain raw strings are tokenized properly")
   {
      auto lines = {
         L"r\"(abc)\"",
         L"R\"(abc)\"",
         L"r\"{abc}\"",
         L"r'[abc]'",
         L"R'{abc}'"
      };
      
      for (auto line : lines)
      {
         RTokens rTokens(line);
         expect_true(rTokens.size() == 1);
         expect_true(rTokens.at(0).isType(RToken::STRING));
      }
   }
   
   test_that("unclosed raw strings are tokenized as errors")
   {
      RTokens rTokens(L"r'(abc");
      expect_true(rTokens.size() == 1);
      expect_true(rTokens.at(0).isType(RToken::ERR));
   }
   
   test_that("the raw string tokenizer restores iterator state if no raw string consumed")
   {
      RTokens rTokens(L"rep('.')");
      expect_true(rTokens.size() == 4);
      expect_true(rTokens.at(0).isType(RToken::ID));
      expect_true(rTokens.at(1).isType(RToken::LPAREN));
      expect_true(rTokens.at(2).isType(RToken::STRING));
      expect_true(rTokens.at(3).isType(RToken::RPAREN));
   }
   
}

} // namespace r_util
} // namespace core 
} // namespace rstudio


