/*
 * SourceLocation.cpp
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

#include <core/libclang/SourceLocation.hpp>

#include <core/libclang/LibClang.hpp>

namespace rstudio {
namespace core {
namespace libclang {

std::ostream& operator << (std::ostream& stream, const FileLocation& loc)
{
   stream << loc.filePath << " [" << loc.line << ", " << loc.column << "]";
   return stream;
}

SourceLocation::SourceLocation()
   : location_(clang().getNullLocation())
{
}

bool SourceLocation::empty() const
{
   return clang().equalLocations(location_, clang().getNullLocation());
}

bool SourceLocation::isFromMainFile() const
{
   return clang().Location_isFromMainFile(location_);
}

bool SourceLocation::isInSystemHeader() const
{
   return clang().Location_isInSystemHeader(location_);
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

void SourceLocation::printExpansionLocation(std::ostream& ostr)
{
   std::string file;
   unsigned line, offset;
   getExpansionLocation(&file, &line, &offset, nullptr);
   ostr << file << " [line: " << line << ", col: " << offset << "]";
}


bool SourceLocation::getSpellingLocation(std::string* pFile,
                                         unsigned* pLine,
                                         unsigned* pColumn,
                                         unsigned* pOffset) const
{
   if (!empty())
   {
      CXFile file;
      clang().getSpellingLocation(location_, &file, pLine, pColumn, pOffset);

      CXString filename = clang().getFileName(file);
      *pFile = toStdString(filename);
      return true;
   }
   else
   {
      return false;
   }
}

FileLocation SourceLocation::getSpellingLocation() const
{
   std::string file;
   unsigned line, column;
   if (getSpellingLocation(&file, &line, &column))
      return FileLocation(FilePath(file), line, column);
   else
      return FileLocation();
}

void SourceLocation::printSpellingLocation(std::ostream& ostr)
{
   std::string file;
   unsigned line, offset;
   if (getSpellingLocation(&file, &line, &offset, nullptr))
      ostr << file << " [line: " << line << ", col: " << offset << "]";
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
} // namespace rstudio
