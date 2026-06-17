/*
 * ChatUpdateThrottleTests.cpp
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

#include <ctime>
#include <limits>

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::throttle;

namespace {

const std::time_t kNow = 1000000000;
const int kDay = 24 * 60 * 60;

ManifestCheckRecord makeRecord(std::time_t t,
                               const std::string& ver,
                               const std::string& proto,
                               bool unsupVer,
                               bool unsupProto)
{
   ManifestCheckRecord r;
   r.lastCheckTime = t;
   r.installedVersion = ver;
   r.rstudioProtocol = proto;
   r.unsupportedInstalledVersion = unsupVer;
   r.unsupportedProtocol = unsupProto;
   return r;
}

} // anonymous namespace

// ---- manifestCheckDue ----

TEST(ChatUpdateThrottle, DueWhenForced)
{
   EXPECT_TRUE(manifestCheckDue(true, true, false, kNow, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueWhenNotInstalled)
{
   EXPECT_TRUE(manifestCheckDue(false, false, false, kNow, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueWhenProtocolMismatch)
{
   EXPECT_TRUE(manifestCheckDue(false, true, true, kNow, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueWhenNoPriorCheck)
{
   EXPECT_TRUE(manifestCheckDue(false, true, false, boost::none, kNow, kDay));
}

TEST(ChatUpdateThrottle, NotDueWithinWindow)
{
   std::time_t last = kNow - (kDay - 1);
   EXPECT_FALSE(manifestCheckDue(false, true, false, last, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueAtWindowBoundary)
{
   std::time_t last = kNow - kDay;
   EXPECT_TRUE(manifestCheckDue(false, true, false, last, kNow, kDay));
}

TEST(ChatUpdateThrottle, DuePastWindow)
{
   std::time_t last = kNow - (kDay + 100);
   EXPECT_TRUE(manifestCheckDue(false, true, false, last, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueWhenLastCheckInFuture)
{
   std::time_t future = kNow + kDay;
   EXPECT_TRUE(manifestCheckDue(false, true, false, future, kNow, kDay));
}

TEST(ChatUpdateThrottle, DueWhenThrottleZero)
{
   // A zero window means "always check": even a check made this instant is due.
   EXPECT_TRUE(manifestCheckDue(false, true, false, kNow, kNow, 0));
}

// ---- throttleSecondsFromHours ----

TEST(ChatUpdateThrottle, ThrottleZeroHoursMeansAlwaysCheck)
{
   EXPECT_EQ(throttleSecondsFromHours(0), 0);
}

TEST(ChatUpdateThrottle, ThrottleNegativeHoursClampedToAlwaysCheck)
{
   EXPECT_EQ(throttleSecondsFromHours(-5), 0);
}

TEST(ChatUpdateThrottle, ThrottlePositiveHoursConvertToSeconds)
{
   EXPECT_EQ(throttleSecondsFromHours(1), 3600);
   EXPECT_EQ(throttleSecondsFromHours(24), kDay);
}

TEST(ChatUpdateThrottle, ThrottleMaxHoursConvertsExactlyWithoutClamping)
{
   // The largest hour count whose seconds still fit in an int must convert
   // exactly, not be clamped.
   const int maxHours = std::numeric_limits<int>::max() / 3600;
   EXPECT_EQ(throttleSecondsFromHours(maxHours), maxHours * 3600);
}

TEST(ChatUpdateThrottle, ThrottleFirstClampedHourReturnsMaxWithoutOverflow)
{
   // The first hour count past the exactly-representable maximum is where the
   // clamp engages; it must return INT_MAX rather than overflow. This pins the
   // boundary so a future > vs >= edit cannot slip past.
   const int maxHours = std::numeric_limits<int>::max() / 3600;
   EXPECT_EQ(throttleSecondsFromHours(maxHours + 1), std::numeric_limits<int>::max());
}

TEST(ChatUpdateThrottle, ThrottleHugeHoursCappedWithoutOverflow)
{
   EXPECT_EQ(throttleSecondsFromHours(std::numeric_limits<int>::max()),
             std::numeric_limits<int>::max());
}

// ---- resolvePersistedBlock ----

TEST(ChatUpdateThrottle, ResolveBothPreservedWhenContextMatches)
{
   ManifestCheckRecord r = makeRecord(kNow, "1.2.3", "10.0", true, true);
   ResolvedBlock b = resolvePersistedBlock(r, "1.2.3", "10.0");
   EXPECT_TRUE(b.unsupportedInstalledVersion);
   EXPECT_TRUE(b.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, ResolveVersionBlockDroppedOnVersionChange)
{
   ManifestCheckRecord r = makeRecord(kNow, "1.2.3", "10.0", true, true);
   ResolvedBlock b = resolvePersistedBlock(r, "1.2.4", "10.0");
   EXPECT_FALSE(b.unsupportedInstalledVersion);
   EXPECT_TRUE(b.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, ResolveBothDroppedOnProtocolChange)
{
   ManifestCheckRecord r = makeRecord(kNow, "1.2.3", "10.0", true, true);
   ResolvedBlock b = resolvePersistedBlock(r, "1.2.3", "11.0");
   EXPECT_FALSE(b.unsupportedInstalledVersion);
   EXPECT_FALSE(b.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, ResolveFalseFlagsStayFalse)
{
   ManifestCheckRecord r = makeRecord(kNow, "1.2.3", "10.0", false, false);
   ResolvedBlock b = resolvePersistedBlock(r, "1.2.3", "10.0");
   EXPECT_FALSE(b.unsupportedInstalledVersion);
   EXPECT_FALSE(b.unsupportedProtocol);
}

// ---- buildSuccessOutcome ----

TEST(ChatUpdateThrottle, OutcomePersistsManifestOnlyNotComposite)
{
   // versionUnsupported=false but protocolMismatch=true: live blocked, persisted NOT.
   SuccessOutcome o = buildSuccessOutcome(kNow, "1.2.3", "10.0", false, true, false);
   EXPECT_TRUE(o.liveUnsupportedInstalledVersion);
   EXPECT_FALSE(o.record.unsupportedInstalledVersion);
}

TEST(ChatUpdateThrottle, OutcomeVersionUnsupportedPersists)
{
   SuccessOutcome o = buildSuccessOutcome(kNow, "1.2.3", "10.0", true, false, false);
   EXPECT_TRUE(o.liveUnsupportedInstalledVersion);
   EXPECT_TRUE(o.record.unsupportedInstalledVersion);
}

TEST(ChatUpdateThrottle, OutcomeCarriesContext)
{
   SuccessOutcome o = buildSuccessOutcome(kNow, "1.2.3", "10.0", false, false, true);
   EXPECT_FALSE(o.liveUnsupportedInstalledVersion);
   EXPECT_EQ(o.record.lastCheckTime, kNow);
   EXPECT_EQ(o.record.installedVersion, "1.2.3");
   EXPECT_EQ(o.record.rstudioProtocol, "10.0");
   EXPECT_TRUE(o.record.unsupportedProtocol);
}

// ---- carryPendingUpdateThroughSkip ----

TEST(ChatUpdateThrottle, ThrottledSkipPreservesPendingUpdateWhenInstalledUnchanged)
{
   // A prior fetch found an available upgrade (0.5.0 installed -> 0.5.1 offered).
   // A later throttled check does not re-fetch and the install is unchanged, so it
   // learns nothing new and must not discard that pending update -- otherwise a
   // click on "Update" afterwards hits "No update available".
   PendingUpdate prior;
   prior.updateAvailable = true;
   prior.isDowngrade = false;
   prior.newVersion = "0.5.1";
   prior.downloadUrl = "https://cdn.posit.co/posit-ai/assistant-rstudio-0.5.1.zip";
   prior.expectedSha256 = "695adb72723a3bb6d69c3e1cafd439f953e68f9ae3e5c0a10265a4314013cbcc";

   PendingUpdate out = carryPendingUpdateThroughSkip(prior, "0.5.0", "0.5.0");

   EXPECT_TRUE(out.updateAvailable);
   EXPECT_FALSE(out.isDowngrade);
   EXPECT_EQ(out.newVersion, "0.5.1");
   EXPECT_EQ(out.downloadUrl, "https://cdn.posit.co/posit-ai/assistant-rstudio-0.5.1.zip");
   EXPECT_EQ(out.expectedSha256, "695adb72723a3bb6d69c3e1cafd439f953e68f9ae3e5c0a10265a4314013cbcc");
}

TEST(ChatUpdateThrottle, ThrottledSkipPreservesDowngradeClassificationWhenInstalledUnchanged)
{
   // A pending downgrade offer must survive a skip with its isDowngrade flag
   // intact, not just an upgrade -- guards against a future edit that hardcodes
   // isDowngrade in the carry path instead of returning the prior verbatim.
   PendingUpdate prior;
   prior.updateAvailable = true;
   prior.isDowngrade = true;
   prior.newVersion = "0.4.8";
   prior.downloadUrl = "https://cdn.posit.co/posit-ai/assistant-rstudio-0.4.8.zip";

   PendingUpdate out = carryPendingUpdateThroughSkip(prior, "0.5.0", "0.5.0");

   EXPECT_TRUE(out.updateAvailable);
   EXPECT_TRUE(out.isDowngrade);
   EXPECT_EQ(out.newVersion, "0.4.8");
}

TEST(ChatUpdateThrottle, ThrottledSkipClearsWhenNoPriorUpdate)
{
   // No pending update from the last check: a skip must not resurrect one from
   // stale leftover fields.
   PendingUpdate prior;
   prior.updateAvailable = false;
   prior.newVersion = "0.5.1";

   PendingUpdate out = carryPendingUpdateThroughSkip(prior, "0.5.0", "0.5.0");

   EXPECT_FALSE(out.updateAvailable);
   EXPECT_TRUE(out.newVersion.empty());
}

TEST(ChatUpdateThrottle, ThrottledSkipClearsWhenInstalledChanged)
{
   // The installed version changed out of band since the check that produced the
   // pending update (a manual or parallel install, or the update was applied).
   // The skip must clear rather than carry a stale target and downgrade
   // classification; the next due fetch recomputes against the current install.
   PendingUpdate prior;
   prior.updateAvailable = true;
   prior.isDowngrade = true;
   prior.newVersion = "0.5.0";
   prior.downloadUrl = "https://cdn.posit.co/posit-ai/assistant-rstudio-0.5.0.zip";

   PendingUpdate out = carryPendingUpdateThroughSkip(prior, "0.5.1", "0.4.0");

   EXPECT_FALSE(out.updateAvailable);
   EXPECT_FALSE(out.isDowngrade);
   EXPECT_TRUE(out.newVersion.empty());
   EXPECT_TRUE(out.downloadUrl.empty());
}

// ---- bumpRecord ----

TEST(ChatUpdateThrottle, BumpPreservesPriorAndBumpsTime)
{
   ManifestCheckRecord prior = makeRecord(kNow - kDay, "1.2.3", "10.0", true, false);
   ManifestCheckRecord b = bumpRecord(prior, kNow);
   EXPECT_EQ(b.lastCheckTime, kNow);
   EXPECT_EQ(b.installedVersion, "1.2.3");
   EXPECT_EQ(b.rstudioProtocol, "10.0");
   EXPECT_TRUE(b.unsupportedInstalledVersion);
   EXPECT_FALSE(b.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, BumpWithNoPriorReturnsDefaultWithTime)
{
   ManifestCheckRecord b = bumpRecord(boost::none, kNow);
   EXPECT_EQ(b.lastCheckTime, kNow);
   EXPECT_TRUE(b.installedVersion.empty());
   EXPECT_FALSE(b.unsupportedInstalledVersion);
}

// ---- recordToPersist (the finish() persistence decision) ----

TEST(ChatUpdateThrottle, PersistUsesStagedRecordOnSuccess)
{
   // A success stages an authoritative record; it is persisted verbatim, which is
   // how a successful check sets or clears the block.
   ManifestCheckRecord staged = makeRecord(kNow, "1.2.3", "10.0", true, false);
   ManifestCheckRecord prior = makeRecord(kNow - kDay, "1.0.0", "10.0", false, true);
   ManifestCheckRecord out = recordToPersist(staged, prior, kNow + 5);
   EXPECT_EQ(out.lastCheckTime, kNow);
   EXPECT_EQ(out.installedVersion, "1.2.3");
   EXPECT_TRUE(out.unsupportedInstalledVersion);
   EXPECT_FALSE(out.unsupportedProtocol);
}

TEST(ChatUpdateThrottle, PersistBumpsAndPreservesPriorWhenNotStaged)
{
   // Failure / bad-manifest path: no staged record -> bump the timestamp (so the
   // attempt is recorded and cannot bypass the throttle) while preserving the prior
   // block (only a success may clear it).
   ManifestCheckRecord prior = makeRecord(kNow - kDay, "1.2.3", "10.0", true, false);
   ManifestCheckRecord out = recordToPersist(boost::none, prior, kNow);
   EXPECT_EQ(out.lastCheckTime, kNow);
   EXPECT_EQ(out.installedVersion, "1.2.3");
   EXPECT_TRUE(out.unsupportedInstalledVersion);
}

TEST(ChatUpdateThrottle, PersistBumpsDefaultWhenNoStagedNoPrior)
{
   ManifestCheckRecord out = recordToPersist(boost::none, boost::none, kNow);
   EXPECT_EQ(out.lastCheckTime, kNow);
   EXPECT_TRUE(out.installedVersion.empty());
   EXPECT_FALSE(out.unsupportedInstalledVersion);
}

// ---- read / write round-trip ----

TEST(ChatUpdateThrottle, RecordRoundTrip)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   ManifestCheckRecord in = makeRecord(kNow, "1.2.3", "10.0", true, false);
   ASSERT_FALSE(writeManifestCheckRecord(tmp, in));

   boost::optional<ManifestCheckRecord> out = readManifestCheckRecord(tmp);
   ASSERT_TRUE(out);
   EXPECT_EQ(out->lastCheckTime, kNow);
   EXPECT_EQ(out->installedVersion, "1.2.3");
   EXPECT_EQ(out->rstudioProtocol, "10.0");
   EXPECT_TRUE(out->unsupportedInstalledVersion);
   EXPECT_FALSE(out->unsupportedProtocol);
   tmp.removeIfExists();
}

TEST(ChatUpdateThrottle, ReadMissingFileReturnsNone)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   tmp.removeIfExists();
   EXPECT_FALSE(readManifestCheckRecord(tmp));
}

TEST(ChatUpdateThrottle, ReadMalformedReturnsNone)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   ASSERT_FALSE(writeStringToFile(tmp, "not json"));
   EXPECT_FALSE(readManifestCheckRecord(tmp));
   tmp.removeIfExists();
}

TEST(ChatUpdateThrottle, ReadMalformedOptionalFieldReturnsNone)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   ASSERT_FALSE(writeStringToFile(tmp,
      "{\"lastCheckTime\":\"100\",\"unsupportedInstalledVersion\":\"notabool\"}"));
   EXPECT_FALSE(readManifestCheckRecord(tmp));
   tmp.removeIfExists();
}

TEST(ChatUpdateThrottle, ReadNonNumericLastCheckTimeReturnsNone)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   // A non-numeric required lastCheckTime is a corrupt record: it must be rejected
   // rather than silently parsed to epoch 0 (which would also let its flags through).
   ASSERT_FALSE(writeStringToFile(tmp,
      "{\"lastCheckTime\":\"notanumber\",\"unsupportedInstalledVersion\":true}"));
   EXPECT_FALSE(readManifestCheckRecord(tmp));
   tmp.removeIfExists();
}

TEST(ChatUpdateThrottle, ReadMissingOptionalFieldsTolerated)
{
   FilePath tmp;
   ASSERT_FALSE(FilePath::tempFilePath(tmp));
   ASSERT_FALSE(writeStringToFile(tmp, "{\"lastCheckTime\":\"100\"}"));
   boost::optional<ManifestCheckRecord> out = readManifestCheckRecord(tmp);
   ASSERT_TRUE(out);
   EXPECT_EQ(out->lastCheckTime, static_cast<std::time_t>(100));
   EXPECT_TRUE(out->installedVersion.empty());
   EXPECT_FALSE(out->unsupportedInstalledVersion);
   tmp.removeIfExists();
}

TEST(ChatUpdateThrottle, StatePathHasExpectedNameAndParent)
{
   FilePath path = manifestCheckStatePath();
   EXPECT_EQ(path.getFilename(), "manifest-check.json");
   EXPECT_EQ(path.getParent().getFilename(), "pai");
}
