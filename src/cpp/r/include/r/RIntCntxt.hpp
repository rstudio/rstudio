/*
 * RIntCntxt.cpp
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

#ifndef R_INTERNAL_CONTEXT_HPP
#define R_INTERNAL_CONTEXT_HPP

#include "RInterface.hpp"
#include "RCntxt.hpp"

namespace rstudio {
namespace r {
namespace context {

namespace detail {

template <typename T> struct version {};
template <> struct version<RCNTXT_34> { enum { value = 34 }; };
template <> struct version<RCNTXT_33> { enum { value = 33 }; };
template <> struct version<RCNTXT_32> { enum { value = 32 }; };

template <typename T> struct has_bytecode
      : public boost::integral_constant<bool, version<T>::value >= 34> {};

template <typename T>
SEXP bcbody(T* pContext, boost::true_type)
{
   return pContext->bcbody;
}

template <typename T>
SEXP bcbody(T* pContext, boost::false_type)
{
   return R_NilValue;
}

template <typename T>
void* bcpc(T* pContext, boost::true_type)
{
   return pContext->bcpc;
}

template <typename T>
void* bcpc(T* pContext, boost::false_type)
{
   return NULL;
}

} // end namespace detail

// header-only implementation of the RCntxtInterface; can serve as an 
// implementation for any memory layout (depending on the template parameter)
template<typename T> class RIntCntxt: public RCntxtInterface
{
public:
   explicit RIntCntxt(T *pCntxt) :
     pCntxt_(pCntxt)
   { }

   SEXP callfun() const
   {
      return pCntxt_->callfun;
   }

   int callflag() const
   {
      return pCntxt_->callflag;
   }

   SEXP call() const
   {
      return pCntxt_->call;
   }

   SEXP srcref() const
   {
      return pCntxt_->srcref;
   }

   SEXP cloenv() const
   {
      return pCntxt_->cloenv;
   }

   RCntxt nextcontext() const
   {
      if (pCntxt_->nextcontext == NULL)
         return RCntxt();
      else
         return RCntxt(pCntxt_->nextcontext);
   }
   
   SEXP bcbody() const
   {
      return detail::bcbody(
               pCntxt_,
               detail::has_bytecode<T>());
   }
   
   void* bcpc() const
   {
      return detail::bcpc(
               pCntxt_,
               detail::has_bytecode<T>());
   }

   bool isNull() const
   {
      return false;
   }

   void* rcntxt() const
   {
      return static_cast<void *>(pCntxt_);
   }

private:
   const T *pCntxt_;
};

} // namespace context
} // namespace r
} // namespace rstudio

#endif

