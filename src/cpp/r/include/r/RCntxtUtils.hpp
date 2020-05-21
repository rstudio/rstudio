/*
 * RCntxtUtils.hpp
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
   RVersion40, // R 4.0 and above
   RVersion34, // R 3.4 until R 4.0 (exclusive)
   RVersion33, // R 3.3 (all versions)
   RVersion32, // R 3.2.5 and below
   RVersionUnknown 
};

RCntxtVersion contextVersion();

RCntxt globalContext();
   
bool inBrowseContext();

bool inDebugHiddenContext();

RCntxt firstFunctionContext();

RCntxt getFunctionContext(const int depth, 
                          int* pFoundDepth = nullptr,
                          SEXP* pEnvironment = nullptr);

bool isByteCodeContext(const RCntxt& cntxt);
bool isByteCodeSrcRef(SEXP srcref);

} // namespace context
} // namespace r
} // namespace rstudio

#endif
