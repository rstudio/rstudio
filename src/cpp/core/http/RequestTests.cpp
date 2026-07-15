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

TEST(RequestTest, MoveAssignTransfersFields)
{
   Request src;
   src.setMethod("POST");
   src.setUri("/some/path");
   src.setHeader("X-Custom-Header", "custom-value");
   src.addCookie("session", "abc123");
   src.setUsername("someuser");
   src.setBody("request body contents");
   src.setContentLength(src.body().size());
   src.setRootPath("/proxy/root");
   src.setHandlerPrefix("/handler/prefix");

   Request dest;
   dest.assign(std::move(src));

   EXPECT_EQ("POST", dest.method());
   EXPECT_EQ("/some/path", dest.uri());
   EXPECT_EQ("custom-value", dest.headerValue("X-Custom-Header"));
   EXPECT_EQ("abc123", dest.cookieValue("session"));
   EXPECT_EQ("someuser", dest.username());
   EXPECT_EQ("request body contents", dest.body());
   EXPECT_EQ("/proxy/root", dest.rootPath());
   EXPECT_EQ("/handler/prefix", dest.handlerPrefix());
}

TEST(RequestTest, CopyAssignTransfersFields)
{
   Request src;
   src.setMethod("POST");
   src.setUri("/some/path");
   src.setHeader("X-Custom-Header", "custom-value");
   src.addCookie("session", "abc123");
   src.setUsername("someuser");
   src.setBody("request body contents");
   src.setContentLength(src.body().size());
   src.setRootPath("/proxy/root");
   src.setHandlerPrefix("/handler/prefix");

   Request dest;
   dest.assign(src);

   EXPECT_EQ("POST", dest.method());
   EXPECT_EQ("/some/path", dest.uri());
   EXPECT_EQ("custom-value", dest.headerValue("X-Custom-Header"));
   EXPECT_EQ("abc123", dest.cookieValue("session"));
   EXPECT_EQ("someuser", dest.username());
   EXPECT_EQ("request body contents", dest.body());
   EXPECT_EQ("/proxy/root", dest.rootPath());
   EXPECT_EQ("/handler/prefix", dest.handlerPrefix());

   // source is untouched by a copy assign
   EXPECT_EQ("POST", src.method());
   EXPECT_EQ("/some/path", src.uri());
   EXPECT_EQ("/proxy/root", src.rootPath());
   EXPECT_EQ("/handler/prefix", src.handlerPrefix());
}

TEST(RequestTest, MoveAssignTransfersFormFields)
{
   Request src;
   src.setContentType("application/x-www-form-urlencoded");
   src.setBody("field1=value1&field2=value2");
   src.setContentLength(src.body().size());

   // force form fields to be parsed and cached on src before the move
   ASSERT_EQ("value1", src.formFieldValue("field1"));

   Request dest;
   dest.assign(std::move(src));

   EXPECT_EQ("value1", dest.formFieldValue("field1"));
   EXPECT_EQ("value2", dest.formFieldValue("field2"));
}

TEST(RequestTest, MoveAssignTransfersFilesAndQueryParams)
{
   Request src;
   src.setUri("/some/path?param1=value1&param2=value2");

   std::string boundary = "TESTBOUNDARY";
   src.setContentType("multipart/form-data; boundary=" + boundary);
   src.setBody(
      "--" + boundary + "\r\n"
      "Content-Disposition: form-data; name=\"upload\"; filename=\"hello.txt\"\r\n"
      "Content-Type: text/plain\r\n"
      "\r\n"
      "hello file contents"
      "\r\n--" + boundary + "--\r\n");
   src.setContentLength(src.body().size());

   // force query params and uploaded files to be parsed and cached on src before the move
   ASSERT_EQ("value1", src.queryParamValue("param1"));
   ASSERT_EQ("hello file contents", src.uploadedFile("upload").contents);

   Request dest;
   dest.assign(std::move(src));

   EXPECT_EQ("value1", dest.queryParamValue("param1"));
   EXPECT_EQ("value2", dest.queryParamValue("param2"));

   const File& uploaded = dest.uploadedFile("upload");
   EXPECT_EQ("hello.txt", uploaded.name);
   EXPECT_EQ("text/plain", uploaded.contentType);
   EXPECT_EQ("hello file contents", uploaded.contents);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
