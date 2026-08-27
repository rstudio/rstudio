/*
 * ChatSlots.hpp
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

#ifndef SESSION_CHAT_SLOTS_HPP
#define SESSION_CHAT_SLOTS_HPP

#include <string>
#include <vector>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace slots {

// Posit Assistant is installed side by side: each installed package version
// gets its own directory ("slot") under <storageDir>/versions, created once
// and never modified afterwards. Slot names are human-readable but carry no
// meaning -- a slot's version and protocol always come from the package.json
// and protocol.json inside it.

/**
 * What a slot says about itself.
 */
struct SlotInfo
{
   // The slot directory.
   core::FilePath path;

   // The slot's directory name, as recorded in the selector.
   std::string name;

   // "version" from the slot's package.json.
   std::string version;

   // "protocol" from the slot's protocol.json.
   std::string protocol;
};

/**
 * The directory holding all slots for a Posit Assistant storage directory.
 *
 * @param storageDir The Posit Assistant storage directory (<userDataDir>/pai).
 * @return The versions directory. Not guaranteed to exist.
 */
core::FilePath versionsDir(const core::FilePath& storageDir);

/**
 * Check that a slot is a complete, coherent Posit Assistant installation.
 *
 * A slot verifies when its expected files exist and are non-empty, its
 * package.json parses and declares a version, its protocol.json parses and
 * declares a protocol, and every file recorded in its install-time manifest
 * still exists at its recorded size. A slot with no manifest does not verify:
 * every slot this module creates gets one, so its absence means the directory
 * was not produced by an install that ran to completion.
 *
 * The manifest check is a stat walk, so this is cheap enough to run on every
 * resolve, including on NFS home directories. It cannot detect corruption that
 * preserves file sizes -- that is what a forced reinstall is for.
 *
 * @param slotDir The slot directory.
 * @param pInfo Output: what the slot says about itself (optional, may be
 *              nullptr; populated only when the slot verifies).
 * @return true if the slot verifies.
 */
bool verifySlot(const core::FilePath& slotDir, SlotInfo* pInfo = nullptr);

/**
 * Every slot under `slotsDir` that verifies, in unspecified order.
 *
 * Staging directories and other hidden entries are skipped. A versions
 * directory that does not exist yields no slots rather than an error.
 *
 * @param slotsDir The directory holding the slots.
 * @return The verifying slots.
 */
std::vector<SlotInfo> verifiedSlots(const core::FilePath& slotsDir);

/**
 * Create an empty staging directory to extract a package into.
 *
 * The staging directory is a sibling of the slots, so publishing it with
 * allocateSlot() is a rename within one filesystem rather than a copy. It is
 * named for the current process and cleared on every call, so a crashed
 * session's leftovers are reclaimed rather than accumulating.
 *
 * @param slotsDir The directory holding the slots.
 * @param pStagingDir Output: the empty staging directory.
 * @return Success, or an error if the directory could not be cleared or
 *         created.
 */
core::Error prepareStagingDir(const core::FilePath& slotsDir,
                              core::FilePath* pStagingDir);

/**
 * How allocateSlot() should respond to finding the name it wants taken.
 */
enum class SlotPolicy
{
   // Take the existing slot if it verifies. An install whose version is
   // already on disk and intact has nothing to add.
   AdoptExisting,

   // Never take an existing slot; allocate a new name instead. A reinstall
   // exists to replace bits that verification cannot fault, so it has to
   // produce a directory it wrote itself.
   AlwaysFresh
};

/**
 * Publish a staged package as a slot, arbitrating with concurrent installers.
 *
 * The staged directory is renamed to `<version>`, or to `<version>-2`,
 * `<version>-3`, ... when that name is taken. Because the rename is the only
 * arbiter, two sessions installing the same version at once cannot corrupt
 * each other: the loser sees the winner's slot and either adopts it or takes
 * the next name.
 *
 * On adoption the staged directory is removed, since nothing else can be
 * using a directory named for this process.
 *
 * @param stagingDir The staged package, as returned by prepareStagingDir().
 *                   Must be a child of the versions directory.
 * @param version The installed version, used to name the slot.
 * @param policy Whether an existing verifying slot may be adopted.
 * @param pSlotDir Output: the published (or adopted) slot directory.
 * @return Success, or an error if the version cannot name a directory, the
 *         staging directory is missing, or no name could be taken.
 */
core::Error allocateSlot(const core::FilePath& stagingDir,
                         const std::string& version,
                         SlotPolicy policy,
                         core::FilePath* pSlotDir);

} // namespace slots
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_SLOTS_HPP
