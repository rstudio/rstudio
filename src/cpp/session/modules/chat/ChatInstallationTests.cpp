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
#include "ChatConstants.hpp"
#include "ChatSelector.hpp"
#include "ChatSlots.hpp"

#include <gtest/gtest.h>
#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Xdg.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::installation;
using namespace rstudio::session::modules::chat::constants;

namespace selector = rstudio::session::modules::chat::selector;
namespace slots = rstudio::session::modules::chat::slots;

namespace {

// Writes the files verifyInstallDir() requires into dir.
void stageInstallation(const FilePath& dir)
{
   dir.completeChildPath(kClientDirPath).ensureDirectory();
   dir.completeChildPath(kServerScriptPath).getParent().ensureDirectory();
   writeStringToFile(dir.completeChildPath(kServerScriptPath), "// mock server script");
   writeStringToFile(
      dir.completeChildPath(kClientDirPath).completeChildPath(kIndexFileName),
      "<html>mock</html>");
}

// Stages an installation that also declares a package version and the
// protocol it was built for, the two inputs the resolver ranks on.
void stageInstallation(const FilePath& dir,
                       const std::string& version,
                       const std::string& protocol = kProtocolVersion)
{
   stageInstallation(dir);
   writeStringToFile(dir.completeChildPath(kPackageJsonFileName),
                     "{\"version\": \"" + version + "\"}");
   writeStringToFile(dir.completeChildPath(kProtocolVersionFileName),
                     "{\"protocol\": \"" + protocol + "\"}");
}

// Stages a compatible installation that declares no version at all, so a
// test can hold compatibility constant and exercise only the version ranking.
void stageVersionlessInstallation(const FilePath& dir)
{
   stageInstallation(dir);
   writeStringToFile(dir.completeChildPath(kProtocolVersionFileName),
                     std::string("{\"protocol\": \"") + kProtocolVersion + "\"}");
}

// A slot exactly as an install leaves it under a storage directory: the
// tree and -- unless a test wants to arrange the selector itself -- a
// selection for the protocol it serves.
FilePath makeSlot(const FilePath& storageDir,
                  const std::string& name,
                  const std::string& version,
                  const std::string& protocol = kProtocolVersion,
                  bool select = true)
{
   FilePath slotDir = slots::versionsDir(storageDir).completeChildPath(name);
   stageInstallation(slotDir, version, protocol);
   if (select)
   {
      EXPECT_FALSE(selector::selectSlot(storageDir, protocol, name));
   }
   return slotDir;
}

} // anonymous namespace

// ============================================================================
// Installation directories
// ============================================================================

TEST(ChatInstallation, VerifyInstallDirReturnsFalseForNonExistentPath)
{
   FilePath nonExistent("/nonexistent/path");
   EXPECT_FALSE(verifyInstallDir(nonExistent));
}

TEST(ChatInstallation, VerifyInstallDirReturnsFalseForIncompleteInstallation)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   // Empty directory is incomplete
   EXPECT_FALSE(verifyInstallDir(tempDir));

   // Create only client dir - still incomplete
   FilePath clientDir = tempDir.completeChildPath(kClientDirPath);
   clientDir.ensureDirectory();
   EXPECT_FALSE(verifyInstallDir(tempDir));

   tempDir.removeIfExists();
}

TEST(ChatInstallation, VerifyInstallDirReturnsTrueForCompleteInstallation)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);

   EXPECT_TRUE(verifyInstallDir(tempDir));

   tempDir.removeIfExists();
}

TEST(ChatInstallation, VerifyInstallDirRejectsAnEmptyServerScript)
{
   // A truncated extraction used to leave a zero-byte main.js that an
   // existence-only check accepted.
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);
   writeStringToFile(tempDir.completeChildPath(kServerScriptPath), "");

   EXPECT_FALSE(verifyInstallDir(tempDir));

   tempDir.removeIfExists();
}

TEST(ChatInstallation, DeclaredVersionIsEmptyForMissingPackageJson)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);

   EXPECT_TRUE(declaredVersion(tempDir).empty());

   tempDir.removeIfExists();
}

