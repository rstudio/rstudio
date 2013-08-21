/*
 * EnvironmentUtils.hpp
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

#include <core/json/Json.hpp>
#include <r/RSexp.hpp>

namespace session {
namespace modules {
namespace environment {

core::json::Value varToJson(SEXP env, const r::sexp::Variable& var);
bool isUnevaluatedPromise(SEXP var);
bool functionDiffersFromSource(SEXP srcRef, const std::string& functionCode);
void sourceRefToJson(const SEXP srcref, core::json::Object* pObject);

} // namespace environment
} // namespace modules
} // namespace session
