/*
 * Cursor.cpp
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

#include <core/libclang/Cursor.hpp>


#include <core/libclang/LibClang.hpp>

namespace core {
namespace libclang {

Cursor::~Cursor()
{
   try
   {

   }
   catch(...)
   {
   }
}

std::string Cursor::displayName() const
{
   return toStdString(clang().getCursorDisplayName(cursor()));
}

CXCursorKind Cursor::getKind() const
{
   return clang().getCursorKind(cursor());
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
   return clang().isCursorDefinition(cursor()) != 0;
}

Cursor Cursor::getReferenced() const
{
   return Cursor(clang().getCursorReferenced(cursor()));
}

Cursor Cursor::getDefinition() const
{
   return Cursor(clang().getCursorDefinition(cursor()));
}

Cursor Cursor::getCannonical() const
{
   return Cursor(clang().getCanonicalCursor(cursor()));
}

Cursor Cursor::getSemanticParent() const
{
   return Cursor(clang().getCursorSemanticParent(cursor()));
}

CXLinkageKind Cursor::getLinkageKind() const
{
   return clang().getCursorLinkage(cursor());
}

bool Cursor::hasExternalLinkage() const
{
   CXLinkageKind kind = getLinkageKind();
   return kind == CXLinkage_External || kind == CXLinkage_UniqueExternal;
}

std::string Cursor::getUSR() const
{
   return toStdString(clang().getCursorUSR(cursor()));
}

SourceLocation Cursor::getSourceLocation() const
{
   return SourceLocation(clang().getCursorLocation(cursor()));
}

bool Cursor::isNull() const
{
   if (! pCursor_)
      return true;
   else
      return (clang().equalCursors(cursor(), clang().getNullCursor()));
}

bool Cursor::operator==(const Cursor& other) const
{
   return clang().equalCursors(cursor(), other.cursor());
}


} // namespace libclang
} // namespace core

