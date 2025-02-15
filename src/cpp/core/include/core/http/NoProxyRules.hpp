/*
 * NoProxyRules.hpp
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

#ifndef NO_PROXY_RULES
#define NO_PROXY_RULES

#include <core/Result.hpp>

namespace rstudio {
namespace core {
namespace http {

/**
 * @class NoProxyRule
 * @brief Abstract base class for defining rules to determine if a proxy should
 * be bypassed.
 *
 * This class provides an interface for creating rules that can be used to check
 * whether a given address and port should bypass the proxy.
 */
class NoProxyRule
{
 public:
   /**
    * @brief Checks if the given address and port match the no proxy rules.
    *
    * This function determines whether the specified address and port
    * conform to the defined proxy rules.
    *
    * @param address The address to be checked against the proxy rules.
    * @param port The port to be checked against the proxy rules.
    * @return true if the address and port match the proxy rules, meaning the
    * proxy should be bypassed. False otherwise.
    */
   virtual bool match(const std::string& address,
                      const std::string& port) const = 0;
   /**
    * @brief Converts the rule to a string.
    * 
    * This function converts the rule to a string.
    * 
    * @return The rule as a string.
    */
   virtual std::string toString() const = 0;
   virtual ~NoProxyRule() = default;
};

/**
 * @class NoProxyRuleDomain
 * @brief A rule that matches a domain name.
 *
 * This class provides a rule that matches an address to a domain name.
 * The domain should be specified without any subdomains. When matching,
 * the rule will check if the address is within the specified domain. e.g.
 * home.example.com will match the domain example.com.
 *
 */
class NoProxyRuleDomain : public NoProxyRule
{
 public:
   explicit NoProxyRuleDomain(const std::string& domain);
   bool match(const std::string& address,
              const std::string& port) const override;
   std::string toString() const override;

 private:
   std::string domain_;
};

/**
 * @class NoProxyRuleWildcard
 * @brief A rule that matches a wildcard.
 *
 * This class provides a rule that matches a single asterisk (*) wildcard,
 * which matches any address. This rule is used to bypass the proxy for all
 * addresses.
 *
 */
class NoProxyRuleWildcard : public NoProxyRule
{
 public:
   bool match(const std::string& address,
              const std::string& port) const override;
   std::string toString() const override;
};

/**
 * @class NoProxyRuleAddress
 * @brief A rule that matches an address and optional port.
 *
 * This class provides a rule that matches exactly to an address and optional
 * port.
 */
class NoProxyRuleAddress : public NoProxyRule
{
 public:
   NoProxyRuleAddress(const std::string& address,
                      const std::string& port = std::string());

   bool match(const std::string& address,
              const std::string& port) const override;
   std::string toString() const override;

 private:
   std::string address_;
   std::string port_;
};

struct CidrBlock
{
   uint32_t range;
   uint32_t mask;
};

/**
 * @class NoProxyRuleCidrBlock
 * @brief A rule that matches an address to a CIDR block.
 *
 * This class provides a rule that matches an address to a CIDR block, e.g.
 * 192.168.0.0/16 will match any address in the entire 16 bit network starting
 * with 192.168.
 */
class NoProxyRuleCidrBlock : public NoProxyRule
{
 public:
   NoProxyRuleCidrBlock(uint32_t range, uint32_t mask);
   NoProxyRuleCidrBlock(const CidrBlock& cidrBlock);

   static Result<CidrBlock> parseCidrBlock(const std::string& cidrBlock);

   bool match(const std::string& address,
              const std::string& port) const override;
   std::string toString() const override;

 private:
   static Result<uint32_t> parseIpAddress(const std::string& address);

   CidrBlock cidrBlock_;
};

std::unique_ptr<NoProxyRule> createNoProxyRule(const std::string& rule);

inline std::unique_ptr<NoProxyRule> createNoProxyRule(
    const std::string& address,
    const std::string& port)
{
   return createNoProxyRule(address + ":" + port);
}

} // namespace http
} // namespace core
} // namespace rstudio

#endif // NoProxyRules
