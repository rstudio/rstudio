/*
 * RTokenizerTests.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/r_util/RTokenizer.hpp>

#include <iostream>

#include <boost/assert.hpp>
#include <boost/foreach.hpp>

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
      verify(defaultTokenType_, value) ;
   }

   void verify(wchar_t tokenType, const std::wstring& value)
   {

      RTokenizer rt(prefix_ + value + suffix_) ;
      RToken t ;
      while (t = rt.nextToken())
      {
         if (t.offset() == prefix_.length())
         {
            std::wcout << value << std::endl;
            BOOST_ASSERT(tokenType == t.type());
            BOOST_ASSERT(value.length() == t.length());
            BOOST_ASSERT(value == t.content());
            return ;
         }
      }

   }

   void verify(const std::deque<std::wstring>& values)
   {
      verify(defaultTokenType_, values);
   }

   void verify(int tokenType, const std::deque<std::wstring>& values)
   {
      BOOST_FOREACH(const std::wstring& value, values)
         verify(tokenType, value);
   }

private:
   const wchar_t defaultTokenType_ ;
   const std::wstring prefix_ ;
   const std::wstring suffix_ ;

};


void testVoid()
{
   RTokenizer rt(L"") ;
   BOOST_ASSERT(!rt.nextToken());
}

void testSimple()
{
   Verifier v(RToken::ERR, L" ", L" ") ;
   v.verify(RToken::LPAREN, L"(") ;
   v.verify(RToken::RPAREN, L")") ;
   v.verify(RToken::LBRACKET, L"[") ;
   v.verify(RToken::RBRACKET, L"]") ;
   v.verify(RToken::LBRACE, L"{") ;
   v.verify(RToken::RBRACE, L"}") ;
   v.verify(RToken::COMMA, L",") ;
   v.verify(RToken::SEMI, L";") ;
}

void testError()
{
   Verifier v(RToken::ERR, L" ", L" ") ;
}

void testComment()
{
   Verifier v(RToken::COMMENT, L" ", L"\n") ;
   v.verify(L"#");
   v.verify(L"# foo #");

   Verifier v2(RToken::COMMENT, L" ", L"\r\n") ;
   v2.verify(L"#");
   v2.verify(L"# foo #");
}


void testNumbers()
{
   Verifier v(RToken::NUMBER, L" ", L" ") ;
   v.verify(L"1");
   v.verify(L"10");
   v.verify(L"0.1");
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
   Verifier v(RToken::OPER, L" ", L" ") ;
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
   v.verify(L"$");
   v.verify(L":");
   v.verify(L"=");
}

void testUOperators()
{
   Verifier v(RToken::UOPER, L" ", L" ") ;
   v.verify(L"%%");
   v.verify(L"%test test%");
}

void testStrings()
{
   Verifier v(RToken::STRING, L" ", L" ") ;
   v.verify(L"\"test\"") ;
   v.verify(L"\" '$\t\r\n\\\"\"") ;
   v.verify(L"\"\"") ;
   v.verify(L"''") ;
   v.verify(L"'\"'") ;
   v.verify(L"'\\\"'") ;
   v.verify(L"'\n'") ;
   v.verify(L"'foo bar \\U654'") ;
}

void testIdentifiers()
{
   Verifier v(RToken::ID, L" ", L" ") ;
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
   Verifier v(RToken::WHITESPACE, L"a", L"z") ;
   v.verify(L" ");
   v.verify(L"      ");
   v.verify(L"\t\n");
   v.verify(L"\x00A0") ;
   v.verify(L"  \x3000  ") ;
   v.verify(L" \x00A0\t\x3000\r  ") ;
}


} // anonymous namespace


void runTokenizerTests()
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


} // namespace r_util
} // namespace core 


