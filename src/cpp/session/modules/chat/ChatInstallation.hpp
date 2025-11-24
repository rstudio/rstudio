/*
 * ChatInstallation.hpp
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

#ifndef RSTUDIO_SESSION_MODULES_CHAT_INSTALLATION_HPP
#define RSTUDIO_SESSION_MODULES_CHAT_INSTALLATION_HPP

namespace rstudio {
namespace core {
class FilePath;
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace chat {

/**
 * Verify that an AI installation at the given path contains all required files.
 */
bool verifyPositAiInstallation(const core::FilePath& positAiPath);

/**
 * Locate the Posit AI installation directory.
 *
 * Searches in order:
 * 1. RSTUDIO_POSIT_AI_PATH environment variable
 * 2. User data directory (~/.local/share/rstudio/ai on Linux)
 * 3. System config directory (/etc/rstudio/ai on Linux)
 *
 * @return FilePath to installation, or empty FilePath if not found
 */
core::FilePath locatePositAiInstallation();

} // end namespace chat
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_CHAT_INSTALLATION_HPP */
