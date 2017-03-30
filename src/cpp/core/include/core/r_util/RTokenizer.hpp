/*
 * RTokenizer.hpp
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

#ifndef CORE_R_UTIL_R_TOKENIZER_HPP
#define CORE_R_UTIL_R_TOKENIZER_HPP

#include <string>
#include <vector>
#include <deque>
#include <algorithm>
#include <iostream>
#include <sstream>

#include <core/RegexUtils.hpp>
#include <core/StringUtils.hpp>
#include <core/collection/Position.hpp>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/regex.hpp>

#include <core/Macros.hpp>

// On Linux confirm that wchar_t is Unicode
#if !defined(_WIN32) && !defined(__APPLE__) && !defined(__STDC_ISO_10646__)
   #error "wchar_t is not Unicode"
#endif

namespace rstudio {
namespace core {

class Error;

namespace r_util {

// Make RToken non-subclassable (since it has copy/byval semantics any
// subclass would be sliced
class RToken_lock
{
   friend class RToken ;
private:
   RToken_lock() {}
   RToken_lock(const RToken_lock&) {}
};

// RToken. Note that RToken instances are only valid as long as the class
// which yielded them (RTokenizer or RTokens) is alive. This is because
// they contain iterators into the original source data rather than their
// own copy of their contents.
class RToken : public virtual RToken_lock
{
public:

   enum TokenType {
      LPAREN,
      RPAREN,
      LBRACKET,
      RBRACKET,
      LDBRACKET,
      RDBRACKET,
      LBRACE,
      RBRACE,
      COMMA,
      SEMI,
      WHITESPACE,
      STRING,
      NUMBER,
      ID,
      OPER,
      UOPER,
      ERR,
      COMMENT
   };

public:
   
   RToken()
      : offset_(-1)
   {}
   
   explicit RToken(TokenType type)
      : type_(type), offset_(-1)
   {}

   RToken(TokenType type,
          std::wstring::const_iterator begin,
          std::wstring::const_iterator end,
          std::size_t offset,
          std::size_t row,
          std::size_t column)
      : type_(type), begin_(begin), end_(end),
        offset_(offset), row_(row), column_(column)
   {
   }
   
   // accessors
   TokenType type() const { return type_; }
   std::wstring content() const { return std::wstring(begin_, end_); }
   const std::string& contentAsUtf8() const;
   std::size_t offset() const { return offset_; }
   std::size_t length() const { return end_ - begin_; }
   std::size_t row() const { return row_; }
   std::size_t column() const { return column_; }
   
   core::collection::Position position() const
   {
      return core::collection::Position(row_, column_);
   }

   // efficient comparison operations
   bool contentEquals(const std::wstring& text) const
   {
      std::size_t distance = std::distance(begin_, end_);
      return distance == text.size() &&
             std::equal(begin_, end_, text.begin());
   }
   
   bool contentEquals(wchar_t character) const
   {
      return std::distance(begin_, end_) == 1 && *begin_ == character;
   }
   
   bool contentContains(const wchar_t character) const
   {
      return std::find(begin_, end_, character) != end_;
   }

   bool contentStartsWith(const std::wstring& text) const
   {
      return std::search(begin_, end_, text.begin(), text.end()) == begin_;
   }

   bool isOperator(const std::wstring& op) const
   {
      return (type_ == RToken::OPER) &&
              std::equal(begin_, end_, op.begin());
   }

   bool isType(TokenType type) const
   {
      return type_ == type;
   }

   // allow direct use in conditional statements (nullability)
   typedef void (*unspecified_bool_type)();
   static void unspecified_bool_true() {}
   operator unspecified_bool_type() const
   {
      return offset_ == static_cast<std::size_t>(-1) ?
                                             0 :
                                             unspecified_bool_true;
   }
   bool operator!() const
   {
      return offset_ == static_cast<std::size_t>(-1);
   }
   
   std::wstring::const_iterator begin() const
   {
      return begin_;
   }
   
   std::wstring::const_iterator end() const
   {
      return end_;
   }
   
   std::pair<std::wstring::const_iterator, std::wstring::const_iterator> range() const
   {
      return std::make_pair(begin_, end_);
   }
   
   std::string asString() const;
   friend std::ostream& operator <<(std::ostream& os,
                                    const RToken& self)
   {
      return os << self.asString();
   }

private:
   TokenType type_;
   std::wstring::const_iterator begin_;
   std::wstring::const_iterator end_;
   std::size_t offset_;
   std::size_t row_;
   std::size_t column_;
};

// Tokenize R code. Note that the RToken instances which are returned are
// valid only during the lifetime of the RTokenizer which yielded them
// (because they store iterators into their content rather than making a copy
// of the content)
class RTokenizer : boost::noncopyable
{
public:
   explicit RTokenizer(const std::wstring& data)
      : data_(data),
        begin_(data_.begin()),
        end_(data_.end()),
        pos_(data_.begin()),
        row_(0),
        column_(0)
   {
   }

   virtual ~RTokenizer() {}

   // COPYING: boost::noncopyable

   RToken nextToken();

private:
   RToken matchWhitespace();
   RToken matchNewline();
   RToken matchStringLiteral();
   RToken matchNumber();
   RToken matchIdentifier();
   RToken matchQuotedIdentifier();
   RToken matchComment();
   RToken matchUserOperator();
   RToken matchOperator();
   bool eol();
   wchar_t peek();
   wchar_t peek(std::size_t lookahead);
   wchar_t eat();
   std::size_t tokenLength(const boost::wregex& regex);
   void eatUntil(const boost::wregex& regex);
   RToken consumeToken(RToken::TokenType tokenType, std::size_t length);
   
private:
   std::wstring data_;
   std::wstring::const_iterator begin_;
   std::wstring::const_iterator end_;
   std::wstring::const_iterator pos_;
   std::size_t row_;
   std::size_t column_;
   std::vector<char> braceStack_; // needed for tokenization of `[[`, `[`
};


// Set of RTokens. Note that the RTokens returned from the set
// are conceptually iterators so are only valid for the lifetime of
// the RTokens object which yielded them.
class RTokens
{
   typedef std::vector<RToken> Tokens;
   
public:
   enum Flags
   {
      None = 0,
      StripWhitespace = 1,
      StripComments = 2
   };

public:
   
   void push_back(const RToken& rToken) { tokens_.push_back(rToken); }
   std::size_t size() const { return tokens_.size(); }
   bool empty() const { return tokens_.empty(); }
   
   // Safe 'at' method that returns a dummy token if
   // an out of bounds offset is specified.
   const RToken& at(std::size_t offset) const
   {
      if (UNLIKELY(offset >= tokens_.size()))
         return dummyToken_;
      return tokens_[offset];
   }
   
   // Unsafe 'at' method that should only used for functions that
   // have validated the range they will be iterating over
   const RToken& atUnsafe(std::size_t offset) const
   {
      return tokens_[offset];
   }
   
   typedef Tokens::const_iterator const_iterator;
   typedef Tokens::iterator iterator;
   
   const_iterator begin() const { return tokens_.begin(); }
   const_iterator end() const { return tokens_.end(); }
   
   RTokens()
      : tokenizer_(L""),
        dummyToken_(RToken::ERR)
   {}
   
   explicit RTokens(const std::wstring& code, int flags = None)
      : tokenizer_(code), dummyToken_(RToken::ERR)
   {
      RToken token;
      while ((token = tokenizer_.nextToken()))
      {
         if ((flags & StripWhitespace) && token.type() == RToken::WHITESPACE)
            continue;

         if ((flags & StripComments) && token.type() == RToken::COMMENT)
            continue;

         push_back(token);
      }
   }
   
   friend std::ostream& operator <<(std::ostream& os,
                                    const RTokens& rTokens)
   {
      for (std::size_t i = 0, n = rTokens.size(); i < n; ++i)
         os << rTokens.atUnsafe(i) << std::endl;
      return os;
   }

private:
    RTokenizer tokenizer_;
    Tokens tokens_;
    RToken dummyToken_;
};

namespace token_utils {

inline bool isBinaryOp(const RToken& token)
{
   if (token.contentEquals(L'!'))
      return false;
   
   return token.isType(RToken::OPER) ||
          token.isType(RToken::UOPER);
}

inline bool isLocalLeftAssign(const RToken& token)
{
   return token.isType(RToken::OPER) && (
            token.contentEquals(L"=") ||
            token.contentEquals(L"<-") ||
            token.contentEquals(L":="));
}

inline bool isLocalRightAssign(const RToken& token)
{
   return token.isType(RToken::OPER) && token.contentEquals(L"->");
}

inline bool isParentLeftAssign(const RToken& token)
{
   return token.isType(RToken::OPER) &&
          token.contentEquals(L"<<-");
}

inline bool isParentRightAssign(const RToken& token)
{
   return token.isType(RToken::OPER) &&
          token.contentEquals(L"->>");
}

inline bool isLeftAssign(const RToken& token)
{
   return token.isType(RToken::OPER) && (
            token.contentEquals(L"=") ||
            token.contentEquals(L"<-") ||
            token.contentEquals(L"<<-") ||
            token.contentEquals(L":="));
}

inline bool isRightAssign(const RToken& token)
{
   return token.isType(RToken::OPER) && (
            token.contentEquals(L"->") ||
            token.contentEquals(L"->>"));
}

inline bool isRightBracket(const RToken& rToken)
{
   return rToken.isType(RToken::RBRACE) ||
          rToken.isType(RToken::RBRACKET) ||
          rToken.isType(RToken::RDBRACKET) ||
          rToken.isType(RToken::RPAREN);
}

inline bool isLeftBracket(const RToken& rToken)
{
   return rToken.isType(RToken::LBRACE) ||
          rToken.isType(RToken::LBRACKET) ||
          rToken.isType(RToken::LDBRACKET) ||
          rToken.isType(RToken::LPAREN);
}

inline bool isDollar(const RToken& rToken)
{
   return rToken.isType(RToken::OPER) &&
          rToken.contentEquals(L"$");
}

inline bool isAt(const RToken& rToken)
{
   return rToken.isType(RToken::OPER) &&
          rToken.contentEquals(L"@");
}

inline bool isId(const RToken& rToken)
{
   return rToken.isType(RToken::ID);
}

inline bool isNamespaceExtractionOperator(const RToken& rToken)
{
   return rToken.isType(RToken::OPER) && (
            rToken.contentEquals(L"::") ||
            rToken.contentEquals(L":::"));
}

inline bool isFunction(const RToken& rToken)
{
   return rToken.isType(RToken::ID) &&
          rToken.contentEquals(L"function");
}

inline bool isString(const RToken& rToken)
{
   return rToken.isType(RToken::STRING);
}

inline bool isComma(const RToken& rToken)
{
   return rToken.isType(RToken::COMMA);
}

inline bool isWhitespace(const RToken& rToken)
{
   return rToken.isType(RToken::WHITESPACE);
}

inline bool isWhitespaceOrComment(const RToken& rToken)
{
   return rToken.isType(RToken::WHITESPACE) ||
          rToken.isType(RToken::COMMENT);
}

inline bool isValidAsIdentifier(const RToken& rToken)
{
   return rToken.isType(RToken::ID) ||
          rToken.isType(RToken::NUMBER) ||
          rToken.isType(RToken::STRING);
}

inline bool hasNewline(const RToken& rToken)
{
   return rToken.isType(RToken::WHITESPACE) &&
          rToken.contentContains(L'\n');
}

inline bool isValidAsUnaryOperator(const RToken& rToken)
{
   return rToken.contentEquals(L"-") ||
          rToken.contentEquals(L"+") ||
          rToken.contentEquals(L"!") ||
          rToken.contentEquals(L"?") ||
          rToken.contentEquals(L"~");
}

inline bool canStartExpression(const RToken& rToken)
{
   return isValidAsUnaryOperator(rToken) ||
          isValidAsIdentifier(rToken);
}

inline bool isExtractionOperator(const RToken& rToken)
{
   return isNamespaceExtractionOperator(rToken) ||
          isDollar(rToken) ||
          isAt(rToken);
}

inline bool isBlank(const RToken& rToken)
{
   return isWhitespace(rToken) && !rToken.contentContains(L'\n');
}

inline bool isWhitespaceWithNewline(const RToken& rToken)
{
   return isWhitespace(rToken) && rToken.contentContains(L'\n');
}

inline bool canOpenArgumentList(const RToken& rToken)
{
   return rToken.isType(RToken::LPAREN) ||
          rToken.isType(RToken::LBRACKET) ||
          rToken.isType(RToken::LDBRACKET);
}

inline bool canCloseArgumentList(const RToken& rToken)
{
   return rToken.isType(RToken::RPAREN) ||
          rToken.isType(RToken::RBRACKET) ||
          rToken.isType(RToken::RDBRACKET);
}

inline RToken::TokenType typeComplement(RToken::TokenType lhsType)
{
   switch (lhsType)
   {
   case RToken::LPAREN:    return RToken::RPAREN;
   case RToken::LBRACE:    return RToken::RBRACE;
   case RToken::LBRACKET:  return RToken::RBRACKET;
   case RToken::LDBRACKET: return RToken::RDBRACKET;
      
   case RToken::RPAREN:    return RToken::LPAREN;
   case RToken::RBRACE:    return RToken::LBRACE;
   case RToken::RBRACKET:  return RToken::LBRACKET;
   case RToken::RDBRACKET: return RToken::LDBRACKET;
   default: return RToken::ERR;
   }
}

inline bool canContinueStatement(const RToken& rToken)
{
   return isBinaryOp(rToken) ||
           canOpenArgumentList(rToken);
}

inline bool isSymbolNamed(const RToken& rToken,
                          const std::wstring& name)
{
   // For strings, check if the content within the quotes
   // is equal to the name provided. TODO: handle escaped
   // quotes within
   if (rToken.isType(RToken::STRING) ||
       (rToken.isType(RToken::ID) && *rToken.begin() == L'`'))
   {
      std::size_t distance = std::distance(
               rToken.begin(), rToken.end());
      if (distance < 2) return false;
      return distance - 2 == name.size() &&
            std::equal(
               rToken.begin() + 1,
               rToken.end() - 1,
               name.begin());
   }
   
   return rToken.contentEquals(name);
}

inline std::string getSymbolName(const RToken& rToken)
{
   if (rToken.isType(RToken::STRING) ||
       (rToken.isType(RToken::ID) && *rToken.begin() == L'`'))
   {
       return string_utils::wideToUtf8(
          std::wstring(rToken.begin() + 1, rToken.end() - 1));
   }
   
   return rToken.contentAsUtf8();
}

inline bool canFollowBinaryOperator(const RToken& rToken)
{
   switch (rToken.type())
   {
   case RToken::ID:
   case RToken::LBRACE:
   case RToken::LPAREN:
   case RToken::NUMBER:
   case RToken::STRING:
      return true;
   default:
      ; // fall-through
   }
   
   if (isValidAsUnaryOperator(rToken))
      return true;
   
   return false;
}

inline bool isPipeOperator(const RToken& rToken)
{
   static const boost::wregex rePipe(L"^%[^>]*>+[^>]*%$");
   return regex_utils::match(rToken.begin(), rToken.end(), rePipe);
}

namespace {

std::vector<std::wstring> makeNaKeywords()
{
   std::vector<std::wstring> keywords;
   
   keywords.push_back(L"NA");
   keywords.push_back(L"NA_character_");
   keywords.push_back(L"NA_complex_");
   keywords.push_back(L"NA_integer_");
   keywords.push_back(L"NA_real_");
   
   return keywords;
}

} // anonymous namespace

inline bool isNaKeyword(const RToken& rToken)
{
   if (!rToken.isType(RToken::ID))
      return false;
   
   static const std::vector<std::wstring> naKeywords = makeNaKeywords();
   for (std::size_t i = 0, n = naKeywords.size(); i < n; ++i)
      if (rToken.contentEquals(naKeywords[i]))
         return true;
   return false;
}

} // end namespace token_utils

} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_ENVIRONMENT_HPP