TEST(ChatInstallation, DeclaredVersionReadsPackageJson)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir, "1.2.3");

   EXPECT_EQ(declaredVersion(tempDir), "1.2.3");

   tempDir.removeIfExists();
}

TEST(ChatInstallation, DeclaredVersionIsEmptyForUnparseablePackageJson)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);
   writeStringToFile(tempDir.completeChildPath(kPackageJsonFileName), "{not json");

   EXPECT_TRUE(declaredVersion(tempDir).empty());

   tempDir.removeIfExists();
}

TEST(ChatInstallation, DeclaredProtocolIsEmptyForLegacyInstall)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);

   EXPECT_TRUE(declaredProtocol(tempDir).empty());

   tempDir.removeIfExists();
}

TEST(ChatInstallation, DeclaredProtocolReadsProtocolJson)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir, "1.2.3", "10.0");

   EXPECT_EQ(declaredProtocol(tempDir), "10.0");

   tempDir.removeIfExists();
}

// ============================================================================
// verifyDeclaredIdentity
// ============================================================================

class ChatInstallationIdentity : public testing::Test
{
protected:
   void SetUp() override
   {
      FilePath tempPath;
      ASSERT_FALSE(FilePath::tempFilePath(tempPath));
      dir_ = tempPath.completePath("staged");
      ASSERT_FALSE(dir_.ensureDirectory());
   }

   void TearDown() override
   {
      dir_.getParent().removeIfExists();
   }

   FilePath dir_;
};

TEST_F(ChatInstallationIdentity, AcceptsAMatchingPackage)
{
   stageInstallation(dir_, "1.2.0", "11.0");
   EXPECT_FALSE(verifyDeclaredIdentity(dir_, "1.2.0", "11.0"));
}

TEST_F(ChatInstallationIdentity, RejectsAVersionMismatch)
{
   stageInstallation(dir_, "1.2.1", "11.0");
   Error error = verifyDeclaredIdentity(dir_, "1.2.0", "11.0");
   ASSERT_TRUE(error);
   EXPECT_EQ(error.getCode(), boost::system::errc::invalid_argument);
   EXPECT_NE(errorDescription(error).find("'1.2.1'"), std::string::npos);
   EXPECT_NE(errorDescription(error).find("'1.2.0'"), std::string::npos);
}

TEST_F(ChatInstallationIdentity, RejectsAProtocolMismatch)
{
   stageInstallation(dir_, "1.2.0", "12.0");
   Error error = verifyDeclaredIdentity(dir_, "1.2.0", "11.0");
   ASSERT_TRUE(error);
   EXPECT_EQ(error.getCode(), boost::system::errc::invalid_argument);
   EXPECT_NE(errorDescription(error).find("'12.0'"), std::string::npos);
}

TEST_F(ChatInstallationIdentity, RejectsAPackageWithoutProtocolJson)
{
   stageInstallation(dir_);
   writeStringToFile(dir_.completeChildPath(kPackageJsonFileName),
                     "{\"version\": \"1.2.0\"}");
   Error error = verifyDeclaredIdentity(dir_, "1.2.0", "11.0");
   ASSERT_TRUE(error);
   EXPECT_EQ(error.getCode(), boost::system::errc::invalid_argument);
}

TEST_F(ChatInstallationIdentity, RejectsAPackageWithoutPackageJson)
{
   stageVersionlessInstallation(dir_);
   Error error = verifyDeclaredIdentity(dir_, "1.2.0", kProtocolVersion);
   ASSERT_TRUE(error);
   EXPECT_EQ(error.getCode(), boost::system::errc::invalid_argument);
}

// ============================================================================
// Bundled installation
// ============================================================================

TEST(ChatInstallation, BundledPathPrefersBinSubdirectory)
{
   // Linux and Windows layout: the bundle sits beside the session binary.
   FilePath resourceDir;
   FilePath::tempFilePath(resourceDir);

   FilePath binDir = resourceDir.completeChildPath("bin")
                                .completeChildPath(kBundledPositAiDirName);
   stageInstallation(binDir);

   EXPECT_EQ(bundledPositAssistantInstallPath(resourceDir), binDir);

   resourceDir.removeIfExists();
}

