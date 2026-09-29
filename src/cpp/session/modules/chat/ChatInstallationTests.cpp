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
// System storage directory
// ============================================================================

TEST(ChatInstallation, SystemStorageDirPrefersBinSubdirectory)
{
   // Linux and Windows layout: the directory sits beside the session binary.
   FilePath resourceDir;
   FilePath::tempFilePath(resourceDir);

   FilePath binDir = resourceDir.completeChildPath("bin")
                                .completeChildPath(kSystemPositAiDirName);
   ASSERT_FALSE(binDir.ensureDirectory());

   EXPECT_EQ(systemStorageDir(resourceDir), binDir);

   resourceDir.removeIfExists();
}

TEST(ChatInstallation, SystemStorageDirFallsBackToResourceRoot)
{
   // macOS app bundle layout: the directory sits next to bin/, not inside it.
   FilePath resourceDir;
   FilePath::tempFilePath(resourceDir);
   ASSERT_FALSE(resourceDir.completeChildPath("bin").ensureDirectory());

   EXPECT_EQ(systemStorageDir(resourceDir),
             resourceDir.completeChildPath(kSystemPositAiDirName));

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

// Every source under a fresh temp root, none of them staged. Each test stages
// only the sources it needs.
class ChatInstallationSearch : public testing::Test
{
protected:
   void SetUp() override
   {
      ASSERT_FALSE(FilePath::tempFilePath(root_));
      ASSERT_FALSE(root_.ensureDirectory());

      paths_.userStorageDir = root_.completeChildPath("user");
      paths_.systemStorageDir = root_.completeChildPath("system");
      bundled_ = paths_.systemStorageDir.completeChildPath(kBundledInstallDirName);
   }

   void TearDown() override
   {
      root_.removeIfExists();
   }

   FilePath root_;
   FilePath bundled_;
   InstallSearchPaths paths_;
};

} // anonymous namespace

// sources competing ----------------------------------------------------------

