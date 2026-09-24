/*
 * EnvironmentUtils.hpp
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

#ifndef SESSION_MODULES_ENVIRONMENT_UTILS_HPP
#define SESSION_MODULES_ENVIRONMENT_UTILS_HPP

#include <shared_core/json/Json.hpp>
#include <r/RSexp.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace environment {

// whether ls() omits name: names beginning with a dot
bool isHiddenName(const std::string& name);

// the names the Environment pane lists for env, per the listing prefs
void listEnvironmentForPane(SEXP env, std::vector<std::string>* pNames);

// every name the pane could list for env, whatever the listing prefs
void listEnvironmentForMonitor(SEXP env, std::vector<std::string>* pNames);

// whether the pane lists name, per the listing prefs
bool isListedInPane(const std::string& name);

core::json::Value varToJson(const std::string& name, SEXP env);
bool isUnevaluatedPromise(const std::string& name, SEXP env);
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
