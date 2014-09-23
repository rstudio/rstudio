/*
 * SourceLocation.hpp
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

#ifndef SESSION_MODULES_CLANG_LIBCLANG_SOURCE_LOCATION_HPP
#define SESSION_MODULES_CLANG_LIBCLANG_SOURCE_LOCATION_HPP


#include "LibClang.hpp"

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

class SourceLocation
{
public:
   SourceLocation();

   explicit SourceLocation(CXSourceLocation location)
      : location_(location)
   {
   }

   // source location objects are managed internal to clang so instances
   // of this type can be freely copied

   bool empty() const;

   void getSpellingLocation(unsigned* pLine,
                            unsigned* pColumn,
                            unsigned* pOffset = NULL) const;

   bool operator==(const SourceLocation& other) const ;
   bool operator!=(const SourceLocation& other) const ;

private:
   CXSourceLocation location_;
};


} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_SOURCE_LOCATION_HPP
