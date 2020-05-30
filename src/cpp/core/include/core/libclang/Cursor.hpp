/*
 * Cursor.hpp
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

#ifndef CORE_LIBCLANG_CURSOR_HPP
#define CORE_LIBCLANG_CURSOR_HPP

#include "clang-c/Index.h"

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace libclang {

class SourceRange;
class SourceLocation;

class Cursor
{
public:

   Cursor();

   explicit Cursor(CXCursor cursor)
      : cursor_(cursor)
   {
   }

public:

   std::string spelling() const;
   std::string displayName() const;

   CXCursorKind getKind() const;

   bool isDeclaration() const;
   bool isReference() const;
   bool isExpression() const;
   bool isStatement() const;
   bool isAttribute() const;
   bool isInvalid() const;
   bool isTranslationUnit() const;
   bool isPreprocessing() const;
   bool isUnexposed() const;

   bool isDefinition() const;

   Cursor getReferenced() const;

   Cursor getDefinition() const;

   Cursor getCannonical() const;

   Cursor getLexicalParent() const;
   Cursor getSemanticParent() const;

   CXLinkageKind getLinkageKind() const;
   bool hasLinkage() const;
   bool hasExternalLinkage() const;

   std::string getUSR() const;

   CXCursor getCXCursor() const { return cursor_; }

   unsigned hash() const;

   SourceLocation getSourceLocation() const;

   SourceRange getExtent() const;

   bool isNull() const;
   bool isValid() const;

   bool operator==(const Cursor& other) const;
   bool operator!=(const Cursor& other) const
   {
      return !(other == *this);
   }

private:
   CXCursor cursor_;
};

} // namespace libclang
} // namespace core
} // namespace rstudio

#endif // CORE_LIBCLANG_CURSOR_HPP
