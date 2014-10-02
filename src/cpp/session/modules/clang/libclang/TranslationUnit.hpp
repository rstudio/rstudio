/*
 * TranslationUnit.hpp
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

#ifndef SESSION_MODULES_CLANG_LIBCLANG_TRANSLATION_UNIT_HPP
#define SESSION_MODULES_CLANG_LIBCLANG_TRANSLATION_UNIT_HPP

#include <iosfwd>
#include <string>

#include "clang-c/Index.h"

#include "Diagnostic.hpp"
#include "CodeCompleteResults.hpp"

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

class TranslationUnit
{
public:
   TranslationUnit()
      : tu_(NULL)
   {
   }

   TranslationUnit(const std::string& filename, CXTranslationUnit tu)
      : filename_(filename), tu_(tu)
   {
   }

   // translation units are managed and disposed by the SourceIndex, so
   // so instances of this class can be freely copied

   bool empty() const { return ! tu_; }

   std::string getSpelling() const;

   bool includesFile(const std::string& filename) const;

   unsigned getNumDiagnostics() const;
   Diagnostic getDiagnostic(unsigned index) const;

   // NOTE: this can return an empty code completion object
   // if the operation fails
   CodeCompleteResults codeCompleteAt(const std::string& filename,
                                      unsigned line,
                                      unsigned column);

   void printResourceUsage(std::ostream& ostr);

private:
   std::string filename_;
   CXTranslationUnit tu_;
};


} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_TRANSLATION_UNIT_HPP
