/*
 * RTokenizer.cpp
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

#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

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
        REST_OF_IDENTIFIER(L"[\\w.]*"),
        QUOTED_IDENTIFIER(L"`[^`]*`"),
        UNTIL_END_QUOTE(L"[\\\\\'\"]"),
        WHITESPACE(L"[\\s\\u00A0\u3000]+"),
        COMMENT(L"#.*?$")
   {
   }

public:
   const boost::wregex NUMBER;
   const boost::wregex HEX_NUMBER;
   const boost::wregex USER_OPERATOR;
   const boost::wregex REST_OF_IDENTIFIER;
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
const wchar_t RToken::ERROR          = 0x1007;
const wchar_t RToken::LDBRACKET      = 0x1008;
const wchar_t RToken::RDBRACKET      = 0x1009;
const wchar_t RToken::COMMENT        = 0x100A;

struct RToken::Impl
{
   Impl(wchar_t type,
        const std::wstring& content,
        std::size_t offset,
        std::size_t length)
      : type(type), content(content), offset(offset), length(length)
   {
   }
   wchar_t type;
   std::wstring content;
   std::size_t offset;
   std::size_t length;
};

RToken::RToken()
   : pImpl_()
{
}

RToken::RToken(wchar_t type,
               const std::wstring& content,
               std::size_t offset,
               std::size_t length)
   : pImpl_(new Impl(type, content, offset, length))
{
}

RToken::~RToken()
{
}



wchar_t RToken::type() const
{
   return pImpl_->type;
}

const std::wstring& RToken::content() const
{
   return pImpl_->content;
}

std::size_t RToken::offset() const
{
   return pImpl_->offset;
}

std::size_t RToken::length() const
{
   return pImpl_->length;
}


bool operator==(const RToken& lhs, const RToken& rhs)
{
   if (!lhs && !rhs)
   {
      return true;
   }
   else if (!lhs || !rhs)
   {
      return false;
   }
   else
   {
      return lhs.type() == rhs.type() &&
             lhs.content() == rhs.content() &&
             lhs.offset() == rhs.offset() &&
             lhs.length() == rhs.length();
   }
}

const std::size_t RTokenRange::NPOS = -1;

RTokenRange::RTokenRange(const std::wstring& code)
   : pos_(NPOS)
{
   RTokenizer::asTokens(code, &tokens_);
}

RTokenRange::RTokenRange(const std::vector<RToken>& tokens)
   : tokens_(tokens), pos_(NPOS)
{
}

bool RTokenRange::isBOD() const
{
   return pos_ == NPOS;
}

bool RTokenRange::isEOD() const
{
   return pos_ > tokens_.size();
}


RToken RTokenRange::currentToken()
{
   if (pos_ != NPOS && (tokens_.size() > pos_))
      return tokens_.at(pos_) ;
   else
      return RToken();
}

RToken RTokenRange::next()
{
   pos_++ ;
   ensureValidIndex() ;
   return currentToken() ;
}

RToken RTokenRange::prev()
{
   pos_-- ;
   ensureValidIndex() ;
   return currentToken() ;
}

Error RTokenRange::moveTo(std::size_t index)
{
   if (index > tokens_.size())
      return systemError(boost::system::errc::invalid_seek, ERROR_LOCATION);

   pos_ = index ;

   return Success();
}

void RTokenRange::moveToBOD()
{
   pos_ = NPOS;
}

void RTokenRange::moveToEOD()
{
   pos_ = tokens_.size();
}

void RTokenRange::ensureValidIndex()
{
   pos_ = std::min(std::max(NPOS, pos_), tokens_.size()) ;
}


void RTokenizer::asTokens(const std::wstring& code,
                          std::vector<RToken>* pTokens)
{
   RTokenizer tokenizer(code);
   RToken token;
   while (token = tokenizer.nextToken())
      pTokens->push_back(token);
}


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
  case L'\u00A0': case L'\u3000':
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

  if (std::iswalpha(c) || c == L'.')
  {
     // From Section 10.3.2, identifiers must not start with
     // a period followed by a digit.
     //
     // Since we're not checking that the second character is
     // not a digit, we must match on identifiers AFTER we have
     // already tried to match on number.
     return matchIdentifier() ;
  }

  RToken oper = matchOperator() ;
  if (oper)
     return oper ;

  // Error!!
  return consumeToken(RToken::ERROR, 1) ;
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

   bool wellFormed = false ;

   while (!eol())
   {
      eatUntil(tokenPatterns().UNTIL_END_QUOTE);

      if (eol())
         break ;

      wchar_t c = eat() ;
      if (c == quot)
      {
         wellFormed = true ;
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

   return RStringToken(RToken::STRING,
                       std::wstring(start, pos_),
                       start - data_.begin(),
                       pos_ - start,
                       wellFormed) ;
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
   std::wstring rest = peek(tokenPatterns().REST_OF_IDENTIFIER) ;
   pos_ += rest.length() ;
   return RToken(RToken::ID,
                 std::wstring(start, pos_),
                 start - data_.begin(),
                 pos_ - start) ;
}

RToken RTokenizer::matchQuotedIdentifier()
{
   std::wstring iden = peek(tokenPatterns().QUOTED_IDENTIFIER) ;
   if (iden.empty())
      return consumeToken(RToken::ERROR, 1);
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
      return consumeToken(RToken::ERROR, 1) ;
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
   else if (pos_ + length > data_.end())
   {
      LOG_WARNING_MESSAGE("Premature EOF");
      return RToken();
   }

   std::wstring::const_iterator start = pos_ ;
   pos_ += length ;
   return RToken(tokenType,
                 std::wstring(start, pos_),
                 start - data_.begin(),
                 length) ;
}


} // namespace r_util
} // namespace core 


