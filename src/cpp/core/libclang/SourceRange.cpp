/*
 * SourceRange.cpp
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

#include <core/libclang/SourceRange.hpp>

#include <core/libclang/LibClang.hpp>

namespace rstudio {
namespace core {
namespace libclang {

std::ostream& operator << (std::ostream& stream, const FileRange& loc)
{
   stream << "start: " << loc.start << std::endl;
   stream << "end  : " << loc.end << std::endl;
   return stream;
}

SourceRange::SourceRange()
   : range_(clang().getNullRange())
{
}

bool SourceRange::isNull() const
{
   return clang().equalRanges(range_, clang().getNullRange());
}

SourceLocation SourceRange::getStart() const
{
   return SourceLocation(clang().getRangeStart(range_));
}

SourceLocation SourceRange::getEnd() const
{
   return SourceLocation(clang().getRangeEnd(range_));
}

FileRange SourceRange::getFileRange() const
{
   FileRange range;
   range.start = getStart().getSpellingLocation();
   range.end = getEnd().getSpellingLocation();
   return range;
}

bool SourceRange::operator==(const SourceRange& other) const
{
   return clang().equalRanges(range_, other.range_) != 0;
}

bool SourceRange::operator!=(const SourceRange& other) const
{
   return clang().equalRanges(range_, other.range_) == 0;
}

} // namespace libclang
} // namespace core
} // namespace rstudio
