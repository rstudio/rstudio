/*
 * RCntxt.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#ifndef R_CONTEXT_HPP
#define R_CONTEXT_HPP

#include "RSexp.hpp"

#include <core/Error.hpp>

#include <boost/iterator/iterator_facade.hpp>

namespace rstudio {
namespace r {
namespace context {

class RCntxt
{
public:
   bool hasSourceRefs();
   bool isDebugHidden();
   bool isErrorHandler();

   SEXP sourceRefs();
   SEXP originalFunctionCall();

   std::string shinyFunctionLabel();

   core::Error functionName(std::string* pFunctionName);   
   core::Error fileName(std::string* pFileName);
   core::Error callSummary(std::string* pCallSummary);

   bool operator==(const RCntxt& other) const;

   // implemented by R version specific internal context classes
   virtual bool isNull() const        = 0;
   virtual SEXP callfun() const       = 0;
   virtual int callflag() const       = 0;
   virtual SEXP call() const          = 0;
   virtual SEXP srcref() const        = 0;
   virtual SEXP cloenv() const        = 0;
   virtual RCntxt nextcontext() const = 0;

   // define an iterator for easy traversal of the context stack
   template <class Value>
   class cntxt_iterator: public boost::iterator_facade<
            cntxt_iterator<Value>, RCntxt, boost::forward_traversal_tag>
   {
   public:
      cntxt_iterator():
         pCntxt_(NULL)
      { }

      explicit cntxt_iterator(Value* pCntxt):
         pCntxt_(pCntxt)
      { }

   private:
      friend class boost::iterator_core_access;

      void increment()
      {
         pCntxt_ = pCntxt_->nextcontext();
      }

      bool equal(cntxt_iterator<Value> const& other) const
      {
         return *pCntxt_ == other.pCntxt_;
      }

      Value& dereference() const
      {
         return *pCntxt_;
      }

      Value* pCntxt_;
   };

   typedef cntxt_iterator<RCntxt> iterator;
   typedef cntxt_iterator<RCntxt const> const_iterator;

   static iterator begin();
   static iterator end();

private:
   // raw pointer to internal RCNTXT
   virtual void* rcntxt() const = 0;

   core::Error invokeFunctionOnCall(const char* rFunction, 
                                    std::string* pResult);
};

} // namespace context
} // namespace r
} // namespace rstudio

#endif