TEST(ChatInstallation, BundledPathFallsBackToResourceRoot)
{
   // macOS app bundle layout: the bundle sits next to bin/, not inside it.
   FilePath resourceDir;
   FilePath::tempFilePath(resourceDir);
   resourceDir.ensureDirectory();

   FilePath rootDir = resourceDir.completeChildPath(kBundledPositAiDirName);

   EXPECT_EQ(bundledPositAssistantInstallPath(resourceDir), rootDir);

   resourceDir.removeIfExists();
}

TEST(ChatInstallation, BundledPathSkipsIncompleteBinSubdirectory)
{
   // A bin candidate that exists but holds no installation must not mask a
   // bundle at the other location.
   FilePath resourceDir;
   FilePath::tempFilePath(resourceDir);
   resourceDir.completeChildPath("bin")
              .completeChildPath(kBundledPositAiDirName)
              .ensureDirectory();

   FilePath rootDir = resourceDir.completeChildPath(kBundledPositAiDirName);
   stageInstallation(rootDir);

   EXPECT_EQ(bundledPositAssistantInstallPath(resourceDir), rootDir);

   resourceDir.removeIfExists();
}

// ============================================================================
// Resolution
// ============================================================================

namespace {

// The unversioned directory beneath a storage root that older releases
// installed into. Named here rather than shared with the resolver, which no
// longer reads it anywhere.
const char* const kLegacyDirName = "bin";

// Every source under a fresh temp root, none of them staged, and
// posit-assistant-path unset. Each test stages only the sources it needs.
class ChatInstallationSearch : public testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(root_));
      ASSERT_FALSE(root_.ensureDirectory());

      paths_.userStorageDir = root_.completeChildPath("user");
      paths_.bundledPath = root_.completeChildPath("bundled");
      adminDir_ = root_.completeChildPath("admin");
   }

   void TearDown() override
   {
      root_.removeIfExists();
   }

   // With a user slot and a newer bundled copy on hand, sets
   // posit-assistant-path to whatever adminDir_ holds now, which is expected
   // to be no valid installation: the bundled copy must not stand in for it,
   // and only the user's own slot may.
   void expectAdminDirHoldsNothing()
   {
      stageInstallation(paths_.bundledPath, "9.0.0");
      FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
      paths_.adminDir = adminDir_;

      EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);

      paths_.userInstallEnabled = false;
      EXPECT_TRUE(locatePositAssistantInstallation(paths_).isEmpty());
   }

   FilePath root_;
   FilePath adminDir_;
   InstallSearchPaths paths_;
};

} // anonymous namespace

// posit-assistant-path unset ------------------------------------------------

