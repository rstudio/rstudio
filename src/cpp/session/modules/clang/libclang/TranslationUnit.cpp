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

#include <core/FilePath.hpp>

#include "Utils.hpp"
#include "SharedLibrary.hpp"
#include "UnsavedFiles.hpp"

namespace session {
namespace modules { 
namespace clang {
namespace libclang {

std::string TranslationUnit::getSpelling() const
{
   return toStdString(clang().getTranslationUnitSpelling(tu_));
}

bool TranslationUnit::includesFile(const std::string& filename) const
{
   return clang().getFile(tu_, filename.c_str()) != NULL;
}

unsigned TranslationUnit::getNumDiagnostics() const
{
   return clang().getNumDiagnostics(tu_);
}

Diagnostic TranslationUnit::getDiagnostic(unsigned index) const
{
   return Diagnostic(clang().getDiagnostic(tu_, index));
}

CodeCompleteResults TranslationUnit::codeCompleteAt(const std::string& filename,
                                                    unsigned line,
                                                    unsigned column)
{
   CXCodeCompleteResults* pResults = clang().codeCompleteAt(
                                 tu_,
                                 filename.c_str(),
                                 line,
                                 column,
                                 unsavedFiles().unsavedFilesArray(),
                                 unsavedFiles().numUnsavedFiles(),
                                 clang().defaultCodeCompleteOptions());

   if (pResults != NULL)
   {
      return CodeCompleteResults(pResults);
   }
   else
   {
      return CodeCompleteResults();
   }
}

void TranslationUnit::printResourceUsage(std::ostream& ostr)
{
   CXTUResourceUsage usage = clang().getCXTUResourceUsage(tu_);

   unsigned long totalBytes = 0;
   for (unsigned i = 0; i<usage.numEntries; i++)
   {
      CXTUResourceUsageEntry entry = usage.entries[i];
      ostr << clang().getTUResourceUsageName(entry.kind) << ": "
           << entry.amount << std::endl;
      totalBytes += entry.amount;
   }

   clang().disposeCXTUResourceUsage(usage);
}


} // namespace libclang
} // namespace clang
} // namespace modules
} // namesapce session

