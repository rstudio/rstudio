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

// The auth cookie must keep byte-identical paths in every deployment where
// the browser-visible path equals the server-known path (see #18621).
TEST(ChatStaticFiles, AuthCookiePathMatchesServerKnownPrefixWithoutClientBaseUrl)
{
   // default root path
   EXPECT_EQ(authCookiePath("https://host/ai-chat/index.html", "",
                            "https://host/"),
             "/");

   // configured www-root-path
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html", "",
                            "https://host/rstudio/"),
             "/rstudio/");

   // Workbench session prefix
   EXPECT_EQ(authCookiePath("https://host/s/0aa27b1d6b8f34dc/ai-chat/index.html",
                            "", "https://host/s/0aa27b1d6b8f34dc/"),
             "/s/0aa27b1d6b8f34dc/");
}

// A root or session prefix that merely contains the route name must not be
// mistaken for the route: matching it would truncate the prefix at the wrong
// offset and scope the token to the whole origin.
TEST(ChatStaticFiles, AuthCookiePathMatchesTheRouteOnlyAtASegmentBoundary)
{
   // www-root-path=/ai-chat-hub
   EXPECT_EQ(authCookiePath("https://host/ai-chat-hub/ai-chat/index.html",
                            "", "https://host/ai-chat-hub/"),
             "/ai-chat-hub/");

   // the same name nested under another prefix
   EXPECT_EQ(authCookiePath("https://host/team/ai-chat-hub/ai-chat/index.html",
                            "", "https://host/team/ai-chat-hub/"),
             "/team/ai-chat-hub/");

   // a prefix with no separating punctuation at all
   EXPECT_EQ(authCookiePath("https://host/ai-chatlab/ai-chat/index.html",
                            "", "https://host/ai-chatlab/"),
             "/ai-chatlab/");

   // a root path that is exactly the route name is still a distinct segment
   EXPECT_EQ(authCookiePath("https://host/ai-chat/ai-chat/index.html", "",
                            "https://host/ai-chat/"),
             "/ai-chat/");

   // and the ordinary case still resolves to the first segment
   EXPECT_EQ(authCookiePath("https://host/ai-chat/index.html", "",
                            "https://host/"),
             "/");
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
             "");
}

// client_init records whatever base the client sent, without checking that it
// could be used as a cookie path, so an unusable one can sit alongside a live
// token. It authorizes nothing and must not be read as permission.
TEST(ChatStaticFiles, AuthCookiePathDeclinesOnAnUnusableRecordedBase)
{
   // a stray double slash, from a proxy or redirect that did not normalize
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html", "",
                            "https://host//rstudio/"),
             "");

   // a base recorded before it could be resolved
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html", "",
                            "garbage"),
             "");
}

// Falling back to the server-known prefix is only safe while that prefix is
// narrower than the whole origin. On a default-root deployment reached
// through an external prefix it is not, so the cookie must not be set at all
// rather than be handed to every application on the host.
TEST(ChatStaticFiles, AuthCookiePathDeclinesRatherThanFallBackToTheOriginRoot)
{
   // a request that did not come from the IDE frontend: a crafted top-level
   // link, or a bookmarked bare iframe URL
   EXPECT_EQ(authCookiePath("http://node01/ai-chat/index.html", "",
                            "https://ood.host/rnode/node01/8787/"),
             "");

   // a reported base the session never authenticated
   EXPECT_EQ(authCookiePath("http://node01/ai-chat/index.html",
                            "https://ood.host/evil/",
                            "https://ood.host/rnode/node01/8787/"),
             "");

   // but a configured root path is narrower than the origin, so the fallback
   // stands
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html", "",
                            "https://host/proxy/rstudio/"),
             "/rstudio/");

   // and a client claiming a prefix the session did not report still falls
   // back to the root it would have used anyway
   EXPECT_EQ(authCookiePath("https://host/ai-chat/index.html",
                            "https://host/evil/", "https://host/"),
             "/");
}

