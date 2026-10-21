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
#include <core/system/Environment.hpp>

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

TEST(RSessionContextTests, ObfuscatedUserIdRemapsLargeUidsUpToMaxUid)
{
   // obfuscatedUserId() reads the minimum uid from this variable when it is set
   core::system::EnvironmentScope minUidScope("RSTUDIO_MINIMUM_USER_ID", "1000");
   const uid_t kMaxUid = 0xFFFFF;

   // a large uid at most kMaxUid above the minimum is remapped to that
   // difference, so it gets the same ID as a uid equal to the difference
   EXPECT_EQ(obfuscatedUserId(1000 + kMaxUid - 1), obfuscatedUserId(kMaxUid - 1));
   EXPECT_EQ(obfuscatedUserId(1000 + kMaxUid), obfuscatedUserId(kMaxUid));
}

TEST(RSessionContextTests, LegacyObfuscatedUserIdPreservesPreviousBoundary)
{
   const uid_t kMaxUid = 0xFFFFF;
   const uid_t kBoundaryUid = 1000 + kMaxUid;

   // with a minimum uid of 0, no uid above kMaxUid is remapped, so this is the ID
   // that kBoundaryUid got before the boundary fix; server builds obfuscate IDs,
   // so the expected values are computed rather than written as literals
   std::string unremappedId;
   {
      core::system::EnvironmentScope minUidScope("RSTUDIO_MINIMUM_USER_ID", "0");
      unremappedId = obfuscatedUserId(kBoundaryUid);
   }

   core::system::EnvironmentScope minUidScope("RSTUDIO_MINIMUM_USER_ID", "1000");
   EXPECT_EQ(legacyObfuscatedUserId(kBoundaryUid), unremappedId);
   EXPECT_NE(legacyObfuscatedUserId(kBoundaryUid), obfuscatedUserId(kBoundaryUid));
   for (uid_t uid : {0u, 1000u, kMaxUid, kBoundaryUid - 1, kBoundaryUid + 1})
      EXPECT_EQ(legacyObfuscatedUserId(uid), obfuscatedUserId(uid));
}

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
