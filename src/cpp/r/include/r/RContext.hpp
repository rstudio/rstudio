/*
 * RContext.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#ifndef R_CONTEXT_HPP
#define R_CONTEXT_HPP

// Lightweight context introspection utilities that depend only on the stable
// prefix of the RCNTXT struct: {nextcontext, callflag}. These two fields have
// been at the same offset in every version of R, and are safe to access by
// casting R_GlobalContext to the minimal struct below.
//
// All functions here are async-signal safe.

namespace rstudio {
namespace r {
namespace context {

// Minimal view of an R context -- only the fields that have been stable
// across all R versions.
struct RContext
{
   RContext* nextcontext;
   int callflag;
};

// Context type flags (mirrored from Defn.h)
enum RContextType
{
   CTXT_TOPLEVEL = 0,
   CTXT_NEXT     = 1,
   CTXT_BREAK    = 2,
   CTXT_LOOP     = 3,
   CTXT_FUNCTION = 4,
   CTXT_CCODE    = 8,
   CTXT_RETURN   = 12,
   CTXT_BROWSER  = 16,
   CTXT_GENERIC  = 20,
   CTXT_RESTART  = 32,
   CTXT_BUILTIN  = 64
};

// Get the global context as a minimal RContext pointer.
RContext* contextStack();


// Returns true when R is at the top-level prompt with no evaluation contexts
// on the stack (i.e. the context stack is empty).
inline bool isTopLevelContext()
{
   return contextStack()->callflag == CTXT_TOPLEVEL;
}

// Returns true when R has one or more function-call contexts on the stack.
// This indicates R is actively executing code (including internal RStudio code).
inline bool hasFunctionContext()
{
   for (auto* ctx = contextStack(); ctx != nullptr; ctx = ctx->nextcontext)
      if (ctx->callflag & CTXT_FUNCTION)
         return true;
   return false;
}

// Returns true when a browser context exists anywhere on the stack.
inline bool hasBrowserContext()
{
   for (auto* ctx = contextStack(); ctx != nullptr; ctx = ctx->nextcontext)
      if (ctx->callflag & CTXT_BROWSER)
         return true;
   return false;
}

// Returns true when we are in a "browse" debugging state: a browser context
// AND at least one function context exist on the stack. This distinguishes
// interactive debugging from browsing at the top level.
inline bool inActiveBrowseContext()
{
   bool foundBrowser = false;
   bool foundFunction = false;
   for (auto* ctx = contextStack(); ctx != nullptr; ctx = ctx->nextcontext)
   {
      if ((ctx->callflag & CTXT_BROWSER) && !(ctx->callflag & CTXT_FUNCTION))
         foundBrowser = true;
      else if (ctx->callflag & CTXT_FUNCTION)
         foundFunction = true;
      if (foundBrowser && foundFunction)
         return true;
   }
   return false;
}

} // namespace context
} // namespace r
} // namespace rstudio

#endif // R_CONTEXT_HPP
