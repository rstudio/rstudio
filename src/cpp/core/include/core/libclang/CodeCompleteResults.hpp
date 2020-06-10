/*
 * CodeCompleteResults.hpp
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

#ifndef CORE_LIBCLANG_HPP
#define CORE_LIBCLANG_HPP

#include <boost/noncopyable.hpp>
#include <boost/shared_ptr.hpp>

#include "clang-c/Index.h"

#include "Diagnostic.hpp"

namespace rstudio {
namespace core {
namespace libclang {

class CodeCompleteResult
{
public:
   explicit CodeCompleteResult(CXCompletionResult result);

   CXCursorKind getKind() const { return kind_; }

   CXAvailabilityKind getAvailability() const { return availability_; }

   unsigned getPriority() const { return priority_; }

   std::string getTypedText() const { return typedText_; }

   std::string getText() const { return text_; }

   std::string getComment() const { return comment_; }

private:
   CXCursorKind kind_;
   CXAvailabilityKind availability_;
   unsigned priority_;
   std::string typedText_;
   std::string text_;
   std::string comment_;
};


class CodeCompleteResults : boost::noncopyable
{
public:
   CodeCompleteResults() : pResults_(nullptr) {}
   explicit CodeCompleteResults(CXCodeCompleteResults* pResults)
      : pResults_(pResults)
   {
   }

   ~CodeCompleteResults();

   bool empty() const { return pResults_ == nullptr; }

   void sort();

   unsigned getNumResults() const { return pResults_->NumResults; }
   CodeCompleteResult getResult(unsigned index) const;

   unsigned long long getContexts() const;

private:
   CXCodeCompleteResults* pResults_;
};

} // namespace libclang
} // namespace core
} // namespace rstudio


#endif // CORE_LIBCLANG_HPP
