/*
 * SessionNodeToolsTests.cpp
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

#include "SessionNodeTools.hpp"

#include <gtest/gtest.h>

#include <core/system/Environment.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace node_tools {
namespace tests {

TEST(SessionNodeToolsTest, AppendNodeOptionEmpty)
{
   EXPECT_EQ(appendNodeOption("", "--use-system-ca"), "--use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionPreservesExisting)
{
   EXPECT_EQ(appendNodeOption("--max-old-space-size=4096", "--use-system-ca"),
             "--max-old-space-size=4096 --use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionIdempotentSingle)
{
   EXPECT_EQ(appendNodeOption("--use-system-ca", "--use-system-ca"),
             "--use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionIdempotentFirstAndMiddle)
{
   EXPECT_EQ(appendNodeOption("--use-system-ca --enable-source-maps", "--use-system-ca"),
             "--use-system-ca --enable-source-maps");
   EXPECT_EQ(appendNodeOption("--a --use-system-ca --b", "--use-system-ca"),
             "--a --use-system-ca --b");
}

TEST(SessionNodeToolsTest, AppendNodeOptionIdempotentLast)
{
   EXPECT_EQ(appendNodeOption("--a --use-system-ca", "--use-system-ca"),
             "--a --use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionPreservesQuotedWhitespace)
{
   // Quoted values with intentional internal whitespace must not be rewritten.
   EXPECT_EQ(appendNodeOption("--require \"/my  app/x.js\"", "--use-system-ca"),
             "--require \"/my  app/x.js\" --use-system-ca");
}

TEST(SessionNodeToolsTest, AppendNodeOptionMatchesWholeTokenOnly)
{
   // A longer option that merely starts with the flag is not a match.
   EXPECT_EQ(appendNodeOption("--use-system-cafoo", "--use-system-ca"),
             "--use-system-cafoo --use-system-ca");
}

TEST(SessionNodeToolsTest, ParseNodeVersionTypical)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v22.22.2\n", &major, &minor));
   EXPECT_EQ(major, 22);
   EXPECT_EQ(minor, 22);
}

TEST(SessionNodeToolsTest, ParseNodeVersionBoundary)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v22.17.0", &major, &minor));
   EXPECT_EQ(major, 22);
   EXPECT_EQ(minor, 17);
}

TEST(SessionNodeToolsTest, ParseNodeVersionBelowGate)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v20.19.4", &major, &minor));
   EXPECT_EQ(major, 20);
   EXPECT_EQ(minor, 19);
}

TEST(SessionNodeToolsTest, ParseNodeVersionMalformed)
{
   int major = 0, minor = 0;
   EXPECT_FALSE(parseNodeVersion("", &major, &minor));
   EXPECT_FALSE(parseNodeVersion("not-a-version", &major, &minor));
}

TEST(SessionNodeToolsTest, VersionSupportsSystemCaBoundary)
{
   EXPECT_FALSE(versionSupportsSystemCa(22, 16));
   EXPECT_TRUE(versionSupportsSystemCa(22, 17));
   EXPECT_TRUE(versionSupportsSystemCa(22, 18));
   EXPECT_TRUE(versionSupportsSystemCa(23, 0));
   EXPECT_FALSE(versionSupportsSystemCa(21, 99));
   EXPECT_FALSE(versionSupportsSystemCa(20, 19));
}

TEST(SessionNodeToolsTest, ParseNodeVersionNoMinor)
{
   int major = 0, minor = 0;
   EXPECT_FALSE(parseNodeVersion("v22", &major, &minor));
}

TEST(SessionNodeToolsTest, ParseNodeVersionLeadingWhitespace)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("  v22.17.0\n", &major, &minor));
   EXPECT_EQ(major, 22);
   EXPECT_EQ(minor, 17);
}

TEST(SessionNodeToolsTest, ParseNodeVersionEmbeddedSuffix)
{
   int major = 0, minor = 0;
   EXPECT_TRUE(parseNodeVersion("v22.17.0-nightly", &major, &minor));
   EXPECT_EQ(major, 22);
   EXPECT_EQ(minor, 17);
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlAddsSchemeToHostAndPort)
{
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("proxy.example.com:8080", &out));
   EXPECT_EQ(out, "http://proxy.example.com:8080");
   EXPECT_TRUE(normalizeProxyUrl("192.168.1.1:3128", &out));
   EXPECT_EQ(out, "http://192.168.1.1:3128");
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlAddsSchemeToBareHost)
{
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("proxy", &out));
   EXPECT_EQ(out, "http://proxy");
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlKeepsUrlsWithScheme)
{
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("http://proxy.example.com:8080/", &out));
   EXPECT_EQ(out, "http://proxy.example.com:8080/");
   EXPECT_TRUE(normalizeProxyUrl("HTTPS://PROXY.EXAMPLE.COM:8080", &out));
   EXPECT_EQ(out, "HTTPS://PROXY.EXAMPLE.COM:8080");
   EXPECT_TRUE(normalizeProxyUrl("http://user:p%40ss@proxy.example.com:8080", &out));
   EXPECT_EQ(out, "http://user:p%40ss@proxy.example.com:8080");
   EXPECT_TRUE(normalizeProxyUrl("socks5://proxy.example.com:1080", &out));
   EXPECT_EQ(out, "socks5://proxy.example.com:1080");
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlStripsQuotesAndWhitespace)
{
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("\"http://proxy.example.com:8080\"", &out));
   EXPECT_EQ(out, "http://proxy.example.com:8080");
   EXPECT_TRUE(normalizeProxyUrl("'proxy.example.com:8080'", &out));
   EXPECT_EQ(out, "http://proxy.example.com:8080");
   EXPECT_TRUE(normalizeProxyUrl("  http://proxy.example.com:8080 \n", &out));
   EXPECT_EQ(out, "http://proxy.example.com:8080");
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlBlankIsUnset)
{
   std::string out = "stale";
   EXPECT_TRUE(normalizeProxyUrl("", &out));
   EXPECT_EQ(out, "");
   out = "stale";
   EXPECT_TRUE(normalizeProxyUrl("  ", &out));
   EXPECT_EQ(out, "");
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlRejectsMissingHost)
{
   std::string out;
   EXPECT_FALSE(normalizeProxyUrl("http://", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://user:pass@:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl(":8080", &out));
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlRejectsEmbeddedWhitespace)
{
   std::string out;
   EXPECT_FALSE(normalizeProxyUrl("http://proxy one:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("proxy.example.com 8080", &out));
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionEnablesWithoutProxyVariables)
{
   core::system::Options env;
   core::system::setenv(&env, "PATH", "/usr/bin");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "1");
   EXPECT_EQ(env.size(), 2u);
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionNormalizesSchemelessValues)
{
   core::system::Options env;
   core::system::setenv(&env, "http_proxy", "proxy.example.com:8080");
   core::system::setenv(&env, "HTTPS_PROXY", "http://secure.example.com:8443");
   core::system::setenv(&env, "no_proxy", "localhost,127.0.0.1");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "1");
   EXPECT_EQ(core::system::getenv(env, "http_proxy"), "http://proxy.example.com:8080");
   EXPECT_EQ(core::system::getenv(env, "HTTPS_PROXY"), "http://secure.example.com:8443");
   EXPECT_EQ(core::system::getenv(env, "no_proxy"), "localhost,127.0.0.1");
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionLeavesFlagUnsetForInvalidValue)
{
   core::system::Options env;
   core::system::setenv(&env, "https_proxy", "http://");
   core::system::setenv(&env, "http_proxy", "proxy.example.com:8080");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "");
   EXPECT_EQ(core::system::getenv(env, "https_proxy"), "http://");
   // Variables before the invalid one are still normalized; the flag alone
   // decides whether Node reads them.
   EXPECT_EQ(core::system::getenv(env, "http_proxy"), "http://proxy.example.com:8080");
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionDropsInheritedFlagForInvalidValue)
{
   core::system::Options env;
   core::system::setenv(&env, "NODE_USE_ENV_PROXY", "1");
   core::system::setenv(&env, "http_proxy", "http://proxy one:8080");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "");
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionHonorsLowercasePrecedence)
{
   // Node reads http_proxy before HTTP_PROXY, so an unused uppercase value
   // must not disable proxy support, and is left as written.
   core::system::Options env;
   core::system::setenv(&env, "HTTP_PROXY", "http://");
   core::system::setenv(&env, "http_proxy", "proxy.example.com:8080");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "1");
   EXPECT_EQ(core::system::getenv(env, "http_proxy"), "http://proxy.example.com:8080");
   EXPECT_EQ(core::system::getenv(env, "HTTP_PROXY"), "http://");

   // The lowercase value is what Node reads even when the uppercase one is fine.
   core::system::Options env2;
   core::system::setenv(&env2, "https_proxy", "http://");
   core::system::setenv(&env2, "HTTPS_PROXY", "http://secure.example.com:8443");

   applyEnvProxyOption(&env2);

   EXPECT_EQ(core::system::getenv(env2, "NODE_USE_ENV_PROXY"), "");
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionEmptyLowercaseHidesUppercase)
{
   // An empty lowercase variable is still the one Node reads, so the
   // uppercase value is never parsed and cannot break startup.
   core::system::Options env;
   core::system::setenv(&env, "http_proxy", "");
   core::system::setenv(&env, "HTTP_PROXY", "http://");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "1");
   EXPECT_EQ(core::system::getenv(env, "HTTP_PROXY"), "http://");
}

} // namespace tests
} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio
