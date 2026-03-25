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

#include <setjmp.h>

#define R_NO_REMAP
#include <Rinternals.h>

namespace rstudio {
namespace r {
namespace context {

namespace {

// View of an R context covering the stable prefix of the RCNTXT struct.
// The jmp_buf field is platform-dependent in size but fixed for a given build.
struct RContext
{
   RContext* nextcontext;
   int callflag;
#ifdef _WIN32
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

RContext* globalContext()
{
   return reinterpret_cast<RContext*>(R_GlobalContext);
}

} // anonymous namespace

bool isTopLevelContext()
{
   return globalContext()->callflag == CTXT_TOPLEVEL;
}

bool hasFunctionContext()
{
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
      if (ctx->callflag & CTXT_FUNCTION)
         return true;
   return false;
}

bool hasBrowserContext()
{
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
      if (ctx->callflag & CTXT_BROWSER)
         return true;
   return false;
}

bool inActiveBrowseContext()
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

bool getFunctionContext(int depth, int* pDepth, SEXP* pEnv)
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
