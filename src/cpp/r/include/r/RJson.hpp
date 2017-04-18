/*
 * RJson.hpp
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

#ifndef R_JSON_HPP
#define R_JSON_HPP

#include <core/json/Json.hpp>

typedef struct SEXPREC *SEXP;

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

// IMPORTANT NOTE: all code in r::json must provide "no jump" guarantee.
// See comment in RInternal.hpp for more info on this

namespace rstudio {
namespace r {
namespace json {
   
core::Error jsonValueFromScalar(SEXP scalarSEXP, core::json::Value* pValue);
core::Error jsonValueFromVector(SEXP vectorSEXP, core::json::Value* pValue);
core::Error jsonValueFromList(SEXP listSEXP, core::json::Value* pValue);
core::Error jsonValueFromObject(SEXP objectSEXP, core::json::Value* pValue);
   
} // namespace json
} // namespace r
} // namespace rstudio

#endif // R_JSON_HPP
