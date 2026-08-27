/*
 * ChatSlotManifestTests.cpp
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

#include "ChatSlotManifest.hpp"

#include <string>

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::slot_manifest;

namespace {

const char* const kManifestFileName = ".slot-manifest.json";

class ChatSlotManifest : public testing::Test
{
protected:
   void SetUp() override
   {
      FilePath tempPath;
      ASSERT_FALSE(FilePath::tempFilePath(tempPath));
      slotDir_ = tempPath.completePath("slot");
      ASSERT_FALSE(slotDir_.ensureDirectory());
   }

   void TearDown() override
   {
      slotDir_.getParent().removeIfExists();
   }

   // Writes `content` at a slot-relative path, creating parent directories.
   void writeFile(const std::string& relativePath, const std::string& content)
   {
      FilePath filePath = slotDir_.completeChildPath(relativePath);
      ASSERT_FALSE(filePath.getParent().ensureDirectory());
      ASSERT_FALSE(writeStringToFile(filePath, content));
   }

   // The tree a package extracts to, in miniature.
   void writeTree()
   {
      writeFile("package.json", "{\"version\":\"1.1.0\"}");
      writeFile("dist/server/main.js", "console.log('hi');");
      writeFile("dist/client/index.html", "<html></html>");
   }

   FilePath slotDir_;
};

TEST_F(ChatSlotManifest, RoundTripsAnInstalledTree)
{
   writeTree();
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   SlotManifest manifest;
   ASSERT_FALSE(readSlotManifest(slotDir_, &manifest));

   EXPECT_EQ(manifest.size(), 3u);
   EXPECT_EQ(manifest["package.json"].size, 19u);
   EXPECT_EQ(manifest["dist/server/main.js"].size, 18u);
   EXPECT_EQ(manifest["dist/client/index.html"].size, 13u);
   EXPECT_TRUE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, RecordsRealSha256)
{
   // Pins that the recorded digest is a SHA-256 of the file's bytes, so the
   // hashes stay usable for the deeper check they are recorded for.
   writeFile("abc.txt", "abc");
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   SlotManifest manifest;
   ASSERT_FALSE(readSlotManifest(slotDir_, &manifest));
   EXPECT_EQ(manifest["abc.txt"].sha256,
             "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
}

TEST_F(ChatSlotManifest, DoesNotRecordItself)
{
   writeTree();
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   SlotManifest manifest;
   ASSERT_FALSE(readSlotManifest(slotDir_, &manifest));
   EXPECT_EQ(manifest.count(kManifestFileName), 0u);
}

TEST_F(ChatSlotManifest, RecordsEmptyFiles)
{
   // A zero-byte file is legitimate inside a package; only the handful of files
   // the backend cannot run without are required to be non-empty, and that is
   // checked elsewhere.
   writeFile("empty", "");
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   SlotManifest manifest;
   ASSERT_FALSE(readSlotManifest(slotDir_, &manifest));
   EXPECT_EQ(manifest.size(), 1u);
   EXPECT_EQ(manifest["empty"].size, 0u);
   EXPECT_TRUE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, DetectsMissingFile)
{
   writeTree();
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   ASSERT_FALSE(slotDir_.completeChildPath("dist/server/main.js").remove());
   EXPECT_FALSE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, DetectsTruncatedFile)
{
   writeTree();
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   writeFile("dist/server/main.js", "");
   EXPECT_FALSE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, DetectsGrownFile)
{
   writeTree();
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   writeFile("dist/server/main.js", "console.log('hi'); // and more");
   EXPECT_FALSE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, DetectsFileReplacedByDirectory)
{
   writeTree();
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   FilePath mainJs = slotDir_.completeChildPath("dist/server/main.js");
   ASSERT_FALSE(mainJs.remove());
   ASSERT_FALSE(mainJs.ensureDirectory());
   EXPECT_FALSE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, IgnoresFilesItDidNotRecord)
{
   // Slots are never modified after install, so an extra file is not evidence
   // that the recorded tree is damaged.
   writeTree();
   ASSERT_FALSE(writeSlotManifest(slotDir_));

   writeFile("dist/server/stray.log", "written by something else");
   EXPECT_TRUE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, RejectsAbsentManifest)
{
   writeTree();

   SlotManifest manifest;
   EXPECT_TRUE(readSlotManifest(slotDir_, &manifest) != Success());
   EXPECT_FALSE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, RejectsUnparseableManifest)
{
   writeTree();
   writeFile(kManifestFileName, "not json");

   SlotManifest manifest;
   EXPECT_TRUE(readSlotManifest(slotDir_, &manifest) != Success());
   EXPECT_FALSE(matchesSlotManifest(slotDir_));
}

TEST_F(ChatSlotManifest, RejectsManifestWithoutFiles)
{
   writeTree();
   writeFile(kManifestFileName, "{}");

   SlotManifest manifest;
   EXPECT_TRUE(readSlotManifest(slotDir_, &manifest) != Success());
}

TEST_F(ChatSlotManifest, RejectsNonObjectEntry)
{
   writeTree();
   writeFile(kManifestFileName, "{\"files\":{\"package.json\":19}}");

   SlotManifest manifest;
   EXPECT_TRUE(readSlotManifest(slotDir_, &manifest) != Success());
}

TEST_F(ChatSlotManifest, RejectsEntryWithoutSize)
{
   writeTree();
   writeFile(kManifestFileName, "{\"files\":{\"package.json\":{\"sha256\":\"ab\"}}}");

   SlotManifest manifest;
   EXPECT_TRUE(readSlotManifest(slotDir_, &manifest) != Success());
}

TEST_F(ChatSlotManifest, RejectsNegativeSize)
{
   writeTree();
   writeFile(kManifestFileName, "{\"files\":{\"package.json\":{\"size\":-1}}}");

   SlotManifest manifest;
   EXPECT_TRUE(readSlotManifest(slotDir_, &manifest) != Success());
}

TEST_F(ChatSlotManifest, RejectsEntryEscapingTheSlot)
{
   // A manifest is only ever written by an install, but it is still read from
   // the user's home directory, so a path that walks out of the slot must not
   // be followed.
   writeTree();
   writeFile(kManifestFileName,
             "{\"files\":{\"../outside.txt\":{\"size\":0,\"sha256\":\"\"}}}");

   EXPECT_FALSE(matchesSlotManifest(slotDir_));
}

} // anonymous namespace
