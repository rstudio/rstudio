/*
 * RRuntime.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#define R_INTERNAL_FUNCTIONS

#include <shared_core/Error.hpp>

#include <core/Log.hpp>
#include <core/system/LibraryLoader.hpp>

#include <r/RRuntime.hpp>
#include <r/RSexpInternal.hpp>

#if defined(_WIN32)
# define kRLibraryName "R.dll"
#else
# include <dlfcn.h>
#endif

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace runtime {

namespace {

// Handle to the loaded R shared library, used for symbol resolution.
void* s_library = nullptr;

// Resolve a function pointer from libR by name.
#define RS_IMPORT_FUNCTION(__NAME__)                                            \
   do                                                                           \
   {                                                                            \
      void* symbol = nullptr;                                                   \
      core::system::loadSymbol(s_library, #__NAME__, &symbol);                  \
      __NAME__ = reinterpret_cast<decltype(__NAME__)>(symbol);                  \
   } while (0)

// Resolve a global variable from libR by name.
// dlsym returns the address of the variable, so we dereference it.
#define RS_IMPORT_DATA(__NAME__)                                                \
   do                                                                           \
   {                                                                            \
      void* symbol = nullptr;                                                   \
      core::system::loadSymbol(s_library, #__NAME__, &symbol);                  \
      if (symbol)                                                               \
         __NAME__ = *reinterpret_cast<decltype(__NAME__)*>(symbol);             \
   } while (0)

// R API function pointers, resolved in initialize().
// nullptr when the runtime R version doesn't provide them.
SEXP (*FORMALS)(SEXP) = nullptr;
SEXP (*BODY)(SEXP) = nullptr;
SEXP (*CLOENV)(SEXP) = nullptr;

SEXP (*PRCODE)(SEXP) = nullptr;
SEXP (*PRENV)(SEXP) = nullptr;
SEXP (*PRVALUE)(SEXP) = nullptr;

SEXP (*R_ClosureFormals)(SEXP) = nullptr;
SEXP (*R_ClosureBody)(SEXP) = nullptr;
SEXP (*R_ClosureEnv)(SEXP) = nullptr;
SEXP (*R_ParentEnv)(SEXP) = nullptr;
SEXP (*R_getVarEx)(SEXP, SEXP, int, SEXP) = nullptr;
int  (*R_GetBindingType)(SEXP, SEXP) = nullptr;
SEXP (*R_DelayedBindingExpression)(SEXP, SEXP) = nullptr;

SEXP (*R_findVarLocInFrame)(SEXP, SEXP) = nullptr;
SEXP (*Rf_findVarInFrame)(SEXP, SEXP) = nullptr;
SEXP (*Rf_findVar)(SEXP, SEXP) = nullptr;

SA_TYPE (*R_GetSaveAction)(void) = nullptr;
SA_TYPE (*R_SetSaveAction)(SA_TYPE) = nullptr;

// R_UnboundValue is imported so we can translate it to nullptr at the boundary.
SEXP R_UnboundValue = nullptr;

// Pointer to the SaveAction global in R (fallback for R < 4.6).
SA_TYPE* s_pSaveAction = nullptr;

// Dispatch function pointers, set in initialize() to either the
// resolved R symbol or a fallback implementation.
SEXP (*s_closureFormals)(SEXP) = nullptr;
SEXP (*s_closureBody)(SEXP) = nullptr;
SEXP (*s_closureEnv)(SEXP) = nullptr;
SEXP (*s_parentEnv)(SEXP) = nullptr;
SEXP (*s_findVarInFrame)(SEXP, SEXP) = nullptr;
SEXP (*s_findVar)(SEXP, SEXP) = nullptr;
int  (*s_getBindingType)(SEXP, SEXP) = nullptr;
SEXP (*s_delayedBindingExpression)(SEXP, SEXP) = nullptr;
SA_TYPE (*s_getSaveAction)(void) = nullptr;
SA_TYPE (*s_setSaveAction)(SA_TYPE) = nullptr;

} // anonymous namespace


// -- Public API --

Error initialize()
{
#if defined(_WIN32)
   // On Windows, R.dll is a separate DLL resolvable via the PATH.
   Error error = core::system::loadLibrary(kRLibraryName, &s_library);
   if (error)
      return error;
#else
   // On POSIX, R is already loaded into the process. Use the process
   // handle so dlsym can find R symbols without needing the library
   // on the search path.
   s_library = ::dlopen(nullptr, RTLD_NOW);
   if (s_library == nullptr)
      return systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);
#endif

   RS_IMPORT_FUNCTION(FORMALS);
   RS_IMPORT_FUNCTION(BODY);
   RS_IMPORT_FUNCTION(CLOENV);

   RS_IMPORT_FUNCTION(PRCODE);
   RS_IMPORT_FUNCTION(PRENV);
   RS_IMPORT_FUNCTION(PRVALUE);

   RS_IMPORT_FUNCTION(R_ClosureBody);
   RS_IMPORT_FUNCTION(R_ClosureEnv);
   RS_IMPORT_FUNCTION(R_ClosureFormals);
   RS_IMPORT_FUNCTION(R_DelayedBindingExpression);
   RS_IMPORT_FUNCTION(R_GetBindingType);
   RS_IMPORT_FUNCTION(R_getVarEx);
   RS_IMPORT_FUNCTION(R_ParentEnv);

   RS_IMPORT_FUNCTION(R_findVarLocInFrame);
   RS_IMPORT_FUNCTION(Rf_findVarInFrame);
   RS_IMPORT_FUNCTION(Rf_findVar);

   RS_IMPORT_FUNCTION(R_GetSaveAction);
   RS_IMPORT_FUNCTION(R_SetSaveAction);

   RS_IMPORT_DATA(R_UnboundValue);

   // closureFormals / closureBody / closureEnv
   s_closureFormals = R_ClosureFormals ? R_ClosureFormals : FORMALS;
   s_closureBody    = R_ClosureBody    ? R_ClosureBody    : BODY;
   s_closureEnv     = R_ClosureEnv     ? R_ClosureEnv     : CLOENV;

   // parentEnv
   if (R_ParentEnv)
   {
      s_parentEnv = R_ParentEnv;
   }
   else
   {
      s_parentEnv = [](SEXP envSEXP) -> SEXP {
         SEXPREC* rec = reinterpret_cast<SEXPREC*>(envSEXP);
         return reinterpret_cast<SEXP>(rec->u.envsxp.enclos);
      };
   }

   // findVarInFrame / findVar
   if (R_getVarEx)
   {
      s_findVarInFrame = [](SEXP envSEXP, SEXP nameSEXP) -> SEXP {
         return R_getVarEx(nameSEXP, envSEXP, 0 /*FALSE*/, nullptr);
      };

      s_findVar = [](SEXP nameSEXP, SEXP envSEXP) -> SEXP {
         return R_getVarEx(nameSEXP, envSEXP, 1 /*TRUE*/, nullptr);
      };
   }
   else
   {
      s_findVarInFrame = Rf_findVarInFrame;
      s_findVar = Rf_findVar;
   }

   // getBindingType
   if (R_GetBindingType)
   {
      s_getBindingType = R_GetBindingType;
   }
   else
   {
      s_getBindingType = [](SEXP symSEXP, SEXP envSEXP) -> int {

         static constexpr unsigned int ACTIVE_BINDING_MASK = 1 << 15;

         SEXP loc = R_findVarLocInFrame(envSEXP, symSEXP);
         if (loc == nullptr || loc == R_NilValue)
            return kBindingTypeNotFound;

         sxpinfo_struct& info = *reinterpret_cast<sxpinfo_struct*>(loc);
         if (info.gp & ACTIVE_BINDING_MASK)
            return kBindingTypeActive;

         if (info.extra != 0)
            return kBindingTypeValue;

         SEXP val = CAR(loc);
         if (val == R_MissingArg)
            return kBindingTypeMissing;

         if (TYPEOF(val) == PROMSXP)
         {
            SEXPREC* promise = reinterpret_cast<SEXPREC*>(val);
            if (promise->u.promsxp.value == R_UnboundValue)
               return kBindingTypeDelayed;
         }

         return kBindingTypeValue;

      };
   }

   // delayedBindingExpression
   if (R_DelayedBindingExpression)
   {
      s_delayedBindingExpression = R_DelayedBindingExpression;
   }
   else
   {
      s_delayedBindingExpression = [](SEXP symSEXP, SEXP envSEXP) -> SEXP {
         SEXP promiseSEXP = Rf_findVarInFrame(envSEXP, symSEXP);
         if (promiseSEXP == R_UnboundValue || TYPEOF(promiseSEXP) != PROMSXP)
            return R_NilValue;
         SEXPREC* promise = reinterpret_cast<SEXPREC*>(promiseSEXP);
         return reinterpret_cast<SEXP>(promise->u.promsxp.expr);
      };
   }

   // getSaveAction / setSaveAction
   if (R_GetSaveAction && R_SetSaveAction)
   {
      s_getSaveAction = R_GetSaveAction;
      s_setSaveAction = R_SetSaveAction;
   }
   else
   {
      if (R_GetSaveAction || R_SetSaveAction)
         LOG_WARNING_MESSAGE("Only one of R_GetSaveAction / R_SetSaveAction resolved; falling back to SaveAction global");

      // Resolve the SaveAction global variable by address.
      void* symbol = nullptr;
      Error error = core::system::loadSymbol(s_library, "SaveAction", &symbol);
      if (error)
      {
         LOG_ERROR(error);

         s_getSaveAction = []() -> SA_TYPE {
            return SA_DEFAULT;
         };

         s_setSaveAction = [](SA_TYPE) -> SA_TYPE {
            return SA_DEFAULT;
         };
      }
      else
      {
         s_pSaveAction = reinterpret_cast<SA_TYPE*>(symbol);

         s_getSaveAction = []() -> SA_TYPE {
            return *s_pSaveAction;
         };

         s_setSaveAction = [](SA_TYPE action) -> SA_TYPE {
            SA_TYPE old = *s_pSaveAction;
            *s_pSaveAction = action;
            return old;
         };
      }
   }

   return Success();
}

