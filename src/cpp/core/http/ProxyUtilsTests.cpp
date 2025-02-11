/*
 * ProxyUtilsTests.cpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

#include <core/http/ProxyUtils.hpp>

#include <core/system/Environment.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

test_context("ProxyUtilsTests")
{
   test_that("httpProxyUrl prefers lower case http_proxy")
   {
      system::setenv("http_proxy", "http://proxy.example.com:8080");
      system::setenv("HTTP_PROXY", "http://proxy2.example.com:8080");

      ProxyUtils utils;
      auto url = utils.httpProxyUrl("example.com", "443");
      REQUIRE(url.has_value());
      REQUIRE(url->hostname() == "proxy.example.com");
      REQUIRE(url->port() == 8080);
   }

   test_that("httpProxyUrl uses HTTP_PROXY if http_proxy is not set")
   {
      system::unsetenv("http_proxy");
      system::setenv("HTTP_PROXY", "http://proxy.example.com:8080");

      ProxyUtils utils;
      auto url = utils.httpProxyUrl("example.com", "443");
      REQUIRE(url.has_value());
      REQUIRE(url->hostname() == "proxy.example.com");
      REQUIRE(url->port() == 8080);
   }

   test_that("httpProxyUrl returns none if no proxy is set")
   {
      system::unsetenv("http_proxy");
      system::unsetenv("HTTP_PROXY");

      ProxyUtils utils;
      auto url = utils.httpProxyUrl("example.com", "443");
      REQUIRE_FALSE(url.has_value());
   }

   test_that("httpsProxyUrl prefers lower case https_proxy")
   {
      system::setenv("https_proxy", "http://proxy.example.com:8080");
      system::setenv("HTTPS_PROXY", "http://proxy2.example.com:8080");

      ProxyUtils utils;
      auto url = utils.httpsProxyUrl("example.com", "443");
      REQUIRE(url.has_value());
      REQUIRE(url->hostname() == "proxy.example.com");
      REQUIRE(url->port() == 8080);
   }

   test_that("httpsProxyUrl uses HTTPS_PROXY if https_proxy is not set")
   {
      system::unsetenv("https_proxy");
      system::setenv("HTTPS_PROXY", "http://proxy.example.com:8080");

      ProxyUtils utils;
      auto url = utils.httpsProxyUrl("example.com", "443");
      REQUIRE(url.has_value());
      REQUIRE(url->hostname() == "proxy.example.com");
      REQUIRE(url->port() == 8080);
   }

   test_that("httpsProxyUrl returns none if no proxy is set")
   {
      system::unsetenv("https_proxy");
      system::unsetenv("HTTPS_PROXY");

      ProxyUtils utils;
      auto url = utils.httpsProxyUrl("example.com", "443");
      REQUIRE_FALSE(url.has_value());
   }

   test_that("httpProxy returns none if address matches no_proxy rule")
   {
      system::setenv("http_proxy", "http://proxy.example.com:8080");
      system::setenv("no_proxy", "example.com");

      ProxyUtils utils;
      auto url = utils.httpProxyUrl("example.com", "443");
      REQUIRE_FALSE(url.has_value());
   }

   test_that("noProxyRules can be added")
   {
      ProxyUtils utils;

      system::setenv("http_proxy", "http://proxy.example.com:8080");
      system::setenv("no_proxy", "example.com");

      auto rule = core::http::createNoProxyRule("127.0.0.1", "8787");
      utils.addNoProxyRule(std::move(rule));

      // proxy other URLs
      REQUIRE(utils.httpProxyUrl("other.example.com", "443").has_value());
      REQUIRE(utils.httpProxyUrl("other.example.com").has_value());

      // proxy requests to localhost targeted at other ports
      REQUIRE(utils.httpProxyUrl("127.0.0.1", "8788").has_value());
      REQUIRE(utils.httpProxyUrl("127.0.0.1").has_value());

      // don't proxy for example.com directly
      REQUIRE_FALSE(utils.httpProxyUrl("example.com", "443").has_value());

      // don't proxy localhost on this port
      REQUIRE_FALSE(utils.httpProxyUrl("127.0.0.1", "8787").has_value());

   }
}

} // end namespace tests
} // end namespace http
} // end namespace core
} // end namespace rstudio
