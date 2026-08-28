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
// Installation Primitives
// ============================================================================
//
// What any directory holding an extracted Posit Assistant package looks like,
// independent of how it got there. The slot machinery in ChatSlots builds its
// stronger, manifest-backed checks on top of these.

/**
 * The Posit Assistant storage directory for this user.
 *
 * Shared by every install: it holds the version slots, the selector naming
 * the active slot per protocol, and the backend's own state. Passed to the
 * backend as --storage.
 *
 * @return <userDataDir>/pai. Not guaranteed to exist.
 */
core::FilePath positAiStorageDir();

/**
 * Check that a directory holds a package the backend could be launched from.
 *
 * The client directory exists and the server script and index.html exist and
 * are non-empty. Non-empty matters: a truncated extraction used to leave a
 * zero-byte main.js that an existence-only check accepted.
 *
 * This is a structural check only. It says nothing about which version or
 * protocol the directory holds, and nothing about whether the tree is intact
 * beyond those three paths -- slots::verifySlot() adds both.
 *
 * @param installDir The directory holding an extracted package.
 * @return true if the directory could be run.
 */
bool verifyInstallDir(const core::FilePath& installDir);

/**
 * The version an installation directory declares, from its package.json.
 *
 * @param installDir The directory holding an extracted package.
 * @return The version, or an empty string when package.json is absent,
 *         unparseable, or declares no non-empty "version" string.
 */
std::string declaredVersion(const core::FilePath& installDir);

/**
 * The protocol an installation directory declares, from its protocol.json.
 *
 * @param installDir The directory holding an extracted package.
 * @return The protocol, or an empty string when protocol.json is absent,
 *         unparseable, or declares no non-empty "protocol" string. An
 *         install that declares nothing is the legacy case that predates the
 *         file; callers treat it as a mismatch rather than an error.
 */
std::string declaredProtocol(const core::FilePath& installDir);

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

// ============================================================================
// Resolution
// ============================================================================

/**
 * The Posit Assistant installation this session runs.
 *
 * Resolved from an ordered list of independent sources, each tried in turn:
 *
 *   1. RSTUDIO_POSIT_AI_PATH -- the development override.
 *   2. <userDataDir>/pai/versions -- the per-user version slots, chosen by
 *      protocol through selected.json. The only writable source.
 *   3. <systemConfigDir>/pai/bin -- the admin-managed install: read-only, and
 *      deliberately unversioned.
 *
 * Sources 1 and 3 are plain directories rather than slots. They carry no
 * install manifest and are not named for a version, so they get
 * verifyInstallDir() and nothing more; what they declare is reported
 * downstream (and drives the update check) rather than gating them here. An
 * override or a system install is taken on the authority of whoever put it
 * there.
 *
 * The answer is resolved on the first call and held for the process lifetime.
 * A session that has resolved keeps running the same directory even after
 * another session installs a newer version, so an install elsewhere never
 * changes what is executing here. Call clearPinnedInstallation() when this
 * session's own install has published a slot.
 *
 * Main thread only: it reads the filesystem and holds unsynchronized state.
 *
 * @return The installation directory, or an empty FilePath when no source
 *         yielded one.
 */
core::FilePath locatePositAssistantInstallation();

/**
 * Discard the pinned resolution so the next request resolves again.
 *
 * Called once this session's install has published a slot and selected it,
 * so the components it restarts come back on the new version.
 */
void clearPinnedInstallation();

/**
 * Get the installed version of Posit Assistant from package.json.
 *
 * Reports on the pinned installation, so it agrees with what this session
 * runs rather than with whatever the newest install on disk happens to be.
 *
 * @return Version string (e.g., "1.2.3"), or empty string if not found or invalid
 */
std::string getInstalledVersion();

/**
 * Get the protocol version the installed Posit Assistant package was built for.
 *
 * Reports on the pinned installation. Legacy installs (before protocol.json
 * existed) return an empty string.
 *
 * @return Protocol version string (e.g., "10.0"), or empty string if missing or unreadable
 */
std::string getInstalledProtocolVersion();

} // namespace installation
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_INSTALLATION_HPP
