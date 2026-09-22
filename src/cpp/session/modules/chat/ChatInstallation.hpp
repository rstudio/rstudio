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

#include <boost/optional.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace installation {

// ============================================================================
// Installation directories
// ============================================================================
//
// What any directory holding an extracted Posit Assistant package looks like,
// independent of how it got there. The slot machinery in ChatSlots builds its
// stronger, manifest-backed verification on top of these, so there is one
// definition of "could be run" and one reader for each identity file.

/**
 * Check that a directory holds a package the backend could be launched from.
 *
 * The client directory exists, and the server script and index.html exist and
 * are non-empty. Non-empty matters: a truncated extraction used to leave a
 * zero-byte main.js that an existence-only check accepted.
 *
 * This is a structural check only. It says nothing about which version or
 * protocol the directory holds, and nothing about whether the tree is intact
 * beyond those three paths -- slots::verifySlot() adds both. It is what the
 * unversioned sources (posit-assistant-path, the administrator's legacy
 * directory, the bundled copy) get, since they carry no manifest.
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
 *         install that declares nothing predates the file; callers treat it
 *         as a protocol mismatch rather than an error.
 */
std::string declaredProtocol(const core::FilePath& installDir);

/**
 * Checks that an extracted package declares the version and protocol it was
 * downloaded as. A package that declares neither file, or other values, is a
 * mis-published one: installing it would select a slot for a protocol another
 * RStudio release resolves, or report a version this session does not run.
 *
 * @param installDir The directory holding the extracted package.
 * @param expectedVersion The version the manifest entry promised.
 * @param expectedProtocol The protocol the manifest entry was chosen for.
 * @return Success, or an invalid_argument error naming both identities.
 */
core::Error verifyDeclaredIdentity(const core::FilePath& installDir,
                                   const std::string& expectedVersion,
                                   const std::string& expectedProtocol);

/**
 * The Posit Assistant storage directory for this user.
 *
 * Shared by every install: it holds the version slots under versions/, the
 * selector naming the active slot per protocol, the record of update checks,
 * and the backend's own configuration. Passed to the backend as --storage.
 *
 * @return <userDataDir>/pai. Not guaranteed to exist.
 */
core::FilePath positAiStorageDir();

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

// ============================================================================
// Resolution
// ============================================================================

/**
 * The sources locatePositAssistantInstallation() resolves from, plus the
 * settings that decide which of them apply. Resolved from session options and
 * the environment by positAssistantSearchPaths(); passed explicitly so tests
 * can drive the resolution without ambient state.
 */
struct InstallSearchPaths
{
   InstallSearchPaths() : userInstallEnabled(true) {}

   // <userDataDir>/pai: the version slots RStudio installs into and the
   // selector naming the active one per protocol. The only source RStudio
   // writes. Its legacy, unversioned bin/ is never read.
   core::FilePath userStorageDir;

   // <systemConfigDir>/pai: the administrator's version slots and selector,
   // in the same layout, which RStudio reads but never writes -- and beneath
   // it the legacy, unversioned bin/ that deployments have today.
   core::FilePath systemStorageDir;

   // posit-assistant-path when set: a single unversioned installation
   // directory, the administrator's explicit choice. Empty when unset.
   core::FilePath pinnedPath;

   // the copy shipped with RStudio; only commercial builds ship one
   core::FilePath bundledPath;

   // the administrator allows users to manage their own installation
   bool userInstallEnabled;
};

/**
 * Resolve the sources for this session.
 *
 * @return InstallSearchPaths for locatePositAssistantInstallation()
 */
InstallSearchPaths positAssistantSearchPaths();

