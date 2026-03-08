/*
 * URLTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
#include <core/http/URL.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace util {

TEST(HttpTest, CanParseSimpleUrl)
{
   URL url("http://www.google.com");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("http", url.protocol());
   EXPECT_EQ("www.google.com", url.hostname());
   EXPECT_EQ("80", url.portStr());
   EXPECT_EQ(80, url.port());
   EXPECT_EQ(std::string(), url.path());
}

TEST(HttpTest, CanParseSimpleHttpsUrl)
{
   URL url("https://www.google.com");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("https", url.protocol());
   EXPECT_EQ("www.google.com", url.hostname());
   EXPECT_EQ("443", url.portStr());
   EXPECT_EQ(443, url.port());
   EXPECT_EQ(std::string(), url.path());
}
   

TEST(HttpTest, CanParseUrlWithPort)
{
   URL url("http://test.localhost:5235");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("http", url.protocol());
   EXPECT_EQ("test.localhost", url.hostname());
   EXPECT_EQ("5235", url.portStr());
   EXPECT_EQ(5235, url.port());
   EXPECT_EQ(std::string(), url.path());
}

TEST(HttpTest, CanParseUrlWithPath)
{
   URL url("http://google.com/search");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("http", url.protocol());
   EXPECT_EQ("google.com", url.hostname());
   EXPECT_EQ("80", url.portStr());
   EXPECT_EQ(80, url.port());
   EXPECT_EQ("/search", url.path());
}

TEST(HttpTest, CanParseComplexUrl)
{
   URL url("https://localhost:9987/a/long/path/?term=happy&term=dog");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("https", url.protocol());
   EXPECT_EQ("localhost", url.hostname());
   EXPECT_EQ("9987", url.portStr());
   EXPECT_EQ(9987, url.port());
   EXPECT_EQ("/a/long/path/?term=happy&term=dog", url.path());
}

TEST(HttpTest, BadUrlsReturnParseFailure)
{
   URL url("127.0.0.1");

   EXPECT_FALSE(url.isValid());
}

TEST(HttpTest, CanCleanUpPaths)
{
   EXPECT_EQ("", URL::cleanupPath(""));
   EXPECT_EQ("/", URL::cleanupPath("/"));
   EXPECT_EQ("", URL::cleanupPath("./"));
   EXPECT_EQ("/", URL::cleanupPath("/./"));
   EXPECT_EQ("/.", URL::cleanupPath("/."));
   EXPECT_EQ("/", URL::cleanupPath("/foo/../"));
   EXPECT_EQ("", URL::cleanupPath("foo/../"));
   EXPECT_EQ("/", URL::cleanupPath("/foo/bar/../../"));
   EXPECT_EQ("", URL::cleanupPath("foo/bar/../../"));
   EXPECT_EQ("/", URL::cleanupPath("/foo/bar/../../"));
   EXPECT_EQ("/foo/..", URL::cleanupPath("/foo/bar/../.."));
   EXPECT_EQ("/foo/?/../", URL::cleanupPath("/foo/?/../"));
   EXPECT_EQ("/foo/#/../", URL::cleanupPath("/foo/#/../"));
   EXPECT_EQ("/foo/?/../#/../", URL::cleanupPath("/foo/?/../#/../"));
   EXPECT_EQ("/", URL::cleanupPath("/foo/bar/../../../"));
   EXPECT_EQ("/baz", URL::cleanupPath("/foo/bar/../../../baz"));
}

TEST(HttpTest, CanParseIPv6LoopbackDefaultPort)
{
   URL url("http://[::1]/path");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("http", url.protocol());
   EXPECT_EQ("::1", url.hostname());
   EXPECT_EQ("80", url.portStr());
   EXPECT_EQ(80, url.port());
   EXPECT_EQ("/path", url.path());
}

TEST(HttpTest, CanParseIPv6LoopbackWithPort)
{
   URL url("https://[::1]:9443/path");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("https", url.protocol());
   EXPECT_EQ("::1", url.hostname());
   EXPECT_EQ("9443", url.portStr());
   EXPECT_EQ(9443, url.port());
   EXPECT_EQ("/path", url.path());
}

TEST(HttpTest, CanParseIPv6FullAddressWithPortAndQuery)
{
   URL url("http://[2001:db8::1]:8080/path?query=1");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("http", url.protocol());
   EXPECT_EQ("2001:db8::1", url.hostname());
   EXPECT_EQ("8080", url.portStr());
   EXPECT_EQ(8080, url.port());
   EXPECT_EQ("/path?query=1", url.path());
}

TEST(HttpTest, CanParseIPv4MappedIPv6Address)
{
   URL url("http://[::ffff:192.168.1.1]:3000/");

   EXPECT_TRUE(url.isValid());
   EXPECT_EQ("http", url.protocol());
   EXPECT_EQ("::ffff:192.168.1.1", url.hostname());
   EXPECT_EQ("3000", url.portStr());
   EXPECT_EQ(3000, url.port());
   EXPECT_EQ("/", url.path());
}

TEST(HttpTest, FormatHostPortIPv4)
{
   EXPECT_EQ("192.168.1.1:8080", URL::formatHostPort("192.168.1.1", "8080"));
   EXPECT_EQ("example.com:443", URL::formatHostPort("example.com", "443"));
}

TEST(HttpTest, FormatHostPortBareIPv6)
{
   EXPECT_EQ("[::1]:8080", URL::formatHostPort("::1", "8080"));
   EXPECT_EQ("[2001:db8::1]:443", URL::formatHostPort("2001:db8::1", "443"));
}

TEST(HttpTest, FormatHostPortBracketedIPv6)
{
   EXPECT_EQ("[::1]:8080", URL::formatHostPort("[::1]", "8080"));
   EXPECT_EQ("[2001:db8::1]:443", URL::formatHostPort("[2001:db8::1]", "443"));
}

TEST(HttpTest, FormatAddress)
{
   EXPECT_EQ("http://192.168.1.1:80", URL::formatAddress("http", "192.168.1.1", "80"));
   EXPECT_EQ("https://[::1]:443", URL::formatAddress("https", "::1", "443"));
   EXPECT_EQ("http://[2001:db8::1]:8080", URL::formatAddress("http", "2001:db8::1", "8080"));
}

TEST(HttpTest, CanCompleteIPv6Urls)
{
   EXPECT_EQ("http://[::1]/foo", URL::complete("http://[::1]", "foo"));
   EXPECT_EQ("http://[::1]:8080/bar", URL::complete("http://[::1]:8080/foo", "bar"));
   EXPECT_EQ("http://[::1]:8080/foo/bar", URL::complete("http://[::1]:8080/foo/", "bar"));
   EXPECT_EQ("http://[::1]:8080/bar", URL::complete("http://[::1]:8080/foo/", "/bar"));
}

TEST(HttpTest, CanCompleteUrls)
{
   EXPECT_EQ("http://www.example.com/foo", URL::complete("http://www.example.com", "foo"));
   EXPECT_EQ("http://www.example.com/bar", URL::complete("http://www.example.com/foo", "bar"));
   EXPECT_EQ("http://www.example.com/foo/bar", URL::complete("http://www.example.com/foo/", "bar"));
   EXPECT_EQ("http://www.example.com:80/bar", URL::complete("http://www.example.com:80/foo/", "/bar"));
   EXPECT_EQ("http://www.example.com:80/foo/baz/qux", URL::complete("http://www.example.com:80/foo/bar", "baz/qux"));
   EXPECT_EQ("http://www.example.com:80/baz/qux", URL::complete("http://www.example.com:80/foo/bar", "../baz/qux"));
   EXPECT_EQ("http://www.example.com:80/foo/baz/qux", URL::complete("http://www.example.com:80/foo/bar/", "../baz/qux"));
   EXPECT_EQ("http://www.example.com:80/foo/bar/qux", URL::complete("http://www.example.com:80/foo/bar/", "baz/../qux"));
   EXPECT_EQ("http://baz", URL::complete("http://www.example.com:80/foo/bar", "http://baz"));

   EXPECT_EQ("foo/bar/baz/qux", URL::complete("foo/bar/", "baz/qux"));
   EXPECT_EQ("foo/baz/qux", URL::complete("foo/bar/", "../baz/qux"));
   EXPECT_EQ("foo/baz/qux", URL::complete("../foo/bar/", "../baz/qux"));
   EXPECT_EQ("foo/baz/qux", URL::complete("../../foo/bar/", "../baz/qux"));
}

TEST(HttpTest, CanUncompleteUrls)
{
   EXPECT_EQ("../qux/quux", URL::uncomplete("/foo/bar/baz", "/foo/qux/quux"));
   EXPECT_EQ("../../qux/quux", URL::uncomplete("/foo/bar/baz/", "/foo/qux/quux"));
   EXPECT_EQ("../qux/quux", URL::uncomplete("/bar/baz", "/qux/quux"));
}

} // namespace util
} // namespace http
} // namespace core
} // namespace rstudio