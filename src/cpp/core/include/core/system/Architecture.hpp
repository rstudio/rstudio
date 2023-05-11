/*
 * Architecture.hpp
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

#ifndef CORE_SYSTEM_ARCHITECTURE_HPP
#define CORE_SYSTEM_ARCHITECTURE_HPP

#include <string>

#include <core/system/System.hpp>
#include <core/system/Process.hpp>

namespace rstudio {
namespace core {
namespace system {

// retrieve the architecture(s) a particular binary file
// has been compiled for. usually this is a single architecture
// and is the same as the host machine, but in some cases
// (e.g. macOS transitioning from x86_64 to arm64) some binaries
// may be compiled for multiple architectures
std::string supportedArchitectures(const core::FilePath& path);

bool haveCompatibleArchitectures(const core::FilePath& lhs,
                                 const core::FilePath& rhs);

} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif /* CORE_SYSTEM_ARCHITECTURE_HPP */
