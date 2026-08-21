/*
 * UtilTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <gtest/gtest.h>

#include <core/http/Util.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace util {

// When the client path equals the server path the server path must be
// returned byte-identical, so deployments without an external proxy prefix
// keep exactly the cookie paths they have today.
TEST(HttpUtilTest, CookiePathUnchangedWhenClientMatchesServer)
{
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio", "https://host/rstudio/"));
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio", "https://host/rstudio"));
   EXPECT_EQ("/rstudio/",
             cookiePathWithExternalPrefix("/rstudio/", "https://host/rstudio/"));
   EXPECT_EQ("/s/0aa27b1d6b8f34dc/",
             cookiePathWithExternalPrefix("/s/0aa27b1d6b8f34dc/",
                                          "https://host/s/0aa27b1d6b8f34dc/"));
   EXPECT_EQ("/", cookiePathWithExternalPrefix("/", "https://host/"));
   EXPECT_EQ("/", cookiePathWithExternalPrefix("/", "https://host"));
}

TEST(HttpUtilTest, CookiePathGainsExternalProxyPrefix)
{
   // configured www-root-path behind an unknown prefix-stripping proxy
   EXPECT_EQ("/proxy/rstudio",
             cookiePathWithExternalPrefix("/rstudio",
                                          "https://host/proxy/rstudio/"));

   // trailing slash style of the server path is preserved
   EXPECT_EQ("/proxy/rstudio/",
             cookiePathWithExternalPrefix("/rstudio/",
                                          "https://host/proxy/rstudio/"));

   // Workbench session prefix behind an unknown proxy prefix
   EXPECT_EQ("/proxy/s/0aa27b1d6b8f34dc/",
             cookiePathWithExternalPrefix("/s/0aa27b1d6b8f34dc/",
                                          "https://host/proxy/s/0aa27b1d6b8f34dc/"));

   // Open OnDemand style multi-segment prefix over the default root path
   EXPECT_EQ("/rnode/node01/8787/",
             cookiePathWithExternalPrefix("/",
                                          "https://ood.host/rnode/node01/8787/"));
}

// The recovered prefix makes the result a sibling of the server path, not a
// subpath of it, so callers must trust clientBaseUrl -- which is why the
// assistant auth cookie authorizes its query parameter against the base
// recorded at client_init before calling this.
TEST(HttpUtilTest, CookiePathPrefixIsASiblingSubtreeNotASubpath)
{
   EXPECT_EQ("/evil/rstudio/",
             cookiePathWithExternalPrefix("/rstudio/",
                                          "https://host/evil/rstudio/"));
}

TEST(HttpUtilTest, CookiePathAcceptsBareClientPath)
{
   EXPECT_EQ("/proxy/rstudio",
             cookiePathWithExternalPrefix("/rstudio", "/proxy/rstudio/"));
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio", "/rstudio/"));
}

TEST(HttpUtilTest, CookiePathIgnoresQueryAndFragment)
{
   EXPECT_EQ("/proxy/rstudio",
             cookiePathWithExternalPrefix(
                "/rstudio", "https://host/proxy/rstudio/?foo=bar#frag"));
}

TEST(HttpUtilTest, CookiePathFallsBackWhenClientDoesNotEndWithServerPath)
{
   // entirely different path
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio", "https://host/other/"));

   // shorter than the server path
   EXPECT_EQ("/s/0aa27b1d6b8f34dc/",
             cookiePathWithExternalPrefix("/s/0aa27b1d6b8f34dc/",
                                          "https://host/"));

   // suffix must match at a path segment boundary
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio",
                                          "https://host/proxyrstudio/"));
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio",
                                          "https://host/proxy/xrstudio/"));
}

TEST(HttpUtilTest, CookiePathFallsBackOnEmptyOrMalformedClientInput)
{
   EXPECT_EQ("/rstudio", cookiePathWithExternalPrefix("/rstudio", ""));
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio", "not a url"));
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio", "relative/rstudio/"));
}

TEST(HttpUtilTest, CookiePathRejectsCharactersInvalidInCookiePath)
{
   // characters that could break out of the Set-Cookie path attribute
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio",
                                          "/pre;fix/rstudio/"));
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio",
                                          "/pre fix/rstudio/"));
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio",
                                          "/pre\r\nfix/rstudio/"));
   EXPECT_EQ("/rstudio",
             cookiePathWithExternalPrefix("/rstudio",
                                          "/pre,fix/rstudio/"));
}

} // namespace util
} // namespace http
} // namespace core
} // namespace rstudio
