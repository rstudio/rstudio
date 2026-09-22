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
#include "ChatInstallation.hpp"

#include <gtest/gtest.h>
#include <boost/optional.hpp>
#include <core/FileSerializer.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/system/System.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::staticfiles;
using namespace rstudio::session::modules::chat::constants;
using rstudio::session::modules::chat::installation::InstallSearchPaths;
using rstudio::session::modules::chat::installation::setSearchPathsForTesting;

TEST(ChatStaticFiles, GetContentTypeReturnsCorrectMimeTypesForCommonExtensions)
{
   EXPECT_EQ(getContentType(".html"), "text/html; charset=utf-8");
   EXPECT_EQ(getContentType(".js"), "application/javascript; charset=utf-8");
   EXPECT_EQ(getContentType(".mjs"), "application/javascript; charset=utf-8");
   EXPECT_EQ(getContentType(".css"), "text/css; charset=utf-8");
   EXPECT_EQ(getContentType(".json"), "application/json; charset=utf-8");
   EXPECT_EQ(getContentType(".png"), "image/png");
   EXPECT_EQ(getContentType(".svg"), "image/svg+xml");
}

TEST(ChatStaticFiles, GetContentTypeReturnsOctetStreamForUnknownExtensions)
{
   EXPECT_EQ(getContentType(".unknown"), "application/octet-stream");
   EXPECT_EQ(getContentType(".xyz"), "application/octet-stream");
   EXPECT_EQ(getContentType(""), "application/octet-stream");
}

TEST(ChatStaticFiles, ValidateAndResolvePathRejectsPathTraversalAttempts)
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
   EXPECT_TRUE(error);
   EXPECT_EQ(error.getCode(), static_cast<int>(boost::system::errc::permission_denied));

   // Try to escape via absolute path
   error = validateAndResolvePath(subDir, "/etc/passwd", &result);
   EXPECT_TRUE(error);

   // Cleanup
   tempDir.removeIfExists();
}

TEST(ChatStaticFiles, ValidateAndResolvePathAllowsValidRelativePaths)
{
   // Create temp directory structure
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   FilePath testFile = tempDir.completeChildPath("test.html");
   writeStringToFile(testFile, "<html>test</html>");

   FilePath result;
   Error error = validateAndResolvePath(tempDir, "test.html", &result);

   EXPECT_FALSE(error);

   // Canonicalize expected path for comparison (handles /private/ prefix on macOS)
   FilePath canonicalTestFile;
   Error canonError = system::realPath(testFile, &canonicalTestFile);
   if (!canonError)
   {
      EXPECT_EQ(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());
   }

   // Cleanup
   tempDir.removeIfExists();
}

TEST(ChatStaticFiles, ValidateAndResolvePathHandlesQueryStringsAndFragments)
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
   EXPECT_FALSE(canonError);

   FilePath result;

   // Test with query string
   Error error = validateAndResolvePath(tempDir, "page.html?param=value", &result);
   EXPECT_FALSE(error);
   EXPECT_EQ(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

   // Test with fragment
   error = validateAndResolvePath(tempDir, "page.html#section", &result);
   EXPECT_FALSE(error);
   EXPECT_EQ(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

   // Test with both
   error = validateAndResolvePath(tempDir, "page.html?param=value#section", &result);
   EXPECT_FALSE(error);
   EXPECT_EQ(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());

   // Cleanup
   tempDir.removeIfExists();
}

TEST(ChatStaticFiles, ValidateAndResolvePathHandlesUrlEncoding)
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
   EXPECT_FALSE(error);

   // Canonicalize expected path for comparison (handles /private/ prefix on macOS)
   FilePath canonicalTestFile;
   Error canonError = system::realPath(testFile, &canonicalTestFile);
   if (!canonError)
   {
      EXPECT_EQ(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());
   }

   // Cleanup
   tempDir.removeIfExists();
}

TEST(ChatStaticFiles, ValidateAndResolvePathCanonicalizesPathsWithDotDot)
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
   EXPECT_FALSE(error);

   // Canonicalize expected path for comparison (handles /private/ prefix on macOS)
   FilePath canonicalTestFile;
   Error canonError = system::realPath(testFile, &canonicalTestFile);
   if (!canonError)
   {
      EXPECT_EQ(result.getAbsolutePath(), canonicalTestFile.getAbsolutePath());
   }

   // Cleanup
   tempDir.removeIfExists();
}