TEST_F(ChatInstallationSearch, PrefersNewestInstallation)
{
   FilePath slot = makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");
   stageInstallation(paths_.bundledPath, "1.5.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, PrefersNewerBundledOverStaleUserSlot)
{
   // A per-user install made before a newer bundle shipped must not shadow
   // it indefinitely.
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(paths_.bundledPath, "1.1.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), paths_.bundledPath);
}

TEST_F(ChatInstallationSearch, BreaksVersionTiesBySource)
{
   FilePath userSlot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(paths_.bundledPath, "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), userSlot);

   ASSERT_FALSE(userSlot.remove());
   EXPECT_EQ(locatePositAssistantInstallation(paths_), paths_.bundledPath);
}

TEST_F(ChatInstallationSearch, PrefersCompatibleProtocolOverHigherVersion)
{
   // A newer package built for another protocol cannot be run by this build,
   // so an older compatible one outranks it.
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(paths_.bundledPath, "9.9.9", "99.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, UserSlotsContributeOnlyThisProtocol)
{
   // The selector is keyed by protocol, so after an RStudio protocol bump the
   // user's slot for the old protocol is not a candidate at all -- the user
   // tier reads as not installed rather than as an incompatible install.
   makeSlot(paths_.userStorageDir, "9.9.9", "9.9.9", "99.0");

   EXPECT_TRUE(locatePositAssistantInstallation(paths_).isEmpty());

   stageInstallation(paths_.bundledPath, "1.0.0");
   EXPECT_EQ(locatePositAssistantInstallation(paths_), paths_.bundledPath);
}

TEST_F(ChatInstallationSearch, FollowsTheUserSelector)
{
   // One candidate per source: the selected slot, not the newest slot.
   makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");
   FilePath selected = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), selected);
}

TEST_F(ChatInstallationSearch, RepairsTheUserSelector)
{
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0", kProtocolVersion, false);
   ASSERT_FALSE(selector::selectSlot(paths_.userStorageDir, kProtocolVersion, "9.9.9"));

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
   EXPECT_EQ(selector::readSelections(paths_.userStorageDir)[kProtocolVersion], "1.0.0");
}

TEST_F(ChatInstallationSearch, IgnoresTheUsersLegacyDirectory)
{
   // Older RStudio releases still mutate pai/bin, so reading it would
   // re-import the corruption the slot layout removes.
   stageInstallation(paths_.userStorageDir.completeChildPath(kLegacyDirName), "9.9.9");

   EXPECT_TRUE(locatePositAssistantInstallation(paths_).isEmpty());
}

TEST_F(ChatInstallationSearch, NeverReadsTheSystemConfigDirectory)
{
   // <systemConfigDir>/pai was once searched without any configuration; an
   // administrator-provided copy now exists only where posit-assistant-path
   // names one. The session's own sources are resolved here, redirected into
   // the temp root so the machine's real directories are not read or
   // repaired.
   rstudio::core::system::EnvironmentScope config(
      "RSTUDIO_CONFIG_DIR", root_.completeChildPath("config").getAbsolutePath().c_str());
   rstudio::core::system::EnvironmentScope data(
      "RSTUDIO_DATA_HOME", root_.completeChildPath("data").getAbsolutePath().c_str());

   FilePath systemDir =
      rstudio::core::system::xdg::systemConfigDir().completePath(kPositAiStorageDirName);
   ASSERT_TRUE(systemDir.isWithin(root_));
   makeSlot(systemDir, "1.0.0", "1.0.0");
   stageInstallation(systemDir.completeChildPath(kLegacyDirName), "1.0.0");

   InstallSearchPaths paths = positAssistantSearchPaths();
   ASSERT_TRUE(paths.userStorageDir.isWithin(root_));
   ASSERT_TRUE(paths.adminDir.isEmpty());
   paths.bundledPath = paths_.bundledPath;

   EXPECT_TRUE(locatePositAssistantInstallation(paths).isEmpty());
}

TEST_F(ChatInstallationSearch, FallsBackToBundledInstallation)
{
   stageInstallation(paths_.bundledPath);

   EXPECT_EQ(locatePositAssistantInstallation(paths_), paths_.bundledPath);
}

TEST_F(ChatInstallationSearch, ReturnsEmptyWhenNothingIsInstalled)
{
   EXPECT_TRUE(locatePositAssistantInstallation(paths_).isEmpty());
}

// posit-assistant-path set --------------------------------------------------

TEST_F(ChatInstallationSearch, FollowsTheAdminSelector)
{
   // An administrator holding users on an older slot beside a newer one.
   makeSlot(adminDir_, "2.0.0", "2.0.0");
   FilePath selected = makeSlot(adminDir_, "1.0.0", "1.0.0");
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), selected);
}

TEST_F(ChatInstallationSearch, ResolvesAroundAStaleAdminSelectorWithoutRewritingIt)
{
   makeSlot(adminDir_, "1.0.0", "1.0.0", kProtocolVersion, false);
   FilePath newest = makeSlot(adminDir_, "2.0.0", "2.0.0", kProtocolVersion, false);
   ASSERT_FALSE(selector::selectSlot(adminDir_, kProtocolVersion, "9.9.9"));
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), newest);
   EXPECT_EQ(selector::readSelections(adminDir_)[kProtocolVersion], "9.9.9");
}

