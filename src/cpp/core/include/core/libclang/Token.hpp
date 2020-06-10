
/*
 * Token.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef CORE_LIBCLANG_TOKEN_HPP
#define CORE_LIBCLANG_TOKEN_HPP

#include <string>

#include <boost/noncopyable.hpp>

#include <core/libclang/TranslationUnit.hpp>

#include "clang-c/Index.h"


namespace rstudio {
namespace core {
namespace libclang {

class SourceRange;
class SourceLocation;

class Token
{
public:
   Token(CXTranslationUnit tu, CXToken token)
      : tu_(tu), token_(token)
   {
   }

   CXTokenKind kind() const;

   std::string spelling() const;

   SourceLocation location() const;

   SourceRange extent() const;

private:
   CXTranslationUnit tu_;
   CXToken token_;
};

class Tokens : boost::noncopyable
{
public:
   Tokens(CXTranslationUnit tu, const SourceRange& sourceRange);
   virtual ~Tokens();

   unsigned numTokens() const { return numTokens_; }
   Token getToken(unsigned index) const { return Token(tu_, pTokens_[index]); }

private:
   CXTranslationUnit tu_;
   CXToken* pTokens_;
   unsigned numTokens_;
};

} // namespace libclang
} // namespace core
} // namespace rstudio

#endif // CORE_LIBCLANG_TOKEN_HPP