namespace {

// Stages the files verifyInstallDir() requires, plus one client asset. The
// asset is a .js so the request under test skips the handler's HTML branch,
// which reads session options and the current editor theme.
FilePath stageInstallationServingApp(const std::string& assetContent)
{
   FilePath dir;
   FilePath::tempFilePath(dir);
   dir.ensureDirectory();

   FilePath clientDir = dir.completeChildPath(kClientDirPath);
   clientDir.ensureDirectory();

   FilePath serverScript = dir.completeChildPath(kServerScriptPath);
   serverScript.getParent().ensureDirectory();
   writeStringToFile(serverScript, "// mock server script");

   writeStringToFile(clientDir.completeChildPath(kIndexFileName),
                     "<html>mock</html>");
   writeStringToFile(clientDir.completeChildPath("app.js"), assetContent);

   return dir;
}

// Requests /ai-chat/app.js from whichever installation the handler serves.
Error requestApp(http::Response* pResponse)
{
   http::Request request;
   request.setUri("/ai-chat/app.js");
   return handleAIChatRequest(request, pResponse);
}

// The handler serves from the installation the session resolved, so each
// test drives that resolution through the pinned-path source: a single
// unversioned directory, which is the simplest thing the resolver accepts.
// The session's own sources are restored, and the backend port cleared, after
// each test so a later test sees the state of a session whose chat backend
// has not started yet. Both setters rebuild the CSP header from whatever is
// resolvable on the machine running the tests, so a test asserting on that
// header must serve an installation of its own rather than rely on the state
// left here.
class ChatStaticFilesResolution : public ::testing::Test
{
protected:
   void TearDown() override
   {
      setSearchPathsForTesting(boost::none);
      setChatBackendPort(kChatBackendPortNone);
   }

   // Makes `install` the installation the session resolves. Discards any
   // held resolution, as changing the sources does.
   void serve(const FilePath& install)
   {
      InstallSearchPaths paths;
      paths.pinnedPath = install;
      paths.userInstallEnabled = false;
      setSearchPathsForTesting(paths);
   }
};

} // anonymous namespace

TEST_F(ChatStaticFilesResolution, ServesAssetsFromTheResolvedInstallation)
{
   FilePath install = stageInstallationServingApp("// resolved build");
   serve(install);

   http::Response response;
   Error error = requestApp(&response);

   EXPECT_FALSE(error);
   EXPECT_EQ(response.statusCode(), http::status::Ok);
   EXPECT_EQ(response.body(), "// resolved build");
   EXPECT_EQ(response.contentType(), getContentType(".js"));

   install.removeIfExists();
}

TEST_F(ChatStaticFilesResolution, ServesFromTheHeldInstallationUntilItIsCleared)
{
   FilePath first = stageInstallationServingApp("// first build");
   FilePath second = stageInstallationServingApp("// second build");

   InstallSearchPaths paths;
   paths.pinnedPath = first;
   paths.userInstallEnabled = false;
   setSearchPathsForTesting(paths);

   http::Response response;
   requestApp(&response);
   ASSERT_EQ(response.body(), "// first build");

   // Replacing the tree the pinned path names is what a third party does to
   // an unversioned directory; the held resolution keeps serving the same
   // path, and the page it loaded keeps getting the same installation.
   ASSERT_FALSE(first.remove());
   ASSERT_FALSE(second.move(first, FilePath::MoveDirect));

   http::Response held;
   requestApp(&held);
   EXPECT_EQ(held.body(), "// second build");

   first.removeIfExists();
}

TEST_F(ChatStaticFilesResolution, ResolvedInstallationThatIsGoneIsNotServedFrom)
{
   FilePath install = stageInstallationServingApp("// removed build");
   serve(install);

   http::Response before;
   requestApp(&before);
   ASSERT_EQ(before.body(), "// removed build");

   // Removed out of band after being resolved. The handler must not keep
   // serving from it just because it was resolved once: with nothing else to
   // resolve, the request fails as if nothing were installed.
   ASSERT_FALSE(install.remove());

   http::Response after;
   Error error = requestApp(&after);

   EXPECT_FALSE(error);
   EXPECT_EQ(after.statusCode(), http::status::NotFound);
}

TEST_F(ChatStaticFilesResolution, PartiallyExtractedInstallationIsNotServedFrom)
{
   FilePath install = stageInstallationServingApp("// partial build");
   serve(install);

   http::Response before;
   requestApp(&before);
   ASSERT_EQ(before.body(), "// partial build");

   // A directory that no longer holds a complete installation -- here the
   // server script is gone -- is resolved around the same way, even though
   // the asset itself is still there.
   ASSERT_FALSE(install.completeChildPath(kServerScriptPath).remove());

   http::Response after;
   requestApp(&after);

   EXPECT_EQ(after.statusCode(), http::status::NotFound);

   install.removeIfExists();
}

