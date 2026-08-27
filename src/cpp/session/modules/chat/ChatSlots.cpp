/*
 * ChatSlots.cpp
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

#include "ChatSlots.hpp"

#include "ChatConstants.hpp"
#include "ChatLogging.hpp"
#include "ChatSlotManifest.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::constants;
using namespace rstudio::session::modules::chat::logging;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace slots {

namespace {

// Slot names past the first for one version run 1.1.0-2, 1.1.0-3, ...; the
// bound only exists so a filesystem that keeps producing collisions fails
// instead of spinning.
const int kMaxAllocationAttempts = 1000;

// Reads a single string field out of a JSON file in a slot. Returns an empty
// string when the file is absent, unparseable, or the field is missing, empty
// or not a string -- callers treat all of those the same way.
std::string readJsonStringField(const FilePath& slotDir,
                                const char* fileName,
                                const char* fieldName)
{
   FilePath filePath = slotDir.completeChildPath(fileName);
   if (!filePath.isRegularFile())
      return std::string();

   std::string content;
   Error error = readStringFromFile(filePath, &content);
   if (error)
      return std::string();

   json::Value value;
   if (value.parse(content) || !value.isObject())
      return std::string();

   json::Object object = value.getObject();
   if (!object.hasMember(fieldName) || !object[fieldName].isString())
      return std::string();

   return object[fieldName].getString();
}

bool existsAndNonEmpty(const FilePath& filePath)
{
   return filePath.isRegularFile() && filePath.getSize() > 0;
}

// The files a Posit Assistant install cannot run without. Existence alone is
// not enough: a truncated download used to leave a zero-byte main.js that the
// old check accepted.
bool hasRequiredFiles(const FilePath& slotDir)
{
   if (!slotDir.completeChildPath(kClientDirPath).isDirectory())
      return false;

   return existsAndNonEmpty(slotDir.completeChildPath(kServerScriptPath)) &&
          existsAndNonEmpty(
             slotDir.completeChildPath(kClientDirPath)
                .completeChildPath(kIndexFileName));
}

// Rejects versions that cannot safely name a directory. A version reaches here
// from a downloaded manifest, so it is not trusted to be a bare version
// string.
bool isUsableSlotName(const std::string& version)
{
   if (version.empty() || version.front() == '.' || version.front() == '-')
      return false;

   return version.find_first_of("/\\:") == std::string::npos;
}

std::string slotNameForOrdinal(const std::string& version, int ordinal)
{
   if (ordinal <= 1)
      return version;

   return version + "-" + safe_convert::numberToString(ordinal);
}

} // anonymous namespace

FilePath versionsDir(const FilePath& storageDir)
{
   return storageDir.completeChildPath(kVersionsDirName);
}

bool verifySlot(const FilePath& slotDir, SlotInfo* pInfo)
{
   if (!slotDir.isDirectory())
      return false;

   if (!hasRequiredFiles(slotDir))
   {
      DLOG("Slot {} is missing required files", slotDir.getAbsolutePath());
      return false;
   }

   std::string version =
      readJsonStringField(slotDir, kPackageJsonFileName, "version");
   if (version.empty())
   {
      DLOG("Slot {} declares no package version", slotDir.getAbsolutePath());
      return false;
   }

   std::string protocol =
      readJsonStringField(slotDir, kProtocolVersionFileName, "protocol");
   if (protocol.empty())
   {
      DLOG("Slot {} declares no protocol version", slotDir.getAbsolutePath());
      return false;
   }

   if (!slot_manifest::matchesSlotManifest(slotDir))
      return false;

   if (pInfo != nullptr)
   {
      pInfo->path = slotDir;
      pInfo->name = slotDir.getFilename();
      pInfo->version = version;
      pInfo->protocol = protocol;
   }

   return true;
}

std::vector<SlotInfo> verifiedSlots(const FilePath& slotsDir)
{
   std::vector<SlotInfo> found;
   if (!slotsDir.isDirectory())
      return found;

   std::vector<FilePath> children;
   Error error = slotsDir.getChildren(children);
   if (error)
   {
      WLOG("Cannot list slots in {}: {}",
           slotsDir.getAbsolutePath(), error.getMessage());
      return found;
   }

   for (const FilePath& child : children)
   {
      // Staging directories and any other bookkeeping are dot-prefixed; a slot
      // never is, because a version cannot start with a dot.
      if (boost::algorithm::starts_with(child.getFilename(), "."))
         continue;

      SlotInfo info;
      if (verifySlot(child, &info))
         found.push_back(info);
   }

   return found;
}

Error prepareStagingDir(const FilePath& slotsDir, FilePath* pStagingDir)
{
   std::string name = std::string(kStagingDirPrefix) +
      safe_convert::numberToString(
         static_cast<int64_t>(core::system::currentProcessId()));

   FilePath stagingDir = slotsDir.completeChildPath(name);

   // Clear rather than allocate a new name: a session that crashed mid-install
   // left its staging directory behind, and this is the only point at which
   // reclaiming it is provably safe -- no other process uses our pid.
   Error error = stagingDir.removeIfExists();
   if (error)
      return error;

   error = stagingDir.ensureDirectory();
   if (error)
      return error;

   *pStagingDir = stagingDir;
   return Success();
}

Error allocateSlot(const FilePath& stagingDir,
                   const std::string& version,
                   SlotPolicy policy,
                   FilePath* pSlotDir)
{
   if (!isUsableSlotName(version))
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Cannot name an install slot for version '" +
                            version + "'",
                         ERROR_LOCATION);
   }

   if (!stagingDir.isDirectory())
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         "Staged install directory " +
                            stagingDir.getAbsolutePath() + " does not exist",
                         ERROR_LOCATION);
   }

   FilePath slotsDir = stagingDir.getParent();

   int ordinal = 1;
   for (int attempt = 0; attempt < kMaxAllocationAttempts; ++attempt)
   {
      FilePath candidate =
         slotsDir.completeChildPath(slotNameForOrdinal(version, ordinal));

      if (candidate.exists())
      {
         if (policy == SlotPolicy::AdoptExisting && verifySlot(candidate))
         {
            DLOG("Adopting existing slot {}", candidate.getAbsolutePath());
            Error error = stagingDir.removeIfExists();
            if (error)
            {
               WLOG("Could not remove staging directory {}: {}",
                    stagingDir.getAbsolutePath(), error.getMessage());
            }

            *pSlotDir = candidate;
            return Success();
         }

         ++ordinal;
         continue;
      }

      // MoveDirect, never MoveCrossDevice: the staging directory is a sibling
      // of the slot, so a copy fallback would mean the invariant that makes
      // this rename atomic has been broken and we want to hear about it.
      Error error = stagingDir.move(candidate, FilePath::MoveDirect);
      if (!error)
      {
         DLOG("Published slot {}", candidate.getAbsolutePath());
         *pSlotDir = candidate;
         return Success();
      }

      // Someone renamed their own staged install into this name between the
      // check above and the rename. Re-examine that name under the same rules
      // rather than reading errno, which varies by platform for this case.
      if (candidate.exists())
      {
         DLOG("Lost the rename race for slot {}", candidate.getAbsolutePath());
         continue;
      }

      return error;
   }

   return systemError(boost::system::errc::file_exists,
                      "No install slot available for version " + version +
                         " in " + slotsDir.getAbsolutePath(),
                      ERROR_LOCATION);
}

} // namespace slots
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
