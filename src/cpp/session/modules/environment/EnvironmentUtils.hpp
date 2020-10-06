/*
 * EnvironmentUtils.hpp
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

#ifndef SESSION_MODULES_ENVIRONMENT_UTILS_HPP
#define SESSION_MODULES_ENVIRONMENT_UTILS_HPP

#include <shared_core/json/Json.hpp>
#include <r/RSexp.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace environment {

core::json::Value varToJson(SEXP env, const r::sexp::Variable& var);
bool isUnevaluatedPromise(SEXP var);
bool functionDiffersFromSource(SEXP srcRef, const std::string& functionCode);
void sourceRefToJson(const SEXP srcref, core::json::Object* pObject);
core::Error sourceFileFromRef(const SEXP srcref, std::string* pFileName);
bool isAltrep(SEXP var);
bool hasAltrep(SEXP var);

} // namespace environment
} // namespace modules
} // namespace session
} // namespace rstudio

#endif /* SESSION_MODULES_ENVIRONMENT_UTILS_HPP */
