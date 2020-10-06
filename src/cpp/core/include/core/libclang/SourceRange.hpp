/*
 * SourceRange.hpp
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

#ifndef CORE_LIBCLANG_SOURCE_RANGE_HPP
#define CORE_LIBCLANG_SOURCE_RANGE_HPP

#include <string>
#include <iosfwd>

#include "clang-c/Index.h"

#include <core/libclang/SourceLocation.hpp>

namespace rstudio {
namespace core {
namespace libclang {

// file range
struct FileRange
{
   FileLocation start;
   FileLocation end;

   bool empty() const { return start.empty(); }

   bool operator==(const FileRange& other) const
   {
      return start == other.start &&
             end == other.end;
   }

   bool operator!=(const FileRange& other) const
   {
      return !(*this == other);
   }
};

std::ostream& operator << (std::ostream& stream, const FileRange& loc);

class SourceRange
{
public:
   SourceRange();

   explicit SourceRange(CXSourceRange range)
      : range_(range)
   {
   }

   // source range objects are managed internal to clang so instances
   // of this type can be freely copied

   CXSourceRange getCXSourceRange() const { return range_; }

   bool isNull() const;

   SourceLocation getStart() const;

   SourceLocation getEnd() const;

   FileRange getFileRange() const;

   bool operator==(const SourceRange& other) const;
   bool operator!=(const SourceRange& other) const;

private:
   CXSourceRange range_;
};


} // namespace libclang
} // namespace core
} // namespace rstudio

#endif // CORE_LIBCLANG_SOURCE_RANGE_HPP
