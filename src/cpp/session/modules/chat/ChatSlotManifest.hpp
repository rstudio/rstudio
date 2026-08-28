/*
 * ChatSlotManifest.hpp
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

#ifndef SESSION_CHAT_SLOT_MANIFEST_HPP
#define SESSION_CHAT_SLOT_MANIFEST_HPP

#include <cstdint>
#include <map>
#include <string>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace slot_manifest {

/**
 * What the install recorded about one file in a slot.
 */
struct ManifestEntry
{
   ManifestEntry() : size(0) {}
   ManifestEntry(std::uintmax_t in_size, const std::string& in_sha256)
      : size(in_size), sha256(in_sha256)
   {
   }

   std::uintmax_t size;

   // Recorded for future use (e.g. an explicit deep integrity check); the
   // routine verification that runs at every resolve compares sizes only.
   std::string sha256;
};

// Relative path (always '/'-separated) to what was recorded for it.
typedef std::map<std::string, ManifestEntry> SlotManifest;

/**
 * Record every regular file in a slot, then write the record into the slot.
 *
 * Called after extraction, while the tree is still staged and known to match
 * the verified package, so the manifest is a statement about bits that were
 * checked once. Directories, symlinks that do not resolve to a regular file,
 * and the manifest file itself are not recorded.
 *
 * @param slotDir Directory holding the extracted package.
 * @return Success, or an error if the tree could not be walked, a file could
 *         not be hashed, or the manifest could not be written.
 */
core::Error writeSlotManifest(const core::FilePath& slotDir);

/**
 * Read the manifest a slot was installed with.
 *
 * @param slotDir Directory holding the extracted package.
 * @param pManifest Output: the recorded entries (set only on success).
 * @return Success, or an error if the manifest is absent, unreadable, or
 *         malformed.
 */
core::Error readSlotManifest(const core::FilePath& slotDir,
                             SlotManifest* pManifest);

/**
 * Check a slot against the manifest it was installed with.
 *
 * Every recorded file must exist and still have its recorded size. This is a
 * stat walk -- no file contents are read -- so it is cheap enough to run at
 * every resolve, including on NFS home directories. Files present in the slot
 * but absent from the manifest are ignored.
 *
 * @param slotDir Directory holding the extracted package.
 * @return true if the manifest is readable and every entry still matches.
 */
bool matchesSlotManifest(const core::FilePath& slotDir);

} // namespace slot_manifest
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_SLOT_MANIFEST_HPP
