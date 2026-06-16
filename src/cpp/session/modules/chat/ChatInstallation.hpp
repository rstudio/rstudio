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
#include <shared_core/Error.hpp>
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
 * Locate the Posit Assistant installation directory.
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
core::FilePath locatePositAssistantInstallation();

/**
 * Get the installed version of Posit Assistant from package.json.
 *
 * @return Version string (e.g., "1.2.3"), or empty string if not found or invalid
 */
std::string getInstalledVersion();

/**
 * Get the protocol version the installed Posit Assistant package was built for.
 *
 * Reads the protocol.json file written at install time. Legacy installs
 * (before this file existed) return an empty string.
 *
 * @return Protocol version string (e.g., "10.0"), or empty string if missing or unreadable
 */
std::string getInstalledProtocolVersion();

/**
 * Ensure a protocol.json file exists in the given installation directory.
 *
 * Newer Posit Assistant packages bundle their own protocol.json. To avoid
 * clobbering the version the package declares, RStudio's compiled-in protocol
 * version is written only when the file is absent (e.g. older packages that
 * predate protocol.json).
 *
 * @param positAiPath Path to the AI installation directory
 * @return Success, or an error if the file was missing and could not be written
 */
core::Error writeProtocolVersionFileIfMissing(const core::FilePath& positAiPath);

} // namespace installation
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_INSTALLATION_HPP
