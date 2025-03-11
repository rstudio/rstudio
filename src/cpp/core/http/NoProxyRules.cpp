/*
 * NoProxyRules.cpp
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

#include <boost/algorithm/string.hpp>
#include <boost/regex.hpp>
#include <boost/lexical_cast.hpp>

#include <core/Log.hpp>


namespace rstudio {
namespace core {
namespace http {

NoProxyRuleDomain::NoProxyRuleDomain(const std::string& domain)
    : domain_(domain)
{
}

bool NoProxyRuleDomain::match(const std::string& address,
                              const std::string& port) const
{
   // Check if the address is within the domain
   return boost::algorithm::ends_with("." + address, domain_);
}

std::string NoProxyRuleDomain::toString() const { return domain_; }

bool NoProxyRuleWildcard::match(const std::string& address,
                                const std::string& port) const
{
   // Always match
   return true;
}

std::string NoProxyRuleWildcard::toString() const { return "*"; }

NoProxyRuleAddress::NoProxyRuleAddress(const std::string& address,
                                       const std::string& port)
    : address_(address), port_(port)
{
}

bool NoProxyRuleAddress::match(const std::string& address,
                               const std::string& port) const
{
   // Check if the address and port match
   return address == address_ && (port_.empty() || port == port_);
}

std::string NoProxyRuleAddress::toString() const
{
   return address_ + (port_.empty() ? "" : ":" + port_);
}

NoProxyRuleCidrBlock::NoProxyRuleCidrBlock(uint32_t range, uint32_t mask)
    : cidrBlock_({range, mask})
{
}

NoProxyRuleCidrBlock::NoProxyRuleCidrBlock(const CidrBlock& cidrBlock)
    : cidrBlock_(cidrBlock)
{
}

Result<CidrBlock>
NoProxyRuleCidrBlock::parseCidrBlock(const std::string& cidrBlock)
{
   std::vector<std::string> parts;
   boost::split(parts, cidrBlock, boost::is_any_of("/"));
   if (parts.size() != 2)
   {
      return Unexpected(systemError(boost::system::errc::invalid_argument,
                                    "Invalid CIDR block: " + cidrBlock,
                                    ERROR_LOCATION));
   }

   const auto rangeResult = parseIpAddress(parts[0]);
   if (!rangeResult)
   {
      auto error = rangeResult.error();
      error.addProperty("description", "Unable to parse CIDR range.");
      return Unexpected(error);
   }

   try
   {
      const auto range = rangeResult.value();
      const auto mask = boost::lexical_cast<uint32_t>(parts[1]);
      if (mask > 32)
         return Unexpected(systemError(boost::system::errc::invalid_argument,
                                       "Invalid CIDR block: " + cidrBlock +
                                           ". Mask must be between 0 and 32.",
                                       ERROR_LOCATION));
      const auto maskBits = 0xFFFFFFFF << (32 - mask);
      return CidrBlock{range, maskBits};
   }
   catch (const boost::bad_lexical_cast& e)
   {
      auto error = systemError(boost::system::errc::invalid_argument,
                               "Invalid CIDR block: " + cidrBlock +
                                   ". Unable to parse CIDR mask.",
                               ERROR_LOCATION);
      error.addProperty("what", e.what());
      return Unexpected(error);
   }
}

bool NoProxyRuleCidrBlock::match(const std::string& address,
                                 const std::string& port) const
{
   const auto addrResult = parseIpAddress(address);
   if (!addrResult)
   {
      // don't emit an error here, as the address may not be an IP address
      return false;
   }
   const auto addr = addrResult.value();

   // Check if the address is within the CIDR block
   return (addr & cidrBlock_.mask) == cidrBlock_.range;
}

std::string NoProxyRuleCidrBlock::toString() const
{
   std::string address;
   address += std::to_string((cidrBlock_.range >> 24) & 0xFF);
   address += ".";
   address += std::to_string((cidrBlock_.range >> 16) & 0xFF);
   address += ".";
   address += std::to_string((cidrBlock_.range >> 8) & 0xFF);
   address += ".";
   address += std::to_string(cidrBlock_.range & 0xFF);

   int maskInt = 0;
   uint32_t maskBits = cidrBlock_.mask;
   while (maskBits & 0xF0000000)
   {
      maskBits <<= 1;
      maskInt++;
   }
   const auto mask = std::to_string(maskInt);

   return address + "/" + mask;
}

Result<uint32_t>
NoProxyRuleCidrBlock::parseIpAddress(const std::string& address)
{
   std::vector<std::string> parts;
   boost::split(parts, address, boost::is_any_of("."));
   if (parts.size() != 4)
   {
      return Unexpected(systemError(boost::system::errc::invalid_argument,
                                    "Invalid IP address: " + address,
                                    ERROR_LOCATION));
   }

   try
   {
      return (boost::lexical_cast<uint32_t>(parts[0]) << 24) |
             (boost::lexical_cast<uint32_t>(parts[1]) << 16) |
             (boost::lexical_cast<uint32_t>(parts[2]) << 8) |
             boost::lexical_cast<uint32_t>(parts[3]);
   }
   catch (const boost::bad_lexical_cast& e)
   {
      auto error = systemError(boost::system::errc::invalid_argument,
                               "Invalid IP address: " + address +
                                   ". Unable to parse octets.",
                               ERROR_LOCATION);
      error.addProperty("what", e.what());
      return Unexpected(error);
   }
}

std::unique_ptr<NoProxyRule> createNoProxyRule(const std::string& rule)
{
   if (rule == "*")
   {
      return std::unique_ptr<NoProxyRule>(new NoProxyRuleWildcard());
   }
   // Matches a top-level domain, domain, or subdomain that starts with a "."
   boost::regex domainRegex(R"(^\.[-a-zA-Z0-9.]+$)");
   if (boost::regex_match(rule, domainRegex))
   {
      return std::unique_ptr<NoProxyRuleDomain>(new NoProxyRuleDomain(rule));
   }

   const auto cidrBlockResult = NoProxyRuleCidrBlock::parseCidrBlock(rule);
   if (cidrBlockResult)
   {
      return std::unique_ptr<NoProxyRuleCidrBlock>(
          new NoProxyRuleCidrBlock(cidrBlockResult.value()));
   }

   std::vector<std::string> parts;
   boost::split(parts, rule, boost::is_any_of(":"));
   if (parts.size() == 2)
   {
      return std::unique_ptr<NoProxyRuleAddress>(
          new NoProxyRuleAddress(parts[0], parts[1]));
   }

   return std::unique_ptr<NoProxyRuleAddress>(new NoProxyRuleAddress(rule));
}

} // namespace http
} // namespace core
} // namespace rstudio
