/*
 * ProxyUtils.hpp
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
#ifndef PROXY_UTILS
#define PROXY_UTILS

#include <boost/optional.hpp>
#include <core/http/URL.hpp>

#include <core/http/NoProxyRules.hpp>

namespace rstudio {
namespace core {
namespace http {

using NoProxyRules = std::vector<std::unique_ptr<NoProxyRule>>; 

class ProxyUtils
{
public:
   ProxyUtils();

   boost::optional<URL> httpProxyUrl(
         const std::string& address = std::string(),
         const std::string& port = std::string()) const;

   boost::optional<URL> httpsProxyUrl(
         const std::string& address = std::string(),
         const std::string& port = std::string()) const;

   void addNoProxyRule(std::unique_ptr<NoProxyRule> rule)
   {
      noProxyRules_.emplace_back(std::move(rule));
   }

   const NoProxyRules& noProxyRules() const { return noProxyRules_; }

private:
   bool shouldProxy(const std::string& address, const std::string& port) const;
   std::string httpProxyVar_;
   std::string httpsProxyVar_;
   NoProxyRules noProxyRules_;
};

ProxyUtils& proxyUtils();

} // namespace http
} // namespace core
} // namespace rstudio

#endif // PROXY_UTILS
