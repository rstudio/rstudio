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

#include <boost/noncopyable.hpp>

#include "LibClang.hpp"
#include "TranslationUnit.hpp"

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

class SourceIndex : boost::noncopyable
{
private:
   // singleton
   friend SourceIndex& sourceIndex();
   SourceIndex();

public:
   virtual ~SourceIndex();

   unsigned getGlobalOptions() const;
   void setGlobalOptions(unsigned options);

   void primeTranslationUnit(const std::string& filename);
   TranslationUnit getTranslationUnit(const std::string& filename);

private:
   void removeTranslationUnit(const std::string& filename);

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
};

// singleton
SourceIndex& sourceIndex();

} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_SOURCE_INDEX_HPP
