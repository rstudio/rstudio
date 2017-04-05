/*
 * RRoutines.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef R_ROUTINES_HPP
#define R_ROUTINES_HPP

#include <vector>

#include <R_ext/Rdynload.h>

namespace rstudio {
namespace r {
namespace routines {

void addCallMethod(const R_CallMethodDef method);
void registerCallMethod(const char* name, DL_FUNC fun, int numArgs);
void registerAll();

#define RS_REGISTER_CALL_METHOD(__NAME__, __NUM_ARGS__)                        \
   do                                                                          \
   {                                                                           \
      R_CallMethodDef callMethodDef;                                           \
      callMethodDef.name = #__NAME__;                                          \
      callMethodDef.fun = (DL_FUNC) __NAME__;                                  \
      callMethodDef.numArgs = __NUM_ARGS__;                                    \
      ::rstudio::r::routines::addCallMethod(callMethodDef);                    \
   } while (0)

} // namespace routines   
} // namespace r
} // namespace rstudio


#endif // R_ROUTINES_HPP 

