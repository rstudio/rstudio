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

TEST(HttpUtilTest, IsValidCookiePathAcceptsOriginAbsolutePaths)
{
   EXPECT_TRUE(isValidCookiePath("/"));
   EXPECT_TRUE(isValidCookiePath("/rstudio/"));
   EXPECT_TRUE(isValidCookiePath("/s/0aa27b1d6b8f34dc/"));

   // percent-escapes are how a path with reserved characters reaches us, and
   // are what the browser compares against, so they stay
   EXPECT_TRUE(isValidCookiePath("/pr%20oxy/rstudio/"));

   // RFC 6265 bars only controls and ';' from a path value, and a comma is
   // legal and unescaped in a URL path, so refusing it would only widen the
   // cookie on a deployment that uses one
   EXPECT_TRUE(isValidCookiePath("/a,b/rstudio/"));

   // an empty segment survives URL parsing, so this is the path a browser on
   // such a base URL sends and the cookie has to be scoped to match it
   EXPECT_TRUE(isValidCookiePath("//rstudio/"));
   EXPECT_TRUE(isValidCookiePath("/a//b/"));
}

TEST(HttpUtilTest, IsValidCookiePathRejectsHeaderBreakingCharacters)
{
   EXPECT_FALSE(isValidCookiePath("/pre;fix/"));
   EXPECT_FALSE(isValidCookiePath("/pre fix/"));
   EXPECT_FALSE(isValidCookiePath("/pre\r\nX-Injected: 1/"));
   EXPECT_FALSE(isValidCookiePath("/pre\\fix/"));
   EXPECT_FALSE(isValidCookiePath("/pre?fix/"));
   EXPECT_FALSE(isValidCookiePath("/pre#fix/"));
   EXPECT_FALSE(isValidCookiePath("/pre\x7F""fix/"));
}

// Bytes above 0x7F must be rejected the same way regardless of whether char
// is signed on this platform.
TEST(HttpUtilTest, IsValidCookiePathRejectsNonAsciiIndependentlyOfCharSignedness)
{
   EXPECT_FALSE(isValidCookiePath("/pr\xC3\xB8xy/"));
   EXPECT_FALSE(isValidCookiePath("/pre\x80""fix/"));
   EXPECT_FALSE(isValidCookiePath("/pre\xFF""fix/"));
}

TEST(HttpUtilTest, IsValidCookiePathRejectsUnnormalizedPaths)
{
   EXPECT_FALSE(isValidCookiePath(""));
   EXPECT_FALSE(isValidCookiePath("relative/"));
   EXPECT_FALSE(isValidCookiePath("/a/./b/"));
   EXPECT_FALSE(isValidCookiePath("/a/../b/"));
   EXPECT_FALSE(isValidCookiePath("/a/."));
   EXPECT_FALSE(isValidCookiePath("/a/.."));
}

// A root path may be configured without a leading slash, and with or without
// a trailing one. Request::rootPath() gives whatever it reads this shape, so
// anything reading the configured value directly has to agree with it -- a
// cookie path derived from the raw value would otherwise be rejected and
// widened to the origin.
TEST(HttpUtilTest, NormalizeRootPathAddsALeadingAndDropsATrailingSlash)
{
   EXPECT_EQ("/rstudio", normalizeRootPath("rstudio"));
   EXPECT_EQ("/rstudio", normalizeRootPath("/rstudio"));
   EXPECT_EQ("/rstudio", normalizeRootPath("/rstudio/"));
   EXPECT_EQ("/rstudio", normalizeRootPath("rstudio/"));
   EXPECT_EQ("/a/b", normalizeRootPath("a/b/"));

   // the root path keeps its only slash
   EXPECT_EQ("/", normalizeRootPath("/"));
   EXPECT_EQ("/", normalizeRootPath(""));

   // and the result is then usable as a cookie path, which is what the port
   // token cookie falls back to
   EXPECT_TRUE(isValidCookiePath(normalizeRootPath("rstudio")));
   EXPECT_TRUE(isValidCookiePath(normalizeRootPath("")));
}

} // namespace util
} // namespace http
} // namespace core
} // namespace rstudio
