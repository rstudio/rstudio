/*
 * ChatSelectorTests.cpp
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

#include <string>

#include <gtest/gtest.h>

#include "ChatSlotManifest.hpp"
#include "ChatSlots.hpp"

#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::selector;
using rstudio::session::modules::chat::slot_manifest::writeSlotManifest;
using rstudio::session::modules::chat::slots::versionsDir;

namespace {

class ChatSelector : public testing::Test
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

   // A slot as an install leaves it: the files the backend needs, the identity
   // files resolution reads, and the manifest describing the tree.
   void makeSlot(const std::string& name,
                 const std::string& version,
                 const std::string& protocol)
   {
      FilePath dir = slot(name);
      writeFile(dir.completeChildPath("dist/server/main.js"), "console.log('hi');");
      writeFile(dir.completeChildPath("dist/client/index.html"), "<html></html>");
      writeFile(dir.completeChildPath("package.json"),
                "{\"version\":\"" + version + "\"}");
      writeFile(dir.completeChildPath("protocol.json"),
                "{\"protocol\":\"" + protocol + "\"}");
      ASSERT_FALSE(writeSlotManifest(dir));
   }

   // A slot that will not verify: no manifest was ever recorded for it.
   void makeDamagedSlot(const std::string& version, const std::string& protocol)
   {
      FilePath dir = slot(version);
      writeFile(dir.completeChildPath("dist/server/main.js"), "console.log('hi');");
      writeFile(dir.completeChildPath("dist/client/index.html"), "<html></html>");
      writeFile(dir.completeChildPath("package.json"),
                "{\"version\":\"" + version + "\"}");
      writeFile(dir.completeChildPath("protocol.json"),
                "{\"protocol\":\"" + protocol + "\"}");
   }

   void writeSelectorFile(const std::string& content)
   {
      writeFile(storageDir_.completeChildPath("selected.json"), content);
   }

   FilePath slot(const std::string& name)
   {
      return versionsDir_.completeChildPath(name);
   }

   FilePath storageDir_;
   FilePath versionsDir_;
};

// ============================================================================
// readSelections / writeSelections
// ============================================================================

TEST_F(ChatSelector, RoundTripsSelections)
{
   Selections written;
   written["11.0"] = "1.1.0-2";
   written["10.0"] = "0.4.8";
   ASSERT_FALSE(writeSelections(storageDir_, written));

   Selections read = readSelections(storageDir_);
   EXPECT_EQ(read.size(), 2u);
   EXPECT_EQ(read["11.0"], "1.1.0-2");
   EXPECT_EQ(read["10.0"], "0.4.8");
}

TEST_F(ChatSelector, WritesSlotNamesKeyedByProtocol)
{
   // Pins the on-disk shape: this file outlives the release that wrote it.
   Selections written;
   written["11.0"] = "1.1.0-2";
   ASSERT_FALSE(writeSelections(storageDir_, written));

   std::string content;
   ASSERT_FALSE(readStringFromFile(
      storageDir_.completeChildPath("selected.json"), &content));

   json::Value value;
   ASSERT_FALSE(value.parse(content));
   ASSERT_TRUE(value.isObject());

   json::Object selected;
   ASSERT_FALSE(json::readObject(value.getObject(), "selected", selected));
   ASSERT_TRUE(selected.hasMember("11.0"));
   EXPECT_EQ(selected["11.0"].getString(), "1.1.0-2");
}

TEST_F(ChatSelector, CreatesTheStorageDirectoryWhenWriting)
{
   ASSERT_FALSE(storageDir_.remove());

   Selections written;
   written["11.0"] = "1.1.0";
   ASSERT_FALSE(writeSelections(storageDir_, written));
   EXPECT_EQ(readSelections(storageDir_)["11.0"], "1.1.0");
}

TEST_F(ChatSelector, ReadsNothingBeforeAnythingIsSelected)
{
   EXPECT_TRUE(readSelections(storageDir_).empty());
}

TEST_F(ChatSelector, ReadsNothingFromUnparseableJson)
{
   writeSelectorFile("not json at all");
   EXPECT_TRUE(readSelections(storageDir_).empty());
}

TEST_F(ChatSelector, ReadsNothingWithoutASelectedObject)
{
   writeSelectorFile("{\"11.0\":\"1.1.0\"}");
   EXPECT_TRUE(readSelections(storageDir_).empty());
}

TEST_F(ChatSelector, IgnoresSelectionsThatAreNotSlotNames)
{
   writeSelectorFile("{\"selected\":{\"11.0\":\"1.1.0\",\"10.0\":42}}");

   Selections read = readSelections(storageDir_);
   EXPECT_EQ(read.size(), 1u);
   EXPECT_EQ(read["11.0"], "1.1.0");
}

TEST_F(ChatSelector, SelectingOneProtocolLeavesTheOthersAlone)
{
   ASSERT_FALSE(selectSlot(storageDir_, "10.0", "0.4.8"));
   ASSERT_FALSE(selectSlot(storageDir_, "11.0", "1.1.0"));

   Selections read = readSelections(storageDir_);
   EXPECT_EQ(read.size(), 2u);
   EXPECT_EQ(read["10.0"], "0.4.8");
   EXPECT_EQ(read["11.0"], "1.1.0");
}

TEST_F(ChatSelector, SelectingReplacesThePreviousSlotForThatProtocol)
{
   ASSERT_FALSE(selectSlot(storageDir_, "11.0", "1.1.0"));
   ASSERT_FALSE(selectSlot(storageDir_, "11.0", "1.1.0-2"));

   Selections read = readSelections(storageDir_);
   EXPECT_EQ(read.size(), 1u);
   EXPECT_EQ(read["11.0"], "1.1.0-2");
}

// ============================================================================
// resolveSlot
// ============================================================================

TEST_F(ChatSelector, ResolvesTheSelectedSlot)
{
   makeSlot("1.1.0", "1.1.0", "11.0");
   makeSlot("1.0.4", "1.0.4", "11.0");
   ASSERT_FALSE(selectSlot(storageDir_, "11.0", "1.0.4"));

   // The selection wins over the newer slot: a session runs what was chosen.
   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.0.4")));
}

TEST_F(ChatSelector, RepairsASelectionOfAMissingSlot)
{
   makeSlot("1.1.0", "1.1.0", "11.0");
   ASSERT_FALSE(selectSlot(storageDir_, "11.0", "1.0.4"));

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
   EXPECT_EQ(readSelections(storageDir_)["11.0"], "1.1.0");
}

TEST_F(ChatSelector, RepairsASelectionOfACorruptSlot)
{
   makeDamagedSlot("1.2.0", "11.0");
   makeSlot("1.1.0", "1.1.0", "11.0");
   ASSERT_FALSE(selectSlot(storageDir_, "11.0", "1.2.0"));

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
   EXPECT_EQ(readSelections(storageDir_)["11.0"], "1.1.0");
}

TEST_F(ChatSelector, RepairsASelectionOfASlotForAnotherProtocol)
{
   makeSlot("2.0.0", "2.0.0", "12.0");
   makeSlot("1.1.0", "1.1.0", "11.0");
   ASSERT_FALSE(selectSlot(storageDir_, "11.0", "2.0.0"));

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
   EXPECT_EQ(readSelections(storageDir_)["11.0"], "1.1.0");
}

TEST_F(ChatSelector, ResolvesAndRecordsWithNoSelectorAtAll)
{
   makeSlot("1.1.0", "1.1.0", "11.0");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
   EXPECT_EQ(readSelections(storageDir_)["11.0"], "1.1.0");
}

TEST_F(ChatSelector, ResolvesAndRewritesAMalformedSelector)
{
   makeSlot("1.1.0", "1.1.0", "11.0");
   writeSelectorFile("{\"selected\": [");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
   EXPECT_EQ(readSelections(storageDir_)["11.0"], "1.1.0");
}

TEST_F(ChatSelector, RepairingOneProtocolLeavesTheOthersAlone)
{
   makeSlot("1.1.0", "1.1.0", "11.0");
   makeSlot("0.4.8", "0.4.8", "10.0");
   ASSERT_FALSE(selectSlot(storageDir_, "11.0", "gone"));
   ASSERT_FALSE(selectSlot(storageDir_, "10.0", "0.4.8"));

   ASSERT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));

   Selections read = readSelections(storageDir_);
   EXPECT_EQ(read["11.0"], "1.1.0");
   EXPECT_EQ(read["10.0"], "0.4.8");
}

TEST_F(ChatSelector, FallsBackToTheNewestVersion)
{
   makeSlot("1.0.4", "1.0.4", "11.0");
   makeSlot("1.10.0", "1.10.0", "11.0");
   makeSlot("1.2.0", "1.2.0", "11.0");

   // Numeric, not lexical: 1.10.0 is newer than 1.2.0.
   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.10.0")));
}

TEST_F(ChatSelector, PrefersTheLatestReinstallOfAVersion)
{
   // Both slots hold 1.1.0; the reinstalled one is the later ordinal.
   makeSlot("1.1.0", "1.1.0", "11.0");
   makeSlot("1.1.0-2", "1.1.0", "11.0");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0-2")));
}

TEST_F(ChatSelector, OrdersReinstallsNumericallyPastNine)
{
   // Comparing names as strings puts "1.1.0-9" above "1.1.0-10", which would
   // hand back the slot a user had just reinstalled to get away from.
   makeSlot("1.1.0", "1.1.0", "11.0");
   makeSlot("1.1.0-9", "1.1.0", "11.0");
   makeSlot("1.1.0-10", "1.1.0", "11.0");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0-10")));
}

TEST_F(ChatSelector, RanksPrereleasesByTheirReleaseVersion)
{
   // SemanticVersion::parse rejects a version with a prerelease suffix, so
   // ordering has to compare the release portion or 1.2.0-beta.1 ranks below
   // every version that happens to parse.
   makeSlot("0.0.1", "0.0.1", "11.0");
   makeSlot("1.2.0-beta.1", "1.2.0-beta.1", "11.0");

   EXPECT_TRUE(
      resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.2.0-beta.1")));
}

TEST_F(ChatSelector, PrefersAReleaseOverItsPrerelease)
{
   makeSlot("1.2.0-beta.1", "1.2.0-beta.1", "11.0");
   makeSlot("1.2.0", "1.2.0", "11.0");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.2.0")));
}

TEST_F(ChatSelector, PrefersTheLatestReinstallOfAPrerelease)
{
   makeSlot("1.2.0-beta.1", "1.2.0-beta.1", "11.0");
   makeSlot("1.2.0-beta.1-2", "1.2.0-beta.1", "11.0");

   EXPECT_TRUE(
      resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.2.0-beta.1-2")));
}

TEST_F(ChatSelector, FallsBackOnlyToSlotsServingTheProtocol)
{
   makeSlot("2.0.0", "2.0.0", "12.0");
   makeSlot("1.1.0", "1.1.0", "11.0");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
   EXPECT_TRUE(resolveSlot(storageDir_, "12.0").isEquivalentTo(slot("2.0.0")));
}

TEST_F(ChatSelector, FallsBackOnlyToSlotsThatVerify)
{
   makeDamagedSlot("1.2.0", "11.0");
   makeSlot("1.1.0", "1.1.0", "11.0");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
}

TEST_F(ChatSelector, IgnoresASelectionNamingAStagingDirectory)
{
   // A staging directory holds a complete tree and so verifies, but its owner
   // renames it away when the install finishes. Resolving one would put the
   // backend in a directory that is about to move.
   makeSlot(".tmp-host-1-abc", "1.2.0", "11.0");
   makeSlot("1.1.0", "1.1.0", "11.0");
   ASSERT_TRUE(rstudio::session::modules::chat::slots::verifySlot(
      slot(".tmp-host-1-abc")));
   writeSelectorFile("{\"selected\":{\"11.0\":\".tmp-host-1-abc\"}}");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
}

TEST_F(ChatSelector, IgnoresASelectionThatTriesToEscapeTheVersionsDirectory)
{
   // completeChildPath() hands back the parent when it rejects an escape, so
   // without a name check this would verify the versions directory itself.
   makeSlot("1.1.0", "1.1.0", "11.0");
   writeSelectorFile("{\"selected\":{\"11.0\":\"../../elsewhere\"}}");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
}

TEST_F(ChatSelector, IgnoresAnEmptySelection)
{
   makeSlot("1.1.0", "1.1.0", "11.0");
   writeSelectorFile("{\"selected\":{\"11.0\":\"\"}}");

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEquivalentTo(slot("1.1.0")));
}

TEST_F(ChatSelector, ResolvesNothingWhenNoSlotServesTheProtocol)
{
   makeSlot("1.1.0", "1.1.0", "11.0");

   EXPECT_TRUE(resolveSlot(storageDir_, "12.0").isEmpty());
   EXPECT_EQ(readSelections(storageDir_).count("12.0"), 0u);
}

TEST_F(ChatSelector, ResolvesNothingBeforeTheFirstInstall)
{
   ASSERT_FALSE(storageDir_.remove());

   EXPECT_TRUE(resolveSlot(storageDir_, "11.0").isEmpty());
   EXPECT_FALSE(storageDir_.exists());
}

} // anonymous namespace
