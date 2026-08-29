/*
 * UrlPorts.hpp
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

#ifndef URL_PORTS_URL_PORTS_HPP
#define URL_PORTS_URL_PORTS_HPP

#include <iostream> 
#include <cstdlib>

#include <shared_core/Error.hpp>

using namespace rstudio;
using namespace rstudio::core;

/**
 * Join RS_SERVER_URL and RS_SESSION_URL with an obfuscated port to form the
 * proxied URL printed by the -l option.
 *
 * @param serverUrl Value of RS_SERVER_URL, which may be empty or unset
 * @param sessionUrl Value of RS_SESSION_URL, empty when the URL has no session segment
 * @param transformedPort The obfuscated port
 */
std::string buildProxiedUrl(const std::string& serverUrl,
                            const std::string& sessionUrl,
                            const std::string& transformedPort)
{
   std::string prefix = serverUrl;
   if (!prefix.empty() && prefix.back() == '/' &&
       !sessionUrl.empty() && sessionUrl.front() == '/')
   {
      prefix.pop_back();
   }
   prefix += sessionUrl;

   // Keep the port path rooted. Neither variable is guaranteed to be set, and
   // a relative result would resolve against the caller's own path.
   if (prefix.empty() || prefix.back() != '/')
      prefix += '/';

   return prefix + "p/" + transformedPort + "/";
}

bool parseArguments(const int argc, char * const argv[], bool& longOutput, int* pPort, std::string* pPortToken)
{
   if (argc < 2)
   {
      return false;
   }

   longOutput = false;
   int portIndex = 1;
   int tokenIndex = 2;

   *pPortToken = std::getenv("RS_PORT_TOKEN");

   if (std::string(argv[1]).at(0) == '-')
   {
      if (std::string(argv[1]).at(1) != 'l')
      {
         return false;
      }

      longOutput = true;
      portIndex = 2;
      tokenIndex = 3;
   }
   int minCount = portIndex + 1;
   int maxCount = tokenIndex + 1;
   if (argc < minCount || argc > maxCount || ((argc == minCount) && pPortToken->empty()))
   {
      return false;
   }

   if (argc > minCount)
      *pPortToken = argv[tokenIndex];
   
   try
   {
      *pPort = std::stoi(argv[portIndex]);
      return true;
   }
   CATCH_UNEXPECTED_EXCEPTION
   return false;
}

#endif
