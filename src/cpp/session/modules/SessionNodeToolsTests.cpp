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

TEST(SessionNodeToolsTest, NormalizeProxyUrlRejectsWhitespaceOutsideCredentials)
{
   std::string out;
   EXPECT_FALSE(normalizeProxyUrl("http://proxy one:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("proxy.example.com 8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://user:pass@proxy one:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:8080/some path", &out));
   EXPECT_FALSE(normalizeProxyUrl("ht tp://proxy.example.com:8080", &out));
   // '?' and '#' end the authority, so an '@' after them is not a credential
   // separator and the whitespace before them is in the host, not the userinfo.
   EXPECT_FALSE(normalizeProxyUrl("http://user:my password#tag@proxy.example.com:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://user:my password?q@proxy.example.com:8080", &out));
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlRejectsPathQueryAndFragment)
{
   // Node takes a bare trailing slash and nothing more after the authority.
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("proxy.example.com:8080/", &out));
   EXPECT_EQ(out, "http://proxy.example.com:8080/");
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:8080/path", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:8080//", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:8080?x", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:8080#f", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:8080/?x", &out));
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlChecksPort)
{
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("http://proxy.example.com:65535", &out));
   EXPECT_TRUE(normalizeProxyUrl("http://proxy.example.com:00080", &out));
   EXPECT_TRUE(normalizeProxyUrl("http://proxy.example.com:", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:abc", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:65536", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:99999", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:99999999999999999999", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:80:80", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.example.com:-1", &out));
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlRejectsUnsupportedSchemes)
{
   // curl and R take these, but Node exits on any scheme other than http,
   // https and socks5.
   std::string out;
   EXPECT_FALSE(normalizeProxyUrl("socks5h://proxy.example.com:1080", &out));
   EXPECT_FALSE(normalizeProxyUrl("socks4://proxy.example.com:1080", &out));
   EXPECT_FALSE(normalizeProxyUrl("ftp://proxy.example.com:1080", &out));
   EXPECT_FALSE(normalizeProxyUrl("://proxy.example.com:1080", &out));
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlChecksHost)
{
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("http://proxy_one.example-corp.com.:8080", &out));
   EXPECT_TRUE(normalizeProxyUrl("http://10.0.0.1", &out));
   EXPECT_TRUE(normalizeProxyUrl("http://[::1]:8080", &out));
   EXPECT_TRUE(normalizeProxyUrl("http://[2001:db8::1]", &out));
   EXPECT_TRUE(normalizeProxyUrl("[::1]:8080", &out));
   EXPECT_EQ(out, "http://[::1]:8080");

   EXPECT_FALSE(normalizeProxyUrl("http://pro%xy:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://pro<xy:8080", &out));

   // a host ending in a number has to be an IPv4 address
   EXPECT_FALSE(normalizeProxyUrl("http://1.2.3.4.5:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://1.2.3.999:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://08.1.1.1:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.1:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://proxy.0x1:8080", &out));

   EXPECT_FALSE(normalizeProxyUrl("http://[::1", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://[]:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://[:::]:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://[::1]8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://[fe80::1%25en0]:8080", &out));
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlChecksCredentials)
{
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("http://user:p@ss@proxy.example.com:8080", &out));
   EXPECT_TRUE(normalizeProxyUrl("http://user:p%2Fss@proxy.example.com:8080", &out));
   EXPECT_TRUE(normalizeProxyUrl("http://user:%C3%A9%E2%82%AC@proxy.example.com:8080", &out));
   EXPECT_TRUE(normalizeProxyUrl("http://@proxy.example.com:8080", &out));

   // a raw slash or backslash ends the authority early
   EXPECT_FALSE(normalizeProxyUrl("http://user:p/ss@proxy.example.com:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://user:p\\ss@proxy.example.com:8080", &out));

   // Node decodes the credentials and throws on a malformed escape
   EXPECT_FALSE(normalizeProxyUrl("http://user:p%zz@proxy.example.com:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://user:p%@proxy.example.com:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://user:p%4@proxy.example.com:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://user:%ff@proxy.example.com:8080", &out));
   EXPECT_FALSE(normalizeProxyUrl("http://user:%C3x%A9@proxy.example.com:8080", &out));
}

