/*
 * ChatSlotManifest.cpp
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

#include "ChatSlotManifest.hpp"

#include "ChatConstants.hpp"
#include "ChatLogging.hpp"

#include <core/FileSerializer.hpp>
#include <core/system/Crypto.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::constants;
using namespace rstudio::session::modules::chat::logging;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace slot_manifest {

namespace {

// Manifest JSON keys.
const char* const kFilesKey = "files";
const char* const kSizeKey = "size";
const char* const kSha256Key = "sha256";

FilePath manifestPath(const FilePath& slotDir)
{
   return slotDir.completeChildPath(kSlotManifestFileName);
}

Error hashFile(const FilePath& filePath, std::string* pSha256)
{
   std::string content;
   Error error = readStringFromFile(filePath, &content);
   if (error)
      return error;

   return core::system::crypto::sha256Hex(content, pSha256);
}

// Collects size and hash for every regular file under `slotDir`, keyed by
// '/'-separated path relative to the slot.
Error collectEntries(const FilePath& slotDir, SlotManifest* pManifest)
{
   Error hashError;

   Error error = slotDir.getChildrenRecursive(
      [&](int, const FilePath& child) -> bool
      {
         if (!child.isRegularFile())
            return true;

         std::string relativePath = child.getRelativePath(slotDir);
         if (relativePath.empty() || relativePath == kSlotManifestFileName)
            return true;

         std::string sha256;
         hashError = hashFile(child, &sha256);
         if (hashError)
            return false;

         (*pManifest)[relativePath] = ManifestEntry(child.getSize(), sha256);
         return true;
      });

   if (error)
      return error;

   return hashError;
}

} // anonymous namespace

Error writeSlotManifest(const FilePath& slotDir)
{
   SlotManifest manifest;
   Error error = collectEntries(slotDir, &manifest);
   if (error)
      return error;

   json::Object files;
   for (const auto& entry : manifest)
   {
      json::Object record;
      record[kSizeKey] = static_cast<uint64_t>(entry.second.size);
      record[kSha256Key] = entry.second.sha256;
      files[entry.first] = record;
   }

   json::Object root;
   root[kFilesKey] = files;

   DLOG("Recording {} files in slot manifest for {}",
        manifest.size(), slotDir.getAbsolutePath());

   return writeStringToFile(manifestPath(slotDir), root.write());
}

Error readSlotManifest(const FilePath& slotDir, SlotManifest* pManifest)
{
   FilePath path = manifestPath(slotDir);

   std::string content;
   Error error = readStringFromFile(path, &content);
   if (error)
      return error;

   json::Value value;
   if (value.parse(content) || !value.isObject())
   {
      return systemError(boost::system::errc::bad_message,
                         "Slot manifest is not a JSON object",
                         ERROR_LOCATION);
   }

   json::Object files;
   error = json::readObject(value.getObject(), kFilesKey, files);
   if (error)
      return error;

   SlotManifest manifest;
   for (const auto& member : files)
   {
      if (!member.getValue().isObject())
      {
         return systemError(boost::system::errc::bad_message,
                            "Slot manifest entry '" + member.getName() +
                               "' is not a JSON object",
                            ERROR_LOCATION);
      }

      json::Object record = member.getValue().getObject();
      if (!record.hasMember(kSizeKey) || !record[kSizeKey].isUInt64())
      {
         return systemError(boost::system::errc::bad_message,
                            "Slot manifest entry '" + member.getName() +
                               "' has no valid size",
                            ERROR_LOCATION);
      }

      std::string sha256;
      if (record.hasMember(kSha256Key) && record[kSha256Key].isString())
         sha256 = record[kSha256Key].getString();

      manifest[member.getName()] =
         ManifestEntry(record[kSizeKey].getUInt64(), sha256);
   }

   *pManifest = manifest;
   return Success();
}

bool matchesSlotManifest(const FilePath& slotDir)
{
   SlotManifest manifest;
   Error error = readSlotManifest(slotDir, &manifest);
   if (error)
   {
      WLOG("Cannot read slot manifest for {}: {}",
           slotDir.getAbsolutePath(), error.getMessage());
      return false;
   }

   for (const auto& entry : manifest)
   {
      FilePath filePath;
      error = slotDir.completeChildPath(entry.first, filePath);
      if (error)
      {
         WLOG("Slot manifest for {} names a path outside the slot: {}",
              slotDir.getAbsolutePath(), entry.first);
         return false;
      }

      if (!filePath.isRegularFile())
      {
         WLOG("Slot {} is missing manifest file {}",
              slotDir.getAbsolutePath(), entry.first);
         return false;
      }

      if (filePath.getSize() != entry.second.size)
      {
         WLOG("Slot {} file {} is {} bytes, manifest recorded {}",
              slotDir.getAbsolutePath(), entry.first,
              filePath.getSize(), entry.second.size);
         return false;
      }
   }

   return true;
}

} // namespace slot_manifest
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
