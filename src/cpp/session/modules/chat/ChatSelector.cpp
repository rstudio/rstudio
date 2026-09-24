/*
 * ChatSelector.cpp
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

#include "ChatSelector.hpp"

#include <vector>

#include "ChatConstants.hpp"
#include "ChatLogging.hpp"
#include "ChatSlots.hpp"
#include "ChatTypes.hpp"

#include <core/FileSerializer.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::constants;
using namespace rstudio::session::modules::chat::logging;
using namespace rstudio::session::modules::chat::types;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace selector {

namespace {

const char* const kSelectedKey = "selected";

FilePath selectorPath(const FilePath& storageDir)
{
   return storageDir.completeChildPath(kSelectorFileName);
}

// Which reinstall of a version a slot holds: 1 for the plain name, N for
// "<version>-N", and 0 when the name is not derived from the version at all.
//
// This is the one place a slot name is read, and only to order slots that are
// otherwise identical -- the alternative, comparing names as strings, ranks
// "1.1.0-9" above "1.1.0-10" and hands a user who just reinstalled the older
// slot back.
int slotOrdinal(const std::string& name, const std::string& version)
{
   if (name == version)
      return 1;

   std::string suffix = version + "-";
   if (name.compare(0, suffix.size(), suffix) != 0)
      return 0;

   std::string ordinal = name.substr(suffix.size());
   if (ordinal.empty() ||
       ordinal.find_first_not_of("0123456789") != std::string::npos)
   {
      return 0;
   }

   return safe_convert::stringTo<int>(ordinal, 0);
}

// Orders two verifying slots by which one a session should prefer: the higher
// version, then the later reinstall of the same version, and finally the name
// so the answer is always stable.
//
// Posit Assistant versions are plain x.y.z, and every other parser site
// rejects a suffixed one, so a version SemanticVersion cannot parse is not
// given a second reading here either: it ranks below every version that does
// parse and is otherwise ordered by name.
bool preferredOver(const slots::SlotInfo& lhs, const slots::SlotInfo& rhs)
{
   SemanticVersion lhsVersion, rhsVersion;
   bool lhsParsed = lhsVersion.parse(lhs.version);
   bool rhsParsed = rhsVersion.parse(rhs.version);

   if (lhsParsed != rhsParsed)
      return lhsParsed;

   if (lhsParsed && lhsVersion != rhsVersion)
      return lhsVersion > rhsVersion;

   // An ordinal counts reinstalls of one version and says nothing about two
   // slots holding different ones, so unparsed versions that differ are
   // ordered by string before any ordinal is looked at.
   if (lhs.version != rhs.version)
      return lhs.version > rhs.version;

   int lhsOrdinal = slotOrdinal(lhs.name, lhs.version);
   int rhsOrdinal = slotOrdinal(rhs.name, rhs.version);
   if (lhsOrdinal != rhsOrdinal)
      return lhsOrdinal > rhsOrdinal;

   return lhs.name > rhs.name;
}

// The best slot for `protocol`, or nothing.
bool bestSlotForProtocol(const FilePath& storageDir,
                         const std::string& protocol,
                         slots::SlotInfo* pInfo)
{
   std::vector<slots::SlotInfo> candidates =
      slots::verifiedSlots(slots::versionsDir(storageDir), protocol);

   bool found = false;
   for (const slots::SlotInfo& candidate : candidates)
   {
      if (!found || preferredOver(candidate, *pInfo))
      {
         *pInfo = candidate;
         found = true;
      }
   }

   return found;
}

} // anonymous namespace

Selections readSelections(const FilePath& storageDir)
{
   Selections selections;

   FilePath path = selectorPath(storageDir);
   if (!path.isRegularFile())
      return selections;

   std::string content;
   Error error = readStringFromFile(path, &content);
   if (error)
   {
      WLOG("Cannot read {}: {}", path.getAbsolutePath(), error.getMessage());
      return selections;
   }

   json::Value value;
   if (value.parse(content) || !value.isObject())
   {
      WLOG("{} is not a JSON object; ignoring it", path.getAbsolutePath());
      return selections;
   }

   json::Object selected;
   error = json::readObject(value.getObject(), kSelectedKey, selected);
   if (error)
   {
      WLOG("{} has no \"{}\" object; ignoring it",
           path.getAbsolutePath(), kSelectedKey);
      return selections;
   }

   for (const auto& member : selected)
   {
      if (member.getValue().isString())
         selections[member.getName()] = member.getValue().getString();
      else
         WLOG("Ignoring non-string selection for protocol {}", member.getName());
   }

   return selections;
}

Error writeSelections(const FilePath& storageDir, const Selections& selections)
{
   Error error = storageDir.ensureDirectory();
   if (error)
      return error;

   json::Object selected;
   for (const auto& selection : selections)
      selected[selection.first] = selection.second;

   json::Object root;
   root[kSelectedKey] = selected;

   return writeStringToFileAtomic(selectorPath(storageDir), root.write());
}

Error selectSlot(const FilePath& storageDir,
                 const std::string& protocol,
                 const std::string& slotName)
{
   Selections selections = readSelections(storageDir);
   selections[protocol] = slotName;
   return writeSelections(storageDir, selections);
}

bool selectInstalledVersion(const FilePath& storageDir,
                            const std::string& protocol,
                            const std::string& version)
{
   // The preferred slot among those holding the version, by the same order
   // the fallback uses. Two slots hold one version after a reinstall, and
   // the later one exists precisely because the earlier one is suspect, so
   // the first slot the directory listing happens to yield will not do.
   std::vector<slots::SlotInfo> installed =
      slots::verifiedSlots(slots::versionsDir(storageDir), protocol);

   bool found = false;
   slots::SlotInfo best;
   for (const slots::SlotInfo& slot : installed)
   {
      if (slot.version != version)
         continue;

      if (!found || preferredOver(slot, best))
      {
         best = slot;
         found = true;
      }
   }

   if (!found)
      return false;

   // A selection that already names a slot holding the version stays: it may
   // be a reinstall chosen over the one preferred here, and it is what the
   // caller is running.
   Selections selections = readSelections(storageDir);
   Selections::const_iterator selection = selections.find(protocol);
   if (selection != selections.end())
   {
      for (const slots::SlotInfo& slot : installed)
      {
         if (slot.name == selection->second && slot.version == version)
         {
            DLOG("Protocol {} already resolves to slot {} holding {}",
                 protocol, slot.name, version);
            return true;
         }
      }
   }

   Error error = selectSlot(storageDir, protocol, best.name);
   if (error)
   {
      WLOG("Version {} is installed as slot {} but could not be recorded: {}",
           version, best.name, error.getMessage());
      return false;
   }

   DLOG("Protocol {} now resolves to already-installed slot {}",
        protocol, best.name);
   return true;
}

FilePath resolveSlot(const FilePath& storageDir,
                     const std::string& protocol,
                     SelectorRepair repair)
{
   FilePath slotsDir = slots::versionsDir(storageDir);

   Selections selections = readSelections(storageDir);
   Selections::const_iterator selection = selections.find(protocol);
   if (selection != selections.end())
   {
      // The selector is a file in the user's home, so its contents are a
      // suggestion, not a path. Without this a dot-prefixed entry would
      // resolve a staging directory -- which verifies, being a complete tree,
      // right up until its owner renames it away underneath the running
      // backend -- and a traversal entry would hand back the versions
      // directory itself, since completeChildPath() returns the parent when it
      // rejects an escape.
      if (!slots::isUsableSlotName(selection->second))
      {
         WLOG("Ignoring selection '{}' for protocol {}: not a slot name",
              selection->second, protocol);
      }
      else
      {
         FilePath slotDir = slotsDir.completeChildPath(selection->second);

         slots::SlotInfo info;
         if (slots::verifySlot(slotDir, &info) && info.protocol == protocol)
         {
            DLOG("Protocol {} resolves to selected slot {}", protocol, info.name);
            return slotDir;
         }

         WLOG("Selected slot '{}' for protocol {} is unusable; looking for another",
              selection->second, protocol);
      }
   }

   slots::SlotInfo fallback;
   if (!bestSlotForProtocol(storageDir, protocol, &fallback))
   {
      DLOG("No install slot serves protocol {}", protocol);
      return FilePath();
   }

   ILOG("Protocol {} now resolves to slot {}", protocol, fallback.name);

   // Best effort: an unwritable storage directory costs us the repair, not the
   // resolve. The same fallback runs again next session.
   if (repair == SelectorRepair::Enabled)
   {
      Error error = selectSlot(storageDir, protocol, fallback.name);
      if (error)
      {
         WLOG("Could not record slot {} for protocol {}: {}",
              fallback.name, protocol, error.getMessage());
      }
   }

   return fallback.path;
}

} // namespace selector
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
