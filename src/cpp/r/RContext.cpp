/*
 * RContext.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <r/RContext.hpp>
#include <r/RInterface.hpp>

#define R_NO_REMAP
#include <Rinternals.h>

namespace rstudio {
namespace r {
namespace context {

RContext* globalContext()
{
   return reinterpret_cast<RContext*>(R_GlobalContext);
}

bool inDebugHiddenContext()
{
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
   {
      if (ctx->callflag & CTXT_FUNCTION)
      {
         // If we find a debugger internal function before any user function,
         // hide it from the user callstack.
         SEXP hideFlag = Rf_getAttrib(ctx->callfun, Rf_install("hideFromDebugger"));
         if (TYPEOF(hideFlag) != NILSXP && Rf_asLogical(hideFlag))
            return true;

         // If we find a function with source refs (user code), don't hide
         SEXP srcref = Rf_getAttrib(ctx->callfun, Rf_install("srcref"));
         if (srcref != R_NilValue)
            return false;
      }
   }
   return false;
}

} // namespace context
} // namespace r
} // namespace rstudio
