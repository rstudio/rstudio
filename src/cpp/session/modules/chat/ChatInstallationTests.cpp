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

#include <gtest/gtest.h>
#include <core/FileSerializer.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::installation;
using namespace rstudio::session::modules::chat::constants;

namespace {

// Writes the files verifyPositAiInstallation() requires into dir.
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
   writeStringToFile(dir.completeChildPath("package.json"),
                     "{\"version\": \"" + version + "\"}");
   writeStringToFile(dir.completeChildPath(kProtocolVersionFileName),
                     "{\"protocol\": \"" + protocol + "\"}");
}

} // anonymous namespace

TEST(ChatInstallation, VerifyPositAiInstallationReturnsFalseForNonExistentPath)
{
   FilePath nonExistent("/nonexistent/path");
   EXPECT_FALSE(verifyPositAiInstallation(nonExistent));
}

TEST(ChatInstallation, VerifyPositAiInstallationReturnsFalseForIncompleteInstallation)
{
   // Create temp directory
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   // Empty directory is incomplete
   EXPECT_FALSE(verifyPositAiInstallation(tempDir));

   // Create only client dir - still incomplete
   FilePath clientDir = tempDir.completeChildPath(kClientDirPath);
   clientDir.ensureDirectory();
   EXPECT_FALSE(verifyPositAiInstallation(tempDir));

   // Cleanup
   tempDir.removeIfExists();
}

TEST(ChatInstallation, VerifyPositAiInstallationReturnsTrueForCompleteInstallation)
{
   // Create temp directory structure
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   FilePath clientDir = tempDir.completeChildPath(kClientDirPath);
   clientDir.ensureDirectory();

   FilePath serverScript = tempDir.completeChildPath(kServerScriptPath);
   serverScript.getParent().ensureDirectory();
   writeStringToFile(serverScript, "// mock server script");

   FilePath indexHtml = clientDir.completeChildPath(kIndexFileName);
   writeStringToFile(indexHtml, "<html>mock</html>");

   // Now it should be valid
   EXPECT_TRUE(verifyPositAiInstallation(tempDir));

   // Cleanup
   tempDir.removeIfExists();
}

TEST(ChatInstallation, GetInstalledVersionReturnsEmptyForMissingPackageJson)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);

   // A staged installation without a package.json has no version to report.
   EXPECT_TRUE(getInstalledVersion(tempDir).empty());

   tempDir.removeIfExists();
}

TEST(ChatInstallation, GetInstalledVersionReturnsEmptyForUnlocatedInstallation)
{
   // locatePositAssistantInstallation() returns an empty path when nothing is
   // installed; the version lookup must not treat that as a directory.
   EXPECT_TRUE(getInstalledVersion(FilePath()).empty());
}

TEST(ChatInstallation, GetInstalledVersionExtractsVersionFromPackageJson)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);

   FilePath packageJson = tempDir.completeChildPath("package.json");
   std::string packageContent = R"({
  "name": "@posit/posit-ai",
  "version": "1.2.3",
  "description": "Test package"
})";
   writeStringToFile(packageJson, packageContent);

   EXPECT_EQ(getInstalledVersion(tempDir), "1.2.3");

   tempDir.removeIfExists();
}

TEST(ChatInstallation, GetInstalledProtocolVersionReturnsEmptyForLegacyInstall)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);

   // No protocol file -> should return empty
   EXPECT_TRUE(getInstalledProtocolVersion(tempDir).empty());

   tempDir.removeIfExists();
}

TEST(ChatInstallation, GetInstalledProtocolVersionReturnsCorrectVersion)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   stageInstallation(tempDir);

   writeStringToFile(tempDir.completeChildPath(kProtocolVersionFileName),
                     "{\"protocol\": \"10.0\"}");

   EXPECT_EQ(getInstalledProtocolVersion(tempDir), "10.0");

   tempDir.removeIfExists();
}

TEST(ChatInstallation, WriteProtocolVersionFileWritesWhenMissing)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   FilePath protoFile = tempDir.completeChildPath(kProtocolVersionFileName);
   EXPECT_FALSE(protoFile.exists());

   Error error = writeProtocolVersionFileIfMissing(tempDir);
   EXPECT_FALSE(error);
   EXPECT_TRUE(protoFile.exists());

   // The written file records RStudio's compiled-in protocol version.
   std::string content;
   error = readStringFromFile(protoFile, &content);
   EXPECT_FALSE(error);

   json::Value value;
   Error parseError = value.parse(content);
   EXPECT_FALSE(parseError);
   ASSERT_TRUE(value.isObject());
   EXPECT_EQ(value.getObject()["protocol"].getString(), std::string(kProtocolVersion));

   tempDir.removeIfExists();
}

