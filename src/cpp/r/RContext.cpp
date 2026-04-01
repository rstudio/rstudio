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
#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/session/RSession.hpp>

#define R_NO_REMAP
#include <Rinternals.h>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace context {

bool isTopLevelContext()
{
   return r::session::atDefaultPrompt() && !r::exec::isExecuting();
}

bool inActiveBrowseContext()
{
   // We're in an active browse context when we're at a browse prompt
   // and the browser's environment is inside a function (not at the
   // top level). This distinguishes debugging inside a function from
   // a bare browser() call at the global prompt.
   return r::session::browserContextActive() &&
          r::session::browserEnv() != R_GlobalEnv;
}

bool getFunctionContext(int depth, bool browsing, int* pDepth, SEXP* pEnv)
{
   // Read the browser environment captured by .rs.captureCurrentEnvironment(),
   // which was injected into the browser REPL by RReadConsole.
   SEXP browserEnv = browsing ? r::session::browserEnv() : R_NilValue;

   // Call the R-side implementation via the normal R_tryEval path.
   SEXP resultSEXP = R_NilValue;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.getFunctionContext")
      .addParam(depth)
      .addParam(browserEnv)
      .call(&resultSEXP, &protect);

   if (error)
      return false;

   // Validate the result is a 2-element list
   if (TYPEOF(resultSEXP) != VECSXP || Rf_length(resultSEXP) < 2)
      return false;

   int foundDepth = r::sexp::asInteger(VECTOR_ELT(resultSEXP, 0));
   SEXP foundEnv = VECTOR_ELT(resultSEXP, 1);

   if (pDepth)
      *pDepth = foundDepth;
   if (pEnv)
      *pEnv = foundEnv;

   return foundDepth > 0;
}

bool inDebugHiddenContext()
{
   bool result = false;
   Error error = r::exec::RFunction(".rs.inDebugHiddenContext")
      .call(&result);
   if (error)
      LOG_ERROR(error);
   return result;
}

} // namespace context
} // namespace r
} // namespace rstudio
