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

#include <r/RCntxt.hpp>
#include <r/RCntxtUtils.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

namespace {
struct RContext {
   struct RContext* nextcontext;
   int callflag;
};
} // end anonymous namespace

// NOTE: Invoked in handleUSR2, so this needs to be async-signal safe.
bool isBusy()
{
   RContext* context = (RContext*) R_GlobalContext;
   for (; context != NULL; context = context->nextcontext)
      if (context->callflag & CTXT_FUNCTION)
         return true;

   return false;
}

} // namespace session
} // namespace r
} // namespace rstudio
