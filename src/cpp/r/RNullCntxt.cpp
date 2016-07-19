/*
 * RNullCntxt.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#ifndef R_NULL_CONTEXT_HPP
#define R_NULL_CONTEXT_HPP

#include <core/Error.hpp>

namespace rstudio {
namespace r {

bool RNullCntxt::isNull() const
{
   return true;
}

SEXP RNullCntxt::callfun() const
{
   return R_NilValue;
}

int RNullCntxt::callflag() const
{
   return 0;
}

SEXP RNullCntxt::call() const
{ 
   return R_NilValue;
}

SEXP RNullCntxt::srcref() const
{
   return R_NilValue;
}

SEXP RNullCntxt::cloenv() const
{
   return R_NilValue;
}

RCntxt RNullCntxt::nextcontext() const
{
   return RNullCntxt();
}

void* RNullCntxt::rcntxt()
{
   return NULL;
}

} // namespace r
} // namespace rstudio

#endif
