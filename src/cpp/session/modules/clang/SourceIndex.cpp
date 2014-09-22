/*
 * SourceIndex.cpp
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

#include "SourceIndex.hpp"


#include "libclang/libclang.hpp"

// SourceIndex is maintained using file system and source editor callbacks


// Separate unsavedFiles structure maintained using sourceEditor callbacks


using namespace core ;

namespace session {
namespace modules { 
namespace clang {

SourceIndex::SourceIndex(int excludeDeclarationsFromPCH, int displayDiagnostics)
{
   index_ = clang().createIndex(excludeDeclarationsFromPCH,
                                displayDiagnostics);
}

SourceIndex::~SourceIndex()
{
   try
   {
      clang().disposeIndex(index_);
   }
   catch(...)
   {
   }
}

unsigned SourceIndex::getGlobalOptions()
{
   return clang().CXIndex_getGlobalOptions(index_);
}

void SourceIndex::setGlobalOptions(unsigned options)
{
   clang().CXIndex_setGlobalOptions(index_, options);
}

} // namespace clang
} // namespace modules
} // namesapce session

