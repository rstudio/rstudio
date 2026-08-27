/*
 * ChatSlotsTests.cpp
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

#include <algorithm>
#include <string>
#include <vector>

#include <gtest/gtest.h>

#include "ChatSlotManifest.hpp"

#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::slots;
using rstudio::session::modules::chat::slot_manifest::writeSlotManifest;

namespace {

const char* const kServerScript = "dist/server/main.js";
const char* const kIndexHtml = "dist/client/index.html";

class ChatSlots : public testing::Test
{
protected:
   void SetUp() override
   {
      FilePath tempPath;
      ASSERT_FALSE(FilePath::tempFilePath(tempPath));
      storageDir_ = tempPath.completePath("pai");
      versionsDir_ = versionsDir(storageDir_);
      ASSERT_FALSE(versionsDir_.ensureDirectory());
   }

   void TearDown() override
   {
      storageDir_.getParent().removeIfExists();
   }

   void writeFile(const FilePath& filePath, const std::string& content)
   {
      ASSERT_FALSE(filePath.getParent().ensureDirectory());
      ASSERT_FALSE(writeStringToFile(filePath, content));
   }

   // The tree a Posit Assistant package extracts to, in miniature. Tests that
   // want a damaged slot call this, break something, and only then record the
   // manifest -- so the manifest agrees with what is on disk and exactly one
   // check is left to fail.
   void writeSlotFiles(const FilePath& dir,
                       const std::string& version,
                       const std::string& protocol)
   {
      writeFile(dir.completeChildPath(kServerScript), "console.log('hi');");
      writeFile(dir.completeChildPath(kIndexHtml), "<html></html>");
      writeFile(dir.completeChildPath("package.json"),
                "{\"version\":\"" + version + "\"}");
      writeFile(dir.completeChildPath("protocol.json"),
                "{\"protocol\":\"" + protocol + "\"}");
   }

   // A slot exactly as an install leaves it.
   void makeSlot(const FilePath& dir,
                 const std::string& version,
                 const std::string& protocol)
   {
      writeSlotFiles(dir, version, protocol);
      ASSERT_FALSE(writeSlotManifest(dir));
   }

   FilePath slot(const std::string& name)
   {
      return versionsDir_.completeChildPath(name);
   }

   // A staged install belonging to some session. Named directly rather than
   // through prepareStagingDir() so one test can model several sessions.
   FilePath staging(const std::string& session)
   {
      return versionsDir_.completeChildPath(".tmp-" + session);
   }

   FilePath storageDir_;
   FilePath versionsDir_;
};

// ============================================================================
// verifySlot
// ============================================================================

TEST_F(ChatSlots, VerifiesAFreshInstall)
{
   makeSlot(slot("1.1.0"), "1.1.0", "11.0");
   EXPECT_TRUE(verifySlot(slot("1.1.0")));
}

TEST_F(ChatSlots, ReportsWhatTheSlotSaysAboutItself)
{
   // The slot name says 1.1.0-2, which is valid semver for something older
   // than 1.1.0; identity has to come from inside the slot instead.
   makeSlot(slot("1.1.0-2"), "1.1.0", "11.0");

   SlotInfo info;
   ASSERT_TRUE(verifySlot(slot("1.1.0-2"), &info));
   EXPECT_EQ(info.name, "1.1.0-2");
   EXPECT_EQ(info.version, "1.1.0");
   EXPECT_EQ(info.protocol, "11.0");
   EXPECT_TRUE(info.path.isEquivalentTo(slot("1.1.0-2")));
}

TEST_F(ChatSlots, RejectsAnAbsentSlot)
{
   EXPECT_FALSE(verifySlot(slot("1.1.0")));
}

TEST_F(ChatSlots, RejectsAMissingServerScript)
{
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");
   ASSERT_FALSE(dir.completeChildPath(kServerScript).remove());
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsAnEmptyServerScript)
{
   // The check this replaces tested only for existence, so a truncated
   // download left a zero-byte main.js that passed.
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");
   writeFile(dir.completeChildPath(kServerScript), "");
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsAnEmptyIndexHtml)
{
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");
   writeFile(dir.completeChildPath(kIndexHtml), "");
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsAMissingClientDirectory)
{
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");
   ASSERT_FALSE(dir.completeChildPath("dist/client").remove());
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsAnUnparseablePackageJson)
{
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");
   writeFile(dir.completeChildPath("package.json"), "{ not json");
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsAPackageJsonWithoutAVersion)
{
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");
   writeFile(dir.completeChildPath("package.json"), "{\"name\":\"assistant\"}");
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsAnEmptyVersion)
{
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "", "11.0");
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsAnUnparseableProtocolJson)
{
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");
   writeFile(dir.completeChildPath("protocol.json"), "[]");
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsAProtocolJsonWithoutAProtocol)
{
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");
   writeFile(dir.completeChildPath("protocol.json"), "{\"version\":\"11.0\"}");
   ASSERT_FALSE(writeSlotManifest(dir));

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsASlotWithNoManifest)
{
   // Every slot this module publishes gets a manifest, so a directory without
   // one was not left behind by an install that finished.
   FilePath dir = slot("1.1.0");
   writeSlotFiles(dir, "1.1.0", "11.0");

   EXPECT_FALSE(verifySlot(dir));
}

TEST_F(ChatSlots, RejectsASlotThatChangedAfterInstall)
{
   FilePath dir = slot("1.1.0");
   makeSlot(dir, "1.1.0", "11.0");
   writeFile(dir.completeChildPath(kServerScript), "console.log('tampered');");

   EXPECT_FALSE(verifySlot(dir));
}

// ============================================================================
// verifiedSlots
// ============================================================================

TEST_F(ChatSlots, ListsOnlyVerifyingSlots)
{
   makeSlot(slot("1.1.0"), "1.1.0", "11.0");
   makeSlot(slot("1.0.4"), "1.0.4", "10.0");
   writeSlotFiles(slot("0.9.0"), "0.9.0", "10.0"); // no manifest

   std::vector<SlotInfo> found = verifiedSlots(versionsDir_);
   ASSERT_EQ(found.size(), 2u);

   std::vector<std::string> names;
   for (const SlotInfo& info : found)
      names.push_back(info.name);
   std::sort(names.begin(), names.end());
   EXPECT_EQ(names[0], "1.0.4");
   EXPECT_EQ(names[1], "1.1.0");
}

TEST_F(ChatSlots, SkipsStagingDirectories)
{
   // A staged install is a complete tree that would otherwise verify; it must
   // not be resolvable before the rename publishes it.
   makeSlot(staging("session-a"), "1.1.0", "11.0");

   EXPECT_TRUE(verifiedSlots(versionsDir_).empty());
}

TEST_F(ChatSlots, FindsNothingBeforeTheFirstInstall)
{
   ASSERT_FALSE(versionsDir_.remove());
   EXPECT_TRUE(verifiedSlots(versionsDir_).empty());
}

// ============================================================================
// prepareStagingDir
// ============================================================================

TEST_F(ChatSlots, PreparesAnEmptyStagingDirectory)
{
   FilePath stagingDir;
   ASSERT_FALSE(prepareStagingDir(versionsDir_, &stagingDir));

   EXPECT_TRUE(stagingDir.isDirectory());
   EXPECT_TRUE(stagingDir.getParent().isEquivalentTo(versionsDir_));

   std::vector<FilePath> children;
   ASSERT_FALSE(stagingDir.getChildren(children));
   EXPECT_TRUE(children.empty());
}

TEST_F(ChatSlots, ClearsWhatACrashedSessionLeftStaged)
{
   FilePath stagingDir;
   ASSERT_FALSE(prepareStagingDir(versionsDir_, &stagingDir));
   writeSlotFiles(stagingDir, "1.0.4", "10.0");

   FilePath second;
   ASSERT_FALSE(prepareStagingDir(versionsDir_, &second));
   ASSERT_TRUE(second.isEquivalentTo(stagingDir));

   std::vector<FilePath> children;
   ASSERT_FALSE(second.getChildren(children));
   EXPECT_TRUE(children.empty());
}

TEST_F(ChatSlots, CreatesTheVersionsDirectoryOnFirstUse)
{
   ASSERT_FALSE(versionsDir_.remove());

   FilePath stagingDir;
   ASSERT_FALSE(prepareStagingDir(versionsDir_, &stagingDir));
   EXPECT_TRUE(versionsDir_.isDirectory());
}

// ============================================================================
// allocateSlot
// ============================================================================

TEST_F(ChatSlots, PublishesAStagedInstallUnderItsVersion)
{
   FilePath stagingDir;
   ASSERT_FALSE(prepareStagingDir(versionsDir_, &stagingDir));
   makeSlot(stagingDir, "1.1.0", "11.0");

   FilePath slotDir;
   ASSERT_FALSE(allocateSlot(stagingDir, "1.1.0",
                             SlotPolicy::AdoptExisting, &slotDir));

   EXPECT_EQ(slotDir.getFilename(), "1.1.0");
   EXPECT_TRUE(verifySlot(slotDir));
   EXPECT_FALSE(stagingDir.exists());
}

TEST_F(ChatSlots, AdoptsAnExistingVerifyingSlot)
{
   // The rename loser: another session published this version first, and its
   // slot is intact, so there is nothing to add.
   makeSlot(slot("1.1.0"), "1.1.0", "11.0");
   FilePath stagingDir = staging("session-b");
   makeSlot(stagingDir, "1.1.0", "11.0");

   FilePath slotDir;
   ASSERT_FALSE(allocateSlot(stagingDir, "1.1.0",
                             SlotPolicy::AdoptExisting, &slotDir));

   EXPECT_EQ(slotDir.getFilename(), "1.1.0");
   EXPECT_FALSE(slot("1.1.0-2").exists());
   EXPECT_FALSE(stagingDir.exists());
}

TEST_F(ChatSlots, BumpsPastAnExistingSlotThatDoesNotVerify)
{
   FilePath damaged = slot("1.1.0");
   writeSlotFiles(damaged, "1.1.0", "11.0"); // no manifest
   FilePath stagingDir = staging("session-b");
   makeSlot(stagingDir, "1.1.0", "11.0");

   FilePath slotDir;
   ASSERT_FALSE(allocateSlot(stagingDir, "1.1.0",
                             SlotPolicy::AdoptExisting, &slotDir));

   EXPECT_EQ(slotDir.getFilename(), "1.1.0-2");
   EXPECT_TRUE(verifySlot(slotDir));
   // The damaged slot is left alone -- another session may be running from it.
   EXPECT_TRUE(damaged.exists());
}

TEST_F(ChatSlots, AdoptsALaterOrdinalWhenTheFirstIsDamaged)
{
   writeSlotFiles(slot("1.1.0"), "1.1.0", "11.0"); // no manifest
   makeSlot(slot("1.1.0-2"), "1.1.0", "11.0");
   FilePath stagingDir = staging("session-c");
   makeSlot(stagingDir, "1.1.0", "11.0");

   FilePath slotDir;
   ASSERT_FALSE(allocateSlot(stagingDir, "1.1.0",
                             SlotPolicy::AdoptExisting, &slotDir));

   EXPECT_EQ(slotDir.getFilename(), "1.1.0-2");
   EXPECT_FALSE(slot("1.1.0-3").exists());
}

TEST_F(ChatSlots, ForcedAllocationNeverAdopts)
{
   // Reinstall exists to replace bits that verification cannot fault, so it
   // has to end up in a directory it wrote itself.
   makeSlot(slot("1.1.0"), "1.1.0", "11.0");
   FilePath stagingDir = staging("session-b");
   makeSlot(stagingDir, "1.1.0", "11.0");

   FilePath slotDir;
   ASSERT_FALSE(allocateSlot(stagingDir, "1.1.0",
                             SlotPolicy::AlwaysFresh, &slotDir));

   EXPECT_EQ(slotDir.getFilename(), "1.1.0-2");
   EXPECT_TRUE(verifySlot(slot("1.1.0")));
   EXPECT_TRUE(verifySlot(slotDir));
}

TEST_F(ChatSlots, ForcedAllocationTakesThePlainNameWhenItIsFree)
{
   FilePath stagingDir = staging("session-a");
   makeSlot(stagingDir, "1.1.0", "11.0");

   FilePath slotDir;
   ASSERT_FALSE(allocateSlot(stagingDir, "1.1.0",
                             SlotPolicy::AlwaysFresh, &slotDir));

   EXPECT_EQ(slotDir.getFilename(), "1.1.0");
}

TEST_F(ChatSlots, AllocatesPastTheSecondOrdinal)
{
   makeSlot(slot("1.1.0"), "1.1.0", "11.0");
   makeSlot(slot("1.1.0-2"), "1.1.0", "11.0");
   FilePath stagingDir = staging("session-c");
   makeSlot(stagingDir, "1.1.0", "11.0");

   FilePath slotDir;
   ASSERT_FALSE(allocateSlot(stagingDir, "1.1.0",
                             SlotPolicy::AlwaysFresh, &slotDir));

   EXPECT_EQ(slotDir.getFilename(), "1.1.0-3");
   EXPECT_TRUE(verifySlot(slotDir));
}

TEST_F(ChatSlots, DoesNotCollideWithAnUnrelatedVersion)
{
   makeSlot(slot("1.1.0"), "1.1.0", "11.0");
   FilePath stagingDir = staging("session-b");
   makeSlot(stagingDir, "1.0.4", "10.0");

   FilePath slotDir;
   ASSERT_FALSE(allocateSlot(stagingDir, "1.0.4",
                             SlotPolicy::AdoptExisting, &slotDir));

   EXPECT_EQ(slotDir.getFilename(), "1.0.4");
   EXPECT_TRUE(verifySlot(slot("1.1.0")));
}

TEST_F(ChatSlots, RejectsAVersionThatCannotNameADirectory)
{
   // The version comes from a downloaded manifest, so it is not trusted to be
   // a bare version string.
   FilePath stagingDir = staging("session-a");
   makeSlot(stagingDir, "1.1.0", "11.0");

   const char* const badVersions[] = {"", ".", "..", ".hidden", "-2",
                                      "a/b", "a\\b", "C:evil"};
   for (const char* version : badVersions)
   {
      FilePath slotDir;
      EXPECT_TRUE(allocateSlot(stagingDir, version,
                               SlotPolicy::AdoptExisting, &slotDir) != Success())
         << "accepted version '" << version << "'";
   }

   EXPECT_TRUE(stagingDir.exists());
}

TEST_F(ChatSlots, RejectsAnAbsentStagingDirectory)
{
   FilePath slotDir;
   EXPECT_TRUE(allocateSlot(staging("never-created"), "1.1.0",
                            SlotPolicy::AdoptExisting, &slotDir) != Success());
}

} // anonymous namespace
