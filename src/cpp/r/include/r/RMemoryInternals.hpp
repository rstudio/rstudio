/*
 * RMemoryInternals.hpp
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

#define R_INTERNAL_FUNCTIONS
#include <r/RInternal.hpp>

#ifndef R_R_MEMORY_INTERNALS_HPP
#define R_R_MEMORY_INTERNALS_HPP

/* Access unexported information about R's memory internals */
extern "C" {
extern SEXP* R_PPStack;
extern int R_PPStackTop;
extern int R_PPStackSize;
}

namespace rstudio {
namespace r {
namespace internals {

int protectStackTop()
{
   return R_PPStackTop;
}

void logProtectedSEXPs()
{
   ::Rprintf("Begin protected R items\n");
   for (int i = 0; i < R_PPStackTop; ++i)
      Rf_PrintValue(R_PPStack[i]);
   ::Rprintf("End protected R items\n");
}

} // end namespace internals
} // end namespace r
} // end namespace rstudio

#endif /* R_R_MEMORY_INTERNALS_HPP */
