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

#include <cctype>

#include <boost/algorithm/string/case_conv.hpp>
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

// Device names Windows resolves no matter which directory they appear in.
// Checked on every platform on purpose: a home directory reached from both
// Windows and Linux sessions has to agree on which names are slots, and
// file_utils::isWindowsReservedName is compiled only on Windows.
bool isReservedDeviceName(const std::string& name)
{
   static const char* const kReserved[] = {
      "con", "prn", "aux", "nul",
      "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
      "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9"
   };

   // Windows resolves the device whatever follows it, so "nul.txt" and
   // "com1.log" are reserved too -- only the part before the first dot counts.
   std::string stem = boost::algorithm::to_lower_copy(
      name.substr(0, name.find('.')));

   for (const char* reserved : kReserved)
   {
      if (stem == reserved)
         return true;
   }

   return false;
}

std::string slotNameForOrdinal(const std::string& version, int ordinal)
{
   if (ordinal <= 1)
      return version;

   return version + "-" + safe_convert::numberToString(ordinal);
}

// Budgeted against the 255-byte limit on a path component, which the ".tmp-"
// prefix, the pid and the nonce also draw on. This truncation is what bounds
// the name: getHostname() returns the HOSTNAME environment variable verbatim,
// and only its gethostname() fallback is capped. A hostname can reach 253
// characters, so this can in principle shorten two long names to the same
// prefix; the nonce, not the hostname, is what keeps staging directories
// distinct.
const std::string::size_type kMaxHostnameLength = 180;

// Names a staging directory. The nonce is what makes it private: no other
// session can compute this name, so nothing else can write into the tree we
// are about to record a manifest for. Host and pid carry no correctness weight
// here -- they are in the name so a later cleanup pass can tell whose
// abandoned extraction it is looking at.
std::string stagingDirName()
{
   std::string hostname =
      core::system::getHostname().substr(0, kMaxHostnameLength);

   for (char& c : hostname)
   {
      if (!std::isalnum(static_cast<unsigned char>(c)) && c != '-' && c != '.')
         c = '_';
   }

   if (hostname.empty())
      hostname = "unknown-host";

   return std::string(kStagingDirPrefix) + hostname + "-" +
      safe_convert::numberToString(
         static_cast<int64_t>(core::system::currentProcessId())) + "-" +
      core::system::generateUuid(false);
}

} // anonymous namespace

bool isUsableSlotName(const std::string& name)
{
   if (name.empty() || name.front() == '.' || name.front() == '-')
      return false;

   // Windows silently drops a trailing dot or space, so the directory on disk
   // would not be the name recorded in the selector -- and on a home shared
   // with Linux sessions the two names are separate directories.
   if (name.back() == '.' || name.back() == ' ')
      return false;

   for (char c : name)
   {
      // Printable ASCII only: this string becomes a directory name in the
      // user's home, and a version is never anything else.
      if (c < 0x20 || c > 0x7e)
         return false;
   }

   if (isReservedDeviceName(name))
      return false;

   // Separators, the drive-letter colon, and the characters Win32 rejects
   // outright. Catching them here turns an opaque rename failure after a full
   // extraction into a clear message.
   return name.find_first_of("/\\:*?\"<>|") == std::string::npos;
}

FilePath versionsDir(const FilePath& storageDir)
{
   return storageDir.completeChildPath(kVersionsDirName);
}

bool verifySlot(const FilePath& slotDir, SlotInfo* pInfo)
{
   if (!slotDir.isDirectory())
      return false;

   // A slot replaced by a link is not a slot: everything in it, manifest
   // included, would be read from a tree the slot does not contain and cannot
   // promise is immutable. Junctions count, being how Windows redirects a
   // directory without is_symlink() reporting it.
   if (slotDir.isSymlink() || slotDir.isJunction())
   {
      DLOG("Slot {} is a link, not a directory", slotDir.getAbsolutePath());
      return false;
   }

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
   FilePath stagingDir = slotsDir.completeChildPath(stagingDirName());

   Error error = stagingDir.ensureDirectory();
   if (error)
      return error;

   *pStagingDir = stagingDir;
   return Success();
}

Error allocateSlot(const FilePath& stagingDir,
                   SlotPolicy policy,
                   FilePath* pSlotDir)
{
   // Record the manifest and verify here rather than trusting the caller, so
   // that "a slot only reaches a final name once it has been checked" is a
   // property of the layout instead of a rule every install path has to
   // remember. The staging directory is private to this call, so the tree
   // being recorded is the one that was just extracted.
   Error error = slot_manifest::writeSlotManifest(stagingDir);
   if (error)
      return error;

   SlotInfo staged;
   if (!verifySlot(stagingDir, &staged))
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Staged install at " + stagingDir.getAbsolutePath() +
                            " is not a complete Posit Assistant installation",
                         ERROR_LOCATION);
   }

   if (!isUsableSlotName(staged.version))
   {
      return systemError(boost::system::errc::invalid_argument,
                         "Cannot name an install slot for version '" +
                            staged.version + "'",
                         ERROR_LOCATION);
   }

   FilePath slotsDir = stagingDir.getParent();

   int ordinal = 1;
   for (int attempt = 0; attempt < kMaxAllocationAttempts; ++attempt)
   {
      FilePath candidate = slotsDir.completeChildPath(
         slotNameForOrdinal(staged.version, ordinal));

      if (candidate.exists())
      {
         // Adopt only what is genuinely interchangeable with what we staged.
         // A slot's name is not evidence of its contents, so the decision has
         // to rest on the package.json and protocol.json inside it.
         SlotInfo existing;
         if (policy == SlotPolicy::AdoptExisting &&
             verifySlot(candidate, &existing) &&
             existing.version == staged.version &&
             existing.protocol == staged.protocol)
         {
            DLOG("Adopting existing slot {}", candidate.getAbsolutePath());
            Error removeError = stagingDir.removeIfExists();
            if (removeError)
            {
               WLOG("Could not remove staging directory {}: {}",
                    stagingDir.getAbsolutePath(), removeError.getMessage());
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
      Error moveError = stagingDir.move(candidate, FilePath::MoveDirect);
      if (!moveError)
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

      return moveError;
   }

   return systemError(boost::system::errc::file_exists,
                      "No install slot available for version " + staged.version +
                         " in " + slotsDir.getAbsolutePath(),
                      ERROR_LOCATION);
}

} // namespace slots
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
