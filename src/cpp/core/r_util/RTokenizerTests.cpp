/*
 * RTokenizerTests.cpp
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

#define RSTUDIO_ENABLE_DEBUG_MACROS

#include <core/r_util/RTokenizer.hpp>

#include <iostream>

#include <gtest/gtest.h>

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
            ASSERT_EQ(tokenType, t.type());
            ASSERT_EQ(value.length(), t.length());
            ASSERT_EQ(value, t.content());
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
   ASSERT_TRUE(!rt.nextToken());
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

   // these tests assume a unicode locale -- note that RStudio
   // will also try to set a UTF-8 locale by default on startup
   const char* locale = setlocale(LC_ALL, "C.UTF-8");
   if (locale != nullptr)
   {
      v.verify(L"\x00C1" L"qc1");                     // 'Áqc1'
      v.verify(L"\x00C1" L"qc1" L"\x00C1");           // 'Áqc1Á'
      setlocale(LC_ALL, locale);
   }
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


TEST(RutilTest, TokenizeVariousStrings)
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

TEST(RutilTest, CommentsTokenizedWithoutTrailingNewline)
{
   RTokens rTokens(L"## this is a comment\n1");
   EXPECT_EQ(3u, rTokens.size());
   EXPECT_TRUE(rTokens.at(0).isType(RToken::COMMENT));
   ASSERT_TRUE(rTokens.at(1).isType(RToken::WHITESPACE));
   ASSERT_TRUE(rTokens.at(1).contentEquals(L"\n"));
   ASSERT_TRUE(rTokens.at(2).isType(RToken::NUMBER));
}

TEST(RutilTest, DoubleStarAsSingleOperator)
{
   RTokens rTokens(L"1 ** 2");
   EXPECT_EQ(5u, rTokens.size());
   EXPECT_TRUE(rTokens.at(0).isType(RToken::NUMBER));
   ASSERT_TRUE(rTokens.at(1).isType(RToken::WHITESPACE));
   ASSERT_TRUE(rTokens.at(2).isType(RToken::OPER));
   ASSERT_TRUE(rTokens.at(2).contentEquals(L"**"));
}

TEST(RutilTest, PlainRawStringTokenization)
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
   EXPECT_EQ(1u, rTokens.size());
      EXPECT_TRUE(rTokens.at(0).isType(RToken::STRING));
   }
}

TEST(RutilTest, UnclosedRawStringsAsErrors)
{
   RTokens rTokens(L"r'(abc");
   EXPECT_EQ(1u, rTokens.size());
   EXPECT_TRUE(rTokens.at(0).isType(RToken::ERR));
}

TEST(RutilTest, RawStringTokenizerRestoresState)
{
   RTokens rTokens(L"rep('.')");
   EXPECT_EQ(4u, rTokens.size());
   EXPECT_TRUE(rTokens.at(0).isType(RToken::ID));
   ASSERT_TRUE(rTokens.at(1).isType(RToken::LPAREN));
   ASSERT_TRUE(rTokens.at(2).isType(RToken::STRING));
   ASSERT_TRUE(rTokens.at(3).isType(RToken::RPAREN));
}

TEST(RutilTest, MultilineStringsAsStrings)
{
   RTokens rTokens(L"'abc\ndef'");
   EXPECT_EQ(1u, rTokens.size());
   EXPECT_TRUE(rTokens.at(0).isType(RToken::STRING));
}

TEST(RutilTest, EscapesInSymbolNames)
{
   RTokens rTokens(L"`a \\` b`");
   EXPECT_EQ(1u, rTokens.size());
   EXPECT_TRUE(rTokens.at(0).isType(RToken::ID));
}

TEST(RutilTest, KnitrChunkEmbeds)
{
   RTokens rTokens(L"<<chunk>>");
   EXPECT_EQ(1u, rTokens.size());
}

TEST(RutilTest, UnicodeLetterIdentification)
{
   RTokens rTokens(L"区 <- 42");
   EXPECT_EQ(5u, rTokens.size());
   EXPECT_TRUE(rTokens.at(0).isType(RToken::ID));
   EXPECT_TRUE(rTokens.at(1).isType(RToken::WHITESPACE));
   EXPECT_TRUE(rTokens.at(2).isType(RToken::OPER));
   EXPECT_TRUE(rTokens.at(3).isType(RToken::WHITESPACE));
   EXPECT_TRUE(rTokens.at(4).isType(RToken::NUMBER));
}

} // namespace r_util
} // namespace core
} // namespace rstudio