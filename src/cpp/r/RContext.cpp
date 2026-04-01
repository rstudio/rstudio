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

#include <string>
#include <vector>

#include <r/RContext.hpp>
#include <r/RExec.hpp>
#include <r/RInterface.hpp>
#include <r/RSexp.hpp>
#include <r/session/RSession.hpp>

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

const char* contextTypeName(int callflag)
{
   switch (callflag)
   {
   case CTXT_TOPLEVEL: return "TOPLEVEL";
   case CTXT_NEXT:     return "NEXT";
   case CTXT_BREAK:    return "BREAK";
   case CTXT_LOOP:     return "LOOP";
   case CTXT_FUNCTION: return "FUNCTION";
   case CTXT_CCODE:    return "CCODE";
   case CTXT_RETURN:   return "RETURN";
   case CTXT_BROWSER:  return "BROWSER";
   case CTXT_GENERIC:  return "GENERIC";
   case CTXT_RESTART:  return "RESTART";
   case CTXT_BUILTIN:  return "BUILTIN";
   default:            return "UNKNOWN";
   }
}

} // anonymous namespace

bool isTopLevelContext()
{
   auto* ctx = globalContext();
   return ctx->callflag == CTXT_TOPLEVEL && ctx->nextcontext == nullptr;
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

SEXP dumpContexts()
{
   r::sexp::Protect protect;

   // first pass: count contexts
   int n = 0;
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext)
      n++;

   // build vectors
   SEXP typeVec = Rf_allocVector(STRSXP, n);
   protect.add(typeVec);
   SEXP callflagVec = Rf_allocVector(INTSXP, n);
   protect.add(callflagVec);
   SEXP callVec = Rf_allocVector(VECSXP, n);
   protect.add(callVec);
   SEXP cloenvVec = Rf_allocVector(VECSXP, n);
   protect.add(cloenvVec);

   int i = 0;
   for (auto* ctx = globalContext(); ctx != nullptr; ctx = ctx->nextcontext, i++)
   {
      SET_STRING_ELT(typeVec, i, Rf_mkChar(contextTypeName(ctx->callflag)));
      INTEGER(callflagVec)[i] = ctx->callflag;
      SET_VECTOR_ELT(callVec, i, ctx->call ? ctx->call : R_NilValue);
      SET_VECTOR_ELT(cloenvVec, i, ctx->cloenv ? ctx->cloenv : R_NilValue);
   }

   // assemble as a list (data.frame-like)
   SEXP result = Rf_allocVector(VECSXP, 4);
   protect.add(result);
   SET_VECTOR_ELT(result, 0, typeVec);
   SET_VECTOR_ELT(result, 1, callflagVec);
   SET_VECTOR_ELT(result, 2, callVec);
   SET_VECTOR_ELT(result, 3, cloenvVec);

   std::vector<std::string> nameStrings = {"type", "callflag", "call", "cloenv"};
   SEXP names = r::sexp::create(nameStrings, &protect);
   Rf_setAttrib(result, R_NamesSymbol, names);

   return result;
}

} // namespace context
} // namespace r
} // namespace rstudio
