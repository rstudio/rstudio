/*
 * RSessionContextTests.cpp
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

#include <core/r_util/RSessionContext.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace r_util {

namespace {

// A canonical session URL body: 5 hex user + 8 hex project + 8 hex session.
const char* const kUser    = "abcde";
const char* const kProject = "12345678";
const char* const kSession = "0a1b2c3d";

std::string makeUrl(const std::string& prefix)
{
   return "/" + prefix + "/" + kUser + kProject + kSession + "/workspaces/";
}

} // anonymous namespace

TEST(RSessionContextTests, ParseSessionUrlPrefixS)
{
   SessionScope scope;
   std::string urlPrefix;
   std::string urlWithoutPrefix;
   parseSessionUrl(makeUrl("s"), &scope, &urlPrefix, &urlWithoutPrefix);

   EXPECT_FALSE(scope.empty());
   EXPECT_EQ(scope.id(), kSession);
   EXPECT_EQ(urlPrefix, std::string("/s/") + kUser + kProject + kSession + "/");
   EXPECT_EQ(urlWithoutPrefix, "/workspaces/");
}

TEST(RSessionContextTests, ParseSessionUrlPrefixN)
{
   SessionScope scope;
   std::string urlPrefix;
   std::string urlWithoutPrefix;
   parseSessionUrl(makeUrl("n"), &scope, &urlPrefix, &urlWithoutPrefix);

   EXPECT_FALSE(scope.empty());
   EXPECT_EQ(scope.id(), kSession);
   EXPECT_EQ(urlPrefix, std::string("/n/") + kUser + kProject + kSession + "/");
   EXPECT_EQ(urlWithoutPrefix, "/workspaces/");
}

TEST(RSessionContextTests, ParseSessionUrlUnknownPrefixDoesNotMatch)
{
   SessionScope scope;
   std::string urlPrefix;
   std::string urlWithoutPrefix;
   // A prefix outside [snp] must not be treated as a session URL.
   parseSessionUrl(makeUrl("x"), &scope, &urlPrefix, &urlWithoutPrefix);

   EXPECT_TRUE(scope.empty());
   EXPECT_TRUE(urlPrefix.empty());
   EXPECT_EQ(urlWithoutPrefix, makeUrl("x"));
}

TEST(RSessionContextTests, ParseSessionUrlEnvVarsWithoutSessionSegment)
{
   // RStudio Server URLs carry no session segment, so the whole URL is the
   // server URL.
   std::string serverUrl;
   std::string sessionUrl;
   parseSessionUrlEnvVars("https://rstudio.example.com/", &serverUrl, &sessionUrl);

   EXPECT_EQ(serverUrl, "https://rstudio.example.com/");
   EXPECT_TRUE(sessionUrl.empty());
}

TEST(RSessionContextTests, ParseSessionUrlEnvVarsWithoutSessionSegmentUnderProxyPath)
{
   // Still no session segment when served under a reverse-proxy sub-path, so
   // the sub-path stays on the server URL with nothing on the session side.
   std::string serverUrl;
   std::string sessionUrl;
   parseSessionUrlEnvVars("https://rstudio.example.com/rstudio/", &serverUrl, &sessionUrl);

   EXPECT_EQ(serverUrl, "https://rstudio.example.com/rstudio/");
   EXPECT_TRUE(sessionUrl.empty());
}

} // namespace r_util
} // namespace core
} // namespace rstudio
