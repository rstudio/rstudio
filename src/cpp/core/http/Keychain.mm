/*
 * Keychain.mm
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/http/Keychain.hpp>
#include <core/Log.hpp>

#include <shared_core/SafeConvert.hpp>

#import <AppKit/NSApplication.h>

namespace rstudio {
namespace core {
namespace http {

std::vector<KeychainCertificateData> getKeychainCertificates()
{
   std::vector<KeychainCertificateData> certificates;

   NSDictionary* query =
         @{static_cast<id>(kSecClass): static_cast<id>(kSecClassCertificate),
           static_cast<id>(kSecMatchLimit): static_cast<id>(kSecMatchLimitAll),
           static_cast<id>(kSecReturnRef): @YES};

   NSArray* certs = nil;
   OSStatus result = SecItemCopyMatching(static_cast<CFDictionaryRef>(query),
                                         (CFTypeRef*)&certs); // C-Style cast required here
   if (result != errSecSuccess)
   {
       LOG_ERROR_MESSAGE("Could not search keychains: error code " + safe_convert::numberToString(result));
       return certificates;
   }

   for (unsigned int i = 0; i < [certs count]; ++i)
   {
      SecCertificateRef cert = reinterpret_cast<SecCertificateRef>([certs objectAtIndex:i]);
      CFDataRef certData = SecCertificateCopyData(cert);

      CFRange range;
      range.location = 0;
      range.length = CFDataGetLength(certData);

      struct KeychainCertificateData keychainCertData;
      keychainCertData.size = range.length;
      keychainCertData.data = boost::shared_ptr<unsigned char>(new unsigned char[range.length]);

      CFDataGetBytes(certData, range, keychainCertData.data.get());

      certificates.push_back(keychainCertData);
   }

   [certs release];
   return certificates;
}

} // namespace http
} // namespace core
} // namespace rstudio

