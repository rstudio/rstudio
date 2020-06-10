/*
 * Token.cpp
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

#include <core/libclang/Token.hpp>

#include <core/libclang/LibClang.hpp>

namespace rstudio {
namespace core {
namespace libclang {

CXTokenKind Token::kind() const
{
   return libclang::clang().getTokenKind(token_);
}

std::string Token::spelling() const
{
   return toStdString(libclang::clang().getTokenSpelling(tu_, token_));
}

SourceLocation Token::location() const
{
   return SourceLocation(libclang::clang().getTokenLocation(tu_, token_));
}

SourceRange Token::extent() const
{
   return SourceRange(libclang::clang().getTokenExtent(tu_, token_));
}

Tokens::Tokens(CXTranslationUnit tu, const SourceRange &sourceRange)
   : tu_(tu), pTokens_(nullptr), numTokens_(0)
{
   libclang::clang().tokenize(tu_,
                              sourceRange.getCXSourceRange(),
                              &pTokens_,
                              &numTokens_);
}

Tokens::~Tokens()
{
   try
   {
      if (pTokens_ != nullptr)
      {
        libclang::clang().disposeTokens(tu_,
                                        pTokens_,
                                        numTokens_);
      }
   }
   catch(...)
   {
   }
}

} // namespace libclang
} // namespace core
} // namespace rstudio