// proxiedUri is reconstructed from request headers, which nothing in the
// server strips or validates, and the cookie path is written into the
// Set-Cookie header unescaped. A path that could break out of that header
// must not be used -- and the origin root is not an acceptable retreat.
TEST(ChatStaticFiles, AuthCookiePathDeclinesOnAnUnusableServerDerivedPath)
{
   // a ";path=/" smuggled through the proxied URI would otherwise be emitted
   // verbatim, and RFC 6265 takes the last Path attribute
   EXPECT_EQ(authCookiePath("https://host/x;path=//ai-chat/index.html", "",
                            "https://host/"),
             "");

   // header-breaking characters in the derived prefix
   EXPECT_EQ(authCookiePath("https://host/a\r\nX-Injected: 1/ai-chat/index.html",
                            "", "https://host/"),
             "");

   // a prefix the browser could never match against a normalized request path
   EXPECT_EQ(authCookiePath("https://host/a//b/ai-chat/index.html", "",
                            "https://host/"),
             "");
}

TEST(ChatStaticFiles, AuthCookiePathIgnoresBogusClientBaseUrl)
{
   // does not end with the server-known prefix
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "https://host/elsewhere/", "https://host/rstudio/"),
             "/rstudio/");

   // not a usable URL or path
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            "garbage", "https://host/rstudio/"),
             "/rstudio/");
}

// proxiedUri is built from request headers that a co-hosted application on
// the same host can set, so a path that is merely well-formed is not enough:
// it must be one the session is actually served under, or the token would be
// delivered to a route of the caller's choosing.
TEST(ChatStaticFiles, AuthCookiePathDeclinesAServerPathTheSessionIsNotServedUnder)
{
   // a route named by the request headers, on a default-root deployment
   EXPECT_EQ(authCookiePath("https://host/evil/ai-chat/index.html",
                            "https://host/evil/", "https://host/"),
             "");

   // and on one reached through an external proxy prefix
   EXPECT_EQ(authCookiePath("https://host/evil/ai-chat/index.html", "",
                            "https://host/proxy/rstudio/"),
             "");

   // a prefix of a path the session is served under is not one of them
   // either: /rstudi is not the /rstudio segment
   EXPECT_EQ(authCookiePath("https://host/rstudi/ai-chat/index.html", "",
                            "https://host/rstudio/"),
             "");

   // the legitimate shapes all still authorize
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html", "",
                            "https://host/rstudio/"),
             "/rstudio/");
   EXPECT_EQ(authCookiePath("https://host/s/0aa27b1d6b8f34dc/ai-chat/index.html",
                            "", "https://host/proxy/s/0aa27b1d6b8f34dc/"),
             "/s/0aa27b1d6b8f34dc/");
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

   // an unescaped occurrence of the route in the query must not be taken for
   // the route itself: the derived prefix would then carry the document path
   // and the query along with it
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html"
                            "?wsUrl=/rstudio/p/abc/ai-chat/",
                            "", "https://host/rstudio/"),
             "/rstudio/");
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html"
                            "#/ai-chat/",
                            "", "https://host/rstudio/"),
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

// The parameter name exists as two bare literals, one here and one in
// loadChatUI() in ChatPresenter.java, with nothing tying them together at
// build time. Pin the wire name and the decoding, so at least this side
// cannot drift silently: if it did, the parameter would arrive empty and the
// cookie would fall back to the server-known path on every request.
TEST(ChatStaticFiles, ClientBaseUrlParameterRoundTripsThroughARequest)
{
   EXPECT_EQ(std::string(kClientBaseUrlParam), "clientBaseUrl");

   // the URI the IDE builds, with the base URL query-string encoded
   http::Request request;
   request.setUri("/ai-chat/index.html?wsUrl=%2Fp%2Fabc%2Fai-chat"
                  "&clientBaseUrl=https%3A%2F%2Fhost%2Fproxy%2Frstudio%2F");

   EXPECT_EQ(request.queryParamValue(kClientBaseUrlParam),
             "https://host/proxy/rstudio/");

   // and that decoded value is what authorizes the external prefix
   EXPECT_EQ(authCookiePath("https://host/rstudio/ai-chat/index.html",
                            request.queryParamValue(kClientBaseUrlParam),
                            "https://host/proxy/rstudio/"),
             "/proxy/rstudio/");

   // a request without it must not be read as reporting an empty base
   http::Request without;
   without.setUri("/ai-chat/index.html?wsUrl=%2Fp%2Fabc%2Fai-chat");
   EXPECT_EQ(without.queryParamValue(kClientBaseUrlParam), "");
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
