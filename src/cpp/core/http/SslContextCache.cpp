/*
 * SslContextCache.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include <core/http/SslContextCache.hpp>
#include <core/http/Ssl.hpp>
#include <core/Log.hpp>

#include <boost/make_shared.hpp>

namespace {
   struct ContextKey
   {
      bool verify;
      std::string certificateAuthority;

      bool operator<(const ContextKey& other) const
      {
         if (verify != other.verify)
            return verify < other.verify;
         return certificateAuthority < other.certificateAuthority;
      }
   };

   std::mutex s_cacheMutex;
   std::map<ContextKey, boost::shared_ptr<boost::asio::ssl::context>> s_contextCache;
}

namespace rstudio {
namespace core {
namespace http {
namespace ssl {

boost::shared_ptr<boost::asio::ssl::context> SslContextCache::getContext(
   bool verify,
   const std::string& certificateAuthority)
{
   ContextKey key{verify, certificateAuthority};

   // Check cache first - common path, could use a read-lock here
   {
      std::lock_guard<std::mutex> lock(s_cacheMutex);
      auto it = s_contextCache.find(key);
      if (it != s_contextCache.end())
      {
         return it->second;
      }
   }

   // Create new context (outside lock to minimize contention)
   bool cacheable = true;
   auto pContext = createContext(verify, certificateAuthority, &cacheable);

   int size = 0;

   // Add to cache
   if (cacheable)
   {
      std::lock_guard<std::mutex> lock(s_cacheMutex);
      size = s_contextCache.size();
      // Double-check in case another thread created it while we were creating ours
      auto it = s_contextCache.find(key);
      if (it != s_contextCache.end())
      {
         return it->second;
      }
      s_contextCache[key] = pContext;
   }

   if (cacheable)
      LOG_DEBUG_MESSAGE("Created new SSL context (verify=" +
                        std::string(verify ? "true" : "false") +
                        ", customCA=" +
                        std::string(certificateAuthority.empty() ? "false" : "true") + ") - cache size: " + std::to_string(size));
   else
      LOG_DEBUG_MESSAGE("Error creating new SSL context - not caching (verify=" +
                        std::string(verify ? "true" : "false") +
                        ", customCA=" +
                        std::string(certificateAuthority.empty() ? "false" : "true") + ") - cache size: " + std::to_string(size));

   return pContext;
}

boost::shared_ptr<boost::asio::ssl::context> SslContextCache::createContext(
   bool verify,
   const std::string& certificateAuthority,
   bool *pCacheable)
{
   auto pContext = boost::make_shared<boost::asio::ssl::context>(
      boost::asio::ssl::context::sslv23_client);

   *pCacheable = initializeSslContext(pContext.get(), verify, certificateAuthority);

   return pContext;
}

void SslContextCache::removeCertFromCache(bool verify, const std::string& certAuthority)
{
   bool found = false;
   int size = 0;
   {
      ContextKey key{verify, certAuthority};

      std::lock_guard<std::mutex> lock(s_cacheMutex);
      auto it = s_contextCache.find(key);
      if (it != s_contextCache.end())
      {   
         found = true; 
         s_contextCache.erase(it);
      }
      size = s_contextCache.size();
   }

   // Logging after the mutex is released
   if (!found)
      LOG_DEBUG_MESSAGE("No cached SSL context for: cert - cacheSize: " + std::to_string(size));
   else
      LOG_DEBUG_MESSAGE("Removed cached SSL context for: cert - cacheSize: " + std::to_string(size));
}

void SslContextCache::clearCache()
{
   std::lock_guard<std::mutex> lock(s_cacheMutex);
   s_contextCache.clear();
   LOG_DEBUG_MESSAGE("Cleared SSL context cache");
}

} // namespace ssl
} // namespace http
} // namespace core
} // namespace rstudio
