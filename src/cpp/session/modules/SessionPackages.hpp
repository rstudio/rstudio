/*
 * SessionPackages.hpp
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

#ifndef SESSION_PACKAGES_HPP
#define SESSION_PACKAGES_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace packages {

core::Error initialize();
void enquePackageStateChanged();

// Cheap syntactic pre-filter: returns true if 'input' contains a function call
// of the form 'fn(' or 'pkg::fn('. This only gates the more expensive, precise
// check in '.rs.isPackageManagementCall' (which parses the input and resolves
// each call's namespace), so that call-free console input -- assignments,
// comments, bare expressions -- never pays for it. Exposed for testing; also
// used by onConsolePrompt.
bool containsCallSyntax(const std::string& input);

} // namespace packages
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_PACKAGES_HPP
