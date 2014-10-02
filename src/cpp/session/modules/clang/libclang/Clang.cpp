/*
 * Clang.cpp
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

#include "Clang.hpp"

#include "SharedLibrary.hpp"
#include "SourceIndex.hpp"

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

// convenience function to load libclang and initialize the source index
bool initialize(CompilationDatabase compilationDB,
                EmbeddedLibrary embedded,
                LibraryVersion requiredVersion,
                int verbose,
                std::string* pDiagnostics)
{
   bool loaded = clang().load(embedded, requiredVersion, pDiagnostics);
   if (!loaded)
      return false;

   sourceIndex().initialize(compilationDB, verbose);

   return true;
}


} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

