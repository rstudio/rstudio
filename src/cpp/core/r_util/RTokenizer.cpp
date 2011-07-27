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
      : NUMBER("[0-9]*(\\.[0-9]*)?([eE][+-]?[0-9]*)?[Li]?"),
        HEX_NUMBER("0x[0-9a-fA-F]*L?"),
        USER_OPERATOR("%[^%]*%"),
        REST_OF_IDENTIFIER("[\\w.]*"),
        UNTIL_END_QUOTE("[\\\\\'\"]"),
        WHITESPACE("[\\s\\u00A0]+")
   {
   }

public:
   const boost::regex NUMBER;
   const boost::regex HEX_NUMBER;
   const boost::regex USER_OPERATOR;
   const boost::regex REST_OF_IDENTIFIER;
   const boost::regex UNTIL_END_QUOTE;
   const boost::regex WHITESPACE;
};

TokenPatterns& tokenPatterns()
{
   static TokenPatterns instance;
   return instance;
}

} // anonymous namespace


const int RToken::LPAREN         = '(';
const int RToken::RPAREN         = ')';
const int RToken::LBRACKET       = '[';
const int RToken::RBRACKET       = ']';
const int RToken::LBRACE         = '{';
const int RToken::RBRACE         = '}';
const int RToken::COMMA          = ',';
const int RToken::SEMI           = ';';
const int RToken::WHITESPACE     = 0x1001;
const int RToken::STRING         = 0x1002;
const int RToken::NUMBER         = 0x1003;
const int RToken::ID             = 0x1004;
const int RToken::OPER           = 0x1005;
const int RToken::UOPER          = 0x1006;
const int RToken::ERROR          = 0x1007;
const int RToken::LDBRACKET      = 0x1008;
const int RToken::RDBRACKET      = 0x1009;

struct RToken::Impl
{
   Impl(int type,
        const std::string& content,
        std::size_t offset,
        std::size_t length)
      : type(type), content(content), offset(offset), length(length)
   {
   }
   int type;
   std::string content;
   std::size_t offset;
   std::size_t length;
};

RToken::RToken()
   : pImpl_()
{
}

RToken::RToken(int type,
               const std::string& content,
               std::size_t offset,
               std::size_t length)
   : pImpl_(new Impl(type, content, offset, length))
{
}

RToken::~RToken()
{
}



int RToken::type() const
{
   return pImpl_->type;
}

const std::string& RToken::content() const
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

RTokenRange::RTokenRange(const std::string& code)
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


void RTokenizer::asTokens(const std::string& code,
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

  char c = peek() ;

  switch (c)
  {
  case '(': case ')':
  case '{': case '}':
  case ';': case ',':
     return consumeToken(c, 1) ;
  case '[':
     if (peek(1) == '[')
        return consumeToken(RToken::LDBRACKET, 2) ;
     else
        return consumeToken(c, 1) ;
  case ']':
     if (peek(1) == ']')
        return consumeToken(RToken::RDBRACKET, 2) ;
     else
        return consumeToken(c, 1) ;
  case '"':
  case '\'':
     return matchStringLiteral() ;
  case ' ': case '\t': case '\r': case '\n':
     return matchWhitespace() ;
  }

  char cNext = peek(1) ;

  if ((c >= '0' && c <= '9')
        || (c == '.' && cNext >= '0' && cNext <= '9'))
  {
     RToken numberToken = matchNumber() ;
     if (numberToken.length() > 0)
        return numberToken ;
  }

  if (std::isalpha(c) || c == '.')
  {
     // From Section 10.3.2, identifiers must not start with
     // a period followed by a digit.
     //
     // Since we're not checking that the second character is
     // not a digit, we must match on identifiers AFTER we have
     // already tried to match on number.
     return matchIdentifier() ;
  }

  if (c == '%')
     return matchUserOperator() ;

  RToken oper = matchOperator() ;
  if (oper)
     return oper ;

  // Error!!
  return consumeToken(RToken::ERROR, 1) ;
}



