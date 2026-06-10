/*
 * ChatUpdateThrottle.cpp
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

#include "ChatUpdateThrottle.hpp"
#include "ChatLogging.hpp"

#include <string>

#include <shared_core/json/Json.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/Xdg.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace throttle {

const int kManifestCheckThrottleSeconds = 24 * 60 * 60;

core::FilePath manifestCheckStatePath()
{
   return core::system::xdg::userDataDir()
      .completePath("pai")
      .completeChildPath("manifest-check.json");
}

boost::optional<ManifestCheckRecord> readManifestCheckRecord(const core::FilePath& stateFile)
{
   if (!stateFile.exists())
      return boost::none;

   std::string content;
   Error error = core::readStringFromFile(stateFile, &content);
   if (error)
   {
      WLOG("Failed to read manifest-check record at {}: {}",
           stateFile.getAbsolutePath(), error.getMessage());
      return boost::none;
   }

   json::Value value;
   if (value.parse(content))
   {
      WLOG("Manifest-check record at {} is not valid JSON; treating as malformed",
           stateFile.getAbsolutePath());
      return boost::none;
   }
   if (!value.isObject())
   {
      WLOG("Manifest-check record at {} is not a JSON object; treating as malformed",
           stateFile.getAbsolutePath());
      return boost::none;
   }

   json::Object obj = value.getObject();

   // lastCheckTime is required: absent or non-numeric means the record is corrupt.
   std::string lastCheckStr;
   if (json::readObject(obj, "lastCheckTime", lastCheckStr))
   {
      WLOG("Manifest-check record at {} is missing a string lastCheckTime; "
           "treating as malformed", stateFile.getAbsolutePath());
      return boost::none;
   }

   boost::optional<long long> lastCheckSeconds =
      core::safe_convert::stringTo<long long>(lastCheckStr);
   if (!lastCheckSeconds)
   {
      WLOG("Manifest-check record at {} has non-numeric lastCheckTime '{}'; "
           "treating as malformed", stateFile.getAbsolutePath(), lastCheckStr);
      return boost::none;
   }

   ManifestCheckRecord record;
   record.lastCheckTime = static_cast<std::time_t>(*lastCheckSeconds);

   // Context + flags are optional: a missing key leaves the struct default in place
   // (the boost::optional<T> readObject overload reports absence as Success/none).
   // A present field of the wrong type means the record is corrupt -> malformed.
   boost::optional<std::string> installedVersion;
   boost::optional<std::string> rstudioProtocol;
   boost::optional<bool> unsupportedInstalledVersion;
   boost::optional<bool> unsupportedProtocol;
   auto readOptionalField = [&](const char* fieldName, auto& outValue) -> bool
   {
      Error fieldError = json::readObject(obj, fieldName, outValue);
      if (fieldError)
      {
         WLOG("Manifest-check record at {} has a malformed '{}' field: {}",
              stateFile.getAbsolutePath(), fieldName, fieldError.getMessage());
         return true;
      }
      return false;
   };
   if (readOptionalField("installedVersion", installedVersion) ||
       readOptionalField("rstudioProtocol", rstudioProtocol) ||
       readOptionalField("unsupportedInstalledVersion", unsupportedInstalledVersion) ||
       readOptionalField("unsupportedProtocol", unsupportedProtocol))
      return boost::none;
   if (installedVersion)
      record.installedVersion = *installedVersion;
   if (rstudioProtocol)
      record.rstudioProtocol = *rstudioProtocol;
   if (unsupportedInstalledVersion)
      record.unsupportedInstalledVersion = *unsupportedInstalledVersion;
   if (unsupportedProtocol)
      record.unsupportedProtocol = *unsupportedProtocol;

   return record;
}

core::Error writeManifestCheckRecord(const core::FilePath& stateFile,
                                     const ManifestCheckRecord& record)
{
   Error error = stateFile.getParent().ensureDirectory();
   if (error)
      return error;

   json::Object obj;
   obj["lastCheckTime"] = std::to_string(static_cast<long long>(record.lastCheckTime));
   obj["installedVersion"] = record.installedVersion;
   obj["rstudioProtocol"] = record.rstudioProtocol;
   obj["unsupportedInstalledVersion"] = record.unsupportedInstalledVersion;
   obj["unsupportedProtocol"] = record.unsupportedProtocol;
   return core::writeStringToFile(stateFile, obj.write());
}

ManifestCheckRecord bumpRecord(boost::optional<ManifestCheckRecord> prior,
                               std::time_t now)
{
   ManifestCheckRecord out = prior.value_or(ManifestCheckRecord());
   out.lastCheckTime = now;
   return out;
}

ManifestCheckRecord recordToPersist(const boost::optional<ManifestCheckRecord>& staged,
                                    const boost::optional<ManifestCheckRecord>& prior,
                                    std::time_t now)
{
   return staged ? *staged : bumpRecord(prior, now);
}

bool manifestCheckDue(bool force,
                      bool installed,
                      bool protocolMismatch,
                      boost::optional<std::time_t> lastCheckTime,
                      std::time_t now,
                      int throttleSeconds)
{
   if (force || !installed || protocolMismatch)
      return true;
   if (!lastCheckTime)
      return true;
   // A future timestamp (clock skew, or a corrupted/copied state file) would
   // otherwise suppress checks until that future time plus the window; treat it
   // as due so checks resume immediately.
   if (now < *lastCheckTime)
      return true;
   std::time_t elapsed = now - *lastCheckTime;
   return elapsed >= static_cast<std::time_t>(throttleSeconds);
}

ResolvedBlock resolvePersistedBlock(const ManifestCheckRecord& record,
                                    const std::string& installedVersion,
                                    const std::string& rstudioProtocol)
{
   bool protoMatch = (record.rstudioProtocol == rstudioProtocol);
   bool versionMatch = (record.installedVersion == installedVersion);

   ResolvedBlock out;
   out.unsupportedProtocol = record.unsupportedProtocol && protoMatch;
   out.unsupportedInstalledVersion =
      record.unsupportedInstalledVersion && versionMatch && protoMatch;
   return out;
}

SuccessOutcome buildSuccessOutcome(std::time_t now,
                                   const std::string& installedVersion,
                                   const std::string& rstudioProtocol,
                                   bool versionUnsupported,
                                   bool protocolMismatch,
                                   bool unsupportedProtocol)
{
   SuccessOutcome out;
   out.liveUnsupportedInstalledVersion = versionUnsupported || protocolMismatch;
   out.record.lastCheckTime = now;
   out.record.installedVersion = installedVersion;
   out.record.rstudioProtocol = rstudioProtocol;
   out.record.unsupportedInstalledVersion = versionUnsupported;
   out.record.unsupportedProtocol = unsupportedProtocol;
   return out;
}

} // namespace throttle
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