TEST(ChatInstallation, WriteProtocolVersionFilePreservesPackageProvidedFile)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   // Simulate a package that bundled its own protocol.json.
   FilePath protoFile = tempDir.completeChildPath(kProtocolVersionFileName);
   std::string packageContent = "{\"protocol\": \"99.0\"}";
   writeStringToFile(protoFile, packageContent);

   Error error = writeProtocolVersionFileIfMissing(tempDir);
   EXPECT_FALSE(error);

   // The package-provided file is left untouched.
   std::string content;
   error = readStringFromFile(protoFile, &content);
   EXPECT_FALSE(error);
   EXPECT_EQ(content, packageContent);

   tempDir.removeIfExists();
}

TEST(ChatInstallation, BundledPathPrefersBinSubdirectory)
{
   // Non-Apple layout: the bundle installs into the directory holding the
   // session binary.
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
   // Apple layout: the bundle sits beside bin/ in the app's Resources
   // directory. The fallback is also what open-source builds resolve to,
   // where neither directory exists.
   FilePath resourceDir;
   FilePath::tempFilePath(resourceDir);
   resourceDir.ensureDirectory();

   FilePath expected = resourceDir.completeChildPath(kBundledPositAiDirName);
   EXPECT_EQ(bundledPositAssistantInstallPath(resourceDir), expected);

   resourceDir.removeIfExists();
}

