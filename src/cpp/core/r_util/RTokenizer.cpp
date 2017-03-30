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
#include <sstream>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>


namespace rstudio {
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
        COMMENT(L"#[^\\n]*$")
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

void updatePosition(std::wstring::const_iterator pos,
                    std::size_t length,
                    std::size_t* pRow,
                    std::size_t* pColumn)
{
   std::size_t newlineCount;
   std::wstring::const_iterator it =
         string_utils::countNewlines(pos, pos + length, &newlineCount);
   
   if (newlineCount == 0)
   {
      *pColumn += length;
   }
   else
   {
      *pRow += newlineCount;
      
      // The column is now the token length, minus the
      // index of the last newline.
      *pColumn = length - (it - pos) - 1;
   }
}

} // anonymous namespace

RToken RTokenizer::nextToken()
{
  if (eol())
     return RToken() ;

  wchar_t c = peek() ;

  switch (c)
  {
  case L'(':
     return consumeToken(RToken::LPAREN, 1);
  case L')':
     return consumeToken(RToken::RPAREN, 1);
  case L'{':
     return consumeToken(RToken::LBRACE, 1);
  case L'}':
     return consumeToken(RToken::RBRACE, 1);
  case L';':
     return consumeToken(RToken::SEMI, 1);
  case L',':
     return consumeToken(RToken::COMMA, 1);
     
  case L'[':
  {
     RToken token;
     if (peek(1) == L'[')
     {
        braceStack_.push_back(RToken::LDBRACKET);
        token = consumeToken(RToken::LDBRACKET, 2);
     }
     else
     {
        braceStack_.push_back(RToken::LBRACKET);
        token = consumeToken(RToken::LBRACKET, 1);
     }
     return token;
  }
     
  case L']':
  {
     if (braceStack_.empty()) // TODO: warn?
     {
        if (peek(1) == L']')
           return consumeToken(RToken::RDBRACKET, 2) ;
        else
           return consumeToken(RToken::RBRACKET, 1);
     }
     else
     {
        RToken token;
        if (peek(1) == L']')
        {
           wchar_t top = braceStack_[braceStack_.size() - 1];
           if (top == RToken::LDBRACKET)
              token = consumeToken(RToken::RDBRACKET, 2);
           else
              token = consumeToken(RToken::RBRACKET, 1);
        }
        else
           token = consumeToken(RToken::RBRACKET, 1);
        
        braceStack_.pop_back();
        return token;
     }
  }
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
   return consumeToken(RToken::WHITESPACE, tokenLength(tokenPatterns().WHITESPACE));
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
   
   std::size_t row = row_;
   std::size_t column = column_;
   updatePosition(start, pos_ - start, &row_, &column_); 

   // NOTE: the Java version of the tokenizer returns a special RStringToken
   // subclass which includes the wellFormed flag as an attribute. Our
   // implementation of RToken is stack based so doesn't support subclasses
   // (because they will be sliced when copied). If we need the well
   // formed flag we can just add it onto RToken.
   return RToken(RToken::STRING,
                 start,
                 pos_,
                 start - data_.begin(),
                 row,
                 column);
}

RToken RTokenizer::matchNumber()
{
   std::size_t length = tokenLength(tokenPatterns().HEX_NUMBER);
   if (length == 0)
      length = tokenLength(tokenPatterns().NUMBER);

   return consumeToken(RToken::NUMBER, length);
}

RToken RTokenizer::matchIdentifier()
{
   std::wstring::const_iterator start = pos_ ;
   eat();
   while (string_utils::isalnum(peek()) || peek() == L'.' || peek() == L'_')
      eat();
   
   std::size_t row = row_;
   std::size_t column = column_;
   updatePosition(start, pos_ - start, &row_, &column_);
   
   return RToken(RToken::ID,
                 start,
                 pos_,
                 start - data_.begin(),
                 row,
                 column);
}

RToken RTokenizer::matchQuotedIdentifier()
{
   std::size_t length = tokenLength(tokenPatterns().QUOTED_IDENTIFIER);
   if (length == 0)
      return consumeToken(RToken::ERR, 1);
   else
      return consumeToken(RToken::ID, length);
}

RToken RTokenizer::matchComment()
{
   return consumeToken(RToken::COMMENT, tokenLength(tokenPatterns().COMMENT));
}

RToken RTokenizer::matchUserOperator()
{
   std::size_t length = tokenLength(tokenPatterns().USER_OPERATOR);
   if (length == 0)
      return consumeToken(RToken::ERR, 1);
   else
      return consumeToken(RToken::UOPER, length);
}


RToken RTokenizer::matchOperator()
{
   wchar_t cNext = peek(1) ;
   wchar_t cNextNext = peek(2);

   switch (peek())
   {
   case L':': // :::, ::, :=
   {
      if (cNext == L'=')
         return consumeToken(RToken::OPER, 2);
      else
         return consumeToken(RToken::OPER, 1 + (cNext == L':') + (cNextNext == L':'));
   }
      
   case L'|':
      return consumeToken(RToken::OPER, cNext == L'|' ? 2 : 1);
      
   case L'&':
      return consumeToken(RToken::OPER, cNext == L'&' ? 2 : 1);
      
   case L'<': // <=, <-, <<-
      
      if (cNext == L'=' || cNext == L'-') // <=, <-
         return consumeToken(RToken::OPER, 2);
      else if (cNext == L'<')
      {
         if (cNextNext == L'-') // <<-
            return consumeToken(RToken::OPER, 3); 
      }
      else // plain old <
         return consumeToken(RToken::OPER, 1);
      
   case L'-': // also -> and ->>
      if (cNext == L'>')
         return consumeToken(RToken::OPER, cNextNext == L'>' ? 3 : 2);
      else
         return consumeToken(RToken::OPER, 1);
      
   case L'*': // '*' and '**' (which R's parser converts to '^')
      return consumeToken(RToken::OPER, cNext == L'*' ? 2 : 1);
      
   case L'+': case L'/': case L'?':
   case L'^': case L'~': case L'$': case L'@':
      // single-character operators
      return consumeToken(RToken::OPER, 1) ;
      
   case L'>': // also >=
      return consumeToken(RToken::OPER, cNext == L'=' ? 2 : 1) ;
      
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

std::size_t RTokenizer::tokenLength(const boost::wregex& regex)
{
   boost::wsmatch match;
   std::wstring::const_iterator end = data_.end();
   boost::match_flag_type flg = boost::match_default | boost::match_continuous;
   if (regex_utils::search(pos_, end, match, regex, flg))
      return match.length();
   else
      return 0;
}

void RTokenizer::eatUntil(const boost::wregex& regex)
{
   boost::wsmatch match;
   std::wstring::const_iterator end = data_.end();
   if (regex_utils::search(pos_, end, match, regex))
   {
      pos_ = match[0].first;
   }
   else
   {
      // eat all on failure to match
      pos_ = data_.end();
   }
}


RToken RTokenizer::consumeToken(RToken::TokenType tokenType,
                                std::size_t length)
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
   
   // Get the row, column for this token
   std::size_t row = row_;
   std::size_t column = column_;
   
   // Update the row, column for the next token.
   updatePosition(pos_, length, &row_, &column_);
   
   std::wstring::const_iterator start = pos_ ;
   pos_ += length ;
   return RToken(tokenType,
                 start,
                 pos_,
                 start - data_.begin(),
                 row,
                 column);
}

class ConversionCache
{
public:
   
   typedef std::wstring key_type;
   typedef std::string mapped_type;
   
   bool contains(const RToken& token) const
   {
      return database_.count(token.content());
   }
   
   std::string& get(const RToken& token)
   {
      return database_[token.content()];
   }
   
   void put(const RToken& token, const mapped_type& value)
   {
      database_[token.content()] = value;
   }
   
private:
   std::map<key_type, mapped_type> database_;
};

ConversionCache& conversionCache()
{
   static ConversionCache instance;
   return instance;
}

const std::string& RToken::contentAsUtf8() const
{
   ConversionCache& cache = conversionCache();
   if (cache.contains(*this))
      return cache.get(*this);
   
   std::string result = string_utils::wideToUtf8(content());
   cache.put(*this, result);
   return cache.get(*this);
}

std::string RToken::asString() const
{
   std::stringstream ss;
   ss << "('" << contentAsUtf8() << "', " << row_ << ", " << column_ << ")";
   return ss.str();
}

} // namespace r_util
} // namespace core 
} // namespace rstudio


