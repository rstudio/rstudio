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
                       F&& callback);

template <typename F>
bool recursiveFindFrame(SEXP frameSEXP,
                        std::set<SEXP>& visitedEnvironments,
                        F&& callback)
{
   for (; frameSEXP != R_NilValue; frameSEXP = CDR(frameSEXP))
   {
      if (!isImmediateBinding(frameSEXP))
      {
         SEXP elSEXP = CAR(frameSEXP);
         if (recursiveFindImpl(elSEXP, visitedEnvironments, std::forward<F>(callback)))
            return true;
      }
   }
   
   return false;
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
      
      // An R environment can either be hashed, or unhashed.
      // A hashed environment contains a VECSXP of 'frame's.
      // An unhashed environment contains a single 'frame'.
      // A 'frame' here is just a pairlist (LISTSXP).
      SEXP hashTableSEXP = HASHTAB(valueSEXP);
      if (hashTableSEXP != R_NilValue)
      {
         R_xlen_t numCells = Rf_xlength(hashTableSEXP);
         for (R_xlen_t i = 0; i < numCells; i++)
         {
            SEXP frameSEXP = VECTOR_ELT(hashTableSEXP, i);
            if (recursiveFindFrame(frameSEXP, visitedEnvironments, std::forward<F>(callback)))
               return true;
         }
      }
      else
      {
         SEXP frameSEXP = FRAME(valueSEXP);
         if (recursiveFindFrame(frameSEXP, visitedEnvironments, std::forward<F>(callback)))
            return true;
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
   std::set<SEXP> visitedEnvironments;
   return internal::recursiveFindImpl(
            valueSEXP,
            visitedEnvironments,
            std::forward<F>(callback));
}


} // end namespace r
} // end namespace rstudio

#endif /* R_HELPERS_HPP */