TEST_F(ChatInstallationSearch, ResolvesTheNewestAdminSlotWithoutCreatingASelector)
{
   makeSlot(adminDir_, "1.0.0", "1.0.0", kProtocolVersion, false);
   FilePath newest = makeSlot(adminDir_, "2.0.0", "2.0.0", kProtocolVersion, false);
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), newest);
   EXPECT_TRUE(selector::readSelections(adminDir_).empty());
}

TEST_F(ChatInstallationSearch, UsesAnUnversionedAdminInstallation)
{
   // The layout posit-assistant-path took before it read slots.
   stageInstallation(adminDir_, "1.0.0");
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), adminDir_);
}

TEST_F(ChatInstallationSearch, DoesNotReadTheAdminBinDirectory)
{
   stageInstallation(adminDir_.completeChildPath(kLegacyDirName), "1.0.0");
   expectAdminDirHoldsNothing();
}

TEST_F(ChatInstallationSearch, AdminSlotAndUnversionedInstallRaceByVersion)
{
   makeSlot(adminDir_, "1.0.0", "1.0.0");
   stageInstallation(adminDir_, "2.0.0");
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), adminDir_);
}

TEST_F(ChatInstallationSearch, AdminSlotWinsATieWithTheUnversionedInstall)
{
   FilePath slot = makeSlot(adminDir_, "1.0.0", "1.0.0");
   stageInstallation(adminDir_, "1.0.0");
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, PrefersCompatibleAdminSlotOverHigherVersionedMismatch)
{
   FilePath slot = makeSlot(adminDir_, "1.0.0", "1.0.0");
   stageInstallation(adminDir_, "9.9.9", "99.0");
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, TreatsMissingProtocolFileAsIncompatible)
{
   FilePath slot = makeSlot(adminDir_, "1.0.0", "1.0.0");
   stageInstallation(adminDir_);
   writeStringToFile(adminDir_.completeChildPath(kPackageJsonFileName),
                     "{\"version\": \"9.9.9\"}");
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, AdminInstallationWinsOverNewerUserSlotAndBundledCopy)
{
   // posit-assistant-path is the administrator's explicit choice, so it does
   // not race the other sources by version -- including when that holds
   // users below the version RStudio ships.
   makeSlot(paths_.userStorageDir, "3.0.0", "3.0.0");
   stageInstallation(paths_.bundledPath, "2.0.0");
   FilePath slot = makeSlot(adminDir_, "1.0.0", "1.0.0");
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);

   paths_.userInstallEnabled = false;
   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, RunsAnAdminInstallationWithAMismatchedProtocol)
{
   // The session reports the mismatch rather than quietly running a copy the
   // administrator did not choose.
   makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");
   stageInstallation(paths_.bundledPath, "2.0.0");
   stageInstallation(adminDir_, "1.0.0", "99.0");
   paths_.adminDir = adminDir_;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), adminDir_);
}

TEST_F(ChatInstallationSearch, FallsBackOnlyToUserSlotWhenAdminDirDoesNotExist)
{
   expectAdminDirHoldsNothing();
}

TEST_F(ChatInstallationSearch, FallsBackOnlyToUserSlotWhenAdminDirIsEmpty)
{
   ASSERT_FALSE(adminDir_.ensureDirectory());
   expectAdminDirHoldsNothing();
}

TEST_F(ChatInstallationSearch, FallsBackOnlyToUserSlotWhenAdminSlotsServeAnotherProtocol)
{
   makeSlot(adminDir_, "9.9.9", "9.9.9", "99.0");
   expectAdminDirHoldsNothing();
}

// user installation disabled -------------------------------------------------

TEST_F(ChatInstallationSearch, SkipsUserSlotsWhenInstallsAreManaged)
{
   makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");
   stageInstallation(paths_.bundledPath, "1.0.0");
   paths_.userInstallEnabled = false;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), paths_.bundledPath);
}

