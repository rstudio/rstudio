/*
 * SourceIndex.hpp
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

#ifndef CORE_LIBCLANG_SOURCE_INDEX_HPP
#define CORE_LIBCLANG_SOURCE_INDEX_HPP

#include <map>
#include <vector>
#include <ctime>

#include <boost/function.hpp>
#include <boost/noncopyable.hpp>

#include "clang-c/Index.h"

#include "TranslationUnit.hpp"

namespace rscore {

class FilePath;

namespace libclang {

struct CompilationDatabase
{
   boost::function<std::vector<std::string>(const std::string&)>
                                            compileArgsForTranslationUnit;
};

class SourceIndex : boost::noncopyable
{   
public:
   static bool isSourceFile(const std::string& filename);
   static bool isSourceFile(const rscore::FilePath& filePath);

public:
   explicit SourceIndex(
               CompilationDatabase compilationDB = CompilationDatabase(),
               int verbose = 0);

   virtual ~SourceIndex();

   UnsavedFiles& unsavedFiles() { return unsavedFiles_; }

   unsigned getGlobalOptions() const;
   void setGlobalOptions(unsigned options);

   // functions used to keep the index "hot" based on recent user edits
   void primeEditorTranslationUnit(const std::string& filename);
   void reprimeEditorTranslationUnit(const std::string& filename);

   // remove translation units so they don't occupy memory
   void removeTranslationUnit(const std::string& filename);
   void removeAllTranslationUnits();

   // get all indexed translation units
   std::map<std::string,CXTranslationUnit> getIndexedTranslationUnits() const;

   // get the translation unit for the passed file (can be a c/cpp file
   // or a header file)
   TranslationUnit getTranslationUnit(const std::string& filename,
                                      bool alwaysReparse = false);

private:

   UnsavedFiles unsavedFiles_;

   CXIndex index_;

   struct StoredTranslationUnit
   {
      StoredTranslationUnit() : lastWriteTime(0), tu(NULL) {}
      StoredTranslationUnit(const std::vector<std::string>& compileArgs,
                            std::time_t lastWriteTime,
                            CXTranslationUnit tu)
         : compileArgs(compileArgs), lastWriteTime(lastWriteTime), tu(tu)
      {
      }
      std::vector<std::string> compileArgs;
      std::time_t lastWriteTime;
      CXTranslationUnit tu;
   };
   typedef std::map<std::string,StoredTranslationUnit> TranslationUnits;
   TranslationUnits translationUnits_;

   CompilationDatabase compilationDB_;

   int verbose_;
};

} // namespace libclang
} // namespace rscore

#endif // CORE_LIBCLANG_SOURCE_INDEX_HPP
