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

#include <core/PerformanceTimer.hpp>

#include <core/system/ProcessArgs.hpp>

#include "UnsavedFiles.hpp"
#include "CompilationDatabase.hpp"

/*
Args/includes come from:

  - Builtin clang headers shipped with the product
  - Platform-specific additions:
      * -stdlib=libstdc++ on OSX
      * Rtools includes on Windows
  - Arguments emmitted from R CMD SHLIB

How to track R CMD SHLIB arguments:

  - For a package, run make --dry-run at startup and then again
    every time a source file is added or the DESCRIPTION or
    Makevars[.win] is changed

  - For a C++ file that is NOT in a package, check for use of Rcpp
    and do make --dry-run with sourceCpp in that case (otherwise
    do make --dry-run with R CMD SHLIB

Separate CompilationDatabase class that tracks all of this and can
cough up the include args for any given file

Note that this implies that code completion in header files will be
done by searching the TranlationUnits for a use of that header

*/


using namespace core ;

namespace session {
namespace modules { 
namespace clang {
namespace libclang {

SourceIndex::SourceIndex()
{
   index_ = clang().createIndex(0, 1);
}

SourceIndex::~SourceIndex()
{
   try
   {
      // dispose all translation units
      for(TranslationUnits::const_iterator it = translationUnits_.begin();
          it != translationUnits_.end(); ++it)
      {
         clang().disposeTranslationUnit(it->second);
      }

      // dispose the index
      clang().disposeIndex(index_);
   }
   catch(...)
   {
   }
}

unsigned SourceIndex::getGlobalOptions() const
{
   return clang().CXIndex_getGlobalOptions(index_);
}

void SourceIndex::setGlobalOptions(unsigned options)
{
   clang().CXIndex_setGlobalOptions(index_, options);
}

void SourceIndex::updateTranslationUnit(const std::string& file)
{
   PerformanceTimer timer("libclang: " + file);

   // check for an existing translation unit, if we don't have one then
   // parse the source file into a translation unit
   TranslationUnits::iterator it = translationUnits_.find(file);
   if (it == translationUnits_.end())
   {
      // get the args from the compilation database
      std::vector<std::string> args = compilationDatabase().argsForFile(file);
      if (!args.empty())
      {
         // get the args in the fashion libclang expects (char**)
         core::system::ProcessArgs argsArray(args);

         // create a new translation unit from the file
         CXTranslationUnit tu = clang().parseTranslationUnit(
                               index_,
                               file.c_str(),
                               argsArray.args(),
                               argsArray.argCount(),
                               unsavedFiles().unsavedFilesArray(),
                               unsavedFiles().numUnsavedFiles(),
                               clang().defaultEditingTranslationUnitOptions());


         // save it if we succeeded
         if (tu != NULL)
         {
            translationUnits_[file] = tu;
         }
         else
         {
            LOG_ERROR_MESSAGE("Error parsing translation unit " + file);
         }
      }
   }

   // lookup and reparse the translation unit (multiple sources indicate that
   // you need to immediately reparse the translation unit after creation
   // in order to get the benefit of precompiled headers). note that this
   // lookup will fail in the case of error occurring during parsing above
   it = translationUnits_.find(file);
   if (it != translationUnits_.end())
   {
      int ret = clang().reparseTranslationUnit(
                                  it->second,
                                  unsavedFiles().numUnsavedFiles(),
                                  unsavedFiles().unsavedFilesArray(),
                                  clang().defaultReparseOptions(it->second));

      // if this returns an error then we need to dispose the translation unit
      if (ret != 0)
      {
         LOG_ERROR_MESSAGE("Error re-parsing translation unit " + file);
         removeTranslationUnit(file);
      }
   }
}

void SourceIndex::removeTranslationUnit(const std::string& filename)
{
   TranslationUnits::iterator it = translationUnits_.find(filename);
   if (it != translationUnits_.end())
   {
      clang().disposeTranslationUnit(it->second);
      translationUnits_.erase(it->first);
   }
}

bool SourceIndex::hasTranslationUnit(const std::string& filename)
{
   return translationUnits_.find(filename) != translationUnits_.end();
}

TranslationUnit SourceIndex::getTranslationUnit(
                                          const std::string& filename) const
{
   // TODO: for header files we'll need to scan the translation
   // units for them and use the appropriate one

   TranslationUnits::const_iterator it = translationUnits_.find(filename);
   if (it != translationUnits_.end())
      return TranslationUnit(it->first, it->second);
   else
      return TranslationUnit();
}



// singleton
SourceIndex& sourceIndex()
{
   static SourceIndex instance;
   return instance;
}

} // namespace libclang
} // namespace clang
} // namespace modules
} // namesapce session

