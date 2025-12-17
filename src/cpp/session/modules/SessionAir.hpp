/*
 * SessionAir.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#ifndef RSTUDIO_SESSION_MODULES_AIR
#define RSTUDIO_SESSION_MODULES_AIR

namespace rstudio {
namespace core {
class Error;
class FilePath;
} // end namespace core
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace air {

core::FilePath getAirTomlPath(const core::FilePath& projectPath);
core::FilePath findAirTomlPath(const core::FilePath& documentPath);

core::Error initialize();

} // end namespace air
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_AIR */