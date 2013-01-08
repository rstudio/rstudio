/*
 * RTokenizer.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

// A very low level assertion (line 595 of cpp_regex_traits.hpp) is firing
// on WindowsXP when we create a boost::wregex within R session. This code
// is part of boost attempting to detect the sort syntax, and the assertion
// is that there are no nulls in a buffer after a call to locale->transform.
// Within the same method there are workarounds for both borland c++ and
// dinkumware c++ std library to strip embedded nulls, so it seems as if
// there is some variation in whether nulls come out of the call to transforn.
// Note also that boost has a macro titled BOOST_REGEX_NO_WIN32 which we are
// not using, which means that boost regex does call into native windows APIs
// for locale oriented functions (thus explaining a difference in runtime
// behavior across platforms). Tokenization appears to continue to work
// correctly in the presence of the assertion (with the annoying side effect
// of logging an assertion every time the product starts up)

#include <core/r_util/RTokenizer.hpp>

#include <boost/regex.hpp>

#include <iostream>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>


namespace core {
namespace r_util {

namespace {

class TokenPatterns
{
private:
   friend TokenPatterns& tokenPatterns();
   TokenPatterns()
      : NUMBER(L"[0-9]*(\\.[0-9]*)?([eE][+-]?[0-9]*)?[Li]?"),
        HEX_NUMBER(L"0x[0-9a-fA-F]*L?"),
        USER_OPERATOR(L"%[^%]*%"),
        QUOTED_IDENTIFIER(L"`[^`]*`"),
        UNTIL_END_QUOTE(L"[\\\\\'\"]"),
        WHITESPACE(L"[\\s\x00A0\x3000]+"),
        COMMENT(L"#.*?$")
   {
   }

public:
   const boost::wregex NUMBER;
   const boost::wregex HEX_NUMBER;
   const boost::wregex USER_OPERATOR;
   const boost::wregex QUOTED_IDENTIFIER;
   const boost::wregex UNTIL_END_QUOTE;
   const boost::wregex WHITESPACE;
   const boost::wregex COMMENT;
};

TokenPatterns& tokenPatterns()
{
   static TokenPatterns instance;
   return instance;
}

} // anonymous namespace


const wchar_t RToken::LPAREN         = L'(';
const wchar_t RToken::RPAREN         = L')';
const wchar_t RToken::LBRACKET       = L'[';
const wchar_t RToken::RBRACKET       = L']';
const wchar_t RToken::LBRACE         = L'{';
const wchar_t RToken::RBRACE         = L'}';
const wchar_t RToken::COMMA          = L',';
const wchar_t RToken::SEMI           = L';';
const wchar_t RToken::WHITESPACE     = 0x1001;
const wchar_t RToken::STRING         = 0x1002;
const wchar_t RToken::NUMBER         = 0x1003;
const wchar_t RToken::ID             = 0x1004;
const wchar_t RToken::OPER           = 0x1005;
const wchar_t RToken::UOPER          = 0x1006;
const wchar_t RToken::ERR            = 0x1007;
const wchar_t RToken::LDBRACKET      = 0x1008;
const wchar_t RToken::RDBRACKET      = 0x1009;
const wchar_t RToken::COMMENT        = 0x100A;


RToken RTokenizer::nextToken()
{
  if (eol())
     return RToken() ;

  wchar_t c = peek() ;

  switch (c)
  {
  case L'(': case L')':
  case L'{': case L'}':
  case L';': case L',':
     return consumeToken(c, 1) ;
  case L'[':
     if (peek(1) == L'[')
        return consumeToken(RToken::LDBRACKET, 2) ;
     else
        return consumeToken(c, 1) ;
  case L']':
     if (peek(1) == L']')
        return consumeToken(RToken::RDBRACKET, 2) ;
     else
        return consumeToken(c, 1) ;
  case L'"':
  case L'\'':
     return matchStringLiteral() ;
  case L'`':
     return matchQuotedIdentifier();
  case L'#':
     return matchComment();
  case L'%':
     return matchUserOperator();
  case L' ': case L'\t': case L'\r': case L'\n':
  case L'\x00A0': case L'\x3000':
     return matchWhitespace() ;
  }

  wchar_t cNext = peek(1) ;

  if ((c >= L'0' && c <= L'9')
        || (c == L'.' && cNext >= L'0' && cNext <= L'9'))
  {
     RToken numberToken = matchNumber() ;
     if (numberToken.length() > 0)
        return numberToken ;
  }

  if (string_utils::isalnum(c) || c == L'.')
  {
     // From Section 10.3.2, identifiers must not start with
     // a digit, nor may they start with a period followed by
     // a digit.
     //
     // Since we're not checking for either condition, we must
     // match on identifiers AFTER we have already tried to
     // match on number.
     return matchIdentifier() ;
  }

  RToken oper = matchOperator() ;
  if (oper)
     return oper ;

  // Error!!
  return consumeToken(RToken::ERR, 1) ;
}



RToken RTokenizer::matchWhitespace()
{
   std::wstring whitespace = peek(tokenPatterns().WHITESPACE) ;
   return consumeToken(RToken::WHITESPACE, whitespace.length()) ;
}

RToken RTokenizer::matchStringLiteral()
{
   std::wstring::const_iterator start = pos_ ;
   wchar_t quot = eat() ;

   while (!eol())
   {
      eatUntil(tokenPatterns().UNTIL_END_QUOTE);

      if (eol())
         break ;

      wchar_t c = eat() ;
      if (c == quot)
      {
         // NOTE: this is where we used to set wellFormed = true
         break ;
      }

      if (c == L'\\')
      {
         if (!eol())
            eat() ;

         // Actually the escape expression can be longer than
         // just the backslash plus one character--but we don't
         // need to distinguish escape expressions from other
         // literal text other than for the purposes of breaking
         // out of the string
      }
   }

   // NOTE: the Java version of the tokenizer returns a special RStringToken
   // subclass which includes the wellFormed flag as an attribute. Our
   // implementation of RToken is stack based so doesn't support subclasses
   // (because they will be sliced when copied). If we need the well
   // formed flag we can just add it onto RToken.
   return RToken(RToken::STRING,
                 start,
                 pos_,
                 start - data_.begin());
}

RToken RTokenizer::matchNumber()
{
   std::wstring num = peek(tokenPatterns().HEX_NUMBER) ;
   if (num.empty())
      num = peek(tokenPatterns().NUMBER) ;

   return consumeToken(RToken::NUMBER, num.length());
}

RToken RTokenizer::matchIdentifier()
{
   std::wstring::const_iterator start = pos_ ;
   eat();
   while (string_utils::isalnum(peek()) || peek() == L'.' || peek() == L'_')
      eat();
   return RToken(RToken::ID,
                 start,
                 pos_,
                 start - data_.begin()) ;
}

RToken RTokenizer::matchQuotedIdentifier()
{
   std::wstring iden = peek(tokenPatterns().QUOTED_IDENTIFIER) ;
   if (iden.empty())
      return consumeToken(RToken::ERR, 1);
   else
      return consumeToken(RToken::ID, iden.length());
}

RToken RTokenizer::matchComment()
{
   std::wstring comment = peek(tokenPatterns().COMMENT);
   return consumeToken(RToken::COMMENT, comment.length());
}

RToken RTokenizer::matchUserOperator()
{
   std::wstring oper = peek(tokenPatterns().USER_OPERATOR) ;
   if (oper.empty())
      return consumeToken(RToken::ERR, 1) ;
   else
      return consumeToken(RToken::UOPER, oper.length()) ;
}


RToken RTokenizer::matchOperator()
{
   wchar_t cNext = peek(1) ;

   switch (peek())
   {
   case L'+': case L'*': case L'/':
   case L'^': case L'&': case L'|':
   case L'~': case L'$': case L':':
      // single-character operators
      return consumeToken(RToken::OPER, 1) ;
   case L'-': // also ->
      return consumeToken(RToken::OPER, cNext == L'>' ? 2 : 1) ;
   case L'>': // also >=
      return consumeToken(RToken::OPER, cNext == L'=' ? 2 : 1) ;
   case L'<': // also <- and <=
      return consumeToken(RToken::OPER, cNext == L'=' ? 2 :
                                       cNext == L'-' ? 2 :
                                       1) ;
   case L'=': // also ==
      return consumeToken(RToken::OPER, cNext == L'=' ? 2 : 1) ;
   case L'!': // also !=
      return consumeToken(RToken::OPER, cNext == L'=' ? 2 : 1) ;
   default:
      return RToken() ;
   }
}

bool RTokenizer::eol()
{
   return pos_ >= data_.end();
}

wchar_t RTokenizer::peek()
{
   return peek(0) ;
}

wchar_t RTokenizer::peek(std::size_t lookahead)
{
   if ((pos_ + lookahead) >= data_.end())
      return 0 ;
   else
      return *(pos_ + lookahead) ;
}

wchar_t RTokenizer::eat()
{
   wchar_t result = *pos_;
   pos_++ ;
   return result ;
}

std::wstring RTokenizer::peek(const boost::wregex& regex)
{
   boost::wsmatch match;
   std::wstring::const_iterator end = data_.end();
   boost::match_flag_type flg = boost::match_default | boost::match_continuous;
   if (boost::regex_search(pos_, end, match, regex, flg))
   {
      return match[0];
   }
   else
   {
      return std::wstring();
   }
}

void RTokenizer::eatUntil(const boost::wregex& regex)
{
   boost::wsmatch match;
   std::wstring::const_iterator end = data_.end();
   if (boost::regex_search(pos_, end, match, regex))
   {
      pos_ = match[0].first;
   }
   else
   {
      // eat all on failure to match
      pos_ = data_.end();
   }
}


RToken RTokenizer::consumeToken(wchar_t tokenType, std::size_t length)
{
   if (length == 0)
   {
      LOG_WARNING_MESSAGE("Can't create zero-length token");
      return RToken();
   }
   else if ((pos_ + length) > data_.end())
   {
      LOG_WARNING_MESSAGE("Premature EOF");
      return RToken();
   }

   std::wstring::const_iterator start = pos_ ;
   pos_ += length ;
   return RToken(tokenType,
                 start,
                 pos_,
                 start - data_.begin()) ;
}


} // namespace r_util
} // namespace core 


