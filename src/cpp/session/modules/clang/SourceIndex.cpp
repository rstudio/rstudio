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

#include "Clang.hpp"
#include "UnsavedFiles.hpp"

using namespace core ;

namespace session {
namespace modules { 
namespace clang {

SourceIndex::SourceIndex()
{
   index_ = clang().createIndex(0, 0);
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

void SourceIndex::updateTranslationUnit(const std::string& filename)
{
   /*
   // check for an existing translation unit, if we don't have one then
   // parse the source file into a translation unit
   TranslationUnits::iterator it = translationUnits_.find(filename);
   if (it == translationUnits_.end())
   {

      // TODO: get the command line for file compliations and use it to
      // create the translation unit

      // get the command line for this file's compilation
      int numClangCommandLineArgs  = 0;
      const char * const *clangCommandLineArgs = NULL;

      // create a new translation unit from the file
      CXTranslationUnit tu = clang().parseTranslationUnit(
                            index_,
                            filename.c_str(),
                            clangCommandLineArgs,
                            numClangCommandLineArgs,
                            unsavedFiles().unsavedFilesArray(),
                            unsavedFiles().numUnsavedFiles(),
                            clang().defaultEditingTranslationUnitOptions());

      // save it for future reference
      translationUnits_[filename] = tu;

   }

   // reparse the translation unit
   // (according to this thread you need to call both parseTranslationUnit and
   // reparseTranslationUnit when adding a new file for caching/performance:
   // http://lists.cs.uiuc.edu/pipermail/cfe-dev/2013-April/028804.html).
   clang().reparseTranslationUnit(it->second,
                                  unsavedFiles().numUnsavedFiles(),
                                  unsavedFiles().unsavedFilesArray(),
                                  clang().defaultReparseOptions(it->second));
   */
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

CXTranslationUnit SourceIndex::getTranslationUnit(
                                          const std::string& filename) const
{
   TranslationUnits::const_iterator it = translationUnits_.find(filename);
   if (it != translationUnits_.end())
      return it->second;
   else
      return NULL;
}

// singleton
SourceIndex& sourceIndex()
{
   static SourceIndex instance;
   return instance;
}


} // namespace clang
} // namespace modules
} // namesapce session