/**
 * Resolve the Posit Assistant installation directory among the given sources.
 *
 * When posit-assistant-path is set (pinnedPath), that path is the
 * administrator's explicit choice: it is used when it holds an installation,
 * and otherwise ends the search for read-only copies -- the bundled copy is
 * not consulted, so a pinned path that holds no installation does not
 * silently downgrade to the shipped version.
 *
 * Otherwise each source contributes at most one candidate:
 * 1. The user's slots (userStorageDir/versions): the slot selected.json names
 *    for this build's protocol, or the newest verifying slot for it, which is
 *    then recorded. Because the selector is keyed by protocol this source
 *    never contributes an incompatible slot. Skipped entirely when
 *    userInstallEnabled is false, so an installation left there before the
 *    administrator disabled user-managed installs -- or copied there to get
 *    around the setting -- is ignored.
 * 2. The administrator's slots (systemStorageDir/versions), resolved the same
 *    way through the administrator's own selected.json, which is never
 *    rewritten. This is how an administrator chooses which installed version
 *    a deployment runs, including holding users on an older slot beside a
 *    newer one.
 * 3. The administrator's legacy directory (systemStorageDir/bin), as a single
 *    unversioned installation.
 * 4. The copy bundled with RStudio, as given by
 *    bundledPositAssistantInstallPath().
 *
 * The candidates race: one whose protocol.json matches this build ranks above
 * one that does not (a missing file counts as a mismatch), then by the
 * version in package.json (missing or unparsable ranks lowest). Equal
 * candidates keep the order above.
 *
 * @param paths The sources to resolve from
 * @return FilePath to the installation directory, or empty FilePath if not found
 */
core::FilePath locatePositAssistantInstallation(const InstallSearchPaths& paths);

/**
 * The Posit Assistant installation this session runs.
 *
 * Resolved from positAssistantSearchPaths() once, on first need, and then
 * held: the backend and agent starts, the asset handler and its
 * Content-Security-Policy, and every report of the installed version and
 * protocol read this one answer, so they cannot drift onto different
 * installations. An install made elsewhere -- by another session, or by an
 * administrator -- takes effect at this session's next start; the one thing
 * that discards the held answer is this session's own successful install
 * (clearPinnedInstallation()), so the components it restarts come back on the
 * new slot.
 *
 * The held path is re-checked with verifyInstallDir() on each read, so an
 * installation removed out of band is resolved around rather than served from.
 * A resolution that found nothing is not held: while nothing is installed
 * there is nothing to keep consistent, and the next read sees an install as
 * soon as it lands.
 *
 * Safe to call from any thread; the asset handler runs on HTTP threads.
 *
 * @return FilePath to the installation directory, or empty FilePath if not found
 */
core::FilePath locatePositAssistantInstallation();

/**
 * Discard the held resolution so the next read resolves again.
 *
 * Called once this session's install has published a slot and selected it.
 */
void clearPinnedInstallation();

/**
 * Whether an installation of the given version, written by this session to
 * its own slots, would then be the one locatePositAssistantInstallation()
 * resolves.
 *
 * The update check offers the manifest version only when installing it
 * would change what runs: a copy that is read-only to the user (the
 * administrator's or the bundled one) and ranks above the offered version
 * would keep winning, and the offer could never be satisfied. The existing
 * user slot is what the install replaces as the selection, so it never
 * competes.
 *
 * @param paths The sources to resolve from
 * @param version The package version the manifest offers
 * @return true if the user's slots would be selected after the install
 */
bool userInstallWouldBeSelected(const InstallSearchPaths& paths, const std::string& version);

/**
 * Whether an installation of the given version in this session's own slots
 * would be the one this session runs.
 *
 * @param version The package version the manifest offers
 * @return true if the user's slots would be selected after the install
 */
bool userInstallWouldBeSelected(const std::string& version);

/**
 * Get the installed version of Posit Assistant from package.json.
 *
 * Reports on the held resolution, so it agrees with what this session runs
 * rather than with whatever the newest install on disk happens to be.
 *
 * @return Version string (e.g., "1.2.3"), or empty string if not found or invalid
 */
std::string getInstalledVersion();

/**
 * Get the protocol version the Posit Assistant package this session runs was
 * built for.
 *
 * Reports on the held resolution. Legacy installs (before protocol.json
 * existed) return an empty string.
 *
 * @return Protocol version string (e.g., "10.0"), or empty string if missing or unreadable
 */
std::string getInstalledProtocolVersion();

/**
 * Resolve from the given sources instead of this session's.
 *
 * A test seam: it is what lets the held resolution, and the asset handler
 * reading it, be exercised against staged directories. Discards the held
 * resolution. Passing boost::none restores positAssistantSearchPaths().
 *
 * @param paths The sources to resolve from, or boost::none for the session's
 */
void setSearchPathsForTesting(const boost::optional<InstallSearchPaths>& paths);

} // namespace installation
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_INSTALLATION_HPP
