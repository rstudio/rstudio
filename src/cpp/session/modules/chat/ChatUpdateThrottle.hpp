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

// Persisted manifest-check record. The two bool flags store only MANIFEST-derived
// decisions; the local protocol.json mismatch is never persisted (it is recomputed
// at reapply time). installedVersion / rstudioProtocol record the context the flags
// were computed for, so a stale block is never applied to a different install.
// Never apply the bool flags directly: resolve them through resolvePersistedBlock(),
// which drops a flag whose context no longer matches the current install.
// rstudioVersion records which RStudio build made the last check attempt: a record
// written by a different build never throttles this one (see manifestCheckDue), so
// the first check after installing a different RStudio build always fetches.
// Compatibility: builds that predate the field ignore the extra key when reading
// and drop it when writing; a missing key reads back as "" here, which compares
// unequal to any real build and so just costs one extra fetch.
struct ManifestCheckRecord
{
   std::time_t lastCheckTime = 0;
   std::string installedVersion;
   std::string rstudioProtocol;
   std::string rstudioVersion;
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

// The pending-update fields of the update state: what the last completed manifest
// check found, and what an install would act on. These are carried across a
// throttled skip (a check that does not re-fetch the manifest).
struct PendingUpdate
{
   bool updateAvailable = false;
   bool isDowngrade = false;
   std::string newVersion;
   std::string downloadUrl;
   std::string expectedSha256;
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

// Pure: the record to persist for a completed fetch attempt. On success `staged`
// holds the authoritative record (which sets or clears the block); otherwise
// preserve `prior`'s block and bump only the timestamp via bumpRecord(). Only a
// success may set or clear the persisted block, and every real attempt bumps the
// timestamp -- so a failed or bad-manifest fetch still records the attempt and
// cannot bypass the throttle. Every attempt (success or failure) also stamps
// rstudioVersion, the running RStudio build: a new build's first check bypasses
// the throttle exactly once, then normal throttling resumes even if that first
// fetch failed.
ManifestCheckRecord recordToPersist(const boost::optional<ManifestCheckRecord>& staged,
                                    const boost::optional<ManifestCheckRecord>& prior,
                                    std::time_t now,
                                    const std::string& rstudioVersion);

// Pure: convert a throttle window expressed in whole hours to seconds, for use as
// the throttleSeconds argument to manifestCheckDue(). A non-positive value yields 0
// ("always check" -- manifestCheckDue treats a zero window as always due); a value
// large enough to overflow int seconds is capped at INT_MAX.
int throttleSecondsFromHours(int hours);

// Pure: is a manifest fetch due now?
//   force || !installed || protocolMismatch || rstudioVersionChanged ||
//   !lastCheckTime || now < *lastCheckTime ||
//   (now - *lastCheckTime) >= throttleSeconds
// rstudioVersionChanged means the persisted record was written by a different
// RStudio build: its timestamp must not throttle this build, so the first check
// after installing a different build always fetches (#18305).
// A future lastCheckTime (clock skew or a corrupted/copied state file) is treated
// as due (the `now < *lastCheckTime` disjunct), so checks resume immediately
// rather than being suppressed.
bool manifestCheckDue(bool force,
                      bool installed,
                      bool protocolMismatch,
                      bool rstudioVersionChanged,
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

// Pure: the pending update to keep after a throttled skip (a check that does not
// re-fetch the manifest). A skip learns nothing new, so the prior fetch's result
// remains authoritative and must not be discarded -- but only while the installed
// version is unchanged from the check that produced it (priorInstalledVersion).
// If it changed out of band (a manual or parallel install, or the update itself
// was applied), the pending target and its upgrade/downgrade classification are
// stale, so the pending update is cleared and the next due fetch recomputes it.
PendingUpdate carryPendingUpdateThroughSkip(const PendingUpdate& prior,
                                            const std::string& priorInstalledVersion,
                                            const std::string& installedVersion);

} // namespace throttle
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_UPDATE_THROTTLE_HPP
