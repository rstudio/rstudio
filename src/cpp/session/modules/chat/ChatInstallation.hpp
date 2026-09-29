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
// verification on top of these, so there is one definition of "could be run"
// and one reader for each identity file.

/**
 * Check that a directory holds a package the backend could be launched from.
 *
 * The client directory exists, and the server script and index.html exist and
 * are non-empty. Non-empty matters: a truncated extraction used to leave a
 * zero-byte main.js that an existence-only check accepted.
 *
 * This is a structural check only. It says nothing about which version or
 * protocol the directory holds -- slots::verifySlot() adds that -- and nothing
 * about whether the tree is intact beyond those three paths. It is all the
 * bundled copy gets, since it is not a slot.
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
 * selector naming the active slot per protocol, and the record of update
 * checks. The assistant's own storage is under ~/.posit/assistant, not here.
 *
 * @return <userDataDir>/pai. Not guaranteed to exist.
 */
core::FilePath positAiStorageDir();

/**
 * The Posit Assistant storage directory RStudio provides for all users.
 *
 * The directory is itself the copy shipped with RStudio, whose files the
 * package replaces on each upgrade. Administrator-installed versions sit
 * inside it in the layout of the user's storage directory: slots under
 * versions/, and a selected.json naming the slot to run per protocol. The
 * package owns neither, so an upgrade leaves them in place. RStudio reads the
 * directory and never writes it; the administrator's tooling owns versions/
 * and the selector.
 *
 * Installed beside the session binary, except in the macOS app bundle where
 * it sits next to bin/ rather than inside it. The bin location is returned
 * unless it holds nothing this build could run -- no bundled copy and no
 * administrator's slot for this build's protocol -- and the other location
 * does, so a partial directory at either one does not mask a usable one at
 * the other. When neither holds anything the bin location is returned, as
 * where the directory is expected. Only Workbench ships a bundled copy.
 *
 * @param resourcePath Root to resolve against (the session resource path)
 * @return FilePath to the system storage directory
 */
core::FilePath systemStorageDir(const core::FilePath& resourcePath);

/**
 * The Posit Assistant storage directory RStudio provides for all users,
 * resolved against this session's resource path.
 *
 * @return FilePath to the system storage directory
 */
core::FilePath systemStorageDir();

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

   // systemStorageDir(): itself the copy shipped with RStudio, holding the
   // administrator's slots under versions/ selected by its own selected.json.
   // Read, never written.
   core::FilePath systemStorageDir;

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
 * Candidates race: one whose protocol.json matches this build ranks above one
 * that does not (a missing file counts as a mismatch), then by the version in
 * package.json (missing or unparsable ranks lowest). Equal candidates keep
 * the order they are listed in below.
 *
 * 1. The user's slots (userStorageDir/versions): the slot selected.json names
 *    for this build's protocol, or the newest verifying slot for it, which is
 *    then recorded. Skipped entirely when userInstallEnabled is false, so an
 *    installation left there before the administrator disabled user-managed
 *    installs -- or copied there to get around the setting -- is ignored.
 * 2. The administrator's slots (systemStorageDir/versions): the slot
 *    systemStorageDir/selected.json names for this build's protocol, or the
 *    newest verifying slot for it. The selector is the administrator's, so it
 *    is never rewritten. It chooses among the administrator's versions only:
 *    a selected slot older than the bundled copy or the user's slot loses to
 *    them, so a selection left by an earlier upgrade cannot hold back a newer
 *    RStudio release.
 * 3. The copy bundled with RStudio (systemStorageDir itself).
 *
 * Both selectors are keyed by protocol, so neither slot source contributes an
 * incompatible slot; only the bundled copy can be one.
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
 * Whether the installation this session runs is one of the user's own slots,
 * rather than a copy the administrator or RStudio provides.
 *
 * Reinstall is offered only then: it replaces bits the user installed, and a
 * fresh user slot beside a read-only copy of the same version would shadow
 * that copy rather than repair it.
 *
 * @return true if the held resolution is a slot under the user's versions/
 */
bool runsUserSlot();

/**
 * Whether an installation of the given version, written by this session to
 * its own slots, would then be the one locatePositAssistantInstallation()
 * resolves.
 *
 * The update check offers the manifest version only when installing it
 * would change what runs: an administrator's slot or a bundled copy that
 * ranks above the offered version would keep winning, and the offer could
 * never be satisfied. The existing user slot is what the install replaces as
 * the selection, so it never competes.
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
