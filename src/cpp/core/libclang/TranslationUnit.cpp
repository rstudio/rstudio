/*
 * TranslationUnit.cpp
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

#include <core/libclang/TranslationUnit.hpp>

#include <gsl/gsl>

#include <shared_core/FilePath.hpp>

#include <core/libclang/Utils.hpp>
#include <core/libclang/LibClang.hpp>
#include <core/libclang/UnsavedFiles.hpp>

namespace rstudio {
namespace core {
namespace libclang {

namespace  {

std::string formatBytes(double value)
{
   int mb = gsl::narrow_cast<int>(value / 1024 / 1024);
   if (mb > 1024)
   {
      double gb = (double)mb / 1024.0;
      boost::format fmt("%1.1f gb");
      return boost::str(fmt % gb);
   }
   else if (mb > 1)
   {
      boost::format fmt("%1% mb");
      return boost::str(fmt % mb);
   }
   else
   {
      boost::format fmt("%1% kb");
      return boost::str(fmt % gsl::narrow_cast<int>(value / 1024));
   }
}

} // anonymous namespace

std::string TranslationUnit::getSpelling() const
{
   return toStdString(clang().getTranslationUnitSpelling(tu_));
}

bool TranslationUnit::includesFile(const std::string& filename) const
{
   return clang().getFile(tu_, filename.c_str()) != nullptr;
}

CXFile TranslationUnit::getFile(const std::string& filename) const
{
   std::string targetFile = filename;
   if (targetFile.empty())
      targetFile = filename_;

   return clang().getFile(tu_, targetFile.c_str());
}

CXResult TranslationUnit::findReferencesInFile(
                              Cursor cursor,
                              CXCursorAndRangeVisitor visitor,
                              const std::string& filename) const
{
   CXFile file = getFile(filename);
   if (file == nullptr)
      return CXResult_Invalid;

   return clang().findReferencesInFile(cursor.getCXCursor(), file, visitor);
}

unsigned TranslationUnit::getNumDiagnostics() const
{
   return clang().getNumDiagnostics(tu_);
}

boost::shared_ptr<Diagnostic>
               TranslationUnit::getDiagnostic(unsigned index) const
{
   return boost::shared_ptr<Diagnostic>(
                     new Diagnostic(clang().getDiagnostic(tu_, index)));
}

Cursor TranslationUnit::getCursor() const
{
   return Cursor(clang().getTranslationUnitCursor(tu_));
}

Cursor TranslationUnit::getCursor(const std::string& filename,
                                  unsigned line,
                                  unsigned column) const
{
   // get the file
   CXFile file = clang().getFile(tu_, filename.c_str());
   if (file == nullptr)
      return Cursor();

   // get the source location
   CXSourceLocation sourceLoc = clang().getLocation(tu_, file, line, column);

   // get the cursor
   CXCursor cursor = clang().getCursor(tu_, sourceLoc);
   if (clang().equalCursors(cursor, clang().getNullCursor()))
      return Cursor();

   // return it
   return Cursor(cursor);
}

boost::shared_ptr<CodeCompleteResults> TranslationUnit::codeCompleteAt(
                                            const std::string& filename,
                                            unsigned line,
                                            unsigned column) const
{
   CXCodeCompleteResults* pResults = clang().codeCompleteAt(
                                 tu_,
                                 filename.c_str(),
                                 line,
                                 column,
                                 pUnsavedFiles_->unsavedFilesArray(),
                                 pUnsavedFiles_->numUnsavedFiles(),
                                 clang().defaultCodeCompleteOptions());

   if (pResults != nullptr)
   {
      clang().sortCodeCompletionResults(pResults->Results,
                                        pResults->NumResults);

      return boost::shared_ptr<CodeCompleteResults>(
                                    new CodeCompleteResults(pResults));
   }
   else
   {
      return boost::shared_ptr<CodeCompleteResults>(new CodeCompleteResults());
   }
}

void TranslationUnit::printResourceUsage(std::ostream& ostr, bool detailed) const
{
   CXTUResourceUsage usage = clang().getCXTUResourceUsage(tu_);

   unsigned long totalBytes = 0;
   for (unsigned i = 0; i < usage.numEntries; i++)
   {
      CXTUResourceUsageEntry entry = usage.entries[i];

      if (detailed)
      {
         ostr << clang().getTUResourceUsageName(entry.kind) << ": "
              << formatBytes(entry.amount) << std::endl;
      }

      if (entry.kind >= CXTUResourceUsage_MEMORY_IN_BYTES_BEGIN &&
          entry.kind <= CXTUResourceUsage_MEMORY_IN_BYTES_END)
      {
         totalBytes += entry.amount;
      }
   }
   ostr << "TOTAL MEMORY: " << formatBytes(totalBytes)
        << " (" << FilePath(getSpelling()).getFilename() << ")" << std::endl;

   clang().disposeCXTUResourceUsage(usage);
}


} // namespace libclang
} // namespace core
} // namespace rstudio


