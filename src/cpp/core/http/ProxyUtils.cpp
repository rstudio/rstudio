/*
 * ProxyUtils.cpp
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

#include <core/http/ProxyUtils.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/regex.hpp>

#include <core/http/NoProxyRules.hpp>
#include <core/system/Environment.hpp>
#include <core/Log.hpp>
#include <core/Result.hpp>


namespace rstudio {
namespace core {
namespace http {

ProxyUtils::ProxyUtils()
{
   auto http_proxy = system::getenv("http_proxy");
   if (http_proxy.empty())
   {
      http_proxy = system::getenv("HTTP_PROXY");
   }
   httpProxyVar_ = http_proxy;

   auto https_proxy = system::getenv("https_proxy");
   if (https_proxy.empty())
   {
      https_proxy = system::getenv("HTTPS_PROXY");
   }
   httpsProxyVar_ = https_proxy;

   noProxyRules_.clear();
   std::vector<std::string> noProxyList;
   auto no_proxy = system::getenv("no_proxy");
   if (no_proxy.empty())
   {
      no_proxy = system::getenv("NO_PROXY");
   }
   if (!no_proxy.empty())
   {
      boost::split(noProxyList, no_proxy, boost::is_any_of(","));
      for (const auto& rule : noProxyList)
      {
         noProxyRules_.emplace_back(createNoProxyRule(rule));
      }
   }
}

boost::optional<URL> ProxyUtils::httpProxyUrl(const std::string& address,
                                              const std::string& port) const
{
   if (httpProxyVar_.empty() || !shouldProxy(address, port))
   {
      return boost::none;
   }

   return URL(httpProxyVar_);
}

boost::optional<URL> ProxyUtils::httpsProxyUrl(const std::string& address,
                                               const std::string& port) const
{
   if (httpsProxyVar_.empty() || !shouldProxy(address, port))
   {
      return boost::none;
   }

   return URL(httpsProxyVar_);
}

bool ProxyUtils::shouldProxy(const std::string& address,
                             const std::string& port) const
{
   if (address.empty() && port.empty())
      return true;

   for (const auto& rule : noProxyRules_)
   {
      if (rule->match(address, port))
      {
         LOG_DEBUG_MESSAGE("Bypassing proxy for address: " + address);
         return false;
      }
   }
   return true;
}

ProxyUtils& proxyUtils()
{
   static ProxyUtils proxyUtils;
   return proxyUtils;
}

} // namespace http
} // namespace core
} // namespace rstudio
