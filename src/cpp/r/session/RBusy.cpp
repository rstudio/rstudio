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

#include <r/RInterface.hpp>

extern "C" {

typedef struct RCNTXT {
   struct RCNTXT *nextcontext;
} RCNTXT, *context;

int Rf_framedepth(RCNTXT* pContext);

} // extern "C"

namespace rstudio {
namespace r {
namespace session {

bool isBusy()
{
   // sanity check
   if (R_GlobalContext == NULL)
      return false;

   // are there any R frames in the global context?
   // if so, conclude this is because R is busy
   return Rf_framedepth((RCNTXT*) R_GlobalContext);
}

} // namespace session
} // namespace r
} // namespace rstudio
