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
 * Get the system-wide Posit Assistant installation directory.
 *
 * This is the posit-assistant-path session option when set, so that an
 * administrator can manage the installation from an arbitrary location, and
 * the XDG config directory otherwise:
 *    - Linux/macOS: /etc/rstudio/pai/bin
 *    - Windows: C:/ProgramData/rstudio/pai/bin
 *
 * @return FilePath to the system-wide installation directory
 */
core::FilePath systemPositAssistantInstallPath();

/**
 * Get the directory holding the Posit Assistant copy shipped with RStudio.
 *
 * The bundle is installed beside the session binary, except in the macOS app
 * bundle where it sits next to bin/ rather than inside it. The first location
 * is returned when it holds a valid installation, and the second otherwise --
 * so the returned path is where the bundle would be even when none is
 * installed. Only commercial builds ship one; in open source neither
 * directory exists.
 *
 * @param resourcePath Root to resolve against (the session resource path)
 * @return FilePath to the bundled installation directory
 */
core::FilePath bundledPositAssistantInstallPath(const core::FilePath& resourcePath);

/**
 * Get the directory holding the Posit Assistant copy shipped with RStudio,
 * resolved against this session's resource path.
 *
 * @return FilePath to the bundled installation directory
 */
core::FilePath bundledPositAssistantInstallPath();

/**
 * The tiers locatePositAssistantInstallation() searches. Resolved from
 * session options by positAssistantSearchPaths(); passed explicitly so tests
 * can drive the search without ambient state.
 */
struct InstallSearchPaths
{
   InstallSearchPaths() : pinnedSystemPath(false) {}

   // XDG user data directory install -- the one RStudio itself writes
   core::FilePath userDataPath;

   // posit-assistant-path when set, the XDG system config directory otherwise
   core::FilePath systemPath;

   // the copy shipped with RStudio; only commercial builds ship one
   core::FilePath bundledPath;

   // posit-assistant-path is set, so systemPath is an administrator's
   // deliberate choice and ends the search when it holds no installation
   bool pinnedSystemPath;
};

/**
 * Resolve the search tiers for this session.
 *
 * @return InstallSearchPaths for locatePositAssistantInstallation()
 */
InstallSearchPaths positAssistantSearchPaths();

/**
 * Locate the Posit Assistant installation directory among the given tiers.
 *
 * Search order:
 * 1. User data directory (XDG-based, platform-appropriate)
 *    - Linux/macOS: ~/.local/share/rstudio/pai/bin
 *    - Windows: %LOCALAPPDATA%/rstudio/pai/bin
 * 2. System-wide installation, as given by systemPositAssistantInstallPath()
 * 3. The copy bundled with RStudio, as given by
 *    bundledPositAssistantInstallPath() -- skipped entirely when
 *    posit-assistant-path is set, so a pinned path that holds no
 *    installation reports "not installed" rather than downgrading to the
 *    shipped version
 *
 * @param paths The tiers to search
 * @return FilePath to the installation directory, or empty FilePath if not found
 */
core::FilePath locatePositAssistantInstallation(const InstallSearchPaths& paths);

/**
 * Locate the Posit Assistant installation directory for this session.
 *
 * @return FilePath to the installation directory, or empty FilePath if not found
 */
core::FilePath locatePositAssistantInstallation();

/**
 * Get the installed version of Posit Assistant from package.json.
 *
 * @param positAiPath Path to the AI installation directory
 * @return Version string (e.g., "1.2.3"), or empty string if not found or invalid
 */
std::string getInstalledVersion(const core::FilePath& positAiPath);

/**
 * Get the installed version of Posit Assistant this session would run.
 *
 * @return Version string (e.g., "1.2.3"), or empty string if not found or invalid
 */
std::string getInstalledVersion();

/**
 * Get the protocol version the given Posit Assistant package was built for.
 *
 * Reads the protocol.json file written at install time. Legacy installs
 * (before this file existed) return an empty string.
 *
 * @param positAiPath Path to the AI installation directory
 * @return Protocol version string (e.g., "10.0"), or empty string if missing or unreadable
 */
std::string getInstalledProtocolVersion(const core::FilePath& positAiPath);

/**
 * Get the protocol version the Posit Assistant package this session would
 * run was built for.
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
