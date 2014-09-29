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
         clang().disposeTranslationUnit(it->second.tu);
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

void SourceIndex::removeTranslationUnit(const std::string& filename)
{
   TranslationUnits::iterator it = translationUnits_.find(filename);
   if (it != translationUnits_.end())
   {
      clang().disposeTranslationUnit(it->second.tu);
      translationUnits_.erase(it->first);
   }
}

void SourceIndex::primeTranslationUnit(const std::string& filename)
{
   // if we have no record of this translation unit then do a first pass
   if (translationUnits_.find(filename) == translationUnits_.end())
      getTranslationUnit(filename);
}

TranslationUnit SourceIndex::getTranslationUnit(
                                          const std::string& filename)
{
   core::PerformanceTimer timer("libclang: " + FilePath(filename).filename());

   // TODO: for header files we'll need to scan the translation
   // units for them and use the appropriate one
   // (perhaps using clang_getFile)

   // get the arguments and last write time for this file
   std::vector<std::string> args = compilationDatabase().argsForSourceFile(filename);
   std::time_t lastWriteTime = FilePath(filename).lastWriteTime();

   // look it up
   TranslationUnits::iterator it = translationUnits_.find(filename);

   // check for various incremental processing scenarios
   if (it != translationUnits_.end())
   {
      // alias record
      StoredTranslationUnit& stored = it->second;

      // already up to date?
      if (args == stored.compileArgs && lastWriteTime == stored.lastWriteTime)
      {
         return TranslationUnit(filename, stored.tu);
      }

      // just needs reparse?
      else if (args == stored.compileArgs)
      {
         int ret = clang().reparseTranslationUnit(
                                stored.tu,
                                unsavedFiles().numUnsavedFiles(),
                                unsavedFiles().unsavedFilesArray(),
                                clang().defaultReparseOptions(stored.tu));

         if (ret == 0)
         {
            // update last write time
            stored.lastWriteTime = lastWriteTime;

            // return it
            return TranslationUnit(filename, stored.tu);
         }
         else
         {
            LOG_ERROR_MESSAGE("Error re-parsing translation unit " + filename);
         }
      }
   }

   // if we got this far then there either was no existing translation
   // unit or we require a full rebuild. in all cases remove any existing
   // translation unit we have
   removeTranslationUnit(filename);

   // get the args in the fashion libclang expects (char**)
   core::system::ProcessArgs argsArray(args);

   // create a new translation unit from the file
   CXTranslationUnit tu = clang().parseTranslationUnit(
                         index_,
                         filename.c_str(),
                         argsArray.args(),
                         argsArray.argCount(),
                         unsavedFiles().unsavedFilesArray(),
                         unsavedFiles().numUnsavedFiles(),
                         clang().defaultEditingTranslationUnitOptions());


   // save and return it if we succeeded
   if (tu != NULL)
   {
      translationUnits_[filename] = StoredTranslationUnit(args,
                                                          lastWriteTime,
                                                          tu);

      return TranslationUnit(filename, tu);
   }
   else
   {
      LOG_ERROR_MESSAGE("Error parsing translation unit " + filename);
      return TranslationUnit();
   }
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

