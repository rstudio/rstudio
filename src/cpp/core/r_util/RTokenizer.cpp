/*
 * RTokenizer.cpp
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

#ifdef __APPLE__
# include <CoreFoundation/CoreFoundation.h>
#endif

#include <core/r_util/RTokenizer.hpp>

#include <boost/regex.hpp>

#include <iostream>
#include <sstream>

#include <shared_core/Error.hpp>

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
        USER_OPERATOR(L"%[^\\n%]*%"),
        WHITESPACE(L"[\\s\x00A0\x3000]+"),
        COMMENT(L"#[^\\n]*$")
   {
   }

public:
   const boost::wregex NUMBER;
   const boost::wregex HEX_NUMBER;
   const boost::wregex USER_OPERATOR;
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

Error tokenizeError(const std::string& reason, const ErrorLocation& location)
{
   Error error(boost::system::errc::invalid_argument, location);
   error.addProperty("reason", reason);
   return error;
}

// on macOS, std::iswalnum() and other variants don't seem to handle certain
// unicode character categories?
//
// https://github.com/rstudio/rstudio/issues/15316
bool isAlphanumeric(wchar_t ch)
{
#ifdef __APPLE__
   
   // if this appears to be an ASCII value, then use the standard library;
   // otherwise, use core foundation
   if (ch < static_cast<wchar_t>(128))
   {
      return iswalnum(ch);
   }
   else
   {
      CFStringRef string = CFStringCreateWithBytes(
               kCFAllocatorDefault,
               (const UInt8*) &ch,
               sizeof (wchar_t),
               kCFStringEncodingUTF32LE,
               false);

      CFCharacterSetRef set = CFCharacterSetGetPredefined(kCFCharacterSetAlphaNumeric);
      CFRange range = CFRangeMake(0, CFStringGetLength(string));
      bool result = CFStringFindCharacterFromSet(string, set, range, 0, NULL);
      CFRelease(string);
      return result;
   }
   
#else
   return iswalnum(ch);
#endif
}

} // anonymous namespace

RToken RTokenizer::nextToken()
{
  if (eol())
     return RToken();

  wchar_t c = peek();

  // check for raw string literals
  if (c == L'r' || c == L'R')
  {
     wchar_t next = peek(1);
     if (next == L'"' || next == L'\'')
     {
        RToken token;
        Error error = matchRawStringLiteral(&token);
        if (!error)
           return token;
     }
  }
  
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
           return consumeToken(RToken::RDBRACKET, 2);
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
  case L'`':
     return matchDelimited();
  case L'#':
     return matchComment();
  case L'%':
     return matchUserOperator();
  case L' ': case L'\t': case L'\r': case L'\n':
  case L'\x00A0': case L'\x3000':
     return matchWhitespace();
  case L'\\':
     return matchIdentifier();
     
  case L'_':
     // R 4.2.0 introduced the pipe-bind operator;
     // parse that as a special identifier.
     return consumeToken(RToken::ID, 1);
  }

  wchar_t cNext = peek(1);

  if ((c >= L'0' && c <= L'9')
        || (c == L'.' && cNext >= L'0' && cNext <= L'9'))
  {
     RToken numberToken = matchNumber();
     if (numberToken.length() > 0)
        return numberToken;
  }

  if (isAlphanumeric(c) || c == L'.')
  {
     // From Section 10.3.2, identifiers must not start with
     // a digit, nor may they start with a period followed by
     // a digit.
     //
     // Since we're not checking for either condition, we must
     // match on identifiers AFTER we have already tried to
     // match on number.
     return matchIdentifier();
  }
  
  // check for embedded knitr chunks
  RToken embeddedChunk = matchKnitrEmbeddedChunk();
  if (embeddedChunk)
     return embeddedChunk;

  RToken oper = matchOperator();
  if (oper)
     return oper;

  // Error!!
  return consumeToken(RToken::ERR, 1);
}



RToken RTokenizer::matchWhitespace()
{
   return consumeToken(RToken::WHITESPACE, tokenLength(tokenPatterns().WHITESPACE));
}

Error RTokenizer::matchRawStringLiteral(RToken* pToken)
{
   auto start = pos_;
   
   // consume leading 'r' or 'R'
   wchar_t firstChar = eat();
   if (!(firstChar == L'r' || firstChar == L'R'))
   {
      pos_ = start;
      return tokenizeError(
               "expected 'r' or 'R' at start of raw string literal",
               ERROR_LOCATION);
   }
   
   // consume quote character
   wchar_t quoteChar = eat();
   if (!(quoteChar == L'"' || quoteChar == L'\''))
   {
      pos_ = start;
      return tokenizeError(
               "expected quote character at start of raw string literal",
               ERROR_LOCATION);
   }
   
   // consume an optional number of hyphens
   int hyphenCount = 0;
   wchar_t ch = eat();
   while (ch == L'-')
   {
      hyphenCount++;
      ch = eat();
   }
   
   // okay, we're now sitting on open parenthesis
   wchar_t lhs = ch;
   
   // form right boundary character based on consumed parenthesis.
   // if it wasn't a parenthesis, just look for the associated closing quote
   wchar_t rhs;
   if (lhs == L'(')
   {
      rhs = L')';
   }
   else if (lhs == L'{')
   {
      rhs = L'}';
   }
   else if (lhs == L'[')
   {
      rhs = L']';
   }
   else
   {
      pos_ = start;
      return tokenizeError(
               "expected opening bracket at start of raw string literal",
               ERROR_LOCATION);
   }
   
   // start searching for the end of the raw string
   bool valid = false;
   
   while (true)
   {
      // i know, i know -- a label!? we use that here just because
      // we need to 'break' out of a nested loop below, and just
      // using a simple goto is cleaner than having e.g. an extra
      // boolean flag tracking whether we should 'continue'
      LOOP:
      
      if (eol())
         break;
      
      // find the boundary character
      wchar_t ch = eat();
      if (ch != rhs)
         goto LOOP;
      
      // consume hyphens
      for (int i = 0; i < hyphenCount; i++)
      {
         ch = peek();
         if (ch != L'-')
            goto LOOP;
         ++pos_;
      }
      
      // consume quote character
      ch = peek();
      if (ch != quoteChar)
         goto LOOP;
      ++pos_;
      
      // we're at the end of the string; break out of the loop
      valid = true;
      break;
   }
   
   // update position
   std::size_t row = row_;
   std::size_t column = column_;
   updatePosition(start, pos_ - start, &row_, &column_);
 
   // set token and return success
   auto type = valid ? RToken::STRING : RToken::ERR;
   *pToken = RToken(type, start, pos_, start - data_.begin(), row, column);
   return Success();
}

RToken RTokenizer::matchDelimited()
{
   auto start = pos_;
   auto quote = eat();

   while (!eol())
   {
      wchar_t ch = eat();
      
      // skip over escaped characters
      if (ch == L'\\')
      {
         if (!eol())
         {
            eat();
            continue;
         }
      }
      
      // check for matching quote
      if (ch == quote)
      {
         break;
      }
   }
   
   // because delimited items can contain newlines,
   // update our row + column position after parsing the token
   std::size_t row = row_;
   std::size_t column = column_;
   updatePosition(start, pos_ - start, &row_, &column_);

   // NOTE: the Java version of the tokenizer returns a special RStringToken
   // subclass which includes the wellFormed flag as an attribute. Our
   // implementation of RToken is stack based so doesn't support subclasses
   // (because they will be sliced when copied). If we need the well
   // formed flag we can just add it onto RToken.
   return RToken(quote == L'`' ? RToken::ID : RToken::STRING,
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
   std::wstring::const_iterator start = pos_;
   
   bool match = true;
   while (match)
   {
      eat();
    
      wchar_t ch = peek();
      match = isAlphanumeric(ch) || ch == L'.' || ch == L'_';
   }
   
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

RToken RTokenizer::matchKnitrEmbeddedChunk()
{
   wchar_t ch;
   
   // bail if we don't start with '<<' here
   if (peek(0) != L'<' ||
       peek(1) != L'<')
   {
      return RToken();
   }
   
   // consume the chunk label, looking for '>>'
   for (int offset = 1; ; offset++)
   {
      // give up on newlines or EOF
      ch = peek(offset);
      if (ch == 0 || ch == L'\n')
         return RToken();
      
      // look for closing '>>'
      if (peek(offset + 0) == L'>' &&
          peek(offset + 1) == L'>')
      {
         return consumeToken(RToken::STRING, offset + 2);
      }
   }
   
   return RToken();
}

RToken RTokenizer::matchOperator()
{
   wchar_t cNext = peek(1);

   switch (peek())
   {
   
   case L':': // :::, ::, :=
   {
      if (cNext == L'=')
      {
         return consumeToken(RToken::OPER, 2);
      }
      else if (cNext == L':')
      {
         wchar_t cNextNext = peek(2);
         return consumeToken(RToken::OPER, cNextNext == L':' ? 3 : 2);
      }
   }
      
   case L'|': // ||, |>, |
      if (cNext == L'|' || cNext == L'>')
         return consumeToken(RToken::OPER, 2);
      else
         return consumeToken(RToken::OPER, 1);
      
   case L'&': // &&, &
      return consumeToken(RToken::OPER, cNext == L'&' ? 2 : 1);
      
   case L'<': // <=, <-, <<-, <
      if (cNext == L'=' || cNext == L'-') // <=, <-
      {
         return consumeToken(RToken::OPER, 2);
      }
      else if (cNext == L'<')
      {
         wchar_t cNextNext = peek(2);
         if (cNextNext == L'-') // <<-
            return consumeToken(RToken::OPER, 3);
      }
      else // plain old <
      {
         return consumeToken(RToken::OPER, 1);
      }
      
   case L'-': // also -> and ->>
      if (cNext == L'>')
      {
         wchar_t cNextNext = peek(2);
         return consumeToken(RToken::OPER, cNextNext == L'>' ? 3 : 2);
      }
      else
      {
         return consumeToken(RToken::OPER, 1);
      }
      
   case L'*': // '*' and '**' (which R's parser converts to '^')
      return consumeToken(RToken::OPER, cNext == L'*' ? 2 : 1);
      
   case L'+': case L'/': case L'?':
   case L'^': case L'~': case L'$': case L'@':
      // single-character operators
      return consumeToken(RToken::OPER, 1);
      
   case L'>': // also >=
      return consumeToken(RToken::OPER, cNext == L'=' ? 2 : 1);
      
   case L'=': // also =>, ==
      if (cNext == L'=' || cNext == '>')
         return consumeToken(RToken::OPER, 2);
      else
         return consumeToken(RToken::OPER, 1);
         
   case L'!': // also !=
      return consumeToken(RToken::OPER, cNext == L'=' ? 2 : 1);
      
   default:
      return RToken();
   }
}

bool RTokenizer::eol()
{
   return pos_ >= data_.end();
}

wchar_t RTokenizer::peek()
{
   return peek(0);
}

wchar_t RTokenizer::peek(int lookahead)
{
   // NOTE: MSVC is extra picky in debug mode, so when we compare
   // iterators here we need to make sure we don't construct an iterator
   // that points outside of the string from which it was derived
   // this is a safer way of checking pos_ + lookahead >= data_.end()
   if (UNLIKELY(data_.end() - pos_ <= lookahead))
      return 0;

   return *(pos_ + lookahead);
}

wchar_t RTokenizer::eat()
{
   wchar_t result = *pos_;
   pos_++;
   return result;
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
   
   std::wstring::const_iterator start = pos_;
   pos_ += length;
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


