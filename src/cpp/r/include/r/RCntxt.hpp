/*
 * RCntxt.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_CONTEXT_HPP
#define R_CONTEXT_HPP

#include "RSexp.hpp"
#include "RCntxtInterface.hpp"

#include <shared_core/Error.hpp>

#include <boost/iterator/iterator_facade.hpp>

namespace rstudio {
namespace r {
namespace context {

// RCnxt is a public-facing class which wraps entries from the R context
// stack (RCNXT* in the R headers), which keeps track of execution contexts
// during the evaluation of R code. Unfortunately, the memory layout
// in the RCNXT* struct is not identical in all R versions, and consequently
// it's necessary to wrap them in a layer of abstraction, which this class
// supplies.
//
// It has value semantics; its only data member is a shared pointer to the class
// managing the RNCTXT* interface. It also implements simple iteration over
// entries in the context stack. RCnxt::begin() returns an iterator beginning
// at R_GlobalContext; RCnxt::end() returns a null context entry.
class RCntxt: public RCntxtInterface
{
public:
   // copy/equality testing
   RCntxt();
   explicit RCntxt(void *rawCntxt);
   bool operator==(const RCntxt& other) const;
   bool operator!=(const RCntxt& other) const;

   // safe coercion to boolean
   explicit operator bool() const
   {
      return pCntxt_ != nullptr;
   }

   // utility/accessor functions
   bool hasSourceRefs() const;
   bool isDebugHidden() const;
   bool isErrorHandler() const;

   // retrieve the source references associated with the context
   // (the source reference where this context originated)
   SEXP contextSourceRefs() const;
   
   // retrieve source references associated with the calling function
   // (where that calling function was actually defined in source)
   SEXP callFunSourceRefs() const;
   
   // for traced functions, the 'real' function will be replaced by
   // a wrapper function -- use this to find the original function
   SEXP originalFunctionCall() const;

   std::string shinyFunctionLabel() const;

   core::Error functionName(std::string* pFunctionName) const;
   core::Error fileName(std::string* pFileName) const;
   core::Error callSummary(std::string* pCallSummary) const;

   // implemented by R version specific internal context classes
   //
   // NOTE: 'srcref()' attribute may be set to <in-bc-interp>
   // symbol for contexts associated with bytecode evaluation!
   bool isNull() const;
   SEXP callfun() const;
   int callflag() const;
   int evaldepth() const;
   SEXP call() const;
   SEXP srcref() const;
   SEXP cloenv() const;
   RCntxt nextcontext() const;

   // define an iterator for easy traversal of the context stack
   template <class Value>
   class cntxt_iterator: public boost::iterator_facade<
            cntxt_iterator<Value>,
            Value,  // value type
            boost::forward_traversal_tag,
            const Value&>  // reference type
   {
   public:
      cntxt_iterator():
         context_()
      { }

      explicit cntxt_iterator(Value pCntxt):
         context_(pCntxt)
      { }

   private:
      friend class boost::iterator_core_access;

      void increment()
      {
         context_ = context_.nextcontext();
      }

      bool equal(cntxt_iterator<Value> const& other) const
      {
         return context_ == other.context_;
      }

      const Value& dereference() const
      {
         return context_;
      }

      Value context_;
   };

   typedef cntxt_iterator<RCntxt> iterator;
   typedef cntxt_iterator<RCntxt const> const_iterator;

   static iterator begin();
   static iterator end();

private:
   boost::shared_ptr<RCntxtInterface> pCntxt_;

   core::Error invokeFunctionOnCall(const char* rFunction, 
                                    std::string* pResult) const;
};

} // namespace context
} // namespace r
} // namespace rstudio

#endif
