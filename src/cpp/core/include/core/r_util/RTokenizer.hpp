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

namespace core {

class Error;

namespace r_util {

class RToken
{
public:
   enum Type
   {
      LPAREN         = '(',
      RPAREN         = ')',
      LBRACKET       = '[',
      RBRACKET       = ']',
      LBRACE         = '{',
      RBRACE         = '}',
      COMMA          = ',',
      SEMI           = ';',
      WHITESPACE     = 0x1001,
      STRING         = 0x1002,
      NUMBER         = 0x1003,
      ID             = 0x1004,
      OPER           = 0x1005,
      UOPER          = 0x1006,
      ERROR          = 0x1007,
      LDBRACKET      = 0x1008, // [[
      RDBRACKET      = 0x1009  // ]]
   };

public:
   RToken();
   RToken(Type type,
          const std::string& content,
          std::size_t offset,
          std::size_t length);
   virtual ~RToken();

   // COPYING: via copyable shared_ptr<Impl>

   // accessors
   Type type() const;
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

bool operator!=(const RToken& lhs, const RToken& rhs)
{
    return !(lhs == rhs);
}

class RStringToken : public RToken
{
public:
   RStringToken(Type type,
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
   RTokenRange(const std::string& code);
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


} // namespace r_util
} // namespace core 


#endif // CORE_R_UTIL_R_ENVIRONMENT_HPP

