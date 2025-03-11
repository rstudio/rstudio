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

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

test_context("NoProxyRulesTests")
{
   test_that("NoProxyRuleWildcard matches everything")
   {
      NoProxyRuleWildcard rule;
      REQUIRE(rule.match("anything", "anyport"));
      REQUIRE(rule.match("192.168.1.1", "80"));
      REQUIRE(rule.match("example.com", "443"));
      REQUIRE(rule.match("", ""));
   }

   test_that("NoProxyRuleDomain matches domains and subdomains")
   {
      NoProxyRuleDomain rule(".example.com");
      REQUIRE(rule.match("example.com", "443"));
      REQUIRE(rule.match("sub.example.com", "443"));
      REQUIRE(rule.match("example.com", "80"));
      REQUIRE(rule.match("sub.example.com", "80"));
      REQUIRE(rule.match("sub.sub.example.com", "443"));
      REQUIRE_FALSE(rule.match("example.org", "443"));
      REQUIRE_FALSE(rule.match("subexample.com", "443"));
   }

   test_that("NoProxyRuleDomain supports subdomains")
   {
      auto rule = createNoProxyRule(".sub.example.com");
      REQUIRE(dynamic_cast<NoProxyRuleDomain*>(rule.get()) != nullptr);
      REQUIRE(rule->match("sub.example.com", "443"));
      REQUIRE(rule->match("sub.sub.example.com", "443"));
      REQUIRE_FALSE(rule->match("example.com", "443"));
      REQUIRE_FALSE(rule->match("subexample.com", "443"));

      rule = createNoProxyRule(".sub.sub.example.com");
      REQUIRE(dynamic_cast<NoProxyRuleDomain*>(rule.get()) != nullptr);
      REQUIRE(rule->match("sub.sub.example.com", "443"));
      REQUIRE(rule->match("sub.sub.sub.example.com", "443"));
      REQUIRE_FALSE(rule->match("sub.example.com", "443"));
      REQUIRE_FALSE(rule->match("example.com", "443"));
      REQUIRE_FALSE(rule->match("subexample.com", "443"));
   }

   test_that("NoProxyRuleDomain supports top-level domains")
   {
      auto rule = createNoProxyRule(".com");
      REQUIRE(dynamic_cast<NoProxyRuleDomain*>(rule.get()) != nullptr);
      REQUIRE(rule->match("example.com", "443"));
      REQUIRE(rule->match("sub.example.com", "443"));
      REQUIRE(rule->match("subexample.com", "443"));
      REQUIRE_FALSE(rule->match("example.org", "443"));
   }

   test_that("NoProxyRuleAddress matches addresses and ports")
   {
      NoProxyRuleAddress ruleWithPort("example.com", "443");
      REQUIRE(ruleWithPort.match("example.com", "443"));
      REQUIRE_FALSE(ruleWithPort.match("example.com", "80"));
      REQUIRE_FALSE(ruleWithPort.match("example.org", "443"));
      REQUIRE_FALSE(ruleWithPort.match("sub.example.com", "443"));
      REQUIRE_FALSE(ruleWithPort.match("example2.com", "443"));

      NoProxyRuleAddress ruleWithoutPort("example.com");
      REQUIRE(ruleWithoutPort.match("example.com", "443"));
      REQUIRE(ruleWithoutPort.match("example.com", "80"));
      REQUIRE_FALSE(ruleWithoutPort.match("example.org", "443"));
      REQUIRE_FALSE(ruleWithoutPort.match("sub.example.com", "443"));
      REQUIRE_FALSE(ruleWithoutPort.match("example2.com", "443"));

      NoProxyRuleAddress ruleAsIp("192.168.4.3", "443");
      REQUIRE(ruleAsIp.match("192.168.4.3", "443"));
      REQUIRE_FALSE(ruleAsIp.match("example.com", "443"));
      REQUIRE_FALSE(ruleAsIp.match("172.168.3.2", "443"));
   }

   test_that("NoProxyRuleCidrBlock matches CIDR blocks")
   {
      const auto cidrBlock = "192.0.2.0/24";
      const auto cidrBlockResult =
          NoProxyRuleCidrBlock::parseCidrBlock(cidrBlock);
      REQUIRE(cidrBlockResult);
      NoProxyRuleCidrBlock rule(cidrBlockResult.value());
      for (uint32_t i = 0; i < 256; ++i)
      {
         const auto address = "192.0.2." + std::to_string(i);
         REQUIRE(rule.match(address, "443"));
      }
      REQUIRE_FALSE(rule.match("192.0.1.0", "443"));
      REQUIRE_FALSE(rule.match("10.3.4.1", "443"));

      const auto cidrBlock2 = "172.0.0.0/8";
      const auto cidrBlockResult2 =
          NoProxyRuleCidrBlock::parseCidrBlock(cidrBlock2);
      REQUIRE(cidrBlockResult2);
      NoProxyRuleCidrBlock rule2(cidrBlockResult2.value());
      REQUIRE(rule2.match("172.4.75.2", "443"));
      REQUIRE_FALSE(rule2.match("171.4.75.2", "443"));

      const auto invalidOctetsBlock = "1.1.1/24";
      const auto invalidOctetsBlockResult =
          NoProxyRuleCidrBlock::parseCidrBlock(invalidOctetsBlock);
      REQUIRE_FALSE(invalidOctetsBlockResult);
      REQUIRE(
          invalidOctetsBlockResult.error() ==
          systemError(boost::system::errc::invalid_argument, ERROR_LOCATION));

      const auto invalidMaskBlock = "172.0.0.0/gh";
      const auto invalidMaskBlockResult =
          NoProxyRuleCidrBlock::parseCidrBlock(invalidMaskBlock);
      REQUIRE_FALSE(invalidMaskBlockResult);
      REQUIRE(
          invalidMaskBlockResult.error() ==
          systemError(boost::system::errc::invalid_argument, ERROR_LOCATION));

      const auto invalidMaskBlockAbove32 = "127.0.0.0/400";
      const auto invalidMaskBlockAbove32Result =
          NoProxyRuleCidrBlock::parseCidrBlock(invalidMaskBlockAbove32);
      REQUIRE_FALSE(invalidMaskBlockAbove32Result);

      const auto invalidMaskBlockNegative = "127.0.0.0/-32";
      const auto invalidMaskBlockNegativeResult =
          NoProxyRuleCidrBlock::parseCidrBlock(invalidMaskBlockNegative);
      REQUIRE_FALSE(invalidMaskBlockNegativeResult);
   }

   test_that("createNoProxyBuilder creates the correct rule")
   {
      auto rule = createNoProxyRule("*");
      REQUIRE(dynamic_cast<NoProxyRuleWildcard*>(rule.get()) != nullptr);
      REQUIRE(rule->match("anything", "anyport"));

      rule = createNoProxyRule(".example.com");
      REQUIRE(dynamic_cast<NoProxyRuleDomain*>(rule.get()) != nullptr);
      REQUIRE(rule->match("sub.example.com", "443"));

      rule = createNoProxyRule("example.com");
      REQUIRE(dynamic_cast<NoProxyRuleAddress*>(rule.get()) != nullptr);
      REQUIRE(rule->match("example.com", "80"));

      rule = createNoProxyRule("example.com:443");
      REQUIRE(dynamic_cast<NoProxyRuleAddress*>(rule.get()) != nullptr);
      REQUIRE(rule->match("example.com", "443"));

      rule = createNoProxyRule("192.0.2.0/24");
      REQUIRE(dynamic_cast<NoProxyRuleCidrBlock*>(rule.get()) != nullptr);
      REQUIRE(rule->match("192.0.2.23", "443"));

      // junk will always return an address rule
      rule = createNoProxyRule("junk");
      REQUIRE(dynamic_cast<NoProxyRuleAddress*>(rule.get()) != nullptr);
   }
}

} // end namespace tests
} // end namespace http
} // end namespace core
} // end namespace rstudio
