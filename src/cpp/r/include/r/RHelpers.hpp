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

#include <utility>

#define R_INTERNAL_FUNCTIONS
#include <r/RInternal.hpp>

#include <r/RSxpInfo.hpp>

namespace rstudio {
namespace r {

inline bool isImmediateBinding(SEXP frameSEXP)
{
   r::sxpinfo* infoSEXP = reinterpret_cast<r::sxpinfo*>(frameSEXP);
   return infoSEXP->extra != 0;
}

template <typename F>
bool recursiveFind(SEXP valueSEXP, F&& callback)
{
   // Apply callback to value.
   if (callback(valueSEXP))
      return true;
   
   // Recurse into other values.
   switch (TYPEOF(valueSEXP))
   {
   
   case ENVSXP:
   {
      // An R environment can either be hashed or unhashed.
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
            for (; frameSEXP != R_NilValue; frameSEXP = CDR(frameSEXP))
            {
               if (!isImmediateBinding(frameSEXP))
               {
                  SEXP elSEXP = CAR(frameSEXP);
                  if (recursiveFind(elSEXP, std::forward<F>(callback)))
                     return true;
               }
            }
         }
      }
      else
      {
         SEXP frameSEXP = FRAME(valueSEXP);
         for (; frameSEXP != R_NilValue; frameSEXP = CDR(frameSEXP))
         {
            if (!isImmediateBinding(frameSEXP))
            {
               SEXP elSEXP = CAR(frameSEXP);
               if (recursiveFind(elSEXP, std::forward<F>(callback)))
                  return true;
            }
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
         if (recursiveFind(elSEXP, std::forward<F>(callback)))
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
         if (recursiveFind(elSEXP, std::forward<F>(callback)))
            return true;
      }
      
      break;
   }
      
   } // switch (TYPEOF(valueSEXP))
   
   return false;
}

} // end namespace r
} // end namespace rstudio

#endif /* R_HELPERS_HPP */

