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
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::chat::staticfiles;
using namespace rstudio::session::modules::chat::constants;

namespace {

// A Posit Assistant installation just complete enough for
// locatePositAssistantInstallation to accept it, pointed at by the
// RSTUDIO_POSIT_AI_PATH override the same function honours.
class FakeAssistantInstallation
{
public:
   FakeAssistantInstallation()
   {
      FilePath::tempFilePath(root_);
      root_.ensureDirectory();

      FilePath clientDir = root_.completeChildPath(kClientDirPath);
      clientDir.ensureDirectory();
      writeStringToFile(clientDir.completeChildPath(kIndexFileName),
                        "<html lang=\"en\"><head></head><body></body></html>");

      FilePath serverScript = root_.completeChildPath(kServerScriptPath);
      serverScript.getParent().ensureDirectory();
      writeStringToFile(serverScript, "// server");

      FilePath assets = clientDir.completeChildPath("assets");
      assets.ensureDirectory();
      writeStringToFile(assets.completeChildPath("app.js"), kAssetBody);

      // a name whose extension is not lowercase; deliberately not a case
      // variant of app.js, which would collide on a case-insensitive
      // filesystem
      writeStringToFile(assets.completeChildPath("Widget.JS"), kAssetBody);

      previous_ = system::getenv(kPathOverride);
      system::setenv(kPathOverride, root_.getAbsolutePath());
   }

   ~FakeAssistantInstallation()
   {
      if (previous_.empty())
         system::unsetenv(kPathOverride);
      else
         system::setenv(kPathOverride, previous_);

      root_.removeIfExists();
   }

   static constexpr const char* kAssetBody = "console.log('app');";

private:
   static constexpr const char* kPathOverride = "RSTUDIO_POSIT_AI_PATH";

   FilePath root_;
   std::string previous_;
};

void requestChatFile(const std::string& uri, http::Response* pResponse)
{
   http::Request request;
   request.setUri(uri);

   Error error = handleAIChatRequest(request, pResponse);
   EXPECT_FALSE(error);
}

} // anonymous namespace

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

// The cookie path is the session prefix the server believes it is serving
// this request under, so it has to come out byte-identical to what the
// browser sees for the browser to send the cookie back (see #18621).
TEST(ChatStaticFiles, AuthCookiePathMatchesTheServerKnownPrefix)
{
   // default root path
   EXPECT_EQ(authCookiePath("https://host/ai-chat/index.html"), "/");

   // configured www-root-path
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html"),
             "/rstudio/");

   // Workbench session prefix
   EXPECT_EQ(authCookiePath("https://host/s/0aa27b1d6b8f34dc/ai-chat/index.html"),
             "/s/0aa27b1d6b8f34dc/");

   // an external proxy prefix the server has been told about, via
   // www-root-path or the X-RStudio-Root-Path header, is already part of
   // proxiedUri and so carries through
   EXPECT_EQ(authCookiePath("https://ood.host/rnode/node01/8787/ai-chat/index.html"),
             "/rnode/node01/8787/");
   EXPECT_EQ(authCookiePath("https://host/proxy/rstudio/ai-chat/index.html"),
             "/proxy/rstudio/");
}

// A root or session prefix that merely contains the route name must not be
// mistaken for the route: matching it would truncate the prefix at the wrong
// offset and scope the token to the whole origin.
TEST(ChatStaticFiles, AuthCookiePathMatchesTheRouteOnlyAtASegmentBoundary)
{
   // www-root-path=/ai-chat-hub
   EXPECT_EQ(authCookiePath("https://host/ai-chat-hub/ai-chat/index.html"),
             "/ai-chat-hub/");

   // the same name nested under another prefix
   EXPECT_EQ(authCookiePath("https://host/team/ai-chat-hub/ai-chat/index.html"),
             "/team/ai-chat-hub/");

   // a prefix with no separating punctuation at all
   EXPECT_EQ(authCookiePath("https://host/ai-chatlab/ai-chat/index.html"),
             "/ai-chatlab/");

   // a root path that is exactly the route name is still a distinct segment
   EXPECT_EQ(authCookiePath("https://host/ai-chat/ai-chat/index.html"),
             "/ai-chat/");

   // and the ordinary case still resolves to the first segment
   EXPECT_EQ(authCookiePath("https://host/ai-chat/index.html"), "/");
}

// proxiedUri is reconstructed from request headers, which nothing in the
// server strips or validates, and the cookie path is written into the
// Set-Cookie header unescaped. A path that could break out of that header
// must not be used -- and the origin root is not an acceptable retreat.
TEST(ChatStaticFiles, AuthCookiePathDeclinesOnAnUnusableServerDerivedPath)
{
   // a ";path=/" smuggled through the proxied URI would otherwise be emitted
   // verbatim, and RFC 6265 takes the last Path attribute
   EXPECT_EQ(authCookiePath("https://host/x;path=//ai-chat/index.html"), "");

   // header-breaking characters in the derived prefix
   EXPECT_EQ(authCookiePath("https://host/a\r\nX-Injected: 1/ai-chat/index.html"),
             "");

   // a prefix the browser could never match against a normalized request path
   EXPECT_EQ(authCookiePath("https://host/a/../b/ai-chat/index.html"), "");
}

