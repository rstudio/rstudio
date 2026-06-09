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

TEST(ChatUpdateThrottle, StatePathHasExpectedNameAndParent)
{
   FilePath path = manifestCheckStatePath();
   EXPECT_EQ(path.getFilename(), "manifest-check.json");
   EXPECT_EQ(path.getParent().getFilename(), "pai");
}
