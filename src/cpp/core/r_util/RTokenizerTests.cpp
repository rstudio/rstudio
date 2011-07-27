/*
 * RTokenizerTests.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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
   Verifier(int defaultTokenType,
            const std::string& prefix,
            const std::string& suffix)
      : defaultTokenType_(defaultTokenType),
        prefix_(prefix),
        suffix_(suffix)
   {
   }

   void verify(const std::string& value)
   {
      verify(defaultTokenType_, value) ;
   }

   void verify(int tokenType, const std::string& value)
   {

      RTokenizer rt(prefix_ + value + suffix_) ;
      RToken t ;
      while (t = rt.nextToken())
      {
         if (t.offset() == prefix_.length())
         {
            std::cerr << value << std::endl;

            BOOST_ASSERT(tokenType == t.type());
            BOOST_ASSERT(value.length() == t.length());
            BOOST_ASSERT(value == t.content());
            return ;
         }
      }

   }

   void verify(const std::vector<std::string>& values)
   {
      verify(defaultTokenType_, values);
   }

   void verify(int tokenType, const std::vector<std::string>& values)
   {
      BOOST_FOREACH(const std::string& value, values)
         verify(tokenType, value);
   }

private:
   const int defaultTokenType_ ;
   const std::string prefix_ ;
   const std::string suffix_ ;

};


void testVoid()
{
   RTokenizer rt("") ;
   BOOST_ASSERT(!rt.nextToken());
}

void testSimple()
{
   Verifier v(RToken::ERROR, " ", " ") ;
   v.verify(RToken::LPAREN, "(") ;
   v.verify(RToken::RPAREN, ")") ;
   v.verify(RToken::LBRACKET, "[") ;
   v.verify(RToken::RBRACKET, "]") ;
   v.verify(RToken::LBRACE, "{") ;
   v.verify(RToken::RBRACE, "}") ;
   v.verify(RToken::COMMA, ",") ;
   v.verify(RToken::SEMI, ";") ;
}

void testError()
{
   Verifier v(RToken::ERROR, " ", " ") ;
   v.verify("#") ;
}


void testNumbers()
{
   Verifier v(RToken::NUMBER, " ", " ") ;
   v.verify("1");
   v.verify("10");
   v.verify("0.1");
   v.verify(".2");
   v.verify("1e-7");
   v.verify("1.2e+7");
   v.verify("2e");
   v.verify("3e+");
   v.verify("0x");
   v.verify("0x0");
   v.verify("0xDEADBEEF");
   v.verify("0xcafebad");
   v.verify("1L");
   v.verify("0x10L");
   v.verify("1000000L");
   v.verify("1e6L");
   v.verify("1.1L");
   v.verify("1e-3L");
   v.verify("2i");
   v.verify("4.1i");
   v.verify("1e-2i");
}


void testOperators()
{
   Verifier v(RToken::OPER, " ", " ") ;
   v.verify("+");
   v.verify("-");
   v.verify("*");
   v.verify("/");
   v.verify("^");
   v.verify(">");
   v.verify(">=");
   v.verify("<");
   v.verify("<=");
   v.verify("==");
   v.verify("!=");
   v.verify("!");
   v.verify("&");
   v.verify("|");
   v.verify("~");
   v.verify("->");
   v.verify("<-");
   v.verify("$");
   v.verify(":");
   v.verify("=");
}

void testUOperators()
{
   Verifier v(RToken::UOPER, " ", " ") ;
   v.verify("%%");
   v.verify("%test test%");
}

void testStrings()
{
   Verifier v(RToken::STRING, " ", " ") ;
   v.verify("\"test\"") ;
   v.verify("\" '$\t\r\n\\\"\"") ;
   v.verify("\"\"") ;
   v.verify("''") ;
   v.verify("'\"'") ;
   v.verify("'\\\"'") ;
   v.verify("'\n'") ;
   //v.verify("'foo bar \\U654'") ;
}

void testIdentifiers()
{
   Verifier v(RToken::ID, " ", " ") ;
   v.verify(".");
   v.verify("...");
   v.verify("..1");
   v.verify("..2");
   v.verify("foo");
   v.verify("FOO");
   v.verify("f1");
   v.verify("a_b");
   v.verify("ab_");
   //v.verify("\u00C1qc1");
}

void testWhitespace()
{
   Verifier v(RToken::WHITESPACE, "a", "z") ;
   v.verify(" ");
   v.verify("      ");
   v.verify("\t\n");
   //v.verify("\u00A0") ;
}


} // anonymous namespace


void runTokenizerTests()
{
   testVoid();
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


