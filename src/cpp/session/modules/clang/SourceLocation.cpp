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

#include "SourceLocation.hpp"

#include "Clang.hpp"

namespace session {
namespace modules { 
namespace clang {

SourceLocation::SourceLocation()
   : location_(clang().getNullLocation())
{
}

bool SourceLocation::empty() const
{
   return clang().equalLocations(location_, clang().getNullLocation());
}

void SourceLocation::getSpellingLocation(unsigned* pLine,
                                         unsigned* pColumn,
                                         unsigned* pOffset) const
{
   clang().getSpellingLocation(location_, NULL, pLine, pColumn, pOffset);
}

bool SourceLocation::operator==(const SourceLocation& other) const
{
   return clang().equalLocations(location_, other.location_) != 0;
}

bool SourceLocation::operator!=(const SourceLocation& other) const
{
   return clang().equalLocations(location_, other.location_) == 0;
}

} // namespace clang
} // namespace modules
} // namesapce session

