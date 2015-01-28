/*
 * SourceRange.hpp
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

#ifndef CORE_LIBCLANG_SOURCE_RANGE_HPP
#define CORE_LIBCLANG_SOURCE_RANGE_HPP

#include <string>
#include <iosfwd>

#include "clang-c/Index.h"

namespace rstudio {
namespace core {
namespace libclang {

class SourceLocation;

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

   bool isNull() const;

   SourceLocation getStart() const;

   SourceLocation getEnd() const;

   bool operator==(const SourceRange& other) const ;
   bool operator!=(const SourceRange& other) const ;

private:
   CXSourceRange range_;
};


} // namespace libclang
} // namespace core
} // namespace rstudio

#endif // CORE_LIBCLANG_SOURCE_RANGE_HPP