TEST(ChatInstallation, BundledPathSkipsIncompleteBinSubdirectory)
{
   // A partial directory beside the session binary must not mask a usable
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
// Search order
// ============================================================================

namespace {

// Builds a search over three sibling directories under a fresh temp root,
// none of them staged. Each test stages only the tiers it needs.
InstallSearchPaths tempSearchPaths(FilePath* pRoot)
{
   FilePath::tempFilePath(*pRoot);
   pRoot->ensureDirectory();

   InstallSearchPaths paths;
   paths.userDataPath = pRoot->completeChildPath("user");
   paths.systemPath = pRoot->completeChildPath("system");
   paths.bundledPath = pRoot->completeChildPath("bundled");
   return paths;
}

} // anonymous namespace

TEST(ChatInstallation, LocatePrefersNewestInstallation)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath, "1.2.0");
   stageInstallation(paths.systemPath, "1.1.0");
   stageInstallation(paths.bundledPath, "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.userDataPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocatePrefersNewerBundledOverStaleUserInstallation)
{
   // A per-user install made before the bundle existed must not shadow a
   // newer copy shipped with RStudio.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath, "1.0.0");
   stageInstallation(paths.bundledPath, "1.2.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.bundledPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocatePrefersNewerSystemOverStaleUserInstallation)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath, "1.0.0");
   stageInstallation(paths.systemPath, "1.1.0");
   stageInstallation(paths.bundledPath, "0.9.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.systemPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateBreaksVersionTiesByTier)
{
   // Equal versions keep the historical order: user, then system, then bundled.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath, "1.0.0");
   stageInstallation(paths.systemPath, "1.0.0");
   stageInstallation(paths.bundledPath, "1.0.0");
   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.userDataPath);

   paths.userDataPath.removeIfExists();
   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.systemPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateRanksVersionlessInstallationLowest)
{
   // An install with no readable package.json cannot claim to be newer than
   // anything, but is still used when it is all there is.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath);
   stageInstallation(paths.bundledPath, "0.1.0");
   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.bundledPath);

   paths.bundledPath.removeIfExists();
   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.userDataPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocatePrefersCompatibleProtocolOverHigherVersion)
{
   // A user install built for another RStudio's protocol would only resolve
   // to the update-required prompt; a compatible bundle runs instead.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath, "2.0.0", "0.0");
   stageInstallation(paths.bundledPath, "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.bundledPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateTreatsMissingProtocolFileAsIncompatible)
{
   // hasProtocolMismatch() counts a missing protocol.json as a mismatch, so
   // the resolver must rank such an install the same way.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath);
   writeStringToFile(paths.userDataPath.completeChildPath("package.json"),
                     "{\"version\": \"2.0.0\"}");
   stageInstallation(paths.bundledPath, "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.bundledPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateFallsBackToHighestVersionWhenNoneIsCompatible)
{
   // With no compatible candidate the newest still resolves, so the existing
   // protocol-mismatch handling sees the same install it always did.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath, "2.0.0", "0.0");
   stageInstallation(paths.bundledPath, "1.0.0", "0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.userDataPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocatePinnedInstallationWinsOverNewerUserInstallation)
{
   // posit-assistant-path is the administrator's explicit choice, so it is
   // used as-is rather than entering the version race.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.pinnedSystemPath = true;
   stageInstallation(paths.userDataPath, "2.0.0");
   stageInstallation(paths.systemPath, "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.systemPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateUsesUserInstallationWhenPinnedPathIsInvalid)
{
   // An invalid pinned path only rules out the bundled copy; a user install
   // is still the user's to run.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.pinnedSystemPath = true;
   stageInstallation(paths.userDataPath, "1.0.0");
   stageInstallation(paths.bundledPath, "2.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.userDataPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateFallsBackToSystemInstallation)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.systemPath);
   stageInstallation(paths.bundledPath);

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.systemPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateFallsBackToBundledInstallation)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.bundledPath);

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.bundledPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateSkipsBundledWhenSystemPathIsPinned)
{
   // An invalid posit-assistant-path ends the search rather than silently
   // downgrading to the copy shipped with RStudio.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.pinnedSystemPath = true;
   stageInstallation(paths.bundledPath);

   EXPECT_TRUE(locatePositAssistantInstallation(paths).isEmpty());

   root.removeIfExists();
}

TEST(ChatInstallation, LocateReturnsEmptyWhenNothingIsInstalled)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);

   EXPECT_TRUE(locatePositAssistantInstallation(paths).isEmpty());

   root.removeIfExists();
}

TEST(ChatInstallation, LocateSkipsUserInstallationWhenInstallsAreManaged)
{
   // A user-level copy left behind (or hand-copied in) is ignored, not
   // removed: the administrator's installation wins.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.userInstallEnabled = false;
   stageInstallation(paths.userDataPath, "2.0.0");
   stageInstallation(paths.systemPath, "1.0.0");

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.systemPath);
   EXPECT_TRUE(verifyPositAiInstallation(paths.userDataPath));

   root.removeIfExists();
}

TEST(ChatInstallation, LocateFindsBundledInstallationWhenInstallsAreManaged)
{
   // With no administrator installation, disabled mode still resolves the
   // copy shipped with RStudio rather than the user's own.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.userInstallEnabled = false;
   stageInstallation(paths.userDataPath);
   stageInstallation(paths.bundledPath);

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.bundledPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateFindsPinnedInstallationWhenInstallsAreManaged)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.userInstallEnabled = false;
   paths.pinnedSystemPath = true;
   stageInstallation(paths.userDataPath);
   stageInstallation(paths.systemPath);

   EXPECT_EQ(locatePositAssistantInstallation(paths), paths.systemPath);

   root.removeIfExists();
}

TEST(ChatInstallation, LocateReturnsEmptyWhenOnlyUserInstallationExistsAndInstallsAreManaged)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.userInstallEnabled = false;
   stageInstallation(paths.userDataPath);

   EXPECT_TRUE(locatePositAssistantInstallation(paths).isEmpty());

   root.removeIfExists();
}

// ============================================================================
// userInstallWouldBeSelected
// ============================================================================

TEST(ChatInstallation, UserInstallWouldBeSelectedWhenNothingIsInstalled)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);

   EXPECT_TRUE(userInstallWouldBeSelected(paths, "1.0.0"));

   root.removeIfExists();
}

TEST(ChatInstallation, UserInstallWouldBeSelectedOverTheUserInstallItReplaces)
{
   // The existing user install is what the install overwrites, so it never
   // competes -- a manifest rollback of the user tier still takes effect.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath, "2.0.0");

   EXPECT_TRUE(userInstallWouldBeSelected(paths, "1.0.0"));

   root.removeIfExists();
}

TEST(ChatInstallation, UserInstallWouldNotBeSelectedBelowNewerReadOnlyInstall)
{
   // Installing an older version than the bundle would change nothing, so the
   // update check must not offer it -- the offer could never be satisfied.
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.bundledPath, "1.2.0");
   EXPECT_FALSE(userInstallWouldBeSelected(paths, "1.1.0"));

   stageInstallation(paths.systemPath, "1.3.0");
   EXPECT_FALSE(userInstallWouldBeSelected(paths, "1.2.5"));

   root.removeIfExists();
}

TEST(ChatInstallation, UserInstallWouldBeSelectedAboveOlderReadOnlyInstall)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.systemPath, "1.0.0");
   stageInstallation(paths.bundledPath, "1.0.0");

   EXPECT_TRUE(userInstallWouldBeSelected(paths, "1.1.0"));
   // A tie resolves to the user tier.
   EXPECT_TRUE(userInstallWouldBeSelected(paths, "1.0.0"));

   root.removeIfExists();
}

TEST(ChatInstallation, UserInstallWouldBeSelectedOverIncompatibleReadOnlyInstall)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.bundledPath, "9.0.0", "0.0");

   EXPECT_TRUE(userInstallWouldBeSelected(paths, "1.0.0"));

   root.removeIfExists();
}

TEST(ChatInstallation, UserInstallWouldBeSelectedOverVersionlessReadOnlyInstall)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.systemPath);

   EXPECT_TRUE(userInstallWouldBeSelected(paths, "1.0.0"));

   root.removeIfExists();
}

TEST(ChatInstallation, UserInstallWouldNotBeSelectedUnderValidPinnedPath)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.pinnedSystemPath = true;
   stageInstallation(paths.systemPath, "0.5.0");

   EXPECT_FALSE(userInstallWouldBeSelected(paths, "9.9.9"));

   root.removeIfExists();
}

TEST(ChatInstallation, UserInstallWouldNotBeSelectedWhenInstallsAreManaged)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   paths.userInstallEnabled = false;

   EXPECT_FALSE(userInstallWouldBeSelected(paths, "1.0.0"));

   root.removeIfExists();
}
