/*
 * ChatInstallationTests.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "ChatInstallation.hpp"

#include <map>
#include <string>

#include <gtest/gtest.h>

#include "ChatConstants.hpp"
#include "ChatSelector.hpp"
#include "ChatSlotManifest.hpp"
#include "ChatSlots.hpp"

#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::installation;
using namespace rstudio::session::modules::chat::constants;

namespace slots = rstudio::session::modules::chat::slots;
namespace selector = rstudio::session::modules::chat::selector;

namespace {

// The environment variables the three resolver sources are driven by. Both XDG
// directories have an RStudio-specific override that resolveXdgPath() takes
// verbatim, which is what makes the user and system sources reachable from a
// test at all.
const char* const kOverrideVar = "RSTUDIO_POSIT_AI_PATH";
const char* const kDataHomeVar = "RSTUDIO_DATA_HOME";
const char* const kSystemConfigVar = "RSTUDIO_CONFIG_DIR";

class ChatInstallation : public testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(root_));
      ASSERT_FALSE(root_.ensureDirectory());

      saved_[kOverrideVar] = system::getenv(kOverrideVar);
      saved_[kDataHomeVar] = system::getenv(kDataHomeVar);
      saved_[kSystemConfigVar] = system::getenv(kSystemConfigVar);

      // Every source starts pointing somewhere empty, so a test that arranges
      // one source is testing that source alone -- including on a developer
      // machine that has Posit Assistant genuinely installed.
      system::unsetenv(kOverrideVar);
      dataHome_ = root_.completeChildPath("data-home");
      systemConfig_ = root_.completeChildPath("system-config");
      ASSERT_FALSE(dataHome_.ensureDirectory());
      ASSERT_FALSE(systemConfig_.ensureDirectory());
      system::setenv(kDataHomeVar, dataHome_.getAbsolutePath());
      system::setenv(kSystemConfigVar, systemConfig_.getAbsolutePath());

      clearPinnedInstallation();
   }

   void TearDown() override
   {
      for (const auto& saved : saved_)
      {
         if (saved.second.empty())
            system::unsetenv(saved.first);
         else
            system::setenv(saved.first, saved.second);
      }

      // The pin outlives the test process otherwise, and the redirected
      // directories it points at are about to be deleted.
      clearPinnedInstallation();
      root_.removeIfExists();
   }

   void writeFile(const FilePath& filePath, const std::string& content)
   {
      ASSERT_FALSE(filePath.getParent().ensureDirectory());
      ASSERT_FALSE(writeStringToFile(filePath, content));
   }

   // The tree a Posit Assistant package extracts to, in miniature.
   void writeInstallFiles(const FilePath& dir,
                          const std::string& version,
                          const std::string& protocol)
   {
      writeFile(dir.completeChildPath(kServerScriptPath), "console.log('hi');");
      writeFile(dir.completeChildPath(kClientDirPath)
                   .completeChildPath(kIndexFileName),
                "<html></html>");
      writeFile(dir.completeChildPath(kPackageJsonFileName),
                "{\"version\":\"" + version + "\"}");
      writeFile(dir.completeChildPath(kProtocolVersionFileName),
                "{\"protocol\":\"" + protocol + "\"}");
   }

   // A slot exactly as an install leaves it: the tree plus the install-time
   // manifest, recorded and then selected for the protocol it serves.
   FilePath makeSelectedSlot(const std::string& version,
                             const std::string& protocol)
   {
      FilePath slotDir = slots::versionsDir(storageDir()).completeChildPath(version);
      writeInstallFiles(slotDir, version, protocol);
      EXPECT_FALSE(rstudio::session::modules::chat::slot_manifest::
                      writeSlotManifest(slotDir));
      EXPECT_FALSE(selector::selectSlot(storageDir(), protocol, version));
      return slotDir;
   }

   FilePath storageDir() const
   {
      return dataHome_.completeChildPath(kPositAiStorageDirName);
   }

   FilePath systemInstallDir() const
   {
      return systemConfig_.completePath(kPositAiDirName);
   }

   FilePath overrideDir() const
   {
      return root_.completeChildPath("override");
   }

   void useOverride(const FilePath& dir)
   {
      system::setenv(kOverrideVar, dir.getAbsolutePath());
   }

   FilePath root_;
   FilePath dataHome_;
   FilePath systemConfig_;
   std::map<const char*, std::string> saved_;
};

} // anonymous namespace

// ---------------------------------------------------------------------------
// Structural verification
// ---------------------------------------------------------------------------

TEST_F(ChatInstallation, VerifyInstallDirRejectsNonExistentPath)
{
   EXPECT_FALSE(verifyInstallDir(root_.completeChildPath("nowhere")));
}

TEST_F(ChatInstallation, VerifyInstallDirRejectsIncompleteInstallation)
{
   FilePath dir = root_.completeChildPath("incomplete");
   ASSERT_FALSE(dir.ensureDirectory());
   EXPECT_FALSE(verifyInstallDir(dir));

   // The client directory alone is not an installation.
   ASSERT_FALSE(dir.completeChildPath(kClientDirPath).ensureDirectory());
   EXPECT_FALSE(verifyInstallDir(dir));
}

TEST_F(ChatInstallation, VerifyInstallDirRejectsEmptyServerScript)
{
   // The regression the old existence-only check let through: a truncated
   // extraction leaves a zero-byte main.js that exists.
   FilePath dir = root_.completeChildPath("truncated");
   writeInstallFiles(dir, "1.0.0", "11.0");
   writeFile(dir.completeChildPath(kServerScriptPath), "");

   EXPECT_FALSE(verifyInstallDir(dir));
}

TEST_F(ChatInstallation, VerifyInstallDirAcceptsCompleteInstallation)
{
   FilePath dir = root_.completeChildPath("complete");
   writeInstallFiles(dir, "1.0.0", "11.0");

   EXPECT_TRUE(verifyInstallDir(dir));
}

// ---------------------------------------------------------------------------
// What an installation declares
// ---------------------------------------------------------------------------

TEST_F(ChatInstallation, DeclaredFieldsComeFromTheInstallationsOwnFiles)
{
   FilePath dir = root_.completeChildPath("declares");
   writeInstallFiles(dir, "1.2.3", "10.0");

   EXPECT_EQ(declaredVersion(dir), "1.2.3");
   EXPECT_EQ(declaredProtocol(dir), "10.0");
}

TEST_F(ChatInstallation, DeclaredFieldsAreEmptyWhenTheFilesAreMissing)
{
   FilePath dir = root_.completeChildPath("bare");
   ASSERT_FALSE(dir.ensureDirectory());

   EXPECT_TRUE(declaredVersion(dir).empty());
   EXPECT_TRUE(declaredProtocol(dir).empty());
}

TEST_F(ChatInstallation, DeclaredFieldsAreEmptyWhenTheFilesAreMalformed)
{
   FilePath dir = root_.completeChildPath("malformed");
   writeFile(dir.completeChildPath(kPackageJsonFileName), "{not json");
   writeFile(dir.completeChildPath(kProtocolVersionFileName), "[]");

   EXPECT_TRUE(declaredVersion(dir).empty());
   EXPECT_TRUE(declaredProtocol(dir).empty());
}

// ---------------------------------------------------------------------------
// Resolution: each source in isolation
// ---------------------------------------------------------------------------

TEST_F(ChatInstallation, ResolvesFromTheEnvironmentOverride)
{
   writeInstallFiles(overrideDir(), "1.0.0", "11.0");
   useOverride(overrideDir());

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             overrideDir().getAbsolutePath());
}

TEST_F(ChatInstallation, ResolvesFromTheSelectedUserSlot)
{
   FilePath slotDir = makeSelectedSlot("1.1.0", kProtocolVersion);

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             slotDir.getAbsolutePath());
}

TEST_F(ChatInstallation, ResolvesFromTheSystemInstall)
{
   writeInstallFiles(systemInstallDir(), "1.0.0", "11.0");

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             systemInstallDir().getAbsolutePath());
}

TEST_F(ChatInstallation, ResolvesToNothingWhenNoSourceHasAnInstallation)
{
   EXPECT_TRUE(locatePositAssistantInstallation().isEmpty());
   EXPECT_TRUE(getInstalledVersion().empty());
   EXPECT_TRUE(getInstalledProtocolVersion().empty());
}

// ---------------------------------------------------------------------------
// Resolution: precedence and fall-through
// ---------------------------------------------------------------------------

TEST_F(ChatInstallation, TheOverrideOutranksBothOtherSources)
{
   writeInstallFiles(overrideDir(), "0.9.0", "11.0");
   useOverride(overrideDir());
   makeSelectedSlot("1.1.0", kProtocolVersion);
   writeInstallFiles(systemInstallDir(), "1.0.0", "11.0");

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             overrideDir().getAbsolutePath());
}

TEST_F(ChatInstallation, TheUserSlotOutranksTheSystemInstall)
{
   FilePath slotDir = makeSelectedSlot("1.1.0", kProtocolVersion);
   writeInstallFiles(systemInstallDir(), "9.9.9", "11.0");

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             slotDir.getAbsolutePath());
}

TEST_F(ChatInstallation, AnUnusableOverrideFallsThroughToTheNextSource)
{
   // Present but not runnable: exactly the case that must not strand a session
   // on a broken development override.
   ASSERT_FALSE(overrideDir().ensureDirectory());
   useOverride(overrideDir());

   FilePath slotDir = makeSelectedSlot("1.1.0", kProtocolVersion);

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             slotDir.getAbsolutePath());
}

TEST_F(ChatInstallation, ASlotServingAnotherProtocolFallsThroughToTheSystemInstall)
{
   // The competing-releases case the versioned layout exists for: a slot
   // installed by an RStudio on a different protocol is not ours to run.
   makeSelectedSlot("2.0.0", "1.0");
   writeInstallFiles(systemInstallDir(), "1.0.0", kProtocolVersion);

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             systemInstallDir().getAbsolutePath());
}

TEST_F(ChatInstallation, ASlotWithoutAManifestFallsThroughToTheSystemInstall)
{
   // A directory that was not left by an install that ran to completion does
   // not verify, however complete its tree looks.
   FilePath slotDir =
      slots::versionsDir(storageDir()).completeChildPath("1.1.0");
   writeInstallFiles(slotDir, "1.1.0", kProtocolVersion);
   ASSERT_FALSE(selector::selectSlot(storageDir(), kProtocolVersion, "1.1.0"));

   writeInstallFiles(systemInstallDir(), "1.0.0", kProtocolVersion);

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             systemInstallDir().getAbsolutePath());
}

TEST_F(ChatInstallation, AnUnversionedSourceNeedsNoManifestOrDeclarations)
{
   // Sources 1 and 3 carry no install manifest and are not named for a
   // version, so they get the structural check and nothing more. A system
   // install predating protocol.json still resolves, and reports the legacy
   // empty protocol the update check already handles.
   FilePath dir = systemInstallDir();
   writeInstallFiles(dir, "1.0.0", "11.0");
   ASSERT_FALSE(dir.completeChildPath(kProtocolVersionFileName).removeIfExists());
   ASSERT_FALSE(dir.completeChildPath(kPackageJsonFileName).removeIfExists());

   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             dir.getAbsolutePath());
   EXPECT_TRUE(getInstalledProtocolVersion().empty());
}

// ---------------------------------------------------------------------------
// Pinning
// ---------------------------------------------------------------------------

TEST_F(ChatInstallation, TheResolutionIsHeldUntilItIsCleared)
{
   FilePath slotDir = makeSelectedSlot("1.1.0", kProtocolVersion);
   ASSERT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             slotDir.getAbsolutePath());
   ASSERT_EQ(getInstalledVersion(), "1.1.0");

   // Another session installs a newer version and selects it. This session
   // keeps running what it resolved.
   FilePath newer = makeSelectedSlot("1.2.0", kProtocolVersion);
   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             slotDir.getAbsolutePath());
   EXPECT_EQ(getInstalledVersion(), "1.1.0");

   // Its own install is what re-resolves it.
   clearPinnedInstallation();
   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             newer.getAbsolutePath());
   EXPECT_EQ(getInstalledVersion(), "1.2.0");
}

TEST_F(ChatInstallation, ReportedVersionAndProtocolComeFromTheResolvedInstall)
{
   // The system install is newer than the selected slot, and the slot is what
   // is being run -- so the slot is what must be reported to the update check.
   makeSelectedSlot("1.1.0", kProtocolVersion);
   writeInstallFiles(systemInstallDir(), "9.9.9", kProtocolVersion);

   EXPECT_EQ(getInstalledVersion(), "1.1.0");
   EXPECT_EQ(getInstalledProtocolVersion(), std::string(kProtocolVersion));
}

// ---------------------------------------------------------------------------
// The install sequence, minus the download
// ---------------------------------------------------------------------------

TEST_F(ChatInstallation, StagingThenAllocatingThenSelectingYieldsAResolvableInstall)
{
   // installPackage() in SessionChat.cpp is this sequence with a download and
   // an unzip in the middle. What is checked here is that its steps compose:
   // a package staged, published and selected is what the resolver hands back.
   FilePath slotsDir = slots::versionsDir(storageDir());
   ASSERT_FALSE(slotsDir.ensureDirectory());

   FilePath stagingDir;
   ASSERT_FALSE(slots::prepareStagingDir(slotsDir, &stagingDir));
   writeInstallFiles(stagingDir, "1.3.0", kProtocolVersion);

   FilePath slotDir;
   ASSERT_FALSE(slots::allocateSlot(
      stagingDir, slots::SlotPolicy::AdoptExisting, &slotDir));

   // The install names the selector entry from the published slot, and selects
   // under the protocol the slot itself declares.
   ASSERT_FALSE(selector::selectSlot(storageDir(),
                                     declaredProtocol(slotDir),
                                     slotDir.getFilename()));

   EXPECT_EQ(declaredVersion(slotDir), "1.3.0");
   EXPECT_EQ(locatePositAssistantInstallation().getAbsolutePath(),
             slotDir.getAbsolutePath());
   EXPECT_EQ(getInstalledVersion(), "1.3.0");
}

// ---------------------------------------------------------------------------
// protocol.json
// ---------------------------------------------------------------------------

TEST_F(ChatInstallation, WriteProtocolVersionFileWritesWhenMissing)
{
   FilePath dir = root_.completeChildPath("proto-missing");
   ASSERT_FALSE(dir.ensureDirectory());

   FilePath protoFile = dir.completeChildPath(kProtocolVersionFileName);
   ASSERT_FALSE(protoFile.exists());

   ASSERT_FALSE(writeProtocolVersionFileIfMissing(dir));
   ASSERT_TRUE(protoFile.exists());

   // The written file records RStudio's compiled-in protocol version.
   EXPECT_EQ(declaredProtocol(dir), std::string(kProtocolVersion));
}

TEST_F(ChatInstallation, WriteProtocolVersionFilePreservesPackageProvidedFile)
{
   FilePath dir = root_.completeChildPath("proto-present");

   // Simulate a package that bundled its own protocol.json.
   std::string packageContent = "{\"protocol\": \"99.0\"}";
   writeFile(dir.completeChildPath(kProtocolVersionFileName), packageContent);

   ASSERT_FALSE(writeProtocolVersionFileIfMissing(dir));

   // The package-provided file is left untouched.
   std::string content;
   ASSERT_FALSE(readStringFromFile(
      dir.completeChildPath(kProtocolVersionFileName), &content));
   EXPECT_EQ(content, packageContent);
}