TEST(ChatStaticFiles, AuthCookiePathUnaffectedByQueryString)
{
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html"
                            "?wsUrl=%2Frstudio%2Fp%2Fabc%2Fai-chat&_t=123"),
             "/rstudio/");

   // an unescaped occurrence of the route in the query must not be taken for
   // the route itself: the derived prefix would then carry the document path
   // and the query along with it
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html"
                            "?wsUrl=/rstudio/p/abc/ai-chat/"),
             "/rstudio/");
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html"
                            "#/ai-chat/"),
             "/rstudio/");
}

// The index.html response carries the assistant auth token cookie, so it must
// never be classified as a long-lived cacheable asset.
TEST(ChatStaticFiles, IsNoStoreExtensionCoversHtmlJavaScriptAndCss)
{
   EXPECT_TRUE(isNoStoreExtension(".html"));
   EXPECT_TRUE(isNoStoreExtension(".htm"));
   EXPECT_TRUE(isNoStoreExtension(".js"));
   EXPECT_TRUE(isNoStoreExtension(".mjs"));
   EXPECT_TRUE(isNoStoreExtension(".css"));

   EXPECT_FALSE(isNoStoreExtension(".png"));
   EXPECT_FALSE(isNoStoreExtension(".woff2"));
   EXPECT_FALSE(isNoStoreExtension(""));
}

// The predicate and the content type map both compare against lowercase
// literals, so the handler has to fold the resolved extension before asking.
// On Windows realPath is purely lexical and leaves the requested case as
// written, so an uppercase name would otherwise be served as an opaque blob
// and cached for a year.
TEST(ChatStaticFiles, HandlerClassifiesAnUppercaseExtension)
{
   FakeAssistantInstallation installation;

   http::Response response;
   requestChatFile("/ai-chat/assets/Widget.JS", &response);

   EXPECT_EQ(response.statusCode(), http::status::Ok);
   EXPECT_EQ(response.headerValue("Content-Type"),
             "application/javascript; charset=utf-8");
   EXPECT_NE(response.headerValue("Cache-Control").find("no-store"),
             std::string::npos);
}

TEST(ChatStaticFiles, HandlerServesAnAssetUnderTheRoute)
{
   FakeAssistantInstallation installation;

   http::Response response;
   requestChatFile("/ai-chat/assets/app.js", &response);

   EXPECT_EQ(response.statusCode(), http::status::Ok);
   EXPECT_EQ(response.body(),
             std::string(FakeAssistantInstallation::kAssetBody));
   EXPECT_EQ(response.headerValue("Content-Type"),
             "application/javascript; charset=utf-8");

   // JavaScript changes with every assistant build, so it is never stored
   EXPECT_NE(response.headerValue("Cache-Control").find("no-store"),
             std::string::npos);
}

// The IDE appends query parameters to every request it makes for this route,
// and they must not participate in resolving the file.
TEST(ChatStaticFiles, HandlerIgnoresTheQueryStringWhenResolvingAFile)
{
   FakeAssistantInstallation installation;

   http::Response response;
   requestChatFile("/ai-chat/assets/app.js?v=abc&clientBaseUrl=https%3A%2F%2Fhost%2F",
                   &response);

   EXPECT_EQ(response.statusCode(), http::status::Ok);
   EXPECT_EQ(response.body(),
             std::string(FakeAssistantInstallation::kAssetBody));
}

// The handler is registered for the bare "/ai-chat" prefix, so it sees
// requests that only start with the route name. Locating the route anywhere
// in the URI let these serve real files under a path no cache rule or
// intermediary would recognize as the route.
TEST(ChatStaticFiles, HandlerRejectsRequestsThatOnlyResembleTheRoute)
{
   FakeAssistantInstallation installation;

   // a file named in the query rather than in the path
   http::Response queryNamed;
   requestChatFile("/ai-chat?x=/ai-chat/assets/app.js", &queryNamed);
   EXPECT_EQ(queryNamed.statusCode(), http::status::BadRequest);
   EXPECT_EQ(queryNamed.body(), "");

   // a junk-suffixed spelling of the route
   http::Response junkSuffixed;
   requestChatFile("/ai-chatXYZ/ai-chat/assets/app.js", &junkSuffixed);
   EXPECT_EQ(junkSuffixed.statusCode(), http::status::BadRequest);
   EXPECT_EQ(junkSuffixed.body(), "");

   // the route without its trailing slash names no file
   http::Response bareRoute;
   requestChatFile("/ai-chat", &bareRoute);
   EXPECT_EQ(bareRoute.statusCode(), http::status::BadRequest);
}

TEST(ChatStaticFiles, HandlerRejectsTraversalOutOfTheClientRoot)
{
   FakeAssistantInstallation installation;

   http::Response response;
   requestChatFile("/ai-chat/../../../../etc/passwd", &response);

   EXPECT_EQ(response.statusCode(), http::status::Forbidden);
}

// Resolution URL-decodes the request path, so the same file can be requested
// under more than one spelling -- which is why cache classification reads the
// resolved extension rather than the requested path.
TEST(ChatStaticFiles, ValidateAndResolvePathDecodesPercentEncodedFileNames)
{
   FilePath tempDir;
   FilePath::tempFilePath(tempDir);
   tempDir.ensureDirectory();

   FilePath indexFile = tempDir.completeChildPath(kIndexFileName);
   writeStringToFile(indexFile, "<html>test</html>");

   FilePath result;
   Error error = validateAndResolvePath(tempDir, "%69ndex.html", &result);
   EXPECT_FALSE(error);
   EXPECT_EQ(result.getFilename(), std::string(kIndexFileName));
   EXPECT_TRUE(isNoStoreExtension(result.getExtension()));

   tempDir.removeIfExists();
}
