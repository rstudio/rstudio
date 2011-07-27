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

namespace core {

class Error;

namespace r_util {

class RToken
{
public:

   static const int LPAREN;
   static const int RPAREN;
   static const int LBRACKET;
   static const int RBRACKET;
   static const int LBRACE;
   static const int RBRACE;
   static const int COMMA;
   static const int SEMI;
   static const int WHITESPACE;
   static const int STRING;
   static const int NUMBER;
   static const int ID;
   static const int OPER;
   static const int UOPER;
   static const int ERROR;
   static const int LDBRACKET;
   static const int RDBRACKET;

public:
   RToken();
   RToken(int type,
          const std::string& content,
          std::size_t offset,
          std::size_t length);
   virtual ~RToken();

   // COPYING: via copyable shared_ptr<Impl>

   // accessors
   int type() const;
   const std::string& content() const;
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
   RStringToken(int type,
                const std::string& content,
                std::size_t offset,
                std::size_t length,
                bool wellFormed)
      : RToken(type, content, offset, length), wellFormed_(wellFormed)
   {
   }

   // COPYING: via compiler

public:
   bool wellFormed() const { return wellFormed_; }

private:
   bool wellFormed_;
};


class RTokenRange : boost::noncopyable
{
public:
   explicit RTokenRange(const std::string& code);
   explicit RTokenRange(const std::vector<RToken>& tokens);
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
   std::vector<RToken> tokens_;
   std::size_t pos_;
   static const std::size_t NPOS;
};


class RTokenizer : boost::noncopyable
{
public:
   static void asTokens(const std::string& code, std::vector<RToken>* pTokens);

public:
   explicit RTokenizer(const std::string& data)
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
   RToken matchUserOperator();
   RToken matchOperator();
   bool eol();
   char peek();
   char peek(std::size_t lookahead);
   char eat();
   std::string peek(const boost::regex& regex);
   void eatUntil(const boost::regex& regex);
   RToken consumeToken(int tokenType, std::size_t length);

private:
   std::string data_;
   std::string::const_iterator pos_;
};


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_ENVIRONMENT_HPP

