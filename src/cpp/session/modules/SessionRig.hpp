/*
 * SessionRig.hpp
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

#ifndef SESSION_RIG_HPP
#define SESSION_RIG_HPP

#include <string>

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace rig {

// Whether a rig binary is available (found on the PATH, in a standard
// location, or previously downloaded by RStudio).
bool isRigAvailable();

// Find an installed R matching the requested version (same major.minor).
// On success, *pInstalled is an object with 'version', 'path' and 'binary';
// when nothing matches it is null.
core::Error findInstalledRVersion(const std::string& version,
                                  core::json::Value* pInstalled);

core::Error initialize();

} // namespace rig
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_RIG_HPP
