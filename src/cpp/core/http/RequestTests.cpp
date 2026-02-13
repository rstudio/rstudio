/*
 * RequestTests.cpp
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

#include <gtest/gtest.h>

#include <core/http/Request.hpp>
#include <core/http/URL.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

void initForwardedRequest(
    Request* pReq,
    const std::string& host,
    const std::string& uri,
    const std::string& xForwardedHost = "",
    const std::string& xForwardedPort = "",
    const std::string& xForwardedProto = "")
{
   pReq->setHost(host);
   pReq->setUri(uri);
   if (!xForwardedHost.empty())
      pReq->setHeader("X-Forwarded-Host", xForwardedHost);
   if (!xForwardedPort.empty())
      pReq->setHeader("X-Forwarded-Port", xForwardedPort);
   if (!xForwardedProto.empty())
      pReq->setHeader("X-Forwarded-Proto", xForwardedProto);
}

TEST(RequestTest, ProxiedUriIPv4ForwardedHost)
{
   Request req;
   initForwardedRequest(&req,
       "localhost:8787", "/path",
       "example.com", "443", "https");
   EXPECT_EQ("https://example.com:443/path", req.proxiedUri());
}

TEST(RequestTest, ProxiedUriIPv4ForwardedHostReplacesPort)
{
   Request req;
   initForwardedRequest(&req,
       "localhost:8787", "/path",
       "example.com:8080", "443", "https");
   EXPECT_EQ("https://example.com:443/path", req.proxiedUri());
}

TEST(RequestTest, ProxiedUriIPv6BracketedForwardedHost)
{
   Request req;
   initForwardedRequest(&req,
       "localhost:8787", "/path",
       "[::1]", "8787", "http");
   EXPECT_EQ("http://[::1]:8787/path", req.proxiedUri());
}

TEST(RequestTest, ProxiedUriIPv6BracketedForwardedHostReplacesPort)
{
   Request req;
   initForwardedRequest(&req,
       "localhost:8787", "/path",
       "[::1]:9090", "8787", "http");
   EXPECT_EQ("http://[::1]:8787/path", req.proxiedUri());
}

TEST(RequestTest, ProxiedUriIPv6BracketedFullAddress)
{
   Request req;
   initForwardedRequest(&req,
       "localhost:8787", "/page",
       "[2001:db8::1]", "443", "https");
   EXPECT_EQ("https://[2001:db8::1]:443/page", req.proxiedUri());
}

TEST(RequestTest, ProxiedUriForwardedHostNoPort)
{
   Request req;
   initForwardedRequest(&req,
       "localhost:8787", "/path",
       "example.com", "", "https");
   EXPECT_EQ("https://example.com/path", req.proxiedUri());
}

TEST(RequestTest, ProxiedUriFallsBackToHost)
{
   Request req;
   initForwardedRequest(&req,
       "localhost:8787", "/path");
   EXPECT_EQ("http://localhost:8787/path", req.proxiedUri());
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
