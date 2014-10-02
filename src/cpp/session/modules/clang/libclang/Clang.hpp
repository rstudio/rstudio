/*
 * Clang.hpp
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

#ifndef SESSION_MODULES_CLANG_LIBCLANG_CLANG_HPP
#define SESSION_MODULES_CLANG_LIBCLANG_CLANG_HPP

#include <string>

#include "CompilationDatabase.hpp"
#include "Diagnostic.hpp"
#include "EmbeddedLibrary.hpp"
#include "LibraryVersion.hpp"
#include "SharedLibrary.hpp"
#include "SourceIndex.hpp"
#include "SourceLocation.hpp"
#include "TranslationUnit.hpp"
#include "UnsavedFiles.hpp"

namespace session {
namespace modules {      
namespace clang {
namespace libclang {


// convenience function to load libclang and initialize the source index
bool initialize(CompilationDatabase compilationDB = CompilationDatabase(),
                EmbeddedLibrary embedded = EmbeddedLibrary(),
                LibraryVersion requiredVersion = LibraryVersion(3,4,0),
                int verbose = 0,
                std::string* pDiagnostics = NULL);



} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_CLANG_HPP
