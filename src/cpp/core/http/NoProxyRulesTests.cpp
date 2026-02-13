/*
 * NoProxyRulesTests.cpp
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

#include <core/http/NoProxyRules.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

TEST(HttpTestNoProxyRule, WildcardMatchesEverything)
{
   NoProxyRuleWildcard rule;
    EXPECT_TRUE(rule.match("anything", "anyport"));
    EXPECT_TRUE(rule.match("192.168.1.1", "80"));
    EXPECT_TRUE(rule.match("example.com", "443"));
    EXPECT_TRUE(rule.match("", ""));
}

TEST(HttpTestNoProxyRule, DomainMatchesDomainsAndSubdomains)
{
   NoProxyRuleDomain rule(".example.com");
    EXPECT_TRUE(rule.match("example.com", "443"));
    EXPECT_TRUE(rule.match("sub.example.com", "443"));
    EXPECT_TRUE(rule.match("example.com", "80"));
    EXPECT_TRUE(rule.match("sub.example.com", "80"));
    EXPECT_TRUE(rule.match("sub.sub.example.com", "443"));
    EXPECT_FALSE(rule.match("example.org", "443"));
    EXPECT_FALSE(rule.match("subexample.com", "443"));
}

TEST(HttpTestNoProxyRule, DomainSupportsSubdomains)
{
   auto rule = createNoProxyRule(".sub.example.com");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleDomain*>(rule.get()));
    EXPECT_TRUE(rule->match("sub.example.com", "443"));
    EXPECT_TRUE(rule->match("sub.sub.example.com", "443"));
    EXPECT_FALSE(rule->match("example.com", "443"));
    EXPECT_FALSE(rule->match("subexample.com", "443"));

   rule = createNoProxyRule(".sub.sub.example.com");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleDomain*>(rule.get()));
    EXPECT_TRUE(rule->match("sub.sub.example.com", "443"));
    EXPECT_TRUE(rule->match("sub.sub.sub.example.com", "443"));
    EXPECT_FALSE(rule->match("sub.example.com", "443"));
    EXPECT_FALSE(rule->match("example.com", "443"));
    EXPECT_FALSE(rule->match("subexample.com", "443"));
}

TEST(HttpTestNoProxyRule, DomainSupportsTopLevelDomains)
{
   auto rule = createNoProxyRule(".com");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleDomain*>(rule.get()));
    EXPECT_TRUE(rule->match("example.com", "443"));
    EXPECT_TRUE(rule->match("sub.example.com", "443"));
    EXPECT_TRUE(rule->match("subexample.com", "443"));
    EXPECT_FALSE(rule->match("example.org", "443"));
}

TEST(HttpTestNoProxyRule, MatchesAddressesAndPorts)
{
   NoProxyRuleAddress ruleWithPort("example.com", "443");
    EXPECT_TRUE(ruleWithPort.match("example.com", "443"));
    EXPECT_FALSE(ruleWithPort.match("example.com", "80"));
    EXPECT_FALSE(ruleWithPort.match("example.org", "443"));
    EXPECT_FALSE(ruleWithPort.match("sub.example.com", "443"));
    EXPECT_FALSE(ruleWithPort.match("example2.com", "443"));

   NoProxyRuleAddress ruleWithoutPort("example.com");
    EXPECT_TRUE(ruleWithoutPort.match("example.com", "443"));
    EXPECT_TRUE(ruleWithoutPort.match("example.com", "80"));
    EXPECT_FALSE(ruleWithoutPort.match("example.org", "443"));
    EXPECT_FALSE(ruleWithoutPort.match("sub.example.com", "443"));
    EXPECT_FALSE(ruleWithoutPort.match("example2.com", "443"));

   NoProxyRuleAddress ruleAsIp("192.168.4.3", "443");
    EXPECT_TRUE(ruleAsIp.match("192.168.4.3", "443"));
    EXPECT_FALSE(ruleAsIp.match("example.com", "443"));
    EXPECT_FALSE(ruleAsIp.match("172.168.3.2", "443"));
}

TEST(HttpTestNoProxyRule, MatchesCidrBlocks)
{
   const auto cidrBlock = "192.0.2.0/24";
   const auto cidrBlockResult =
       NoProxyRuleCidrBlock::parseCidrBlock(cidrBlock);
   ASSERT_TRUE(cidrBlockResult);
   NoProxyRuleCidrBlock rule(cidrBlockResult.value());
   for (uint32_t i = 0; i < 256; ++i)
   {
      const auto address = "192.0.2." + std::to_string(i);
    EXPECT_TRUE(rule.match(address, "443"));
   }
   EXPECT_FALSE(rule.match("192.0.1.0", "443"));
   EXPECT_FALSE(rule.match("10.3.4.1", "443"));

   const auto cidrBlock2 = "172.0.0.0/8";
   const auto cidrBlockResult2 =
       NoProxyRuleCidrBlock::parseCidrBlock(cidrBlock2);
   ASSERT_TRUE(cidrBlockResult2);
   NoProxyRuleCidrBlock rule2(cidrBlockResult2.value());
   EXPECT_TRUE(rule2.match("172.4.75.2", "443"));
   EXPECT_FALSE(rule2.match("171.4.75.2", "443"));

   const auto invalidOctetsBlock = "1.1.1/24";
   const auto invalidOctetsBlockResult =
       NoProxyRuleCidrBlock::parseCidrBlock(invalidOctetsBlock);
   ASSERT_FALSE(invalidOctetsBlockResult);
   EXPECT_EQ(systemError(boost::system::errc::invalid_argument, ERROR_LOCATION),
         invalidOctetsBlockResult.error());

   const auto invalidMaskBlock = "172.0.0.0/gh";
   const auto invalidMaskBlockResult =
       NoProxyRuleCidrBlock::parseCidrBlock(invalidMaskBlock);
   ASSERT_FALSE(invalidMaskBlockResult);
   EXPECT_EQ(systemError(boost::system::errc::invalid_argument, ERROR_LOCATION),
         invalidMaskBlockResult.error());

   const auto invalidMaskBlockAbove32 = "127.0.0.0/400";
   const auto invalidMaskBlockAbove32Result =
       NoProxyRuleCidrBlock::parseCidrBlock(invalidMaskBlockAbove32);
   ASSERT_FALSE(invalidMaskBlockAbove32Result);

   const auto invalidMaskBlockNegative = "127.0.0.0/-32";
   const auto invalidMaskBlockNegativeResult =
       NoProxyRuleCidrBlock::parseCidrBlock(invalidMaskBlockNegative);
   ASSERT_FALSE(invalidMaskBlockNegativeResult);
}

TEST(HttpTestNoProxyRule, CreateNoProxyBuilderCreatesCorrectRule)
{
   auto rule = createNoProxyRule("*");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleWildcard*>(rule.get()));
    EXPECT_TRUE(rule->match("anything", "anyport"));

   rule = createNoProxyRule(".example.com");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleDomain*>(rule.get()));
    EXPECT_TRUE(rule->match("sub.example.com", "443"));

   rule = createNoProxyRule("example.com");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleAddress*>(rule.get()));
    EXPECT_TRUE(rule->match("example.com", "80"));

   rule = createNoProxyRule("example.com:443");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleAddress*>(rule.get()));
    EXPECT_TRUE(rule->match("example.com", "443"));

   rule = createNoProxyRule("192.0.2.0/24");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleCidrBlock*>(rule.get()));
    EXPECT_TRUE(rule->match("192.0.2.23", "443"));

   // junk will always return an address rule
   rule = createNoProxyRule("junk");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleAddress*>(rule.get()));

   // IPv6 bracketed address without port
   rule = createNoProxyRule("[::1]");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleAddress*>(rule.get()));
    EXPECT_TRUE(rule->match("::1", "80"));
    EXPECT_TRUE(rule->match("::1", "443"));

   // IPv6 bracketed address with port
   rule = createNoProxyRule("[::1]:8080");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleAddress*>(rule.get()));
    EXPECT_TRUE(rule->match("::1", "8080"));
    EXPECT_FALSE(rule->match("::1", "443"));

   // IPv6 full address with port
   rule = createNoProxyRule("[2001:db8::1]:443");
    EXPECT_NE(nullptr, dynamic_cast<NoProxyRuleAddress*>(rule.get()));
    EXPECT_TRUE(rule->match("2001:db8::1", "443"));
    EXPECT_FALSE(rule->match("2001:db8::1", "80"));
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio