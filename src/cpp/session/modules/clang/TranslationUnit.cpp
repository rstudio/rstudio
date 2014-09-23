/*
 * TranslationUnit.cpp
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

#include "TranslationUnit.hpp"

#include "Clang.hpp"
#include "UnsavedFiles.hpp"
#include "Utils.hpp"

namespace session {
namespace modules { 
namespace clang {

std::string TranslationUnit::getSpelling() const
{
   return toStdString(clang().getTranslationUnitSpelling(tu_));
}

unsigned TranslationUnit::getNumDiagnostics() const
{
   return clang().getNumDiagnostics(tu_);
}

Diagnostic TranslationUnit::getDiagnostic(unsigned index) const
{
   return Diagnostic(clang().getDiagnostic(tu_, index));
}

CodeCompleteResults TranslationUnit::codeCompleteAt(unsigned line,
                                                    unsigned column)
{
   CXCodeCompleteResults* pResults = clang().codeCompleteAt(
                                 tu_,
                                 filename_.c_str(),
                                 line,
                                 column,
                                 unsavedFiles().unsavedFilesArray(),
                                 unsavedFiles().numUnsavedFiles(),
                                 clang().defaultCodeCompleteOptions());

   return CodeCompleteResults(pResults);
}

} // namespace clang
} // namespace modules
} // namesapce session

