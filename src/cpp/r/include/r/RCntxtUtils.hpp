/*
 * RCntxtUtils.hpp
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

#ifndef R_CONTEXT_UTILS_HPP
#define R_CONTEXT_UTILS_HPP

#include <shared_core/Error.hpp>
#include "RCntxt.hpp"

namespace rstudio {
namespace r {
namespace context {

#define BROWSER_FUNCTION 0

// represents the version of the memory layout of the RCNTXT structure
enum RCntxtVersion
{
   RVersion44, // R (>= 4.4.0)
   RVersion40, // R (>= 4.0.0)
   RVersion34, // R (>= 3.4.0
   RVersion33, // R (== 3.3.0)
   RVersion32, // R (3.2.x and older)
   RVersionUnknown 
};

RCntxtVersion contextVersion();

RCntxt globalContext();
   
bool inBrowseContext();

bool inDebugHiddenContext();

RCntxt getFunctionContext(const int depth, 
                          int* pFoundDepth = nullptr,
                          SEXP* pEnvironment = nullptr);

bool isByteCodeContext(const RCntxt& cntxt);
bool isByteCodeSrcRef(SEXP srcref);

} // namespace context
} // namespace r
} // namespace rstudio

#endif