TEST_F(ChatInstallationSearch, PrefersNewestInstallation)
{
   FilePath slot = makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");
   stageInstallation(bundled_, "1.5.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, PrefersNewerBundledOverStaleUserSlot)
{
   // A per-user install made before a newer bundle shipped must not shadow
   // it indefinitely.
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_, "1.1.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
}

TEST_F(ChatInstallationSearch, PrefersNewerAdminSlotOverUserSlot)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   FilePath adminSlot = makeSlot(paths_.systemStorageDir, "2.0.0", "2.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), adminSlot);
}

TEST_F(ChatInstallationSearch, PrefersNewerUserSlotOverAdminSlot)
{
   // The administrator's selector chooses among their own versions; it does
   // not hold a user who installed a newer one below it.
   FilePath userSlot = makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");
   makeSlot(paths_.systemStorageDir, "1.0.0", "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), userSlot);
}

TEST_F(ChatInstallationSearch, BreaksVersionTiesBySource)
{
   FilePath userSlot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   FilePath adminSlot = makeSlot(paths_.systemStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_, "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), userSlot);

   ASSERT_FALSE(userSlot.remove());
   EXPECT_EQ(locatePositAssistantInstallation(paths_), adminSlot);

   ASSERT_FALSE(adminSlot.remove());
   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
}

TEST_F(ChatInstallationSearch, PrefersCompatibleProtocolOverHigherVersion)
{
   // A newer package built for another protocol cannot be run by this build,
   // so an older compatible one outranks it.
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_, "9.9.9", "99.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, TreatsMissingProtocolFileAsIncompatible)
{
   FilePath slot = makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_);
   writeStringToFile(bundled_.completeChildPath(kPackageJsonFileName),
                     "{\"version\": \"9.9.9\"}");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), slot);
}

TEST_F(ChatInstallationSearch, UserSlotsContributeOnlyThisProtocol)
{
   // The selector is keyed by protocol, so after an RStudio protocol bump the
   // user's slot for the old protocol is not a candidate at all -- the user
   // tier reads as not installed rather than as an incompatible install.
   makeSlot(paths_.userStorageDir, "9.9.9", "9.9.9", "99.0");

   EXPECT_TRUE(locatePositAssistantInstallation(paths_).isEmpty());

   stageInstallation(bundled_, "1.0.0");
   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
}

TEST_F(ChatInstallationSearch, AdminSlotsContributeOnlyThisProtocol)
{
   // What a Workbench upgrade to a new protocol leaves behind: the
   // administrator's versions for the old protocol, and a bundle for the new.
   makeSlot(paths_.systemStorageDir, "9.9.9", "9.9.9", "99.0");
   stageInstallation(bundled_, "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
}

// the user's selector --------------------------------------------------------

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

// the administrator's selector -----------------------------------------------

TEST_F(ChatInstallationSearch, FollowsTheAdminSelector)
{
   // An administrator rolling back to an older version beside a newer one.
   makeSlot(paths_.systemStorageDir, "2.0.0", "2.0.0");
   FilePath selected = makeSlot(paths_.systemStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_, "0.5.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), selected);
}

TEST_F(ChatInstallationSearch, AdminSelectionDoesNotHoldBackANewerBundledCopy)
{
   // A selection left by an upgrade made before a newer Workbench release
   // must not keep running over that release's bundle.
   makeSlot(paths_.systemStorageDir, "2.0.0", "2.0.0");
   makeSlot(paths_.systemStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_, "1.5.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
}

TEST_F(ChatInstallationSearch, ResolvesAroundAStaleAdminSelectorWithoutRewritingIt)
{
   makeSlot(paths_.systemStorageDir, "1.0.0", "1.0.0", kProtocolVersion, false);
   FilePath newest =
      makeSlot(paths_.systemStorageDir, "2.0.0", "2.0.0", kProtocolVersion, false);
   ASSERT_FALSE(selector::selectSlot(paths_.systemStorageDir, kProtocolVersion, "9.9.9"));

   EXPECT_EQ(locatePositAssistantInstallation(paths_), newest);
   EXPECT_EQ(selector::readSelections(paths_.systemStorageDir)[kProtocolVersion], "9.9.9");
}

TEST_F(ChatInstallationSearch, ResolvesTheNewestAdminSlotWithoutCreatingASelector)
{
   makeSlot(paths_.systemStorageDir, "1.0.0", "1.0.0", kProtocolVersion, false);
   FilePath newest =
      makeSlot(paths_.systemStorageDir, "2.0.0", "2.0.0", kProtocolVersion, false);

   EXPECT_EQ(locatePositAssistantInstallation(paths_), newest);
   EXPECT_TRUE(selector::readSelections(paths_.systemStorageDir).empty());
}

TEST_F(ChatInstallationSearch, ReadsOnlyTheBundledCopyAndTheAdminSlots)
{
   // The system directory itself was the bundle's layout through 2026.09,
   // and bin/ is the unversioned layout of a storage root; neither is read.
   stageInstallation(paths_.systemStorageDir, "9.9.9");
   stageInstallation(paths_.systemStorageDir.completeChildPath(kLegacyDirName), "9.9.9");
   stageInstallation(paths_.systemStorageDir.completeChildPath("9.9.9"), "9.9.9");

   EXPECT_TRUE(locatePositAssistantInstallation(paths_).isEmpty());
}

TEST_F(ChatInstallationSearch, NeverReadsTheSystemConfigDirectory)
{
   // <systemConfigDir>/pai was once searched as the administrator's storage
   // directory; the one beside the session binary replaces it. The session's
   // own sources are resolved here, redirected into the temp root so the
   // machine's real directories are not read or repaired.
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
   paths.systemStorageDir = paths_.systemStorageDir;

   EXPECT_TRUE(locatePositAssistantInstallation(paths).isEmpty());
}

TEST_F(ChatInstallationSearch, FallsBackToBundledInstallation)
{
   stageInstallation(bundled_);

   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
}

TEST_F(ChatInstallationSearch, ReturnsEmptyWhenNothingIsInstalled)
{
   EXPECT_TRUE(locatePositAssistantInstallation(paths_).isEmpty());
}

// user installation disabled -------------------------------------------------

TEST_F(ChatInstallationSearch, SkipsUserSlotsWhenInstallsAreManaged)
{
   makeSlot(paths_.userStorageDir, "2.0.0", "2.0.0");
   stageInstallation(bundled_, "1.0.0");
   paths_.userInstallEnabled = false;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
}

TEST_F(ChatInstallationSearch, LeavesTheUserSelectorAloneWhenInstallsAreManaged)
{
   // Ignoring the user's directory includes not repairing its selector.
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0", kProtocolVersion, false);
   stageInstallation(bundled_, "1.0.0");
   paths_.userInstallEnabled = false;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
   EXPECT_TRUE(selector::readSelections(paths_.userStorageDir).empty());
}

TEST_F(ChatInstallationSearch, FindsBundledInstallationWhenInstallsAreManaged)
{
   stageInstallation(bundled_, "1.0.0");
   paths_.userInstallEnabled = false;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), bundled_);
}

TEST_F(ChatInstallationSearch, FindsAdminSlotWhenInstallsAreManaged)
{
   FilePath adminSlot = makeSlot(paths_.systemStorageDir, "1.0.0", "1.0.0");
   paths_.userInstallEnabled = false;

   EXPECT_EQ(locatePositAssistantInstallation(paths_), adminSlot);
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
   stageInstallation(bundled_, "2.0.0");

   EXPECT_FALSE(userInstallWouldBeSelected(paths_, "1.5.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedAboveOlderReadOnlyInstall)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_, "1.2.0");

   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "1.5.0"));
}

TEST_F(ChatInstallationSearch, UserInstallRanksAgainstTheAdminSlot)
{
   makeSlot(paths_.systemStorageDir, "2.0.0", "2.0.0");

   EXPECT_FALSE(userInstallWouldBeSelected(paths_, "1.5.0"));
   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "2.0.0"));
   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "2.5.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedOverIncompatibleReadOnlyInstall)
{
   stageInstallation(bundled_, "9.9.9", "99.0");

   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "1.0.0"));
}

