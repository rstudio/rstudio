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

namespace r {
namespace routines {

void addCMethod(const R_CMethodDef method);
void addCallMethod(const R_CallMethodDef method);

void registerAll();
   
} // namespace routines   
} // namespace r


#endif // R_ROUTINES_HPP 

