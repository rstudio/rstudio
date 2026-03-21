/*
 * RHelpers.hpp
 *
 * Copyright (C) 2023 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_HELPERS_HPP
#define R_HELPERS_HPP

#include <set>
#include <utility>

#define R_INTERNAL_FUNCTIONS
#include <r/RInternal.hpp>
#include <r/RSexp.hpp>

#include <r/RSxpInfo.hpp>

namespace rstudio {
namespace r {

namespace internal {

inline bool isImmediateBinding(SEXP frameSEXP)
{
   r::sxpinfo* info = reinterpret_cast<r::sxpinfo*>(frameSEXP);
   return info->extra != 0;
}

template <typename F>
bool recursiveFindImpl(SEXP valueSEXP,
                       std::set<SEXP>& visitedEnvironments,
                       F&& callback)
{
   // Apply callback to value.
   if (callback(valueSEXP))
      return true;

   // Recurse into other values.
   switch (TYPEOF(valueSEXP))
   {

   case ENVSXP:
   {
      // Avoid infinite recursion -- environments can contain themselves!
      auto result = visitedEnvironments.insert(valueSEXP);
      if (result.second == false)
         return false;

      // Enumerate bindings via the public API.
      r::sexp::Protect protect;
      SEXP namesSEXP;
      protect.add(namesSEXP = r::sexp::listEnvironment(valueSEXP));
      R_xlen_t n = Rf_xlength(namesSEXP);
      for (R_xlen_t i = 0; i < n; i++)
      {
         SEXP symSEXP = Rf_installChar(STRING_ELT(namesSEXP, i));
         if (r::sexp::isActiveBinding(symSEXP, valueSEXP))
            continue;
         SEXP elSEXP = Rf_findVarInFrame(valueSEXP, symSEXP);
         if (elSEXP != R_UnboundValue &&
             recursiveFindImpl(elSEXP, visitedEnvironments, std::forward<F>(callback)))
         {
            return true;
         }
      }

      break;
   }

   case VECSXP:
   {
      R_xlen_t n = Rf_xlength(valueSEXP);
      for (R_xlen_t i = 0; i < n; i++)
      {
         SEXP elSEXP = VECTOR_ELT(valueSEXP, i);
         if (recursiveFindImpl(elSEXP, visitedEnvironments, std::forward<F>(callback)))
            return true;
      }
      
      break;
   }
      
   case LISTSXP:
   case LANGSXP:
   {
      for (; valueSEXP != R_NilValue; valueSEXP = CDR(valueSEXP))
      {
         SEXP elSEXP = CAR(valueSEXP);
         if (recursiveFindImpl(elSEXP, visitedEnvironments, std::forward<F>(callback)))
            return true;
      }
      
      break;
   }
      
   } // switch (TYPEOF(valueSEXP))
   
   return false;
}

} // end namespace internal

template <typename F>
bool recursiveFind(SEXP valueSEXP, F&& callback)
{
   // We wrap the recursive search in R_ToplevelExec to catch any R errors
   // that might be thrown by R API functions (e.g. R_BindingIsActive,
   // Rf_findVarInFrame) during environment traversal. This prevents
   // longjmps from skipping C++ stack unwinding.
   //
   // Note that the visited environments set is allocated here (outside
   // R_ToplevelExec) so that it is properly destructed even if R longjmps.
   struct Context
   {
      SEXP valueSEXP;
      F* callback;
      std::set<SEXP> visitedEnvironments;
      bool result;
   };

   Context context = { valueSEXP, &callback, {}, false };

   auto toplevelCallback = [](void* data)
   {
      auto* ctx = static_cast<Context*>(data);
      ctx->result = internal::recursiveFindImpl(
         ctx->valueSEXP,
         ctx->visitedEnvironments,
         *ctx->callback);
   };

   Rboolean success = R_ToplevelExec(toplevelCallback, &context);

   // If the traversal encountered an R error, treat it conservatively
   // as "found" so callers err on the side of caution (e.g. isSerializable
   // will treat the object as non-serializable rather than silently
   // classifying it as safe).
   if (!success)
      return true;

   return context.result;
}


} // end namespace r
} // end namespace rstudio

#endif /* R_HELPERS_HPP */

