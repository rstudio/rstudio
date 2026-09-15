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

#include <gtest/gtest.h>
#include <core/FileSerializer.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/system/System.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::staticfiles;
using namespace rstudio::session::modules::chat::constants;

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

// Stages the files verifyPositAiInstallation() requires, plus one client
// asset. The asset is a .js so the request under test skips the handler's
// HTML branch, which reads session options and the current editor theme.
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

// Unpins the installation and clears the backend port after each test, so a
// later test sees the state of a session whose chat backend has not started
// yet.
class ChatStaticFilesPin : public ::testing::Test
{
protected:
   void TearDown() override
   {
      setInstallationPath(FilePath());
      setChatBackendPort(kChatBackendPortNone);
   }
};

} // anonymous namespace

TEST_F(ChatStaticFilesPin, ServesAssetsFromThePinnedInstallation)
{
   FilePath install = stageInstallationServingApp("// pinned build");
   setInstallationPath(install);

   http::Response response;
   Error error = requestApp(&response);

   EXPECT_FALSE(error);
   EXPECT_EQ(response.statusCode(), http::status::Ok);
   EXPECT_EQ(response.body(), "// pinned build");
   EXPECT_EQ(response.contentType(), getContentType(".js"));

   install.removeIfExists();
}

TEST_F(ChatStaticFilesPin, LaterPinReplacesTheEarlierInstallation)
{
   FilePath first = stageInstallationServingApp("// first build");
   FilePath second = stageInstallationServingApp("// second build");

   // A backend restart re-resolves and pins again; the newer pin is what the
   // page that restart loads must be served from.
   setInstallationPath(first);
   setInstallationPath(second);

   http::Response response;
   Error error = requestApp(&response);

   EXPECT_FALSE(error);
   EXPECT_EQ(response.body(), "// second build");

   first.removeIfExists();
   second.removeIfExists();
}

TEST_F(ChatStaticFilesPin, PinnedInstallationThatIsGoneIsNotServedFrom)
{
   FilePath install = stageInstallationServingApp("// removed build");
   setInstallationPath(install);

   // Confirm the pin is live before removing what it names, so the assertion
   // below is about the removal and not about the pin never having worked.
   http::Response served;
   EXPECT_FALSE(requestApp(&served));
   EXPECT_EQ(served.body(), "// removed build");

   // A rolled-back update or an out-of-band removal leaves the pin naming a
   // directory that is gone. The handler must resolve for itself rather than
   // answer from the vanished path for the rest of the session. What it
   // resolves to depends on what is installed on this machine, so only the
   // negative is asserted.
   install.removeIfExists();

   http::Response response;
   requestApp(&response);

   EXPECT_NE(response.body(), "// removed build");

   // Forbidden is the signature of the dead pin having been used: the client
   // root under it cannot be canonicalized, so validateAndResolvePath()
   // rejects the path. Resolving instead answers Ok or NotFound depending on
   // what this machine has installed, but never this.
   EXPECT_NE(response.statusCode(), http::status::Forbidden);
}

TEST_F(ChatStaticFilesPin, PartiallyExtractedPinnedInstallationIsNotServedFrom)
{
   FilePath install = stageInstallationServingApp("// partial build");
   setInstallationPath(install);

   http::Response served;
   EXPECT_FALSE(requestApp(&served));
   EXPECT_EQ(served.body(), "// partial build");

   // An extraction that failed and could not be cleaned up leaves the root in
   // place without the files that make it an installation. The asset itself
   // survives here, so serving it would succeed -- which is exactly why the
   // pin must be tested against verifyPositAiInstallation() and not merely
   // for the root's existence.
   install.completeChildPath(kClientDirPath)
      .completeChildPath(kIndexFileName)
      .removeIfExists();

   http::Response response;
   requestApp(&response);

   EXPECT_NE(response.body(), "// partial build");

   install.removeIfExists();
}

TEST_F(ChatStaticFilesPin, UnpinnedInstallationIsNotServedFrom)
{
   FilePath install = stageInstallationServingApp("// unpinned build");
   setInstallationPath(install);
   setInstallationPath(FilePath());

   // With nothing pinned the handler resolves the installation for itself, so
   // it must not still be serving the one that was pinned. What it resolves to
   // instead depends on what is installed on this machine, so only the
   // negative is asserted.
   http::Response response;
   requestApp(&response);

   EXPECT_NE(response.body(), "// unpinned build");

   install.removeIfExists();
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

TEST_F(ChatStaticFilesPin, CspFollowsAnUpdateThatReplacesTheInstallationInPlace)
{
   FilePath install = stageInstallationServingCsp("https://before.example");
   setInstallationPath(install);
   setChatBackendPort(1234);

   EXPECT_NE(requestPageCsp().find("https://before.example"), std::string::npos);

   // An in-session update extracts over the same directory, so the path the
   // backend restart pins is unchanged -- only the contents differ. The
   // directives must come from the installation being served, not from
   // whatever the first request happened to read.
   writeStringToFile(install.completeChildPath(kCspConfigPath),
                     "{\"connect-src\": \"https://after.example\"}");
   setChatBackendPort(5678);

   std::string header = requestPageCsp();
   EXPECT_NE(header.find("https://after.example"), std::string::npos);
   EXPECT_EQ(header.find("https://before.example"), std::string::npos);

   install.removeIfExists();
}

TEST_F(ChatStaticFilesPin, CspIsReadFromTheInstallationThePinNames)
{
   FilePath first = stageInstallationServingCsp("https://first.example");
   FilePath second = stageInstallationServingCsp("https://second.example");

   setInstallationPath(first);
   setChatBackendPort(1234);
   EXPECT_NE(requestPageCsp().find("https://first.example"), std::string::npos);

   // A restart onto another installation -- what a versioned install does --
   // must serve that installation's directives.
   setInstallationPath(second);
   setChatBackendPort(5678);

   std::string header = requestPageCsp();
   EXPECT_NE(header.find("https://second.example"), std::string::npos);
   EXPECT_EQ(header.find("https://first.example"), std::string::npos);

   first.removeIfExists();
   second.removeIfExists();
}