TEST_F(ChatInstallationSearch, LeavesTheUserSelectorAloneWhenInstallsAreManaged)
{
   // Ignoring the user's directory includes not repairing its selector.
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0", kProtocolVersion, false);
   stageInstallation(paths_.bundledPath, "1.0.0");
   paths_.userInstallEnabled = false;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), paths_.bundledPath);
   EXPECT_TRUE(selector::readSelections(paths_.userStorageDir).empty());
}

TEST_F(ChatInstallationSearch, FindsBundledInstallationWhenInstallsAreManaged)
{
   stageInstallation(paths_.bundledPath, "1.0.0");
   paths_.userInstallEnabled = false;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), paths_.bundledPath);
}

TEST_F(ChatInstallationSearch, FindsAdminInstallationWhenInstallsAreManaged)
{
   stageInstallation(adminDir_, "1.0.0");
   paths_.adminDir = adminDir_;
   paths_.userInstallEnabled = false;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), adminDir_);
}

TEST_F(ChatInstallationSearch, ReturnsEmptyWhenOnlyUserSlotExistsAndInstallsAreManaged)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   paths_.userInstallEnabled = false;

   EXPECT_TRUE(locatePositAssistantInstallation(paths_).isEmpty());
}

// ============================================================================
// userInstallWouldBeSelected
// ============================================================================

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedWhenNothingIsInstalled)
{
   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "1.0.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedOverTheUserSlotItReplaces)
{
   // The existing user slot stops being selected when the new one is; a
   // downgrade offered by the manifest is still installable.
   makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");

   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "1.0.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldNotBeSelectedBelowNewerReadOnlyInstall)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(paths_.bundledPath, "2.0.0");

   EXPECT_FALSE(userInstallWouldBeSelected(paths_, "1.5.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedAboveOlderReadOnlyInstall)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(paths_.bundledPath, "1.2.0");

   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "1.5.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedOverIncompatibleReadOnlyInstall)
{
   stageInstallation(paths_.bundledPath, "9.9.9", "99.0");

   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "1.0.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedOverVersionlessReadOnlyInstall)
{
   // A copy that cannot say what it is must not claim to be newer than one
   // that can.
   stageVersionlessInstallation(paths_.bundledPath);

   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "0.0.1"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldNotBeSelectedUnderValidAdminDir)
{
   stageInstallation(adminDir_, "0.0.1");
   paths_.adminDir = adminDir_;

   EXPECT_FALSE(userInstallWouldBeSelected(paths_, "9.9.9"));

   ASSERT_FALSE(adminDir_.remove());
   makeSlot(adminDir_, "0.0.1", "0.0.1");

   EXPECT_FALSE(userInstallWouldBeSelected(paths_, "9.9.9"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedWhenAdminDirHoldsNothing)
{
   // The bundled copy is not consulted while posit-assistant-path is set, so
   // it does not block the offer either.
   stageInstallation(paths_.bundledPath, "9.0.0");
   paths_.adminDir = adminDir_;

   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "1.0.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldNotBeSelectedWhenInstallsAreManaged)
{
   paths_.userInstallEnabled = false;

   EXPECT_FALSE(userInstallWouldBeSelected(paths_, "1.0.0"));
}

// ============================================================================
// The held resolution
// ============================================================================

namespace {

// Points the session's resolution at the same temp root, and restores the
// session's own sources afterwards so no later test resolves against a
// directory that is about to be deleted.
class ChatInstallationHeld : public ChatInstallationSearch
{
protected:
   void SetUp() override
   {
      ChatInstallationSearch::SetUp();
      setSearchPathsForTesting(paths_);
   }

   void TearDown() override
   {
      setSearchPathsForTesting(boost::none);
      ChatInstallationSearch::TearDown();
   }

   // Re-points the resolution at the fixture's paths after a test changed
   // them. Discards the held answer, like any change of sources.
   void applyPaths()
   {
      setSearchPathsForTesting(paths_);
   }
};

} // anonymous namespace

TEST_F(ChatInstallationHeld, HoldsTheFirstResolution)
{
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   ASSERT_EQ(locatePositAssistantInstallation(), slot);

   // An install made elsewhere -- here a newer copy that would outrank the
   // held one -- takes effect at the next session start, not now.
   stageInstallation(paths_.bundledPath, "2.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(), slot);
   EXPECT_EQ(getInstalledVersion(), "1.0.0");
}

TEST_F(ChatInstallationHeld, ClearingTheResolutionResolvesAgain)
{
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   ASSERT_EQ(locatePositAssistantInstallation(), slot);

   // What this session's own install does once it has published a slot.
   FilePath newer = makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");
   clearPinnedInstallation();

   EXPECT_EQ(locatePositAssistantInstallation(), newer);
   EXPECT_EQ(getInstalledVersion(), "2.0.0");
}

TEST_F(ChatInstallationHeld, ResolvesAgainWhenTheHeldInstallationIsGone)
{
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(paths_.bundledPath, "0.5.0");
   ASSERT_EQ(locatePositAssistantInstallation(), slot);

   // Removed out of band: the answer is re-checked on each read, so the
   // requests are served by whatever other source still holds a copy.
   ASSERT_FALSE(slot.remove());

   EXPECT_EQ(locatePositAssistantInstallation(), paths_.bundledPath);
}

TEST_F(ChatInstallationHeld, DoesNotHoldAnEmptyResolution)
{
   ASSERT_TRUE(locatePositAssistantInstallation().isEmpty());
   EXPECT_TRUE(getInstalledVersion().empty());

   // While nothing is installed there is nothing to keep consistent, so an
   // install landing from anywhere is seen on the next read.
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(), slot);
}

TEST_F(ChatInstallationHeld, ReportsTheHeldInstallationsProtocol)
{
   stageInstallation(paths_.bundledPath, "1.0.0", "99.0");
   ASSERT_EQ(locatePositAssistantInstallation(), paths_.bundledPath);

   EXPECT_EQ(getInstalledProtocolVersion(), "99.0");
   EXPECT_EQ(getInstalledVersion(), "1.0.0");
}

TEST_F(ChatInstallationHeld, ReportsNothingForALegacyInstallWithoutProtocol)
{
   stageInstallation(adminDir_);
   paths_.adminDir = adminDir_;
   applyPaths();
   ASSERT_EQ(locatePositAssistantInstallation(), adminDir_);

   EXPECT_TRUE(getInstalledProtocolVersion().empty());
   EXPECT_TRUE(getInstalledVersion().empty());
}

TEST_F(ChatInstallationHeld, RunsUserSlotWhenTheUsersSlotIsHeld)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(paths_.bundledPath, "1.0.0");

   EXPECT_TRUE(runsUserSlot());
}

TEST_F(ChatInstallationHeld, DoesNotRunUserSlotForReadOnlyCopies)
{
   // Each read-only source in turn, with the same version in no user slot:
   // a reinstall would add a user copy rather than repair this one.
   stageInstallation(paths_.bundledPath, "1.0.0");
   EXPECT_FALSE(runsUserSlot());

   stageInstallation(adminDir_, "1.0.0");
   paths_.adminDir = adminDir_;
   applyPaths();
   EXPECT_FALSE(runsUserSlot());

   ASSERT_FALSE(adminDir_.remove());
   makeSlot(adminDir_, "1.0.0", "1.0.0");
   applyPaths();
   EXPECT_FALSE(runsUserSlot());
}

TEST_F(ChatInstallationHeld, DoesNotRunUserSlotWhenAReadOnlyCopyOutranksIt)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(paths_.bundledPath, "2.0.0");

   EXPECT_FALSE(runsUserSlot());
}

TEST_F(ChatInstallationHeld, DoesNotRunUserSlotWhenNothingIsInstalled)
{
   EXPECT_FALSE(runsUserSlot());
}

TEST_F(ChatInstallationHeld, ChangingTheSourcesDiscardsTheHeldResolution)
{
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   ASSERT_EQ(locatePositAssistantInstallation(), slot);

   paths_.userInstallEnabled = false;
   applyPaths();

   EXPECT_TRUE(locatePositAssistantInstallation().isEmpty());
}
