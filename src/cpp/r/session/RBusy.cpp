/*
 * RBusy.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <r/RExec.hpp>
#include <r/RSexp.hpp>

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {

bool isBusy()
{
   // sys.calls() will return NULL if there are no calls on the stack
   r::sexp::Protect protect;
   SEXP callsSEXP = R_NilValue;
   Error error = r::exec::RFunction("sys.calls").call(&callsSEXP, &protect);
   return callsSEXP != R_NilValue;
}

} // namespace session
} // namespace r
} // namespace rstudio