TEST_F(ChatStaticFilesResolution, ClearingTheResolutionServesTheNewInstallation)
{
   FilePath first = stageInstallationServingApp("// first build");
   FilePath second = stageInstallationServingApp("// second build");
   serve(first);

   http::Response response;
   requestApp(&response);
   ASSERT_EQ(response.body(), "// first build");

   // What an install does: the sources now resolve elsewhere, and the held
   // answer is discarded so the components restarting -- and the page --
   // come back on the new installation.
   serve(second);

   http::Response cleared;
   requestApp(&cleared);
   EXPECT_EQ(cleared.body(), "// second build");

   first.removeIfExists();
   second.removeIfExists();
}

namespace {

// Stages an installation whose client serves one non-index HTML page, plus a
// dist/csp.json carrying the given connect-src. A non-index page takes the
// handler's CSP branch without the index-only theme injection and auth cookie.
FilePath stageInstallationServingCsp(const std::string& connectSrc)
{
   FilePath dir = stageInstallationServingApp("// csp build");

   writeStringToFile(dir.completeChildPath(kClientDirPath)
                        .completeChildPath("page.html"),
                     "<html>page</html>");

   writeStringToFile(dir.completeChildPath(kCspConfigPath),
                     "{\"connect-src\": \"" + connectSrc + "\"}");

   return dir;
}

// Requests the non-index page, returning the CSP header the handler set.
std::string requestPageCsp()
{
   http::Request request;
   request.setUri("/ai-chat/page.html");

   http::Response response;
   Error error = handleAIChatRequest(request, &response);
   EXPECT_FALSE(error);

   return response.headerValue("Content-Security-Policy");
}

} // anonymous namespace

TEST_F(ChatStaticFilesResolution, CspIsRereadWhenTheBackendPortChanges)
{
   FilePath install = stageInstallationServingCsp("https://before.example");
   serve(install);
   setChatBackendPort(1234);

   EXPECT_NE(requestPageCsp().find("https://before.example"), std::string::npos);

   // An unversioned installation replaced in place keeps its path, so only
   // the contents differ. A backend restart re-reads the directives, so the
   // policy served is the one belonging to what is being served now.
   writeStringToFile(install.completeChildPath(kCspConfigPath),
                     "{\"connect-src\": \"https://after.example\"}");
   setChatBackendPort(5678);

   std::string header = requestPageCsp();
   EXPECT_NE(header.find("https://after.example"), std::string::npos);
   EXPECT_EQ(header.find("https://before.example"), std::string::npos);

   install.removeIfExists();
}

TEST_F(ChatStaticFilesResolution, CspIsReadFromTheInstallationBeingServed)
{
   FilePath first = stageInstallationServingCsp("https://first.example");
   FilePath second = stageInstallationServingCsp("https://second.example");

   serve(first);
   setChatBackendPort(1234);
   EXPECT_NE(requestPageCsp().find("https://first.example"), std::string::npos);

   // A restart onto another installation -- what a versioned install does --
   // must serve that installation's directives.
   serve(second);
   setChatBackendPort(5678);

   std::string header = requestPageCsp();
   EXPECT_NE(header.find("https://second.example"), std::string::npos);
   EXPECT_EQ(header.find("https://first.example"), std::string::npos);

   first.removeIfExists();
   second.removeIfExists();
}

TEST_F(ChatStaticFilesResolution, CspFollowsTheResolutionWithoutABackendPortChange)
{
   FilePath first = stageInstallationServingCsp("https://first.example");
   FilePath second = stageInstallationServingCsp("https://second.example");

   serve(first);
   setChatBackendPort(1234);
   EXPECT_NE(requestPageCsp().find("https://first.example"), std::string::npos);

   // Changing which installation is served is enough on its own: the
   // resolution can change with no backend start behind it -- an install
   // clears it, a removed installation is resolved around -- and a policy
   // that only followed the port would outlive the installation it came from.
   serve(second);

   std::string header = requestPageCsp();
   EXPECT_NE(header.find("https://second.example"), std::string::npos);
   EXPECT_EQ(header.find("https://first.example"), std::string::npos);

   first.removeIfExists();
   second.removeIfExists();
}
