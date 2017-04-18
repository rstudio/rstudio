/*
 * RJsonRpc.hpp
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

#ifndef R_R_JSON_RPC_HPP
#define R_R_JSON_RPC_HPP

#include <string>

#include <core/json/JsonRpc.hpp>

typedef struct SEXPREC *SEXP;

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace json {
   
core::Error getRpcMethods(core::json::JsonRpcMethods* pMethods);
   
} // namespace json
} // namespace r
} // namespace rstudio

#endif // R_R_JSON_RPC_HPP
