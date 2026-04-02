/*
 * RBusy.cpp
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

#include <r/session/RSession.hpp>

namespace rstudio {
namespace r {
namespace session {

// NOTE: Invoked in handleUSR2, so this needs to be async-signal safe.
// Returns true when R is not at the default top-level prompt — this includes
// user code execution, debug/browser sessions, loops, and internal RStudio
// R code execution. This is intentionally broader than the previous check
// which only looked for function contexts (CTXT_FUNCTION).
bool isBusy()
{
   return !isAtTopLevel();
}

} // namespace session
} // namespace r
} // namespace rstudio