TEST(SessionNodeToolsTest, NormalizeProxyUrlKeepsWhitespaceInCredentials)
{
   // Node percent-encodes whitespace in the userinfo, so these already work.
   std::string out;
   EXPECT_TRUE(normalizeProxyUrl("http://user:my password@proxy.example.com:8080", &out));
   EXPECT_EQ(out, "http://user:my password@proxy.example.com:8080");
   EXPECT_TRUE(normalizeProxyUrl("user:my password@proxy.example.com:8080", &out));
   EXPECT_EQ(out, "http://user:my password@proxy.example.com:8080");
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

   core::system::Options env2;
   core::system::setenv(&env2, "NODE_USE_ENV_PROXY", "1");
   core::system::setenv(&env2, "https_proxy", "http://user:my password#tag@proxy.example.com:8080");

   applyEnvProxyOption(&env2);

   EXPECT_EQ(core::system::getenv(env2, "NODE_USE_ENV_PROXY"), "");
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionNegatesInheritedNodeOptionForInvalidValue)
{
   // --use-env-proxy in NODE_OPTIONS enables the parsing without the flag.
   core::system::Options env;
   core::system::setenv(&env, "NODE_OPTIONS", "--max-old-space-size=4096 --use-env-proxy");
   core::system::setenv(&env, "http_proxy", "http://proxy.example.com:abc");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "");
   EXPECT_EQ(core::system::getenv(env, "NODE_OPTIONS"),
             "--max-old-space-size=4096 --use-env-proxy --no-use-env-proxy");

   // Other options are left alone, as is everything when the value is valid.
   core::system::Options env2;
   core::system::setenv(&env2, "NODE_OPTIONS", "--max-old-space-size=4096");
   core::system::setenv(&env2, "http_proxy", "http://proxy.example.com:abc");

   applyEnvProxyOption(&env2);

   EXPECT_EQ(core::system::getenv(env2, "NODE_OPTIONS"), "--max-old-space-size=4096");

   core::system::Options env3;
   core::system::setenv(&env3, "NODE_OPTIONS", "--use-env-proxy");
   core::system::setenv(&env3, "http_proxy", "proxy.example.com:8080");

   applyEnvProxyOption(&env3);

   EXPECT_EQ(core::system::getenv(env3, "NODE_USE_ENV_PROXY"), "1");
   EXPECT_EQ(core::system::getenv(env3, "NODE_OPTIONS"), "--use-env-proxy");
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionLeavesFlagUnsetForUrlsNodeRejects)
{
   // Each of these makes Node 24 exit during startup with the flag on.
   const char* const values[] = {
      "http://proxy.example.com:8080/path",
      "http://proxy.example.com:8080?x",
      "http://proxy.example.com:abc",
      "http://proxy.example.com:99999",
      "http://user:p/ss@proxy:8080",
      "socks5h://proxy.example.com:1080",
      "ftp://proxy:1080"
   };

   for (const char* value : values)
   {
      core::system::Options env;
      core::system::setenv(&env, "NODE_USE_ENV_PROXY", "1");
      core::system::setenv(&env, "https_proxy", value);

      applyEnvProxyOption(&env);

      EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "") << value;
      EXPECT_EQ(core::system::getenv(env, "https_proxy"), value);
   }
}

// Windows environment names are case-insensitive (core::system::setenv and
// Node both treat http_proxy and HTTP_PROXY as one variable), so the
// lowercase-first precedence only exists on POSIX.
#ifndef _WIN32

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

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionEmptyLowercaseFallsBackToUppercase)
{
   // Node's http module reads the uppercase variable when the lowercase one
   // is empty, so an invalid uppercase value still has to turn the flag off.
   core::system::Options env;
   core::system::setenv(&env, "http_proxy", "");
   core::system::setenv(&env, "HTTP_PROXY", "http://");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "");

   core::system::Options env2;
   core::system::setenv(&env2, "https_proxy", "");
   core::system::setenv(&env2, "HTTPS_PROXY", "http://proxy.example.com:abc");

   applyEnvProxyOption(&env2);

   EXPECT_EQ(core::system::getenv(env2, "NODE_USE_ENV_PROXY"), "");

   // ... and a repairable one is repaired.
   core::system::Options env3;
   core::system::setenv(&env3, "http_proxy", "");
   core::system::setenv(&env3, "HTTP_PROXY", "proxy.example.com:8080");

   applyEnvProxyOption(&env3);

   EXPECT_EQ(core::system::getenv(env3, "NODE_USE_ENV_PROXY"), "1");
   EXPECT_EQ(core::system::getenv(env3, "http_proxy"), "");
   EXPECT_EQ(core::system::getenv(env3, "HTTP_PROXY"), "http://proxy.example.com:8080");
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionBlankLowercaseFallsBackToUppercase)
{
   // A blank lowercase value is emptied (fetch() would fail to parse it),
   // which sends the http module on to the uppercase one.
   core::system::Options env;
   core::system::setenv(&env, "http_proxy", "  ");
   core::system::setenv(&env, "HTTP_PROXY", "http://");

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "");
   EXPECT_EQ(core::system::getenv(env, "http_proxy"), "");
}

#endif // !_WIN32

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionMixedCaseName)
{
   core::system::Options env;
   env.push_back(std::make_pair("Http_Proxy", "proxy.example.com:8080"));

   applyEnvProxyOption(&env);

   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "1");
#ifdef _WIN32
   // Node reads it as http_proxy on Windows, so it must be normalized.
   EXPECT_EQ(core::system::getenv(env, "http_proxy"), "http://proxy.example.com:8080");
#else
   // Node never reads this spelling on POSIX; leave it alone.
   EXPECT_EQ(env[0].second, "proxy.example.com:8080");
#endif
}

TEST(SessionNodeToolsTest, ApplyEnvProxyOptionMixedCaseInvalidValue)
{
   core::system::Options env;
   env.push_back(std::make_pair("NODE_USE_ENV_PROXY", "1"));
   env.push_back(std::make_pair("Https_Proxy", "http://"));

   applyEnvProxyOption(&env);

#ifdef _WIN32
   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "");
#else
   EXPECT_EQ(core::system::getenv(env, "NODE_USE_ENV_PROXY"), "1");
#endif
}

} // namespace tests
} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio
