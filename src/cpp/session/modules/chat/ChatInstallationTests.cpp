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

TEST(ChatInstallation, LocatePrefersUserInstallation)
{
   FilePath root;
   InstallSearchPaths paths = tempSearchPaths(&root);
   stageInstallation(paths.userDataPath);
   stageInstallation(paths.systemPath);
   stageInstallation(paths.bundledPath);

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
