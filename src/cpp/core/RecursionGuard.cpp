/*
 * RecursionGuard.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/RecursionGuard.hpp>

namespace rstudio {
namespace core {

RecursionGuard::RecursionGuard (int* pCounter):
   pCounter_(pCounter)
{ 
   *pCounter_ = *pCounter_ + 1;
}

RecursionGuard::~RecursionGuard() 
{ 
   *pCounter_ = *pCounter_ - 1;
}

} // namespace core
} // namespace rstudio

