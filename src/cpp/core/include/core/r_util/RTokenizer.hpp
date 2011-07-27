/*
 * RTokenizer.hpp
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

#ifndef CORE_R_UTIL_R_TOKENIZER_HPP
#define CORE_R_UTIL_R_TOKENIZER_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/regex_fwd.hpp>

// On Linux confirm that wchar_t is Unicode
#if !defined(_WIN32) && !defined(__APPLE__) && !defined(__STDC_ISO_10646__)
   #error "wchar_t is not Unicode"
#endif

namespace core {

class Error;

namespace r_util {

class RToken
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
   static const wchar_t ERROR;
   static const wchar_t LDBRACKET;
   static const wchar_t RDBRACKET;
   static const wchar_t COMMENT;

public:
   RToken();
   RToken(wchar_t type,
          std::wstring::const_iterator begin,
          std::wstring::const_iterator end,
          std::size_t offset,
          std::size_t length);
   virtual ~RToken();

   // COPYING: via copyable shared_ptr<Impl>

   // accessors
   wchar_t type() const;
   std::wstring content() const;
   std::size_t offset() const;
   std::size_t length() const;

   // allow direct use in conditional statements (nullability)
   typedef void (*unspecified_bool_type)();
   static void unspecified_bool_true() {}
   operator unspecified_bool_type() const
   {
      return pImpl_.get() == NULL ? 0 : unspecified_bool_true;
   }
   bool operator!() const
   {
      return pImpl_.get() == NULL;
   }

private:
   struct Impl;
   boost::shared_ptr<Impl> pImpl_;
};

bool operator==(const RToken& lhs, const RToken& rhs);

inline bool operator!=(const RToken& lhs, const RToken& rhs)
{
    return !(lhs == rhs);
}

class RStringToken : public RToken
{
public:
   RStringToken(wchar_t type,
                std::wstring::const_iterator begin,
                std::wstring::const_iterator end,
                std::size_t offset,
                std::size_t length,
                bool wellFormed)
      : RToken(type, begin, end, offset, length), wellFormed_(wellFormed)
   {
   }

   // COPYING: via compiler

public:
   bool wellFormed() const { return wellFormed_; }

private:
   bool wellFormed_;
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


// Tokenize R code and access it as a range of RTokens. Note that the RToken
// instances which are returned are valid only during the lifetime of
// the RTokenRange which yielded them (because they store iterators into
// their content rather than making a copy of the content)
class RTokenRange : boost::noncopyable
{
public:
   explicit RTokenRange(const std::wstring& code);
   virtual ~RTokenRange() {}

   // COPYING: boost::noncopyable

   bool isBOD() const;
   bool isEOD() const;

   RToken currentToken();
   RToken next();
   RToken prev();

   Error moveTo(std::size_t index);
   void moveToBOD();
   void moveToEOD();

private:
   void ensureValidIndex();

private:
   RTokenizer tokenizer_;
   std::vector<RToken> tokens_;
   std::size_t pos_;
   static const std::size_t NPOS;
};


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_ENVIRONMENT_HPP

