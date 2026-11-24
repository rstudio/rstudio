/*
 * SessionJulia.hpp
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

#ifndef SESSION_MODULES_JULIA_HPP
#define SESSION_MODULES_JULIA_HPP

namespace rstudio {
namespace core {

class Error;

} // end namespace core
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace julia {

// has the administrator left Julia integration on (julia-enabled)?
bool isAllowed();

// is Julia allowed, and has the user turned the Julia console on
// (julia_console_enabled)?
bool isEnabled();

// is the console's Julia REPL (SessionJulia.R) reading input?
bool isReplActive();

// can the session be suspended without losing a running Julia's state?
bool isSuspendable();

core::Error initialize();

} // end namespace julia
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_MODULES_JULIA_HPP */
