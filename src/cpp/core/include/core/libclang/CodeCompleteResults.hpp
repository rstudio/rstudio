/*
 * CodeCompleteResults.hpp
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

#ifndef CORE_LIBCLANG_HPP
#define CORE_LIBCLANG_HPP

#include <boost/shared_ptr.hpp>

#include "clang-c/Index.h"

#include "Diagnostic.hpp"

namespace core {
namespace libclang {

class CodeCompleteResult
{
public:
   explicit CodeCompleteResult(CXCompletionResult result)
      : result_(result)
   {
   }

   std::string getText() const;

   std::string getBriefComment() const;

private:
   CXCompletionResult result_;
};


class CodeCompleteResults
{
public:
   CodeCompleteResults() {}
   explicit CodeCompleteResults(CXCodeCompleteResults* pResults)
      : pResults_(new CXCodeCompleteResults*(pResults))
   {
   }

   ~CodeCompleteResults();

   bool empty() const { return ! pResults_; }

   void sort();

   unsigned getNumResults() const { return results()->NumResults; }
   CodeCompleteResult getResult(unsigned index) const;

   unsigned getNumDiagnostics() const;
   Diagnostic getDiagnostic(unsigned index) const;

   unsigned long long getContexts() const;


private:
   CXCodeCompleteResults* results() const { return *pResults_; }

private:
   boost::shared_ptr<CXCodeCompleteResults*> pResults_;
};

} // namespace libclang
} // namespace core


#endif // CORE_LIBCLANG_HPP
