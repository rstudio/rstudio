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

#include <tests/TestThat.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::installation;
using namespace rstudio::session::modules::chat::constants;

test_context("ChatInstallation")
{
   test_that("verifyPositAiInstallation returns false for non-existent path")
   {
      FilePath nonExistent("/nonexistent/path");
      expect_false(verifyPositAiInstallation(nonExistent));
   }

   test_that("verifyPositAiInstallation returns false for incomplete installation")
   {
      // Create temp directory
      FilePath tempDir;
      FilePath::tempFilePath(tempDir);
      tempDir.ensureDirectory();

      // Empty directory is incomplete
      expect_false(verifyPositAiInstallation(tempDir));

      // Create only client dir - still incomplete
      FilePath clientDir = tempDir.completeChildPath(kClientDirPath);
      clientDir.ensureDirectory();
      expect_false(verifyPositAiInstallation(tempDir));

      // Cleanup
      tempDir.removeIfExists();
   }

   test_that("verifyPositAiInstallation returns true for complete installation")
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
      expect_true(verifyPositAiInstallation(tempDir));

      // Cleanup
      tempDir.removeIfExists();
   }

   test_that("getInstalledVersion returns empty string for non-existent installation")
   {
      // Save original env var
      std::string originalPath = system::getenv("RSTUDIO_POSIT_AI_PATH");

      // Create empty temp directory to use as invalid installation path
      FilePath tempDir;
      FilePath::tempFilePath(tempDir);
      tempDir.ensureDirectory();

      // Set RSTUDIO_POSIT_AI_PATH to empty directory - this ensures
      // locatePositAiInstallation will check this location first
      system::setenv("RSTUDIO_POSIT_AI_PATH", tempDir.getAbsolutePath());

      // Check if there's a valid installation in standard system locations
      // by temporarily unsetting env var and checking
      system::unsetenv("RSTUDIO_POSIT_AI_PATH");
      bool hasSystemInstallation = !locatePositAiInstallation().isEmpty();
      system::setenv("RSTUDIO_POSIT_AI_PATH", tempDir.getAbsolutePath());

      std::string version = getInstalledVersion();

      if (hasSystemInstallation)
      {
         // System has a valid installation in standard locations,
         // so getInstalledVersion() will find it - this is correct behavior
         // Just verify we get a non-empty version string
         expect_false(version.empty());
      }
      else
      {
         // No system installation, temp dir is invalid, should return empty
         expect_true(version.empty());
      }

      // Cleanup
      tempDir.removeIfExists();

      // Restore env var
      if (!originalPath.empty())
         system::setenv("RSTUDIO_POSIT_AI_PATH", originalPath);
      else
         system::unsetenv("RSTUDIO_POSIT_AI_PATH");
   }

   test_that("getInstalledVersion extracts version from package.json")
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
      expect_equal(version, "1.2.3");

      // Cleanup
      if (!originalPath.empty())
         system::setenv("RSTUDIO_POSIT_AI_PATH", originalPath);
      else
         system::unsetenv("RSTUDIO_POSIT_AI_PATH");

      tempDir.removeIfExists();
   }

   test_that("locatePositAiInstallation respects RSTUDIO_POSIT_AI_PATH env var")
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
      FilePath found = locatePositAiInstallation();
      expect_equal(found.getAbsolutePath(), tempDir.getAbsolutePath());

      // Cleanup
      if (!originalPath.empty())
         system::setenv("RSTUDIO_POSIT_AI_PATH", originalPath);
      else
         system::unsetenv("RSTUDIO_POSIT_AI_PATH");

      tempDir.removeIfExists();
   }
}
