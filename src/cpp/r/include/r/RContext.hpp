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

// Context introspection utilities. These use tracked state (prompt detection,
// captured environments) and R-side sys.*() functions rather than directly
// accessing R_GlobalContext or the RCNTXT struct.

typedef struct SEXPREC* SEXP;

namespace rstudio {
namespace r {
namespace context {

// Returns true when R is at the top-level prompt with no evaluation contexts
// on the stack (i.e. the context stack is empty).
bool isTopLevelContext();

// Returns true when we are in a "browse" debugging state: at a browse prompt
// with the browser environment inside a function (not the global env). This
// distinguishes interactive debugging from browsing at the top level.
bool inActiveBrowseContext();

// Find the function context associated with the browser, or at a given depth.
//
// When depth == 0 (BROWSER_FUNCTION): finds the outermost function context
// whose cloenv matches the browser context's cloenv. Sets *pDepth to the
// inner-to-outer depth and *pEnv to the closure environment. The browser
// context's environment is read from the context stack when `browsing` is true.
//
// When depth > 0: finds the function context at the given inner-to-outer
// depth. Sets *pEnv to its closure environment.
//
// Returns false if no matching context was found.
bool getFunctionContext(int depth, bool browsing, int* pDepth, SEXP* pEnv);

// Check if the topmost function on the stack is a debugger-internal
// function (has the "hideFromDebugger" attribute).
bool inDebugHiddenContext();

} // namespace context
} // namespace r
} // namespace rstudio

#endif // R_CONTEXT_HPP
