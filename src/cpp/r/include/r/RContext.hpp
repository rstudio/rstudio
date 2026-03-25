/*
 * RContext.hpp
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

#ifndef R_CONTEXT_HPP
#define R_CONTEXT_HPP

#include <setjmp.h>

typedef struct SEXPREC* SEXP;
extern "C" SEXP R_NilValue;

// Lightweight context introspection utilities that depend only on the stable
// prefix of the RCNTXT struct (nextcontext through cloenv). These fields have
// been at the same offset in every supported version of R, and are safe to
// access by casting R_GlobalContext to the struct below.
//
// Functions using only {nextcontext, callflag} are async-signal safe.

namespace rstudio {
namespace r {
namespace context {

// View of an R context covering the stable prefix of the RCNTXT struct.
// The jmp_buf field is platform-dependent in size but fixed for a given build.
struct RContext
{
   RContext* nextcontext;
   int callflag;
#ifdef _WIN32
   // R's JMP_BUF on Windows wraps jmp_buf with signal mask fields
   struct { jmp_buf buf; int sigmask; int savedmask; } cjmpbuf;
#else
   sigjmp_buf cjmpbuf;
#endif
   int cstacktop;
   int evaldepth;
   SEXP promargs;
   SEXP callfun;
   SEXP sysparent;
   SEXP call;
   SEXP cloenv;
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
// R_GlobalContext is initialized early in R startup, before any RStudio
// code runs, so the return value is never null.
RContext* globalContext();


// Returns true when R is at the top-level prompt with no evaluation contexts
// on the stack (i.e. the context stack is empty).
inline bool isTopLevelContext()
{
   return globalContext()->callflag == CTXT_TOPLEVEL;
}

// Returns true when R has one or more function-call contexts on the stack.
// This indicates R is actively executing code (including internal RStudio code).
inline bool hasFunctionContext()
{
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
      if (ctx->callflag & CTXT_FUNCTION)
         return true;
   return false;
}

// Returns true when a browser context exists anywhere on the stack.
inline bool hasBrowserContext()
{
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
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
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
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

// Find the function context associated with the browser, or at a given depth.
//
// When depth == 0 (BROWSER_FUNCTION): finds the outermost function context
// whose cloenv matches the browser context's cloenv. Sets *pDepth to the
// inner-to-outer depth and *pEnv to the closure environment.
//
// When depth > 0: finds the function context at the given inner-to-outer
// depth. Sets *pEnv to its closure environment.
//
// Returns false if no matching context was found.
inline bool getFunctionContext(int depth, int* pDepth, SEXP* pEnv)
{
   int currentDepth = 0;
   int foundDepth = 0;
   SEXP foundEnv = nullptr;

   // Find the browser context's cloenv (for depth == 0 mode)
   SEXP browseEnv = nullptr;
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
   {
      if ((ctx->callflag & CTXT_BROWSER) && browseEnv == nullptr)
      {
         browseEnv = ctx->cloenv;
         break;
      }
   }

   // Walk the context stack looking for function contexts
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
   {
      if (ctx->callflag & CTXT_FUNCTION)
      {
         currentDepth++;

         if (depth == 0 && browseEnv != nullptr && ctx->cloenv == browseEnv)
         {
            foundDepth = currentDepth;
            foundEnv = ctx->cloenv;
            // continue -- we want the outermost match
         }
         else if (depth > 0 && currentDepth >= depth)
         {
            foundDepth = currentDepth;
            foundEnv = ctx->cloenv;
            break;
         }
      }
   }

   if (pDepth)
      *pDepth = foundDepth;
   if (pEnv)
      *pEnv = foundEnv ? foundEnv : R_NilValue;

   return foundDepth > 0;
}

// Check if the topmost function on the stack is a debugger-internal
// function (has the "hideFromDebugger" attribute).
bool inDebugHiddenContext();

} // namespace context
} // namespace r
} // namespace rstudio

#endif // R_CONTEXT_HPP
