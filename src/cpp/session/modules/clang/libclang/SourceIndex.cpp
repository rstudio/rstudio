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

#include "UnsavedFiles.hpp"

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

void SourceIndex::updateTranslationUnit(const std::string& filename)
{
   PerformanceTimer timer("libclang: " + filename);

   // check for an existing translation unit, if we don't have one then
   // parse the source file into a translation unit
   TranslationUnits::iterator it = translationUnits_.find(filename);
   if (it == translationUnits_.end())
   {
      // for now we use the default R command line args for an Rcpp
      // package on OSX. We ultimately need to do this dynamically.
      // The best way to accomplish this would seem to be the creation
      // of a compliation database json file and then the reading of it

      std::vector<const char*> args;
      args.push_back("-stdlib=libstdc++");
      std::string builtinHeaders = "-I" + clang().builtinHeaders();
      args.push_back(builtinHeaders.c_str());
      args.push_back("-I/Library/Frameworks/R.framework/Resources/include");
      args.push_back("-DNDEBUG");
      args.push_back("-I/usr/local/include");
      args.push_back("-I/usr/local/include/freetype2");
      args.push_back("-I/opt/X11/include");
      args.push_back("-I/Library/Frameworks/R.framework/Resources/library/Rcpp/include");

      // create a new translation unit from the file
      CXTranslationUnit tu = clang().parseTranslationUnit(
                            index_,
                            filename.c_str(),
                            &(args[0]),
                            args.size(),
                            unsavedFiles().unsavedFilesArray(),
                            unsavedFiles().numUnsavedFiles(),
                            clang().defaultEditingTranslationUnitOptions());


      // save it if we succeeded
      if (tu != NULL)
      {
         translationUnits_[filename] = tu;
      }
      else
      {
         LOG_ERROR_MESSAGE("Error parsing translation unit " + filename);
      }
   }

   // lookup and reparse the translation unit (multiple sources indicate that
   // you need to immediately reparse the translation unit after creation
   // in order to get the benefit of precompiled headers). note that this
   // lookup will fail in the case of error occurring during parsing above
   it = translationUnits_.find(filename);
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
         LOG_ERROR_MESSAGE("Error re-parsing translation unit " + filename);
         removeTranslationUnit(filename);
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

