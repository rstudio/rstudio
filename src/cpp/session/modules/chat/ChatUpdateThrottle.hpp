/*
 * ChatUpdateThrottle.hpp
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

#ifndef SESSION_CHAT_UPDATE_THROTTLE_HPP
#define SESSION_CHAT_UPDATE_THROTTLE_HPP

#include <ctime>
#include <string>

#include <boost/optional.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace throttle {

// Default throttle window: 24 hours.
extern const int kManifestCheckThrottleSeconds;

// Persisted manifest-check record. The two bool flags store only MANIFEST-derived
// decisions; the local protocol.json mismatch is never persisted (it is recomputed
// at reapply time). installedVersion / rstudioProtocol record the context the flags
// were computed for, so a stale block is never applied to a different install.
struct ManifestCheckRecord
{
   std::time_t lastCheckTime = 0;
   std::string installedVersion;
   std::string rstudioProtocol;
   bool unsupportedInstalledVersion = false;
   bool unsupportedProtocol = false;
};

// Persisted unsupported flags resolved against the current install.
struct ResolvedBlock
{
   bool unsupportedInstalledVersion = false;
   bool unsupportedProtocol = false;
};

// The live flag (for s_updateState) and the record (to persist) for a successful
// manifest check, derived from the same inputs.
struct SuccessOutcome
{
   bool liveUnsupportedInstalledVersion = false;
   ManifestCheckRecord record;
};

// Path of the persisted record: <userDataDir>/pai/manifest-check.json.
core::FilePath manifestCheckStatePath();

// Read the record. Returns none on missing / unreadable / malformed file, or when
// the required lastCheckTime field is absent.
boost::optional<ManifestCheckRecord> readManifestCheckRecord(const core::FilePath& stateFile);

// Write the record (ensureDirectory on the parent first).
core::Error writeManifestCheckRecord(const core::FilePath& stateFile,
                                     const ManifestCheckRecord& record);

// Preserve a prior record's flags + context, bumping only lastCheckTime. Returns a
// default record (flags false, empty context) with `now` when prior is none.
ManifestCheckRecord bumpRecord(boost::optional<ManifestCheckRecord> prior,
                               std::time_t now);

// Pure: is a manifest fetch due now?
//   force || !installed || protocolMismatch || !lastCheckTime ||
//   (now - *lastCheckTime) >= throttleSeconds
// A future lastCheckTime (clock skew or a corrupted/copied state file) is also
// treated as due, so checks resume immediately rather than being suppressed.
bool manifestCheckDue(bool force,
                      bool installed,
                      bool protocolMismatch,
                      boost::optional<std::time_t> lastCheckTime,
                      std::time_t now,
                      int throttleSeconds);

// Pure: resolve which persisted flags still apply to the current install.
// unsupportedProtocol is kept when the stored RStudio protocol matches;
// unsupportedInstalledVersion is kept only when both the stored installed version
// and RStudio protocol match.
ResolvedBlock resolvePersistedBlock(const ManifestCheckRecord& record,
                                    const std::string& installedVersion,
                                    const std::string& rstudioProtocol);

// Pure: split a successful check's result into the live composite flag
// (versionUnsupported || protocolMismatch) and the manifest-only record.
SuccessOutcome buildSuccessOutcome(std::time_t now,
                                   const std::string& installedVersion,
                                   const std::string& rstudioProtocol,
                                   bool versionUnsupported,
                                   bool protocolMismatch,
                                   bool unsupportedProtocol);

} // namespace throttle
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_UPDATE_THROTTLE_HPP
