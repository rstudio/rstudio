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

// Lightweight context introspection utilities. All access to R_GlobalContext
// and the RCNTXT struct is encapsulated in RContext.cpp.

typedef struct SEXPREC* SEXP;

namespace rstudio {
namespace r {
namespace context {

// Returns true when R is at the top-level prompt with no evaluation contexts
// on the stack (i.e. the context stack is empty).
bool isTopLevelContext();

// Returns true when R has one or more function-call contexts on the stack.
// This indicates R is actively executing code (including internal RStudio code).
// NOTE: async-signal safe.
bool hasFunctionContext();

// Returns true when a browser context exists anywhere on the stack.
bool hasBrowserContext();

// Returns true when we are in a "browse" debugging state: a browser context
// AND at least one function context exist on the stack. This distinguishes
// interactive debugging from browsing at the top level.
bool inActiveBrowseContext();

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
bool getFunctionContext(int depth, int* pDepth, SEXP* pEnv);

// Check if the topmost function on the stack is a debugger-internal
// function (has the "hideFromDebugger" attribute).
bool inDebugHiddenContext();

} // namespace context
} // namespace r
} // namespace rstudio

#endif // R_CONTEXT_HPP
