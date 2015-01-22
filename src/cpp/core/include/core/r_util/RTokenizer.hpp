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
#include <deque>
#include <algorithm>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/regex_fwd.hpp>

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

   static const wchar_t LPAREN;
   static const wchar_t RPAREN;
   static const wchar_t LBRACKET;
   static const wchar_t RBRACKET;
   static const wchar_t LBRACE;
   static const wchar_t RBRACE;
   static const wchar_t COMMA;
   static const wchar_t SEMI;
   static const wchar_t WHITESPACE;
   static const wchar_t STRING;
   static const wchar_t NUMBER;
   static const wchar_t ID;
   static const wchar_t OPER;
   static const wchar_t UOPER;
   static const wchar_t ERR;
   static const wchar_t LDBRACKET;
   static const wchar_t RDBRACKET;
   static const wchar_t COMMENT;

public:
   RToken()
      : offset_(-1)
   {
   }

   RToken(wchar_t type,
          std::wstring::const_iterator begin,
          std::wstring::const_iterator end,
          std::size_t offset)
      : type_(type), begin_(begin), end_(end), offset_(offset)
   {
   }

   // COPYING: via compiler (copyable members)

   // accessors
   wchar_t type() const { return type_; }
   std::wstring content() const { return std::wstring(begin_, end_); }
   std::string contentAsUtf8() const;
   std::size_t offset() const { return offset_; }
   std::size_t length() const { return end_ - begin_; }

   // efficient comparison operations
   bool contentEquals(const std::wstring& text) const
   {
      return std::equal(begin_, end_, text.begin());
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

   bool isType(wchar_t type) const
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

private:
   wchar_t type_;
   std::wstring::const_iterator begin_;
   std::wstring::const_iterator end_;
   std::size_t offset_;
};

// Tokenize R code. Note that the RToken instances which are returned are
// valid only during the lifetime of the RTokenizer which yielded them
// (because they store iterators into their content rather than making a copy
// of the content)
class RTokenizer : boost::noncopyable
{
public:
   explicit RTokenizer(const std::wstring& data)
      : data_(data), pos_(data_.begin())
   {
   }

   virtual ~RTokenizer() {}

   // COPYING: boost::noncopyable

   RToken nextToken();

private:
   RToken matchWhitespace();
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
   std::wstring peek(const boost::wregex& regex);
   void eatUntil(const boost::wregex& regex);
   RToken consumeToken(wchar_t tokenType, std::size_t length);

private:
   std::wstring data_;
   std::wstring::const_iterator pos_;
};


// Set of RTokens. Note that the RTokens returned from the set
// are conceptually iterators so are only valid for the lifetime of
// the RTokens object which yielded them.
class RTokens : public std::deque<RToken>, boost::noncopyable
{
public:
   enum Flags
   {
      None = 0,
      StripWhitespace = 1,
      StripComments = 2
   };

public:
   explicit RTokens(const std::wstring& code, int flags = None)
      : tokenizer_(code)
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

private:
    RTokenizer tokenizer_;
};

namespace token_utils {

inline bool isBinaryOp(const RToken& token)
{
   return token.isType(RToken::OPER) ||
          token.isType(RToken::UOPER);
}

inline bool isLocalLeftAssign(const RToken& token)
{
   return token.isType(RToken::OPER) && (
            token.content() == L"=" ||
            token.content() == L"<-");
}

inline bool isLocalRightAssign(const RToken& token)
{
   return token.isType(RToken::OPER) && (
            token.content() == L"->");
}

inline bool isGlobalLeftAssign(const RToken& token)
{
   return token.isType(RToken::OPER) &&
         token.content() == L"<<-";
}

inline bool isGlobalRightAssign(const RToken& token)
{
   return token.isType(RToken::OPER) &&
         token.content() == L"->>";
}

inline bool isLeftAssign(const RToken& token)
{
   return token.isType(RToken::OPER) && (
            token.content() == L"=" ||
            token.content() == L"<-" ||
            token.content() == L"<<-");
}

inline bool isRightAssign(const RToken& token)
{
   return token.isType(RToken::OPER) && (
            token.content() == L"->" ||
            token.content() == L"->>");
}

inline bool isRightBrace(const RToken& rToken)
{
   return rToken.isType(RToken::RBRACE) ||
          rToken.isType(RToken::RBRACKET) ||
          rToken.isType(RToken::RDBRACKET) ||
          rToken.isType(RToken::RPAREN);
}

inline bool isLeftBrace(const RToken& rToken)
{
   return rToken.isType(RToken::LBRACE) ||
          rToken.isType(RToken::LBRACKET) ||
          rToken.isType(RToken::LDBRACKET) ||
          rToken.isType(RToken::LPAREN);
}

inline bool isDollar(const RToken& rToken)
{
   return rToken.isType(RToken::OPER) &&
          rToken.content() == L"$";
}

inline bool isAt(const RToken& rToken)
{
   return rToken.isType(RToken::OPER) &&
          rToken.content() == L"@";
}

inline bool isId(const RToken& rToken)
{
   return rToken.isType(RToken::ID);
}

inline bool isNamespace(const RToken& rToken)
{
   return rToken.isType(RToken::OPER) && (
            rToken.content() == L"::" ||
            rToken.content() == L":::");
}

inline bool isFunction(const RToken& rToken)
{
   return rToken.isType(RToken::ID) &&
         rToken.content() == L"function";
}

inline bool isString(const RToken& rToken)
{
   return rToken.isType(RToken::STRING);
}

inline bool isComma(const RToken& rToken)
{
   return rToken.isType(RToken::COMMA);
}

} // end namespace token_utils

} // namespace r_util
} // namespace core 
} // namespace rstudio


#endif // CORE_R_UTIL_R_ENVIRONMENT_HPP

