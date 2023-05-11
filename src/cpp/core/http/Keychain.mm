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

#import <AppKit/NSApplication.h>

#define kRootCertsKeychainPath "/System/Library/Keychains/SystemRootCertificates.keychain"

namespace rstudio {
namespace core {
namespace http {

std::vector<KeychainCertificateData> getKeychainCertificates()
{
   // some up-front declarations
   OSStatus status;
   std::vector<KeychainCertificateData> certificates;
 
   // root certificates are not included by default in SecItemCopyMatching searches,
   // so we need to explicitly add that keychain to the search list
   SecKeychainRef rootCertsKeychain;
   status = SecKeychainOpen(kRootCertsKeychainPath, &rootCertsKeychain);
   if (status != errSecSuccess)
   {
      LOG_ERROR_MESSAGE("Could not open system root certificate store");
      return certificates;
   }
   
   // read current (default) search list
   CFArrayRef currentSearchList;
   SecKeychainCopySearchList(&currentSearchList);
   
   // copy to mutable array (so we can append root certs)
   NSMutableArray* searchList = [(NSArray*) currentSearchList mutableCopy];
   [searchList addObject: (id) rootCertsKeychain];
   
   // build our query
   NSDictionary* query = @{
      (id) kSecMatchSearchList: (id) searchList,
      (id) kSecClass:           (id) kSecClassCertificate,
      (id) kSecMatchLimit:      (id) kSecMatchLimitAll,
      (id) kSecReturnRef:       @YES
   };
   
   // execute the query
   CFArrayRef certs;
   OSStatus result = SecItemCopyMatching((CFDictionaryRef) query, (CFTypeRef*) &certs);
   
   // release values required by the query
   [searchList release];
   CFRelease(currentSearchList);
   CFRelease(rootCertsKeychain);
   
   // check for and bail on failure
   if (result != errSecSuccess)
   {
      LOG_ERROR_MESSAGE("Could not search keychains: error code " + safe_convert::numberToString(result));
      return certificates;
   }
   
   // iterate and copy certificate data
   for (CFIndex i = 0; i < CFArrayGetCount(certs); ++i)
   {
      // copy the certificate data into a raw byte representation
      // (these will be converted by OpenSSL appropriately later)
      SecCertificateRef cert = (SecCertificateRef) CFArrayGetValueAtIndex(certs, i);
      CFDataRef certData = SecCertificateCopyData(cert);
      
      CFRange range;
      range.location = 0;
      range.length = CFDataGetLength(certData);
      
      struct KeychainCertificateData keychainCertData;
      keychainCertData.size = range.length;
      keychainCertData.data = boost::shared_ptr<unsigned char>(new unsigned char[range.length]);
      
      CFDataGetBytes(certData, range, keychainCertData.data.get());
      CFRelease(certData);
      
      certificates.push_back(keychainCertData);
   }

   CFRelease(certs);
   return certificates;
}

} // namespace http
} // namespace core
} // namespace rstudio
