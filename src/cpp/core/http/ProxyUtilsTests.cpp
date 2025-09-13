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

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

TEST(HttpTest, HttpProxyUrlPrefersLowerCase)
{
   system::setenv("http_proxy", "http://proxy.example.com:8080");
   system::setenv("HTTP_PROXY", "http://proxy2.example.com:8080");

   ProxyUtils utils;
   auto url = utils.httpProxyUrl("example.com", "443");
   ASSERT_TRUE(url.has_value());
   EXPECT_EQ("proxy.example.com", url->hostname());
   EXPECT_EQ(8080, url->port());
}

TEST(HttpTest, HttpProxyUrlFallsBackToUpperCase)
{
   system::unsetenv("http_proxy");
   system::setenv("HTTP_PROXY", "http://proxy.example.com:8080");

   ProxyUtils utils;
   auto url = utils.httpProxyUrl("example.com", "443");
   ASSERT_TRUE(url.has_value());
   EXPECT_EQ("proxy.example.com", url->hostname());
   EXPECT_EQ(8080, url->port());
}

TEST(HttpTest, HttpProxyUrlReturnsNoneWhenNotSet)
{
   system::unsetenv("http_proxy");
   system::unsetenv("HTTP_PROXY");

   ProxyUtils utils;
   auto url = utils.httpProxyUrl("example.com", "443");
   EXPECT_FALSE(url.has_value());
}

TEST(HttpTest, HttpsProxyUrlPrefersLowerCase)
{
   system::setenv("https_proxy", "http://proxy.example.com:8080");
   system::setenv("HTTPS_PROXY", "http://proxy2.example.com:8080");

   ProxyUtils utils;
   auto url = utils.httpsProxyUrl("example.com", "443");
   ASSERT_TRUE(url.has_value());
   EXPECT_EQ("proxy.example.com", url->hostname());
   EXPECT_EQ(8080, url->port());
}

TEST(HttpTest, HttpsProxyUrlFallsBackToUpperCase)
{
   system::unsetenv("https_proxy");
   system::setenv("HTTPS_PROXY", "http://proxy.example.com:8080");

   ProxyUtils utils;
   auto url = utils.httpsProxyUrl("example.com", "443");
   ASSERT_TRUE(url.has_value());
   EXPECT_EQ("proxy.example.com", url->hostname());
   EXPECT_EQ(8080, url->port());
}

TEST(HttpTest, HttpsProxyUrlReturnsNoneWhenNotSet)
{
   system::unsetenv("https_proxy");
   system::unsetenv("HTTPS_PROXY");

   ProxyUtils utils;
   auto url = utils.httpsProxyUrl("example.com", "443");
   EXPECT_FALSE(url.has_value());
}

TEST(HttpTest, HttpProxyRespectsNoProxyRule)
{
   system::setenv("http_proxy", "http://proxy.example.com:8080");
   system::setenv("no_proxy", "example.com");

   ProxyUtils utils;
   auto url = utils.httpProxyUrl("example.com", "443");
   EXPECT_FALSE(url.has_value());
}

TEST(HttpTest, NoProxyRulesCanBeAdded)
{
   system::setenv("http_proxy", "http://proxy.example.com:8080");
   system::setenv("no_proxy", "example.com");

   // Create utils after setting environment variables
   ProxyUtils utils;
   
   auto rule = core::http::createNoProxyRule("127.0.0.1:8787");
   utils.addNoProxyRule(std::move(rule));

   // proxy other URLs
   EXPECT_TRUE(utils.httpProxyUrl("other.example.com", "443").has_value());
   EXPECT_TRUE(utils.httpProxyUrl("other.example.com").has_value());

   // proxy requests to localhost targeted at other ports
   EXPECT_TRUE(utils.httpProxyUrl("127.0.0.1", "8788").has_value());
   EXPECT_TRUE(utils.httpProxyUrl("127.0.0.1").has_value());

   // don't proxy for example.com directly
   EXPECT_FALSE(utils.httpProxyUrl("example.com", "443").has_value());

   // don't proxy localhost on this port
   EXPECT_FALSE(utils.httpProxyUrl("127.0.0.1", "8787").has_value());
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio