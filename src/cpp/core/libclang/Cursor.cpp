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


bool Cursor::isDefinition() const
{
   return clang().isCursorDefinition(cursor()) != 0;
}

Cursor Cursor::getDefinition() const
{
   return Cursor(clang().getCursorDefinition(cursor()));
}

Cursor Cursor::getCannonical() const
{
   return Cursor(clang().getCanonicalCursor(cursor()));
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

} // namespace libclang
} // namespace core