RToken RTokenizer::matchWhitespace()
{
   std::string whitespace = peek(tokenPatterns().WHITESPACE) ;
   return consumeToken(RToken::WHITESPACE, whitespace.length()) ;
}

RToken RTokenizer::matchStringLiteral()
{
   std::string::const_iterator start = pos_ ;
   char quot = eat() ;

   bool wellFormed = false ;

   while (!eol())
   {
      eatUntil(tokenPatterns().UNTIL_END_QUOTE);

      if (eol())
         break ;

      char c = eat() ;
      if (c == quot)
      {
         wellFormed = true ;
         break ;
      }

      if (c == '\\')
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
                       std::string(start, pos_),
                       start - data_.begin(),
                       pos_ - start,
                       wellFormed) ;
}

RToken RTokenizer::matchNumber()
{
   std::string num = peek(tokenPatterns().HEX_NUMBER) ;
   if (num.empty())
      num = peek(boost::regex(tokenPatterns().NUMBER)) ;

   return consumeToken(RToken::NUMBER, num.length());
}

RToken RTokenizer::matchIdentifier()
{
   std::string::const_iterator start = pos_ ;
   eat();
   std::string rest = peek(tokenPatterns().REST_OF_IDENTIFIER) ;
   pos_ += rest.length() ;
   return RToken(RToken::ID,
                 std::string(start, pos_),
                 start - data_.begin(),
                 pos_ - start) ;
}

RToken RTokenizer::matchUserOperator()
{
   std::string oper = peek(tokenPatterns().USER_OPERATOR) ;
   if (oper.empty())
      return consumeToken(RToken::ERROR, 1) ;
   else
      return consumeToken(RToken::UOPER, oper.length()) ;
}


RToken RTokenizer::matchOperator()
{
   char cNext = peek(1) ;

   switch (peek())
   {
   case '+': case '*': case '/':
   case '^': case '&': case '|':
   case '~': case '$': case ':':
      // single-character operators
      return consumeToken(RToken::OPER, 1) ;
   case '-': // also ->
      return consumeToken(RToken::OPER, cNext == '>' ? 2 : 1) ;
   case '>': // also >=
      return consumeToken(RToken::OPER, cNext == '=' ? 2 : 1) ;
   case '<': // also <- and <=
      return consumeToken(RToken::OPER, cNext == '=' ? 2 :
                                       cNext == '-' ? 2 :
                                       1) ;
   case '=': // also ==
      return consumeToken(RToken::OPER, cNext == '=' ? 2 : 1) ;
   case '!': // also !=
      return consumeToken(RToken::OPER, cNext == '=' ? 2 : 1) ;
   default:
      return RToken() ;
   }
}

bool RTokenizer::eol()
{
   return pos_ >= data_.end();
}

char RTokenizer::peek()
{
   return peek(0) ;
}

char RTokenizer::peek(std::size_t lookahead)
{
   if ((pos_ + lookahead) >= data_.end())
      return 0 ;
   else
      return *(pos_ + lookahead) ;
}

char RTokenizer::eat()
{
   char result = *pos_;
   pos_++ ;
   return result ;
}

std::string RTokenizer::peek(const boost::regex& regex)
{
   boost::smatch match;
   std::string::const_iterator end = data_.end();
   boost::match_flag_type flg = boost::match_default | boost::match_continuous;
   if (boost::regex_search(pos_, end, match, regex, flg))
   {
      return match[0];
   }
   else
   {
      return std::string();
   }
}

void RTokenizer::eatUntil(const boost::regex& regex)
{
   boost::smatch match;
   std::string::const_iterator end = data_.end();
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


RToken RTokenizer::consumeToken(int tokenType, std::size_t length)
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

   std::string::const_iterator start = pos_ ;
   pos_ += length ;
   return RToken(tokenType,
                 std::string(start, pos_),
                 start - data_.begin(),
                 length) ;
}


} // namespace r_util
} // namespace core 


