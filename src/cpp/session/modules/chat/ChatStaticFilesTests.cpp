/*
 * ChatStaticFilesTests.cpp
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

#include "ChatStaticFiles.hpp"
#include "ChatConstants.hpp"

#include <tests/TestThat.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::staticfiles;
using namespace rstudio::session::modules::chat::constants;

test_context("ChatStaticFiles")
{
   test_that("getContentType returns correct MIME types for common extensions")
   {
      expect_equal(getContentType(".html"), "text/html; charset=utf-8");
      expect_equal(getContentType(".js"), "application/javascript; charset=utf-8");
      expect_equal(getContentType(".mjs"), "application/javascript; charset=utf-8");
      expect_equal(getContentType(".css"), "text/css; charset=utf-8");
      expect_equal(getContentType(".json"), "application/json; charset=utf-8");
      expect_equal(getContentType(".png"), "image/png");
      expect_equal(getContentType(".svg"), "image/svg+xml");
   }

   test_that("getContentType returns octet-stream for unknown extensions")
   {
      expect_equal(getContentType(".unknown"), "application/octet-stream");
      expect_equal(getContentType(".xyz"), "application/octet-stream");
      expect_equal(getContentType(""), "application/octet-stream");
   }

   test_that("validateAndResolvePath rejects path traversal attempts")
   {
      // Create temp directory
      FilePath tempDir;
      FilePath::tempFilePath(tempDir);
      tempDir.ensureDirectory();

      FilePath subDir = tempDir.completeChildPath("sub");
      subDir.ensureDirectory();

      FilePath result;

      // Try to escape via ../
      Error error = validateAndResolvePath(subDir, "../outside.txt", &result);
      expect_true(error);
      expect_equal(error.getCode(), static_cast<int>(boost::system::errc::permission_denied));

      // Try to escape via absolute path
      error = validateAndResolvePath(subDir, "/etc/passwd", &result);
      expect_true(error);

      // Cleanup
      tempDir.removeIfExists();
   }

   test_that("validateAndResolvePath allows valid relative paths")
   {
      // Create temp directory structure
      FilePath tempDir;
      FilePath::tempFilePath(tempDir);
      tempDir.ensureDirectory();

      FilePath testFile = tempDir.completeChildPath("test.html");
      writeStringToFile(testFile, "<html>test</html>");

      FilePath result;
      Error error = validateAndResolvePath(tempDir, "test.html", &result);

      expect_false(error);

      // Canonicalize expected path for comparison (handles /private/ prefix on macOS)
      FilePath canonicalTestFile;
      Error canonError = system::realPath(testFile, &canonicalTestFile);
      if (!canonError)
         expect_equal(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

      // Cleanup
      tempDir.removeIfExists();
   }

   test_that("validateAndResolvePath handles query strings and fragments")
   {
      // Create temp directory
      FilePath tempDir;
      FilePath::tempFilePath(tempDir);
      tempDir.ensureDirectory();

      FilePath testFile = tempDir.completeChildPath("page.html");
      writeStringToFile(testFile, "<html>test</html>");

      // Canonicalize expected path once for comparison (handles /private/ prefix on macOS)
      FilePath canonicalTestFile;
      Error canonError = system::realPath(testFile, &canonicalTestFile);
      expect_false(canonError);

      FilePath result;

      // Test with query string
      Error error = validateAndResolvePath(tempDir, "page.html?param=value", &result);
      expect_false(error);
      expect_equal(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

      // Test with fragment
      error = validateAndResolvePath(tempDir, "page.html#section", &result);
      expect_false(error);
      expect_equal(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

      // Test with both
      error = validateAndResolvePath(tempDir, "page.html?param=value#section", &result);
      expect_false(error);
      expect_equal(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

      // Cleanup
      tempDir.removeIfExists();
   }

   test_that("validateAndResolvePath handles URL encoding")
   {
      // Create temp directory with special characters
      FilePath tempDir;
      FilePath::tempFilePath(tempDir);
      tempDir.ensureDirectory();

      FilePath testFile = tempDir.completeChildPath("file with spaces.html");
      writeStringToFile(testFile, "<html>test</html>");

      FilePath result;

      // Test URL encoded path (space = %20)
      Error error = validateAndResolvePath(tempDir, "file%20with%20spaces.html", &result);
      expect_false(error);

      // Canonicalize expected path for comparison (handles /private/ prefix on macOS)
      FilePath canonicalTestFile;
      Error canonError = system::realPath(testFile, &canonicalTestFile);
      if (!canonError)
         expect_equal(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

      // Cleanup
      tempDir.removeIfExists();
   }

   test_that("validateAndResolvePath canonicalizes paths with ..")
   {
      // Create temp directory structure
      FilePath tempDir;
      FilePath::tempFilePath(tempDir);
      tempDir.ensureDirectory();

      FilePath subDir = tempDir.completeChildPath("sub");
      subDir.ensureDirectory();

      FilePath testFile = tempDir.completeChildPath("test.html");
      writeStringToFile(testFile, "<html>test</html>");

      FilePath result;

      // Valid path with .. that stays within root
      // sub/../test.html should resolve to test.html
      Error error = validateAndResolvePath(tempDir, "sub/../test.html", &result);
      expect_false(error);

      // Canonicalize expected path for comparison (handles /private/ prefix on macOS)
      FilePath canonicalTestFile;
      Error canonError = system::realPath(testFile, &canonicalTestFile);
      if (!canonError)
         expect_equal(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

      // Cleanup
      tempDir.removeIfExists();
   }
}
