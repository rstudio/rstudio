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

#ifndef SESSION_CHAT_INSTALLATION_HPP
#define SESSION_CHAT_INSTALLATION_HPP

#include <string>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace installation {

// ============================================================================
// Installation Detection
// ============================================================================

/**
 * Verify that an AI installation at the given path contains all required files.
 *
 * Checks for:
 * - dist/client directory
 * - dist/server/main.js script
 * - dist/client/index.html file
 *
 * @param positAiPath Path to the AI installation directory
 * @return true if all required files exist, false otherwise
 */
bool verifyPositAiInstallation(const core::FilePath& positAiPath);

/**
 * Locate the Posit AI installation directory.
 *
 * Search order:
 * 1. RSTUDIO_POSIT_AI_PATH environment variable (for development/testing)
 * 2. User data directory (XDG-based, platform-appropriate)
 *    - Linux: ~/.local/share/rstudio/ai
 *    - macOS: ~/Library/Application Support/RStudio/ai
 *    - Windows: %LOCALAPPDATA%/RStudio/ai
 * 3. System-wide installation (XDG config directory)
 *    - Linux: /etc/rstudio/ai
 *    - Windows: C:/ProgramData/RStudio/ai
 *
 * @return FilePath to the installation directory, or empty FilePath if not found
 */
core::FilePath locatePositAiInstallation();

/**
 * Get the installed version of Posit AI from package.json.
 *
 * @return Version string (e.g., "1.2.3"), or empty string if not found or invalid
 */
std::string getInstalledVersion();

} // namespace installation
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_INSTALLATION_HPP
