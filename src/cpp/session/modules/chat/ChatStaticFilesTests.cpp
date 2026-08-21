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

// The auth cookie must keep byte-identical paths in every deployment where
// the browser-visible path equals the server-known path (see #18621).
TEST(ChatStaticFiles, AuthCookiePathMatchesServerKnownPrefixWithoutClientBaseUrl)
{
   // default root path
   EXPECT_EQ(authCookiePath("https://host/ai-chat/index.html", "", ""), "/");

   // configured www-root-path
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html", "", ""),
             "/rstudio/");

   // Workbench session prefix
   EXPECT_EQ(authCookiePath("https://host/s/0aa27b1d6b8f34dc/ai-chat/index.html",
                            "", ""),
             "/s/0aa27b1d6b8f34dc/");
}

// A root or session prefix that merely contains the route name must not be
// mistaken for the route: matching it would truncate the prefix at the wrong
// offset and scope the token to the whole origin.
TEST(ChatStaticFiles, AuthCookiePathMatchesTheRouteOnlyAtASegmentBoundary)
{
   // www-root-path=/ai-chat-hub
   EXPECT_EQ(authCookiePath("https://host/ai-chat-hub/ai-chat/index.html",
                            "", ""),
             "/ai-chat-hub/");

   // the same name nested under another prefix
   EXPECT_EQ(authCookiePath("https://host/team/ai-chat-hub/ai-chat/index.html",
                            "", ""),
             "/team/ai-chat-hub/");

   // a prefix with no separating punctuation at all
   EXPECT_EQ(authCookiePath("https://host/ai-chatlab/ai-chat/index.html",
                            "", ""),
             "/ai-chatlab/");

   // a root path that is exactly the route name is still a distinct segment
   EXPECT_EQ(authCookiePath("https://host/ai-chat/ai-chat/index.html", "", ""),
             "/ai-chat/");

   // and the ordinary case still resolves to the first segment
   EXPECT_EQ(authCookiePath("https://host/ai-chat/index.html", "", ""), "/");
}

TEST(ChatStaticFiles, AuthCookiePathUnchangedWhenClientBaseUrlMatchesServer)
{
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "https://host/rstudio/", "https://host/rstudio/"),
             "/rstudio/");
   EXPECT_EQ(authCookiePath("https://host/ai-chat/index.html",
                            "https://host/", "https://host/"),
             "/");
}

TEST(ChatStaticFiles, AuthCookiePathIncludesExternalProxyPrefix)
{
   // configured root path behind an unknown prefix-stripping proxy
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "https://host/proxy/rstudio/",
                            "https://host/proxy/rstudio/"),
             "/proxy/rstudio/");

   // Workbench session prefix behind an unknown proxy prefix
   EXPECT_EQ(authCookiePath("https://host/s/0aa27b1d6b8f34dc/ai-chat/index.html",
                            "https://host/proxy/s/0aa27b1d6b8f34dc/",
                            "https://host/proxy/s/0aa27b1d6b8f34dc/"),
             "/proxy/s/0aa27b1d6b8f34dc/");

   // Open OnDemand style prefix over the default root path
   EXPECT_EQ(authCookiePath("http://node01/ai-chat/index.html",
                            "https://ood.host/rnode/node01/8787/",
                            "https://ood.host/rnode/node01/8787/"),
             "/rnode/node01/8787/");
}

// A crafted link cannot scope the auth token to a route of the attacker's
// choosing: the query parameter is honored only when the base recorded by
// the CSRF-protected client_init request agrees with it.
TEST(ChatStaticFiles, AuthCookiePathRejectsClientBaseUrlTheSessionDidNotReport)
{
   // attacker-chosen sibling route, victim connected directly
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "https://host/evil/rstudio/",
                            "https://host/rstudio/"),
             "/rstudio/");

   // attacker-chosen route, victim connected through a real proxy prefix
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "https://host/evil/rstudio/",
                            "https://host/proxy/rstudio/"),
             "/rstudio/");

   // no client_init recorded yet, so nothing authorizes the parameter
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "https://host/proxy/rstudio/", ""),
             "/rstudio/");
}

TEST(ChatStaticFiles, AuthCookiePathIgnoresBogusClientBaseUrl)
{
   // does not end with the server-known prefix
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "https://host/elsewhere/", "https://host/elsewhere/"),
             "/rstudio/");

   // not a usable URL or path
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "garbage", "garbage"),
             "/rstudio/");
}

// The query parameter arrives percent-decoded while client_init records the
// raw JSON string, but both sides start from the same GWT.getHostPageBaseURL()
// value, so a base URL containing an escape still authorizes -- and the escape
// is preserved, which is the form the browser matches against.
TEST(ChatStaticFiles, AuthCookiePathHandlesPercentEscapesInTheBaseUrl)
{
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "https://host/pr%20oxy/rstudio/",
                            "https://host/pr%20oxy/rstudio/"),
             "/pr%20oxy/rstudio/");
}

TEST(ChatStaticFiles, AuthCookiePathUnaffectedByQueryString)
{
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html"
                            "?wsUrl=%2Frstudio%2Fp%2Fabc%2Fai-chat&_t=123"
                            "&clientBaseUrl=https%3A%2F%2Fhost%2Fproxy%2Frstudio%2F",
                            "https://host/proxy/rstudio/",
                            "https://host/proxy/rstudio/"),
             "/proxy/rstudio/");
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
