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
#include <r/RInterface.hpp>
#include <r/RSexp.hpp>

#include <setjmp.h>

#define R_NO_REMAP
#include <Rinternals.h>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace context {

namespace {

// Platform-dependent jump buffer type, matching R's RCNTXT definition.
#ifdef _WIN32
typedef struct { jmp_buf buf; int sigmask; int savedmask; } JMP_BUF;
#else
typedef sigjmp_buf JMP_BUF;
#endif

// View of an R context covering the stable prefix of the RCNTXT struct.
// The JMP_BUF field is platform-dependent in size but fixed for a given build.
struct RContext
{
   RContext* nextcontext;
   int callflag;
   JMP_BUF cjmpbuf;
   int cstacktop;
   int evaldepth;
   SEXP promargs;
   SEXP callfun;
   SEXP sysparent;
   SEXP call;
   SEXP cloenv;
};

// Context type flags (mirrored from Defn.h).
// These are bitmasks; some types combine flags from simpler types.
// In particular, any type with the CTXT_FUNCTION bit set (bit 2) is
// treated as a function context by sys.frame() and related APIs.
enum RContextType
{
   CTXT_TOPLEVEL = 0b0000000,
   CTXT_NEXT     = 0b0000001,
   CTXT_BREAK    = 0b0000010,
   CTXT_LOOP     = 0b0000011,  // CTXT_NEXT | CTXT_BREAK
   CTXT_FUNCTION = 0b0000100,
   CTXT_CCODE    = 0b0001000,
   CTXT_RETURN   = 0b0001100,  // CTXT_CCODE | CTXT_FUNCTION
   CTXT_BROWSER  = 0b0010000,
   CTXT_GENERIC  = 0b0010100,  // CTXT_BROWSER | CTXT_FUNCTION
   CTXT_RESTART  = 0b0100000,
   CTXT_BUILTIN  = 0b1000000,
};

RContext* globalContext()
{
   return reinterpret_cast<RContext*>(R_GlobalContext);
}

} // anonymous namespace

bool isTopLevelContext()
{
   auto* ctx = globalContext();
   return ctx->callflag == CTXT_TOPLEVEL && ctx->nextcontext == nullptr;
}

SEXP browserContextEnv()
{
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
      if (ctx->callflag & CTXT_BROWSER)
         return ctx->cloenv;
   return R_NilValue;
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

bool getFunctionContext(int depth, bool browsing, int* pDepth, SEXP* pEnv)
{
   // Call the R-side implementation which uses sys.frame() / sys.nframe()
   // rather than walking the RCNTXT stack directly.
   SEXP resultSEXP = R_NilValue;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.getFunctionContext")
      .addParam(depth)
      .addParam(browsing ? browserContextEnv() : R_NilValue)
      .call(&resultSEXP, &protect);

   if (error)
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
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
   {
      if (ctx->callflag & CTXT_FUNCTION)
      {
         // If we find a debugger internal function before any user function,
         // hide it from the user callstack.
         SEXP hideFlag = Rf_getAttrib(ctx->callfun, Rf_install("hideFromDebugger"));
         if (TYPEOF(hideFlag) != NILSXP && Rf_asLogical(hideFlag) == TRUE)
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
