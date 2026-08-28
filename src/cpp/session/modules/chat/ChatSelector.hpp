/*
 * ChatSelector.hpp
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

#ifndef SESSION_CHAT_SELECTOR_HPP
#define SESSION_CHAT_SELECTOR_HPP

#include <map>
#include <string>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace selector {

// selected.json records which install slot is active for each Posit Assistant
// protocol version, so RStudio releases built against different protocols can
// share one home directory without competing for a single install:
//
//     {"selected": {"11.0": "1.1.0-2", "10.0": "0.4.8"}}
//
// It is advisory. Resolution falls back to the newest matching slot on disk
// whenever the file disagrees with reality, so the file can be lost or damaged
// without stranding a session. Note what that recovery is and is not: it
// converges on the newest verifying slot for the protocol, not on whatever the
// lost entry named. While a selection only ever records the version just
// installed that is the same answer, but a selection made to hold a session on
// an older slot would not survive.

// Protocol version to slot directory name.
typedef std::map<std::string, std::string> Selections;

/**
 * Read the recorded selections.
 *
 * A missing, unreadable, or malformed file reads as no selections rather than
 * an error: every selection is recoverable from the slots themselves, so there
 * is nothing for a caller to do differently.
 *
 * @param storageDir The Posit Assistant storage directory (<userDataDir>/pai).
 * @return The recorded selections, ignoring any entry that is not a string.
 */
Selections readSelections(const core::FilePath& storageDir);

/**
 * Replace the recorded selections.
 *
 * Written to a temporary file and renamed into place, so a reader never sees a
 * partial file. That does not serialize the read-modify-write in selectSlot():
 * two sessions recording different protocols at once can drop one of the
 * entries, and the next resolveSlot() replaces the dropped one with the newest
 * matching slot rather than restoring what it said.
 *
 * @param storageDir The Posit Assistant storage directory.
 * @param selections The selections to record.
 * @return Success, or an error if the file could not be written.
 */
core::Error writeSelections(const core::FilePath& storageDir,
                            const Selections& selections);

/**
 * Record the active slot for one protocol, leaving other protocols alone.
 *
 * @param storageDir The Posit Assistant storage directory.
 * @param protocol The protocol version the slot serves (e.g. "11.0").
 * @param slotName The slot's directory name (e.g. "1.1.0-2").
 * @return Success, or an error if the file could not be written.
 */
core::Error selectSlot(const core::FilePath& storageDir,
                       const std::string& protocol,
                       const std::string& slotName);

/**
 * Record an existing slot that already holds `version` as the active one.
 *
 * An install whose target version is already on disk in a verifying slot has
 * nothing to download; it is a selector update. Any matching slot will do --
 * two slots holding one version hold the same package -- so the first one
 * found is taken. A forced reinstall exists precisely to replace bits that
 * verification cannot fault, so it must not use this.
 *
 * @param storageDir The Posit Assistant storage directory.
 * @param protocol The protocol version the slot must serve (e.g. "11.0").
 * @param version The package version the slot must hold (e.g. "1.1.0").
 * @return true when a slot was found and recorded. false when no slot holds
 *         that version for that protocol, or the selector could not be
 *         written.
 */
bool selectInstalledVersion(const core::FilePath& storageDir,
                            const std::string& protocol,
                            const std::string& version);

/**
 * Find the slot to run for a protocol.
 *
 * Prefers the recorded selection. When that slot is missing, fails
 * verification, or turns out to serve a different protocol, falls back to the
 * newest-versioned verifying slot for the protocol and records it, so a
 * dropped or damaged selector heals itself. Ties on version are broken by slot
 * name, descending -- arbitrary, but stable across sessions.
 *
 * Repair is best effort: a slot is still returned when the storage directory
 * cannot be written.
 *
 * @param storageDir The Posit Assistant storage directory.
 * @param protocol The protocol version to resolve for (e.g. "11.0").
 * @return The slot directory, or an empty FilePath when no slot serves the
 *         protocol.
 */
core::FilePath resolveSlot(const core::FilePath& storageDir,
                           const std::string& protocol);

} // namespace selector
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_SELECTOR_HPP
