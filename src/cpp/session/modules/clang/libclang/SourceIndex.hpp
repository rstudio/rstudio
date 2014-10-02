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

#ifndef SESSION_MODULES_CLANG_LIBCLANG_SOURCE_INDEX_HPP
#define SESSION_MODULES_CLANG_LIBCLANG_SOURCE_INDEX_HPP

#include <map>

#include <boost/function.hpp>
#include <boost/noncopyable.hpp>

#include "LibClang.hpp"
#include "TranslationUnit.hpp"

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

struct CompilationDatabase
{
   bool empty() const { return ! compileArgsForTranslationUnit; }
   boost::function<std::vector<std::string>(const std::string&)>
                                            compileArgsForTranslationUnit;
   boost::function<std::vector<std::string>()> translationUnits;
};

class SourceIndex : boost::noncopyable
{   
private:
   // singleton
   friend SourceIndex& sourceIndex();
   SourceIndex();

public:
   static bool isTranslationUnit(const std::string& filename);

public:
   virtual ~SourceIndex();

   void initialize(CompilationDatabase compilationDatabase, int verbose);

   unsigned getGlobalOptions() const;
   void setGlobalOptions(unsigned options);

   // functions used to keep the index "hot" based on recent user edits
   void primeTranslationUnit(const std::string& filename);
   void reprimeTranslationUnit(const std::string& filename);

   // remove translation units so they don't occupy memory
   void removeTranslationUnit(const std::string& filename);
   void removeAllTranslationUnits();

   // get the translation unit for the passed file (can be a c/cpp file
   // or a header file)
   TranslationUnit getTranslationUnit(const std::string& filename);

private:

   // get a translation unit for a header file
   TranslationUnit getHeaderTranslationUnit(const std::string& filename);

private:
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

   CompilationDatabase compilationDatabase_;

   int verbose_;
};

// singleton
SourceIndex& sourceIndex();

} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_SOURCE_INDEX_HPP
