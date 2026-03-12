/*
 * SessionTrust.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#ifndef SESSION_TRUST_HPP
#define SESSION_TRUST_HPP

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace trust {

// Called early in session startup, before R initialization.
// Determines trust status for the given project directory.
void checkTrust(const core::FilePath& projectDir,
                const core::FilePath& userHomePath);

// Whether startup files (.Rprofile, .Renviron) should be suppressed
bool shouldSuppressStartupFiles();

// Whether .RData restore should be suppressed
bool shouldSuppressWorkspaceRestore();

// Returns the explicit trust setting for the current project directory:
// "trusted", "untrusted", or "default" (not in either list)
std::string explicitTrustSetting();

// Add a directory to the trusted or untrusted list
core::Error setTrust(const core::FilePath& directory, bool trusted);

// Remove the current project directory from both trust lists
core::Error resetTrust();

// Returns trust request data for inclusion in session info.
// If a trust prompt is needed, returns an object with "directory" and
// "risky_files" fields. Otherwise returns an empty object.
core::json::Object trustRequestData();

// Module initialization (registers RPCs and client event handlers)
core::Error initialize();

namespace overlay {

// Returns the default value for trust-enabled when not explicitly configured.
// Open-source returns false; Pro/Workbench overrides to return true.
bool trustEnabledByDefault();

} // namespace overlay

} // namespace trust
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_TRUST_HPP