TEST_F(ChatInstallationSearch, UserInstallWouldBeSelectedOverVersionlessReadOnlyInstall)
{
   // A copy that cannot say what it is must not claim to be newer than one
   // that can.
   stageVersionlessInstallation(bundled_);

   EXPECT_TRUE(userInstallWouldBeSelected(paths_, "0.0.1"));
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
   makeSlot(paths_.systemStorageDir, "2.0.0", "2.0.0");

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
   stageInstallation(bundled_, "0.5.0");
   ASSERT_EQ(locatePositAssistantInstallation(), slot);

   // Removed out of band: the answer is re-checked on each read, so the
   // requests are served by whatever other source still holds a copy.
   ASSERT_FALSE(slot.remove());

   EXPECT_EQ(locatePositAssistantInstallation(), bundled_);
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
   stageInstallation(bundled_, "1.0.0", "99.0");
   ASSERT_EQ(locatePositAssistantInstallation(), bundled_);

   EXPECT_EQ(getInstalledProtocolVersion(), "99.0");
   EXPECT_EQ(getInstalledVersion(), "1.0.0");
}

TEST_F(ChatInstallationHeld, ReportsNothingForALegacyInstallWithoutProtocol)
{
   stageInstallation(bundled_);
   ASSERT_EQ(locatePositAssistantInstallation(), bundled_);

   EXPECT_TRUE(getInstalledProtocolVersion().empty());
   EXPECT_TRUE(getInstalledVersion().empty());
}

TEST_F(ChatInstallationHeld, RunsUserSlotWhenTheUsersSlotIsHeld)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_, "1.0.0");

   EXPECT_TRUE(runsUserSlot());
}

TEST_F(ChatInstallationHeld, DoesNotRunUserSlotForReadOnlyCopies)
{
   // Each read-only source in turn, with no user slot: a reinstall would add
   // a user copy rather than repair this one.
   stageInstallation(bundled_, "1.0.0");
   EXPECT_FALSE(runsUserSlot());

   makeSlot(paths_.systemStorageDir, "2.0.0", "2.0.0");
   applyPaths();
   ASSERT_NE(locatePositAssistantInstallation(), bundled_);
   EXPECT_FALSE(runsUserSlot());
}

TEST_F(ChatInstallationHeld, DoesNotRunUserSlotWhenAReadOnlyCopyOutranksIt)
{
   makeSlot(paths_.userStorageDir, "1.0.0", "1.0.0");
   stageInstallation(bundled_, "2.0.0");

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
