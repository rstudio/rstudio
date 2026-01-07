/*
 * SslContextCache.hpp
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

#ifndef CORE_HTTP_SSL_CONTEXT_CACHE_HPP
#define CORE_HTTP_SSL_CONTEXT_CACHE_HPP

#include <map>
#include <mutex>
#include <memory>
#include <string>

#include "BoostAsioSsl.hpp"

namespace rstudio {
namespace core {
namespace http {
namespace ssl {

/**
 * Cache for SSL contexts to avoid repeated expensive initialization.
 * 
 * SSL contexts are expensive to create because they:
 * - Load and parse system CA certificate bundles (100+ certs)
 * - Cause lock contention in OpenSSL when accessed from multiple threads
 * 
 * This cache allows contexts to be created once and reused across connections.
 * The contexts themselves are thread-safe for creating SSL sessions.
 */
class SslContextCache
{
public:
   /**
    * Get or create an SSL context with the specified configuration.
    * 
    * @param verify Whether to verify SSL certificates
    * @param certificateAuthority Optional CA certificate (PEM format)
    * @return Shared pointer to an SSL context configured as requested
    */
   static boost::shared_ptr<boost::asio::ssl::context> getContext(
      bool verify,
      const std::string& certificateAuthority = std::string());

   /**
    * Clear all cached contexts. Useful for testing or reconfiguration.
    */
   static void clearCache();

   /*
    * Remove the ssl context for a specific certAuthority that was used
    */
   static void removeCertFromCache(bool verify, const std::string& certAuthority);

private:
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

   static boost::shared_ptr<boost::asio::ssl::context> createContext(
      bool verify,
      const std::string& certificateAuthority,
      bool *pCacheable);

   static std::mutex cacheMutex_;
   static std::map<ContextKey, boost::shared_ptr<boost::asio::ssl::context>> contextCache_;
};

} // namespace ssl
} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_SSL_CONTEXT_CACHE_HPP
