/*
 * SourceLocation.cpp
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

#include <core/libclang/SourceLocation.hpp>

#include <core/libclang/LibClang.hpp>

namespace core {
namespace libclang {

SourceLocation::SourceLocation()
   : location_(clang().getNullLocation())
{
}

bool SourceLocation::empty() const
{
   return clang().equalLocations(location_, clang().getNullLocation());
}

void SourceLocation::getExpansionLocation(std::string* pFile,
                                          unsigned* pLine,
                                          unsigned* pColumn,
                                          unsigned* pOffset) const
{
   CXFile file;
   clang().getExpansionLocation(location_, &file, pLine, pColumn, pOffset);

   CXString filename = clang().getFileName(file);
   *pFile = toStdString(filename);
}


void SourceLocation::getSpellingLocation(std::string* pFile,
                                         unsigned* pLine,
                                         unsigned* pColumn,
                                         unsigned* pOffset) const
{
   CXFile file;
   clang().getSpellingLocation(location_, &file, pLine, pColumn, pOffset);

   CXString filename = clang().getFileName(file);
   *pFile = toStdString(filename);
}

bool SourceLocation::operator==(const SourceLocation& other) const
{
   return clang().equalLocations(location_, other.location_) != 0;
}

bool SourceLocation::operator!=(const SourceLocation& other) const
{
   return clang().equalLocations(location_, other.location_) == 0;
}

} // namespace libclang
} // namespace core