SEXP closureFormals(SEXP x)
{
   return s_closureFormals(x);
}

SEXP closureBody(SEXP x)
{
   return s_closureBody(x);
}

SEXP closureEnv(SEXP x)
{
   return s_closureEnv(x);
}

SEXP parentEnv(SEXP envSEXP)
{
   return s_parentEnv(envSEXP);
}

// Convert R_UnboundValue to nullptr at the boundary so callers
// can use simple null checks instead of referencing a non-API symbol.
static SEXP unboundToNull(SEXP value)
{
   return (value == R_UnboundValue) ? nullptr : value;
}

SEXP findVarInFrame(SEXP envSEXP, SEXP nameSEXP)
{
   return unboundToNull(s_findVarInFrame(envSEXP, nameSEXP));
}

SEXP findVar(SEXP nameSEXP, SEXP envSEXP)
{
   return unboundToNull(s_findVar(nameSEXP, envSEXP));
}

int getBindingType(SEXP symSEXP, SEXP envSEXP)
{
   return s_getBindingType(symSEXP, envSEXP);
}

SEXP delayedBindingExpression(SEXP symSEXP, SEXP envSEXP)
{
   return s_delayedBindingExpression(symSEXP, envSEXP);
}

SA_TYPE getSaveAction()
{
   return s_getSaveAction();
}

SA_TYPE setSaveAction(SA_TYPE action)
{
   return s_setSaveAction(action);
}

} // namespace runtime
} // namespace r
} // namespace rstudio
