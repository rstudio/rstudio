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
#include <core/system/Environment.hpp>
#include <shared_core/json/Json.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::installation;
using namespace rstudio::session::modules::chat::constants;

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

TEST(ChatInstallation, GetInstalledVersionReturnsEmptyStringForNonExistentInstallation)
{
   // Save original env var
   std::string originalPath = system::getenv("RSTUDIO_POSIT_AI_PATH");

   // Create empty temp directory to use as invalid installation path
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   // Set RSTUDIO_POSIT_AI_PATH to empty directory - this ensures
   // locatePositAssistantInstallation will check this location first
   system::setenv("RSTUDIO_POSIT_AI_PATH", tempDir.getAbsolutePath());

   // Check if there's a valid installation in standard system locations
   // by temporarily unsetting env var and checking
   system::unsetenv("RSTUDIO_POSIT_AI_PATH");
   bool hasSystemInstallation = !locatePositAssistantInstallation().isEmpty();
   system::setenv("RSTUDIO_POSIT_AI_PATH", tempDir.getAbsolutePath());

   std::string version = getInstalledVersion();

   if (hasSystemInstallation)
   {
      // System has a valid installation in standard locations,
      // so getInstalledVersion() will find it - this is correct behavior
      // Just verify we get a non-empty version string
      EXPECT_FALSE(version.empty());
   }
   else
   {
      // No system installation, temp dir is invalid, should return empty
      EXPECT_TRUE(version.empty());
   }

   // Cleanup
   tempDir.removeIfExists();

   // Restore env var
   if (!originalPath.empty())
      system::setenv("RSTUDIO_POSIT_AI_PATH", originalPath);
   else
      system::unsetenv("RSTUDIO_POSIT_AI_PATH");
}

TEST(ChatInstallation, GetInstalledVersionExtractsVersionFromPackageJson)
{
   // Create mock installation
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   // Create required structure
   FilePath clientDir = tempDir.completeChildPath(kClientDirPath);
   clientDir.ensureDirectory();

   FilePath serverScript = tempDir.completeChildPath(kServerScriptPath);
   serverScript.getParent().ensureDirectory();
   writeStringToFile(serverScript, "// mock");

   FilePath indexHtml = clientDir.completeChildPath(kIndexFileName);
   writeStringToFile(indexHtml, "<html>mock</html>");

   // Create package.json with version
   FilePath packageJson = tempDir.completeChildPath("package.json");
   std::string packageContent = R"({
  "name": "@posit/posit-ai",
  "version": "1.2.3",
  "description": "Test package"
})";
   writeStringToFile(packageJson, packageContent);

   // Set env var to point to this installation
   std::string originalPath = system::getenv("RSTUDIO_POSIT_AI_PATH");
   system::setenv("RSTUDIO_POSIT_AI_PATH", tempDir.getAbsolutePath());

   // Should extract version
   std::string version = getInstalledVersion();
   EXPECT_EQ(version, "1.2.3");

   // Cleanup
   if (!originalPath.empty())
      system::setenv("RSTUDIO_POSIT_AI_PATH", originalPath);
   else
      system::unsetenv("RSTUDIO_POSIT_AI_PATH");

   tempDir.removeIfExists();
}

TEST(ChatInstallation, LocatePositAiInstallationRespectsRStudioPositAiPathEnvVar)
{
   // Create mock installation
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   FilePath clientDir = tempDir.completeChildPath(kClientDirPath);
   clientDir.ensureDirectory();

   FilePath serverScript = tempDir.completeChildPath(kServerScriptPath);
   serverScript.getParent().ensureDirectory();
   writeStringToFile(serverScript, "// mock");

   FilePath indexHtml = clientDir.completeChildPath(kIndexFileName);
   writeStringToFile(indexHtml, "<html>mock</html>");

   // Save and set env var
   std::string originalPath = system::getenv("RSTUDIO_POSIT_AI_PATH");
   system::setenv("RSTUDIO_POSIT_AI_PATH", tempDir.getAbsolutePath());

   // Should find it via env var
   FilePath found = locatePositAssistantInstallation();
   EXPECT_EQ(found.getAbsolutePath(), tempDir.getAbsolutePath());

   // Cleanup
   if (!originalPath.empty())
      system::setenv("RSTUDIO_POSIT_AI_PATH", originalPath);
   else
      system::unsetenv("RSTUDIO_POSIT_AI_PATH");

   tempDir.removeIfExists();
}

TEST(ChatInstallation, GetInstalledProtocolVersionReturnsEmptyForLegacyInstall)
{
   // Create mock installation without protocol file
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   FilePath clientDir = tempDir.completeChildPath(kClientDirPath);
   clientDir.ensureDirectory();

   FilePath serverScript = tempDir.completeChildPath(kServerScriptPath);
   serverScript.getParent().ensureDirectory();
   writeStringToFile(serverScript, "// mock");

   FilePath indexHtml = clientDir.completeChildPath(kIndexFileName);
   writeStringToFile(indexHtml, "<html>mock</html>");

   // Set env var to point to this installation
   std::string originalPath = system::getenv("RSTUDIO_POSIT_AI_PATH");
   system::setenv("RSTUDIO_POSIT_AI_PATH", tempDir.getAbsolutePath());

   // No protocol file → should return empty
   std::string proto = getInstalledProtocolVersion();
   EXPECT_TRUE(proto.empty());

   // Cleanup
   if (!originalPath.empty())
      system::setenv("RSTUDIO_POSIT_AI_PATH", originalPath);
   else
      system::unsetenv("RSTUDIO_POSIT_AI_PATH");

   tempDir.removeIfExists();
}

TEST(ChatInstallation, GetInstalledProtocolVersionReturnsCorrectVersion)
{
   // Create mock installation with protocol file
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   FilePath clientDir = tempDir.completeChildPath(kClientDirPath);
   clientDir.ensureDirectory();

   FilePath serverScript = tempDir.completeChildPath(kServerScriptPath);
   serverScript.getParent().ensureDirectory();
   writeStringToFile(serverScript, "// mock");

   FilePath indexHtml = clientDir.completeChildPath(kIndexFileName);
   writeStringToFile(indexHtml, "<html>mock</html>");

   // Write protocol.json file
   FilePath protoFile = tempDir.completeChildPath(kProtocolVersionFileName);
   writeStringToFile(protoFile, "{\"protocol\": \"10.0\"}");

   // Set env var to point to this installation
   std::string originalPath = system::getenv("RSTUDIO_POSIT_AI_PATH");
   system::setenv("RSTUDIO_POSIT_AI_PATH", tempDir.getAbsolutePath());

   std::string proto = getInstalledProtocolVersion();
   EXPECT_EQ(proto, "10.0");

   // Cleanup
   if (!originalPath.empty())
      system::setenv("RSTUDIO_POSIT_AI_PATH", originalPath);
   else
      system::unsetenv("RSTUDIO_POSIT_AI_PATH");

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
