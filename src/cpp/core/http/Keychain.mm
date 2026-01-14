/*
 * Keychain.mm
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

#include <core/http/Keychain.hpp>
#include <core/Log.hpp>

#include <shared_core/SafeConvert.hpp>

#include <set>

#import <AppKit/NSApplication.h>

namespace rstudio {
namespace core {
namespace http {

namespace {

void appendCertificateData(SecCertificateRef cert,
                           std::vector<KeychainCertificateData>& certificates,
                           std::set<std::vector<unsigned char>>& seen)
{
   if (cert == nullptr)
      return;

   CFDataRef certData = SecCertificateCopyData(cert);
   if (certData == nullptr)
      return;

   CFIndex length = CFDataGetLength(certData);
   const unsigned char* bytes = CFDataGetBytePtr(certData);

   // Check for duplicate
   std::vector<unsigned char> certBytes(bytes, bytes + length);
   if (seen.find(certBytes) != seen.end())
   {
      CFRelease(certData);
      return;
   }
   seen.insert(certBytes);

   struct KeychainCertificateData keychainCertData;
   keychainCertData.size = length;
   keychainCertData.data = boost::shared_ptr<unsigned char>(new unsigned char[length]);

   memcpy(keychainCertData.data.get(), bytes, length);
   CFRelease(certData);

   certificates.push_back(keychainCertData);
}

} // anonymous namespace

std::vector<KeychainCertificateData> getKeychainCertificates()
{
   std::vector<KeychainCertificateData> certificates;
   std::set<std::vector<unsigned char>> seen;

   // Get system trusted root certificates using the modern API
   CFArrayRef rootCerts = nullptr;
   OSStatus status = SecTrustCopyAnchorCertificates(&rootCerts);
   if (status == errSecSuccess && rootCerts != nullptr)
   {
      for (CFIndex i = 0; i < CFArrayGetCount(rootCerts); ++i)
      {
         SecCertificateRef cert = (SecCertificateRef) CFArrayGetValueAtIndex(rootCerts, i);
         appendCertificateData(cert, certificates, seen);
      }
      CFRelease(rootCerts);
   }
   else
   {
      LOG_ERROR_MESSAGE("Could not get system root certificates: error code " + safe_convert::numberToString(status));
   }

   // Get certificates from user/system keychains
   NSDictionary* query = @{
      (id) kSecClass:           (id) kSecClassCertificate,
      (id) kSecMatchLimit:      (id) kSecMatchLimitAll,
      (id) kSecReturnRef:       @YES
   };

   CFArrayRef certs = nullptr;
   OSStatus result = SecItemCopyMatching((CFDictionaryRef) query, (CFTypeRef*) &certs);

   if (result != errSecSuccess)
   {
      // errSecItemNotFound is not an error - just means no additional certs in keychains
      if (result != errSecItemNotFound)
      {
         LOG_ERROR_MESSAGE("Could not search keychains: error code " + safe_convert::numberToString(result));
      }
      return certificates;
   }

   // iterate and copy certificate data
   for (CFIndex i = 0; i < CFArrayGetCount(certs); ++i)
   {
      SecCertificateRef cert = (SecCertificateRef) CFArrayGetValueAtIndex(certs, i);
      appendCertificateData(cert, certificates, seen);
   }

   CFRelease(certs);
   return certificates;
}

} // namespace http
} // namespace core
} // namespace rstudio
