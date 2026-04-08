/*
 * RRuntime.hpp
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

#ifndef R_R_RUNTIME_HPP
#define R_R_RUNTIME_HPP

#include <shared_core/Error.hpp>

#define R_NO_REMAP
#include <Rinternals.h>
#include <R_ext/RStartup.h>

// Runtime-safe wrappers for R API functions that may or may not exist
// depending on the version of R loaded at runtime. Symbols are resolved
// once during initialize() and dispatched through cached function pointers.
//
// This avoids compile-time R_VERSION checks, which break when the version
// of R used at compile time differs from the version loaded at runtime.

namespace rstudio {
namespace r {
namespace runtime {

// Initialize the runtime dispatch layer. Must be called after R is loaded
// but before any of the functions below are used. Returns an error if the
// R shared library cannot be loaded.
core::Error initialize();

// R_ClosureFormals (R >= 4.5), falls back to FORMALS.
SEXP closureFormals(SEXP x);

// R_ClosureBody (R >= 4.5), falls back to BODY.
SEXP closureBody(SEXP x);

// R_ClosureEnv (R >= 4.5), falls back to CLOENV.
SEXP closureEnv(SEXP x);

// R_ParentEnv (R >= 4.5), falls back to struct access.
SEXP parentEnv(SEXP envSEXP);

// R_getVarEx (R >= 4.6), falls back to Rf_findVarInFrame.
SEXP findVarInFrame(SEXP envSEXP, SEXP nameSEXP);

// R_getVarEx with inheritance (R >= 4.6), falls back to Rf_findVar.
SEXP findVar(SEXP nameSEXP, SEXP envSEXP);

// R_GetBindingType (R >= 4.6), falls back to manual struct inspection.
// Returns the R_BindingType_t enum value as an int.
int getBindingType(SEXP symSEXP, SEXP envSEXP);

// R_DelayedBindingExpression (R >= 4.6), falls back to promsxp struct access.
SEXP delayedBindingExpression(SEXP symSEXP, SEXP envSEXP);

// R_BindingType_t constants (must match the enum values in R >= 4.6).
// TODO: Add static_assert against R_BindingType_t once R 4.6 is released.
constexpr int kBindingTypeUnbound = 0;
constexpr int kBindingTypeValue   = 1;
constexpr int kBindingTypeMissing = 2;
constexpr int kBindingTypeDelayed = 3;
constexpr int kBindingTypeForced  = 4;
constexpr int kBindingTypeActive  = 5;

// Value returned by getBindingType when the symbol is not found
// (runtime R < 4.6 and R_findVarLocInFrame returns NULL/R_NilValue).
constexpr int kBindingTypeNotFound = -1;

// R_GetSaveAction / R_SetSaveAction (R >= 4.6), falls back to direct SaveAction global.
SA_TYPE getSaveAction();
SA_TYPE setSaveAction(SA_TYPE action);

} // namespace runtime
} // namespace r
} // namespace rstudio

#endif // R_R_RUNTIME_HPP
