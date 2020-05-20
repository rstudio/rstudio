/*
 * Cursor.cpp
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

#include <core/libclang/Cursor.hpp>


#include <core/libclang/LibClang.hpp>

namespace rstudio {
namespace core {
namespace libclang {

Cursor::Cursor()
   : cursor_(clang().getNullCursor())
{
}

std::string Cursor::spelling() const
{
   return toStdString(clang().getCursorSpelling(cursor_));
}

std::string Cursor::displayName() const
{
   return toStdString(clang().getCursorDisplayName(cursor_));
}

CXCursorKind Cursor::getKind() const
{
   return clang().getCursorKind(cursor_);
}

bool Cursor::isDeclaration() const
{
   return clang().isDeclaration(getKind());
}

bool Cursor::isReference() const
{
   return clang().isReference(getKind());
}

bool Cursor::isExpression() const
{
   return clang().isExpression(getKind());
}

bool Cursor::isStatement() const
{
   return clang().isStatement(getKind());
}

bool Cursor::isAttribute() const
{
   return clang().isAttribute(getKind());
}

bool Cursor::isInvalid() const
{
   return clang().isInvalid(getKind());
}

bool Cursor::isTranslationUnit() const
{
   return clang().isTranslationUnit(getKind());
}

bool Cursor::isPreprocessing() const
{
   return clang().isPreprocessing(getKind());
}

bool Cursor::isUnexposed() const
{
   return clang().isUnexposed(getKind());
}

bool Cursor::isDefinition() const
{
   return clang().isCursorDefinition(cursor_) != 0;
}

Cursor Cursor::getReferenced() const
{
   return Cursor(clang().getCursorReferenced(cursor_));
}

Cursor Cursor::getDefinition() const
{
   return Cursor(clang().getCursorDefinition(cursor_));
}

Cursor Cursor::getCannonical() const
{
   return Cursor(clang().getCanonicalCursor(cursor_));
}

Cursor Cursor::getLexicalParent() const
{
   return Cursor(clang().getCursorLexicalParent(cursor_));
}

Cursor Cursor::getSemanticParent() const
{
   return Cursor(clang().getCursorSemanticParent(cursor_));
}

CXLinkageKind Cursor::getLinkageKind() const
{
   return clang().getCursorLinkage(cursor_);
}

bool Cursor::hasLinkage() const
{
   CXLinkageKind kind = getLinkageKind();
   return kind == CXLinkage_Internal ||
          kind == CXLinkage_External ||
          kind == CXLinkage_UniqueExternal;
}

bool Cursor::hasExternalLinkage() const
{
   CXLinkageKind kind = getLinkageKind();
   return kind == CXLinkage_External || kind == CXLinkage_UniqueExternal;
}

std::string Cursor::getUSR() const
{
   return toStdString(clang().getCursorUSR(cursor_));
}

SourceLocation Cursor::getSourceLocation() const
{
   return SourceLocation(clang().getCursorLocation(cursor_));
}


SourceRange Cursor::getExtent() const
{
   return SourceRange(clang().getCursorExtent(cursor_));
}

unsigned Cursor::hash() const
{
   return clang().hashCursor(cursor_);
}

bool Cursor::isNull() const
{
   return (clang().equalCursors(cursor_, clang().getNullCursor()));
}

bool Cursor::isValid() const
{
   return (!isNull() && !isInvalid());
}

bool Cursor::operator==(const Cursor& other) const
{
   return clang().equalCursors(cursor_, other.cursor_);
}


} // namespace libclang
} // namespace core
} // namespace rstudio

