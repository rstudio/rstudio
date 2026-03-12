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

#include <tests/TestThat.hpp>

#include <core/http/Util.hpp>
#include <core/http/URL.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace util {

test_context("HttpUtil Tests")
{
   test_that("Can parse simple url")
   {
      URL url("http://www.google.com");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "www.google.com");
      expect_true(url.portStr() == "80");
      expect_true(url.port() == 80);
      expect_true(url.path() == std::string());
   }

   test_that("Can parse simple https url")
   {
      URL url("https://www.google.com");

      expect_true(url.isValid());
      expect_true(url.protocol() == "https");
      expect_true(url.hostname() == "www.google.com");
      expect_true(url.portStr() == "443");
      expect_true(url.port() == 443);
      expect_true(url.path() == std::string());
   }
   
   test_that("Can parse url with port")
   {
      URL url("http://test.localhost:5235");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "test.localhost");
      expect_true(url.portStr() == "5235");
      expect_true(url.port() == 5235);
      expect_true(url.path() == std::string());
   }

   test_that("Can parse url with path")
   {
      URL url("http://google.com/search");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "google.com");
      expect_true(url.portStr() == "80");
      expect_true(url.port() == 80);
      expect_true(url.path() == "/search");
   }

   test_that("Can parse complex url")
   {
      URL url("https://localhost:9987/a/long/path/?term=happy&term=dog");

      expect_true(url.isValid());
      expect_true(url.protocol() == "https");
      expect_true(url.hostname() == "localhost");
      expect_true(url.portStr() == "9987");
      expect_true(url.port() == 9987);
      expect_true(url.path() == "/a/long/path/?term=happy&term=dog");
   }

   test_that("Bad urls return parse failure")
   {
      URL url("127.0.0.1");

      expect_false(url.isValid());
   }

   test_that("Can clean up paths")
   {
      expect_equal(URL::cleanupPath(""), "");
      expect_equal(URL::cleanupPath("/"), "/");
      expect_equal(URL::cleanupPath("./"), "");
      expect_equal(URL::cleanupPath("/./"), "/");
      expect_equal(URL::cleanupPath("/."), "/.");
      expect_equal(URL::cleanupPath("/foo/../"), "/");
      expect_equal(URL::cleanupPath("foo/../"), "");
      expect_equal(URL::cleanupPath("/foo/bar/../../"), "/");
      expect_equal(URL::cleanupPath("foo/bar/../../"), "");
      expect_equal(URL::cleanupPath("/foo/bar/../../"), "/");
      expect_equal(URL::cleanupPath("/foo/bar/../.."), "/foo/..");
      expect_equal(URL::cleanupPath("/foo/?/../"), "/foo/?/../");
      expect_equal(URL::cleanupPath("/foo/#/../"), "/foo/#/../");
      expect_equal(URL::cleanupPath("/foo/?/../#/../"), "/foo/?/../#/../");
      expect_equal(URL::cleanupPath("/foo/bar/../../../"), "/");
      expect_equal(URL::cleanupPath("/foo/bar/../../../baz"), "/baz");
   }

   test_that("Can complete URLs")
   {
      expect_equal(URL::complete("http://www.example.com", "foo"), "http://www.example.com/foo");
      expect_equal(URL::complete("http://www.example.com/foo", "bar"), "http://www.example.com/bar");
      expect_equal(URL::complete("http://www.example.com/foo/", "bar"), "http://www.example.com/foo/bar");
      expect_equal(URL::complete("http://www.example.com:80/foo/", "/bar"), "http://www.example.com:80/bar");
      expect_equal(URL::complete("http://www.example.com:80/foo/bar", "baz/qux"), "http://www.example.com:80/foo/baz/qux");
      expect_equal(URL::complete("http://www.example.com:80/foo/bar", "../baz/qux"), "http://www.example.com:80/baz/qux");
      expect_equal(URL::complete("http://www.example.com:80/foo/bar/", "../baz/qux"), "http://www.example.com:80/foo/baz/qux");
      expect_equal(URL::complete("http://www.example.com:80/foo/bar/", "baz/../qux"), "http://www.example.com:80/foo/bar/qux");
      expect_equal(URL::complete("http://www.example.com:80/foo/bar", "http://baz"), "http://baz");

      expect_equal(URL::complete("foo/bar/", "baz/qux"), "foo/bar/baz/qux");
      expect_equal(URL::complete("foo/bar/", "../baz/qux"), "foo/baz/qux");
      expect_equal(URL::complete("../foo/bar/", "../baz/qux"), "foo/baz/qux");
      expect_equal(URL::complete("../../foo/bar/", "../baz/qux"), "foo/baz/qux");
   }

   test_that("Can uncomplete URLs")
   {
      expect_equal(URL::uncomplete("/foo/bar/baz", "/foo/qux/quux"), "../qux/quux");
      expect_equal(URL::uncomplete("/foo/bar/baz/", "/foo/qux/quux"), "../../qux/quux");
      expect_equal(URL::uncomplete("/bar/baz", "/qux/quux"), "../qux/quux");
   }
}

   test_that("Can parse IPv6 loopback with default port")
   {
      URL url("http://[::1]/path");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "::1");
      expect_true(url.portStr() == "80");
      expect_true(url.port() == 80);
      expect_true(url.path() == "/path");
   }

   test_that("Can parse IPv6 loopback with port")
   {
      URL url("https://[::1]:9443/path");

      expect_true(url.isValid());
      expect_true(url.protocol() == "https");
      expect_true(url.hostname() == "::1");
      expect_true(url.portStr() == "9443");
      expect_true(url.port() == 9443);
      expect_true(url.path() == "/path");
   }

   test_that("Can parse IPv6 full address with port and query")
   {
      URL url("http://[2001:db8::1]:8080/path?query=1");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "2001:db8::1");
      expect_true(url.portStr() == "8080");
      expect_true(url.port() == 8080);
      expect_true(url.path() == "/path?query=1");
   }

   test_that("Can parse IPv4-mapped IPv6 address")
   {
      URL url("http://[::ffff:192.168.1.1]:3000/");

      expect_true(url.isValid());
      expect_true(url.protocol() == "http");
      expect_true(url.hostname() == "::ffff:192.168.1.1");
      expect_true(url.portStr() == "3000");
      expect_true(url.port() == 3000);
      expect_true(url.path() == "/");
   }

   test_that("formatHostPort works for IPv4")
   {
      expect_equal(URL::formatHostPort("192.168.1.1", "8080"), std::string("192.168.1.1:8080"));
      expect_equal(URL::formatHostPort("example.com", "443"), std::string("example.com:443"));
   }

   test_that("formatHostPort works for bare IPv6")
   {
      expect_equal(URL::formatHostPort("::1", "8080"), std::string("[::1]:8080"));
      expect_equal(URL::formatHostPort("2001:db8::1", "443"), std::string("[2001:db8::1]:443"));
   }

   test_that("formatHostPort works for bracketed IPv6")
   {
      expect_equal(URL::formatHostPort("[::1]", "8080"), std::string("[::1]:8080"));
      expect_equal(URL::formatHostPort("[2001:db8::1]", "443"), std::string("[2001:db8::1]:443"));
   }

   test_that("formatAddress works")
   {
      expect_equal(URL::formatAddress("http", "192.168.1.1", "80"), std::string("http://192.168.1.1:80"));
      expect_equal(URL::formatAddress("https", "::1", "443"), std::string("https://[::1]:443"));
      expect_equal(URL::formatAddress("http", "2001:db8::1", "8080"), std::string("http://[2001:db8::1]:8080"));
   }

   test_that("Can complete IPv6 URLs")
   {
      expect_equal(URL::complete("http://[::1]", "foo"), std::string("http://[::1]/foo"));
      expect_equal(URL::complete("http://[::1]:8080/foo", "bar"), std::string("http://[::1]:8080/bar"));
      expect_equal(URL::complete("http://[::1]:8080/foo/", "bar"), std::string("http://[::1]:8080/foo/bar"));
      expect_equal(URL::complete("http://[::1]:8080/foo/", "/bar"), std::string("http://[::1]:8080/bar"));
   }
}

} // end namespace util
} // end namespace http
} // end namespace core
} // end namespace rstudio
